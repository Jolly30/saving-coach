package com.savingcoach.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.ai.AiFinanceAssistant
import com.savingcoach.app.ai.SpeechRecognizerManager
import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.model.Expense
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.ChatRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import com.savingcoach.app.data.model.SavingsDeposit
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ExpenseCategoryEntity
import com.savingcoach.app.data.repository.ExpenseCategoryRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val aiFinanceAssistant: AiFinanceAssistant,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val expenseRepository: ExpenseRepository,
    private val challengeRepository: SavingChallengeRepository,
    private val categoryRepository: ExpenseCategoryRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _savingExpenseMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val savingExpenseMessageIds: StateFlow<Set<String>> = _savingExpenseMessageIds.asStateFlow()

    private val _activeChallenges = MutableStateFlow<List<SavingChallenge>>(emptyList())
    val activeChallenges: StateFlow<List<SavingChallenge>> = _activeChallenges.asStateFlow()

    private val _categories = MutableStateFlow<List<ExpenseCategoryEntity>>(emptyList())
    val categories: StateFlow<List<ExpenseCategoryEntity>> = _categories.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private fun cleanTitleForComparison(title: String): String {
        return title.filter { it.isLetterOrDigit() || it.isWhitespace() }.lowercase().trim()
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val userId: String
        get() = authRepository.getCurrentUserId() ?: "anonymous"

    val isListening: StateFlow<Boolean> = speechRecognizerManager.isListening
    val partialVoiceText: StateFlow<String> = speechRecognizerManager.recognizedText
    val rmsDb: StateFlow<Float> = speechRecognizerManager.rmsDb

    init {
        // Handle background job re-joining if a request is already running
        val isJobActive = activeJobs[userId]?.isActive == true
        if (isJobActive) {
            _isTyping.value = true
            viewModelScope.launch {
                activeJobs[userId]?.join()
                _isTyping.value = false
                val errorMsg = activeErrors.remove(userId)
                if (errorMsg != null) {
                    _error.value = errorMsg
                }
            }
        } else {
            val errorMsg = activeErrors.remove(userId)
            if (errorMsg != null) {
                _error.value = errorMsg
            }
        }

        viewModelScope.launch {
            chatRepository.getChatHistory(userId).collect { history ->
                _messages.value = history
            }
        }
        viewModelScope.launch {
            challengeRepository.getActiveChallenges(userId)
                .catch { emit(emptyList()) }
                .collect { challenges ->
                    _activeChallenges.value = challenges
                }
        }
        viewModelScope.launch {
            val yearMonthStr = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            categoryRepository.getCategories(userId, yearMonthStr)
                .catch { emit(emptyList()) }
                .collect { cats ->
                    _categories.value = cats
                }
        }
        viewModelScope.launch {
            speechRecognizerManager.error.collect { err ->
                if (err != null) {
                    _error.value = err
                }
            }
        }
        viewModelScope.launch {
            var wasListening = false
            speechRecognizerManager.isListening.collect { listening ->
                if (wasListening && !listening) {
                    appendVoiceText(speechRecognizerManager.recognizedText.value)
                }
                wasListening = listening
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isTyping.value) return

        val userMessage = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            userId = userId,
            role = "user",
            content = content.trim(),
            timestamp = System.currentTimeMillis(),
            type = "query"
        )

        _isTyping.value = true
        _error.value = null

        viewModelScope.launch {
            chatRepository.saveMessage(userId, userMessage)
        }

        val activeChalls = _activeChallenges.value

        val job = applicationScope.launch {
            try {
                aiFinanceAssistant.getFinanceAdvice(userId, content)
                    .onSuccess { aiMessage ->
                        var finalMessage = aiMessage
                        val parsed = aiMessage.parsedExpense
                        if (parsed != null) {
                            android.util.Log.d("ChatViewModel", "Active challenges count: ${activeChalls.size}")
                            activeChalls.forEach { 
                                android.util.Log.d("ChatViewModel", "Challenge: ${it.title}, ID: ${it.id}, lastDepositDate: ${it.lastDepositDate}, completedSteps: ${it.completedDaysCount}, isActive: ${it.isActive}, isCompleted: ${it.isCompleted}")
                            }
                            val isChallenge = parsed.isChallenge || parsed.action == "prompt_challenge_confirmation" || parsed.action == "mark_challenge_saving"
                            if (isChallenge) {
                                var challengeTitle = parsed.challengeTitle.ifBlank { parsed.merchant }
                                var cleanQuery = cleanTitleForComparison(challengeTitle)
                                var targetChallenge = if (cleanQuery.isNotBlank()) {
                                    activeChalls.firstOrNull { cleanTitleForComparison(it.title) == cleanQuery }
                                } else null

                                // Fallback: If not matched by title, match against active challenges mentioned in user content or AI content
                                if (targetChallenge == null && activeChalls.isNotEmpty()) {
                                    val cleanUserContent = cleanTitleForComparison(content)
                                    val cleanAiContent = cleanTitleForComparison(aiMessage.content)
                                    targetChallenge = activeChalls.firstOrNull { chall ->
                                        val cleanDb = cleanTitleForComparison(chall.title)
                                        cleanDb.isNotBlank() && (cleanUserContent.contains(cleanDb) || cleanAiContent.contains(cleanDb))
                                    }
                                    if (targetChallenge != null) {
                                        challengeTitle = targetChallenge.title
                                        cleanQuery = cleanTitleForComparison(challengeTitle)
                                        val updatedParsed = parsed.copy(
                                            challengeTitle = targetChallenge.title,
                                            merchant = targetChallenge.title
                                        )
                                        finalMessage = finalMessage.copy(parsedExpense = updatedParsed)
                                    }
                                }

                                if (targetChallenge != null) {
                                    if (hasDepositedToday(targetChallenge.id)) {
                                        val warningText = if (parsed.language == "my") {
                                            "ယနေ့အတွက် '${targetChallenge.title}' တွင် စုဆောင်းမှု ပြုလုပ်ပြီးပါပြီ။ မနက်ဖြန်မှ ထပ်မံစုဆောင်းပါ။"
                                        } else {
                                            "You have already logged a contribution to '${targetChallenge.title}' today! Please try to save tomorrow."
                                        }
                                        val warningMessage = aiMessage.copy(
                                            content = warningText,
                                            parsedExpense = null,
                                            parsedExpenses = null
                                        )
                                        chatRepository.saveMessage(userId, warningMessage)
                                        return@onSuccess
                                    }

                                    // Handle FLEXI template - requires user to input amount
                                    if (targetChallenge.template == com.savingcoach.app.data.model.ChallengeTemplate.FLEXI) {
                                        if (parsed.amount == 0.0) {
                                            // Ask user for amount and do not show the card yet
                                            val askAmountText = if (parsed.language == "my") {
                                                "ဘယ်လောက် စုမလဲ? ငွေပမာဏ ထည့်ပေးပါ။"
                                            } else {
                                                "How much would you like to save? Please enter the amount."
                                            }
                                            val askAmountMessage = aiMessage.copy(
                                                content = askAmountText,
                                                parsedExpense = null,
                                                parsedExpenses = null
                                            )
                                            chatRepository.saveMessage(userId, askAmountMessage)
                                            return@onSuccess
                                        }
                                        // If amount is provided, proceed with saving (will be handled in confirmChallengeSaving)
                                    }

                                    // For CONSTANT, NO_SPEND, ENVELOPE templates - ignore user's amount and calculate automatically
                                    if (targetChallenge.template == com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE) {
                                        // Always calculate surprise amount for ENVELOPE (ignore user's amount)
                                        val surpriseAmount = calculateEnvelopeSurpriseAmount(targetChallenge)

                                        val updatedParsed = parsed.copy(amount = surpriseAmount)
                                        val cleanContent = if (parsed.language == "my") {
                                            "ကျေးဇူးပြု၍ '${targetChallenge.title}' တွင် စုဆောင်းရန် အတည်ပြုပေးပါ။"
                                        } else {
                                            "Please confirm the deposit to your '${targetChallenge.title}'."
                                        }
                                        finalMessage = aiMessage.copy(
                                            content = cleanContent,
                                            parsedExpense = updatedParsed,
                                            parsedExpenses = aiMessage.parsedExpenses?.map {
                                                if (it.challengeTitle.isNotBlank() && cleanTitleForComparison(it.challengeTitle) == cleanQuery) {
                                                    it.copy(amount = surpriseAmount)
                                                } else it
                                            }
                                        )
                                    } else if (targetChallenge.template == com.savingcoach.app.data.model.ChallengeTemplate.CONSTANT ||
                                        targetChallenge.template == com.savingcoach.app.data.model.ChallengeTemplate.NO_SPEND) {
                                        // Always calculate constant amount (ignore user's amount)
                                        val constantAmount = calculateConstantAmount(targetChallenge)

                                        val updatedParsed = parsed.copy(amount = constantAmount)
                                        val cleanContent = if (parsed.language == "my") {
                                            "ကျေးဇူးပြု၍ '${targetChallenge.title}' တွင် စုဆောင်းရန် အတည်ပြုပေးပါ။"
                                        } else {
                                            "Please confirm the deposit to your '${targetChallenge.title}'."
                                        }
                                        finalMessage = aiMessage.copy(
                                            content = cleanContent,
                                            parsedExpense = updatedParsed,
                                            parsedExpenses = aiMessage.parsedExpenses?.map {
                                                if (it.challengeTitle.isNotBlank() && cleanTitleForComparison(it.challengeTitle) == cleanQuery) {
                                                    it.copy(amount = constantAmount)
                                                } else it
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        chatRepository.saveMessage(userId, finalMessage)
                    }
                    .onFailure { e ->
                        activeErrors[userId] = e.message ?: "Failed to get AI response"
                        _error.value = e.message ?: "Failed to get AI response"
                    }
            } catch (e: Exception) {
                activeErrors[userId] = e.message ?: "Failed to get AI response"
                _error.value = e.message ?: "Failed to get AI response"
            } finally {
                _isTyping.value = false
                activeJobs.remove(userId)
            }
        }

        activeJobs[userId] = job
    }

    private suspend fun ensureCategoryExists(categoryName: String) {
        if (categoryName.isBlank()) return
        val currentCategories = _categories.value
        val cleanQuery = cleanTitleForComparison(categoryName)
        val exists = currentCategories.any { cleanTitleForComparison(it.name) == cleanQuery }
        if (!exists) {
            throw Exception("Category '$categoryName' does not exist. Please switch to an existing category.")
        }
    }

    private suspend fun getExistingChallenge(challengeTitle: String): SavingChallenge {
        val titleClean = challengeTitle.trim()
        val cleanQuery = cleanTitleForComparison(titleClean)
        val existing = _activeChallenges.value.firstOrNull {
            cleanTitleForComparison(it.title) == cleanQuery
        }
        if (existing != null) return existing

        throw Exception("Challenge '$titleClean' does not exist. Please switch to an existing challenge.")
    }

    fun clearError() {
        _error.value = null
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun startVoiceInput() {
        speechRecognizerManager.startListening()
    }

    fun stopVoiceInput() {
        speechRecognizerManager.stopListening()
    }

    fun cancelVoiceInput() {
        speechRecognizerManager.cancelListening()
    }

    private fun appendVoiceText(transcription: String) {
        if (transcription.isNotBlank()) {
            val current = _inputText.value
            _inputText.value = if (current.isBlank()) transcription else "$current $transcription"
        }
    }

    fun saveParsedExpense(message: ChatMessage) {
        saveParsedExpenseAtIndex(message, 0)
    }

    fun saveParsedExpenseAtIndex(message: ChatMessage, index: Int) {
        val expenses = message.parsedExpenses ?: listOfNotNull(message.parsedExpense)
        if (index < 0 || index >= expenses.size) return
        val parsed = expenses[index]
        when (parsed.action) {
            "prompt_user_category_choice" -> {
                saveExpenseWithCategoryAtIndex(message, index, parsed.category)
            }
            "prompt_challenge_confirmation", "mark_challenge_saving" -> {
                confirmChallengeSaving(message)
            }
            else -> {
                val currencyCode = parsed.currency.ifBlank { "MMK" }
                val expenseCategory = parsed.category.ifBlank { "Other" }
                val expenseMerchant = parsed.item.ifBlank { parsed.merchant }
                if (parsed.isChallenge) {
                    confirmChallengeSaving(message)
                } else {
                    if (message.savedExpenseIndices.contains(index) || (index == 0 && message.expenseSaved)) return
                    val savingKey = "${message.id}_$index"
                    if (_savingExpenseMessageIds.value.contains(savingKey)) return
                    _savingExpenseMessageIds.value = _savingExpenseMessageIds.value + savingKey
                    viewModelScope.launch {
                        try {
                            ensureCategoryExists(expenseCategory)
                            val expense = Expense(
                                amount = parsed.amount,
                                category = expenseCategory,
                                merchant = expenseMerchant,
                                description = "Added via AI Chat",
                                date = parsed.date.ifBlank { java.time.LocalDate.now().toString() },
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                                source = "chat",
                                currency = currencyCode,
                                userId = userId
                            )
                            expenseRepository.addExpense(expense)
                            
                            val updatedIndices = message.savedExpenseIndices + index
                            val allSaved = updatedIndices.size >= (message.parsedExpenses?.size ?: 1)
                            val updatedMessage = message.copy(
                                savedExpenseIndices = updatedIndices,
                                expenseSaved = allSaved
                            )
                            chatRepository.updateMessage(userId, updatedMessage)
                        } catch (e: Exception) {
                            _error.value = "Failed to save: ${e.message}"
                        } finally {
                            _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - savingKey
                        }
                    }
                }
            }
        }
    }

    fun updateExpenseCategory(message: ChatMessage, category: String) {
        updateExpenseCategoryAtIndex(message, 0, category)
    }

    fun updateExpenseCategoryAtIndex(message: ChatMessage, index: Int, category: String) {
        val expenses = message.parsedExpenses ?: listOfNotNull(message.parsedExpense)
        if (index < 0 || index >= expenses.size) return
        val parsed = expenses[index]
        
        val updatedMessage = if (message.parsedExpenses != null) {
            val list = expenses.toMutableList()
            list[index] = parsed.copy(category = category)
            message.copy(parsedExpenses = list)
        } else {
            message.copy(parsedExpense = parsed.copy(category = category))
        }

        viewModelScope.launch {
            try {
                chatRepository.updateMessage(userId, updatedMessage)
            } catch (e: Exception) {
                _error.value = "Failed to update category: ${e.message}"
            }
        }
    }

    fun saveExpenseWithCategory(message: ChatMessage, chosenCategory: String) {
        saveExpenseWithCategoryAtIndex(message, 0, chosenCategory)
    }

    fun saveExpenseWithCategoryAtIndex(message: ChatMessage, index: Int, chosenCategory: String) {
        val expenses = message.parsedExpenses ?: listOfNotNull(message.parsedExpense)
        if (index < 0 || index >= expenses.size) return
        val parsed = expenses[index]
        if (message.savedExpenseIndices.contains(index) || (index == 0 && message.expenseSaved)) return

        val savingKey = "${message.id}_$index"
        if (_savingExpenseMessageIds.value.contains(savingKey)) return
        _savingExpenseMessageIds.value = _savingExpenseMessageIds.value + savingKey

        viewModelScope.launch {
            try {
                ensureCategoryExists(chosenCategory)
                val expense = Expense(
                    amount = parsed.amount,
                    category = chosenCategory.ifBlank { "Other" },
                    merchant = parsed.item.ifBlank { parsed.merchant },
                    description = "Added via AI Chat (Category: $chosenCategory)",
                    date = parsed.date.ifBlank { java.time.LocalDate.now().toString() },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    source = "chat",
                    currency = parsed.currency.ifBlank { "MMK" },
                    userId = userId
                )
                expenseRepository.addExpense(expense)
                
                val updatedMessage = if (message.parsedExpenses != null) {
                    val list = expenses.toMutableList()
                    list[index] = parsed.copy(category = chosenCategory)
                    val updatedIndices = message.savedExpenseIndices + index
                    message.copy(
                        parsedExpenses = list,
                        savedExpenseIndices = updatedIndices,
                        expenseSaved = updatedIndices.size >= list.size
                    )
                } else {
                    message.copy(
                        expenseSaved = true,
                        parsedExpense = parsed.copy(category = chosenCategory)
                    )
                }
                chatRepository.updateMessage(userId, updatedMessage)
            } catch (e: Exception) {
                _error.value = "Failed to save: ${e.message}"
            } finally {
                _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - savingKey
            }
        }
    }

    private fun hasDepositedToday(challengeId: String): Boolean {
        val challenge = _activeChallenges.value.find { it.id == challengeId } ?: return false
        val parts = challenge.lastDepositDate.split("|")
        val completedSteps = if (parts.size > 1) (parts[1].toIntOrNull() ?: 0) else 0
        if (completedSteps == 0) return false
        val lastDate = if (parts.isNotEmpty()) parts[0] else ""
        return lastDate == java.time.LocalDate.now().toString()
    }

    /**
     * Calculate the surprise amount for envelope challenges.
     * This is extracted to avoid code duplication between confirmChallengeSaving and switchChallengeSaving.
     */
    private fun calculateEnvelopeSurpriseAmount(targetChallenge: com.savingcoach.app.data.model.SavingChallenge): Double {
        val parts = targetChallenge.lastDepositDate.split("|")
        val completedSteps = if (parts.size > 1) (parts[1].toIntOrNull() ?: 0) else 0
        val duration = if (parts.size > 2) parts[2] else "30"
        val totalSteps = duration.toIntOrNull() ?: 30
        val remainingEnvelopes = (totalSteps - completedSteps).coerceAtLeast(1)
        val remainingAmount = (targetChallenge.targetAmount - targetChallenge.currentAmount).coerceAtLeast(0.0)

        val surprise = if (remainingEnvelopes == 1) {
            remainingAmount
        } else {
            val average = remainingAmount / remainingEnvelopes
            val randomFactor = 0.7 + (Math.random() * 0.6)
            val rawSurprise = average * randomFactor
            if (rawSurprise >= 1000.0) {
                ((rawSurprise / 1000.0).toInt() * 1000.0).coerceAtLeast(1000.0)
            } else if (rawSurprise >= 100.0) {
                ((rawSurprise / 100.0).toInt() * 100.0).coerceAtLeast(100.0)
            } else {
                rawSurprise.coerceAtLeast(1.0)
            }
        }

        return if (remainingEnvelopes == 1) {
            remainingAmount.coerceAtLeast(0.0)
        } else {
            val maxAllowed = (remainingAmount - ((remainingEnvelopes - 1) * 1.0)).coerceAtLeast(0.0)
            surprise.coerceAtLeast(1.0).coerceAtMost(maxAllowed)
        }
    }

    /**
     * Calculate the constant amount for constant challenges.
     * This is extracted to avoid code duplication between confirmChallengeSaving and switchChallengeSaving.
     */
    private fun calculateConstantAmount(targetChallenge: com.savingcoach.app.data.model.SavingChallenge): Double {
        val parts = targetChallenge.lastDepositDate.split("|")
        val duration = if (parts.size > 2) parts[2] else "30"
        val totalSteps = duration.toIntOrNull() ?: 30
        return if (totalSteps > 0) targetChallenge.targetAmount / totalSteps else 0.0
    }

    /**
     * Calculate deposit amount based on challenge template.
     * - FLEXI: Use user's amount (must be > 0, otherwise should have been caught earlier)
     * - CONSTANT, NO_SPEND: Always calculate automatically (ignore user's amount)
     * - ENVELOPE: Always calculate surprise amount (ignore user's amount)
     */
    private fun calculateDepositAmount(
        targetChallenge: com.savingcoach.app.data.model.SavingChallenge,
        parsedAmount: Double
    ): Double {
        return when (targetChallenge.template) {
            com.savingcoach.app.data.model.ChallengeTemplate.FLEXI -> {
                // FLEXI requires user to provide amount
                parsedAmount
            }
            com.savingcoach.app.data.model.ChallengeTemplate.CONSTANT,
            com.savingcoach.app.data.model.ChallengeTemplate.NO_SPEND -> {
                // CONSTANT and NO_SPEND: calculate automatically, ignore user's amount
                calculateConstantAmount(targetChallenge)
            }
            com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE -> {
                // ENVELOPE: calculate surprise amount, ignore user's amount
                calculateEnvelopeSurpriseAmount(targetChallenge)
            }
        }
    }

    fun confirmChallengeSaving(message: ChatMessage, overrideAmount: Double? = null) {
        val parsed = message.parsedExpense ?: return
        if (message.expenseSaved || _savingExpenseMessageIds.value.contains(message.id)) return

        _savingExpenseMessageIds.value = _savingExpenseMessageIds.value + message.id

        viewModelScope.launch {
            try {
                val challengeTitle = parsed.challengeTitle.ifBlank { parsed.merchant }
                val targetChallenge = getExistingChallenge(challengeTitle)

                if (hasDepositedToday(targetChallenge.id)) {
                    val errMsg = if (parsed.language == "my") {
                        "ယနေ့အတွက် '${targetChallenge.title}' တွင် စုဆောင်းမှု ပြုလုပ်ပြီးပါပြီ။ မနက်ဖြန်မှ ထပ်မံစုဆောင်းပါ။"
                    } else {
                        "You have already logged a contribution to '${targetChallenge.title}' today! Please try to save tomorrow."
                    }
                    _error.value = errMsg
                    return@launch
                }
                val parts = targetChallenge.lastDepositDate.split("|")
                val completedSteps = if (parts.size > 1) (parts[1].toIntOrNull() ?: 0) else 0
                val duration = if (parts.size > 2) parts[2] else "30"
                val totalSteps = duration.toIntOrNull() ?: 30

                val depositAmount = overrideAmount ?: calculateDepositAmount(targetChallenge, parsed.amount)

                // Safety check: FLEXI template requires amount > 0
                if (targetChallenge.template == com.savingcoach.app.data.model.ChallengeTemplate.FLEXI && depositAmount <= 0.0) {
                    val errMsg = if (parsed.language == "my") {
                        "ငွေပမာဏ ထည့်ပေးပါ။"
                    } else {
                        "Please enter an amount to save."
                    }
                    _error.value = errMsg
                    return@launch
                }

                val deposit = SavingsDeposit(
                    id = "dep_${System.currentTimeMillis()}",
                    challengeId = targetChallenge.id,
                    amount = depositAmount,
                    date = parsed.date.ifBlank { java.time.LocalDate.now().toString() },
                    note = "Added via AI Chat (Confirmed)",
                    createdAt = System.currentTimeMillis()
                )
                val nextSteps = completedSteps + 1
                val newCurrentAmount = targetChallenge.currentAmount + depositAmount
                val isNowCompleted = when (targetChallenge.template) {
                    com.savingcoach.app.data.model.ChallengeTemplate.FLEXI -> newCurrentAmount >= targetChallenge.targetAmount
                    com.savingcoach.app.data.model.ChallengeTemplate.CONSTANT,
                    com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE,
                    com.savingcoach.app.data.model.ChallengeTemplate.NO_SPEND -> nextSteps >= totalSteps
                }
                val updatedChallenge = targetChallenge.copy(
                    currentAmount = newCurrentAmount,
                    lastDepositDate = java.time.LocalDate.now().toString() + "|" + nextSteps + "|" + duration,
                    isCompleted = targetChallenge.isCompleted || isNowCompleted,
                    isActive = targetChallenge.isActive && !isNowCompleted
                )
                challengeRepository.createChallenge(updatedChallenge)

                challengeRepository.addDeposit(userId, targetChallenge.id, deposit)

                val updatedMessage = message.copy(
                    expenseSaved = true,
                    parsedExpense = parsed.copy(challengeTitle = targetChallenge.title, amount = depositAmount)
                )
                chatRepository.updateMessage(userId, updatedMessage)
            } catch (e: Exception) {
                _error.value = "Failed to confirm challenge saving: ${e.message}"
            } finally {
                _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - message.id
            }
        }
    }

    fun switchChallengeSaving(message: ChatMessage, newChallengeTitle: String, overrideAmount: Double? = null) {
        val parsed = message.parsedExpense ?: return
        if (message.expenseSaved || _savingExpenseMessageIds.value.contains(message.id)) return

        _savingExpenseMessageIds.value = _savingExpenseMessageIds.value + message.id

        viewModelScope.launch {
            try {
                val cleanQuery = cleanTitleForComparison(newChallengeTitle)
                val targetChallenge = _activeChallenges.value.firstOrNull {
                    cleanTitleForComparison(it.title) == cleanQuery
                }

                if (targetChallenge != null) {
                    if (hasDepositedToday(targetChallenge.id)) {
                        val errMsg = if (parsed.language == "my") {
                            "ယနေ့အတွက် '${targetChallenge.title}' တွင် စုဆောင်းမှု ပြုလုပ်ပြီးပါပြီ။ မနက်ဖြန်မှ ထပ်မံစုဆောင်းပါ။"
                        } else {
                            "You have already logged a contribution to '${targetChallenge.title}' today! Please try to save tomorrow."
                        }
                        _error.value = errMsg
                        return@launch
                    }
                    val parts = targetChallenge.lastDepositDate.split("|")
                    val completedSteps = if (parts.size > 1) (parts[1].toIntOrNull() ?: 0) else 0
                    val duration = if (parts.size > 2) parts[2] else "30"
                    val totalSteps = duration.toIntOrNull() ?: 30

                    val depositAmount = overrideAmount ?: calculateDepositAmount(targetChallenge, parsed.amount)

                    // Safety check: FLEXI template requires amount > 0
                    if (targetChallenge.template == com.savingcoach.app.data.model.ChallengeTemplate.FLEXI && depositAmount <= 0.0) {
                        val errMsg = if (parsed.language == "my") {
                            "ငွေပမာဏ ထည့်ပေးပါ။"
                        } else {
                            "Please enter an amount to save."
                        }
                        _error.value = errMsg
                        return@launch
                    }

                    val deposit = SavingsDeposit(
                        id = "dep_${System.currentTimeMillis()}",
                        challengeId = targetChallenge.id,
                        amount = depositAmount,
                        date = parsed.date.ifBlank { java.time.LocalDate.now().toString() },
                        note = "Added via AI Chat (Switched to ${targetChallenge.title})",
                        createdAt = System.currentTimeMillis()
                    )
                    val nextSteps = completedSteps + 1
                    val newCurrentAmount = targetChallenge.currentAmount + depositAmount
                    val isNowCompleted = when (targetChallenge.template) {
                        com.savingcoach.app.data.model.ChallengeTemplate.FLEXI -> newCurrentAmount >= targetChallenge.targetAmount
                        com.savingcoach.app.data.model.ChallengeTemplate.CONSTANT,
                        com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE,
                        com.savingcoach.app.data.model.ChallengeTemplate.NO_SPEND -> nextSteps >= totalSteps
                    }
                    val updatedChallenge = targetChallenge.copy(
                        currentAmount = newCurrentAmount,
                        lastDepositDate = java.time.LocalDate.now().toString() + "|" + nextSteps + "|" + duration,
                        isCompleted = targetChallenge.isCompleted || isNowCompleted,
                        isActive = targetChallenge.isActive && !isNowCompleted
                    )
                    challengeRepository.createChallenge(updatedChallenge)

                    challengeRepository.addDeposit(userId, targetChallenge.id, deposit)

                    val updatedMessage = message.copy(
                        expenseSaved = true,
                        parsedExpense = parsed.copy(challengeTitle = targetChallenge.title, amount = depositAmount)
                    )
                    chatRepository.updateMessage(userId, updatedMessage)
                } else {
                    _error.value = "Challenge '$newChallengeTitle' not found."
                }
            } catch (e: Exception) {
                _error.value = "Failed to switch challenge: ${e.message}"
            } finally {
                _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - message.id
            }
        }
    }

    fun cancelAction(message: ChatMessage, index: Int = 0) {
        viewModelScope.launch {
            try {
                _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - "${message.id}_$index"
                _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - message.id
                val newCancelledList = if (message.cancelledExpenseIndices.contains(index)) {
                    message.cancelledExpenseIndices
                } else {
                    message.cancelledExpenseIndices + index
                }
                val totalItemsCount = message.parsedExpenses?.size ?: 1
                val allCompleted = (newCancelledList.size + message.savedExpenseIndices.size) >= totalItemsCount
                
                val updatedMessage = message.copy(
                    cancelledExpenseIndices = newCancelledList,
                    expenseCancelled = allCompleted && newCancelledList.isNotEmpty()
                )
                chatRepository.updateMessage(userId, updatedMessage)
            } catch (e: Exception) {
                _error.value = "Failed to cancel action: ${e.message}"
            }
        }
    }



    companion object {
        private val parentJob = SupervisorJob()
        private val applicationScope = CoroutineScope(parentJob + Dispatchers.IO)
        private val activeJobs = ConcurrentHashMap<String, Job>()
        private val activeErrors = ConcurrentHashMap<String, String>()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.destroy()
    }
}
