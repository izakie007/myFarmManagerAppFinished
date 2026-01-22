package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Task
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Task entity
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE cycle_id = :cycleId ORDER BY created_at DESC")
    fun getTasksByCycle(cycleId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE worker_id = :workerId ORDER BY created_at DESC")
    fun getTasksByWorker(workerId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE worker_id = :workerId AND cycle_id = :cycleId ORDER BY created_at DESC")
    fun getTasksByWorkerAndCycle(workerId: Int, cycleId: Int): Flow<List<Task>>

    @Query("""
        SELECT * FROM tasks
        WHERE worker_id = :workerId
        AND cycle_id = :cycleId
        AND UPPER(status) = 'COMPLETED'
        AND quantity IS NOT NULL
        ORDER BY created_at ASC
    """)
    fun getCompletedTasksByWorkerAndCycle(workerId: Int, cycleId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY created_at DESC")
    fun getTasksByStatus(status: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY created_at DESC")
    fun getTasksByCategory(category: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdFlow(taskId: Int): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE id IN (:taskIds)")
    suspend fun getTasksByIds(taskIds: List<Int>): List<Task>

    @Query("SELECT * FROM tasks WHERE start_date >= :startDate AND start_date <= :endDate ORDER BY start_date DESC")
    fun getTasksBetweenDates(startDate: Long, endDate: Long): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Int)

    // Wage calculation queries
    @Query("""
        SELECT * FROM tasks
        WHERE worker_id = :workerId
        AND cycle_id = :cycleId
        AND UPPER(status) = 'COMPLETED'
        AND quantity IS NOT NULL
        AND quantity > 0
        AND paid_in_wage_payment_id IS NULL
        ORDER BY created_at ASC
    """)
    suspend fun getUnpaidCompletedTasksForWorker(workerId: Int, cycleId: Int): List<Task>

    @Query("""
        SELECT SUM(quantity * pay_rate) FROM tasks
        WHERE worker_id = :workerId
        AND cycle_id = :cycleId
        AND UPPER(status) = 'COMPLETED'
        AND quantity IS NOT NULL
        AND paid_in_wage_payment_id IS NULL
    """)
    suspend fun calculateUnpaidWageForWorker(workerId: Int, cycleId: Int): Double?

    @Query("""
        UPDATE tasks
        SET paid_in_wage_payment_id = :wagePaymentId
        WHERE worker_id = :workerId
        AND cycle_id = :cycleId
        AND UPPER(status) = 'COMPLETED'
        AND quantity IS NOT NULL
        AND paid_in_wage_payment_id IS NULL
    """)
    suspend fun markTasksAsPaid(workerId: Int, cycleId: Int, wagePaymentId: Int)

    @Query("UPDATE tasks SET paid_in_wage_payment_id = :wagePaymentId WHERE id = :taskId")
    suspend fun markTaskAsPaid(taskId: Int, wagePaymentId: Int)

    @Query("SELECT * FROM tasks WHERE paid_in_wage_payment_id = :wagePaymentId")
    fun getTasksByWagePayment(wagePaymentId: Int): Flow<List<Task>>

    @Query("SELECT COUNT(*) FROM tasks WHERE worker_id = :workerId AND UPPER(status) = 'COMPLETED'")
    suspend fun getCompletedTaskCountForWorker(workerId: Int): Int

    @Query("SELECT DISTINCT worker_id FROM tasks WHERE cycle_id = :cycleId AND UPPER(status) = 'COMPLETED' AND quantity IS NOT NULL")
    suspend fun getWorkerIdsWithCompletedTasks(cycleId: Int): List<Int>

    // Get recent tasks (limit)
    @Query("SELECT * FROM tasks ORDER BY created_at DESC LIMIT :limit")
    fun getRecentTasks(limit: Int): Flow<List<Task>>

    @Query("UPDATE tasks SET cycle_id = :newCycleId WHERE cycle_id = :oldCycleId AND paid_in_wage_payment_id IS NULL")
    suspend fun moveUnpaidTasksToNewCycle(oldCycleId: Int, newCycleId: Int)
}
