package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Consumption
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Consumption entity
 */
@Dao
interface ConsumptionDao {

    @Query("SELECT * FROM consumptions ORDER BY date DESC")
    fun getAllConsumptions(): Flow<List<Consumption>>

    @Query("SELECT * FROM consumptions ORDER BY date DESC")
    fun getAllConsumption(): Flow<List<Consumption>>

    @Query("SELECT * FROM consumptions WHERE cycle_id = :cycleId ORDER BY date DESC")
    fun getConsumptionsByCycle(cycleId: Int): Flow<List<Consumption>>

    @Query("SELECT * FROM consumptions WHERE id = :consumptionId")
    suspend fun getConsumptionById(consumptionId: Int): Consumption?

    @Query("SELECT * FROM consumptions WHERE id = :consumptionId")
    fun getConsumptionByIdFlow(consumptionId: Int): Flow<Consumption?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(consumption: Consumption): Long

    @Update
    suspend fun update(consumption: Consumption)

    @Delete
    suspend fun delete(consumption: Consumption)

    @Query("DELETE FROM consumptions WHERE id = :consumptionId")
    suspend fun deleteById(consumptionId: Int)

    // Get total consumption value for cycle (sum of valued_at_price which already contains quantity * price)
    @Query("SELECT COALESCE(SUM(valued_at_price), 0.0) FROM consumptions WHERE cycle_id = :cycleId")
    suspend fun getTotalConsumptionValueForCycle(cycleId: Int): Double

    @Query("SELECT COALESCE(SUM(valued_at_price), 0.0) FROM consumptions WHERE cycle_id = :cycleId")
    fun getTotalConsumptionValueForCycleFlow(cycleId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(valued_at_price), 0.0) FROM consumptions")
    fun getTotalConsumptionValue(): Flow<Double>

    // Get total gallons consumed
    @Query("SELECT COALESCE(SUM(quantity_gallons), 0.0) FROM consumptions")
    suspend fun getTotalGallonsConsumed(): Double

    @Query("SELECT COALESCE(SUM(quantity_gallons), 0.0) FROM consumptions")
    fun getTotalOilConsumed(): Flow<Double>

    // Get total gallons consumed for cycle
    @Query("SELECT COALESCE(SUM(quantity_gallons), 0.0) FROM consumptions WHERE cycle_id = :cycleId")
    suspend fun getTotalGallonsConsumedForCycle(cycleId: Int): Double

    // Get most consumed (single record with highest quantity)
    @Query("SELECT * FROM consumptions ORDER BY quantity_gallons DESC LIMIT 1")
    suspend fun getMostConsumed(): Consumption?

    // Get least consumed (single record with lowest quantity)
    @Query("SELECT * FROM consumptions ORDER BY quantity_gallons ASC LIMIT 1")
    suspend fun getLeastConsumed(): Consumption?

    // Get recent consumptions (limit)
    @Query("SELECT * FROM consumptions ORDER BY date DESC LIMIT :limit")
    fun getRecentConsumptions(limit: Int): Flow<List<Consumption>>

    // Get consumption count for cycle
    @Query("SELECT COUNT(*) FROM consumptions WHERE cycle_id = :cycleId")
    suspend fun getConsumptionCountForCycle(cycleId: Int): Int
}
