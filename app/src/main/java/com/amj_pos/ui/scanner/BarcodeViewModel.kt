package com.amj_pos.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.scanner.BarcodeScanner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BarcodeViewModel(private val barcodeScanner: BarcodeScanner) : ViewModel() {

    private val _scanResult = MutableSharedFlow<String?>()
    val scanResult = _scanResult.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    fun startScan() {
        viewModelScope.launch {
            val result = barcodeScanner.scan()
            result.onSuccess { barcode ->
                _scanResult.emit(barcode)
            }.onFailure { e ->
                _error.emit(e.message ?: "Scanning failed")
            }
        }
    }
}
