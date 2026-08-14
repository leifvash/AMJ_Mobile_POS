package com.amj_pos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.printer.BluetoothDeviceInfo
import com.amj_pos.domain.printer.PrinterRepository
import com.amj_pos.domain.printer.PrinterStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PrinterSettingsUiState(
    val connectionStatus: PrinterStatus = PrinterStatus.DISCONNECTED,
    val connectedDeviceName: String? = null,
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val isLoading: Boolean = false
)

class PrinterSettingsViewModel(
    private val printerRepository: PrinterRepository
) : ViewModel() {

    val uiState: StateFlow<PrinterSettingsUiState> = combine(
        printerRepository.connectionState,
        printerRepository.connectedDeviceName
    ) { status, name ->
        PrinterSettingsUiState(
            connectionStatus = status,
            connectedDeviceName = name,
            pairedDevices = printerRepository.getPairedDevices()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrinterSettingsUiState()
    )

    fun connectToDevice(address: String) {
        viewModelScope.launch {
            printerRepository.connect(address)
        }
    }

    fun disconnect() {
        printerRepository.disconnect()
    }

    fun printTestPage() {
        viewModelScope.launch {
            printerRepository.printTestPage()
        }
    }

    fun refreshDevices() {
        // Force state update by repository if needed, but getPairedDevices() is called in combine
    }
}
