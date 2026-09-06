package com.savingcoach.app.ai

import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.model.FinnhubNewsResponse
import com.savingcoach.app.data.model.UserHolding
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

        val aiResult = chatRepository.sendToAi(
            userId = userId,
            userMessage = query,
            systemPrompt = systemPrompt
        )

        // Resilient Fallback for Financial Status/Report queries:
        // If the AI failed, returned empty, or returned the generic canned greeting loop
        // ("I'm here to help with your finances. What would you like to know?" or Burmese equivalent),
        // synthesize a rich financial coaching report from local data instead of failing or echoing a greeting.
        val hasMyanmarText = query.contains(Regex("[\\u1000-\\u109F]"))
        val detectedLang = if (hasMyanmarText) "my" else "en"

        if (isExpenseReportQuery(query)) {
            val isSuccess = aiResult.isSuccess
            val content = aiResult.getOrNull()?.content?.trim() ?: ""
            val isGenericFallback = content.isBlank() ||
                    content.contains("I'm here to help with your finances", ignoreCase = true) ||
                    content.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ", ignoreCase = true)
            val incomplete = isTruncatedOrIncomplete(content)
            val isOffTopic = content.contains("Daily Safe-to-Spend", ignoreCase = true) ||
                    content.contains("Financial Overview", ignoreCase = true) ||
                    content.contains("Life Interpretation", ignoreCase = true)

            if (!isSuccess || isGenericFallback || incomplete || isOffTopic) {
                val synthesizedExpenseReport = buildExpenseReportResponse(userId, detectedLang)
                return Result.success(
                    ChatMessage(
                        id = "ai_expense_report_${System.currentTimeMillis()}",
                        userId = userId,
                        role = "ai",
                        content = synthesizedExpenseReport,
                        timestamp = System.currentTimeMillis(),
                        type = "advice"
                    )
                )
            }
        } else if (isSavingsReportQuery(query)) {
            val isSuccess = aiResult.isSuccess
            val content = aiResult.getOrNull()?.content?.trim() ?: ""
            val isGenericFallback = content.isBlank() ||
                    content.contains("I'm here to help with your finances", ignoreCase = true) ||
                    content.contains("I'm here to help", ignoreCase = true) ||
                    content.contains("What would you like to know", ignoreCase = true) ||
                    content.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ", ignoreCase = true) ||
                    content.contains("ဘာများ ကူညီပေးရမလဲ", ignoreCase = true)
            val incomplete = isTruncatedOrIncomplete(content)
            val isOffTopic = content.contains("Daily Safe-to-Spend", ignoreCase = true) ||
                    content.contains("Expense Report", ignoreCase = true)

            if (!isSuccess || isGenericFallback || incomplete || isOffTopic) {
                val synthesizedReport = buildSavingsReportResponse(userId, detectedLang)
                return Result.success(
                    ChatMessage(
                        id = "ai_savings_report_${System.currentTimeMillis()}",
                        userId = userId,
                        role = "ai",
                        content = synthesizedReport,
                        timestamp = System.currentTimeMillis(),
                        type = "advice"
                    )
                )
            }
        } else if (isSavingAdviceQuery(query)) {
            val isSuccess = aiResult.isSuccess
            val content = aiResult.getOrNull()?.content?.trim() ?: ""
            val isGenericFallback = content.isBlank() ||
                    content.contains("I'm here to help with your finances", ignoreCase = true) ||
                    content.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ", ignoreCase = true)
            val incomplete = isSavingAdviceIncomplete(content)
            val isCorrupted = if (detectedLang == "my") AiChatRepository.isCorruptedBurmeseResponse(content, query) else false

            if (!isSuccess || isGenericFallback || incomplete || isCorrupted) {
                val synthesizedAdvice = buildSavingAdviceResponse(userId, query, detectedLang)
                return Result.success(
                    ChatMessage(
                        id = "ai_saving_advice_${System.currentTimeMillis()}",
                        userId = userId,
                        role = "ai",
                        content = synthesizedAdvice,
                        timestamp = System.currentTimeMillis(),
                        type = "advice"
                    )
                )
            }
        } else if (isMarketPriceQuery(query)) {
            val isSuccess = aiResult.isSuccess
            val content = aiResult.getOrNull()?.content?.trim() ?: ""
            val isGenericFallback = content.isBlank() ||
                    content.contains("I'm here to help with your finances", ignoreCase = true) ||
                    content.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ", ignoreCase = true)
            val incomplete = isMarketPriceResponseIncomplete(content)
            val isCorrupted = if (detectedLang == "my") AiChatRepository.isCorruptedBurmeseResponse(content, query) else false

            if (!isSuccess || isGenericFallback || incomplete || isCorrupted) {
                val synthesizedPrice = buildMarketPriceResponse(userId, query, detectedLang)
                return Result.success(
                    ChatMessage(
                        id = "ai_market_price_${System.currentTimeMillis()}",
                        userId = userId,
                        role = "ai",
                        content = synthesizedPrice,
                        timestamp = System.currentTimeMillis(),
                        type = "advice"
                    )
                )
            }
        } else if (isNewsQuery(query)) {
            val isSuccess = aiResult.isSuccess
            val content = aiResult.getOrNull()?.content?.trim() ?: ""
            val isGenericFallback = content.isBlank() ||
                    content.contains("I'm here to help with your finances", ignoreCase = true) ||
                    content.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ", ignoreCase = true)
            val incomplete = isNewsResponseIncomplete(content)
            val isCorrupted = if (detectedLang == "my") AiChatRepository.isCorruptedBurmeseResponse(content, query) else false

            if (!isSuccess || isGenericFallback || incomplete || isCorrupted) {
                val synthesizedRecap = buildNewsRecapResponse(userId, query, detectedLang)
                return Result.success(
                    ChatMessage(
                        id = "ai_news_recap_${System.currentTimeMillis()}",
                        userId = userId,
                        role = "ai",
                        content = synthesizedRecap,
                        timestamp = System.currentTimeMillis(),
                        type = "advice"
                    )
                )
            }
        } else if (isFinancialReportQuery(query)) {
            val isSuccess = aiResult.isSuccess
            val content = aiResult.getOrNull()?.content?.trim() ?: ""
            val isGenericFallback = content.isBlank() ||
                    content.contains("I'm here to help with your finances", ignoreCase = true) ||
                    content.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ", ignoreCase = true)

            // Detect truncated, incomplete, or thinking-polluted responses (e.g. cut off mid-number like "Monthly Budget: 100,")
            val incomplete = isTruncatedOrIncomplete(content)

            if (!isSuccess || isGenericFallback || incomplete) {
                val synthesizedReport = buildFinancialReportResponse(userId, detectedLang, query)
                return Result.success(
                    ChatMessage(
                        id = "ai_report_${System.currentTimeMillis()}",
                        userId = userId,
                        role = "ai",
                        content = synthesizedReport,
                        timestamp = System.currentTimeMillis(),
                        type = "advice"
                    )
                )
            }
        } else if (isInvestmentAdviceQuery(query)) {
            val isSuccess = aiResult.isSuccess
            val content = aiResult.getOrNull()?.content?.trim() ?: ""
            val isGenericFallback = content.isBlank() ||
                    content.contains("I'm here to help with your finances", ignoreCase = true) ||
                    content.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ", ignoreCase = true)
            val incomplete = isInvestmentAdviceIncomplete(content)
            val isCorrupted = if (detectedLang == "my") AiChatRepository.isCorruptedBurmeseResponse(content, query) else false

            if (!isSuccess || isGenericFallback || incomplete || isCorrupted) {
                val synthesizedAdvice = buildInvestmentAdviceResponse(userId, query, detectedLang)
                return Result.success(
                    ChatMessage(
                        id = "ai_investment_advice_${System.currentTimeMillis()}",
                        userId = userId,
                        role = "ai",
                        content = synthesizedAdvice,
                        timestamp = System.currentTimeMillis(),
                        type = "advice"
                    )
                )
            }
        }

        return aiResult
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

    companion object {
        fun isExpenseReportQuery(query: String): Boolean {
            val q = query.lowercase(Locale.US).trim()
            // Exclude advice queries that mention expenses/spending as context
            val adviceTerms = listOf(
                "should i buy", "should we buy", "should i invest", "good to buy",
                "good time to buy", "where to invest", "how to invest", "buy now",
                "ဝယ်သင့်", "ဝယ်သင့်မသင့်", "ဝယ်သင့် မဝယ်သင့်", "မဝယ်သင့်", "ဝယ်ရမလား",
                "ဝယ်ရင်", "ရင်းနှီးမြှုပ်နှံ", "ရင်းနှီးမြုပ်နှံ", "ဘယ်လိုစု", "ငွေစုနည်း"
            )
            if (adviceTerms.any { q.contains(it) }) return false

            val patterns = listOf(
                "report expense", "report expenses", "expense report", "expenses report",
                "spending report", "expense summary", "expenses summary", "my expenses",
                "expense breakdown", "spending breakdown", "breakdown of expenses",
                "what did i spend", "what did i spend on", "list my expenses",
                "အသုံးစရိတ် အစီရင်ခံစာ", "အသုံးစရိတ် စာရင်း", "အသုံးစရိတ် အကျဉ်းချုပ်",
                "သုံးထားတာတွေ ပြပါ", "သုံးထားတာတွေ", "အသုံးစရိတ်များ"
            )
            return patterns.any { q.contains(it) }
        }

        fun isSavingsReportQuery(query: String): Boolean {
            val q = query.lowercase(Locale.US).trim()
            val isAdvice = q.contains("how to save") ||
                    q.contains("how can i save") ||
                    q.contains("how should i save") ||
                    q.contains("how do i save") ||
                    q.contains("ဘယ်လိုစု") ||
                    q.contains("စုရမလဲ") ||
                    q.contains("စုရင်ကောင်းမလဲ") ||
                    q.contains("ငွေစုနည်း") ||
                    q.contains("ငွေစုဖို့")
            if (isAdvice) return false

            val patterns = listOf(
                "report about my savings", "report about savings", "report my savings",
                "savings report", "saving report", "report saving", "report savings",
                "my savings", "my saving", "saving summary", "savings summary",
                "challenge report", "challenges report", "my challenges", "challenge summary",
                "savings status", "saving status", "how much did i save", "how much have i saved",
                "savings condition", "saving condition", "savings progress", "saving progress",
                "how is my saving", "how are my savings", "how's my savings", "how my savings condition", "how my saving condition",
                "how saving is going", "how savings is going", "how is saving going", "how is savings going",
                "how are savings going", "how are my savings going", "how is my saving going", "how is my savings going",
                "how's saving going", "how's savings going", "how's my saving going", "how's my savings going",
                "how saving going", "how savings going", "how my saving is going", "how my savings is going",
                "how saving is doing", "how savings is doing", "how is saving doing", "how are savings doing",
                "how is my saving doing", "how are my savings doing", "how my savings is doing", "how my saving is doing",
                "saving is going", "savings is going", "savings are going", "saving going", "savings going",
                "saving doing", "savings doing", "saving update", "savings update",
                "saving tracker", "savings tracker", "saving tracking", "savings tracking",
                "challenge status", "challenges status", "challenge progress", "challenges progress",
                "how challenge is going", "how challenges are going", "how is challenge going", "how are challenges going",
                "how's challenge going", "how's challenges going", "how my challenge is going", "how my challenges are going",
                "how's my challenge going", "how's my challenges going",
                "ငွေစုတာ အစီရင်ခံစာ", "ငွေစု အစီရင်ခံစာ", "စုထားတဲ့ငွေ အစီရင်ခံစာ",
                "စိန်ခေါ်မှု အစီရင်ခံစာ", "ငွေစုစာရင်း", "ငွေစုထားတာ", "ငွေစုတာ", "စိန်ခေါ်မှုများ",
                "ငွေစုအခြေအနေ", "ငွေစုတဲ့ အခြေအနေ", "ငွေစုထားတဲ့ အခြေအနေ",
                "ငွေစုတာ ဘယ်လိုလဲ", "ငွေစုတာ ဘယ်လိုရှိလဲ", "ငွေစုတာ ဘယ်လိုသွားနေလဲ",
                "ငွေစုတာ ဘယ်လောက်ရပြီလဲ", "ငွေစုတာ ဘယ်လောက်ရှိပြီလဲ", "ငွေဘယ်လောက်စုမိပြီလဲ",
                "စုထားတဲ့ငွေ ဘယ်လောက်ရှိပြီလဲ", "စိန်ခေါ်မှု ဘယ်လိုရှိလဲ", "စိန်ခေါ်မှု အခြေအနေ", "စိန်ခေါ်မှုများ အခြေအနေ"
            )
            if (patterns.any { q.contains(it) }) return true

            val savingsProgressRegex = Regex("(?i)\\bhow\\b.*\\b(?:saving|savings|challenge|challenges)\\b.*\\b(?:going|doing|condition|progress|status|coming|along)\\b")
            val savingsStateRegex = Regex("(?i)\\b(?:saving|savings|challenge|challenges)\\b.*\\b(?:going|doing|condition|progress|status|coming|along)\\b")

            return savingsProgressRegex.containsMatchIn(q) || savingsStateRegex.containsMatchIn(q)
        }

        fun isSavingAdviceQuery(query: String): Boolean {
            if (isExpenseReportQuery(query)) return false
            if (isSavingsReportQuery(query)) return false
            if (isMarketPriceQuery(query)) return false
            if (isNewsQuery(query)) return false
            val q = query.lowercase(Locale.US).trim()

            // Burmese saving advice patterns
            // e.g. "Bluetooth speaker လိုချင်တာ ၅၀၀၀၀ တဲ့ ဘယ်လိုစုရင်ကောင်းမလဲ", "ဖုန်းဝယ်ချင်လို့ ၁၀၀၀၀၀ ဘယ်လိုစုရမလဲ", "ဘယ်လိုငွေစုရမလဲ"
            val hasBurmeseSaving = q.contains("ဘယ်လိုစု") ||
                    q.contains("ငွေဘယ်လိုစု") ||
                    q.contains("ဘယ်လိုငွေစု") ||
                    q.contains("ငွေစုနည်း") ||
                    q.contains("ငွေစုဖို့") ||
                    q.contains("စုရင်ကောင်းမလဲ") ||
                    q.contains("စုရမလဲ") ||
                    Regex("(?:လိုချင်|ဝယ်ချင်).*(?:စု|ဘယ်လို)").containsMatchIn(q)

            if (hasBurmeseSaving) return true

            // English saving advice patterns
            val enPatterns = listOf(
                "how to save", "how can i save", "how should i save", "how do i save",
                "saving advice", "saving tip", "saving tips", "saving plan", "plan to save",
                "save money", "how to save money", "tips to save", "advice on saving"
            )
            if (enPatterns.any { q.contains(it) }) return true

            val hasEnglishGoal = Regex("(?i)(?:want to buy|want to save for|saving up for|how to save for).*(?:save|plan|how|much|goal)").containsMatchIn(q)
            return hasEnglishGoal
        }

        fun parseTimeframe(text: String): Pair<Int?, String> {
            val lower = text.lowercase(Locale.US)

            // English month patterns
            if (Regex("\\b(?:in|within)?\\s*(?:one|1|a)\\s*month\\b").containsMatchIn(lower)) {
                return Pair(30, "1 Month")
            }
            val enMonthsMatch = Regex("\\b(?:in|within)?\\s*(\\d+)\\s*months?\\b").find(lower)
            if (enMonthsMatch != null) {
                val m = enMonthsMatch.groupValues[1].toIntOrNull() ?: 1
                return Pair(m * 30, "$m Months")
            }
            if (Regex("\\b(?:in|within)?\\s*two\\s*months\\b").containsMatchIn(lower)) {
                return Pair(60, "2 Months")
            }

            // English week patterns
            if (Regex("\\b(?:in|within)?\\s*(?:one|1|a)\\s*week\\b").containsMatchIn(lower)) {
                return Pair(7, "1 Week")
            }
            if (Regex("\\b(?:in|within)?\\s*(?:two|2)\\s*weeks\\b").containsMatchIn(lower)) {
                return Pair(14, "2 Weeks")
            }
            val enWeeksMatch = Regex("\\b(?:in|within)?\\s*(\\d+)\\s*weeks?\\b").find(lower)
            if (enWeeksMatch != null) {
                val w = enWeeksMatch.groupValues[1].toIntOrNull() ?: 1
                return Pair(w * 7, "$w Weeks")
            }

            // English day patterns
            val enDaysMatch = Regex("\\b(?:in|within)?\\s*(\\d+)\\s*days?\\b").find(lower)
            if (enDaysMatch != null) {
                val d = enDaysMatch.groupValues[1].toIntOrNull() ?: 1
                return Pair(d, "$d Days")
            }

            // English year patterns
            if (Regex("\\b(?:in|within)?\\s*(?:one|1|a)\\s*year\\b").containsMatchIn(lower)) {
                return Pair(365, "1 Year")
            }

            // Burmese month patterns
            if (Regex("(?:တစ်လ|၁\\s*လ|1\\s*လ)(?:အတွင်း)?").containsMatchIn(text)) {
                return Pair(30, "၁ လ")
            }
            val myMonthMatch = Regex("(\\d+)\\s*လ(?:အတွင်း)?").find(text)
            if (myMonthMatch != null) {
                val m = myMonthMatch.groupValues[1].toIntOrNull() ?: 1
                return Pair(m * 30, "$m လ")
            }

            // Burmese week patterns
            if (Regex("(?:တစ်ပတ်|၁\\s*ပတ်|1\\s*ပတ်)(?:အတွင်း)?").containsMatchIn(text)) {
                return Pair(7, "၁ ပတ်")
            }
            if (Regex("(?:နှစ်ပတ်|၂\\s*ပတ်|2\\s*ပတ်)(?:အတွင်း)?").containsMatchIn(text)) {
                return Pair(14, "၂ ပတ်")
            }
            val myWeekMatch = Regex("(\\d+)\\s*ပတ်(?:အတွင်း)?").find(text)
            if (myWeekMatch != null) {
                val w = myWeekMatch.groupValues[1].toIntOrNull() ?: 1
                return Pair(w * 7, "$w ပတ်")
            }

            // Burmese day patterns
            val myDayMatch = Regex("(\\d+)\\s*ရက်(?:အတွင်း)?").find(text)
            if (myDayMatch != null) {
                val d = myDayMatch.groupValues[1].toIntOrNull() ?: 1
                return Pair(d, "$d ရက်")
            }

            return Pair(null, "")
        }

        private fun cleanGoalItemName(raw: String): String {
            val cleaned = raw
                .replace(Regex("(?i)^(?:i\\s+want\\s+to\\s+buy|i\\s+want\\s+to\\s+save(?:\\s+for)?|i\\s+want|buy|save|a|an|the|for|on|in)\\s+"), "")
                .replace(Regex("(?i)\\b(?:in|within)?\\s*(?:one|two|three|four|five|six|seven|eight|nine|ten|a|an|\\d+)?\\s*(?:days?|weeks?|months?|years?)\\b"), "")
                .replace(Regex("(?:တစ်လ|၁\\s*လ|\\d+\\s*လ|တစ်ပတ်|၁\\s*ပတ်|\\d+\\s*ပတ်|\\d+\\s*ရက်)(?:အတွင်း)?"), "")
                .replace(Regex("(?i)\\s+(?:for|on|in|at|cost|costs|costing|တန်|ဖိုး|အတွက်|ဝယ်ဖို့|ဝယ်ရန်)$"), "")
                .trim()
            if (cleaned.equals("k", ignoreCase = true) ||
                cleaned.equals("ks", ignoreCase = true) ||
                cleaned.equals("mmk", ignoreCase = true) ||
                cleaned.equals("usd", ignoreCase = true) ||
                cleaned.equals("kyat", ignoreCase = true) ||
                cleaned.equals("kyats", ignoreCase = true) ||
                cleaned == "ကျပ်" ||
                cleaned.all { it.isDigit() || it.isWhitespace() } ||
                cleaned.length <= 1) {
                return ""
            }
            return cleaned
        }

        private fun parseRawAmount(raw: String): Double {
            val clean = raw.replace(",", "").trim()
            if (clean.endsWith("k", ignoreCase = true)) {
                val base = clean.dropLast(1).trim().toDoubleOrNull() ?: 0.0
                return base * 1000.0
            }
            return clean.toDoubleOrNull() ?: 0.0
        }

        fun parseSavingGoal(query: String): ParsedSavingGoal {
            val normalized = com.savingcoach.app.utils.BurmeseNumeralConverter.convert(query).trim()
            val (targetDays, timeframeText) = parseTimeframe(normalized)

            // 1. Burmese with item, amount, and desire verb
            // e.g. "Bluetooth speaker လိုချင်တာ 50000 တဲ့ ဘယ်လိုစုရင်ကောင်းမလဲ", "ဖုန်း 100000 ဝယ်ချင်လို့ ဘယ်လိုစုရမလဲ"
            val myGoalRegex1 = Regex("(.+?)(?:\\s*လိုချင်(?:တာ|လို့)?|\\s*ဝယ်ချင်(?:တာ|လို့)?|\\s*အတွက်)\\s*([\\d,]+(?:\\.\\d+)?(?:\\s*[kK](?![a-zA-Z]))?)\\s*(?:ကျပ်|ks|mmk|တဲ့)?.*", RegexOption.IGNORE_CASE)
            val match1 = myGoalRegex1.find(normalized)
            if (match1 != null) {
                val rawItem = match1.groupValues[1].trim()
                val amount = parseRawAmount(match1.groupValues[2])
                val cleanItem = cleanGoalItemName(rawItem)
                if (amount > 0 || cleanItem.isNotBlank()) {
                    return ParsedSavingGoal(item = cleanItem, amount = amount, targetDays = targetDays, timeframeText = timeframeText)
                }
            }

            // 2. Burmese with amount first, then item
            // e.g. "50000 တန် Bluetooth speaker ဝယ်ချင်လို့ ဘယ်လိုစုရမလဲ"
            val myGoalRegex2 = Regex("([\\d,]+(?:\\.\\d+)?(?:\\s*[kK](?![a-zA-Z]))?)\\s*(?:ကျပ်|ks|mmk)?(?:\\s*တန်)?\\s*(.+?)(?:\\s*လိုချင်|\\s*ဝယ်ချင်|\\s*အတွက်|\\s*ဝယ်ဖို့).*", RegexOption.IGNORE_CASE)
            val match2 = myGoalRegex2.find(normalized)
            if (match2 != null) {
                val amount = parseRawAmount(match2.groupValues[1])
                val rawItem = match2.groupValues[2].trim()
                val cleanItem = cleanGoalItemName(rawItem)
                if (amount > 0 || cleanItem.isNotBlank()) {
                    return ParsedSavingGoal(item = cleanItem, amount = amount, targetDays = targetDays, timeframeText = timeframeText)
                }
            }

            // 3. Burmese with amount and saving verb (without specific item)
            // e.g. "100k ဘယ်လိုစုရမလဲ", "၁ ပတ် အတွင်း 100k ဘယ်လိုစုရမလဲ", "၅၀၀၀၀ စုဖို့ ဘယ်လိုလုပ်ရမလဲ"
            val myGoalRegex3 = Regex("([\\d,]+(?:\\.\\d+)?(?:\\s*[kK](?![a-zA-Z]))?)\\s*(?:ကျပ်|ks|mmk)?\\s*(?:ဘယ်လို)?\\s*(?:စု(?:ရမလဲ|ရင်ကောင်းမလဲ|ဖို့|ချင်လို့|ချင်တာ|မယ်|ကြမလဲ|စမလဲ))", RegexOption.IGNORE_CASE)
            val match3 = myGoalRegex3.find(normalized)
            if (match3 != null) {
                val amount = parseRawAmount(match3.groupValues[1])
                if (amount > 0) {
                    return ParsedSavingGoal(item = "", amount = amount, targetDays = targetDays, timeframeText = timeframeText)
                }
            }

            // 4. English - "how to save [amount] for [item]" or "how to save for [item] [amount]"
            val enGoalRegex1 = Regex("(?i)(?:how\\s+(?:can|should|to|do)\\s+(?:i\\s+)?save)\\s*(?:([\\d,]+(?:\\.\\d+)?(?:\\s*[kK](?![a-zA-Z]))?)\\s*(?:mmk|ks|usd)?\\s*(?:for|on|in)?\\s*(.+)|(?:for|on)\\s*(.+?)\\s+(?:for|cost|costs|costing|priced at)?\\s*([\\d,]+(?:\\.\\d+)?(?:\\s*[kK](?![a-zA-Z]))?))")
            val enMatch1 = enGoalRegex1.find(normalized)
            if (enMatch1 != null) {
                if (enMatch1.groupValues[1].isNotBlank()) {
                    val amount = parseRawAmount(enMatch1.groupValues[1])
                    val rawItem = enMatch1.groupValues[2].trim()
                    val cleanItem = cleanGoalItemName(rawItem)
                    return ParsedSavingGoal(item = cleanItem, amount = amount, targetDays = targetDays, timeframeText = timeframeText)
                } else if (enMatch1.groupValues[3].isNotBlank()) {
                    val rawItem = enMatch1.groupValues[3].trim()
                    val amount = parseRawAmount(enMatch1.groupValues[4])
                    val cleanItem = cleanGoalItemName(rawItem)
                    return ParsedSavingGoal(item = cleanItem, amount = amount, targetDays = targetDays, timeframeText = timeframeText)
                }
            }

            // 5. Fallback - find all numbers in the query and select the target monetary amount
            val numMatches = Regex("([\\d,]+(?:\\.\\d+)?(?:\\s*[kK](?![a-zA-Z]))?)").findAll(normalized).toList()
            val parsedAmounts = numMatches.map { parseRawAmount(it.groupValues[1]) }.filter { it > 0 }
            val fallbackAmount = when {
                parsedAmounts.isEmpty() -> 0.0
                parsedAmounts.size == 1 -> parsedAmounts.first()
                else -> {
                    // When multiple numbers exist (e.g. timeframe "1 week" and target "100000"),
                    // the target monetary amount is the largest number.
                    parsedAmounts.maxOrNull() ?: 0.0
                }
            }
            return ParsedSavingGoal(item = "", amount = fallbackAmount, targetDays = targetDays, timeframeText = timeframeText)
        }

        fun isSavingAdviceIncomplete(content: String): Boolean {
            val trimmed = content.trim()
            if (trimmed.length < 40) return true
            if (trimmed.endsWith(",") ||
                trimmed.endsWith(":") ||
                trimmed.endsWith("•") ||
                trimmed.endsWith("-") ||
                trimmed.endsWith("**") ||
                trimmed.endsWith("/") ||
                trimmed.endsWith("(") ||
                trimmed.endsWith("[") ||
                trimmed.endsWith("=") ||
                trimmed.endsWith("+") ||
                trimmed.endsWith("*")
            ) return true
            if (trimmed.count { it == '(' } > trimmed.count { it == ')' }) return true
            if (trimmed.count { it == '[' } > trimmed.count { it == ']' }) return true
            return false
        }

        fun isFinancialReportQuery(query: String): Boolean {
            if (isExpenseReportQuery(query)) return false
            if (isSavingsReportQuery(query)) return false
            if (isSavingAdviceQuery(query)) return false
            if (isMarketPriceQuery(query)) return false
            if (isNewsQuery(query)) return false
            if (isInvestmentAdviceQuery(query)) return false
            val q = query.lowercase(Locale.US).trim()
            if (q == "report" || q == "financial report" || q == "finance report" || q == "wealth" || q == "my wealth") return true
            val patterns = listOf(
                "report about my financial", "report my financial", "inventory", "spending and saving",
                "spending & saving", "financial status", "financial overview",
                "financial summary", "my financial", "my finances",
                "how my inventory is going", "how is my inventory", "all the spending and saving",
                "how am i doing", "how is my money", "where did my money go",
                "wealth", "net worth", "networth", "portfolio",
                "financial analysis", "analyze my financial", "analysis my financial",
                "analyze my finances", "analysis my finances", "analyze my money", "analysis of my finances",
                "analysis my wealth", "analyze my wealth", "analyse my wealth", "analysis of my wealth",
                "wealth analysis", "wealth report", "wealth overview", "analyze wealth",
                "my net worth", "investment report", "investment overview", "investment analysis",
                "analyze my investment", "analyze my investments", "my investments", "my investment",
                "analyze my portfolio", "analysis my portfolio", "portfolio analysis", "portfolio report",
                "ဘဏ္ဍာရေး", "အခြေအနေ", "သုံးတာနဲ့ စုတာ", "အစီရင်ခံစာ", "ငွေစာရင်း",
                "ကြွယ်ဝမှု", "ချမ်းသာကြွယ်ဝမှု", "ပိုင်ဆိုင်မှု", "ငွေကြေးသုံးသပ်ချက်",
                "ရင်းနှီးမြှုပ်နှံမှု အစီရင်ခံစာ", "ရင်းနှီးမြှုပ်နှံမှု အခြေအနေ"
            )
            return patterns.any { q.contains(it) }
        }

        fun isTruncatedOrIncomplete(content: String): Boolean {
            val trimmed = content.trim()
            if (trimmed.length < 150) return true

            // Trailing cutoffs (ends with unfinished punctuation, operators, or open brackets)
            if (trimmed.endsWith(",") ||
                trimmed.endsWith(":") ||
                trimmed.endsWith("•") ||
                trimmed.endsWith("-") ||
                trimmed.endsWith("**") ||
                trimmed.endsWith("/") ||
                trimmed.endsWith("(") ||
                trimmed.endsWith("[") ||
                trimmed.endsWith("=") ||
                trimmed.endsWith("+") ||
                trimmed.endsWith("*")
            ) return true

            // Unclosed brackets or parentheses indicating mid-expression cut-off
            if (trimmed.count { it == '(' } > trimmed.count { it == ')' }) return true
            if (trimmed.count { it == '[' } > trimmed.count { it == ']' }) return true

            // Mid-number cutoffs like "100,"
            if (Regex("(?i)\\b100,\\s*$").containsMatchIn(trimmed)) return true

            // Leaked system prompt directives or meta-instructions
            if (Regex("(?i)(?:Analyze User Input|Identify Role|Determine the Gap|Identify Required Fields|Extract merchant)").containsMatchIn(trimmed)) return true
            if (Regex("(?i)(?:What remaining funds and days left mean|Specific daily spending guardrail|Remaining Budget\\s*/|Address the single biggest spending category|Exactly ONE practical|Life Interpretation: What remaining)").containsMatchIn(trimmed)) return true

            // Financial reports that lack actual figures (only leaked boilerplate or definitions without values)
            val hasDigits = trimmed.any { it.isDigit() }
            val isBoilerplateOnly = !hasDigits && (trimmed.contains("Safe-to-Spend", ignoreCase = true) || trimmed.contains("Life Interpretation", ignoreCase = true))
            if (isBoilerplateOnly) return true

            return false
        }

        fun buildSavingAdviceResponse(userId: String, query: String, language: String): String {
            val goal = parseSavingGoal(query)
            val item = goal.item
            val amount = goal.amount
            val targetDays = goal.targetDays
            val timeframeText = goal.timeframeText

            return if (amount > 0) {
                val formattedAmt = String.format(Locale.US, "%,.0f", amount)

                if (targetDays != null && targetDays > 0) {
                    val dailyPace = amount / targetDays.toDouble()
                    val formattedDaily = String.format(Locale.US, "%,.0f", dailyPace)

                    if (language == "my") {
                        val myAmt = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formattedAmt)
                        val myDaily = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formattedDaily)
                        val myTargetDays = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(targetDays.toString())
                        val tfBurmese = if (timeframeText.isNotBlank()) timeframeText else "$myTargetDays ရက်"
                        val header = if (item.isNotBlank()) {
                            "🎯 $item အတွက် $myAmt ကျပ် စုဆောင်းရန် အစီအစဉ် ($tfBurmese အတွင်း)"
                        } else {
                            "🎯 $myAmt ကျပ် စုဆောင်းရန် အစီအစဉ် ($tfBurmese အတွင်း)"
                        }

                        val weeklySection = if (targetDays >= 14) {
                            val weeks = (targetDays / 7).coerceAtLeast(1)
                            val weeklyPace = amount / weeks.toDouble()
                            val formattedWeekly = String.format(Locale.US, "%,.0f", weeklyPace)
                            val myWeekly = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formattedWeekly)
                            val myWeeks = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(weeks.toString())
                            "\n\n၂။ အပတ်စဉ် စုဆောင်းနည်း:\n• တစ်ပတ်လျှင် $myWeekly ကျပ် ($myWeeks ပတ်) ပုံမှန် စုဆောင်းပါ။"
                        } else ""

                        val biWeeklySection = if (targetDays >= 28) {
                            val biWeeklyPace = amount / 2.0
                            val formattedBiWeekly = String.format(Locale.US, "%,.0f", biWeeklyPace)
                            val myBiWeekly = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formattedBiWeekly)
                            val myTwo = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits("2")
                            val myFifteen = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits("15")
                            "\n\n၃။ လဝက်တစ်ကြိမ် စုဆောင်းနည်း:\n• $myFifteen ရက်တစ်ကြိမ် (လစာရချိန်) $myBiWeekly ကျပ်စီ $myTwo ကြိမ် စုဆောင်းပါ။"
                        } else ""

                        val challengeTitle = if (item.isNotBlank()) item else "$tfBurmese ပန်းတိုင်"

                        """
                        $header

                        $tfBurmese အတွင်း $myAmt ကျပ် ပြည့်မီရန် အောက်ပါအတိုင်း ခွဲဝေစုဆောင်းနိုင်ပါတယ်-

                        ၁။ နေ့စဉ် စုဆောင်းနည်း:
                        • တစ်ရက်လျှင် $myDaily ကျပ် ($myTargetDays ရက်တိတိ) စုဆောင်းပါ။$weeklySection$biWeeklySection

                        💡 အကြံပြုချက်-
                        • နေ့စဉ် မနက်တိုင်း $myDaily ကျပ်ကို မသုံးစွဲမီ သီးသန့်ဖယ်ထားခြင်းဖြင့် ပန်းတိုင်ကို အလွယ်တကူ ရောက်ရှိနိုင်ပါတယ်။
                        • App ထဲရှိ Challenges tab တွင် '$challengeTitle' အမည်ဖြင့် စိန်ခေါ်မှုအသစ်တစ်ခု ဖန်တီးပြီး ယနေ့မှစတင် စုဆောင်းလိုက်ပါ!
                        """.trimIndent()
                    } else {
                        val tfEnglish = if (timeframeText.isNotBlank()) timeframeText else "$targetDays Days"
                        val header = if (item.isNotBlank()) {
                            "🎯 Saving Plan for $item: $formattedAmt MMK in $tfEnglish"
                        } else {
                            "🎯 Saving Plan: $formattedAmt MMK in $tfEnglish ($targetDays Days)"
                        }

                        val weeklySection = if (targetDays >= 14) {
                            val weeks = (targetDays / 7).coerceAtLeast(1)
                            val weeklyPace = amount / weeks.toDouble()
                            val formattedWeekly = String.format(Locale.US, "%,.0f", weeklyPace)
                            "\n\n2. Weekly Target:\n• Save $formattedWeekly MMK / week across $weeks weeks."
                        } else ""

                        val biWeeklySection = if (targetDays >= 28) {
                            val biWeeklyPace = amount / 2.0
                            val formattedBiWeekly = String.format(Locale.US, "%,.0f", biWeeklyPace)
                            "\n\n3. Bi-Weekly / Payday Option:\n• Save $formattedBiWeekly MMK every 2 weeks (2 installments)."
                        } else ""

                        val challengeTitle = if (item.isNotBlank()) item else "$tfEnglish Target"

                        """
                        $header

                        To reach your $formattedAmt MMK goal within $tfEnglish, here is your breakdown:

                        1. Daily Target:
                        • Save $formattedDaily MMK / day for $targetDays days.$weeklySection$biWeeklySection

                        💡 Coach Tip:
                        • Setting aside $formattedDaily MMK each morning into a separate wallet before daily spending keeps you completely on track.
                        • Create a new '$challengeTitle' challenge in the Challenges tab to track your progress daily!
                        """.trimIndent()
                    }
                } else {
                    val targetItem = if (item.isNotBlank()) item else if (language == "my") "ပစ္စည်း" else "your goal"

                    val fastDaily = amount / 10
                    val modDaily = amount / 20
                    val relaxedDaily = amount / 50

                    val formattedFast = String.format(Locale.US, "%,.0f", fastDaily)
                    val formattedMod = String.format(Locale.US, "%,.0f", modDaily)
                    val formattedRelaxed = String.format(Locale.US, "%,.0f", relaxedDaily)

                    if (language == "my") {
                        val myAmt = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formattedAmt)
                        val myFast = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formattedFast)
                        val myMod = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formattedMod)
                        val myRelaxed = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formattedRelaxed)

                        val header = if (item.isNotBlank()) {
                            "🎯 $targetItem ($myAmt ကျပ်) ဝယ်ယူရန် ငွေစုအကြံပြုချက်များ"
                        } else {
                            "🎯 $myAmt ကျပ် စုဆောင်းရန် ငွေစုအကြံပြုချက်များ"
                        }

                        """
                        $header

                        သင့်အတွက် သင့်တော်မည့် ငွေစုနည်းလမ်း ၃ မျိုး ဖြစ်ပါတယ်-

                        ၁။ ရက်တို စုဆောင်းနည်း (၁၀ ရက်)
                        • တစ်ရက်လျှင် $myFast ကျပ် စုဆောင်းပါက ၁၀ ရက်အတွင်း ရရှိပါမည်။

                        ၂။ အသင့်အတင့် စုဆောင်းနည်း (ရက် ၂၀ / ၃ ပတ်ခန့်)
                        • တစ်ရက်လျှင် $myMod ကျပ် စုဆောင်းပါက ရက် ၂၀ အတွင်း ရရှိပါမည်။

                        ၃။ အေးအေးဆေးဆေး စုဆောင်းနည်း (ရက် ၅၀)
                        • တစ်ရက်လျှင် $myRelaxed ကျပ် စုဆောင်းပါက ရက် ၅၀ အတွင်း ရရှိပါမည်။

                        💡 အကြံပြုချက်-
                        • နေ့စဉ် မုန့်ဖိုး၊ ကော်ဖီဖိုး စသည့် အသုံးစရိတ်အသေးစားလေးများကို အနည်းငယ် လျှော့ချရုံဖြင့် အလွယ်တကူ စုဆောင်းနိုင်ပါတယ်။
                        • App ထဲတွင် '$targetItem' အမည်ဖြင့် Saving Challenge အသစ်တစ်ခု ဖန်တီးပြီး ယနေ့မှစတင် စုဆောင်းလိုက်ပါ!
                        """.trimIndent()
                    } else {
                        val header = if (item.isNotBlank()) {
                            "🎯 Saving Plan for $targetItem ($formattedAmt MMK)"
                        } else {
                            "🎯 Saving Plan for $formattedAmt MMK"
                        }

                        """
                        $header

                        Here are 3 realistic daily saving paces you can choose from:

                        1. Sprint Pace (10 Days):
                        • Save $formattedFast MMK / day to reach your goal in 10 days.

                        2. Balanced Pace (20 Days / ~3 Weeks):
                        • Save $formattedMod MMK / day to reach your goal in 20 days.

                        3. Relaxed Habit Pace (50 Days):
                        • Save $formattedRelaxed MMK / day to steadily reach your goal without stress.

                        💡 Coach Tip:
                        • Trimming small daily discretionary spending like snacks or dining out easily funds this.
                        • Create a new Saving Challenge titled '$targetItem' in the app to track your daily progress!
                        """.trimIndent()
                    }
                }
            } else {
                if (language == "my") {
                    """
                    🎯 ငွေစုဆောင်းခြင်း အကြံပြုချက်

                    ငွေစုဆောင်းရန် အောက်ပါ အခြေခံအချက် ၃ ချက်ကို လိုက်နာနိုင်ပါတယ်-
                    ၁။ ၅၀/၃၀/၂၀ စည်းမျဉ်းသုံးပါ - ဝင်ငွေ၏ ၂၀% ကို မဖြစ်မနေ စုဆောင်းပါ။
                    ၂။ နေ့စဉ် မလိုအပ်သော ကော်ဖီဖိုး၊ မုန့်ဖိုး အသုံးစရိတ်များကို လျှော့ချပါ။
                    ၃။ စိန်ခေါ်မှု အသစ်တစ်ခု စတင်ပါ - Challenges tab သို့သွား၍ 1K a Day စိန်ခေါ်မှုကို စတင်လိုက်ပါ။
                    """.trimIndent()
                } else {
                    """
                    🎯 Saving Advice & Tips

                    To build a strong savings habit, try these 3 proven steps:
                    1. Use the 50/30/20 Rule: Allocate 20% of your income directly to savings first.
                    2. Cut micro-expenses: Daily snacks or unnecessary subscriptions add up fast.
                    3. Start small: Go to the Challenges tab and join the '1K a Day' challenge!
                    """.trimIndent()
                }
            }
        }

        fun isInvestmentAdviceQuery(query: String): Boolean {
            if (isExpenseReportQuery(query)) return false
            if (isSavingsReportQuery(query)) return false
            if (isMarketPriceQuery(query)) return false
            if (isNewsQuery(query)) return false
            val q = query.lowercase(Locale.US).trim()

            // Exclude explicit expense logging (e.g. "bought gold for 50000 ks", "ရွှေ ၅၀၀၀၀ ဖိုး ဝယ်တယ်")
            if (Regex("(?i)(?:bought|spend|spent|paid).*\\d+").containsMatchIn(q)) return false
            if (Regex("(?:သုံး|ဝယ်ထား|ဝယ်ခဲ့|ဝယ်တယ်).*\\d+").containsMatchIn(q)) return false

            val assetTerms = listOf(
                "bitcoin", "btc", "crypto", "cryptocurrency", "eth", "ethereum", "sol", "solana",
                "gold", "gld", "xau", "silver", "stock", "stocks", "share", "shares", "equity", "etf",
                "tesla", "tsla", "apple", "aapl", "nvidia", "nvda", "google", "microsoft", "amazon",
                "s&p", "sp500", "nasdaq", "bnb", "binance", "doge", "xrp", "ada", "cardano",
                "token", "coin", "altcoin",
                "ဘစ်ကွိုင်", "ခရစ်ပတို", "ရွှေ", "စတော့", "ရှယ်ယာ", "ဒေါ်လာ", "ဆိုလာနာ", "တက်စလာ"
            )
            val mentionsAsset = assetTerms.any { q.contains(it) }

            val adviceVerbs = listOf(
                "ဝယ်သင့်", "ဝယ်သင့်မသင့်", "ဝယ်သင့် မဝယ်သင့်", "ဝယ်သင့်မဝယ်သင့်", "မဝယ်သင့်",
                "ဝယ်ရမလား", "ဝယ်ရမှာလား", "ဝယ်ရင်ကောင်း", "ဝယ်ဖို့ကောင်း",
                "ဝယ်လို့ကောင်း", "ဝယ်ထားသင့်", "ဝယ်စုသင့်",
                "စုသင့်", "ရင်းနှီးမြှုပ်နှံ", "ရင်းနှီးမြုပ်နှံ",
                "ဘာဝယ်သင့်", "ဘယ်ဟာဝယ်သင့်", "ဘယ်ဟာပိုကောင်း", "ဘယ်ဟာကောင်း", "ဘယ်ဟာ ဝယ်ရမလဲ", "ဘယ်ဟာ စုသင့်",
                "ဘယ်ဟာ", "ဘာဝယ်", "ဘယ်ဟာဝယ်"
            )
            val hasAdviceVerb = adviceVerbs.any { q.contains(it) }

            // 1. Mentions asset + Burmese advice verb (e.g. "ခုချိန် Bitcoin ဝယ်သင့်လား", "Crypto ဝယ်ရင်ကောင်းမလား", "ရွှေဝယ်သင့်လား", "Tesla stock ဝယ်ရင်ကောင်းမလား", "btc နဲ့ sol ဘာဝယ်သင့်")
            if (mentionsAsset && hasAdviceVerb) return true

            // Comparison patterns (e.g. "btc နဲ့ sol ဘာဝယ်သင့်", "btc vs sol which should i buy")
            val comparisonPatterns = listOf(
                "btc vs sol", "sol vs btc", "btc and sol", "sol and btc", "btc နဲ့ sol", "sol နဲ့ btc",
                "which should i buy", "which one to buy", "which is better", "which one is better",
                "vs", "versus"
            )
            if (mentionsAsset && comparisonPatterns.any { q.contains(it) }) return true

            // Budget/expense-linked investment queries (e.g. "ဒီလအသုံးစရိတ်ကို ကြည့်ပြီး sol ဝယ်သင့် မဝယ်သင့်", "based on my spending should I buy")
            val budgetLinkedPhrases = listOf(
                "အသုံးစရိတ်ကို ကြည့်ပြီး", "အသုံးစရိတ် ကြည့်ပြီး", "အသုံးစရိတ်ကိုကြည့်ပြီး",
                "ဘတ်ဂျက်ကို ကြည့်ပြီး", "ဘတ်ဂျက် ကြည့်ပြီး", "ဘတ်ဂျက်ကိုကြည့်ပြီး",
                "based on my spending", "looking at my expenses", "based on my budget", "looking at my budget"
            )
            if (budgetLinkedPhrases.any { q.contains(it) } && (mentionsAsset || q.contains("ဝယ်") || q.contains("buy") || q.contains("invest") || q.contains("စု"))) return true
            if (q.contains("ဝယ်သင့် မဝယ်သင့်") || q.contains("ဝယ်သင့်မဝယ်သင့်")) return true

            // 2. Burmese standalone advice patterns
            val hasBurmeseInvestment = q.contains("ရွှေဝယ်") ||
                    q.contains("ရွှေစု") ||
                    q.contains("ရွှေရင်းနှီးမြှုပ်နှံ") ||
                    (q.contains("ရွှေ") && (q.contains("ဝယ်") || q.contains("သင့်") || q.contains("ကောင်း") || q.contains("စု"))) ||
                    (q.contains("ဘစ်ကွိုင်") && (q.contains("ဝယ်") || q.contains("သင့်") || q.contains("ကောင်း") || q.contains("စု"))) ||
                    (q.contains("ခရစ်ပတို") && (q.contains("ဝယ်") || q.contains("သင့်") || q.contains("ကောင်း") || q.contains("စု"))) ||
                    (q.contains("စတော့") && (q.contains("ဝယ်") || q.contains("သင့်") || q.contains("ကောင်း") || q.contains("စု"))) ||
                    (q.contains("တက်စလာ") && (q.contains("ဝယ်") || q.contains("သင့်") || q.contains("ကောင်း") || q.contains("စု"))) ||
                    q.contains("ရင်းနှီးမြှုပ်နှံ") ||
                    q.contains("ရင်းနှီးမြုပ်နှံ") ||
                    q.contains("ဝယ်သင့်လား") ||
                    q.contains("ဝယ်သင့်သလား") ||
                    q.contains("ဝယ်ရင်ကောင်းမလား") ||
                    q.contains("ဝယ်ဖို့ကောင်းလား") ||
                    q.contains("ဝယ်လို့ကောင်းလား")

            if (hasBurmeseInvestment) return true

            // 3. English investment advice patterns
            val enAdviceTerms = listOf(
                "should i buy", "should we buy", "good time to buy", "buy today",
                "buy right now", "buy now", "is it good to buy", "is now a good time",
                "invest in", "investing in", "investment advice", "advice on",
                "where to invest", "where should i invest", "how to invest", "how should i invest",
                "how to start investing", "good investment", "investment strategy", "investment plan"
            )
            if (mentionsAsset && enAdviceTerms.any { q.contains(it) }) return true

            val goldPatterns = listOf(
                "gold should i buy", "should i buy gold", "buy gold today", "invest in gold",
                "investing in gold", "gold investment", "gold investing", "buy gold",
                "is gold a good investment", "should i invest in gold", "buy some gold",
                "advice on gold", "gold advice"
            )
            if (goldPatterns.any { q.contains(it) }) return true

            val cryptoPatterns = listOf(
                "should i buy bitcoin", "should i buy btc", "buy bitcoin", "buy btc",
                "should i buy crypto", "invest in crypto", "invest in bitcoin",
                "crypto investment", "should i buy solana", "should i buy sol",
                "buy crypto", "bitcoin advice", "crypto advice", "should i buy eth",
                "should i buy ethereum", "btc vs sol", "sol vs btc"
            )
            if (cryptoPatterns.any { q.contains(it) }) return true

            val stockPatterns = listOf(
                "should i buy stocks", "how to invest in stocks", "stock investment",
                "invest in shares", "how to buy stocks", "invest in equity", "stock market",
                "buy stocks", "invest in etf", "invest in etfs", "stock advice",
                "should i buy tesla", "should i buy tsla", "tesla stock", "tsla stock",
                "buy tesla stock", "buy tesla", "invest in tesla"
            )
            if (stockPatterns.any { q.contains(it) }) return true

            val generalPatterns = listOf(
                "investment advice", "how to invest", "where to invest", "where should i invest",
                "how should i invest", "how to start investing", "invest my money",
                "investing tips", "investment strategy", "investment plan",
                "what should i invest in", "good investment", "guide to investing"
            )
            if (generalPatterns.any { q.contains(it) }) return true

            return false
        }

        fun isInvestmentAdviceIncomplete(content: String): Boolean {
            val trimmed = content.trim()
            if (trimmed.length < 80) return true

            // Trailing cutoffs
            if (trimmed.endsWith(",") ||
                trimmed.endsWith(":") ||
                trimmed.endsWith("•") ||
                trimmed.endsWith("-") ||
                trimmed.endsWith("**") ||
                trimmed.endsWith("/") ||
                trimmed.endsWith("(") ||
                trimmed.endsWith("[") ||
                trimmed.endsWith("=") ||
                trimmed.endsWith("+") ||
                trimmed.endsWith("*")
            ) return true

            // If response is ONLY a markdown header or only header lines with no body text
            if (Regex("^#{1,6}\\s+[^\\n]+$").matches(trimmed)) return true
            val substantiveLines = trimmed.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
            if (substantiveLines.isEmpty()) return true

            // Unclosed brackets or parentheses
            if (trimmed.count { it == '(' } > trimmed.count { it == ')' }) return true
            if (trimmed.count { it == '[' } > trimmed.count { it == ']' }) return true

            val lower = trimmed.lowercase(Locale.US)
            val refusalPatterns = listOf(
                "don't have access to", "do not have access to",
                "cannot give financial advice", "can't give financial advice",
                "as an ai, i cannot", "as an ai language model",
                "မသိရှိပါ", "မသိပါ", "လက်လှမ်းမမီပါ"
            )
            if (refusalPatterns.any { lower.contains(it) }) return true

            return false
        }

        fun isMarketPriceQuery(query: String): Boolean {
            if (isExpenseReportQuery(query)) return false
            if (isSavingsReportQuery(query)) return false
            val q = query.lowercase(Locale.US).trim()

            // Exclude explicit expense logging (e.g. "bought gold for 50000 ks", "ရွှေ ၅၀၀၀၀ ဖိုး ဝယ်တယ်")
            if (Regex("(?i)(?:bought|spend|spent|paid).*\\d+").containsMatchIn(q)) return false
            if (Regex("(?:သုံး|ဝယ်ထား|ဝယ်ခဲ့|ဝယ်တယ်).*\\d+").containsMatchIn(q)) return false

            val assetKeywords = listOf(
                "gold", "gld", "xau", "bitcoin", "btc", "ethereum", "eth", "solana", "sol",
                "crypto", "cryptocurrency", "stock", "stocks", "share", "shares",
                "ရွှေ", "ဘစ်ကွိုင်", "ခရစ်ပတို", "စတော့", "ရှယ်ယာ", "ဒေါ်လာ"
            )
            val priceKeywords = listOf(
                "price", "cost of", "rate", "quote", "value of", "how much is",
                "what is the price", "what's the price", "what price", "price is",
                "ဈေး", "စျေး", "ပေါက်ဈေး", "ပေါက်စျေး", "ဘယ်လောက်", "ဈေးနှုန်း", "စျေးနှုန်း", "ခြေနေ", "အခြေအနေ"
            )

            val mentionsAsset = assetKeywords.any { q.contains(it) }
            val asksPrice = priceKeywords.any { q.contains(it) }

            if (mentionsAsset && asksPrice) return true

            // Burmese market price queries
            val hasBurmesePrice = (q.contains("ရွှေ") && (q.contains("ဈေး") || q.contains("စျေး") || q.contains("ဘယ်လောက်") || q.contains("ခြေနေ"))) ||
                    (q.contains("ဘစ်ကွိုင်") && (q.contains("ဈေး") || q.contains("စျေး") || q.contains("ဘယ်လောက်") || q.contains("ခြေနေ"))) ||
                    (q.contains("ခရစ်ပတို") && (q.contains("ဈေး") || q.contains("စျေး") || q.contains("ဘယ်လောက်") || q.contains("ခြေနေ"))) ||
                    (q.contains("စတော့") && (q.contains("ဈေး") || q.contains("စျေး") || q.contains("ဘယ်လောက်") || q.contains("ခြေနေ"))) ||
                    q.contains("ရွှေဈေး") || q.contains("ရွှေစျေး") ||
                    q.contains("ရွှေပေါက်ဈေး") || q.contains("ရွှေပေါက်စျေး") ||
                    q.contains("စျေးနှုန်း") || q.contains("ဈေးနှုန်း")
            if (hasBurmesePrice) return true

            // Exact regex for queries like "now what price is gold"
            if (Regex("(?i)(?:what|how much).*price.*(?:gold|bitcoin|btc|eth|sol|crypto|stock|shares|gld)").containsMatchIn(q)) return true
            if (Regex("(?i)(?:gold|bitcoin|btc|eth|sol|crypto|stock|shares|gld).*(?:price|how much|rate)").containsMatchIn(q)) return true

            return false
        }

        fun isMarketPriceResponseIncomplete(content: String): Boolean {
            val trimmed = content.trim()
            if (trimmed.length < 80) return true

            val lower = trimmed.lowercase(Locale.US)
            val refusalPatterns = listOf(
                "don't have access to real-time",
                "do not have access to real-time",
                "don't have access to",
                "do not have access to",
                "current market data in my system",
                "latest market information is currently unavailable",
                "cannot tell you what the current market price is",
                "can't tell you what the current market price is",
                "unable to tell you the price",
                "unable to tell you what the current market price is",
                "unavailable to me",
                "currently unavailable",
                "not available to me",
                "no real-time",
                "don't have real-time",
                "do not have real-time",
                "cannot access real-time",
                "မသိရှိပါ",
                "မရရှိနိုင်ပါ",
                "လက်လှမ်းမမီပါ",
                "မသိရပါ",
                "မရှိသေးပါ"
            )
            if (refusalPatterns.any { lower.contains(it) }) return true

            // Trailing cutoffs
            if (trimmed.endsWith(",") ||
                trimmed.endsWith(":") ||
                trimmed.endsWith("•") ||
                trimmed.endsWith("-") ||
                trimmed.endsWith("**") ||
                trimmed.endsWith("/") ||
                trimmed.endsWith("(") ||
                trimmed.endsWith("[") ||
                trimmed.endsWith("=") ||
                trimmed.endsWith("+") ||
                trimmed.endsWith("*")
            ) return true

            // If response is ONLY a markdown header or only header lines with no body text
            if (Regex("^#{1,6}\\s+[^\\n]+$").matches(trimmed)) return true
            val substantiveLines = trimmed.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
            if (substantiveLines.isEmpty()) return true

            // Unclosed brackets or parentheses
            if (trimmed.count { it == '(' } > trimmed.count { it == ')' }) return true
            if (trimmed.count { it == '[' } > trimmed.count { it == ']' }) return true

            return false
        }

        fun isNewsQuery(query: String): Boolean {
            if (isExpenseReportQuery(query)) return false
            if (isSavingsReportQuery(query)) return false
            if (isMarketPriceQuery(query)) return false
            val q = query.lowercase(Locale.US).trim()

            // Exclude explicit expense logging (e.g. "spent 5000 on newspaper", "သတင်းစာ 5000 ဖိုး ဝယ်တယ်")
            if (Regex("(?i)(?:bought|spend|spent|paid).*\\d+").containsMatchIn(q)) return false
            if (Regex("(?:သုံး|ဝယ်ထား|ဝယ်ခဲ့|ဝယ်တယ်).*\\d+").containsMatchIn(q)) return false

            // Burmese news patterns
            val hasBurmeseNews = q.contains("သတင်း") ||
                    q.contains("သတင်းများ") ||
                    q.contains("သတင်းအကျဉ်းချုပ်") ||
                    q.contains("သတင်းတွေ")
            if (hasBurmeseNews) return true

            // English news patterns
            val newsPatterns = listOf(
                "news", "recap", "headline", "headlines", "market update", "crypto update",
                "stock update", "daily update", "morning update"
            )
            return newsPatterns.any { q.contains(it) }
        }

        fun isNewsResponseIncomplete(content: String): Boolean {
            val trimmed = content.trim()
            if (trimmed.length < 80) return true

            val lower = trimmed.lowercase(Locale.US)
            val refusalPatterns = listOf(
                "unavailable",
                "don't have access",
                "do not have access",
                "not available",
                "cannot access",
                "can't access",
                "unable to access",
                "no access to",
                "no real-time",
                "don't have real-time",
                "do not have real-time",
                "what would you prefer",
                "မရရှိနိုင်ပါ",
                "မရနိုင်ပါ",
                "မသိရှိပါ",
                "လက်လှမ်းမမီပါ",
                "မသိရသေးပါ",
                "မရှိသေးပါ"
            )
            if (refusalPatterns.any { lower.contains(it) }) return true

            // Trailing cutoffs
            if (trimmed.endsWith(",") ||
                trimmed.endsWith(":") ||
                trimmed.endsWith("•") ||
                trimmed.endsWith("-") ||
                trimmed.endsWith("**") ||
                trimmed.endsWith("/") ||
                trimmed.endsWith("(") ||
                trimmed.endsWith("[") ||
                trimmed.endsWith("=") ||
                trimmed.endsWith("+") ||
                trimmed.endsWith("*")
            ) return true

            // If response is ONLY a markdown header or only header lines with no body text
            if (Regex("^#{1,6}\\s+[^\\n]+$").matches(trimmed)) return true
            val substantiveLines = trimmed.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
            if (substantiveLines.isEmpty()) return true

            // Unclosed brackets or parentheses
            if (trimmed.count { it == '(' } > trimmed.count { it == ')' }) return true
            if (trimmed.count { it == '[' } > trimmed.count { it == ']' }) return true

            return false
        }

        fun buildNewsRecapResponse(
            query: String,
            language: String,
            newsList: List<FinnhubNewsResponse>
        ): String {
            val q = query.lowercase(Locale.US)
            val isCrypto = q.contains("crypto") || q.contains("bitcoin") || q.contains("btc") ||
                    q.contains("sol") || q.contains("eth") || q.contains("ခရစ်ပတို") || q.contains("ဘစ်ကွိုင်")
            val isStock = q.contains("stock") || q.contains("share") || q.contains("etf") ||
                    q.contains("စတော့") || q.contains("ရှယ်ယာ")

            return if (language == "my") {
                buildBurmeseNewsRecap(newsList, isCrypto, isStock)
            } else {
                buildEnglishNewsRecap(newsList, isCrypto, isStock)
            }
        }

        private fun buildEnglishNewsRecap(
            newsList: List<FinnhubNewsResponse>,
            isCrypto: Boolean,
            isStock: Boolean
        ): String {
            val title = when {
                isCrypto -> "📰 Today's Crypto & Market News Recap"
                isStock -> "📰 Today's Stock & Market News Recap"
                else -> "📰 Today's Financial Market News Recap"
            }

            if (newsList.isEmpty()) {
                val focusText = if (isCrypto) "Crypto & Digital Assets" else "Financial Markets"
                return """
                $title

                Currently, live news feeds are momentarily refreshing. Here is your smart market overview:

                📌 Key $focusText Dynamics:
                • Market Sentiment: Macro indicators, central bank policies, and liquidity continue to steer overall volatility.
                • Risk Management: Speculative swings require disciplined position sizing rather than reactionary trading.

                💡 Coach's Smart Strategy:
                • Avoid FOMO (Fear of Missing Out) during green spikes and stay calm during dips.
                • Protect your daily living budget and emergency savings before allocating to volatile assets.
                • Utilize Dollar-Cost Averaging (DCA) to smooth out entry prices over time.
                """.trimIndent()
            }

            val headlinesBlock = newsList.take(4).mapIndexed { idx, item ->
                val src = item.source.ifBlank { "Market News" }
                val summaryText = if (item.summary.isNotBlank() && item.summary != item.headline) {
                    val cleanSummary = item.summary.replace(Regex("<[^>]*>"), "").trim()
                    val shortSummary = if (cleanSummary.length > 160) cleanSummary.take(157) + "..." else cleanSummary
                    "\n   • $shortSummary"
                } else ""
                "${idx + 1}. **${item.headline.trim()}** ($src)$summaryText"
            }.joinToString("\n\n")

            val takeaways = when {
                isCrypto -> """
                • Volatility & Momentum: High market sensitivity to regulatory developments, institutional ETF flows, and macroeconomic liquidity.
                • Investor Caution: Rapid short-term rallies often experience sharp pullbacks; manage leverage and exposure prudently.
                """.trimIndent()
                isStock -> """
                • Earnings & Macro Signals: Markets remain focused on corporate earnings, inflation reports, and interest rate guidance.
                • Sector Rotations: Defensive sectors and quality large-caps offer balance against cyclical market fluctuations.
                """.trimIndent()
                else -> """
                • Macro Overview: Global financial markets are reacting to ongoing economic indicators and monetary policy directions.
                • Diversification: Asset diversification across cash, commodities, and equities remains the strongest defense against sudden market shifts.
                """.trimIndent()
            }

            val coachStrategy = when {
                isCrypto -> """
                • Stick to Dollar-Cost Averaging (DCA) rather than trying to time unpredictable news headlines.
                • Never invest money allocated for essential expenses or short-term emergency funds into volatile crypto.
                • Take profits systematically when goals are reached instead of letting paper gains evaporate.
                """.trimIndent()
                else -> """
                • Focus on long-term wealth building rather than reacting to day-to-day headlines.
                • Keep 3–6 months of living expenses safely in an emergency fund before making risky capital commitments.
                • Review your monthly savings rate to ensure investments are funded from surplus cash flow.
                """.trimIndent()
            }

            return """
            $title

            🔥 Top Headlines & Key Updates:
            $headlinesBlock

            📊 Market Takeaways:
            $takeaways

            💡 Coach Perspective & Smart Strategy:
            $coachStrategy
            """.trimIndent()
        }

        private fun buildBurmeseNewsRecap(
            newsList: List<FinnhubNewsResponse>,
            isCrypto: Boolean,
            isStock: Boolean
        ): String {
            val title = when {
                isCrypto -> "📰 ယနေ့ ခရစ်ပတိုနှင့် စျေးကွက်သတင်း အကျဉ်းချုပ်"
                isStock -> "📰 ယနေ့ စတော့နှင့် စျေးကွက်သတင်း အကျဉ်းချုပ်"
                else -> "📰 ယနေ့ ဘဏ္ဍာရေးနှင့် စျေးကွက်သတင်း အကျဉ်းချုပ်"
            }

            if (newsList.isEmpty()) {
                val marketType = if (isCrypto) "ခရစ်ပတို (Crypto)" else "ဘဏ္ဍာရေးစျေးကွက်"
                return """
                $title

                လတ်တလော အင်တာနက်ချိတ်ဆက်မှုကြောင့် တိုက်ရိုက်သတင်းများကို ရယူ၍မရသေးပါ။ သို့သော် သိထားသင့်သည့် အဓိက အချက်များမှာ-

                📌 $marketType ၏ အဓိက သဘောတရားများ:
                • စျေးကွက်များသည် သတင်းများ၊ အတိုးနှုန်းမူဝါဒများနှင့် ကမ္ဘာ့စီးပွားရေးအခြေအနေများအပေါ် မူတည်၍ အတက်အကျ ကြမ်းလေ့ရှိပါတယ်။
                • ရေတိုသတင်းများနောက် လိုက်ပါပြီး အလောတကြီး ရင်းနှီးမြှုပ်နှံခြင်းကို သတိပြုသင့်ပါတယ်။

                💡 ငွေကြေးအကြံပေး (Coach) ၏ အကြံပြုချက်:
                • အရေးပေါ်ရန်ပုံငွေနှင့် လစဉ် မရှိမဖြစ် ကုန်ကျစရိတ်များကို အရင်ဆုံး ခိုင်မာအောင် စုဆောင်းပါ။
                • စျေးကွက်မငြိမ်သက်မှုကို လျှော့ချနိုင်ရန် တစ်ကြိမ်တည်း အကုန်မထည့်ဘဲ ပုံမှန်ခွဲထည့်သည့်နည်း (DCA) ဖြင့် စနစ်တကျ စီမံပါ။
                • မဆုံးရှုံးနိုင်သော မရှိမဖြစ်ငွေများကို စွန့်စားရမှုများသော နေရာများတွင် မထည့်ဝင်ပါနှင့်။
                """.trimIndent()
            }

            val headlinesBlock = newsList.take(4).mapIndexed { idx, item ->
                val src = item.source.ifBlank { "Market News" }
                val summaryText = if (item.summary.isNotBlank() && item.summary != item.headline) {
                    val cleanSummary = item.summary.replace(Regex("<[^>]*>"), "").trim()
                    val shortSummary = if (cleanSummary.length > 160) cleanSummary.take(157) + "..." else cleanSummary
                    "\n   • $shortSummary"
                } else ""
                "${idx + 1}. **${item.headline.trim()}** ($src)$summaryText"
            }.joinToString("\n\n")

            val takeaways = when {
                isCrypto -> """
                • စျေးကွက်မတည်ငြိမ်မှု: အဖွဲ့အစည်းကြီးများ၏ ရင်းနှီးမြှုပ်နှံမှု၊ အစိုးရစည်းမျဉ်းများနှင့် ကမ္ဘာ့ငွေကြေးမူဝါဒများကြောင့် စျေးနှုန်းများ အတက်အကျ မြန်ဆန်နေပါတယ်။
                • သတိပြုရန်အချက်: ရေတိုစျေးတက်ခြင်းများတွင် အလောတကြီး ဝယ်ယူခြင်း (FOMO) ကို ရှောင်ရှားပြီး စျေးကွက်ပြန်လည်ကျဆင်းနိုင်ခြေကို အမြဲထည့်သွင်း စဉ်းစားသင့်ပါတယ်။
                """.trimIndent()
                isStock -> """
                • စီးပွားရေးအညွှန်းကိန်းများ: ကုမ္ပဏီများ၏ အမြတ်ငွေစာရင်းများ၊ အတိုးနှုန်းနှုန်းထားများနှင့် စျေးကွက်အခြေအနေများအပေါ် ရင်းနှီးမြှုပ်နှံသူများ အထူးအာရုံစိုက်နေကြပါတယ်။
                • မျှတစွာခွဲဝေမှု: စွန့်စားမှုနည်းပါးသော အစုရှယ်ယာများနှင့် ဟန်ချက်ညီအောင် ထိန်းညှိထားခြင်းက စျေးကွက်အတက်အကျဒဏ်ကို သက်သာစေပါတယ်။
                """.trimIndent()
                else -> """
                • စျေးကွက်သုံးသပ်ချက်: ကမ္ဘာ့ဘဏ္ဍာရေးစျေးကွက်များသည် ငွေကြေးဖောင်းပွမှုနှင့် ဗဟိုဘဏ်များ၏ မူဝါဒများအရ လှုပ်ရှားနေပါတယ်။
                • ဖြန့်ကြက်ရင်းနှီးမြှုပ်နှံခြင်း: ပိုင်ဆိုင်မှုများကို တစ်ခုတည်းတွင် မစုစည်းဘဲ ခွဲဝေရင်းနှီးမြှုပ်နှံထားခြင်းက အကောင်းဆုံး နည်းလမ်းဖြစ်ပါတယ်။
                """.trimIndent()
            }

            val coachStrategy = when {
                isCrypto -> """
                • သတင်းခေါင်းစဉ်များကြောင့် စိတ်လှုပ်ရှားပြီး ရင်းနှီးမြှုပ်နှံမည့်အစား ပုံမှန်ပမာဏတစ်ခုစီ ခွဲထည့်သည့် Dollar-Cost Averaging (DCA) စနစ်ကို ကျင့်သုံးပါ။
                • နေ့စဉ်စားဝတ်နေရေးနှင့် အရေးပေါ်ငွေများကို ထိခိုက်စေမည့် စွန့်စားမှုမျိုး ဘယ်တော့မှ မလုပ်ပါနှင့်။
                • သတ်မှတ်ထားသော ပန်းတိုင်ရောက်ပါက သင့်လျော်သော အမြတ်ငွေကို ထုတ်ယူစုဆောင်းဖို့ မမေ့ပါနှင့်။
                """.trimIndent()
                else -> """
                • နေ့စဉ်သတင်းများထက် မိမိ၏ ရေရှည်ဘဏ္ဍာရေးပန်းတိုင်ကို အဓိကထား အာရုံစိုက်ပါ။
                • မည်သည့်ရင်းနှီးမြှုပ်နှံမှုမဆို မစတင်မီ အနည်းဆုံး ၃ လမှ ၆ လစာ အရေးပေါ်ရန်ပုံငွေကို သီးသန့်ဖယ်ထားပါ။
                • လစဉ် ပိုလျှံငွေထဲမှသာ စနစ်တကျ ခွဲဝေရင်းနှီးမြှုပ်နှံပါ။
                """.trimIndent()
            }

            return """
            $title

            🔥 အဓိက သတင်းခေါင်းစဉ်များနှင့် အကျဉ်းချုပ်:
            $headlinesBlock

            📊 စျေးကွက် သုံးသပ်ချက်:
            $takeaways

            💡 ငွေကြေးအကြံပေး (Coach) ၏ အကြံပြုချက်:
            $coachStrategy
            """.trimIndent()
        }

        fun buildMarketPriceResponse(
            query: String,
            language: String,
            isGold: Boolean,
            isCrypto: Boolean,
            holdings: List<UserHolding>,
            goldSpotPrice: Double = 4476.60,
            goldSpotChange: Double = -1.39,
            gldPrice: Double = 406.77,
            gldChange: Double = -0.84,
            btcPrice: Double = 79980.0,
            btcChange: Double = 0.5
        ): String {
            return if (language == "my") {
                buildBurmeseMarketPriceResponse(
                    isGold = isGold,
                    isCrypto = isCrypto,
                    holdings = holdings,
                    goldSpotPrice = goldSpotPrice,
                    goldSpotChange = goldSpotChange,
                    gldPrice = gldPrice,
                    gldChange = gldChange,
                    btcPrice = btcPrice,
                    btcChange = btcChange
                )
            } else {
                buildEnglishMarketPriceResponse(
                    isGold = isGold,
                    isCrypto = isCrypto,
                    holdings = holdings,
                    goldSpotPrice = goldSpotPrice,
                    goldSpotChange = goldSpotChange,
                    gldPrice = gldPrice,
                    gldChange = gldChange,
                    btcPrice = btcPrice,
                    btcChange = btcChange
                )
            }
        }

        private fun buildEnglishMarketPriceResponse(
            isGold: Boolean,
            isCrypto: Boolean,
            holdings: List<UserHolding>,
            goldSpotPrice: Double,
            goldSpotChange: Double,
            gldPrice: Double,
            gldChange: Double,
            btcPrice: Double,
            btcChange: Double
        ): String {
            if (isGold) {
                val formattedSpot = String.format(Locale.US, "%,.2f", goldSpotPrice)
                val formattedSpotChg = String.format(Locale.US, "%+.2f", goldSpotChange)
                val formattedGld = String.format(Locale.US, "%,.2f", gldPrice)
                val formattedGldChg = String.format(Locale.US, "%+.2f", gldChange)

                val gldHolding = holdings.find {
                    it.symbol.equals("GLD", ignoreCase = true) ||
                    it.type.equals("commodity", ignoreCase = true) ||
                    it.name.contains("Gold", ignoreCase = true)
                }
                val portfolioSection = if (gldHolding != null) {
                    val units = gldHolding.units
                    val buyPrice = gldHolding.buyPrice
                    val currentValue = units * gldPrice
                    val pnl = currentValue - (units * buyPrice)
                    val pnlPercent = if (buyPrice > 0) ((gldPrice - buyPrice) / buyPrice) * 100.0 else 0.0
                    val pnlSign = if (pnl >= 0) "+" else ""
                    val ticker = gldHolding.displayTicker.ifBlank { gldHolding.symbol }
                    """
                    📦 Your Portfolio Position:
                    • Holding: $units unit of $ticker (Cost basis: $${String.format(Locale.US, "%.2f", buyPrice)} USD)
                    • Current Estimated Value: $${String.format(Locale.US, "%.2f", currentValue)} USD ($pnlSign${String.format(Locale.US, "%.2f", pnlPercent)}% / $pnlSign$${String.format(Locale.US, "%.2f", pnl)} USD)
                    """.trimIndent()
                } else ""

                return """
                🟡 Gold Market Price & Portfolio Overview

                💰 Current Market Quotes:
                • International Spot Gold (XAU / GC): ~$$formattedSpot USD / troy oz ($formattedSpotChg% 24h)
                • SPDR Gold Shares (GLD ETF): ~$$formattedGld USD / share ($formattedGldChg% 24h)
                • Myanmar Domestic Gold (Academy 24K): ~7,000,000 – 7,500,000 MMK / kyat-tha (16.33 grams)
                ${if (portfolioSection.isNotBlank()) "\n$portfolioSection\n" else ""}
                📊 Market Dynamics:
                • Gold serves as a classic defensive safe-haven asset, store of value, and hedge against inflation and currency depreciation.

                💡 Coach's Smart Strategy:
                • 5%–10% Allocation Rule: Keep precious metals to a measured portion of your total wealth.
                • Emergency Fund First: Ensure you have 3–6 months of liquid cash reserves before allocating capital to metals.
                • DCA Accumulation: Use Dollar-Cost Averaging rather than trying to time daily price highs and lows.

                (Note: Market prices fluctuate in real time. This information is for educational guidance and portfolio tracking.)
                """.trimIndent()
            } else if (isCrypto) {
                val formattedBtc = String.format(Locale.US, "%,.2f", btcPrice)
                val formattedBtcChg = String.format(Locale.US, "%+.2f", btcChange)

                val btcHolding = holdings.find {
                    it.symbol.equals("BTC", ignoreCase = true) ||
                    it.symbol.equals("BITCOIN", ignoreCase = true) ||
                    it.type.equals("crypto", ignoreCase = true)
                }
                val cryptoPortfolioSection = if (btcHolding != null) {
                    val units = btcHolding.units
                    val buyPrice = btcHolding.buyPrice
                    val currentValue = units * btcPrice
                    val pnl = currentValue - (units * buyPrice)
                    val pnlPercent = if (buyPrice > 0) ((btcPrice - buyPrice) / buyPrice) * 100.0 else 0.0
                    val pnlSign = if (pnl >= 0) "+" else ""
                    val ticker = btcHolding.displayTicker.ifBlank { btcHolding.symbol }
                    """
                    📦 Your Portfolio Position:
                    • Holding: $units unit of $ticker (Cost basis: $${String.format(Locale.US, "%.2f", buyPrice)} USD)
                    • Current Estimated Value: $${String.format(Locale.US, "%.2f", currentValue)} USD ($pnlSign${String.format(Locale.US, "%.2f", pnlPercent)}% / $pnlSign$${String.format(Locale.US, "%.2f", pnl)} USD)
                    """.trimIndent()
                } else ""

                return """
                🪙 Crypto Market Price & Portfolio Overview

                💰 Current Market Quotes:
                • Bitcoin (BTC): ~$$formattedBtc USD ($formattedBtcChg% 24h)
                ${if (cryptoPortfolioSection.isNotBlank()) "\n$cryptoPortfolioSection\n" else ""}
                📊 Market Dynamics:
                • Crypto assets experience significant price fluctuations driven by liquidity, ETF flows, and macroeconomic trends.

                💡 Coach's Smart Strategy:
                • 1%–5% Allocation Limit: Limit high-volatility speculative assets so price swings never endanger your living expenses.
                • Core Savings First: Protect your monthly budget and emergency cash cushion first.
                • Dollar-Cost Averaging (DCA): Accumulate on a fixed schedule to avoid emotional FOMO at peaks.

                (Note: Crypto prices fluctuate 24/7. This information is for educational guidance.)
                """.trimIndent()
            } else {
                return """
                📈 Financial Market Prices & Overview

                💰 Current Market Quotes:
                • Spot Gold (GC): ~$${String.format(Locale.US, "%,.2f", goldSpotPrice)} USD / oz
                • SPDR Gold Shares (GLD): ~$${String.format(Locale.US, "%,.2f", gldPrice)} USD
                • Bitcoin (BTC): ~$${String.format(Locale.US, "%,.2f", btcPrice)} USD

                💡 Coach Perspective:
                • Long-term consistency and disciplined asset allocation outperform chasing individual market moves.
                • Prioritize your emergency savings buffer before expanding speculative positions.
                """.trimIndent()
            }
        }

        private fun buildBurmeseMarketPriceResponse(
            isGold: Boolean,
            isCrypto: Boolean,
            holdings: List<UserHolding>,
            goldSpotPrice: Double,
            goldSpotChange: Double,
            gldPrice: Double,
            gldChange: Double,
            btcPrice: Double,
            btcChange: Double
        ): String {
            if (isGold) {
                val formattedSpot = String.format(Locale.US, "%,.2f", goldSpotPrice)
                val formattedSpotChg = String.format(Locale.US, "%+.2f", goldSpotChange)
                val formattedGld = String.format(Locale.US, "%,.2f", gldPrice)
                val formattedGldChg = String.format(Locale.US, "%+.2f", gldChange)

                val gldHolding = holdings.find {
                    it.symbol.equals("GLD", ignoreCase = true) ||
                    it.type.equals("commodity", ignoreCase = true) ||
                    it.name.contains("Gold", ignoreCase = true)
                }
                val myPortfolioSection = if (gldHolding != null) {
                    val units = gldHolding.units
                    val buyPrice = gldHolding.buyPrice
                    val currentValue = units * gldPrice
                    val pnl = currentValue - (units * buyPrice)
                    val pnlPercent = if (buyPrice > 0) ((gldPrice - buyPrice) / buyPrice) * 100.0 else 0.0
                    val pnlSign = if (pnl >= 0) "+" else ""
                    val ticker = gldHolding.displayTicker.ifBlank { gldHolding.symbol }
                    """
                    📦 သင့်၏ ရင်းနှီးမြှုပ်နှံမှု အခြေအနေ:
                    • ပိုင်ဆိုင်မှု: $ticker $units ယူနစ် (ဝယ်ယူစျေး: $${String.format(Locale.US, "%.2f", buyPrice)} USD)
                    • လက်ရှိခန့်မှန်းတန်ဖိုး: $${String.format(Locale.US, "%.2f", currentValue)} USD ($pnlSign${String.format(Locale.US, "%.2f", pnlPercent)}% အမြတ်/အရှုံး)
                    """.trimIndent()
                } else ""

                return """
                🟡 ရွှေစျေးကွက်ပေါက်စျေးနှင့် ပိုင်ဆိုင်မှု သုံးသပ်ချက်

                💰 လက်ရှိ စျေးကွက်ပေါက်စျေးများ:
                • နိုင်ငံတကာ ရွှေစျေး (Spot Gold / GC): ~$$formattedSpot USD / အောင်စ ($formattedSpotChg% 24h)
                • SPDR Gold Shares (GLD ETF): ~$$formattedGld USD / ရှယ်ယာ ($formattedGldChg% 24h)
                • မြန်မာ့ရွှေစျေး (အကယ်ဒမီ ၂၄ ပဲရည် မီးလင်းရွှေ): ကျပ် ၇,၀၀၀,၀၀၀ – ၇,၅၀၀,၀၀၀ ဝန်းကျင် / ကျပ်သား (၁၆.၃၃ ဂရမ်)
                ${if (myPortfolioSection.isNotBlank()) "\n$myPortfolioSection\n" else ""}
                📊 စျေးကွက်သုံးသပ်ချက်:
                • ရွှေသည် ငွေကြေးဖောင်းပွမှုနှင့် ငွေတန်ဖိုးကျဆင်းမှုကို ကာကွယ်ပေးနိုင်သော ခိုင်မာသည့် အကာအကွယ်ပိုင်ဆိုင်မှု (Hedge) ဖြစ်ပါတယ်။

                💡 ငွေကြေးအကြံပေး (Coach) ၏ အကြံပြုချက်:
                • ၅% မှ ၁၀% အချိုးအစားသာ ရင်းနှီးမြှုပ်နှံပါ: ပိုင်ဆိုင်မှုအားလုံးကို ရွှေတစ်ခုတည်းတွင် မထည့်ဝင်သင့်ပါ။
                • အရေးပေါ်သုံးငွေ (Emergency Fund) ၃ လမှ ၆ လစာ အရင်ဆုံး ဖယ်ထားပါ။
                • နေ့စဉ်စျေးအတက်အကျထက် Dollar-Cost Averaging (DCA) နည်းလမ်းဖြင့် ရေရှည်အတွက် စနစ်တကျ စုဆောင်းပါ။

                (မှတ်ချက် - စျေးကွက်ပေါက်စျေးများသည် အချိန်နှင့်အမျှ ပြောင်းလဲနိုင်ပြီး လေ့လာသင်ယူရန်အတွက်သာ ဖြစ်ပါတယ်။)
                """.trimIndent()
            } else if (isCrypto) {
                val formattedBtc = String.format(Locale.US, "%,.2f", btcPrice)
                val formattedBtcChg = String.format(Locale.US, "%+.2f", btcChange)

                val btcHolding = holdings.find {
                    it.symbol.equals("BTC", ignoreCase = true) ||
                    it.symbol.equals("BITCOIN", ignoreCase = true) ||
                    it.type.equals("crypto", ignoreCase = true)
                }
                val myCryptoSection = if (btcHolding != null) {
                    val units = btcHolding.units
                    val buyPrice = btcHolding.buyPrice
                    val currentValue = units * btcPrice
                    val pnl = currentValue - (units * buyPrice)
                    val pnlPercent = if (buyPrice > 0) ((btcPrice - buyPrice) / buyPrice) * 100.0 else 0.0
                    val pnlSign = if (pnl >= 0) "+" else ""
                    val ticker = btcHolding.displayTicker.ifBlank { btcHolding.symbol }
                    """
                    📦 သင့်၏ ရင်းနှီးမြှုပ်နှံမှု အခြေအနေ:
                    • ပိုင်ဆိုင်မှု: $ticker $units ယူနစ် (ဝယ်ယူစျေး: $${String.format(Locale.US, "%.2f", buyPrice)} USD)
                    • လက်ရှိခန့်မှန်းတန်ဖိုး: $${String.format(Locale.US, "%.2f", currentValue)} USD ($pnlSign${String.format(Locale.US, "%.2f", pnlPercent)}% အမြတ်/အရှုံး)
                    """.trimIndent()
                } else ""

                return """
                🪙 ခရစ်ပတို စျေးကွက်ပေါက်စျေးနှင့် သုံးသပ်ချက်

                💰 လက်ရှိ စျေးကွက်ပေါက်စျေးများ:
                • ဘစ်ကွိုင် (Bitcoin - BTC): ~$$formattedBtc USD ($formattedBtcChg% 24h)
                ${if (myCryptoSection.isNotBlank()) "\n$myCryptoSection\n" else ""}
                📊 စျေးကွက်သုံးသပ်ချက်:
                • ခရစ်ပတိုစျေးကွက်သည် စျေးအတက်အကျ အလွန်မြန်ဆန်သောကြောင့် သတိထား စောင့်ကြည့်သင့်ပါတယ်။

                💡 ငွေကြေးအကြံပေး (Coach) ၏ အကြံပြုချက်:
                • ၁% မှ ၅% ထက်ပို၍ မစွန့်စားပါနှင့်: မဆုံးရှုံးနိုင်သော မရှိမဖြစ်ငွေများကို ခရစ်ပတိုထဲ မထည့်ပါနှင့်။
                • အရေးပေါ်သုံးငွေနှင့် လစဉ်ဘတ်ဂျက်ကို အရင်ဆုံး ကာကွယ်ပါ။
                • Dollar-Cost Averaging (DCA) နည်းလမ်းဖြင့် ပုံမှန်ခွဲထည့်ပါ။

                (မှတ်ချက် - ခရစ်ပတိုစျေးနှုန်းများသည် အချိန်ပြည့် ပြောင်းလဲနေပြီး လေ့လာသင်ယူရန်အတွက်သာ ဖြစ်ပါတယ်။)
                """.trimIndent()
            } else {
                return """
                📈 ဘဏ္ဍာရေး စျေးကွက်ပေါက်စျေးများ

                💰 လက်ရှိ ပေါက်စျေးများ:
                • ရွှေ (Spot Gold / GC): ~$${String.format(Locale.US, "%,.2f", goldSpotPrice)} USD / အောင်စ
                • SPDR Gold Shares (GLD): ~$${String.format(Locale.US, "%,.2f", gldPrice)} USD
                • ဘစ်ကွိုင် (BTC): ~$${String.format(Locale.US, "%,.2f", btcPrice)} USD

                💡 ငွေကြေးအကြံပေး ၏ အကြံပြုချက်:
                • ရေရှည်တည်ငြိမ်မှုနှင့် စနစ်တကျ ရင်းနှီးမြှုပ်နှံမှုသည် နေ့စဉ်စျေးလိုက်ကြည့်ခြင်းထက် ပိုမိုထိရောက်ပါတယ်။
                """.trimIndent()
            }
        }

        fun buildInvestmentAdviceResponse(
            userId: String,
            query: String,
            language: String,
            existingHoldingSummary: String = "",
            remainingBudgetMmk: Double = 0.0,
            btcPrice: Double? = null,
            btcChange: Double? = null,
            goldPrice: Double? = null,
            goldChange: Double? = null,
            totalSpentMmk: Double = 0.0,
            budgetLimitMmk: Double = 0.0,
            daysLeft: Int = 0,
            cryptoSymbol: String = "BTC",
            cryptoPrice: Double? = null,
            cryptoChange: Double? = null,
            solPrice: Double? = null,
            solChange: Double? = null
        ): String {
            val q = query.lowercase(Locale.US).trim()
            val isGold = q.contains("gold") || q.contains("ရွှေ") || q.contains("gld") || q.contains("xau")
            val isSol = q.contains("sol") || q.contains("solana") || q.contains("ဆိုလာနာ")
            val isEth = q.contains("eth") || q.contains("ethereum")
            val isBtc = q.contains("btc") || q.contains("bitcoin") || q.contains("ဘစ်ကွိုင်")
            val isBtcVsSol = (isBtc && isSol) || q.contains("btc vs sol") || q.contains("sol vs btc") ||
                    q.contains("btc နဲ့ sol") || q.contains("sol နဲ့ btc") || q.contains("btc နှင့် sol") || q.contains("sol နှင့် btc")
            val isTesla = q.contains("tesla") || q.contains("tsla") || q.contains("တက်စလာ")
            val isCrypto = q.contains("crypto") || q.contains("ခရစ်ပတို") || isSol || isEth || isBtc || isBtcVsSol
            val isStock = isTesla || q.contains("stock") || q.contains("stocks") || q.contains("share") ||
                    q.contains("shares") || q.contains("equity") || q.contains("etf") || q.contains("စတော့") || q.contains("ရှယ်ယာ") ||
                    q.contains("apple") || q.contains("aapl") || q.contains("nvidia") || q.contains("nvda")

            val effectiveCryptoSymbol = when {
                cryptoSymbol != "BTC" -> cryptoSymbol
                isSol && !isBtcVsSol -> "SOL"
                isEth -> "ETH"
                else -> "BTC"
            }
            val effectiveCryptoPrice = cryptoPrice ?: btcPrice
            val effectiveCryptoChange = cryptoChange ?: btcChange

            return if (language == "my") {
                when {
                    isBtcVsSol -> buildBurmeseBtcVsSolAdvice(existingHoldingSummary, remainingBudgetMmk, btcPrice, btcChange, solPrice ?: cryptoPrice, solChange ?: cryptoChange, totalSpentMmk, budgetLimitMmk, daysLeft)
                    isTesla -> buildBurmeseTeslaAdvice(existingHoldingSummary, remainingBudgetMmk, totalSpentMmk, budgetLimitMmk, daysLeft)
                    isGold -> buildBurmeseGoldAdvice(existingHoldingSummary, remainingBudgetMmk, goldPrice, goldChange, totalSpentMmk, budgetLimitMmk, daysLeft)
                    isCrypto -> buildBurmeseCryptoAdvice(existingHoldingSummary, remainingBudgetMmk, totalSpentMmk, budgetLimitMmk, daysLeft, effectiveCryptoSymbol, effectiveCryptoPrice, effectiveCryptoChange)
                    isStock -> buildBurmeseStockAdvice(existingHoldingSummary)
                    else -> buildBurmeseGeneralInvestmentAdvice(existingHoldingSummary, remainingBudgetMmk)
                }
            } else {
                when {
                    isBtcVsSol -> buildEnglishBtcVsSolAdvice(existingHoldingSummary, remainingBudgetMmk, btcPrice, btcChange, solPrice ?: cryptoPrice, solChange ?: cryptoChange, totalSpentMmk, budgetLimitMmk, daysLeft)
                    isTesla -> buildEnglishTeslaAdvice(existingHoldingSummary, remainingBudgetMmk, totalSpentMmk, budgetLimitMmk, daysLeft)
                    isGold -> buildEnglishGoldAdvice(existingHoldingSummary, remainingBudgetMmk, goldPrice, goldChange, totalSpentMmk, budgetLimitMmk, daysLeft)
                    isCrypto -> buildEnglishCryptoAdvice(existingHoldingSummary, remainingBudgetMmk, totalSpentMmk, budgetLimitMmk, daysLeft, effectiveCryptoSymbol, effectiveCryptoPrice, effectiveCryptoChange)
                    isStock -> buildEnglishStockAdvice(existingHoldingSummary)
                    else -> buildEnglishGeneralInvestmentAdvice(existingHoldingSummary, remainingBudgetMmk)
                }
            }
        }

        private fun buildEnglishGoldAdvice(
            holdingSummary: String,
            remainingBudget: Double,
            goldPrice: Double? = null,
            goldChange: Double? = null,
            totalSpent: Double = 0.0,
            budgetLimit: Double = 0.0,
            daysLeft: Int = 0
        ): String {
            val hasBudgetData = budgetLimit > 0.0 || totalSpent > 0.0 || remainingBudget != 0.0
            val budgetBlock = if (hasBudgetData) {
                val spentFormatted = String.format(Locale.US, "%,.0f", totalSpent)
                val limitFormatted = String.format(Locale.US, "%,.0f", budgetLimit)
                val remFormatted = String.format(Locale.US, "%,.0f", remainingBudget)
                val daysStr = if (daysLeft > 0) " ($daysLeft days remaining)" else ""
                "\n📊 Your Monthly Spending & Budget Status:\n• Total Spent: $spentFormatted MMK\n• Monthly Budget: $limitFormatted MMK\n• Remaining Budget: $remFormatted MMK$daysStr\n"
            } else ""

            val holdingNote = if (holdingSummary.isNotBlank()) "\n• Your Portfolio: $holdingSummary\n" else ""
            val priceNote = if (goldPrice != null && goldPrice > 0) {
                val formattedGold = String.format(Locale.US, "%,.2f", goldPrice)
                val chgStr = if (goldChange != null) String.format(Locale.US, " (%+.2f%% 24h)", goldChange) else ""
                "\n• Current Gold Benchmark: Spot Gold ~$$formattedGold USD / oz$chgStr | Myanmar Domestic Gold ~7,000,000 – 7,500,000 MMK / kyat-tha\n"
            } else ""
            val budgetNote = when {
                remainingBudget <= 0 && hasBudgetData -> {
                    "\n• Budget Awareness: Caution! Your monthly budget is currently exhausted or at its limit. Prioritize your daily living cushion first before purchasing commodities.\n"
                }
                remainingBudget > 0 -> {
                    val formatted = String.format(Locale.US, "%,.0f", remainingBudget)
                    "\n• Budget Awareness: You currently have $formatted MMK remaining this month. Protect your daily living buffer first before allocating to commodities.\n"
                }
                else -> ""
            }

            return """
                🟡 Gold as an Investment: Coach's Perspective
                $budgetBlock
                Gold serves as a classic defensive asset, store of value, and hedge against inflation and currency depreciation. If you're asking whether you should buy gold today, here is a structured coaching approach:

                1. Secure Your Emergency Fund First:
                Precious metals are not an emergency cash cushion. Before allocating funds to gold, ensure you have 3 to 6 months of liquid living expenses saved in cash or liquid savings.

                2. Recommended Allocation (5% – 10%):
                Gold should complement your portfolio, not dominate it. Most financial coaches recommend holding 5% to 10% of your total wealth in precious metals to protect purchasing power without sacrificing overall liquidity.

                3. Buying Strategy — Dollar-Cost Averaging (DCA):
                Avoid trying to time daily price spikes. Instead of making a single large purchase, buy in small, consistent increments over time. This smooths out short-term price fluctuations.

                4. Physical Gold vs. Paper Gold / ETFs:
                • Physical Gold: Direct ownership (e.g., Academy gold bars/coins in Myanmar), but requires safe storage and carries dealer buy/sell spreads.
                • Gold ETFs (e.g., GLD): Liquid and trades like a stock without physical storage hassles.
                $priceNote$holdingNote$budgetNote
                (Note: This is educational guidance to support your financial planning, not professional financial advice.)

                Would you like to set up a monthly saving target or challenge to steadily accumulate your gold reserves?
            """.trimIndent()
        }

        private fun buildBurmeseGoldAdvice(
            holdingSummary: String,
            remainingBudget: Double,
            goldPrice: Double? = null,
            goldChange: Double? = null,
            totalSpent: Double = 0.0,
            budgetLimit: Double = 0.0,
            daysLeft: Int = 0
        ): String {
            val hasBudgetData = budgetLimit > 0.0 || totalSpent > 0.0 || remainingBudget != 0.0
            val budgetBlock = if (hasBudgetData) {
                val spentFormatted = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(String.format(Locale.US, "%,.0f", totalSpent))
                val limitFormatted = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(String.format(Locale.US, "%,.0f", budgetLimit))
                val remFormatted = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(String.format(Locale.US, "%,.0f", remainingBudget))
                val daysStr = if (daysLeft > 0) " (${com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(daysLeft.toString())} ရက် ကျန်ရှိ)" else ""
                "\n📊 ယခုလ သင့်၏ အသုံးစရိတ်နှင့် ဘတ်ဂျက် အခြေအနေ:\n• သုံးစွဲပြီးငွေ: $spentFormatted ကျပ်\n• လစဉ်ဘတ်ဂျက်: $limitFormatted ကျပ်\n• လက်ကျန်ငွေ: $remFormatted ကျပ်$daysStr\n"
            } else ""

            val holdingNote = if (holdingSummary.isNotBlank()) "\n• သင့်လက်ရှိ ပိုင်ဆိုင်မှု: $holdingSummary\n" else ""
            val priceNote = if (goldPrice != null && goldPrice > 0) {
                val formattedGold = String.format(Locale.US, "%,.2f", goldPrice)
                val chgStr = if (goldChange != null) String.format(Locale.US, " (%+.2f%% 24h)", goldChange) else ""
                "\n• လက်ရှိ ရွှေပေါက်စျေး: Spot Gold ~$$formattedGold USD / အောင်စ$chgStr | မြန်မာ့ရွှေ (အကယ်ဒမီ ၂၄ ပဲရည်) ~ကျပ် ၇,၀၀၀,၀၀၀ – ၇,၅၀၀,၀၀၀ ဝန်းကျင် / ကျပ်သား\n"
            } else ""
            val budgetNote = when {
                remainingBudget <= 0 && hasBudgetData -> {
                    "\n• လစဉ်ဘတ်ဂျက် အခြေအနေ: သတိပြုရန်! ယခုလ ဘတ်ဂျက်ကုန်လွန်/ပြည့်လုနီးပါးဖြစ်နေပါသည်။ အခြေခံစားဝတ်နေရေးစရိတ်ကို အရင်ဆုံး ထိန်းသိမ်းရန် အကြံပြုပါသည် (ယခုလတွင် ရွှေမဝယ်သင့်သေးပါ)။\n"
                }
                remainingBudget > 0 -> {
                    val formatted = String.format(Locale.US, "%,.0f", remainingBudget)
                    val myFormatted = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formatted)
                    "\n• လစဉ်ဘတ်ဂျက် အခြေအနေ: ယခုလတွင် ကျန်ငွေ $myFormatted ကျပ် ကျန်ရှိပါသေးသည်။ နေ့စဉ်စားဝတ်နေရေး စရိတ်ကို မထိခိုက်စေဘဲ ပိုလျှံငွေဖြင့်သာ စတင်ပါ။\n"
                }
                else -> ""
            }

            return """
                🟡 ရွှေရင်းနှီးမြှုပ်နှံမှုနှင့် ပတ်သက်၍ အကြံပြုချက်
                $budgetBlock
                ရွှေ (Gold) သည် အမြတ်များများရရှိရန်ထက် ငွေကြေးဖောင်းပွမှု (Inflation) နှင့် ငွေတန်ဖိုးကျဆင်းမှုကို ကာကွယ်ပေးနိုင်သော အကာအကွယ်ပိုင်ဆိုင်မှု (Defensive Asset / Hedge) တစ်ခု ဖြစ်ပါသည်။ ရွှေဝယ်ယူရန် စဉ်းစားနေပါက အောက်ပါအချက်များကို လိုက်နာရန် အကြံပြုလိုပါတယ်-

                ၁။ အရေးပေါ်သုံးငွေ (Emergency Fund) အရင်ရှိပါစေ:
                ရွှေမဝယ်မီ မိမိ၏ အနည်းဆုံး ၃ လမှ ၆ လစာ မရှိမဖြစ် နေထိုင်စရိတ်ကို အလွယ်တကူ ထုတ်ယူနိုင်သော ငွေသား/စုငွေအဖြစ် အရင်ဆုံး သီးသန့်စုဆောင်းထားသင့်ပါသည်။

                ၂။ သင့်တင့်သော အချိုးအစား (၅% မှ ၁၀%):
                စုစုပေါင်း ပိုင်ဆိုင်မှု၏ ၅% မှ ၁၀% ခန့်သာ ရွှေတွင် ထားရှိရန် အကြံပြုပါသည်။ ဝင်ငွေ သို့မဟုတ် စုငွေအားလုံးကို ရွှေတစ်ခုတည်းတွင် မစုပြုံသင့်ပါ။

                ၃။ ပုံမှန်ခွဲဝေဝယ်ယူနည်း (Dollar-Cost Averaging - DCA):
                ဈေးအတက်အကျကို နေ့စဉ်ခန့်မှန်းပြီး တစ်ကြိမ်တည်း အများကြီးဝယ်မည့်အစား လစဉ် ပုံမှန်ငွေပမာဏတစ်ခုဖြင့် ခွဲဝေစုဆောင်းဝယ်ယူခြင်းက ဈေးအမြင့်ဆုံးအချိန်တွင် မိသွားမည့် အန္တရာယ်ကို လျှော့ချပေးနိုင်ပါတယ်။

                ၄။ ရွှေဝယ်ယူရာတွင် သတိပြုရန်:
                • အကယ်ဒမီ ရွှေတုံး/ရွှေပြား: ရေရှည်စုဆောင်းရန် အသင့်တော်ဆုံးဖြစ်ပြီး အလျော့တွက်နှင့် လက်ခ သက်သာပါသည်။
                • ရွှေထည်လက်ဝတ်ရတနာ: လက်ခနှင့် အလျော့တွက်များသောကြောင့် ရင်းနှီးမြှုပ်နှံမှု သီးသန့်အတွက် မသင့်တော်ပါ။
                $priceNote$holdingNote$budgetNote
                (မှတ်ချက် - ဤအချက်အလက်သည် ငွေကြေးစီမံခန့်ခွဲမှု လေ့လာသင်ယူရန်အတွက်သာဖြစ်ပြီး တရားဝင်ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်မဟုတ်ပါ။)

                ရွှေဝယ်ယူစုဆောင်းဖို့အတွက် လစဉ် စုငွေပန်းတိုင် (Saving Challenge) တစ်ခု သတ်မှတ်ချင်ပါသလား။
            """.trimIndent()
        }

        private fun buildEnglishCryptoAdvice(
            holdingSummary: String,
            remainingBudget: Double = 0.0,
            totalSpent: Double = 0.0,
            budgetLimit: Double = 0.0,
            daysLeft: Int = 0,
            cryptoSymbol: String = "BTC",
            cryptoPrice: Double? = null,
            cryptoChange: Double? = null
        ): String {
            val isSol = cryptoSymbol.equals("SOL", ignoreCase = true)
            val isEth = cryptoSymbol.equals("ETH", ignoreCase = true)
            val assetDisplayName = when {
                isSol -> "Solana (SOL)"
                isEth -> "Ethereum (ETH)"
                else -> "Bitcoin"
            }

            val title = if (isSol) {
                "🪙 Solana (SOL) Investment & Financial Coaching Review"
            } else if (isEth) {
                "🪙 Ethereum (ETH) Investment & Financial Coaching Review"
            } else {
                "🪙 Cryptocurrency & Bitcoin: Coach's Perspective"
            }

            val hasBudgetData = budgetLimit > 0.0 || totalSpent > 0.0 || remainingBudget != 0.0
            val budgetBlock = if (hasBudgetData) {
                val spentFormatted = String.format(Locale.US, "%,.0f", totalSpent)
                val limitFormatted = String.format(Locale.US, "%,.0f", budgetLimit)
                val remFormatted = String.format(Locale.US, "%,.0f", remainingBudget)
                val daysStr = if (daysLeft > 0) " ($daysLeft days remaining)" else ""
                "\n📊 Your Monthly Spending & Budget Status:\n• Total Spent: $spentFormatted MMK\n• Monthly Budget: $limitFormatted MMK\n• Remaining Budget: $remFormatted MMK$daysStr\n"
            } else ""

            val effectivePrice = cryptoPrice ?: when {
                isSol -> 185.0
                isEth -> 3100.0
                else -> 79980.0
            }
            val formattedPrice = String.format(Locale.US, "%,.2f", effectivePrice)
            val chgStr = if (cryptoChange != null) String.format(Locale.US, " (%+.2f%% 24h)", cryptoChange) else ""
            val priceBlock = if (isSol) {
                "\n💰 Current Solana (SOL) Benchmark Price: ~$$formattedPrice USD$chgStr\n"
            } else if (isEth) {
                "\n💰 Current Ethereum (ETH) Benchmark Price: ~$$formattedPrice USD$chgStr\n"
            } else {
                "\n• Current Bitcoin Benchmark Price: ~$$formattedPrice USD$chgStr\n"
            }

            val holdingNote = if (holdingSummary.isNotBlank()) "\n• Your Portfolio: $holdingSummary\n" else ""

            val recommendationBlock = if (hasBudgetData && remainingBudget <= 0.0) {
                """
                💡 Coach's Recommendation (Should You Buy?):
                ❌ Do NOT buy right now!
                Your monthly budget is currently exhausted or at its limit with ${if (daysLeft > 0) "$daysLeft days" else "several days"} remaining in the month. Protect your essential daily living expenses first. Investing in volatile assets when you have no budget buffer exposes you to immediate financial stress.
                """.trimIndent()
            } else {
                """
                💡 Coach's Recommendation (Should You Buy?):
                ⚠️ Proceed with high caution and strict discipline (If buying, follow these 3 core rules):

                1. Strict Risk Management (1% – 5% Allocation):
                Cryptocurrencies like $assetDisplayName carry high market volatility. Limit crypto to a small portion of your overall portfolio (typically 1% to 5%). Never invest money you might need for living expenses within the next 1 to 2 years.

                2. Foundation Prerequisite:
                Ensure your emergency fund (3 to 6 months living expenses) and primary savings goals are fully funded before taking on speculative volatility.

                3. Avoid FOMO — Use Dollar-Cost Averaging (DCA):
                Never chase sudden market spikes or invest a lump sum. Committing a fixed, small amount on a recurring schedule minimizes emotional trading and averages your entry price.
                """.trimIndent()
            }

            return """
                $title
                $budgetBlock$priceBlock
                $recommendationBlock$holdingNote
                (Note: This is educational guidance to support your financial planning, not professional financial advice.)

                Would you like to track your crypto holdings alongside your monthly budget?
            """.trimIndent()
        }

        private fun buildBurmeseCryptoAdvice(
            holdingSummary: String,
            remainingBudget: Double = 0.0,
            totalSpent: Double = 0.0,
            budgetLimit: Double = 0.0,
            daysLeft: Int = 0,
            cryptoSymbol: String = "BTC",
            cryptoPrice: Double? = null,
            cryptoChange: Double? = null
        ): String {
            val isSol = cryptoSymbol.equals("SOL", ignoreCase = true)
            val isEth = cryptoSymbol.equals("ETH", ignoreCase = true)
            val shortAsset = when {
                isSol -> "Solana (SOL)"
                isEth -> "Ethereum (ETH)"
                else -> "Bitcoin (BTC)"
            }

            val title = if (isSol) {
                "🪙 Solana (SOL) ရင်းနှီးမြှုပ်နှံမှုနှင့် ဘဏ္ဍာရေး သုံးသပ်ချက်"
            } else if (isEth) {
                "🪙 Ethereum (ETH) ရင်းနှီးမြှုပ်နှံမှုနှင့် ဘဏ္ဍာရေး သုံးသပ်ချက်"
            } else {
                "🪙 ခရစ်ပတိုနှင့် ဘစ်ကွိုင် (Bitcoin) ရင်းနှီးမြှုပ်နှံမှု အကြံပြုချက်"
            }

            val hasBudgetData = budgetLimit > 0.0 || totalSpent > 0.0 || remainingBudget != 0.0
            val budgetBlock = if (hasBudgetData) {
                val spentFormatted = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(String.format(Locale.US, "%,.0f", totalSpent))
                val limitFormatted = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(String.format(Locale.US, "%,.0f", budgetLimit))
                val remFormatted = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(String.format(Locale.US, "%,.0f", remainingBudget))
                val daysStr = if (daysLeft > 0) " (${com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(daysLeft.toString())} ရက် ကျန်ရှိ)" else ""
                "\n📊 ယခုလ သင့်၏ အသုံးစရိတ်နှင့် ဘတ်ဂျက် အခြေအနေ:\n• သုံးစွဲပြီးငွေ: $spentFormatted ကျပ်\n• လစဉ်ဘတ်ဂျက်: $limitFormatted ကျပ်\n• လက်ကျန်ငွေ: $remFormatted ကျပ်$daysStr\n"
            } else ""

            val effectivePrice = cryptoPrice ?: when {
                isSol -> 185.0
                isEth -> 3100.0
                else -> 79980.0
            }
            val formattedPrice = String.format(Locale.US, "%,.2f", effectivePrice)
            val chgStr = if (cryptoChange != null) String.format(Locale.US, " (%+.2f%% 24h)", cryptoChange) else ""
            val priceBlock = if (isSol) {
                "\n💰 လက်ရှိ Solana (SOL) စျေးကွက်ပေါက်စျေး: ~$$formattedPrice USD$chgStr\n"
            } else if (isEth) {
                "\n💰 လက်ရှိ Ethereum (ETH) စျေးကွက်ပေါက်စျေး: ~$$formattedPrice USD$chgStr\n"
            } else {
                "\n• လက်ရှိ Bitcoin စျေးကွက်ပေါက်စျေး: ~$$formattedPrice USD$chgStr\n"
            }

            val holdingNote = if (holdingSummary.isNotBlank()) "\n• သင့်လက်ရှိ ပိုင်ဆိုင်မှု: $holdingSummary\n" else ""

            val daysStr = if (daysLeft > 0) com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(daysLeft.toString()) + " ရက်" else "ရက်အနည်းငယ်"
            val recommendationBlock = if (hasBudgetData && remainingBudget <= 0.0) {
                """
                💡 Coach ၏ အကြံပြုချက် (ဝယ်သင့် မဝယ်သင့်):
                ❌ ယခုလတွင် မဝယ်သင့်သေးပါ!
                သင့်လစဉ်ဘတ်ဂျက် ကုန်လွန်/ပြည့်လုနီးပါးဖြစ်နေပြီး လကုန်ရန် $daysStr ကျန်ရှိနေသေးသဖြင့် နေ့စဉ် မရှိမဖြစ် စားဝတ်နေရေးစရိတ်ကို အရင်ဆုံး အဓိကထား ကာကွယ်သင့်ပါတယ်။ ဈေးနှုန်းအတက်အကျကြမ်းသော Crypto များတွင် လက်ကျန်ငွေမရှိဘဲ ရင်းနှီးမြှုပ်နှံပါက ဘဏ္ဍာရေးအကျပ်အတည်း ဖြစ်ပေါ်စေနိုင်ပါသည်။
                """.trimIndent()
            } else {
                """
                💡 Coach ၏ အကြံပြုချက် (ဝယ်သင့် မဝယ်သင့်):
                ⚠️ အကန့်အသတ်ဖြင့်သာ သတိထားပြီး စဉ်းစားသင့်ပါသည် (ဝယ်ယူမည်ဆိုပါက အောက်ပါ စည်းမျဉ်း ၃ ချက်ကို လိုက်နာပါ)-

                ၁။ စွန့်စားနိုင်ခြေ ထိန်းညှိခြင်း (1% – 5% / ၁% မှ ၅% အချိုးအစားသာ):
                $shortAsset နှင့် Cryptocurrency များသည် ဈေးနှုန်းအတက်အကျ အလွန်ကြမ်းတမ်းသောကြောင့် မိမိ၏ ပိုလျှံငွေ စုစုပေါင်း၏ 1% မှ 5% (၁% မှ ၅%) ထက် မပိုစေသင့်ပါ။ မဝေးတော့သောအနာဂတ် (၁ နှစ်မှ ၂ နှစ်အတွင်း) အသုံးပြုရန် လိုအပ်သော ငွေကြေးများကို လုံးဝ မထည့်ဝင်သင့်ပါ။

                ၂။ အရေးပေါ်သုံးငွေ (Emergency Fund) အရင်ရှိပါစေ:
                အခြေခံ အရေးပေါ်သုံးငွေ ၃ လမှ ၆ လစာ မရှိမဖြစ် နေထိုင်စရိတ်ကို အလွယ်တကူ ထုတ်ယူနိုင်သော ငွေသား/စုငွေအဖြစ် သီးသန့် အရင်ဖယ်ထားပြီးမှသာ ပိုလျှံငွေဖြင့် စတင်ပါ။

                ၃။ Dollar-Cost Averaging (DCA) နည်းလမ်းဖြင့် ပုံမှန်ခွဲဝယ်ပါ:
                ဈေးတက်နေချိန်တွင် အလောတကြီး လိုက်ဝယ်ခြင်း (FOMO) ကို ရှောင်ရှားပါ။ တစ်ကြိမ်တည်း အများကြီးဝယ်မည့်အစား လစဉ် သို့မဟုတ် အပတ်စဉ် ပုံမှန်ငွေပမာဏအနည်းငယ်စီဖြင့် ခွဲဝေစုဆောင်းဝယ်ယူခြင်းဖြင့် ဈေးအတက်အကျဒဏ်ကို လျှော့ချပါ။
                """.trimIndent()
            }

            return """
                $title
                $budgetBlock$priceBlock
                $recommendationBlock$holdingNote
                (မှတ်ချက် - ဤအချက်အလက်သည် ငွေကြေးစီမံခန့်ခွဲမှု လေ့လာသင်ယူရန်အတွက်သာဖြစ်ပြီး တရားဝင်ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်မဟုတ်ပါ။)

                သင့်ရင်းနှီးမြှုပ်နှံမှုများကို လစဉ်ဘတ်ဂျက်နှင့် ချိတ်ဆက်မှတ်တမ်းတင်လိုပါသလား။
            """.trimIndent()
        }

        private fun buildEnglishStockAdvice(holdingSummary: String): String {
            val holdingNote = if (holdingSummary.isNotBlank()) "\n• Your Portfolio: $holdingSummary\n" else ""

            return """
                📈 Stock Market & Equities: Coach's Perspective

                Investing in broad stock market index funds and quality equities is one of the most proven paths to long-term wealth compounding:

                1. Broad Diversification Over Stock Picking:
                Instead of gambling on single speculative stocks, prioritize broad exchange-traded Index Funds (ETFs) that hold hundreds of companies.

                2. Time Horizon (3–5+ Years):
                Equities fluctuate in the short term but trend upwards historically over multi-year periods. Only invest capital with a multi-year horizon.

                3. Dollar-Cost Averaging:
                Automate regular monthly contributions regardless of market headlines to capture long-term compounding.
                $holdingNote
                (Note: This is educational guidance to support your financial planning, not professional financial advice.)

                Would you like to review how stock investments can fit into your monthly savings target?
            """.trimIndent()
        }

        private fun buildBurmeseStockAdvice(holdingSummary: String): String {
            val holdingNote = if (holdingSummary.isNotBlank()) "\n• သင့်လက်ရှိ ပိုင်ဆိုင်မှု: $holdingSummary\n" else ""

            return """
                📈 စတော့ရှယ်ယာ ရင်းနှီးမြှုပ်နှံမှု အကြံပြုချက်

                စတော့ရှယ်ယာ (Stocks) နှင့် အစုရှယ်ယာ ရန်ပုံငွေ (Index Funds / ETFs) များသည် ရေရှည်တွင် ကြွယ်ဝမှု တိုးပွားစေရန် အကောင်းဆုံး နည်းလမ်းတစ်ခု ဖြစ်ပါသည်-

                ၁။ အစုအပြုံလိုက် ခွဲဝေရင်းနှီးမြှုပ်နှံခြင်း (Diversification):
                ကုမ္ပဏီတစ်ခုတည်း၏ စတော့ကို ရွေးချယ်စွန့်စားမည့်အစား ကုမ္ပဏီရာပေါင်းများစွာ ပါဝင်သော Index Fund / ETF များတွင် ခွဲဝေရင်းနှီးမြှုပ်နှံပါ။

                ၂။ ရေရှည်ရည်မှန်းချက် (၃ နှစ်မှ ၅ နှစ်အထက်):
                စတော့ဈေးကွက်သည် ကာလတိုတွင် အတက်အကျရှိနိုင်သော်လည်း ရေရှည်တွင် စီးပွားရေးတိုးတက်မှုနှင့်အတူ တိုးပွားလာလေ့ရှိပါသည်။

                ၃။ ပုံမှန်လစဉ် စုဆောင်းရင်းနှီးမြှုပ်နှံပါ:
                လစဉ် ပုံမှန်ငွေပမာဏတစ်ခု သတ်မှတ်၍ ရင်းနှီးမြှုပ်နှံခြင်းဖြင့် အမြတ်ငွေ အဆပေါင်းများစွာ တိုးပွားခြင်း (Compound Interest) ၏ အကျိုးကျေးဇူးကို ရရှိနိုင်ပါတယ်။
                $holdingNote
                (မှတ်ချက် - ဤအချက်အလက်သည် ငွေကြေးစီမံခန့်ခွဲမှု လေ့လာသင်ယူရန်အတွက်သာဖြစ်ပြီး တရားဝင်ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်မဟုတ်ပါ။)

                စတော့ရင်းနှီးမြှုပ်နှံမှုအတွက် လစဉ်ငွေစုပန်းတိုင် ချမှတ်လိုပါသလား။
            """.trimIndent()
        }

        private fun buildBurmeseBtcVsSolAdvice(
            holdingSummary: String,
            remainingBudget: Double,
            btcPrice: Double? = null,
            btcChange: Double? = null,
            solPrice: Double? = null,
            solChange: Double? = null,
            totalSpent: Double = 0.0,
            budgetLimit: Double = 0.0,
            daysLeft: Int = 0
        ): String {
            val holdingNote = if (holdingSummary.isNotBlank()) "\n• သင့်လက်ရှိ ပိုင်ဆိုင်မှု: $holdingSummary\n" else ""
            val hasBudgetData = budgetLimit > 0.0 || totalSpent > 0.0 || remainingBudget != 0.0
            val budgetBlock = if (hasBudgetData) {
                val spentFormatted = String.format(Locale.US, "%,.0f", totalSpent)
                val limitFormatted = String.format(Locale.US, "%,.0f", budgetLimit)
                val remFormatted = String.format(Locale.US, "%,.0f", remainingBudget)
                val daysStr = if (daysLeft > 0) " (ကျန်ရှိရက် $daysLeft ရက်)" else ""
                "\n📊 သင့်လစဉ်ဘတ်ဂျက်နှင့် အသုံးစရိတ် အခြေအနေ:\n• သုံးစွဲပြီးငွေ: $spentFormatted MMK\n• လစဉ်ဘတ်ဂျက်: $limitFormatted MMK\n• ကျန်ရှိငွေ: $remFormatted MMK$daysStr\n"
            } else ""

            val effectiveBtc = btcPrice ?: 79980.0
            val effectiveSol = solPrice ?: 185.0
            val btcStr = String.format(Locale.US, "%,.2f", effectiveBtc)
            val solStr = String.format(Locale.US, "%,.2f", effectiveSol)
            val btcChgStr = if (btcChange != null) String.format(Locale.US, " (%+.2f%%)", btcChange) else ""
            val solChgStr = if (solChange != null) String.format(Locale.US, " (%+.2f%%)", solChange) else ""

            return """
                🪙 Bitcoin (BTC) နှင့် Solana (SOL) နှိုင်းယှဉ်ချက်နှင့် ရင်းနှီးမြှုပ်နှံမှု အကြံပြုချက်

                Bitcoin (BTC) နှင့် Solana (SOL) တို့သည် ရည်ရွယ်ချက်နှင့် သဘောသဘာဝ ကွဲပြားသော Crypto ပိုင်ဆိုင်မှုများ ဖြစ်ကြပါတယ်-
                $budgetBlock
                💰 စျေးကွက်စံနှုန်း (Benchmark Prices):
                • Bitcoin (BTC): ~$$btcStr USD$btcChgStr
                • Solana (SOL): ~$$solStr USD$solChgStr

                ၁။ မတူညီသော သဘောသဘာဝနှင့် အခန်းကဏ္ဍ:
                • Bitcoin (BTC): Crypto လောက၏ "ဒစ်ဂျစ်တယ်ရွှေ" (Digital Gold) ဖြစ်ပြီး တန်ဖိုးသိုလှောင်ရာ (Store of Value) အဖြစ် အခိုင်မာဆုံး ရပ်တည်နေပါတယ်။ အတက်အကျရှိသော်လည်း Crypto အချင်းချင်းကြားတွင် စိတ်အချရဆုံး အခြေခံအုတ်မြစ် ဖြစ်ပါတယ်။
                • Solana (SOL): မြန်နှုန်းမြင့်ပြီး (High TPS) ဓာတ်ငွေ့ခ (Gas Fee) သက်သာသော Layer-1 Smart Contract ကွန်ရက်ဖြစ်ပါတယ်။ DeFi, NFT နှင့် meme coin များကြောင့် တိုးတက်မှုမြန်သော်လည်း ဈေးနှုန်းအတက်အကျ ပိုမိုကြမ်းတမ်းပါတယ်။

                ၂။ မည်သည့်အရာကို ဝယ်ယူသင့်သလဲ (Which Should You Buy?):
                • ရေရှည်တည်ငြိမ်မှုနှင့် အခြေခံအုတ်မြစ် လိုချင်ပါက: BTC ကို အဓိက ဦးစားပေးသင့်ပါတယ်။
                • နည်းပညာတိုးတက်မှုနှင့် စွန့်စားရမှု ပိုမိုခံနိုင်ပါက: SOL ကို အနည်းငယ် ရွေးချယ်နိုင်ပါတယ်။
                • စမတ်ကျသော ခွဲဝေမှု (Allocation Strategy): Crypto ရင်းနှီးမြှုပ်နှံရာတွင် BTC ကို Core အဖြစ် 70%-80% ထားရှိပြီး၊ SOL ကို Tactical အဖြစ် 20%-30% ခန့်သာ ခွဲဝေရင်းနှီးမြှုပ်နှံခြင်း (Portfolio Split) က ပိုမိုဟန်ချက်ညီစေပါတယ်။

                ၃။ ငွေကြေးစည်းမျဉ်းနှင့် သတိပြုရန် (Guardrails):
                • စုစုပေါင်း Crypto ပိုင်ဆိုင်မှုသည် မိမိ Net Worth ၏ 1% မှ 5% ထက် မကျော်လွန်သင့်ပါ။
                • မဝယ်ယူမီ အနည်းဆုံး ၃-၆ လစာ အရေးပေါ်သုံးငွေ (Emergency Fund) သီးသန့် ဖယ်ထားပါ။
                • တစ်ကြိမ်တည်း အလုံးအရင်း မဝယ်ဘဲ DCA (Dollar-Cost Averaging) နည်းလမ်းဖြင့် ပုံမှန် အနည်းငယ်စီသာ ခွဲဝယ်ပါ။
                $holdingNote
                (မှတ်ချက် - ဤအချက်အလက်သည် ငွေကြေးစီမံခန့်ခွဲမှု လေ့လာသင်ယူရန်အတွက်သာဖြစ်ပြီး တရားဝင် ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်မဟုတ်ပါ။)

                သင့်လစဉ်ဘတ်ဂျက်နှင့်အညီ သင့်တော်သော Crypto ငွေစုပန်းတိုင် ချမှတ်လိုပါသလား။
            """.trimIndent()
        }

        private fun buildEnglishBtcVsSolAdvice(
            holdingSummary: String,
            remainingBudget: Double,
            btcPrice: Double? = null,
            btcChange: Double? = null,
            solPrice: Double? = null,
            solChange: Double? = null,
            totalSpent: Double = 0.0,
            budgetLimit: Double = 0.0,
            daysLeft: Int = 0
        ): String {
            val holdingNote = if (holdingSummary.isNotBlank()) "\n• Your Portfolio: $holdingSummary\n" else ""
            val hasBudgetData = budgetLimit > 0.0 || totalSpent > 0.0 || remainingBudget != 0.0
            val budgetBlock = if (hasBudgetData) {
                val spentFormatted = String.format(Locale.US, "%,.0f", totalSpent)
                val limitFormatted = String.format(Locale.US, "%,.0f", budgetLimit)
                val remFormatted = String.format(Locale.US, "%,.0f", remainingBudget)
                val daysStr = if (daysLeft > 0) " ($daysLeft days remaining)" else ""
                "\n📊 Your Monthly Spending & Budget Status:\n• Total Spent: $spentFormatted MMK\n• Monthly Budget: $limitFormatted MMK\n• Remaining Budget: $remFormatted MMK$daysStr\n"
            } else ""

            val effectiveBtc = btcPrice ?: 79980.0
            val effectiveSol = solPrice ?: 185.0
            val btcStr = String.format(Locale.US, "%,.2f", effectiveBtc)
            val solStr = String.format(Locale.US, "%,.2f", effectiveSol)
            val btcChgStr = if (btcChange != null) String.format(Locale.US, " (%+.2f%%)", btcChange) else ""
            val solChgStr = if (solChange != null) String.format(Locale.US, " (%+.2f%%)", solChange) else ""

            return """
                🪙 Bitcoin (BTC) vs. Solana (SOL): Investment Coaching Comparison

                Bitcoin (BTC) and Solana (SOL) serve fundamentally different roles in a portfolio:
                $budgetBlock
                💰 Current Benchmark Prices:
                • Bitcoin (BTC): ~$$btcStr USD$btcChgStr
                • Solana (SOL): ~$$solStr USD$solChgStr

                1. Fundamental Differences:
                • Bitcoin (BTC): The "Digital Gold" of cryptocurrency and primary store of value. It has the longest track record, institutional backing, and relatively lower volatility compared to other crypto assets.
                • Solana (SOL): A high-throughput Layer-1 blockchain optimized for sub-second finality and negligible transaction fees. It has a vibrant DeFi and consumer ecosystem, but carries higher ecosystem and market volatility.

                2. Which One Should You Buy?
                • For Core Stability & Long-term Defense: Prioritize Bitcoin (BTC).
                • For High-Beta Growth & Tech Upside: Allocate a smaller portion to Solana (SOL).
                • Recommended Crypto Split: If allocating within a crypto bucket, a 70%-80% BTC (Core) and 20%-30% SOL (Satellite) split balances stability with growth potential.

                3. Financial Guardrails:
                • Total crypto exposure should remain strictly within 1% to 5% of your total net worth.
                • Always secure 3 to 6 months of liquid emergency savings before deploying capital into crypto.
                • Use Dollar-Cost Averaging (DCA) rather than lump-sum timing to smooth out market volatility.
                $holdingNote
                (Note: This is educational guidance to support your financial planning, not professional financial advice.)

                Would you like to explore setting a target crypto allocation or linking this with your monthly savings targets?
            """.trimIndent()
        }

        private fun buildBurmeseTeslaAdvice(
            holdingSummary: String,
            remainingBudget: Double = 0.0,
            totalSpent: Double = 0.0,
            budgetLimit: Double = 0.0,
            daysLeft: Int = 0
        ): String {
            val holdingNote = if (holdingSummary.isNotBlank()) "\n• သင့်လက်ရှိ ပိုင်ဆိုင်မှု: $holdingSummary\n" else ""
            val hasBudgetData = budgetLimit > 0.0 || totalSpent > 0.0 || remainingBudget != 0.0
            val budgetBlock = if (hasBudgetData) {
                val spentFormatted = String.format(Locale.US, "%,.0f", totalSpent)
                val limitFormatted = String.format(Locale.US, "%,.0f", budgetLimit)
                val remFormatted = String.format(Locale.US, "%,.0f", remainingBudget)
                val daysStr = if (daysLeft > 0) " (ကျန်ရှိရက် $daysLeft ရက်)" else ""
                "\n📊 သင့်လစဉ်ဘတ်ဂျက်နှင့် အသုံးစရိတ် အခြေအနေ:\n• သုံးစွဲပြီးငွေ: $spentFormatted MMK\n• လစဉ်ဘတ်ဂျက်: $limitFormatted MMK\n• ကျန်ရှိငွေ: $remFormatted MMK$daysStr\n"
            } else ""

            return """
                🚗 Tesla (TSLA) စတော့ရှယ်ယာ ရင်းနှီးမြှုပ်နှံမှု အကြံပြုချက်

                Tesla (TSLA) စတော့ရှယ်ယာ ဝယ်ယူရန် စဉ်းစားနေပါက အောက်ပါ အဓိကအချက်များကို သတိပြုသင့်ပါတယ်-
                $budgetBlock
                ၁။ ကုမ္ပဏီအခြေအနေနှင့် ဈေးကွက်အတက်အကျ (Company Profile & Volatility):
                Tesla သည် လျှပ်စစ်ကား (EV) နှင့် နည်းပညာ၊ AI ကဏ္ဍတွင် ကမ္ဘာ့ထိပ်တန်း ဦးဆောင်ကုမ္ပဏီဖြစ်သော်လည်း စတော့ဈေးနှုန်း အတက်အကျ (Volatility) အလွန်မြင့်မားပါတယ်။

                ၂။ Single Stock Risk နှင့် Diversification (ခွဲဝေရင်းနှီးမြှုပ်နှံခြင်း):
                ကုမ္ပဏီတစ်ခုတည်း၏ စတော့ (Single Stock) ပေါ်တွင်သာ ငွေအလုံးအရင်း စိုက်ထုတ်ခြင်းသည် စွန့်စားရမှု ကြီးမားပါတယ်။ S&P 500 သို့မဟုတ် Broad Market ETF ကဲ့သို့သော ကုမ္ပဏီပေါင်း ၅၀၀ ကျော် ပါဝင်သော ရန်ပုံငွေများကို အဓိက အခြေခံ (Core) အဖြစ် ထားရှိပြီးမှသာ Tesla ကဲ့သို့ တိုးတက်မှုမြန် စတော့များကို မိမိ Portfolio ၏ 5% မှ 10% ခန့်သာ အပိုဆောင်း ထည့်ဝင်သင့်ပါတယ်။

                ၃။ အရေးပေါ်သုံးငွေ (Emergency Fund) နှင့် DCA:
                စတော့မဝယ်ယူမီ အနည်းဆုံး ၃ လမှ ၆ လစာ အရေးပေါ်သုံးငွေ (Emergency Fund) စုဆောင်းထားပြီးဖြစ်ရန် လိုအပ်ပါတယ်။ ငွေအားလုံး တစ်ပြိုင်နက် မထည့်ဘဲ ဒေါ်လာကုန်ကျစရိတ် ပျမ်းမျှခြင်း (DCA - Dollar-Cost Averaging) နည်းလမ်းဖြင့် လစဉ် ပုံမှန် ပမာဏ အနည်းငယ်စီ ခွဲဝေဝယ်ယူခြင်းက အန္တရာယ် အနည်းဆုံး ဖြစ်ပါတယ်။
                $holdingNote
                (မှတ်ချက် - ဤအချက်အလက်သည် ငွေကြေးစီမံခန့်ခွဲမှု လေ့လာသင်ယူရန်အတွက်သာဖြစ်ပြီး တရားဝင် ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်မဟုတ်ပါ။)

                Tesla သို့မဟုတ် အခြားစတော့များအတွက် လစဉ်ငွေစုပန်းတိုင် ချမှတ်ပြီး စတင်လေ့လာလိုပါသလား။
            """.trimIndent()
        }

        private fun buildEnglishTeslaAdvice(
            holdingSummary: String,
            remainingBudget: Double = 0.0,
            totalSpent: Double = 0.0,
            budgetLimit: Double = 0.0,
            daysLeft: Int = 0
        ): String {
            val holdingNote = if (holdingSummary.isNotBlank()) "\n• Your Portfolio: $holdingSummary\n" else ""
            val hasBudgetData = budgetLimit > 0.0 || totalSpent > 0.0 || remainingBudget != 0.0
            val budgetBlock = if (hasBudgetData) {
                val spentFormatted = String.format(Locale.US, "%,.0f", totalSpent)
                val limitFormatted = String.format(Locale.US, "%,.0f", budgetLimit)
                val remFormatted = String.format(Locale.US, "%,.0f", remainingBudget)
                val daysStr = if (daysLeft > 0) " ($daysLeft days remaining)" else ""
                "\n📊 Your Monthly Spending & Budget Status:\n• Total Spent: $spentFormatted MMK\n• Monthly Budget: $limitFormatted MMK\n• Remaining Budget: $remFormatted MMK$daysStr\n"
            } else ""

            return """
                🚗 Tesla (TSLA) Stock Investment & Financial Coaching Review

                If you are considering buying Tesla (TSLA) stock, here is the coach's perspective:
                $budgetBlock
                1. Growth Profile & High Volatility:
                Tesla is an established global leader in electric vehicles (EV), clean energy, and autonomous tech. However, individual tech growth stocks carry substantially higher volatility than broad market indexes.

                2. Single-Stock Risk vs. Index Diversification:
                Concentrating capital in a single stock introduces significant downside risk. A sound wealth-building strategy uses diversified index funds (such as an S&P 500 ETF) as the core 80%-90% foundation, limiting individual stock picks like Tesla to 5%-10% of your total investment portfolio.

                3. Financial Prerequisites & DCA:
                • Ensure you have 3 to 6 months of living expenses saved in a liquid emergency fund before buying single stocks.
                • Never invest money needed for short-term living expenses or monthly bills.
                • Use Dollar-Cost Averaging (DCA) to build positions gradually rather than timing market swings.
                $holdingNote
                (Note: This is educational guidance to support your financial planning, not professional financial advice.)

                Would you like to set a savings goal or explore index fund diversification options?
            """.trimIndent()
        }

        private fun buildEnglishGeneralInvestmentAdvice(holdingSummary: String, remainingBudget: Double): String {
            val holdingNote = if (holdingSummary.isNotBlank()) "\n• Current Active Holdings: $holdingSummary\n" else ""
            val budgetNote = if (remainingBudget > 0) {
                val formatted = String.format(Locale.US, "%,.0f", remainingBudget)
                "\n• Monthly Buffer: $formatted MMK remaining in this month's budget.\n"
            } else ""

            return """
                🏛️ Smart Investment & Wealth Building: Coach's Roadmap

                A successful personal finance strategy follows the 3-Tier Wealth Building Pyramid:

                Tier 1: Foundation (Emergency Fund)
                • Save 3 to 6 months of living expenses in liquid cash/savings. This ensures you never have to sell investments at a loss during emergencies.

                Tier 2: Defensive Assets (5% – 10%)
                • Gold & Fixed Reserves: Preserves capital and shields against inflation and currency depreciation.

                Tier 3: Growth Assets (70% – 85%)
                • Broad Equities / ETFs: Compounding growth engines over 5+ years.
                • Speculative / Crypto (1% – 5% max): Small high-upside allocation.
                $holdingNote$budgetNote
                (Note: This is educational guidance to support your financial planning, not professional financial advice.)

                Which investment area would you like to explore next? We can connect it directly with your monthly saving goals!
            """.trimIndent()
        }

        private fun buildBurmeseGeneralInvestmentAdvice(holdingSummary: String, remainingBudget: Double): String {
            val holdingNote = if (holdingSummary.isNotBlank()) "\n• လက်ရှိ ပိုင်ဆိုင်မှုများ: $holdingSummary\n" else ""
            val budgetNote = if (remainingBudget > 0) {
                val formatted = String.format(Locale.US, "%,.0f", remainingBudget)
                val myFormatted = com.savingcoach.app.utils.BurmeseNumeralConverter.toBurmeseDigits(formatted)
                "\n• လစဉ်ဘတ်ဂျက်: လက်ကျန်ငွေ $myFormatted ကျပ်\n"
            } else ""

            return """
                🏛️ စနစ်တကျ ရင်းနှီးမြှုပ်နှံမှုနှင့် ကြွယ်ဝမှု တည်ဆောက်ခြင်း လမ်းညွှန်

                ရေရှည်ကြွယ်ဝချမ်းသာရန် အောက်ပါ အဆင့် ၃ ဆင့်ဖြင့် စနစ်တကျ စတင်ရန် အကြံပြုလိုပါတယ်-

                အဆင့် ၁ - အခြေခံ အရေးပေါ်သုံးငွေ (Emergency Fund)
                • အနည်းဆုံး ၃ လမှ ၆ လစာ မရှိမဖြစ် ကုန်ကျစရိတ်ကို ငွေသား/စုငွေအဖြစ် အရင်စုဆောင်းပါ။ အရေးပေါ်ကိစ္စကြုံပါက ရင်းနှီးမြှုပ်နှံမှုများကို အရှုံးခံရောင်းစရာမလိုစေရန် ကာကွယ်ပေးပါသည်။

                အဆင့် ၂ - အကာအကွယ် ပိုင်ဆိုင်မှုများ (၅% မှ ၁၀%)
                • ရွှေ (Gold) နှင့် တည်ငြိမ်သော စုငွေများ: ငွေကြေးဖောင်းပွမှုနှင့် ငွေတန်ဖိုးကျဆင်းမှုဒဏ်ကို ကာကွယ်ပေးပါသည်။

                အဆင့် ၃ - တိုးပွားပိုင်ဆိုင်မှုများ (Growth Assets)
                • စတော့ရှယ်ယာ / ETFs: ရေရှည် ၅ နှစ်အထက် အမြတ်တိုးပွားစေသော အင်ဂျင်များ ဖြစ်ပါသည်။
                • ခရစ်ပတို (Crypto): အများဆုံး ၁% မှ ၅% ခန့်သာ စွန့်စားသင့်ပါသည်။
                $holdingNote$budgetNote
                (မှတ်ချက် - ဤအချက်အလက်သည် ငွေကြေးစီမံခန့်ခွဲမှု လေ့လာသင်ယူရန်အတွက်သာဖြစ်ပြီး တရားဝင်ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်မဟုတ်ပါ။)

                မည်သည့် ရင်းနှီးမြှုပ်နှံမှုကဏ္ဍကို အရင်ဆုံး လေ့လာချင်ပါသလဲ။ သင့်လစဉ် စုငွေပန်းတိုင်နှင့် ချိတ်ဆက်ပေးနိုင်ပါတယ်။
            """.trimIndent()
        }
    }

    suspend fun buildExpenseReportResponse(userId: String, language: String): String {
        return try {
            val now = LocalDate.now()
            val yearMonth = YearMonth.now()
            val yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

            val expenses = expenseRepository.getExpensesForMonth(userId, yearMonthStr).firstOrNull() ?: emptyList()
            val totalSpent = expenses.sumOf { it.amount }
            val count = expenses.size

            val categoryMap = expenses
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }

            val recentExpenses = expenses.sortedByDescending { it.createdAt }.take(3)

            val monthNameEn = now.month.name.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) }
            val monthNameMy = when (now.monthValue) {
                1 -> "ဇန်နဝါရီ"
                2 -> "ဖေဖော်ဝါရီ"
                3 -> "မတ်"
                4 -> "ဧပြီ"
                5 -> "မေ"
                6 -> "ဇွန်"
                7 -> "ဇူလိုင်"
                8 -> "ဩဂုတ်"
                9 -> "စက်တင်ဘာ"
                10 -> "အောက်တိုဘာ"
                11 -> "နိုဝင်ဘာ"
                12 -> "ဒီဇင်ဘာ"
                else -> ""
            }

            if (language == "my") {
                if (expenses.isEmpty()) {
                    return "📋 ဒီလ အသုံးစရိတ် အစီရင်ခံစာ ($monthNameMy ${now.year})\n\n💡 ဒီလအတွက် အသုံးစရိတ် မှတ်တမ်း မရှိသေးပါ။ အသုံးစရိတ် မှတ်တမ်းတင်လိုပါက 'ကော်ဖီ ၂,၅၀၀ သုံးတယ်' စသဖြင့် ပြောပြပေးပါ။"
                }

                val catLines = categoryMap.joinToString("\n") { (cat, amt) ->
                    val burmeseCat = com.savingcoach.app.ui.chat.CategoryResolver.toBurmeseName(cat)
                    val pct = if (totalSpent > 0) (amt / totalSpent) * 100 else 0.0
                    "• $burmeseCat: ${String.format(Locale.US, "%,.0f", amt)} MMK (${String.format(Locale.US, "%.1f", pct)}%)"
                }

                val recentLines = recentExpenses.joinToString("\n") { exp ->
                    val name = when {
                        exp.description.isNotBlank() -> exp.description
                        exp.merchant.isNotBlank() -> exp.merchant
                        else -> com.savingcoach.app.ui.chat.CategoryResolver.toBurmeseName(exp.category)
                    }
                    "• $name: ${String.format(Locale.US, "%,.0f", exp.amount)} MMK"
                }

                val topCat = categoryMap.firstOrNull()
                val topCatName = if (topCat != null) com.savingcoach.app.ui.chat.CategoryResolver.toBurmeseName(topCat.key) else ""
                val insight = if (topCat != null) {
                    "💡 သုံးသပ်ချက်:\nဒီလတွင် အသုံးစရိတ်အများဆုံးက \"$topCatName\" ဖြစ်ပါတယ်။ မိမိ၏ အသုံးစရိတ် ကဏ္ဍများကို သိရှိထားခြင်းက ငွေပိုစုနိုင်ရန် အဓိက သော့ချက်ဖြစ်ပါတယ်။"
                } else ""

                "📋 အသုံးစရိတ် အစီရင်ခံစာ ($monthNameMy ${now.year})\n\n💰 စုစုပေါင်း သုံးစွဲမှု:\n• ${String.format(Locale.US, "%,.0f", totalSpent)} MMK ($count ကြိမ်)\n\n📊 ကဏ္ဍအလိုက် သုံးစွဲမှု:\n$catLines\n\n🕒 လတ်တလော အသုံးစရိတ်များ:\n$recentLines\n\n$insight".trim()
            } else {
                if (expenses.isEmpty()) {
                    return "📋 Expense Report ($monthNameEn ${now.year})\n\n💡 No expenses recorded yet this month. To log an expense, you can type e.g. 'Coffee 2,500' or 'Lunch 5,000'."
                }

                val catLines = categoryMap.joinToString("\n") { (cat, amt) ->
                    val pct = if (totalSpent > 0) (amt / totalSpent) * 100 else 0.0
                    "• $cat: ${String.format(Locale.US, "%,.0f", amt)} MMK (${String.format(Locale.US, "%.1f", pct)}%)"
                }

                val recentLines = recentExpenses.joinToString("\n") { exp ->
                    val name = when {
                        exp.description.isNotBlank() -> exp.description
                        exp.merchant.isNotBlank() -> exp.merchant
                        else -> exp.category
                    }
                    "• $name: ${String.format(Locale.US, "%,.0f", exp.amount)} MMK"
                }

                val topCat = categoryMap.firstOrNull()
                val insight = if (topCat != null) {
                    "💡 Expense Insight:\nYour largest spending category this month is ${topCat.key}. Tracking where your money goes is your superpower toward smart financial control!"
                } else ""

                "📋 Expense Report ($monthNameEn ${now.year})\n\n💰 Total Spending:\n• ${String.format(Locale.US, "%,.0f", totalSpent)} MMK ($count transactions)\n\n📊 Spending by Category:\n$catLines\n\n🕒 Recent Expenses:\n$recentLines\n\n$insight".trim()
            }
        } catch (e: Exception) {
            if (language == "my") {
                "အသုံးစရိတ် အချက်အလက် ရယူ၍ မရပါ။ နောက်မှ ထပ်ကြိုးစားကြည့်ပါ။"
            } else {
                "Unable to generate expense report right now. Please try again later."
            }
        }
    }

    suspend fun buildFinancialReportResponse(userId: String, language: String, query: String = ""): String {
        return try {
            val now = LocalDate.now()
            val yearMonth = YearMonth.now()
            val yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

            val budget = budgetRepository.getBudget(userId, yearMonthStr).firstOrNull()
            val expenses = expenseRepository.getExpensesForMonth(userId, yearMonthStr).firstOrNull() ?: emptyList()
            val challenges = savingChallengeRepository.getActiveChallenges(userId).firstOrNull() ?: emptyList()
            val holdings = try {
                investmentRepository.getHoldingsOnce(userId)
                    .filter { !it.isStoppedCompat }
            } catch (_: Exception) {
                emptyList()
            }

            val budgetLimit = budget?.limit ?: 0.0
            val totalSpent = expenses.sumOf { it.amount }
            val remaining = budgetLimit - totalSpent
            val daysLeft = yearMonth.lengthOfMonth() - now.dayOfMonth
            val percentLeft = if (budgetLimit > 0) ((remaining / budgetLimit) * 100).coerceAtLeast(0.0) else 0.0

            val topCategories = expenses
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
                .take(3)

            val dailySafeToSpend = if (daysLeft > 0 && remaining > 0) remaining / daysLeft else 0.0
            val biggestLeak = topCategories.firstOrNull()
            val activeChallenges = challenges.filter { it.isActive }

            val isWealth = query.contains("wealth", ignoreCase = true) ||
                    query.contains("net worth", ignoreCase = true) ||
                    query.contains("networth", ignoreCase = true) ||
                    query.contains("portfolio", ignoreCase = true) ||
                    query.contains("investment", ignoreCase = true) ||
                    query.contains("ကြွယ်ဝမှု") ||
                    query.contains("ပိုင်ဆိုင်မှု")

            if (language == "my") {
                val title = if (isWealth) "🏛️ သင့်ရဲ့ ကြွယ်ဝမှုနှင့် ဘဏ္ဍာရေး သုံးသပ်ချက် (Wealth Overview)" else "📊 သင့်ရဲ့ ဘဏ္ဍာရေးအခြေအနေ သုံးသပ်ချက်"
                val burmeseCat = if (biggestLeak != null) com.savingcoach.app.ui.chat.CategoryResolver.toBurmeseName(biggestLeak.key) else ""

                // 1. Interpret what figures mean for daily life
                val lifeInterpretation = when {
                    budgetLimit <= 0 -> "ဒီလအတွင်း စုစုပေါင်း ${String.format(Locale.US, "%,.0f", totalSpent)} MMK သုံးစွဲထားပါတယ်။ လစဉ်ဘတ်ဂျက်တစ်ခု သတ်မှတ်ထားပါက နေ့စဉ်သုံးစွဲမှုအတွက် ပိုမိုတိကျသော လမ်းညွှန်ချက် ရရှိနိုင်ပါတယ်။"
                    remaining > budgetLimit * 0.5 -> "လစဉ်ဘတ်ဂျက်ထဲက ${String.format(Locale.US, "%,.0f", remaining)} MMK (${String.format(Locale.US, "%.1f", percentLeft)}%) ကျန်ရှိပြီး လကုန်ရန် $daysLeft ရက်ကျန်ပါသေးတယ်။ နေ့စဉ်ဘဝမှာ စိတ်အေးချမ်းသာစွာ သုံးစွဲနိုင်တဲ့ လုံလောက်တဲ့ အခြေအနေမှာ ရှိနေပါတယ်။"
                    remaining > 0 -> "ကျန်ရှိသော $daysLeft ရက်အတွက် ${String.format(Locale.US, "%,.0f", remaining)} MMK (${String.format(Locale.US, "%.1f", percentLeft)}%) ကျန်ရှိပါတယ်။ ဘတ်ဂျက်မကျော်စေဖို့ နေ့စဉ်သုံးစွဲမှုကို အနည်းငယ် သတိပြုပေးပါ။"
                    else -> "ဒီလအတွက် လျာထားသော ဘတ်ဂျက် ပြည့်သွားပါပြီ (လကုန်ရန် $daysLeft ရက်ကျန်)။ မလိုအပ်သော အသုံးစရိတ်များကို လျှော့ချပြီး မရှိမဖြစ်လိုအပ်ချက်များကိုသာ ဦးစားပေးပါ။"
                }

                // 2. Daily Safe-to-Spend
                val safeToSpendText = if (budgetLimit > 0) {
                    "🎯 နေ့စဉ် စိတ်ချလက်ချသုံးနိုင်သော ပမာဏ (Daily Safe-to-Spend):\n• တစ်ရက်လျှင် ${String.format(Locale.US, "%,.0f", dailySafeToSpend)} MMK (ကျန်ရှိသော $daysLeft ရက်အတွက်)"
                } else {
                    "🎯 နေ့စဉ် စိတ်ချလက်ချသုံးနိုင်သော ပမာဏ:\n• သင့်အတွက် သင့်တော်သော နေ့စဉ်ပမာဏ တွက်ချက်နိုင်ရန် လစဉ်ဘတ်ဂျက် သတ်မှတ်ပေးပါ။"
                }

                // 3. Single biggest spending leak with zero judgment
                val biggestLeakText = if (biggestLeak != null) {
                    val pct = if (totalSpent > 0) " (စုစုပေါင်းအသုံးစရိတ်၏ ${String.format(Locale.US, "%.1f", (biggestLeak.value / totalSpent) * 100)}%)" else ""
                    "💡 အများဆုံး သုံးစွဲထားသည့် ကဏ္ဍ (Zero Judgment):\n• $burmeseCat: ${String.format(Locale.US, "%,.0f", biggestLeak.value)} MMK$pct\nဒီကဏ္ဍမှာ အသုံးများတာ သဘာဝကျပြီး နားလည်ပေးလို့ ရပါတယ် — ကိုယ့်ငွေ ဘယ်ရောက်သွားလဲဆိုတာ သိရှိထားခြင်းက အကောင်းဆုံး စတင်မှုဖြစ်ပါတယ်။"
                } else {
                    "💡 အသုံးစရိတ်:\n• ဒီလအတွက် အသုံးစရိတ် မှတ်တမ်း မရှိသေးပါ — အစကောင်းတစ်ခုပါပဲ!"
                }

                val validChallenges = activeChallenges.filter { it.targetAmount > 0 }
                val challStr = if (validChallenges.isNotEmpty()) {
                    val cLines = validChallenges.take(2).joinToString("\n") {
                        val progress = ((it.currentAmount / it.targetAmount) * 100).toInt()
                        "• ${it.title}: ${String.format(Locale.US, "%,.0f", it.currentAmount)}/${String.format(Locale.US, "%,.0f", it.targetAmount)} MMK ($progress% ပြီးစီး)"
                    }
                    "🏆 ငွေစုစိန်ခေါ်မှုများ (Savings):\n$cLines\n\n"
                } else {
                    "🏆 ငွေစုခြင်း:\n• လက်ရှိတွင် စိန်ခေါ်မှု မရှိသေးပါ။ 1K a Day ကဲ့သို့ စိန်ခေါ်မှုလေးတစ်ခု စတင်လိုက်ပါ။\n\n"
                }

                // 4. Investment Section
                val investmentSection = if (holdings.isNotEmpty()) {
                    val totalCostBasisUsd = holdings.sumOf { it.units * it.buyPrice }
                    val formattedCostBasis = String.format(Locale.US, "%.2f", totalCostBasisUsd)
                    val holdingLines = holdings.take(3).joinToString("\n") { h ->
                        val ticker = h.displayTicker.ifBlank { h.symbol }
                        val price = String.format(Locale.US, "%.2f", h.buyPrice)
                        val assetClass = when (h.type.lowercase(Locale.US)) {
                            "crypto" -> "Cryptocurrency"
                            "commodity" -> "ရွှေ / ကုန်စည်"
                            "stock" -> "စတော့ရှယ်ယာ"
                            else -> h.type
                        }
                        "• $ticker: ${h.units} units @ $$price USD ($assetClass)"
                    }
                    "📈 ရင်းနှီးမြှုပ်နှံမှု အခြေအနေ (Investments):\n• စုစုပေါင်း ကုန်ကျစရိတ်: $$formattedCostBasis USD (ပိုင်ဆိုင်မှု ${holdings.size} ခု)\n$holdingLines\n\n"
                } else {
                    "📈 ရင်းနှီးမြှုပ်နှံမှု (Investments):\n• လက်ရှိတွင် ရင်းနှီးမြှုပ်နှံမှု မှတ်တမ်း မရှိသေးပါ။ အရေးပေါ်ရန်ပုံငွေ အရင်တည်ဆောက်ရန် အကြံပြုပါတယ်။\n\n"
                }

                // 5. Exactly ONE practical, low-effort step this week
                val oneStepThisWeek = when {
                    activeChallenges.isNotEmpty() -> {
                        val targetChallenge = activeChallenges.first()
                        "🌱 ဒီတစ်ပတ် လုပ်ဆောင်နိုင်မည့် ရိုးရှင်းသော အဆင့်:\n• သင့်ရဲ့ \"${targetChallenge.title}\" စိန်ခေါ်မှုထဲသို့ ဒီနေ့ ၁,၀၀၀ MMK ခန့် စတင်ထည့်ဝင်ပြီး ငွေစုအလေ့အကျင့်ကို အရှိန်ယူလိုက်ပါ။"
                    }
                    biggestLeak != null -> {
                        "🌱 ဒီတစ်ပတ် လုပ်ဆောင်နိုင်မည့် ရိုးရှင်းသော အဆင့်:\n• ဒီတစ်ပတ်မှာ အဆာပြေမုန့် သို့မဟုတ် အပြင်ထွက်စားတာကို တစ်ကြိမ်ခန့် လျှော့ပြီး အပိုငွေ စုဆောင်းကြည့်ပါ။"
                    }
                    else -> {
                        "🌱 ဒီတစ်ပတ် လုပ်ဆောင်နိုင်မည့် ရိုးရှင်းသော အဆင့်:\n• တစ်နေ့ ၁,၀၀၀ ကျပ် စုဆောင်းခြင်းကဲ့သို့ လွယ်ကူသော စိန်ခေါ်မှုလေးတစ်ခု စတင်ကြည့်ပါ။"
                    }
                }

                "$title\n\n$lifeInterpretation\n\n$safeToSpendText\n\n$biggestLeakText\n\n$challStr$investmentSection$oneStepThisWeek"
            } else {
                val title = if (isWealth) "🏛️ Wealth & Financial Overview" else "📊 Financial Overview & Coach Report"

                // 1. Interpret what figures mean for daily life
                val lifeInterpretation = when {
                    budgetLimit <= 0 -> "You've spent ${String.format(Locale.US, "%,.0f", totalSpent)} MMK this month. Setting a monthly budget limit would give you a clear daily guardrail!"
                    remaining > budgetLimit * 0.5 -> "You have ${String.format(Locale.US, "%,.0f", remaining)} MMK left (${String.format(Locale.US, "%.1f", percentLeft)}%) with $daysLeft days remaining. That gives you solid breathing room for your day-to-day routine!"
                    remaining > 0 -> "You have ${String.format(Locale.US, "%,.0f", remaining)} MMK left (${String.format(Locale.US, "%.1f", percentLeft)}%) for the next $daysLeft days. You're still in the green, but mindful daily pacing will keep you stress-free."
                    else -> "You've reached your planned budget for the month with $daysLeft days to go. Let's protect your peace of mind by keeping daily spending strictly to essentials."
                }

                // 2. Daily Safe-to-Spend
                val safeToSpendText = if (budgetLimit > 0) {
                    "🎯 Daily Safe-to-Spend:\n• ${String.format(Locale.US, "%,.0f", dailySafeToSpend)} MMK / day for the remaining $daysLeft days of the month."
                } else {
                    "🎯 Daily Safe-to-Spend:\n• Set a monthly budget to unlock your personalized daily safe-to-spend target."
                }

                // 3. Single biggest spending leak with zero judgment
                val biggestLeakText = if (biggestLeak != null) {
                    val pct = if (totalSpent > 0) " (${String.format(Locale.US, "%.1f", (biggestLeak.value / totalSpent) * 100)}% of your total spend)" else ""
                    "💡 Biggest Outflow (Zero Judgment):\n• ${biggestLeak.key}: ${String.format(Locale.US, "%,.0f", biggestLeak.value)} MMK$pct\nIt's completely natural for this category to take the biggest bite—knowing where your money goes is your superpower, not a mistake."
                } else {
                    "💡 Spending Outflow:\n• No expenses recorded yet this month—a clean slate!"
                }

                val validChallenges = activeChallenges.filter { it.targetAmount > 0 }
                val challStr = if (validChallenges.isNotEmpty()) {
                    val cLines = validChallenges.take(2).joinToString("\n") {
                        val progress = ((it.currentAmount / it.targetAmount) * 100).toInt()
                        "• ${it.title}: ${String.format(Locale.US, "%,.0f", it.currentAmount)}/${String.format(Locale.US, "%,.0f", it.targetAmount)} MMK ($progress% complete)"
                    }
                    "🏆 Active Saving Challenges:\n$cLines\n\n"
                } else {
                    "🏆 Savings:\n• No active saving challenges yet. Starting small with '1K a Day' builds great momentum!\n\n"
                }

                // 4. Investment Section
                val investmentSection = if (holdings.isNotEmpty()) {
                    val totalCostBasisUsd = holdings.sumOf { it.units * it.buyPrice }
                    val formattedCostBasis = String.format(Locale.US, "%.2f", totalCostBasisUsd)
                    val holdingLines = holdings.take(3).joinToString("\n") { h ->
                        val ticker = h.displayTicker.ifBlank { h.symbol }
                        val price = String.format(Locale.US, "%.2f", h.buyPrice)
                        val assetClass = when (h.type.lowercase(Locale.US)) {
                            "crypto" -> "Cryptocurrency"
                            "commodity" -> "Commodity / Gold"
                            "stock" -> "Stock Equity"
                            else -> h.type
                        }
                        "• $ticker: ${h.units} units @ $$price USD ($assetClass)"
                    }
                    "📈 Investments & Portfolio:\n• Total Cost Basis: $$formattedCostBasis USD across ${holdings.size} active holdings\n$holdingLines\n\n"
                } else {
                    "📈 Investments & Portfolio:\n• No active investment holdings recorded yet. Securing an emergency buffer first is the ideal wealth strategy!\n\n"
                }

                // 5. Exactly ONE practical, low-effort step this week
                val oneStepThisWeek = when {
                    activeChallenges.isNotEmpty() -> {
                        val targetChallenge = activeChallenges.first()
                        "🌱 One Step This Week:\n• Deposit just 1,000 MMK into your \"${targetChallenge.title}\" challenge today to build momentum with zero stress."
                    }
                    biggestLeak != null -> {
                        "🌱 One Step This Week:\n• Try packing a snack or making one meal at home this week to effortlessly keep an extra 3,000 MMK in your pocket."
                    }
                    else -> {
                        "🌱 One Step This Week:\n• Pick a small goal like saving 1,000 MMK a day to kick off an easy savings habit."
                    }
                }

                "$title\n\n$lifeInterpretation\n\n$safeToSpendText\n\n$biggestLeakText\n\n$challStr$investmentSection$oneStepThisWeek"
            }
        } catch (e: Exception) {
            if (language == "my") {
                "ဘဏ္ဍာရေးအချက်အလက် ရယူ၍ မရပါ။ နောက်မှ ထပ်ကြိုးစားကြည့်ပါ။"
            } else {
                "Unable to generate financial report right now. Please try again later."
            }
        }
    }

    suspend fun buildSavingsReportResponse(userId: String, language: String): String {
        return try {
            val challenges = savingChallengeRepository.getActiveChallenges(userId).firstOrNull() ?: emptyList()
            val activeChallenges = challenges.filter { it.isActive }

            val totalTarget = activeChallenges.sumOf { it.targetAmount }
            val totalSaved = activeChallenges.sumOf { it.currentAmount }
            val overallProgress = if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt() else 0

            if (language == "my") {
                if (activeChallenges.isEmpty()) {
                    """
                    🏆 ငွေစုခြင်း အစီရင်ခံစာ (Savings Report)

                    လက်ရှိတွင် သတ်မှတ်ထားသော ငွေစုစိန်ခေါ်မှု (Saving Challenge) မရှိသေးပါ။

                    💡 စတင်ရန် အကြံပြုချက်-
                    • '1K a Day' (တစ်နေ့ ၁,၀၀၀ ကျပ် စုဆောင်းခြင်းဖြင့် စတင်ပါ)
                    • '52-Week Challenge' (အပတ်စဉ် ပုံမှန် တိုးပြီး စုဆောင်းပါ)
                    • သို့မဟုတ် မိမိစိတ်ကြိုက် ရည်မှန်းချက် (ဥပမာ- ဖုန်းဝယ်ရန်၊ အရေးပေါ်ရန်ပုံငွေ) ဖြင့် စိန်ခေါ်မှုအသစ်တစ်ခု ဖန်တီးပြီး စတင်လိုက်ပါ!
                    """.trimIndent()
                } else {
                    val challengeList = activeChallenges.joinToString("\n") { c ->
                        val progress = if (c.targetAmount > 0) ((c.currentAmount / c.targetAmount) * 100).toInt() else 0
                        val remaining = (c.targetAmount - c.currentAmount).coerceAtLeast(0.0)
                        val remText = if (remaining > 0) " (လိုသေး: ${String.format(Locale.US, "%,.0f", remaining)} MMK)" else " (ပြည့်သွားပါပြီ! 🎉)"
                        "• ${c.title}: ${String.format(Locale.US, "%,.0f", c.currentAmount)} / ${String.format(Locale.US, "%,.0f", c.targetAmount)} MMK ($progress%)$remText"
                    }

                    val formattedSaved = String.format(Locale.US, "%,.0f", totalSaved)
                    val formattedTarget = String.format(Locale.US, "%,.0f", totalTarget)

                    val coachTip = when {
                        overallProgress >= 80 -> "အရမ်းကောင်းပါတယ်! ပန်းတိုင်ရောက်ဖို့ အနီးကပ်ဆုံး အခြေအနေကို ရောက်ရှိနေပါပြီ။"
                        overallProgress >= 40 -> "ငွေစုနှုန်း တည်ငြိမ်နေပြီး တိုးတက်မှု အားရစရာ ကောင်းပါတယ်။ ဆက်လက် ထိန်းသိမ်းထားပါ။"
                        else -> "စတင်စုဆောင်းမှုက အရေးကြီးဆုံး ခြေလှမ်းဖြစ်ပါတယ်။ နေ့စဉ် ပမာဏအနည်းငယ်စီ ပုံမှန်ထည့်ဝင်ပေးပါ။"
                    }

                    """
                    🏆 ငွေစုခြင်း အစီရင်ခံစာ (Savings & Challenges Report)

                    💰 စုစုပေါင်း စုဆောင်းပြီးငွေ:
                    • $formattedSaved / $formattedTarget MMK (စုစုပေါင်း $overallProgress% ပြီးစီး)

                    🎯 လက်ရှိ ငွေစုစိန်ခေါ်မှုများ:
                    $challengeList

                    💡 ငွေကြေးအကြံပြုချက်:
                    $coachTip
                    """.trimIndent()
                }
            } else {
                if (activeChallenges.isEmpty()) {
                    """
                    🏆 Savings & Challenges Report

                    You don't have any active saving challenges yet.

                    💡 Recommended Next Steps:
                    • Start a '1K a Day' challenge (save 1,000 MMK daily)
                    • Try the '52-Week Challenge' to steadily build your savings
                    • Or create a custom challenge for specific goals like a new gadget or emergency fund!
                    """.trimIndent()
                } else {
                    val challengeList = activeChallenges.joinToString("\n") { c ->
                        val progress = if (c.targetAmount > 0) ((c.currentAmount / c.targetAmount) * 100).toInt() else 0
                        val remaining = (c.targetAmount - c.currentAmount).coerceAtLeast(0.0)
                        val remText = if (remaining > 0) " (${String.format(Locale.US, "%,.0f", remaining)} MMK to go)" else " (Completed! 🎉)"
                        "• ${c.title}: ${String.format(Locale.US, "%,.0f", c.currentAmount)} / ${String.format(Locale.US, "%,.0f", c.targetAmount)} MMK ($progress%)$remText"
                    }

                    val formattedSaved = String.format(Locale.US, "%,.0f", totalSaved)
                    val formattedTarget = String.format(Locale.US, "%,.0f", totalTarget)

                    val coachTip = when {
                        overallProgress >= 80 -> "Fantastic work! You are on the home stretch to completing your targets."
                        overallProgress >= 40 -> "Great steady progress! Consistency is your greatest financial superpower."
                        else -> "Building momentum! Even small, frequent deposits keep you moving in the right direction."
                    }

                    """
                    🏆 Savings & Challenges Report

                    💰 Total Savings Progress:
                    • $formattedSaved / $formattedTarget MMK ($overallProgress% complete across ${activeChallenges.size} active challenges)

                    🎯 Active Challenges:
                    $challengeList

                    💡 Coach Tip:
                    $coachTip
                    """.trimIndent()
                }
            }
        } catch (e: Exception) {
            if (language == "my") {
                "ငွေစုစာရင်း အချက်အလက် ရယူ၍ မရပါ။ နောက်မှ ထပ်ကြိုးစားကြည့်ပါ။"
            } else {
                "Unable to generate savings report right now. Please try again later."
            }
        }
    }

    fun buildSavingAdviceResponse(userId: String, query: String, language: String): String {
        return Companion.buildSavingAdviceResponse(userId, query, language)
    }

    suspend fun buildNewsRecapResponse(userId: String, query: String, language: String): String {
        val currentTime = System.currentTimeMillis()
        val newsList = if (cachedNews.isNotEmpty() && (currentTime - lastNewsFetchTime < newsCacheTtlMs)) {
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

        return Companion.buildNewsRecapResponse(query, language, newsList)
    }

    suspend fun buildMarketPriceResponse(userId: String, query: String, language: String): String {
        val q = query.lowercase(Locale.US).trim()
        val isGold = q.contains("gold") || q.contains("ရွှေ") || q.contains("gld") || q.contains("xau")
        val isCrypto = q.contains("crypto") || q.contains("bitcoin") || q.contains("btc") ||
                q.contains("eth") || q.contains("sol") || q.contains("ခရစ်ပတို") || q.contains("ဘစ်ကွိုင်")

        val holdings = try {
            investmentRepository.getHoldingsOnce(userId)
                .filter { !it.isStoppedCompat }
        } catch (_: Exception) {
            emptyList()
        }

        // Fetch quotes with robust defaults if offline
        val goldSpotQuote = if (isGold) {
            try { marketApiService.getStockQuote("GC=F").getOrNull() } catch (_: Exception) { null }
        } else null

        val gldQuote = if (isGold) {
            try { marketApiService.getStockQuote("GLD").getOrNull() } catch (_: Exception) { null }
        } else null

        val btcQuote = if (isCrypto) {
            try { marketApiService.getStockQuote("BTC-USD").getOrNull() } catch (_: Exception) { null }
        } else null

        return Companion.buildMarketPriceResponse(
            query = query,
            language = language,
            isGold = isGold,
            isCrypto = isCrypto,
            holdings = holdings,
            goldSpotPrice = goldSpotQuote?.livePrice ?: 4476.60,
            goldSpotChange = goldSpotQuote?.change24h ?: -1.39,
            gldPrice = gldQuote?.livePrice ?: 406.77,
            gldChange = gldQuote?.change24h ?: -0.84,
            btcPrice = btcQuote?.livePrice ?: 79980.0,
            btcChange = btcQuote?.change24h ?: 0.5
        )
    }

    suspend fun buildInvestmentAdviceResponse(userId: String, query: String, language: String): String {
        val holdings = try {
            investmentRepository.getHoldingsOnce(userId)
                .filter { !it.isStoppedCompat }
        } catch (_: Exception) {
            emptyList()
        }
        val holdingSummary = if (holdings.isNotEmpty()) {
            holdings.take(3).joinToString(", ") { holding ->
                val ticker = holding.displayTicker.ifBlank { holding.symbol }
                "$ticker (${holding.units} units)"
            }
        } else ""

        val now = LocalDate.now()
        val yearMonth = YearMonth.now()
        val yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val budget = try { budgetRepository.getBudget(userId, yearMonthStr).firstOrNull() } catch (_: Exception) { null }
        val expenses = try { expenseRepository.getExpensesForMonth(userId, yearMonthStr).firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
        val budgetLimit = budget?.limit ?: 0.0
        val totalSpent = expenses.sumOf { it.amount }
        val remainingBudget = budgetLimit - totalSpent
        val daysLeft = maxOf(1, yearMonth.lengthOfMonth() - now.dayOfMonth)

        val q = query.lowercase(Locale.US).trim()
        val isGold = q.contains("gold") || q.contains("ရွှေ") || q.contains("gld") || q.contains("xau")
        val isSol = q.contains("sol") || q.contains("solana") || q.contains("ဆိုလာနာ")
        val isEth = q.contains("eth") || q.contains("ethereum")
        val isBtc = q.contains("btc") || q.contains("bitcoin") || q.contains("ဘစ်ကွိုင်")
        val isBtcVsSol = (isBtc && isSol) || q.contains("btc vs sol") || q.contains("sol vs btc") ||
                q.contains("btc နဲ့ sol") || q.contains("sol နဲ့ btc") || q.contains("btc နှင့် sol") || q.contains("sol နှင့် btc")
        val isTesla = q.contains("tesla") || q.contains("tsla") || q.contains("တက်စလာ")
        val isCrypto = q.contains("crypto") || q.contains("ခရစ်ပတို") || isSol || isEth || isBtc || isBtcVsSol

        val cryptoSymbol = when {
            isBtcVsSol -> "BTC"
            isSol -> "SOL"
            isEth -> "ETH"
            isBtc -> "BTC"
            else -> "BTC"
        }
        val cryptoTicker = when {
            isSol && !isBtcVsSol -> "SOL-USD"
            isEth -> "ETH-USD"
            else -> "BTC-USD"
        }
        val fallbackCryptoPrice = when {
            isSol && !isBtcVsSol -> 185.0
            isEth -> 3100.0
            else -> 79980.0
        }

        val cryptoQuote = if (isCrypto) {
            try { marketApiService.getStockQuote(cryptoTicker).getOrNull() } catch (_: Exception) { null }
        } else null
        val cryptoPrice = if (cryptoQuote != null && cryptoQuote.livePrice > 0.0) cryptoQuote.livePrice else fallbackCryptoPrice
        val cryptoChange = cryptoQuote?.change24h

        val btcPrice = if (isBtcVsSol) {
            val btcQuote = try { marketApiService.getStockQuote("BTC-USD").getOrNull() } catch (_: Exception) { null }
            if (btcQuote != null && btcQuote.livePrice > 0.0) btcQuote.livePrice else 79980.0
        } else cryptoPrice
        val btcChange = if (isBtcVsSol) {
            val btcQuote = try { marketApiService.getStockQuote("BTC-USD").getOrNull() } catch (_: Exception) { null }
            btcQuote?.change24h
        } else cryptoChange

        val solPrice = if (isBtcVsSol) {
            val solQuote = try { marketApiService.getStockQuote("SOL-USD").getOrNull() } catch (_: Exception) { null }
            if (solQuote != null && solQuote.livePrice > 0.0) solQuote.livePrice else 185.0
        } else if (isSol) cryptoPrice else null
        val solChange = if (isBtcVsSol) {
            val solQuote = try { marketApiService.getStockQuote("SOL-USD").getOrNull() } catch (_: Exception) { null }
            solQuote?.change24h
        } else if (isSol) cryptoChange else null

        val goldQuote = if (isGold) {
            try { marketApiService.getStockQuote("GC=F").getOrNull() } catch (_: Exception) { null }
        } else null
        val goldPrice = if (goldQuote != null && goldQuote.livePrice > 0.0) goldQuote.livePrice else 2890.0
        val goldChange = goldQuote?.change24h

        return Companion.buildInvestmentAdviceResponse(
            userId = userId,
            query = query,
            language = language,
            existingHoldingSummary = holdingSummary,
            remainingBudgetMmk = remainingBudget,
            totalSpentMmk = totalSpent,
            budgetLimitMmk = budgetLimit,
            daysLeft = daysLeft,
            cryptoSymbol = cryptoSymbol,
            cryptoPrice = cryptoPrice,
            cryptoChange = cryptoChange,
            btcPrice = btcPrice,
            btcChange = btcChange,
            solPrice = solPrice,
            solChange = solChange,
            goldPrice = goldPrice,
            goldChange = goldChange
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
                .joinToString(", ") { "${it.key}: ${String.format(Locale.US, "%,.0f", it.value)} MMK" }

            val recentExpenses = expenses
                .sortedByDescending { it.createdAt }
                .take(3)
                .joinToString(", ") { "${it.category}: ${String.format(Locale.US, "%,.0f", it.amount)} MMK" }

            // Build challenge context
            val challengeContext = if (challenges.isNotEmpty()) {
                val challengeList = challenges.joinToString("\n") { challenge ->
                    val progress = if (challenge.targetAmount > 0) {
                        ((challenge.currentAmount / challenge.targetAmount) * 100).toInt()
                    } else 0
                    "- ${challenge.title}: ${String.format(Locale.US, "%,.0f", challenge.currentAmount)}/${String.format(Locale.US, "%,.0f", challenge.targetAmount)} MMK ($progress% complete, ${challenge.template})"
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
                val holdingLines = mutableListOf<String>()
                for (holding in holdings.take(5)) {
                    val ticker = holding.displayTicker.ifBlank { holding.symbol }
                    val buyPrice = String.format(Locale.US, "%.2f", holding.buyPrice)
                    val livePriceData = try {
                        marketApiService.getStockQuote(holding.symbol).getOrNull()
                    } catch (_: Exception) { null }
                    val currentPriceText = if (livePriceData != null && livePriceData.livePrice > 0.0) {
                        val lp = String.format(Locale.US, "%.2f", livePriceData.livePrice)
                        val chg = String.format(Locale.US, "%+.2f%%", livePriceData.change24h)
                        " | Current Market Price: $$lp USD ($chg 24h)"
                    } else ""
                    val assetClass = when (holding.type.lowercase(Locale.US)) {
                        "crypto" -> "Cryptocurrency"
                        "commodity" -> "Commodity / Gold"
                        "stock" -> "Stock Equity"
                        else -> holding.type
                    }
                    holdingLines.add("- $ticker: ${holding.units} units @ buy price $$buyPrice USD ($assetClass)$currentPriceText")
                }
                val holdingList = holdingLines.joinToString("\n")
                """
                Portfolio Summary (${holdings.size} active holdings):
                Total Cost Basis: $$formattedCostBasis USD
                Top Holdings:
                $holdingList
                """.trimIndent()
            } else {
                "Portfolio: No active holdings"
            }

            // Benchmark Asset Prices context
            val goldQuote = try { marketApiService.getStockQuote("GC=F").getOrNull() } catch (_: Exception) { null }
            val gldQuote = try { marketApiService.getStockQuote("GLD").getOrNull() } catch (_: Exception) { null }
            val btcQuote = try { marketApiService.getStockQuote("BTC-USD").getOrNull() } catch (_: Exception) { null }

            val benchmarkContext = buildString {
                appendLine("Benchmark Asset Prices:")
                val spotGold = goldQuote?.livePrice ?: 4476.60
                val spotGoldChg = goldQuote?.change24h ?: -1.39
                appendLine("- Spot Gold (GC=F): \$${String.format(Locale.US, "%,.2f", spotGold)} USD / troy oz (${String.format(Locale.US, "%+.2f%%", spotGoldChg)} 24h)")

                val gldVal = gldQuote?.livePrice ?: 406.77
                val gldChg = gldQuote?.change24h ?: -0.84
                appendLine("- SPDR Gold Shares (GLD ETF): \$${String.format(Locale.US, "%,.2f", gldVal)} USD (${String.format(Locale.US, "%+.2f%%", gldChg)} 24h)")

                val btcVal = btcQuote?.livePrice ?: 79980.0
                val btcChg = btcQuote?.change24h ?: 0.5
                appendLine("- Bitcoin (BTC): \$${String.format(Locale.US, "%,.2f", btcVal)} USD (${String.format(Locale.US, "%+.2f%%", btcChg)} 24h)")
            }.trimEnd()

            return """
                [HIDDEN SYSTEM CONTEXT - DO NOT MENTION THIS BLOCK TO THE USER]
                Today's Date: ${now.format(DateTimeFormatter.ISO_LOCAL_DATE)}
                Current Month: ${now.month.name} ${now.year}
                Monthly Budget: ${String.format(Locale.US, "%,.0f", budgetLimit)} MMK
                Total Spent: ${String.format(Locale.US, "%,.0f", totalSpent)} MMK
                Remaining Budget: ${String.format(Locale.US, "%,.0f", remaining)} MMK
                Days Left in Month: $daysLeft
                Top Categories: ${topCategories.ifBlank { "None" }}
                Recent Expenses: ${recentExpenses.ifBlank { "None" }}
                $challengeContext
                
                $portfolioContext
                
                $benchmarkContext
                
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

data class ParsedSavingGoal(
    val item: String = "",
    val amount: Double = 0.0,
    val targetDays: Int? = null,
    val timeframeText: String = ""
)
