package com.palmfarm.manager.ui.finances.income

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.data.database.entities.Consumption
import com.palmfarm.manager.databinding.ItemConsumptionBinding
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for displaying consumption in RecyclerView
 */
class ConsumptionAdapter(
    private val onDeleteClick: (Consumption) -> Unit
) : ListAdapter<Consumption, ConsumptionAdapter.ConsumptionViewHolder>(ConsumptionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsumptionViewHolder {
        val binding = ItemConsumptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConsumptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConsumptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConsumptionViewHolder(
        private val binding: ItemConsumptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(consumption: Consumption) {
            binding.tvQuantity.text = "${consumption.quantityGallons} gallons"
            binding.tvValue.text = CurrencyUtils.formatAmount(consumption.valuedAtPrice)
            binding.tvDate.text = DateUtils.formatToDisplay(consumption.date)

            if (consumption.purpose.isNullOrEmpty()) {
                binding.tvPurpose.visibility = View.GONE
            } else {
                binding.tvPurpose.visibility = View.VISIBLE
                binding.tvPurpose.text = consumption.purpose
            }

            // Delete button
            binding.btnDelete.setOnClickListener {
                onDeleteClick(consumption)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class ConsumptionDiffCallback : DiffUtil.ItemCallback<Consumption>() {
        override fun areItemsTheSame(oldItem: Consumption, newItem: Consumption): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Consumption, newItem: Consumption): Boolean {
            return oldItem == newItem
        }
    }
}
