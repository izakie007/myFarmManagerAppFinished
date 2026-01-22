package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Farm
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Farm entity
 */
@Dao
interface FarmDao {

    @Query("SELECT * FROM farms ORDER BY created_at DESC")
    fun getAllFarms(): Flow<List<Farm>>

    @Query("SELECT * FROM farms WHERE id = :farmId")
    suspend fun getFarmById(farmId: Int): Farm?

    @Query("SELECT * FROM farms WHERE id = :farmId")
    fun getFarmByIdFlow(farmId: Int): Flow<Farm?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(farm: Farm): Long

    @Update
    suspend fun update(farm: Farm)

    @Delete
    suspend fun delete(farm: Farm)

    @Query("DELETE FROM farms WHERE id = :farmId")
    suspend fun deleteById(farmId: Int)

    @Query("SELECT COUNT(*) FROM farms")
    suspend fun getFarmCount(): Int

    @Query("SELECT SUM(productive_palms) FROM farms")
    suspend fun getTotalProductivePalms(): Int?

    @Query("SELECT SUM(unproductive_palms) FROM farms")
    suspend fun getTotalUnproductivePalms(): Int?

    @Query("SELECT SUM(productive_palms + unproductive_palms) FROM farms")
    suspend fun getTotalPalms(): Int?

    @Query("SELECT COALESCE(SUM(productive_palms + unproductive_palms), 0) FROM farms")
    fun getTotalPalmsFlow(): Flow<Int>
}
