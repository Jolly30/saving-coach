package com.savingcoach.app.ui.chat

import com.savingcoach.app.data.model.ExpenseCategoryEntity
import com.savingcoach.app.ui.expenses.ExpenseCategory

object CategoryResolver {

    val DEFAULT_ENTITIES: List<ExpenseCategoryEntity> = ExpenseCategory.DEFAULT_CATEGORIES.map {
        ExpenseCategoryEntity(
            emoji = it.emoji,
            name = it.name,
            target = it.target,
            isCustom = it.isCustom
        )
    }

    private val ALIAS_MAP = mapOf(
        "food" to "Food & Dining",
        "dining" to "Food & Dining",
        "food dining" to "Food & Dining",
        "food and dining" to "Food & Dining",
        "meal" to "Food & Dining",
        "meals" to "Food & Dining",
        "drink" to "Food & Dining",
        "drinks" to "Food & Dining",
        "beverage" to "Food & Dining",
        "restaurant" to "Food & Dining",
        "cafe" to "Food & Dining",
        "breakfast" to "Food & Dining",
        "lunch" to "Food & Dining",
        "dinner" to "Food & Dining",
        "coffee" to "Food & Dining",
        "tea" to "Food & Dining",
        "အစားအသောက်" to "Food & Dining",
        "ထမင်း" to "Food & Dining",
        "မုန့်" to "Food & Dining",
        "fruit" to "Food & Dining",
        "fruits" to "Food & Dining",
        "vegetable" to "Food & Dining",
        "vegetables" to "Food & Dining",
        "produce" to "Food & Dining",
        "သံပရာသီး" to "Food & Dining",
        "ဟင်းနုနွယ်" to "Food & Dining",
        "ဟင်းသီးဟင်းရွက်" to "Food & Dining",
        "အသီးအရွက်" to "Food & Dining",
        "အသီးအနှံ" to "Food & Dining",
        "အသီး" to "Food & Dining",
        "သီးနှံ" to "Food & Dining",
        "ဟင်းရွက်" to "Food & Dining",
        "ကန်စွန်းရွက်" to "Food & Dining",
        "ကန်စွန်း" to "Food & Dining",
        "စလုံတီး" to "Food & Dining",
        "နံပြား" to "Food & Dining",
        "မနက်စာ" to "Food & Dining",
        "နေ့လယ်စာ" to "Food & Dining",
        "ညစာ" to "Food & Dining",
        "လက်ဖက်ရည်" to "Food & Dining",
        "ကော်ဖီ" to "Food & Dining",
        "ပလာတာ" to "Food & Dining",
        "အီကြာကွေး" to "Food & Dining",
        "ပေါင်မုန့်" to "Food & Dining",
        "မုန့်ဟင်းခါး" to "Food & Dining",
        "အုန်းနို့ခေါက်ဆွဲ" to "Food & Dining",
        "ရှမ်းခေါက်ဆွဲ" to "Food & Dining",
        "ဒူးရင်းသီး" to "Food & Dining",
        "ဒူးရင်း" to "Food & Dining",
        "သရက်သီး" to "Food & Dining",
        "ထမင်းကြော်" to "Food & Dining",
        "အအေး" to "Food & Dining",
        "ဖျော်ရည်" to "Food & Dining",

        "transport" to "Transportation",
        "transportation" to "Transportation",
        "transit" to "Transportation",
        "bus" to "Transportation",
        "car" to "Transportation",
        "taxi" to "Transportation",
        "ybs" to "Transportation",
        "grab" to "Transportation",
        "travel" to "Transportation",
        "gas" to "Transportation",
        "fuel" to "Transportation",
        "ခရီးစရိတ်" to "Transportation",
        "ကားခ" to "Transportation",
        "ယာဉ်" to "Transportation",
        "bike" to "Transportation",
        "bicycle" to "Transportation",
        "cycle" to "Transportation",
        "electric bike" to "Transportation",
        "စက်ဘီး" to "Transportation",

        "shopping" to "Shopping",
        "shop" to "Shopping",
        "clothes" to "Shopping",
        "clothing" to "Shopping",
        "groceries" to "Shopping",
        "grocery" to "Shopping",
        "market" to "Shopping",
        "store" to "Shopping",
        "ဈေးဝယ်" to "Shopping",
        "ဝယ်ယူမှု" to "Shopping",
        "အဝတ်" to "Shopping",
        "ဓာတ်မီး" to "Shopping",
        "မီးသီး" to "Shopping",
        "flashlight" to "Shopping",
        "torch" to "Shopping",
        "လျှပ်စစ်ကြိုးခွေ" to "Shopping",
        "လျှပ်စစ်ကြိုး" to "Shopping",
        "ကြိုးခွေ" to "Shopping",
        "ဝါယာကြိုး" to "Shopping",

        "bills" to "Bills & Utilities",
        "utilities" to "Bills & Utilities",
        "bill" to "Bills & Utilities",
        "utility" to "Bills & Utilities",
        "bills utilities" to "Bills & Utilities",
        "bills and utilities" to "Bills & Utilities",
        "electricity" to "Bills & Utilities",
        "water" to "Bills & Utilities",
        "internet" to "Bills & Utilities",
        "wifi" to "Bills & Utilities",
        "phone" to "Bills & Utilities",
        "mobile" to "Bills & Utilities",
        "ဘေလ်" to "Bills & Utilities",
        "ဘေလ်များ" to "Bills & Utilities",
        "ဖုန်းဘေ" to "Bills & Utilities",
        "မီးဘေ" to "Bills & Utilities",
        "လျှပ်စစ်" to "Bills & Utilities",
        "မီတာခ" to "Bills & Utilities",

        "entertainment" to "Entertainment",
        "entertain" to "Entertainment",
        "movie" to "Entertainment",
        "movies" to "Entertainment",
        "cinema" to "Entertainment",
        "games" to "Entertainment",
        "gaming" to "Entertainment",
        "game" to "Entertainment",
        "fun" to "Entertainment",
        "ဖျော်ဖြေရေး" to "Entertainment",

        "education" to "Education",
        "edu" to "Education",
        "school" to "Education",
        "course" to "Education",
        "courses" to "Education",
        "class" to "Education",
        "study" to "Education",
        "books" to "Education",
        "book" to "Education",
        "tuition" to "Education",
        "ပညာရေး" to "Education",
        "ကျောင်း" to "Education",

        "health" to "Health",
        "healthcare" to "Health",
        "medical" to "Health",
        "medicine" to "Health",
        "clinic" to "Health",
        "hospital" to "Health",
        "doctor" to "Health",
        "pharmacy" to "Health",
        "fitness" to "Health",
        "gym" to "Health",
        "ကျန်းမာရေး" to "Health",
        "ဆေး" to "Health",

        "other" to "Other",
        "others" to "Other",
        "misc" to "Other",
        "miscellaneous" to "Other",
        "general" to "Other",
        "အခြား" to "Other"
    )

