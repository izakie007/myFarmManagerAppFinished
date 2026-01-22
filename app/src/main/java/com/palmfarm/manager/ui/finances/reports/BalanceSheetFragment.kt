package com.palmfarm.manager.ui.finances.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentBalanceSheetBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Balance Sheet Report fragment with assets, liabilities, and equity
 */
class BalanceSheetFragment : BaseFragment<FragmentBalanceSheetBinding>() {

    private val viewModel: ReportsViewModel by viewModels { ViewModelFactory.create() }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBalanceSheetBinding {
        return FragmentBalanceSheetBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        // No specific setup needed
    }

    override fun setupObservers() {
        observeBalanceSheetData()
    }

    /**
     * Observe balance sheet data
     */
    private fun observeBalanceSheetData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getBalanceSheetData().collect { data ->
                // Update date
                binding.tvAsOfDate.text = getString(
                    R.string.as_of_date,
                    DateUtils.formatToDisplay(data.asOfDate)
                )

                // Current Assets
                val currentAssetsText = buildString {
                    data.currentAssets.forEach { (name, value) ->
                        append("  $name: ${CurrencyUtils.formatAmount(value)}\n")
                    }
                }
                binding.tvCurrentAssetsBreakdown.text = currentAssetsText
                binding.tvTotalCurrentAssets.text = CurrencyUtils.formatAmount(data.totalCurrentAssets)

                // Fixed Assets
                binding.tvFixedAssets.text = CurrencyUtils.formatAmount(data.fixedAssets)

                // Total Assets
                binding.tvTotalAssets.text = CurrencyUtils.formatAmount(data.totalAssets)

                // Liabilities
                binding.tvLoansPayable.text = CurrencyUtils.formatAmount(data.loansPayable)
                binding.tvWagesPayable.text = CurrencyUtils.formatAmount(data.wagesPayable)
                binding.tvTotalLiabilities.text = CurrencyUtils.formatAmount(data.totalLiabilities)

                // Equity
                binding.tvNetWorth.text = CurrencyUtils.formatAmount(data.netWorth)

                // Set color for net worth
                binding.tvNetWorth.setTextColor(
                    if (data.netWorth >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )

                // Verification section
                binding.tvAssetsCheck.text = CurrencyUtils.formatAmount(data.totalAssets)
                binding.tvLiabilitiesEquityCheck.text = CurrencyUtils.formatAmount(
                    data.totalLiabilities + data.netWorth
                )

                // Check if balanced
                val isBalanced = abs(
                    data.totalAssets - (data.totalLiabilities + data.netWorth)
                ) < 0.01

                if (isBalanced) {
                    binding.tvBalanceStatus.text = "✓ Balance Sheet is balanced"
                    binding.tvBalanceStatus.setTextColor(requireContext().getColor(R.color.green_500))
                } else {
                    binding.tvBalanceStatus.text = "⚠ Balance Sheet is not balanced"
                    binding.tvBalanceStatus.setTextColor(requireContext().getColor(R.color.md_theme_error))
                }
            }
        }
    }
}
