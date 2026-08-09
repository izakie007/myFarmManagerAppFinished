package com.palmfarm.manager.ui.production

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Harvest
import com.palmfarm.manager.databinding.FragmentAddHarvestBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding or editing a harvest record
 */
class AddHarvestFragment : BaseFragment<FragmentAddHarvestBinding>() {

    private val viewModel: ProductionViewModel by viewModels { ViewModelFactory.create() }
    private val args: AddHarvestFragmentArgs by navArgs()

    private var harvestDateMillis: Long = System.currentTimeMillis()
    private var harvestNumber: Int = 1
    private var harvestId: Int = 0
    private var existingHarvest: Harvest? = null
    private var workerIdToSelect: Int? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddHarvestBinding {
        return FragmentAddHarvestBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        harvestId = args.harvestId
        setupDatePicker()
        setupSaveButton()

        if (harvestId > 0) {
            loadHarvest(harvestId)
        } else {
            loadNextHarvestNumber()
        }
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeWorkers.collect { workers ->
                val workerNames = workers.map { it.fullName }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, workerNames)
                binding.actvWorker.setAdapter(adapter)

                workerIdToSelect?.let { workerId ->
                    val worker = workers.find { it.id == workerId }
                    worker?.let {
                        binding.actvWorker.setText(it.fullName, false)
                        workerIdToSelect = null
                    }
                }
            }
        }
    }

    private fun loadNextHarvestNumber() {
        viewLifecycleOwner.lifecycleScope.launch {
            harvestNumber = viewModel.getNextHarvestNumber()
            binding.tvHarvestNumber.text = "Harvest #$harvestNumber"
        }
    }

    private fun loadHarvest(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val harvest = viewModel.getHarvestById(id).first()
            if (harvest == null) {
                showError("Harvest not found")
                findNavController().navigateUp()
                return@launch
            }

            existingHarvest = harvest
            harvestNumber = harvest.harvestNumber
            harvestDateMillis = harvest.date
            workerIdToSelect = harvest.harvesterId

            binding.tvHarvestNumber.text = "Harvest #$harvestNumber"
            binding.etHarvestDate.setText(DateUtils.formatToDisplay(harvestDateMillis))
            binding.etNumberOfBunches.setText(harvest.numberOfBunches.toString())
            binding.etRemarks.setText(harvest.remarks.orEmpty())
            binding.btnSave.setText(R.string.action_save)

            val worker = viewModel.activeWorkers.value.find { it.id == harvest.harvesterId }
            worker?.let { binding.actvWorker.setText(it.fullName, false) }
        }
    }

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

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveHarvest()
        }
    }

    private fun saveHarvest() {
        val workerName = binding.actvWorker.text.toString()
        val numberOfBunches = binding.etNumberOfBunches.text.toString().toIntOrNull() ?: 0
        val remarks = binding.etRemarks.text.toString()

        if (workerName.isEmpty()) {
            showError("Worker is required")
            return
        }

        if (numberOfBunches <= 0) {
            showError("Number of bunches must be greater than 0")
            return
        }

        val workerId = viewModel.activeWorkers.value.find { it.fullName == workerName }?.id ?: 0
        if (workerId == 0) {
            showError("Invalid worker selected")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (harvestId > 0) {
                val existing = existingHarvest ?: return@launch
                val harvest = existing.copy(
                    harvesterId = workerId,
                    date = harvestDateMillis,
                    numberOfBunches = numberOfBunches,
                    remarks = remarks.ifEmpty { null },
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.updateHarvest(harvest)
            } else {
                val cycleId = viewModel.getCurrentCycleId()
                val nextHarvestNumber = viewModel.getNextHarvestNumber()
                val harvest = Harvest(
                    id = 0,
                    cycleId = cycleId,
                    harvestNumber = nextHarvestNumber,
                    harvesterId = workerId,
                    date = harvestDateMillis,
                    numberOfBunches = numberOfBunches,
                    remarks = remarks.ifEmpty { null },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.addHarvest(harvest)
            }

            findNavController().navigateUp()
        }
    }
}
