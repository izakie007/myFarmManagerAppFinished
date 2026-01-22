package com.palmfarm.manager.ui.finances.fixedcosts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.ItemFixedCostBinding
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for displaying fixed costs with depreciation in RecyclerView
 */
class FixedCostAdapter(
    private val onItemClick: (FixedCostWithDepreciation) -> Unit,
    private val onDeleteClick: (FixedCostWithDepreciation) -> Unit
) : ListAdapter<FixedCostWithDepreciation, FixedCostAdapter.FixedCostViewHolder>(FixedCostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FixedCostViewHolder {
        val binding = ItemFixedCostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FixedCostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FixedCostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FixedCostViewHolder(
        private val binding: ItemFixedCostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FixedCostWithDepreciation) {
            val fixedCost = item.fixedCost

            binding.tvName.text = fixedCost.itemName
            binding.tvType.text = fixedCost.category
            binding.tvAmount.text = CurrencyUtils.formatAmount(fixedCost.amount)
            binding.tvStartDate.text = DateUtils.formatToDisplay(fixedCost.date)

            // Depreciation progress
            binding.progressDepreciation.progress = item.depreciationPercent.toInt()
            binding.tvDepreciationPercent.text = String.format("%.1f%%", item.depreciationPercent)

            // Used value and value left
            binding.tvUsedValue.text = CurrencyUtils.formatAmount(item.usedValue)
            binding.tvValueLeft.text = CurrencyUtils.formatAmount(item.valueLeft)

            // Monthly depreciation
            binding.tvMonthlyDepreciation.text = CurrencyUtils.formatAmount(item.monthlyDepreciation)

            // Grey out if fully depreciated
            if (item.isFullyDepreciated) {
                binding.root.alpha = 0.5f
                binding.tvFullyDepreciated.visibility = android.view.View.VISIBLE
            } else {
                binding.root.alpha = 1.0f
                binding.tvFullyDepreciated.visibility = android.view.View.GONE
            }

            // Set type icon
            val iconRes = when (fixedCost.category) {
                "Land", "LAND" -> R.drawable.ic_production
                "Equipment", "EQUIPMENT" -> R.drawable.ic_task
                else -> R.drawable.ic_finances
            }
            binding.ivTypeIcon.setImageResource(iconRes)

            // Click listeners
            binding.root.setOnClickListener {
                onItemClick(item)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class FixedCostDiffCallback : DiffUtil.ItemCallback<FixedCostWithDepreciation>() {
        override fun areItemsTheSame(
            oldItem: FixedCostWithDepreciation,
            newItem: FixedCostWithDepreciation
        ): Boolean {
            return oldItem.fixedCost.id == newItem.fixedCost.id
        }

        override fun areContentsTheSame(
            oldItem: FixedCostWithDepreciation,
            newItem: FixedCostWithDepreciation
        ): Boolean {
            return oldItem == newItem
        }
    }
}
