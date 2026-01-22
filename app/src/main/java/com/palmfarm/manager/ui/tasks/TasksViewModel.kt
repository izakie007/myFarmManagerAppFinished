package com.palmfarm.manager.ui.tasks

import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.entities.Task
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.data.repository.TaskRepository
import com.palmfarm.manager.data.repository.WorkerRepository
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import com.palmfarm.manager.ui.common.BaseViewModel
import com.palmfarm.manager.utils.ValidationUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Tasks screen
 */
class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val workerRepository: WorkerRepository,
    private val cycleRepository: ProductionCycleRepository
) : BaseViewModel() {

    // Filter and sort options
    private val _filterCategory = MutableStateFlow<String?>(null)
    private val _filterStatus = MutableStateFlow<String?>(null)
    private val _filterWorker = MutableStateFlow<Int?>(null)
    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)

    // Tasks list
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // Active workers for dropdown
    private val _activeWorkers = MutableStateFlow<List<Worker>>(emptyList())
    val activeWorkers: StateFlow<List<Worker>> = _activeWorkers.asStateFlow()

    // Current filter/sort state
    val currentFilterCategory: StateFlow<String?> = _filterCategory.asStateFlow()
    val currentFilterStatus: StateFlow<String?> = _filterStatus.asStateFlow()
    val currentFilterWorker: StateFlow<Int?> = _filterWorker.asStateFlow()
    val currentSortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    init {
        loadTasks()
        loadActiveWorkers()
    }

    /**
     * Load tasks with current filter and sort options
     */
    private fun loadTasks() {
        viewModelScope.launch {
            try {
                // Get base flow based on filter
                val baseFlow = when {
                    _filterWorker.value != null -> {
                        taskRepository.getTasksByWorker(_filterWorker.value!!)
                    }
                    _filterCategory.value != null -> {
                        taskRepository.getTasksByCategory(_filterCategory.value!!)
                    }
                    _filterStatus.value != null -> {
                        taskRepository.getTasksByStatus(_filterStatus.value!!)
                    }
                    else -> {
                        taskRepository.getAllTasks()
                    }
                }

                // Collect and apply sorting
                baseFlow.collect { tasksList ->
                    _tasks.value = applySorting(tasksList, _sortOption.value)
                }
            } catch (e: Exception) {
                showError("Failed to load tasks: ${e.message}")
            }
        }
    }

    /**
     * Load active workers for dropdown
     */
    private fun loadActiveWorkers() {
        viewModelScope.launch {
            workerRepository.getActiveWorkers().collect { workers ->
                _activeWorkers.value = workers
            }
        }
    }

    /**
     * Apply sorting to task list
     */
    private fun applySorting(tasks: List<Task>, sortOption: SortOption): List<Task> {
        return when (sortOption) {
            SortOption.DATE_ASC -> tasks.sortedBy { it.startDate }
            SortOption.DATE_DESC -> tasks.sortedByDescending { it.startDate }
            SortOption.CATEGORY -> tasks.sortedBy { it.category }
            SortOption.STATUS -> tasks.sortedBy { it.status }
            SortOption.WORKER -> tasks.sortedBy { it.workerId }
        }
    }

    /**
     * Set filter by category
     */
    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
        loadTasks()
    }

    /**
     * Set filter by status
     */
    fun setFilterStatus(status: String?) {
        _filterStatus.value = status
        loadTasks()
    }

    /**
     * Set filter by worker
     */
    fun setFilterWorker(workerId: Int?) {
        _filterWorker.value = workerId
        loadTasks()
    }

    /**
     * Set sort option
     */
    fun setSortOption(sortOption: SortOption) {
        _sortOption.value = sortOption
        loadTasks()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _filterCategory.value = null
        _filterStatus.value = null
        _filterWorker.value = null
        loadTasks()
    }

    /**
     * Save task (insert or update)
     */
    fun saveTask(task: Task) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    // Validate task
                    val validationError = validateTask(task)
                    if (validationError != null) {
                        showError(validationError)
                        return@executeWithLoading
                    }

                    if (task.id == 0) {
                        // Insert new task
                        taskRepository.insertTask(task)
                        showSuccess("Task created successfully")
                    } else {
                        // Update existing task
                        taskRepository.updateTask(task)
                        showSuccess("Task updated successfully")
                    }
                } catch (e: Exception) {
                    showError("Failed to save task: ${e.message}")
                }
            }
        }
    }

    /**
     * Delete task
     */
    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    taskRepository.deleteTaskById(taskId)
                    showSuccess("Task deleted successfully")
                } catch (e: Exception) {
                    showError("Failed to delete task: ${e.message}")
                }
            }
        }
    }

    /**
     * Update task status
     */
    fun updateTaskStatus(taskId: Int, status: String) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    taskRepository.updateTaskStatus(taskId, status)
                    showSuccess("Task status updated")
                } catch (e: Exception) {
                    showError("Failed to update status: ${e.message}")
                }
            }
        }
    }

    /**
     * Validate task before saving
     */
    private fun validateTask(task: Task): String? {
        if (!ValidationUtils.isNotEmpty(task.description)) {
            return "Description is required"
        }

        if (!ValidationUtils.isNotEmpty(task.category)) {
            return "Category is required"
        }

        if (task.workerId == 0) {
            return "Worker is required"
        }

        // Note: Task entity only has 'quantity', not 'quantityCompleted'
        if (task.status == "Completed" && (task.quantity == null || task.quantity == 0.0)) {
            return "Completed tasks must have quantity specified"
        }

        // Check endDate is after startDate (endDate is nullable)
        task.endDate?.let { end ->
            if (task.startDate > end) {
                return "End date must be after start date"
            }
        }

        return null
    }

    /**
     * Get current cycle ID
     */
    suspend fun getCurrentCycleId(): Int {
        val currentCycle = cycleRepository.getCurrentCycle().first()
        return currentCycle?.id ?: 0
    }

    /**
     * Get task by ID
     */
    fun getTaskById(taskId: Int): Flow<Task?> {
        return taskRepository.getTaskById(taskId)
    }
}

/**
 * Sort options for tasks
 */
enum class SortOption {
    DATE_ASC,
    DATE_DESC,
    CATEGORY,
    STATUS,
    WORKER
}
