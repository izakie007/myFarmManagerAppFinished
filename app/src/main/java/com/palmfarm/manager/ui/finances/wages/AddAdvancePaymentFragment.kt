package com.palmfarm.manager.ui.finances.wages

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.databinding.FragmentAddAdvancePaymentBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class AddAdvancePaymentFragment : BaseFragment<FragmentAddAdvancePaymentBinding>() {

    private val viewModel: WagesViewModel by viewModels { ViewModelFactory.create() }
    private val args: AddAdvancePaymentFragmentArgs by navArgs()

    private var selectedWorkerId: Int? = null
    private var selectedDate: Long = System.currentTimeMillis()
    private var workers: List<Worker> = emptyList()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddAdvancePaymentBinding {
        return FragmentAddAdvancePaymentBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.etDate.setText(DateUtils.formatToDisplay(selectedDate))
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etDate.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        binding.etAmount.doAfterTextChanged {
            binding.tilAmount.error = null
        }

        binding.actvWorker.setOnItemClickListener { _, _, position, _ ->
            if (position in workers.indices) {
                val worker = workers[position]
                selectWorker(worker)
            }
        }

        binding.btnSaveAdvance.setOnClickListener {
            saveAdvance()
        }
    }

    override fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeWorkers.collect { workerList ->
                workers = workerList
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    workerList.map { it.fullName }
                )
                binding.actvWorker.setAdapter(adapter)

                if (selectedWorkerId == null && args.workerId >= 0) {
                    workerList.firstOrNull { it.id == args.workerId }?.let { worker ->
                        binding.actvWorker.setText(worker.fullName, false)
                        selectWorker(worker)
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDate = picked.timeInMillis
                binding.etDate.setText(DateUtils.formatToDisplay(selectedDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun selectWorker(worker: Worker) {
        selectedWorkerId = worker.id
        binding.tilWorker.error = null
        binding.tvAdvanceLimit.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val limitInfo = viewModel.getAdvanceLimit(worker.id)
                val remaining = (limitInfo.maxAllowedAdvance - limitInfo.currentAdvances).coerceAtLeast(0.0)
                binding.tvAdvanceLimit.isVisible = true
                binding.tvAdvanceLimit.text = getString(
                    R.string.advance_limit_remaining,
                    CurrencyUtils.formatAmount(remaining)
                )
            } catch (e: Exception) {
                binding.tvAdvanceLimit.isVisible = true
                binding.tvAdvanceLimit.text = e.message
            } finally {
                setLoading(false)
            }
        }
    }

    private fun saveAdvance() {
        val workerId = selectedWorkerId
        if (workerId == null) {
            binding.tilWorker.error = getString(R.string.error_select_worker)
            binding.actvWorker.requestFocus()
            return
        }

        val amount = binding.etAmount.text?.toString()?.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = getString(R.string.error_enter_amount)
            binding.etAmount.requestFocus()
            return
        }

        val reason = binding.etReason.text?.toString()?.trim().orEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            val result = viewModel.addAdvancePayment(
                workerId = workerId,
                amount = amount,
                date = selectedDate,
                reason = reason
            )
            setLoading(false)

            result.onSuccess { _ ->
                showSuccess(getString(R.string.success_advance_saved))
                findNavController().navigateUp()
            }.onFailure { error ->
                showError(error.message ?: getString(R.string.error_saving_advance))
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progress.isVisible = isLoading
        binding.btnSaveAdvance.isEnabled = !isLoading
    }
}

