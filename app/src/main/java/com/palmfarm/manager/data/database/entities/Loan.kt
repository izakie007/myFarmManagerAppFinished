package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Calendar

/**
 * Loan entity - Stores loan information with simple interest calculations
 */
@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "lender_name")
    val lenderName: String,

    @ColumnInfo(name = "purpose")
    val purpose: String? = null,

    @ColumnInfo(name = "principal")
    val principal: Double, // XAF

    @ColumnInfo(name = "interest_rate")
    val interestRate: Double, // Percentage (e.g., 10.0 for 10%)

    @ColumnInfo(name = "start_date")
    val startDate: Long, // Timestamp

    @ColumnInfo(name = "period_months")
    val periodMonths: Int,

    @ColumnInfo(name = "end_date")
    val endDate: Long, // Computed: startDate + periodMonths

    @ColumnInfo(name = "total_interest")
    val totalInterest: Double, // Computed: principal × (interestRate / 100)

    @ColumnInfo(name = "total_owed")
    val totalOwed: Double, // Computed: principal + totalInterest

    @ColumnInfo(name = "monthly_payment")
    val monthlyPayment: Double, // Computed: totalOwed / periodMonths

    @ColumnInfo(name = "number_of_payments_made")
    val numberOfPaymentsMade: Int = 0,

    @ColumnInfo(name = "total_paid")
    val totalPaid: Double, // Computed: numberOfPaymentsMade × monthlyPayment

    @ColumnInfo(name = "total_left")
    val totalLeft: Double, // Computed: totalOwed - totalPaid

    @ColumnInfo(name = "is_fully_paid")
    val isFullyPaid: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Calculate end date from start date and period
         */
        @Ignore
        fun calculateEndDate(startDate: Long, periodMonths: Int): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDate
            calendar.add(Calendar.MONTH, periodMonths)
            return calendar.timeInMillis
        }

        /**
         * Calculate total interest
         */
        @Ignore
        fun calculateTotalInterest(principal: Double, interestRate: Double): Double {
            return principal * (interestRate / 100.0)
        }

        /**
         * Calculate total owed
         */
        @Ignore
        fun calculateTotalOwed(principal: Double, totalInterest: Double): Double {
            return principal + totalInterest
        }

        /**
         * Calculate monthly payment
         */
        @Ignore
        fun calculateMonthlyPayment(totalOwed: Double, periodMonths: Int): Double {
            return if (periodMonths > 0) totalOwed / periodMonths else 0.0
        }
    }
}
