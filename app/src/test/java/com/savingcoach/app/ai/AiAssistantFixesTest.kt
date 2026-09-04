package com.savingcoach.app.ai

import com.savingcoach.app.data.model.ParsedExpense
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AiAssistantFixesTest {

    // Mirror of the private cleanThinking method in AiChatRepository
    private fun cleanThinking(text: String): String {
        if (text.isBlank()) return ""

        val cleaned = text.replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("(?s)\\[think\\].*?\\[/think\\]"), "")
            .replace(Regex("(?s)```thought.*?```"), "")
            .trim()

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

        val thinkingPatterns = listOf(
            Regex("(?i)If there are \\d+ days left"),
            Regex("(?i)because \\d+-\\d+=\\d+"),
            Regex("(?i)days have passed,? so today is"),
            Regex("(?i)today is (?:January|February|March|April|May|June|July|August|September|October|November|December) \\d+"),
            Regex("(?i)The user (?:is |said |wants |asked |is asking |mentioned |wrote |typed |logging |just said )"),
            Regex("(?i)The user is logging an expense"),
            Regex("(?i)This is (?:an?|another) (?:expense|challenge|income|transaction|saving) (?:logging )?request"),
            Regex("(?i)(?:Analyze|Analyzing) (?:User|the) Input"),
            Regex("(?i)(?:User|The user) (?:says|said|wants|asked|is asking|mentioned|wrote|typed|logging|just said)"),
            Regex("(?i)(?:This is an?|Another) (?:instruction|request|expense|challenge)"),
            Regex("(?i)The format expected is"),
            Regex("(?i)Identify Required Fields"),
            Regex("(?i)Determine Response Language"),
            Regex("(?i)Formulate Extraction"),
            Regex("(?i)Possible response:"),
            Regex("(?i)The amount is \\d+"),
            Regex("(?i)category would be"),
            Regex("(?i)merchant is (?:not )?specified"),
            Regex("(?i)date is today's date from context"),
            Regex("(?i)date from context:?"),
            Regex("(?i)from context: \\d{4}-\\d{2}-\\d{2}"),
            Regex("(?i)So this is .+ for \\d+ MMK total"),
            Regex("(?i)\\(since it's .+\\)"),
            Regex("(?i)(?:Actually,? wait|Actually,? I |Actually,? looking|Actually,? the|Wait,? but|Let me |I need to |I should |I'll |I will )"),
            Regex("(?i)(?:Let me format|Let me parse|Let me analyze|Let me check|Let me think|Let me work|Let me go)"),
            Regex("(?i)(?:I need to (?:output|extract|follow|determine|process|handle|write|check|format))"),
            Regex("(?i)(?:Wait,? but the rules|The rules (?:also )?say|According to (?:the )?rules)"),
            Regex("(?i)The rules say:?"),
            Regex("(?i)Wait,? let me re-read"),
            Regex("(?i)Wait,? let me (?:check|think|look|analyze)"),
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
            Regex("(?i)(?:If amount is not specified|If the user)"),
            Regex("(?i)(?:Challenge Title:|challengeTitle:)"),
            Regex("(?i)(?:Non-existent|non existent|does not exist)"),
            Regex("(?i)(?:match from active challenges|match the challenge)"),
            Regex("(?i)(?:Looking at the|According to (?:the )?(?:hidden|context|rules|EXPENSE|CHALLENGE))"),
            Regex("(?i)(?:Based on the (?:hidden|context|rules))"),
            Regex("(?i)(?:Following the (?:EXPENSE|CHALLENGE) rules)"),
            Regex("(?i)(?:Here'?s a thinking process|Here'?s (?:what|how))"),
            Regex("(?i)(?:So (?:date|amount|category))"),
            Regex("(?i)(?:Let'?s (?:parse|analyze|think|check))"),
            Regex("(?i)(?:The structure should be|The structure is)"),
            Regex("(?i)(?:For this request|For this user)"),
            Regex("(?i)(?:amount:.*category:|category:.*merchant:)"),
            Regex("(?i)(?:Acknowled(?:ge|ing) the)"),
            Regex("(?i)(?:Mention the|Keep (?:it|the|a|short))"),
            Regex("(?i)(?:You (?:should|would|need to) (?:acknowledge|mention|include|output))"),
            Regex("(?i)(?:The user (?:said|wants|is asking|mentioned|wrote))"),
            Regex("(?i)(?:I should (?:acknowledge|mention|include|output|write))"),
            Regex("(?i)(?:Step \\d|Phase \\d|First,|Second,|Third,)"),
            Regex("(?i)(?:JSON structure|JSON block|JSON data)"),
            Regex("(?i)(?:•\\s*(?:amount|category|merchant|date|currency|acknowledge|mention|keep):?)"),
            Regex("(?i)(?:\\d+\\.\\s*(?:amount|category|merchant|date))"),
            Regex("(?i)(?:Actually,? looking (?:more |at the |closely))"),
            Regex("(?i)(?:Actually,? I think)"),
            Regex("(?i)(?:Actually,? this (?:could|might|seems))"),
            Regex("(?i)(?:Active Challenges \\(\\d+\\)):"),
            Regex("(?i)(?:•\\s*\\w+:.*MMK.*complete)")
        )

        val isThinking = thinkingPatterns.any { it.containsMatchIn(cleaned) }

        if (isThinking) {
            val paragraphs = cleaned.split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotBlank() }
            val userFacing = mutableListOf<String>()

            for (p in paragraphs) {
                val lines = p.lines().map { it.trim() }.filter { it.isNotBlank() }
                val isAllBullets = lines.isNotEmpty() && lines.all {
                    it.startsWith("•") || it.startsWith("*") || it.startsWith("-")
                }

                val isParagraphThinking = isAllBullets ||
                        thinkingPatterns.any { it.containsMatchIn(p) } ||
                        p.startsWith("•") || p.startsWith("*") || p.startsWith("-") ||
                        Regex("(?i)^(?:amount|category|merchant|date|currency):").containsMatchIn(p) ||
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
                        p.contains("So if I add", ignoreCase = true) ||
                        p.contains("So my response should be", ignoreCase = true) ||
                        p.contains("careful not to overstep", ignoreCase = true) ||
                        p.contains("Do NOT automatically save", ignoreCase = true) ||
                        p.contains("NEVER mix", ignoreCase = true) ||
                        p.startsWith("Something like", ignoreCase = true) ||
                        p.startsWith("Wait,", ignoreCase = true) ||
                        p.startsWith("Also,", ignoreCase = true) ||
                        Regex("(?i)Analyze User Input|Identify Required Fields|Determine Response Language|Formulate Extraction|Possible response").containsMatchIn(p) ||
                        Regex("(?i)The format expected is|This is an instruction").containsMatchIn(p) ||
                        Regex("(?i)^(?:1|2|3|4|5)\\.\\s*\\*\\*").containsMatchIn(p) ||
                        Regex("(?i)The amount is \\d+|category would be|merchant is (?:not )?specified|date is today's date|from context:").containsMatchIn(p) ||
                        Regex("(?i)So this is .+ for \\d+ MMK total").containsMatchIn(p) ||
                        p.contains("days left in", ignoreCase = true) ||
                        p.contains("days have passed", ignoreCase = true) ||
                        p.startsWith("let's", ignoreCase = true) ||
                        p.startsWith("so date", ignoreCase = true) ||
                        p.startsWith("I need to", ignoreCase = true) ||
                        p.startsWith("First", ignoreCase = true) ||
                        p.startsWith("Second", ignoreCase = true) ||
                        p.startsWith("Third", ignoreCase = true) ||
                        p.contains("hidden context", ignoreCase = true) ||
                        p.contains("JSON block", ignoreCase = true) ||
                        p.contains("json structure", ignoreCase = true) ||
                        p.contains("prompt_challenge_confirmation", ignoreCase = true) ||
                        p.contains("mark_challenge_saving", ignoreCase = true) ||
                        p.contains("non-existent", ignoreCase = true) ||
                        p.contains("Challenge Title:", ignoreCase = true) ||
                        p.contains("challengeTitle:", ignoreCase = true) ||
                        p.contains("match from active", ignoreCase = true) ||
                        p.contains("does not exist", ignoreCase = true) ||
                        Regex("(?i)^Challenge title:").containsMatchIn(p) ||
                        p.contains("The structure should be", ignoreCase = true) ||
                        p.contains("For this request", ignoreCase = true) ||
                        p.contains("Acknowledging the", ignoreCase = true) ||
                        p.contains("Mention the", ignoreCase = true) ||
                        Regex("(?i)Step \\d|Phase \\d").containsMatchIn(p) ||
                        p.contains("JSON structure", ignoreCase = true) ||
                        p.contains("JSON data", ignoreCase = true) ||
                        Regex("(?i)Active Challenges \\(\\d+\\)").containsMatchIn(p) ||
                        Regex("(?i)•\\s*\\w+:.*MMK.*complete").containsMatchIn(p) ||
                        Regex("(?i)Actually,? looking (?:more |at the |closely)").containsMatchIn(p) ||
                        Regex("(?i)Actually,? I think").containsMatchIn(p) ||
                        Regex("(?i)Actually,? this (?:could|might|seems)").containsMatchIn(p)

                if (!isParagraphThinking) {
                    userFacing.add(p)
                }
            }

            val filteredFacing = userFacing.filter { it.length > 10 || it.any { c -> c.isDigit() } }
            return filteredFacing.joinToString("\n\n").trim()
        }

        return cleaned
    }

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
            lower.contains("ခေါက်ဆွဲ") || lower.contains("ဟင်း") -> "Food"
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
        assertEquals("Food", myExpenseKone.category)
        assertTrue("Merchant should contain food items", myExpenseKone.merchant.contains("ညစာ"))
    }
}
