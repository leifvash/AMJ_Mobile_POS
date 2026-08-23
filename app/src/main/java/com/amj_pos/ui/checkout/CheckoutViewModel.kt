package com.amj_pos.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.repository.UtangRepository
import com.amj_pos.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository,
    private val barcodeScanner: BarcodeScanner,
    private val printerRepository: PrinterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState

    private val _manualSearchQuery = MutableStateFlow("")
    val manualSearchQuery: StateFlow<String> = _manualSearchQuery

    private var currentUserName: String? = null
    private var selectedBranch: String = ""

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                currentUserName = user?.name
            }
        }

        viewModelScope.launch {
            authRepository.selectedBranch.collect { branch ->
                selectedBranch = branch ?: ""
            }
        }
        
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
        val existingItem = _uiState.value.items.find { it.productId == product.id && it.unitType == "Whole" }
        val currentQtyInCart = _uiState.value.items.filter { it.productId == product.id }.sumOf { it.quantity }
        
        if (product.currentStock < (currentQtyInCart + 1.0)) {
            _uiState.update { it.copy(error = "Out of stock: ${product.name}") }
            return
        }

        val updatedItems = if (existingItem != null) {
            _uiState.value.items.map {
                if (it.productId == product.id && it.unitType == "Whole") {
                    it.copy(
                        quantity = it.quantity + 1.0,
                        sellPrice = it.sellPrice + product.unitPrice
                    )
                } else it
            }
        } else {
            _uiState.value.items + TransactionItem(
                transactionId = 0,
                productId = product.id,
                productName = product.name,
                quantity = 1.0,
                sellPrice = product.unitPrice,
                unitType = "Whole",
                unitName = product.unitName
            )
        }
        
        updateTotals(updatedItems)
    }

    fun toggleUnit(productId: Long, currentUnit: String) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId) ?: return@launch

            val items = _uiState.value.items.toMutableList()
            val index = items.indexOfFirst { it.productId == productId && it.unitType == currentUnit }
            if (index == -1) return@launch

            val nextUnit = if (currentUnit == "Whole") "Half" else "Whole"

            val unitCountPerItem = if (nextUnit == "Half") 0.5 else 1.0
            val unitPrice = if (nextUnit == "Half") product.unitPrice / 2.0 else product.unitPrice
            
            // Calculate how many items are in this line
            val itemCount = if (currentUnit == "Half") {
                (items[index].quantity / 0.5).toInt()
            } else {
                items[index].quantity.toInt()
            }

            val newTotalQuantity = itemCount * unitCountPerItem
            val newTotalSellPrice = itemCount * unitPrice

            // Check stock
            val otherItemsQty = items.filterIndexed { i, item -> i != index && item.productId == productId }.sumOf { it.quantity }
            if (product.currentStock < (otherItemsQty + newTotalQuantity)) {
                _uiState.update { it.copy(error = "Not enough stock for $nextUnit unit") }
                return@launch
            }

            items[index] = items[index].copy(
                unitType = nextUnit,
                quantity = newTotalQuantity,
                sellPrice = newTotalSellPrice
            )
            updateTotals(items)
        }
    }

    fun removeItemFromCart(productId: Long, unitType: String) {
        val updatedItems = _uiState.value.items.filterNot { it.productId == productId && it.unitType == unitType }
        updateTotals(updatedItems)
    }

    fun updateQuantity(productId: Long, unitType: String, delta: Int) {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId) ?: return@launch
            val items = _uiState.value.items.toMutableList()
            val index = items.indexOfFirst { it.productId == productId && it.unitType == unitType }
            
            if (index != -1) {
                val currentItem = items[index]
                
                val perUnitQty = if (unitType == "Half") 0.5 else 1.0
                val perUnitPrice = if (unitType == "Half") product.unitPrice / 2.0 else product.unitPrice
                
                val newQty = currentItem.quantity + (delta * perUnitQty)
                val newPrice = currentItem.sellPrice + (delta * perUnitPrice)
                
                if (newQty <= 0) {
                    items.removeAt(index)
                } else {
                    val otherItemsQty = items.filterIndexed { i, item -> i != index && item.productId == productId }.sumOf { it.quantity }
                    if (newQty + otherItemsQty > product.currentStock) {
                        _uiState.update { it.copy(error = "Only ${product.currentStock} ${product.unitName}s left in stock.") }
                        return@launch
                    }
                    items[index] = currentItem.copy(quantity = newQty, sellPrice = newPrice)
                }
                updateTotals(items)
            }
        }
    }

    fun updateTotals(items: List<TransactionItem>) {
        val total = items.sumOf { it.sellPrice }
        _uiState.update { it.copy(items = items, totalAmount = total) }
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
                isUtang = isUtang,
                customerId = customerId,
                paymentMethod = state.paymentMethod,
                branchName = selectedBranch
            )
            
            try {
                transactionRepository.executeSale(transaction, state.items, isUtang)
                
                // Print Receipt with Cashier Name
                printerRepository.printReceipt(transaction, state.items, currentUserName)
                
                _uiState.value = CheckoutUiState() // Reset
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Checkout failed: ${e.message}") }
            }
        }
    }
}
