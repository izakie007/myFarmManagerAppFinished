package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Harvest entity - Stores harvest records
 */
@Entity(
    tableName = "harvests",
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
            childColumns = ["harvester_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cycle_id"]),
        Index(value = ["harvester_id"])
    ]
)
data class Harvest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "harvest_number")
    val harvestNumber: Int, // Auto-incremented per cycle

    @ColumnInfo(name = "harvester_id")
    val harvesterId: Int,

    @ColumnInfo(name = "date")
    val date: Long, // Timestamp

    @ColumnInfo(name = "number_of_bunches")
    val numberOfBunches: Int,

    @ColumnInfo(name = "remarks")
    val remarks: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
