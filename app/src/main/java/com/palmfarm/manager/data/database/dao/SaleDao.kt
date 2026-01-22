package com.palmfarm.manager.data.database.dao

import androidx.room.*
import com.palmfarm.manager.data.database.entities.Sale
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Sale entity
 */
@Dao
interface SaleDao {

    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE cycle_id = :cycleId ORDER BY date DESC")
    fun getSalesByCycle(cycleId: Int): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: Int): Sale?

    @Query("SELECT * FROM sales WHERE id = :saleId")
    fun getSaleByIdFlow(saleId: Int): Flow<Sale?>

    // Get last sale by unit
    @Query("SELECT * FROM sales WHERE unit = :unit ORDER BY date DESC LIMIT 1")
    fun getLastSaleByUnit(unit: String): Flow<Sale?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: Sale): Long

    @Update
    suspend fun update(sale: Sale)

    @Delete
    suspend fun delete(sale: Sale)

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteById(saleId: Int)

    // Get last sales price (for GALLON unit)
    @Query("SELECT unit_price FROM sales WHERE unit = 'GALLON' ORDER BY date DESC LIMIT 1")
    suspend fun getLastSalesPricePerGallon(): Double?

    @Query("SELECT unit_price FROM sales WHERE unit = 'GALLON' ORDER BY date DESC LIMIT 1")
    fun getLastSalesPricePerGallonFlow(): Flow<Double?>

    // Get highest sales price
    @Query("SELECT * FROM sales WHERE unit = 'GALLON' ORDER BY unit_price DESC LIMIT 1")
    suspend fun getHighestPriceSale(): Sale?

    // Get lowest sales price
    @Query("SELECT * FROM sales WHERE unit = 'GALLON' ORDER BY unit_price ASC LIMIT 1")
    suspend fun getLowestPriceSale(): Sale?

    // Get total sales amount for cycle
    @Query("SELECT COALESCE(SUM(total_amount), 0.0) FROM sales WHERE cycle_id = :cycleId")
    suspend fun getTotalSalesAmountForCycle(cycleId: Int): Double

    @Query("SELECT COALESCE(SUM(total_amount), 0.0) FROM sales WHERE cycle_id = :cycleId")
    fun getTotalIncomeForCycle(cycleId: Int): Flow<Double>

    @Query("SELECT COALESCE(SUM(total_amount), 0.0) FROM sales")
    fun getTotalSalesAmount(): Flow<Double>

    @Query("SELECT COALESCE(SUM(total_amount), 0.0) FROM sales WHERE cycle_id = :cycleId")
    fun getTotalSalesAmountForCycleFlow(cycleId: Int): Flow<Double>

    // Get total gallons sold (all time)
    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM sales WHERE unit = 'GALLON'")
    suspend fun getTotalGallonsSold(): Double

    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM sales WHERE unit = 'GALLON'")
    fun getTotalOilSold(): Flow<Double>

    // Get total gallons sold for cycle
    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM sales WHERE cycle_id = :cycleId AND unit = 'GALLON'")
    suspend fun getTotalGallonsSoldForCycle(cycleId: Int): Double

    // Get recent sales (limit)
    @Query("SELECT * FROM sales ORDER BY date DESC LIMIT :limit")
    fun getRecentSales(limit: Int): Flow<List<Sale>>

    // Get sales count for cycle
    @Query("SELECT COUNT(*) FROM sales WHERE cycle_id = :cycleId")
    suspend fun getSalesCountForCycle(cycleId: Int): Int

    // Get total bunches sold for cycle
    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM sales WHERE cycle_id = :cycleId AND unit = 'BUNCH'")
    suspend fun getTotalBunchesSoldForCycle(cycleId: Int): Double
    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM sales WHERE cycle_id = :cycleId AND unit = 'BUNCH'")
    fun getTotalBunchesSoldForCycleFlow(cycleId: Int): Flow<Double>
    // Get total tonnes sold for cycle (for conversion to bunches)
    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM sales WHERE cycle_id = :cycleId AND unit = 'TONNE'")
    suspend fun getTotalTonnesSoldForCycle(cycleId: Int): Double
    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM sales WHERE cycle_id = :cycleId AND unit = 'TONNE'")
    fun getTotalTonnesSoldForCycleFlow(cycleId: Int): Flow<Double>
    // Get total quantity sold for all units (for backwards compatibility)
    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM sales")
    fun getTotalQuantitySold(): Flow<Double>
}
