package com.amj_pos.data.scanner

import android.content.Context
import com.amj_pos.domain.scanner.BarcodeScanner
import com.google.android.gms.code.scanner.GmsBarcodeScannerOptions
import com.google.android.gms.code.scanner.GmsBarcodeScanning
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementation of BarcodeScanner using Google Play Services Code Scanner API.
 * This does not require camera permissions as the UI is handled by Google Play Services.
 */
class GoogleBarcodeScanner(context: Context) : BarcodeScanner {

    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .enableAutoZoom()
        .build()

    private val scanner = GmsBarcodeScanning.getClient(context, options)

    override suspend fun scan(): Result<String?> = suspendCancellableCoroutine { continuation ->
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                continuation.resume(Result.success(barcode.rawValue))
            }
            .addOnCanceledListener {
                continuation.resume(Result.success(null))
            }
            .addOnFailureListener { e ->
                if (e is ApiException && e.statusCode == CommonStatusCodes.CANCELED) {
                    continuation.resume(Result.success(null))
                } else {
                    continuation.resume(Result.failure(e))
                }
            }
    }
}
