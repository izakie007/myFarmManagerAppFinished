package com.palmfarm.manager.ui.finances.fixedcosts

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.FixedCost
import com.palmfarm.manager.databinding.FragmentAddEditFixedCostBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding or editing a fixed cost
 */
class AddEditFixedCostFragment : BaseFragment<FragmentAddEditFixedCostBinding>() {

    private val viewModel: FixedCostsViewModel by viewModels { ViewModelFactory.create() }

    private var currentFixedCost: FixedCost? = null
    private var fixedCostId: Int = 0
    private var startDateMillis: Long = System.currentTimeMillis()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddEditFixedCostBinding {
        return FragmentAddEditFixedCostBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Get fixed cost ID from arguments
        fixedCostId = arguments?.getInt("fixedCostId", 0) ?: 0

        setupTypeDropdown()
        setupDatePicker()
        setupSaveButton()

        // Load fixed cost if editing
        if (fixedCostId > 0) {
            loadFixedCost(fixedCostId)
        }
    }

    override fun setupObservers() {
        // No specific observers needed
    }

    /**
     * Setup type dropdown
     */
    private fun setupTypeDropdown() {
        val types = arrayOf(
            getString(R.string.fixed_cost_type_land),
            getString(R.string.fixed_cost_type_equipment)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.actvType.setAdapter(adapter)
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
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Setup save button
     */
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveFixedCost()
        }
    }

    /**
     * Load fixed cost for editing
     */
    private fun loadFixedCost(fixedCostId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val fixedCostsWithDepreciation = viewModel.fixedCostsWithDepreciation.first()
            val item = fixedCostsWithDepreciation.find { it.fixedCost.id == fixedCostId }

            if (item != null) {
                currentFixedCost = item.fixedCost
                populateFixedCostData(item.fixedCost)
            }
        }
    }

    /**
     * Populate form with fixed cost data
     */
    private fun populateFixedCostData(fixedCost: FixedCost) {
        binding.etName.setText(fixedCost.itemName)
        binding.actvType.setText(fixedCost.category, false)
        binding.etAmount.setText(fixedCost.amount.toString())
        binding.etLifeSpan.setText(fixedCost.lifeSpanYears.toString())

        startDateMillis = fixedCost.date
        binding.etStartDate.setText(DateUtils.formatToDisplay(fixedCost.date))
    }

    /**
     * Save fixed cost
     */
    private fun saveFixedCost() {
        val name = binding.etName.text.toString()
        val categoryInput = binding.actvType.text.toString()
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val lifeSpan = binding.etLifeSpan.text.toString().toIntOrNull()

        // Validation
        if (name.isEmpty()) {
            showError(getString(R.string.error_field_required))
            return
        }

        if (categoryInput.isEmpty()) {
            showError(getString(R.string.error_field_required))
            return
        }

        if (amount == null || amount <= 0) {
            showError(getString(R.string.error_invalid_amount))
            return
        }

        if (lifeSpan == null || lifeSpan <= 0) {
            showError("Please enter a valid life span")
            return
        }

        // Calculate monthly depreciation
        val monthlyDepreciation = amount / (lifeSpan * 12)

        val fixedCost = FixedCost(
            id = currentFixedCost?.id ?: 0,
            date = startDateMillis,
            amount = amount,
            paymentType = "PURCHASE", // Default to PURCHASE, can be enhanced with dropdown
            category = categoryInput,
            itemName = name,
            lifeSpanYears = lifeSpan,
            monthlyDepreciation = monthlyDepreciation,
            createdAt = currentFixedCost?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModel.saveFixedCost(fixedCost)

        // Navigate back
        findNavController().navigateUp()
    }
}
