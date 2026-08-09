package com.amj_pos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.data.local.entities.Product
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.repository.UtangRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository,
    private val utangRepository: UtangRepository
) : ViewModel() {

    fun clearAllData() {
        viewModelScope.launch {
            transactionRepository.deleteAllTransactions()
            productRepository.deleteAllProducts()
            utangRepository.deleteAllUtangData()
        }
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
}
