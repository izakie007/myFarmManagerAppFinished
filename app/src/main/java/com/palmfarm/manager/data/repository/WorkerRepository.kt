package com.palmfarm.manager.data.repository

import com.palmfarm.manager.data.database.dao.WorkerDao
import com.palmfarm.manager.data.database.entities.Worker
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Worker operations
 */
class WorkerRepository(
    private val workerDao: WorkerDao
) {

    /**
     * Get all workers
     */
    fun getAllWorkers(): Flow<List<Worker>> {
        return workerDao.getAllWorkers()
    }

    /**
     * Get active workers only
     */
    fun getActiveWorkers(): Flow<List<Worker>> {
        return workerDao.getActiveWorkers()
    }

    /**
     * Get worker by ID
     */
    fun getWorkerById(workerId: Int): Flow<Worker?> {
        return workerDao.getWorkerByIdFlow(workerId)
    }

    /**
     * Insert new worker
     */
    suspend fun insertWorker(worker: Worker): Long {
        return workerDao.insert(worker)
    }

    /**
     * Update existing worker
     */
    suspend fun updateWorker(worker: Worker) {
        workerDao.update(worker)
    }

    /**
     * Delete worker
     */
    suspend fun deleteWorker(worker: Worker) {
        workerDao.delete(worker)
    }

    /**
     * Deactivate worker
     */
    suspend fun deactivateWorker(workerId: Int) {
        workerDao.updateWorkerStatus(workerId, false)
    }

    /**
     * Activate worker
     */
    suspend fun activateWorker(workerId: Int) {
        workerDao.updateWorkerStatus(workerId, true)
    }

    /**
     * Search workers by name or phone
     */
    fun searchWorkers(query: String): Flow<List<Worker>> {
        return workerDao.searchWorkers("%$query%")
    }
}
