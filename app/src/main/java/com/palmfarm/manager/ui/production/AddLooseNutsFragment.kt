package com.palmfarm.manager.ui.production

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.palmfarm.manager.data.database.entities.LooseNutsPicking
import com.palmfarm.manager.databinding.FragmentAddLooseNutsBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding or editing a loose nuts picking record
 */
class AddLooseNutsFragment : BaseFragment<FragmentAddLooseNutsBinding>() {

    private val viewModel: ProductionViewModel by viewModels { ViewModelFactory.create() }
    private val args: AddLooseNutsFragmentArgs by navArgs()

    private var pickingDateMillis: Long = System.currentTimeMillis()
    private var looseNutsId: Int = 0
    private var existingLooseNuts: LooseNutsPicking? = null
    private var workerIdToSelect: Int? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddLooseNutsBinding {
        return FragmentAddLooseNutsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        looseNutsId = args.looseNutsId
        setupDatePicker()
        setupSaveButton()

        if (looseNutsId > 0) {
            loadLooseNuts(looseNutsId)
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

    private fun loadLooseNuts(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val looseNuts = viewModel.getLooseNutsById(id).first()
            if (looseNuts == null) {
                showError("Loose nuts record not found")
                findNavController().navigateUp()
                return@launch
            }

            existingLooseNuts = looseNuts
            pickingDateMillis = looseNuts.date
            workerIdToSelect = looseNuts.pickerId

            binding.etLooseNutsDate.setText(DateUtils.formatToDisplay(pickingDateMillis))
            binding.etNumberOfBags.setText(
                if (looseNuts.numberOfBags % 1.0 == 0.0) {
                    looseNuts.numberOfBags.toInt().toString()
                } else {
                    looseNuts.numberOfBags.toString()
                }
            )

            val worker = viewModel.activeWorkers.value.find { it.id == looseNuts.pickerId }
            worker?.let { binding.actvWorker.setText(it.fullName, false) }
        }
    }

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

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveLooseNuts()
        }
    }

    private fun saveLooseNuts() {
        val workerName = binding.actvWorker.text.toString()
        val numberOfBags = binding.etNumberOfBags.text.toString().toIntOrNull() ?: 0

        if (workerName.isEmpty()) {
            showError("Worker is required")
            return
        }

        if (numberOfBags <= 0) {
            showError("Number of bags must be greater than 0")
            return
        }

        val workerId = viewModel.activeWorkers.value.find { it.fullName == workerName }?.id ?: 0
        if (workerId == 0) {
            showError("Invalid worker selected")
            return
        }

        if (looseNutsId > 0) {
            val existing = existingLooseNuts ?: return
            val looseNuts = existing.copy(
                pickerId = workerId,
                date = pickingDateMillis,
                numberOfBags = numberOfBags.toDouble(),
                updatedAt = System.currentTimeMillis()
            )
            viewModel.updateLooseNuts(looseNuts)
        } else {
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
        }

        findNavController().navigateUp()
    }
}
