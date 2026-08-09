package com.palmfarm.manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palmfarm.manager.PalmFarmApp
import com.palmfarm.manager.data.repository.*
import com.palmfarm.manager.domain.usecases.CalculateWageUseCase
import com.palmfarm.manager.domain.usecases.BackupRestoreUseCase
import com.palmfarm.manager.ui.dashboard.DashboardViewModel
import com.palmfarm.manager.ui.finances.FinancesViewModel
import com.palmfarm.manager.ui.finances.cash.CashTransactionViewModel
import com.palmfarm.manager.ui.finances.expenses.ExpensesViewModel
import com.palmfarm.manager.ui.finances.fixedcosts.FixedCostsViewModel
import com.palmfarm.manager.ui.finances.income.IncomeViewModel
import com.palmfarm.manager.ui.finances.loans.LoansViewModel
import com.palmfarm.manager.ui.finances.reports.ReportsViewModel
import com.palmfarm.manager.ui.finances.wages.WagesViewModel
import com.palmfarm.manager.ui.production.ProductionViewModel
import com.palmfarm.manager.ui.tasks.TasksViewModel
import com.palmfarm.manager.ui.settings.SettingsViewModel
import com.palmfarm.manager.ui.analytics.AnalyticsViewModel

/**
 * Factory for creating ViewModels with repository dependencies
 */
class ViewModelFactory(private val app: PalmFarmApp) : ViewModelProvider.Factory {

    // Lazy initialize repositories
    val productionCycleRepository by lazy {
        ProductionCycleRepository(
            productionCycleDao = app.database.productionCycleDao(),
            harvestDao = app.database.harvestDao(),
            expenseDao = app.database.expenseDao(),
            saleDao = app.database.saleDao(),
            wagePaymentDao = app.database.wagePaymentDao(),
            advancePaymentDao = app.database.advancePaymentDao(),
            loanDao = app.database.loanDao()
        )
    }

    private val taskRepository by lazy {
        TaskRepository(
            taskDao = app.database.taskDao()
        )
    }

    private val productionRepository by lazy {
        ProductionRepository(
            harvestDao = app.database.harvestDao(),
            millingDao = app.database.millingDao(),
            looseNutsDao = app.database.looseNutsPickingDao(),
            saleDao = app.database.saleDao(),
            consumptionDao = app.database.consumptionDao(),
            productionCycleDao = app.database.productionCycleDao(),
            appSettingsDao = app.database.appSettingsDao()
        )
    }

    private val workerRepository by lazy {
        WorkerRepository(
            workerDao = app.database.workerDao()
        )
    }

    // Lazy initialize use cases
    private val calculateWageUseCase by lazy {
        CalculateWageUseCase(
            taskDao = app.database.taskDao(),
            advancePaymentDao = app.database.advancePaymentDao()
        )
    }

    private val backupRestoreUseCase by lazy {
        BackupRestoreUseCase(
            context = app.applicationContext,
            database = app.database
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    productionCycleDao = app.database.productionCycleDao(),
                    cycleRepository = productionCycleRepository,
                    productionRepository = productionRepository,
                    taskRepository = taskRepository,
                    farmDao = app.database.farmDao()
                ) as T
            }
            modelClass.isAssignableFrom(TasksViewModel::class.java) -> {
                TasksViewModel(
                    taskRepository = taskRepository,
                    workerRepository = workerRepository,
                    cycleRepository = productionCycleRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProductionViewModel::class.java) -> {
                ProductionViewModel(
                    productionRepository = productionRepository,
                    cycleRepository = productionCycleRepository,
                    workerRepository = workerRepository
                ) as T
            }
            modelClass.isAssignableFrom(FinancesViewModel::class.java) -> {
                FinancesViewModel(
                    expenseDao = app.database.expenseDao(),
                    fixedCostDao = app.database.fixedCostDao(),
                    wagePaymentDao = app.database.wagePaymentDao(),
                    advancePaymentDao = app.database.advancePaymentDao(),
                    saleDao = app.database.saleDao(),
                    consumptionDao = app.database.consumptionDao(),
                    loanDao = app.database.loanDao(),
                    taskDao = app.database.taskDao(),
                    cycleRepository = productionCycleRepository,
                    productionRepository = productionRepository
                ) as T
            }
            modelClass.isAssignableFrom(ExpensesViewModel::class.java) -> {
                ExpensesViewModel(
                    expenseDao = app.database.expenseDao(),
                    cycleRepository = productionCycleRepository
                ) as T
            }
            modelClass.isAssignableFrom(FixedCostsViewModel::class.java) -> {
                FixedCostsViewModel(
                    fixedCostDao = app.database.fixedCostDao()
                ) as T
            }
            modelClass.isAssignableFrom(WagesViewModel::class.java) -> {
                WagesViewModel(
                    wagePaymentDao = app.database.wagePaymentDao(),
                    advancePaymentDao = app.database.advancePaymentDao(),
                    taskDao = app.database.taskDao(),
                    workerDao = app.database.workerDao(),
                    appSettingsDao = app.database.appSettingsDao(),
                    cycleRepository = productionCycleRepository,
                    calculateWageUseCase = calculateWageUseCase
                ) as T
            }
            modelClass.isAssignableFrom(IncomeViewModel::class.java) -> {
                IncomeViewModel(
                    saleDao = app.database.saleDao(),
                    consumptionDao = app.database.consumptionDao(),
                    productionRepository = productionRepository,
                    cycleRepository = productionCycleRepository
                ) as T
            }
            modelClass.isAssignableFrom(LoansViewModel::class.java) -> {
                LoansViewModel(
                    loanDao = app.database.loanDao()
                ) as T
            }
            modelClass.isAssignableFrom(ReportsViewModel::class.java) -> {
                ReportsViewModel(
                    expenseDao = app.database.expenseDao(),
                    fixedCostDao = app.database.fixedCostDao(),
                    wagePaymentDao = app.database.wagePaymentDao(),
                    advancePaymentDao = app.database.advancePaymentDao(),
                    saleDao = app.database.saleDao(),
                    consumptionDao = app.database.consumptionDao(),
                    loanDao = app.database.loanDao(),
                    taskDao = app.database.taskDao(),
                    cashTransactionDao = app.database.cashTransactionDao(),
                    appSettingsDao = app.database.appSettingsDao(),
                    cycleRepository = productionCycleRepository,
                    productionRepository = productionRepository
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    appSettingsDao = app.database.appSettingsDao(),
                    farmDao = app.database.farmDao(),
                    workerDao = app.database.workerDao(),
                    backupRestoreUseCase = backupRestoreUseCase
                ) as T
            }
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
                AnalyticsViewModel(
                    cycleRepository = productionCycleRepository,
                    productionRepository = productionRepository,
                    harvestDao = app.database.harvestDao(),
                    millingDao = app.database.millingDao(),
                    farmDao = app.database.farmDao(),
                    expenseDao = app.database.expenseDao(),
                    wagePaymentDao = app.database.wagePaymentDao(),
                    advancePaymentDao = app.database.advancePaymentDao(),
                    saleDao = app.database.saleDao(),
                    consumptionDao = app.database.consumptionDao(),
                    fixedCostDao = app.database.fixedCostDao()
                ) as T
            }
            modelClass.isAssignableFrom(CashTransactionViewModel::class.java) -> {
                CashTransactionViewModel(
                    cashTransactionDao = app.database.cashTransactionDao()
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        /**
         * Create ViewModelFactory with application context
         */
        fun create(): ViewModelFactory {
            return ViewModelFactory(PalmFarmApp.getInstance())
        }
    }
}
