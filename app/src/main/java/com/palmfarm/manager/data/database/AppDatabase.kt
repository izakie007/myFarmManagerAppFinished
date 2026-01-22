package com.palmfarm.manager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.palmfarm.manager.data.database.dao.*
import com.palmfarm.manager.data.database.entities.*

/**
 * Main Room Database for Palm Farm Manager App
 * Version 3 - Schema hash refresh to align Room schema with latest entities
 */
@Database(
    entities = [
        AppSettings::class,
        Farm::class,
        Worker::class,
        ProductionCycle::class,
        Task::class,
        Harvest::class,
        LooseNutsPicking::class,
        Milling::class,
        Sale::class,
        Consumption::class,
        Expense::class,
        FixedCost::class,
        Loan::class,
        AdvancePayment::class,
        WagePayment::class,
        CashTransaction::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    // DAO abstract methods
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun farmDao(): FarmDao
    abstract fun workerDao(): WorkerDao
    abstract fun productionCycleDao(): ProductionCycleDao
    abstract fun taskDao(): TaskDao
    abstract fun harvestDao(): HarvestDao
    abstract fun looseNutsPickingDao(): LooseNutsPickingDao
    abstract fun millingDao(): MillingDao
    abstract fun saleDao(): SaleDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun fixedCostDao(): FixedCostDao
    abstract fun loanDao(): LoanDao
    abstract fun advancePaymentDao(): AdvancePaymentDao
    abstract fun wagePaymentDao(): WagePaymentDao
    abstract fun cashTransactionDao(): CashTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "palm_farm_db"

        /**
         * Migration from version 1 to 2
         * Adds cash_transactions table for cash flow tracking
         * This migration is safe and preserves all existing data
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create cash_transactions table
                // Using IF NOT EXISTS for safety - won't fail if table already exists
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cash_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transaction_type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `reference_id` INTEGER NOT NULL,
                        `reference_type` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // Note: All existing tables and data remain unchanged
            }
        }

        /**
         * Migration from version 2 to 3
         * Converts millings.drums_cooked from INTEGER to REAL and rebuilds the table.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `millings_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cycle_id` INTEGER NOT NULL,
                        `miller_id` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `bunches_milled` INTEGER NOT NULL,
                        `drums_cooked` REAL NOT NULL,
                        `oil_produced_gallons` REAL NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY(`cycle_id`) REFERENCES `production_cycles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                        FOREIGN KEY(`miller_id`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `millings_new` (
                        id, cycle_id, miller_id, date, bunches_milled,
                        drums_cooked, oil_produced_gallons, created_at, updated_at
                    )
                    SELECT
                        id, cycle_id, miller_id, date, bunches_milled,
                        CAST(drums_cooked AS REAL), oil_produced_gallons, created_at, updated_at
                    FROM `millings`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `millings`")
                db.execSQL("ALTER TABLE `millings_new` RENAME TO `millings`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_millings_cycle_id` ON `millings` (`cycle_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_millings_miller_id` ON `millings` (`miller_id`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Close the database instance
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
