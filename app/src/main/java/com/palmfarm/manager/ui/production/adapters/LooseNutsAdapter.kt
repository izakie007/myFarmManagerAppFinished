package com.palmfarm.manager.ui.production.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.LooseNutsPicking
import com.palmfarm.manager.databinding.ItemLooseNutsBinding
import com.palmfarm.manager.utils.DateUtils

/**
 * Adapter for loose nuts records
 */
class LooseNutsAdapter(
    private val onLooseNutsClick: (LooseNutsPicking) -> Unit,
    private val onLooseNutsMenuClick: (LooseNutsPicking, View) -> Unit
) : ListAdapter<LooseNutsPicking, LooseNutsAdapter.ViewHolder>(DiffCallback()) {

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
        val binding = ItemLooseNutsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onLooseNutsClick, onLooseNutsMenuClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), workerMap)
    }

    class ViewHolder(
        private val binding: ItemLooseNutsBinding,
        private val onLooseNutsClick: (LooseNutsPicking) -> Unit,
        private val onLooseNutsMenuClick: (LooseNutsPicking, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(looseNuts: LooseNutsPicking, workerMap: Map<Int, String>) {
            val context = binding.root.context
            val workerName = workerMap[looseNuts.pickerId]
                ?: context.getString(R.string.worker_fallback_format, looseNuts.pickerId)
            binding.tvLooseNutsWorker.text = context.getString(R.string.worker_name_format, workerName)

            binding.tvLooseNutsDate.text = DateUtils.formatToDisplay(looseNuts.date)
            val bagsText = if (looseNuts.numberOfBags % 1.0 == 0.0) {
                looseNuts.numberOfBags.toInt().toString()
            } else {
                looseNuts.numberOfBags.toString()
            }
            binding.tvLooseNutsBags.text = context.getString(R.string.loose_nuts_bags_format, bagsText)

            binding.root.setOnClickListener { onLooseNutsClick(looseNuts) }
            binding.btnLooseNutsMenu.setOnClickListener { onLooseNutsMenuClick(looseNuts, it) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<LooseNutsPicking>() {
        override fun areItemsTheSame(oldItem: LooseNutsPicking, newItem: LooseNutsPicking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LooseNutsPicking, newItem: LooseNutsPicking): Boolean {
            return oldItem == newItem
        }
    }
}
