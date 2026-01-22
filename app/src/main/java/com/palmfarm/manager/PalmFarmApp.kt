package com.palmfarm.manager

import android.app.Application
import com.palmfarm.manager.data.database.AppDatabase
import com.palmfarm.manager.data.production.ProductionCycleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class for Palm Farm Manager
 * Initializes the database and other app-wide dependencies
 */
class PalmFarmApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var productionCycleManager: ProductionCycleManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize database
        database = AppDatabase.getDatabase(this)
        productionCycleManager = ProductionCycleManager(database)

        // Check and create production cycle if needed
        scheduleProductionCycleCheck()
    }

    override fun onTerminate() {
        super.onTerminate()
        AppDatabase.closeDatabase()
    }

    /**
     * Check if current production cycle has ended and create new one if needed
     */
    fun scheduleProductionCycleCheck() {
        applicationScope.launch {
            productionCycleManager.ensureActiveCycle()
        }
    }

    fun scheduleCycleReconfiguration(startMonth: Int) {
        applicationScope.launch {
            productionCycleManager.applyStartMonthChange(startMonth)
        }
    }

    companion object {
        private lateinit var instance: PalmFarmApp

        fun getInstance(): PalmFarmApp {
            return instance
        }
    }
}