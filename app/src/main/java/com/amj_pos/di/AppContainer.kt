package com.amj_pos.di

import android.content.Context
import com.amj_pos.data.local.db.DatabaseProvider
import com.amj_pos.data.repository.ProductRepositoryImpl
import com.amj_pos.data.repository.TransactionRepositoryImpl
import com.amj_pos.data.repository.UtangRepositoryImpl
import com.amj_pos.data.scanner.GoogleBarcodeScanner
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.repository.UtangRepository
import com.amj_pos.domain.scanner.BarcodeScanner

/**
 * Dependency container for manual DI. 
 * Minimizes object allocations and keeps the app lightweight.
 */
interface AppContainer {
    val productRepository: ProductRepository
    val transactionRepository: TransactionRepository
    val utangRepository: UtangRepository
    val barcodeScanner: BarcodeScanner
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database by lazy { DatabaseProvider.getDatabase(context) }

    override val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(database.productDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database)
    }

    override val utangRepository: UtangRepository by lazy {
        UtangRepositoryImpl(database.utangDao())
    }

    override val barcodeScanner: BarcodeScanner by lazy {
        GoogleBarcodeScanner(context)
    }
}
