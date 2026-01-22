package com.palmfarm.manager.ui.production

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.data.database.entities.Harvest
import com.palmfarm.manager.databinding.FragmentAddHarvestBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding a harvest record
 */
class AddHarvestFragment : BaseFragment<FragmentAddHarvestBinding>() {

    private val viewModel: ProductionViewModel by viewModels { ViewModelFactory.create() }

    private var harvestDateMillis: Long = System.currentTimeMillis()
    private var harvestNumber: Int = 1

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddHarvestBinding {
        return FragmentAddHarvestBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        loadNextHarvestNumber()
        setupDatePicker()
        setupSaveButton()
    }

    override fun setupObservers() {
        // Observe workers for dropdown
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeWorkers.collect { workers ->
                val workerNames = workers.map { it.fullName }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, workerNames)
                binding.actvWorker.setAdapter(adapter)
            }
        }
    }

    /**
     * Load next harvest number
     */
    private fun loadNextHarvestNumber() {
        viewLifecycleOwner.lifecycleScope.launch {
            harvestNumber = viewModel.getNextHarvestNumber()
            binding.tvHarvestNumber.text = "Harvest #$harvestNumber"
        }
    }

    /**
     * Setup date picker
     */
    private fun setupDatePicker() {
        binding.etHarvestDate.setText(DateUtils.formatToDisplay(harvestDateMillis))

        binding.etHarvestDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = harvestDateMillis
            }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // Cannot be future date
                    if (selectedCalendar.timeInMillis > System.currentTimeMillis()) {
                        showError("Harvest date cannot be in the future")
                        return@DatePickerDialog
                    }

                    harvestDateMillis = selectedCalendar.timeInMillis
                    binding.etHarvestDate.setText(DateUtils.formatToDisplay(harvestDateMillis))
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
            saveHarvest()
        }
    }

    /**
     * Save harvest
     */
    private fun saveHarvest() {
        val workerName = binding.actvWorker.text.toString()
        val numberOfBunches = binding.etNumberOfBunches.text.toString().toIntOrNull() ?: 0
        val remarks = binding.etRemarks.text.toString()

        // Validation
        if (workerName.isEmpty()) {
            showError("Worker is required")
            return
        }

        if (numberOfBunches <= 0) {
            showError("Number of bunches must be greater than 0")
            return
        }

        // Find worker ID
        val workerId = viewModel.activeWorkers.value.find { it.fullName == workerName }?.id ?: 0
        if (workerId == 0) {
            showError("Invalid worker selected")
            return
        }

        // Get current cycle ID
        val cycleId = viewModel.getCurrentCycleId()

        // Fetch fresh harvest number right before saving to ensure it's correct
        viewLifecycleOwner.lifecycleScope.launch {
            val nextHarvestNumber = viewModel.getNextHarvestNumber()

        val harvest = Harvest(
            id = 0,
            cycleId = cycleId,
                harvestNumber = nextHarvestNumber,
            harvesterId = workerId,
            date = harvestDateMillis,
            numberOfBunches = numberOfBunches,
            remarks = if (remarks.isNotEmpty()) remarks else null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModel.addHarvest(harvest)

        // Navigate back
        findNavController().navigateUp()
        }
    }
}
