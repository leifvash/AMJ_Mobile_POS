package com.amj_pos.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.scanner.BarcodeScanner
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val items: List<TransactionItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val totalProfit: Double = 0.0,
    val isScanning: Boolean = false,
    val error: String? = null
)

class CheckoutViewModel(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val barcodeScanner: BarcodeScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState

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
            val existingItem = _uiState.value.items.find { it.productId == product.id }
            
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
                    transactionId = 0, // Set during execution
                    productId = product.id,
                    productName = product.name,
                    quantity = 1,
                    sellPricePerPiece = product.pieceRetailPrice,
                    costPricePerPiece = product.costPricePerPiece,
                    profit = product.pieceRetailPrice - product.costPricePerPiece
                )
            }
            
            updateTotals(updatedItems)
        } else {
            _uiState.update { it.copy(error = "Product not found") }
        }
    }

    private fun updateTotals(items: List<TransactionItem>) {
        val total = items.sumOf { it.sellPricePerPiece * it.quantity }
        val profit = items.sumOf { it.profit }
        _uiState.update { it.copy(items = items, totalAmount = total, totalProfit = profit) }
    }

    fun completeSale(customerId: Long? = null, isUtang: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.items.isEmpty()) return@launch
            
            val transaction = Transaction(
                totalAmount = state.totalAmount,
                totalProfit = state.totalProfit,
                isUtang = isUtang,
                customerId = customerId
            )
            
            try {
                transactionRepository.executeSale(transaction, state.items, isUtang)
                _uiState.value = CheckoutUiState() // Reset
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Checkout failed: ${e.message}") }
            }
        }
    }
}
