package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Expense
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Expense entity
 */
@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE cycle_id = :cycleId ORDER BY date DESC")
    fun getExpensesByCycle(cycleId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE cycle_id = :cycleId AND category = :category ORDER BY date DESC")
    fun getExpensesByCycleAndCategory(cycleId: Int, category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Int): Expense?

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    fun getExpenseByIdFlow(expenseId: Int): Flow<Expense?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: Int)

    // Get total expenses for cycle
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE cycle_id = :cycleId")
    suspend fun getTotalExpensesForCycle(cycleId: Int): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE cycle_id = :cycleId")
    fun getTotalExpensesByCycle(cycleId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE cycle_id = :cycleId")
    fun getTotalExpensesForCycleFlow(cycleId: Int): Flow<Double>

    // Get total expenses (all cycles)
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses")
    fun getTotalExpenses(): Flow<Double>

    // Get total expenses by category
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE category = :category")
    suspend fun getTotalExpensesByCategory(category: String): Double

    // Get total expenses by category for cycle
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE cycle_id = :cycleId AND category = :category")
    suspend fun getTotalExpensesByCategoryForCycle(cycleId: Int, category: String): Double

    // Get recent expenses (limit)
    @Query("SELECT * FROM expenses ORDER BY date DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int): Flow<List<Expense>>

    // Get expense count for cycle
    @Query("SELECT COUNT(*) FROM expenses WHERE cycle_id = :cycleId")
    suspend fun getExpenseCountForCycle(cycleId: Int): Int

    // Get expenses within date range
    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>
}
