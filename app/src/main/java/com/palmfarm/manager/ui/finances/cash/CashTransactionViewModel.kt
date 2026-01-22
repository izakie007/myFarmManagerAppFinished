package com.palmfarm.manager.ui.finances.cash

import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.dao.CashTransactionDao
import com.palmfarm.manager.data.database.entities.CashTransaction
import com.palmfarm.manager.ui.common.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Cash Transaction management
 */
class CashTransactionViewModel(
    private val cashTransactionDao: CashTransactionDao
) : BaseViewModel() {

    // All cash transactions
    private val _transactions = MutableStateFlow<List<CashTransaction>>(emptyList())
    val transactions: StateFlow<List<CashTransaction>> = _transactions.asStateFlow()

    // Current cash balance
    private val _currentBalance = MutableStateFlow(0.0)
    val currentBalance: StateFlow<Double> = _currentBalance.asStateFlow()

    // Cash flow summary
    private val _cashFlowSummary = MutableStateFlow<CashFlowSummary?>(null)
    val cashFlowSummary: StateFlow<CashFlowSummary?> = _cashFlowSummary.asStateFlow()

    init {
        loadTransactions()
        loadBalance()
        loadCashFlowSummary()
    }

    /**
     * Load all cash transactions
     */
    fun loadTransactions() {
        viewModelScope.launch {
            try {
                showLoading()
                cashTransactionDao.getAllTransactions().collect { transactions ->
                    _transactions.value = transactions
                }
                hideLoading()
            } catch (e: Exception) {
                handleException(e, "Failed to load cash transactions")
            }
        }
    }

    /**
     * Load current cash balance
     */
    private fun loadBalance() {
        viewModelScope.launch {
            try {
                val balance = cashTransactionDao.getCurrentBalance() ?: 0.0
                _currentBalance.value = balance
            } catch (e: Exception) {
                handleException(e, "Failed to load cash balance")
            }
        }
    }

    /**
     * Load cash flow summary
     */
    private fun loadCashFlowSummary() {
        viewModelScope.launch {
            try {
                val summary = cashTransactionDao.getCashFlowSummary()
                _cashFlowSummary.value = CashFlowSummary(
                    cashIn = summary.cashIn,
                    cashOut = summary.cashOut,
                    netCashFlow = summary.netCashFlow
                )
            } catch (e: Exception) {
                handleException(e, "Failed to load cash flow summary")
            }
        }
    }

    /**
     * Insert new cash transaction
     */
    fun insertTransaction(transaction: CashTransaction) {
        viewModelScope.launch {
            try {
                showLoading()
                cashTransactionDao.insertTransaction(transaction)
                loadBalance()
                loadCashFlowSummary()
                showSuccess("Transaction recorded successfully")
                hideLoading()
            } catch (e: Exception) {
                handleException(e, "Failed to record transaction")
            }
        }
    }

    /**
     * Update existing transaction
     */
    fun updateTransaction(transaction: CashTransaction) {
        viewModelScope.launch {
            try {
                showLoading()
                cashTransactionDao.updateTransaction(transaction)
                loadBalance()
                loadCashFlowSummary()
                showSuccess("Transaction updated successfully")
                hideLoading()
            } catch (e: Exception) {
                handleException(e, "Failed to update transaction")
            }
        }
    }

    /**
     * Delete transaction
     */
    fun deleteTransaction(transactionId: Int) {
        viewModelScope.launch {
            try {
                showLoading()
                cashTransactionDao.deleteTransaction(transactionId)
                loadBalance()
                loadCashFlowSummary()
                showSuccess("Transaction deleted successfully")
                hideLoading()
            } catch (e: Exception) {
                handleException(e, "Failed to delete transaction")
            }
        }
    }

    /**
     * Get transaction by ID
     */
    suspend fun getTransactionById(transactionId: Int): CashTransaction? {
        return try {
            cashTransactionDao.getTransactionById(transactionId)
        } catch (e: Exception) {
            handleException(e, "Failed to load transaction")
            null
        }
    }

    /**
     * Get opening balance transaction
     */
    suspend fun getOpeningBalance(): CashTransaction? {
        return try {
            cashTransactionDao.getOpeningBalance()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if opening balance exists
     */
    suspend fun hasOpeningBalance(): Boolean {
        return try {
            cashTransactionDao.getOpeningBalance() != null
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Cash flow summary data class
 */
data class CashFlowSummary(
    val cashIn: Double,
    val cashOut: Double,
    val netCashFlow: Double
)
