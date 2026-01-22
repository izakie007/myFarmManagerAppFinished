package com.palmfarm.manager.ui.finances.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.palmfarm.manager.R
import com.palmfarm.manager.databinding.FragmentReportsBinding
import com.palmfarm.manager.ui.ViewModelFactory
import com.palmfarm.manager.ui.common.BaseFragment
import com.palmfarm.manager.utils.PdfGenerator
import kotlinx.coroutines.launch

/**
 * Reports hub fragment - navigation to different financial reports
 */
class ReportsFragment : BaseFragment<FragmentReportsBinding>() {

    private val viewModel: ReportsViewModel by viewModels { ViewModelFactory.create() }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentReportsBinding {
        return FragmentReportsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupNavigationCards()
        setupGenerateReportButton()
    }

    override fun setupObservers() {
        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnGenerateReport.isEnabled = !isLoading
        }

        // Observe error messages
        viewModel.error.observe(viewLifecycleOwner) { message ->
            message?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        // Observe success messages
        viewModel.success.observe(viewLifecycleOwner) { message ->
            message?.let {
                showToast(it)
                viewModel.clearSuccess()
            }
        }
    }

    /**
     * Setup navigation cards for each report type
     */
    private fun setupNavigationCards() {
        binding.cardCashFlow.setOnClickListener {
            findNavController().navigate(R.id.action_reports_to_cashFlow)
        }

        binding.cardProfitability.setOnClickListener {
            findNavController().navigate(R.id.action_reports_to_profitability)
        }

        binding.cardBalanceSheet.setOnClickListener {
            findNavController().navigate(R.id.action_reports_to_balanceSheet)
        }
    }

    /**
     * Setup generate report button
     */
    private fun setupGenerateReportButton() {
        binding.btnGenerateReport.setOnClickListener {
            generateFinancialReport()
        }
    }

    /**
     * Generate comprehensive financial report
     */
    private fun generateFinancialReport() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.generateComprehensiveFinancialReport(requireContext())
            result.onSuccess { file ->
                PdfGenerator(requireContext()).openPdf(file)
                showToast("Financial report generated: ${file.name}")
            }.onFailure { error ->
                showError(error.message ?: "Failed to generate financial report")
            }
        }
    }
}