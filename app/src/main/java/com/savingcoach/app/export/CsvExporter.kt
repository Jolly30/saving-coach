package com.savingcoach.app.export

import android.content.Context
import com.savingcoach.app.data.model.Expense
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File

object CsvExporter {

    fun formatDateToDayMonthYear(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return try {
            val parts = dateStr.trim().split(Regex("[-/]"))
            if (parts.size == 3 && parts[0].length == 4) {
                val year = parts[0]
                val month = parts[1].padStart(2, '0')
                val day = parts[2].padStart(2, '0')
                "$day/$month/$year"
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun exportExpensesToCsv(context: Context, expenses: List<Expense>): File {
        val fileName = "expenses_export_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        val fos = java.io.FileOutputStream(file)
        fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // Explicit BOM
        val writer = java.io.OutputStreamWriter(fos, kotlin.text.Charsets.UTF_8)
        val format = CSVFormat.Builder.create()
            .setHeader("Date", "Category", "Merchant", "Amount (MMK)", "Notes")
            .build()
            
        val printer = CSVPrinter(writer, format)
        
        // Sort chronologically in increasing order (oldest to newest, e.g. August to September)
        val sortedExpenses = expenses.sortedWith(
            compareBy<Expense> { it.date }.thenBy { it.createdAt }
        )

        try {
            for (expense in sortedExpenses) {
                printer.printRecord(
                    formatDateToDayMonthYear(expense.date),
                    expense.category,
                    expense.merchant.ifBlank { "N/A" },
                    String.format(java.util.Locale.US, "%.2f", expense.amount),
                    expense.description.ifBlank { "" }
                )
            }
        } finally {
            printer.flush()
            printer.close()
        }
        
        return file
    }

    private val EMOJI_REGEX = Regex("[\\x{1F300}-\\x{1F5FF}\\x{1F900}-\\x{1F9FF}\\x{1F600}-\\x{1F64F}\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{1F1E6}-\\x{1F1FF}\\x{1F191}-\\x{1F251}\\x{1F004}\\x{1F0CF}\\x{1F170}-\\x{1F171}\\x{1F17E}-\\x{1F17F}\\x{1F18E}\\x{3030}\\x{2B50}\\x{2B55}\\x{2934}-\\x{2935}\\x{2B05}-\\x{2B07}\\x{2B1B}-\\x{2B1C}\\x{3297}\\x{3299}\\x{303D}\\x{00A9}\\x{00AE}\\x{2122}\\x{23F3}\\x{24C2}\\x{23E9}-\\x{23EF}\\x{25B6}\\x{23F8}-\\x{23FA}\\x{1FA70}-\\x{1FAFF}]")

    private fun stripEmoji(input: String): String {
        return EMOJI_REGEX.replace(input, "").trim()
    }

    fun exportSavingsToCsv(
        context: Context, 
        deposits: List<com.savingcoach.app.data.model.SavingsDeposit>,
        challenges: Map<String, com.savingcoach.app.data.model.SavingChallenge>
    ): File {
        val timestamp = System.currentTimeMillis()
        val file = java.io.File(context.cacheDir, "saving_history_$timestamp.csv")
        val fos = java.io.FileOutputStream(file)
        
        // Explicitly write UTF-8 BOM bytes so mobile Excel recognizes it
        fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        
        val writer = java.io.OutputStreamWriter(fos, kotlin.text.Charsets.UTF_8)
        val format = org.apache.commons.csv.CSVFormat.Builder.create()
            .setHeader("Date", "End Date", "Challenge Name", "Deposit Amount", "Target Amount")
            .build()
            
        val printer = org.apache.commons.csv.CSVPrinter(writer, format)
        
        // Sort chronologically in increasing order (oldest to newest, e.g. August to September)
        val sortedDeposits = deposits.sortedWith(
            compareBy<com.savingcoach.app.data.model.SavingsDeposit> { it.date }.thenBy { it.createdAt }
        )

        try {
            for (deposit in sortedDeposits) {
                val challenge = challenges[deposit.challengeId]
                if (challenge != null) {
                    val status = getChallengeStatus(challenge)
                    printer.printRecord(
                        formatDateToDayMonthYear(deposit.date),
                        formatDateToDayMonthYear(status),
                        stripEmoji(challenge.title),
                        String.format(java.util.Locale.US, "%.2f", deposit.amount),
                        String.format(java.util.Locale.US, "%.2f", challenge.targetAmount)
                    )
                }
            }
        } finally {
            printer.flush()
            printer.close()
        }
        
        return file
    }

    private fun getChallengeStatus(challenge: com.savingcoach.app.data.model.SavingChallenge): String {
        if (challenge.isCompleted || challenge.currentAmount >= challenge.targetAmount) {
            val lastDepositStr = challenge.lastDepositDate
            return if (lastDepositStr.isNotBlank()) lastDepositStr else "Completed"
        } else {
            return if (challenge.isActive) "Active" else "-"
        }
    }

    fun exportInvestmentsToCsv(
        context: Context,
        holdings: List<com.savingcoach.app.data.model.ComputedHolding>
    ): File {
        val timestamp = System.currentTimeMillis()
        val file = java.io.File(context.cacheDir, "investments_export_$timestamp.csv")
        val fos = java.io.FileOutputStream(file)
        
        fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        
        val writer = java.io.OutputStreamWriter(fos, kotlin.text.Charsets.UTF_8)
        val format = org.apache.commons.csv.CSVFormat.Builder.create()
            .setHeader("Symbol", "Name", "Units", "Avg Buy Price (MMK)", "Live/Exit Price (MMK)", "Cost Basis (MMK)", "Current/Exit Value (MMK)", "P&L (MMK)", "ROI (%)", "Status")
            .build()
            
        val printer = org.apache.commons.csv.CSVPrinter(writer, format)
        
        try {
            for (computed in holdings) {
                val status = if (computed.holding.isStoppedCompat) "Sold Out" else "Holding"
                printer.printRecord(
                    computed.holding.displayTicker,
                    computed.holding.name,
                    computed.holding.units.toString(),
                    String.format(java.util.Locale.US, "%.2f", computed.holding.buyPrice),
                    String.format(java.util.Locale.US, "%.2f", computed.livePrice),
                    String.format(java.util.Locale.US, "%.2f", computed.costBasis),
                    String.format(java.util.Locale.US, "%.2f", computed.liquidValue),
                    String.format(java.util.Locale.US, "%.2f", computed.unrealizedPL),
                    String.format(java.util.Locale.US, "%.2f%%", computed.roiPercentage),
                    status
                )
            }
        } finally {
            printer.flush()
            printer.close()
        }
        
        return file
    }
}
