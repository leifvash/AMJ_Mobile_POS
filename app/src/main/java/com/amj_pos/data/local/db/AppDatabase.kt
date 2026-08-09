package com.amj_pos.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.amj_pos.data.local.converters.DataConverters
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
    version = 2,
    exportSchema = false
)
@TypeConverters(DataConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun utangDao(): UtangDao
    abstract fun transactionDao(): TransactionDao
}
