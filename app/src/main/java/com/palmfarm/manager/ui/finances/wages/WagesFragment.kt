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
import com.palmfarm.manager.databinding.FragmentWagesBinding
import com.palmfarm.manager.databinding.ItemPaidWageBinding
import com.palmfarm.manager.databinding.ItemUnpaidWageBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WagesFragment : BaseFragment<FragmentWagesBinding>() {

    private val viewModel: WagesViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var unpaidAdapter: UnpaidWageAdapter
    private lateinit var processedAdapter: ProcessedWageAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWagesBinding {
        return FragmentWagesBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupUnpaidList()
        setupProcessedList()

        binding.btnAddAdvance.setOnClickListener {
            findNavController().navigate(R.id.action_wages_to_addAdvancePaymentFragment)
        }

        binding.btnProcessAll.setOnClickListener {
            showPaymentMethodDialog { method ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = viewModel.processAllPendingWages(requireContext(), method)
                    if (result.processed > 0) {
                        showToast(getString(R.string.wages_process_all_success, result.processed))
                    }
                    if (result.errors.isNotEmpty()) {
                        showError(
                            result.errors.joinToString(separator = "\n")
                        )
                    }
                }
            }
        }

        binding.layoutProcessedHeader.setOnClickListener { toggleProcessedSection() }
    }

    override fun setupObservers() {
        observeUnpaidSummaries()
        observeProcessedWages()
        observeSummary()
        observeLoading()
    }

    private fun setupUnpaidList() {
        unpaidAdapter = UnpaidWageAdapter(
            onDetail = { summary ->
                navigateToDetail(workerId = summary.worker.id, paymentId = -1)
            },
            onProcess = { summary ->
                if (!summary.hasPayableWork) {
                    showToast(getString(R.string.wages_no_completed_tasks))
                    return@UnpaidWageAdapter
                }
                showPaymentMethodDialog { method ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val taskIds = summary.completedTasks.map { it.id }
                        val result = viewModel.processWorkerWage(
                            context = requireContext(),
                            workerId = summary.worker.id,
                            selectedTaskIds = taskIds,
                            paymentMethod = method
                        )
                        result.onSuccess {
                            showToast(getString(R.string.wages_processed_single, summary.worker.fullName))
                        }.onFailure { error ->
                            showError(error.message ?: getString(R.string.wages_error_unknown))
                        }
                    }
                }
            }
        )

        binding.rvUnpaid.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = unpaidAdapter
        }
    }

    private fun setupProcessedList() {
        processedAdapter = ProcessedWageAdapter(
            onDetail = { paid ->
                navigateToDetail(workerId = paid.worker.id, paymentId = paid.wagePayment.id)
            },
            onPrint = { paid ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = viewModel.printPayslip(requireContext(), paid.wagePayment.id)
                    result.onSuccess { file ->
                        showToast(getString(R.string.wages_payslip_generated, file.name))
                    }.onFailure { error ->
                        showError(error.message ?: getString(R.string.wages_error_pdf_failed))
                    }
                }
            }
        )

        binding.rvProcessed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = processedAdapter
        }
    }

    private fun observeUnpaidSummaries() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.unpaidSummaries.collectLatest { summaries ->
                unpaidAdapter.submitList(summaries)
                binding.tvUnpaidEmpty.visibility = if (summaries.isEmpty()) View.VISIBLE else View.GONE
                binding.btnProcessAll.isEnabled = summaries.any { it.hasPayableWork }
            }
        }
    }

    private fun observeProcessedWages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.processedWages.collectLatest { paid ->
                processedAdapter.submitList(paid)
                binding.tvProcessedEmpty.visibility = if (paid.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wageSummary.collectLatest { summary ->
                binding.tvPendingGross.text = CurrencyUtils.formatAmount(summary.pendingGross)
                binding.tvPendingNet.text = CurrencyUtils.formatAmount(summary.pendingNet)
                binding.tvProcessedNet.text = CurrencyUtils.formatAmount(summary.processedNet)
                binding.tvInProgressCount.text = summary.inProgressTaskCount.toString()
            }
        }
    }

    private fun observeLoading() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun toggleProcessedSection() {
        val visible = binding.rvProcessed.visibility == View.VISIBLE
        binding.rvProcessed.visibility = if (visible) View.GONE else View.VISIBLE
        binding.ivProcessedToggle.rotation = if (visible) 0f else 180f
    }

    private fun showPaymentMethodDialog(onSelected: (String) -> Unit) {
        val methods = resources.getStringArray(R.array.wage_payment_methods)
        var selectedIndex = 0
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wages_select_payment_method)
            .setSingleChoiceItems(methods, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(R.string.action_process) { _, _ ->
                onSelected(methods[selectedIndex])
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun navigateToDetail(workerId: Int, paymentId: Int) {
        val bundle = Bundle().apply {
            putInt("workerId", workerId)
            putInt("paymentId", paymentId)
        }
        findNavController().navigate(R.id.action_wages_to_detail, bundle)
    }
}

private class UnpaidWageAdapter(
    private val onDetail: (WagesViewModel.WorkerWageSummary) -> Unit,
    private val onProcess: (WagesViewModel.WorkerWageSummary) -> Unit
) : androidx.recyclerview.widget.ListAdapter<WagesViewModel.WorkerWageSummary, UnpaidWageAdapter.UnpaidViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<WagesViewModel.WorkerWageSummary>() {
        override fun areItemsTheSame(
            oldItem: WagesViewModel.WorkerWageSummary,
            newItem: WagesViewModel.WorkerWageSummary
        ) = oldItem.worker.id == newItem.worker.id

        override fun areContentsTheSame(
            oldItem: WagesViewModel.WorkerWageSummary,
            newItem: WagesViewModel.WorkerWageSummary
        ) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnpaidViewHolder {
        val binding = ItemUnpaidWageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UnpaidViewHolder(binding, onDetail, onProcess)
    }

    override fun onBindViewHolder(holder: UnpaidViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UnpaidViewHolder(
        private val binding: ItemUnpaidWageBinding,
        private val onDetail: (WagesViewModel.WorkerWageSummary) -> Unit,
        private val onProcess: (WagesViewModel.WorkerWageSummary) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: WagesViewModel.WorkerWageSummary) = with(binding) {
            tvWorkerName.text = summary.worker.fullName
            tvCompletedTasks.text = tvCompletedTasks.context.getString(
                R.string.wages_completed_tasks_count,
                summary.completedTasks.size
            )
            tvInProgressTasks.text = tvInProgressTasks.context.getString(
                R.string.wages_in_progress_tasks_count,
                summary.inProgressTasks.size
            )
            tvGrossAmount.text = CurrencyUtils.formatAmount(summary.completedGross)
            tvOutstandingAdvance.text = CurrencyUtils.formatAmount(summary.outstandingAdvances)
            tvNetAmount.text = CurrencyUtils.formatAmount(summary.estimatedNet)

            btnProcess.isEnabled = summary.hasPayableWork
            btnProcess.alpha = if (summary.hasPayableWork) 1f else 0.5f

            root.setOnClickListener { onDetail(summary) }
            btnDetails.setOnClickListener { onDetail(summary) }
            btnProcess.setOnClickListener { onProcess(summary) }
        }
    }
}

private class ProcessedWageAdapter(
    private val onDetail: (WagesViewModel.ProcessedWageSummary) -> Unit,
    private val onPrint: (WagesViewModel.ProcessedWageSummary) -> Unit
) : androidx.recyclerview.widget.ListAdapter<WagesViewModel.ProcessedWageSummary, ProcessedWageAdapter.ProcessedViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<WagesViewModel.ProcessedWageSummary>() {
        override fun areItemsTheSame(
            oldItem: WagesViewModel.ProcessedWageSummary,
            newItem: WagesViewModel.ProcessedWageSummary
        ) = oldItem.wagePayment.id == newItem.wagePayment.id

        override fun areContentsTheSame(
            oldItem: WagesViewModel.ProcessedWageSummary,
            newItem: WagesViewModel.ProcessedWageSummary
        ) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessedViewHolder {
        val binding = ItemPaidWageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProcessedViewHolder(binding, onDetail, onPrint)
    }

    override fun onBindViewHolder(holder: ProcessedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProcessedViewHolder(
        private val binding: ItemPaidWageBinding,
        private val onDetail: (WagesViewModel.ProcessedWageSummary) -> Unit,
        private val onPrint: (WagesViewModel.ProcessedWageSummary) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: WagesViewModel.ProcessedWageSummary) = with(binding) {
            tvWorkerName.text = summary.worker.fullName
            tvPaymentDate.text = android.text.format.DateFormat.format(
                "dd MMM yyyy",
                summary.wagePayment.paymentDate
            )
            tvNetAmount.text = CurrencyUtils.formatAmount(summary.wagePayment.netPayment)
            tvGrossAmount.text = CurrencyUtils.formatAmount(summary.wagePayment.grossWage)

            root.setOnClickListener { onDetail(summary) }
            btnDetails.setOnClickListener { onDetail(summary) }
            btnPrint.setOnClickListener { onPrint(summary) }
        }
    }
}
