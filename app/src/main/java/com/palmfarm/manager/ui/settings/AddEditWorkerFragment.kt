package com.palmfarm.manager.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.databinding.FragmentAddEditWorkerBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fragment for adding or editing a worker
 */
class AddEditWorkerFragment : BaseFragment<FragmentAddEditWorkerBinding>() {

    private val viewModel: SettingsViewModel by viewModels { ViewModelFactory.create() }
    private var workerId: Int = 0
    private var currentWorker: Worker? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddEditWorkerBinding {
        return FragmentAddEditWorkerBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        // Get worker ID from arguments
        workerId = arguments?.getInt("workerId", 0) ?: 0

        setupSpecialtyDropdown()
        setupStatusToggle()
        setupButtons()

        // Load worker if editing
        if (workerId > 0) {
            loadWorker(workerId)
        }
    }

    override fun setupObservers() {
        // No specific observers needed
    }

    /**
     * Setup specialty dropdown
     */
    private fun setupSpecialtyDropdown() {
        val specialties = arrayOf(
            "Harvester",
            "Miller",
            "Transporter",
            "General Worker",
            "Supervisor",
            "Custom"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            specialties
        )

        binding.actvSpecialty.setAdapter(adapter)

        binding.actvSpecialty.setOnItemClickListener { _, _, position, _ ->
            if (specialties[position] == "Custom") {
                binding.tilCustomSpecialty.visibility = View.VISIBLE
            } else {
                binding.tilCustomSpecialty.visibility = View.GONE
            }
        }
    }

    /**
     * Setup status toggle
     */
    private fun setupStatusToggle() {
        binding.switchActive.isChecked = true
    }

    /**
     * Setup buttons
     */
    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveWorker()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /**
     * Load worker for editing
     */
    private fun loadWorker(workerId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val worker = viewModel.getWorkerById(workerId)

            if (worker != null) {
                currentWorker = worker
                populateWorkerData(worker)
            }
        }
    }

    /**
     * Populate form with worker data
     */
    private fun populateWorkerData(worker: Worker) {
        binding.etWorkerName.setText(worker.fullName)
        binding.etPhoneNumber.setText(worker.phoneNumber)
        binding.actvSpecialty.setText(worker.specialty, false)
        binding.switchActive.isChecked = worker.isActive

        // Check if custom specialty
        val predefinedSpecialties = listOf("Harvester", "Miller", "Transporter", "General Worker", "Supervisor")
        if (!predefinedSpecialties.contains(worker.specialty)) {
            binding.actvSpecialty.setText("Custom", false)
            binding.tilCustomSpecialty.visibility = View.VISIBLE
            binding.etCustomSpecialty.setText(worker.specialty)
        }
    }

    /**
     * Save worker
     */
    private fun saveWorker() {
        val workerName = binding.etWorkerName.text.toString()
        var phoneNumber = binding.etPhoneNumber.text.toString()
        var specialty = binding.actvSpecialty.text.toString()
        val isActive = binding.switchActive.isChecked

        // Handle custom specialty
        if (specialty == "Custom") {
            specialty = binding.etCustomSpecialty.text.toString()
            if (specialty.isEmpty()) {
                showError("Please enter custom specialty")
                return
            }
        }

        // Format phone number for Cameroon
        if (phoneNumber.isNotEmpty() && !phoneNumber.startsWith("+237")) {
            phoneNumber = "+237$phoneNumber"
        }

        val worker = Worker(
            id = currentWorker?.id ?: 0,
            firstName = workerName,
            lastName = null,
            phoneNumber = phoneNumber,
            specialty = specialty,
            isActive = isActive,
            createdAt = currentWorker?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Validate
        val validationResult = viewModel.validateWorker(worker)
        if (!validationResult.isValid) {
            showError(validationResult.message)
            return
        }

        viewModel.saveWorker(worker)
        showToast(getString(R.string.success_saved))
        findNavController().navigateUp()
    }
}
