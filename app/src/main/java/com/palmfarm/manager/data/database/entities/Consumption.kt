package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Consumption entity - Stores oil consumption records
 */
@Entity(
    tableName = "consumptions",
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
        Index(value = ["date"])
    ]
)
data class Consumption(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "date")
    val date: Long, // Timestamp

    @ColumnInfo(name = "quantity_gallons")
    val quantityGallons: Double,

    @ColumnInfo(name = "purpose")
    val purpose: String? = null,

    @ColumnInfo(name = "valued_at_price")
    val valuedAtPrice: Double, // Last sales price at time of consumption

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
