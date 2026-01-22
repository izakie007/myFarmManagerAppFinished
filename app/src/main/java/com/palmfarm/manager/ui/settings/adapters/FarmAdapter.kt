package com.palmfarm.manager.ui.settings.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.data.database.entities.Farm
import com.palmfarm.manager.databinding.ItemFarmBinding

/**
 * Adapter for farm list
 */
class FarmAdapter(
    private val onEditClick: (Farm) -> Unit,
    private val onDeleteClick: (Farm) -> Unit
) : ListAdapter<Farm, FarmAdapter.FarmViewHolder>(FarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FarmViewHolder {
        val binding = ItemFarmBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FarmViewHolder(binding, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: FarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FarmViewHolder(
        private val binding: ItemFarmBinding,
        private val onEditClick: (Farm) -> Unit,
        private val onDeleteClick: (Farm) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(farm: Farm) {
            binding.tvFarmName.text = farm.name
            binding.tvLocation.text = farm.location

            val matureText = "${farm.productivePalms} productive"
            val immatureText = "${farm.unproductivePalms} unproductive"
            val totalText = "${farm.totalPalms} total palms"
            binding.tvPalmCount.text = "$matureText • $immatureText • $totalText"

            binding.btnEdit.setOnClickListener {
                onEditClick(farm)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(farm)
            }
        }
    }
}

/**
 * DiffUtil callback for efficient RecyclerView updates
 */
private class FarmDiffCallback : DiffUtil.ItemCallback<Farm>() {
    override fun areItemsTheSame(oldItem: Farm, newItem: Farm): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Farm, newItem: Farm): Boolean {
        return oldItem == newItem
    }
}
