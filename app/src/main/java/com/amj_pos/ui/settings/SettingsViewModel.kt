package com.amj_pos.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val storeName: String = "",
    val storeAddress: String = "",
    val lowStockThreshold: String = "5",
    val lastBackupDate: String = "Never"
)

class SettingsViewModel(
    context: Context,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        _uiState.update { it.copy(
            storeName = sharedPrefs.getString("store_name", "AMJ SARI-SARI STORE") ?: "AMJ SARI-SARI STORE",
            storeAddress = sharedPrefs.getString("store_address", "NHA Graceville, Mambuaya, Cagayan de Oro City") ?: "",
            lowStockThreshold = sharedPrefs.getInt("low_stock_threshold", 5).toString()
        ) }
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

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
