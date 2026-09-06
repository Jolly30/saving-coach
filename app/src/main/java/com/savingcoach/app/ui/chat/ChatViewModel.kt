package com.savingcoach.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.ai.AiFinanceAssistant
import com.savingcoach.app.ai.SpeechRecognizerManager
import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.model.ParsedExpense
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

    private val _categories = MutableStateFlow<List<ExpenseCategoryEntity>>(CategoryResolver.DEFAULT_ENTITIES)
    val categories: StateFlow<List<ExpenseCategoryEntity>> = _categories.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private fun cleanTitleForComparison(title: String): String {
        return title.filter {
            it.isLetterOrDigit() ||
            it.isWhitespace() ||
            Character.getType(it) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(it) == Character.COMBINING_SPACING_MARK.toInt()
        }.lowercase().trim()
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
            kotlinx.coroutines.flow.combine(
                categoryRepository.getCategories(userId, yearMonthStr).catch { emit(emptyList()) },
                categoryRepository.getDeletedCategoryNames(userId, yearMonthStr).catch { emit(emptySet()) }
            ) { savedCats, deletedNames ->
                mergeCategories(savedCats, deletedNames)
            }.collect { merged ->
                _categories.value = merged
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
                        val rawExpensesList: List<ParsedExpense> = aiMessage.parsedExpenses
                            ?.takeIf { it.isNotEmpty() }
                            ?: listOfNotNull(aiMessage.parsedExpense)

                        if (rawExpensesList.isNotEmpty()) {
                            android.util.Log.d("ChatViewModel", "Active challenges count: ${activeChalls.size}")
                            activeChalls.forEach { 
                                android.util.Log.d("ChatViewModel", "Challenge: ${it.title}, ID: ${it.id}, lastDepositDate: ${it.lastDepositDate}, completedSteps: ${it.completedDaysCount}, isActive: ${it.isActive}, isCompleted: ${it.isCompleted}")
                            }

                            val activeCats = _categories.value
                            val cleanUserContent = cleanTitleForComparison(content)
                            val cleanAiContent = cleanTitleForComparison(aiMessage.content)

                            val processedExpenses = mutableListOf<ParsedExpense>()
                            val alreadySavedChallenges = mutableListOf<SavingChallenge>()
                            val detectedLang = rawExpensesList.firstOrNull()?.language ?: if (content.contains(Regex("[\\u1000-\\u109F]"))) "my" else "en"

                            for (exp in rawExpensesList) {
                                val isChallenge = exp.isChallenge || exp.action == "prompt_challenge_confirmation" || exp.action == "mark_challenge_saving"
                                if (isChallenge) {
                                    val challengeTitle = exp.challengeTitle.ifBlank { exp.merchant }
                                    val cleanQuery = cleanTitleForComparison(challengeTitle)
                                    var targetChallenge = if (cleanQuery.isNotBlank()) {
                                        activeChalls.firstOrNull { cleanTitleForComparison(it.title) == cleanQuery }
                                    } else null

                                    // Fallback match against active challenges mentioned in user content, AI content, or challengeTitle
                                    if (targetChallenge == null && activeChalls.isNotEmpty()) {
                                        targetChallenge = activeChalls.firstOrNull { chall ->
                                            val cleanDb = cleanTitleForComparison(chall.title)
                                            cleanDb.isNotBlank() && (
                                                cleanQuery.contains(cleanDb) ||
                                                cleanDb.contains(cleanQuery) ||
                                                cleanUserContent.contains(cleanDb) ||
                                                cleanAiContent.contains(cleanDb)
                                            )
                                        }
                                    }

                                    if (targetChallenge != null) {
                                        if (hasDepositedToday(targetChallenge.id)) {
                                            alreadySavedChallenges.add(targetChallenge)
                                        } else {
                                            val updatedAmount = when (targetChallenge.template) {
                                                com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE -> {
                                                    calculateEnvelopeSurpriseAmount(targetChallenge)
                                                }
                                                com.savingcoach.app.data.model.ChallengeTemplate.CONSTANT,
                                                com.savingcoach.app.data.model.ChallengeTemplate.NO_SPEND -> {
                                                    calculateConstantAmount(targetChallenge)
                                                }
                                                com.savingcoach.app.data.model.ChallengeTemplate.FLEXI -> {
                                                    exp.amount
                                                }
                                            }
                                            val updatedExp = exp.copy(
                                                challengeTitle = targetChallenge.title,
                                                merchant = targetChallenge.title,
                                                amount = updatedAmount
                                            )
                                            processedExpenses.add(updatedExp)
                                        }
                                    } else {
                                        processedExpenses.add(exp)
                                    }
                                } else {
                                    // Expense
                                    val resolvedCat = CategoryResolver.resolve(exp.category, activeCats)
                                    val finalCat = resolvedCat?.name ?: exp.category
                                    val fallbackItem = if (exp.item.isBlank()) {
                                        if (exp.merchant.isNotBlank()) exp.merchant else extractItemFromContent(content)
                                    } else exp.item
                                    processedExpenses.add(exp.copy(category = finalCat, item = fallbackItem))
                                }
                            }

                            if (alreadySavedChallenges.isNotEmpty() && processedExpenses.isEmpty()) {
                                // ALL requested challenges were already deposited today!
                                val warningText = if (detectedLang == "my") {
                                    if (alreadySavedChallenges.size == 1) {
                                        "ယနေ့အတွက် '${alreadySavedChallenges.first().title}' တွင် စုဆောင်းမှု ပြုလုပ်ပြီးပါပြီ။ မနက်ဖြန်မှ ထပ်မံစုဆောင်းပါ။"
                                    } else {
                                        val names = alreadySavedChallenges.joinToString(" နှင့် ") { chall -> "'${chall.title}'" }
                                        "ယနေ့အတွက် $names တွင် စုဆောင်းမှု ပြုလုပ်ပြီးပါပြီ။ မနက်ဖြန်မှ ထပ်မံစုဆောင်းပါ။"
                                    }
                                } else {
                                    if (alreadySavedChallenges.size == 1) {
                                        "You have already logged a contribution to '${alreadySavedChallenges.first().title}' today! Please try to save tomorrow."
                                    } else {
                                        val names = alreadySavedChallenges.joinToString(" and ") { chall -> "'${chall.title}'" }
                                        "You have already logged contributions to $names today! Please try to save tomorrow."
                                    }
                                }
                                val warningMessage = aiMessage.copy(
                                    content = warningText,
                                    parsedExpense = null,
                                    parsedExpenses = null
                                )
                                chatRepository.saveMessage(userId, warningMessage)
                                return@onSuccess
                            }

                            if (alreadySavedChallenges.isNotEmpty() && processedExpenses.isNotEmpty()) {
                                // SOME challenges were already deposited, but others are eligible or expenses exist!
                                val alreadySavedPart = if (detectedLang == "my") {
                                    val names = alreadySavedChallenges.joinToString(" နှင့် ") { chall -> "'${chall.title}'" }
                                    "ယနေ့အတွက် $names တွင် စုဆောင်းမှု ပြုလုပ်ပြီးပါပြီ။"
                                } else {
                                    val names = alreadySavedChallenges.joinToString(" and ") { chall -> "'${chall.title}'" }
                                    "You have already logged a contribution to $names today."
                                }

                                val readyPart = if (detectedLang == "my") {
                                    val allChallenges = processedExpenses.all { it.isChallenge }
                                    val allExpenses = processedExpenses.all { !it.isChallenge }
                                    if (allChallenges) {
                                        val names = processedExpenses.joinToString(" နှင့် ") { exp: ParsedExpense -> "'${exp.challengeTitle.ifBlank { exp.item }}'" }
                                        "$names အတွက် ငွေစုရန် ပြင်ဆင်ထားပါတယ်။ အောက်ပါ Card ${if (processedExpenses.size > 1) "များ" else ""}တွင် အတည်ပြုပေးပါ။"
                                    } else if (allExpenses) {
                                        val names = processedExpenses.joinToString(", ") { exp: ParsedExpense ->
                                            val n = exp.item.ifBlank { CategoryResolver.toBurmeseName(exp.category) }
                                            val a = if (exp.amount > 0) " (${exp.amount.toLong()} ကျပ်)" else ""
                                            "$n$a"
                                        }
                                        "$names အတွက် မှတ်သားထားပါတယ်။ အောက်ပါ Card ${if (processedExpenses.size > 1) "များ" else ""}တွင် အတည်ပြုပေးပါ။"
                                    } else {
                                        val items = processedExpenses.joinToString(", ") { exp: ParsedExpense ->
                                            if (exp.isChallenge) {
                                                exp.challengeTitle.ifBlank { exp.item }
                                            } else {
                                                val n = exp.item.ifBlank { CategoryResolver.toBurmeseName(exp.category) }
                                                val a = if (exp.amount > 0) " (${exp.amount.toLong()} ကျပ်)" else ""
                                                "$n$a"
                                            }
                                        }
                                        "$items အတွက် ပြင်ဆင်ထားပါတယ်။ အောက်ပါ Card များတွင် အတည်ပြုပေးပါ။"
                                    }
                                } else {
                                    val allChallenges = processedExpenses.all { it.isChallenge }
                                    val allExpenses = processedExpenses.all { !it.isChallenge }
                                    if (allChallenges) {
                                        val names = processedExpenses.joinToString(" and ") { exp: ParsedExpense -> "'${exp.challengeTitle.ifBlank { exp.item }}'" }
                                        "I've prepared your deposit for $names. Please confirm below."
                                    } else if (allExpenses) {
                                        val names = processedExpenses.joinToString(", ") { exp: ParsedExpense ->
                                            val n = exp.item.ifBlank { exp.category }
                                            val a = if (exp.amount > 0) " (${exp.amount.toLong()} ${exp.currency})" else ""
                                            "$n$a"
                                        }
                                        "I've noted your expense for $names. Please confirm below."
                                    } else {
                                        "Please confirm the deposit and expense below."
                                    }
                                }

                                finalMessage = aiMessage.copy(
                                    content = "$alreadySavedPart $readyPart",
                                    parsedExpense = processedExpenses.firstOrNull(),
                                    parsedExpenses = if (processedExpenses.size > 1) processedExpenses else (if (aiMessage.parsedExpenses != null) processedExpenses else null)
                                )
                            } else {
                                // No challenges were already deposited today.
                                val firstExp = processedExpenses.firstOrNull()
                                if (processedExpenses.size == 1 && firstExp != null && firstExp.isChallenge) {
                                    val targetChallenge = activeChalls.firstOrNull { cleanTitleForComparison(it.title) == cleanTitleForComparison(firstExp.challengeTitle) }
                                    if (targetChallenge?.template == com.savingcoach.app.data.model.ChallengeTemplate.FLEXI && firstExp.amount == 0.0) {
                                        val askAmountText = if (detectedLang == "my") {
                                            "ကျေးဇူးပြု၍ '${targetChallenge.title}' တွင် စုဆောင်းရန် ငွေပမာဏ ထည့်သွင်းပြီး အတည်ပြုပေးပါ။"
                                        } else {
                                            "Please enter the amount and confirm the deposit to your '${targetChallenge.title}'."
                                        }
                                        finalMessage = finalMessage.copy(content = askAmountText)
                                    } else if (finalMessage.content.isBlank()) {
                                        val cleanContent = if (detectedLang == "my") {
                                            "ကျေးဇူးပြု၍ '${firstExp.challengeTitle}' တွင် စုဆောင်းရန် အတည်ပြုပေးပါ။"
                                        } else {
                                            "Please confirm the deposit to your '${firstExp.challengeTitle}'."
                                        }
                                        finalMessage = finalMessage.copy(content = cleanContent)
                                    }
                                }
                                finalMessage = finalMessage.copy(
                                    parsedExpense = processedExpenses.firstOrNull(),
                                    parsedExpenses = if (processedExpenses.size > 1) processedExpenses else (if (aiMessage.parsedExpenses != null) processedExpenses else null)
                                )
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

    private fun mergeCategories(
        saved: List<ExpenseCategoryEntity>,
        deletedNames: Set<String>
    ): List<ExpenseCategoryEntity> {
        val defaults = CategoryResolver.DEFAULT_ENTITIES
        if (saved.isEmpty()) {
            return defaults.filterNot { deletedNames.contains(it.name.lowercase()) }
        }

        val savedMap = saved.associateBy { it.name.lowercase() }
        val result = mutableListOf<ExpenseCategoryEntity>()

        for (default in defaults) {
            if (deletedNames.contains(default.name.lowercase())) continue
            val savedEntity = savedMap[default.name.lowercase()]
            if (savedEntity != null) {
                result.add(default.copy(target = savedEntity.target, emoji = savedEntity.emoji))
            } else {
                result.add(default)
            }
        }

        for (savedEntity in saved) {
            if (defaults.none { it.name.equals(savedEntity.name, ignoreCase = true) }) {
                result.add(savedEntity)
            }
        }

        return if (result.isNotEmpty()) result else defaults
    }

    private fun extractItemFromContent(content: String): String {
        val text = content.trim()
        val myMatch = Regex("(.+?)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks|mmk|ဖိုး|ကုန်|ကုန်တယ်|ကျ|ကျတယ်|ပေးရတယ်|ရှင်း|ရှင်းတယ်)?$").find(text)
        if (myMatch != null) {
            val item = myMatch.groupValues[1].trim()
            if (item.isNotBlank() && !item.contains("စု")) return item
        }
        val enMatch = Regex("(?i)(?:log|paid|spent|bought)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?\\s*(?:for|on|at)\\s*(.+)").find(text)
        if (enMatch != null) {
            val item = enMatch.groupValues[2].trim()
            if (item.isNotBlank()) return item
        }
        return ""
    }

    private suspend fun ensureCategoryExists(categoryName: String) {
        if (categoryName.isBlank()) return
        val currentCategories = _categories.value
        val resolved = CategoryResolver.resolve(categoryName, currentCategories)
        if (resolved == null) {
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
                confirmChallengeSaving(message, index)
            }
            else -> {
                val currencyCode = parsed.currency.ifBlank { "MMK" }
                val resolvedCat = CategoryResolver.resolve(parsed.category, _categories.value)
                val expenseCategory = resolvedCat?.name ?: parsed.category.ifBlank { "Other" }
                val expenseMerchant = parsed.item.ifBlank { parsed.merchant }
                if (parsed.isChallenge) {
                    confirmChallengeSaving(message, index)
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
                val resolvedCat = CategoryResolver.resolve(chosenCategory, _categories.value)
                val finalCategory = resolvedCat?.name ?: chosenCategory.ifBlank { "Other" }
                val expense = Expense(
                    amount = parsed.amount,
                    category = finalCategory,
                    merchant = parsed.item.ifBlank { parsed.merchant },
                    description = "Added via AI Chat (Category: $finalCategory)",
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

    fun confirmChallengeSaving(message: ChatMessage, index: Int = 0, overrideAmount: Double? = null) {
        val expenses = message.parsedExpenses ?: listOfNotNull(message.parsedExpense)
        if (index < 0 || index >= expenses.size) return
        val parsed = expenses[index]
        if (message.savedExpenseIndices.contains(index) || (index == 0 && message.expenseSaved)) return

        val savingKey = "${message.id}_$index"
        if (_savingExpenseMessageIds.value.contains(savingKey)) return
        _savingExpenseMessageIds.value = _savingExpenseMessageIds.value + savingKey

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

                val updatedIndices = message.savedExpenseIndices + index
                val totalCount = message.parsedExpenses?.size ?: 1
                val allSaved = updatedIndices.size >= totalCount
                val updatedParsedExpenses = message.parsedExpenses?.mapIndexed { idx, p ->
                    if (idx == index) p.copy(challengeTitle = targetChallenge.title, amount = depositAmount) else p
                }
                val updatedMessage = message.copy(
                    savedExpenseIndices = updatedIndices,
                    expenseSaved = allSaved,
                    parsedExpense = if (index == 0) parsed.copy(challengeTitle = targetChallenge.title, amount = depositAmount) else message.parsedExpense,
                    parsedExpenses = updatedParsedExpenses
                )
                chatRepository.updateMessage(userId, updatedMessage)
            } catch (e: Exception) {
                _error.value = "Failed to confirm challenge saving: ${e.message}"
            } finally {
                _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - savingKey
            }
        }
    }

    fun switchChallengeSaving(message: ChatMessage, newChallengeTitle: String, overrideAmount: Double? = null, index: Int = 0) {
        val expenses = message.parsedExpenses ?: listOfNotNull(message.parsedExpense)
        if (index < 0 || index >= expenses.size) return
        val parsed = expenses[index]
        if (message.savedExpenseIndices.contains(index) || (index == 0 && message.expenseSaved)) return

        val savingKey = "${message.id}_$index"
        if (_savingExpenseMessageIds.value.contains(savingKey)) return
        _savingExpenseMessageIds.value = _savingExpenseMessageIds.value + savingKey

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

                    val updatedIndices = message.savedExpenseIndices + index
                    val totalCount = message.parsedExpenses?.size ?: 1
                    val allSaved = updatedIndices.size >= totalCount
                    val updatedParsedExpenses = message.parsedExpenses?.mapIndexed { idx, p ->
                        if (idx == index) p.copy(challengeTitle = targetChallenge.title, amount = depositAmount) else p
                    }
                    val updatedMessage = message.copy(
                        savedExpenseIndices = updatedIndices,
                        expenseSaved = allSaved,
                        parsedExpense = if (index == 0) parsed.copy(challengeTitle = targetChallenge.title, amount = depositAmount) else message.parsedExpense,
                        parsedExpenses = updatedParsedExpenses
                    )
                    chatRepository.updateMessage(userId, updatedMessage)
                } else {
                    _error.value = "Challenge '$newChallengeTitle' not found."
                }
            } catch (e: Exception) {
                _error.value = "Failed to switch challenge: ${e.message}"
            } finally {
                _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - savingKey
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
