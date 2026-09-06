package com.savingcoach.app.ai

import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.savingcoach.app.data.model.ParsedExpense
import java.time.LocalDate
import java.util.Locale

@Singleton
class AiChatRepository @Inject constructor(
    private val proxyService: GeminiProxyService,
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val localHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var hasLoadedFromFirestore = false

    override fun getChatHistory(userId: String): Flow<List<ChatMessage>> {
        // Load from Firestore once
        if (!hasLoadedFromFirestore) {
            hasLoadedFromFirestore = true
            loadFromFirestore(userId)
        }
        return localHistory.map { messages ->
            messages.filter { it.userId == userId }
                .sortedBy { it.timestamp }
        }
    }

    private fun loadFromFirestore(userId: String) {
        // Load in background - will update localHistory when complete
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("chatMessages")
                    .orderBy("timestamp")
                    .get()
                    .await()

                val rawMessages = snapshot.toObjects(ChatMessage::class.java)
                val messages = rawMessages.map { msg ->
                    if (msg.role == "ai") {
                        val cleaned = cleanThinking(msg.content)
                        val repairedExpenses = if (msg.parsedExpenses == null && msg.parsedExpense != null) {
                            val exp = msg.parsedExpense
                            val rawCandidate = if (exp.item.isNotBlank()) exp.item else exp.merchant
                            if (rawCandidate.isNotBlank() && (rawCandidate.contains(" and ", ignoreCase = true) || rawCandidate.contains(" နဲ့ "))) {
                                val combinedQuery = "${exp.amount.toLong()} for $rawCandidate"
                                val extracted = extractFallbackExpenses(combinedQuery, exp.language)
                                if (extracted.size > 1) extracted else null
                            } else null
                        } else null

                        val finalExpenses = repairedExpenses ?: msg.parsedExpenses
                        val finalExpense = repairedExpenses?.firstOrNull() ?: msg.parsedExpense
                        val finalContent = if (cleaned.isNotBlank()) {
                            cleaned
                        } else if (finalExpenses != null && finalExpenses.size > 1) {
                            val items = finalExpenses.joinToString(", ") { "${it.item.ifBlank { it.category }} (${it.amount.toLong()} ${it.currency})" }
                            "I've noted your expenses for $items. Please confirm below."
                        } else cleaned

                        msg.copy(
                            content = finalContent,
                            parsedExpense = finalExpense,
                            parsedExpenses = finalExpenses
                        )
                    } else msg
                }
                localHistory.value = messages
            } catch (_: Exception) {
                // Firestore load failed, start with empty history
            }
        }
    }

    override suspend fun saveMessage(userId: String, message: ChatMessage) {
        // Save to local state
        localHistory.value = localHistory.value + message

        // Save to Firestore
        try {
            firestore.collection("users")
                .document(userId)
                .collection("chatMessages")
                .add(message.copy(userId = userId))
                .await()
        } catch (_: Exception) {
            // Firestore save failed, but local state is updated
        }
    }

    override suspend fun updateMessage(userId: String, message: ChatMessage) {
        // Update local state
        localHistory.value = localHistory.value.map { if (it.id == message.id) message else it }

        // Update in Firestore
        try {
            val querySnapshot = firestore.collection("users")
                .document(userId)
                .collection("chatMessages")
                .whereEqualTo("id", message.id)
                .get()
                .await()
                
            for (doc in querySnapshot.documents) {
                doc.reference.set(message).await()
            }
        } catch (_: Exception) {
            // Firestore update failed, but local state is updated
        }
    }

    /**
     * Send a message to the AI and get a response.
     * Returns the AI's reply as a ChatMessage.
     */
    override suspend fun sendToAi(
        userId: String,
        userMessage: String,
        systemPrompt: String?
    ): Result<ChatMessage> {
        // Filter out JSON messages, clean historical thinking, and sort chronologically
        val cleanHistory = localHistory.value
            .filter { it.userId == userId && !it.content.contains("{") }
            .map { msg ->
                if (msg.role == "ai") {
                    msg.copy(content = cleanThinking(msg.content))
                } else msg
            }
            .filter { it.content.isNotBlank() }
            .sortedBy { it.timestamp }

        // Token-aware truncation: preserve recent messages within token budget
        var accumulatedTokens = estimateTokens(userMessage)
        val recentHistory = cleanHistory.reversed().takeWhile { msg ->
            val msgTokens = estimateTokens(msg.content)
            if (accumulatedTokens + msgTokens <= MAX_INPUT_TOKENS) {
                accumulatedTokens += msgTokens
                true
            } else {
                false
            }
        }.reversed()

        val recentMessages = recentHistory + ChatMessage(
            id = "temp_${System.currentTimeMillis()}",
            userId = userId,
            role = "user",
            content = userMessage,
            timestamp = System.currentTimeMillis()
        )

        return proxyService.chat(recentMessages, systemPrompt).map { reply ->
            var finalReply = reply
            val parsedExpensesList = mutableListOf<ParsedExpense>()
            var msgType = "advice"

            val hasMyanmarText = userMessage.contains(Regex("[\\u1000-\\u109F]"))
            val detectedLang = if (hasMyanmarText) "my" else "en"

            val expenseRegex = "\\[EXPENSE_DATA\\](.*?)\\[/EXPENSE_DATA\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matches = expenseRegex.findAll(reply).toList()
            if (matches.isNotEmpty()) {
                for (match in matches) {
                    val jsonStr = match.groupValues[1].trim()
                    val list = parseExpenseDataList(jsonStr, detectedLang, userMessage)
                    parsedExpensesList.addAll(list)
                }
                finalReply = expenseRegex.replace(reply, "").trim()
                if (parsedExpensesList.isNotEmpty()) {
                    msgType = if (parsedExpensesList.any { it.isChallenge }) "challenge" else "expense"
                }
            }

            // Client-side fallback: If AI failed to output [EXPENSE_DATA], or missed items from a multi-item request
            val fallbackList = extractFallbackExpenses(userMessage, detectedLang)
            if (parsedExpensesList.isEmpty()) {
                parsedExpensesList.addAll(fallbackList)
            } else if (fallbackList.size > parsedExpensesList.size) {
                parsedExpensesList.clear()
                parsedExpensesList.addAll(fallbackList)
                finalReply = ""
            }
            if (parsedExpensesList.isNotEmpty()) {
                msgType = if (parsedExpensesList.any { it.isChallenge }) "challenge" else "expense"
            }

            finalReply = cleanThinking(finalReply)

            // Language consistency & Hallucination detection: If user typed Burmese, reject corrupted scripts, hybrid tokens, or uninvited English words
            if (detectedLang == "my") {
                if (isCorruptedBurmeseResponse(finalReply, userMessage)) {
                    finalReply = ""
                }
            } else {
                // In English mode, reject foreign scripts
                if (Regex("[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F\\u0E00-\\u0E7F]").containsMatchIn(finalReply)) {
                    finalReply = ""
                }
            }

            // If the model output consisted entirely of internal thinking, provide a clean acknowledgment
            if (finalReply.isBlank()) {
                finalReply = when {
                    parsedExpensesList.size > 1 -> {
                        val allChallenges = parsedExpensesList.all { it.isChallenge }
                        val allExpenses = parsedExpensesList.all { !it.isChallenge }
                        if (detectedLang == "my") {
                            val items = parsedExpensesList.joinToString(", ") {
                                if (it.isChallenge) {
                                    val title = it.challengeTitle.ifBlank { it.item }
                                    val amt = if (it.amount > 0) " (${it.amount.toLong()} ကျပ်)" else ""
                                    "$title$amt"
                                } else {
                                    val name = if (it.item.isNotBlank()) it.item else com.savingcoach.app.ui.chat.CategoryResolver.toBurmeseName(it.category)
                                    val amt = if (it.amount > 0) " (${it.amount.toLong()} ကျပ်)" else ""
                                    "$name$amt"
                                }
                            }
                            if (allChallenges) {
                                "$items အတွက် ငွေစုရန် မှတ်သားထားပါတယ်။ အောက်ပါ Card များတွင် အတည်ပြုပေးပါ။"
                            } else if (allExpenses) {
                                "$items အတွက် မှတ်သားထားပါတယ်။ အောက်ပါ Card များတွင် အတည်ပြုပေးပါ။"
                            } else {
                                "$items အတွက် မှတ်သားထားပါတယ်။ အောက်ပါ Card များတွင် အတည်ပြုပေးပါ။"
                            }
                        } else {
                            val items = parsedExpensesList.joinToString(", ") {
                                val name = if (it.isChallenge) it.challengeTitle.ifBlank { it.item } else it.item.ifBlank { it.category }
                                val amt = if (it.amount > 0) " (${it.amount.toLong()} ${it.currency})" else ""
                                "$name$amt"
                            }
                            if (allChallenges) {
                                "I've prepared your deposits for $items. Please confirm below."
                            } else {
                                "I've noted your requests for $items. Please confirm below."
                            }
                        }
                    }
                    parsedExpensesList.isNotEmpty() && parsedExpensesList.first().isChallenge -> {
                        val title = parsedExpensesList.first().challengeTitle.ifBlank { "saving challenge" }
                        if (detectedLang == "my") {
                            "$title အတွက် ငွေစုရန် မှတ်သားထားပါတယ်။ အောက်ပါ Card တွင် အတည်ပြုပေးပါ။"
                        } else {
                            "I've prepared your deposit for $title. Please confirm below."
                        }
                    }
                    parsedExpensesList.isNotEmpty() -> {
                        val exp = parsedExpensesList.first()
                        val cat = exp.category.ifBlank { "Expense" }
                        val amtStr = if (exp.amount > 0) " ${exp.amount.toLong()} ${exp.currency}" else ""
                        val targetName = exp.item.ifBlank {
                            if (detectedLang == "my") {
                                com.savingcoach.app.ui.chat.CategoryResolver.toBurmeseName(cat)
                            } else {
                                cat
                            }
                        }
                        if (detectedLang == "my") {
                            "$targetName အတွက်$amtStr မှတ်သားထားပါတယ်။ အောက်ပါ Card တွင် အတည်ပြုပေးပါ။"
                        } else {
                            "I've noted your expense for $targetName$amtStr. Please confirm below."
                        }
                    }
                    else -> {
                        if (AiFinanceAssistant.isInvestmentAdviceQuery(userMessage)) {
                            AiFinanceAssistant.buildInvestmentAdviceResponse(
                                userId = userId,
                                query = userMessage,
                                language = detectedLang
                            )
                        } else if (detectedLang == "my") {
                            "နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ။"
                        } else {
                            "I'm here to help with your finances. What would you like to know?"
                        }
                    }
                }
            }

            val primaryExpense = parsedExpensesList.firstOrNull()
            val multipleExpenses = if (parsedExpensesList.size > 1) parsedExpensesList else null

            ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                userId = userId,
                role = "ai",
                content = finalReply,
                timestamp = System.currentTimeMillis(),
                type = msgType,
                parsedExpense = primaryExpense,
                parsedExpenses = multipleExpenses
            )
        }
    }

    companion object {
        private const val MAX_INPUT_TOKENS = 3000
        const val BURMESE_CLASSIFIERS = "လုံး|ခု|စီး|ထုပ်|ဘူး|ဗူး|တွဲ|ပွဲ|ထည်|ချောင်း|ကောင်|ယောက်|ခွက်|ပြား|ချပ်|လိပ်|ပုလင်း|ကီလို|ပိဿာ|ကျပ်သား"
        const val BURMESE_VERBS_PATTERN = "(?:ကျပ်|ကျ|ks|mmk|ဖိုး|တန်(?:ဖိုး)?|ကုန်(?:တယ်|ခဲ့(?:တယ်|တာ)?|သွားတယ်|တာ)?|ကျ(?:တယ်|ခဲ့(?:တယ်|တာ)?|သွားတယ်|တာ)?|ပေး(?:ရတယ်|ခဲ့ရတယ်|လိုက်ရတယ်|ရတာ)|ရှင်း(?:တယ်|ခဲ့|လိုက်တယ်|တာ)?|ဝယ်(?:တယ်|ခဲ့(?:တယ်|တာ)?|လိုက်(?:တယ်|တာ)|ထား(?:တယ်|တာ)?|တာ)?|သုံး(?:တယ်|ခဲ့(?:တယ်|တာ)?|လိုက်(?:တယ်|တာ)|တာ)?|(?:မနက်စာ|နေ့လယ်စာ|ညစာ)?\\s*(?:စား|သောက်)(?:တယ်|ခဲ့(?:တယ်|တာ)?|လိုက်(?:တယ်|တာ)|ထား(?:တယ်|တာ)?|တာ)?|(?:မနက်စာ|နေ့လယ်စာ|ညစာ)?\\s*(?:အ)?တွက်|တဲ့|ကွာ|ဗျ|ဗျာ|ရှင့်|နော်|လေ|ပေါ့)"
        const val BURMESE_CHALLENGE_VERBS_PATTERN = "(?:စု|ထည့်)(?:ရ)?(?:ဦး|ဦး)?(?:မယ်(?:မလား|လား)?|တယ်|ခဲ့(?:တယ်)?|လိုက်(?:တယ်)?|ထား(?:တယ်)?|တာ|ချင်(?:တယ်)?|မလား|လား|ဖို့|မလို့|တော့မယ်)"

        fun parseQtyWord(qtyWord: String): Int {
            val clean = qtyWord.trim()
            val digitsOnly = clean.filter { it.isDigit() }
            if (digitsOnly.isNotBlank()) {
                return digitsOnly.toIntOrNull() ?: 1
            }
            return when {
                clean.contains("ဆယ်") -> 10
                clean.contains("ကိုး") -> 9
                clean.contains("ရှစ်") -> 8
                clean.contains("ခုနစ်") || clean.contains("ခုနှစ်") -> 7
                clean.contains("ခြောက်") -> 6
                clean.contains("ငါး") -> 5
                clean.contains("လေး") -> 4
                clean.contains("သုံး") -> 3
                clean.contains("နှစ်") -> 2
                clean.contains("တစ်") -> 1
                else -> 1
            }
        }

        fun parseExpenseDataList(rawJson: String, detectedLang: String, userMessage: String = ""): List<ParsedExpense> {
            return try {
                val cleanJson = rawJson
                    .replace(Regex("^```[a-zA-Z]*\\s*"), "")
                    .replace(Regex("\\s*```$"), "")
                    .trim()
                val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(cleanJson)
                val initialList = when (jsonElement) {
                    is JsonArray -> {
                        jsonElement.mapNotNull { item ->
                            if (item is JsonObject) {
                                parseSingleExpenseObject(item, detectedLang, userMessage)
                            } else null
                        }
                    }
                    is JsonObject -> {
                        listOfNotNull(parseSingleExpenseObject(jsonElement, detectedLang, userMessage))
                    }
                    else -> emptyList()
                }

                // If user wrote in Burmese, reconcile items: LLMs can hallucinate or mistranslate Burmese items
                // into English (e.g. translating "ကန်စွန်းရွက်နှစ်စီး" into "canned fish 2 cans" or "ဓာတ်မီး" into "laptop").
                if (detectedLang == "my" && userMessage.isNotBlank() && initialList.isNotEmpty()) {
                    val fallbackExpenses = extractFallbackExpenses(userMessage, detectedLang)
                    val userHasEnglish = userMessage.contains(Regex("[a-zA-Z]"))

                    initialList.mapIndexed { index, exp ->
                        if (exp.isChallenge) return@mapIndexed exp
                        val itemHasEnglish = exp.item.contains(Regex("[a-zA-Z]"))
                        val itemInUserMessage = exp.item.isNotBlank() && userMessage.contains(exp.item, ignoreCase = true)

                        if ((!userHasEnglish && itemHasEnglish) || (!itemInUserMessage && fallbackExpenses.isNotEmpty())) {
                            val matchedFallback = fallbackExpenses.firstOrNull { Math.abs(it.amount - exp.amount) < 1.0 }
                                ?: if (index in fallbackExpenses.indices) fallbackExpenses[index] else null

                            if (matchedFallback != null && matchedFallback.item.isNotBlank()) {
                                exp.copy(
                                    item = matchedFallback.item,
                                    category = if ((exp.category == "Other" || exp.category.isBlank()) && matchedFallback.category != "Other") matchedFallback.category else exp.category
                                )
                            } else {
                                exp
                            }
                        } else {
                            exp
                        }
                    }
                } else {
                    initialList
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    fun parseExpenseData(rawJson: String, detectedLang: String, userMessage: String = ""): ParsedExpense? {
        return parseExpenseDataList(rawJson, detectedLang, userMessage).firstOrNull()
    }

    private fun parseSingleExpenseObject(
        jsonElement: JsonObject,
        detectedLang: String,
        userMessage: String = ""
    ): ParsedExpense? {
        return try {
            fun getString(vararg keys: String): String {
                for (key in keys) {
                    val el = jsonElement[key]
                    if (el != null) {
                        val str = (el as? JsonPrimitive)?.content?.trim()
                        if (!str.isNullOrBlank()) return str
                    }
                }
                return ""
            }

            fun getDouble(vararg keys: String): Double {
                for (key in keys) {
                    val el = jsonElement[key]
                    if (el != null) {
                        val prim = el as? JsonPrimitive
                        val num = prim?.content?.toDoubleOrNull()
                        if (num != null) return num
                    }
                }
                return 0.0
            }

            fun getBoolean(vararg keys: String): Boolean {
                for (key in keys) {
                    val el = jsonElement[key]
                    if (el != null) {
                        val prim = el as? JsonPrimitive
                        val b = prim?.content?.toBooleanStrictOrNull()
                        if (b != null) return b
                    }
                }
                return false
            }

            val isChallenge = getBoolean("isChallenge", "is_challenge") ||
                    getString("action") in listOf("prompt_challenge_confirmation", "mark_challenge_saving")

            var challengeTitle = getString("challengeTitle", "challenge_title", "challenge_name", "challengeName", "challenge", "title", "name")
            val merchant = getString("merchant", "vendor", "place", "shop")
            var category = getString("category", "type").ifBlank { "Other" }
            val amount = getDouble("amount", "cost", "price", "value")
            var date = getString("date", "datetime")
            val action = getString("action").ifBlank { if (isChallenge) "prompt_challenge_confirmation" else "log_expense" }
            var item = getString("item", "description")
            val currency = getString("currency").ifBlank { "MMK" }

            // Normalize category if expense
            if (!isChallenge) {
                category = com.savingcoach.app.ui.chat.CategoryResolver.resolve(category, emptyList())?.name ?: category
            }

            // Fallback item extraction if item is blank (only if userMessage is a simple single-item message)
            if (!isChallenge && item.isBlank()) {
                val fallbackItem = extractItemFromMessage(userMessage)
                if (fallbackItem.isNotBlank() && !fallbackItem.contains(" and ", ignoreCase = true) && !fallbackItem.contains(" နဲ့ ")) {
                    item = fallbackItem
                } else if (merchant.isNotBlank()) {
                    item = merchant
                }
            }

            // Sanitize date: if "YYYY-MM-DD", empty, or placeholder, use today's date
            if (date.isBlank() || date.contains("YYYY", ignoreCase = true) || date.length < 8) {
                date = java.time.LocalDate.now().toString()
            }

            // Fallback for challengeTitle if isChallenge is true but challengeTitle is still blank
            if (isChallenge && challengeTitle.isBlank()) {
                challengeTitle = merchant
            }

            ParsedExpense(
                merchant = merchant,
                amount = amount,
                category = category,
                date = date,
                language = detectedLang,
                isChallenge = isChallenge,
                challengeTitle = challengeTitle,
                action = action,
                item = item,
                currency = currency
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isCorruptedBurmeseResponse(reply: String, userMessage: String): Boolean {
        if (reply.isBlank()) return false

        // 1. Foreign non-Burmese non-Latin scripts (Korean Hangul, Thai, Chinese, Japanese)
        if (Regex("[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F\\u0E00-\\u0E7F\\u4E00-\\u9FFF\\u3040-\\u309F\\u30A0-\\u30FF]").containsMatchIn(reply)) {
            return true
        }

        // 2. Burmese-English hybrid hyphenated tokens (e.g. "စုရ-cache", "အ-cache")
        if (Regex("[\\u1000-\\u109F]+-[a-zA-Z]+|[a-zA-Z]+-[\\u1000-\\u109F]+").containsMatchIn(reply)) {
            return true
        }

        // 3. Burmese text must have Burmese characters
        val hasBurmese = reply.any { it.code in 0x1000..0x109F }
        if (!hasBurmese) {
            return true
        }

        // 4. Hallucinated English words in Burmese mode
        val userEnglishWords = Regex("[a-zA-Z]{2,}")
            .findAll(userMessage)
            .map { it.value.lowercase(Locale.US) }
            .toSet()

        val allowedTokens = setOf(
            "mmk", "usd", "ks", "k", "ai", "card", "cards", "app", "id", "ok",
            "challenge", "challenges", "saving", "coach",
            // Financial & Investment terminology
            "gold", "gld", "xau", "etf", "etfs", "dca", "inflation", "hedge", "crypto",
            "cryptocurrency", "cryptocurrencies", "bitcoin", "btc", "eth", "ethereum",
            "sol", "solana", "stock", "stocks", "equity", "equities", "share", "shares",
            "market", "fund", "funds", "dollar", "percent", "roi", "note", "risk",
            "wallet", "cash", "bank", "banks", "asset", "assets", "portfolio",
            "investment", "invest", "investing", "gemini", "google", "finnhub",
            "academy", "rate", "rates", "budget", "emergency", "plan",
            "spot", "quote", "quotes", "benchmark", "balance", "balances",
            "unit", "units", "yield", "yields", "trade", "trading", "trader",
            "fomo", "volatile", "volatility", "pnl", "diversification", "diversify",
            // Stock companies & indices
            "tesla", "tsla", "apple", "aapl", "nvidia", "nvda", "google", "microsoft", "amazon",
            "tech", "ev", "nasdaq", "sp500", "bluechip", "index", "dividend", "dividends",
            // Crypto tokens & terms
            "bnb", "binance", "doge", "xrp", "ada", "cardano", "token", "tokens", "coin", "coins",
            "altcoin", "altcoins", "layer", "blockchain", "blockchains", "smart", "contract", "contracts",
            "speed", "tps", "fee", "fees", "gas", "ecosystem", "ecosystems", "defi", "nft", "nfts",
            "staking", "halving", "marketcap", "digital", "currency", "network", "platform",
            "high", "reward", "safe", "core", "reserve", "growth", "value", "bull", "bear"
        )

        // Strip parenthesized English annotations, e.g. "ရွှေ (Gold)", "ငွေကြေးဖောင်းပွမှု (Inflation)", "(DCA)"
        // PromptBuilder explicitly instructs: "If you need to mention an English term, put it in parentheses"
        val textWithoutParentheses = reply.replace(Regex("\\([a-zA-Z0-9\\s,./\\-–—]+\\)"), " ")

        val uninvitedEnglishWords = Regex("[a-zA-Z]{2,}")
            .findAll(textWithoutParentheses)
            .map { it.value.lowercase(Locale.US) }
            .filter { it !in allowedTokens && it !in userEnglishWords }
            .toList()

        if (uninvitedEnglishWords.isNotEmpty()) {
            val hasSevereHallucinations = uninvitedEnglishWords.any { word ->
                word in setOf(
                    "monasterize", "cache", "achievement", "rule", "rules",
                    "instruction", "prompt", "user", "message", "expense", "expenses",
                    "breaking", "down", "mentioning", "draft", "thinking", "response",
                    "analyze", "analyzing", "required", "fields", "determine", "formulate", "extraction"
                )
            }
            if (hasSevereHallucinations || uninvitedEnglishWords.size > 15) {
                return true
            }
        }

        return false
    }

    fun cleanThinking(text: String): String {
        if (text.isBlank()) return ""

        // 1. Remove XML/markdown thought tags
        val cleaned = text.replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("(?s)\\[think\\].*?\\[/think\\]"), "")
            .replace(Regex("(?s)```thought.*?```"), "")
            .trim()

        // Reject foreign corrupted scripts (Korean/Hangul) or hybrid BPE tokens
        if (Regex("[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F]").containsMatchIn(cleaned) ||
            Regex("[\\u1000-\\u109F]+-[a-zA-Z]+|[a-zA-Z]+-[\\u1000-\\u109F]+").containsMatchIn(cleaned)) {
            val expenseMatch = Regex("\\[EXPENSE_DATA\\][\\s\\S]*?\\[/EXPENSE_DATA\\]").find(cleaned)
            return expenseMatch?.value ?: ""
        }

        // 2. Check for explicit response headers (e.g. "Response:", "Draft response:", "Draft - Mental Refinement:")
        val explicitResponseMatch = Regex("(?i)(?:\\d+\\.\\s*)?\\*{0,2}(?:Draft\\s*[-–]\\s*Mental Refinement|Mental Refinement|Draft response|Conversational response|Final response|Response|Answer)\\*{0,2}:\\*{0,2}\\s*(?:\\*\\([^\\)]*\\)\\*\\s*)?[\"“]?([\\s\\S]+?)[\"”]?$").find(cleaned)
        if (explicitResponseMatch != null && explicitResponseMatch.groupValues[1].isNotBlank()) {
            val extracted = explicitResponseMatch.groupValues[1].trim().trim('"', '“', '”')
            if (extracted.length > 10) return extracted
        }

        // 2b. Check for draft quotes like: Something like "..."
        val quoteMatch = Regex("(?i)(?:Something like|My response should be|Response would be|I should say|Start with|Something along the lines of)\\s*[\"“]([\\s\\S]+?)[\"”]").find(cleaned)
        if (quoteMatch != null && quoteMatch.groupValues[1].isNotBlank()) {
            val candidate = quoteMatch.groupValues[1].trim()
            if (!candidate.contains("EXPENSE_DATA") && !candidate.contains("hidden context", ignoreCase = true) && !candidate.contains("rules say", ignoreCase = true)) {
                return candidate
            }
        }

        // 3. Aggressive thinking detection - catch ALL internal monologue patterns
        val thinkingPatterns = listOf(
            // Date deduction reasoning
            Regex("(?i)If there are \\d+ days left"),
            Regex("(?i)because \\d+-\\d+=\\d+"),
            Regex("(?i)days have passed,? so today is"),
            Regex("(?i)today is (?:January|February|March|April|May|June|July|August|September|October|November|December) \\d+"),

            // User intent statements
            Regex("(?i)The user (?:is |said |wants |asked |is asking |mentioned |wrote |typed |logging |just said )"),
            Regex("(?i)The user is logging an expense"),
            Regex("(?i)This is (?:an?|another) (?:expense|challenge|income|transaction|saving) (?:logging )?request"),

            // Input analysis and processing steps
            Regex("(?i)(?:Analyze|Analyzing) (?:User|the) Input"),
            Regex("(?i)(?:User|The user) (?:says|said|wants|asked|is asking|mentioned|wrote|typed|logging|just said)"),
            Regex("(?i)(?:This is an?|Another) (?:instruction|request|expense|challenge)"),
            Regex("(?i)The format expected is"),
            Regex("(?i)Identify Required Fields"),
            Regex("(?i)Determine Response Language"),
            Regex("(?i)Formulate Extraction"),
            Regex("(?i)Possible response:"),

            // Model field extraction reasoning patterns
            Regex("(?i)The amount is \\d+"),
            Regex("(?i)category would be"),
            Regex("(?i)merchant is (?:not )?specified"),
            Regex("(?i)date is today's date from context"),
            Regex("(?i)date from context:?"),
            Regex("(?i)from context: \\d{4}-\\d{2}-\\d{2}"),
            Regex("(?i)So this is .+ for \\d+ MMK total"),
            Regex("(?i)So this is mentioning (?:two|three|multiple|several|an?|\\d+) expenses?:?"),
            Regex("(?i)mentioning (?:two|three|multiple|several) expenses?:?"),
            Regex("(?m)^\\s*[\"“].+?[\"”]\\s*=\\s*[\"”].+?[\"”]"),
            Regex("(?i)^The user'?s message:?"),
            Regex("(?i)^Breaking (?:it )?down:?"),
            Regex("(?m)^\\s*[•\\*\\-]\\s*[\"“].+?[\"”]\\s*="),
            Regex("(?m)^\\s*[•\\*\\-]\\s*[\"“].+?[\"”]\\s*:"),
            Regex("(?i)(?:Coffee|Breakfast|Lunch|Dinner) for \\d+ MMK"),
            Regex("(?i)\\(since it's .+\\)"),

            // Candidate reasoning & extraction steps
            Regex("(?i)\\d+\\.\\s*[\"“].+?[\"”].*category"),
            Regex("(?i)\\d+\\.\\s*[\"“].+?[\"”].*item is"),
            Regex("(?i)For\\s+[\"“].+?[\"”]:?"),
            Regex("(?i)This is .+? category"),
            Regex("(?i)category,\\s*item is"),
            Regex("(?i)item is [\"“].+?[\"”]"),
            Regex("(?i)[•\\*\\-\\u2022\\u2023\\u25E6\\u2043\\u2219]?\\s*(?:amount|category|item|merchant|date|currency)\\s*:"),
            Regex("(?i)\\(not (?:specified|mentioned|provided)\\)"),
            Regex("(?i)Wait,? (?:let me|can I|what if|how to)"),
            Regex("(?i)structure for (?:two|multiple|an) expense"),
            Regex("(?i)can I output multiple"),
            Regex("(?i)Can \\[EXPENSE_DATA\\]"),
            Regex("(?i)The prompt (?:says|shows)"),

            // Processing/intent statements
            Regex("(?i)(?:Actually,? wait|Actually,? looking closely|Actually,? let me|Wait,? but (?:the rules|I need))"),
            Regex("(?i)(?:Let me (?:format|parse|analyze|extract|check the rules|think about))"),
            Regex("(?i)(?:I need to (?:output|extract|follow the rules|determine the category|format the JSON))"),

            // Rules/challenge detection
            Regex("(?i)(?:Wait,? but the rules|The rules (?:also )?say|According to (?:the )?rules)"),
            Regex("(?i)The rules say:?"),
            Regex("(?i)Wait,? let me re-read"),
            Regex("(?i)Then (?:at the end|the data block)"),
            Regex("(?i)Also,? the strict prohibition:?"),
            Regex("(?i)Write your natural conversational response first"),
            Regex("(?i)Looking at the hidden context:?"),
            Regex("(?i)So if I add \\d+"),
            Regex("(?i)So my response should be"),
            Regex("(?i)But I need to be careful not to overstep"),
            Regex("(?i)Do NOT automatically save the expense"),
            Regex("(?i)NEVER mix (?:Burmese|English) words"),
            Regex("(?i)Present Situations \\(general knowledge\\)"),
            Regex("(?i)Today's approximate ranges"),
            Regex("(?i)Exchange Rate \\(USD → MMK\\)"),
            Regex("(?i)Something like\\s*[\"“]"),
            Regex("(?i)(?:Challenge action values|CHALLENGE DETECTION|EXPENSE DETECTION)"),
            Regex("(?i)(?:Challenge Title:|challengeTitle:)"),

            // Looking at context
            Regex("(?i)(?:Looking at the (?:hidden context|rules|prompt|instruction))"),
            Regex("(?i)(?:Based on the (?:hidden context|rules|prompt|instruction))"),
            Regex("(?i)(?:Following the (?:EXPENSE|CHALLENGE) rules)"),

            // Structure/output thinking
            Regex("(?i)(?:The structure should be|The structure is)"),
            Regex("(?i)(?:For this request|For this user)"),
            Regex("(?i)(?:JSON structure|JSON block|JSON data)"),
            Regex("(?i)(?:•\\s*(?:amount|category|merchant|date|currency|acknowledge|mention|keep):?)"),
            Regex("(?i)(?:\\d+\\.\\s*(?:amount|category|merchant|date))"),

            // Prompt regurgitation & multi-challenge / mixed thinking
            Regex("(?i)So for this case.*"),
            Regex("(?i)with (?:two|three|multiple|several|\\d+) (?:challenges|expenses).*"),
            Regex("(?i)For the (?:expense|challenge) part.*"),
            Regex("(?i)And for (?:mixed|multiple).*"),
            Regex("(?i)\"?Example for (?:Mixed|Multiple|Challenge|Expense).*"),
            Regex("(?i)^User:\\s*\"?.*"),
            Regex("(?i)^Assistant:\\s*\"?.*"),
            Regex("(?i)I need:\\s*$"),
            Regex("(?i)would be .* category"),

            // Analysis thinking
            Regex("(?i)(?:Active Challenges \\(\\d+\\)):"),
            Regex("(?i)(?:•\\s*\\w+:.*MMK.*complete)"),

            // Leaked prompt directives
            Regex("(?i)(?:What remaining funds and days left mean|Specific daily spending guardrail|Remaining Budget\\s*/|Address the single biggest spending category)")
        )

        val isThinking = thinkingPatterns.any { it.containsMatchIn(cleaned) }

        if (isThinking) {
            val paragraphs = cleaned.split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotBlank() }
            val userFacing = mutableListOf<String>()

            for (p in paragraphs) {
                val lines = p.lines().map { it.trim() }.filter { it.isNotBlank() }
                val hasExtractionBullets = lines.any {
                    Regex("(?i)^[•\\*\\-\\u2022\\u2023\\u25E6\\u2043\\u2219]?\\s*(?:amount|category|item|merchant|date|currency)\\s*:").containsMatchIn(it)
                }
                val hasPromptDirectiveBullets = lines.any {
                    Regex("(?i)^[•\\*\\-\\u2022\\u2023\\u25E6\\u2043\\u2219]?\\s*(?:Acknowledge the|Mention the|Keep it|Do NOT automatically)\\b").containsMatchIn(it)
                }

                val isParagraphThinking = hasExtractionBullets ||
                        hasPromptDirectiveBullets ||
                        thinkingPatterns.any { it.containsMatchIn(p) } ||
                        Regex("(?i)^For\\s+[\"“]").containsMatchIn(p) ||
                        Regex("(?i)^\\d+\\.\\s*[\"“].+?[\"”]").containsMatchIn(p) ||
                        p.contains("EXPENSE DETECTION", ignoreCase = true) ||
                        p.contains("CHALLENGE DETECTION", ignoreCase = true) ||
                        p.contains("The rules say", ignoreCase = true) ||
                        p.contains("Wait, let me", ignoreCase = true) ||
                        p.contains("re-read the", ignoreCase = true) ||
                        p.contains("Then at the end", ignoreCase = true) ||
                        p.contains("Then the data block", ignoreCase = true) ||
                        p.contains("strict prohibition", ignoreCase = true) ||
                        p.contains("Write your natural conversational", ignoreCase = true) ||
                        p.contains("Looking at the hidden context", ignoreCase = true) ||
                        p.contains("careful not to overstep", ignoreCase = true) ||
                        p.contains("Do NOT automatically save", ignoreCase = true) ||
                        p.contains("NEVER mix", ignoreCase = true) ||
                        p.contains("Breaking it down", ignoreCase = true) ||
                        p.contains("Breaking down", ignoreCase = true) ||
                        p.contains("mentioning two expenses", ignoreCase = true) ||
                        (p.contains("mentioning", ignoreCase = true) && p.contains("expenses", ignoreCase = true)) ||
                        Regex("(?m)^\\s*[\"“].+?[\"”]\\s*=\\s*[\"”].+?[\"”]").containsMatchIn(p) ||
                        Regex("(?m)^\\s*[•\\*\\-]\\s*[\"“].+?[\"”]\\s*=").containsMatchIn(p) ||
                        Regex("(?i)The user'?s message:?").containsMatchIn(p) ||
                        Regex("(?i)\\d+\\.\\s*.+? for \\d+ MMK").containsMatchIn(p) ||
                        p.startsWith("Something like", ignoreCase = true) ||
                        p.startsWith("Wait,", ignoreCase = true) ||
                        Regex("(?i)Analyze User Input|Identify Required Fields|Determine Response Language|Formulate Extraction|Possible response").containsMatchIn(p) ||
                        Regex("(?i)The format expected is|This is an instruction").containsMatchIn(p) ||
                        Regex("(?i)^\\d+\\.\\s*\\*\\*Draft\\s*[-–]\\s*Mental Refinement:\\*\\*").containsMatchIn(p) ||
                        p.contains("hidden context", ignoreCase = true) ||
                        p.contains("JSON block", ignoreCase = true) ||
                        p.contains("json structure", ignoreCase = true) ||
                        p.contains("prompt_challenge_confirmation", ignoreCase = true) ||
                        p.contains("mark_challenge_saving", ignoreCase = true) ||
                        p.contains("Active Challenges (", ignoreCase = true) ||
                        Regex("(?i)•\\s*\\w+:.*MMK.*complete").containsMatchIn(p) ||
                        p.contains("So for this case", ignoreCase = true) ||
                        p.contains("two challenges", ignoreCase = true) ||
                        p.contains("multiple challenges", ignoreCase = true) ||
                        p.contains("For the expense part", ignoreCase = true) ||
                        p.contains("For the challenge part", ignoreCase = true) ||
                        p.contains("mixed expense", ignoreCase = true) ||
                        p.contains("Example for", ignoreCase = true) ||
                        p.startsWith("User:", ignoreCase = true) ||
                        p.startsWith("Assistant:", ignoreCase = true) ||
                        Regex("^\\s*[\\[\\{\\]\\}]\\s*$").containsMatchIn(p)

                if (!isParagraphThinking) {
                    val filteredLines = lines.filterNot { line ->
                        Regex("(?i)^[•\\*\\-\\u2022\\u2023\\u25E6\\u2043\\u2219]?\\s*(?:amount|category|item|merchant|date|currency)\\s*:").containsMatchIn(line) ||
                        Regex("(?i)^For\\s+[\"“].+?[\"”]:?").containsMatchIn(line) ||
                        Regex("(?i)^\\d+\\.\\s*[\"“].+?[\"”].*category").containsMatchIn(line) ||
                        Regex("^\\s*[\\[\\{\\]\\},]+\\s*$").containsMatchIn(line) ||
                        Regex("^\\s*\"[a-zA-Z_]+\"\\s*:.*").containsMatchIn(line) ||
                        Regex("(?i)^So for this case").containsMatchIn(line) ||
                        Regex("(?i)^For the (?:expense|challenge) part").containsMatchIn(line) ||
                        Regex("(?i)^And for (?:mixed|multiple)").containsMatchIn(line) ||
                        Regex("(?i)^\"?Example for").containsMatchIn(line) ||
                        Regex("(?i)^User:").containsMatchIn(line) ||
                        Regex("(?i)^Assistant:").containsMatchIn(line) ||
                        Regex("(?i)I need:\\s*$").containsMatchIn(line) ||
                        line.contains("(not specified)", ignoreCase = true)
                    }
                    if (filteredLines.isNotEmpty()) {
                        userFacing.add(filteredLines.joinToString("\n"))
                    }
                }
            }

            // Filter out very short fragments that are likely thinking remnants
            val filteredFacing = userFacing.filter { it.length > 10 || it.any { c -> c.isDigit() } }

            val rawCombined = filteredFacing.joinToString("\n\n").trim()
            return sanitizeHeadingOutput(rawCombined)
        }

        return sanitizeHeadingOutput(cleaned)
    }

    private fun sanitizeHeadingOutput(text: String): String {
        if (text.isBlank()) return ""
        // Strip trailing orphaned markdown headings
        val withoutTrailingHeading = text.replace(Regex("(?s)\\n+#{1,6}\\s+[^\\n]+$"), "").trim()
        // Check if there are any substantive non-heading lines
        val substantiveLines = withoutTrailingHeading.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
        if (substantiveLines.isEmpty()) {
            return ""
        }
        return withoutTrailingHeading
    }

        fun cleanChallengeTitle(raw: String): String {
            return raw.trim()
                .replace(Regex("(?i)^(?:for|into|towards|to|in|a|an|the)\\s+"), "")
                .replace(Regex("(?i)\\s+(?:challenge|goal)$"), "")
                .replace(Regex("(?i)\\s+(?:today|yesterday|tonight|ဒီနေ့|မနေ့က)$"), "")
                .replace(Regex("^(?:မေ့တော့မလို့\\s+|မေ့လို့\\s+|မေ့တော့မလို\\s+|မေ့လို\\s+|နောက်ပြီး\\s+|ပြီးတော့\\s+)"), "")
                .replace(Regex("(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ|ကို|ဖိုး|ရန်|ဖို့|$BURMESE_CHALLENGE_VERBS_PATTERN)$"), "")
                .replace(Regex("^(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ|ကို|ဖိုး|ရန်|ဖို့)"), "")
                .trim()
        }

        fun extractFallbackExpenses(userMessage: String, detectedLang: String): List<ParsedExpense> {
            val text = userMessage.trim()
            if (text.isBlank()) return emptyList()

            // Normalize Burmese digits and phonetic numerals
            val normalizedText = com.savingcoach.app.utils.BurmeseNumeralConverter.convert(text)
            val todayStr = LocalDate.now().toString()

            // 1. Check if 2+ numbers are present for multi-item/challenge messages
            val numberMatches = Regex("\\d+(?:,\\d+)*(?:\\.\\d+)?").findAll(normalizedText).toList()
            val splitRegex = Regex("(?i)(?:\\s+(?:and|&|\\+)\\s+|\\s*(?:နဲ့|နှင့်|ပြီးတော့|နောက်ပြီး|ပြီးရင်|ဒါ့အပြင်|အပြင်|ပြီး)\\s*|\\s*[၊။,;]+\\s*|\\n+)")
            var clauses = normalizedText.split(splitRegex).map { it.trim() }.filter { it.isNotBlank() }

            // If not split by standard punctuation/conjunctions, check for boundary between an expense statement and a challenge statement
            if (clauses.size < 2 && detectedLang == "my") {
                try {
                    val expenseVerbEnd = "(?:ကုန်တယ်|ကုန်ခဲ့တယ်|ကုန်သွားတယ်|ကျတယ်|ကျခဲ့တယ်|ရှင်းတယ်|ရှင်းခဲ့တယ်|ဝယ်တယ်|ဝယ်ခဲ့တယ်|ဝယ်လိုက်တယ်|သုံးတယ်|သုံးခဲ့တယ်|သုံးလိုက်တယ်|စားတယ်|စားခဲ့တယ်|သောက်တယ်|သောက်ခဲ့တယ်)(?:\\s*(?:မနက်စာ|နေ့လယ်စာ|ညစာ)?\\s*(?:အ)?တွက်)?"
                    val expenseCurrencyEnd = "(?:[\\d,]+\\s*(?:ဖိုး|ကျပ်|ks|mmk)(?:\\s*(?:မနက်စာ|နေ့လယ်စာ|ညစာ)?\\s*(?:အ)?တွက်)?)"
                    val challengeClause = "(?:(?:မေ့တော့မလို့\\s+|မေ့လို့\\s+)?[a-zA-Z0-9\\u1000-\\u109F\\s]+?(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?\\s*(?:[\\d,]+)?\\s*(?:ကျပ်|ks|mmk)?\\s*$BURMESE_CHALLENGE_VERBS_PATTERN)$"

                    val expMatch = Regex("($expenseVerbEnd)\\s+($challengeClause)").find(normalizedText)
                        ?: Regex("($expenseCurrencyEnd)\\s+($challengeClause)").find(normalizedText)

                    if (expMatch != null && expMatch.groups[1] != null) {
                        val splitIndex = expMatch.range.first + expMatch.groups[1]!!.value.length
                        val part1 = normalizedText.substring(0, splitIndex).trim()
                        val part2 = normalizedText.substring(splitIndex).trim()
                        if (part1.isNotBlank() && part2.isNotBlank()) {
                            clauses = listOf(part1, part2)
                        }
                    } else {
                        val challengeToExpensePattern = Regex("($BURMESE_CHALLENGE_VERBS_PATTERN)\\s+([a-zA-Z0-9\\u1000-\\u109F\\s]+?\\s*\\d+)")
                        val chalMatch = challengeToExpensePattern.find(normalizedText)
                        if (chalMatch != null && chalMatch.groups[1] != null) {
                            val splitIndex = chalMatch.range.first + chalMatch.groups[1]!!.value.length
                            val part1 = normalizedText.substring(0, splitIndex).trim()
                            val part2 = normalizedText.substring(splitIndex).trim()
                            if (part1.isNotBlank() && part2.isNotBlank()) {
                                clauses = listOf(part1, part2)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Fallback gracefully without throwing regex syntax exceptions
                }
            }

            val hasChallengeContext = normalizedText.contains(Regex("(?i)\\b(?:save|put|deposit)\\b")) ||
                    normalizedText.contains(Regex("(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?\\s*(?:[\\d,]+)?\\s*(?:ကျပ်|ks|mmk)?\\s*$BURMESE_CHALLENGE_VERBS_PATTERN"))

            if (clauses.size >= 2 && (numberMatches.size >= 2 || hasChallengeContext)) {
                val parsedList = clauses.mapNotNull { clause ->
                    parseSingleExpenseClause(clause, todayStr, detectedLang, hasChallengeContext)
                }
                if (parsedList.size >= 2) {
                    return parsedList
                }
            }

            // 2. Fallback to single challenge extraction
            val singleChallenge = extractFallbackExpenseOrChallenge(text, detectedLang)
            if (singleChallenge != null && singleChallenge.isChallenge) {
                return listOf(singleChallenge)
            }

            // 3. Fallback to single expense extraction
            val singleFromClause = parseSingleExpenseClause(normalizedText, todayStr, detectedLang, false)
            if (singleFromClause != null) {
                return listOf(singleFromClause)
            }
            val single = extractFallbackExpenseOrChallenge(text, detectedLang)
            return listOfNotNull(single)
        }

        private fun parseSingleExpenseClause(
            clause: String,
            todayStr: String,
            detectedLang: String,
            hasChallengeContext: Boolean = false
        ): ParsedExpense? {
            var text = clause.trim()
            if (text.isBlank()) return null

            // Remove leading conversational preface / filler
            text = text.replace(Regex("^(?:မေ့တော့မလို့|မေ့လို့|မေ့တော့မလို)\\s*"), "").trim()

            // Remove leading action words or date words
            text = text.replace(Regex("(?i)^(?:today|yesterday|tonight|this\\s+(?:morning|afternoon|evening)|just\\s+now|ဒီနေ့|မနေ့က|ခုနက|အခု|ညက)?\\s*(?:i\\s+)?(?:log|spent|paid|bought|add|added|ဝယ်ခဲ့တာ|ဝယ်ခဲ့တဲ့|ဝယ်လိုက်တာ|ဝယ်လိုက်တဲ့|ဝယ်တာ|ဝယ်ထားတာ|သုံးခဲ့တာ|သုံးလိုက်တာ|သုံးတာ|ဝယ်|သုံး|စားခဲ့တာ|စားခဲ့တဲ့|စားလိုက်တာ|စားတာ|သောက်ခဲ့တာ|သောက်ခဲ့တဲ့|သောက်လိုက်တာ|သောက်တာ|စား|သောက်)?(?:\\s+(?:on|for))?\\s+"), "").trim()
            // Remove trailing currency, date words, or Burmese verbs
            text = text.replace(Regex("(?i)(?:\\s*(?:today|yesterday|tonight|this\\s+(?:morning|afternoon|evening)|just\\s+now|ဒီနေ့|မနေ့က|ခုနက|mmk|ks|kyats?|kyat|$BURMESE_VERBS_PATTERN))+$"), "").trim()

            // 1. Check English challenge patterns inside clause
            // Pattern A: "save 5000 for Camera", "put 10000 into Gucci Bag", or with hasChallengeContext: "10000 for Gucci Bag"
            val enChalMatch = Regex("(?i)^(?:save|put|deposit)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?\\s*(?:for|into|towards|to|in)\\s+(.+)").find(text)
            if (enChalMatch != null) {
                val hasExplicitSaveVerb = clause.trim().contains(Regex("(?i)^(?:save|put|deposit)\\b"))
                val hasExpenseVerb = clause.trim().contains(Regex("(?i)\\b(?:log|spent|paid|bought|add|added)\\b"))
                val rawTitle = enChalMatch.groupValues[2].trim()
                val title = cleanChallengeTitle(rawTitle)
                val isCommonExpenseWord = title.contains(Regex("(?i)^(?:food|dinner|lunch|breakfast|coffee|tea|groceries|clothes|shopping|taxi|bus|gas|drink)$"))
                if (!hasExpenseVerb && !isCommonExpenseWord && (hasExplicitSaveVerb || hasChallengeContext)) {
                    val amount = enChalMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                    if (amount > 0 && title.isNotBlank()) {
                        return ParsedExpense(
                            amount = amount,
                            category = "Savings",
                            merchant = title,
                            date = todayStr,
                            language = detectedLang,
                            isChallenge = true,
                            challengeTitle = title,
                            action = "prompt_challenge_confirmation",
                            item = title,
                            currency = "MMK"
                        )
                    }
                }
            }

            // Pattern B: English title first: "save for Camera 5000", "put into Gucci Bag 10000", "Camera save 5000"
            val enTitleFirstChal = Regex("(?i)^(?:save|put|deposit)?\\s*(?:for|into|towards|to|in)?\\s*(.+?)\\s+(?:save|put|deposit)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?$").find(text)
            if (enTitleFirstChal != null) {
                val rawTitle = enTitleFirstChal.groupValues[1].trim()
                val amount = enTitleFirstChal.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                val title = cleanChallengeTitle(rawTitle)
                if (amount > 0 && title.isNotBlank()) {
                    return ParsedExpense(
                        amount = amount,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // Pattern C: English challenge without amount: "save for Gucci Bag", "save Gucci Bag", "put into Camera", "save in 1K a Day"
            val enNoAmountChal = Regex("(?i)^(?:save|put|deposit)(?:\\s+(?:for|into|towards|to|in))?\\s+(.+?)(?:\\s+(?:today|yesterday|tonight))?$").find(text)
            if (enNoAmountChal != null) {
                val rawTitle = enNoAmountChal.groupValues[1].trim()
                val title = cleanChallengeTitle(rawTitle)
                if (title.isNotBlank() && !title.contains(Regex("(?i)^(?:food|dinner|lunch|breakfast|coffee|tea|groceries|clothes|shopping|taxi|bus|gas|drink)$"))) {
                    return ParsedExpense(
                        amount = 0.0,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // 2. Burmese challenge patterns inside clause
            // Pattern A: Title first with Burmese challenge verb:
            // "Camera အတွက် 5000 စုမယ်", "Gucci Bag ဝယ်ဖို့ 10000 ထည့်မယ်", "Camera 5000 စုမယ်", "Camera 5000 ထည့်မယ်"
            val myChalMatch = Regex("([^\\d]+?)(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks|mmk)?\\s*$BURMESE_CHALLENGE_VERBS_PATTERN").find(text)
            if (myChalMatch != null) {
                val rawTitle = myChalMatch.groupValues[1].trim()
                val amount = myChalMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                val title = cleanChallengeTitle(rawTitle)
                if (amount > 0 && title.isNotBlank()) {
                    return ParsedExpense(
                        amount = amount,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // Pattern B: Title first with connective ("အတွက်", "ဝယ်ဖို့", "ထဲကို", "ထဲ") when hasChallengeContext is true
            // e.g. "Camera အတွက် 5000" in "Camera အတွက် 5000 နဲ့ Gucci Bag အတွက် 10000 စုမယ်"
            if (hasChallengeContext) {
                val myChalConnectiveMatch = Regex("([^\\d]+?)(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks|mmk)?$").find(text)
                if (myChalConnectiveMatch != null) {
                    val rawTitle = myChalConnectiveMatch.groupValues[1].trim()
                    val amount = myChalConnectiveMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                    val title = cleanChallengeTitle(rawTitle)
                    if (amount > 0 && title.isNotBlank()) {
                        return ParsedExpense(
                            amount = amount,
                            category = "Savings",
                            merchant = title,
                            date = todayStr,
                            language = detectedLang,
                            isChallenge = true,
                            challengeTitle = title,
                            action = "prompt_challenge_confirmation",
                            item = title,
                            currency = "MMK"
                        )
                    }
                }
            }

            // Pattern C: Amount first Burmese challenge:
            // "5000 ကို Camera အတွက် စုမယ်", "5000 Camera စုမယ်"
            val myAmountFirstChal = Regex("([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks|mmk)?\\s*(?:ကို|ဖိုး)?\\s*([^\\d]+?)(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?\\s*$BURMESE_CHALLENGE_VERBS_PATTERN").find(text)
            if (myAmountFirstChal != null) {
                val amount = myAmountFirstChal.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                val rawTitle = myAmountFirstChal.groupValues[2].trim()
                val title = cleanChallengeTitle(rawTitle)
                if (amount > 0 && title.isNotBlank()) {
                    return ParsedExpense(
                        amount = amount,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // Pattern D: Burmese challenge without amount: "Gucci Bag ဝယ်ဖို့ စုမယ်", "Camera အတွက် စုမယ်", "Camera စုမယ်", "Camera ထည့်မယ်"
            val myNoAmountChal = Regex("([^\\d]+?)(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?\\s*$BURMESE_CHALLENGE_VERBS_PATTERN").find(text)
            if (myNoAmountChal != null) {
                val rawTitle = myNoAmountChal.groupValues[1].trim()
                val title = cleanChallengeTitle(rawTitle)
                if (title.isNotBlank() && !title.contains(Regex("(?i)^(?:food|dinner|lunch|breakfast|coffee|groceries|clothes)$"))) {
                    return ParsedExpense(
                        amount = 0.0,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // 3. Regular expenses
            // Pattern 0a: Quantity first, per-unit price second: e.g. "ဒူးရင်းသီး နှစ်လုံး ဝယ်ခဲ့တယ် တစ်လုံး 50000", "ပန်းသီး 3လုံး တစ်လုံး 2000"
            val qtyPatternA = Regex("(.+?)\\s*([\\d]+|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ခုနစ်|ခုနှစ်|ရှစ်|ကိုး|ဆယ်)\\s*($BURMESE_CLASSIFIERS)?(?:\\s*(?:ဝယ်ခဲ့တယ်|ဝယ်ခဲ့တာ|ဝယ်တယ်|ဝယ်လိုက်တယ်|ဝယ်ထားတယ်|ဝယ်တာ|ဝယ်|စားခဲ့တယ်|စားတယ်|သောက်ခဲ့တယ်|သောက်တယ်))?\\s*တစ်(?:$BURMESE_CLASSIFIERS)?\\s*(?:ကျပ်|ks|mmk)?\\s*([\\d,]+(?:\\.\\d+)?)").find(text)
            if (qtyPatternA != null) {
                val rawItemName = qtyPatternA.groupValues[1].trim()
                val qtyWord = qtyPatternA.groupValues[2].trim()
                val unitClassifier = qtyPatternA.groupValues[3].trim()
                val unitPrice = qtyPatternA.groupValues[4].replace(",", "").toDoubleOrNull() ?: 0.0

                val qty = parseQtyWord(qtyWord)
                val totalAmount = if (unitPrice > 0) qty * unitPrice else unitPrice
                var item = if (unitClassifier.isNotBlank()) "$rawItemName $qty $unitClassifier".trim() else rawItemName
                item = item.replace(Regex("(?:\\s*(?:က|ကို|အတွက်|ဖိုး|တန်(?:ဖိုး)?|နဲ့|for|on|at|of|cost|costs|costing|$BURMESE_VERBS_PATTERN))+$", RegexOption.IGNORE_CASE), "").trim()
                if (totalAmount > 0 && item.isNotBlank() && !item.contains("စု") && !item.contains("ထည့်")) {
                    val category = inferCategory(item)
                    return ParsedExpense(
                        amount = totalAmount,
                        category = category,
                        merchant = item,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = false,
                        item = item,
                        currency = "MMK"
                    )
                }
            }

            // Pattern 0b: Per-unit price first, quantity second: e.g. "သရက်သီး တစ်လုံး 800 ငါ 3လုံးဝယ်ခဲ့တယ်", "သရက်သီး တစ်လုံးကို 800 3လုံး"
            val qtyPatternB = Regex("(.+?)\\s*တစ်($BURMESE_CLASSIFIERS)?(?:ကို)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks|mmk|ဖိုး)?\\s*(?:ငါ|ကျွန်တော်|ကျနော်|ကျွန်မ|ကျမ|သူ|နဲ့|ဖိုး)?\\s*([\\d]+|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ခုနစ်|ခုနှစ်|ရှစ်|ကိုး|ဆယ်)\\s*($BURMESE_CLASSIFIERS)?(?:\\s*(?:ဝယ်ခဲ့တယ်|ဝယ်ခဲ့တာ|ဝယ်တယ်|ဝယ်လိုက်တယ်|ဝယ်ထားတယ်|ဝယ်တာ|ဝယ်|စားခဲ့တယ်|စားတယ်|သောက်ခဲ့တယ်|သောက်တယ်))?").find(text)
            if (qtyPatternB != null) {
                val rawItemName = qtyPatternB.groupValues[1].trim()
                val unitClassifierA = qtyPatternB.groupValues[2].trim()
                val unitPrice = qtyPatternB.groupValues[3].replace(",", "").toDoubleOrNull() ?: 0.0
                val qtyWord = qtyPatternB.groupValues[4].trim()
                val unitClassifierB = qtyPatternB.groupValues[5].trim()
                val unitClassifier = unitClassifierA.ifBlank { unitClassifierB }

                val qty = parseQtyWord(qtyWord)
                val totalAmount = if (unitPrice > 0) qty * unitPrice else unitPrice
                var item = if (unitClassifier.isNotBlank()) "$rawItemName $qty $unitClassifier".trim() else rawItemName
                item = item.replace(Regex("(?:\\s*(?:က|ကို|အတွက်|ဖိုး|တန်(?:ဖိုး)?|နဲ့|for|on|at|of|cost|costs|costing|$BURMESE_VERBS_PATTERN))+$", RegexOption.IGNORE_CASE), "").trim()
                if (totalAmount > 0 && item.isNotBlank() && !item.contains("စု") && !item.contains("ထည့်")) {
                    val category = inferCategory(item)
                    return ParsedExpense(
                        amount = totalAmount,
                        category = category,
                        merchant = item,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = false,
                        item = item,
                        currency = "MMK"
                    )
                }
            }

            // Pattern 1: amount first: e.g. "15000 for dinner", "15000 on lunch", "15000 dinner", "15000 ဖိုး ထမင်း"
            val amountFirst = Regex("(?i)^([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?|ဖိုး)?\\s*(?:for|on|at|of)?\\s*(.+)$").find(text)
            if (amountFirst != null) {
                val amount = amountFirst.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                val rawItem = amountFirst.groupValues[2].trim().trim('"', '\'', '“', '”')
                var item = rawItem.replace(Regex("(?i)^(?:for|on|at|of|a|an|the)\\s+"), "").trim()
                item = item.replace(Regex("(?:\\s*(?:က|ကို|အတွက်|ဖိုး|တန်(?:ဖိုး)?|နဲ့|for|on|at|of|cost|costs|costing|$BURMESE_VERBS_PATTERN))+$", RegexOption.IGNORE_CASE), "").trim()
                if (amount > 0 && item.isNotBlank() && !item.contains("စု") && !item.contains("ထည့်")) {
                    val category = inferCategory(item)
                    return ParsedExpense(
                        amount = amount,
                        category = category,
                        merchant = item,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = false,
                        item = item,
                        currency = "MMK"
                    )
                }
            }

            // Pattern 2: item first: e.g. "electric bike for 4500", "dinner 15000", "ထမင်းကြော် 15000", "စက်ဘီး 4500", "clothes for 50000"
            val itemFirst = Regex("(?i)^(.+?)\\s*(?:for|on|at|of|cost|costs|costing)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?|ကျပ်|ကျ|$BURMESE_VERBS_PATTERN)*$").find(text)
            if (itemFirst != null) {
                var item = itemFirst.groupValues[1].trim().trim('"', '\'', '“', '”')
                val amount = itemFirst.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                item = item.replace(Regex("(?:\\s*(?:က|ကို|အတွက်|ဖိုး|တန်(?:ဖိုး)?|နဲ့|for|on|at|of|cost|costs|costing|$BURMESE_VERBS_PATTERN))+$", RegexOption.IGNORE_CASE), "").trim()
                item = item.replace(Regex("(?i)^(?:a|an|the|on|for)\\s+"), "").trim()
                if (amount > 0 && item.isNotBlank() && !item.contains("စု") && !item.contains("ထည့်")) {
                    val category = inferCategory(item)
                    return ParsedExpense(
                        amount = amount,
                        category = category,
                        merchant = item,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = false,
                        item = item,
                        currency = "MMK"
                    )
                }
            }

            // 4. Bare challenge title when in challenge context (e.g. "Camera" in "save Gucci Bag and Camera", "Gucci Bag" in "Gucci Bag နဲ့ Camera စုမယ်")
            if (hasChallengeContext) {
                // English bare challenge title: e.g. "Camera", "for Camera"
                val enBareMatch = Regex("(?i)^(?:for|into|towards|to|in)?\\s*([a-zA-Z0-9\\s'-]+?)(?:\\s+(?:today|yesterday|tonight))?$").find(text)
                if (enBareMatch != null) {
                    val rawTitle = enBareMatch.groupValues[1].trim()
                    val title = cleanChallengeTitle(rawTitle)
                    if (title.isNotBlank() &&
                        !title.contains(Regex("(?i)^(?:food|dinner|lunch|breakfast|coffee|tea|groceries|clothes|shopping|taxi|bus|gas|drink)$")) &&
                        !text.contains(Regex("(?i)\\b(?:log|spent|paid|bought|add|added)\\b"))
                    ) {
                        return ParsedExpense(
                            amount = 0.0,
                            category = "Savings",
                            merchant = title,
                            date = todayStr,
                            language = detectedLang,
                            isChallenge = true,
                            challengeTitle = title,
                            action = "prompt_challenge_confirmation",
                            item = title,
                            currency = "MMK"
                        )
                    }
                }

                // Burmese bare challenge title: e.g. "Gucci Bag", "Camera အတွက်"
                val myBareMatch = Regex("([^\\d]+?)(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?$").find(text)
                if (myBareMatch != null) {
                    val rawTitle = myBareMatch.groupValues[1].trim()
                    val title = cleanChallengeTitle(rawTitle)
                    if (title.isNotBlank() &&
                        !title.contains(Regex("(?:မနက်စာ|နေ့လယ်စာ|ညစာ|ထမင်း|ဟင်း|လက်ဖက်ရည်|ကော်ဖီ|မုန့်|အစားအသောက်)")) &&
                        !text.contains(Regex("(?:စား|သောက်|ဝယ်|သုံး)"))
                    ) {
                        return ParsedExpense(
                            amount = 0.0,
                            category = "Savings",
                            merchant = title,
                            date = todayStr,
                            language = detectedLang,
                            isChallenge = true,
                            challengeTitle = title,
                            action = "prompt_challenge_confirmation",
                            item = title,
                            currency = "MMK"
                        )
                    }
                }
            }

            return null
        }

        fun extractFallbackExpenseOrChallenge(userMessage: String, detectedLang: String): ParsedExpense? {
            val text = userMessage.trim()
            val todayStr = LocalDate.now().toString()

            // 1. Challenge patterns (English & Burmese)
            // English: "save 5000 for Camera", "put 10000 into Gucci Bag"
            val enChallengeMatch = Regex("(?i)(?:save|put|deposit)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?\\s*(?:for|into|towards|to|in)\\s*(.+)").find(text)
            if (enChallengeMatch != null) {
                val amount = enChallengeMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                val rawTitle = enChallengeMatch.groupValues[2].trim()
                val title = cleanChallengeTitle(rawTitle)
                if (amount > 0 && title.isNotBlank()) {
                    return ParsedExpense(
                        amount = amount,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // English challenge without amount: "save for Gucci Bag", "save Gucci Bag", "put into Camera", "deposit to Gucci Bag", "save in 1K a Day"
            val enNoAmountMatch = Regex("(?i)^(?:save|put|deposit)(?:\\s+(?:for|into|towards|to|in))?\\s+(.+?)(?:\\s+(?:today|yesterday|tonight))?$").find(text)
            if (enNoAmountMatch != null) {
                val rawTitle = enNoAmountMatch.groupValues[1].trim()
                val title = cleanChallengeTitle(rawTitle)
                if (title.isNotBlank() && !title.contains(Regex("(?i)^(?:food|dinner|lunch|breakfast|coffee|groceries|clothes)$"))) {
                    return ParsedExpense(
                        amount = 0.0,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // Burmese challenge: "Gucci Bag ဝယ်ဖို့ 5000 စုမယ်", "Camera အတွက် 5000 စုမယ်", "Camera 5000 စုမယ်", "Camera 5000 ထည့်မယ်"
            val myChallengeMatch = Regex("([^\\d]+?)(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks|mmk)?\\s*$BURMESE_CHALLENGE_VERBS_PATTERN").find(text)
            if (myChallengeMatch != null) {
                val rawTitle = myChallengeMatch.groupValues[1].trim()
                val amount = myChallengeMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                val title = cleanChallengeTitle(rawTitle)
                if (amount > 0 && title.isNotBlank()) {
                    return ParsedExpense(
                        amount = amount,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // Burmese challenge amount first: "5000 ကို Camera အတွက် စုမယ်"
            val myAmountFirstChallenge = Regex("([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks|mmk)?\\s*(?:ကို|ဖိုး)?\\s*([^\\d]+?)(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?\\s*$BURMESE_CHALLENGE_VERBS_PATTERN").find(text)
            if (myAmountFirstChallenge != null) {
                val amount = myAmountFirstChallenge.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                val rawTitle = myAmountFirstChallenge.groupValues[2].trim()
                val title = cleanChallengeTitle(rawTitle)
                if (amount > 0 && title.isNotBlank()) {
                    return ParsedExpense(
                        amount = amount,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

            // Burmese challenge without amount: "Gucci Bag ဝယ်ဖို့ စုမယ်", "Camera အတွက် စုမယ်", "Camera စုမယ်", "Camera ထည့်မယ်"
            val myNoAmountMatch = Regex("([^\\d]+?)(?:ဝယ်ဖို့|အတွက်|ထဲကို|ထဲ)?\\s*$BURMESE_CHALLENGE_VERBS_PATTERN").find(text)
            if (myNoAmountMatch != null) {
                val rawTitle = myNoAmountMatch.groupValues[1].trim()
                val title = cleanChallengeTitle(rawTitle)
                if (title.isNotBlank() && !title.contains(Regex("(?i)^(?:food|dinner|lunch|breakfast|coffee|groceries|clothes)$"))) {
                    return ParsedExpense(
                        amount = 0.0,
                        category = "Savings",
                        merchant = title,
                        date = todayStr,
                        language = detectedLang,
                        isChallenge = true,
                        challengeTitle = title,
                        action = "prompt_challenge_confirmation",
                        item = title,
                        currency = "MMK"
                    )
                }
            }

        // 2. Expense patterns (English)
        // e.g. "Log 1600 for YBS Transportation", "Log 1800 for YBS", "1500 for coffee", "spent 3000 on lunch"
        val enExpenseMatch = Regex("(?i)(?:log|paid|spent|bought)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?\\s*(?:for|on|at)\\s*(.+?)(?:\\s+(?:today|yesterday|tonight))?$").find(text)
        if (enExpenseMatch != null) {
            val amount = enExpenseMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            val item = enExpenseMatch.groupValues[2].replace(Regex("(?i)^(?:a|an|the|on|for)\\s+"), "").trim()
            if (amount > 0 && item.isNotBlank()) {
                val category = inferCategory(item)
                return ParsedExpense(
                    amount = amount,
                    category = category,
                    merchant = item,
                    date = todayStr,
                    language = detectedLang,
                    isChallenge = false,
                    item = item,
                    currency = "MMK"
                )
            }
        }

        // English item-first: e.g. "I spent on clothes for 50000 today", "clothes for 50000", "electric bike for 4500"
        val enItemFirstMatch = Regex("(?i)^(?:today|yesterday)?\\s*(?:i\\s+)?(?:spent|paid|bought)?\\s*(?:on|for)?\\s*(.+?)\\s+(?:for|cost|costs|costing)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?(?:\\s+(?:today|yesterday|tonight))?$").find(text)
        if (enItemFirstMatch != null) {
            val rawItem = enItemFirstMatch.groupValues[1].trim()
            val item = rawItem.replace(Regex("(?i)^(?:a|an|the|on|for)\\s+"), "").trim()
            val amount = enItemFirstMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
            if (amount > 0 && item.isNotBlank() && !item.contains("save", ignoreCase = true)) {
                val category = inferCategory(item)
                return ParsedExpense(
                    amount = amount,
                    category = category,
                    merchant = item,
                    date = todayStr,
                    language = detectedLang,
                    isChallenge = false,
                    item = item,
                    currency = "MMK"
                )
            }
        }

        // 3. Expense patterns (Burmese)
        // e.g. "ညစာ ထမင်းကြော် နဲ့ ကွေကာအုတ် 5800 ကုန်", "နေ့လယ်စာ ထမင်းကြော် နဲ့ လက်ဖက်ရည် 6800", "ကော်ဖီဖိုး ၂၀၀၀", "ထမင်း ၃၅၀၀ ဖိုး", "ဟင်းနုနွယ် 2800 ဝယ်ခဲ့"
        val myExpenseMatch = Regex("(.+?)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?|kyat|ကျပ်|ကျ|$BURMESE_VERBS_PATTERN)*$").find(text)
        if (myExpenseMatch != null) {
            var item = myExpenseMatch.groupValues[1].trim()
            item = item.replace(Regex("(?:\\s*(?:for|on|at|of|cost|costs|costing|$BURMESE_VERBS_PATTERN))+$", RegexOption.IGNORE_CASE), "").trim()
            item = item.replace(Regex("(?i)\\s+(?:for|on|at|of|cost|costs|costing)$"), "").trim()
            val amount = myExpenseMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
            if (amount > 0 && item.isNotBlank() && !item.contains("စု")) {
                val category = inferCategory(item)
                return ParsedExpense(
                    amount = amount,
                    category = category,
                    merchant = item,
                    date = todayStr,
                    language = detectedLang,
                    isChallenge = false,
                    item = item,
                    currency = "MMK"
                )
            }
        }

        return null
    }

    private fun inferCategory(item: String): String {
        val resolved = com.savingcoach.app.ui.chat.CategoryResolver.resolve(item, emptyList())
        if (resolved != null && resolved.name != "Other") {
            return resolved.name
        }
        val lower = item.lowercase()
        return when {
            lower.contains("ybs") || lower.contains("bus") || lower.contains("taxi") ||
            lower.contains("grab") || lower.contains("car") || lower.contains("transport") ||
            lower.contains("bike") || lower.contains("bicycle") || lower.contains("cycle") ||
            lower.contains("train") || lower.contains("flight") || lower.contains("ride") ||
            lower.contains("ကားခ") || lower.contains("ယာဉ်") || lower.contains("စက်ဘီး") -> "Transportation"

            lower.contains("coffee") || lower.contains("tea") || lower.contains("lunch") ||
            lower.contains("dinner") || lower.contains("food") || lower.contains("breakfast") ||
            lower.contains("drink") || lower.contains("ထမင်း") || lower.contains("လက်ဖက်ရည်") ||
            lower.contains("ကော်ဖီ") || lower.contains("မုန့်") || lower.contains("ညစာ") ||
            lower.contains("မနက်စာ") || lower.contains("နေ့လယ်စာ") || lower.contains("ကွေကာ") ||
            lower.contains("ခေါက်ဆွဲ") || lower.contains("ဟင်း") || lower.contains("သီး") ||
            lower.contains("ရွက်") || lower.contains("နွယ်") || lower.contains("အသား") ||
            lower.contains("ငါး") || lower.contains("ကြက်သွန်") || lower.contains("အသီး") ||
            lower.contains("သံပရာ") || lower.contains("ကန်စွန်း") || lower.contains("စလုံတီး") ||
            lower.contains("နံပြား") || lower.contains("ပလာတာ") || lower.contains("အီကြာကွေး") ||
            lower.contains("ပေါင်မုန့်") || lower.contains("မုန့်ဟင်းခါး") || lower.contains("အုန်းနို့") ||
            lower.contains("ရှမ်းခေါက်ဆွဲ") || lower.contains("fruit") || lower.contains("vegetable") ||
            lower.contains("produce") || lower.contains("snack") || lower.contains("အအေး") ||
            lower.contains("ဖျော်ရည်") -> "Food & Dining"

            lower.contains("bag") || lower.contains("shoe") || lower.contains("clothes") ||
            lower.contains("shirt") || lower.contains("shopping") || lower.contains("အဝတ်") ||
            lower.contains("ဖိနပ်") || lower.contains("ဓာတ်မီး") || lower.contains("မီးသီး") ||
            lower.contains("မီးချောင်း") || lower.contains("flashlight") || lower.contains("torch") ||
            lower.contains("လျှပ်စစ်ကြိုး") || lower.contains("ကြိုးခွေ") || lower.contains("ဝါယာကြိုး") -> "Shopping"

            lower.contains("bill") || lower.contains("wifi") || lower.contains("internet") ||
            lower.contains("electricity") || lower.contains("water") || lower.contains("ဖုန်းဘေ") ||
            lower.contains("မီးဘေ") || lower.contains("မီတာခ") || lower.contains("လျှပ်စစ်") -> "Bills & Utilities"

            lower.contains("medicine") || lower.contains("hospital") || lower.contains("clinic") ||
            lower.contains("doctor") || lower.contains("ဆေး") -> "Health"

            lower.contains("book") || lower.contains("school") || lower.contains("course") ||
            lower.contains("class") || lower.contains("ကျောင်း") -> "Education"

            else -> "Other"
        }
    }

    fun extractItemFromMessage(userMessage: String): String {
        val text = userMessage.trim()
        if (text.isBlank()) return ""

        // Burmese expense: e.g. "ထမင်းကြော် နဲ့ လက်ဖက်ရည် 8500 ကုန်", "ကော်ဖီ ၃၀၀၀", "ဟင်းနုနွယ် 2800 ဝယ်ခဲ့"
        val myMatch = Regex("(.+?)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?|kyat|$BURMESE_VERBS_PATTERN)*$").find(text)
        if (myMatch != null) {
            val item = myMatch.groupValues[1].trim()
            if (item.isNotBlank() && !item.contains("စု")) return item
        }

        // English expense amount-first: e.g. "8500 for fried rice and tea", "spent 5000 on coffee"
        val enMatch = Regex("(?i)(?:log|paid|spent|bought)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?\\s*(?:for|on|at)\\s*(.+?)(?:\\s+(?:today|yesterday|tonight))?$").find(text)
        if (enMatch != null) {
            val item = enMatch.groupValues[2].replace(Regex("(?i)^(?:a|an|the|on|for)\\s+"), "").trim()
            if (item.isNotBlank()) return item
        }

        // English expense item-first: e.g. "I spent on clothes for 50000 today", "clothes for 50000"
        val enItemFirst = Regex("(?i)^(?:today|yesterday)?\\s*(?:i\\s+)?(?:spent|paid|bought)?\\s*(?:on|for)?\\s*(.+?)\\s+(?:for|cost|costs|costing)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?(?:\\s+(?:today|yesterday|tonight))?$").find(text)
        if (enItemFirst != null) {
            val item = enItemFirst.groupValues[1].replace(Regex("(?i)^(?:a|an|the|on|for)\\s+"), "").trim()
            if (item.isNotBlank() && !item.contains("save", ignoreCase = true)) return item
        }

        return ""
    }

    private fun estimateTokens(text: String): Int {
        val isBurmese = text.any { it.code in 0x1000..0x109F }
        val charsPerToken = if (isBurmese) 2 else 4
        return (text.length / charsPerToken).coerceAtLeast(1)
    }
    }
}

