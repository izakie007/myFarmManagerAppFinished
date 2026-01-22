package com.palmfarm.manager.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Utility functions for file operations
 */
object FileUtils {

    /**
     * Get downloads directory for backups and payslips
     */
    fun getDownloadsDirectory(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    /**
     * Get backup directory
     */
    fun getBackupDirectory(): File {
        val dir = File(getDownloadsDirectory(), Constants.BACKUP_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Get payslip directory
     */
    fun getPayslipDirectory(): File {
        val dir = File(getDownloadsDirectory(), Constants.PAYSLIP_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Get report directory
     */
    fun getReportDirectory(): File {
        val dir = File(getDownloadsDirectory(), Constants.REPORT_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Get receipts directory (internal storage)
     */
    fun getReceiptsDirectory(context: Context): File {
        val dir = File(context.filesDir, Constants.RECEIPT_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Create backup file name
     */
    fun createBackupFileName(): String {
        val timestamp = DateUtils.formatToFileName(DateUtils.getCurrentTimestamp())
        return "${Constants.BACKUP_FILE_PREFIX}$timestamp.db"
    }

    /**
     * Create payslip file name
     */
    fun createPayslipFileName(workerName: String, date: Long): String {
        val formattedDate = DateUtils.formatToFileName(date)
        val safeName = workerName.replace(" ", "")
        return "${Constants.PAYSLIP_FILE_PREFIX}${safeName}_$formattedDate.pdf"
    }

    /**
     * Create report file name
     */
    fun createReportFileName(): String {
        val timestamp = DateUtils.formatToFileName(DateUtils.getCurrentTimestamp())
        return "${Constants.REPORT_FILE_PREFIX}$timestamp.pdf"
    }

    /**
     * Create analytics report file name
     */
    fun createAnalyticsReportFileName(): String {
        val timestamp = DateUtils.formatToFileName(DateUtils.getCurrentTimestamp())
        return "${Constants.ANALYTICS_REPORT_PREFIX}$timestamp.pdf"
    }

    /**
     * Create receipt file name
     */
    fun createReceiptFileName(): String {
        val timestamp = DateUtils.formatToFileName(DateUtils.getCurrentTimestamp())
        return "receipt_$timestamp.jpg"
    }

    /**
     * Copy file
     */
    fun copyFile(source: File, destination: File): Boolean {
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete file
     */
    fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if file exists
     */
    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    /**
     * Get file size in bytes
     */
    fun getFileSize(file: File): Long {
        return if (file.exists()) file.length() else 0
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Check if database file is valid (basic check)
     */
    fun isDatabaseFileValid(file: File): Boolean {
        return try {
            if (!file.exists() || !file.isFile) return false

            // Basic check: SQLite files start with "SQLite format 3\u0000"
            val header = ByteArray(16)
            FileInputStream(file).use { input ->
                input.read(header)
            }

            val headerString = String(header, Charsets.UTF_8)
            headerString.startsWith("SQLite format 3")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all backup files
     */
    fun getBackupFiles(): List<File> {
        val backupDir = getBackupDirectory()
        return backupDir.listFiles { file ->
            file.isFile && file.name.startsWith(Constants.BACKUP_FILE_PREFIX) && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Get all payslip files
     */
    fun getPayslipFiles(): List<File> {
        val payslipDir = getPayslipDirectory()
        return payslipDir.listFiles { file ->
            file.isFile && file.name.startsWith(Constants.PAYSLIP_FILE_PREFIX) && file.name.endsWith(".pdf")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Clean up old files (keep last N files)
     */
    fun cleanupOldFiles(directory: File, keepCount: Int = 10) {
        val files = directory.listFiles()?.sortedByDescending { it.lastModified() } ?: return

        files.drop(keepCount).forEach { file ->
            deleteFile(file)
        }
    }

    /**
     * Get database path
     */
    fun getDatabasePath(context: Context, databaseName: String): File {
        return context.getDatabasePath(databaseName)
    }
}
