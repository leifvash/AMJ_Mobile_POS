package com.amj_pos.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.printer.PrinterRepository
import com.amj_pos.domain.printer.PrinterStatus
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.repository.UtangRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val dailyProfit: Double = 0.0,
    val totalUtang: Double = 0.0,
    val lowStockProducts: List<Product> = emptyList(),
    val printerStatus: PrinterStatus = PrinterStatus.DISCONNECTED,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val utangRepository: UtangRepository,
    private val productRepository: ProductRepository,
    private val printerRepository: PrinterRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getDailyProfit(),
        utangRepository.getTotalOutstandingUtang(),
        productRepository.getLowStockProducts(threshold = 5),
        printerRepository.connectionState
    ) { profit, utang, lowStock, printer ->
        DashboardUiState(
            dailyProfit = profit,
            totalUtang = utang,
            lowStockProducts = lowStock,
            printerStatus = printer,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun connectPrinter() {
        viewModelScope.launch {
            printerRepository.connect()
        }
    }
}
