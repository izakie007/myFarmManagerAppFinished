package com.palmfarm.manager.ui.finances.income

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.entities.Consumption
import com.palmfarm.manager.data.database.entities.Sale
import com.palmfarm.manager.databinding.FragmentIncomeBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Fragment for managing income (sales and consumption)
 */
class IncomeFragment : BaseFragment<FragmentIncomeBinding>() {

    private val viewModel: IncomeViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var saleAdapter: SaleAdapter
    private lateinit var consumptionAdapter: ConsumptionAdapter

    private var currentTab = 0 // 0 = Sales, 1 = Consumption

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentIncomeBinding {
        return FragmentIncomeBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupTabs()
        setupRecyclerView()
        setupFabs()
    }

    override fun setupObservers() {
        observeMetrics()
        observeSales()
        observeConsumption()
    }

    /**
     * Setup tabs for Sales/Consumption
     */
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.sales_tab))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.consumption_tab))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateVisibility()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    /**
     * Setup RecyclerView for both sales and consumption
     */
    private fun setupRecyclerView() {
        // Sales adapter
        saleAdapter = SaleAdapter(
            onDeleteClick = { sale ->
                showDeleteConfirmation(sale)
            }
        )

        // Consumption adapter
        consumptionAdapter = ConsumptionAdapter(
            onDeleteClick = { consumption ->
                showDeleteConfirmation(consumption)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        updateVisibility()
    }

    /**
     * Update visibility based on current tab
     */
    private fun updateVisibility() {
        if (currentTab == 0) {
            // Sales tab
            binding.recyclerView.adapter = saleAdapter
            binding.fabAddSale.show()
            binding.fabAddConsumption.hide()
        } else {
            // Consumption tab
            binding.recyclerView.adapter = consumptionAdapter
            binding.fabAddSale.hide()
            binding.fabAddConsumption.show()
        }
    }

    /**
     * Setup FABs
     */
    private fun setupFabs() {
        binding.fabAddSale.setOnClickListener {
            findNavController().navigate(R.id.action_income_to_addSale)
        }

        binding.fabAddConsumption.setOnClickListener {
            findNavController().navigate(R.id.action_income_to_addConsumption)
        }
    }

    /**
     * Show delete confirmation for sale
     */
    private fun showDeleteConfirmation(sale: Sale) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage("Delete this sale?")
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteSale(sale.id)
                showToast(getString(R.string.success_deleted))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Show delete confirmation for consumption
     */
    private fun showDeleteConfirmation(consumption: Consumption) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage("Delete this consumption record?")
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteConsumption(consumption.id)
                showToast(getString(R.string.success_deleted))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Observe income metrics
     */
    private fun observeMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.incomeMetrics.collect { metrics ->
                binding.tvTotalSales.text = CurrencyUtils.formatAmount(metrics.totalSales)
                binding.tvTotalConsumption.text = CurrencyUtils.formatAmount(metrics.totalConsumption)
                binding.tvTotalIncome.text = CurrencyUtils.formatAmount(metrics.totalIncome)
                binding.tvOilStock.text = String.format("%.1f gal", metrics.oilStock)
                binding.tvBunchesAvailable.text = "${metrics.bunchesAvailable} bunches"
                binding.tvLastSalesPrice.text = CurrencyUtils.formatAmount(metrics.lastSalesPrice)
            }
        }
    }

    /**
     * Observe sales
     */
    private fun observeSales() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sales.collect { sales ->
                saleAdapter.submitList(sales)
            }
        }
    }

    /**
     * Observe consumption
     */
    private fun observeConsumption() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.consumption.collect { consumption ->
                consumptionAdapter.submitList(consumption)
            }
        }
    }
}
