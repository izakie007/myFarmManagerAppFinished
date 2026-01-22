package com.palmfarm.manager.ui.finances.loans

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentLoansBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Fragment for managing loans
 */
class LoansFragment : BaseFragment<FragmentLoansBinding>() {

    private val viewModel: LoansViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var activeAdapter: LoanAdapter
    private lateinit var paidAdapter: LoanAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoansBinding {
        return FragmentLoansBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupActiveRecyclerView()
        setupPaidRecyclerView()
        setupFab()
    }

    override fun setupObservers() {
        observeActiveLoans()
        observePaidLoans()
        observeSummary()
    }

    /**
     * Setup active loans RecyclerView
     */
    private fun setupActiveRecyclerView() {
        activeAdapter = LoanAdapter(
            onItemClick = { loan ->
                navigateToDetail(loan.id)
            }
        )

        binding.recyclerViewActive.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeAdapter
        }
    }

    /**
     * Setup paid loans RecyclerView
     */
    private fun setupPaidRecyclerView() {
        paidAdapter = LoanAdapter(
            onItemClick = { loan ->
                navigateToDetail(loan.id)
            }
        )

        binding.recyclerViewPaid.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = paidAdapter
        }

        // Collapsible section
        binding.tvPaidSectionHeader.setOnClickListener {
            togglePaidSection()
        }
    }

    /**
     * Toggle paid section visibility
     */
    private fun togglePaidSection() {
        if (binding.recyclerViewPaid.visibility == android.view.View.VISIBLE) {
            binding.recyclerViewPaid.visibility = android.view.View.GONE
            binding.ivPaidExpand.rotation = 0f
        } else {
            binding.recyclerViewPaid.visibility = android.view.View.VISIBLE
            binding.ivPaidExpand.rotation = 180f
        }
    }

    /**
     * Setup FAB
     */
    private fun setupFab() {
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_loans_to_add)
        }
    }

    /**
     * Navigate to loan detail
     */
    private fun navigateToDetail(loanId: Int) {
        val bundle = android.os.Bundle().apply {
            putInt("loanId", loanId)
        }
        findNavController().navigate(R.id.action_loans_to_detail, bundle)
    }

    /**
     * Observe active loans
     */
    private fun observeActiveLoans() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeLoans.collect { loans ->
                activeAdapter.submitList(loans)

                // Show empty state
                if (loans.isEmpty()) {
                    binding.recyclerViewActive.visibility = android.view.View.GONE
                    binding.tvActiveEmpty.visibility = android.view.View.VISIBLE
                } else {
                    binding.recyclerViewActive.visibility = android.view.View.VISIBLE
                    binding.tvActiveEmpty.visibility = android.view.View.GONE
                }
            }
        }
    }

    /**
     * Observe paid loans
     */
    private fun observePaidLoans() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.paidLoans.collect { loans ->
                paidAdapter.submitList(loans)

                // Show empty state
                if (loans.isEmpty()) {
                    binding.tvPaidEmpty.visibility = android.view.View.VISIBLE
                } else {
                    binding.tvPaidEmpty.visibility = android.view.View.GONE
                }
            }
        }
    }

    /**
     * Observe loans summary
     */
    private fun observeSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loansSummary.collect { summary ->
                binding.tvTotalDebt.text = CurrencyUtils.formatAmount(summary.totalDebt)
                binding.tvMonthlyPayment.text = CurrencyUtils.formatAmount(summary.monthlyPayment)
                binding.tvActiveLoansCount.text = summary.activeLoansCount.toString()
            }
        }
    }
}
