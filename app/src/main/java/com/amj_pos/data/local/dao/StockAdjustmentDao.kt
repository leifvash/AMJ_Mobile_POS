package com.amj_pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.amj_pos.data.local.entities.StockAdjustment
import kotlinx.coroutines.flow.Flow

@Dao
interface StockAdjustmentDao {
    @Insert
    suspend fun insertAdjustment(adjustment: StockAdjustment)

    @Query("SELECT * FROM stock_adjustments WHERE productId = :productId ORDER BY timestamp DESC")
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAdjustments(): Flow<List<StockAdjustment>>
}
