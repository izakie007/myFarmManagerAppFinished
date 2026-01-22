package com.palmfarm.manager.ui.dashboard.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.ItemCycleHistoryBinding
import com.palmfarm.manager.ui.dashboard.CycleHistorySummary
import com.palmfarm.manager.utils.CurrencyUtils

/**
 * Adapter for cycle history horizontal RecyclerView on Dashboard
 */
class CycleHistoryAdapter : ListAdapter<CycleHistorySummary, CycleHistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCycleHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCycleHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CycleHistorySummary) {
            binding.tvCycleName.text = item.cycleName
            binding.tvBunchesInfo.text = itemView.context.getString(
                R.string.bunches_ratio_format,
                item.realizedBunches,
                item.expectedBunches
            )
            binding.tvExpenses.text = CurrencyUtils.formatAmount(item.totalExpenses)
            binding.tvIncome.text = CurrencyUtils.formatAmount(item.totalIncome)
            binding.tvProfit.text = CurrencyUtils.formatAmount(item.profit)

            // Set profit color based on value
            val profitColor = if (item.profit >= 0) {
                ContextCompat.getColor(itemView.context, R.color.green_500)
            } else {
                ContextCompat.getColor(itemView.context, R.color.md_theme_error)
            }
            binding.tvProfit.setTextColor(profitColor)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CycleHistorySummary>() {
        override fun areItemsTheSame(oldItem: CycleHistorySummary, newItem: CycleHistorySummary): Boolean {
            return oldItem.cycleId == newItem.cycleId
        }

        override fun areContentsTheSame(oldItem: CycleHistorySummary, newItem: CycleHistorySummary): Boolean {
            return oldItem == newItem
        }
    }
}
