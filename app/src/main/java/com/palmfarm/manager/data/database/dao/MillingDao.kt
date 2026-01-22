package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Milling
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Milling entity
 */
@Dao
interface MillingDao {

    @Query("SELECT * FROM millings ORDER BY date DESC")
    fun getAllMillings(): Flow<List<Milling>>

    @Query("SELECT * FROM millings WHERE cycle_id = :cycleId ORDER BY date DESC")
    fun getMillingsByCycle(cycleId: Int): Flow<List<Milling>>

    @Query("SELECT * FROM millings WHERE miller_id = :millerId ORDER BY date DESC")
    fun getMillingsByMiller(millerId: Int): Flow<List<Milling>>

    @Query("SELECT * FROM millings WHERE id = :millingId")
    suspend fun getMillingByIdOnce(millingId: Int): Milling?

    @Query("SELECT * FROM millings WHERE id = :millingId")
    fun getMillingById(millingId: Int): Flow<Milling?>

    @Query("SELECT * FROM millings WHERE id = :millingId")
    fun getMillingByIdFlow(millingId: Int): Flow<Milling?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milling: Milling): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilling(milling: Milling): Long

    @Update
    suspend fun update(milling: Milling)

    @Update
    suspend fun updateMilling(milling: Milling)

    @Delete
    suspend fun delete(milling: Milling)

    @Delete
    suspend fun deleteMilling(milling: Milling)

    @Query("DELETE FROM millings WHERE id = :millingId")
    suspend fun deleteById(millingId: Int)

    // Get total bunches milled for a cycle
    @Query("SELECT COALESCE(SUM(bunches_milled), 0) FROM millings WHERE cycle_id = :cycleId")
    suspend fun getTotalBunchesMilledForCycle(cycleId: Int): Int

    @Query("SELECT COALESCE(SUM(bunches_milled), 0) FROM millings WHERE cycle_id = :cycleId")
    fun getTotalBunchesMilledForCycleFlow(cycleId: Int): Flow<Int>

    @Query("SELECT COALESCE(SUM(bunches_milled), 0) FROM millings WHERE cycle_id = :cycleId")
    fun getTotalBunchesMilledByCycle(cycleId: Int): Flow<Int>

    // Get total drums cooked for a cycle
    @Query("SELECT COALESCE(SUM(drums_cooked), 0) FROM millings WHERE cycle_id = :cycleId")
    suspend fun getTotalDrumsCookedForCycle(cycleId: Int): Int

    @Query("SELECT COALESCE(SUM(drums_cooked), 0) FROM millings WHERE cycle_id = :cycleId")
    fun getTotalDrumsCookedForCycleFlow(cycleId: Int): Flow<Int>

    @Query("SELECT COALESCE(SUM(drums_cooked), 0) FROM millings WHERE cycle_id = :cycleId")
    fun getTotalDrumsByCycle(cycleId: Int): Flow<Int>

    // Get total oil produced for a cycle
    @Query("SELECT COALESCE(SUM(oil_produced_gallons), 0.0) FROM millings WHERE cycle_id = :cycleId")
    suspend fun getTotalOilProducedForCycle(cycleId: Int): Double

    @Query("SELECT COALESCE(SUM(oil_produced_gallons), 0.0) FROM millings WHERE cycle_id = :cycleId")
    fun getTotalOilProducedForCycleFlow(cycleId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(oil_produced_gallons), 0.0) FROM millings WHERE cycle_id = :cycleId")
    fun getTotalOilProducedByCycle(cycleId: Int): Flow<Double>

    // Get total oil produced (all time)
    @Query("SELECT COALESCE(SUM(oil_produced_gallons), 0.0) FROM millings")
    fun getTotalOilProduced(): Flow<Double>

    // Get milling count for cycle
    @Query("SELECT COUNT(*) FROM millings WHERE cycle_id = :cycleId")
    suspend fun getMillingCountForCycle(cycleId: Int): Int

    // Get recent millings (limit)
    @Query("SELECT * FROM millings ORDER BY date DESC LIMIT :limit")
    fun getRecentMillings(limit: Int): Flow<List<Milling>>
}
