package com.palmfarm.manager.ui.production

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Harvest
import com.palmfarm.manager.data.database.entities.LooseNutsPicking
import com.palmfarm.manager.data.database.entities.Milling
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
        harvestAdapter = HarvestAdapter(
            onHarvestClick = { harvest ->
                navigateToEditHarvest(harvest.id)
            },
            onHarvestMenuClick = { harvest, view ->
                showHarvestMenu(harvest, view)
            }
        )
        binding.rvHarvests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = harvestAdapter
        }

        millingAdapter = MillingAdapter(
            onMillingClick = { milling ->
                navigateToEditMilling(milling.id)
            },
            onMillingMenuClick = { milling, view ->
                showMillingMenu(milling, view)
            }
        )
        binding.rvMillings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = millingAdapter
        }

        looseNutsAdapter = LooseNutsAdapter(
            onLooseNutsClick = { looseNuts ->
                navigateToEditLooseNuts(looseNuts.id)
            },
            onLooseNutsMenuClick = { looseNuts, view ->
                showLooseNutsMenu(looseNuts, view)
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

    private fun navigateToEditHarvest(harvestId: Int) {
        val action = ProductionFragmentDirections.actionProductionToAddHarvest(harvestId = harvestId)
        findNavController().navigate(action)
    }

    private fun navigateToEditMilling(millingId: Int) {
        val action = ProductionFragmentDirections.actionProductionToAddMilling(millingId = millingId)
        findNavController().navigate(action)
    }

    private fun navigateToEditLooseNuts(looseNutsId: Int) {
        val action = ProductionFragmentDirections.actionProductionToAddLooseNuts(looseNutsId = looseNutsId)
        findNavController().navigate(action)
    }

    private fun showHarvestMenu(harvest: Harvest, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_task_item, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    navigateToEditHarvest(harvest.id)
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteHarvest(harvest)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showMillingMenu(milling: Milling, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_task_item, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    navigateToEditMilling(milling.id)
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteMilling(milling)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showLooseNutsMenu(looseNuts: LooseNutsPicking, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_task_item, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    navigateToEditLooseNuts(looseNuts.id)
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteLooseNuts(looseNuts)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDeleteHarvest(harvest: Harvest) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_harvest)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deleteHarvest(harvest)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun confirmDeleteMilling(milling: Milling) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_milling)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deleteMilling(milling)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun confirmDeleteLooseNuts(looseNuts: LooseNutsPicking) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_loose_nuts)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deleteLooseNuts(looseNuts)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    /**
     * Observe production metrics
     */
    private fun observeMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productionMetrics.collect { metrics ->
                binding.tvBunchesMilled.text = metrics.bunchesMilled.toString()
                binding.tvBunchesAvailable.text = metrics.bunchesAvailable.toString()
                binding.tvOilPerBunch.text = getString(R.string.decimal_two_places_format, metrics.oilPerBunch)
                binding.tvOilPerDrum.text = getString(R.string.decimal_two_places_format, metrics.oilPerDrum)
                binding.tvBunchesPerDrum.text = getString(R.string.decimal_two_places_format, metrics.bunchesPerDrum)
                binding.tvOilStock.text = getString(R.string.oil_stock_gallons_format, metrics.oilStock)
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
