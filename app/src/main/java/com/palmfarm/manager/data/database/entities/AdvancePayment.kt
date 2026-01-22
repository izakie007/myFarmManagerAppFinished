package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AdvancePayment entity - Stores advance wage payments to workers
 */
@Entity(
    tableName = "advance_payments",
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
data class AdvancePayment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "worker_id")
    val workerId: Int,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "amount")
    val amount: Double, // XAF

    @ColumnInfo(name = "purpose")
    val purpose: String? = null,

    @ColumnInfo(name = "date")
    val date: Long, // Timestamp

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
