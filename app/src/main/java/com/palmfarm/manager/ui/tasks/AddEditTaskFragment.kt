package com.palmfarm.manager.ui.tasks

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Task
import com.palmfarm.manager.databinding.FragmentAddEditTaskBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.Constants
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment for adding or editing a task
 */
class AddEditTaskFragment : BaseFragment<FragmentAddEditTaskBinding>() {

    private val viewModel: TasksViewModel by viewModels { ViewModelFactory.create() }

    private var currentTask: Task? = null
    private var startDateMillis: Long = System.currentTimeMillis()
    private var endDateMillis: Long = System.currentTimeMillis()
    private var taskId: Int = 0
    private var workerIdToSelect: Int? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddEditTaskBinding {
        return FragmentAddEditTaskBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        // Get task ID from arguments
        taskId = arguments?.getInt("taskId", 0) ?: 0

        setupDropdowns()
        setupDatePickers()
        setupWageCalculation()
        setupSaveButton()

        // Load task if editing
        if (taskId > 0) {
            loadTask(taskId)
        }
    }

    override fun setupObservers() {
        // Observe workers for dropdown
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeWorkers.collect { workers ->
                val workerNames = workers.map { it.fullName }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, workerNames)
                binding.actvWorker.setAdapter(adapter)
                
                // Pre-select worker if editing
                workerIdToSelect?.let { workerId ->
                    val worker = workers.find { it.id == workerId }
                    worker?.let {
                        binding.actvWorker.setText(it.fullName, false)
                        workerIdToSelect = null // Clear after setting
                    }
                }
            }
        }
    }

    /**
     * Setup dropdown menus
     */
    private fun setupDropdowns() {
        // Category dropdown
        val categories = arrayOf(
            getString(R.string.category_harvest),
            getString(R.string.category_fertilizing),
            getString(R.string.category_pruning),
            getString(R.string.category_milling),
            getString(R.string.category_picknut),
            getString(R.string.category_carrying),
            getString(R.string.category_planting),
            getString(R.string.category_stacking),
            getString(R.string.category_threshing),
            getString(R.string.category_weeding),
            getString(R.string.category_other)
        )
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(categoryAdapter)

        // Status dropdown
        val statuses = arrayOf(
            getString(R.string.status_pending),
            getString(R.string.status_in_progress),
            getString(R.string.status_completed),
            getString(R.string.status_cancelled)
        )
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        binding.actvStatus.setAdapter(statusAdapter)

        // Set default status
        binding.actvStatus.setText(getString(R.string.status_pending), false)

        // Show/hide quantity completed based on status
        binding.actvStatus.setOnItemClickListener { _, _, position, _ ->
            if (statuses[position] == getString(R.string.status_completed)) {
                binding.tilQuantityCompleted.isEnabled = true
            }
        }
    }

    /**
     * Setup date pickers
     */
    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            showDatePicker(startDateMillis) { selectedDate ->
                startDateMillis = selectedDate
                binding.etStartDate.setText(DateUtils.formatToDisplay(selectedDate))
            }
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker(endDateMillis) { selectedDate ->
                endDateMillis = selectedDate
                binding.etEndDate.setText(DateUtils.formatToDisplay(selectedDate))
            }
        }

        // Set initial dates
        binding.etStartDate.setText(DateUtils.formatToDisplay(startDateMillis))
        binding.etEndDate.setText(DateUtils.formatToDisplay(endDateMillis))
    }

    /**
     * Show date picker dialog
     */
    private fun showDatePicker(initialDate: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = initialDate
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Setup real-time wage calculation
     */
    private fun setupWageCalculation() {
        val calculateWage = {
            val quantity = binding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
            val unitPayRate = binding.etUnitPayRate.text.toString().toDoubleOrNull() ?: 0.0
            val totalWage = quantity * unitPayRate
            binding.tvTotalWage.text = String.format("%.0f XAF", totalWage)
        }

        binding.etQuantity.doAfterTextChanged { calculateWage() }
        binding.etUnitPayRate.doAfterTextChanged { calculateWage() }
    }

    /**
     * Setup save button
     */
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveTask()
        }
    }

    /**
     * Load task for editing
     */
    private fun loadTask(taskId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTaskById(taskId).collect { task ->
                if (task != null) {
                    currentTask = task
                    populateTaskData(task)
                }
            }
        }
    }

    /**
     * Populate form with task data
     */
    private fun populateTaskData(task: Task) {
        binding.etDescription.setText(task.description)
        binding.actvCategory.setText(task.category, false)
        binding.actvStatus.setText(mapConstantStatusToDisplay(task.status), false)
        binding.etQuantity.setText(task.quantity?.toString() ?: "")
        binding.etUnitPayRate.setText(task.payRate.toString())
        binding.etQuantityCompleted.setText(task.quantity?.toString() ?: "")
        binding.etRemarks.setText("") // Task entity doesn't have remarks field

        startDateMillis = task.startDate
        endDateMillis = task.endDate ?: System.currentTimeMillis()
        binding.etStartDate.setText(DateUtils.formatToDisplay(task.startDate))
        binding.etEndDate.setText(DateUtils.formatToDisplay(task.endDate ?: System.currentTimeMillis()))

        // Store workerId for pre-selection when workers are loaded
        workerIdToSelect = task.workerId
    }

    /**
     * Save task
     */
    private fun saveTask() {
        val description = binding.etDescription.text.toString()
        val category = binding.actvCategory.text.toString()
        val workerName = binding.actvWorker.text.toString()
        val statusDisplay = binding.actvStatus.text.toString()
        val quantity = binding.etQuantity.text.toString().toDoubleOrNull()
        val payRateInput = binding.etUnitPayRate.text.toString().toDoubleOrNull() ?: 0.0
        // Note: quantityCompleted is in UI but Task entity only has 'quantity' field
        // Note: remarks is in UI but Task entity doesn't have 'remarks' field

        // Basic validation
        if (description.isEmpty()) {
            showError("Description is required")
            return
        }

        if (category.isEmpty()) {
            showError("Category is required")
            return
        }

        if (workerName.isEmpty()) {
            showError("Worker is required")
            return
        }

        // Find worker ID from name
        val workerId = viewModel.activeWorkers.value.find { it.fullName == workerName }?.id ?: 0
        if (workerId == 0) {
            showError("Invalid worker selected")
            return
        }

        // Get current cycle ID from repository
        viewLifecycleOwner.lifecycleScope.launch {
            val cycleId = viewModel.getCurrentCycleId()
            if (cycleId <= 0) {
                showError("No active production cycle. Configure season start month first.")
                return@launch
            }

            val normalizedStatus = mapDisplayStatusToConstant(statusDisplay)
            if (normalizedStatus == null) {
                showError("Select a valid status")
                return@launch
            }

            val task = Task(
                id = currentTask?.id ?: 0,
                workerId = workerId,
                cycleId = cycleId,
                category = category,
                description = description,
                startDate = startDateMillis,
                endDate = endDateMillis,
                quantity = quantity,
                unit = null, // Can be added to UI later if needed
                payRate = payRateInput,
                status = normalizedStatus,
                paidInWagePaymentId = currentTask?.paidInWagePaymentId,
                createdAt = currentTask?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            viewModel.saveTask(task)

            // Navigate back
            findNavController().navigateUp()
        }
    }
    private fun mapDisplayStatusToConstant(status: String): String? {
        return when (status) {
            getString(R.string.status_pending) -> Constants.TASK_STATUS_PENDING
            getString(R.string.status_in_progress) -> Constants.TASK_STATUS_IN_PROGRESS
            getString(R.string.status_completed) -> Constants.TASK_STATUS_COMPLETED
            getString(R.string.status_cancelled) -> Constants.TASK_STATUS_CANCELLED
            else -> null
        }
    }

    private fun mapConstantStatusToDisplay(constant: String): String {
        return when (constant.uppercase()) {
            Constants.TASK_STATUS_PENDING -> getString(R.string.status_pending)
            Constants.TASK_STATUS_IN_PROGRESS -> getString(R.string.status_in_progress)
            Constants.TASK_STATUS_COMPLETED -> getString(R.string.status_completed)
            Constants.TASK_STATUS_CANCELLED -> getString(R.string.status_cancelled)
            else -> getString(R.string.status_pending)
        }
    }
}
