package com.amj_pos.domain.repository

import com.amj_pos.data.local.entities.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun addCategory(name: String): Long
    suspend fun deleteCategory(category: Category)
    suspend fun syncFromFirestore()
}
