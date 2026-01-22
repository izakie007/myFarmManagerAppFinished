package com.palmfarm.manager.ui.finances.wages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.R
import com.palmfarm.manager.data.database.dao.AdvancePaymentDao
import com.palmfarm.manager.data.database.dao.AppSettingsDao
import com.palmfarm.manager.data.database.dao.TaskDao
import com.palmfarm.manager.data.database.dao.WagePaymentDao
import com.palmfarm.manager.data.database.dao.WorkerDao
import com.palmfarm.manager.data.database.entities.AdvancePayment
import com.palmfarm.manager.data.database.entities.Task
import com.palmfarm.manager.data.database.entities.WagePayment
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import com.palmfarm.manager.domain.usecases.AdvanceValidationResult
import com.palmfarm.manager.domain.usecases.CalculateWageUseCase
import com.palmfarm.manager.utils.Constants
import com.palmfarm.manager.utils.PdfGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

class WagesViewModel(
    private val wagePaymentDao: WagePaymentDao,
    private val advancePaymentDao: AdvancePaymentDao,
    private val taskDao: TaskDao,
    private val workerDao: WorkerDao,
    private val appSettingsDao: AppSettingsDao,
    private val cycleRepository: ProductionCycleRepository,
    private val calculateWageUseCase: CalculateWageUseCase
) : ViewModel() {

    data class WorkerWageSummary(
        val worker: Worker,
        val cycleId: Int,
        val completedTasks: List<Task>,
        val inProgressTasks: List<Task>,
        val outstandingAdvances: Double
    ) {
        val completedGross: Double = completedTasks.sumOf { (it.quantity ?: 0.0) * it.payRate }
        val estimatedAdvanceApplication: Double = min(outstandingAdvances, completedGross)
        val estimatedNet: Double = completedGross - estimatedAdvanceApplication
        val hasPayableWork: Boolean = completedTasks.isNotEmpty()
        val hasAnyTasks: Boolean = completedTasks.isNotEmpty() || inProgressTasks.isNotEmpty()
    }

    data class ProcessedWageSummary(
        val wagePayment: WagePayment,
        val worker: Worker,
        val paidTasks: List<Task>
    )

    data class WageSummary(
        val pendingGross: Double = 0.0,
        val pendingNet: Double = 0.0,
        val processedNet: Double = 0.0,
        val inProgressTaskCount: Int = 0
    )

    data class ProcessAllResult(
        val processed: Int,
        val failed: Int,
        val errors: List<String>
    )

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val currentCycleIdFlow: StateFlow<Int> = cycleRepository.getCurrentCycle()
        .map { it?.id ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val activeWorkers: StateFlow<List<Worker>> = workerDao.getAllWorkers()
        .map { workers -> workers.filter { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun addAdvancePayment(
        workerId: Int,
        amount: Double,
        date: Long,
        reason: String
    ): Result<Unit> {
        return runCatchingWithLoading {
            val cycleId = requireActiveCycle()
            val validation = calculateWageUseCase.validateAdvancePayment(workerId, cycleId, amount)
            if (!validation.isValid) {
                throw IllegalStateException(validation.message)
            }

            val advance = AdvancePayment(
                workerId = workerId,
                cycleId = cycleId,
                amount = amount,
                purpose = reason.ifBlank { null },
                date = date
            )
            advancePaymentDao.insert(advance)
            Unit
        }
    }

    suspend fun getAdvanceLimit(workerId: Int): AdvanceValidationResult {





        val cycleId = requireActiveCycle()
        return calculateWageUseCase.validateAdvancePayment(workerId, cycleId, 0.0)
    }

    val unpaidSummaries: StateFlow<List<WorkerWageSummary>> = currentCycleIdFlow
        .flatMapLatest { cycleId ->
            if (cycleId <= 0) {
                flowOf(emptyList())
            } else {
                combine(
                    workerDao.getAllWorkers(),
                    taskDao.getTasksByCycle(cycleId),
                    wagePaymentDao.getWagePaymentsByCycle(cycleId),
                    advancePaymentDao.getAdvancePaymentsByCycle(cycleId)
                ) { workers, tasks, payments, advances ->
                    buildUnpaidSummaries(cycleId, workers, tasks, payments, advances)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val processedWages: StateFlow<List<ProcessedWageSummary>> = currentCycleIdFlow
        .flatMapLatest { cycleId ->
            if (cycleId <= 0) {
                flowOf(emptyList())
            } else {
                combine(
                    wagePaymentDao.getWagePaymentsByCycle(cycleId),
                    workerDao.getAllWorkers(),
                    taskDao.getTasksByCycle(cycleId)
                ) { payments, workers, tasks ->
                    buildProcessedSummaries(payments, workers, tasks)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val wageSummary: StateFlow<WageSummary> = combine(
        unpaidSummaries,
        processedWages,
        advancePaymentDao.getTotalAdvances()
    ) { unpaid, processed, totalAdvances ->
        val netPaid = processed.sumOf { it.wagePayment.netPayment }
        WageSummary(
            pendingGross = unpaid.sumOf { it.completedGross },
            pendingNet = unpaid.sumOf { it.estimatedNet },
            processedNet = netPaid + totalAdvances,
            inProgressTaskCount = unpaid.sumOf { it.inProgressTasks.size }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WageSummary())

    fun observeWorkerSummary(workerId: Int): Flow<WorkerWageSummary?> {
        return unpaidSummaries.map { summaries -> summaries.find { it.worker.id == workerId } }
    }

    fun observeProcessedWage(paymentId: Int): Flow<ProcessedWageSummary?> {
        return processedWages.map { paid -> paid.find { it.wagePayment.id == paymentId } }
    }

    suspend fun processWorkerWage(
        context: Context,
        workerId: Int,
        selectedTaskIds: List<Int>,
        paymentMethod: String
    ): Result<File> {
        return runCatchingWithLoading {
            val cycleId = requireActiveCycle()
            val worker = workerDao.getWorkerById(workerId)
                ?: error("Worker not found")

            if (selectedTaskIds.isEmpty()) {
                error(context.getString(R.string.wages_error_no_tasks_selected))
            }

            val workerTasks = taskDao.getTasksByWorkerAndCycle(workerId, cycleId).first()
            val selectedTasks = workerTasks.filter {
                selectedTaskIds.contains(it.id) &&
                    it.paidInWagePaymentId == null &&
                    it.status.equals(Constants.TASK_STATUS_COMPLETED, ignoreCase = true)
            }

            if (selectedTasks.isEmpty()) {
                error(context.getString(R.string.wages_error_no_payable_tasks))
            }

            val pendingTasks = workerTasks.filter {
                it.paidInWagePaymentId == null &&
                    (!it.status.equals(Constants.TASK_STATUS_COMPLETED, ignoreCase = true) ||
                        !selectedTaskIds.contains(it.id))
            }

            val pdf = processWorkerWageInternal(
                context = context,
                worker = worker,
                cycleId = cycleId,
                selectedTasks = selectedTasks,
                pendingTasks = pendingTasks,
                paymentMethod = paymentMethod
            )

            PdfGenerator(context).openPdf(pdf)
            pdf
        }
    }

    suspend fun processAllPendingWages(
        context: Context,
        paymentMethod: String
    ): ProcessAllResult {
        _loading.update { true }
        return try {
            val summaries = unpaidSummaries.value
            if (summaries.isEmpty()) {
                ProcessAllResult(processed = 0, failed = 0, errors = emptyList())
            } else {
                val cycleId = requireActiveCycle()
                val errors = mutableListOf<String>()
                var processedCount = 0

                summaries.forEach { summary ->
                    if (!summary.hasPayableWork) return@forEach

                    try {
                        val pdf = processWorkerWageInternal(
                            context = context,
                            worker = summary.worker,
                            cycleId = cycleId,
                            selectedTasks = summary.completedTasks,
                            pendingTasks = summary.inProgressTasks,
                            paymentMethod = paymentMethod
                        )
                        PdfGenerator(context).openPdf(pdf)
                        processedCount++
                    } catch (ex: Exception) {
                        val message = ex.message ?: context.getString(R.string.wages_error_unknown)
                        errors.add("${summary.worker.fullName}: $message")
                    }
                }

                ProcessAllResult(
                    processed = processedCount,
                    failed = errors.size,
                    errors = errors
                )
            }
        } finally {
            _loading.update { false }
        }
    }

    suspend fun printPayslip(context: Context, paymentId: Int): Result<File> {
        return runCatchingWithLoading<File> {
            val payment = wagePaymentDao.getWagePaymentById(paymentId)
                ?: error(context.getString(R.string.wages_error_payment_missing))
            val worker = workerDao.getWorkerById(payment.workerId)
                ?: error(context.getString(R.string.wages_error_worker_missing))

            val paidTasks = taskDao.getTasksByWagePayment(paymentId).first()
            val pendingTasks = taskDao.getTasksByWorkerAndCycle(payment.workerId, payment.cycleId).first()
                .filter { it.paidInWagePaymentId == null }

            val advances = advancePaymentDao.getAdvancePaymentsByWorkerAndCycle(payment.workerId, payment.cycleId).first()
            val enterpriseName = appSettingsDao.getSettings().first()?.enterpriseName
                ?: context.getString(R.string.app_name)

            val pdf = PdfGenerator(context).generatePayslip(
                wagePayment = payment,
                worker = worker,
                tasks = paidTasks,
                advances = advances,
                pendingTasks = pendingTasks,
                enterpriseName = enterpriseName
            ) ?: error(context.getString(R.string.wages_error_pdf_failed))

            wagePaymentDao.updatePayslipPath(paymentId, pdf.absolutePath)
            PdfGenerator(context).openPdf(pdf)
            pdf
        }
    }


    private fun buildUnpaidSummaries(
        cycleId: Int,
        workers: List<Worker>,
        tasks: List<Task>,
        payments: List<WagePayment>,
        advances: List<com.palmfarm.manager.data.database.entities.AdvancePayment>
    ): List<WorkerWageSummary> {
        val workersById = workers.associateBy { it.id }
        val tasksByWorker = tasks.filter { it.cycleId == cycleId }.groupBy { it.workerId }
        val paymentsByWorker = payments.groupBy { it.workerId }
        val advancesByWorker = advances.groupBy { it.workerId }

        val workerIds = mutableSetOf<Int>()
        workerIds.addAll(tasksByWorker.keys)
        workerIds.addAll(paymentsByWorker.keys)
        workerIds.addAll(advancesByWorker.keys)

        return workerIds.mapNotNull { workerId ->
            val worker = workersById[workerId] ?: return@mapNotNull null
            if (!worker.isActive) return@mapNotNull null

            val workerTasks = tasksByWorker[workerId].orEmpty()

            val completedUnpaid = workerTasks.filter {
                it.paidInWagePaymentId == null &&
                    it.status.equals(Constants.TASK_STATUS_COMPLETED, ignoreCase = true) &&
                    (it.quantity ?: 0.0) > 0.0
            }.sortedBy { it.createdAt }

            val inProgress = workerTasks.filter {
                it.paidInWagePaymentId == null &&
                    !it.status.equals(Constants.TASK_STATUS_COMPLETED, ignoreCase = true)
            }.sortedBy { it.createdAt }

            if (completedUnpaid.isEmpty() && inProgress.isEmpty()) return@mapNotNull null

            val totalAdvances = advancesByWorker[workerId].orEmpty().sumOf { it.amount }
            val advancesApplied = paymentsByWorker[workerId].orEmpty().sumOf { it.totalAdvances }
            val outstandingAdvances = (totalAdvances - advancesApplied).coerceAtLeast(0.0)

            WorkerWageSummary(
                worker = worker,
                cycleId = cycleId,
                completedTasks = completedUnpaid,
                inProgressTasks = inProgress,
                outstandingAdvances = outstandingAdvances
            )
        }.sortedBy { it.worker.fullName }
    }

    private fun buildProcessedSummaries(
        payments: List<WagePayment>,
        workers: List<Worker>,
        tasks: List<Task>
    ): List<ProcessedWageSummary> {
        val workerById = workers.associateBy { it.id }
        val tasksByPayment = tasks.filter { it.paidInWagePaymentId != null }
            .groupBy { it.paidInWagePaymentId }

        return payments.mapNotNull { payment ->
            val worker = workerById[payment.workerId] ?: return@mapNotNull null
            val paidTasks = tasksByPayment[payment.id]?.sortedBy { it.createdAt } ?: emptyList()
            ProcessedWageSummary(
                wagePayment = payment,
                worker = worker,
                paidTasks = paidTasks
            )
        }.sortedByDescending { it.wagePayment.paymentDate }
    }

    private suspend fun processWorkerWageInternal(
        context: Context,
        worker: Worker,
        cycleId: Int,
        selectedTasks: List<Task>,
        pendingTasks: List<Task>,
        paymentMethod: String
    ): File {
        if (selectedTasks.isEmpty()) {
            throw IllegalStateException(context.getString(R.string.wages_error_no_payable_tasks))
        }

        val gross = selectedTasks.sumOf { (it.quantity ?: 0.0) * it.payRate }
        if (gross <= 0.0) {
            throw IllegalStateException(context.getString(R.string.wages_error_no_payable_tasks))
        }

        val outstandingAdvances = calculateOutstandingAdvances(worker.id, cycleId)
        val advanceApplied = min(outstandingAdvances, gross)
        val net = gross - advanceApplied

        val wagePayment = WagePayment(
            id = 0,
            cycleId = cycleId,
            workerId = worker.id,
            grossWage = gross,
            totalAdvances = advanceApplied,
            netPayment = net,
            paymentDate = System.currentTimeMillis(),
            paymentMethod = paymentMethod,
            referenceNumber = "WAGE-${System.currentTimeMillis()}-${worker.id}",
            payslipPath = "",
            createdAt = System.currentTimeMillis()
        )

        val paymentId = wagePaymentDao.insert(wagePayment).toInt()
        selectedTasks.forEach { task ->
            taskDao.markTaskAsPaid(task.id, paymentId)
        }

        val advances = advancePaymentDao.getAdvancePaymentsByWorkerAndCycle(worker.id, cycleId).first()
        val enterpriseName = appSettingsDao.getSettings().first()?.enterpriseName
            ?: context.getString(R.string.app_name)

        val pdf = PdfGenerator(context).generatePayslip(
            wagePayment = wagePayment.copy(id = paymentId),
            worker = worker,
            tasks = selectedTasks,
            advances = advances,
            pendingTasks = pendingTasks,
            enterpriseName = enterpriseName
        ) ?: throw IllegalStateException(context.getString(R.string.wages_error_pdf_failed))

        wagePaymentDao.updatePayslipPath(paymentId, pdf.absolutePath)
        return pdf
    }

    private suspend fun calculateOutstandingAdvances(workerId: Int, cycleId: Int): Double {
        val totalAdvances = advancePaymentDao.getAdvancePaymentsByWorkerAndCycle(workerId, cycleId).first()
            .sumOf { it.amount }
        val applied = wagePaymentDao.getWagePaymentsByWorkerAndCycle(workerId, cycleId).first()
            .sumOf { it.totalAdvances }
        return (totalAdvances - applied).coerceAtLeast(0.0)
    }

    private suspend fun requireActiveCycle(): Int {
        // First check current value
        val currentValue = currentCycleIdFlow.value
        if (currentValue > 0) {
            return currentValue
        }
        // If current value is 0, wait for a valid cycle ID
        val cycleId = currentCycleIdFlow.first { it > 0 }
        if (cycleId <= 0) throw IllegalStateException("No active production cycle")
        return cycleId
    }

    private suspend fun <T> runCatchingWithLoading(block: suspend () -> T): Result<T> {
        _loading.update { true }
        return try {
            Result.success(block())
        } catch (ex: Exception) {
            Result.failure(ex)
        } finally {
            _loading.update { false }
        }
    }
}
