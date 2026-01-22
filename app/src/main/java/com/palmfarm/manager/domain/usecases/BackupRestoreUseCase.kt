package com.palmfarm.manager.domain.usecases

import android.content.Context
import android.os.Environment
import android.util.Log
import com.palmfarm.manager.data.database.AppDatabase
import com.palmfarm.manager.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Use case for database backup and restore
 */
class BackupRestoreUseCase(
    private val context: Context,
    private val database: AppDatabase
) {

    /**
     * Backup database to external storage
     */
    suspend fun backupDatabase(): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Get database file path
            val (dbFile, walFile, shmFile) = getDatabaseFiles()

            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found"))
            }

            // Create backup directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir == null || !downloadsDir.exists()) {
                return@withContext Result.failure(Exception("Downloads directory not accessible. Please grant storage permission."))
            }

            val backupDir = File(downloadsDir, Constants.BACKUP_FOLDER)

            if (!backupDir.exists()) {
                val created = backupDir.mkdirs()
                if (!created && !backupDir.exists()) {
                    return@withContext Result.failure(Exception("Failed to create backup directory"))
                }
            }

            // Create backup file name with timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss_SSS", Locale.getDefault())
                .format(Date())
            val backupFile = File(backupDir, "${Constants.BACKUP_FILE_PREFIX}$timestamp.zip")

            // Checkpoint WAL file before backup
            try {
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            } catch (e: Exception) {
                // Log but continue - WAL checkpoint failure shouldn't prevent backup
                Log.w("BackupRestore", "WAL checkpoint failed, continuing with backup", e)
            }

            // Collect database-related files
            val filesToBackup = listOfNotNull(
                dbFile.takeIf { it.exists() },
                walFile.takeIf { it.exists() },
                shmFile.takeIf { it.exists() }
            )

            // Zip all database files into a single archive
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                filesToBackup.forEach { file ->
                    FileInputStream(file).use { input ->
                        val entry = ZipEntry(file.name)
                        zipOut.putNextEntry(entry)
                        input.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }
            }

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore database from backup file
     */
    suspend fun restoreDatabase(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validate backup file exists
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }

            val tempDir = File(context.cacheDir, "restore_temp").apply {
                deleteRecursively()
                mkdirs()
            }

            val extractedFiles = extractBackupArchive(backupFile, tempDir)
            val (dbFileName, walFileName, shmFileName) = getDatabaseFileNames()

            val extractedDb = File(tempDir, dbFileName)
            if (!extractedDb.exists()) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(Exception("Backup archive missing database file"))
            }

            val extractedWal = File(tempDir, walFileName)
            val extractedShm = File(tempDir, shmFileName)

            val (currentDb, currentWal, currentShm) = getDatabaseFiles()

            // Create safety backup before restoring
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir == null || !downloadsDir.exists()) {
                return@withContext Result.failure(Exception("Downloads directory not accessible. Please grant storage permission."))
            }

            val backupDir = File(downloadsDir, Constants.BACKUP_FOLDER)
            if (!backupDir.exists()) {
                val created = backupDir.mkdirs()
                if (!created && !backupDir.exists()) {
                    return@withContext Result.failure(Exception("Failed to create backup directory"))
                }
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss_SSS", Locale.getDefault())
                .format(Date())
            val safetyBackup = File(backupDir, "${Constants.BACKUP_FILE_PREFIX}BeforeRestore_$timestamp.db")

            if (currentDb.exists()) {
                currentDb.copyTo(safetyBackup, overwrite = false)
            }

            // Close database connections and clear singleton
            database.close()
            // Clear the singleton instance so it can be recreated
            com.palmfarm.manager.data.database.AppDatabase.closeDatabase()

            // Replace current database with backup
            extractedDb.copyTo(currentDb, overwrite = true)

            if (extractedWal.exists()) {
                extractedWal.copyTo(currentWal, overwrite = true)
            } else if (currentWal.exists()) {
                currentWal.delete()
            }

            if (extractedShm.exists()) {
                extractedShm.copyTo(currentShm, overwrite = true)
            } else if (currentShm.exists()) {
                currentShm.delete()
            }

            // Database will be automatically recreated when next accessed via AppDatabase.getDatabase()
            // No need to manually reopen - Room will handle it

            tempDir.deleteRecursively()

            Result.success(Unit)
        } catch (e: Exception) {
            // Attempt to restore from safety backup if available
            try {
                val currentDb = context.getDatabasePath("palm_farm_db")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null && downloadsDir.exists()) {
                    val backupDir = File(downloadsDir, Constants.BACKUP_FOLDER)
                    if (backupDir.exists()) {
                val safetyBackups = backupDir.listFiles { file ->
                    file.name.startsWith("${Constants.BACKUP_FILE_PREFIX}BeforeRestore_")
                }?.sortedByDescending { it.lastModified() }

                if (safetyBackups?.isNotEmpty() == true) {
                    val latestSafetyBackup = safetyBackups.first()
                    latestSafetyBackup.copyTo(currentDb, overwrite = true)
                            // Clear singleton so database can be recreated
                            com.palmfarm.manager.data.database.AppDatabase.closeDatabase()
                        }
                    }
                }
            } catch (restoreError: Exception) {
                // Ignore restore error - log but don't fail
                Log.e("BackupRestore", "Failed to restore from safety backup", restoreError)
            }

            Result.failure(e)
        }
    }

    /**
     * Get list of all backup files
     */
    suspend fun getBackupFiles(): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir == null || !downloadsDir.exists()) {
                return@withContext Result.failure(Exception("Downloads directory not accessible. Please grant storage permission."))
            }

            val backupDir = File(downloadsDir, Constants.BACKUP_FOLDER)

            if (!backupDir.exists()) {
                return@withContext Result.success(emptyList())
            }

            val backupFiles = backupDir.listFiles { file ->
                file.name.startsWith(Constants.BACKUP_FILE_PREFIX) && file.name.endsWith(".zip")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            Result.success(backupFiles.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a backup file
     */
    suspend fun deleteBackupFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get backup file size in MB
     */
    fun getBackupFileSize(file: File): Double {
        return file.length() / (1024.0 * 1024.0)
    }

    /**
     * Get formatted timestamp from backup file name
     */
    fun getBackupTimestamp(file: File): String {
        val fileName = file.nameWithoutExtension
        val parts = fileName.split("_")

        if (parts.size >= 3) {
            val date = parts[1] // yyyy-MM-dd
            val time = parts[2] // HHmmss

            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
                val displayFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                val parsedDate = dateFormat.parse("${date}_$time")
                return displayFormat.format(parsedDate ?: Date())
            } catch (e: Exception) {
                return fileName
            }
        }

        return fileName
    }

    private fun getDatabaseFileNames(): Triple<String, String, String> {
        val name = database.openHelper.databaseName ?: DEFAULT_DB_NAME
        return Triple(name, "$name-wal", "$name-shm")
    }

    private fun getDatabaseFiles(): Triple<File, File, File> {
        val (name, walName, shmName) = getDatabaseFileNames()
        val dbFile = context.getDatabasePath(name)
        val parent = dbFile.parentFile
            ?: throw IllegalStateException("Database parent directory is null")
        val walFile = File(parent, walName)
        val shmFile = File(parent, shmName)
        return Triple(dbFile, walFile, shmFile)
    }

    private fun extractBackupArchive(zipFile: File, destination: File): List<File> {
        val extracted = mutableListOf<File>()
        try {
        ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                    // Security: Prevent zip slip vulnerability
                val outFile = File(destination, entry.name)
                    val canonicalPath = outFile.canonicalPath
                    val canonicalDest = destination.canonicalPath
                    if (!canonicalPath.startsWith(canonicalDest + File.separator)) {
                        throw SecurityException("Invalid zip entry path: ${entry.name}")
                    }
                    
                    // Ensure parent directory exists
                    outFile.parentFile?.mkdirs()
                    
                FileOutputStream(outFile).use { output ->
                    zipIn.copyTo(output)
                }
                extracted.add(outFile)
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            }
        } catch (e: Exception) {
            // Clean up partially extracted files
            extracted.forEach { it.delete() }
            throw Exception("Failed to extract backup archive: ${e.message}", e)
        }
        return extracted
    }

    companion object {
        private const val DEFAULT_DB_NAME = "palm_farm_db"
    }
}
