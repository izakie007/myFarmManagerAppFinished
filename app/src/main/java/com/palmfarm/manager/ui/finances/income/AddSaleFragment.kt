package com.palmfarm.manager.ui.finances.income

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Sale
import com.palmfarm.manager.databinding.FragmentAddSaleBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding a sale
 */
class AddSaleFragment : BaseFragment<FragmentAddSaleBinding>() {

    private val viewModel: IncomeViewModel by viewModels { ViewModelFactory.create() }
    private var dateMillis: Long = System.currentTimeMillis()
    private var currentCycleId: Int = 0

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddSaleBinding {
        return FragmentAddSaleBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        loadCurrentCycle()
        setupUnitDropdown()
        setupDatePicker()
        setupAmountCalculation()
        setupSaveButton()
        displayStockInfo()
    }

    override fun setupObservers() {
        // Observe income metrics to update oil stock reactively
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.incomeMetrics.collect { metrics ->
                binding.tvOilStock.text = String.format("%.1f gal", metrics.oilStock)
                binding.tvBunchesAvailable.text = "${metrics.bunchesAvailable} bunches"
            }
        }
    }

    /**
     * Load current cycle ID
     */
    private fun loadCurrentCycle() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cycleRepository = ViewModelFactory.create().productionCycleRepository
            val cycle = cycleRepository.getCurrentCycle().first()
            currentCycleId = cycle?.id ?: 1
        }
    }

    /**
     * Setup unit dropdown
     */
    private fun setupUnitDropdown() {
        val units = arrayOf("GALLON", "TONNE", "BUNCH")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.actvUnit.setAdapter(adapter)
        binding.actvUnit.setText("GALLON", false)
    }

    /**
     * Setup date picker
     */
    private fun setupDatePicker() {
        binding.etDate.setText(DateUtils.formatToDisplay(dateMillis))

        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    dateMillis = selectedCalendar.timeInMillis
                    binding.etDate.setText(DateUtils.formatToDisplay(dateMillis))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Setup automatic amount calculation
     */
    private fun setupAmountCalculation() {
        val calculateAmount = {
            val quantity = binding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val unitPrice = binding.etUnitPrice.text.toString().toDoubleOrNull() ?: 0.0
            val totalAmount = quantity * unitPrice
            binding.tvTotalAmount.text = String.format("%.0f XAF", totalAmount)
        }

        binding.etQuantity.doAfterTextChanged { calculateAmount() }
        binding.etUnitPrice.doAfterTextChanged { calculateAmount() }
    }

    /**
     * Setup save button
     */
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveSale()
        }
    }

    /**
     * Display stock information (initial display, will be updated reactively via observer)
     */
    private fun displayStockInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            val metrics = viewModel.incomeMetrics.first()
            binding.tvOilStock.text = String.format("%.1f gal", metrics.oilStock)
            binding.tvBunchesAvailable.text = "${metrics.bunchesAvailable} bunches"
        }
    }

    /**
     * Save sale
     */
    private fun saveSale() {
        val quantity = binding.etQuantity.text.toString().toDoubleOrNull()
        val unit = binding.actvUnit.text.toString()
        val unitPrice = binding.etUnitPrice.text.toString().toDoubleOrNull()
        val buyerName = binding.etBuyer.text.toString()

        // Validation
        if (quantity == null || quantity <= 0) {
            showError("Please enter a valid quantity")
            return
        }

        if (unit.isEmpty()) {
            showError("Please select a unit")
            return
        }

        if (unitPrice == null || unitPrice <= 0) {
            showError("Please enter a valid unit price")
            return
        }

        val totalAmount = quantity * unitPrice

        val sale = Sale(
            id = 0,
            cycleId = currentCycleId,
            date = dateMillis,
            quantity = quantity,
            unit = unit,
            unitPrice = unitPrice,
            totalAmount = totalAmount,
            buyerName = buyerName.ifEmpty { null },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Save sale
        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.saveSale(sale)

            result.onSuccess {
                showToast(getString(R.string.success_saved))
                findNavController().navigateUp()
            }.onFailure { error ->
                showError(error.message ?: "Failed to save sale")
            }
        }
    }
}
