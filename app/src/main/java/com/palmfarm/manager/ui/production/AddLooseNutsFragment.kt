package com.palmfarm.manager.ui.production

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.data.database.entities.LooseNutsPicking
import com.palmfarm.manager.databinding.FragmentAddLooseNutsBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding a loose nuts picking record
 */
class AddLooseNutsFragment : BaseFragment<FragmentAddLooseNutsBinding>() {

    private val viewModel: ProductionViewModel by viewModels { ViewModelFactory.create() }

    private var pickingDateMillis: Long = System.currentTimeMillis()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddLooseNutsBinding {
        return FragmentAddLooseNutsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
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
     * Setup date picker
     */
    private fun setupDatePicker() {
        binding.etLooseNutsDate.setText(DateUtils.formatToDisplay(pickingDateMillis))

        binding.etLooseNutsDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = pickingDateMillis
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
                        showError("Date cannot be in the future")
                        return@DatePickerDialog
                    }

                    pickingDateMillis = selectedCalendar.timeInMillis
                    binding.etLooseNutsDate.setText(DateUtils.formatToDisplay(pickingDateMillis))
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
            saveLooseNuts()
        }
    }

    /**
     * Save loose nuts record
     */
    private fun saveLooseNuts() {
        val workerName = binding.actvWorker.text.toString()
        val numberOfBags = binding.etNumberOfBags.text.toString().toIntOrNull() ?: 0

        // Validation
        if (workerName.isEmpty()) {
            showError("Worker is required")
            return
        }

        if (numberOfBags <= 0) {
            showError("Number of bags must be greater than 0")
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

        val looseNuts = LooseNutsPicking(
            id = 0,
            cycleId = cycleId,
            pickerId = workerId,
            date = pickingDateMillis,
            numberOfBags = numberOfBags.toDouble(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModel.addLooseNuts(looseNuts)

        // Navigate back
        findNavController().navigateUp()
    }
}
