package com.palmfarm.manager.ui.finances.expenses

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentExpensesBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Fragment for managing expenses
 */
class ExpensesFragment : BaseFragment<FragmentExpensesBinding>() {

    private val viewModel: ExpensesViewModel by viewModels { ViewModelFactory.create() }
    private lateinit var adapter: ExpenseAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentExpensesBinding {
        return FragmentExpensesBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupFab()
        setupMenu()
        setupFilterChips()
    }

    override fun setupObservers() {
        observeExpenses()
        observeTotalExpenses()
    }

    /**
     * Setup RecyclerView with adapter
     */
    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            onItemClick = { expense ->
                // Navigate to edit
                val bundle = android.os.Bundle().apply {
                    putInt("expenseId", expense.id)
                }
                findNavController().navigate(R.id.action_expenses_to_addEditExpense, bundle)
            },
            onDeleteClick = { expense ->
                showDeleteConfirmation(expense)
            }
        )

        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@ExpensesFragment.adapter
        }
    }

    /**
     * Setup FAB for adding expenses
     */
    private fun setupFab() {
        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_expenses_to_addEditExpense)
        }
    }

    /**
     * Setup toolbar menu
     */
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_expenses, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filter_period -> {
                        showPeriodFilterDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Setup filter chips for categories
     */
    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            viewModel.setFilterCategory(null)
            updateChipSelection(binding.chipAll.id)
        }

        binding.chipSupplies.setOnClickListener {
            viewModel.setFilterCategory("Supplies")
            updateChipSelection(binding.chipSupplies.id)
        }

        binding.chipMaintenance.setOnClickListener {
            viewModel.setFilterCategory("Maintenance")
            updateChipSelection(binding.chipMaintenance.id)
        }

        binding.chipFuel.setOnClickListener {
            viewModel.setFilterCategory("Fuel")
            updateChipSelection(binding.chipFuel.id)
        }

        binding.chipTransport.setOnClickListener {
            viewModel.setFilterCategory("Transport")
            updateChipSelection(binding.chipTransport.id)
        }

        // Default: All selected
        binding.chipAll.isChecked = true
    }

    /**
     * Update chip selection
     */
    private fun updateChipSelection(selectedChipId: Int) {
        binding.chipAll.isChecked = selectedChipId == binding.chipAll.id
        binding.chipSupplies.isChecked = selectedChipId == binding.chipSupplies.id
        binding.chipMaintenance.isChecked = selectedChipId == binding.chipMaintenance.id
        binding.chipFuel.isChecked = selectedChipId == binding.chipFuel.id
        binding.chipTransport.isChecked = selectedChipId == binding.chipTransport.id
    }

    /**
     * Show period filter dialog
     */
    private fun showPeriodFilterDialog() {
        val periods = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_current_cycle),
            getString(R.string.filter_month),
            getString(R.string.filter_quarter),
            getString(R.string.filter_year)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_period)
            .setItems(periods) { _, which ->
                val period = when (which) {
                    0 -> FilterPeriod.ALL
                    1 -> FilterPeriod.CURRENT_CYCLE
                    2 -> FilterPeriod.MONTH
                    3 -> FilterPeriod.QUARTER
                    4 -> FilterPeriod.YEAR
                    else -> FilterPeriod.ALL
                }
                viewModel.setFilterPeriod(period)
            }
            .show()
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmation(expense: com.palmfarm.manager.data.database.entities.Expense) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_expense)
            .setMessage(R.string.delete_expense_confirmation)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteExpense(expense.id)
                showToast(getString(R.string.expense_deleted))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Observe expenses list
     */
    private fun observeExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expenses.collect { expenses ->
                adapter.submitList(expenses)

                // Show empty state
                if (expenses.isEmpty()) {
                    binding.rvExpenses.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvExpenses.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Observe total expenses
     */
    private fun observeTotalExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalExpenses.collect { total ->
                binding.tvTotalExpenses.text = CurrencyUtils.formatAmount(total)
            }
        }
    }
}
