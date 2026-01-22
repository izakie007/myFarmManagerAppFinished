package com.palmfarm.manager.ui.finances.expenses

import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.dao.ExpenseDao
import com.palmfarm.manager.data.database.entities.Expense
import com.palmfarm.manager.databinding.FragmentAddEditExpenseBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Calendar

/**
 * Fragment for adding or editing an expense with photo capture
 */
class AddEditExpenseFragment : BaseFragment<FragmentAddEditExpenseBinding>() {

    private val viewModel: ExpensesViewModel by viewModels { ViewModelFactory.create() }

    private var currentExpense: Expense? = null
    private var expenseId: Int = 0
    private var dateMillis: Long = System.currentTimeMillis()
    private var currentPhotoPath: String? = null
    private var currentPhotoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            // Photo saved successfully
            showToast(getString(R.string.photo_captured))
            binding.ivReceiptPreview.visibility = android.view.View.VISIBLE
            binding.ivReceiptPreview.setImageURI(currentPhotoUri)
            binding.btnRemovePhoto.visibility = android.view.View.VISIBLE
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            capturePhoto()
        } else {
            showError(getString(R.string.camera_permission_denied))
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddEditExpenseBinding {
        return FragmentAddEditExpenseBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Get expense ID from arguments
        expenseId = arguments?.getInt("expenseId", 0) ?: 0

        setupCategoryDropdown()
        setupDatePicker()
        setupPhotoButtons()
        setupSaveButton()

        // Load expense if editing
        if (expenseId > 0) {
            loadExpense(expenseId)
        }
    }

    override fun setupObservers() {
        // No specific observers needed
    }

    /**
     * Setup category dropdown
     */
    private fun setupCategoryDropdown() {
        val categories = arrayOf(
            getString(R.string.expense_category_supplies),
            getString(R.string.expense_category_maintenance),
            getString(R.string.expense_category_fuel),
            getString(R.string.expense_category_transport),
            getString(R.string.expense_category_other)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    /**
     * Setup date picker
     */
    private fun setupDatePicker() {
        binding.etDate.setText(DateUtils.formatToDisplay(dateMillis))

        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    dateMillis = selectedCalendar.timeInMillis
                    binding.etDate.setText(DateUtils.formatToDisplay(dateMillis))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Setup photo capture buttons
     */
    private fun setupPhotoButtons() {
        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndCapture()
        }

        binding.btnRemovePhoto.setOnClickListener {
            currentPhotoPath = null
            currentPhotoUri = null
            binding.ivReceiptPreview.visibility = android.view.View.GONE
            binding.btnRemovePhoto.visibility = android.view.View.GONE
        }
    }

    /**
     * Check camera permission and capture photo
     */
    private fun checkCameraPermissionAndCapture() {
        when {
            requireContext().checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                capturePhoto()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                showError(getString(R.string.camera_permission_rationale))
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Capture photo using camera
     */
    private fun capturePhoto() {
        // Create file for photo
        val photoDir = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "ExpenseReceipts"
        )
        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }

        val photoFile = File(photoDir, "receipt_${System.currentTimeMillis()}.jpg")
        currentPhotoPath = photoFile.absolutePath

        currentPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        takePictureLauncher.launch(currentPhotoUri)
    }

    /**
     * Setup save button
     */
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveExpense()
        }
    }

    /**
     * Load expense for editing
     */
    private fun loadExpense(expenseId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val expense = viewModel.getExpenseById(expenseId)

            if (expense != null) {
                currentExpense = expense
                populateExpenseData(expense)
            }
        }
    }

    /**
     * Populate form with expense data
     */
    private fun populateExpenseData(expense: Expense) {
        binding.etAmount.setText(expense.amount.toString())
        binding.actvCategory.setText(expense.category, false)
        binding.etDescription.setText(expense.description)

        dateMillis = expense.date
        binding.etDate.setText(DateUtils.formatToDisplay(expense.date))

        // Show photo if exists
        if (expense.receiptPhotoPath != null) {
            currentPhotoPath = expense.receiptPhotoPath
            val photoFile = File(expense.receiptPhotoPath)
            if (photoFile.exists()) {
                currentPhotoUri = Uri.fromFile(photoFile)
                binding.ivReceiptPreview.setImageURI(currentPhotoUri)
                binding.ivReceiptPreview.visibility = android.view.View.VISIBLE
                binding.btnRemovePhoto.visibility = android.view.View.VISIBLE
            }
        }
    }

    /**
     * Save expense
     */
    private fun saveExpense() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val category = binding.actvCategory.text.toString()
        val description = binding.etDescription.text.toString()

        // Validation
        if (amount == null || amount <= 0) {
            showError(getString(R.string.error_invalid_amount))
            return
        }

        if (category.isEmpty()) {
            showError(getString(R.string.error_category_required))
            return
        }

        if (description.isEmpty()) {
            showError(getString(R.string.error_description_required))
            return
        }

        // Get current cycle ID
        viewLifecycleOwner.lifecycleScope.launch {
            val cycleRepository = ViewModelFactory.create().productionCycleRepository
            val currentCycle = cycleRepository.getCurrentCycle().first()
            val cycleId = currentCycle?.id ?: 1

            val expense = Expense(
                id = currentExpense?.id ?: 0,
                cycleId = cycleId,
                date = dateMillis,
                amount = amount,
                category = category,
                description = description,
                receiptPhotoPath = currentPhotoPath,
                createdAt = currentExpense?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            viewModel.saveExpense(expense)

            // Navigate back
            findNavController().navigateUp()
        }
    }
}
