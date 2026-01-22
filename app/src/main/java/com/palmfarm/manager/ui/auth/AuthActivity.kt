package com.palmfarm.manager.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.palmfarm.manager.databinding.ActivityAuthBinding
import com.palmfarm.manager.ui.main.MainActivity
import com.palmfarm.manager.utils.BiometricUtils
import com.palmfarm.manager.utils.Constants
import com.palmfarm.manager.utils.gone
import com.palmfarm.manager.utils.hideKeyboard
import com.palmfarm.manager.utils.showToast
import com.palmfarm.manager.utils.visible

/**
 * Authentication activity - handles password and biometric authentication
 */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()
    private var lockoutTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        // Observe authentication state
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    showLoading(true)
                }
                is AuthViewModel.AuthState.Success -> {
                    showLoading(false)
                    onAuthenticationSuccess()
                }
                is AuthViewModel.AuthState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
                is AuthViewModel.AuthState.BiometricRequired -> {
                    showBiometricUI()
                }
                is AuthViewModel.AuthState.PasswordRequired -> {
                    showPasswordUI()
                }
                is AuthViewModel.AuthState.NoAuthRequired -> {
                    onAuthenticationSuccess()
                }
            }
        }

        // Observe lockout state
        viewModel.lockoutState.observe(this) { lockoutState ->
            if (lockoutState.isLockedOut) {
                showLockoutUI(lockoutState.remainingSeconds)
            } else {
                hideLockoutUI()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnUnlock.setOnClickListener {
            val password = binding.etPassword.text.toString()
            if (password.isBlank()) {
                binding.etPassword.error = "Please enter password"
                return@setOnClickListener
            }
            binding.root.hideKeyboard()
            viewModel.validatePassword(password)
        }

        binding.btnBiometric.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun showPasswordUI() {
        binding.layoutPassword.visible()
        binding.layoutBiometric.gone()
        binding.tvTitle.text = "Enter Password"
        binding.etPassword.requestFocus()
    }

    private fun showBiometricUI() {
        // Check if biometric is available
        if (BiometricUtils.isBiometricAvailable(this)) {
            binding.layoutPassword.gone()
            binding.layoutBiometric.visible()
            binding.tvTitle.text = "Biometric Authentication"

            // Auto-show biometric prompt
            showBiometricPrompt()
        } else {
            // Biometric not available, show error and switch to password if available
            val message = BiometricUtils.getBiometricStatusMessage(this)
            showToast(message)
            // Fallback to password authentication if available
            viewModel.allowPasswordFallback()
        }
    }

    private fun showBiometricPrompt() {
        BiometricUtils.showBiometricPrompt(
            activity = this,
            title = "Palm Farm Manager",
            subtitle = "Confirm your fingerprint to continue",
            negativeButtonText = "Use Password",
            onSuccess = {
                viewModel.onBiometricSuccess()
            },
            onError = { errorCode, errorMessage ->
                when (errorCode) {
                    androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED -> {
                        // User canceled or chose to use password - allow fallback
                        viewModel.allowPasswordFallback()
                    }
                    else -> {
                        // Other errors - show error but allow password fallback
                        showToast("Biometric error: $errorMessage")
                        viewModel.allowPasswordFallback()
                    }
                }
            },
            onFailed = {
                // Biometric failed but user can try again or use password
                showToast("Biometric authentication failed. Try again or use password")
            }
        )
    }

    private fun showLockoutUI(remainingSeconds: Int) {
        binding.btnUnlock.isEnabled = false
        binding.btnBiometric.isEnabled = false
        binding.etPassword.isEnabled = false

        // Start countdown timer
        lockoutTimer?.cancel()
        lockoutTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.tvError.text = "Too many failed attempts. Try again in $seconds seconds"
                binding.tvError.visible()
            }

            override fun onFinish() {
                hideLockoutUI()
                viewModel.checkLockoutStatus()
            }
        }.start()
    }

    private fun hideLockoutUI() {
        lockoutTimer?.cancel()
        binding.btnUnlock.isEnabled = true
        binding.btnBiometric.isEnabled = true
        binding.etPassword.isEnabled = true
        binding.tvError.gone()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.apply {
            if (show) visible() else gone()
        }
        binding.btnUnlock.isEnabled = !show
        binding.btnBiometric.isEnabled = !show
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visible()
        binding.etPassword.error = message
    }

    private fun onAuthenticationSuccess() {
        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.resetAuthState()
    }
}
