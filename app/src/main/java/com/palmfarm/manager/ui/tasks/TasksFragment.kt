package com.palmfarm.manager.ui.tasks

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentTasksBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.ui.tasks.adapters.TaskAdapter
import com.palmfarm.manager.utils.gone
import com.palmfarm.manager.utils.visible
import kotlinx.coroutines.launch

/**
 * Tasks fragment - manages farm tasks
 */
class TasksFragment : BaseFragment<FragmentTasksBinding>() {

    private val viewModel: TasksViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var taskAdapter: TaskAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTasksBinding {
        return FragmentTasksBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupToolbar()
        setupRecyclerView()
        setupFab()
    }

    override fun setupObservers() {
        observeTasks()
        observeFilters()
        observeWorkers()
    }

    /**
     * Setup toolbar with menu
     */
    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_filter -> {
                    showFilterDialog()
                    true
                }
                R.id.action_sort -> {
                    showSortDialog()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                // Navigate to edit task with taskId
                val action = TasksFragmentDirections.actionTasksToAddEditTask(taskId = task.id)
                findNavController().navigate(action)
            },
            onTaskMenuClick = { task, view ->
                showTaskMenu(task, view)
            }
        )

        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    /**
     * Setup FAB to add new task
     */
    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            // Navigate to add task (taskId = 0 means new task)
            val action = TasksFragmentDirections.actionTasksToAddEditTask(taskId = 0)
            findNavController().navigate(action)
        }
    }

    /**
     * Observe tasks list
     */
    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tasks.collect { tasks ->
                if (tasks.isEmpty()) {
                    binding.rvTasks.gone()
                    binding.layoutEmptyState.visible()
                } else {
                    binding.rvTasks.visible()
                    binding.layoutEmptyState.gone()
                    taskAdapter.submitList(tasks)
                }
            }
        }
    }

    /**
     * Observe active filters
     */
    private fun observeFilters() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentFilterCategory.collect { category ->
                updateFilterChips()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentFilterStatus.collect { status ->
                updateFilterChips()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentFilterWorker.collect { workerId ->
                updateFilterChips()
            }
        }
    }

    /**
     * Observe workers to populate adapter worker map
     */
    private fun observeWorkers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeWorkers.collect { workers ->
                val workerMap = workers.associate { it.id to it.fullName }
                taskAdapter.updateWorkerMap(workerMap)
            }
        }
    }

    /**
     * Update filter chips display
     */
    private fun updateFilterChips() {
        val hasFilters = viewModel.currentFilterCategory.value != null ||
                viewModel.currentFilterStatus.value != null ||
                viewModel.currentFilterWorker.value != null

        if (hasFilters) {
            binding.scrollFilters.visible()
            binding.chipClearFilters.setOnClickListener {
                viewModel.clearFilters()
            }
        } else {
            binding.scrollFilters.gone()
        }
    }

    /**
     * Show filter dialog
     */
    private fun showFilterDialog() {
        val categories = arrayOf(
            "All",
            getString(R.string.category_harvest),
            getString(R.string.category_weeding),
            getString(R.string.category_fertilizing),
            getString(R.string.category_pruning),
            getString(R.string.category_planting),
            getString(R.string.category_other)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_tasks)
            .setItems(categories) { _, which ->
                val category = if (which == 0) null else categories[which]
                viewModel.setFilterCategory(category)
            }
            .show()
    }

    /**
     * Show sort dialog
     */
    private fun showSortDialog() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_date_newest),
            getString(R.string.sort_by_date_oldest),
            getString(R.string.sort_by_category),
            getString(R.string.sort_by_status),
            getString(R.string.sort_by_worker)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_tasks)
            .setItems(sortOptions) { _, which ->
                val sortOption = when (which) {
                    0 -> SortOption.DATE_DESC
                    1 -> SortOption.DATE_ASC
                    2 -> SortOption.CATEGORY
                    3 -> SortOption.STATUS
                    4 -> SortOption.WORKER
                    else -> SortOption.DATE_DESC
                }
                viewModel.setSortOption(sortOption)
            }
            .show()
    }

    /**
     * Show task context menu
     */
    private fun showTaskMenu(task: com.palmfarm.manager.data.database.entities.Task, view: android.view.View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_task_item, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    val action = TasksFragmentDirections.actionTasksToAddEditTask(taskId = task.id)
                    findNavController().navigate(action)
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteTask(task)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Confirm task deletion
     */
    private fun confirmDeleteTask(task: com.palmfarm.manager.data.database.entities.Task) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_task)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deleteTask(task.id)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}
