package com.amj_pos.domain.scanner

interface BarcodeScanner {
    /**
     * Triggers the barcode scanner UI.
     * Returns the scanned barcode string or null if cancelled/failed.
     */
    suspend fun scan(): Result<String?>
}
