package com.palmfarm.manager.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.PalmFarmApp
import com.palmfarm.manager.data.preferences.AppPreferences
import com.palmfarm.manager.utils.Constants
import com.palmfarm.manager.utils.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for authentication
 */
class AuthViewModel : ViewModel() {

    private val database = PalmFarmApp.getInstance().database
    private val appSettingsDao = database.appSettingsDao()
    private val appPreferences = AppPreferences(PalmFarmApp.getInstance())

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _authMethod = MutableLiveData<String>()
    val authMethod: LiveData<String> = _authMethod

    private val _lockoutState = MutableLiveData<LockoutState>()
    val lockoutState: LiveData<LockoutState> = _lockoutState

    sealed class AuthState {
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
        object BiometricRequired : AuthState()
        object PasswordRequired : AuthState()
        object NoAuthRequired : AuthState()
    }

    data class LockoutState(
        val isLockedOut: Boolean,
        val remainingSeconds: Int
    )

    init {
        loadAuthMethod()
        checkLockoutStatus()
    }

    /**
     * Load authentication method from database
     */
    private fun loadAuthMethod() {
        viewModelScope.launch {
            try {
                val settings = withContext(Dispatchers.IO) {
                    appSettingsDao.getSettingsOnce()
                }

                val method = settings?.authMethod ?: Constants.AUTH_METHOD_NONE
                _authMethod.value = method

                when (method) {
                    Constants.AUTH_METHOD_NONE -> _authState.value = AuthState.NoAuthRequired
                    Constants.AUTH_METHOD_BIOMETRIC -> _authState.value = AuthState.BiometricRequired
                    Constants.AUTH_METHOD_PASSWORD -> _authState.value = AuthState.PasswordRequired
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to load authentication settings")
            }
        }
    }

    /**
     * Check lockout status
     */
    fun checkLockoutStatus() {
        val isLockedOut = appPreferences.isLockedOut()
        val remainingSeconds = appPreferences.getRemainingLockoutTimeSeconds()
        _lockoutState.value = LockoutState(isLockedOut, remainingSeconds)
    }

    /**
     * Validate password
     */
    fun validatePassword(enteredPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // Check if locked out
            if (appPreferences.isLockedOut()) {
                val remaining = appPreferences.getRemainingLockoutTimeSeconds()
                _authState.value = AuthState.Error("Too many failed attempts. Try again in $remaining seconds")
                checkLockoutStatus()
                return@launch
            }

            try {
                val settings = withContext(Dispatchers.IO) {
                    appSettingsDao.getSettingsOnce()
                }

                val storedHash = settings?.passwordHash

                if (storedHash == null) {
                    _authState.value = AuthState.Error("Password not set")
                    return@launch
                }

                val isValid = withContext(Dispatchers.IO) {
                    PasswordHasher.verifyPassword(enteredPassword, storedHash)
                }

                if (isValid) {
                    // Success - reset failed attempts
                    appPreferences.resetFailedAuthAttempts()
                    _authState.value = AuthState.Success
                } else {
                    // Failed attempt
                    val failedAttempts = appPreferences.incrementFailedAuthAttempts()

                    if (failedAttempts >= Constants.MAX_PASSWORD_ATTEMPTS) {
                        // Lock out user
                        appPreferences.saveLockoutTime(System.currentTimeMillis())
                        checkLockoutStatus()
                        _authState.value = AuthState.Error("Too many failed attempts. Account locked for 30 seconds")
                    } else {
                        val remaining = Constants.MAX_PASSWORD_ATTEMPTS - failedAttempts
                        _authState.value = AuthState.Error("Invalid password. $remaining attempts remaining")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Authentication failed: ${e.message}")
            }
        }
    }

    /**
     * Handle biometric authentication success
     */
    fun onBiometricSuccess() {
        _authState.value = AuthState.Success
    }

    /**
     * Handle biometric authentication error
     */
    fun onBiometricError(errorMessage: String) {
        _authState.value = AuthState.Error(errorMessage)
    }

    /**
     * Handle biometric authentication failed (not an error, just failed to recognize)
     */
    fun onBiometricFailed() {
        _authState.value = AuthState.Error("Biometric authentication failed. Try again")
    }

    /**
     * Get number of failed attempts
     */
    fun getFailedAttempts(): Int {
        return appPreferences.getFailedAuthAttempts()
    }

    /**
     * Reset authentication state
     */
    fun resetAuthState() {
        loadAuthMethod()
        checkLockoutStatus()
    }

    /**
     * Reload auth method (useful when biometric fails and we need to check for password fallback)
     */
    fun reloadAuthMethod() {
        loadAuthMethod()
    }

    /**
     * Allow fallback to password when biometric fails
     * This checks if password is set and allows switching to password auth
     */
    fun allowPasswordFallback() {
        viewModelScope.launch {
            try {
                val settings = withContext(Dispatchers.IO) {
                    appSettingsDao.getSettingsOnce()
                }
                
                // If password is set, allow fallback to password
                if (settings?.passwordHash != null) {
                    _authState.value = AuthState.PasswordRequired
                } else {
                    // No password available, show error
                    _authState.value = AuthState.Error("Biometric authentication failed and no password is set")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to check authentication options")
            }
        }
    }
}
