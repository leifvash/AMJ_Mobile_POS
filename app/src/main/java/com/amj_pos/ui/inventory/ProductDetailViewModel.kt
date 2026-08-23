package com.amj_pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.data.local.entities.StockAdjustment
import com.amj_pos.domain.repository.AuthRepository
import com.amj_pos.domain.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val name: String = "",
    val unitName: String = "",
    val unitPrice: String = "",
    val piecesPerUnit: String = "",
    val stock: String = "",
    val adjustments: List<StockAdjustment> = emptyList(),
    val isSaved: Boolean = false,
    val error: String? = null
)

class ProductDetailViewModel(
    private val productRepository: ProductRepository,
    private val authRepository: AuthRepository,
    private val productId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState

    private var currentBranch = ""

    init {
        viewModelScope.launch {
            authRepository.selectedBranch.collect { branch ->
                currentBranch = branch ?: ""
            }
        }

        viewModelScope.launch {
            val productFlow = flow { emit(productRepository.getProductById(productId)) }
            val adjustmentsFlow = productRepository.getStockAdjustments(productId)

            combine(productFlow, adjustmentsFlow) { product, adjustments ->
                product?.let { p ->
                    _uiState.update { it.copy(
                        product = p,
                        name = p.name,
                        unitName = p.unitName,
                        unitPrice = p.unitPrice.toString(),
                        piecesPerUnit = p.piecesPerUnit.toString(),
                        stock = p.currentStock.toString(),
                        adjustments = adjustments
                    ) }
                }
            }.collect()
        }
    }

    fun onUnitNameChange(newName: String) = _uiState.update { it.copy(unitName = newName) }
    fun onUnitPriceChange(newPrice: String) = _uiState.update { it.copy(unitPrice = newPrice) }
    fun onPiecesPerUnitChange(newPieces: String) = _uiState.update { it.copy(piecesPerUnit = newPieces) }
    fun onStockChange(newStock: String) = _uiState.update { it.copy(stock = newStock) }
    fun onMessageShown() = _uiState.update { it.copy(error = null) }

    fun adjustStock(amount: Double, reason: String) {
        viewModelScope.launch {
            val adjustment = StockAdjustment(
                productId = productId,
                adjustmentAmount = amount,
                reason = reason,
                branchName = currentBranch
            )
            productRepository.adjustStock(adjustment)
        }
    }

    fun updateProduct() {
        val state = _uiState.value
        val product = state.product ?: return
        
        val newUnitPrice = state.unitPrice.toDoubleOrNull()
        val newPiecesPerUnit = state.piecesPerUnit.toIntOrNull()
        val newStock = state.stock.toDoubleOrNull()

        if (newUnitPrice == null || newPiecesPerUnit == null || newStock == null) {
            _uiState.update { it.copy(error = "Please enter valid numbers") }
            return
        }

        viewModelScope.launch {
            try {
                val updatedProduct = product.copy(
                    unitName = state.unitName,
                    unitPrice = newUnitPrice,
                    piecesPerUnit = newPiecesPerUnit,
                    currentStock = newStock,
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
