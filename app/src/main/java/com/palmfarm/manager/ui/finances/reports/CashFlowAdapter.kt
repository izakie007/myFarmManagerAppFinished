package com.palmfarm.manager.ui.finances.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.ItemCashFlowMonthBinding
import com.palmfarm.manager.utils.CurrencyUtils

/**
 * Adapter for displaying monthly cash flow data
 */
class CashFlowAdapter : ListAdapter<MonthlyFinancials, CashFlowAdapter.CashFlowViewHolder>(CashFlowDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashFlowViewHolder {
        val binding = ItemCashFlowMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CashFlowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CashFlowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CashFlowViewHolder(
        private val binding: ItemCashFlowMonthBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(monthly: MonthlyFinancials) {
            binding.tvMonth.text = monthly.month
            binding.tvIncome.text = CurrencyUtils.formatAmount(monthly.income)
            binding.tvExpenses.text = CurrencyUtils.formatAmount(monthly.expenses)
            binding.tvNetCashFlow.text = CurrencyUtils.formatAmount(monthly.netCashFlow)
            binding.tvCumulativeBalance.text = CurrencyUtils.formatAmount(monthly.cumulativeBalance)

            // Set color for net cash flow
            binding.tvNetCashFlow.setTextColor(
                if (monthly.netCashFlow >= 0) {
                    binding.root.context.getColor(R.color.green_500)
                } else {
                    binding.root.context.getColor(R.color.md_theme_error)
                }
            )

            // Set color for cumulative balance
            binding.tvCumulativeBalance.setTextColor(
                if (monthly.cumulativeBalance >= 0) {
                    binding.root.context.getColor(R.color.green_500)
                } else {
                    binding.root.context.getColor(R.color.md_theme_error)
                }
            )
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class CashFlowDiffCallback : DiffUtil.ItemCallback<MonthlyFinancials>() {
        override fun areItemsTheSame(oldItem: MonthlyFinancials, newItem: MonthlyFinancials): Boolean {
            return oldItem.month == newItem.month
        }

        override fun areContentsTheSame(oldItem: MonthlyFinancials, newItem: MonthlyFinancials): Boolean {
            return oldItem == newItem
        }
    }
}
