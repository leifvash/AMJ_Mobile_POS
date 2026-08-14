package com.amj_pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amj_pos.di.AppContainer
import com.amj_pos.ui.analytics.AnalyticsViewModel
import com.amj_pos.ui.checkout.CheckoutViewModel
import com.amj_pos.ui.dashboard.DashboardViewModel
import com.amj_pos.ui.inventory.AddProductViewModel
import com.amj_pos.ui.inventory.InventoryViewModel
import com.amj_pos.ui.inventory.ProductDetailViewModel
import com.amj_pos.ui.settings.PrinterSettingsViewModel
import com.amj_pos.ui.settings.SettingsViewModel
import com.amj_pos.ui.transactions.TransactionHistoryViewModel
import com.amj_pos.ui.utang.CustomerDetailViewModel
import com.amj_pos.ui.utang.UtangViewModel
import com.amj_pos.ui.scanner.BarcodeViewModel

class ViewModelFactory(
    private val container: AppContainer,
    private val entityId: Long? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    container.transactionRepository,
                    container.utangRepository,
                    container.productRepository,
                    container.printerRepository
                ) as T
            }
            modelClass.isAssignableFrom(CustomerDetailViewModel::class.java) -> {
                CustomerDetailViewModel(
                    container.utangRepository,
                    entityId ?: throw IllegalArgumentException("ID required")
                ) as T
            }
            modelClass.isAssignableFrom(ProductDetailViewModel::class.java) -> {
                ProductDetailViewModel(
                    container.productRepository,
                    entityId ?: throw IllegalArgumentException("ID required")
                ) as T
            }
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
                AnalyticsViewModel(container.transactionRepository) as T
            }
            modelClass.isAssignableFrom(TransactionHistoryViewModel::class.java) -> {
                TransactionHistoryViewModel(container.transactionRepository) as T
            }
            modelClass.isAssignableFrom(InventoryViewModel::class.java) -> {
                InventoryViewModel(
                    container.productRepository,
                    container.barcodeScanner
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    container.productRepository,
                    container.transactionRepository,
                    container.utangRepository
                ) as T
            }
            modelClass.isAssignableFrom(PrinterSettingsViewModel::class.java) -> {
                PrinterSettingsViewModel(container.printerRepository) as T
            }
            modelClass.isAssignableFrom(UtangViewModel::class.java) -> {
                UtangViewModel(container.utangRepository) as T
            }
            modelClass.isAssignableFrom(AddProductViewModel::class.java) -> {
                AddProductViewModel(
                    container.productRepository,
                    container.barcodeScanner
                ) as T
            }
            modelClass.isAssignableFrom(BarcodeViewModel::class.java) -> {
                BarcodeViewModel(container.barcodeScanner) as T
            }
            modelClass.isAssignableFrom(CheckoutViewModel::class.java) -> {
                CheckoutViewModel(
                    container.productRepository,
                    container.transactionRepository,
                    container.utangRepository,
                    container.barcodeScanner,
                    container.printerRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
