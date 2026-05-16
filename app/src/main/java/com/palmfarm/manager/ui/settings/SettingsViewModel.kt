package com.palmfarm.manager.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.PalmFarmApp
import com.palmfarm.manager.data.database.dao.AppSettingsDao
import com.palmfarm.manager.data.database.dao.FarmDao
import com.palmfarm.manager.data.database.dao.WorkerDao
import com.palmfarm.manager.data.database.entities.AppSettings
import com.palmfarm.manager.data.database.entities.Farm
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.domain.usecases.BackupRestoreUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for Settings screen
 */
class SettingsViewModel(
    private val appSettingsDao: AppSettingsDao,
    private val farmDao: FarmDao,
    private val workerDao: WorkerDao,
    private val backupRestoreUseCase: BackupRestoreUseCase
) : ViewModel() {

    companion object {
        private const val DEFAULT_SEASON_START_MONTH = 10
    }

    private suspend fun getOrCreateSettings(): AppSettings {
        return appSettingsDao.getSettingsOnce() ?: defaultSettings().also {
            appSettingsDao.insert(it)
        }
    }

    private fun defaultSettings() = AppSettings(
        id = 1,
        enterpriseName = "",
        location = "",
        enterprisePhone = "",
        tonnage = 0.0,
        authMethod = "NONE",
        passwordHash = null,
        seasonStartMonth = DEFAULT_SEASON_START_MONTH,
        currentCycleExpectedBunches = 0
    )

    private suspend fun upsertSettings(transform: (AppSettings) -> AppSettings) {
        val current = getOrCreateSettings()
        val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
        appSettingsDao.insert(updated)
    }

    /**
     * Get app settings
     */
    val appSettings: Flow<AppSettings?> = appSettingsDao.getSettings()

    /**
     * Get all farms
     */
    val farms: Flow<List<Farm>> = farmDao.getAllFarms()

    /**
     * Get all workers
     */
    val workers: Flow<List<Worker>> = workerDao.getAllWorkers()

    /**
     * Get active workers
     */
    val activeWorkers: Flow<List<Worker>> = workerDao.getActiveWorkers()

    /**
     * Get total palm count across all farms
     */
    val totalPalms: Flow<Int> = farms.map { farmList ->
        farmList.sumOf { it.totalPalms }
    }

    /**
     * Backup database
     */
    suspend fun backupDatabase(): Result<File> {
        return backupRestoreUseCase.backupDatabase()
    }

    /**
     * Restore database from file
     */
    suspend fun restoreDatabase(backupFile: File): Result<Unit> {
        return backupRestoreUseCase.restoreDatabase(backupFile)
    }

    suspend fun restoreDatabaseFromUri(backupUri: Uri): Result<Unit> {
        return backupRestoreUseCase.restoreDatabaseFromUri(backupUri)
    }

    /**
     * Get list of backup files
     */
    suspend fun getBackupFiles(): Result<List<File>> {
        return backupRestoreUseCase.getBackupFiles()
    }

    /**
     * Get backup file info
     */
    fun getBackupInfo(file: File): BackupFileInfo {
        return BackupFileInfo(
            file = file,
            timestamp = backupRestoreUseCase.getBackupTimestamp(file),
            sizeMB = backupRestoreUseCase.getBackupFileSize(file)
        )
    }

    /**
     * Save or update enterprise information
     * User must provide all required settings
     */
    fun saveEnterpriseInfo(
        enterpriseName: String,
        location: String,
        phone: String,
        seasonStartMonth: Int,
        currentCycleExpectedBunches: Int,
        tonnage: Double
    ) {
        viewModelScope.launch {
            upsertSettings { current ->
                current.copy(
                    enterpriseName = enterpriseName.trim(),
                    location = location.trim(),
                    enterprisePhone = phone.trim(),
                    seasonStartMonth = seasonStartMonth.coerceIn(1, 12),
                    currentCycleExpectedBunches = currentCycleExpectedBunches.coerceAtLeast(1),
                    tonnage = tonnage
                )
            }
        }
    }

    /**
     * Update production settings
     */
    fun updateProductionSettings(
        seasonStartMonth: Int,
        currentCycleExpectedBunches: Int,
        tonnage: Double
    ) {
        viewModelScope.launch {
            upsertSettings { current ->
                current.copy(
                    seasonStartMonth = seasonStartMonth.coerceIn(1, 12),
                    currentCycleExpectedBunches = currentCycleExpectedBunches.coerceAtLeast(1),
                    tonnage = tonnage
                )
            }
        }
    }

    fun updateEnterpriseName(name: String) {
        viewModelScope.launch {
            upsertSettings { current -> current.copy(enterpriseName = name) }
        }
    }

    fun updateEnterpriseLocation(location: String) {
        viewModelScope.launch {
            upsertSettings { current -> current.copy(location = location) }
        }
    }

    fun updateEnterprisePhone(phone: String) {
        viewModelScope.launch {
            upsertSettings { current -> current.copy(enterprisePhone = phone) }
        }
    }

    fun updateSeasonStartMonth(month: Int) {
        if (month !in 1..12) return
        viewModelScope.launch {
            upsertSettings { current -> current.copy(seasonStartMonth = month) }
            PalmFarmApp.getInstance().scheduleCycleReconfiguration(month)
        }
    }

    fun updateExpectedBunches(bunches: Int?) {
        viewModelScope.launch {
            val value = (bunches ?: 0).coerceAtLeast(1)
            upsertSettings { current -> current.copy(currentCycleExpectedBunches = value) }
            PalmFarmApp.getInstance().database.productionCycleDao().updateCurrentCycleExpectedBunches(value)
        }
    }

    fun updateTonnage(tonnage: Double?) {
        viewModelScope.launch {
            val value = tonnage ?: 0.0
            upsertSettings { current -> current.copy(tonnage = value) }
        }
    }

    /**
     * Update authentication method
     */
    fun updateAuthMethod(authMethod: String, passwordHash: String?) {
        viewModelScope.launch {
            val currentSettings = appSettings.firstOrNull()
            if (currentSettings == null) {
                // Minimal with provided
                val newSettings = AppSettings(
                    id = 1,
                    enterpriseName = "",
                    location = "",
                    enterprisePhone = "",
                    tonnage = 0.0,
                    authMethod = authMethod,
                    passwordHash = passwordHash,
                    seasonStartMonth = DEFAULT_SEASON_START_MONTH,
                    currentCycleExpectedBunches = 0
                )
                appSettingsDao.insert(newSettings)
            } else {
                val updatedSettings = currentSettings.copy(
                    authMethod = authMethod,
                    passwordHash = passwordHash,
                    updatedAt = System.currentTimeMillis()
                )
                appSettingsDao.update(updatedSettings)
            }
        }
    }

    /**
     * Farm Management
     */
    fun saveFarm(farm: Farm) {
        viewModelScope.launch {
            if (farm.id == 0) {
                farmDao.insert(farm)
            } else {
                farmDao.update(farm)
            }
        }
    }

    fun deleteFarm(farmId: Int) {
        viewModelScope.launch {
            farmDao.deleteById(farmId)
        }
    }

    suspend fun getFarmById(farmId: Int): Farm? {
        return farmDao.getFarmById(farmId)
    }

    /**
     * Worker Management
     */
    fun saveWorker(worker: Worker) {
        viewModelScope.launch {
            if (worker.id == 0) {
                workerDao.insert(worker)
            } else {
                workerDao.update(worker)
            }
        }
    }

    fun deleteWorker(workerId: Int): Result<Unit> {
        return try {
            viewModelScope.launch {
                workerDao.deleteById(workerId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkerById(workerId: Int): Worker? {
        return workerDao.getWorkerById(workerId)
    }

    /**
     * Validate farm data
     */
    fun validateFarm(farm: Farm): ValidationResult {
        if (farm.name.isEmpty()) {
            return ValidationResult(isValid = false, message = "Farm name is required")
        }

        if (farm.productivePalms <= 0 && farm.unproductivePalms <= 0) {
            return ValidationResult(isValid = false, message = "At least one palm must exist")
        }

        return ValidationResult(isValid = true, message = "Valid")
    }

    /**
     * Validate worker data
     */
    fun validateWorker(worker: Worker): ValidationResult {
        if (worker.firstName.isEmpty()) {
            return ValidationResult(isValid = false, message = "Worker name is required")
        }

        worker.phoneNumber?.let { phone ->
            if (phone.isNotEmpty()) {
                // Basic phone validation for Cameroon (+237 XXX XXX XXX)
                val phonePattern = Regex("^\\+237\\d{9}$|^\\d{9}$")
                if (!phonePattern.matches(phone)) {
                    return ValidationResult(isValid = false, message = "Invalid phone number format")
                }
            }
        }

        return ValidationResult(isValid = true, message = "Valid")
    }

    /**
     * Validate enterprise settings
     */
    fun validateEnterpriseSettings(
        enterpriseName: String,
        location: String,
        seasonStartMonth: Int,
        currentCycleExpectedBunches: Int,
        tonnage: Double
    ): ValidationResult {
        if (enterpriseName.isEmpty()) {
            return ValidationResult(isValid = false, message = "Enterprise name is required")
        }

        if (location.isEmpty()) {
            return ValidationResult(isValid = false, message = "Location is required")
        }

        if (seasonStartMonth < 1 || seasonStartMonth > 12) {
            return ValidationResult(isValid = false, message = "Season start month must be between 1 and 12")
        }

        if (currentCycleExpectedBunches < 0) {
            return ValidationResult(isValid = false, message = "Expected bunches cannot be negative")
        }

        if (tonnage <= 0) {
            return ValidationResult(isValid = false, message = "Tonnage must be greater than 0")
        }

        return ValidationResult(isValid = true, message = "Valid")
    }
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

/**
 * Backup file information
 */
data class BackupFileInfo(
    val file: File,
    val timestamp: String,
    val sizeMB: Double
)