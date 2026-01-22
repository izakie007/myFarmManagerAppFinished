package com.palmfarm.manager.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.data.database.entities.Farm
import com.palmfarm.manager.databinding.FragmentAddEditFarmBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fragment for adding or editing a farm
 */
class AddEditFarmFragment : BaseFragment<FragmentAddEditFarmBinding>() {

    private val viewModel: SettingsViewModel by viewModels { ViewModelFactory.create() }
    private var farmId: Int = 0
    private var currentFarm: Farm? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddEditFarmBinding {
        return FragmentAddEditFarmBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        // Get farm ID from arguments
        farmId = arguments?.getInt("farmId", 0) ?: 0

        setupAutoCalculation()
        setupButtons()

        // Load farm if editing
        if (farmId > 0) {
            loadFarm(farmId)
        }
    }

    override fun setupObservers() {
        // No specific observers needed
    }

    /**
     * Setup auto-calculation for total palms
     */
    private fun setupAutoCalculation() {
        val calculateTotal = {
            val productivePalms = binding.etMaturePalms.text.toString().toIntOrNull() ?: 0
            val unproductivePalms = binding.etImmaturePalms.text.toString().toIntOrNull() ?: 0
            val totalPalms = productivePalms + unproductivePalms
            binding.tvTotalPalms.text = "$totalPalms total palms"
        }

        binding.etMaturePalms.doAfterTextChanged { calculateTotal() }
        binding.etImmaturePalms.doAfterTextChanged { calculateTotal() }
    }

    /**
     * Setup buttons
     */
    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveFarm()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Load farm for editing
     */
    private fun loadFarm(farmId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val farm = viewModel.getFarmById(farmId)

            if (farm != null) {
                currentFarm = farm
                populateFarmData(farm)
            }
        }
    }

    /**
     * Populate form with farm data
     */
    private fun populateFarmData(farm: Farm) {
        binding.etFarmName.setText(farm.name)
        binding.etLocation.setText(farm.location)
        binding.etMaturePalms.setText(farm.productivePalms.toString())
        binding.etImmaturePalms.setText(farm.unproductivePalms.toString())
    }

    /**
     * Save farm
     */
    private fun saveFarm() {
        val farmName = binding.etFarmName.text.toString()
        val location = binding.etLocation.text.toString()
        val productivePalms = binding.etMaturePalms.text.toString().toIntOrNull() ?: 0
        val unproductivePalms = binding.etImmaturePalms.text.toString().toIntOrNull() ?: 0

        val farm = Farm(
            id = currentFarm?.id ?: 0,
            name = farmName,
            sizeInHectares = currentFarm?.sizeInHectares ?: 0.0, // Keep existing or default
            location = location,
            productivePalms = productivePalms,
            unproductivePalms = unproductivePalms,
            createdAt = currentFarm?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Validate
        val validationResult = viewModel.validateFarm(farm)
        if (!validationResult.isValid) {
            showError(validationResult.message)
            return
        }

        viewModel.saveFarm(farm)
        showToast("Farm saved successfully")
        findNavController().navigateUp()
    }
}
