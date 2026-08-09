package com.palmfarm.manager.ui.production.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Harvest
import com.palmfarm.manager.databinding.ItemHarvestBinding
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for harvest records
 */
class HarvestAdapter(
    private val onHarvestClick: (Harvest) -> Unit,
    private val onHarvestMenuClick: (Harvest, View) -> Unit
) : ListAdapter<Harvest, HarvestAdapter.ViewHolder>(DiffCallback()) {

    private var workerMap: Map<Int, String> = emptyMap()

    /**
     * Update worker name mapping
     */
    fun updateWorkerMap(newWorkerMap: Map<Int, String>) {
        if (workerMap == newWorkerMap) return
        workerMap = newWorkerMap
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHarvestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onHarvestClick, onHarvestMenuClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), workerMap)
    }

    class ViewHolder(
        private val binding: ItemHarvestBinding,
        private val onHarvestClick: (Harvest) -> Unit,
        private val onHarvestMenuClick: (Harvest, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(harvest: Harvest, workerMap: Map<Int, String>) {
            val context = binding.root.context
            binding.tvHarvestNumber.text = context.getString(
                R.string.harvest_number_format,
                harvest.harvestNumber
            )

            val workerName = workerMap[harvest.harvesterId]
                ?: context.getString(R.string.worker_fallback_format, harvest.harvesterId)
            binding.tvHarvestWorker.text = context.getString(R.string.worker_name_format, workerName)

            binding.tvHarvestDate.text = DateUtils.formatToDisplay(harvest.date)
            binding.tvHarvestBunches.text = harvest.numberOfBunches.toString()

            binding.root.setOnClickListener { onHarvestClick(harvest) }
            binding.btnHarvestMenu.setOnClickListener { onHarvestMenuClick(harvest, it) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Harvest>() {
        override fun areItemsTheSame(oldItem: Harvest, newItem: Harvest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Harvest, newItem: Harvest): Boolean {
            return oldItem == newItem
        }
    }
}
