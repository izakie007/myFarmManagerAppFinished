package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Worker
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Worker entity
 */
@Dao
interface WorkerDao {

    @Query("SELECT * FROM workers ORDER BY first_name ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE is_active = 1 ORDER BY first_name ASC")
    fun getActiveWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE is_active = 0 ORDER BY first_name ASC")
    fun getInactiveWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :workerId")
    suspend fun getWorkerById(workerId: Int): Worker?

    @Query("SELECT * FROM workers WHERE id = :workerId")
    fun getWorkerByIdFlow(workerId: Int): Flow<Worker?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(worker: Worker): Long

    @Update
    suspend fun update(worker: Worker)

    @Delete
    suspend fun delete(worker: Worker)

    @Query("DELETE FROM workers WHERE id = :workerId")
    suspend fun deleteById(workerId: Int)

    @Query("UPDATE workers SET is_active = :isActive WHERE id = :workerId")
    suspend fun updateWorkerStatus(workerId: Int, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM workers WHERE is_active = 1")
    suspend fun getActiveWorkerCount(): Int

    @Query("SELECT * FROM workers WHERE specialty = :specialty AND is_active = 1")
    fun getWorkersBySpecialty(specialty: String): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE first_name LIKE :query OR last_name LIKE :query OR phone_number LIKE :query ORDER BY first_name ASC")
    fun searchWorkers(query: String): Flow<List<Worker>>
}
