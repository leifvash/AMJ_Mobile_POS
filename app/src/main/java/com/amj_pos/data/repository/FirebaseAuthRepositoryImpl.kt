package com.amj_pos.data.repository

import com.amj_pos.domain.model.User
import com.amj_pos.domain.repository.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val _selectedBranch = MutableStateFlow<String?>(null)
    override val selectedBranch: Flow<String?> = _selectedBranch.asStateFlow()

    override fun setSelectedBranch(branch: String?) {
        _selectedBranch.value = branch
    }

    override fun getInventoryPassword(): Flow<String> = callbackFlow {
        val listener = firestore.collection("settings").document("inventory")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.getString("password") ?: "")
                } else {
                    trySend("")
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun setInventoryPassword(password: String) {
        firestore.collection("settings").document("inventory")
            .set(mapOf("password" to password)).await()
    }

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            } else {
                // Fetch full details from Firestore
                // Note: In a real app, you might want to handle this more robustly
                firestore.collection("users").document(firebaseUser.uid).get()
                    .addOnSuccessListener { doc ->
                        val user = doc.toObject(User::class.java)
                        trySend(user)
                    }
                    .addOnFailureListener {
                        trySend(null)
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun login(username: String, pass: String): Result<User> {
        return try {
            val email = if (username.contains("@")) username else "$username@amjpos.com"
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw Exception("Login failed")
            val user = fetchUserDetails(uid) ?: throw Exception("User data not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
        _selectedBranch.value = null
    }

    override suspend fun fetchUserDetails(uid: String): User? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun registerEmployee(name: String, username: String, pass: String, branchId: String): Result<Unit> {
        return try {
            val email = if (username.contains("@")) username else "$username@amjpos.com"
            // Use a secondary Firebase app to register the employee without logging out the owner
            val defaultApp = FirebaseApp.getInstance()
            val secondaryAppName = "SecondaryApp"
            val secondaryApp = try {
                FirebaseApp.getInstance(secondaryAppName)
            } catch (e: Exception) {
                FirebaseApp.initializeApp(defaultApp.applicationContext, defaultApp.options, secondaryAppName)
            }
            
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
            
            val result = secondaryAuth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw Exception("Registration failed")
            
            val employee = User(
                uid = uid,
                name = name,
                email = email,
                role = "employee",
                assigned_branch = branchId
            )
            
            firestore.collection("users").document(uid).set(employee).await()
            
            // Log out the secondary app immediately so it doesn't hold a session
            secondaryAuth.signOut()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
