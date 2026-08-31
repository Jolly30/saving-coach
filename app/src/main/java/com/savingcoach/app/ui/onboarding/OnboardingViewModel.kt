package com.savingcoach.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun saveAge(age: Int?, onComplete: () -> Unit) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userRepository.updateAge(uid, age)
            _uiState.value = _uiState.value.copy(isLoading = false)
            onComplete()
        }
    }

    fun saveGender(gender: String?, onComplete: () -> Unit) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userRepository.updateGender(uid, gender)
            _uiState.value = _uiState.value.copy(isLoading = false)
            onComplete()
        }
    }

    fun saveFieldOfWork(field: String?, onComplete: () -> Unit) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userRepository.updateFieldOfWork(uid, field)
            _uiState.value = _uiState.value.copy(isLoading = false)
            onComplete()
        }
    }

    fun saveSalaryRange(range: String?, onComplete: () -> Unit) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userRepository.updateSalaryRange(uid, range)
            userRepository.updateOnboardingCompleted(uid)
            _uiState.value = _uiState.value.copy(isLoading = false)
            onComplete()
        }
    }
}
