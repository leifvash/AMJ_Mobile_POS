package com.amj_pos.data.repository

import com.amj_pos.data.local.dao.CategoryDao
import com.amj_pos.data.local.entities.Category
import com.amj_pos.domain.repository.CategoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val firestore: FirebaseFirestore
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    
    override suspend fun addCategory(name: String): Long {
        val category = Category(name = name)
        val id = categoryDao.insertCategory(category)
        syncCategoryToFirestore(category.copy(id = id))
        return id
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
        firestore.collection("categories").document(category.id.toString()).delete()
    }

    override suspend fun syncFromFirestore() {
        try {
            val snapshot = firestore.collection("categories").get().await()
            val remoteCategories = snapshot.toObjects(Category::class.java)
            remoteCategories.forEach { category ->
                categoryDao.insertCategory(category)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncCategoryToFirestore(category: Category) {
        firestore.collection("categories")
            .document(category.id.toString())
            .set(category)
    }
}
