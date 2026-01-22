package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Farm entity - Stores farm information
 */
@Entity(tableName = "farms")
data class Farm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "size_in_hectares")
    val sizeInHectares: Double,

    @ColumnInfo(name = "productive_palms")
    val productivePalms: Int,

    @ColumnInfo(name = "unproductive_palms")
    val unproductivePalms: Int,

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Total palms (computed property)
     */
    @Ignore
    val totalPalms: Int = productivePalms + unproductivePalms
}
