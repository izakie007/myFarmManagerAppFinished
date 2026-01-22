package com.palmfarm.manager.ui.dashboard

import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.entities.ProductionCycle
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import com.palmfarm.manager.data.repository.ProductionRepository
import com.palmfarm.manager.data.repository.TaskRepository
import com.palmfarm.manager.data.database.dao.FarmDao
import com.palmfarm.manager.data.database.dao.ProductionCycleDao
import com.palmfarm.manager.ui.common.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Dashboard screen
 */
class DashboardViewModel(
    private val cycleRepository: ProductionCycleRepository,
    private val productionRepository: ProductionRepository,
    private val taskRepository: TaskRepository,
    private val farmDao: FarmDao,
    private val productionCycleDao: ProductionCycleDao  // Added dependency
) : BaseViewModel() {

    private val currentCycleState: StateFlow<ProductionCycle?> = cycleRepository.getCurrentCycle()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentCycle: StateFlow<ProductionCycle?> = currentCycleState

    val currentRealizedBunches: StateFlow<Long> = currentCycleState
        .filterNotNull()
        .flatMapLatest { cycle ->
            productionCycleDao.getRealizedBunchesForCycle(cycle.id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val cycleProgress: StateFlow<Double> = combine(currentCycleState, currentRealizedBunches) { cycle, realized ->
        if (cycle != null && cycle.expectedBunches > 0) {
            (realized.toDouble() / cycle.expectedBunches.toDouble()).coerceIn(0.0, 1.0) * 100.0
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val keyMetrics: StateFlow<KeyMetrics> = currentCycleState
        .filterNotNull()
        .flatMapLatest { cycle ->
            combine(
                productionCycleDao.getRealizedBunchesForCycle(cycle.id),
                farmDao.getTotalPalmsFlow(),
                productionRepository.oilPerBunchFlow(cycle.id),
                cycleRepository.getCycleExpensesFlow(cycle.id),
                cycleRepository.getCycleIncomeFlow(cycle.id)
            ) { realized, totalPalms, oilPerBunch, expenses, income ->
                val palms = totalPalms.takeIf { it > 0 } ?: 0
                val bunchesPerTree = if (palms > 0) realized.toDouble() / palms.toDouble() else 0.0
                KeyMetrics(
                    bunchesPerTree = bunchesPerTree,
                    oilPerBunch = oilPerBunch,
                    cycleExpense = expenses,
                    cycleIncome = income
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, KeyMetrics())

    val recentActivities: StateFlow<List<ActivityItem>> = combine(
        productionRepository.getRecentProductionActivitiesFlow(5),
        taskRepository.getRecentTasks(5)
    ) { productionActivities, tasks ->
        val activityItems = productionActivities.map { activity ->
            val type = when (activity.type) {
                "Harvest" -> ActivityType.HARVEST
                "Milling" -> ActivityType.MILLING
                else -> ActivityType.OTHER
            }
            ActivityItem(
                type = type,
                title = activity.type,
                description = activity.description,
                date = activity.date
            )
        } + tasks.map { task ->
            ActivityItem(
                type = ActivityType.TASK,
                title = "Task: ${task.category}",
                description = "${task.description} - ${task.status}",
                date = task.startDate
            )
        }

        activityItems.sortedByDescending { it.date }.take(5)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val cycleHistory: StateFlow<List<CycleHistorySummary>> = cycleRepository.getAllCycles()
        .map { cycles -> cycles.sortedByDescending { it.startDate }.take(3) }
        .flatMapLatest { cycles ->
            combineFlows(
                cycles.map { cycle ->
                    combine(
                        productionCycleDao.getRealizedBunchesForCycle(cycle.id),
                        cycleRepository.getCycleExpensesFlow(cycle.id),
                        cycleRepository.getCycleIncomeFlow(cycle.id)
                    ) { realized, expenses, income ->
                        CycleHistorySummary(
                            cycleId = cycle.id,
                            cycleName = cycle.cycleName,
                            realizedBunches = realized.toInt(),
                            expectedBunches = cycle.expectedBunches,
                            totalExpenses = expenses,
                            totalIncome = income,
                            profit = income - expenses
                        )
                    }
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun <T> combineFlows(flows: List<Flow<T>>): Flow<List<T>> {
        return when {
            flows.isEmpty() -> flowOf(emptyList())
            flows.size == 1 -> flows.first().map { listOf(it) }
            else -> {
                val initial = flows.first().map { listOf(it) }
                flows.drop(1).fold(initial) { acc, flow ->
                    acc.combine(flow) { list, value -> list + value }
                }
            }
        }
    }

    fun refresh() {
        // Streams are already hot; method retained for UI compatibility
    }

    /**
     * Navigate to specific section
     */
    fun navigateToTasks() {
        // Navigation handled by fragment
    }

    fun navigateToProduction() {
        // Navigation handled by fragment
    }

    fun navigateToFinances() {
        // Navigation handled by fragment
    }

    fun navigateToSettings() {
        // Navigation handled by fragment
    }

    fun navigateToAnalytics() {
        // Navigation handled by fragment
    }
}