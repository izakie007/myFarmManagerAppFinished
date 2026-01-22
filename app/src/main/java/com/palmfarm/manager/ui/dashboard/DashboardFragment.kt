package com.palmfarm.manager.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentDashboardBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.ui.dashboard.adapters.CycleHistoryAdapter
import com.palmfarm.manager.ui.dashboard.adapters.RecentActivityAdapter
import com.palmfarm.manager.utils.CurrencyUtils
import com.palmfarm.manager.utils.DateUtils
import com.palmfarm.manager.utils.gone
import com.palmfarm.manager.utils.visible
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Dashboard fragment - shows production cycle progress, key metrics, and recent activity
 */
class DashboardFragment : BaseFragment<FragmentDashboardBinding>() {

    private val viewModel: DashboardViewModel by viewModels { ViewModelFactory.create() }

    private lateinit var recentActivityAdapter: RecentActivityAdapter
    private lateinit var cycleHistoryAdapter: CycleHistoryAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDashboardBinding {
        return FragmentDashboardBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerViews()
        setupQuickActions()
    }

    override fun setupObservers() {
        observeCurrentCycle()
        observeCurrentRealizedBunches()
        observeCycleProgress()
        observeKeyMetrics()
        observeRecentActivities()
        observeCycleHistory()
    }

    /**
     * Setup RecyclerViews for recent activity and cycle history
     */
    private fun setupRecyclerViews() {
        // Recent Activity RecyclerView
        recentActivityAdapter = RecentActivityAdapter()
        binding.rvRecentActivity.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentActivityAdapter
        }

        // Cycle History RecyclerView (horizontal)
        cycleHistoryAdapter = CycleHistoryAdapter()
        binding.rvCycleHistory.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = cycleHistoryAdapter
        }
    }

    /**
     * Setup quick action buttons
     */
    private fun setupQuickActions() {
        binding.btnTasks.setOnClickListener {
            findNavController().navigate(R.id.navigation_tasks)
        }

        binding.btnProduction.setOnClickListener {
            findNavController().navigate(R.id.navigation_production)
        }

        binding.btnFinances.setOnClickListener {
            findNavController().navigate(R.id.navigation_finances)
        }

        binding.btnAnalytics.setOnClickListener {
            findNavController().navigate(R.id.navigation_analytics)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
        }
    }

    /**
     * Observe current production cycle
     */
    private fun observeCurrentCycle() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentCycle.collect { cycle ->
                if (cycle != null) {
                    // Show cycle info
                    binding.cardCurrentCycle.visible()
                    binding.layoutEmptyState.gone()

                    binding.tvCycleName.text = cycle.cycleName
                    binding.tvCycleDates.text = getString(
                        R.string.date_range_format,
                        DateUtils.formatToDisplay(cycle.startDate),
                        DateUtils.formatToDisplay(cycle.endDate)
                    )
                    // Bunches progress moved to realized observer
                } else {
                    // Show empty state
                    binding.cardCurrentCycle.gone()
                    binding.layoutEmptyState.visible()
                }
            }
        }
    }

    /**
     * Observe current realized bunches
     */
    private fun observeCurrentRealizedBunches() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentCycle.combine(viewModel.currentRealizedBunches) { cycle, realized ->
                Pair(cycle, realized)
            }.collect { (cycle, realized) ->
                if (cycle != null) {
                    binding.tvBunchesProgress.text = getString(
                        R.string.bunches_progress_format,
                        realized,
                        cycle.expectedBunches
                    )
                }
            }
        }
    }

    /**
     * Observe cycle progress
     */
    private fun observeCycleProgress() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cycleProgress.collect { progress ->
                binding.progressCycle.progress = progress.toInt()
                binding.tvProgressPercentage.text = getString(
                    R.string.percentage_format,
                    progress.toInt()
                )
            }
        }
    }

    /**
     * Observe key metrics
     */
    private fun observeKeyMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.keyMetrics.collect { metrics ->
                // Bunches per tree
                binding.tvBunchesPerTree.text = String.format("%.1f", metrics.bunchesPerTree)

                // Oil per bunch
                binding.tvOilPerBunch.text = String.format("%.2f", metrics.oilPerBunch)

                // Cycle expense (abbreviated format)
                binding.tvCycleExpense.text = CurrencyUtils.formatAmountAbbreviated(metrics.cycleExpense)

                // Cycle income (abbreviated format)
                binding.tvCycleIncome.text = CurrencyUtils.formatAmountAbbreviated(metrics.cycleIncome)
            }
        }
    }

    /**
     * Observe recent activities
     */
    private fun observeRecentActivities() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentActivities.collect { activities ->
                recentActivityAdapter.submitList(activities)
            }
        }
    }

    /**
     * Observe cycle history
     */
    private fun observeCycleHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cycleHistory.collect { history ->
                cycleHistoryAdapter.submitList(history)
            }
        }
    }
}