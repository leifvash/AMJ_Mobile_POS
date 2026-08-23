package com.amj_pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Category
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.repository.CategoryRepository
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.scanner.BarcodeScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddProductUiState(
    val name: String = "",
    val barcode: String = "",
    val hasNoBarcode: Boolean = false,
    val unitName: String = "Case", // Case, Pack, Box, etc.
    val piecesPerUnit: String = "1",
    val unitPrice: String = "",
    val initialStockUnits: String = "0",
    val category: String = "Uncategorized",
    val categories: List<Category> = emptyList(),
    val isScanning: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class AddProductViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val barcodeScanner: BarcodeScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onNameChange(newName: String) = _uiState.update { it.copy(name = newName) }
    fun onBarcodeChange(newBarcode: String) = _uiState.update { it.copy(barcode = newBarcode, hasNoBarcode = false) }
    fun onNoBarcodeToggle(checked: Boolean) = _uiState.update { it.copy(hasNoBarcode = checked, barcode = if (checked) "" else it.barcode) }
    fun onUnitNameChange(newUnit: String) = _uiState.update { it.copy(unitName = newUnit) }
    fun onPiecesPerUnitChange(newPieces: String) = _uiState.update { it.copy(piecesPerUnit = newPieces) }
    fun onUnitPriceChange(newPrice: String) = _uiState.update { it.copy(unitPrice = newPrice) }
    fun onInitialStockChange(newStock: String) = _uiState.update { it.copy(initialStockUnits = newStock) }
    fun onCategoryChange(newCategory: String) = _uiState.update { it.copy(category = newCategory) }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.addCategory(name)
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(error = null) }

    fun scanBarcode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, hasNoBarcode = false) }
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

        val piecesPerUnit = state.piecesPerUnit.toIntOrNull()
        val unitPrice = state.unitPrice.toDoubleOrNull()

        if (piecesPerUnit == null || unitPrice == null) {
            _uiState.update { it.copy(error = "Please fill in price and unit ratio correctly") }
            return
        }

        viewModelScope.launch {
            try {
                val product = Product(
                    name = state.name,
                    barcode = if (state.hasNoBarcode) null else state.barcode.ifBlank { null },
                    unitName = state.unitName,
                    piecesPerUnit = piecesPerUnit,
                    unitPrice = unitPrice,
                    currentStock = state.initialStockUnits.toDoubleOrNull() ?: 0.0,
                    category = state.category
                )
                productRepository.upsertProduct(product)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
