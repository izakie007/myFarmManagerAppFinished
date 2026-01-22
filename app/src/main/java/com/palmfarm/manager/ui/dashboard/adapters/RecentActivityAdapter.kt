package com.palmfarm.manager.ui.dashboard.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.ItemRecentActivityBinding
import com.palmfarm.manager.ui.dashboard.ActivityItem
import com.palmfarm.manager.ui.dashboard.ActivityType
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for recent activity list on Dashboard
 */
class RecentActivityAdapter : ListAdapter<ActivityItem, RecentActivityAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentActivityBinding.inflate(
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
        private val binding: ItemRecentActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ActivityItem) {
            binding.tvActivityTitle.text = item.title
            binding.tvActivityDescription.text = item.description
            binding.tvActivityDate.text = DateUtils.formatTimeAgo(item.date)

            // Set icon based on activity type
            val iconRes = when (item.type) {
                ActivityType.HARVEST -> R.drawable.ic_task
                ActivityType.MILLING -> R.drawable.ic_production
                ActivityType.TASK -> R.drawable.ic_task
                ActivityType.EXPENSE -> R.drawable.ic_finances
                ActivityType.SALE -> R.drawable.ic_finances
                ActivityType.LOAN_PAYMENT -> R.drawable.ic_finances
                ActivityType.WAGE_PAYMENT -> R.drawable.ic_task
                ActivityType.OTHER -> R.drawable.ic_dashboard
            }
            binding.ivActivityIcon.setImageResource(iconRes)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ActivityItem>() {
        override fun areItemsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            return oldItem.date == newItem.date && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            return oldItem == newItem
        }
    }
}
