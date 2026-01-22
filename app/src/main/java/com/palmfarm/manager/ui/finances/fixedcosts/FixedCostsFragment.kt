package com.palmfarm.manager.ui.finances.fixedcosts

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentFixedCostsBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Fragment for managing fixed costs with depreciation
 */
class FixedCostsFragment : BaseFragment<FragmentFixedCostsBinding>() {

    private val viewModel: FixedCostsViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var adapter: FixedCostAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFixedCostsBinding {
        return FragmentFixedCostsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupFab()
        setupMenu()
        setupFilterChips()
    }

    override fun setupObservers() {
        observeFixedCosts()
        observeTotals()
    }

    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        adapter = FixedCostAdapter(
            onItemClick = { fixedCostWithDepreciation ->
                // Navigate to edit
                val bundle = android.os.Bundle().apply {
                    putInt("fixedCostId", fixedCostWithDepreciation.fixedCost.id)
                }
                findNavController().navigate(R.id.action_fixedCosts_to_addEdit, bundle)
            },
            onDeleteClick = { fixedCostWithDepreciation ->
                showDeleteConfirmation(fixedCostWithDepreciation.fixedCost.id)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@FixedCostsFragment.adapter
        }
    }

    /**
     * Setup FAB for adding fixed costs
     */
    private fun setupFab() {
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_fixedCosts_to_addEdit)
        }
    }

    /**
     * Setup toolbar menu
     */
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // No additional menu items needed
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Setup filter chips for types
     */
    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            viewModel.setFilterType(null)
            updateChipSelection(binding.chipAll.id)
        }

        binding.chipLand.setOnClickListener {
            viewModel.setFilterType("Land")
            updateChipSelection(binding.chipLand.id)
        }

        binding.chipEquipment.setOnClickListener {
            viewModel.setFilterType("Equipment")
            updateChipSelection(binding.chipEquipment.id)
        }

        // Default: All selected
        binding.chipAll.isChecked = true
    }

    /**
     * Update chip selection
     */
    private fun updateChipSelection(selectedChipId: Int) {
        binding.chipAll.isChecked = selectedChipId == binding.chipAll.id
        binding.chipLand.isChecked = selectedChipId == binding.chipLand.id
        binding.chipEquipment.isChecked = selectedChipId == binding.chipEquipment.id
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmation(fixedCostId: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_fixed_cost)
            .setMessage(R.string.confirm_delete_title)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteFixedCost(fixedCostId)
                showToast(getString(R.string.success_deleted))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Observe fixed costs list
     */
    private fun observeFixedCosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fixedCostsWithDepreciation.collect { fixedCosts ->
                adapter.submitList(fixedCosts)

                // Show empty state
                if (fixedCosts.isEmpty()) {
                    binding.recyclerView.visibility = android.view.View.GONE
                    binding.tvEmptyState.visibility = android.view.View.VISIBLE
                } else {
                    binding.recyclerView.visibility = android.view.View.VISIBLE
                    binding.tvEmptyState.visibility = android.view.View.GONE
                }
            }
        }
    }

    /**
     * Observe totals
     */
    private fun observeTotals() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalValueLeft.collect { total ->
                binding.tvTotalValueLeft.text = CurrencyUtils.formatAmount(total)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalDepreciation.collect { total ->
                binding.tvTotalDepreciation.text = CurrencyUtils.formatAmount(total)
            }
        }
    }
}
