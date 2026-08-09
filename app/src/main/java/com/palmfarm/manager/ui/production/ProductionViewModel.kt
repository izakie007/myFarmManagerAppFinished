package com.palmfarm.manager.ui.production

import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.entities.Harvest
import com.palmfarm.manager.data.database.entities.LooseNutsPicking
import com.palmfarm.manager.data.database.entities.Milling
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import com.palmfarm.manager.data.repository.ProductionRepository
import com.palmfarm.manager.data.repository.WorkerRepository
import com.palmfarm.manager.ui.common.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Production screen
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductionViewModel(
    private val productionRepository: ProductionRepository,
    private val cycleRepository: ProductionCycleRepository,
    private val workerRepository: WorkerRepository
) : BaseViewModel() {
    private val currentCycle = cycleRepository.getCurrentCycle()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val currentCycleIdFlow = currentCycle
        .map { it?.id ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val harvests: StateFlow<List<Harvest>> = currentCycle
        .filterNotNull()
        .flatMapLatest { cycle ->
            productionRepository.getHarvestsByCycle(cycle.id)
        }
        .map { harvests -> harvests.sortedByDescending { it.date } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val millings: StateFlow<List<Milling>> = currentCycle
        .filterNotNull()
        .flatMapLatest { cycle ->
            productionRepository.getMillingsByCycle(cycle.id)
        }
        .map { millings -> millings.sortedByDescending { it.date } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val looseNuts: StateFlow<List<LooseNutsPicking>> = currentCycle
        .filterNotNull()
        .flatMapLatest { cycle ->
            productionRepository.getLooseNutsByCycle(cycle.id)
        }
        .map { nuts -> nuts.sortedByDescending { it.date } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeWorkers: StateFlow<List<Worker>> = workerRepository.getActiveWorkers()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val productionMetrics: StateFlow<ProductionMetrics> = currentCycle
        .filterNotNull()
        .flatMapLatest { cycle ->
            val bunchesFlow = combine(
                productionRepository.totalBunchesMilledFlow(cycle.id),
                productionRepository.totalHarvestedFlow(cycle.id),
                productionRepository.calculateBunchesAvailableFlow(cycle.id)
            ) { totalMilled, totalHarvested, bunchesAvailable ->
                Triple(totalMilled, totalHarvested, bunchesAvailable)
            }

            val efficiencyFlow = combine(
                productionRepository.oilPerBunchFlow(cycle.id),
                productionRepository.oilPerDrumFlow(cycle.id),
                productionRepository.bunchesPerDrumFlow(cycle.id)
            ) { oilPerBunch, oilPerDrum, bunchesPerDrum ->
                Triple(oilPerBunch, oilPerDrum, bunchesPerDrum)
            }

            combine(
                bunchesFlow,
                efficiencyFlow,
                productionRepository.oilStockFlow()
            ) { bunchesTriple, efficiencyTriple, oilStock ->
                val (totalMilled, totalHarvested, bunchesAvailable) = bunchesTriple
                val (oilPerBunch, oilPerDrum, bunchesPerDrum) = efficiencyTriple

                ProductionMetrics(
                    bunchesMilled = totalMilled,
                    bunchesAvailable = bunchesAvailable,
                    oilPerBunch = oilPerBunch,
                    oilPerDrum = oilPerDrum,
                    bunchesPerDrum = bunchesPerDrum,
                    totalHarvested = totalHarvested,
                    oilStock = oilStock
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProductionMetrics())

    // ==================== HARVEST OPERATIONS ====================

    /**
     * Get next harvest number
     */
    suspend fun getNextHarvestNumber(): Int {
        val cycleId = currentCycleIdFlow.value
        return if (cycleId > 0) {
            productionRepository.getNextHarvestNumber(cycleId)
        } else {
            1
        }
    }

    /**
     * Add new harvest
     */
    fun addHarvest(harvest: Harvest) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    // Validate harvest
                    val validationError = validateHarvest(harvest)
                    if (validationError != null) {
                        showError(validationError)
                        return@executeWithLoading
                    }

                    productionRepository.insertHarvest(harvest)
                    showSuccess("Harvest recorded successfully")
                } catch (e: Exception) {
                    showError("Failed to add harvest: ${e.message}")
                }
            }
        }
    }

    /**
     * Update harvest
     */
    fun updateHarvest(harvest: Harvest) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    val validationError = validateHarvest(harvest)
                    if (validationError != null) {
                        showError(validationError)
                        return@executeWithLoading
                    }

                    productionRepository.updateHarvest(harvest)
                    showSuccess("Harvest updated successfully")
                } catch (e: Exception) {
                    showError("Failed to update harvest: ${e.message}")
                }
            }
        }
    }

    /**
     * Delete harvest
     */
    fun deleteHarvest(harvest: Harvest) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    productionRepository.deleteHarvest(harvest)
                    showSuccess("Harvest deleted successfully")
                } catch (e: Exception) {
                    showError("Failed to delete harvest: ${e.message}")
                }
            }
        }
    }

    /**
     * Validate harvest
     */
    private fun validateHarvest(harvest: Harvest): String? {
        if (harvest.harvesterId == 0) {
            return "Worker is required"
        }

        if (harvest.numberOfBunches <= 0) {
            return "Number of bunches must be greater than 0"
        }

        if (harvest.date > System.currentTimeMillis()) {
            return "Harvest date cannot be in the future"
        }

        return null
    }

    // ==================== MILLING OPERATIONS ====================

    /**
     * Add new milling
     */
    fun addMilling(milling: Milling) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    // Validate milling
                    val validationError = validateMilling(milling)
                    if (validationError != null) {
                        showError(validationError)
                        return@executeWithLoading
                    }

                    // Check if bunches available is sufficient
                    val cycleId = currentCycleIdFlow.value
                    val bunchesAvailable = productionRepository.calculateBunchesAvailable(cycleId)
                    if (milling.bunchesMilled > bunchesAvailable) {
                        showError("Cannot mill ${milling.bunchesMilled} bunches. Only $bunchesAvailable available.")
                        return@executeWithLoading
                    }

                    productionRepository.insertMilling(milling)
                    showSuccess("Milling recorded successfully")
                } catch (e: Exception) {
                    showError("Failed to add milling: ${e.message}")
                }
            }
        }
    }

    /**
     * Update milling
     */
    fun updateMilling(milling: Milling) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    val validationError = validateMilling(milling)
                    if (validationError != null) {
                        showError(validationError)
                        return@executeWithLoading
                    }

                    val existing = productionRepository.getMillingById(milling.id).first()
                    val cycleId = milling.cycleId
                    val bunchesAvailable = productionRepository.calculateBunchesAvailable(cycleId)
                    val effectiveAvailable = bunchesAvailable + (existing?.bunchesMilled ?: 0)
                    if (milling.bunchesMilled > effectiveAvailable) {
                        showError("Cannot mill ${milling.bunchesMilled} bunches. Only $effectiveAvailable available.")
                        return@executeWithLoading
                    }

                    productionRepository.updateMilling(milling)
                    showSuccess("Milling updated successfully")
                } catch (e: Exception) {
                    showError("Failed to update milling: ${e.message}")
                }
            }
        }
    }

    /**
     * Delete milling
     */
    fun deleteMilling(milling: Milling) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    productionRepository.deleteMilling(milling)
                    showSuccess("Milling deleted successfully")
                } catch (e: Exception) {
                    showError("Failed to delete milling: ${e.message}")
                }
            }
        }
    }

    /**
     * Validate milling
     */
    private fun validateMilling(milling: Milling): String? {
        if (milling.millerId == 0) {
            return "Worker is required"
        }

        if (milling.bunchesMilled <= 0) {
            return "Bunches milled must be greater than 0"
        }

        if (milling.drumsCooked <= 0) {
            return "Drums cooked must be greater than 0"
        }

        if (milling.oilProducedGallons <= 0.0) {
            return "Oil produced must be greater than 0"
        }

        if (milling.date > System.currentTimeMillis()) {
            return "Milling date cannot be in the future"
        }

        return null
    }

    // ==================== LOOSE NUTS OPERATIONS ====================

    /**
     * Add new loose nuts picking
     */
    fun addLooseNuts(looseNuts: LooseNutsPicking) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    val validationError = validateLooseNuts(looseNuts)
                    if (validationError != null) {
                        showError(validationError)
                        return@executeWithLoading
                    }

                    productionRepository.insertLooseNuts(looseNuts)
                    showSuccess("Loose nuts recorded successfully")
                } catch (e: Exception) {
                    showError("Failed to add loose nuts: ${e.message}")
                }
            }
        }
    }

    /**
     * Update loose nuts picking
     */
    fun updateLooseNuts(looseNuts: LooseNutsPicking) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    val validationError = validateLooseNuts(looseNuts)
                    if (validationError != null) {
                        showError(validationError)
                        return@executeWithLoading
                    }

                    productionRepository.updateLooseNuts(looseNuts)
                    showSuccess("Loose nuts updated successfully")
                } catch (e: Exception) {
                    showError("Failed to update loose nuts: ${e.message}")
                }
            }
        }
    }

    /**
     * Delete loose nuts picking
     */
    fun deleteLooseNuts(looseNuts: LooseNutsPicking) {
        viewModelScope.launch {
            executeWithLoading {
                try {
                    productionRepository.deleteLooseNuts(looseNuts)
                    showSuccess("Loose nuts deleted successfully")
                } catch (e: Exception) {
                    showError("Failed to delete loose nuts: ${e.message}")
                }
            }
        }
    }

    /**
     * Validate loose nuts
     */
    private fun validateLooseNuts(looseNuts: LooseNutsPicking): String? {
        if (looseNuts.pickerId == 0) {
            return "Worker is required"
        }

        if (looseNuts.numberOfBags <= 0) {
            return "Number of bags must be greater than 0"
        }

        if (looseNuts.date > System.currentTimeMillis()) {
            return "Date cannot be in the future"
        }

        return null
    }

    /**
     * Get current cycle ID
     */
    fun getCurrentCycleId(): Int {
        return currentCycleIdFlow.value
    }

    fun getHarvestById(harvestId: Int): Flow<Harvest?> {
        return productionRepository.getHarvestById(harvestId)
    }

    fun getMillingById(millingId: Int): Flow<Milling?> {
        return productionRepository.getMillingById(millingId)
    }

    fun getLooseNutsById(looseNutsId: Int): Flow<LooseNutsPicking?> {
        return productionRepository.getLooseNutsById(looseNutsId)
    }
}

/**
 * Production metrics data class
 */
data class ProductionMetrics(
    val bunchesMilled: Int = 0,
    val bunchesAvailable: Int = 0,
    val oilPerBunch: Double = 0.0,
    val oilPerDrum: Double = 0.0,
    val bunchesPerDrum: Double = 0.0,
    val totalHarvested: Int = 0,
    val oilStock: Double = 0.0
)
