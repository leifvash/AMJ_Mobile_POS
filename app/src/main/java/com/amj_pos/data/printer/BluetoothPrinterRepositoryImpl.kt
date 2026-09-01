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
                // Ensure previous resources are cleared before connecting
                closeResourcesInternal()

                val device = if (targetAddress != null) {
                    bluetoothAdapter.getRemoteDevice(targetAddress)
                } else {
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

                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    bluetoothSocket?.connect()
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        bluetoothSocket = method.invoke(device, 1) as BluetoothSocket
                        bluetoothSocket?.connect()
                    } catch (fallbackException: Exception) {
                        fallbackException.printStackTrace()
                        closeResourcesInternal()
                        _connectionState.value = PrinterStatus.ERROR
                        return@withContext
                    }
                }
                
                outputStream = bluetoothSocket?.outputStream
                _connectedDeviceName.value = device.name
                _connectionState.value = PrinterStatus.CONNECTED
                
                sharedPrefs.edit().putString(KEY_PRINTER_ADDRESS, device.address).apply()
                
            } catch (e: Exception) {
                e.printStackTrace()
                closeResourcesInternal()
                _connectedDeviceName.value = null
                _connectionState.value = PrinterStatus.ERROR
            }
        }
    }

    private fun closeResourcesInternal() {
        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (e: Exception) {
            // Ignored
        } finally {
            outputStream = null
        }

        try {
            bluetoothSocket?.close()
        } catch (e: Exception) {
            // Ignored
        } finally {
            bluetoothSocket = null
        }
    }

    override fun disconnect() {
        closeResourcesInternal()
        _connectedDeviceName.value = null
        _connectionState.value = PrinterStatus.DISCONNECTED
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
                os.write("32 CHARS LINE WIDTH TEST:\n".toByteArray())
                os.write("12345678901234567890123456789012\n".toByteArray())
                os.write("--------------------------------\n".toByteArray())
                os.write(byteArrayOf(10, 10, 10, 10))
                os.flush()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun formatLine(left: String, right: String): String {
        val totalWidth = 32
        val spaceCount = totalWidth - left.length - right.length
        return if (spaceCount > 0) {
            left + " ".repeat(spaceCount) + right
        } else {
            left.take(totalWidth - right.length - 1) + " " + right
        }
    }

    private fun formatQuantity(qty: Double): String {
        val whole = qty.toInt()
        val fraction = qty - whole
        return when {
            fraction == 0.0 -> whole.toString()
            fraction == 0.5 -> if (whole == 0) "1/2" else "$whole and 1/2"
            else -> String.format(Locale.getDefault(), "%.1f", qty)
        }
    }

    override suspend fun printReceipt(transaction: Transaction, items: List<TransactionItem>, cashierName: String?): Boolean {
        if (_connectionState.value != PrinterStatus.CONNECTED) {
            connect()
            if (_connectionState.value != PrinterStatus.CONNECTED) return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val os = outputStream ?: return@withContext false
                val ESC: Byte = 27
                val LF: Byte = 10
                val center = byteArrayOf(ESC, 97, 1)
                val left = byteArrayOf(ESC, 97, 0)
                val boldOn = byteArrayOf(ESC, 69, 1)
                val boldOff = byteArrayOf(ESC, 69, 0)
                val init = byteArrayOf(ESC, 64)

                val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                val storeName = appPrefs.getString("store_name", "AMJ SARI-SARI STORE") ?: "AMJ SARI-SARI STORE"

                os.write(init)
                os.write(center)
                os.write(boldOn)
                os.write("$storeName\n".toByteArray())
                os.write(boldOff)
                
                // Dynamic Branch Address
                when (transaction.branchName) {
                    "Bayanga" -> {
                        os.write("Branch: Bayanga\n".toByteArray())
                        os.write("Bayanga, Cagayan de Oro City\n".toByteArray())
                    }
                    "Mambuaya" -> {
                        os.write("Branch: Mambuaya\n".toByteArray())
                        os.write("NHA Graceville, Mambuaya,\n".toByteArray())
                        os.write("Cagayan de Oro City\n".toByteArray())
                    }
                    else -> {
                        // All Branches or unknown
                        os.write("Cagayan de Oro City\n".toByteArray())
                    }
                }
                
                // 12-hour AM/PM format
                val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                os.write("${sdf.format(Date(transaction.timestamp))}\n".toByteArray())
                
                cashierName?.let {
                    os.write("Cashier: ${it.take(23)}\n".toByteArray())
                }
                
                os.write("--------------------------------\n".toByteArray())
                
                os.write(left)
                items.forEach { item ->
                    os.write("${item.productName.take(32)}\n".toByteArray())
                    val qtyStr = formatQuantity(item.quantity)
                    val leftText = "$qtyStr ${item.unitName}"
                    val rightText = "P${item.sellPrice}"
                    os.write("${formatLine(leftText, rightText)}\n".toByteArray())
                }
                
                os.write("--------------------------------\n".toByteArray())
                os.write(boldOn)
                os.write("${formatLine("TOTAL:", "P${transaction.totalAmount}")}\n".toByteArray())
                os.write(boldOff)
                
                os.write(center)
                os.write("\nThank You! Come Again.\n".toByteArray())
                os.write(byteArrayOf(LF, LF, LF, LF))
                
                os.flush()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
