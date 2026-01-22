package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.AdvancePayment
import kotlinx.coroutines.flow.Flow

/**
 * DAO for AdvancePayment entity
 */
@Dao
interface AdvancePaymentDao {

    @Query("SELECT * FROM advance_payments ORDER BY date DESC")
    fun getAllAdvancePayments(): Flow<List<AdvancePayment>>

    @Query("SELECT * FROM advance_payments WHERE worker_id = :workerId ORDER BY date DESC")
    fun getAdvancePaymentsByWorker(workerId: Int): Flow<List<AdvancePayment>>

    @Query("SELECT * FROM advance_payments WHERE worker_id = :workerId AND cycle_id = :cycleId ORDER BY date DESC")
    fun getAdvancePaymentsByWorkerAndCycle(workerId: Int, cycleId: Int): Flow<List<AdvancePayment>>

    @Query("SELECT * FROM advance_payments WHERE cycle_id = :cycleId ORDER BY date DESC")
    fun getAdvancePaymentsByCycle(cycleId: Int): Flow<List<AdvancePayment>>

    @Query("SELECT * FROM advance_payments WHERE id = :advancePaymentId")
    suspend fun getAdvancePaymentById(advancePaymentId: Int): AdvancePayment?

    @Query("SELECT * FROM advance_payments WHERE id = :advancePaymentId")
    fun getAdvancePaymentByIdFlow(advancePaymentId: Int): Flow<AdvancePayment?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(advancePayment: AdvancePayment): Long

    @Update
    suspend fun update(advancePayment: AdvancePayment)

    @Delete
    suspend fun delete(advancePayment: AdvancePayment)

    @Query("DELETE FROM advance_payments WHERE id = :advancePaymentId")
    suspend fun deleteById(advancePaymentId: Int)

    // Get total advances for a worker in a cycle
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM advance_payments WHERE worker_id = :workerId AND cycle_id = :cycleId")
    suspend fun getTotalAdvancesForWorkerInCycle(workerId: Int, cycleId: Int): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM advance_payments WHERE worker_id = :workerId AND cycle_id = :cycleId")
    fun getTotalAdvancesForWorkerInCycleFlow(workerId: Int, cycleId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM advance_payments WHERE worker_id = :workerId AND cycle_id = :cycleId")
    fun getTotalAdvancesByWorkerAndCycle(workerId: Int, cycleId: Int): Flow<Double>

    // Get total advances (all cycles)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM advance_payments")
    fun getTotalAdvances(): Flow<Double>

    // Get total advances for cycle
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM advance_payments WHERE cycle_id = :cycleId")
    suspend fun getTotalAdvancesForCycle(cycleId: Int): Double

    @Query("SELECT COUNT(*) FROM advance_payments WHERE worker_id = :workerId AND cycle_id = :cycleId")
    suspend fun getAdvanceCountForWorkerInCycle(workerId: Int, cycleId: Int): Int
}
