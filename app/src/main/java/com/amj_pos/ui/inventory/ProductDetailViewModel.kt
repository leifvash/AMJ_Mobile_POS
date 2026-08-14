package com.amj_pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val name: String = "",
    val price: String = "",
    val stock: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
)

class ProductDetailViewModel(
    private val productRepository: ProductRepository,
    private val productId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            val product = productRepository.getProductById(productId)
            product?.let { p ->
                _uiState.update { it.copy(
                    product = p,
                    name = p.name,
                    price = p.pieceRetailPrice.toString(),
                    stock = p.currentStockInPieces.toString()
                ) }
            }
        }
    }

    fun onPriceChange(newPrice: String) = _uiState.update { it.copy(price = newPrice) }
    fun onStockChange(newStock: String) = _uiState.update { it.copy(stock = newStock) }
    fun onMessageShown() = _uiState.update { it.copy(error = null) }

    fun updateProduct() {
        val state = _uiState.value
        val product = state.product ?: return
        
        val newPrice = state.price.toDoubleOrNull()
        val newStock = state.stock.toIntOrNull()

        if (newPrice == null || newStock == null) {
            _uiState.update { it.copy(error = "Please enter valid price and stock numbers") }
            return
        }

        viewModelScope.launch {
            try {
                val updatedProduct = product.copy(
                    pieceRetailPrice = newPrice,
                    currentStockInPieces = newStock,
                    updatedAt = System.currentTimeMillis()
                )
                productRepository.upsertProduct(updatedProduct)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
