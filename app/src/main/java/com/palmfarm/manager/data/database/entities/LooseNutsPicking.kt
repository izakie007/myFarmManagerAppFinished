package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * LooseNutsPicking entity - Stores loose nuts picking records
 */
@Entity(
    tableName = "loose_nuts_pickings",
    foreignKeys = [
        ForeignKey(
            entity = ProductionCycle::class,
            parentColumns = ["id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["id"],
            childColumns = ["picker_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cycle_id"]),
        Index(value = ["picker_id"])
    ]
)
data class LooseNutsPicking(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "picker_id")
    val pickerId: Int,

    @ColumnInfo(name = "date")
    val date: Long, // Timestamp

    @ColumnInfo(name = "number_of_bags")
    val numberOfBags: Double, // Can be fractional (e.g., 2.5)

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
