package com.palmfarm.manager.data.repository

import com.palmfarm.manager.data.database.dao.TaskDao
import com.palmfarm.manager.data.database.entities.Task
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Task operations
 */
class TaskRepository(
    private val taskDao: TaskDao
) {

    /**
     * Get all tasks
     */
    fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
    }

    /**
     * Get task by ID
     */
    fun getTaskById(taskId: Int): Flow<Task?> {
        return taskDao.getTaskByIdFlow(taskId)
    }

    /**
     * Get tasks by worker ID
     */
    fun getTasksByWorker(workerId: Int): Flow<List<Task>> {
        return taskDao.getTasksByWorker(workerId)
    }

    /**
     * Get tasks by category
     */
    fun getTasksByCategory(category: String): Flow<List<Task>> {
        return taskDao.getTasksByCategory(category)
    }

    /**
     * Get tasks by status
     */
    fun getTasksByStatus(status: String): Flow<List<Task>> {
        return taskDao.getTasksByStatus(status)
    }

    /**
     * Get pending tasks
     */
    fun getPendingTasks(): Flow<List<Task>> {
        return taskDao.getTasksByStatus("IN_PROGRESS")
    }

    /**
     * Get completed tasks
     */
    fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getTasksByStatus("COMPLETED")
    }

    /**
     * Insert new task
     */
    suspend fun insertTask(task: Task): Long {
        return taskDao.insert(task)
    }

    /**
     * Update existing task
     */
    suspend fun updateTask(task: Task) {
        taskDao.update(task)
    }

    /**
     * Delete task
     */
    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
    }

    /**
     * Delete task by ID
     */
    suspend fun deleteTaskById(taskId: Int) {
        taskDao.deleteById(taskId)
    }

    /**
     * Update task status
     */
    suspend fun updateTaskStatus(taskId: Int, status: String) {
        val task = taskDao.getTaskById(taskId)
        task?.let {
            taskDao.update(it.copy(status = status, updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Get tasks for a date range
     */
    fun getTasksBetweenDates(startDate: Long, endDate: Long): Flow<List<Task>> {
        return taskDao.getTasksBetweenDates(startDate, endDate)
    }

    /**
     * Get recent tasks (last 5)
     */
    fun getRecentTasks(limit: Int = 5): Flow<List<Task>> {
        return taskDao.getRecentTasks(limit)
    }
}
