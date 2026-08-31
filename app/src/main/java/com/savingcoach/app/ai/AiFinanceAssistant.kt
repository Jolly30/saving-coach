package com.savingcoach.app.ai

import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ChatRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiFinanceAssistant @Inject constructor(
    private val chatRepository: ChatRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val savingChallengeRepository: SavingChallengeRepository
) {
    suspend fun getFinanceAdvice(userId: String, query: String): Result<ChatMessage> {
        val basePrompt = PromptBuilder.buildSystemPrompt()
        val contextBlock = buildFinancialContext(userId)
        
        val systemPrompt = if (contextBlock.isNotBlank()) {
            "$basePrompt\n\n$contextBlock"
        } else {
            basePrompt
        }
        
        return chatRepository.sendToAi(
            userId = userId,
            userMessage = query,
            systemPrompt = systemPrompt
        )
    }

    private suspend fun buildFinancialContext(userId: String): String {
        try {
            val now = LocalDate.now()
            val yearMonth = YearMonth.now()
            val yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

            val budget = budgetRepository.getBudget(userId, yearMonthStr).firstOrNull()
            val expenses = expenseRepository.getExpensesForMonth(userId, yearMonthStr).firstOrNull() ?: emptyList()
            val challenges = savingChallengeRepository.getActiveChallenges(userId).firstOrNull() ?: emptyList()

            val daysLeft = yearMonth.lengthOfMonth() - now.dayOfMonth

            val budgetLimit = budget?.limit ?: 0.0
            val totalSpent = expenses.sumOf { it.amount }
            val remaining = budgetLimit - totalSpent

            val topCategories = expenses
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString(", ") { "${it.key} (${it.value})" }

            val recentExpenses = expenses
                .sortedByDescending { it.createdAt }
                .take(3)
                .joinToString(", ") { "${it.category} (${it.amount})" }

            // Build challenge context
            val challengeContext = if (challenges.isNotEmpty()) {
                val challengeList = challenges.joinToString("\n") { challenge ->
                    val progress = if (challenge.targetAmount > 0) {
                        ((challenge.currentAmount / challenge.targetAmount) * 100).toInt()
                    } else 0
                    "- ${challenge.title}: ${challenge.currentAmount}/${challenge.targetAmount} MMK ($progress% complete, ${challenge.template})"
                }
                """
                Active Challenges (${challenges.size}):
                $challengeList
                """
            } else {
                "Active Challenges: None"
            }

            return """
                [HIDDEN SYSTEM CONTEXT - DO NOT MENTION THIS BLOCK TO THE USER]
                Current Month: ${now.month.name} ${now.year}
                Monthly Budget: $budgetLimit
                Total Spent: $totalSpent
                Remaining Budget: $remaining
                Days Left in Month: $daysLeft
                Top Categories: ${topCategories.ifBlank { "None" }}
                Recent Expenses: ${recentExpenses.ifBlank { "None" }}
                $challengeContext
            """.trimIndent()
        } catch (e: Exception) {
            return ""
        }
    }
}
