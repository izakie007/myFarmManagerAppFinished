package com.palmfarm.manager.ui.finances.cash

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentCashTransactionsBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.ui.finances.cash.adapters.CashTransactionAdapter
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.gone
import com.palmfarm.manager.utils.visible
import kotlinx.coroutines.launch

/**
 * Fragment for displaying cash transactions list
 */
class CashTransactionFragment : BaseFragment<FragmentCashTransactionsBinding>() {

    private val viewModel: CashTransactionViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var adapter: CashTransactionAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCashTransactionsBinding {
        return FragmentCashTransactionsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerView()
        setupFab()
    }

    override fun setupObservers() {
        observeTransactions()
        observeBalance()
        observeCashFlowSummary()
        observeLoading()
        observeErrors()
    }

    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        adapter = CashTransactionAdapter(
            onEditClick = { transaction ->
                val bundle = android.os.Bundle().apply {
                    putInt("transactionId", transaction.id)
                }
                findNavController().navigate(
                    R.id.action_cashTransactions_to_addEditTransaction,
                    bundle
                )
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmation(transaction)
            }
        )

        binding.recyclerViewTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CashTransactionFragment.adapter
        }
    }

    /**
     * Setup FAB for adding new transaction
     */
    private fun setupFab() {
        binding.fabAddTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_cashTransactions_to_addEditTransaction)
        }
    }

    /**
     * Observe cash transactions list
     */
    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactions.collect { transactions ->
                adapter.submitList(transactions)

                // Show/hide empty state
                if (transactions.isEmpty()) {
                    binding.recyclerViewTransactions.gone()
                    binding.layoutEmptyState.visible()
                } else {
                    binding.recyclerViewTransactions.visible()
                    binding.layoutEmptyState.gone()
                }
            }
        }
    }

    /**
     * Observe current cash balance
     */
    private fun observeBalance() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentBalance.collect { balance ->
                binding.tvCurrentBalance.text = CurrencyUtils.formatAmount(balance)
            }
        }
    }

    /**
     * Observe cash flow summary
     */
    private fun observeCashFlowSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cashFlowSummary.collect { summary ->
                summary?.let {
                    binding.tvCashIn.text = CurrencyUtils.formatAmountAbbreviated(it.cashIn)
                    binding.tvCashOut.text = CurrencyUtils.formatAmountAbbreviated(it.cashOut)
                    binding.tvNetFlow.text = CurrencyUtils.formatAmountAbbreviated(it.netCashFlow)
                }
            }
        }
    }

    /**
     * Observe loading state
     */
    private fun observeLoading() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
            }
        }
    }

    /**
     * Observe errors
     */
    private fun observeErrors() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            success?.let {
                showToast(it)
                viewModel.clearSuccess()
            }
        }
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmation(transaction: com.palmfarm.manager.data.database.entities.CashTransaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(getString(R.string.delete_transaction_confirmation, transaction.description))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteTransaction(transaction.id)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
