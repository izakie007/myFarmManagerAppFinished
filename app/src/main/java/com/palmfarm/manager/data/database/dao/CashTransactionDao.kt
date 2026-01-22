package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.CashTransaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Cash Transaction entity
 */
@Dao
interface CashTransactionDao {

    @Query("SELECT * FROM cash_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<CashTransaction>>

    @Query("SELECT * FROM cash_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): CashTransaction?

    @Query("SELECT * FROM cash_transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<CashTransaction>>

    @Query("SELECT * FROM cash_transactions WHERE transaction_type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<CashTransaction>>

    @Query("SELECT SUM(amount) FROM cash_transactions WHERE transaction_type = 'CASH_IN' OR transaction_type = 'OPENING_BALANCE'")
    suspend fun getTotalCashIn(): Double?

    @Query("SELECT SUM(amount) FROM cash_transactions WHERE transaction_type = 'CASH_OUT'")
    suspend fun getTotalCashOut(): Double?

    /**
     * Get current cash balance (all cash in minus all cash out)
     */
    @Query("""
        SELECT
            COALESCE(
                (SELECT SUM(amount) FROM cash_transactions WHERE transaction_type IN ('CASH_IN', 'OPENING_BALANCE')),
                0
            ) -
            COALESCE(
                (SELECT SUM(amount) FROM cash_transactions WHERE transaction_type = 'CASH_OUT'),
                0
            ) as balance
    """)
    suspend fun getCurrentBalance(): Double?

    /**
     * Get current cash balance as a reactive Flow
     */
    @Query("""
        SELECT
            COALESCE(
                (SELECT SUM(amount) FROM cash_transactions WHERE transaction_type IN ('CASH_IN', 'OPENING_BALANCE')),
                0
            ) -
            COALESCE(
                (SELECT SUM(amount) FROM cash_transactions WHERE transaction_type = 'CASH_OUT'),
                0
            ) as balance
    """)
    fun getCurrentBalanceFlow(): Flow<Double?>

    @Query("SELECT * FROM cash_transactions WHERE transaction_type = 'OPENING_BALANCE' LIMIT 1")
    suspend fun getOpeningBalance(): CashTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: CashTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CashTransaction): Long

    @Update
    suspend fun update(transaction: CashTransaction)

    @Update
    suspend fun updateTransaction(transaction: CashTransaction)

    @Delete
    suspend fun delete(transaction: CashTransaction)

    @Query("DELETE FROM cash_transactions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM cash_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Int)

    /**
     * Get cash flow summary for all time
     */
    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN transaction_type IN ('CASH_IN', 'OPENING_BALANCE') THEN amount ELSE 0 END), 0) as cashIn,
            COALESCE(SUM(CASE WHEN transaction_type = 'CASH_OUT' THEN amount ELSE 0 END), 0) as cashOut
        FROM cash_transactions
    """)
    suspend fun getCashFlowSummary(): CashFlowSummary

    /**
     * Get cash flow for a period
     */
    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN transaction_type IN ('CASH_IN', 'OPENING_BALANCE') THEN amount ELSE 0 END), 0) as cashIn,
            COALESCE(SUM(CASE WHEN transaction_type = 'CASH_OUT' THEN amount ELSE 0 END), 0) as cashOut
        FROM cash_transactions
        WHERE date >= :startDate AND date <= :endDate
    """)
    suspend fun getCashFlowForPeriod(startDate: Long, endDate: Long): CashFlowSummary
}

/**
 * Cash flow summary
 */
data class CashFlowSummary(
    val cashIn: Double,
    val cashOut: Double
) {
    val netCashFlow: Double get() = cashIn - cashOut
}
