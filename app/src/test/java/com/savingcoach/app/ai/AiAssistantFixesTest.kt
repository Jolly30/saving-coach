package com.savingcoach.app.ai

import com.savingcoach.app.data.model.FinnhubNewsResponse
import com.savingcoach.app.data.model.UserHolding
import com.savingcoach.app.data.model.ParsedExpense
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AiAssistantFixesTest {

    // Delegate to AiChatRepository.cleanThinking
    private fun cleanThinking(text: String): String = AiChatRepository.cleanThinking(text)

    // Mirror of parseExpenseData helper
    private fun parseExpenseData(rawJson: String, detectedLang: String = "en"): ParsedExpense? {
        return try {
            val cleanJson = rawJson
                .replace(Regex("^```[a-zA-Z]*\\s*"), "")
                .replace(Regex("\\s*```$"), "")
                .trim()
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(cleanJson)
            if (jsonElement !is JsonObject) return null

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
            val category = getString("category", "type").ifBlank { "Other" }
            val amount = getDouble("amount", "cost", "price", "value")
            var date = getString("date", "datetime")
            val action = getString("action").ifBlank { if (isChallenge) "prompt_challenge_confirmation" else "log_expense" }
            val item = getString("item", "description")
            val currency = getString("currency").ifBlank { "MMK" }

            if (date.isBlank() || date.contains("YYYY", ignoreCase = true) || date.length < 8) {
                date = LocalDate.now().toString()
            }

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

    @Test
    fun testScreenshot1_leakedExtractionThinkingIsStripped() {
        val input = """
            The structure should be:

            For this request:
            • amount: 800
            • category: "Transportation" (they said "YBS Transportation")
            • merchant: "YBS" (they mentioned YBS)
            • date: today (YYYY-MM-DD)
        """.trimIndent()

        val cleaned = cleanThinking(input)
        assertTrue("Leaked extraction structure should be completely stripped", cleaned.isBlank())
    }

    @Test
    fun testScreenshot2_leakedDateMathReasoningIsStripped() {
        val input = "If there are 27 days left in September 2026, and September has 30 days, then today is September 4th (because 30-27=3 days have passed, so today is the 4th)."
        val cleaned = cleanThinking(input)
        assertTrue("Date math reasoning should be completely stripped", cleaned.isBlank())
    }

    @Test
    fun testScreenshot3_leakedPromptBulletsAreStripped() {
        val input = """
            • Acknowledge the challenge save request
            • Mention the challenge name and amount
            • Keep
        """.trimIndent()

        val cleaned = cleanThinking(input)
        assertTrue("Leaked prompt bullet points should be stripped", cleaned.isBlank())
    }

    @Test
    fun testValidUserFacingContentPreservedWhenThinkingFiltered() {
        val input = """
            Let me analyze this request.
            Amount: 5000

            မင်္ဂလာပါ။ နေ့လယ်စာ ထမင်းကြော် နဲ့ ကော်ဖီ အတွက် ၅,၅၀၀ ကျပ် မှတ်တမ်းတင်ပေးပါမယ်။
        """.trimIndent()

        val cleaned = cleanThinking(input)
        assertTrue("User facing Burmese response should be preserved", cleaned.contains("နေ့လယ်စာ ထမင်းကြော်"))
        assertFalse("Thinking should be removed", cleaned.contains("Let me analyze"))
    }

    @Test
    fun testParseExpenseData_handlesAlternativeChallengeTitleKeys() {
        val jsonSnakeCase = """
            {
                "isChallenge": true,
                "challenge_title": "Gucci Bag",
                "action": "prompt_challenge_confirmation",
                "amount": 10000,
                "currency": "MMK"
            }
        """.trimIndent()

        val parsed = parseExpenseData(jsonSnakeCase, "my")
        assertNotNull(parsed)
        assertTrue(parsed!!.isChallenge)
        assertEquals("Gucci Bag", parsed.challengeTitle)
        assertEquals(10000.0, parsed.amount, 0.001)
    }

    @Test
    fun testParseExpenseData_handlesMarkdownCodeFences() {
        val markdownJson = """
            ```json
            {
                "amount": 800,
                "category": "Transportation",
                "merchant": "YBS",
                "date": "2026-09-03"
            }
            ```
        """.trimIndent()

        val parsed = parseExpenseData(markdownJson, "en")
        assertNotNull(parsed)
        assertEquals(800.0, parsed!!.amount, 0.001)
        assertEquals("Transportation", parsed.category)
        assertEquals("YBS", parsed.merchant)
        assertEquals("2026-09-03", parsed.date)
    }

    @Test
    fun testParseExpenseData_sanitizesPlaceholderDate() {
        val jsonPlaceholderDate = """
            {
                "amount": 800,
                "category": "Transportation",
                "merchant": "YBS",
                "date": "YYYY-MM-DD"
            }
        """.trimIndent()

        val parsed = parseExpenseData(jsonPlaceholderDate, "en")
        assertNotNull(parsed)
        assertFalse(parsed!!.date.contains("YYYY"))
        assertEquals(LocalDate.now().toString(), parsed.date)
    }

    @Test
    fun testChallengeMatching_matchesEmojiAndCaseInsensitive() {
        fun cleanTitle(title: String): String {
            return title.filter { it.isLetterOrDigit() || it.isWhitespace() }.lowercase().trim()
        }

        val dbTitle = "👜 Gucci Bag"
        val query = "gucci bag"
        assertEquals(cleanTitle(dbTitle), cleanTitle(query))

        val userMessage = "Gucci Bag ဝယ်ဖို့ 10000 စုမယ်"
        assertTrue(cleanTitle(userMessage).contains(cleanTitle(dbTitle)))
    }

    @Test
    fun testNewScreenshots_coffeeContextLeakIsStripped() {
        val input = """
            Logged 1,500 MMK for Coffee. You've spent..." but I need to check the context. Looking at the hidden context: Total Spent: 15500.0, Top Categories: Food & Dining (15000.0), Transportation (500.0). So if I add 1500 for coffee (Food), the new total would be 17000, and Food would be 16500.

            But actually, I should just respond naturally and then add the expense data block. The rules say: "Write your natural conversational response first. Then, at the VERY END of your message, append a hidden data block."

            So my response should be something like: "Logged 1,500 MMK for Coffee. That's your second food expense today — on track with your budget?" or something similar. But I need to be careful not to overstep - the rules say "Do NOT automatically save the expense. Just acknowledge it normally in...
        """.trimIndent()

        val cleaned = cleanThinking(input)
        assertTrue("Internal context math monologue should be stripped", cleaned.isBlank() || cleaned.contains("Logged 1,500 MMK for Coffee"))
        assertFalse("Rules and hidden context must never be exposed", cleaned.contains("hidden context") || cleaned.contains("The rules say"))
    }

    @Test
    fun testNewScreenshots_quotedDraftResponseIsExtracted() {
        val input = """
            Something like "Great! You want to save 5,000 MMK for the Gucci Bag challenge. That's wonderful progress towards your goal! Currently you've saved 15,000.0 MMK out of 250,000.0 MMK (6% complete), so this new save will help you move forward on your challenge."

            Then the data block at the end:
            [EXPENSE_DATA]
            {
              "isChallenge": true,
              "challengeTitle": "Gucci Bag",
              "action": "prompt_challenge_confirmation",
              "amount": 5000,
              "currency": "MMK"
            }
            [/EXPENSE_DATA]

            Wait, let me re-read the rules for challenge detection more carefully:
        """.trimIndent()

        val withoutExpense = input.replace(Regex("\\[EXPENSE_DATA\\][\\s\\S]*?\\[/EXPENSE_DATA\\]"), "").trim()
        val cleaned = cleanThinking(withoutExpense)
        assertEquals("Great! You want to save 5,000 MMK for the Gucci Bag challenge. That's wonderful progress towards your goal! Currently you've saved 15,000.0 MMK out of 250,000.0 MMK (6% complete), so this new save will help you move forward on your challenge.", cleaned)
    }

    @Test
    fun testNewScreenshots_promptDirectiveRepeatIsStripped() {
        val input = "Write your natural conversational response first. Then, at the VERY END of your message, append a hidden data block:"
        val cleaned = cleanThinking(input)
        assertTrue("Prompt directive echo should be stripped", cleaned.isBlank())
    }

    @Test
    fun testNemotronMentalRefinementIsExtracted() {
        val input = """
            **
               - Warm welcome/affirmation
               - Step-by-step practical guide

               Structure:
               - Acknowledge it's a great first step
               - Step 1: Know where you stand

            4.  **Draft - Mental Refinement:**
               *(Friendly tone, AI assistant persona)*
               "Hey there! That's awesome you're ready to start saving – it's one of the best things you can do for your peace of mind and future self. Here’s a simple, stress-free way to begin:

               1. **Track a little first** – You don’t need a complex spreadsheet right away."
        """.trimIndent()

        val cleaned = cleanThinking(input)
        assertTrue("Extracted reply should start with user greeting", cleaned.startsWith("Hey there! That's awesome"))
        assertFalse("Mental Refinement meta tags should not be present", cleaned.contains("Draft - Mental Refinement"))
    }

    private fun inferCategory(item: String): String {
        val lower = item.lowercase()
        return when {
            lower.contains("ybs") || lower.contains("bus") || lower.contains("taxi") ||
            lower.contains("grab") || lower.contains("car") || lower.contains("transport") ||
            lower.contains("ကားခ") || lower.contains("ယာဉ်") -> "Transportation"
            lower.contains("coffee") || lower.contains("tea") || lower.contains("lunch") ||
            lower.contains("dinner") || lower.contains("food") || lower.contains("breakfast") ||
            lower.contains("drink") || lower.contains("ထမင်း") || lower.contains("လက်ဖက်ရည်") ||
            lower.contains("ကော်ဖီ") || lower.contains("မုန့်") || lower.contains("ညစာ") ||
            lower.contains("မနက်စာ") || lower.contains("နေ့လယ်စာ") || lower.contains("ကွေကာ") ||
            lower.contains("ခေါက်ဆွဲ") || lower.contains("ဟင်း") -> "Food & Dining"
            else -> "Other"
        }
    }

    private fun extractFallbackExpenseOrChallenge(userMessage: String, detectedLang: String): ParsedExpense? {
        val text = userMessage.trim()
        val todayStr = LocalDate.now().toString()

        val enChallengeMatch = Regex("(?i)(?:save|put|deposit)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?\\s*(?:for|into|towards|to)\\s*(.+)").find(text)
        if (enChallengeMatch != null) {
            val amount = enChallengeMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            val title = enChallengeMatch.groupValues[2].trim()
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

        val myChallengeMatch = Regex("(.+?)(?:ဝယ်ဖို့|အတွက်)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks)?\\s*စု(?:မယ်|ချင်)").find(text)
        if (myChallengeMatch != null) {
            val title = myChallengeMatch.groupValues[1].trim()
            val amount = myChallengeMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
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

        val enExpenseMatch = Regex("(?i)(?:log|paid|spent|bought)?\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:mmk|ks|kyats?)?\\s*(?:for|on|at)\\s*(.+)").find(text)
        if (enExpenseMatch != null) {
            val amount = enExpenseMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            val item = enExpenseMatch.groupValues[2].trim()
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

        val myExpenseMatch = Regex("(.+?)\\s*([\\d,]+(?:\\.\\d+)?)\\s*(?:ကျပ်|ks|mmk|ဖိုး|ကုန်|ကုန်တယ်|ကျ|ကျတယ်|ပေးရတယ်|ရှင်း|ရှင်းတယ်)?$").find(text)
        if (myExpenseMatch != null) {
            val item = myExpenseMatch.groupValues[1].trim()
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

    @Test
    fun testFallbackExpenseExtractor_handlesLatestScreenshotInputs() {
        val expense1 = extractFallbackExpenseOrChallenge("Log 1600 for YBS Transportation", "en")
        assertNotNull(expense1)
        assertEquals(1600.0, expense1!!.amount, 0.001)
        assertEquals("Transportation", expense1.category)
        assertEquals("YBS Transportation", expense1.merchant)
        assertFalse(expense1.isChallenge)

        val expense2 = extractFallbackExpenseOrChallenge("Log 1800 for YBS", "en")
        assertNotNull(expense2)
        assertEquals(1800.0, expense2!!.amount, 0.001)
        assertEquals("Transportation", expense2.category)
        assertEquals("YBS", expense2.merchant)

        val challenge = extractFallbackExpenseOrChallenge("save 5000 for Camera", "en")
        assertNotNull(challenge)
        assertEquals(5000.0, challenge!!.amount, 0.001)
        assertEquals("Camera", challenge.challengeTitle)
        assertTrue(challenge.isChallenge)

        val myChallenge = extractFallbackExpenseOrChallenge("Gucci Bag ဝယ်ဖို့ 5000 စုမယ်", "my")
        assertNotNull(myChallenge)
        assertEquals(5000.0, myChallenge!!.amount, 0.001)
        assertEquals("Gucci Bag", myChallenge.challengeTitle)
        assertTrue(myChallenge.isChallenge)

        // New test from screenshot media_1788455263214.jpg
        val myExpenseKone = extractFallbackExpenseOrChallenge("ညစာ ထမင်းကြော် နဲ့ ကွေကာအုတ် 5800 ကုန်", "my")
        assertNotNull("Burmese expense with ကုန် suffix must be recognized", myExpenseKone)
        assertEquals(5800.0, myExpenseKone!!.amount, 0.001)
        assertEquals("Food & Dining", myExpenseKone.category)
        assertTrue("Merchant should contain food items", myExpenseKone.merchant.contains("ညစာ"))
    }

    @Test
    fun testCategoryResolver_resolvesFoodAndOtherAliases() {
        val resolvedFood = com.savingcoach.app.ui.chat.CategoryResolver.resolve("Food", emptyList())
        assertNotNull(resolvedFood)
        assertEquals("Food & Dining", resolvedFood!!.name)

        val resolvedDining = com.savingcoach.app.ui.chat.CategoryResolver.resolve("dining", emptyList())
        assertNotNull(resolvedDining)
        assertEquals("Food & Dining", resolvedDining!!.name)

        val resolvedBurmeseFood = com.savingcoach.app.ui.chat.CategoryResolver.resolve("အစားအသောက်", emptyList())
        assertNotNull(resolvedBurmeseFood)
        assertEquals("Food & Dining", resolvedBurmeseFood!!.name)

        val resolvedBills = com.savingcoach.app.ui.chat.CategoryResolver.resolve("bills", emptyList())
        assertNotNull(resolvedBills)
        assertEquals("Bills & Utilities", resolvedBills!!.name)

        val resolvedTransport = com.savingcoach.app.ui.chat.CategoryResolver.resolve("transport", emptyList())
        assertNotNull(resolvedTransport)
        assertEquals("Transportation", resolvedTransport!!.name)

        val resolvedShopping = com.savingcoach.app.ui.chat.CategoryResolver.resolve("shopping", emptyList())
        assertNotNull(resolvedShopping)
        assertEquals("Shopping", resolvedShopping!!.name)
    }

    @Test
    fun testBurmeseExpense_friedRiceAndTea_fromScreenshot() {
        val input = "ထမင်းကြော် နဲ့ လက်ဖက်ရည် 8500 ကုန်"
        val parsed = extractFallbackExpenseOrChallenge(input, "my")
        assertNotNull(parsed)
        assertEquals(8500.0, parsed!!.amount, 0.001)
        assertEquals("Food & Dining", parsed.category)
        assertEquals("ထမင်းကြော် နဲ့ လက်ဖက်ရည်", parsed.item)
        assertFalse(parsed.isChallenge)

        val burmeseCategoryName = com.savingcoach.app.ui.chat.CategoryResolver.toBurmeseName(parsed.category)
        assertEquals("အစားအသောက်", burmeseCategoryName)
    }

    @Test
    fun testCleanThinking_stripsLeakedExtractionNotes_fromScreenshot() {
        val leakedAiReply = """
1. "15000 for dinner" - This is Food & Dining category, item is "dinner"
2. "4500 for electric bike" - This is Shopping category, item is "electric bike"

For "15000 for dinner":
• amount: 15000
• category: Food & Dining
• item: dinner
• merchant: "" (not specified)
• date: 2026-09-05

For "4500 for electric bike":
• amount: 4500
• category: Shopping
• item: electric bike
• merchant: "" (not specified)
• date: 2026-09-05
        """.trimIndent()
        val cleaned = AiChatRepository.cleanThinking(leakedAiReply)
        assertEquals("", cleaned)
    }

    @Test
    fun testCleanThinking_preservesFriendlyUserFacingReply_whileStrippingLeakedReasoning() {
        val mixedAiReply = """
I've noted your expenses for dinner and electric bike.

1. "15000 for dinner" - This is Food & Dining category, item is "dinner"
2. "4500 for electric bike" - This is Shopping category, item is "electric bike"

For "15000 for dinner":
• amount: 15000
• category: Food & Dining
• item: dinner
• merchant: "" (not specified)
• date: 2026-09-05
        """.trimIndent()
        val cleaned = AiChatRepository.cleanThinking(mixedAiReply)
        assertEquals("I've noted your expenses for dinner and electric bike.", cleaned)
    }

    @Test
    fun testParseExpenseDataList_parsesJsonArrayWithTwoExpenses() {
        val jsonArray = """
[
  {
    "amount": 15000,
    "category": "Food & Dining",
    "item": "dinner",
    "merchant": "",
    "date": "2026-09-05"
  },
  {
    "amount": 4500,
    "category": "Transportation",
    "item": "electric bike",
    "merchant": "",
    "date": "2026-09-05"
  }
]
        """.trimIndent()
        val parsedList = AiChatRepository.parseExpenseDataList(jsonArray, "en")
        assertEquals(2, parsedList.size)
        assertEquals(15000.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("dinner", parsedList[0].item)
        assertEquals(4500.0, parsedList[1].amount, 0.001)
        assertEquals("Transportation", parsedList[1].category)
        assertEquals("electric bike", parsedList[1].item)
    }

    @Test
    fun testExtractFallbackExpenses_twoExpensesFromUserMessage() {
        val userMsg = "log 15000 for dinner and electric bike for 4500"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "en")
        assertEquals(2, parsedList.size)
        assertEquals(15000.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("dinner", parsedList[0].item)
        assertEquals(4500.0, parsedList[1].amount, 0.001)
        assertEquals("Transportation", parsedList[1].category)
        assertEquals("electric bike", parsedList[1].item)
    }

    @Test
    fun testExtractFallbackExpenses_burmeseTwoExpenses() {
        val userMsg = "ညစာ ၁၅၀၀၀ နဲ့ စက်ဘီး ၄၅၀၀"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)
        assertEquals(15000.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("ညစာ", parsedList[0].item)
        assertEquals(4500.0, parsedList[1].amount, 0.001)
        assertEquals("Transportation", parsedList[1].category)
        assertEquals("စက်ဘီး", parsedList[1].item)
    }

    @Test
    fun testExtractFallbackExpenses_singleExpenseWithAndInItem_doesNotSplit() {
        val userMsg = "dinner and coffee for 5000"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "en")
        assertEquals(1, parsedList.size)
        assertEquals(5000.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("dinner and coffee", parsedList[0].item)
    }

    @Test
    fun testCategoryResolver_electricBikeAndBikeAliases() {
        val resolvedElectricBike = com.savingcoach.app.ui.chat.CategoryResolver.resolve("electric bike", emptyList())
        assertNotNull(resolvedElectricBike)
        assertEquals("Transportation", resolvedElectricBike!!.name)

        val resolvedBike = com.savingcoach.app.ui.chat.CategoryResolver.resolve("bike", emptyList())
        assertNotNull(resolvedBike)
        assertEquals("Transportation", resolvedBike!!.name)

        val resolvedBurmeseBike = com.savingcoach.app.ui.chat.CategoryResolver.resolve("စက်ဘီး", emptyList())
        assertNotNull(resolvedBurmeseBike)
        assertEquals("Transportation", resolvedBurmeseBike!!.name)
    }

    @Test
    fun testHistoricalCompoundExpense_autoSplitsIntoTwoCards() {
        val historicalCompoundItem = "dinner and electric bike for 4500"
        val historicalAmount = 15000.0
        val combinedQuery = "${historicalAmount.toLong()} for $historicalCompoundItem"
        val parsedList = AiChatRepository.extractFallbackExpenses(combinedQuery, "en")
        assertEquals(2, parsedList.size)
        assertEquals(15000.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("dinner", parsedList[0].item)

        assertEquals(4500.0, parsedList[1].amount, 0.001)
        assertEquals("Transportation", parsedList[1].category)
        assertEquals("electric bike", parsedList[1].item)
    }

    @Test
    fun testExtractFallbackExpenses_burmeseTwoExpensesWithVerbsAndProduce() {
        val userMsg = "သံပရာသီး 800 ဖိုး နဲ့ ဟင်းနုနွယ် နှစ်စီး 2800 ဝယ်ခဲ့"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertEquals(800.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("သံပရာသီး", parsedList[0].item)

        assertEquals(2800.0, parsedList[1].amount, 0.001)
        assertEquals("Food & Dining", parsedList[1].category)
        assertEquals("ဟင်းနုနွယ် နှစ်စီး", parsedList[1].item)
    }

    @Test
    fun testExtractFallbackExpenses_burmeseSingleExpenseWithVerb() {
        val userMsg = "ဟင်းနုနွယ် 2800 ဝယ်ခဲ့"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(1, parsedList.size)

        assertEquals(2800.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("ဟင်းနုနွယ်", parsedList[0].item)
    }

    @Test
    fun testCategoryResolver_limeAndSpinachProduceAliases() {
        val resolvedLime = com.savingcoach.app.ui.chat.CategoryResolver.resolve("သံပရာသီး", emptyList())
        assertNotNull(resolvedLime)
        assertEquals("Food & Dining", resolvedLime!!.name)

        val resolvedSpinach = com.savingcoach.app.ui.chat.CategoryResolver.resolve("ဟင်းနုနွယ်", emptyList())
        assertNotNull(resolvedSpinach)
        assertEquals("Food & Dining", resolvedSpinach!!.name)

        val resolvedVeg = com.savingcoach.app.ui.chat.CategoryResolver.resolve("ဟင်းသီးဟင်းရွက်", emptyList())
        assertNotNull(resolvedVeg)
        assertEquals("Food & Dining", resolvedVeg!!.name)

        val resolvedEnVeg = com.savingcoach.app.ui.chat.CategoryResolver.resolve("vegetables", emptyList())
        assertNotNull(resolvedEnVeg)
        assertEquals("Food & Dining", resolvedEnVeg!!.name)

        val resolvedEnFruit = com.savingcoach.app.ui.chat.CategoryResolver.resolve("fruit", emptyList())
        assertNotNull(resolvedEnFruit)
        assertEquals("Food & Dining", resolvedEnFruit!!.name)
    }

    @Test
    fun testExtractFallbackExpenses_burmeseBurmeseNumeralsWithVerbs() {
        val userMsg = "သံပရာသီး ၈၀၀ ဖိုး နဲ့ ဟင်းနုနွယ် နှစ်စီး ၂၈၀၀ ဝယ်ခဲ့"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertEquals(800.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("သံပရာသီး", parsedList[0].item)

        assertEquals(2800.0, parsedList[1].amount, 0.001)
        assertEquals("Food & Dining", parsedList[1].category)
        assertEquals("ဟင်းနုနွယ် နှစ်စီး", parsedList[1].item)
    }

    @Test
    fun testExtractFallbackExpenses_morningGloryAndFlashlight() {
        val userMsg = "ကန်စွန်းရွက်နှစ်စီး 5800 နဲ့ ဓာတ်မီး 25000 တန်ဝယ်ခဲ့တယ်"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertEquals(5800.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("ကန်စွန်းရွက်နှစ်စီး", parsedList[0].item)

        assertEquals(25000.0, parsedList[1].amount, 0.001)
        assertEquals("Shopping", parsedList[1].category)
        assertEquals("ဓာတ်မီး", parsedList[1].item)
    }

    @Test
    fun testParseExpenseDataList_reconcilesHallucinatedEnglishItemsWithBurmeseMessage() {
        val jsonArray = """
[
  {
    "amount": 5800,
    "category": "Food & Dining",
    "item": "canned fish 2 cans",
    "date": "2026-09-05"
  },
  {
    "amount": 25000,
    "category": "Shopping",
    "item": "laptop",
    "date": "2026-09-05"
  }
]
        """.trimIndent()
        val userMsg = "ကန်စွန်းရွက်နှစ်စီး 5800 နဲ့ ဓာတ်မီး 25000 တန်ဝယ်ခဲ့တယ်"
        val parsedList = AiChatRepository.parseExpenseDataList(jsonArray, "my", userMsg)
        assertEquals(2, parsedList.size)

        assertEquals(5800.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals("ကန်စွန်းရွက်နှစ်စီး", parsedList[0].item)

        assertEquals(25000.0, parsedList[1].amount, 0.001)
        assertEquals("Shopping", parsedList[1].category)
        assertEquals("ဓာတ်မီး", parsedList[1].item)
    }

    @Test
    fun testCategoryResolver_morningGloryAndFlashlight() {
        val resolvedGlory = com.savingcoach.app.ui.chat.CategoryResolver.resolve("ကန်စွန်းရွက်", emptyList())
        assertNotNull(resolvedGlory)
        assertEquals("Food & Dining", resolvedGlory!!.name)

        val resolvedTorch = com.savingcoach.app.ui.chat.CategoryResolver.resolve("ဓာတ်မီး", emptyList())
        assertNotNull(resolvedTorch)
        assertEquals("Shopping", resolvedTorch!!.name)
    }

    @Test
    fun testExtractFallbackExpenses_spentOnClothesFor50000Today() {
        val userMsg = "I spent on clothes for 50000 today"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "en")
        assertEquals(1, parsedList.size)

        assertEquals(50000.0, parsedList[0].amount, 0.001)
        assertEquals("Shopping", parsedList[0].category)
        assertEquals("clothes", parsedList[0].item)
    }

    @Test
    fun testExtractFallbackExpenseOrChallenge_spentOnClothesFor50000Today() {
        val userMsg = "I spent on clothes for 50000 today"
        val parsed = AiChatRepository.extractFallbackExpenseOrChallenge(userMsg, "en")
        assertNotNull(parsed)

        assertEquals(50000.0, parsed!!.amount, 0.001)
        assertEquals("Shopping", parsed.category)
        assertEquals("clothes", parsed.item)
    }

    @Test
    fun testExtractFallbackExpenses_spent50000OnClothesToday() {
        val userMsg = "spent 50000 on clothes today"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "en")
        assertEquals(1, parsedList.size)

        assertEquals(50000.0, parsedList[0].amount, 0.001)
        assertEquals("Shopping", parsedList[0].category)
        assertEquals("clothes", parsedList[0].item)
    }

    @Test
    fun testExtractFallbackExpenses_multiChallengeEnglish() {
        val userMsg = "save 5000 for Camera and 10000 for Gucci Bag"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "en")
        assertEquals(2, parsedList.size)

        assertTrue(parsedList[0].isChallenge)
        assertEquals("Camera", parsedList[0].challengeTitle)
        assertEquals(5000.0, parsedList[0].amount, 0.001)

        assertTrue(parsedList[1].isChallenge)
        assertEquals("Gucci Bag", parsedList[1].challengeTitle)
        assertEquals(10000.0, parsedList[1].amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenses_multiChallengeBurmese() {
        val userMsg = "Camera အတွက် 5000 နဲ့ Gucci Bag အတွက် 10000 စုမယ်"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertTrue(parsedList[0].isChallenge)
        assertEquals("Camera", parsedList[0].challengeTitle)
        assertEquals(5000.0, parsedList[0].amount, 0.001)

        assertTrue(parsedList[1].isChallenge)
        assertEquals("Gucci Bag", parsedList[1].challengeTitle)
        assertEquals(10000.0, parsedList[1].amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenses_multiChallengeBurmeseWithBothVerbs() {
        val userMsg = "Camera အတွက် 5000 စုမယ် နဲ့ Gucci Bag ဝယ်ဖို့ 10000 ထည့်မယ်"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertTrue(parsedList[0].isChallenge)
        assertEquals("Camera", parsedList[0].challengeTitle)
        assertEquals(5000.0, parsedList[0].amount, 0.001)

        assertTrue(parsedList[1].isChallenge)
        assertEquals("Gucci Bag", parsedList[1].challengeTitle)
        assertEquals(10000.0, parsedList[1].amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenses_mixedExpenseAndChallengeEnglish() {
        val userMsg = "dinner 15000 and save 5000 for Camera"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "en")
        assertEquals(2, parsedList.size)

        assertFalse(parsedList[0].isChallenge)
        assertEquals("dinner", parsedList[0].item)
        assertEquals("Food & Dining", parsedList[0].category)
        assertEquals(15000.0, parsedList[0].amount, 0.001)

        assertTrue(parsedList[1].isChallenge)
        assertEquals("Camera", parsedList[1].challengeTitle)
        assertEquals(5000.0, parsedList[1].amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenses_mixedExpenseAndChallengeBurmese() {
        val userMsg = "dinner 15000 နဲ့ Camera အတွက် 5000 စုမယ်"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertFalse(parsedList[0].isChallenge)
        assertEquals("dinner", parsedList[0].item)
        assertEquals(15000.0, parsedList[0].amount, 0.001)

        assertTrue(parsedList[1].isChallenge)
        assertEquals("Camera", parsedList[1].challengeTitle)
        assertEquals(5000.0, parsedList[1].amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenseOrChallenge_burmeseChallengeVerbs() {
        val p1 = AiChatRepository.extractFallbackExpenseOrChallenge("Camera အတွက် 5000 ထည့်မယ်", "my")
        assertNotNull(p1)
        assertTrue(p1!!.isChallenge)
        assertEquals("Camera", p1.challengeTitle)
        assertEquals(5000.0, p1.amount, 0.001)

        val p2 = AiChatRepository.extractFallbackExpenseOrChallenge("Gucci Bag ဝယ်ဖို့ 10000 စုတယ်", "my")
        assertNotNull(p2)
        assertTrue(p2!!.isChallenge)
        assertEquals("Gucci Bag", p2.challengeTitle)
        assertEquals(10000.0, p2.amount, 0.001)
    }

    @Test
    fun testParseExpenseDataList_multiChallengeJsonArray() {
        val json = """
        [
          {
            "isChallenge": true,
            "challengeTitle": "Camera",
            "action": "prompt_challenge_confirmation",
            "amount": 5000,
            "currency": "MMK"
          },
          {
            "isChallenge": true,
            "challengeTitle": "Gucci Bag",
            "action": "prompt_challenge_confirmation",
            "amount": 10000,
            "currency": "MMK"
          }
        ]
        """.trimIndent()
        val list = AiChatRepository.parseExpenseDataList(json, "en")
        assertEquals(2, list.size)
        assertTrue(list[0].isChallenge)
        assertEquals("Camera", list[0].challengeTitle)
        assertEquals(5000.0, list[0].amount, 0.001)
        assertTrue(list[1].isChallenge)
        assertEquals("Gucci Bag", list[1].challengeTitle)
        assertEquals(10000.0, list[1].amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenseOrChallenge_noAmountChallenges() {
        val p1 = AiChatRepository.extractFallbackExpenseOrChallenge("save for Gucci Bag", "en")
        assertNotNull(p1)
        assertTrue(p1!!.isChallenge)
        assertEquals("Gucci Bag", p1.challengeTitle)
        assertEquals(0.0, p1.amount, 0.001)

        val p2 = AiChatRepository.extractFallbackExpenseOrChallenge("Gucci Bag အတွက် စုမယ်", "my")
        assertNotNull(p2)
        assertTrue(p2!!.isChallenge)
        assertEquals("Gucci Bag", p2.challengeTitle)
        assertEquals(0.0, p2.amount, 0.001)

        val p3 = AiChatRepository.extractFallbackExpenseOrChallenge("Camera စုမယ်", "my")
        assertNotNull(p3)
        assertTrue(p3!!.isChallenge)
        assertEquals("Camera", p3.challengeTitle)
        assertEquals(0.0, p3.amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenses_noAmountChallenges() {
        val listEn = AiChatRepository.extractFallbackExpenses("save for Gucci Bag", "en")
        assertEquals(1, listEn.size)
        assertTrue(listEn[0].isChallenge)
        assertEquals("Gucci Bag", listEn[0].challengeTitle)
        assertEquals(0.0, listEn[0].amount, 0.001)

        val listMy = AiChatRepository.extractFallbackExpenses("Gucci Bag အတွက် စုမယ်", "my")
        assertEquals(1, listMy.size)
        assertTrue(listMy[0].isChallenge)
        assertEquals("Gucci Bag", listMy[0].challengeTitle)
        assertEquals(0.0, listMy[0].amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenses_burmeseTeaShopBreakfast() {
        val userMsg = "စလုံတီး တစ်ခွက် ၅၀၀၀ နဲ့ နံပြား ၂၀၀၀ တန် မနက်စာစားခဲ့တယ်"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertEquals(5000.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)
        assertTrue(parsedList[0].item.contains("စလုံတီး"))

        assertEquals(2000.0, parsedList[1].amount, 0.001)
        assertEquals("Food & Dining", parsedList[1].category)
        assertTrue(parsedList[1].item.contains("နံပြား"))
    }

    @Test
    fun testCategoryResolver_teaShopAndMealAliases() {
        val resolvedTea = com.savingcoach.app.ui.chat.CategoryResolver.resolve("စလုံတီး", emptyList())
        assertNotNull(resolvedTea)
        assertEquals("Food & Dining", resolvedTea!!.name)

        val resolvedNaan = com.savingcoach.app.ui.chat.CategoryResolver.resolve("နံပြား", emptyList())
        assertNotNull(resolvedNaan)
        assertEquals("Food & Dining", resolvedNaan!!.name)
    }

    @Test
    fun testCleanThinking_translationAndBreakdownLeakage() {
        val rawThinking = """
            "စလုံတီး တစ်ခွက် ၅၀၀၀" = "Coffee 1 cup 5000"
            "နံပြား ၂၀၀၀ တန် မနက်စာစားခဲ့တယ်" = "and 2000 breakfast ate/did"

            So this is mentioning two expenses:
            1. Coffee for 5000 MMK
            2. Breakfast for 2000 MMK

            The user's message: "စလုံတီး တစ်ခွက် ၅၀၀၀ နဲ့ နံပြား ၂၀၀၀ တန် မနက်စာစားခဲ့တယ်"

            Breaking it down:
            • "စလုံတီး" = coffee
            • "တစ်ခွက်" = cup
            • "၅၀၀၀" = 5000
            • "နံပြား" = breakfast (or could be "နေ့လယ်စာ" = morning meal)
            • "၂၀၀၀" = 2000
        """.trimIndent()

        val cleaned = AiChatRepository.cleanThinking(rawThinking)
        assertTrue("Leaked thinking should be completely cleaned, was: $cleaned", cleaned.isBlank())
    }

    @Test
    fun testExtractFallbackExpenses_durianQuantityAndTea() {
        val userMsg = "ဒူးရင်းသီး နှစ်လုံး ဝယ်ခဲ့တယ် တစ်လုံး ၅၀၀၀၀ တဲ့ ပြီးတော့ လက်ဖက်ရည် သောက်ခဲ့ ၆၀၀၀ကျ"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertTrue(parsedList[0].item.contains("ဒူးရင်းသီး"))
        assertEquals(100000.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)

        assertEquals("လက်ဖက်ရည်", parsedList[1].item)
        assertEquals(6000.0, parsedList[1].amount, 0.001)
        assertEquals("Food & Dining", parsedList[1].category)
    }

    @Test
    fun testCategoryResolver_durian() {
        val resolved = com.savingcoach.app.ui.chat.CategoryResolver.resolve("ဒူးရင်းသီး", emptyList())
        assertNotNull(resolved)
        assertEquals("Food & Dining", resolved!!.name)
    }

    @Test
    fun testExtractFallbackExpenses_twoChallengesWithoutAmount() {
        val userMsg = "save Gucci Bag and Camera"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "en")
        assertEquals(2, parsedList.size)

        assertTrue(parsedList[0].isChallenge)
        assertEquals("Gucci Bag", parsedList[0].challengeTitle)
        assertEquals(0.0, parsedList[0].amount, 0.001)

        assertTrue(parsedList[1].isChallenge)
        assertEquals("Camera", parsedList[1].challengeTitle)
        assertEquals(0.0, parsedList[1].amount, 0.001)
    }

    @Test
    fun testExtractFallbackExpenses_mixedChallengeWithoutAmountAndExpense() {
        val userMsg = "save Gucci Bag and log 4500 for Tea"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "en")
        assertEquals(2, parsedList.size)

        assertTrue(parsedList[0].isChallenge)
        assertEquals("Gucci Bag", parsedList[0].challengeTitle)
        assertEquals(0.0, parsedList[0].amount, 0.001)

        assertFalse(parsedList[1].isChallenge)
        assertEquals("Tea", parsedList[1].item)
        assertEquals(4500.0, parsedList[1].amount, 0.001)
        assertEquals("Food & Dining", parsedList[1].category)
    }

    @Test
    fun testExtractFallbackExpenses_burmeseTwoChallengesWithoutAmount() {
        val userMsg = "Gucci Bag နဲ့ Camera စုမယ်"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertTrue(parsedList[0].isChallenge)
        assertEquals("Gucci Bag", parsedList[0].challengeTitle)
        assertEquals(0.0, parsedList[0].amount, 0.001)

        assertTrue(parsedList[1].isChallenge)
        assertEquals("Camera", parsedList[1].challengeTitle)
        assertEquals(0.0, parsedList[1].amount, 0.001)
    }

    @Test
    fun testCleanThinking_screenshotThinkingMonologues() {
        val thinking1 = """
            So for this case with two challenges, I need:
            [
            {
        """.trimIndent()
        val cleaned1 = AiChatRepository.cleanThinking(thinking1)
        assertTrue("Thinking monologue 1 should be completely removed, was: '$cleaned1'", cleaned1.isBlank())

        val thinking2 = """
            For the expense part, "Tea" would be Food & Dining category.

            And for mixed expense + challenge:
            "Example for Mixed Expense + Challenge:
            User: "
        """.trimIndent()
        val cleaned2 = AiChatRepository.cleanThinking(thinking2)
        assertTrue("Thinking monologue 2 should be completely removed, was: '$cleaned2'", cleaned2.isBlank())
    }

    @Test
    fun testExtractFallbackExpenses_mangoQuantityAndFriedRice() {
        val userMsg = "သရက်သီး တစ်လုံး ၈၀၀ ငါ ၃လုံးဝယ်ခဲ့တယ် ပြီးတော့ ထမင်းကြော် ၂ပွဲ က ၇၀၀၀ ကုန်တယ်"
        val parsedList = AiChatRepository.extractFallbackExpenses(userMsg, "my")
        assertEquals(2, parsedList.size)

        assertTrue(parsedList[0].item.contains("သရက်သီး"))
        assertEquals(2400.0, parsedList[0].amount, 0.001)
        assertEquals("Food & Dining", parsedList[0].category)

        assertTrue(parsedList[1].item.contains("ထမင်းကြော်"))
        assertEquals(7000.0, parsedList[1].amount, 0.001)
        assertEquals("Food & Dining", parsedList[1].category)
        assertFalse("Item should not contain trailing particle 'က', was: ${parsedList[1].item}", parsedList[1].item.endsWith("က"))
    }

    @Test
    fun testCategoryResolver_mangoAndFriedRice() {
        val mango = com.savingcoach.app.ui.chat.CategoryResolver.resolve("သရက်သီး", emptyList())
        assertNotNull(mango)
        assertEquals("Food & Dining", mango!!.name)

        val friedRice = com.savingcoach.app.ui.chat.CategoryResolver.resolve("ထမင်းကြော်", emptyList())
        assertNotNull(friedRice)
        assertEquals("Food & Dining", friedRice!!.name)
    }

    @Test
    fun testCleanThinking_preservesFinancialReportWithBullets() {
        val report = """
            Here is your financial report for this month:

            • Monthly Budget: 100,000,000 MMK
            • Spent so far: 399,600 MMK
            • Remaining Budget: 99,600,400 MMK (99.6% remaining)
            • Days left in month: 24

            Top spending categories:
            • Transportation: 268,100 MMK
            • Shopping: 1,000 MMK

            You are in a great position! With 24 days left, you have plenty of room to allocate surplus towards your savings challenges.
        """.trimIndent()

        val cleaned = AiChatRepository.cleanThinking(report)
        assertTrue("Financial report should be preserved", cleaned.contains("Monthly Budget: 100,000,000 MMK"))
        assertTrue("Top spending categories should be preserved", cleaned.contains("Transportation: 268,100 MMK"))
        assertTrue("Closing advice should be preserved", cleaned.contains("plenty of room to allocate surplus"))
    }

    @Test
    fun testCleanThinking_preservesNumberedCoachingAdvice() {
        val advice = """
            Here are 3 tips to improve your savings:

            1. **Track everyday expenses**: Keep logging your daily coffee and food expenses to spot trends.
            2. **Automate savings**: Set aside 10% of your income at the start of each month.
            3. **Build an emergency fund**: Aim for 3 to 6 months of living expenses.
        """.trimIndent()

        val cleaned = AiChatRepository.cleanThinking(advice)
        assertTrue("First advice tip should be preserved", cleaned.contains("1. **Track everyday expenses**"))
        assertTrue("Second advice tip should be preserved", cleaned.contains("2. **Automate savings**"))
        assertTrue("Third advice tip should be preserved", cleaned.contains("3. **Build an emergency fund**"))
    }

    @Test
    fun testCleanThinking_preservesConversationalPhrasing() {
        val conversational = """
            Let me help you review your finances this month.
            I will break down your spending for you.
            You have 24 days left in the month to stay on track.
        """.trimIndent()

        val cleaned = AiChatRepository.cleanThinking(conversational)
        assertTrue("Conversational intro should be preserved", cleaned.contains("review your finances"))
        assertTrue("Days left sentence should be preserved", cleaned.contains("24 days left in the month"))
    }

    @Test
    fun testCleanThinking_preservesInvestmentGuidance() {
        val investmentAdvice = """
            Here is a review of your investment holdings and some educational insights:

            Looking at your portfolio:
            • BTC (0.55 BTC): Crypto asset offering high potential upside with elevated market volatility.
            • SOL (0.1 SOL): Smart contract platform asset.
            • GLD (1 unit): Gold ETF, acting as an inflation hedge and store of value.

            Key Coaching Principles:
            1. **Diversification**: Balance volatile crypto assets with traditional equities and stable cash reserves.
            2. **Emergency Buffer**: Maintain 3-6 months of liquid expenses before adding speculative holdings.
            3. **Dollar-Cost Averaging**: Invest fixed amounts regularly to smooth out market volatility.

            (Note: This is educational guidance to assist your financial planning, not professional financial advice.)
        """.trimIndent()

        val cleaned = AiChatRepository.cleanThinking(investmentAdvice)
        assertTrue("Holdings should be preserved", cleaned.contains("BTC (0.55 BTC)"))
        assertTrue("GLD should be preserved", cleaned.contains("GLD (1 unit)"))
        assertTrue("Coaching principles should be preserved", cleaned.contains("1. **Diversification**"))
        assertTrue("Educational note should be preserved", cleaned.contains("educational guidance"))
    }

    @Test
    fun testPromptBuilder_containsEmpatheticPersonaAnd5Rules() {
        val prompt = PromptBuilder.buildSystemPrompt()
        assertTrue(prompt.contains("You are an empathetic, proactive personal financial coach named Saving Coach"))
        assertTrue(prompt.contains("Do not merely list raw figures; interpret what they mean for the user's daily life"))
        assertTrue(prompt.contains("Calculate and highlight a \"Daily Safe-to-Spend\" amount based on remaining days"))
        assertTrue(prompt.contains("Call out the single biggest spending leak with zero judgment"))
        assertTrue(prompt.contains("Provide exactly ONE practical, low-effort step the user can take this week"))
        assertTrue(prompt.contains("Keep the tone encouraging, concise, and focused on behavioral change"))
    }

    @Test
    fun testCleanThinking_preservesEmpatheticCoachingSummary() {
        val report = """
            📊 Financial Overview & Coach Report

            You have 950,000 MMK left (95.0%) with 24 days remaining. That gives you solid breathing room for your day-to-day routine!

            🎯 Daily Safe-to-Spend:
            • 39,583 MMK / day for the remaining 24 days of the month.

            💡 Biggest Outflow (Zero Judgment):
            • Transportation: 268,100 MMK (84.3% of your total spend)
            It's completely natural for this category to take the biggest bite—knowing where your money goes is your superpower, not a mistake.

            🌱 One Step This Week:
            • Try packing a snack or making one meal at home this week to effortlessly keep an extra 3,000 MMK in your pocket.
        """.trimIndent()

        val cleaned = AiChatRepository.cleanThinking(report)
        assertTrue(cleaned.contains("Daily Safe-to-Spend"))
        assertTrue(cleaned.contains("Biggest Outflow (Zero Judgment)"))
        assertTrue(cleaned.contains("One Step This Week"))
        assertTrue(cleaned.contains("39,583 MMK / day"))
    }

    @Test
    fun testCleanThinking_preservesBurmeseEmpatheticCoachingSummary() {
        val burmeseReport = """
            📊 သင့်ရဲ့ ဘဏ္ဍာရေးအခြေအနေ သုံးသပ်ချက်

            လစဉ်ဘတ်ဂျက်ထဲက ၉၅၀,၀၀၀ MMK (၉၅.၀%) ကျန်ရှိပြီး လကုန်ရန် ၂၄ ရက်ကျန်ပါသေးတယ်။ နေ့စဉ်ဘဝမှာ စိတ်အေးချမ်းသာစွာ သုံးစွဲနိုင်တဲ့ လုံလောက်တဲ့ အခြေအနေမှာ ရှိနေပါတယ်။

            🎯 နေ့စဉ် စိတ်ချလက်ချသုံးနိုင်သော ပမာဏ (Daily Safe-to-Spend):
            • တစ်ရက်လျှင် ၃၉,၅၈၃ MMK (ကျန်ရှိသော ၂၄ ရက်အတွက်)

            💡 အများဆုံး သုံးစွဲထားသည့် ကဏ္ဍ (Zero Judgment):
            • သယ်ယူပို့ဆောင်ရေး: ၂၆၈,၁၀၀ MMK (၈၄.၃%)
            ဒီကဏ္ဍမှာ အသုံးများတာ သဘာဝကျပြီး နားလည်ပေးလို့ ရပါတယ် — ကိုယ့်ငွေ ဘယ်ရောက်သွားလဲဆိုတာ သိရှိထားခြင်းက အကောင်းဆုံး စတင်မှုဖြစ်ပါတယ်။

            🌱 ဒီတစ်ပတ် လုပ်ဆောင်နိုင်မည့် ရိုးရှင်းသော အဆင့်:
            • သင့်ရဲ့ "Gucci Bag" စိန်ခေါ်မှုထဲသို့ ဒီနေ့ ၁,၀၀၀ MMK ခန့် စတင်ထည့်ဝင်ပြီး ငွေစုအလေ့အကျင့်ကို အရှိန်ယူလိုက်ပါ။
        """.trimIndent()

        val cleaned = AiChatRepository.cleanThinking(burmeseReport)
        assertTrue(cleaned.contains("နေ့စဉ် စိတ်ချလက်ချသုံးနိုင်သော ပမာဏ"))
        assertTrue(cleaned.contains("Zero Judgment"))
        assertTrue(cleaned.contains("ဒီတစ်ပတ် လုပ်ဆောင်နိုင်မည့် ရိုးရှင်းသော အဆင့်"))
    }

    @Test
    fun testIsFinancialReportQuery_matchesReportMyFinancial() {
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("report my financial"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("report about my financial"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("all the spending and saving"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("how my inventory is going"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("financial overview"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("analysis my wealth"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("analyze my wealth"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("wealth analysis"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("my net worth"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("portfolio analysis"))
        assertTrue(AiFinanceAssistant.isFinancialReportQuery("ကြွယ်ဝမှု"))
        // Disambiguation: "report expense" must NOT be treated as a full financial overview
        assertFalse(AiFinanceAssistant.isFinancialReportQuery("report expense"))
        assertFalse(AiFinanceAssistant.isFinancialReportQuery("expense report"))
    }

    @Test
    fun testIsExpenseReportQuery_matchesReportExpense() {
        assertTrue(AiFinanceAssistant.isExpenseReportQuery("report expense"))
        assertTrue(AiFinanceAssistant.isExpenseReportQuery("expense report"))
        assertTrue(AiFinanceAssistant.isExpenseReportQuery("expenses report"))
        assertTrue(AiFinanceAssistant.isExpenseReportQuery("spending report"))
        assertTrue(AiFinanceAssistant.isExpenseReportQuery("what did i spend"))
        assertTrue(AiFinanceAssistant.isExpenseReportQuery("အသုံးစရိတ် အစီရင်ခံစာ"))
        // Financial overview queries must NOT be treated as expense report
        assertFalse(AiFinanceAssistant.isExpenseReportQuery("report my financial"))
        assertFalse(AiFinanceAssistant.isExpenseReportQuery("financial overview"))
    }

    @Test
    fun testReportCompleteness_detectsTruncatedCutoff() {
        val screenshotCutoff = """
            📊 Financial Overview & Coach Report

            Budget Health:
            • Monthly Budget: 100,
        """.trimIndent()

        assertTrue(
            "Screenshot cutoff ending in '100,' must be identified as truncated",
            AiFinanceAssistant.isTruncatedOrIncomplete(screenshotCutoff)
        )

        val completeReport = """
            📊 Financial Overview & Coach Report

            You have 950,000 MMK left (95.0%) with 24 days remaining. That gives you solid breathing room for your day-to-day routine!

            🎯 Daily Safe-to-Spend:
            • 39,583 MMK / day for the remaining 24 days of the month.

            💡 Biggest Outflow (Zero Judgment):
            • Transportation: 268,100 MMK (84.3% of your total spend)
            It's completely natural for this category to take the biggest bite—knowing where your money goes is your superpower, not a mistake.

            🌱 One Step This Week:
            • Try packing a snack or making one meal at home this week to effortlessly keep an extra 3,000 MMK in your pocket.
        """.trimIndent()

        assertFalse("Complete report must not be identified as truncated", AiFinanceAssistant.isTruncatedOrIncomplete(completeReport))
    }

    @Test
    fun testReportCompleteness_detectsScreenshotDirectiveCutoff() {
        val screenshotDirectiveCutoff = """
            1. Life Interpretation: What remaining funds and days left mean for daily peace of mind and realistic breathing room.
            2. Daily Safe-to-Spend: Specific daily spending guardrail based on remaining days (Remaining Budget / 
        """.trimIndent()

        assertTrue(
            "Screenshot response repeating prompt instructions and cutting off at '(Remaining Budget /' must be detected",
            AiFinanceAssistant.isTruncatedOrIncomplete(screenshotDirectiveCutoff)
        )
    }

    @Test
    fun testScreenshot1_isSavingsReportQueryMatches() {
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("report about my savings"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("report my savings"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("savings report"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("saving report"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("my savings"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("ငွေစုတာ အစီရင်ခံစာ"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("ငွေစု အစီရင်ခံစာ"))

        // Disambiguation
        assertFalse(AiFinanceAssistant.isSavingsReportQuery("report about my financial"))
        assertFalse(AiFinanceAssistant.isSavingsReportQuery("report expense"))
        assertFalse(AiFinanceAssistant.isFinancialReportQuery("report about my savings"))
        assertFalse(AiFinanceAssistant.isExpenseReportQuery("report about my savings"))
    }

    @Test
    fun testScreenshot2_isSavingAdviceQueryMatches() {
        assertTrue(AiFinanceAssistant.isSavingAdviceQuery("Bluetooth speaker လိုချင်တာ ၅၀၀၀၀ တဲ့ ဘယ်လိုစုရင်ကောင်းမလဲ"))
        assertTrue(AiFinanceAssistant.isSavingAdviceQuery("ဖုန်းဝယ်ချင်လို့ ၁၀၀၀၀၀ ဘယ်လိုစုရမလဲ"))
        assertTrue(AiFinanceAssistant.isSavingAdviceQuery("ဘယ်လိုငွေစုရမလဲ"))
        assertTrue(AiFinanceAssistant.isSavingAdviceQuery("ငွေဘယ်လိုစုရမလဲ"))
        assertTrue(AiFinanceAssistant.isSavingAdviceQuery("How to save 50000 for bluetooth speaker"))
        assertTrue(AiFinanceAssistant.isSavingAdviceQuery("how to save money"))

        // Disambiguation
        assertFalse(AiFinanceAssistant.isSavingAdviceQuery("report about my savings"))
        assertFalse(AiFinanceAssistant.isSavingAdviceQuery("report expense"))
        assertFalse(AiFinanceAssistant.isSavingAdviceQuery("report my financial"))
    }

    @Test
    fun testScreenshot2_parseSavingGoalExtractsItemAndAmount() {
        val goal1 = AiFinanceAssistant.parseSavingGoal("Bluetooth speaker လိုချင်တာ ၅၀၀၀၀ တဲ့ ဘယ်လိုစုရင်ကောင်းမလဲ")
        assertEquals("Bluetooth speaker", goal1.item)
        assertEquals(50000.0, goal1.amount, 0.01)

        val goal2 = AiFinanceAssistant.parseSavingGoal("50000 တန် Bluetooth speaker ဝယ်ချင်လို့ ဘယ်လိုစုရမလဲ")
        assertEquals("Bluetooth speaker", goal2.item)
        assertEquals(50000.0, goal2.amount, 0.01)

        val goal3 = AiFinanceAssistant.parseSavingGoal("how to save 50000 for bluetooth speaker")
        assertTrue(goal3.item.contains("bluetooth speaker", ignoreCase = true))
        assertEquals(50000.0, goal3.amount, 0.01)
    }

    @Test
    fun testScreenshot2_detectsCorruptedMultilingualBurmeseOutput() {
        val userQuery = "Bluetooth speaker လိုချင်တာ ၅၀၀၀၀ တဲ့ ဘယ်လိုစုရင်ကောင်းမလဲ"
        val screenshot2Reply = """
            မင်္ဂလာပါ! Bluetooth speaker ၅၀၀၀၀ ကျပ် စုရင်ကောင်းလို့ အသုံးစရိတ်အားလုံး အောက်ပါ လွယ်ကွက်များကို စ monasterize လေ့လာနိုင်ပါတယ်။

            1. စရိယာစနစ်ကို ဖန်တီးပါ
            • ၅၀၀၀၀ ကျပ် ကို ၁၀ ရက်တွက်လိုကွယ်လဲ?
            ... ၅၀၀၀၀ ÷ ၁၀ = ၅၀၀၀ ကျပ် / ရက်
            ... သင့်စာသားက လေ့လာပြီး စာသားစုလုံးကို ၅၀၀၀ ကျပ်씩 စုရ-cache လုပ်ရင် ၁၀ ရက်မှာ achievement လုပ်နိုင်ပါတယ်!

            2. စက်ပေးမှုတစ်ခုကို ခွဲထားပါ
            • နေ့တစ်ရက်မှာ ၅၀၀၀ ကျပ် စုရ-cache လုပ်ရလိုအပ်လဲ, လက်နက်နေ စားနောက်နောက်ကွာ လုပ်သော ပတ္တက်ရာ အသုံးစရိတ် ခွဲထားရင် အရင်အထိ ၅၀၀၀၀ ကျပ်ကိုလည်း အ-cache လုပ်နိုင်ပါတယ်။
        """.trimIndent()

        // 1. Must be identified as corrupted by isCorruptedBurmeseResponse
        assertTrue(
            "Screenshot 2 corrupted text containing Korean, hybrid tokens, and hallucinated English must be detected",
            AiChatRepository.isCorruptedBurmeseResponse(screenshot2Reply, userQuery)
        )

        // 2. cleanThinking must reject or wipe corrupted foreign text
        val cleaned = cleanThinking(screenshot2Reply)
        assertTrue(
            "cleanThinking should reject text with Korean particles and hybrid tokens",
            cleaned.isBlank()
        )

        // 3. Valid Burmese response repeating user's English product name must NOT be flagged as corrupted
        val validBurmeseReply = """
            Bluetooth speaker (၅၀,၀၀၀ ကျပ်) ဝယ်ယူရန် ငွေစုအကြံပြုချက်များ ဖြစ်ပါတယ်-
            ၁။ ရက်တို စုဆောင်းနည်း (၁၀ ရက်): တစ်ရက် ၅,၀၀၀ ကျပ် စုဆောင်းပါက ၁၀ ရက်အတွင်း ရရှိပါမည်။
            ၂။ အသင့်အတင့် စုဆောင်းနည်း (ရက် ၂၀): တစ်ရက် ၂,၅၀၀ ကျပ် စုဆောင်းပါက ရက် ၂၀ အတွင်း ရရှိပါမည်။
            ၃။ အေးအေးဆေးဆေး စုဆောင်းနည်း (ရက် ၅၀): တစ်ရက် ၁,၀၀၀ ကျပ် စုဆောင်းပါက ရက် ၅၀ အတွင်း ရရှိပါမည်။
        """.trimIndent()

        assertFalse(
            "Valid Burmese reply mentioning user's Bluetooth speaker must not be marked corrupted",
            AiChatRepository.isCorruptedBurmeseResponse(validBurmeseReply, userQuery)
        )
    }

    @Test
    fun testScreenshot3_parseTimeframe_detectsMonthsWeeksDays() {
        val (days1, text1) = AiFinanceAssistant.parseTimeframe("how to save 300000 in one month")
        assertEquals(30, days1)
        assertEquals("1 Month", text1)

        val (days2, text2) = AiFinanceAssistant.parseTimeframe("how to save 50000 in 2 weeks")
        assertEquals(14, days2)
        assertEquals("2 Weeks", text2)

        val (days3, text3) = AiFinanceAssistant.parseTimeframe("၃၀၀၀၀၀ ကို ၁ လအတွင်း ဘယ်လိုစုရမလဲ")
        assertEquals(30, days3)
        assertEquals("၁ လ", text3)
    }

    @Test
    fun testScreenshot3_parseSavingGoal_doesNotTreatTimeframeAsItem() {
        val goal = AiFinanceAssistant.parseSavingGoal("how to save 300000 in one month")
        assertEquals(300000.0, goal.amount, 0.01)
        assertEquals(30, goal.targetDays)
        assertEquals("1 Month", goal.timeframeText)
        assertEquals("Item should be empty, not 'in one month'", "", goal.item)

        val goalWithItem = AiFinanceAssistant.parseSavingGoal("how to save 300000 for a laptop in one month")
        assertEquals(300000.0, goalWithItem.amount, 0.01)
        assertEquals(30, goalWithItem.targetDays)
        assertEquals("laptop", goalWithItem.item)
    }

    @Test
    fun testScreenshot3_buildSavingAdviceResponse_calculatesTimeframePaceCorrectly() {
        val query = "how to save 300000 in one month"
        val goal = AiFinanceAssistant.parseSavingGoal(query)
        assertEquals(30, goal.targetDays)

        // Test English advice generation
        val enAdvice = AiFinanceAssistant.buildSavingAdviceResponse("test_user", query, "en")
        assertFalse("Must never output 'Saving Plan for in one month'", enAdvice.contains("for in one month"))
        assertTrue("Must have proper header with 1 Month and 30 Days", enAdvice.contains("300,000 MMK in 1 Month (30 Days)"))
        assertTrue("Daily pace must be 10,000 MMK / day (300,000 / 30)", enAdvice.contains("10,000 MMK / day"))
        assertTrue("Weekly pace must be 75,000 MMK / week (300,000 / 4)", enAdvice.contains("75,000 MMK / week"))

        // Test Burmese advice generation
        val myQuery = "၃၀၀၀၀၀ ကို ၁ လအတွင်း ဘယ်လိုစုရမလဲ"
        val myAdvice = AiFinanceAssistant.buildSavingAdviceResponse("test_user", myQuery, "my")
        assertFalse(myAdvice.contains("for in one month"))
        assertTrue("Must have Burmese daily pace of 10,000 kyats", myAdvice.contains("၁၀,၀၀၀ ကျပ်"))
        assertTrue("Must have Burmese weekly pace of 75,000 kyats", myAdvice.contains("၇၅,၀၀၀ ကျပ်"))
    }

    @Test
    fun testScreenshot5_cleanThinking_rejectsOrphanedHeader() {
        val orphanedHeader = "## Gold as an Investment"
        val cleaned = cleanThinking(orphanedHeader)
        assertTrue(
            "cleanThinking must reject output consisting solely of a markdown header",
            cleaned.isBlank()
        )

        val headerWithBody = """
            ## Gold as an Investment

            Gold can serve as a store of value and an inflation hedge.
        """.trimIndent()
        val cleanedWithBody = cleanThinking(headerWithBody)
        assertTrue("Must preserve header when substantive body text is present", cleanedWithBody.contains("Gold as an Investment"))
        assertTrue("Must preserve body content", cleanedWithBody.contains("store of value"))

        val trailingHeader = """
            Gold can serve as a store of value.

            ## Considerations
        """.trimIndent()
        val cleanedTrailing = cleanThinking(trailingHeader)
        assertFalse("Must strip trailing orphaned header", cleanedTrailing.contains("## Considerations"))
        assertTrue("Must preserve substantive content", cleanedTrailing.contains("store of value"))
    }

    @Test
    fun testScreenshot5_isCorruptedBurmeseResponse_allowsFinancialTermsAndParenthesizedEnglish() {
        val userQuery = "ရွှေဝယ်သင့်လား"

        // 1. Valid Burmese gold advice with parenthesized English annotations and financial loanwords
        val burmeseGoldReply = """
            ရွှေ (Gold) သည် ငွေကြေးဖောင်းပွမှု (Inflation) ကို ကာကွယ်ပေးနိုင်သော အကာအကွယ်ပိုင်ဆိုင်မှု (Defensive Asset / Hedge) တစ်ခု ဖြစ်ပါသည်။
            အရေးပေါ်သုံးငွေ (Emergency Fund) အရင်ရှိပါစေ။ DCA စနစ်ဖြင့် ပုံမှန်ခွဲဝေဝယ်ယူပါ။
        """.trimIndent()

        assertFalse(
            "Valid Burmese gold advice with parenthesized English terms and financial loanwords must NOT be flagged as corrupted",
            AiChatRepository.isCorruptedBurmeseResponse(burmeseGoldReply, userQuery)
        )

        // 2. Severe English leakage in Burmese mode must still be detected
        val leakedReply = "ရွှေက ကောင်းပါတယ်။ Here is why the user should buy gold and breaking down expenses."
        assertTrue(
            "Severe prompt leakage / English sentences in Burmese mode must be flagged as corrupted",
            AiChatRepository.isCorruptedBurmeseResponse(leakedReply, userQuery)
        )
    }

    @Test
    fun testScreenshot5_isInvestmentAdviceQuery_detectsEnglishAndBurmeseQueries() {
        // English gold queries
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("gold should I buy today"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("should I buy gold"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("buy gold today"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("is gold a good investment"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("should i invest in gold"))

        // Burmese gold queries
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("ရွှေဝယ်သင့်လား"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("ရွှေဝယ်ရမလား"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("ရွှေစုသင့်လား"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("ရွှေရင်းနှီးမြှုပ်နှံမှု"))

        // Crypto queries
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("should I buy bitcoin"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("crypto investment"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("ဘစ်ကွိုင် ဝယ်သင့်လား"))

        // Stock queries
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("how to invest in stocks"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("စတော့ရှယ်ယာ ဝယ်သင့်လား"))

        // General queries
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("investment advice"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("where should I invest"))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery("ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်"))

        // Negatives: expense logging or reports should NOT be investment advice
        assertFalse(AiFinanceAssistant.isInvestmentAdviceQuery("bought gold for 100000 mmk"))
        assertFalse(AiFinanceAssistant.isInvestmentAdviceQuery("spent 50000 on shopping"))
        assertFalse(AiFinanceAssistant.isInvestmentAdviceQuery("report about my financial"))
        assertFalse(AiFinanceAssistant.isInvestmentAdviceQuery("report about my savings"))
    }

    @Test
    fun testScreenshot5_isInvestmentAdviceIncomplete_detectsOrphanedHeaderAndCutoffs() {
        assertTrue(
            "Single markdown header must be detected as incomplete",
            AiFinanceAssistant.isInvestmentAdviceIncomplete("## Gold as an Investment")
        )
        assertTrue(
            "Short response under 80 chars must be detected as incomplete",
            AiFinanceAssistant.isInvestmentAdviceIncomplete("Gold is an inflation hedge.")
        )
        assertTrue(
            "Response ending with trailing punctuation cutoff must be detected as incomplete",
            AiFinanceAssistant.isInvestmentAdviceIncomplete("Gold is a defensive asset and store of value that protects against inflation and currency risk, so you should consider:")
        )

        val completeAdvice = """
            🟡 Gold as an Investment: Coach's Perspective

            Gold serves as a classic defensive asset, store of value, and hedge against inflation.
            1. Secure your emergency fund first.
            2. Allocate 5% to 10% of total wealth.
            3. Use Dollar-Cost Averaging (DCA).

            (Note: This is educational guidance to support your financial planning, not professional financial advice.)
        """.trimIndent()

        assertFalse(
            "Complete advice must NOT be marked incomplete",
            AiFinanceAssistant.isInvestmentAdviceIncomplete(completeAdvice)
        )
    }

    @Test
    fun testScreenshot5_buildInvestmentAdviceResponse_providesStructuredCoaching() {
        // 1. English Gold Advice
        val enGold = AiFinanceAssistant.buildInvestmentAdviceResponse("test_user", "gold should I buy today", "en")
        assertTrue("Must have gold title", enGold.contains("Gold as an Investment: Coach's Perspective"))
        assertTrue("Must advise emergency fund first", enGold.contains("Emergency Fund First"))
        assertTrue("Must mention 5% – 10% allocation", enGold.contains("5% – 10%"))
        assertTrue("Must mention DCA", enGold.contains("Dollar-Cost Averaging (DCA)"))
        assertTrue("Must include educational disclaimer", enGold.contains("(Note: This is educational guidance"))
        assertTrue("Must conclude with coach question", enGold.contains("Would you like to set up a monthly saving target"))

        // 2. Burmese Gold Advice
        val myGold = AiFinanceAssistant.buildInvestmentAdviceResponse("test_user", "ရွှေဝယ်သင့်လား", "my")
        assertTrue("Must have Burmese gold title", myGold.contains("ရွှေရင်းနှီးမြှုပ်နှံမှုနှင့် ပတ်သက်၍ အကြံပြုချက်"))
        assertTrue("Must advise emergency fund in Burmese", myGold.contains("အရေးပေါ်သုံးငွေ (Emergency Fund) အရင်ရှိပါစေ"))
        assertTrue("Must mention 5% to 10% in Burmese", myGold.contains("၅% မှ ၁၀%"))
        assertTrue("Must mention DCA in Burmese", myGold.contains("Dollar-Cost Averaging - DCA"))
        assertTrue("Must mention Academy gold bars", myGold.contains("အကယ်ဒမီ ရွှေတုံး/ရွှေပြား"))
        assertTrue("Must include Burmese educational disclaimer", myGold.contains("(မှတ်ချက် - ဤအချက်အလက်သည် ငွေကြေးစီမံခန့်ခွဲမှု လေ့လာသင်ယူရန်အတွက်သာဖြစ်ပြီး"))
        assertTrue("Must conclude with Burmese coach question", myGold.contains("Saving Challenge"))

        // 3. Crypto Advice
        val enCrypto = AiFinanceAssistant.buildInvestmentAdviceResponse("test_user", "should I buy bitcoin", "en")
        assertTrue("Must have crypto title", enCrypto.contains("Cryptocurrency & Bitcoin: Coach's Perspective"))
        assertTrue("Must recommend 1% to 5% allocation", enCrypto.contains("1% – 5% Allocation"))

        // 4. Stock Advice
        val enStock = AiFinanceAssistant.buildInvestmentAdviceResponse("test_user", "how to invest in stocks", "en")
        assertTrue("Must have stock title", enStock.contains("Stock Market & Equities: Coach's Perspective"))
        assertTrue("Must recommend diversification and ETFs", enStock.contains("Index Funds", ignoreCase = true))
    }

    @Test
    fun testScreenshot6_isNewsQuery_detectsNewsQueries() {
        // Exact query from user's Screenshot 6
        assertTrue(AiFinanceAssistant.isNewsQuery("today hot news for crypto and make it recap"))

        // Common news and recap queries
        assertTrue(AiFinanceAssistant.isNewsQuery("crypto news"))
        assertTrue(AiFinanceAssistant.isNewsQuery("hot news"))
        assertTrue(AiFinanceAssistant.isNewsQuery("market news"))
        assertTrue(AiFinanceAssistant.isNewsQuery("stock update"))
        assertTrue(AiFinanceAssistant.isNewsQuery("bitcoin news"))
        assertTrue(AiFinanceAssistant.isNewsQuery("daily market update"))
        assertTrue(AiFinanceAssistant.isNewsQuery("give me a news recap"))

        // Burmese news queries
        assertTrue(AiFinanceAssistant.isNewsQuery("သတင်း"))
        assertTrue(AiFinanceAssistant.isNewsQuery("ခရစ်ပတို သတင်း"))
        assertTrue(AiFinanceAssistant.isNewsQuery("စျေးကွက်သတင်း"))
        assertTrue(AiFinanceAssistant.isNewsQuery("ဒီနေ့ သတင်းအကျဉ်းချုပ် ပြောပြပါ"))

        // Negatives: expense logging with amounts, reports
        assertFalse(AiFinanceAssistant.isNewsQuery("spent 5000 on news magazine"))
        assertFalse(AiFinanceAssistant.isNewsQuery("report about my savings"))
        assertFalse(AiFinanceAssistant.isNewsQuery("report about my financial"))
        assertFalse(AiFinanceAssistant.isNewsQuery("report expense"))
    }

    @Test
    fun testScreenshot6_isNewsResponseIncomplete_detectsRefusalsAndCutoffs() {
        // Screenshot 6 exact refusal response
        val screenshot6Refusal = "I don't have access to today's specific crypto market news in my system — the latest market news is currently unavailable. However, I can share some general crypto educational recap if that would be helpful! ... What would you prefer?"
        assertTrue(
            "Screenshot 6 refusal must be detected as incomplete/refusal",
            AiFinanceAssistant.isNewsResponseIncomplete(screenshot6Refusal)
        )

        // Other common refusals
        assertTrue(AiFinanceAssistant.isNewsQuery("crypto news"))
        assertTrue(AiFinanceAssistant.isNewsResponseIncomplete("Latest market news is currently unavailable right now."))
        assertTrue(AiFinanceAssistant.isNewsResponseIncomplete("I do not have access to live crypto data."))
        assertTrue(AiFinanceAssistant.isNewsResponseIncomplete("သတင်းများ မရရှိနိုင်ပါ နောက်မှ ပြန်ကြိုးစားပါ"))

        // Trailing cutoff
        val cutoffNews = """
            Here is today's crypto news recap:
            1. Bitcoin crossed $95,000 following institutional ETF inflows,
        """.trimIndent()
        assertTrue(
            "Trailing punctuation cutoff must be detected as incomplete",
            AiFinanceAssistant.isNewsResponseIncomplete(cutoffNews)
        )

        // Complete valid response
        val validNewsRecap = """
            📰 Today's Crypto & Market News Recap

            🔥 Top Headlines & Key Updates:
            1. **Bitcoin hits new high amid strong ETF demand** (CoinTelegraph)
               • Crypto markets surged today with Bitcoin approaching new records.

            📊 Market Takeaways:
            • Strong institutional inflows continue to support market momentum.

            💡 Coach Perspective & Smart Strategy:
            • Stick to Dollar-Cost Averaging (DCA) and do not FOMO at all-time highs.
        """.trimIndent()
        assertFalse(
            "Complete recap must not be marked incomplete",
            AiFinanceAssistant.isNewsResponseIncomplete(validNewsRecap)
        )
    }

    @Test
    fun testScreenshot6_buildNewsRecapResponse_synthesizesLiveAndOfflineRecaps() {
        val sampleNews = listOf(
            FinnhubNewsResponse(
                id = 1,
                headline = "Bitcoin holds steady near \$90,000 as institutional demand grows",
                source = "CoinTelegraph",
                summary = "Cryptocurrency markets demonstrated resilience this week as Bitcoin stabilized near \$90,000.",
                url = "https://cointelegraph.com/news/btc-steady"
            ),
            FinnhubNewsResponse(
                id = 2,
                headline = "Ethereum staking yields rise following protocol upgrade",
                source = "Yahoo Finance",
                summary = "Ethereum network activity reached monthly peaks after smart contract improvements.",
                url = "https://finance.yahoo.com/eth"
            )
        )

        // 1. English Crypto News Recap with live headlines
        val enRecap = AiFinanceAssistant.buildNewsRecapResponse("today hot news for crypto and make it recap", "en", sampleNews)
        assertTrue("Must include title", enRecap.contains("Today's Crypto & Market News Recap"))
        assertTrue("Must include headline 1", enRecap.contains("Bitcoin holds steady"))
        assertTrue("Must include source 1", enRecap.contains("CoinTelegraph"))
        assertTrue("Must include summary 1", enRecap.contains("Cryptocurrency markets demonstrated resilience"))
        assertTrue("Must include headline 2", enRecap.contains("Ethereum staking yields rise"))
        assertTrue("Must include source 2", enRecap.contains("Yahoo Finance"))
        assertTrue("Must include takeaways", enRecap.contains("Market Takeaways:"))
        assertTrue("Must include coach strategy", enRecap.contains("Coach Perspective & Smart Strategy:"))
        assertTrue("Must advise DCA", enRecap.contains("Dollar-Cost Averaging (DCA)"))

        // 2. Burmese News Recap with live headlines
        val myRecap = AiFinanceAssistant.buildNewsRecapResponse("ခရစ်ပတို သတင်းအကျဉ်းချုပ်", "my", sampleNews)
        assertTrue("Must include Burmese title", myRecap.contains("ယနေ့ ခရစ်ပတိုနှင့် စျေးကွက်သတင်း အကျဉ်းချုပ်"))
        assertTrue("Must include headline in English cleanly", myRecap.contains("Bitcoin holds steady"))
        assertTrue("Must include Burmese headlines header", myRecap.contains("အဓိက သတင်းခေါင်းစဉ်များနှင့် အကျဉ်းချုပ်:"))
        assertTrue("Must include Burmese takeaways header", myRecap.contains("စျေးကွက် သုံးသပ်ချက်:"))
        assertTrue("Must include Burmese coach header", myRecap.contains("ငွေကြေးအကြံပေး (Coach) ၏ အကြံပြုချက်:"))
        assertTrue("Must advise DCA in Burmese", myRecap.contains("Dollar-Cost Averaging (DCA)"))

        // 3. Offline / Empty News Recap fallback
        val offlineRecapEn = AiFinanceAssistant.buildNewsRecapResponse("today hot news for crypto and make it recap", "en", emptyList())
        assertTrue("Must include crypto title in offline recap", offlineRecapEn.contains("Today's Crypto & Market News Recap"))
        assertTrue("Must include coach smart strategy", offlineRecapEn.contains("Coach's Smart Strategy:"))
        assertTrue("Must mention DCA in offline recap", offlineRecapEn.contains("Dollar-Cost Averaging (DCA)"))

        val offlineRecapMy = AiFinanceAssistant.buildNewsRecapResponse("သတင်း", "my", emptyList())
        assertTrue("Must include Burmese title in offline recap", offlineRecapMy.contains("ယနေ့ ဘဏ္ဍာရေးနှင့် စျေးကွက်သတင်း အကျဉ်းချုပ်"))
        assertTrue("Must include Burmese coach guidance", offlineRecapMy.contains("ငွေကြေးအကြံပေး (Coach) ၏ အကြံပြုချက်:"))
    }

    @Test
    fun testScreenshot7_isMarketPriceQuery_detectsPriceQueries() {
        // Screenshot 7 exact query
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("now what price is gold"))

        // Common price queries
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("what price is gold"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("gold price"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("price of gold"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("current gold price"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("how much is gold"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("bitcoin price"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("btc price"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("price of solana"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("what is the price of GLD"))

        // Burmese price queries
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("ရွှေဈေး"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("ရွှေစျေး"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("ရွှေဈေးဘယ်လောက်လဲ"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("ဒီနေ့ ရွှေပေါက်စျေး"))
        assertTrue(AiFinanceAssistant.isMarketPriceQuery("ဘစ်ကွိုင် စျေး"))

        // Negatives: expense logging, investment advice without price ask, reports, news
        assertFalse(AiFinanceAssistant.isMarketPriceQuery("bought gold for 50000 ks"))
        assertFalse(AiFinanceAssistant.isMarketPriceQuery("should I buy gold"))
        assertFalse(AiFinanceAssistant.isMarketPriceQuery("is gold a good investment"))
        assertFalse(AiFinanceAssistant.isMarketPriceQuery("report about my savings"))
        assertFalse(AiFinanceAssistant.isMarketPriceQuery("today hot news for crypto and make it recap"))
    }

    @Test
    fun testScreenshot7_isMarketPriceResponseIncomplete_detectsRefusalsAndCutoffs() {
        // Screenshot 7 exact refusal response
        val screenshot7Refusal = """
            I don't have access to real-time gold prices or current market data in my system. The latest market information is currently unavailable to me.
            
            From your portfolio context, I can see you hold GLD (a gold ETF/stock equity) at 1.0 units with a cost basis of $401.79 USD per unit, but I cannot tell you what the current market price is today.
        """.trimIndent()
        assertTrue(
            "Screenshot 7 refusal must be detected as incomplete/refusal",
            AiFinanceAssistant.isMarketPriceResponseIncomplete(screenshot7Refusal)
        )

        // Other common refusals
        assertTrue(AiFinanceAssistant.isMarketPriceResponseIncomplete("Current market data in my system is not available to me."))
        assertTrue(AiFinanceAssistant.isMarketPriceResponseIncomplete("I cannot access real-time commodity data right now."))
        assertTrue(AiFinanceAssistant.isMarketPriceResponseIncomplete("ရွှေစျေးနှုန်းများ လက်လှမ်းမမီပါ မသိရှိပါ"))

        // Trailing cutoff
        val cutoff = "Gold is currently trading at $4,476.60 USD / oz, while GLD ETF is,"
        assertTrue(AiFinanceAssistant.isMarketPriceResponseIncomplete(cutoff))

        // Complete valid response
        val valid = """
            🟡 Gold Market Price & Portfolio Overview

            💰 Current Market Quotes:
            • International Spot Gold (XAU / GC): ~$4,476.60 USD / troy oz (-1.39% 24h)
            • SPDR Gold Shares (GLD ETF): ~$406.77 USD / share (-0.84% 24h)
            • Myanmar Domestic Gold (Academy 24K): ~7,000,000 – 7,500,000 MMK / kyat-tha

            💡 Coach's Smart Strategy:
            • 5%–10% Allocation Rule: Keep precious metals to a measured portion of your total wealth.
            • DCA Accumulation: Use Dollar-Cost Averaging rather than trying to time daily price highs and lows.
        """.trimIndent()
        assertFalse(
            "Complete price summary must not be marked incomplete",
            AiFinanceAssistant.isMarketPriceResponseIncomplete(valid)
        )
    }

    @Test
    fun testScreenshot7_buildMarketPriceResponse_synthesizesGoldAndCryptoPricesWithHoldings() {
        // User holds 1.0 unit of GLD @ $401.79 USD (matching Screenshot 7 exactly)
        val sampleHoldings = listOf(
            UserHolding(
                id = "holding_gld",
                symbol = "GLD",
                displayTicker = "GLD",
                name = "SPDR Gold Shares",
                type = "commodity",
                units = 1.0,
                buyPrice = 401.79,
                date = "2026-09-01"
            )
        )

        // 1. English Gold Price Response (matching Screenshot 7 query: "now what price is gold")
        val enGold = AiFinanceAssistant.buildMarketPriceResponse(
            query = "now what price is gold",
            language = "en",
            isGold = true,
            isCrypto = false,
            holdings = sampleHoldings,
            goldSpotPrice = 4476.60,
            goldSpotChange = -1.39,
            gldPrice = 406.77,
            gldChange = -0.84
        )
        assertTrue("Must include Gold Title", enGold.contains("Gold Market Price & Portfolio Overview"))
        assertTrue("Must quote Spot Gold", enGold.contains("Spot Gold (XAU / GC): ~$4,476.60 USD"))
        assertTrue("Must quote GLD ETF", enGold.contains("SPDR Gold Shares (GLD ETF): ~$406.77 USD"))
        assertTrue("Must quote Myanmar domestic gold context", enGold.contains("Myanmar Domestic Gold (Academy 24K)"))
        assertTrue("Must show user's holding position", enGold.contains("Holding: 1.0 unit of GLD (Cost basis: $401.79 USD)"))
        assertTrue("Must calculate current value with gain", enGold.contains("Current Estimated Value: $406.77 USD (+1.24% / +$4.98 USD)"))
        assertTrue("Must advise 5%-10% allocation", enGold.contains("5%–10% Allocation Rule"))
        assertTrue("Must advise emergency fund first", enGold.contains("Emergency Fund First"))
        assertTrue("Must advise DCA", enGold.contains("Dollar-Cost Averaging"))

        // 2. Burmese Gold Price Response
        val myGold = AiFinanceAssistant.buildMarketPriceResponse(
            query = "ရွှေဈေးဘယ်လောက်လဲ",
            language = "my",
            isGold = true,
            isCrypto = false,
            holdings = sampleHoldings,
            goldSpotPrice = 4476.60,
            goldSpotChange = -1.39,
            gldPrice = 406.77,
            gldChange = -0.84
        )
        assertTrue("Must include Burmese Title", myGold.contains("ရွှေစျေးကွက်ပေါက်စျေးနှင့် ပိုင်ဆိုင်မှု သုံးသပ်ချက်"))
        assertTrue("Must include spot gold in Burmese", myGold.contains("နိုင်ငံတကာ ရွှေစျေး (Spot Gold / GC): ~$4,476.60 USD"))
        assertTrue("Must include GLD in Burmese", myGold.contains("SPDR Gold Shares (GLD ETF): ~$406.77 USD"))
        assertTrue("Must include Academy Gold in Burmese", myGold.contains("မြန်မာ့ရွှေစျေး (အကယ်ဒမီ ၂၄ ပဲရည် မီးလင်းရွှေ)"))
        assertTrue("Must calculate Burmese holding value", myGold.contains("GLD 1.0 ယူနစ် (ဝယ်ယူစျေး: $401.79 USD)"))
        assertTrue("Must advise DCA in Burmese", myGold.contains("Dollar-Cost Averaging (DCA)"))
        assertTrue("Must advise 5%-10% in Burmese", myGold.contains("၅% မှ ၁၀% အချိုးအစားသာ ရင်းနှီးမြှုပ်နှံပါ"))

        // 3. Crypto Price Response
        val enCrypto = AiFinanceAssistant.buildMarketPriceResponse(
            query = "bitcoin price",
            language = "en",
            isGold = false,
            isCrypto = true,
            holdings = emptyList(),
            btcPrice = 79980.0,
            btcChange = 0.5
        )
        assertTrue("Must include crypto title", enCrypto.contains("Crypto Market Price & Portfolio Overview"))
        assertTrue("Must include Bitcoin quote", enCrypto.contains("Bitcoin (BTC): ~$79,980.00 USD"))
        assertTrue("Must advise 1%-5% limit", enCrypto.contains("1%–5% Allocation Limit"))
    }

    @Test
    fun testScreenshot8_isInvestmentAdviceQuery_detectsMixedBurmeseEnglishBitcoinQueries() {
        // Exact query from user's Screenshot 8
        assertTrue(
            "Must detect 'ခုချိန် Bitcoin ဝယ်သင့်လား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("ခုချိန် Bitcoin ဝယ်သင့်လား")
        )
        assertTrue(
            "Must detect 'Bitcoin ဝယ်သင့်လား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("Bitcoin ဝယ်သင့်လား")
        )
        assertTrue(
            "Must detect 'BTC ဝယ်သင့်လား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("BTC ဝယ်သင့်လား")
        )
        assertTrue(
            "Must detect 'Crypto ဝယ်သင့်လား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("Crypto ဝယ်သင့်လား")
        )
        assertTrue(
            "Must detect 'Crypto ဝယ်ရင်ကောင်းမလား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("Crypto ဝယ်ရင်ကောင်းမလား")
        )
        assertTrue(
            "Must detect 'Bitcoin ဝယ်လို့ကောင်းလား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("Bitcoin ဝယ်လို့ကောင်းလား")
        )
        assertTrue(
            "Must detect 'Gold ဝယ်သင့်လား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("Gold ဝယ်သင့်လား")
        )
        assertTrue(
            "Must detect 'စတော့ ဝယ်သင့်လား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("စတော့ ဝယ်သင့်လား")
        )
    }

    @Test
    fun testScreenshot8_isMarketPriceQuery_detectsMixedPriceAndConditionQueries() {
        // Query from user's Screenshot 8
        assertTrue(
            "Must detect 'ရွှေဈေး ခြေနေ ကောင်းလား'",
            AiFinanceAssistant.isMarketPriceQuery("ရွှေဈေး ခြေနေ ကောင်းလား")
        )
        assertTrue(
            "Must detect 'Bitcoin ဈေး'",
            AiFinanceAssistant.isMarketPriceQuery("Bitcoin ဈေး")
        )
        assertTrue(
            "Must detect 'Bitcoin ဈေး ဘယ်လောက်လဲ'",
            AiFinanceAssistant.isMarketPriceQuery("Bitcoin ဈေး ဘယ်လောက်လဲ")
        )
        assertTrue(
            "Must detect 'ခုချိန် Bitcoin ဈေး ဘယ်လောက်လဲ'",
            AiFinanceAssistant.isMarketPriceQuery("ခုချိန် Bitcoin ဈေး ဘယ်လောက်လဲ")
        )
        assertTrue(
            "Must detect 'Crypto ဈေး'",
            AiFinanceAssistant.isMarketPriceQuery("Crypto ဈေး")
        )
    }

    @Test
    fun testScreenshot8_buildInvestmentAdviceResponse_synthesizesBitcoinAdviceWithPrice() {
        val myAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "test_user",
            query = "ခုချိန် Bitcoin ဝယ်သင့်လား",
            language = "my",
            btcPrice = 79980.0,
            btcChange = 0.5
        )

        assertTrue("Must include Burmese title", myAdvice.contains("ခရစ်ပတိုနှင့် ဘစ်ကွိုင် (Bitcoin) ရင်းနှီးမြှုပ်နှံမှု အကြံပြုချက်"))
        assertTrue("Must include live Bitcoin price", myAdvice.contains("လက်ရှိ Bitcoin စျေးကွက်ပေါက်စျေး: ~$79,980.00 USD"))
        assertTrue("Must advise 1%-5% limit", myAdvice.contains("၁% မှ ၅%"))
        assertTrue("Must advise emergency fund", myAdvice.contains("အရေးပေါ်သုံးငွေ (Emergency Fund)"))
        assertTrue("Must advise DCA", myAdvice.contains("Dollar-Cost Averaging (DCA)"))
        assertTrue("Must include educational note", myAdvice.contains("တရားဝင်ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်မဟုတ်ပါ"))

        // Verify English version as well
        val enAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "test_user",
            query = "should I buy bitcoin right now",
            language = "en",
            btcPrice = 79980.0,
            btcChange = 0.5
        )
        assertTrue("Must include English title", enAdvice.contains("Cryptocurrency & Bitcoin: Coach's Perspective"))
        assertTrue("Must include live benchmark price in English", enAdvice.contains("Current Bitcoin Benchmark Price: ~$79,980.00 USD"))
        assertTrue("Must advise 1%-5% limit in English", enAdvice.contains("1% – 5% Allocation"))
        assertTrue("Must advise DCA in English", enAdvice.contains("Dollar-Cost Averaging (DCA)"))
    }

    @Test
    fun testScreenshot9_spendingLinkedCryptoAdvice_synthesizesBudgetAndSolQuote() {
        // 1. Query detection for Screenshot 9 Burmese and English variants
        assertTrue(
            "Must detect Screenshot 9 query 'ဒီလအသုံးစရိတ်ကို ကြည့်ပြီး sol ဝယ်သင့် မဝယ်သင့်'",
            AiFinanceAssistant.isInvestmentAdviceQuery("ဒီလအသုံးစရိတ်ကို ကြည့်ပြီး sol ဝယ်သင့် မဝယ်သင့်")
        )
        assertTrue(
            "Must detect 'ဒီလအသုံးစရိတ်ကို ကြည့်ပြီး bitcoin ဝယ်သင့်လား'",
            AiFinanceAssistant.isInvestmentAdviceQuery("ဒီလအသုံးစရိတ်ကို ကြည့်ပြီး bitcoin ဝယ်သင့်လား")
        )
        assertTrue(
            "Must detect 'based on my spending should I buy sol'",
            AiFinanceAssistant.isInvestmentAdviceQuery("based on my spending should I buy sol")
        )
        assertTrue(
            "Must detect 'looking at my expenses should I buy bitcoin'",
            AiFinanceAssistant.isInvestmentAdviceQuery("looking at my expenses should I buy bitcoin")
        )
        assertTrue(
            "Must detect standalone 'sol ဝယ်သင့် မဝယ်သင့်'",
            AiFinanceAssistant.isInvestmentAdviceQuery("sol ဝယ်သင့် မဝယ်သင့်")
        )

        // 2. Burmese advice with budget surplus
        val mySurplusAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "test_user",
            query = "ဒီလအသုံးစရိတ်ကို ကြည့်ပြီး sol ဝယ်သင့် မဝယ်သင့်",
            language = "my",
            totalSpentMmk = 150000.0,
            budgetLimitMmk = 300000.0,
            remainingBudgetMmk = 150000.0,
            daysLeft = 24,
            cryptoSymbol = "SOL",
            cryptoPrice = 185.0,
            cryptoChange = 2.45
        )

        assertTrue("Must include Solana title", mySurplusAdvice.contains("Solana (SOL) ရင်းနှီးမြှုပ်နှံမှုနှင့် ဘဏ္ဍာရေး သုံးသပ်ချက်"))
        assertTrue("Must include budget block header", mySurplusAdvice.contains("📊 ယခုလ သင့်၏ အသုံးစရိတ်နှင့် ဘတ်ဂျက် အခြေအနေ:"))
        assertTrue("Must display spent MMK in Burmese digits", mySurplusAdvice.contains("၁၅၀,၀၀၀ ကျပ်"))
        assertTrue("Must display budget limit MMK in Burmese digits", mySurplusAdvice.contains("၃၀၀,၀၀၀ ကျပ်"))
        assertTrue("Must display live Solana price", mySurplusAdvice.contains("~$185.00 USD"))
        assertTrue("Must include coach verdict block", mySurplusAdvice.contains("💡 Coach ၏ အကြံပြုချက် (ဝယ်သင့် မဝယ်သင့်):"))
        assertTrue("Must advise 1%-5% allocation", mySurplusAdvice.contains("၁% မှ ၅%"))
        assertTrue("Must advise emergency fund", mySurplusAdvice.contains("အရေးပေါ်သုံးငွေ (Emergency Fund)"))
        assertTrue("Must advise DCA", mySurplusAdvice.contains("Dollar-Cost Averaging (DCA)"))
        assertTrue("Must include educational disclaimer", mySurplusAdvice.contains("တရားဝင်ရင်းနှီးမြှုပ်နှံမှု အကြံဉာဏ်မဟုတ်ပါ"))

        // 3. Burmese advice when budget is exhausted / overspent
        val myExhaustedAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "test_user",
            query = "ဒီလအသုံးစရိတ်ကို ကြည့်ပြီး sol ဝယ်သင့် မဝယ်သင့်",
            language = "my",
            totalSpentMmk = 320000.0,
            budgetLimitMmk = 300000.0,
            remainingBudgetMmk = -20000.0,
            daysLeft = 10,
            cryptoSymbol = "SOL",
            cryptoPrice = 185.0
        )
        assertTrue("Must advise NOT to buy when budget exhausted", myExhaustedAdvice.contains("❌ ယခုလတွင် မဝယ်သင့်သေးပါ!"))

        // 4. English advice with budget surplus
        val enSurplusAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "test_user",
            query = "based on my spending should I buy sol",
            language = "en",
            totalSpentMmk = 150000.0,
            budgetLimitMmk = 300000.0,
            remainingBudgetMmk = 150000.0,
            daysLeft = 24,
            cryptoSymbol = "SOL",
            cryptoPrice = 185.0,
            cryptoChange = 2.45
        )

        assertTrue("Must include English Solana title", enSurplusAdvice.contains("Solana (SOL) Investment & Financial Coaching Review"))
        assertTrue("Must include English budget header", enSurplusAdvice.contains("📊 Your Monthly Spending & Budget Status:"))
        assertTrue("Must display spent MMK", enSurplusAdvice.contains("150,000 MMK"))
        assertTrue("Must display budget limit MMK", enSurplusAdvice.contains("300,000 MMK"))
        assertTrue("Must display Solana benchmark price", enSurplusAdvice.contains("~$185.00 USD"))
        assertTrue("Must include English coach recommendation header", enSurplusAdvice.contains("💡 Coach's Recommendation (Should You Buy?):"))
        assertTrue("Must advise 1%-5% limit in English", enSurplusAdvice.contains("1% – 5% Allocation"))
        assertTrue("Must advise DCA in English", enSurplusAdvice.contains("Dollar-Cost Averaging (DCA)"))

        // 5. English advice when budget is exhausted
        val enExhaustedAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "test_user",
            query = "based on my spending should I buy sol",
            language = "en",
            totalSpentMmk = 350000.0,
            budgetLimitMmk = 300000.0,
            remainingBudgetMmk = -50000.0,
            daysLeft = 5,
            cryptoSymbol = "SOL",
            cryptoPrice = 185.0
        )
        assertTrue("Must advise NOT to buy in English when budget exhausted", enExhaustedAdvice.contains("❌ Do NOT buy right now!"))
    }

    @Test
    fun testScreenshot1_multiItemBurmeseExpenseWithBurmeseComma() {
        val input = "လျှပ်စစ်ကြိုးခွေ 47800 ကုန်တယ် ၊ အအေးသောက်တာ 3000"
        val parsedList = AiChatRepository.extractFallbackExpenses(input, "my")
        assertEquals("Must extract exactly 2 expenses", 2, parsedList.size)

        val wire = parsedList[0]
        assertEquals(47800.0, wire.amount, 0.001)
        assertEquals("လျှပ်စစ်ကြိုးခွေ", wire.item)
        assertEquals("Shopping", wire.category)
        assertFalse(wire.isChallenge)

        val drink = parsedList[1]
        assertEquals(3000.0, drink.amount, 0.001)
        assertEquals("အအေး", drink.item)
        assertEquals("Food & Dining", drink.category)
        assertFalse(drink.isChallenge)
    }

    @Test
    fun testScreenshot1_mixedExpenseAndChallengeInquiry() {
        val input = "ထမင်းကြော် 5000 ဖိုး ကုန်တယ် ညစာတွက် မေ့တော့မလို့ Gucci Bag ဝယ်ဖို့စုရဦးမယ်မလား"
        val parsedList = AiChatRepository.extractFallbackExpenses(input, "my")
        assertEquals("Must extract 2 items (1 expense and 1 challenge)", 2, parsedList.size)

        val expense = parsedList[0]
        assertFalse("First item must be an expense", expense.isChallenge)
        assertEquals(5000.0, expense.amount, 0.001)
        assertEquals("ထမင်းကြော်", expense.item)
        assertEquals("Food & Dining", expense.category)

        val challenge = parsedList[1]
        assertTrue("Second item must be a challenge", challenge.isChallenge)
        assertEquals("Gucci Bag", challenge.challengeTitle)
        assertEquals(0.0, challenge.amount, 0.001)
    }

    @Test
    fun testCategoryResolver_wireAndColdDrinkAliases() {
        val resolvedWire = com.savingcoach.app.ui.chat.CategoryResolver.resolve("လျှပ်စစ်ကြိုးခွေ", emptyList())
        assertNotNull(resolvedWire)
        assertEquals("Shopping", resolvedWire!!.name)

        val resolvedWireShort = com.savingcoach.app.ui.chat.CategoryResolver.resolve("လျှပ်စစ်ကြိုး", emptyList())
        assertNotNull(resolvedWireShort)
        assertEquals("Shopping", resolvedWireShort!!.name)

        val resolvedCoil = com.savingcoach.app.ui.chat.CategoryResolver.resolve("ကြိုးခွေ", emptyList())
        assertNotNull(resolvedCoil)
        assertEquals("Shopping", resolvedCoil!!.name)

        val resolvedDrink = com.savingcoach.app.ui.chat.CategoryResolver.resolve("အအေး", emptyList())
        assertNotNull(resolvedDrink)
        assertEquals("Food & Dining", resolvedDrink!!.name)

        val resolvedJuice = com.savingcoach.app.ui.chat.CategoryResolver.resolve("ဖျော်ရည်", emptyList())
        assertNotNull(resolvedJuice)
        assertEquals("Food & Dining", resolvedJuice!!.name)

        val resolvedElectric = com.savingcoach.app.ui.chat.CategoryResolver.resolve("လျှပ်စစ်", emptyList())
        assertNotNull(resolvedElectric)
        assertEquals("Bills & Utilities", resolvedElectric!!.name)
    }

    @Test
    fun testMultiChallenge_saveGucciBagAndCamera_extractsBothChallenges() {
        val input = "save Gucci Bag and Camera"
        val parsedList = AiChatRepository.extractFallbackExpenses(input, "en")
        assertEquals("Must extract 2 challenges", 2, parsedList.size)

        val chal1 = parsedList[0]
        assertTrue(chal1.isChallenge)
        assertEquals("Gucci Bag", chal1.challengeTitle)

        val chal2 = parsedList[1]
        assertTrue(chal2.isChallenge)
        assertEquals("Camera", chal2.challengeTitle)
    }

    @Test
    fun testBurmeseChallengeVerbsPattern_matchesNaturalVariations() {
        val regex = Regex(AiChatRepository.BURMESE_CHALLENGE_VERBS_PATTERN)
        assertTrue(regex.containsMatchIn("စုရဦးမယ်မလား"))
        assertTrue(regex.containsMatchIn("စုရဦးမယ်မလား"))
        assertTrue(regex.containsMatchIn("စုရမလား"))
        assertTrue(regex.containsMatchIn("စုဦးမယ်"))
        assertTrue(regex.containsMatchIn("စုဦးမယ်"))
        assertTrue(regex.containsMatchIn("စုမလို့"))
        assertTrue(regex.containsMatchIn("စုဖို့"))
        assertTrue(regex.containsMatchIn("စုတော့မယ်"))
        assertTrue(regex.containsMatchIn("ထည့်ရမလား"))
        assertTrue(regex.containsMatchIn("ထည့်ဦးမယ်"))
    }

    @Test
    fun testLookBehindRegexCrash_extractFallbackExpenses_doesNotThrowPatternSyntaxException() {
        // Must never throw PatternSyntaxException on English or Burmese inputs
        val enList = AiChatRepository.extractFallbackExpenses("how my savings condition", "en")
        assertTrue(enList.isEmpty())

        val input = "ထမင်းကြော် 5000 ဖိုး ကုန်တယ် ညစာတွက် မေ့တော့မလို့ Gucci Bag ဝယ်ဖို့စုရဦးမယ်မလား"
        val parsedList = AiChatRepository.extractFallbackExpenses(input, "my")
        assertEquals("Must extract 2 items (1 expense and 1 challenge)", 2, parsedList.size)

        val expense = parsedList[0]
        assertFalse(expense.isChallenge)
        assertEquals(5000.0, expense.amount, 0.001)
        assertEquals("ထမင်းကြော်", expense.item)

        val challenge = parsedList[1]
        assertTrue(challenge.isChallenge)
        assertEquals("Gucci Bag", challenge.challengeTitle)
    }

    @Test
    fun testSavingsConditionQuery_isSavingsReportQuery() {
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how my savings condition"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how is my saving"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how are my savings"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how's my savings"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("savings condition"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("savings progress"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("ငွေစုအခြေအနေ"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("ငွေစုတဲ့ အခြေအနေ"))
    }

    @Test
    fun testScreenshot1_TeslaStockAdvice_detectsQueryAndReturnsTeslaAdvice() {
        val q1 = "Tesla stock ဝယ်ရင်ကောင်းမလား"
        val q2 = "Tesla ဝယ်ရင်ကောင်းမလား"
        val q3 = "should I buy tesla stock"

        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery(q1))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery(q2))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery(q3))

        val burmeseAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "user_test",
            query = q1,
            language = "my",
            remainingBudgetMmk = 100000.0,
            budgetLimitMmk = 500000.0,
            totalSpentMmk = 400000.0,
            daysLeft = 10
        )
        assertTrue("Must contain Tesla", burmeseAdvice.contains("Tesla"))
        assertTrue("Must contain TSLA", burmeseAdvice.contains("TSLA"))
        assertTrue("Must mention EV", burmeseAdvice.contains("EV"))
        assertTrue("Must mention S&P 500", burmeseAdvice.contains("S&P 500"))
        assertTrue("Must mention DCA", burmeseAdvice.contains("DCA"))
        assertTrue("Must mention Emergency Fund", burmeseAdvice.contains("Emergency Fund") || burmeseAdvice.contains("အရေးပေါ်သုံးငွေ"))
        assertFalse("Must not be generic greeting", burmeseAdvice.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ"))

        // Must not be flagged as corrupted
        assertFalse("Burmese Tesla advice must not be flagged corrupted", AiChatRepository.isCorruptedBurmeseResponse(burmeseAdvice, q1))

        // English advice test
        val englishAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "user_test",
            query = q3,
            language = "en"
        )
        assertTrue("Must contain Tesla", englishAdvice.contains("Tesla"))
        assertTrue("Must contain S&P 500", englishAdvice.contains("S&P 500"))
        assertTrue("Must contain DCA", englishAdvice.contains("DCA"))
    }

    @Test
    fun testScreenshot2_BtcCryptoAdvice_detectsQueryAndReturnsBtcAdvice() {
        val q = "btc ဝယ်သင့်လား"
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery(q))

        val advice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "user_test",
            query = q,
            language = "my",
            remainingBudgetMmk = 50000.0,
            btcPrice = 85000.0
        )
        assertTrue("Must mention Bitcoin or BTC", advice.contains("Bitcoin") || advice.contains("BTC") || advice.contains("ဘစ်ကွိုင်"))
        assertTrue("Must mention allocation guardrail", advice.contains("1%") || advice.contains("5%"))
        assertTrue("Must mention DCA", advice.contains("DCA"))
        assertFalse("Must not be generic greeting", advice.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ"))
        assertFalse("Burmese BTC advice must not be corrupted", AiChatRepository.isCorruptedBurmeseResponse(advice, q))
    }

    @Test
    fun testScreenshot3_BtcAndSolComparison_detectsQueryAndReturnsComparativeAdvice() {
        val q1 = "btc နဲ့ sol ဘာဝယ်သင့်"
        val q2 = "btc vs sol which should i buy"
        val q3 = "sol နဲ့ btc ဘယ်ဟာပိုကောင်း"

        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery(q1))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery(q2))
        assertTrue(AiFinanceAssistant.isInvestmentAdviceQuery(q3))

        val burmeseAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "user_test",
            query = q1,
            language = "my",
            remainingBudgetMmk = 80000.0,
            btcPrice = 84000.0,
            cryptoPrice = 175.0
        )
        assertTrue("Must mention Bitcoin (BTC)", burmeseAdvice.contains("Bitcoin") || burmeseAdvice.contains("BTC"))
        assertTrue("Must mention Solana (SOL)", burmeseAdvice.contains("Solana") || burmeseAdvice.contains("SOL"))
        assertTrue("Must mention Digital Gold", burmeseAdvice.contains("Digital Gold") || burmeseAdvice.contains("ဒစ်ဂျစ်တယ်ရွှေ"))
        assertTrue("Must mention High TPS or layer", burmeseAdvice.contains("TPS") || burmeseAdvice.contains("Layer-1"))
        assertTrue("Must mention Core 70%-80% allocation", burmeseAdvice.contains("70%-80%"))
        assertTrue("Must mention Tactical 20%-30% allocation", burmeseAdvice.contains("20%-30%"))
        assertFalse("Must not be generic greeting", burmeseAdvice.contains("နားလည်ပါပြီ။ ဘာများ ကူညီပေးရမလဲ"))
        assertFalse("Burmese comparison advice must not be corrupted", AiChatRepository.isCorruptedBurmeseResponse(burmeseAdvice, q1))

        val englishAdvice = AiFinanceAssistant.buildInvestmentAdviceResponse(
            userId = "user_test",
            query = q2,
            language = "en",
            btcPrice = 84000.0,
            solPrice = 175.0
        )
        assertTrue("Must mention Bitcoin", englishAdvice.contains("Bitcoin"))
        assertTrue("Must mention Solana", englishAdvice.contains("Solana"))
        assertTrue("Must mention Digital Gold", englishAdvice.contains("Digital Gold"))
        assertTrue("Must mention 70%-80%", englishAdvice.contains("70%-80%"))
        assertTrue("Must mention 20%-30%", englishAdvice.contains("20%-30%"))
    }

    @Test
    fun testFinancialAllowedTokens_notCorrupted() {
        val query = "Tesla stock ဝယ်ရင်ကောင်းမလား"
        val sampleReply = """
            Tesla (TSLA) စတော့ရှယ်ယာ ဝယ်ယူရန် စဉ်းစားနေပါက အောက်ပါအချက်များကို သတိပြုသင့်ပါတယ်-
            ၁။ Tesla သည် လျှပ်စစ်ကား (EV) နှင့် နည်းပညာကဏ္ဍတွင် ဦးဆောင်နေသော ကုမ္ပဏီဖြစ်သော်လည်း စတော့ဈေးနှုန်း အတက်အကျ (volatility) အလွန်ကြမ်းတမ်းပါတယ်။
            ၂။ ကုမ္ပဏီတစ်ခုတည်း၏ single stock ကို ဝယ်ယူမည့်အစား S&P 500 သို့မဟုတ် broad market ETF များတွင် diversified လုပ်ပြီး ရင်းနှီးမြှုပ်နှံခြင်းက ပိုမိုစိတ်ချရပါတယ်။
            ၃။ သင့်လစဉ်ဘတ်ဂျက်နှင့် အရေးပေါ်သုံးငွေ (emergency fund) ကို အရင်ဖယ်ထားပြီးမှ ပိုလျှံငွေဖြင့် DCA နည်းလမ်းဖြင့်သာ စတင်သင့်ပါတယ်။
        """.trimIndent()

        assertFalse(
            "Financial reply with Tesla, TSLA, EV, S&P 500, ETF, DCA must not be marked corrupted",
            AiChatRepository.isCorruptedBurmeseResponse(sampleReply, query)
        )
    }

    @Test
    fun testScreenshotMedia1788712296010_howSavingIsGoing_isSavingsReportQuery() {
        // Exact query from screenshot media_1788712296010.png
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how saving is going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how savings is going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how is saving going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how is savings going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how are savings going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how are my savings going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how's saving going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how's savings going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how saving going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how savings going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how saving is doing"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how savings is doing"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("how's my challenge going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("saving is going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("savings is going"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("ငွေစုတာ ဘယ်လိုလဲ"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("ငွေစုတာ ဘယ်လိုရှိလဲ"))
        assertTrue(AiFinanceAssistant.isSavingsReportQuery("ငွေစုတာ ဘယ်လိုသွားနေလဲ"))

        // Saving advice queries should NOT match savings report
        assertFalse(AiFinanceAssistant.isSavingsReportQuery("how to save money"))
        assertFalse(AiFinanceAssistant.isSavingsReportQuery("how can i save"))
        assertFalse(AiFinanceAssistant.isSavingsReportQuery("ဘယ်လိုငွေစုရမလဲ"))
        assertFalse(AiFinanceAssistant.isSavingsReportQuery("Bluetooth speaker လိုချင်တာ ၅၀၀၀၀ တဲ့ ဘယ်လိုစုရင်ကောင်းမလဲ"))
    }

    @Test
    fun testScreenshotMedia1788712436418_100kSavingPlan_parses100kAs100000AndNotItemK() {
        val query = "how to save 100k in a week"

        // 1. Verify parseSavingGoal parses 100k as 100,000 MMK and does not treat 'k' as the item name
        val goal = AiFinanceAssistant.parseSavingGoal(query)
        assertEquals(100000.0, goal.amount, 0.001)
        assertEquals(7, goal.targetDays)
        assertEquals("1 Week", goal.timeframeText)
        assertEquals("", goal.item) // Must NOT be "k"!

        // 2. Verify buildSavingAdviceResponse generates accurate 100,000 MMK breakdown
        val response = AiFinanceAssistant.buildSavingAdviceResponse("user_test", query, "en")
        assertTrue("Must contain 100,000 MMK", response.contains("100,000 MMK"))
        assertTrue("Must contain daily target ~14,286 MMK", response.contains("14,286 MMK"))
        assertFalse("Must NOT contain 'Saving Plan for k'", response.contains("Saving Plan for k"))
        assertFalse("Must NOT contain '100 MMK'", response.contains("100 MMK"))
        assertFalse("Must NOT contain 'Save 14 MMK'", response.contains("Save 14 MMK"))
        assertFalse("Must NOT contain 'Create a new 'k' challenge'", response.contains("'k' challenge"))

        // 3. Burmese variation
        val myQuery = "၁ ပတ် အတွင်း 100k ဘယ်လိုစုရမလဲ"
        val myGoal = AiFinanceAssistant.parseSavingGoal(myQuery)
        assertEquals(100000.0, myGoal.amount, 0.001)
        assertEquals(7, myGoal.targetDays)
        assertEquals("", myGoal.item)

        // 4. Other amount variations with k
        val goal50k = AiFinanceAssistant.parseSavingGoal("how to save 50k in 10 days")
        assertEquals(50000.0, goal50k.amount, 0.001)
        assertEquals(10, goal50k.targetDays)
        assertEquals("", goal50k.item)

        val goalPhone = AiFinanceAssistant.parseSavingGoal("how to save 100k for phone in 1 month")
        assertEquals(100000.0, goalPhone.amount, 0.001)
        assertEquals(30, goalPhone.targetDays)
        assertEquals("phone", goalPhone.item)
    }
}

