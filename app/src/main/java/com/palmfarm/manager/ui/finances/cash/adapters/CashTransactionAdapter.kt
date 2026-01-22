package com.palmfarm.manager.ui.finances.cash.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.CashTransaction
import com.palmfarm.manager.databinding.ItemCashTransactionBinding
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for cash transaction list
 */
class CashTransactionAdapter(
    private val onEditClick: (CashTransaction) -> Unit,
    private val onDeleteClick: (CashTransaction) -> Unit
) : ListAdapter<CashTransaction, CashTransactionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCashTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCashTransactionBinding,
        private val onEditClick: (CashTransaction) -> Unit,
        private val onDeleteClick: (CashTransaction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: CashTransaction) {
            // Date
            binding.tvDate.text = DateUtils.formatToDisplay(transaction.date)

            // Transaction type with icon
            val context = binding.root.context
            val transactionTypeText = when (transaction.transactionType) {
                CashTransaction.TYPE_OPENING_BALANCE -> context.getString(R.string.transaction_type_opening_balance)
                CashTransaction.TYPE_CASH_IN -> context.getString(R.string.transaction_type_cash_in)
                CashTransaction.TYPE_CASH_OUT -> context.getString(R.string.transaction_type_cash_out)
                CashTransaction.TYPE_ADJUSTMENT -> context.getString(R.string.transaction_type_adjustment)
                else -> transaction.transactionType
            }
            binding.tvTransactionType.text = transactionTypeText

            // Description
            binding.tvDescription.text = transaction.description

            // Category (if not empty)
            if (transaction.category.isNotEmpty()) {
                binding.tvCategory.text = transaction.category
            } else {
                binding.tvCategory.text = "-"
            }

            // Amount with color coding
            val amountText = CurrencyUtils.formatAmount(transaction.amount)
            binding.tvAmount.text = amountText

            // Color code based on transaction type
            val amountColor = when (transaction.transactionType) {
                CashTransaction.TYPE_CASH_IN, CashTransaction.TYPE_OPENING_BALANCE -> {
                    ContextCompat.getColor(context, R.color.success_green)
                }
                CashTransaction.TYPE_CASH_OUT -> {
                    ContextCompat.getColor(context, R.color.error_red)
                }
                else -> {
                    ContextCompat.getColor(context, R.color.text_primary)
                }
            }
            binding.tvAmount.setTextColor(amountColor)

            // Click listeners
            binding.btnEdit.setOnClickListener {
                onEditClick(transaction)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(transaction)
            }

            // Card click for details
            binding.root.setOnClickListener {
                onEditClick(transaction)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CashTransaction>() {
        override fun areItemsTheSame(
            oldItem: CashTransaction,
            newItem: CashTransaction
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: CashTransaction,
            newItem: CashTransaction
        ): Boolean {
            return oldItem == newItem
        }
    }
}
