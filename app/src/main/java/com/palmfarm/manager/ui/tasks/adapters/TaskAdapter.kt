package com.palmfarm.manager.ui.tasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.data.database.entities.Task
import com.palmfarm.manager.databinding.ItemTaskBinding
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for tasks list
 */
class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskMenuClick: (Task, View) -> Unit
) : ListAdapter<Task, TaskAdapter.ViewHolder>(DiffCallback()) {

    private var workerMap: Map<Int, String> = emptyMap()

    /**
     * Update worker name mapping
     */
    fun updateWorkerMap(newWorkerMap: Map<Int, String>) {
        workerMap = newWorkerMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onTaskClick, onTaskMenuClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), workerMap)
    }

    class ViewHolder(
        private val binding: ItemTaskBinding,
        private val onTaskClick: (Task) -> Unit,
        private val onTaskMenuClick: (Task, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task, workerMap: Map<Int, String>) {
            binding.tvTaskCategory.text = task.category
            binding.chipTaskStatus.text = task.status
            binding.tvTaskDescription.text = task.description
            
            // Display worker name from map, fallback to ID if not found
            val workerName = workerMap[task.workerId] ?: "Worker #${task.workerId}"
            binding.tvTaskWorker.text = "Worker: $workerName"

            // Format dates
            val startDate = DateUtils.formatToDisplay(task.startDate)
            val endDate = DateUtils.formatToDisplay(task.endDate ?: System.currentTimeMillis())
            binding.tvTaskDates.text = "$startDate - $endDate"

            // Quantity display (Task entity only has 'quantity', not 'quantityCompleted')
            val quantityText = if (task.quantity != null && task.quantity > 0) {
                "${task.quantity.toInt()}"
            } else {
                "Not specified"
            }
            binding.tvTaskQuantity.text = quantityText

            // Calculate and display wage (quantity * payRate)
            val wage = (task.quantity ?: 0.0) * task.payRate
            binding.tvTaskWage.text = CurrencyUtils.formatAmount(wage)

            // Click listeners
            binding.root.setOnClickListener { onTaskClick(task) }
            binding.btnTaskMenu.setOnClickListener { onTaskMenuClick(task, it) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
