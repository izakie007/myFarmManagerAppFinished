package com.palmfarm.manager.ui.finances.reports

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentCashFlowBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Cash Flow Report fragment with monthly breakdown
 */
class CashFlowFragment : BaseFragment<FragmentCashFlowBinding>() {

    private val viewModel: ReportsViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var adapter: CashFlowAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCashFlowBinding {
        return FragmentCashFlowBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerView()
        setupMenu()
    }

    override fun setupObservers() {
        observeCashFlowData()
    }

    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        adapter = CashFlowAdapter()

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@CashFlowFragment.adapter
        }
    }

    /**
     * Setup toolbar menu for period selection
     */
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_reports, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_select_period -> {
                        showPeriodDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Show period selection dialog
     */
    private fun showPeriodDialog() {
        val periods = arrayOf(
            getString(R.string.filter_current_cycle),
            getString(R.string.filter_month),
            getString(R.string.filter_quarter),
            getString(R.string.filter_year),
            getString(R.string.filter_all)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_period)
            .setItems(periods) { _, which ->
                val period = when (which) {
                    0 -> ReportPeriod.CURRENT_CYCLE
                    1 -> ReportPeriod.MONTH
                    2 -> ReportPeriod.QUARTER
                    3 -> ReportPeriod.YEAR
                    4 -> ReportPeriod.ALL_TIME
                    else -> ReportPeriod.CURRENT_CYCLE
                }
                viewModel.setReportPeriod(period)
            }
            .show()
    }

    /**
     * Observe cash flow data
     */
    private fun observeCashFlowData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getCashFlowData().collect { data ->
                // Update summary
                binding.tvTotalIncome.text = CurrencyUtils.formatAmount(data.totalIncome)
                binding.tvTotalExpenses.text = CurrencyUtils.formatAmount(data.totalExpenses)
                binding.tvNetCashFlow.text = CurrencyUtils.formatAmount(data.netCashFlow)

                // Set color for net cash flow
                binding.tvNetCashFlow.setTextColor(
                    if (data.netCashFlow >= 0) {
                        requireContext().getColor(R.color.green_500)
                    } else {
                        requireContext().getColor(R.color.md_theme_error)
                    }
                )

                // Update monthly data
                adapter.submitList(data.monthlyData)

                // Show empty state
                if (data.monthlyData.isEmpty()) {
                    binding.recyclerView.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
            }
        }
    }
}
