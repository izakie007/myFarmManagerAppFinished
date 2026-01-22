package com.palmfarm.manager.data.database.dao

import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.room.*
import com.palmfarm.manager.data.database.entities.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * DAO for AppSettings entity
 * Singleton table with only one record (id = 1)
 */
@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: AppSettings)

    @Update
    suspend fun update(settings: AppSettings)

    @Query("UPDATE app_settings SET enterprise_name = :name WHERE id = 1")
    suspend fun updateEnterpriseName(name: String?)

    @Query("UPDATE app_settings SET location = :location WHERE id = 1")
    suspend fun updateLocation(location: String?)

    @Query("UPDATE app_settings SET enterprise_phone = :phone WHERE id = 1")
    suspend fun updateEnterprisePhone(phone: String?)

    @Query("UPDATE app_settings SET tonnage = :tonnage WHERE id = 1")
    suspend fun updateTonnage(tonnage: Double)

    @Query("UPDATE app_settings SET auth_method = :method, password_hash = :hash WHERE id = 1")
    suspend fun updateAuthMethod(method: String, hash: String?)

    @Query("UPDATE app_settings SET season_start_month = :month WHERE id = 1")
    suspend fun updateSeasonStartMonth(month: Int)

    @Query("UPDATE app_settings SET current_cycle_expected_bunches = :bunches WHERE id = 1")
    suspend fun updateExpectedBunches(bunches: Int)

    @Query("SELECT auth_method FROM app_settings WHERE id = 1")
    suspend fun getAuthMethod(): String?

    @Query("SELECT password_hash FROM app_settings WHERE id = 1")
    suspend fun getPasswordHash(): String?
}
