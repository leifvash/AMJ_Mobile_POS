package com.amj_pos.domain.repository

import com.amj_pos.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val selectedBranch: Flow<String?>
    fun setSelectedBranch(branch: String?)
    fun getInventoryPassword(): Flow<String>
    suspend fun setInventoryPassword(password: String)
    suspend fun login(username: String, pass: String): Result<User>
    suspend fun logout()
    suspend fun fetchUserDetails(uid: String): User?
    suspend fun registerEmployee(name: String, username: String, pass: String): Result<Unit>
}
