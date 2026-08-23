package com.amj_pos.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amj_pos.data.local.db.DatabaseProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TransactionSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val transactionId = inputData.getLong("transactionId", -1L)
        if (transactionId == -1L) return Result.failure()

        return try {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val transaction = db.transactionDao().getTransactionById(transactionId) ?: return Result.failure()
            val items = db.transactionDao().getItemsForTransaction(transactionId)

            val transactionData = hashMapOf(
                "remoteId" to transaction.remoteId,
                "timestamp" to transaction.timestamp,
                "totalAmount" to transaction.totalAmount,
                "isUtang" to transaction.isUtang,
                "customerId" to transaction.customerId,
                "paymentMethod" to transaction.paymentMethod.name,
                "branchName" to transaction.branchName,
                "items" to items.map { item ->
                    hashMapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "quantity" to item.quantity,
                        "sellPrice" to item.sellPrice,
                        "unitType" to item.unitType,
                        "unitName" to item.unitName
                    )
                }
            )

            FirebaseFirestore.getInstance().collection("transactions")
                .document(transaction.remoteId)
                .set(transactionData)
                .await()

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
