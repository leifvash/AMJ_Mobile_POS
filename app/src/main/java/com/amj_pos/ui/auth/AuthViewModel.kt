package com.amj_pos.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.model.User
import com.amj_pos.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class AuthViewModel(
    context: Context,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private var lastActivityTime = System.currentTimeMillis()
    private var timerJob: Job? = null

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val selectedBranch: StateFlow<String?> = authRepository.selectedBranch
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        startInactivityTimer()
    }

    private fun startInactivityTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val timeoutMinutes = sharedPrefs.getInt("inactivity_timeout", 0)
                if (timeoutMinutes > 0) {
                    val idleTime = System.currentTimeMillis() - lastActivityTime
                    if (idleTime > timeoutMinutes * 60 * 1000) {
                        logout()
                        break
                    }
                }
                delay(30.seconds) // Check every 30 seconds
            }
        }
    }

    fun resetTimeout() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun selectBranch(branch: String) {
        authRepository.setSelectedBranch(branch)
    }

    fun login(username: String, pass: String, onResult: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.login(username, pass)
            onResult(result)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
