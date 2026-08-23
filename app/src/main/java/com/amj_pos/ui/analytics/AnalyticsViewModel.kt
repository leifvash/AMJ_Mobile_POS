package com.amj_pos.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.BranchPerformance
import com.amj_pos.data.local.entities.DailyStat
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.*

data class AnalyticsUiState(
    val dailyStats: List<DailyStat> = emptyList(),
    val topProducts: List<TransactionItem> = emptyList(),
    val branchPerformance: List<BranchPerformance> = emptyList(),
    val isLoading: Boolean = false
)

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        transactionRepository.getDailyStats(7),
        transactionRepository.getTopSellingProducts(5),
        transactionRepository.getBranchPerformance()
    ) { stats, top, branch ->
        AnalyticsUiState(
            dailyStats = stats,
            topProducts = top,
            branchPerformance = branch,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState(isLoading = true)
    )
}
