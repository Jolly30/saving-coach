package com.savingcoach.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BurmeseNumeralConverterTest {

    @Test
    fun testExpenseWordNotCorrupted() {
        val query = "ဒီလအသုံးစရိတ်ဘယ်လောက်ကျန်သေးလဲ"
        val converted = BurmeseNumeralConverter.convert(query)
        assertEquals("ဒီလအသုံးစရိတ်ဘယ်လောက်ကျန်သေးလဲ", converted)
    }

    @Test
    fun testSpendingPhrasesNotCorrupted() {
        val query = "ဒီလ ဘယ်လောက် သုံးထားလဲ"
        val converted = BurmeseNumeralConverter.convert(query)
        assertEquals("ဒီလ ဘယ်လောက် သုံးထားလဲ", converted)
    }

    @Test
    fun testMyanmarUnicodeDigits() {
        val query = "မနက်စာ ၃၀၀၀ ကျပ်"
        val converted = BurmeseNumeralConverter.convert(query)
        assertEquals("မနက်စာ 3000 ကျပ်", converted)
    }

    @Test
    fun testSpokenCompoundNumerals() {
        assertEquals("Coffee 2500", BurmeseNumeralConverter.convert("Coffee 2ထောင်5ရာ"))
        assertEquals("Saving 15000", BurmeseNumeralConverter.convert("Saving 1သောင်း5ထောင်"))
        assertEquals("Save 30000", BurmeseNumeralConverter.convert("Save 3သောင်း"))
        assertEquals("Phone 5000", BurmeseNumeralConverter.convert("Phone 5ထောင်"))
        assertEquals("Gold 100000", BurmeseNumeralConverter.convert("Gold 1သိန်း"))
    }

    @Test
    fun testCoffeeBurmeseDigits() {
        val query = "coffee ၂၅၀၀ မှတ်ပေး"
        val converted = BurmeseNumeralConverter.convert(query)
        assertEquals("coffee 2500 မှတ်ပေး", converted)
    }

    @Test
    fun testTargetCurrencyResolution() {
        // MMK mode
        assertEquals("MMK", InvestmentCalculations.getTargetCurrency("MMK", isInvestment = false))
        assertEquals("MMK", InvestmentCalculations.getTargetCurrency("MMK", isInvestment = true))

        // USD mode
        assertEquals("USD", InvestmentCalculations.getTargetCurrency("USD", isInvestment = false))
        assertEquals("USD", InvestmentCalculations.getTargetCurrency("USD", isInvestment = true))

        // Mixed mode (MMK for daily, USD for investments)
        assertEquals("MMK", InvestmentCalculations.getTargetCurrency("mixed", isInvestment = false))
        assertEquals("USD", InvestmentCalculations.getTargetCurrency("mixed", isInvestment = true))
    }

    @Test
    fun testCurrencyLabels() {
        assertEquals("MMK", InvestmentCalculations.getCurrencyLabel("MMK", isInvestment = false))
        assertEquals("MMK", InvestmentCalculations.getCurrencyLabel("MMK", isInvestment = true))

        assertEquals("$", InvestmentCalculations.getCurrencyLabel("USD", isInvestment = false))
        assertEquals("$", InvestmentCalculations.getCurrencyLabel("USD", isInvestment = true))

        assertEquals("MMK", InvestmentCalculations.getCurrencyLabel("mixed", isInvestment = false))
        assertEquals("$", InvestmentCalculations.getCurrencyLabel("mixed", isInvestment = true))
    }

    @Test
    fun testMultiCurrencyResolutionScenarios() {
        val usdRate = 4500.0

        // Scenario A: USD Mode
        val targetUSD = InvestmentCalculations.getTargetCurrency("USD", isInvestment = false)
        assertEquals("USD", targetUSD)
        val groceriesUSD = InvestmentCalculations.convertAmount(45000.0, "MMK", targetUSD, usdRate)
        val utilitiesUSD = InvestmentCalculations.convertAmount(90000.0, "MMK", targetUSD, usdRate)
        val coffeeUSD = InvestmentCalculations.convertAmount(5.0, "USD", targetUSD, usdRate)
        val softwareUSD = InvestmentCalculations.convertAmount(15.0, "USD", targetUSD, usdRate)
        val rainyDay1USD = InvestmentCalculations.convertAmount(18000.0, "MMK", targetUSD, usdRate)
        val rainyDay2USD = InvestmentCalculations.convertAmount(10.0, "USD", targetUSD, usdRate)

        assertEquals(10.0, groceriesUSD, 0.001)
        assertEquals(20.0, utilitiesUSD, 0.001)
        assertEquals(5.0, coffeeUSD, 0.001)
        assertEquals(15.0, softwareUSD, 0.001)
        assertEquals(50.0, groceriesUSD + utilitiesUSD + coffeeUSD + softwareUSD, 0.001)
        assertEquals(14.0, rainyDay1USD + rainyDay2USD, 0.001)

        // Scenario B: Mixed Mode
        val targetMixedExpense = InvestmentCalculations.getTargetCurrency("mixed", isInvestment = false)
        val targetMixedInv = InvestmentCalculations.getTargetCurrency("mixed", isInvestment = true)
        assertEquals("MMK", targetMixedExpense)
        assertEquals("USD", targetMixedInv)

        val groceriesMixed = InvestmentCalculations.convertAmount(45000.0, "MMK", targetMixedExpense, usdRate)
        val utilitiesMixed = InvestmentCalculations.convertAmount(90000.0, "MMK", targetMixedExpense, usdRate)
        val coffeeMixed = InvestmentCalculations.convertAmount(5.0, "USD", targetMixedExpense, usdRate)
        val softwareMixed = InvestmentCalculations.convertAmount(15.0, "USD", targetMixedExpense, usdRate)
        val rainyDay1Mixed = InvestmentCalculations.convertAmount(18000.0, "MMK", targetMixedExpense, usdRate)
        val rainyDay2Mixed = InvestmentCalculations.convertAmount(10.0, "USD", targetMixedExpense, usdRate)

        assertEquals(45000.0, groceriesMixed, 0.001)
        assertEquals(90000.0, utilitiesMixed, 0.001)
        assertEquals(22500.0, coffeeMixed, 0.001)
        assertEquals(67500.0, softwareMixed, 0.001)
        assertEquals(225000.0, groceriesMixed + utilitiesMixed + coffeeMixed + softwareMixed, 0.001)
        assertEquals(63000.0, rainyDay1Mixed + rainyDay2Mixed, 0.001)

        val etfMixed = InvestmentCalculations.convertAmount(100.0, "USD", targetMixedInv, usdRate)
        assertEquals(100.0, etfMixed, 0.001)
    }

    @Test
    fun testAppLanguageAndTranslations() {
        assertEquals(com.savingcoach.app.data.repository.AppLanguage.EN, com.savingcoach.app.data.repository.AppLanguage.fromCode("en"))
        assertEquals(com.savingcoach.app.data.repository.AppLanguage.MY, com.savingcoach.app.data.repository.AppLanguage.fromCode("my"))
        assertEquals(com.savingcoach.app.data.repository.AppLanguage.EN, com.savingcoach.app.data.repository.AppLanguage.fromCode("invalid"))

        val enStrings = com.savingcoach.app.ui.localization.AppLocale.getStrings(com.savingcoach.app.data.repository.AppLanguage.EN)
        val myStrings = com.savingcoach.app.ui.localization.AppLocale.getStrings(com.savingcoach.app.data.repository.AppLanguage.MY)

        assertEquals("Dashboard", enStrings.navDashboard)
        assertEquals("ပင်မ", myStrings.navDashboard)

        assertEquals("Expenses", enStrings.navExpenses)
        assertEquals("အသုံးစရိတ်", myStrings.navExpenses)

        assertEquals("Delete Expense", enStrings.deleteExpenseConfirmTitle)
        assertEquals("အသုံးစရိတ် ဖျက်ရန်", myStrings.deleteExpenseConfirmTitle)

        assertEquals("Stop Challenge", enStrings.stopChallengeTitle)
        assertEquals("စိန်ခေါ်မှု ရပ်တန့်ရန်", myStrings.stopChallengeTitle)

        assertEquals("Are you sure you want to delete this expense of 10,000 MMK?", enStrings.deleteExpenseConfirmMsg("10,000 MMK"))
        assertEquals("ပမာဏ 10,000 MMK ရှိသော ဤအသုံးစရိတ်ကို ဖျက်ရန် သေချာပါသလား?", myStrings.deleteExpenseConfirmMsg("10,000 MMK"))

        assertEquals("Edit Field of Work", enStrings.editFieldOfWorkTitle)
        assertEquals("လုပ်ငန်းနယ်ပယ် ပြင်ဆင်ရန်", myStrings.editFieldOfWorkTitle)

        assertEquals("Edit Username", enStrings.editUsernameTitle)
        assertEquals("အသုံးပြုသူအမည် ပြင်ဆင်ရန်", myStrings.editUsernameTitle)

        assertEquals("Edit Age", enStrings.editAgeTitle)
        assertEquals("အသက် ပြင်ဆင်ရန်", myStrings.editAgeTitle)

        assertEquals("Edit Gender", enStrings.editGenderTitle)
        assertEquals("ကျား/မ ပြင်ဆင်ရန်", myStrings.editGenderTitle)

        assertEquals("Edit Salary Range", enStrings.editSalaryTitle)
        assertEquals("လစာပမာဏ ပြင်ဆင်ရန်", myStrings.editSalaryTitle)

        assertEquals("Under 1,000,000 MMK", enStrings.salaryUnder1M)
        assertEquals("1,000,000 ကျပ် အောက်", myStrings.salaryUnder1M)

        assertEquals("1,000,000 - 3,000,000 MMK", enStrings.salary1MTo3M)
        assertEquals("1,000,000 - 3,000,000 ကျပ်", myStrings.salary1MTo3M)

        assertEquals("3,000,000 - 5,400,000 MMK", enStrings.salary3MTo54M)
        assertEquals("3,000,000 - 5,400,000 ကျပ်", myStrings.salary3MTo54M)

        assertEquals("5,400,000 - 10,000,000 MMK", enStrings.salary54MTo10M)
        assertEquals("5,400,000 - 10,000,000 ကျပ်", myStrings.salary54MTo10M)

        assertEquals("Above 10,000,000 MMK", enStrings.salaryAbove10M)
        assertEquals("10,000,000 ကျပ် အထက်", myStrings.salaryAbove10M)

        assertEquals("Export Data", enStrings.exportDataTitle)
        assertEquals("ဒေတာ ထုတ်ယူရန်", myStrings.exportDataTitle)

        assertEquals("Export to CSV", enStrings.exportToCsv)
        assertEquals("CSV ဖြင့် ထုတ်ယူမည်", myStrings.exportToCsv)

        assertEquals("Bills & Utilities", enStrings.categoryBillsAndUtilities)
        assertEquals("ဘေလ်နှင့် အသုံးစရိတ်များ", myStrings.categoryBillsAndUtilities)

        assertEquals("All", enStrings.calendarFilterAll)
        assertEquals("အားလုံး", myStrings.calendarFilterAll)

        assertEquals("August 2026", enStrings.formatMonthYear(java.time.YearMonth.of(2026, 8)))
        assertEquals("သြဂုတ် 2026", myStrings.formatMonthYear(java.time.YearMonth.of(2026, 8)))

        assertEquals(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"), enStrings.dayHeaders)
        assertEquals(listOf("တနင်္ဂနွေ", "တနင်္လာ", "အင်္ဂါ", "ဗုဒ္ဓဟူး", "ကြာသပတေး", "သောကြာ", "စနေ"), myStrings.dayHeaders)

        assertEquals("Expenses", enStrings.expenses)
        assertEquals("အသုံးစရိတ်", myStrings.expenses)

        assertEquals("Investments", enStrings.investments)
        assertEquals("ရင်းနှီးမြှုပ်နှံမှု", myStrings.investments)

        // Numeral conversion tests
        assertEquals("၁,၀၀၀,၀၀၀", BurmeseNumeralConverter.toBurmeseDigits("1,000,000"))
        assertEquals("၁၄,၁၆၈,၆၉၀", BurmeseNumeralConverter.toBurmeseDigits("14,168,690"))
        assertEquals("-၁၃,၁၆၈,၆၉၀", BurmeseNumeralConverter.toBurmeseDigits("-13,168,690"))

        assertEquals("1,000,000", enStrings.formatAmount(1000000.0))
        assertEquals("1,000,000", myStrings.formatAmount(1000000.0))

        assertEquals("14", enStrings.formatNumber(14))
        assertEquals("14", myStrings.formatNumber(14))

        assertEquals("Month", enStrings.monthView)
        assertEquals("လ", myStrings.monthView)

        assertEquals("Year", enStrings.yearView)
        assertEquals("နှစ်", myStrings.yearView)

        assertEquals("August", enStrings.formatMonthName(java.time.YearMonth.of(2026, 8)))
        assertEquals("သြဂုတ်", myStrings.formatMonthName(java.time.YearMonth.of(2026, 8)))

        assertEquals("August 2026", enStrings.formatMonthYear(java.time.YearMonth.of(2026, 8)))
        assertEquals("သြဂုတ် 2026", myStrings.formatMonthYear(java.time.YearMonth.of(2026, 8)))

        assertEquals("SPENDING BUCKETS", enStrings.spendingBuckets)
        assertEquals("သုံးစွဲမှု ကဏ္ဍများ", myStrings.spendingBuckets)

        assertEquals("New Bucket", enStrings.newBucket)
        assertEquals("ကဏ္ဍအသစ်", myStrings.newBucket)

        assertEquals("All Buckets", enStrings.allBuckets)
        assertEquals("ကဏ္ဍအားလုံး", myStrings.allBuckets)

        assertEquals("Food & Dining", enStrings.localizeCategory("Food & Dining"))
        assertEquals("အစားအသောက်", myStrings.localizeCategory("Food & Dining"))

        assertEquals("Entertainment", enStrings.localizeCategory("Entertainment"))
        assertEquals("ဖျော်ဖြေရေး", myStrings.localizeCategory("Entertainment"))

        assertEquals("Education", enStrings.localizeCategory("Education"))
        assertEquals("ပညာရေး", myStrings.localizeCategory("Education"))

        assertEquals("Health", enStrings.localizeCategory("Health"))
        assertEquals("ကျန်းမာရေး", myStrings.localizeCategory("Health"))

        val testDate = "2026-08-27"
        assertEquals("27 Aug 2026", enStrings.formatExpenseDateTime(0L, testDate))
        assertEquals("27 သြဂုတ် 2026", myStrings.formatExpenseDateTime(0L, testDate))

        assertEquals("Log Expense", enStrings.logExpenseTitle)
        assertEquals("အသုံးစရိတ် မှတ်တမ်းတင်ရန်", myStrings.logExpenseTitle)

        assertEquals("Save Expense", enStrings.saveExpense)
        assertEquals("အသုံးစရိတ် သိမ်းမည်", myStrings.saveExpense)

        assertEquals("Under 1,000,000 MMK", enStrings.salaryUnder1M)
        assertEquals("1,000,000 ကျပ် အောက်", myStrings.salaryUnder1M)

        // Challenge localization tests
        assertEquals("Saving Challenges", enStrings.challengesTitle)
        assertEquals("ငွေစုစိန်ခေါ်မှုများ", myStrings.challengesTitle)

        assertEquals("Total Saved", enStrings.totalSaved)
        assertEquals("စုစုပေါင်း စုငွေ", myStrings.totalSaved)

        assertEquals("New Challenge", enStrings.newChallenge)
        assertEquals("စိန်ခေါ်မှု အသစ်", myStrings.newChallenge)

        assertEquals("Edit Challenge", enStrings.editChallengeTitle)
        assertEquals("စိန်ခေါ်မှု ပြင်ဆင်ရန်", myStrings.editChallengeTitle)

        assertEquals("Stop Challenge", enStrings.stopChallengeTitle)
        assertEquals("စိန်ခေါ်မှု ရပ်တန့်ရန်", myStrings.stopChallengeTitle)

        assertEquals("Delete Challenge", enStrings.deleteChallengeTitle)
        assertEquals("စိန်ခေါ်မှု ဖျက်ရန်", myStrings.deleteChallengeTitle)

        assertEquals("Constant", enStrings.templateConstant)
        assertEquals("ပုံသေ", myStrings.templateConstant)

        assertEquals("Flexi", enStrings.templateFlexi)
        assertEquals("စိတ်ကြိုက်", myStrings.templateFlexi)

        assertEquals("Envelope", enStrings.templateEnvelope)
        assertEquals("စာအိတ်", myStrings.templateEnvelope)

        assertEquals("No-Spend", enStrings.templateNoSpend)
        assertEquals("မသုံးစွဲရ", myStrings.templateNoSpend)

        assertEquals("5 days left", enStrings.daysLeftCount(5L))
        assertEquals("5 ရက်ကျန်", myStrings.daysLeftCount(5L))

        assertEquals("3 of 10 steps done", enStrings.stepsDoneCount(3, 10))
        assertEquals("3 / 10 ပြီးစီး", myStrings.stepsDoneCount(3, 10))

        assertEquals("3 of 10 days completed", enStrings.daysCompletedCount(3, 10))
        assertEquals("3 / 10 ရက် ပြီးစီး", myStrings.daysCompletedCount(3, 10))

        assertEquals("50% complete", enStrings.percentComplete(50))
        assertEquals("50% ပြီးစီး", myStrings.percentComplete(50))

        assertEquals("Save 10,000 / day", enStrings.savePerDay("10,000"))
        assertEquals("တစ်ရက်လျှင် 10,000 စုပါ", myStrings.savePerDay("10,000"))

        assertEquals("Saved Envelope #5", enStrings.savedEnvelopeNumber(5))
        assertEquals("စုဆောင်းပြီး စာအိတ် #5", myStrings.savedEnvelopeNumber(5))

        assertEquals("Zero Spend Day 3", enStrings.zeroSpendDayTitle(3))
        assertEquals("ငွေမသုံးစွဲသောရက် 3", myStrings.zeroSpendDayTitle(3))

        assertEquals("A custom saving streak to hit your goal", enStrings.challengeCustomDesc)
        assertEquals("သင့်ပစ်မှတ်ပြည့်မီစေရန် စိတ်ကြိုက်ငွေစုခရီးစဉ်", myStrings.challengeCustomDesc)

        assertEquals("Pick an envelope, save the number", enStrings.challengeEnvelopeDesc)
        assertEquals("စာအိတ်တစ်ခုရွေးပြီး ဖော်ပြထားသော ပမာဏကို စုဆောင်းပါ", myStrings.challengeEnvelopeDesc)

        assertEquals("One intense week of saving", enStrings.challenge7DayDesc)
        assertEquals("တစ်ပတ်တာ စုဆောင်းမှု ခရီးစဉ်", myStrings.challenge7DayDesc)

        assertEquals("Zero non-essentials for 7 days", enStrings.challengeNoSpendDesc)
        assertEquals("7 ရက်ကြာ မလိုအပ်သောအသုံးစရိတ်များ လုံးဝမသုံးစွဲရန်", myStrings.challengeNoSpendDesc)

        assertEquals("3 Days Left", enStrings.localizeChallengeTitle("3 Days Left"))
        assertEquals("3 ရက်ကျန်", myStrings.localizeChallengeTitle("3 Days Left"))

        // Investment localization tests
        assertEquals("Search holdings and news...", enStrings.searchHoldingsAndNews)
        assertEquals("ပိုင်ဆိုင်မှုများနှင့် သတင်းများ ရှာဖွေရန်...", myStrings.searchHoldingsAndNews)

        assertEquals("News", enStrings.news)
        assertEquals("သတင်းများ", myStrings.news)

        assertEquals("SOLD OUT", enStrings.soldOut)
        assertEquals("ရောင်းချပြီး", myStrings.soldOut)

        assertEquals("Units owned", enStrings.unitsOwned)
        assertEquals("ပိုင်ဆိုင်သည့် အရေအတွက်", myStrings.unitsOwned)

        assertEquals("Avg buy price", enStrings.avgBuyPrice)
        assertEquals("ပျမ်းမျှ ဝယ်ယူဈေး", myStrings.avgBuyPrice)

        assertEquals("Current value", enStrings.currentValue)
        assertEquals("လက်ရှိ တန်ဖိုး", myStrings.currentValue)

        assertEquals("Save to Portfolio", enStrings.saveToPortfolio)
        assertEquals("ရင်းနှီးမြှုပ်နှံမှုတွင် သိမ်းမည်", myStrings.saveToPortfolio)
    }
}
