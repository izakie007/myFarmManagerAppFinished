package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Harvest
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Harvest entity
 */
@Dao
interface HarvestDao {

    @Query("SELECT * FROM harvests ORDER BY date DESC")
    fun getAllHarvests(): Flow<List<Harvest>>

    @Query("SELECT * FROM harvests WHERE cycle_id = :cycleId ORDER BY harvest_number ASC")
    fun getHarvestsByCycle(cycleId: Int): Flow<List<Harvest>>

    @Query("SELECT * FROM harvests WHERE harvester_id = :harvesterId ORDER BY date DESC")
    fun getHarvestsByHarvester(harvesterId: Int): Flow<List<Harvest>>

    @Query("SELECT * FROM harvests WHERE id = :harvestId")
    suspend fun getHarvestByIdOnce(harvestId: Int): Harvest?

    @Query("SELECT * FROM harvests WHERE id = :harvestId")
    fun getHarvestById(harvestId: Int): Flow<Harvest?>

    @Query("SELECT * FROM harvests WHERE id = :harvestId")
    fun getHarvestByIdFlow(harvestId: Int): Flow<Harvest?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(harvest: Harvest): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHarvest(harvest: Harvest): Long

    @Update
    suspend fun update(harvest: Harvest)

    @Update
    suspend fun updateHarvest(harvest: Harvest)

    @Delete
    suspend fun delete(harvest: Harvest)

    @Delete
    suspend fun deleteHarvest(harvest: Harvest)

    @Query("DELETE FROM harvests WHERE id = :harvestId")
    suspend fun deleteById(harvestId: Int)

    // Get next harvest number for a cycle
    @Query("SELECT COALESCE(MAX(harvest_number), 0) + 1 FROM harvests WHERE cycle_id = :cycleId")
    suspend fun getNextHarvestNumber(cycleId: Int): Int

    // Get last harvest number for a cycle (returns 0 if no harvests)
    @Query("SELECT COALESCE(MAX(harvest_number), 0) FROM harvests WHERE cycle_id = :cycleId")
    fun getLastHarvestNumberForCycle(cycleId: Int): Flow<Int>

    // Get total bunches harvested for a cycle
    @Query("SELECT COALESCE(SUM(number_of_bunches), 0) FROM harvests WHERE cycle_id = :cycleId")
    suspend fun getTotalBunchesForCycle(cycleId: Int): Int

    @Query("SELECT COALESCE(SUM(number_of_bunches), 0) FROM harvests WHERE cycle_id = :cycleId")
    fun getTotalBunchesForCycleFlow(cycleId: Int): Flow<Int>

    @Query("SELECT COALESCE(SUM(number_of_bunches), 0) FROM harvests WHERE cycle_id = :cycleId")
    fun getTotalBunchesByCycle(cycleId: Int): Flow<Int>

    // Get harvest count for cycle
    @Query("SELECT COUNT(*) FROM harvests WHERE cycle_id = :cycleId")
    suspend fun getHarvestCountForCycle(cycleId: Int): Int

    // Get recent harvests (limit)
    @Query("SELECT * FROM harvests ORDER BY date DESC LIMIT :limit")
    fun getRecentHarvests(limit: Int): Flow<List<Harvest>>
}
