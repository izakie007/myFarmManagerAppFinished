package com.palmfarm.manager.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.palmfarm.manager.data.database.entities.AdvancePayment
import com.palmfarm.manager.data.database.entities.Task
import com.palmfarm.manager.data.database.entities.WagePayment
import com.palmfarm.manager.data.database.entities.Worker
import com.palmfarm.manager.ui.finances.reports.BalanceSheetData
import com.palmfarm.manager.ui.finances.reports.CashFlowData
import com.palmfarm.manager.ui.finances.reports.ProfitabilityData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for generating PDF documents
 */
class PdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595 // A4 width in points
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 50
        private const val LINE_HEIGHT = 20
    }

    /**
     * Wrap text to fit within a given width
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            
            if (width <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                // If single word is too long, break it
                if (paint.measureText(word) > maxWidth) {
                    var remaining = word
                    while (paint.measureText(remaining) > maxWidth) {
                        var charCount = 0
                        while (charCount < remaining.length && 
                               paint.measureText(remaining.substring(0, charCount + 1)) <= maxWidth) {
                            charCount++
                        }
                        if (charCount == 0) charCount = 1 // At least one character
                        lines.add(remaining.substring(0, charCount))
                        remaining = remaining.substring(charCount)
                    }
                    currentLine = remaining
                } else {
                    currentLine = word
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return if (lines.isEmpty()) listOf("") else lines
    }

    /**
     * Generate worker payslip PDF
     */
    fun generatePayslip(
        wagePayment: WagePayment,
        worker: Worker,
        tasks: List<Task>,
        advances: List<AdvancePayment>,
        pendingTasks: List<Task> = emptyList(),
        enterpriseName: String
    ): File? {
        return try {
            // Create PDF document
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Create paints for different text styles
            val titlePaint = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val headerPaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
            }

            val normalPaint = Paint().apply {
                textSize = 12f
            }

            val smallPaint = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.GRAY
            }

            var yPosition = MARGIN.toFloat()

            // Header
            canvas.drawText(enterpriseName, PAGE_WIDTH / 2f, yPosition, titlePaint)
            yPosition += LINE_HEIGHT * 1.5f

            canvas.drawText("PAYMENT SLIP", PAGE_WIDTH / 2f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 1.5f

            // Date and Reference
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            canvas.drawText(
                "Date: ${dateFormat.format(Date(wagePayment.paymentDate))}",
                MARGIN.toFloat(),
                yPosition,
                normalPaint
            )
            canvas.drawText(
                "Ref: ${wagePayment.referenceNumber}",
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT * 2f

            // Worker Information
            canvas.drawText("Worker Information", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Name: ${worker.fullName}", MARGIN.toFloat(), yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Phone: ${worker.phoneNumber}", MARGIN.toFloat(), yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Specialty: ${worker.specialty}", MARGIN.toFloat(), yPosition, normalPaint)
            yPosition += LINE_HEIGHT * 2f

            // Tasks Table
            canvas.drawText("Work Completed", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            // Calculate column positions (50% Description, 12.5% Qty, 12.5% Rate, 25% Amount)
            val tableWidth = PAGE_WIDTH - 2 * MARGIN
            val descStartX = MARGIN.toFloat()
            val descWidth = tableWidth * 0.5f
            val qtyStartX = descStartX + descWidth
            val qtyWidth = tableWidth * 0.125f
            val rateStartX = qtyStartX + qtyWidth
            val rateWidth = tableWidth * 0.125f
            val amountStartX = rateStartX + rateWidth
            val amountWidth = tableWidth * 0.25f

            // Table headers
            canvas.drawText("Description", descStartX, yPosition, smallPaint)
            canvas.drawText("Qty", qtyStartX, yPosition, smallPaint)
            canvas.drawText("Rate", rateStartX, yPosition, smallPaint)
            canvas.drawText("Amount", amountStartX, yPosition, smallPaint)
            yPosition += LINE_HEIGHT.toFloat()

            // Draw line
            canvas.drawLine(
                MARGIN.toFloat(),
                yPosition,
                PAGE_WIDTH - MARGIN.toFloat(),
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT / 2f

            // Task rows
            tasks.forEach { task ->
                val taskQuantity = task.quantity ?: 0.0
                val amount = taskQuantity * task.payRate
                
                // Wrap description text
                val descLines = wrapText(task.description, normalPaint, descWidth)
                val startY = yPosition
                
                // Draw description (wrapped)
                descLines.forEachIndexed { index, line ->
                    canvas.drawText(line, descStartX, yPosition + (index * LINE_HEIGHT), normalPaint)
                }
                
                // Draw other columns aligned to first line
                canvas.drawText(taskQuantity.toString(), qtyStartX, startY, normalPaint)
                canvas.drawText(
                    CurrencyUtils.formatAmount(task.payRate),
                    rateStartX,
                    startY,
                    normalPaint
                )
                canvas.drawText(
                    CurrencyUtils.formatAmount(amount),
                    amountStartX,
                    startY,
                    normalPaint
                )
                
                // Move yPosition based on number of description lines
                yPosition += LINE_HEIGHT * descLines.size.coerceAtLeast(1)
            }

            yPosition += LINE_HEIGHT / 2f

            // Draw line
            canvas.drawLine(
                MARGIN.toFloat(),
                yPosition,
                PAGE_WIDTH - MARGIN.toFloat(),
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT.toFloat()

            // Pending tasks
            if (pendingTasks.isNotEmpty()) {
                yPosition += LINE_HEIGHT.toFloat()
                canvas.drawText("Pending Work (Not Processed)", MARGIN.toFloat(), yPosition, headerPaint)
                yPosition += LINE_HEIGHT.toFloat()

                // Calculate column positions (50% Description, 12.5% Qty, 12.5% Rate, 25% Amount)
                val tableWidth = PAGE_WIDTH - 2 * MARGIN
                val descStartX = MARGIN.toFloat()
                val descWidth = tableWidth * 0.5f
                val qtyStartX = descStartX + descWidth
                val qtyWidth = tableWidth * 0.125f
                val rateStartX = qtyStartX + qtyWidth
                val rateWidth = tableWidth * 0.125f
                val amountStartX = rateStartX + rateWidth
                val amountWidth = tableWidth * 0.25f

                canvas.drawText("Description", descStartX, yPosition, smallPaint)
                canvas.drawText("Qty", qtyStartX, yPosition, smallPaint)
                canvas.drawText("Rate", rateStartX, yPosition, smallPaint)
                canvas.drawText("Amount", amountStartX, yPosition, smallPaint)
                yPosition += LINE_HEIGHT.toFloat()

                canvas.drawLine(
                    MARGIN.toFloat(),
                    yPosition,
                    PAGE_WIDTH - MARGIN.toFloat(),
                    yPosition,
                    normalPaint
                )
                yPosition += LINE_HEIGHT / 2f

                pendingTasks.forEach { task ->
                    val taskQuantity = task.quantity ?: 0.0
                    val amount = taskQuantity * task.payRate
                    
                    // Wrap description text
                    val descLines = wrapText(task.description, normalPaint, descWidth)
                    val startY = yPosition
                    
                    // Draw description (wrapped)
                    descLines.forEachIndexed { index, line ->
                        canvas.drawText(line, descStartX, yPosition + (index * LINE_HEIGHT), normalPaint)
                    }
                    
                    // Draw other columns aligned to first line
                    canvas.drawText(taskQuantity.toString(), qtyStartX, startY, normalPaint)
                    canvas.drawText(
                        CurrencyUtils.formatAmount(task.payRate),
                        rateStartX,
                        startY,
                        normalPaint
                    )
                    canvas.drawText(
                        "${CurrencyUtils.formatAmount(amount)} (Pending)",
                        amountStartX,
                        startY,
                        normalPaint
                    )
                    
                    // Move yPosition based on number of description lines
                    yPosition += LINE_HEIGHT * descLines.size.coerceAtLeast(1)
                }
            }

            yPosition += LINE_HEIGHT.toFloat()

            // Payment Summary
            canvas.drawText("Payment Summary", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText(
                "Gross Wage:",
                MARGIN.toFloat(),
                yPosition,
                normalPaint
            )
            canvas.drawText(
                CurrencyUtils.formatAmount(wagePayment.grossWage),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText(
                "Advances:",
                MARGIN.toFloat(),
                yPosition,
                normalPaint
            )
            canvas.drawText(
                CurrencyUtils.formatAmount(wagePayment.totalAdvances),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT * 1.5f

            // Net amount (bold)
            canvas.drawText(
                "Net Amount:",
                MARGIN.toFloat(),
                yPosition,
                headerPaint
            )
            canvas.drawText(
                CurrencyUtils.formatAmount(wagePayment.netPayment),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                headerPaint
            )
            yPosition += LINE_HEIGHT * 2f

            // Payment method
            canvas.drawText(
                "Payment Method: ${wagePayment.paymentMethod}",
                MARGIN.toFloat(),
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT * 3f

            // Footer
            canvas.drawText(
                "Worker Signature: ___________________",
                MARGIN.toFloat(),
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT * 2f

            canvas.drawText(
                "Generated on ${dateFormat.format(Date())}",
                MARGIN.toFloat(),
                yPosition,
                smallPaint
            )

            // Finish page
            pdfDocument.finishPage(page)

            // Save to file
            val payslipDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "PalmFarm/Payslips"
            )
            if (!payslipDir.exists()) {
                payslipDir.mkdirs()
            }

            val fileName = "Payslip_${worker.fullName}_${wagePayment.referenceNumber}.pdf"
            val file = File(payslipDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Open the generated PDF with an external viewer.
     */
    fun openPdf(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open payslip"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generate financial report PDF with charts
     */
    fun generateFinancialReport(
        reportTitle: String,
        reportData: Map<String, Any>,
        period: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Create paints
            val titlePaint = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val headerPaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
            }

            val normalPaint = Paint().apply {
                textSize = 12f
            }

            val smallPaint = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.GRAY
            }

            var yPosition = MARGIN.toFloat()

            // Title
            canvas.drawText(reportTitle, PAGE_WIDTH / 2f, yPosition, titlePaint)
            yPosition += LINE_HEIGHT * 1.5f

            // Period
            canvas.drawText(period, PAGE_WIDTH / 2f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 2f

            // Render report data based on type
            when {
                reportTitle == "Cash Flow Report" -> {
                    renderCashFlowReport(canvas, reportData, yPosition, headerPaint, normalPaint)
                }
                reportTitle == "Profitability Report" -> {
                    renderProfitabilityReport(canvas, reportData, yPosition, headerPaint, normalPaint)
                }
                reportTitle == "Balance Sheet" -> {
                    renderBalanceSheet(canvas, reportData, yPosition, headerPaint, normalPaint)
                }
                else -> {
                    // Generic report rendering
                    reportData.forEach { (key, value) ->
                        canvas.drawText("$key: $value", MARGIN.toFloat(), yPosition, normalPaint)
                        yPosition += LINE_HEIGHT.toFloat()
                    }
                }
            }

            // Footer
            yPosition = PAGE_HEIGHT - MARGIN.toFloat() - LINE_HEIGHT * 2
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            canvas.drawText(
                "Generated on ${dateFormat.format(Date())}",
                MARGIN.toFloat(),
                yPosition,
                smallPaint
            )

            pdfDocument.finishPage(page)

            // Save to file
            val reportsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "PalmFarm/Reports"
            )
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${reportTitle.replace(" ", "_")}_$timestamp.pdf"
            val file = File(reportsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate comprehensive financial report with all sections
     */
    fun generateComprehensiveFinancialReport(
        enterpriseName: String,
        cashFlowData: CashFlowData,
        profitabilityData: ProfitabilityData,
        balanceSheetData: BalanceSheetData,
        period: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1

            // Create paints
            val titlePaint = Paint().apply {
                textSize = 24f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val headerPaint = Paint().apply {
                textSize = 16f
                isFakeBoldText = true
            }

            val normalPaint = Paint().apply {
                textSize = 12f
            }

            val smallPaint = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.GRAY
            }

            // Page 1: Cover and Cash Flow
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = MARGIN.toFloat()

            // Cover
            canvas.drawText(enterpriseName, PAGE_WIDTH / 2f, yPosition, titlePaint)
            yPosition += LINE_HEIGHT * 2f
            canvas.drawText("COMPREHENSIVE FINANCIAL REPORT", PAGE_WIDTH / 2f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 1.5f
            canvas.drawText(period, PAGE_WIDTH / 2f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT * 3f

            // Cash Flow Section
            canvas.drawText("CASH FLOW REPORT", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 1.5f

            // Cash Inflows
            canvas.drawText("Cash Inflows", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            val totalIncome = cashFlowData.totalIncome
            canvas.drawText("Sales & Consumption", MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(CurrencyUtils.formatAmount(totalIncome), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Total Inflows:", MARGIN.toFloat(), yPosition, headerPaint)
            canvas.drawText(CurrencyUtils.formatAmount(totalIncome), PAGE_WIDTH - MARGIN - 150f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 2f

            // Cash Outflows
            canvas.drawText("Cash Outflows", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            val totalExpenses = cashFlowData.totalExpenses
            canvas.drawText("Total Expenses", MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(CurrencyUtils.formatAmount(totalExpenses), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Total Outflows:", MARGIN.toFloat(), yPosition, headerPaint)
            canvas.drawText(CurrencyUtils.formatAmount(totalExpenses), PAGE_WIDTH - MARGIN - 150f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 2f

            // Net Cash Flow
            val netCashFlow = cashFlowData.netCashFlow
            val cashFlowPaint = Paint(headerPaint).apply {
                color = if (netCashFlow >= 0) android.graphics.Color.BLACK
                else android.graphics.Color.RED
            }
            canvas.drawText("Net Cash Flow:", MARGIN.toFloat(), yPosition, headerPaint)
            canvas.drawText(CurrencyUtils.formatAmount(netCashFlow), PAGE_WIDTH - MARGIN - 150f, yPosition, cashFlowPaint)

            pdfDocument.finishPage(page)

            // Page 2: Profitability
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = MARGIN.toFloat()

            canvas.drawText("PROFITABILITY REPORT", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 2f

            // Revenue
            canvas.drawText("Revenue", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Sales", MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(CurrencyUtils.formatAmount(profitabilityData.salesIncome), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Consumption", MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(CurrencyUtils.formatAmount(profitabilityData.consumptionIncome), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Total Revenue:", MARGIN.toFloat(), yPosition, headerPaint)
            canvas.drawText(CurrencyUtils.formatAmount(profitabilityData.totalIncome), PAGE_WIDTH - MARGIN - 150f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 2f

            // Costs
            canvas.drawText("Costs", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            profitabilityData.expensesBreakdown.forEach { (category, amount) ->
                canvas.drawText(category, MARGIN.toFloat() + 20, yPosition, normalPaint)
                canvas.drawText(CurrencyUtils.formatAmount(amount), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
                yPosition += LINE_HEIGHT.toFloat()
            }

            canvas.drawText("Depreciation", MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(CurrencyUtils.formatAmount(profitabilityData.totalDepreciation), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            val totalCosts = profitabilityData.operationalExpenses + profitabilityData.totalDepreciation
            canvas.drawText("Total Costs:", MARGIN.toFloat(), yPosition, headerPaint)
            canvas.drawText(CurrencyUtils.formatAmount(totalCosts), PAGE_WIDTH - MARGIN - 150f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 2f

            // Net Profit
            val netProfit = profitabilityData.netBalance
            val profitPaint = Paint(headerPaint).apply {
                color = if (netProfit >= 0) android.graphics.Color.rgb(76, 175, 80)
                else android.graphics.Color.RED
            }
            canvas.drawText("Net Profit:", MARGIN.toFloat(), yPosition, profitPaint)
            canvas.drawText(CurrencyUtils.formatAmount(netProfit), PAGE_WIDTH - MARGIN - 150f, yPosition, profitPaint)
            yPosition += LINE_HEIGHT * 1.5f

            canvas.drawText("Profit Margin: ${String.format("%.1f%%", profitabilityData.netMarginPercent)}", MARGIN.toFloat(), yPosition, normalPaint)

            pdfDocument.finishPage(page)

            // Page 3: Balance Sheet
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = MARGIN.toFloat()

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            canvas.drawText("BALANCE SHEET", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()
            canvas.drawText("As of ${dateFormat.format(Date(balanceSheetData.asOfDate))}", MARGIN.toFloat(), yPosition, normalPaint)
            yPosition += LINE_HEIGHT * 2f

            // Assets
            canvas.drawText("Assets", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            balanceSheetData.currentAssets.forEach { (asset, amount) ->
                canvas.drawText(asset, MARGIN.toFloat() + 20, yPosition, normalPaint)
                canvas.drawText(CurrencyUtils.formatAmount(amount), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
                yPosition += LINE_HEIGHT.toFloat()
            }

            canvas.drawText("Fixed Assets", MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(CurrencyUtils.formatAmount(balanceSheetData.fixedAssets), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Total Assets:", MARGIN.toFloat(), yPosition, headerPaint)
            canvas.drawText(CurrencyUtils.formatAmount(balanceSheetData.totalAssets), PAGE_WIDTH - MARGIN - 150f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 2f

            // Liabilities
            canvas.drawText("Liabilities", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Loans Payable", MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(CurrencyUtils.formatAmount(balanceSheetData.loansPayable), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Wages Payable", MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(CurrencyUtils.formatAmount(balanceSheetData.wagesPayable), PAGE_WIDTH - MARGIN - 150f, yPosition, normalPaint)
            yPosition += LINE_HEIGHT.toFloat()

            canvas.drawText("Total Liabilities:", MARGIN.toFloat(), yPosition, headerPaint)
            canvas.drawText(CurrencyUtils.formatAmount(balanceSheetData.totalLiabilities), PAGE_WIDTH - MARGIN - 150f, yPosition, headerPaint)
            yPosition += LINE_HEIGHT * 2f

            // Equity
            canvas.drawText("Net Worth (Equity):", MARGIN.toFloat(), yPosition, headerPaint)
            canvas.drawText(CurrencyUtils.formatAmount(balanceSheetData.netWorth), PAGE_WIDTH - MARGIN - 150f, yPosition, headerPaint)

            // Footer
            yPosition = PAGE_HEIGHT - MARGIN.toFloat() - LINE_HEIGHT * 2
            canvas.drawText("Generated on ${dateFormat.format(Date())}", MARGIN.toFloat(), yPosition, smallPaint)

            pdfDocument.finishPage(page)

            // Save to file
            val reportsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "PalmFarm/Reports"
            )
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Comprehensive_Financial_Report_$timestamp.pdf"
            val file = File(reportsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun renderCashFlowReport(
        canvas: android.graphics.Canvas,
        data: Map<String, Any>,
        startY: Float,
        headerPaint: Paint,
        normalPaint: Paint
    ) {
        var yPosition = startY

        // Cash Inflows
        canvas.drawText("Cash Inflows", MARGIN.toFloat(), yPosition, headerPaint)
        yPosition += LINE_HEIGHT.toFloat()

        val cashInflows = data["cash_inflows"] as? Map<String, Double> ?: emptyMap()
        cashInflows.forEach { (source, amount) ->
            canvas.drawText(source, MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(
                CurrencyUtils.formatAmount(amount),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT.toFloat()
        }

        val totalInflows = data["total_inflows"] as? Double ?: 0.0
        canvas.drawText("Total Inflows:", MARGIN.toFloat(), yPosition, headerPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(totalInflows),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            headerPaint
        )
        yPosition += LINE_HEIGHT * 2f

        // Cash Outflows
        canvas.drawText("Cash Outflows", MARGIN.toFloat(), yPosition, headerPaint)
        yPosition += LINE_HEIGHT.toFloat()

        val cashOutflows = data["cash_outflows"] as? Map<String, Double> ?: emptyMap()
        cashOutflows.forEach { (category, amount) ->
            canvas.drawText(category, MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(
                CurrencyUtils.formatAmount(amount),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT.toFloat()
        }

        val totalOutflows = data["total_outflows"] as? Double ?: 0.0
        canvas.drawText("Total Outflows:", MARGIN.toFloat(), yPosition, headerPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(totalOutflows),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            headerPaint
        )
        yPosition += LINE_HEIGHT * 2f

        // Net Cash Flow
        val netCashFlow = data["net_cash_flow"] as? Double ?: 0.0
        canvas.drawText("Net Cash Flow:", MARGIN.toFloat(), yPosition, headerPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(netCashFlow),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            headerPaint
        )
    }

    private fun renderProfitabilityReport(
        canvas: android.graphics.Canvas,
        data: Map<String, Any>,
        startY: Float,
        headerPaint: Paint,
        normalPaint: Paint
    ) {
        var yPosition = startY

        // Revenue
        canvas.drawText("Revenue", MARGIN.toFloat(), yPosition, headerPaint)
        yPosition += LINE_HEIGHT.toFloat()

        val revenue = data["revenue"] as? Map<String, Double> ?: emptyMap()
        revenue.forEach { (source, amount) ->
            canvas.drawText(source, MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(
                CurrencyUtils.formatAmount(amount),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT.toFloat()
        }

        val totalRevenue = data["total_revenue"] as? Double ?: 0.0
        canvas.drawText("Total Revenue:", MARGIN.toFloat(), yPosition, headerPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(totalRevenue),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            headerPaint
        )
        yPosition += LINE_HEIGHT * 2f

        // Costs
        canvas.drawText("Costs", MARGIN.toFloat(), yPosition, headerPaint)
        yPosition += LINE_HEIGHT.toFloat()

        val costs = data["costs"] as? Map<String, Double> ?: emptyMap()
        costs.forEach { (category, amount) ->
            canvas.drawText(category, MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(
                CurrencyUtils.formatAmount(amount),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT.toFloat()
        }

        val totalCosts = data["total_costs"] as? Double ?: 0.0
        canvas.drawText("Total Costs:", MARGIN.toFloat(), yPosition, headerPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(totalCosts),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            headerPaint
        )
        yPosition += LINE_HEIGHT * 2f

        // Net Profit
        val netProfit = data["net_profit"] as? Double ?: 0.0
        val profitPaint = Paint(headerPaint).apply {
            color = if (netProfit >= 0) android.graphics.Color.rgb(76, 175, 80)
            else android.graphics.Color.RED
        }
        canvas.drawText("Net Profit:", MARGIN.toFloat(), yPosition, profitPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(netProfit),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            profitPaint
        )
        yPosition += LINE_HEIGHT * 1.5f

        // Profit Margin
        val profitMargin = data["profit_margin"] as? Double ?: 0.0
        canvas.drawText(
            "Profit Margin: ${String.format("%.1f%%", profitMargin)}",
            MARGIN.toFloat(),
            yPosition,
            normalPaint
        )
    }

    private fun renderBalanceSheet(
        canvas: android.graphics.Canvas,
        data: Map<String, Any>,
        startY: Float,
        headerPaint: Paint,
        normalPaint: Paint
    ) {
        var yPosition = startY

        // Assets
        canvas.drawText("Assets", MARGIN.toFloat(), yPosition, headerPaint)
        yPosition += LINE_HEIGHT.toFloat()

        val assets = data["assets"] as? Map<String, Double> ?: emptyMap()
        assets.forEach { (asset, amount) ->
            canvas.drawText(asset, MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(
                CurrencyUtils.formatAmount(amount),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT.toFloat()
        }

        val totalAssets = data["total_assets"] as? Double ?: 0.0
        canvas.drawText("Total Assets:", MARGIN.toFloat(), yPosition, headerPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(totalAssets),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            headerPaint
        )
        yPosition += LINE_HEIGHT * 2f

        // Liabilities
        canvas.drawText("Liabilities", MARGIN.toFloat(), yPosition, headerPaint)
        yPosition += LINE_HEIGHT.toFloat()

        val liabilities = data["liabilities"] as? Map<String, Double> ?: emptyMap()
        liabilities.forEach { (liability, amount) ->
            canvas.drawText(liability, MARGIN.toFloat() + 20, yPosition, normalPaint)
            canvas.drawText(
                CurrencyUtils.formatAmount(amount),
                PAGE_WIDTH - MARGIN - 150f,
                yPosition,
                normalPaint
            )
            yPosition += LINE_HEIGHT.toFloat()
        }

        val totalLiabilities = data["total_liabilities"] as? Double ?: 0.0
        canvas.drawText("Total Liabilities:", MARGIN.toFloat(), yPosition, headerPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(totalLiabilities),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            headerPaint
        )
        yPosition += LINE_HEIGHT * 2f

        // Equity
        val equity = data["equity"] as? Double ?: 0.0
        canvas.drawText("Equity:", MARGIN.toFloat(), yPosition, headerPaint)
        canvas.drawText(
            CurrencyUtils.formatAmount(equity),
            PAGE_WIDTH - MARGIN - 150f,
            yPosition,
            headerPaint
        )
    }
}
