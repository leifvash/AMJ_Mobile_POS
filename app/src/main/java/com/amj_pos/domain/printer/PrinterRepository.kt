package com.amj_pos.domain.printer

import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for managing the Bluetooth thermal printer.
 */
interface PrinterRepository {
    val connectionState: StateFlow<PrinterStatus>
    val connectedDeviceName: StateFlow<String?>

    suspend fun connect(address: String? = null)
    fun disconnect()
    suspend fun printReceipt(transaction: Transaction, items: List<TransactionItem>): Boolean
    suspend fun printTestPage(): Boolean
    fun getPairedDevices(): List<BluetoothDeviceInfo>
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)

enum class PrinterStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    ERROR
}
