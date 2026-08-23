package com.amj_pos.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.model.User
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val dailySales: Double = 0.0,
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
    private val categoryRepository: com.amj_pos.domain.repository.CategoryRepository,
    private val printerRepository: PrinterRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    init {
        // Sync products, categories, and recent transactions from Firestore on startup
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.syncFromFirestore()
            categoryRepository.syncFromFirestore()
            transactionRepository.syncTransactionsFromFirestore()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        authRepository.currentUser,
        printerRepository.connectionState,
        productRepository.getLowStockProducts(threshold = 5),
        utangRepository.getTotalOutstandingUtang()
    ) { user, printer, lowStock, utang ->
        DashboardParams(user, printer, lowStock, utang)
    }.flatMapLatest { params ->
        val branch = if (params.user?.role?.lowercase() == "employee") params.user.assigned_branch else ""

        transactionRepository.getDailySales(branch).map { sales ->
            DashboardUiState(
                dailySales = sales,
                totalUtang = params.utang,
                lowStockProducts = params.lowStock,
                printerStatus = params.printer,
                userRole = params.user?.role?.lowercase() ?: "employee",
                userName = params.user?.name ?: "",
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private data class DashboardParams(
        val user: User?,
        val printer: PrinterStatus,
        val lowStock: List<Product>,
        val utang: Double
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
