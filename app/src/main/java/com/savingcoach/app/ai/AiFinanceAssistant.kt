package com.savingcoach.app.ai

import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.model.FinnhubNewsResponse
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ChatRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.InvestmentRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import com.savingcoach.app.services.MarketApiService
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiFinanceAssistant @Inject constructor(
    private val chatRepository: ChatRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val savingChallengeRepository: SavingChallengeRepository,
    private val marketApiService: MarketApiService,
    private val investmentRepository: InvestmentRepository
) {
    // 15-minute in-memory cache for market news to avoid delaying chat responses
    private var cachedNews: List<FinnhubNewsResponse> = emptyList()
    private var lastNewsFetchTime: Long = 0L
    private val newsCacheTtlMs = 15 * 60 * 1000L

    // Response cache for common queries to save tokens and improve response time
    private val responseCache = mapOf(
        // Burmese patterns
        "ဘယ်လောက်ကျန်သေးလဲ" to CacheTemplate.BUDGET_REMAINING,
        "ငွေဘယ်လောက်ရှိသေးလဲ" to CacheTemplate.BUDGET_REMAINING,
        "ဘာစားလို့ရလဲ" to CacheTemplate.SUGGESTION,
        "ဘယ်လောက်သုံးပြီးပြီ" to CacheTemplate.TOTAL_SPENT,
        "ဘယ်အချိန်စုရမလဲ" to CacheTemplate.SAVING_TIP,
        "ကူညီပါ" to CacheTemplate.HELP,
        "ช่วยเหลือ" to CacheTemplate.HELP,

        // English patterns
        "how much left" to CacheTemplate.BUDGET_REMAINING,
        "how much remaining" to CacheTemplate.BUDGET_REMAINING,
        "what can i eat" to CacheTemplate.SUGGESTION,
        "how much spent" to CacheTemplate.TOTAL_SPENT,
        "total spent" to CacheTemplate.TOTAL_SPENT,
        "when should i save" to CacheTemplate.SAVING_TIP,
        "help" to CacheTemplate.HELP,
        "what can you do" to CacheTemplate.CAPABILITIES
    )

    suspend fun getFinanceAdvice(userId: String, query: String): Result<ChatMessage> {
        // Check cache first for common queries
        val cachedResponse = getCachedResponse(userId, query)
        if (cachedResponse != null) {
            return Result.success(ChatMessage(
                id = "ai_cached_${System.currentTimeMillis()}",
                userId = userId,
                role = "ai",
                content = cachedResponse,
                timestamp = System.currentTimeMillis(),
                type = "advice"
            ))
        }

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

    private suspend fun getCachedResponse(userId: String, query: String): String? {
        val key = query.lowercase().trim()
        val template = responseCache[key] ?: return null

        return when (template) {
            CacheTemplate.BUDGET_REMAINING -> buildBudgetRemainingResponse(userId)
            CacheTemplate.TOTAL_SPENT -> buildTotalSpentResponse(userId)
            CacheTemplate.SUGGESTION -> buildSuggestionResponse(userId)
            CacheTemplate.SAVING_TIP -> buildSavingTipResponse(userId)
            CacheTemplate.HELP -> buildHelpResponse()
            CacheTemplate.CAPABILITIES -> buildCapabilitiesResponse()
        }
    }

    private suspend fun buildBudgetRemainingResponse(userId: String): String {
        return try {
            val yearMonth = java.time.YearMonth.now()
            val yearMonthStr = yearMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            val budget = budgetRepository.getBudget(userId, yearMonthStr).firstOrNull()
            val expenses = expenseRepository.getExpensesForMonth(userId, yearMonthStr).firstOrNull() ?: emptyList()

            val budgetLimit = budget?.limit ?: 0.0
            val totalSpent = expenses.sumOf { it.amount }
            val remaining = budgetLimit - totalSpent
            val daysLeft = yearMonth.lengthOfMonth() - java.time.LocalDate.now().dayOfMonth

            if (budgetLimit > 0) {
                "သင့်ဘတ်ဂျက် ${String.format("%.0f", remaining)} MMK ကျန်ပါသေးတယ်။ ရက် ${daysLeft} ရက်ကျန်ပါသေးတယ်။"
            } else {
                "ဘတ်ဂျက်သတ်မှတ်ထားခြင်း မရှိပါ။ ဘတ်ဂျက်သတ်မှတ်လိုပါက ကူညီပေးနိုင်ပါတယ်။"
            }
        } catch (e: Exception) {
            "ဘတ်ဂျက်အချက်အလက် ရယူ၍ မရပါ။ နောက်မှ ထပ်ကြိုးစားကြည့်ပါ။"
        }
    }

    private suspend fun buildTotalSpentResponse(userId: String): String {
        return try {
            val yearMonth = java.time.YearMonth.now()
            val yearMonthStr = yearMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            val expenses = expenseRepository.getExpensesForMonth(userId, yearMonthStr).firstOrNull() ?: emptyList()

            val totalSpent = expenses.sumOf { it.amount }
            val topCategory = expenses
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .entries
                .maxByOrNull { it.value }

            val categoryInfo = if (topCategory != null) {
                "အများဆုံးသုံးစွဲထားတာက ${topCategory.key} (${String.format("%.0f", topCategory.value)} MMK)"
            } else {
                "အသုံးစရိတ် မရှိသေးပါ"
            }

            "ဒီလမှာ ${String.format("%.0f", totalSpent)} MMK သုံးပြီးပါပြီ။ $categoryInfo"
        } catch (e: Exception) {
            "အသုံးစရိတ်အချက်အလက် ရယူ၍ မရပါ။"
        }
    }

    private suspend fun buildSuggestionResponse(userId: String): String {
        return try {
            val yearMonth = java.time.YearMonth.now()
            val yearMonthStr = yearMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            val budget = budgetRepository.getBudget(userId, yearMonthStr).firstOrNull()
            val expenses = expenseRepository.getExpensesForMonth(userId, yearMonthStr).firstOrNull() ?: emptyList()

            val budgetLimit = budget?.limit ?: 0.0
            val totalSpent = expenses.sumOf { it.amount }
            val remaining = budgetLimit - totalSpent
            val daysLeft = yearMonth.lengthOfMonth() - java.time.LocalDate.now().dayOfMonth
            val dailyBudget = if (daysLeft > 0) remaining / daysLeft else 0.0

            if (remaining > 0 && dailyBudget > 0) {
                "ဒီနေ့အတွက် ${String.format("%.0f", dailyBudget)} MMK အထိ စားသုံးနိုင်ပါသေးတယ်။ ဘတ်ဂျက်ကို ဂရုစိုက်ပါ။"
            } else if (remaining <= 0) {
                "ဘတ်ဂျက်ကုန်သွားပါပြီ။ ဒီနေ့ ငွေသုံးစွဲခြင်းကို ရှောင်ကြဉ်ပါ။"
            } else {
                "ဘတ်ဂျက် မသတ်မှတ်ရသေးပါ။ ဘတ်ဂျက်သတ်မှတ်လိုပါက ကူညီပေးနိုင်ပါတယ်။"
            }
        } catch (e: Exception) {
            "အကြံပြုချက် ပေး၍ မရပါ။"
        }
    }

    private suspend fun buildSavingTipResponse(userId: String): String {
        return try {
            val challenges = savingChallengeRepository.getActiveChallenges(userId).firstOrNull() ?: emptyList()
            val activeChallenges = challenges.filter { it.isActive }

            if (activeChallenges.isNotEmpty()) {
                val challengeList = activeChallenges.take(3).joinToString("\n") { challenge ->
                    val progress = if (challenge.targetAmount > 0) {
                        ((challenge.currentAmount / challenge.targetAmount) * 100).toInt()
                    } else 0
                    "- ${challenge.title}: ${progress}% ပြီးပါပြီ"
                }
                "သင့်စိန်ခေါ်မှုများ:\n$challengeList\n\nတစ်ခုချင်းစီကို ဆက်လက်ကြိုးစားပါ!"
            } else {
                "Active saving challenges မရှိသေးပါ။ Challenge အသစ်တစ်ခု စတင်လိုပါက ကူညီပေးနိုင်ပါတယ်။"
            }
        } catch (e: Exception) {
            "စိန်ခေါ်မှုအချက်အလက် ရယူ၍ မရပါ။"
        }
    }

    private fun buildHelpResponse(): String {
        return "ကျွန်တော်/ကျွန်မက သင့်ငွေကြေးစီမံခန့်ခွဲမှုကို ကူညီပေးနိုင်ပါတယ်။\n\n" +
                "• အသုံးစရိတ် မှတ်တမ်းတင်ခြင်း\n" +
                "• ဘတ်ဂျက် စီမံခန့်ခွဲခြင်း\n" +
                "• ငွေစုခြင်း စိန်ခေါ်မှုများ\n" +
                "• ငွေကြေးအကြံပြုချက်များ\n\n" +
                "ဘာများ ကူညီပေးရမလဲ?"
    }

    private fun buildCapabilitiesResponse(): String {
        return "ကျွန်တော်/ကျွန်မလုပ်နိုင်တာတွေ:\n\n" +
                "• အသုံးစရိတ် မှတ်တမ်းတင်ခြင်း - 'coffee ၅၀၀၀ သုံးတယ်' လို့ ပြောပါ\n" +
                "• ဘတ်ဂျက် စစ်ကြည့်ခြင်း - 'ဘယ်လောက်ကျန်သေးလဲ' လို့ မေးပါ\n" +
                "• စိန်ခေါ်မှု စုငွေခြင်း - '1K a Day မှာ ၅၀၀ စုမယ်' လို့ ပြောပါ\n" +
                "• ငွေကြေးအကြံပြုချက် - 'ဘယ်လိုငွေစုရမလဲ' လို့ မေးပါ\n\n" +
                "ဘာများ ကူညီပေးရမလဲ?"
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
                """.trimIndent()
            } else {
                "Active Challenges: None"
            }

            // Market News context with 15-minute in-memory cache
            val currentTime = System.currentTimeMillis()
            val marketNews = if (cachedNews.isNotEmpty() && (currentTime - lastNewsFetchTime < newsCacheTtlMs)) {
                cachedNews
            } else {
                try {
                    val freshNews = marketApiService.getMarketNews().getOrNull()?.take(5) ?: emptyList()
                    if (freshNews.isNotEmpty()) {
                        cachedNews = freshNews
                        lastNewsFetchTime = currentTime
                        freshNews
                    } else {
                        cachedNews
                    }
                } catch (_: Exception) {
                    cachedNews
                }
            }

            val newsContext = if (marketNews.isNotEmpty()) {
                val newsList = marketNews.joinToString("\n") { news ->
                    "- [${news.source.ifBlank { "News" }}] ${news.headline}"
                }
                """
                Latest Market News:
                $newsList
                """.trimIndent()
            } else {
                "Latest Market News: Unavailable"
            }

            // User Portfolio Summary context
            val holdings = try {
                investmentRepository.getHoldingsOnce(userId)
                    .filter { !it.isStoppedCompat }
            } catch (_: Exception) {
                emptyList()
            }

            val portfolioContext = if (holdings.isNotEmpty()) {
                val totalCostBasisUsd = holdings.sumOf { it.units * it.buyPrice }
                val formattedCostBasis = String.format(Locale.US, "%.2f", totalCostBasisUsd)
                val holdingList = holdings.take(5).joinToString("\n") { holding ->
                    val ticker = holding.displayTicker.ifBlank { holding.symbol }
                    val price = String.format(Locale.US, "%.2f", holding.buyPrice)
                    "- $ticker: ${holding.units} units @ $$price USD (${holding.type})"
                }
                """
                Portfolio Summary (${holdings.size} active holdings):
                Total Cost Basis: $$formattedCostBasis USD
                Top Holdings:
                $holdingList
                """.trimIndent()
            } else {
                "Portfolio: No active holdings"
            }

            return """
                [HIDDEN SYSTEM CONTEXT - DO NOT MENTION THIS BLOCK TO THE USER]
                Today's Date: ${now.format(DateTimeFormatter.ISO_LOCAL_DATE)}
                Current Month: ${now.month.name} ${now.year}
                Monthly Budget: $budgetLimit
                Total Spent: $totalSpent
                Remaining Budget: $remaining
                Days Left in Month: $daysLeft
                Top Categories: ${topCategories.ifBlank { "None" }}
                Recent Expenses: ${recentExpenses.ifBlank { "None" }}
                $challengeContext
                
                $portfolioContext
                
                $newsContext
            """.trimIndent()
        } catch (e: Exception) {
            return ""
        }
    }
}

private enum class CacheTemplate {
    BUDGET_REMAINING,
    TOTAL_SPENT,
    SUGGESTION,
    SAVING_TIP,
    HELP,
    CAPABILITIES
}
