package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

/**
 * FixedCost entity - Stores fixed costs with depreciation tracking
 */
@Entity(tableName = "fixed_costs")
data class FixedCost(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "date")
    val date: Long, // Purchase/start date timestamp

    @ColumnInfo(name = "amount")
    val amount: Double, // XAF (total cost)

    @ColumnInfo(name = "payment_type")
    val paymentType: String, // Values: "RENT", "PURCHASE"

    @ColumnInfo(name = "category")
    val category: String, // Values: "LAND", "EQUIPMENT"

    @ColumnInfo(name = "item_name")
    val itemName: String,

    @ColumnInfo(name = "life_span_years")
    val lifeSpanYears: Int,

    @ColumnInfo(name = "monthly_depreciation")
    val monthlyDepreciation: Double, // Computed: amount / (lifeSpanYears × 12)

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculate used value based on months elapsed since purchase date
     */
    @Ignore
    fun calculateUsedValue(): Double {
        val monthsElapsed = getMonthsElapsed()
        return monthlyDepreciation * monthsElapsed
    }

    /**
     * Calculate value left
     */
    @Ignore
    fun calculateValueLeft(): Double {
        val used = calculateUsedValue()
        return maxOf(0.0, amount - used)
    }

    /**
     * Check if fully depreciated
     */
    @Ignore
    fun isFullyDepreciated(): Boolean {
        return calculateValueLeft() <= 0.0
    }

    /**
     * Get months elapsed since purchase date
     */
    @Ignore
    private fun getMonthsElapsed(): Int {
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - date
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        return (diffInDays / 30).toInt() // Approximate month as 30 days
    }
}
