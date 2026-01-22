package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sale entity - Stores oil sales records
 */
@Entity(
    tableName = "sales",
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
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "date")
    val date: Long, // Timestamp

    @ColumnInfo(name = "quantity")
    val quantity: Double,

    @ColumnInfo(name = "unit")
    val unit: String, // Values: "GALLON", "TONNE", "BUNCH"

    @ColumnInfo(name = "unit_price")
    val unitPrice: Double, // XAF per unit

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double, // quantity × unitPrice

    @ColumnInfo(name = "buyer_name")
    val buyerName: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
