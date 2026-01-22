package com.palmfarm.manager.ui.production

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Milling
import com.palmfarm.manager.databinding.FragmentAddMillingBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding a milling record
 */
class AddMillingFragment : BaseFragment<FragmentAddMillingBinding>() {

    private val viewModel: ProductionViewModel by viewModels { ViewModelFactory.create() }

    private var millingDateMillis: Long = System.currentTimeMillis()
    private var oilUnit: OilUnit = OilUnit.GALLONS
    private var bunchesAvailable: Int? = 0

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddMillingBinding {
        return FragmentAddMillingBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupDatePicker()
        setupOilUnitToggle()
        setupBunchesValidation()
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

        // Observe bunches available
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productionMetrics.collect { metrics ->
                bunchesAvailable = metrics.bunchesAvailable
                binding.tvBunchesAvailable.text = getString(R.string.milling_bunches_available, bunchesAvailable)
            }
        }

        // Observe success messages
        viewModel.success.observe(viewLifecycleOwner) { message ->
            message?.let {
                showToast(it)
                viewModel.clearSuccess()
                // Navigate back only on success
                findNavController().navigateUp()
            }
        }

        // Observe error messages
        viewModel.error.observe(viewLifecycleOwner) { message ->
            message?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
        }
    }

    /**
     * Setup date picker
     */
    private fun setupDatePicker() {
        binding.etMillingDate.setText(DateUtils.formatToDisplay(millingDateMillis))

        binding.etMillingDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = millingDateMillis
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
                        showError("Milling date cannot be in the future")
                        return@DatePickerDialog
                    }

                    millingDateMillis = selectedCalendar.timeInMillis
                    binding.etMillingDate.setText(DateUtils.formatToDisplay(millingDateMillis))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Setup oil unit toggle
     */
    private fun setupOilUnitToggle() {
        binding.toggleOilUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                oilUnit = when (checkedId) {
                    R.id.btnGallons -> OilUnit.GALLONS
                    R.id.btnLitres -> OilUnit.LITRES
                    else -> OilUnit.GALLONS
                }
            }
        }
    }

    /**
     * Setup bunches validation
     */
    private fun setupBunchesValidation() {
        binding.etBunchesMilled.doAfterTextChanged {
            val bunches = it.toString().toIntOrNull() ?: 0
            if (bunches > bunchesAvailable!!) {
                binding.tilBunchesMilled.error = "Cannot exceed $bunchesAvailable available bunches"
            } else {
                binding.tilBunchesMilled.error = null
            }
        }
    }

    /**
     * Setup save button
     */
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveMilling()
        }
    }

    /**
     * Save milling
     */
    private fun saveMilling() {
        val workerName = binding.actvWorker.text.toString()
        val bunchesMilled = binding.etBunchesMilled.text.toString().toIntOrNull() ?: 0
        val drumsCooked = binding.etDrumsCooked.text.toString().toDoubleOrNull() ?: 0.0
        val oilProduced = binding.etOilProduced.text.toString().toDoubleOrNull() ?: 0.0

        // Validation
        if (workerName.isEmpty()) {
            showError("Worker is required")
            return
        }

        if (bunchesMilled <= 0) {
            showError("Bunches milled must be greater than 0")
            return
        }

        if (bunchesMilled > bunchesAvailable!!) {
            showError("Cannot mill $bunchesMilled bunches. Only $bunchesAvailable available.")
            return
        }

        if (drumsCooked <= 0) {
            showError("Drums cooked must be greater than 0")
            return
        }

        if (oilProduced <= 0.0) {
            showError("Oil produced must be greater than 0")
            return
        }

        // Find worker ID
        val workerId = viewModel.activeWorkers.value.find { it.fullName == workerName }?.id ?: 0
        if (workerId == 0) {
            showError("Invalid worker selected")
            return
        }

        // Convert to gallons if needed
        val oilInGallons = if (oilUnit == OilUnit.LITRES) {
            oilProduced / 20.0 // 20 litres = 1 gallon
        } else {
            oilProduced
        }

        // Get current cycle ID
        val cycleId = viewModel.getCurrentCycleId()
        if (cycleId <= 0) {
            showError("No active production cycle. Configure season start month first.")
            return
        }

        val milling = Milling(
            id = 0,
            cycleId = cycleId,
            millerId = workerId,
            date = millingDateMillis,
            bunchesMilled = bunchesMilled,
            drumsCooked = drumsCooked,
            oilProducedGallons = oilInGallons,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Save milling - navigation will happen on success via observer
        viewModel.addMilling(milling)
    }

    enum class OilUnit {
        GALLONS, LITRES
    }
}
