package com.amj_pos.domain.model

import com.google.firebase.Timestamp

data class Receipt(
    val receiptId: String = "",
    val branchId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val totalAmount: Double = 0.0,
    val items: List<ReceiptItem> = emptyList(),
    val printStatus: PrintStatus = PrintStatus.PENDING
)

data class ReceiptItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
)

enum class PrintStatus {
    PENDING,
    PRINTED
}
