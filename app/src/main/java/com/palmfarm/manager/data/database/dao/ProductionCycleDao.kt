package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.ProductionCycle
import kotlinx.coroutines.flow.Flow

/**
 * DAO for ProductionCycle entity
 */
@Dao
interface ProductionCycleDao {

    @Query("SELECT * FROM production_cycles ORDER BY start_date DESC")
    fun getAllCycles(): Flow<List<ProductionCycle>>

    @Query("SELECT * FROM production_cycles WHERE is_current = 1 LIMIT 1")
    fun getCurrentCycle(): Flow<ProductionCycle?>

    @Query("SELECT * FROM production_cycles WHERE is_current = 1 LIMIT 1")
    suspend fun getCurrentCycleOnce(): ProductionCycle?

    @Query("SELECT * FROM production_cycles WHERE id = :cycleId")
    suspend fun getCycleByIdOnce(cycleId: Int): ProductionCycle?

    @Query("SELECT * FROM production_cycles WHERE id = :cycleId")
    fun getCycleById(cycleId: Int): Flow<ProductionCycle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: ProductionCycle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: ProductionCycle): Long

    @Update
    suspend fun update(cycle: ProductionCycle)

    @Update
    suspend fun updateCycle(cycle: ProductionCycle)

    @Delete
    suspend fun delete(cycle: ProductionCycle)

    @Query("UPDATE production_cycles SET is_current = 0 WHERE is_current = 1")
    suspend fun markAllCyclesAsNotCurrent()

    @Query("UPDATE production_cycles SET is_current = 1 WHERE id = :cycleId")
    suspend fun markCycleAsCurrent(cycleId: Int)

    @Query("UPDATE production_cycles SET is_current = 0 WHERE id = :cycleId")
    suspend fun markCycleAsNotCurrent(cycleId: Int)

    @Query("UPDATE production_cycles SET realized_bunches = :realizedBunches WHERE id = :cycleId")
    suspend fun updateRealizedBunches(cycleId: Int, realizedBunches: Int)

    @Query("UPDATE production_cycles SET expected_bunches = :expectedBunches WHERE is_current = 1")
    suspend fun updateCurrentCycleExpectedBunches(expectedBunches: Int)

    @Query("UPDATE production_cycles SET start_date = :startDate, end_date = :endDate, cycle_name = :cycleName WHERE is_current = 1")
    suspend fun updateCurrentCycleSchedule(startDate: Long, endDate: Long, cycleName: String)

    @Query("SELECT COALESCE(SUM(number_of_bunches), 0) FROM harvests WHERE cycle_id = :cycleId")
    fun getRealizedBunchesForCycle(cycleId: Int): Flow<Long>

    @Query("SELECT COALESCE(SUM(h.number_of_bunches), 0) FROM harvests h INNER JOIN production_cycles c ON h.cycle_id = c.id WHERE c.is_current = 1")
    fun getCurrentRealizedBunches(): Flow<Long>

    @Query("SELECT * FROM production_cycles WHERE is_current = 0 ORDER BY start_date DESC LIMIT :limit")
    fun getCompletedCycles(limit: Int): Flow<List<ProductionCycle>>

    @Query("SELECT COUNT(*) FROM production_cycles")
    suspend fun getCycleCount(): Int

    @Query("SELECT * FROM production_cycles WHERE start_date <= :timestamp AND end_date >= :timestamp LIMIT 1")
    suspend fun getCycleForDate(timestamp: Long): ProductionCycle?
}
