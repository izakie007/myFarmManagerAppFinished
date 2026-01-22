package com.palmfarm.manager.ui.finances.income

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.data.database.entities.Sale
import com.palmfarm.manager.databinding.ItemSaleBinding
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for displaying sales in RecyclerView
 */
class SaleAdapter(
    private val onDeleteClick: (Sale) -> Unit
) : ListAdapter<Sale, SaleAdapter.SaleViewHolder>(SaleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val binding = ItemSaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SaleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SaleViewHolder(
        private val binding: ItemSaleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sale: Sale) {
            binding.tvQuantityUnit.text = "${sale.quantity} ${sale.unit}"
            binding.tvUnitPrice.text = CurrencyUtils.formatAmount(sale.unitPrice)
            binding.tvTotalAmount.text = CurrencyUtils.formatAmount(sale.totalAmount)
            binding.tvDate.text = DateUtils.formatToDisplay(sale.date)

            if (sale.buyerName.isNullOrEmpty()) {
                binding.tvBuyer.visibility = View.GONE
            } else {
                binding.tvBuyer.visibility = View.VISIBLE
                binding.tvBuyer.text = "Buyer: ${sale.buyerName}"
            }

            // Delete button
            binding.btnDelete.setOnClickListener {
                onDeleteClick(sale)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class SaleDiffCallback : DiffUtil.ItemCallback<Sale>() {
        override fun areItemsTheSame(oldItem: Sale, newItem: Sale): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Sale, newItem: Sale): Boolean {
            return oldItem == newItem
        }
    }
}
