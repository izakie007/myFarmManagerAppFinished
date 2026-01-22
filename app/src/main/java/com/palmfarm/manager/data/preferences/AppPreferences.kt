package com.palmfarm.manager.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.palmfarm.manager.utils.Constants

/**
 * Encrypted SharedPreferences for storing app preferences
 * Used for authentication state, last backup time, etc.
 */
class AppPreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        Constants.PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Save last backup timestamp
     */
    fun saveLastBackupTime(timestamp: Long) {
        sharedPreferences.edit().putLong(Constants.PREF_LAST_BACKUP_TIME, timestamp).apply()
    }

    /**
     * Get last backup timestamp
     */
    fun getLastBackupTime(): Long {
        return sharedPreferences.getLong(Constants.PREF_LAST_BACKUP_TIME, 0L)
    }

    /**
     * Save failed authentication attempts
     */
    fun saveFailedAuthAttempts(count: Int) {
        sharedPreferences.edit().putInt(Constants.PREF_FAILED_AUTH_ATTEMPTS, count).apply()
    }

    /**
     * Get failed authentication attempts
     */
    fun getFailedAuthAttempts(): Int {
        return sharedPreferences.getInt(Constants.PREF_FAILED_AUTH_ATTEMPTS, 0)
    }

    /**
     * Increment failed authentication attempts
     */
    fun incrementFailedAuthAttempts(): Int {
        val current = getFailedAuthAttempts()
        val newCount = current + 1
        saveFailedAuthAttempts(newCount)
        return newCount
    }

    /**
     * Reset failed authentication attempts
     */
    fun resetFailedAuthAttempts() {
        saveFailedAuthAttempts(0)
    }

    /**
     * Save lockout time
     */
    fun saveLockoutTime(timestamp: Long) {
        sharedPreferences.edit().putLong(Constants.PREF_LOCKOUT_TIME, timestamp).apply()
    }

    /**
     * Get lockout time
     */
    fun getLockoutTime(): Long {
        return sharedPreferences.getLong(Constants.PREF_LOCKOUT_TIME, 0L)
    }

    /**
     * Check if account is currently locked out
     */
    fun isLockedOut(): Boolean {
        val lockoutTime = getLockoutTime()
        if (lockoutTime == 0L) return false

        val currentTime = System.currentTimeMillis()
        val timeSinceLockout = currentTime - lockoutTime

        return if (timeSinceLockout < Constants.LOCKOUT_DURATION_MS) {
            true
        } else {
            // Lockout period has passed, clear lockout
            saveLockoutTime(0L)
            resetFailedAuthAttempts()
            false
        }
    }

    /**
     * Get remaining lockout time in seconds
     */
    fun getRemainingLockoutTimeSeconds(): Int {
        val lockoutTime = getLockoutTime()
        if (lockoutTime == 0L) return 0

        val currentTime = System.currentTimeMillis()
        val timeSinceLockout = currentTime - lockoutTime
        val remaining = Constants.LOCKOUT_DURATION_MS - timeSinceLockout

        return if (remaining > 0) {
            (remaining / 1000).toInt()
        } else {
            0
        }
    }

    /**
     * Save custom preference string
     */
    fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    /**
     * Get custom preference string
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * Save custom preference boolean
     */
    fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Get custom preference boolean
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * Save custom preference int
     */
    fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    /**
     * Get custom preference int
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    /**
     * Save custom preference long
     */
    fun saveLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    /**
     * Get custom preference long
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    /**
     * Clear all preferences
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Remove specific preference
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}
