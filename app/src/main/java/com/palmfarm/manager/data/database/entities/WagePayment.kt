package com.palmfarm.manager.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * WagePayment entity - Stores processed wage payments
 * Once created, these records are immutable
 */
@Entity(
    tableName = "wage_payments",
    foreignKeys = [
        ForeignKey(
            entity = Worker::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductionCycle::class,
            parentColumns = ["id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["worker_id"]),
        Index(value = ["cycle_id"])
    ]
)
data class WagePayment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "worker_id")
    val workerId: Int,

    @ColumnInfo(name = "cycle_id")
    val cycleId: Int,

    @ColumnInfo(name = "gross_wage")
    val grossWage: Double, // XAF (sum of task payments)

    @ColumnInfo(name = "total_advances")
    val totalAdvances: Double, // XAF (sum of advances for this payment)

    @ColumnInfo(name = "net_payment")
    val netPayment: Double, // grossWage - totalAdvances

    @ColumnInfo(name = "payment_date")
    val paymentDate: Long, // Timestamp

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String, // Values: "CASH", "TRANSFER", "OTHER"

    @ColumnInfo(name = "payslip_path")
    val payslipPath: String, // Local file path to PDF

    @ColumnInfo(name = "reference_number")
    val referenceNumber: String, // Auto-generated unique reference

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
