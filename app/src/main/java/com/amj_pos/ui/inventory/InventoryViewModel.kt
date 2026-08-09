package com.amj_pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.scanner.BarcodeScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class InventoryViewModel(
    private val productRepository: ProductRepository,
    private val barcodeScanner: BarcodeScanner
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InventoryUiState> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                productRepository.getAllProducts()
            } else {
                productRepository.searchProducts(query)
            }
        }
        .map { products -> InventoryUiState(products = products) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InventoryUiState(isLoading = true)
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun seedMockData() {
        viewModelScope.launch {
            val mockProducts = listOf(
                Product(name = "Kopiko Brown Sachet", barcode = "123456", bulkCostPrice = 100.0, piecesPerBulk = 12, pieceRetailPrice = 10.0, currentStockInPieces = 48),
                Product(name = "Lucky Me! Pancit Canton", barcode = "789012", bulkCostPrice = 600.0, piecesPerBulk = 50, pieceRetailPrice = 15.0, currentStockInPieces = 100),
                Product(name = "Coca-Cola 290ml", barcode = "345678", bulkCostPrice = 240.0, piecesPerBulk = 12, pieceRetailPrice = 25.0, currentStockInPieces = 24),
                Product(name = "Silver Swan Soy Sauce", barcode = "901234", bulkCostPrice = 150.0, piecesPerBulk = 10, pieceRetailPrice = 18.0, currentStockInPieces = 20)
            )
            mockProducts.forEach { productRepository.upsertProduct(it) }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }

    fun addBulkStock(productId: Long, bulkQuantity: Int) {
        viewModelScope.launch {
            productRepository.addBulkStock(productId, bulkQuantity)
        }
    }
}
