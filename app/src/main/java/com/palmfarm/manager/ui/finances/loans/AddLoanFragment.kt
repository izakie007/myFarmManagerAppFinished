package com.palmfarm.manager.ui.finances.loans

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Loan
import com.palmfarm.manager.databinding.FragmentAddLoanBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding a loan with real-time calculation
 */
class AddLoanFragment : BaseFragment<FragmentAddLoanBinding>() {

    private val viewModel: LoansViewModel by viewModels { ViewModelFactory.create() }
    private var currentLoan: Loan? = null
    private var loanId: Int = 0
    private var startDateMillis: Long = System.currentTimeMillis()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddLoanBinding {
        return FragmentAddLoanBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Get loan ID from arguments
        loanId = arguments?.getInt("loanId", 0) ?: 0

        setupDatePicker()
        setupRealTimeCalculation()
        setupSaveButton()

        // Load loan if editing
        if (loanId > 0) {
            loadLoan(loanId)
        }
    }

    override fun setupObservers() {
        // No specific observers needed
    }

    /**
     * Setup date picker
     */
    private fun setupDatePicker() {
        binding.etStartDate.setText(DateUtils.formatToDisplay(startDateMillis))

        binding.etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = startDateMillis }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    startDateMillis = selectedCalendar.timeInMillis
                    binding.etStartDate.setText(DateUtils.formatToDisplay(startDateMillis))
                    calculateLoan()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Setup real-time loan calculation
     */
    private fun setupRealTimeCalculation() {
        binding.etPrincipal.doAfterTextChanged { calculateLoan() }
        binding.etInterestRate.doAfterTextChanged { calculateLoan() }
        binding.etPeriod.doAfterTextChanged { calculateLoan() }
    }

    /**
     * Calculate loan details
     */
    private fun calculateLoan() {
        val principal = binding.etPrincipal.text.toString().toDoubleOrNull() ?: 0.0
        val interestRate = binding.etInterestRate.text.toString().toDoubleOrNull() ?: 0.0
        val periodMonths = binding.etPeriod.text.toString().toIntOrNull() ?: 0

        if (principal > 0 && periodMonths > 0) {
            val calculation = viewModel.calculateLoanDetails(
                principal = principal,
                interestRate = interestRate,
                periodMonths = periodMonths,
                startDate = startDateMillis
            )

            // Display calculated values
            binding.tvTotalInterest.text = CurrencyUtils.formatAmount(calculation.totalInterest)
            binding.tvTotalOwed.text = CurrencyUtils.formatAmount(calculation.totalOwed)
            binding.tvMonthlyPayment.text = CurrencyUtils.formatAmount(calculation.monthlyPayment)
            binding.tvEndDate.text = DateUtils.formatToDisplay(calculation.endDate)
        } else {
            // Clear calculated values
            binding.tvTotalInterest.text = "0 XAF"
            binding.tvTotalOwed.text = "0 XAF"
            binding.tvMonthlyPayment.text = "0 XAF"
            binding.tvEndDate.text = "--"
        }
    }

    /**
     * Setup save button
     */
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveLoan()
        }
    }

    /**
     * Load loan for editing
     */
    private fun loadLoan(loanId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val loan = viewModel.getLoanById(loanId).first()

            if (loan != null) {
                currentLoan = loan
                populateLoanData(loan)
            }
        }
    }

    /**
     * Populate form with loan data
     */
    private fun populateLoanData(loan: Loan) {
        binding.etPrincipal.setText(loan.principal.toString())
        binding.etInterestRate.setText(loan.interestRate.toString())
        binding.etPeriod.setText(loan.periodMonths.toString())
        binding.etLender.setText(loan.lenderName)
        binding.etPurpose.setText(loan.purpose ?: "")

        startDateMillis = loan.startDate
        binding.etStartDate.setText(DateUtils.formatToDisplay(loan.startDate))

        calculateLoan()
    }

    /**
     * Save loan
     */
    private fun saveLoan() {
        val principal = binding.etPrincipal.text.toString().toDoubleOrNull()
        val interestRate = binding.etInterestRate.text.toString().toDoubleOrNull()
        val periodMonths = binding.etPeriod.text.toString().toIntOrNull()
        val lenderName = binding.etLender.text.toString()
        val purpose = binding.etPurpose.text.toString()

        // Validation
        if (principal == null || principal <= 0) {
            showError("Please enter a valid principal amount")
            return
        }

        if (interestRate == null || interestRate < 0) {
            showError("Please enter a valid interest rate")
            return
        }

        if (periodMonths == null || periodMonths <= 0) {
            showError("Please enter a valid period")
            return
        }

        if (lenderName.isEmpty()) {
            showError("Please enter lender name")
            return
        }

        // Calculate loan details
        val calculation = viewModel.calculateLoanDetails(
            principal = principal,
            interestRate = interestRate,
            periodMonths = periodMonths,
            startDate = startDateMillis
        )

        val numberOfPaymentsMade = currentLoan?.numberOfPaymentsMade ?: 0
        val totalPaid = numberOfPaymentsMade * calculation.monthlyPayment
        val totalLeft = calculation.totalOwed - totalPaid

        val loan = Loan(
            id = currentLoan?.id ?: 0,
            lenderName = lenderName,
            purpose = purpose.ifEmpty { null },
            principal = principal,
            interestRate = interestRate,
            startDate = startDateMillis,
            periodMonths = periodMonths,
            endDate = calculation.endDate,
            totalInterest = calculation.totalInterest,
            totalOwed = calculation.totalOwed,
            monthlyPayment = calculation.monthlyPayment,
            numberOfPaymentsMade = numberOfPaymentsMade,
            totalPaid = totalPaid,
            totalLeft = totalLeft,
            isFullyPaid = currentLoan?.isFullyPaid ?: false,
            createdAt = currentLoan?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModel.saveLoan(loan)

        // Navigate back
        findNavController().navigateUp()
    }
}
