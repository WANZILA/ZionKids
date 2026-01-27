package com.example.zionkids.presentation.viewModels

    import androidx.lifecycle.ViewModel
    import com.example.zionkids.domain.repositories.online.AuthRepository

    import dagger.hilt.android.lifecycle.HiltViewModel
    import javax.inject.Inject

    @HiltViewModel
    class SplashViewModel @Inject constructor(
        private val authRepo: AuthRepository
    ) : ViewModel() {
        fun isLoggedIn(): Boolean = authRepo.currentUser() != null
    }
