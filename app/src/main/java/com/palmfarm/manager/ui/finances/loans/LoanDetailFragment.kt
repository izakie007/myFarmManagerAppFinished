package com.palmfarm.manager.ui.finances.loans

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Loan
import com.palmfarm.manager.databinding.FragmentLoanDetailBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fragment for viewing loan details and recording payments
 */
class LoanDetailFragment : BaseFragment<FragmentLoanDetailBinding>() {

    private val viewModel: LoansViewModel by viewModels { ViewModelFactory.create() }
    private var loanId: Int = 0
    private var currentLoan: Loan? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoanDetailBinding {
        return FragmentLoanDetailBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Get loan ID from arguments
        loanId = arguments?.getInt("loanId") ?: 0

        if (loanId > 0) {
            loadLoanDetails(loanId)
        }

        setupButtons()
    }

    override fun setupObservers() {
        // No specific observers needed - data loaded once
    }

    /**
     * Load loan details
     */
    private fun loadLoanDetails(loanId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val loan = viewModel.getLoanById(loanId).first()

            if (loan != null) {
                currentLoan = loan
                displayLoanDetails(loan)
            } else {
                showError("Loan not found")
                findNavController().navigateUp()
            }
        }
    }

    /**
     * Display loan details
     */
    private fun displayLoanDetails(loan: Loan) {
        // Loan info
        binding.tvLender.text = loan.lenderName
        binding.tvPrincipal.text = CurrencyUtils.formatAmount(loan.principal)
        binding.tvInterestRate.text = "${loan.interestRate}%"
        binding.tvTotalInterest.text = CurrencyUtils.formatAmount(loan.totalInterest)
        binding.tvTotalOwed.text = CurrencyUtils.formatAmount(loan.totalOwed)
        binding.tvMonthlyPayment.text = CurrencyUtils.formatAmount(loan.monthlyPayment)
        binding.tvStartDate.text = DateUtils.formatToDisplay(loan.startDate)
        binding.tvEndDate.text = DateUtils.formatToDisplay(loan.endDate)
        binding.tvPeriod.text = "${loan.periodMonths} months"

        if (loan.purpose.isNullOrEmpty()) {
            binding.tvPurpose.visibility = android.view.View.GONE
        } else {
            binding.tvPurpose.visibility = android.view.View.VISIBLE
            binding.tvPurpose.text = loan.purpose
        }

        // Payment status
        binding.tvPaymentsMade.text = "${loan.numberOfPaymentsMade} / ${loan.periodMonths}"
        binding.tvTotalLeft.text = CurrencyUtils.formatAmount(loan.totalLeft)

        // Progress bar
        val progress = if (loan.periodMonths > 0) {
            (loan.numberOfPaymentsMade.toFloat() / loan.periodMonths) * 100
        } else {
            0f
        }
        binding.progressPayments.progress = progress.toInt()

        // Payment history (simple display)
        val historyText = buildString {
            for (i in 1..loan.periodMonths) {
                if (i <= loan.numberOfPaymentsMade) {
                    append("Month $i: Paid (${CurrencyUtils.formatAmount(loan.monthlyPayment)})\n")
                } else {
                    append("Month $i: Pending\n")
                }
            }
        }
        binding.tvPaymentHistory.text = historyText

        // Show appropriate buttons
        if (loan.isFullyPaid) {
            binding.btnRecordPayment.visibility = android.view.View.GONE
            binding.btnEdit.visibility = android.view.View.GONE
            binding.btnDelete.visibility = android.view.View.GONE
            binding.tvFullyPaid.visibility = android.view.View.VISIBLE
        } else {
            binding.btnRecordPayment.visibility = android.view.View.VISIBLE
            binding.tvFullyPaid.visibility = android.view.View.GONE

            if (loan.numberOfPaymentsMade == 0) {
                binding.btnEdit.visibility = android.view.View.VISIBLE
                binding.btnDelete.visibility = android.view.View.VISIBLE
            } else {
                binding.btnEdit.visibility = android.view.View.GONE
                binding.btnDelete.visibility = android.view.View.GONE
            }
        }
    }

    /**
     * Setup buttons
     */
    private fun setupButtons() {
        binding.btnRecordPayment.setOnClickListener {
            recordPayment()
        }

        binding.btnEdit.setOnClickListener {
            // Navigate to edit
            val bundle = android.os.Bundle().apply {
                putInt("loanId", loanId)
            }
            findNavController().navigate(R.id.action_loans_to_add, bundle)
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    /**
     * Record payment
     */
    private fun recordPayment() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.record_payment)
            .setMessage("Record payment for this month?")
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = viewModel.recordLoanPayment(loanId)

                    result.onSuccess {
                        showToast(getString(R.string.payment_recorded))
                        // Reload loan details
                        loadLoanDetails(loanId)
                    }.onFailure { error ->
                        showError(error.message ?: "Failed to record payment")
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Show delete confirmation
     */
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage("Delete this loan?")
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = viewModel.deleteLoan(loanId)

                    result.onSuccess {
                        showToast(getString(R.string.success_deleted))
                        findNavController().navigateUp()
                    }.onFailure { error ->
                        showError(error.message ?: getString(R.string.cannot_delete_loan))
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
