package com.palmfarm.manager.data.repository

import com.palmfarm.manager.data.database.dao.*
import com.palmfarm.manager.data.database.entities.Harvest
import com.palmfarm.manager.data.database.entities.Milling
import com.palmfarm.manager.data.database.entities.LooseNutsPicking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for Production operations (Harvest, Milling, Loose Nuts)
 */
class ProductionRepository(
    private val harvestDao: HarvestDao,
    private val millingDao: MillingDao,
    private val looseNutsDao: LooseNutsPickingDao,
    private val saleDao: SaleDao,
    private val consumptionDao: ConsumptionDao,
    private val productionCycleDao: ProductionCycleDao,
    private val appSettingsDao: AppSettingsDao
) {

    // ==================== HARVEST ====================

    /**
     * Get all harvests for current cycle
     */
    fun getAllHarvests(): Flow<List<Harvest>> {
        return harvestDao.getAllHarvests()
    }

    /**
     * Get harvest by ID
     */
    fun getHarvestById(harvestId: Int): Flow<Harvest?> {
        return harvestDao.getHarvestById(harvestId)
    }

    /**
     * Get harvests by cycle
     */
    fun getHarvestsByCycle(cycleId: Int): Flow<List<Harvest>> {
        return harvestDao.getHarvestsByCycle(cycleId)
    }

    /**
     * Get next harvest number for current cycle
     */
    suspend fun getNextHarvestNumber(cycleId: Int): Int {
        return harvestDao.getNextHarvestNumber(cycleId)
    }

    /**
     * Insert new harvest
     */
    suspend fun insertHarvest(harvest: Harvest): Long {
        val harvestId = harvestDao.insertHarvest(harvest)

        // Update cycle realized bunches
        updateCycleRealizedBunches(harvest.cycleId)

        return harvestId
    }

    /**
     * Update existing harvest
     */
    suspend fun updateHarvest(harvest: Harvest) {
        harvestDao.updateHarvest(harvest)

        // Recalculate cycle realized bunches
        updateCycleRealizedBunches(harvest.cycleId)
    }

    /**
     * Delete harvest
     */
    suspend fun deleteHarvest(harvest: Harvest) {
        harvestDao.deleteHarvest(harvest)

        // Recalculate cycle realized bunches
        updateCycleRealizedBunches(harvest.cycleId)
    }

    /**
     * Update cycle realized bunches based on harvests
     */
    private suspend fun updateCycleRealizedBunches(cycleId: Int) {
        val totalBunches = harvestDao.getTotalBunchesByCycle(cycleId).first()
        productionCycleDao.updateRealizedBunches(cycleId, totalBunches)
    }

    // ==================== MILLING ====================

    /**
     * Get all millings
     */
    fun getAllMillings(): Flow<List<Milling>> {
        return millingDao.getAllMillings()
    }

    /**
     * Get milling by ID
     */
    fun getMillingById(millingId: Int): Flow<Milling?> {
        return millingDao.getMillingById(millingId)
    }

    /**
     * Get millings by cycle
     */
    fun getMillingsByCycle(cycleId: Int): Flow<List<Milling>> {
        return millingDao.getMillingsByCycle(cycleId)
    }

    /**
     * Insert new milling
     */
    suspend fun insertMilling(milling: Milling): Long {
        return millingDao.insertMilling(milling)
    }

    /**
     * Update existing milling
     */
    suspend fun updateMilling(milling: Milling) {
        millingDao.updateMilling(milling)
    }

    /**
     * Delete milling
     */
    suspend fun deleteMilling(milling: Milling) {
        millingDao.deleteMilling(milling)
    }

    /**
     * Calculate bunches available for milling
     * = Total harvested - Total milled
     */
    suspend fun calculateBunchesAvailable(cycleId: Int): Int {
        val totalHarvested = harvestDao.getTotalBunchesByCycle(cycleId).first()
        val totalMilled = millingDao.getTotalBunchesMilledByCycle(cycleId).first()

        // Calculate total bunches sold directly
        val totalBunchesSold = saleDao.getTotalBunchesSoldForCycle(cycleId)
        // Convert tonnes to bunches
        val totalTonnesSold = saleDao.getTotalTonnesSoldForCycle(cycleId)
        val tonnageConversion = appSettingsDao.getSettingsOnce()?.tonnage ?: 0.0
        val tonnesAsBunches = if (tonnageConversion > 0) {
            totalTonnesSold * tonnageConversion
        } else {
            0.0
        }
        val totalSold = totalBunchesSold + tonnesAsBunches
        val result = (totalHarvested - totalMilled - totalSold).toInt().coerceAtLeast(0)
        // Debug logging - remove in production
        println("DEBUG: calculateBunchesAvailable for cycle $cycleId")
        println("  totalHarvested: $totalHarvested")
        println("  totalMilled: $totalMilled")
        println("  totalBunchesSold: $totalBunchesSold")
        println("  totalTonnesSold: $totalTonnesSold")
        println("  tonnageConversion: $tonnageConversion")
        println("  tonnesAsBunches: $tonnesAsBunches")
        println("  totalSold: $totalSold")
        println("  result: $result")
        return result
    }

    /**
     * Calculate bunches available for milling (Flow version for auto-refresh)
     * = Total harvested - Total milled - Total bunches sold - Total tonnes sold (converted to bunches)
     */
    fun calculateBunchesAvailableFlow(cycleId: Int): Flow<Int> {
        return combine(
            harvestDao.getTotalBunchesByCycle(cycleId),
            millingDao.getTotalBunchesMilledByCycle(cycleId),
            saleDao.getTotalBunchesSoldForCycleFlow(cycleId),
            saleDao.getTotalTonnesSoldForCycleFlow(cycleId),
            appSettingsDao.getSettings()
        ) { totalHarvested, totalMilled, totalBunchesSold, totalTonnesSold, appSettings ->
            val tonnageConversion = appSettings?.tonnage ?: 0.0
            val tonnesAsBunches = if (tonnageConversion > 0) {
                totalTonnesSold * tonnageConversion
            } else {
                0.0
            }
            val totalSold = totalBunchesSold + tonnesAsBunches
            (totalHarvested - totalMilled - totalSold).toInt().coerceAtLeast(0)
        }
    }

    /**
     * Reactive oil stock = produced - sold - consumed
     */
    fun oilStockFlow(): Flow<Double> {
        return combine(
            millingDao.getTotalOilProduced(),
            saleDao.getTotalOilSold(),
            consumptionDao.getTotalOilConsumed()
        ) { produced, sold, consumed ->
            produced - sold - consumed
        }
    }

    fun oilPerBunchFlow(cycleId: Int): Flow<Double> {
        return combine(
            millingDao.getTotalOilProducedByCycle(cycleId),
            millingDao.getTotalBunchesMilledByCycle(cycleId)
        ) { totalOil, totalBunches ->
            // Match Analytics calculation: oilProduced * 20 / bunchesMilled
            if (totalBunches > 0) totalOil * 20 / totalBunches.toDouble() else 0.0
        }
    }

    fun oilPerDrumFlow(cycleId: Int): Flow<Double> {
        return combine(
            millingDao.getTotalOilProducedByCycle(cycleId),
            millingDao.getTotalDrumsByCycle(cycleId)
        ) { totalOil, totalDrums ->
            if (totalDrums > 0) totalOil / totalDrums.toDouble() else 0.0
        }
    }

    fun bunchesPerDrumFlow(cycleId: Int): Flow<Double> {
        return combine(
            millingDao.getTotalBunchesMilledByCycle(cycleId),
            millingDao.getTotalDrumsByCycle(cycleId)
        ) { totalBunches, totalDrums ->
            if (totalDrums > 0) totalBunches.toDouble() / totalDrums.toDouble() else 0.0
        }
    }

    fun totalHarvestedFlow(cycleId: Int): Flow<Int> = harvestDao.getTotalBunchesByCycle(cycleId)

    fun totalBunchesMilledFlow(cycleId: Int): Flow<Int> = millingDao.getTotalBunchesMilledByCycle(cycleId)

    fun getRecentProductionActivitiesFlow(limit: Int = 5): Flow<List<ProductionActivity>> {
        return combine(
            harvestDao.getRecentHarvests(limit),
            millingDao.getRecentMillings(limit)
        ) { harvests, millings ->
            val harvestActivities = harvests.map { harvest ->
                ProductionActivity(
                    type = "Harvest",
                    date = harvest.date,
                    description = "Harvest #${harvest.harvestNumber}: ${harvest.numberOfBunches} bunches",
                    id = harvest.id
                )
            }
            val millingActivities = millings.map { milling ->
                ProductionActivity(
                    type = "Milling",
                    date = milling.date,
                    description = "Milled ${milling.bunchesMilled} bunches → ${milling.oilProducedGallons} gallons",
                    id = milling.id
                )
            }

            (harvestActivities + millingActivities)
                .sortedByDescending { it.date }
                .take(limit)
        }
    }

    /**
     * Validate if milling quantity is valid
     */
    suspend fun validateMillingQuantity(cycleId: Int, bunchesToMill: Int): Boolean {
        val available = calculateBunchesAvailable(cycleId)
        return bunchesToMill <= available
    }

    /**
     * Calculate current oil stock (in gallons)
     * = Total produced - Total sold - Total consumed
     */
    suspend fun calculateOilStock(): Double {
        val totalProduced = millingDao.getTotalOilProduced().first()
        val totalSold = saleDao.getTotalOilSold().first()
        val totalConsumed = consumptionDao.getTotalOilConsumed().first()
        return totalProduced - totalSold - totalConsumed
    }

    /**
     * Calculate oil per bunch (gallons per bunch)
     */
    suspend fun calculateOilPerBunch(cycleId: Int): Double {
        val totalMilled = millingDao.getTotalBunchesMilledByCycle(cycleId).first()
        val totalOil = millingDao.getTotalOilProducedByCycle(cycleId).first()
        return if (totalMilled > 0) {
            totalOil * 20 / totalMilled.toDouble()
        } else {
            0.0
        }
    }

    /**
     * Calculate oil per drum (gallons per drum)
     */
    suspend fun calculateOilPerDrum(cycleId: Int): Double {
        val totalDrums = millingDao.getTotalDrumsByCycle(cycleId).first()
        val totalOil = millingDao.getTotalOilProducedByCycle(cycleId).first()
        return if (totalDrums > 0) {
            totalOil / totalDrums.toDouble()
        } else {
            0.0
        }
    }

    /**
     * Calculate bunches per drum (gallons per drum)
     */
    suspend fun calculateBunchesPerDrum(cycleId: Int): Double {
        val totalDrums = millingDao.getTotalDrumsByCycle(cycleId).first()
        val totalMilled = millingDao.getTotalBunchesMilledByCycle(cycleId).first()
        return if (totalDrums > 0) {
            totalMilled / totalDrums.toDouble()
        } else {
            0.0
        }
    }

    // ==================== LOOSE NUTS ====================

    /**
     * Get all loose nuts pickings
     */
    fun getAllLooseNuts(): Flow<List<LooseNutsPicking>> {
        return looseNutsDao.getAllLooseNuts()
    }

    /**
     * Get loose nuts by ID
     */
    fun getLooseNutsById(looseNutsId: Int): Flow<LooseNutsPicking?> {
        return looseNutsDao.getLooseNutsById(looseNutsId)
    }

    /**
     * Get loose nuts by cycle
     */
    fun getLooseNutsByCycle(cycleId: Int): Flow<List<LooseNutsPicking>> {
        return looseNutsDao.getLooseNutsByCycle(cycleId)
    }

    /**
     * Insert new loose nuts record
     */
    suspend fun insertLooseNuts(looseNuts: LooseNutsPicking): Long {
        return looseNutsDao.insertLooseNuts(looseNuts)
    }

    /**
     * Update existing loose nuts record
     */
    suspend fun updateLooseNuts(looseNuts: LooseNutsPicking) {
        looseNutsDao.updateLooseNuts(looseNuts)
    }

    /**
     * Delete loose nuts record
     */
    suspend fun deleteLooseNuts(looseNuts: LooseNutsPicking) {
        looseNutsDao.deleteLooseNuts(looseNuts)
    }

    /**
     * Get recent production activities (last 5)
     */
    suspend fun getRecentProductionActivities(limit: Int = 5): List<ProductionActivity> {
        val activities = mutableListOf<ProductionActivity>()

        // Get recent harvests
        val harvests = harvestDao.getRecentHarvests(limit).first()
        harvests.forEach { harvest ->
            activities.add(
                ProductionActivity(
                    type = "Harvest",
                    date = harvest.date,
                    description = "Harvest #${harvest.harvestNumber}: ${harvest.numberOfBunches} bunches",
                    id = harvest.id
                )
            )
        }

        // Get recent millings
        val millings = millingDao.getRecentMillings(limit).first()
        millings.forEach { milling ->
            activities.add(
                ProductionActivity(
                    type = "Milling",
                    date = milling.date,
                    description = "Milled ${milling.bunchesMilled} bunches → ${milling.oilProducedGallons} gallons",
                    id = milling.id
                )
            )
        }

        // Sort by date descending and take limit
        return activities.sortedByDescending { it.date }.take(limit)
    }
}

/**
 * Data class for production activity display
 */
data class ProductionActivity(
    val type: String,
    val date: Long,
    val description: String,
    val id: Int
)
