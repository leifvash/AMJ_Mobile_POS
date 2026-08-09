package com.amj_pos.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.amj_pos.data.local.dao.ProductDao
import com.amj_pos.data.local.dao.TransactionDao
import com.amj_pos.data.local.dao.UtangDao
import com.amj_pos.data.local.entities.*

@Database(
    entities = [
        Product::class,
        Customer::class,
        UtangRecord::class,
        Transaction::class,
        TransactionItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun utangDao(): UtangDao
    abstract fun transactionDao(): TransactionDao
}
