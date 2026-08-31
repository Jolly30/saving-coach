package com.savingcoach.app.utils

import kotlin.math.ceil

enum class ActivityTier { LOW, NEUTRAL, HIGH }
enum class ActivityPhase { COLD_START, CALIBRATION, PERCENTILE }

data class ActivityTierResult(
    val tier: ActivityTier,
    val phase: ActivityPhase,
    val thresholdLow: Double,
    val thresholdHigh: Double
)

object ActivityTierCalculator {

    fun calculateTier(currentAmount: Double, history: List<Double>): ActivityTierResult {
        // Filter out zero or negative values to get genuine history
        val validHistory = history.filter { it > 0.0 }
        
        // Phase 1: Cold Start
        if (validHistory.size < 7) {
            return ActivityTierResult(
                tier = ActivityTier.NEUTRAL,
                phase = ActivityPhase.COLD_START,
                thresholdLow = 0.0,
                thresholdHigh = 0.0
            )
        }
        
        // Phase 2: Calibration (7 to 13 entries)
        if (validHistory.size in 7..13) {
            val median = calculateMedian(validHistory)
            val thresholdLow = 0.7 * median
            val thresholdHigh = 1.3 * median
            
            val tier = when {
                currentAmount < thresholdLow -> ActivityTier.LOW
                currentAmount > thresholdHigh -> ActivityTier.HIGH
                else -> ActivityTier.NEUTRAL
            }
            
            return ActivityTierResult(
                tier = tier,
                phase = ActivityPhase.CALIBRATION,
                thresholdLow = thresholdLow,
                thresholdHigh = thresholdHigh
            )
        }
        
        // Phase 3: Rolling Percentiles (>= 14 entries)
        val recentHistory = validHistory.takeLast(30).sorted()
        val p20 = calculatePercentile(recentHistory, 20.0)
        val p80 = calculatePercentile(recentHistory, 80.0)
        
        if (p20 == p80) {
            return ActivityTierResult(
                tier = ActivityTier.NEUTRAL,
                phase = ActivityPhase.PERCENTILE,
                thresholdLow = p20,
                thresholdHigh = p80
            )
        }
        
        val tier = when {
            currentAmount <= p20 -> ActivityTier.LOW
            currentAmount >= p80 -> ActivityTier.HIGH
            else -> ActivityTier.NEUTRAL
        }
        
        return ActivityTierResult(
            tier = tier,
            phase = ActivityPhase.PERCENTILE,
            thresholdLow = p20,
            thresholdHigh = p80
        )
    }

    private fun calculateMedian(sortedList: List<Double>): Double {
        val sorted = sortedList.sorted()
        val size = sorted.size
        if (size == 0) return 0.0
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
        } else {
            sorted[size / 2]
        }
    }

    private fun calculatePercentile(sortedList: List<Double>, percentile: Double): Double {
        if (sortedList.isEmpty()) return 0.0
        if (sortedList.size == 1) return sortedList.first()
        
        val index = (percentile / 100.0) * (sortedList.size - 1)
        val lower = index.toInt()
        val upper = ceil(index).toInt()
        
        if (lower == upper) {
            return sortedList[lower]
        }
        
        val weight = index - lower
        return sortedList[lower] * (1 - weight) + sortedList[upper] * weight
    }
}
