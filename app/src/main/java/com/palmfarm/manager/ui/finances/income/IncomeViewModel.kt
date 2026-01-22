package com.palmfarm.manager.ui.finances.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.palmfarm.manager.data.database.dao.ConsumptionDao
import com.palmfarm.manager.data.database.dao.SaleDao
import com.palmfarm.manager.data.database.entities.Consumption
import com.palmfarm.manager.data.database.entities.Sale
import com.palmfarm.manager.data.repository.ProductionCycleRepository
import com.palmfarm.manager.data.repository.ProductionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Income management (Sales and Consumption)
 */
class IncomeViewModel(
    private val saleDao: SaleDao,
    private val consumptionDao: ConsumptionDao,
    private val productionRepository: ProductionRepository,
    private val cycleRepository: ProductionCycleRepository
) : ViewModel() {

    private val currentCycleId = cycleRepository.getCurrentCycle()
        .map { it?.id ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /**
     * Get all sales
     */
    val sales: StateFlow<List<Sale>> = saleDao.getAllSales()
        .map { sales -> sales.sortedByDescending { it.date } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Get all consumption
     */
    val consumption: StateFlow<List<Consumption>> = consumptionDao.getAllConsumption()
        .map { consumption -> consumption.sortedByDescending { it.date } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Get income metrics
     */
    val incomeMetrics: StateFlow<IncomeMetrics> = currentCycleId
        .flatMapLatest { cycleId ->
            val bunchesFlow = if (cycleId > 0) {
                productionRepository.calculateBunchesAvailableFlow(cycleId)
            } else {
                flowOf(0)
            }

            val salesFlow = combine(
                saleDao.getTotalSalesAmount(),
                saleDao.getTotalOilSold()
            ) { totalSales, quantitySold ->
                totalSales to quantitySold
            }

            val consumptionFlow = combine(
                consumptionDao.getTotalConsumptionValue(),
                consumptionDao.getTotalOilConsumed()
            ) { totalConsumption, quantityConsumed ->
                totalConsumption to quantityConsumed
            }

            combine(
                salesFlow,
                consumptionFlow,
                productionRepository.oilStockFlow(),
                bunchesFlow,
                saleDao.getLastSalesPricePerGallonFlow()
            ) { salesPair, consumptionPair, oilStock, bunchesAvailable, lastSalesPrice ->
                val (totalSales, quantitySold) = salesPair
                val (totalConsumption, quantityConsumed) = consumptionPair
                IncomeMetrics(
                    totalSales = totalSales,
                    quantitySold = quantitySold,
                    totalConsumption = totalConsumption,
                    quantityConsumed = quantityConsumed,
                    totalIncome = totalSales + totalConsumption,
                    oilStock = oilStock,
                    bunchesAvailable = bunchesAvailable,
                    lastSalesPrice = lastSalesPrice ?: 0.0
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, IncomeMetrics())

    /**
     * Save sale
     */
    suspend fun saveSale(sale: Sale): Result<Unit> {
        return try {
            // Validate stock availability
            val validation = validateSale(sale)
            if (!validation.isValid) {
                return Result.failure(Exception(validation.message))
            }

            saleDao.insert(sale)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate sale against available stock
     */
    private suspend fun validateSale(sale: Sale): ValidationResult {
        val cycleId = currentCycleId.value
        val oilStock = productionRepository.calculateOilStock()
        val bunchesAvailable = productionRepository.calculateBunchesAvailable(cycleId)

        return when (sale.unit) {
            "GALLON" -> {
                if (sale.quantity > oilStock) {
                    ValidationResult(
                        isValid = false,
                        message = "Insufficient oil stock. Available: $oilStock gallons"
                    )
                } else {
                    ValidationResult(isValid = true, message = "Valid")
                }
            }
            "BUNCH" -> {
                if (sale.quantity > bunchesAvailable) {
                    ValidationResult(
                        isValid = false,
                        message = "Insufficient bunches. Available: $bunchesAvailable bunches"
                    )
                } else {
                    ValidationResult(isValid = true, message = "Valid")
                }
            }
            "TONNE" -> {
                // 1 tonne = multiple bunches (assume tonnage from settings)
                // For now, just allow
                ValidationResult(isValid = true, message = "Valid")
            }
            else -> ValidationResult(isValid = false, message = "Invalid unit")
        }
    }

    /**
     * Save consumption
     */
    suspend fun saveConsumption(consumption: Consumption): Result<Unit> {
        return try {
            // Validate oil stock
            val oilStock = productionRepository.calculateOilStock()
            if (consumption.quantityGallons > oilStock) {
                return Result.failure(Exception("Insufficient oil stock. Available: $oilStock gallons"))
            }

            consumptionDao.insert(consumption)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete sale
     */
    fun deleteSale(saleId: Int) {
        viewModelScope.launch {
            saleDao.deleteById(saleId)
        }
    }

    /**
     * Delete consumption
     */
    fun deleteConsumption(consumptionId: Int) {
        viewModelScope.launch {
            consumptionDao.deleteById(consumptionId)
        }
    }
}

/**
 * Data class for income metrics
 */
data class IncomeMetrics(
    val totalSales: Double = 0.0,
    val quantitySold: Double = 0.0,
    val totalConsumption: Double = 0.0,
    val quantityConsumed: Double = 0.0,
    val totalIncome: Double = 0.0,
    val oilStock: Double = 0.0,
    val bunchesAvailable: Int = 0,
    val lastSalesPrice: Double = 0.0
)

/**
 * Data class for validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)
