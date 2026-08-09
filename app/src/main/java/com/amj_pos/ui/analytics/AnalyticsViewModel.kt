package com.amj_pos.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.DailyStat
import com.amj_pos.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnalyticsUiState(
    val dailyStats: List<DailyStat> = emptyList(),
    val isLoading: Boolean = false
)

class AnalyticsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = transactionRepository.getDailyStats(7)
        .map { stats -> AnalyticsUiState(dailyStats = stats) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState(isLoading = true)
        )
}
