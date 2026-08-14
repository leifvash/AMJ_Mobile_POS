package com.amj_pos.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.printer.PrinterRepository
import com.amj_pos.domain.printer.PrinterStatus
import com.amj_pos.domain.repository.AuthRepository
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.repository.UtangRepository
import kotlinx.coroutines.Dispatchers
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
    val userRole: String = "employee",
    val userName: String = "",
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val utangRepository: UtangRepository,
    private val productRepository: ProductRepository,
    private val printerRepository: PrinterRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getDailyProfit(),
        utangRepository.getTotalOutstandingUtang(),
        productRepository.getLowStockProducts(threshold = 5),
        printerRepository.connectionState,
        authRepository.currentUser
    ) { profit, utang, lowStock, printer, user ->
        DashboardUiState(
            dailyProfit = profit,
            totalUtang = utang,
            lowStockProducts = lowStock,
            printerStatus = printer,
            userRole = user?.role?.lowercase() ?: "employee",
            userName = user?.name ?: "",
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun connectPrinter() {
        viewModelScope.launch(Dispatchers.IO) {
            printerRepository.connect()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
