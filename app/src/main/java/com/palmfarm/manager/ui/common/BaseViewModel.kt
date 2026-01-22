package com.palmfarm.manager.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Base ViewModel class with common functionality
 */
abstract class BaseViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    /**
     * Show loading state
     */
    protected fun showLoading() {
        _loading.value = true
    }

    /**
     * Hide loading state
     */
    protected fun hideLoading() {
        _loading.value = false
    }

    /**
     * Show error message
     */
    protected fun showError(message: String) {
        _error.value = message
        hideLoading()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Show success message
     */
    protected fun showSuccess(message: String) {
        _success.value = message
        hideLoading()
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _success.value = null
    }

    /**
     * Handle exception
     */
    protected fun handleException(e: Exception, defaultMessage: String = "An error occurred") {
        val message = e.message ?: defaultMessage
        showError(message)
    }

    /**
     * Execute action with loading and error handling
     */
    protected suspend fun executeWithLoading(action: suspend () -> Unit) {
        try {
            showLoading()
            action()
            hideLoading()
        } catch (e: Exception) {
            handleException(e)
        }
    }
}
