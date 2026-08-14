package com.amj_pos.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.amj_pos.data.local.entities.Transaction
import com.amj_pos.data.local.entities.TransactionItem
import com.amj_pos.domain.printer.BluetoothDeviceInfo
import com.amj_pos.domain.printer.PrinterRepository
import com.amj_pos.domain.printer.PrinterStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class BluetoothPrinterRepositoryImpl(private val context: Context) : PrinterRepository {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow(PrinterStatus.DISCONNECTED)
    override val connectionState: StateFlow<PrinterStatus> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    override val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val PREFS_NAME = "printer_prefs"
    private val KEY_PRINTER_ADDRESS = "last_printer_address"
    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @SuppressLint("MissingPermission")
    override fun getPairedDevices(): List<BluetoothDeviceInfo> {
        return bluetoothAdapter?.bondedDevices?.map { 
            BluetoothDeviceInfo(it.name ?: "Unknown", it.address)
        } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String?) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = PrinterStatus.ERROR
            return
        }

        val targetAddress = address ?: sharedPrefs.getString(KEY_PRINTER_ADDRESS, null)

        _connectionState.value = PrinterStatus.CONNECTING

        withContext(Dispatchers.IO) {
            try {
                val device = if (targetAddress != null) {
                    bluetoothAdapter.getRemoteDevice(targetAddress)
                } else {
                    // Auto-discovery: Find the first paired device that looks like a printer
                    bluetoothAdapter.bondedDevices.find { 
                        it.name?.contains("58", ignoreCase = true) == true || 
                        it.name?.contains("printer", ignoreCase = true) == true ||
                        it.name?.contains("JK", ignoreCase = true) == true ||
                        it.name?.contains("5801", ignoreCase = true) == true
                    }
                }

                if (device == null) {
                    _connectionState.value = PrinterStatus.ERROR
                    return@withContext
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                
                _connectedDeviceName.value = device.name
                _connectionState.value = PrinterStatus.CONNECTED
                
                // Save successful address for next time
                sharedPrefs.edit().putString(KEY_PRINTER_ADDRESS, device.address).apply()
                
            } catch (e: Exception) {
                e.printStackTrace()
                _connectedDeviceName.value = null
                _connectionState.value = PrinterStatus.ERROR
            }
        }
    }

    override fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _connectedDeviceName.value = null
            _connectionState.value = PrinterStatus.DISCONNECTED
        }
    }

    override suspend fun printTestPage(): Boolean {
        if (_connectionState.value != PrinterStatus.CONNECTED) return false
        
        return withContext(Dispatchers.IO) {
            try {
                val os = outputStream ?: return@withContext false
                val ESC: Byte = 27
                val init = byteArrayOf(ESC, 64)
                val center = byteArrayOf(ESC, 97, 1)
                
                os.write(init)
                os.write(center)
                os.write("\n\n--- AMJ POS ---\n".toByteArray())
                os.write("PRINTER TEST SUCCESSFUL\n".toByteArray())
                os.write("Model: JK-5801H Compatible\n".toByteArray())
                os.write("Date: ${Date()}\n".toByteArray())
                os.write("--------------------------------\n".toByteArray())
                os.write(byteArrayOf(10, 10, 10, 10)) // Feed
                os.flush()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun printReceipt(transaction: Transaction, items: List<TransactionItem>): Boolean {
        if (_connectionState.value != PrinterStatus.CONNECTED) {
            connect()
            if (_connectionState.value != PrinterStatus.CONNECTED) return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val os = outputStream ?: return@withContext false
                
                // ESC/POS Commands
                val ESC: Byte = 27
                val LF: Byte = 10
                
                val center = byteArrayOf(ESC, 97, 1)
                val left = byteArrayOf(ESC, 97, 0)
                val boldOn = byteArrayOf(ESC, 69, 1)
                val boldOff = byteArrayOf(ESC, 69, 0)
                val init = byteArrayOf(ESC, 64)

                os.write(init)
                os.write(center)
                os.write(boldOn)
                os.write("AMJ SARI-SARI STORE\n".toByteArray())
                os.write(boldOff)
                os.write("NHA Graceville, Mambuaya,\n".toByteArray())
                os.write("Cagayan de Oro City\n".toByteArray())
                
                val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                os.write("${sdf.format(Date())}\n".toByteArray())
                
                os.write("--------------------------------\n".toByteArray())
                os.write(left)
                
                items.forEach { item ->
                    val line = "${item.productName}\n"
                    val details = "  ${item.quantity} x P${item.sellPricePerPiece} = P${item.sellPricePerPiece * item.quantity}\n"
                    os.write(line.toByteArray())
                    os.write(details.toByteArray())
                }
                
                os.write("--------------------------------\n".toByteArray())
                os.write(boldOn)
                os.write("TOTAL: P${transaction.totalAmount}\n".toByteArray())
                os.write(boldOff)
                os.write(center)
                os.write("\nThank You! Come Again.\n".toByteArray())
                os.write(byteArrayOf(LF, LF, LF, LF)) // Feed lines
                
                os.flush()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
