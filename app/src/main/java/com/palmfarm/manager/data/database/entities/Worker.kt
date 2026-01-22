package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Worker entity - Stores worker information
 */
@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String? = null,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String? = null,

    @ColumnInfo(name = "specialty")
    val specialty: String? = null, // Rabattage, Cutting, Milling, or custom

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true, // Soft delete flag

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Full name (computed property)
     */
    @Ignore
    val fullName: String = if (lastName != null) {
        "$firstName $lastName"
    } else {
        firstName
    }
}
