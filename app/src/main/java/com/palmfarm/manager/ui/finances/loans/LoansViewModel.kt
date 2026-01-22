package com.palmfarm.manager.ui.finances.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.dao.LoanDao
import com.palmfarm.manager.data.database.entities.Loan
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for Loans management
 */
class LoansViewModel(
    private val loanDao: LoanDao
) : ViewModel() {

    /**
     * Get all active loans (not fully paid)
     */
    val activeLoans: StateFlow<List<Loan>> = loanDao.getActiveLoans()
        .map { loans -> loans.sortedByDescending { it.startDate } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Get all fully paid loans
     */
    val paidLoans: StateFlow<List<Loan>> = loanDao.getAllLoans()
        .map { loans ->
            loans.filter { it.isFullyPaid }.sortedByDescending { it.startDate }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Get loans summary
     */
    val loansSummary: StateFlow<LoansSummary> = activeLoans.map { loans ->
        LoansSummary(
            totalDebt = loans.sumOf { it.totalLeft },
            monthlyPayment = loans.sumOf { it.monthlyPayment },
            activeLoansCount = loans.size
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, LoansSummary())

    /**
     * Calculate loan details from input parameters
     */
    fun calculateLoanDetails(
        principal: Double,
        interestRate: Double,
        periodMonths: Int,
        startDate: Long
    ): LoanCalculation {
        // Simple interest calculation
        val totalInterest = principal * (interestRate / 100)
        val totalOwed = principal + totalInterest
        val monthlyPayment = totalOwed / periodMonths

        // Calculate end date
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startDate
            add(Calendar.MONTH, periodMonths)
        }
        val endDate = calendar.timeInMillis

        return LoanCalculation(
            totalInterest = totalInterest,
            totalOwed = totalOwed,
            monthlyPayment = monthlyPayment,
            endDate = endDate
        )
    }

    /**
     * Save loan
     */
    fun saveLoan(loan: Loan) {
        viewModelScope.launch {
            if (loan.id == 0) {
                loanDao.insert(loan)
            } else {
                loanDao.update(loan)
            }
        }
    }

    /**
     * Record loan payment
     */
    suspend fun recordLoanPayment(loanId: Int): Result<Unit> {
        return try {
            val loan = loanDao.getLoanById(loanId)
                ?: return Result.failure(Exception("Loan not found"))

            if (loan.isFullyPaid) {
                return Result.failure(Exception("Loan is already fully paid"))
            }

            val newPaymentsMade = loan.numberOfPaymentsMade + 1
            val newTotalLeft = loan.totalOwed - (loan.monthlyPayment * newPaymentsMade)
            val isFullyPaid = newTotalLeft <= 0

            val updatedLoan = loan.copy(
                numberOfPaymentsMade = newPaymentsMade,
                totalLeft = if (newTotalLeft < 0) 0.0 else newTotalLeft,
                isFullyPaid = isFullyPaid
            )

            loanDao.update(updatedLoan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete loan (only if no payments made)
     */
    suspend fun deleteLoan(loanId: Int): Result<Unit> {
        return try {
            val loan = loanDao.getLoanById(loanId)
                ?: return Result.failure(Exception("Loan not found"))

            if (loan.numberOfPaymentsMade > 0) {
                return Result.failure(Exception("Cannot delete loan with payments made"))
            }

            loanDao.deleteById(loanId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get loan by ID
     */
    fun getLoanById(loanId: Int): Flow<Loan?> {
        return loanDao.getLoanByIdFlow(loanId)
    }
}

/**
 * Data class for loan calculation
 */
data class LoanCalculation(
    val totalInterest: Double,
    val totalOwed: Double,
    val monthlyPayment: Double,
    val endDate: Long
)

/**
 * Data class for loans summary
 */
data class LoansSummary(
    val totalDebt: Double = 0.0,
    val monthlyPayment: Double = 0.0,
    val activeLoansCount: Int = 0
)
