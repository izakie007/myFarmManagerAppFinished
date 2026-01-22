package com.palmfarm.manager.ui.production.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.data.database.entities.Milling
import com.palmfarm.manager.databinding.ItemMillingBinding
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for milling records
 */
class MillingAdapter(
    private val onMillingClick: (Milling) -> Unit
) : ListAdapter<Milling, MillingAdapter.ViewHolder>(DiffCallback()) {

    private var workerMap: Map<Int, String> = emptyMap()

    /**
     * Update worker name mapping
     */
    fun updateWorkerMap(newWorkerMap: Map<Int, String>) {
        workerMap = newWorkerMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMillingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onMillingClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), workerMap)
    }

    class ViewHolder(
        private val binding: ItemMillingBinding,
        private val onMillingClick: (Milling) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(milling: Milling, workerMap: Map<Int, String>) {
            binding.tvMillingDate.text = DateUtils.formatToDisplay(milling.date)
            
            // Display worker name from map, fallback to ID if not found
            val workerName = workerMap[milling.millerId] ?: "Worker #${milling.millerId}"
            binding.tvMillingWorker.text = "Worker: $workerName"
            
            binding.tvMillingOil.text = String.format("%.1f gal", milling.oilProducedGallons)
            binding.tvMillingBunches.text = "${milling.bunchesMilled} bunches"
            binding.tvMillingDrums.text = "${milling.drumsCooked} drums"

            binding.root.setOnClickListener { onMillingClick(milling) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Milling>() {
        override fun areItemsTheSame(oldItem: Milling, newItem: Milling): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Milling, newItem: Milling): Boolean {
            return oldItem == newItem
        }
    }
}
