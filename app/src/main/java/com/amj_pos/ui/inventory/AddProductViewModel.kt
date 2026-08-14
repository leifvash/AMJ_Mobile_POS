package com.amj_pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.scanner.BarcodeScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddProductUiState(
    val name: String = "",
    val barcode: String = "",
    val bulkCostPrice: String = "",
    val piecesPerBulk: String = "1",
    val pieceRetailPrice: String = "",
    val initialStockPieces: String = "0",
    val isScanning: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class AddProductViewModel(
    private val productRepository: ProductRepository,
    private val barcodeScanner: BarcodeScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState

    fun onNameChange(newName: String) = _uiState.update { it.copy(name = newName) }
    fun onBarcodeChange(newBarcode: String) = _uiState.update { it.copy(barcode = newBarcode) }
    fun onBulkCostPriceChange(newPrice: String) = _uiState.update { it.copy(bulkCostPrice = newPrice) }
    fun onPiecesPerBulkChange(newPieces: String) = _uiState.update { it.copy(piecesPerBulk = newPieces) }
    fun onPieceRetailPriceChange(newPrice: String) = _uiState.update { it.copy(pieceRetailPrice = newPrice) }
    fun onInitialStockChange(newStock: String) = _uiState.update { it.copy(initialStockPieces = newStock) }

    fun onMessageShown() = _uiState.update { it.copy(error = null) }

    fun scanBarcode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val result = barcodeScanner.scan()
            result.onSuccess { barcode ->
                if (barcode != null) {
                    _uiState.update { it.copy(barcode = barcode) }
                }
            }
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun saveProduct() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Product Name is required") }
            return
        }

        val bulkCost = state.bulkCostPrice.toDoubleOrNull()
        val piecesPerBulk = state.piecesPerBulk.toIntOrNull()
        val retailPrice = state.pieceRetailPrice.toDoubleOrNull()

        if (bulkCost == null || piecesPerBulk == null || retailPrice == null) {
            _uiState.update { it.copy(error = "Please fill in all prices and ratios correctly") }
            return
        }

        viewModelScope.launch {
            try {
                val product = Product(
                    name = state.name,
                    barcode = state.barcode.ifBlank { null },
                    bulkCostPrice = state.bulkCostPrice.toDoubleOrNull() ?: 0.0,
                    piecesPerBulk = state.piecesPerBulk.toIntOrNull() ?: 1,
                    pieceRetailPrice = state.pieceRetailPrice.toDoubleOrNull() ?: 0.0,
                    currentStockInPieces = state.initialStockPieces.toIntOrNull() ?: 0
                )
                productRepository.upsertProduct(product)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
