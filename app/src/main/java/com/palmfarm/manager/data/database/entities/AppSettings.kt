package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AppSettings entity - Stores application-wide settings
 * This is a singleton table with only one record (id always = 1)
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1, // Always 1 for singleton

    @ColumnInfo(name = "enterprise_name")
    val enterpriseName: String?, // No default, nullable

    @ColumnInfo(name = "location")
    val location: String?, // No default, nullable

    @ColumnInfo(name = "enterprise_phone")
    val enterprisePhone: String?,  // New field, nullable

    @ColumnInfo(name = "tonnage")
    val tonnage: Double, // No default (Kotlin requires init, but no value set)

    @ColumnInfo(name = "auth_method")
    val authMethod: String, // No default

    @ColumnInfo(name = "password_hash")
    val passwordHash: String?, // No default, nullable

    @ColumnInfo(name = "season_start_month")
    val seasonStartMonth: Int, // No default

    @ColumnInfo(name = "current_cycle_expected_bunches")
    val currentCycleExpectedBunches: Int, // No default

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)