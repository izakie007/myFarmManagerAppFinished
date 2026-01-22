package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.WagePayment
import kotlinx.coroutines.flow.Flow

/**
 * DAO for WagePayment entity
 * These records are immutable once created
 */
@Dao
interface WagePaymentDao {

    @Query("SELECT * FROM wage_payments ORDER BY payment_date DESC")
    fun getAllWagePayments(): Flow<List<WagePayment>>

    @Query("SELECT * FROM wage_payments WHERE worker_id = :workerId ORDER BY payment_date DESC")
    fun getWagePaymentsByWorker(workerId: Int): Flow<List<WagePayment>>

    @Query("SELECT * FROM wage_payments WHERE cycle_id = :cycleId ORDER BY payment_date DESC")
    fun getWagePaymentsByCycle(cycleId: Int): Flow<List<WagePayment>>

    @Query("SELECT * FROM wage_payments WHERE worker_id = :workerId AND cycle_id = :cycleId ORDER BY payment_date DESC")
    fun getWagePaymentsByWorkerAndCycle(workerId: Int, cycleId: Int): Flow<List<WagePayment>>

    @Query("SELECT * FROM wage_payments WHERE worker_id = :workerId AND cycle_id = :cycleId ORDER BY payment_date DESC LIMIT 1")
    fun getWagePaymentByWorkerAndCycle(workerId: Int, cycleId: Int): Flow<WagePayment?>

    @Query("SELECT * FROM wage_payments WHERE id = :wagePaymentId")
    suspend fun getWagePaymentById(wagePaymentId: Int): WagePayment?

    @Query("SELECT * FROM wage_payments WHERE id = :wagePaymentId")
    fun getWagePaymentByIdFlow(wagePaymentId: Int): Flow<WagePayment?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(wagePayment: WagePayment): Long

    @Query("UPDATE wage_payments SET payslip_path = :payslipPath WHERE id = :paymentId")
    suspend fun updatePayslipPath(paymentId: Int, payslipPath: String)

    // Note: No update or delete operations - wage payments are immutable

    // Get total wages paid (all cycles)
    @Query("SELECT COALESCE(SUM(net_payment), 0.0) FROM wage_payments")
    fun getTotalWagesPaid(): Flow<Double>

    // Note: getTotalUnpaidWages would require joining with tasks table
    // For now, this should be calculated in the ViewModel or UseCase level
    // by getting unpaid completed tasks and calculating their total

    // Get total unpaid wages
    @Query("SELECT COALESCE(SUM(gross_wage) - SUM(net_payment), 0.0) FROM wage_payments")
    fun getTotalUnpaidWages(): Flow<Double>

    // Get total wages paid for cycle
    @Query("SELECT COALESCE(SUM(net_payment), 0.0) FROM wage_payments WHERE cycle_id = :cycleId")
    suspend fun getTotalWagesPaidForCycle(cycleId: Int): Double

    @Query("SELECT COALESCE(SUM(net_payment), 0.0) FROM wage_payments WHERE cycle_id = :cycleId")
    fun getTotalWagesPaidForCycleFlow(cycleId: Int): Flow<Double>

    // Get total wages paid for worker
    @Query("SELECT COALESCE(SUM(net_payment), 0.0) FROM wage_payments WHERE worker_id = :workerId")
    suspend fun getTotalWagesPaidForWorker(workerId: Int): Double

    // Get total gross wages for cycle
    @Query("SELECT COALESCE(SUM(gross_wage), 0.0) FROM wage_payments WHERE cycle_id = :cycleId")
    suspend fun getTotalGrossWagesForCycle(cycleId: Int): Double

    // Check if worker has wage payment in cycle
    @Query("SELECT COUNT(*) > 0 FROM wage_payments WHERE worker_id = :workerId AND cycle_id = :cycleId")
    suspend fun hasWagePaymentInCycle(workerId: Int, cycleId: Int): Boolean

    // Get recent wage payments (limit)
    @Query("SELECT * FROM wage_payments ORDER BY payment_date DESC LIMIT :limit")
    fun getRecentWagePayments(limit: Int): Flow<List<WagePayment>>

    // Get wage payment count for cycle
    @Query("SELECT COUNT(*) FROM wage_payments WHERE cycle_id = :cycleId")
    suspend fun getWagePaymentCountForCycle(cycleId: Int): Int
}
