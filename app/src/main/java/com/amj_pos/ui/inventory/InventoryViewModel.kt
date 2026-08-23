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
                Product(name = "Kopiko Brown Sachet", barcode = "123456", unitName = "Pack", piecesPerUnit = 12, unitPrice = 100.0, currentStock = 4.0),
                Product(name = "Lucky Me! Pancit Canton", barcode = "789012", unitName = "Box", piecesPerUnit = 50, unitPrice = 600.0, currentStock = 2.0),
                Product(name = "Coca-Cola 290ml", barcode = "345678", unitName = "Case", piecesPerUnit = 12, unitPrice = 240.0, currentStock = 2.0),
                Product(name = "Silver Swan Soy Sauce", barcode = "901234", unitName = "Pack", piecesPerUnit = 10, unitPrice = 150.0, currentStock = 2.0)
            )
            mockProducts.forEach { productRepository.upsertProduct(it) }
        }
    }

    fun archiveProduct(productId: Long) {
        viewModelScope.launch {
            productRepository.archiveProduct(productId)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }
}
