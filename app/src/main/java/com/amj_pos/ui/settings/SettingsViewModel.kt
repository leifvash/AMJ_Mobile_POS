package com.amj_pos.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.repository.AuthRepository
import com.amj_pos.domain.repository.CategoryRepository
import com.amj_pos.domain.repository.ProductRepository
import com.amj_pos.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUiState(
    val storeName: String = "",
    val storeAddress: String = "",
    val lowStockThreshold: String = "5",
    val inactivityTimeout: String = "0", // 0 means disabled
    val inventoryPassword: String = "",
    val lastBackupDate: String = "Never",
    val isSyncing: Boolean = false,
    val userRole: String = "employee",
    val syncMessage: String? = null
)

class SettingsViewModel(
    context: Context,
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        _uiState.update { it.copy(
            storeName = sharedPrefs.getString("store_name", "AMJ SARI-SARI STORE") ?: "AMJ SARI-SARI STORE",
            storeAddress = sharedPrefs.getString("store_address", "NHA Graceville, Mambuaya, Cagayan de Oro City") ?: "",
            lowStockThreshold = sharedPrefs.getInt("low_stock_threshold", 5).toString(),
            inactivityTimeout = sharedPrefs.getInt("inactivity_timeout", 0).toString(),
            lastBackupDate = sharedPrefs.getString("last_sync_date", "Never") ?: "Never"
        ) }

        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(userRole = user?.role?.lowercase() ?: "employee") }
            }
        }

        viewModelScope.launch {
            authRepository.getInventoryPassword().collect { password ->
                _uiState.update { it.copy(inventoryPassword = password) }
            }
        }
    }

    fun onInventoryPasswordChange(password: String) {
        _uiState.update { it.copy(inventoryPassword = password) }
        viewModelScope.launch {
            authRepository.setInventoryPassword(password)
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = "Syncing with cloud...") }
            try {
                productRepository.syncFromFirestore()
                categoryRepository.syncFromFirestore()
                transactionRepository.syncTransactionsFromFirestore()
                
                val now = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())
                sharedPrefs.edit().putString("last_sync_date", now).apply()
                
                _uiState.update { it.copy(
                    isSyncing = false, 
                    lastBackupDate = now,
                    syncMessage = "Sync complete!"
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, syncMessage = "Sync failed: ${e.message}") }
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }

    fun onStoreNameChange(name: String) {
        _uiState.update { it.copy(storeName = name) }
        sharedPrefs.edit().putString("store_name", name).apply()
    }

    fun onStoreAddressChange(address: String) {
        _uiState.update { it.copy(storeAddress = address) }
        sharedPrefs.edit().putString("store_address", address).apply()
    }

    fun onThresholdChange(value: String) {
        _uiState.update { it.copy(lowStockThreshold = value) }
        value.toIntOrNull()?.let {
            sharedPrefs.edit().putInt("low_stock_threshold", it).apply()
        }
    }

    fun onTimeoutChange(value: String) {
        _uiState.update { it.copy(inactivityTimeout = value) }
        value.toIntOrNull()?.let {
            sharedPrefs.edit().putInt("inactivity_timeout", it).apply()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
