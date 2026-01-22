package com.palmfarm.manager.domain.usecases

import android.util.Log
import com.palmfarm.manager.data.database.dao.AdvancePaymentDao
import com.palmfarm.manager.data.database.dao.TaskDao
import com.palmfarm.manager.data.database.entities.Task
import kotlinx.coroutines.flow.first

/**
 * Use case for calculating worker wages
 */
class CalculateWageUseCase(
    private val taskDao: TaskDao,
    private val advancePaymentDao: AdvancePaymentDao
) {

    /**
     * Execute wage calculation for a worker in a cycle
     */
    suspend fun execute(workerId: Int, cycleId: Int): WageCalculation {
        // Validate cycle ID
        if (cycleId <= 0) {
            return WageCalculation(
                workerId = workerId,
                cycleId = cycleId,
                tasks = emptyList(),
                grossWage = 0.0,
                totalAdvances = 0.0,
                netWage = 0.0
            )
        }

        // Get unpaid completed tasks with quantities > 0 for worker in cycle
        // Use the specific query that already filters for unpaid tasks and quantity > 0 at database level
        val tasks = taskDao.getUnpaidCompletedTasksForWorker(workerId, cycleId)

        // Calculate gross wage from tasks (with null safety for quantity)
        val grossWage = tasks.sumOf { (it.quantity ?: 0.0) * it.payRate }

        // Get total advances (using suspend function that returns Double directly)
        val advances = advancePaymentDao.getTotalAdvancesForWorkerInCycle(workerId, cycleId)

        // Calculate net wage
        val netWage = grossWage - advances

        return WageCalculation(
            workerId = workerId,
            cycleId = cycleId,
            tasks = tasks,
            grossWage = grossWage,
            totalAdvances = advances,
            netWage = netWage
        )
    }

    /**
     * Validate if advance payment is allowed
     * Advances cannot exceed 50% of gross wage
     */
    suspend fun validateAdvancePayment(
        workerId: Int,
        cycleId: Int,
        newAdvanceAmount: Double
    ): AdvanceValidationResult {
        // Debug logging
        Log.d("CalculateWageUseCase", "validateAdvancePayment: workerId=$workerId, cycleId=$cycleId")
        
        // Check if cycle ID is valid
        if (cycleId <= 0) {
            Log.w("CalculateWageUseCase", "Invalid cycle ID: $cycleId")
            return AdvanceValidationResult(
                isValid = false,
                message = "No active production cycle",
                maxAllowedAdvance = 0.0,
                currentAdvances = 0.0
            )
        }

        // Get all tasks for this worker and cycle for debugging (using Flow.first())
        try {
            val allTasks = taskDao.getTasksByWorkerAndCycle(workerId, cycleId).first()
            Log.d("CalculateWageUseCase", "All tasks for worker $workerId in cycle $cycleId: ${allTasks.size}")
            allTasks.forEach { task ->
                Log.d("CalculateWageUseCase", "Task ${task.id}: status=${task.status}, quantity=${task.quantity}, paidInWagePaymentId=${task.paidInWagePaymentId}, cycleId=${task.cycleId}")
            }
        } catch (e: Exception) {
            Log.e("CalculateWageUseCase", "Error getting tasks for debugging", e)
        }

        val wageCalculation = execute(workerId, cycleId)
        
        Log.d("CalculateWageUseCase", "Wage calculation: grossWage=${wageCalculation.grossWage}, totalAdvances=${wageCalculation.totalAdvances}, tasks=${wageCalculation.tasks.size}")

        val maxAllowedAdvance = wageCalculation.grossWage * 0.5
        val totalAdvancesAfter = wageCalculation.totalAdvances + newAdvanceAmount

        return if (totalAdvancesAfter > maxAllowedAdvance) {
            AdvanceValidationResult(
                isValid = false,
                message = "Advance would exceed 50% limit. Max allowed: ${maxAllowedAdvance - wageCalculation.totalAdvances}",
                maxAllowedAdvance = maxAllowedAdvance,
                currentAdvances = wageCalculation.totalAdvances
            )
        } else {
            AdvanceValidationResult(
                isValid = true,
                message = "Advance is within limit",
                maxAllowedAdvance = maxAllowedAdvance,
                currentAdvances = wageCalculation.totalAdvances
            )
        }
    }
}

/**
 * Data class for wage calculation result
 */
data class WageCalculation(
    val workerId: Int,
    val cycleId: Int,
    val tasks: List<Task>,
    val grossWage: Double,
    val totalAdvances: Double,
    val netWage: Double
)

/**
 * Data class for advance validation result
 */
data class AdvanceValidationResult(
    val isValid: Boolean,
    val message: String,
    val maxAllowedAdvance: Double,
    val currentAdvances: Double
)
