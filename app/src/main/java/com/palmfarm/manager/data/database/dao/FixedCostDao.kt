package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.FixedCost
import kotlinx.coroutines.flow.Flow

/**
 * DAO for FixedCost entity
 */
@Dao
interface FixedCostDao {

    @Query("SELECT * FROM fixed_costs ORDER BY date DESC")
    fun getAllFixedCosts(): Flow<List<FixedCost>>

    @Query("SELECT * FROM fixed_costs WHERE category = :category ORDER BY date DESC")
    fun getFixedCostsByCategory(category: String): Flow<List<FixedCost>>

    @Query("SELECT * FROM fixed_costs WHERE payment_type = :paymentType ORDER BY date DESC")
    fun getFixedCostsByPaymentType(paymentType: String): Flow<List<FixedCost>>

    @Query("SELECT * FROM fixed_costs WHERE id = :fixedCostId")
    suspend fun getFixedCostById(fixedCostId: Int): FixedCost?

    @Query("SELECT * FROM fixed_costs WHERE id = :fixedCostId")
    fun getFixedCostByIdFlow(fixedCostId: Int): Flow<FixedCost?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fixedCost: FixedCost): Long

    @Update
    suspend fun update(fixedCost: FixedCost)

    @Delete
    suspend fun delete(fixedCost: FixedCost)

    @Query("DELETE FROM fixed_costs WHERE id = :fixedCostId")
    suspend fun deleteById(fixedCostId: Int)

    // Get all active (not fully depreciated) fixed costs
    // Note: This returns all fixed costs; filtering for not fully depreciated needs to be done in the repository
    @Query("SELECT * FROM fixed_costs ORDER BY date DESC")
    suspend fun getActiveFixedCosts(): List<FixedCost>

    // Get total depreciation for a given month
    // This returns all fixed costs to calculate depreciation in repository based on months elapsed
    @Query("SELECT SUM(monthly_depreciation) FROM fixed_costs")
    suspend fun getTotalMonthlyDepreciation(): Double?

    @Query("SELECT COUNT(*) FROM fixed_costs")
    suspend fun getFixedCostCount(): Int
}
