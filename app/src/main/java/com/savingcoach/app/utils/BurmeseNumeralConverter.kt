package com.savingcoach.app.utils

object BurmeseNumeralConverter {

    /**
     * Converts standard Arabic digits (0-9) into Myanmar / Burmese digits (၀-၉).
     */
    fun toBurmeseDigits(text: String): String {
        if (text.isEmpty()) return text
        val arabicToMyanmar = mapOf(
            '0' to '၀', '1' to '၁', '2' to '၂', '3' to '၃', '4' to '၄',
            '5' to '၅', '6' to '၆', '7' to '၇', '8' to '၈', '9' to '၉'
        )
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(arabicToMyanmar[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Translates Myanmar/Burmese digits (၀-၉) and common spoken financial components
     * like "သောင်း" (ten-thousand), "ထောင်" (thousand), "ရာ" (hundred), and "ထောင့်"
     * into standard Arabic numerals.
     */
    fun convert(text: String): String {
        if (text.isBlank()) return text
        
        var result = text
        
        // 1. Replace Burmese Unicode digits with Arabic digits
        val myanmarDigits = mapOf(
            '၀' to '0', '၁' to '1', '၂' to '2', '၃' to '3', '၄' to '4',
            '၅' to '5', '၆' to '6', '၇' to '7', '၈' to '8', '၉' to '9'
        )
        myanmarDigits.forEach { (my, ar) ->
            result = result.replace(my, ar)
        }
        
        // 2. Normalize common spoken phonetic variations
        // "ထောင့်" is commonly spoken instead of "ထောင်" (thousand) when followed by hundreds (e.g. 2ထောင့်5ရာ or ထောင့်ငါးရာ)
        result = result.replace("ထောင့်", "ထောင်")
        
        // Handle "ထောင်" at beginning of number phrase like "ထောင်ငါးရာ" -> "1ထောင်5ရာ"
        val startThousandPattern = "(^|\\s)ထောင်(?=\\s*(?:\\d|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ခုနစ်|ခုနှစ်|ရှစ်|ကိုး|ရာ))".toRegex()
        result = startThousandPattern.replace(result) { match ->
            "${match.groupValues[1]}1ထောင်"
        }
        val startTenThousandPattern = "(^|\\s)သောင်း(?=\\s*(?:\\d|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ခုနစ်|ခုနှစ်|ရှစ်|ကိုး|ထောင်))".toRegex()
        result = startTenThousandPattern.replace(result) { match ->
            "${match.groupValues[1]}1သောင်း"
        }

        // 3. Map word-based Burmese numerals to digits ONLY when immediately followed by numerical multipliers (သိန်း, သောင်း, ထောင်, ရာ, ဆယ်)
        val wordToDigitMap = mapOf(
            "ခုနှစ်" to "7", "ခုနစ်" to "7",
            "တစ်" to "1", "နှစ်" to "2", "သုံး" to "3", "လေး" to "4", "ငါး" to "5",
            "ခြောက်" to "6", "ရှစ်" to "8", "ကိုး" to "9"
        )
        
        val wordMultiplierPattern = "(^|[^\\u1000-\\u109F])(ခုနှစ်|ခုနစ်|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ရှစ်|ကိုး)(?=\\s*(?:သိန်း|သောင်း|ထောင်|ရာ|ဆယ်))".toRegex()
        result = wordMultiplierPattern.replace(result) { match ->
            val prefix = match.groupValues[1]
            val word = match.groupValues[2]
            val digit = wordToDigitMap[word] ?: word
            "$prefix$digit"
        }

        // Replace word numerals when preceded by a number and followed by "ရာ" or "ထောင်" (e.g., "1သောင်း ငါးထောင်" -> "1သောင်း 5ထောင်")
        val tenKPlusKWordPattern = "(\\d+\\s*သောင်း\\s*)(ခုနှစ်|ခုနစ်|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ရှစ်|ကိုး)(\\s*ထောင်)".toRegex()
        result = tenKPlusKWordPattern.replace(result) { match ->
            val prefix = match.groupValues[1]
            val word = match.groupValues[2]
            val suffix = match.groupValues[3]
            val digit = wordToDigitMap[word] ?: word
            "$prefix$digit$suffix"
        }

        val kPlusHWordPattern = "(\\d+\\s*ထောင်\\s*)(ခုနှစ်|ခုနစ်|တစ်|နှစ်|သုံး|လေး|ငါး|ခြောက်|ရှစ်|ကိုး)(\\s*ရာ)".toRegex()
        result = kPlusHWordPattern.replace(result) { match ->
            val prefix = match.groupValues[1]
            val word = match.groupValues[2]
            val suffix = match.groupValues[3]
            val digit = wordToDigitMap[word] ?: word
            "$prefix$digit$suffix"
        }

        // 4. Handle compound values like "2ထောင်5ရာ" (2500) or "1သောင်း5ထောင်" (15000) using regular expressions
        // Pattern: [digits]သောင်း[digits]ထောင် -> e.g. "1သောင်း5ထောင်" -> 15000
        val compoundPatternTenKAndK = "(\\d+)\\s*သောင်း\\s*(\\d+)\\s*ထောင်".toRegex()
        result = compoundPatternTenKAndK.replace(result) { match ->
            val tenK = match.groupValues[1].toIntOrNull() ?: 0
            val k = match.groupValues[2].toIntOrNull() ?: 0
            (tenK * 10000 + k * 1000).toString()
        }

        // Pattern: [digits]ထောင်[digits]ရာ -> e.g. "2ထောင်5ရာ" -> 2500
        val compoundPatternKAndH = "(\\d+)\\s*ထောင်\\s*(\\d+)\\s*ရာ".toRegex()
        result = compoundPatternKAndH.replace(result) { match ->
            val k = match.groupValues[1].toIntOrNull() ?: 0
            val h = match.groupValues[2].toIntOrNull() ?: 0
            (k * 1000 + h * 100).toString()
        }

        // 5. Handle single multipliers
        // e.g. "1သိန်း" -> 100000, "2.5သိန်း" -> 250000
        val lakhPattern = "(\\d+(\\.\\d+)?)\\s*သိန်း".toRegex()
        result = lakhPattern.replace(result) { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null) (num * 100000).toInt().toString() else match.value
        }

        // e.g. "1.5သောင်း" -> 15000 or "2သောင်း" -> 20000
        val tenThousandPattern = "(\\d+(\\.\\d+)?)\\s*သောင်း".toRegex()
        result = tenThousandPattern.replace(result) { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null) (num * 10000).toInt().toString() else match.value
        }

        // e.g. "5ထောင်" -> 5000
        val thousandPattern = "(\\d+(\\.\\d+)?)\\s*ထောင်".toRegex()
        result = thousandPattern.replace(result) { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null) (num * 1000).toInt().toString() else match.value
        }

        // e.g. "3ရာ" -> 300
        val hundredPattern = "(\\d+(\\.\\d+)?)\\s*ရာ".toRegex()
        result = hundredPattern.replace(result) { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null) (num * 100).toInt().toString() else match.value
        }
        
        return result
    }
}
