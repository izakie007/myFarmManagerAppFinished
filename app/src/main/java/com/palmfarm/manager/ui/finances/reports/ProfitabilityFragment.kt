package com.palmfarm.manager.ui.finances.reports

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentProfitabilityBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Profitability Report fragment with margins and percentages
 */
class ProfitabilityFragment : BaseFragment<FragmentProfitabilityBinding>() {

    private val viewModel: ReportsViewModel by viewModels { ViewModelFactory.create() }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfitabilityBinding {
        return FragmentProfitabilityBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupMenu()
    }

    override fun setupObservers() {
        observeProfitabilityData()
    }

    /**
     * Setup toolbar menu for period selection
     */
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_reports, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_select_period -> {
                        showPeriodDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Show period selection dialog
     */
    private fun showPeriodDialog() {
        val periods = arrayOf(
            getString(R.string.filter_current_cycle),
            getString(R.string.filter_month),
            getString(R.string.filter_quarter),
            getString(R.string.filter_year),
            getString(R.string.filter_all)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_period)
            .setItems(periods) { _, which ->
                val period = when (which) {
                    0 -> ReportPeriod.CURRENT_CYCLE
                    1 -> ReportPeriod.MONTH
                    2 -> ReportPeriod.QUARTER
                    3 -> ReportPeriod.YEAR
                    4 -> ReportPeriod.ALL_TIME
                    else -> ReportPeriod.CURRENT_CYCLE
                }
                viewModel.setReportPeriod(period)
            }
            .show()
    }

    /**
     * Observe profitability data
     */
    private fun observeProfitabilityData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getProfitabilityData().collect { data ->
                // Income section
                binding.tvSalesIncome.text = CurrencyUtils.formatAmount(data.salesIncome)
                binding.tvConsumptionIncome.text = CurrencyUtils.formatAmount(data.consumptionIncome)
                binding.tvTotalIncome.text = CurrencyUtils.formatAmount(data.totalIncome)

                // Expenses section
                binding.tvOperationalExpenses.text = CurrencyUtils.formatAmount(data.operationalExpenses)

                // Expenses breakdown
                val breakdownText = buildString {
                    data.expensesBreakdown.forEach { (category, amount) ->
                        append("  $category: ${CurrencyUtils.formatAmount(amount)}\n")
                    }
                }
                binding.tvExpensesBreakdown.text = breakdownText

                binding.tvDepreciation.text = CurrencyUtils.formatAmount(data.totalDepreciation)

                // Metrics
                binding.tvGrossMargin.text = CurrencyUtils.formatAmount(data.grossMargin)
                binding.tvEbitda.text = CurrencyUtils.formatAmount(data.ebitda)
                binding.tvNetBalance.text = CurrencyUtils.formatAmount(data.netBalance)
                binding.tvBenefitToCostRatio.text = String.format("%.1f", data.benefitToCostRatio)
                binding.tvProfitRate.text = String.format("%.2f%%", data.profitRate)

                // Set colors for margins
                binding.tvGrossMargin.setTextColor(
                    if (data.grossMargin >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )

                binding.tvNetBalance.setTextColor(
                    if (data.netBalance >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )

                // Percentages
                binding.tvGrossMarginPercent.text = String.format("%.1f%%", data.grossMarginPercent)
                binding.tvNetMarginPercent.text = String.format("%.1f%%", data.netMarginPercent)

                // Set colors for percentages
                binding.tvGrossMarginPercent.setTextColor(
                    if (data.grossMarginPercent >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )

                binding.tvNetMarginPercent.setTextColor(
                    if (data.netMarginPercent >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )

                // Set colors for BCR
                binding.tvBenefitToCostRatio.setTextColor(
                    if (data.benefitToCostRatio >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )

                // Set colors for PR
                binding.tvProfitRate.setTextColor(
                    if (data.profitRate >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )
            }
        }
    }
}
