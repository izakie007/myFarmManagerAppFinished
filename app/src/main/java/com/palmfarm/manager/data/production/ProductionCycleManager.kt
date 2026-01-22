package com.palmfarm.manager.data.production

import androidx.room.withTransaction
import com.palmfarm.manager.data.database.AppDatabase
import com.palmfarm.manager.data.database.entities.AppSettings
import com.palmfarm.manager.data.database.entities.ProductionCycle
import com.palmfarm.manager.utils.DateUtils

/**
 * Centralized manager for production cycle lifecycle.
 *
 * Responsibilities:
 * - Ensure there is always at most one active cycle.
 * - Create a new cycle when none exists or when the previous one has expired.
 * - Realign the active cycle when the user changes the season start month.
 * - Normalise configuration values (e.g. expected bunches) to prevent crashes.
 */
class ProductionCycleManager(
    private val database: AppDatabase
) {

    companion object {
        private const val DEFAULT_START_MONTH = 10
    }

    /**
     * Ensure the database has a valid “current” production cycle that matches the
     * latest configuration in `app_settings`.
     *
     * - If no cycle exists, one is created from the current date / start month.
     * - If the current cycle ended, a fresh one is created that covers “today”.
     * - If configuration changed (start month / expected bunches), the active
     *   cycle is realigned without touching history.
     */
    suspend fun ensureActiveCycle() {
        database.withTransaction {
            val settings = getOrInitialiseSettings()
            val startMonth = normaliseStartMonth(settings)

            val expectedBunches = normaliseExpectedBunches(settings)
            val desired = buildDesiredCycleSnapshot(startMonth, expectedBunches)
            val dao = database.productionCycleDao()
            val current = dao.getCurrentCycleOnce()
            val now = System.currentTimeMillis()

            if (current == null) {
                createAndActivateCycle(dao, desired)
                return@withTransaction
            }

            if (now > current.endDate) {
                finaliseCurrentCycle(dao, current.id)
                createAndActivateCycle(dao, desired)
                return@withTransaction
            }

            val needsSpanUpdate =
                current.startDate != desired.startDate || current.endDate != desired.endDate
            val needsNameUpdate = current.cycleName != desired.cycleName
            val needsExpectedUpdate = current.expectedBunches != expectedBunches

            if (needsSpanUpdate || needsNameUpdate || needsExpectedUpdate || !current.isCurrent) {
                val updated = current.copy(
                    startDate = desired.startDate,
                    endDate = desired.endDate,
                    cycleName = desired.cycleName,
                    expectedBunches = expectedBunches,
                    isCurrent = true
                )
                dao.updateCycle(updated)
                dao.markAllCyclesAsNotCurrent()
                dao.markCycleAsCurrent(updated.id)
            }
        }
    }

    /**
     * Apply a user-driven change to the season start month.
     * The new month is persisted in settings (handled by callers) and then the
     * active cycle is realigned around that month.
     */
    suspend fun applyStartMonthChange(explicitStartMonth: Int) {
        if (explicitStartMonth !in 1..12) return
        database.withTransaction {
            val settings = getOrInitialiseSettings()
            val expectedBunches = normaliseExpectedBunches(settings)
            val desired = buildDesiredCycleSnapshot(explicitStartMonth, expectedBunches)
            val dao = database.productionCycleDao()
            val current = dao.getCurrentCycleOnce()

            if (current == null) {
                createAndActivateCycle(dao, desired)
                return@withTransaction
            }

            val updated = current.copy(
                startDate = desired.startDate,
                endDate = desired.endDate,
                cycleName = desired.cycleName,
                expectedBunches = expectedBunches,
                isCurrent = true
            )

            dao.updateCycle(updated)
            dao.markAllCyclesAsNotCurrent()
            dao.markCycleAsCurrent(updated.id)
        }
    }

    // region helpers

    private suspend fun getOrInitialiseSettings(): AppSettings {
        val dao = database.appSettingsDao()
        val settings = dao.getSettingsOnce()
        if (settings != null) return settings

        val fallback = AppSettings(
            id = 1,
            enterpriseName = "",
            location = "",
            enterprisePhone = "",
            tonnage = 0.0,
            authMethod = "NONE",
            passwordHash = null,
            seasonStartMonth = DEFAULT_START_MONTH,
            currentCycleExpectedBunches = 0
        )
        dao.insert(fallback)
        return fallback
    }

    private suspend fun normaliseExpectedBunches(settings: AppSettings): Int {
        val expected = settings.currentCycleExpectedBunches.coerceAtLeast(1)
        if (expected != settings.currentCycleExpectedBunches) {
            val updated = settings.copy(
                currentCycleExpectedBunches = expected,
                updatedAt = System.currentTimeMillis()
            )
            database.appSettingsDao().insert(updated)
        }
        return expected
    }

    private suspend fun normaliseStartMonth(settings: AppSettings): Int {
        val startMonth = settings.seasonStartMonth.takeIf { it in 1..12 } ?: DEFAULT_START_MONTH
        if (startMonth != settings.seasonStartMonth) {
            val updated = settings.copy(
                seasonStartMonth = startMonth,
                updatedAt = System.currentTimeMillis()
            )
            database.appSettingsDao().insert(updated)
        }
        return startMonth
    }

    private fun buildDesiredCycleSnapshot(
        seasonStartMonth: Int,
        expectedBunches: Int
    ): CycleSnapshot {
        val startDate = DateUtils.getCurrentCycleStartDate(seasonStartMonth)
        val endDate = DateUtils.calculateCycleEndDate(startDate)
        val cycleName = DateUtils.calculateCycleName(startDate)
        return CycleSnapshot(
            startDate = startDate,
            endDate = endDate,
            cycleName = cycleName,
            expectedBunches = expectedBunches
        )
    }

    private suspend fun createAndActivateCycle(
        dao: com.palmfarm.manager.data.database.dao.ProductionCycleDao,
        snapshot: CycleSnapshot
    ) {
        dao.markAllCyclesAsNotCurrent()
        val newCycle = ProductionCycle(
            cycleName = snapshot.cycleName,
            startDate = snapshot.startDate,
            endDate = snapshot.endDate,
            expectedBunches = snapshot.expectedBunches,
            realizedBunches = 0,
            isCurrent = true
        )
        val newId = dao.insertCycle(newCycle).toInt()
        dao.markCycleAsCurrent(newId)
    }

    private suspend fun finaliseCurrentCycle(
        dao: com.palmfarm.manager.data.database.dao.ProductionCycleDao,
        cycleId: Int
    ) {
        dao.markCycleAsNotCurrent(cycleId)
    }

    private data class CycleSnapshot(
        val startDate: Long,
        val endDate: Long,
        val cycleName: String,
        val expectedBunches: Int
    )

    // endregion
}

