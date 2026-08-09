package com.palmfarm.manager.ui.production

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Milling
import com.palmfarm.manager.databinding.FragmentAddMillingBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding or editing a milling record
 */
class AddMillingFragment : BaseFragment<FragmentAddMillingBinding>() {

    private val viewModel: ProductionViewModel by viewModels { ViewModelFactory.create() }
    private val args: AddMillingFragmentArgs by navArgs()

    private var millingDateMillis: Long = System.currentTimeMillis()
    private var oilUnit: OilUnit = OilUnit.GALLONS
    private var bunchesAvailable: Int = 0
    private var millingId: Int = 0
    private var existingMilling: Milling? = null
    private var workerIdToSelect: Int? = null
    private var navigateOnSuccess: Boolean = false

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddMillingBinding {
        return FragmentAddMillingBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        millingId = args.millingId
        setupDatePicker()
        setupOilUnitToggle()
        setupBunchesValidation()
        setupSaveButton()

        if (millingId > 0) {
            loadMilling(millingId)
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productionMetrics.collect { metrics ->
                val originalBunches = existingMilling?.bunchesMilled ?: 0
                bunchesAvailable = metrics.bunchesAvailable + originalBunches
                binding.tvBunchesAvailable.text = getString(R.string.milling_bunches_available, bunchesAvailable)
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { message ->
            message?.let {
                showToast(it)
                viewModel.clearSuccess()
                if (navigateOnSuccess) {
                    findNavController().navigateUp()
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            message?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
        }
    }

    private fun loadMilling(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val milling = viewModel.getMillingById(id).first()
            if (milling == null) {
                showError("Milling record not found")
                findNavController().navigateUp()
                return@launch
            }

            existingMilling = milling
            millingDateMillis = milling.date
            workerIdToSelect = milling.millerId

            binding.etMillingDate.setText(DateUtils.formatToDisplay(millingDateMillis))
            binding.etBunchesMilled.setText(milling.bunchesMilled.toString())
            binding.etDrumsCooked.setText(milling.drumsCooked.toString())
            binding.etOilProduced.setText(milling.oilProducedGallons.toString())
            binding.toggleOilUnit.check(R.id.btnGallons)
            oilUnit = OilUnit.GALLONS

            val worker = viewModel.activeWorkers.value.find { it.id == milling.millerId }
            worker?.let { binding.actvWorker.setText(it.fullName, false) }
        }
    }

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

    private fun setupBunchesValidation() {
        binding.etBunchesMilled.doAfterTextChanged {
            val bunches = it.toString().toIntOrNull() ?: 0
            if (bunches > bunchesAvailable) {
                binding.tilBunchesMilled.error = "Cannot exceed $bunchesAvailable available bunches"
            } else {
                binding.tilBunchesMilled.error = null
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveMilling()
        }
    }

    private fun saveMilling() {
        val workerName = binding.actvWorker.text.toString()
        val bunchesMilled = binding.etBunchesMilled.text.toString().toIntOrNull() ?: 0
        val drumsCooked = binding.etDrumsCooked.text.toString().toDoubleOrNull() ?: 0.0
        val oilProduced = binding.etOilProduced.text.toString().toDoubleOrNull() ?: 0.0

        if (workerName.isEmpty()) {
            showError("Worker is required")
            return
        }

        if (bunchesMilled <= 0) {
            showError("Bunches milled must be greater than 0")
            return
        }

        if (bunchesMilled > bunchesAvailable) {
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

        val workerId = viewModel.activeWorkers.value.find { it.fullName == workerName }?.id ?: 0
        if (workerId == 0) {
            showError("Invalid worker selected")
            return
        }

        val oilInGallons = if (oilUnit == OilUnit.LITRES) {
            oilProduced / 20.0
        } else {
            oilProduced
        }

        navigateOnSuccess = true

        if (millingId > 0) {
            val existing = existingMilling ?: return
            val milling = existing.copy(
                millerId = workerId,
                date = millingDateMillis,
                bunchesMilled = bunchesMilled,
                drumsCooked = drumsCooked,
                oilProducedGallons = oilInGallons,
                updatedAt = System.currentTimeMillis()
            )
            viewModel.updateMilling(milling)
        } else {
            val cycleId = viewModel.getCurrentCycleId()
            if (cycleId <= 0) {
                showError("No active production cycle. Configure season start month first.")
                navigateOnSuccess = false
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
            viewModel.addMilling(milling)
        }
    }

    enum class OilUnit {
        GALLONS, LITRES
    }
}
