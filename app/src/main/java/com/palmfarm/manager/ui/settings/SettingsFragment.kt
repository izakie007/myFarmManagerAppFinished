package com.palmfarm.manager.ui.settings

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Filter
import androidx.core.widget.doAfterTextChanged
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Farm
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.databinding.FragmentSettingsBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.ui.settings.adapters.FarmAdapter
import com.palmfarm.manager.ui.settings.adapters.WorkerAdapter
import com.palmfarm.manager.utils.BiometricUtils
import com.palmfarm.manager.utils.PermissionHelper
import com.palmfarm.manager.utils.SecurityUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Settings fragment with enterprise info, farms, workers, and configurations
 */
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var farmAdapter: FarmAdapter
    private lateinit var workerAdapter: WorkerAdapter

    private var isBindingData = false
    private var nameUpdateJob: Job? = null
    private var locationUpdateJob: Job? = null
    private var phoneUpdateJob: Job? = null
    private var expectedBunchesJob: Job? = null
    private var tonnageJob: Job? = null

    // Track pending backup/restore operation
    private var pendingBackupOperation = false
    private var pendingRestoreOperation = false
    private val restoreFilePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        confirmRestoreFromUri(uri)
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupEnterpriseSection()
        setupFarmsSection()
        setupWorkersSection()
        setupProductionSettingsSection()
        setupAuthenticationSection()
        setupDataManagementSection()
        setupAboutSection()
    }

    override fun setupObservers() {
        observeAppSettings()
        observeFarms()
        observeWorkers()
        observeTotalPalms()
    }

    /**
     * Setup enterprise information section with auto-save
     */
    private fun setupEnterpriseSection() {
        binding.etEnterpriseName.doAfterTextChanged { text ->
            if (isBindingData) return@doAfterTextChanged
            nameUpdateJob?.cancel()
            nameUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                viewModel.updateEnterpriseName(text?.toString() ?: "")
            }
        }

        binding.etEnterpriseLocation.doAfterTextChanged { text ->
            if (isBindingData) return@doAfterTextChanged
            locationUpdateJob?.cancel()
            locationUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                viewModel.updateEnterpriseLocation(text?.toString() ?: "")
            }
        }

        binding.etEnterprisePhone.doAfterTextChanged { text ->
            if (isBindingData) return@doAfterTextChanged
            phoneUpdateJob?.cancel()
            phoneUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                viewModel.updateEnterprisePhone(text?.toString() ?: "")
            }
        }
    }

    /**
     * Setup farms section
     */
    private fun setupFarmsSection() {
        farmAdapter = FarmAdapter(
            onEditClick = { farm ->
                val bundle = Bundle().apply {
                    putInt("farmId", farm.id)
                }
                findNavController().navigate(R.id.action_settings_to_addEditFarm, bundle)
            },
            onDeleteClick = { farm ->
                showDeleteFarmConfirmation(farm)
            }
        )

        binding.recyclerViewFarms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = farmAdapter
        }

        binding.btnAddFarm.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_addEditFarm)
        }
    }

    /**
     * Setup workers section
     */
    private fun setupWorkersSection() {
        workerAdapter = WorkerAdapter(
            onEditClick = { worker ->
                val bundle = Bundle().apply {
                    putInt("workerId", worker.id)
                }
                findNavController().navigate(R.id.action_settings_to_addEditWorker, bundle)
            },
            onDeleteClick = { worker ->
                showDeleteWorkerConfirmation(worker)
            }
        )

        binding.recyclerViewWorkers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = workerAdapter
        }

        binding.btnAddWorker.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_addEditWorker)
        }
    }

    /**
     * Setup production settings section
     */
    private fun setupProductionSettingsSection() {
        // Season start month dropdown
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val monthItems = months.toList()

        val monthAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            monthItems
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        return FilterResults().apply {
                            values = monthItems
                            count = monthItems.size
                        }
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }

                    override fun convertResultToString(resultValue: Any?): CharSequence {
                        return resultValue as? CharSequence ?: ""
                    }
                }
            }
        }

        binding.actvSeasonStartMonth.setAdapter(monthAdapter)
        binding.actvSeasonStartMonth.keyListener = null
        binding.actvSeasonStartMonth.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.actvSeasonStartMonth.showDropDown()
        }
        binding.actvSeasonStartMonth.setOnClickListener {
            binding.actvSeasonStartMonth.showDropDown()
        }

        binding.actvSeasonStartMonth.setOnItemClickListener { _, _, position, _ ->
            if (isBindingData) return@setOnItemClickListener
            viewModel.updateSeasonStartMonth(position + 1)
        }

        binding.etExpectedBunches.doAfterTextChanged {
            if (isBindingData) return@doAfterTextChanged
            expectedBunchesJob?.cancel()
            expectedBunchesJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                val value = it?.toString()?.toIntOrNull()
                viewModel.updateExpectedBunches(value)
            }
        }

        binding.etTonnage.doAfterTextChanged {
            if (isBindingData) return@doAfterTextChanged
            tonnageJob?.cancel()
            tonnageJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                val value = it?.toString()?.toDoubleOrNull()
                viewModel.updateTonnage(value)
            }
        }
    }

    /**
     * Setup authentication section
     */
    private fun setupAuthenticationSection() {
        binding.btnChangeAuthentication.setOnClickListener {
            showAuthenticationDialog()
        }
    }

    /**
     * Setup data management section
     */
    private fun setupDataManagementSection() {
        binding.btnBackup.setOnClickListener {
            performBackup()
        }

        binding.btnRestore.setOnClickListener {
            performRestore()
        }
    }

    /**
     * Setup about section
     */
    private fun setupAboutSection() {
        binding.tvAppVersion.text = "Version 2.2"
        binding.tvAppInfo.text = "Palm Farm Manager \nA comprehensive palm farm management app \nBuilt by Eng Isaac Epie"
    }

    /**
     * Observe app settings
     */
    private fun observeAppSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.appSettings.collect { settings ->
                isBindingData = true
                try {
                    if (settings != null) {
                        updateEditText(binding.etEnterpriseName, settings.enterpriseName.orEmpty())
                        updateEditText(binding.etEnterpriseLocation, settings.location.orEmpty())
                        updateEditText(binding.etEnterprisePhone, settings.enterprisePhone.orEmpty())

                        val months = listOf(
                            "January", "February", "March", "April", "May", "June",
                            "July", "August", "September", "October", "November", "December"
                        )
                        val monthLabel = if (settings.seasonStartMonth in 1..12) {
                            months[settings.seasonStartMonth - 1]
                        } else {
                            ""
                        }
                        if (binding.actvSeasonStartMonth.text?.toString() != monthLabel) {
                            binding.actvSeasonStartMonth.setText(monthLabel, false)
                        }

                        updateEditText(
                            binding.etExpectedBunches,
                            if (settings.currentCycleExpectedBunches > 0)
                                settings.currentCycleExpectedBunches.toString()
                            else ""
                        )
                        updateEditText(
                            binding.etTonnage,
                            if (settings.tonnage > 0.0)
                                settings.tonnage.toString()
                            else ""
                        )

                        // Display current authentication method
                        val authMethodText = when (settings.authMethod) {
                            "NONE" -> "None"
                            "PASSWORD" -> "Password"
                            "BIOMETRIC" -> "Biometric"
                            else -> "None"
                        }
                        binding.tvCurrentAuthMethod.text = "Current: $authMethodText"
                    } else {
                        updateEditText(binding.etEnterpriseName, "")
                        updateEditText(binding.etEnterpriseLocation, "")
                        updateEditText(binding.etEnterprisePhone, "")
                        binding.actvSeasonStartMonth.setText("", false)
                        updateEditText(binding.etExpectedBunches, "")
                        updateEditText(binding.etTonnage, "")
                    }
                } finally {
                    isBindingData = false
                }
            }
        }
    }

    private fun updateEditText(editText: EditText, value: String) {
        if (editText.text?.toString() == value) return

        val hadFocus = editText.hasFocus()
        val start = if (hadFocus) editText.selectionStart else -1
        val end = if (hadFocus) editText.selectionEnd else -1

        editText.setText(value)

        if (hadFocus) {
            val clampedStart = start.coerceIn(0, value.length)
            val clampedEnd = end.coerceIn(0, value.length)
            editText.setSelection(clampedStart, clampedEnd)
        }
    }

    /**
     * Observe farms
     */
    private fun observeFarms() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.farms.collect { farms ->
                farmAdapter.submitList(farms)

                if (farms.isEmpty()) {
                    binding.recyclerViewFarms.visibility = View.GONE
                    binding.tvEmptyFarms.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewFarms.visibility = View.VISIBLE
                    binding.tvEmptyFarms.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Observe workers
     */
    private fun observeWorkers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.workers.collect { workers ->
                workerAdapter.submitList(workers)

                if (workers.isEmpty()) {
                    binding.recyclerViewWorkers.visibility = View.GONE
                    binding.tvEmptyWorkers.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewWorkers.visibility = View.VISIBLE
                    binding.tvEmptyWorkers.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Observe total palms
     */
    private fun observeTotalPalms() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalPalms.collect { total ->
                binding.tvTotalPalmsCount.text = "$total total palms across all farms"
            }
        }
    }

    /**
     * Show delete farm confirmation
     */
    private fun showDeleteFarmConfirmation(farm: Farm) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage("Delete ${farm.name}?")
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteFarm(farm.id)
                showToast(getString(R.string.success_deleted))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Show delete worker confirmation
     */
    private fun showDeleteWorkerConfirmation(worker: Worker) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage("Delete ${worker.firstName}?")
            .setPositiveButton(R.string.action_delete) { _, _ ->
                val result = viewModel.deleteWorker(worker.id)
                result.onSuccess {
                    showToast(getString(R.string.success_deleted))
                }.onFailure {
                    showError("Cannot delete worker with existing tasks or wages")
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Show authentication dialog
     */
    private fun showAuthenticationDialog() {
        val authMethods = arrayOf("None", "Password", "Biom")
        var selectedMethod = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Authentication")
            .setSingleChoiceItems(authMethods, selectedMethod) { _, which ->
                selectedMethod = which
            }
            .setPositiveButton("Save") { _, _ ->
                when (selectedMethod) {
                    0 -> updateAuthMethod("NONE", null)
                    1 -> showPasswordDialog()
                    2 -> setupBiometricAuth()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Show password dialog
     */
    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_password, null)
        val etPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPassword)
        val tilPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPassword)
        val tilConfirmPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilConfirmPassword)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Password")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val password = etPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                // Validate password
                tilPassword.error = null
                tilConfirmPassword.error = null

                when {
                    password.isEmpty() -> {
                        tilPassword.error = "Password is required"
                    }
                    password.length < 4 -> {
                        tilPassword.error = "Password must be at least 4 characters"
                    }
                    password != confirmPassword -> {
                        tilConfirmPassword.error = "Passwords do not match"
                    }
                    else -> {
                        // Hash password and save
                        val passwordHash = SecurityUtils.hashPassword(password)
                        updateAuthMethod("PASSWORD", passwordHash)
                    }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Setup biometric authentication
     */
    private fun setupBiometricAuth() {
        // Check if biometric is available
        // Use BIOMETRIC_WEAK to match the authenticator used in BiometricUtils
        if (BiometricUtils.isBiometricAvailable(requireContext())) {
            // Biometric available, set it up
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Enable Biometric")
                .setMessage("Biometric authentication will be required to access the app")
                .setPositiveButton("Enable") { _, _ ->
                    updateAuthMethod("BIOMETRIC", null)
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } else {
            // Biometric not available, show error message
            val message = BiometricUtils.getBiometricStatusMessage(requireContext())
            showError(message)
        }
    }

    /**
     * Update authentication method
     */
    private fun updateAuthMethod(method: String, passwordHash: String?) {
        viewModel.updateAuthMethod(method, passwordHash)
        showToast("Authentication method updated")
    }

    /**
     * Perform database backup
     */
    private fun performBackup() {
        // Check if permissions are granted
        if (!PermissionHelper.hasBackupRestorePermissions(requireContext())) {
            // Request permissions
            pendingBackupOperation = true
            showPermissionRationaleAndRequest(isBackup = true)
            return
        }

        // Permissions granted, proceed with backup
        executeBackup()
    }

    /**
     * Execute backup operation
     */
    private fun executeBackup() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading()
                val result = viewModel.backupDatabase()

                result.onSuccess { backupFile ->
                    hideLoading()
                    val info = viewModel.getBackupInfo(backupFile)
                    showToast("Backup successful: ${info.timestamp}")

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Backup Created")
                        .setMessage("Backup file: ${backupFile.name}\nSize: ${String.format("%.2f MB", info.sizeMB)}\nLocation: ${backupFile.parent}")
                        .setPositiveButton("OK", null)
                        .show()
                }.onFailure { error ->
                    hideLoading()
                    showError("Backup failed: ${error.message}")
                }
            } catch (e: Exception) {
                hideLoading()
                showError("Backup error: ${e.message}")
            }
        }
    }

    /**
     * Perform database restore
     */
    private fun performRestore() {
        // Use SAF picker so user can restore from any folder/app source.
        restoreFilePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
    }

    /**
     * Execute restore operation
     */
    private fun executeRestore() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = viewModel.getBackupFiles()

                result.onSuccess { backupFiles ->
                    if (backupFiles.isEmpty()) {
                        showToast("No backup files found")
                        return@launch
                    }

                    // Show backup file selection dialog
                    val fileNames = backupFiles.map { file ->
                        val info = viewModel.getBackupInfo(file)
                        "${info.timestamp} (${String.format("%.2f MB", info.sizeMB)})"
                    }.toTypedArray()

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Select Backup to Restore")
                        .setItems(fileNames) { _, which ->
                            confirmRestore(backupFiles[which])
                        }
                        .setNegativeButton(R.string.action_cancel, null)
                        .show()
                }.onFailure { error ->
                    showError("Failed to load backups: ${error.message}")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    /**
     * Show permission rationale and request permissions
     */
    private fun showPermissionRationaleAndRequest(isBackup: Boolean) {
        val operationType = if (isBackup) "Backup" else "Restore"
        val message = PermissionHelper.getPermissionRationaleMessage(Manifest.permission.READ_EXTERNAL_STORAGE)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$operationType Permission Required")
            .setMessage(message)
            .setPositiveButton("Grant Permission") { _, _ ->
                PermissionHelper.requestBackupRestorePermissions(this)
            }
            .setNegativeButton("Cancel") { _, _ ->
                pendingBackupOperation = false
                pendingRestoreOperation = false
                showToast("$operationType cancelled - permission required")
            }
            .show()
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionHelper.handlePermissionResult(
            requestCode = requestCode,
            grantResults = grantResults,
            onGranted = {
                // Permission granted, execute pending operation
                when {
                    pendingBackupOperation -> {
                        pendingBackupOperation = false
                        executeBackup()
                    }
                    pendingRestoreOperation -> {
                        pendingRestoreOperation = false
                        executeRestore()
                    }
                }
            },
            onDenied = {
                // Permission denied
                pendingBackupOperation = false
                pendingRestoreOperation = false

                val operation = when (requestCode) {
                    PermissionHelper.REQUEST_STORAGE_ALL -> "Backup/Restore"
                    else -> "This operation"
                }

                showError("$operation requires storage permission. Please grant permission in app settings.")
            }
        )
    }

    /**
     * Confirm restore operation
     */
    private fun confirmRestore(backupFile: File) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Restore")
            .setMessage("This will replace all current data with the backup. A safety backup of current data will be created. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                performRestoreOperation(backupFile)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Perform actual restore operation
     */
    private fun performRestoreOperation(backupFile: File) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading()
                val result = viewModel.restoreDatabase(backupFile)

                result.onSuccess {
                    hideLoading()
                    showToast("Restore successful")

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Restore Complete")
                        .setMessage("Database restored successfully. Please restart the app for changes to take effect.")
                        .setPositiveButton("OK") { _, _ ->
                            // Exit app
                            requireActivity().finishAffinity()
                        }
                        .setCancelable(false)
                        .show()
                }.onFailure { error ->
                    hideLoading()
                    showError("Restore failed: ${error.message}")
                }
            } catch (e: Exception) {
                hideLoading()
                showError("Restore error: ${e.message}")
            }
        }
    }

    private fun confirmRestoreFromUri(backupUri: android.net.Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Restore")
            .setMessage("This will replace all current data with the selected backup file. A safety backup of current data will be created. Continue?")
            .setPositiveButton("Restore") { _, _ ->
                performRestoreOperationFromUri(backupUri)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun performRestoreOperationFromUri(backupUri: android.net.Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading()
                val result = viewModel.restoreDatabaseFromUri(backupUri)
                result.onSuccess {
                    hideLoading()
                    showToast("Restore successful")
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Restore Complete")
                        .setMessage("Database restored successfully. Please restart the app for changes to take effect.")
                        .setPositiveButton("OK") { _, _ -> requireActivity().finishAffinity() }
                        .setCancelable(false)
                        .show()
                }.onFailure { error ->
                    hideLoading()
                    showError("Restore failed: ${error.message}")
                }
            } catch (e: Exception) {
                hideLoading()
                showError("Restore error: ${e.message}")
            }
        }
    }
}
