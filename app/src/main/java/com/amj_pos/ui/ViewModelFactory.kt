package com.amj_pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amj_pos.di.AppContainer
import com.amj_pos.ui.checkout.CheckoutViewModel
import com.amj_pos.ui.scanner.BarcodeViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(BarcodeViewModel::class.java) -> {
                BarcodeViewModel(container.barcodeScanner) as T
            }
            modelClass.isAssignableFrom(CheckoutViewModel::class.java) -> {
                CheckoutViewModel(
                    container.productRepository,
                    container.transactionRepository,
                    container.barcodeScanner
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
