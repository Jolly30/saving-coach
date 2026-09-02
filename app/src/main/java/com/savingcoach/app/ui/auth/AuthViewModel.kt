package com.savingcoach.app.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

sealed interface AuthNavigationEvent {
    object NavigateToDashboard : AuthNavigationEvent
    object NavigateToOnboarding : AuthNavigationEvent
    object NavigateToVerifyEmail : AuthNavigationEvent
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val needsOnboarding: Boolean = false,
    val needsEmailVerification: Boolean = false,
    val verificationEmail: String = "",
    val resendSuccessMessage: String? = null,
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

    private val _navigationEvents = MutableSharedFlow<AuthNavigationEvent>(replay = 0, extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<AuthNavigationEvent> = _navigationEvents.asSharedFlow()

    fun syncAuthState() {
        _uiState.value = AuthUiState(
            isLoading = false,
            isSignedIn = false,
            needsOnboarding = false,
            needsEmailVerification = false,
            verificationEmail = "",
            resendSuccessMessage = null,
            error = null,
            emailOrUsername = "",
            password = "",
            username = "",
            isSignUp = _uiState.value.isSignUp
        )
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

    fun setSignUpMode(isSignUp: Boolean) {
        val defaultUsername = if (isSignUp && _uiState.value.username.isBlank()) {
            "user_${UUID.randomUUID().toString().substring(0, 8)}"
        } else {
            _uiState.value.username
        }
        _uiState.value = _uiState.value.copy(
            isSignUp = isSignUp,
            username = defaultUsername,
            error = null
        )
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
        val emailInput = state.emailOrUsername.trim()
        val passwordInput = state.password

        if (emailInput.isBlank() || passwordInput.isBlank()) {
            _uiState.value = state.copy(error = "Please enter all required fields")
            return
        }

        if (state.isSignUp) {
            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                _uiState.value = state.copy(error = "Please enter a valid email address")
                return
            }
            if (passwordInput.length < 6) {
                _uiState.value = state.copy(error = "Password must be at least 6 characters")
                return
            }
        }

        val finalUsername = if (state.isSignUp && state.username.isBlank()) {
            "user_${UUID.randomUUID().toString().substring(0, 8)}"
        } else {
            state.username
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            if (state.isSignUp) {
                val result = authRepository.signUpWithEmail(emailInput, passwordInput, finalUsername)
                result.fold(
                    onSuccess = {
                        val uid = authRepository.getCurrentUserId()
                        if (uid != null) {
                            userRepository.createUserProfile(
                                com.savingcoach.app.data.model.User(
                                    uid = uid,
                                    email = emailInput,
                                    username = finalUsername,
                                    onboardingCompleted = false
                                )
                            )
                        }
                        // Send email verification immediately
                        authRepository.sendEmailVerification()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            needsEmailVerification = true,
                            verificationEmail = emailInput,
                            error = null
                        )
                        viewModelScope.launch {
                            _navigationEvents.emit(AuthNavigationEvent.NavigateToVerifyEmail)
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Registration failed"
                        )
                    }
                )
            } else {
                val emailToSignIn = if (!emailInput.contains("@")) {
                    val emailResult = userRepository.getEmailByUsername(emailInput)
                    val foundEmail = emailResult.getOrNull()
                    if (foundEmail.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No account found with username '$emailInput'"
                        )
                        return@launch
                    }
                    foundEmail
                } else {
                    emailInput
                }

                val result = authRepository.signInWithEmail(emailToSignIn, passwordInput)
                result.fold(
                    onSuccess = {
                        if (!authRepository.isEmailVerified()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                needsEmailVerification = true,
                                verificationEmail = authRepository.getCurrentUserEmail() ?: emailToSignIn,
                                error = null
                            )
                            viewModelScope.launch {
                                _navigationEvents.emit(AuthNavigationEvent.NavigateToVerifyEmail)
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                            val uid = authRepository.getCurrentUserId()
                            if (uid != null) {
                                viewModelScope.launch {
                                    val userResult = userRepository.getUserProfile(uid)
                                    val user = userResult.getOrNull()
                                    if (user?.onboardingCompleted == false) {
                                        _navigationEvents.emit(AuthNavigationEvent.NavigateToOnboarding)
                                    } else {
                                        _navigationEvents.emit(AuthNavigationEvent.NavigateToDashboard)
                                    }
                                }
                            } else {
                                viewModelScope.launch {
                                    _navigationEvents.emit(AuthNavigationEvent.NavigateToDashboard)
                                }
                            }
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
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val reloadResult = authRepository.reloadUser()
            
            if (reloadResult.isSuccess && reloadResult.getOrNull() == true) {
                _uiState.value = _uiState.value.copy(isLoading = false, needsEmailVerification = false, error = null)
                val uid = authRepository.getCurrentUserId()
                if (uid != null) {
                    val userResult = userRepository.getUserProfile(uid)
                    val user = userResult.getOrNull()
                    if (user?.onboardingCompleted == false) {
                        _navigationEvents.emit(AuthNavigationEvent.NavigateToOnboarding)
                    } else {
                        _navigationEvents.emit(AuthNavigationEvent.NavigateToDashboard)
                    }
                } else {
                    _navigationEvents.emit(AuthNavigationEvent.NavigateToDashboard)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Email is not verified yet. Please check your inbox and click the verification link."
                )
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, resendSuccessMessage = null)
            val result = authRepository.sendEmailVerification()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    resendSuccessMessage = "Verification email resent! Please check your inbox."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to resend verification email"
                )
            }
        }
    }

    fun resetToSignIn() {
        _uiState.value = AuthUiState(
            isLoading = false,
            isSignedIn = false,
            needsOnboarding = false,
            needsEmailVerification = false,
            verificationEmail = "",
            resendSuccessMessage = null,
            error = null,
            emailOrUsername = "",
            password = "",
            username = "",
            isSignUp = false
        )
    }

    fun prepareForChangeEmail(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState(
                isLoading = false,
                isSignedIn = false,
                needsOnboarding = false,
                needsEmailVerification = false,
                verificationEmail = "",
                resendSuccessMessage = null,
                error = null,
                emailOrUsername = "",
                password = "",
                username = "user_${UUID.randomUUID().toString().substring(0, 8)}",
                isSignUp = true
            )
            onComplete()
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.signOut()
            resetToSignIn()
            onComplete()
        }
    }

    fun onGoogleIdTokenReceived(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.signInWithGoogle(idToken)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    val uid = authRepository.getCurrentUserId()
                    if (uid != null) {
                        val userResult = userRepository.getUserProfile(uid)
                        val user = userResult.getOrNull()
                        if (user?.onboardingCompleted == false) {
                            _navigationEvents.emit(AuthNavigationEvent.NavigateToOnboarding)
                        } else {
                            _navigationEvents.emit(AuthNavigationEvent.NavigateToDashboard)
                        }
                    } else {
                        _navigationEvents.emit(AuthNavigationEvent.NavigateToDashboard)
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

    fun clearResendMessage() {
        _uiState.value = _uiState.value.copy(resendSuccessMessage = null)
    }
}
