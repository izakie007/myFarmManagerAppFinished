package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.LooseNutsPicking
import kotlinx.coroutines.flow.Flow

/**
 * DAO for LooseNutsPicking entity
 */
@Dao
interface LooseNutsPickingDao {

    @Query("SELECT * FROM loose_nuts_pickings ORDER BY date DESC")
    fun getAllLooseNutsPickings(): Flow<List<LooseNutsPicking>>

    @Query("SELECT * FROM loose_nuts_pickings ORDER BY date DESC")
    fun getAllLooseNuts(): Flow<List<LooseNutsPicking>>

    @Query("SELECT * FROM loose_nuts_pickings WHERE cycle_id = :cycleId ORDER BY date DESC")
    fun getLooseNutsPickingsByCycle(cycleId: Int): Flow<List<LooseNutsPicking>>

    @Query("SELECT * FROM loose_nuts_pickings WHERE cycle_id = :cycleId ORDER BY date DESC")
    fun getLooseNutsByCycle(cycleId: Int): Flow<List<LooseNutsPicking>>

    @Query("SELECT * FROM loose_nuts_pickings WHERE picker_id = :pickerId ORDER BY date DESC")
    fun getLooseNutsPickingsByPicker(pickerId: Int): Flow<List<LooseNutsPicking>>

    @Query("SELECT * FROM loose_nuts_pickings WHERE id = :pickingId")
    suspend fun getLooseNutsPickingById(pickingId: Int): LooseNutsPicking?

    @Query("SELECT * FROM loose_nuts_pickings WHERE id = :pickingId")
    fun getLooseNutsById(pickingId: Int): Flow<LooseNutsPicking?>

    @Query("SELECT * FROM loose_nuts_pickings WHERE id = :pickingId")
    fun getLooseNutsPickingByIdFlow(pickingId: Int): Flow<LooseNutsPicking?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(looseNutsPicking: LooseNutsPicking): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLooseNuts(looseNutsPicking: LooseNutsPicking): Long

    @Update
    suspend fun update(looseNutsPicking: LooseNutsPicking)

    @Update
    suspend fun updateLooseNuts(looseNutsPicking: LooseNutsPicking)

    @Delete
    suspend fun delete(looseNutsPicking: LooseNutsPicking)

    @Delete
    suspend fun deleteLooseNuts(looseNutsPicking: LooseNutsPicking)

    @Query("DELETE FROM loose_nuts_pickings WHERE id = :pickingId")
    suspend fun deleteById(pickingId: Int)

    @Query("SELECT COALESCE(SUM(number_of_bags), 0.0) FROM loose_nuts_pickings WHERE cycle_id = :cycleId")
    suspend fun getTotalBagsForCycle(cycleId: Int): Double

    @Query("SELECT COUNT(*) FROM loose_nuts_pickings WHERE cycle_id = :cycleId")
    suspend fun getPickingCountForCycle(cycleId: Int): Int
}
