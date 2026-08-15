package com.savingcoach.app.data.mock

import com.savingcoach.app.data.model.Budget
import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.model.Expense
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.SavingsDeposit
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.model.ExpenseCategoryEntity
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ChatRepository
import com.savingcoach.app.data.repository.ExpenseCategoryRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockExpenseRepository @Inject constructor() : ExpenseRepository {
    private val expenses = MutableStateFlow<List<Expense>>(emptyList())

    override fun getExpensesForMonth(userId: String, yearMonth: String): Flow<List<Expense>> =
        expenses.map { list -> list.filter { it.date.startsWith(yearMonth) } }

    override fun getExpensesForDate(userId: String, date: String): Flow<List<Expense>> =
        expenses.map { list -> list.filter { it.date == date } }

    override fun getAllExpenses(userId: String): Flow<List<Expense>> = expenses

    override suspend fun addExpense(expense: Expense): String {
        val id = "exp_${System.currentTimeMillis()}"
        expenses.value = expenses.value + expense.copy(id = id)
        return id
    }

    override suspend fun updateExpense(expense: Expense) {
        expenses.value = expenses.value.map { if (it.id == expense.id) expense else it }
    }

    override suspend fun deleteExpense(expenseId: String) {
        expenses.value = expenses.value.filter { it.id != expenseId }
    }
}

@Singleton
class MockBudgetRepository @Inject constructor() : BudgetRepository {

    private val budgets = MutableStateFlow<Map<String, Budget>>(emptyMap())

    override fun getBudget(userId: String, yearMonth: String): Flow<Budget?> {
        return budgets.map { it[yearMonth] }
    }

    override suspend fun setBudget(userId: String, budget: Budget) {
        budgets.value = budgets.value + (budget.month to budget)
    }

    override suspend fun updateLimit(userId: String, yearMonth: String, newLimit: Double) {
        val current = budgets.value[yearMonth]
        if (current != null) {
            budgets.value = budgets.value + (yearMonth to current.copy(limit = newLimit))
        }
    }

}


@Singleton
class MockChatRepository @Inject constructor() : ChatRepository {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun getChatHistory(userId: String): Flow<List<ChatMessage>> = messages

    override suspend fun saveMessage(userId: String, message: ChatMessage) {
        messages.value = messages.value + message
    }

    override suspend fun updateMessage(userId: String, message: ChatMessage) {
        messages.value = messages.value.map { if (it.id == message.id) message else it }
    }

    override suspend fun sendToAi(
        userId: String,
        userMessage: String,
        systemPrompt: String?
    ): Result<ChatMessage> {
        // Mock response for testing
        val aiMessage = ChatMessage(
            id = "ai_${System.currentTimeMillis()}",
            userId = userId,
            role = "ai",
            content = "This is a mock response. In production, this would call the AI proxy.",
            timestamp = System.currentTimeMillis(),
            type = "advice"
        )
        return Result.success(aiMessage)
    }
}

@Singleton
class MockSavingChallengeRepository @Inject constructor() : SavingChallengeRepository {
    private val challenges = MutableStateFlow<List<SavingChallenge>>(emptyList())
    private val deposits = MutableStateFlow<Map<String, List<SavingsDeposit>>>(emptyMap())

    override fun getActiveChallenges(userId: String): Flow<List<SavingChallenge>> =
        challenges.map { list -> list.filter { it.isActive && !it.isCompleted } }

    override fun getAllChallenges(userId: String): Flow<List<SavingChallenge>> = challenges

    override fun getDeposits(userId: String, challengeId: String): Flow<List<SavingsDeposit>> =
        deposits.map { it[challengeId] ?: emptyList() }

    override suspend fun createChallenge(challenge: SavingChallenge): String {
        val id = "ch_${System.currentTimeMillis()}"
        challenges.value = challenges.value + challenge.copy(id = id)
        return id
    }

    override suspend fun addDeposit(userId: String, challengeId: String, deposit: SavingsDeposit) {
        val current = deposits.value.toMutableMap()
        val list = (current[challengeId] ?: emptyList()) + deposit
        current[challengeId] = list
        deposits.value = current
    }

    override suspend fun completeChallenge(userId: String, challengeId: String) {
        challenges.value = challenges.value.map {
            if (it.id == challengeId) it.copy(isCompleted = true, isActive = false) else it
        }
    }

    override suspend fun deleteChallenge(userId: String, challengeId: String) {
        challenges.value = challenges.value.filter { it.id != challengeId }
    }
}

@Singleton
class MockAuthRepository @Inject constructor() : AuthRepository {
    private var signedIn = false

    override fun isUserSignedIn(): Boolean = signedIn

    override fun getCurrentUserId(): String? = if (signedIn) "mock_user_1" else null

    override suspend fun signInWithGoogle(idToken: String): Result<AuthResult> {
        signedIn = true
        return Result.success(null as AuthResult)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthResult> {
        signedIn = true
        return Result.success(null as AuthResult)
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthResult> {
        signedIn = true
        return Result.success(null as AuthResult)
    }

    override suspend fun signOut() {
        signedIn = false
    }
}

@Singleton
class MockExpenseCategoryRepository @Inject constructor() : ExpenseCategoryRepository {
    private val categories = MutableStateFlow<Map<String, List<ExpenseCategoryEntity>>>(emptyMap())
    private val deletedNames = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    override fun getCategories(userId: String, yearMonth: String): Flow<List<ExpenseCategoryEntity>> =
        categories.map { map -> map[yearMonth] ?: emptyList() }

    override fun getDeletedCategoryNames(userId: String, yearMonth: String): Flow<Set<String>> =
        deletedNames.map { map -> map[yearMonth] ?: emptySet() }

    override suspend fun saveCategories(
        userId: String,
        yearMonth: String,
        categories: List<ExpenseCategoryEntity>,
        deletedNames: Set<String>
    ) {
        this.categories.value = this.categories.value + (yearMonth to categories)
        this.deletedNames.value = this.deletedNames.value + (yearMonth to deletedNames)
    }
}
