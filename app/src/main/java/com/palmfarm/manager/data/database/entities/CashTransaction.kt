package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cash transaction entity for tracking cash flow
 */
@Entity(tableName = "cash_transactions")
data class CashTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String, // "OPENING_BALANCE", "CASH_IN", "CASH_OUT", "ADJUSTMENT"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "date")
    val date: Long,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "category")
    val category: String = "", // Links to expense/income category

    @ColumnInfo(name = "reference_id")
    val referenceId: Int = 0, // Links to related transaction (sale, expense, etc.)

    @ColumnInfo(name = "reference_type")
    val referenceType: String = "", // "SALE", "EXPENSE", "WAGE", "LOAN", etc.

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_OPENING_BALANCE = "OPENING_BALANCE"
        const val TYPE_CASH_IN = "CASH_IN"
        const val TYPE_CASH_OUT = "CASH_OUT"
        const val TYPE_ADJUSTMENT = "ADJUSTMENT"
    }
}
