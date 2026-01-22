package com.palmfarm.manager.ui.finances.loans

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.data.database.entities.Loan
import com.palmfarm.manager.databinding.ItemLoanBinding
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for displaying loans in RecyclerView
 */
class LoanAdapter(
    private val onItemClick: (Loan) -> Unit
) : ListAdapter<Loan, LoanAdapter.LoanViewHolder>(LoanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoanViewHolder {
        val binding = ItemLoanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LoanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LoanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LoanViewHolder(
        private val binding: ItemLoanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(loan: Loan) {
            binding.tvLender.text = loan.lenderName
            binding.tvPrincipal.text = CurrencyUtils.formatAmount(loan.principal)
            binding.tvInterestRate.text = "${loan.interestRate}%"
            binding.tvStartDate.text = DateUtils.formatToDisplay(loan.startDate)

            // Payment info
            binding.tvMonthlyPayment.text = CurrencyUtils.formatAmount(loan.monthlyPayment)
            binding.tvTotalLeft.text = CurrencyUtils.formatAmount(loan.totalLeft)
            binding.tvPaymentsMade.text = "${loan.numberOfPaymentsMade} / ${loan.periodMonths}"

            // Progress bar
            val progress = if (loan.periodMonths > 0) {
                (loan.numberOfPaymentsMade.toFloat() / loan.periodMonths) * 100
            } else {
                0f
            }
            binding.progressPayments.progress = progress.toInt()

            // Purpose if available
            if (loan.purpose.isNullOrEmpty()) {
                binding.tvPurpose.visibility = android.view.View.GONE
            } else {
                binding.tvPurpose.visibility = android.view.View.VISIBLE
                binding.tvPurpose.text = loan.purpose
            }

            // Fully paid indicator
            if (loan.isFullyPaid) {
                binding.tvFullyPaid.visibility = android.view.View.VISIBLE
                binding.root.alpha = 0.7f
            } else {
                binding.tvFullyPaid.visibility = android.view.View.GONE
                binding.root.alpha = 1.0f
            }

            // Click listener
            binding.root.setOnClickListener {
                onItemClick(loan)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class LoanDiffCallback : DiffUtil.ItemCallback<Loan>() {
        override fun areItemsTheSame(oldItem: Loan, newItem: Loan): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Loan, newItem: Loan): Boolean {
            return oldItem == newItem
        }
    }
}
