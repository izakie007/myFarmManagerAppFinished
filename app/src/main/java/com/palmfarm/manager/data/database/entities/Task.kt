package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Task entity - Stores farm task assignments
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductionCycle::class,
            parentColumns = ["id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["worker_id"]),
        Index(value = ["cycle_id"])
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "worker_id")
    val workerId: Int,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "category")
    val category: String, // e.g., "Harvesting", "Maintenance", "Milling"

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "start_date")
    val startDate: Long, // Timestamp

    @ColumnInfo(name = "end_date")
    val endDate: Long? = null, // Timestamp, nullable

    @ColumnInfo(name = "quantity")
    val quantity: Double? = null, // Can be filled later

    @ColumnInfo(name = "unit")
    val unit: String? = null, // e.g., "bunches", "bags", "hectares"

    @ColumnInfo(name = "pay_rate")
    val payRate: Double, // XAF per unit

    @ColumnInfo(name = "status")
    val status: String, // Values: "IN_PROGRESS", "COMPLETED"

    @ColumnInfo(name = "paid_in_wage_payment_id")
    val paidInWagePaymentId: Int? = null, // Reference to wage payment if already paid

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
