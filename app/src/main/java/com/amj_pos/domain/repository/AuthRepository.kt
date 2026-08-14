package com.amj_pos.domain.repository

import com.amj_pos.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun login(email: String, pass: String): Result<User>
    suspend fun logout()
    suspend fun fetchUserDetails(uid: String): User?
    suspend fun registerEmployee(name: String, email: String, pass: String, branchId: String): Result<Unit>
}
