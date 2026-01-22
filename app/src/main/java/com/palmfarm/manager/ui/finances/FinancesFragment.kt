package com.palmfarm.manager.ui.finances

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentFinancesBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Finances fragment - Hub for expenses, wages, income, loans, and financial reports
 */
class FinancesFragment : BaseFragment<FragmentFinancesBinding>() {

    private val viewModel: FinancesViewModel by viewModels { ViewModelFactory.create() }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFinancesBinding {
        return FragmentFinancesBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupNavigationButtons()
    }

    override fun setupObservers() {
        observeFinanceSummary()
    }

    /**
     * Setup navigation buttons for each finance category
     */
    private fun setupNavigationButtons() {
        binding.cardExpenses.setOnClickListener {
            findNavController().navigate(R.id.action_finances_to_expenses)
        }

        binding.cardFixedCosts.setOnClickListener {
            findNavController().navigate(R.id.action_finances_to_fixedCosts)
        }

        binding.cardWages.setOnClickListener {
            findNavController().navigate(R.id.action_finances_to_wages)
        }

        binding.cardIncome.setOnClickListener {
            findNavController().navigate(R.id.action_finances_to_income)
        }

        binding.cardLoans.setOnClickListener {
            findNavController().navigate(R.id.action_finances_to_loans)
        }

        binding.cardCashTransactions.setOnClickListener {
            findNavController().navigate(R.id.action_finances_to_cashTransactions)
        }

        binding.cardReports.setOnClickListener {
            findNavController().navigate(R.id.action_finances_to_reports)
        }
    }

    /**
     * Observe finance summary data
     */
    private fun observeFinanceSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.financeSummary.collect { summary ->
                // Update summary cards
                binding.tvTotalIncome.text = CurrencyUtils.formatAmount(summary.totalIncome)
                binding.tvTotalExpenses.text = CurrencyUtils.formatAmount(summary.totalExpenses)
                binding.tvNetBalance.text = CurrencyUtils.formatAmount(summary.netBalance)

                // Set net balance color
                binding.tvNetBalance.setTextColor(
                    if (summary.netBalance >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )

                // Update category details
                binding.tvExpensesAmount.text = CurrencyUtils.formatAmount(summary.totalExpenses)
                binding.tvFixedCostsCount.text = getString(R.string.fixed_costs_count, summary.activeFixedCostsCount)
                binding.tvWagesUnpaid.text = CurrencyUtils.formatAmount(summary.totalWagesUnpaid)
                binding.tvWagesPaid.text = CurrencyUtils.formatAmount(summary.totalWagesPaid)
                binding.tvSalesAmount.text = CurrencyUtils.formatAmount(summary.totalSales)
                binding.tvConsumptionAmount.text = CurrencyUtils.formatAmount(summary.totalConsumption)
                binding.tvLoansDebt.text = CurrencyUtils.formatAmount(summary.totalLoanDebt)
                binding.tvLoansCount.text = getString(R.string.loans_count, summary.activeLoansCount)
            }
        }
    }
}
