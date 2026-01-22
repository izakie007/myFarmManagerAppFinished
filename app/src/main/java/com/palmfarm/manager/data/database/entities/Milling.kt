package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Milling entity - Stores milling operation records
 */
@Entity(
    tableName = "millings",
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
            childColumns = ["miller_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cycle_id"]),
        Index(value = ["miller_id"])
    ]
)
data class Milling(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "miller_id")
    val millerId: Int,

    @ColumnInfo(name = "date")
    val date: Long, // Timestamp

    @ColumnInfo(name = "bunches_milled")
    val bunchesMilled: Int,

    @ColumnInfo(name = "drums_cooked")
    val drumsCooked: Double,

    @ColumnInfo(name = "oil_produced_gallons")
    val oilProducedGallons: Double, // Manual entry, stored in gallons

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
