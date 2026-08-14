package com.amj_pos.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.repository.UtangRepository
import com.amj_pos.domain.scanner.BarcodeScanner
import com.amj_pos.domain.printer.PrinterRepository
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.data.local.entities.PaymentMethod
import com.amj_pos.data.local.entities.Customer
import com.amj_pos.data.local.entities.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val items: List<TransactionItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val totalProfit: Double = 0.0,
    val isScanning: Boolean = false,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val searchProducts: List<Product> = emptyList(),
    val error: String? = null
)

class CheckoutViewModel(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val utangRepository: UtangRepository,
    private val barcodeScanner: BarcodeScanner,
    private val printerRepository: PrinterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState

    private val _manualSearchQuery = MutableStateFlow("")
    val manualSearchQuery: StateFlow<String> = _manualSearchQuery

    init {
        viewModelScope.launch {
            utangRepository.getCustomers().collect { customers ->
                _uiState.update { it.copy(customers = customers) }
            }
        }

        @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            _manualSearchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(emptyList())
                    else productRepository.searchProducts(query)
                }
                .collect { products ->
                    _uiState.update { it.copy(searchProducts = products) }
                }
        }
    }

    fun onManualSearchQueryChange(query: String) {
        _manualSearchQuery.value = query
    }

    fun onVoiceResult(text: String) {
        _manualSearchQuery.value = text
    }

    fun scanBarcode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null) }
            val result = barcodeScanner.scan()
            
            result.onSuccess { barcode ->
                if (barcode != null) {
                    addProductByBarcode(barcode)
                }
                _uiState.update { it.copy(isScanning = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isScanning = false, error = "Scan failed: ${e.message}") }
            }
        }
    }

    private suspend fun addProductByBarcode(barcode: String) {
        val product = productRepository.getProductByBarcode(barcode)
        if (product != null) {
            addItemToCart(product)
        } else {
            _uiState.update { it.copy(error = "Product not found in inventory") }
        }
    }

    fun addItemToCart(product: Product) {
        val existingItem = _uiState.value.items.find { it.productId == product.id }
        val currentQtyInCart = existingItem?.quantity ?: 0
        
        if (product.currentStockInPieces <= currentQtyInCart) {
            _uiState.update { it.copy(error = "Out of stock: ${product.name}") }
            return
        }

        val updatedItems = if (existingItem != null) {
            _uiState.value.items.map {
                if (it.productId == product.id) {
                    val newQty = it.quantity + 1
                    it.copy(
                        quantity = newQty,
                        profit = (it.sellPricePerPiece - it.costPricePerPiece) * newQty
                    )
                } else it
            }
        } else {
            _uiState.value.items + TransactionItem(
                transactionId = 0,
                productId = product.id,
                productName = product.name,
                quantity = 1,
                sellPricePerPiece = product.pieceRetailPrice,
                costPricePerPiece = product.costPricePerPiece,
                profit = product.pieceRetailPrice - product.costPricePerPiece
            )
        }
        
        updateTotals(updatedItems)
    }

    fun removeItemFromCart(productId: Long) {
        val updatedItems = _uiState.value.items.filter { it.productId != productId }
        updateTotals(updatedItems)
    }

    fun updateQuantity(productId: Long, delta: Int) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId) ?: return@launch
            val items = _uiState.value.items.toMutableList()
            val index = items.indexOfFirst { it.productId == productId }
            
            if (index != -1) {
                val currentItem = items[index]
                val newQty = currentItem.quantity + delta
                
                if (newQty <= 0) {
                    items.removeAt(index)
                } else if (newQty > product.currentStockInPieces) {
                    _uiState.update { it.copy(error = "Only ${product.currentStockInPieces} pieces left in stock.") }
                    return@launch
                } else {
                    items[index] = currentItem.copy(
                        quantity = newQty,
                        profit = (currentItem.sellPricePerPiece - currentItem.costPricePerPiece) * newQty
                    )
                }
                updateTotals(items)
            }
        }
    }

    fun updateTotals(items: List<TransactionItem>) {
        val total = items.sumOf { it.sellPricePerPiece * it.quantity }
        val profit = items.sumOf { it.profit }
        _uiState.update { it.copy(items = items, totalAmount = total, totalProfit = profit) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onCustomerSelected(customer: Customer?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun completeSale() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.items.isEmpty()) return@launch
            
            val isUtang = state.paymentMethod == PaymentMethod.UTANG
            val customerId = if (isUtang) state.selectedCustomer?.id else null
            
            if (isUtang && customerId == null) {
                _uiState.update { it.copy(error = "Please select a customer for Utang") }
                return@launch
            }

            val transaction = Transaction(
                totalAmount = state.totalAmount,
                totalProfit = state.totalProfit,
                isUtang = isUtang,
                customerId = customerId,
                paymentMethod = state.paymentMethod
            )
            
            try {
                transactionRepository.executeSale(transaction, state.items, isUtang)
                
                // Print Receipt
                printerRepository.printReceipt(transaction, state.items)
                
                _uiState.value = CheckoutUiState() // Reset
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Checkout failed: ${e.message}") }
            }
        }
    }
}
