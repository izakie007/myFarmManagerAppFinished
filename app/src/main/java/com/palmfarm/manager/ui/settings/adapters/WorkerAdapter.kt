package com.palmfarm.manager.ui.settings.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.databinding.ItemWorkerBinding

/**
 * Adapter for worker list
 */
class WorkerAdapter(
    private val onEditClick: (Worker) -> Unit,
    private val onDeleteClick: (Worker) -> Unit
) : ListAdapter<Worker, WorkerAdapter.WorkerViewHolder>(WorkerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerViewHolder {
        val binding = ItemWorkerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorkerViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: WorkerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WorkerViewHolder(
        private val binding: ItemWorkerBinding,
        private val onEditClick: (Worker) -> Unit,
        private val onDeleteClick: (Worker) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(worker: Worker) {
            binding.tvWorkerName.text = worker.fullName
            binding.tvSpecialty.text = worker.specialty

            if (!worker.phoneNumber.isNullOrEmpty()) {
                binding.tvPhoneNumber.text = worker.phoneNumber
                binding.tvPhoneNumber.visibility = android.view.View.VISIBLE
            } else {
                binding.tvPhoneNumber.visibility = android.view.View.GONE
            }

            // Status indicator
            if (worker.isActive) {
                binding.tvStatus.text = "Active"
                binding.tvStatus.setBackgroundResource(com.palmfarm.manager.R.drawable.bg_status_active)
            } else {
                binding.tvStatus.text = "Inactive"
                binding.tvStatus.setBackgroundResource(com.palmfarm.manager.R.drawable.bg_status_inactive)
            }

            binding.btnEdit.setOnClickListener {
                onEditClick(worker)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(worker)
            }
        }
    }
}

/**
 * DiffUtil callback for efficient RecyclerView updates
 */
private class WorkerDiffCallback : DiffUtil.ItemCallback<Worker>() {
    override fun areItemsTheSame(oldItem: Worker, newItem: Worker): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Worker, newItem: Worker): Boolean {
        return oldItem == newItem
    }
}
