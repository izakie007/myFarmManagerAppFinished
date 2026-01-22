package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Expense entity - Stores operational expenses
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ProductionCycle::class,
            parentColumns = ["id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cycle_id"]),
        Index(value = ["category"]),
        Index(value = ["date"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "date")
    val date: Long, // Timestamp

    @ColumnInfo(name = "amount")
    val amount: Double, // XAF

    @ColumnInfo(name = "category")
    val category: String, // Values: "SUPPLIES", "MAINTENANCE", "FUEL", "TRANSPORT"

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "receipt_photo_path")
    val receiptPhotoPath: String? = null, // Local file path

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
