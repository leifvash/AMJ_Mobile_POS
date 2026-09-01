package com.amj_pos.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amj_pos.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrationUiState(
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class EmployeeRegistrationViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }
    fun onMessageShown() = _uiState.update { it.copy(error = null) }

    fun register() {
        val state = _uiState.value
        if (state.name.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "All fields are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.registerEmployee(
                state.name, state.username, state.password
            )
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, success = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun resetSuccess() = _uiState.update { it.copy(success = false, name = "", username = "", password = "") }
}