    fun clean(text: String): String {
        return text.filter {
            it.isLetterOrDigit() ||
            it.isWhitespace() ||
            Character.getType(it) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(it) == Character.COMBINING_SPACING_MARK.toInt()
        }.lowercase().trim()
    }

    /**
     * Resolves an input category string to a matching ExpenseCategoryEntity.
     * Searches available categories first (falling back to DEFAULT_ENTITIES).
     */
    fun resolve(categoryName: String, available: List<ExpenseCategoryEntity>): ExpenseCategoryEntity? {
        if (categoryName.isBlank()) return null
        val pool = if (available.isNotEmpty()) available else DEFAULT_ENTITIES
        val cleanQuery = clean(categoryName)

        // 1. Direct exact match
        val directMatch = pool.firstOrNull { clean(it.name) == cleanQuery }
        if (directMatch != null) return directMatch

        // 2. Exact alias match
        val mappedCanonicalName = ALIAS_MAP[cleanQuery]
        if (mappedCanonicalName != null) {
            val aliasMatch = pool.firstOrNull { clean(it.name) == clean(mappedCanonicalName) }
            if (aliasMatch != null) return aliasMatch
        }

        // 3. Check if cleanQuery contains any alias key or vice versa (check longer/more specific keys first)
        val sortedAliases = ALIAS_MAP.entries.sortedByDescending { it.key.length }
        for ((aliasKey, canonical) in sortedAliases) {
            val cleanKey = clean(aliasKey)
            if (cleanQuery == cleanKey || cleanQuery.contains(cleanKey) || (cleanKey.length >= 4 && cleanKey.contains(cleanQuery))) {
                val match = pool.firstOrNull { clean(it.name) == clean(canonical) }
                if (match != null) return match
            }
        }

        // 4. Substring / word containment match against pool names
        val containMatch = pool.firstOrNull {
            val dbClean = clean(it.name)
            dbClean.contains(cleanQuery) || cleanQuery.contains(dbClean)
        }
        if (containMatch != null) return containMatch

        // 5. Fallback to "Other"
        return pool.firstOrNull { clean(it.name) == "other" }
    }

    /**
     * Translates category to natural Burmese name for chatbot responses.
     */
    fun toBurmeseName(categoryName: String): String {
        val cleanName = clean(categoryName)
        return when {
            cleanName.contains("food") || cleanName.contains("dining") -> "အစားအသောက်"
            cleanName.contains("transport") -> "ခရီးစရိတ်"
            cleanName.contains("shop") -> "ဈေးဝယ်စရိတ်"
            cleanName.contains("bill") || cleanName.contains("util") -> "ဘေလ်များ"
            cleanName.contains("entertain") -> "ဖျော်ဖြေရေး"
            cleanName.contains("edu") -> "ပညာရေး"
            cleanName.contains("health") -> "ကျန်းမာရေး"
            else -> "အသုံးစရိတ်"
        }
    }
}
