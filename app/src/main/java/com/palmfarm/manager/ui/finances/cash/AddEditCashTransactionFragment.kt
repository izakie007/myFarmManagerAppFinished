package com.palmfarm.manager.ui.finances.cash

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.CashTransaction
import com.palmfarm.manager.databinding.FragmentAddCashTransactionBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding/editing cash transactions
 */
class AddEditCashTransactionFragment : BaseFragment<FragmentAddCashTransactionBinding>() {

    private val viewModel: CashTransactionViewModel by viewModels { ViewModelFactory.create() }
    private var transactionId: Int = 0
    private var selectedDate: Long = System.currentTimeMillis()
    private var existingTransaction: CashTransaction? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddCashTransactionBinding {
        return FragmentAddCashTransactionBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        // Get transaction ID from arguments (0 = new transaction)
        transactionId = arguments?.getInt("transactionId", 0) ?: 0

        setupTransactionTypeDropdown()
        setupDatePicker()
        setupButtons()

        if (transactionId > 0) {
            loadTransaction()
        } else {
            // Default to current date for new transactions
            binding.etDate.setText(DateUtils.formatToDisplay(selectedDate))
        }
    }

    override fun setupObservers() {
        observeLoading()
        observeErrors()
    }

    /**
     * Setup transaction type dropdown
     */
    private fun setupTransactionTypeDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Check if opening balance exists
            val hasOpeningBalance = viewModel.hasOpeningBalance()

            val transactionTypes = if (hasOpeningBalance) {
                // If opening balance exists, don't show it as an option
                arrayOf(
                    getString(R.string.transaction_type_cash_in),
                    getString(R.string.transaction_type_cash_out),
                    getString(R.string.transaction_type_adjustment)
                )
            } else {
                // If no opening balance, include it
                arrayOf(
                    getString(R.string.transaction_type_opening_balance),
                    getString(R.string.transaction_type_cash_in),
                    getString(R.string.transaction_type_cash_out),
                    getString(R.string.transaction_type_adjustment)
                )
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                transactionTypes
            )

            binding.actvTransactionType.setAdapter(adapter)
        }
    }

    /**
     * Setup date picker
     */
    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        binding.tilDate.setEndIconOnClickListener {
            showDatePicker()
        }
    }

    /**
     * Show date picker dialog
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDate
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val newCalendar = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                selectedDate = newCalendar.timeInMillis
                binding.etDate.setText(DateUtils.formatToDisplay(selectedDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Setup action buttons
     */
    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Load existing transaction for editing
     */
    private fun loadTransaction() {
        viewLifecycleOwner.lifecycleScope.launch {
            val transaction = viewModel.getTransactionById(transactionId)
            transaction?.let {
                existingTransaction = it
                populateFields(it)
            }
        }
    }

    /**
     * Populate fields with existing transaction data
     */
    private fun populateFields(transaction: CashTransaction) {
        // Transaction type
        val transactionTypeText = when (transaction.transactionType) {
            CashTransaction.TYPE_OPENING_BALANCE -> getString(R.string.transaction_type_opening_balance)
            CashTransaction.TYPE_CASH_IN -> getString(R.string.transaction_type_cash_in)
            CashTransaction.TYPE_CASH_OUT -> getString(R.string.transaction_type_cash_out)
            CashTransaction.TYPE_ADJUSTMENT -> getString(R.string.transaction_type_adjustment)
            else -> transaction.transactionType
        }
        binding.actvTransactionType.setText(transactionTypeText, false)

        // Amount
        binding.etAmount.setText(transaction.amount.toString())

        // Date
        selectedDate = transaction.date
        binding.etDate.setText(DateUtils.formatToDisplay(selectedDate))

        // Description
        binding.etDescription.setText(transaction.description)

        // Category
        binding.etCategory.setText(transaction.category)
    }

    /**
     * Save transaction
     */
    private fun saveTransaction() {
        // Validate inputs
        if (!validateInputs()) {
            return
        }

        val transactionTypeText = binding.actvTransactionType.text.toString()
        val transactionType = when (transactionTypeText) {
            getString(R.string.transaction_type_opening_balance) -> CashTransaction.TYPE_OPENING_BALANCE
            getString(R.string.transaction_type_cash_in) -> CashTransaction.TYPE_CASH_IN
            getString(R.string.transaction_type_cash_out) -> CashTransaction.TYPE_CASH_OUT
            getString(R.string.transaction_type_adjustment) -> CashTransaction.TYPE_ADJUSTMENT
            else -> CashTransaction.TYPE_CASH_IN
        }

        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val description = binding.etDescription.text.toString()
        val category = binding.etCategory.text.toString()

        val transaction = if (transactionId > 0 && existingTransaction != null) {
            // Update existing transaction
            existingTransaction!!.copy(
                transactionType = transactionType,
                amount = amount,
                date = selectedDate,
                description = description,
                category = category
            )
        } else {
            // Create new transaction
            CashTransaction(
                transactionType = transactionType,
                amount = amount,
                date = selectedDate,
                description = description,
                category = category
            )
        }

        // Save transaction
        if (transactionId > 0) {
            viewModel.updateTransaction(transaction)
        } else {
            viewModel.insertTransaction(transaction)
        }

        // Navigate back
        findNavController().navigateUp()
    }

    /**
     * Validate inputs
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        // Transaction type
        if (binding.actvTransactionType.text.isNullOrEmpty()) {
            binding.tilTransactionType.error = getString(R.string.error_transaction_type_required)
            isValid = false
        } else {
            binding.tilTransactionType.error = null
        }

        // Amount
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = getString(R.string.error_amount_invalid)
            isValid = false
        } else {
            binding.tilAmount.error = null
        }

        // Description
        if (binding.etDescription.text.isNullOrEmpty()) {
            binding.tilDescription.error = getString(R.string.error_description_required)
            isValid = false
        } else {
            binding.tilDescription.error = null
        }

        return isValid
    }

    /**
     * Observe loading state
     */
    private fun observeLoading() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
            }
        }
    }

    /**
     * Observe errors
     */
    private fun observeErrors() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            success?.let {
                showToast(it)
                viewModel.clearSuccess()
            }
        }
    }
}
