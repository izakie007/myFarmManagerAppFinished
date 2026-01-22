package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Loan
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Loan entity
 */
@Dao
interface LoanDao {

    @Query("SELECT * FROM loans ORDER BY start_date DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE is_fully_paid = 0 ORDER BY start_date DESC")
    fun getActiveLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE is_fully_paid = 1 ORDER BY start_date DESC")
    fun getFullyPaidLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoanById(loanId: Int): Loan?

    @Query("SELECT * FROM loans WHERE id = :loanId")
    fun getLoanByIdFlow(loanId: Int): Flow<Loan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan): Long

    @Update
    suspend fun update(loan: Loan)

    @Delete
    suspend fun delete(loan: Loan)

    @Query("DELETE FROM loans WHERE id = :loanId")
    suspend fun deleteById(loanId: Int)

    // Update payments made
    @Query("UPDATE loans SET number_of_payments_made = :paymentsMade WHERE id = :loanId")
    suspend fun updatePaymentsMade(loanId: Int, paymentsMade: Int)

    // Get total monthly payment from all active loans
    @Query("SELECT COALESCE(SUM(monthly_payment), 0.0) FROM loans WHERE is_fully_paid = 0")
    suspend fun getTotalMonthlyPaymentForActiveLoans(): Double

    @Query("SELECT COALESCE(SUM(monthly_payment), 0.0) FROM loans WHERE is_fully_paid = 0")
    fun getTotalMonthlyPaymentForActiveLoansFlow(): Flow<Double>

    // Get total amount left to pay
    @Query("SELECT COALESCE(SUM(total_left), 0.0) FROM loans WHERE is_fully_paid = 0")
    suspend fun getTotalAmountLeft(): Double

    @Query("SELECT COUNT(*) FROM loans WHERE is_fully_paid = 0")
    suspend fun getActiveLoanCount(): Int
}
