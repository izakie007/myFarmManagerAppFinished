package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProductionCycle entity - Stores production cycle metadata
 * A production cycle runs from season start month to the same month next year
 */
@Entity(
    tableName = "production_cycles",
    indices = [
        Index(value = ["is_current"]),
        Index(value = ["start_date", "end_date"]),
        Index(value = ["cycle_name"], unique = true)
    ]
)
data class ProductionCycle(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "cycle_name")
    val cycleName: String, // e.g., "2024-2025"

    @ColumnInfo(name = "start_date")
    val startDate: Long, // Timestamp of cycle start

    @ColumnInfo(name = "end_date")
    val endDate: Long, // Timestamp of cycle end

    @ColumnInfo(name = "expected_bunches")
    val expectedBunches: Int, // Expected bunches for this cycle

    @ColumnInfo(name = "realized_bunches")
    val realizedBunches: Int, // Actual bunches harvested in this cycle

    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean, // Only one cycle should be current

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(cycleName.isNotBlank()) { "Cycle name required" }
        require(cycleName.length <= 100) { "Cycle name too long" }
        require(expectedBunches > 0) { "Expected bunches must be positive" }
        require(realizedBunches >= 0) { "Realized bunches cannot be negative" }
        require(endDate > startDate) { "Invalid date range" }
        require(createdAt > 0) { "Created timestamp required" }
    }
}
