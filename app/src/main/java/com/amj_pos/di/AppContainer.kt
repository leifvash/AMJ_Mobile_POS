package com.amj_pos.di

import android.content.Context
import com.amj_pos.data.local.db.DatabaseProvider
import com.amj_pos.data.repository.ProductRepositoryImpl
import com.amj_pos.data.repository.TransactionRepositoryImpl
import com.amj_pos.data.repository.UtangRepositoryImpl
import com.amj_pos.data.scanner.GoogleBarcodeScanner
import com.amj_pos.data.printer.BluetoothPrinterRepositoryImpl
import com.amj_pos.data.repository.FirebaseAuthRepositoryImpl
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import com.amj_pos.domain.repository.UtangRepository
import com.amj_pos.domain.scanner.BarcodeScanner
import com.amj_pos.domain.printer.PrinterRepository
import com.amj_pos.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dependency container for manual DI. 
 * Minimizes object allocations and keeps the app lightweight.
 */
interface AppContainer {
    val context: Context
    val productRepository: ProductRepository
    val transactionRepository: TransactionRepository
    val utangRepository: UtangRepository
    val barcodeScanner: BarcodeScanner
    val printerRepository: PrinterRepository
    val authRepository: AuthRepository
}

class AppContainerImpl(override val context: Context) : AppContainer {
    private val database by lazy { DatabaseProvider.getDatabase(context) }
    
    private val firestore by lazy { 
        FirebaseFirestore.getInstance()
    }
    
    init {
        // Optimize: Initialize Firestore settings in background
        CoroutineScope(Dispatchers.IO).launch {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
        }
    }
    
    private val auth by lazy { FirebaseAuth.getInstance() }

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

    override val printerRepository: PrinterRepository by lazy {
        BluetoothPrinterRepositoryImpl(context)
    }

    override val authRepository: AuthRepository by lazy {
        FirebaseAuthRepositoryImpl(auth, firestore)
    }
}
