package com.example.zionkids.presentation.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.domain.repositories.online.AuthRepository
//import com.example.zionkids.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    var ui by mutableStateOf(AuthUiState())
        private set

    init {
        repo.currentUser()?.let {
            ui = ui.copy(isLoggedIn = true)
        }
    }

    fun onEmailChange(v: String) { ui = ui.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { ui = ui.copy(password = v, error = null) }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        ui = ui.copy(loading = true, error = null)
        repo.signIn(ui.email.trim(), ui.password)
            .onSuccess { ui = ui.copy(loading = false, isLoggedIn = true); onSuccess() }
            .onFailure { ui = ui.copy(loading = false, error = it.message ?: "Sign in failed") }
    }

    fun signUp(onSuccess: () -> Unit) = viewModelScope.launch {
        ui = ui.copy(loading = true, error = null)
        repo.signUp(ui.email.trim(), ui.password)
            .onSuccess { ui = ui.copy(loading = false, isLoggedIn = true); onSuccess() }
            .onFailure { ui = ui.copy(loading = false, error = it.message ?: "Sign up failed") }
    }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)
