package com.palmfarm.manager.ui.finances.income

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Consumption
import com.palmfarm.manager.databinding.FragmentAddConsumptionBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding consumption record
 */
class AddConsumptionFragment : BaseFragment<FragmentAddConsumptionBinding>() {

    private val viewModel: IncomeViewModel by viewModels { ViewModelFactory.create() }
    private var dateMillis: Long = System.currentTimeMillis()
    private var currentCycleId: Int = 0
    private var lastSalesPrice: Double = 0.0

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddConsumptionBinding {
        return FragmentAddConsumptionBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        loadCurrentCycle()
        setupDatePicker()
        setupValueCalculation()
        setupSaveButton()
        displayInfo()
    }

    override fun setupObservers() {
        // Observe income metrics to update last sales price and oil stock reactively
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.incomeMetrics.collect { metrics ->
                lastSalesPrice = metrics.lastSalesPrice
                binding.tvLastSalesPrice.text = String.format("%.0f XAF/gal", lastSalesPrice)
                binding.tvOilStock.text = String.format("%.1f gal", metrics.oilStock)
                
                // Recalculate value if quantity is already entered
                if (binding.etQuantity.text.toString().isNotEmpty()) {
                    calculateValue()
                }
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
     * Setup automatic value calculation
     */
    private fun setupValueCalculation() {
        binding.etQuantity.doAfterTextChanged {
            calculateValue()
        }
    }

    /**
     * Calculate consumption value based on last sales price
     */
    private fun calculateValue() {
        val quantity = binding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val value = quantity * lastSalesPrice
        binding.tvValue.text = String.format("%.0f XAF", value)
    }

    /**
     * Setup save button
     */
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveConsumption()
        }
    }

    /**
     * Display oil stock and last sales price (initial display, will be updated reactively via observer)
     */
    private fun displayInfo() {
        // Initial display - will be updated reactively via observer
        viewLifecycleOwner.lifecycleScope.launch {
            val metrics = viewModel.incomeMetrics.first()
            binding.tvOilStock.text = String.format("%.1f gal", metrics.oilStock)
            // Last sales price will be updated via observer
        }
    }

    /**
     * Save consumption
     */
    private fun saveConsumption() {
        val quantity = binding.etQuantity.text.toString().toDoubleOrNull()
        val purpose = binding.etPurpose.text.toString()

        // Validation
        if (quantity == null || quantity <= 0) {
            showError("Please enter a valid quantity")
            return
        }

        val value = quantity * lastSalesPrice

        val consumption = Consumption(
            id = 0,
            cycleId = currentCycleId,
            date = dateMillis,
            quantityGallons = quantity,
            purpose = purpose.ifEmpty { null },
            valuedAtPrice = value,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Save consumption
        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.saveConsumption(consumption)

            result.onSuccess {
                showToast(getString(R.string.success_saved))
                findNavController().navigateUp()
            }.onFailure { error ->
                showError(error.message ?: "Failed to save consumption")
            }
        }
    }
}
