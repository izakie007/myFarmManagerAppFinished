package com.palmfarm.manager.ui.production

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmfarm.manager.databinding.FragmentProductionBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.ui.production.adapters.HarvestAdapter
import com.palmfarm.manager.ui.production.adapters.LooseNutsAdapter
import com.palmfarm.manager.ui.production.adapters.MillingAdapter
import kotlinx.coroutines.launch

/**
 * Production fragment - tracks harvest, milling, and loose nuts picking
 */
class ProductionFragment : BaseFragment<FragmentProductionBinding>() {

    private val viewModel: ProductionViewModel by viewModels { ViewModelFactory.create() }

    private lateinit var harvestAdapter: HarvestAdapter
    private lateinit var millingAdapter: MillingAdapter
    private lateinit var looseNutsAdapter: LooseNutsAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProductionBinding {
        return FragmentProductionBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerViews()
        setupButtons()
    }

    override fun setupObservers() {
        observeMetrics()
        observeHarvests()
        observeMillings()
        observeLooseNuts()
        observeWorkers()
    }

    /**
     * Setup all RecyclerViews
     */
    private fun setupRecyclerViews() {
        // Harvest RecyclerView
        harvestAdapter = HarvestAdapter(
            onHarvestClick = { harvest ->
                showToast("Edit harvest #${harvest.harvestNumber}")
            }
        )
        binding.rvHarvests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = harvestAdapter
        }

        // Milling RecyclerView
        millingAdapter = MillingAdapter(
            onMillingClick = { milling ->
                showToast("Edit milling record")
            }
        )
        binding.rvMillings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = millingAdapter
        }

        // Loose Nuts RecyclerView
        looseNutsAdapter = LooseNutsAdapter(
            onLooseNutsClick = { looseNuts ->
                showToast("Edit loose nuts record")
            }
        )
        binding.rvLooseNuts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = looseNutsAdapter
        }
    }

    /**
     * Setup add buttons
     */
    private fun setupButtons() {
        binding.btnAddHarvest.setOnClickListener {
            val action = ProductionFragmentDirections.actionProductionToAddHarvest()
            findNavController().navigate(action)
        }

        binding.btnAddMilling.setOnClickListener {
            val action = ProductionFragmentDirections.actionProductionToAddMilling()
            findNavController().navigate(action)
        }

        binding.btnAddLooseNuts.setOnClickListener {
            val action = ProductionFragmentDirections.actionProductionToAddLooseNuts()
            findNavController().navigate(action)
        }
    }

    /**
     * Observe production metrics
     */
    private fun observeMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productionMetrics.collect { metrics ->
                binding.tvBunchesMilled.text = metrics.bunchesMilled.toString()
                binding.tvBunchesAvailable.text = metrics.bunchesAvailable.toString()
                binding.tvOilPerBunch.text = String.format("%.2f", metrics.oilPerBunch)
                binding.tvOilPerDrum.text = String.format("%.2f", metrics.oilPerDrum)
                binding.tvBunchesPerDrum.text = String.format("%.2f", metrics.bunchesPerDrum)
                binding.tvOilStock.text = String.format("%.1f gal", metrics.oilStock)
            }
        }
    }

    /**
     * Observe harvests
     */
    private fun observeHarvests() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.harvests.collect { harvests ->
                harvestAdapter.submitList(harvests)
            }
        }
    }

    /**
     * Observe millings
     */
    private fun observeMillings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.millings.collect { millings ->
                millingAdapter.submitList(millings)
            }
        }
    }

    /**
     * Observe loose nuts
     */
    private fun observeLooseNuts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.looseNuts.collect { looseNutsList ->
                looseNutsAdapter.submitList(looseNutsList)
            }
        }
    }

    /**
     * Observe workers to populate adapter worker maps
     */
    private fun observeWorkers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeWorkers.collect { workers ->
                val workerMap = workers.associate { it.id to it.fullName }
                harvestAdapter.updateWorkerMap(workerMap)
                millingAdapter.updateWorkerMap(workerMap)
                looseNutsAdapter.updateWorkerMap(workerMap)
            }
        }
    }
}
