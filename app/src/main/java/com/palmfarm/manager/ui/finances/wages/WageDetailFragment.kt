package com.palmfarm.manager.ui.finances.wages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Task
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.databinding.FragmentWageDetailBinding
import com.palmfarm.manager.databinding.ItemWageTaskPendingBinding
import com.palmfarm.manager.databinding.ItemWageTaskSelectableBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WageDetailFragment : BaseFragment<FragmentWageDetailBinding>() {

    private val viewModel: WagesViewModel by viewModels { ViewModelFactory.create() }

    private var workerId: Int = -1
    private var paymentId: Int = -1

    private val selectedTaskIds = linkedSetOf<Int>()
    private var currentSummary: WagesViewModel.WorkerWageSummary? = null

    private lateinit var completedAdapter: SelectableTaskAdapter
    private lateinit var pendingAdapter: PendingTaskAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWageDetailBinding = FragmentWageDetailBinding.inflate(inflater, container, false)

    override fun setupViews() {
        workerId = arguments?.getInt("workerId") ?: -1
        paymentId = arguments?.getInt("paymentId") ?: -1

        if (workerId <= 0 && paymentId <= 0) {
            showError(getString(R.string.wages_error_worker_missing))
            findNavController().navigateUp()
            return
        }

        completedAdapter = SelectableTaskAdapter { taskId, checked ->
            if (checked) selectedTaskIds.add(taskId) else selectedTaskIds.remove(taskId)
            currentSummary?.let { updateTotals(it) }
        }
        pendingAdapter = PendingTaskAdapter()

        binding.rvCompletedTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completedAdapter
        }

        binding.rvPendingTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendingAdapter
        }

        binding.btnAddAdvance.setOnClickListener {
            val bundle = Bundle().apply { putInt("workerId", workerId) }
            findNavController().navigate(R.id.action_wageDetail_to_addAdvancePaymentFragment, bundle)
        }

        binding.btnProcessSelected.setOnClickListener { showPaymentMethodDialog() }
        binding.btnPrintPayslip.setOnClickListener { printPayslip() }

        toggleMode(paymentId > 0)
    }

    override fun setupObservers() {
        if (paymentId > 0) {
            observeProcessedWage()
        } else {
            observeUnpaidSummary()
        }
    }

    private fun observeUnpaidSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.observeWorkerSummary(workerId).collectLatest { summary ->
                if (summary == null) {
                    return@collectLatest
                }
                currentSummary = summary
                renderWorker(summary.worker)

                completedAdapter.submitList(summary.completedTasks)
                pendingAdapter.submitList(summary.inProgressTasks)

                selectedTaskIds.clear()
                selectedTaskIds.addAll(summary.completedTasks.map { it.id })

                val hasPending = summary.inProgressTasks.isNotEmpty()
                binding.tvPendingSection.visibility = if (hasPending) View.VISIBLE else View.GONE
                binding.rvPendingTasks.visibility = if (hasPending) View.VISIBLE else View.GONE

                updateTotals(summary)
            }
        }
    }

    private fun observeProcessedWage() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.observeProcessedWage(paymentId).collectLatest { processed ->
                if (processed == null) {
                    return@collectLatest
                }

                renderWorker(processed.worker)
                completedAdapter.submitList(processed.paidTasks)
                pendingAdapter.submitList(emptyList())
                binding.tvPendingSection.visibility = View.GONE
                binding.rvPendingTasks.visibility = View.GONE

                binding.tvGrossAmount.text = CurrencyUtils.formatAmount(processed.wagePayment.grossWage)
                binding.tvAdvanceApplied.text = CurrencyUtils.formatAmount(processed.wagePayment.totalAdvances)
                binding.tvNetAmount.text = CurrencyUtils.formatAmount(processed.wagePayment.netPayment)
            }
        }
    }

    private fun renderWorker(worker: Worker) {
        binding.tvWorkerName.text = worker.fullName
        binding.tvWorkerPhone.text = worker.phoneNumber
        binding.tvWorkerSpecialty.text = worker.specialty
    }

    private fun updateTotals(summary: WagesViewModel.WorkerWageSummary) {
        val selected = summary.completedTasks.filter { selectedTaskIds.contains(it.id) }
        val gross = selected.sumOf { (it.quantity ?: 0.0) * it.payRate }
        val advanceApplied = kotlin.math.min(summary.outstandingAdvances, gross)
        val net = gross - advanceApplied

        binding.tvGrossAmount.text = CurrencyUtils.formatAmount(gross)
        binding.tvAdvanceApplied.text = CurrencyUtils.formatAmount(advanceApplied)
        binding.tvNetAmount.text = CurrencyUtils.formatAmount(net)

        binding.btnProcessSelected.isEnabled = selected.isNotEmpty()
        binding.tvSelectionHint.visibility = if (selected.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showPaymentMethodDialog() {
        val summary = currentSummary ?: return
        if (selectedTaskIds.isEmpty()) {
            showToast(getString(R.string.wages_error_no_tasks_selected))
            return
        }

        val methods = resources.getStringArray(R.array.wage_payment_methods)
        var selectedIndex = 0
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wages_select_payment_method)
            .setSingleChoiceItems(methods, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(R.string.action_process) { _, _ ->
                processSelected(summary, methods[selectedIndex])
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun processSelected(summary: WagesViewModel.WorkerWageSummary, method: String) {
        val tasks = summary.completedTasks.filter { selectedTaskIds.contains(it.id) }
        if (tasks.isEmpty()) {
            showToast(getString(R.string.wages_error_no_tasks_selected))
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.processWorkerWage(
                context = requireContext(),
                workerId = summary.worker.id,
                selectedTaskIds = tasks.map { it.id },
                paymentMethod = method
            )

            result.onSuccess {
                showToast(getString(R.string.wages_processed_single, summary.worker.fullName))
                findNavController().navigateUp()
            }.onFailure { error ->
                showError(error.message ?: getString(R.string.wages_error_unknown))
            }
        }
    }

    private fun printPayslip() {
        if (paymentId <= 0) return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.printPayslip(requireContext(), paymentId)
            result.onSuccess { file ->
                showToast(getString(R.string.wages_payslip_generated, file.name))
            }.onFailure { error ->
                showError(error.message ?: getString(R.string.wages_error_pdf_failed))
            }
        }
    }

    private fun toggleMode(processed: Boolean) {
        if (processed) {
            binding.btnProcessSelected.visibility = View.GONE
            binding.btnAddAdvance.visibility = View.GONE
            binding.btnPrintPayslip.visibility = View.VISIBLE
            binding.tvSelectionHint.visibility = View.GONE
            completedAdapter.setInteractionsEnabled(false)
        } else {
            binding.btnProcessSelected.visibility = View.VISIBLE
            binding.btnAddAdvance.visibility = View.VISIBLE
            binding.btnPrintPayslip.visibility = View.GONE
            completedAdapter.setInteractionsEnabled(true)
        }
    }

    private class SelectableTaskAdapter(
        private val selectionChanged: (taskId: Int, checked: Boolean) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<Task, SelectableTaskAdapter.TaskViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
        }
    ) {

        private val currentSelection = mutableSetOf<Int>()
        private var interactionsEnabled: Boolean = true

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val binding = ItemWageTaskSelectableBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TaskViewHolder(binding, ::toggleSelection, ::isSelected)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bind(getItem(position), interactionsEnabled)
        }

        override fun submitList(list: List<Task>?) {
            currentSelection.clear()
            list?.forEach { currentSelection.add(it.id) }
            super.submitList(list)
        }

        fun setInteractionsEnabled(enabled: Boolean) {
            interactionsEnabled = enabled
            notifyDataSetChanged()
        }

        private fun toggleSelection(taskId: Int) {
            val nowSelected = if (currentSelection.contains(taskId)) {
                currentSelection.remove(taskId); false
            } else {
                currentSelection.add(taskId); true
            }
            selectionChanged(taskId, nowSelected)
        }

        private fun isSelected(taskId: Int) = currentSelection.contains(taskId)

        class TaskViewHolder(
            private val binding: ItemWageTaskSelectableBinding,
            private val toggle: (Int) -> Unit,
            private val isSelected: (Int) -> Boolean
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(task: Task, enabled: Boolean) = with(binding) {
                val quantity = task.quantity ?: 0.0
                val rate = CurrencyUtils.formatAmount(task.payRate)
                val amount = CurrencyUtils.formatAmount(quantity * task.payRate)

                tvTaskDescription.text = task.description
                tvTaskSummary.text = root.context.getString(
                    R.string.wages_task_summary,
                    CurrencyUtils.formatQuantity(quantity),
                    rate
                )
                tvTaskAmount.text = amount

                cbTask.isEnabled = enabled
                root.isEnabled = enabled

                cbTask.setOnCheckedChangeListener(null)
                cbTask.isChecked = isSelected(task.id)
                cbTask.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != isSelected(task.id)) {
                        toggle(task.id)
                    }
                }

                root.setOnClickListener { toggle(task.id) }
            }
        }
    }

    private class PendingTaskAdapter :
        androidx.recyclerview.widget.ListAdapter<Task, PendingTaskAdapter.PendingTaskViewHolder>(
            object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Task>() {
                override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
                override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
            }
        ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingTaskViewHolder {
            val binding = ItemWageTaskPendingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PendingTaskViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PendingTaskViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class PendingTaskViewHolder(
            private val binding: ItemWageTaskPendingBinding
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(task: Task) = with(binding) {
                val quantity = task.quantity ?: 0.0
                val rate = CurrencyUtils.formatAmount(task.payRate)
                val amount = CurrencyUtils.formatAmount(quantity * task.payRate)

                tvTaskDescription.text = task.description
                tvTaskStatus.text = task.status
                tvTaskDetails.text = root.context.getString(
                    R.string.wages_task_summary,
                    CurrencyUtils.formatQuantity(quantity),
                    rate
                )
                tvTaskAmount.text = amount
            }
        }
    }
}
