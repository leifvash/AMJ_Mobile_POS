package com.amj_pos.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amj_pos.data.local.db.DatabaseProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProductSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val productId = inputData.getLong("productId", -1L)
        if (productId == -1L) return Result.failure()

        return try {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val product = db.productDao().getProductById(productId) ?: return Result.failure()

            FirebaseFirestore.getInstance().collection("products")
                .document(product.id.toString())
                .set(product)
                .await()

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
