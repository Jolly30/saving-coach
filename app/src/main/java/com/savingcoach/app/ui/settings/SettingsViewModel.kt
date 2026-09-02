package com.savingcoach.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.UserRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import com.savingcoach.app.data.repository.InvestmentRepository
import com.savingcoach.app.data.repository.ExchangeRateRepository
import com.savingcoach.app.services.MarketApiService
import com.savingcoach.app.data.model.UserHolding
import android.content.Context
import com.savingcoach.app.export.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import com.savingcoach.app.data.repository.ThemePreferences
import com.savingcoach.app.data.repository.AppThemeMode
import com.savingcoach.app.data.repository.LanguagePreferences
import com.savingcoach.app.data.repository.AppLanguage
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val username: String = "Loading...",
    val email: String = "Loading...",
    val age: String = "Not set",
    val gender: String = "Not set",
    val salaryRange: String = "Not set",
    val fieldOfWork: String = "Not set",
    val currencyPreference: String = "MMK",
    val language: AppLanguage = AppLanguage.EN,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isExporting: Boolean = false,
    val exportFile: java.io.File? = null,
    val availableCategories: List<String> = emptyList(),
    val availableChallenges: List<com.savingcoach.app.data.model.SavingChallenge> = emptyList(),
    val availableHoldings: List<UserHolding> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val savingChallengeRepository: SavingChallengeRepository,
    private val investmentRepository: InvestmentRepository,
    private val marketApiService: MarketApiService,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val themePreferences: ThemePreferences,
    private val languagePreferences: LanguagePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        observeThemeMode()
        observeLanguage()
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    private fun observeLanguage() {
        viewModelScope.launch {
            languagePreferences.language.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun setLanguage(appLanguage: AppLanguage, onSuccess: () -> Unit) {
        languagePreferences.setLanguage(appLanguage)
        _uiState.update { it.copy(language = appLanguage) }
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                userRepository.updateLanguagePreference(uid, appLanguage.code)
            }
            onSuccess()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                // Collect user profile flow
                launch {
                    userRepository.getUserProfileFlow(uid).collect { user ->
                        if (user != null) {
                            _uiState.update { it.copy(
                                username = user.username,
                                email = user.email,
                                age = user.age?.toString() ?: "Not set",
                                gender = user.gender ?: "Not set",
                                fieldOfWork = user.fieldOfWork ?: "Not set",
                                salaryRange = user.salaryRange ?: "Not set",
                                currencyPreference = user.currencyPreference,
                                isLoading = false
                            ) }
                        } else {
                            _uiState.update { it.copy(
                                username = "Unknown",
                                email = "Unknown",
                                isLoading = false
                            ) }
                        }
                    }
                }
                
                // Load available categories and challenges for export
                launch {
                    val expenses = expenseRepository.getAllExpenses(uid).first()
                    val uniqueCategories = expenses.map { it.category }.distinct().sorted()
                    _uiState.update { it.copy(availableCategories = uniqueCategories) }
                }
                
                launch {
                    val challenges = savingChallengeRepository.getAllChallenges(uid).first()
                    _uiState.update { it.copy(availableChallenges = challenges) }
                }
                
                launch {
                    investmentRepository.getHoldings(uid).collect { holdings ->
                        _uiState.update { it.copy(availableHoldings = holdings) }
                    }
                }
            } else {
                _uiState.update { it.copy(
                    username = "Unknown",
                    email = "Unknown",
                    isLoading = false
                ) }
            }
        }
    }

    fun updateUsername(newUsername: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (newUsername.isBlank() || newUsername == _uiState.value.username) {
                onSuccess()
                return@launch
            }
            
            val isTakenResult = userRepository.isUsernameTaken(newUsername)
            if (isTakenResult.isSuccess && isTakenResult.getOrDefault(false)) {
                _uiState.value = _uiState.value.copy(error = "Username is already taken")
                return@launch
            }
            
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val result = userRepository.updateUsername(uid, newUsername)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    username = newUsername,
                    error = null
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message ?: "Failed to update username"
                )
            }
        }
    }

    fun updateAge(newAge: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val result = userRepository.updateAge(uid, newAge)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(age = newAge.toString(), error = null)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message ?: "Failed to update age")
            }
        }
    }

    fun updateGender(newGender: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val result = userRepository.updateGender(uid, newGender)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(gender = newGender, error = null)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message ?: "Failed to update gender")
            }
        }
    }

    fun updateSalaryRange(newRange: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val result = userRepository.updateSalaryRange(uid, newRange)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(salaryRange = newRange, error = null)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message ?: "Failed to update salary range")
            }
        }
    }

    fun updateFieldOfWork(newField: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val result = userRepository.updateFieldOfWork(uid, newField)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(fieldOfWork = newField, error = null)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message ?: "Failed to update field of work")
            }
        }
    }

    fun updateCurrencyPreference(newCurrency: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            val result = userRepository.updateCurrencyPreference(uid, newCurrency)
            if (result.isSuccess) {
                _uiState.update { it.copy(currencyPreference = newCurrency, error = null) }
                onSuccess()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to update currency preference") }
            }
        }
    }

    fun updateEmail(newEmail: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            
            // First update auth
            val authResult = authRepository.updateEmail(newEmail)
            if (authResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update email. You might need to sign out and sign back in to change your email."
                )
                return@launch
            }
            
            // Then update profile
            val result = userRepository.updateEmail(uid, newEmail)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(email = newEmail, error = null)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message ?: "Failed to update email in profile")
            }
        }
    }

    fun sendPasswordResetEmail(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val email = _uiState.value.email
            if (email == "Loading..." || email == "Unknown" || email.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "No email address found.")
                return@launch
            }
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(error = "Password reset email sent!")
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.changePassword(oldPassword, newPassword)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to change password. Make sure your old password is correct."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }

    fun exportData(
        context: Context,
        type: String,
        startDateStr: String? = null,
        endDateStr: String? = null,
        selectedCategories: List<String> = emptyList(),
        selectedChallengeIds: List<String> = emptyList(),
        selectedHoldingIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportFile = null, error = null) }
            try {
                val uid = authRepository.getCurrentUserId() ?: throw Exception("User not signed in")
                val file = if (type == "Expenses") {
                    var expenses = expenseRepository.getAllExpenses(uid).first()
                    
                    if (!startDateStr.isNullOrBlank()) {
                        expenses = expenses.filter { it.date >= startDateStr }
                    }
                    if (!endDateStr.isNullOrBlank()) {
                        expenses = expenses.filter { it.date <= endDateStr }
                    }
                    if (selectedCategories.isNotEmpty()) {
                        expenses = expenses.filter { it.category in selectedCategories }
                    }
                    
                    val sortedExpenses = expenses.sortedWith(
                        compareBy<com.savingcoach.app.data.model.Expense> { it.date }.thenBy { it.createdAt }
                    )
                    CsvExporter.exportExpensesToCsv(context, sortedExpenses)
                } else if (type == "Savings") {
                    val challengesList = savingChallengeRepository.getAllChallenges(uid).first()
                    val challengesMap = challengesList.associateBy { it.id }
                    
                    val allDeposits = mutableListOf<com.savingcoach.app.data.model.SavingsDeposit>()
                    for (challenge in challengesList) {
                        if (selectedChallengeIds.isEmpty() || challenge.id in selectedChallengeIds) {
                            val deposits = savingChallengeRepository.getDeposits(uid, challenge.id).first()
                            allDeposits.addAll(deposits)
                        }
                    }
                    
                    var filteredDeposits = allDeposits.toList()
                    if (!startDateStr.isNullOrBlank()) {
                        filteredDeposits = filteredDeposits.filter { it.date >= startDateStr }
                    }
                    if (!endDateStr.isNullOrBlank()) {
                        filteredDeposits = filteredDeposits.filter { it.date <= endDateStr }
                    }
                    
                    val sortedDeposits = filteredDeposits.sortedWith(
                        compareBy<com.savingcoach.app.data.model.SavingsDeposit> { it.date }.thenBy { it.createdAt }
                    )
                    CsvExporter.exportSavingsToCsv(context, sortedDeposits, challengesMap)
                } else {
                    val allHoldings = investmentRepository.getHoldings(uid).first()
                    var filteredHoldings = allHoldings
                    if (selectedHoldingIds.isNotEmpty()) {
                        filteredHoldings = filteredHoldings.filter { it.id in selectedHoldingIds }
                    }
                    
                    val usdRate = exchangeRateRepository.usdToMmkRate.value
                    val computedHoldings = mutableListOf<com.savingcoach.app.data.model.ComputedHolding>()
                    
                    val activeHoldings = filteredHoldings.filter { !it.isStoppedCompat }
                    val stoppedHoldings = filteredHoldings.filter { it.isStoppedCompat }
                    
                    // Fetch active crypto prices in batch
                    val activeCrypto = activeHoldings.filter { it.type == "crypto" }
                    val cryptoPrices = if (activeCrypto.isNotEmpty()) {
                        marketApiService.getCryptoPrices(activeCrypto.map { it.symbol }).getOrDefault(emptyMap())
                    } else {
                        emptyMap()
                    }
                    
                    // Fetch active stock prices
                    val activeStocks = activeHoldings.filter { it.type == "stock" }
                    val stockPrices = mutableMapOf<String, com.savingcoach.app.data.model.CachedPrice>()
                    activeStocks.forEach { stock ->
                        marketApiService.getStockQuote(stock.symbol).onSuccess {
                            stockPrices[stock.symbol] = it
                        }
                    }
                    
                    // Compute active crypto
                    activeCrypto.forEach { holding ->
                        val priceData = cryptoPrices[holding.symbol]
                        val livePrice = if (priceData != null) priceData.livePrice * usdRate else holding.buyPrice
                        val change24h = priceData?.change24h ?: 0.0
                        computedHoldings.add(
                            com.savingcoach.app.utils.InvestmentCalculations.computeHolding(holding, livePrice, change24h)
                        )
                    }
                    
                    // Compute active stocks
                    activeStocks.forEach { holding ->
                        val priceData = stockPrices[holding.symbol]
                        val livePrice = if (priceData != null) priceData.livePrice * usdRate else holding.buyPrice
                        val change24h = priceData?.change24h ?: 0.0
                        computedHoldings.add(
                            com.savingcoach.app.utils.InvestmentCalculations.computeHolding(holding, livePrice, change24h)
                        )
                    }
                    
                    // Compute stopped holdings
                    stoppedHoldings.forEach { holding ->
                        computedHoldings.add(
                            com.savingcoach.app.utils.InvestmentCalculations.computeHolding(holding, holding.exitPrice, 0.0)
                        )
                    }
                    
                    CsvExporter.exportInvestmentsToCsv(context, computedHoldings)
                }
                _uiState.update { it.copy(isExporting = false, exportFile = file) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isExporting = false, error = "Failed to export data: ${e.message}") 
                }
            }
        }
    }

    fun clearExportFile() {
        _uiState.value = _uiState.value.copy(exportFile = null)
    }
}
