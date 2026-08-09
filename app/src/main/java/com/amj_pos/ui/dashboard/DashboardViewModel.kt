package com.amj_pos.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.repository.UtangRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val dailyProfit: Double = 0.0,
    val totalUtang: Double = 0.0,
    val lowStockProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    transactionRepository: TransactionRepository,
    utangRepository: UtangRepository,
    productRepository: ProductRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getDailyProfit(),
        utangRepository.getTotalOutstandingUtang(),
        productRepository.getLowStockProducts(threshold = 5)
    ) { profit, utang, lowStock ->
        DashboardUiState(
            dailyProfit = profit,
            totalUtang = utang,
            lowStockProducts = lowStock,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
