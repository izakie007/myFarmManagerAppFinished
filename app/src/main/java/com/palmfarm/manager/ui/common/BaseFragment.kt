package com.palmfarm.manager.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Base Fragment class with common functionality
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    abstract fun setupViews()

    abstract fun setupObservers()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Show loading indicator
     */
    protected open fun showLoading() {
        // Override in subclasses to show loading UI
    }

    /**
     * Hide loading indicator
     */
    protected open fun hideLoading() {
        // Override in subclasses to hide loading UI
    }

    /**
     * Show error message
     */
    protected open fun showError(message: String) {
        // Override in subclasses to show error UI
        view?.let {
            com.google.android.material.snackbar.Snackbar
                .make(it, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .show()
        }
    }

    /**
     * Show success message
     */
    protected open fun showSuccess(message: String) {
        view?.let {
            com.google.android.material.snackbar.Snackbar
                .make(it, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Show toast message
     */
    protected fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * Show empty state
     */
    protected open fun showEmptyState(message: String) {
        // Override in subclasses to show empty state UI
    }

    /**
     * Hide empty state
     */
    protected open fun hideEmptyState() {
        // Override in subclasses to hide empty state UI
    }
}
