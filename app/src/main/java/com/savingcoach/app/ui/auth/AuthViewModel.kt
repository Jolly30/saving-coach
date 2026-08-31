package com.savingcoach.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.savingcoach.app.data.repository.UserRepository

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val needsOnboarding: Boolean = false,
    val error: String? = null,
    val emailOrUsername: String = "",
    val password: String = "",
    val username: String = "",
    val isSignUp: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkInitialAuthState()
    }

    private fun checkInitialAuthState() {
        if (authRepository.isUserSignedIn()) {
            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                viewModelScope.launch {
                    val userResult = userRepository.getUserProfile(uid)
                    val user = userResult.getOrNull()
                    if (user?.onboardingCompleted == false) {
                        _uiState.value = _uiState.value.copy(isSignedIn = false, needsOnboarding = true)
                    } else {
                        _uiState.value = _uiState.value.copy(isSignedIn = true, needsOnboarding = false)
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(isSignedIn = false, needsOnboarding = false)
            }
        }
    }

    fun onEmailOrUsernameChanged(input: String) {
        _uiState.value = _uiState.value.copy(emailOrUsername = input)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }
    
    fun onUsernameChanged(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun toggleSignUpMode() {
        val nextIsSignUp = !_uiState.value.isSignUp
        val defaultUsername = if (nextIsSignUp && _uiState.value.username.isBlank()) {
            "user_${UUID.randomUUID().toString().substring(0, 8)}"
        } else {
            _uiState.value.username
        }
        
        _uiState.value = _uiState.value.copy(
            isSignUp = nextIsSignUp,
            username = defaultUsername,
            error = null
        )
    }

    fun submit() {
        val state = _uiState.value
        if (state.emailOrUsername.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please enter all required fields")
            return
        }

        val finalUsername = if (state.isSignUp && state.username.isBlank()) {
            "user_${UUID.randomUUID().toString().substring(0, 8)}"
        } else {
            state.username
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = if (state.isSignUp) {
                authRepository.signUpWithEmail(state.emailOrUsername, state.password, finalUsername)
            } else {
                authRepository.signInWithEmailOrUsername(state.emailOrUsername, state.password)
            }

            result.fold(
                onSuccess = {
                    val uid = authRepository.getCurrentUserId()
                    if (uid != null) {
                        val userResult = userRepository.getUserProfile(uid)
                        val user = userResult.getOrNull()
                        if (user?.onboardingCompleted == false) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSignedIn = false,
                                needsOnboarding = true,
                                error = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSignedIn = true,
                                needsOnboarding = false,
                                error = null
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSignedIn = true,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Authentication failed"
                    )
                }
            )
        }
    }

    fun onGoogleIdTokenReceived(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.signInWithGoogle(idToken)

            result.fold(
                onSuccess = {
                    val uid = authRepository.getCurrentUserId()
                    if (uid != null) {
                        val userResult = userRepository.getUserProfile(uid)
                        val user = userResult.getOrNull()
                        if (user?.onboardingCompleted == false) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSignedIn = false,
                                needsOnboarding = true
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSignedIn = true,
                                needsOnboarding = false
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSignedIn = true
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Google sign-in failed"
                    )
                }
            )
        }
    }

    fun onGoogleSignInError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "Please enter your email to reset password")
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Password reset email sent!")
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
