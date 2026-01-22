package com.palmfarm.manager.ui.finances.expenses

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Expense
import com.palmfarm.manager.databinding.ItemExpenseBinding
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for displaying expenses in RecyclerView
 */
class ExpenseAdapter(
    private val onItemClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            binding.tvCategory.text = expense.category
            binding.tvDescription.text = expense.description
            binding.tvAmount.text = CurrencyUtils.formatAmount(expense.amount)
            binding.tvDate.text = DateUtils.formatToDisplay(expense.date)

            // Set category icon based on category
            val iconRes = when (expense.category) {
                "Supplies" -> R.drawable.ic_production
                "Maintenance" -> R.drawable.ic_task
                "Fuel" -> R.drawable.ic_production
                "Transport" -> R.drawable.ic_production
                else -> R.drawable.ic_finances
            }
            binding.ivCategoryIcon.setImageResource(iconRes)

            // Show photo indicator if photo exists
            if (expense.receiptPhotoPath != null) {
                binding.ivReceiptIndicator.visibility = View.VISIBLE
            } else {
                binding.ivReceiptIndicator.visibility = View.GONE
            }

            // Click listeners
            binding.root.setOnClickListener {
                onItemClick(expense)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(expense)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}
