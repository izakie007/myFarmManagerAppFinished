package com.palmfarm.manager.ui.analytics

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.palmfarm.manager.databinding.FragmentAnalyticsBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Analytics fragment with KPIs and charts
 */
class AnalyticsFragment : BaseFragment<FragmentAnalyticsBinding>() {

    private val viewModel: AnalyticsViewModel by viewModels { ViewModelFactory.create() }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAnalyticsBinding {
        return FragmentAnalyticsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupCharts()
    }

    override fun setupObservers() {
        observeProductionKPIs()
        observeFinancialKPIs()
        observeChartData()
    }

    /**
     * Setup all charts with styling
     */
    private fun setupCharts() {
        setupIncomeExpensesChart()
        setupProductionTrendsChart()
        setupExpensePieChart()
        setupCostAnalysisChart()
        setupProfitabilityChart()
        setupBunchesOnlyChart()
        setupOilOnlyChart()
        setupOilRatioChart()
        setupPerBunchFinancialChart()
        setupPerGallonFinancialChart()
        setupCumulativeFinancialChart()
    }

    /**
     * Setup Income vs Expenses Line Chart
     */
    private fun setupIncomeExpensesChart() {
        binding.lineChartIncomeExpenses.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            // X Axis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY
            
            // Y Axis
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false
            
            // Legend
            legend.isEnabled = true
            legend.textColor = Color.GRAY
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.form = Legend.LegendForm.LINE
        }
    }

    /**
     * Setup Production Trends Line Chart
     */
    private fun setupProductionTrendsChart() {
        binding.lineChartProduction.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY
            
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false
            
            legend.isEnabled = true
            legend.textColor = Color.GRAY
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.form = Legend.LegendForm.LINE
        }
    }

    /**
     * Setup Expense Breakdown Pie Chart
     */
    private fun setupExpensePieChart() {
        binding.pieChartExpenses.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            holeRadius = 40f
            transparentCircleRadius = 45f
            setDrawCenterText(true)
            centerText = "Expenses"
            setCenterTextSize(14f)
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(10f)
            
            legend.isEnabled = true
            legend.textColor = Color.GRAY
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.form = Legend.LegendForm.CIRCLE
        }
    }

    /**
     * Setup Cost Analysis Bar Chart
     */
    private fun setupCostAnalysisChart() {
        binding.barChartCosts.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setDrawGridBackground(false)
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY
            
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false
            
            legend.isEnabled = false
        }
    }

    /**
     * Setup Profitability Multi-bar Chart
     */
    private fun setupProfitabilityChart() {
        binding.barChartProfitability.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setDrawGridBackground(false)
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY
            
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false
            
            legend.isEnabled = true
            legend.textColor = Color.GRAY
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.form = Legend.LegendForm.SQUARE
        }
    }

    private fun setupBunchesOnlyChart() {
        binding.lineChartBunchesOnly.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false

            legend.isEnabled = true
            legend.textColor = Color.GRAY
        }
    }

    private fun setupOilOnlyChart() {
        binding.lineChartOilOnly.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false

            legend.isEnabled = true
            legend.textColor = Color.GRAY
        }
    }

    private fun setupOilRatioChart() {
        binding.lineChartOilRatio.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false

            legend.isEnabled = true
            legend.textColor = Color.GRAY
        }
    }

    private fun setupPerBunchFinancialChart() {
        binding.lineChartPerBunchFinancial.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false

            legend.isEnabled = true
            legend.textColor = Color.GRAY
        }
    }

    private fun setupPerGallonFinancialChart() {
        binding.lineChartPerGallonFinancial.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false

            legend.isEnabled = true
            legend.textColor = Color.GRAY
        }
    }

    private fun setupCumulativeFinancialChart() {
        binding.lineChartCumulativeFinancial.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.GRAY

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.LTGRAY
            axisLeft.textColor = Color.GRAY
            axisRight.isEnabled = false

            legend.isEnabled = true
            legend.textColor = Color.GRAY
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            legend.form = Legend.LegendForm.LINE
        }
    }

    /**
     * Observe chart data and populate charts
     */
    private fun observeChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getChartData().collect { chartData ->
                populateIncomeExpensesChart(chartData.incomeExpensesData)
                populateProductionChart(chartData.productionData)
                populateExpensePieChart(chartData.expenseBreakdown)
                populateCostAnalysisChart(chartData.costAnalysis)
                populateProfitabilityChart(chartData.profitabilityData)
                populateBunchesOnlyChart(chartData.bunchesOnlyData)
                populateOilOnlyChart(chartData.oilOnlyData)
                populateOilRatioChart(chartData.oilToBunchRatioData)
                populatePerBunchFinancialChart(chartData.perBunchFinancialData)
                populatePerGallonFinancialChart(chartData.perGallonFinancialData)
                populateCumulativeFinancialChart(chartData.cumulativeFinancialData)
            }
        }
    }

    /**
     * Populate Income vs Expenses Line Chart
     */
    private fun populateIncomeExpensesChart(data: List<IncomeExpensePoint>) {
        val incomeEntries = mutableListOf<Entry>()
        val expenseEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        
        data.forEachIndexed { index, point ->
            incomeEntries.add(Entry(index.toFloat(), point.income.toFloat()))
            expenseEntries.add(Entry(index.toFloat(), point.expense.toFloat()))
            labels.add(point.label)
        }
        
        val incomeDataSet = LineDataSet(incomeEntries, "Income").apply {
            color = Color.rgb(76, 175, 80)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(76, 175, 80))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        val expenseDataSet = LineDataSet(expenseEntries, "Expenses").apply {
            color = Color.rgb(244, 67, 54)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(244, 67, 54))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        val lineData = LineData(incomeDataSet, expenseDataSet)
        binding.lineChartIncomeExpenses.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = lineData
            animateX(1000)
            invalidate()
        }
    }

    /**
     * Populate Production Trends Line Chart
     */
    private fun populateProductionChart(data: List<ProductionPoint>) {
        val harvestEntries = mutableListOf<Entry>()
        val oilEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        
        data.forEachIndexed { index, point ->
            harvestEntries.add(Entry(index.toFloat(), point.bunches.toFloat()))
            oilEntries.add(Entry(index.toFloat(), point.oilGallons.toFloat()))
            labels.add(point.label)
        }
        
        val harvestDataSet = LineDataSet(harvestEntries, "Bunches Harvested").apply {
            color = Color.rgb(33, 150, 243)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(33, 150, 243))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        val oilDataSet = LineDataSet(oilEntries, "Oil Produced (gal)").apply {
            color = Color.rgb(255, 152, 0)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(255, 152, 0))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        
        val lineData = LineData(harvestDataSet, oilDataSet)
        binding.lineChartProduction.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = lineData
            animateX(1000)
            invalidate()
        }
    }

    /**
     * Populate Expense Breakdown Pie Chart
     */
    private fun populateExpensePieChart(data: Map<String, Double>) {
        val entries = mutableListOf<PieEntry>()
        
        data.forEach { (category, amount) ->
            if (amount > 0) {
                entries.add(PieEntry(amount.toFloat(), category))
            }
        }
        
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.rgb(76, 175, 80),
                Color.rgb(33, 150, 243),
                Color.rgb(255, 152, 0),
                Color.rgb(156, 39, 176),
                Color.rgb(244, 67, 54),
                Color.rgb(0, 188, 212)
            )
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
            valueFormatter = PercentFormatter(binding.pieChartExpenses)
        }
        
        val pieData = PieData(dataSet)
        binding.pieChartExpenses.apply {
            this.data = pieData
            animateY(1000)
            invalidate()
        }
    }

    /**
     * Populate Cost Analysis Bar Chart
     */
    private fun populateCostAnalysisChart(data: List<CostItem>) {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        data.forEachIndexed { index, item ->
            entries.add(BarEntry(index.toFloat(), item.amount.toFloat()))
            labels.add(item.category)
        }
        
        val dataSet = BarDataSet(entries, "Costs").apply {
            color = Color.rgb(33, 150, 243)
            valueTextSize = 10f
            valueTextColor = Color.GRAY
        }
        
        val barData = BarData(dataSet)
        barData.barWidth = 0.8f
        
        binding.barChartCosts.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = barData
            animateY(1000)
            invalidate()
        }
    }

    /**
     * Populate Profitability Multi-bar Chart
     */
    private fun populateProfitabilityChart(data: List<ProfitabilityPoint>) {
        val revenueEntries = mutableListOf<BarEntry>()
        val costEntries = mutableListOf<BarEntry>()
        val profitEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        data.forEachIndexed { index, point ->
            revenueEntries.add(BarEntry(index.toFloat(), point.revenue.toFloat()))
            costEntries.add(BarEntry(index.toFloat(), point.costs.toFloat()))
            profitEntries.add(BarEntry(index.toFloat(), point.profit.toFloat()))
            labels.add(point.label)
        }
        
        val revenueDataSet = BarDataSet(revenueEntries, "Revenue").apply {
            color = Color.rgb(76, 175, 80)
            valueTextSize = 9f
        }
        
        val costDataSet = BarDataSet(costEntries, "Costs").apply {
            color = Color.rgb(244, 67, 54)
            valueTextSize = 9f
        }
        
        val profitDataSet = BarDataSet(profitEntries, "Profit").apply {
            color = Color.rgb(33, 150, 243)
            valueTextSize = 9f
        }
        
        val barData = BarData(revenueDataSet, costDataSet, profitDataSet)
        
        val groupSpace = 0.3f
        val barSpace = 0.05f
        val barWidth = 0.2f
        
        barData.barWidth = barWidth
        
        binding.barChartProfitability.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.setCenterAxisLabels(true)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = data.size.toFloat()
            this.data = barData
            groupBars(0f, groupSpace, barSpace)
            animateY(1000)
            invalidate()
        }
    }

    private fun populateBunchesOnlyChart(data: List<BunchesPoint>) {
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        data.forEachIndexed { index, point ->
            entries.add(Entry(index.toFloat(), point.bunches.toFloat()))
            labels.add(point.label)
        }

        val dataSet = LineDataSet(entries, "Bunches Harvested").apply {
            color = Color.rgb(63, 81, 181)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(63, 81, 181))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(dataSet)
        binding.lineChartBunchesOnly.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = lineData
            animateX(1000)
            invalidate()
        }
    }

    private fun populateOilOnlyChart(data: List<OilPoint>) {
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        data.forEachIndexed { index, point ->
            entries.add(Entry(index.toFloat(), point.gallons.toFloat()))
            labels.add(point.label)
        }

        val dataSet = LineDataSet(entries, "Oil Produced (gal)").apply {
            color = Color.rgb(255, 152, 0)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(255, 152, 0))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(dataSet)
        binding.lineChartOilOnly.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = lineData
            animateX(1000)
            invalidate()
        }
    }

    private fun populateOilRatioChart(data: List<OilToBunchRatioPoint>) {
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        data.forEachIndexed { index, point ->
            entries.add(Entry(index.toFloat(), point.gallonsPerBunch.toFloat()))
            labels.add(point.label)
        }

        val dataSet = LineDataSet(entries, "Oil per Bunch (L)").apply {
            color = Color.rgb(0, 188, 212)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(0, 188, 212))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(dataSet)
        binding.lineChartOilRatio.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = lineData
            animateX(1000)
            invalidate()
        }
    }

    private fun populatePerBunchFinancialChart(data: List<PerUnitFinancialPoint>) {
        val costEntries = mutableListOf<Entry>()
        val incomeEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        data.forEachIndexed { index, point ->
            costEntries.add(Entry(index.toFloat(), point.costPerUnit.toFloat()))
            incomeEntries.add(Entry(index.toFloat(), point.incomePerUnit.toFloat()))
            labels.add(point.label)
        }

        val costDataSet = LineDataSet(costEntries, "Cost per Bunch").apply {
            color = Color.rgb(244, 67, 54)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(244, 67, 54))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val incomeDataSet = LineDataSet(incomeEntries, "Income per Bunch").apply {
            color = Color.rgb(76, 175, 80)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(76, 175, 80))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(costDataSet, incomeDataSet)
        binding.lineChartPerBunchFinancial.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = lineData
            animateX(1000)
            invalidate()
        }
    }

    private fun populatePerGallonFinancialChart(data: List<PerUnitFinancialPoint>) {
        val costEntries = mutableListOf<Entry>()
        val incomeEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        data.forEachIndexed { index, point ->
            costEntries.add(Entry(index.toFloat(), point.costPerUnit.toFloat()))
            incomeEntries.add(Entry(index.toFloat(), point.incomePerUnit.toFloat()))
            labels.add(point.label)
        }

        val costDataSet = LineDataSet(costEntries, "Cost per Gallon").apply {
            color = Color.rgb(121, 85, 72)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(121, 85, 72))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val incomeDataSet = LineDataSet(incomeEntries, "Income per Gallon").apply {
            color = Color.rgb(0, 150, 136)
            lineWidth = 2.5f
            setCircleColor(Color.rgb(0, 150, 136))
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(costDataSet, incomeDataSet)
        binding.lineChartPerGallonFinancial.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = lineData
            animateX(1000)
            invalidate()
        }
    }

    private fun populateCumulativeFinancialChart(data: List<CumulativeFinancialPoint>) {
        val costEntries = mutableListOf<Entry>()
        val incomeEntries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        data.forEachIndexed { index, point ->
            costEntries.add(Entry(index.toFloat(), point.cumulativeCost.toFloat()))
            incomeEntries.add(Entry(index.toFloat(), point.cumulativeIncome.toFloat()))
            labels.add(point.label)
        }

        val costDataSet = LineDataSet(costEntries, "Cumulative Cost").apply {
            color = Color.rgb(244, 67, 54)
            lineWidth = 3f
            setCircleColor(Color.rgb(244, 67, 54))
            circleRadius = 5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val incomeDataSet = LineDataSet(incomeEntries, "Cumulative Income").apply {
            color = Color.rgb(76, 175, 80)
            lineWidth = 3f
            setCircleColor(Color.rgb(76, 175, 80))
            circleRadius = 5f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val lineData = LineData(costDataSet, incomeDataSet)
        binding.lineChartCumulativeFinancial.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            this.data = lineData
            animateX(1000)
            invalidate()
        }
    }

    /**
     * Observe production KPIs
     */
    private fun observeProductionKPIs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getProductionKPIs().collect { kpis ->
                // All-time bunches per tree
                binding.tvAllTimeBunchesPerTree.text = String.format("%.2f", kpis.allTimeBunchesPerTree)

                // Current bunches per tree
                binding.tvCurrentBunchesPerTree.text = String.format("%.2f", kpis.currentBunchesPerTree)

                // Percentage change
                val bunchesChangeText = formatPercentageChange(kpis.bunchesPerTreeChange)
                binding.tvBunchesPerTreeChange.text = bunchesChangeText
                binding.tvBunchesPerTreeChange.setTextColor(getChangeColor(kpis.bunchesPerTreeChange))

                // All-time oil per bunch
                binding.tvAllTimeOilPerBunch.text = String.format("%.2f L", kpis.allTimeOilPerBunch)

                // Current oil per bunch
                binding.tvCurrentOilPerBunch.text = String.format("%.2f L", kpis.currentOilPerBunch)

                // Percentage change
                val oilChangeText = formatPercentageChange(kpis.oilPerBunchChange)
                binding.tvOilPerBunchChange.text = oilChangeText
                binding.tvOilPerBunchChange.setTextColor(getChangeColor(kpis.oilPerBunchChange))
            }
        }
    }

    /**
     * Observe financial KPIs
     */
    private fun observeFinancialKPIs() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getFinancialKPIs().collect { kpis ->
                // Cost per bunch
                binding.tvCostPerBunch.text = CurrencyUtils.formatAmount(kpis.costPerBunch)

                // Profit margin
                binding.tvProfitMargin.text = String.format("%.1f%%", kpis.profitMargin)
                binding.tvProfitMargin.setTextColor(getChangeColor(kpis.profitMargin))

                // ROI
                binding.tvRoi.text = String.format("%.1f%%", kpis.roi)
                binding.tvRoi.setTextColor(getChangeColor(kpis.roi))

                // Total revenue
                binding.tvTotalRevenue.text = CurrencyUtils.formatAmount(kpis.totalRevenue)

                // Total costs
                binding.tvTotalCosts.text = CurrencyUtils.formatAmount(kpis.totalCosts)

                // Net profit
                binding.tvNetProfit.text = CurrencyUtils.formatAmount(kpis.netProfit)
                binding.tvNetProfit.setTextColor(getChangeColor(if (kpis.netProfit >= 0) 1.0 else -1.0))

                // Income per bunch
                binding.tvIncomePerBunch.text = CurrencyUtils.formatAmount(kpis.incomePerBunch)

                // Profit per bunch
                binding.tvProfitPerBunch.text = CurrencyUtils.formatAmount(kpis.profitPerBunch)
                binding.tvProfitPerBunch.setTextColor(getChangeColor(if (kpis.profitPerBunch >= 0) 1.0 else -1.0))
            }
        }
    }

    /**
     * Format percentage change with sign
     */
    private fun formatPercentageChange(change: Double): String {
        val sign = if (change >= 0) "+" else ""
        return String.format("%s%.1f%%", sign, change)
    }

    /**
     * Get color for change value
     */
    private fun getChangeColor(change: Double): Int {
        return if (change >= 0) {
            requireContext().getColor(com.palmfarm.manager.R.color.green_500)
        } else {
            requireContext().getColor(com.palmfarm.manager.R.color.md_theme_error)
        }
    }
}
