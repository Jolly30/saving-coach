package com.savingcoach.app.utils

import com.savingcoach.app.data.model.ComputedHolding
import com.savingcoach.app.data.model.PortfolioSummary
import com.savingcoach.app.data.model.UserHolding
import java.text.NumberFormat
import java.util.Locale

/**
 * Pure calculation functions for investment portfolio computations.
 * All functions are stateless and deterministic.
 */
object InvestmentCalculations {

    /**
     * Calculate cost basis for a single holding.
     * costBasis = units * buyPrice
     */
    fun calculateCostBasis(units: Double, buyPrice: Double): Double {
        return units * buyPrice
    }

    /**
     * Calculate liquid value for a single holding.
     * liquidValue = units * livePrice
     */
    fun calculateLiquidValue(units: Double, livePrice: Double): Double {
        return units * livePrice
    }

    /**
     * Calculate unrealized profit/loss for a single holding.
     * unrealizedPL = liquidValue - costBasis
     */
    fun calculateUnrealizedPL(liquidValue: Double, costBasis: Double): Double {
        return liquidValue - costBasis
    }

    /**
     * Calculate ROI percentage for a single holding.
     * roiPercentage = buyPrice > 0 ? ((livePrice - buyPrice) / buyPrice) * 100 : 0
     */
    fun calculateROIPercentage(buyPrice: Double, livePrice: Double): Double {
        return if (buyPrice > 0) {
            ((livePrice - buyPrice) / buyPrice) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Compute all derived values for a single holding given live market data.
     */
    fun computeHolding(
        holding: UserHolding,
        livePrice: Double,
        change24h: Double,
        currencyRateMultiplier: Double = 1.0
    ): ComputedHolding {
        val convertedBuyPrice = holding.buyPrice * currencyRateMultiplier
        val costBasis = calculateCostBasis(holding.units, convertedBuyPrice)
        
        val currentPrice = if (holding.isStoppedCompat) holding.exitPrice * currencyRateMultiplier else livePrice
        val currentChange24h = if (holding.isStoppedCompat) 0.0 else change24h

        val liquidValue = calculateLiquidValue(holding.units, currentPrice)
        val unrealizedPL = calculateUnrealizedPL(liquidValue, costBasis)
        val roiPercentage = calculateROIPercentage(convertedBuyPrice, currentPrice)

        return ComputedHolding(
            holding = holding,
            livePrice = currentPrice,
            change24h = currentChange24h,
            costBasis = costBasis,
            liquidValue = liquidValue,
            unrealizedPL = unrealizedPL,
            roiPercentage = roiPercentage
        )
    }

    /**
     * Compute portfolio summary from a list of computed holdings.
     */
    fun computePortfolioSummary(holdings: List<ComputedHolding>): PortfolioSummary {
        val activeHoldings = holdings.filter { !it.holding.isStoppedCompat }
        val stoppedHoldings = holdings.filter { it.holding.isStoppedCompat }

        val totalLiquidValue = activeHoldings.sumOf { it.liquidValue }
        val totalCostBasis = activeHoldings.sumOf { it.costBasis }
        val totalUnrealizedPL = totalLiquidValue - totalCostBasis
        
        val totalRealizedPL = stoppedHoldings.sumOf { it.unrealizedPL }

        val totalROI = if (totalCostBasis > 0) {
            (totalUnrealizedPL / totalCostBasis) * 100.0
        } else {
            0.0
        }

        val stockValue = activeHoldings
            .filter { it.holding.type == "stock" }
            .sumOf { it.liquidValue }
        val cryptoValue = activeHoldings
            .filter { it.holding.type == "crypto" }
            .sumOf { it.liquidValue }

        return PortfolioSummary(
            totalLiquidValue = totalLiquidValue,
            totalCostBasis = totalCostBasis,
            totalUnrealizedPL = totalUnrealizedPL,
            totalRealizedPL = totalRealizedPL,
            totalROI = totalROI,
            stockValue = stockValue,
            cryptoValue = cryptoValue
        )
    }

    /**
     * Determine the active target currency based on user preference and domain.
     */
    fun getTargetCurrency(currencyPreference: String, isInvestment: Boolean = false): String {
        return when (currencyPreference.lowercase()) {
            "usd" -> "USD"
            "mixed" -> if (isInvestment) "USD" else "MMK"
            else -> "MMK"
        }
    }

    /**
     * Convert an amount from one currency to another using the USD/MMK exchange rate.
     */
    fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String, usdRate: Double): Double {
        val from = fromCurrency.trim().uppercase()
        val to = toCurrency.trim().uppercase()
        if (from == to || from.isEmpty() || to.isEmpty()) return amount

        return when {
            from == "MMK" && to == "USD" -> if (usdRate > 0.0) amount / usdRate else amount
            from == "USD" && to == "MMK" -> amount * (if (usdRate > 0.0) usdRate else 1.0)
            else -> amount
        }
    }

    /**
     * Format a USD amount with proper currency formatting.
     * Uses Intl.NumberFormat equivalent for consistent display.
     */
    fun formatCurrency(amount: Double): String {
        val format = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        return format.format(amount)
    }

    /**
     * Format an amount dynamically with consistent number formatting.
     */
    fun formatValue(amount: Double, currencyPreference: String = "MMK", rate: Double = 1.0, isInvestment: Boolean = false): String {
        val format = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        return format.format(amount)
    }

    /**
     * Get display currency label ($ or MMK) based on preference and domain.
     */
    fun getCurrencyLabel(currencyPreference: String, isInvestment: Boolean = false): String {
        return if (getTargetCurrency(currencyPreference, isInvestment) == "USD") "$" else "MMK"
    }


    /**
     * Format a percentage with sign and one decimal place.
     * Example: +25.3% or -7.1%
     */
    fun formatPercentage(percentage: Double): String {
        val sign = if (percentage >= 0) "+" else ""
        return "$sign${String.format("%.1f", percentage)}%"
    }

    /**
     * Format a P/L amount with sign and currency.
     * Example: +$682.50 or -$150.00
     */
    fun formatPL(amount: Double): String {
        val sign = if (amount >= 0) "+" else ""
        return "$sign${formatCurrency(amount)}"
    }

    /**
     * Format a P/L amount dynamically based on currency preference and exchange rate.
     */
    fun formatPLValue(amount: Double, currencyPreference: String, rate: Double, isInvestment: Boolean = false): String {
        val sign = if (amount >= 0) "+" else "-"
        return "$sign${formatValue(Math.abs(amount), currencyPreference, rate, isInvestment)}"
    }

    /**
     * Format units with appropriate precision.
     * Crypto: up to 8 decimal places
     * Stocks: 2 decimal places (whole shares display as integer)
     */
    fun formatUnits(units: Double, type: String): String {
        return if (type == "crypto") {
            // Remove trailing zeros, up to 8 decimals
            String.format("%.8f", units).trimEnd('0').trimEnd('.')
        } else {
            // Stocks: show as integer if whole number
            if (units == units.toLong().toDouble()) {
                units.toLong().toString()
            } else {
                String.format("%.2f", units)
            }
        }
    }

    /**
     * Get stock/crypto allocation percentages for donut chart.
     * Returns Pair(stockPercent, cryptoPercent).
     */
    fun getAllocationPercentages(summary: PortfolioSummary): Pair<Double, Double> {
        val total = summary.stockValue + summary.cryptoValue
        if (total <= 0) return Pair(0.0, 0.0)

        val stockPercent = (summary.stockValue / total) * 100.0
        val cryptoPercent = (summary.cryptoValue / total) * 100.0
        return Pair(stockPercent, cryptoPercent)
    }
}
