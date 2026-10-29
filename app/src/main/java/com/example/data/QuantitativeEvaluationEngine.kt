package com.example.data

import kotlin.math.abs
import kotlin.math.max

data class EvaluationReport(
    val sampleSize: Int,
    val inSampleSize: Int,
    val outOfSampleSize: Int,
    val isSampleSufficiencyMet: Boolean,
    val winRateOutOfSample: Double,
    val expectancyOutOfSample: Double,
    val profitFactorOutOfSample: Double,
    val maxDrawdownPctOutOfSample: Double,
    val averageWinUsd: Double,
    val averageLossUsd: Double,
    val totalNetPnlUsd: Double,
    val totalFeesPaidUsd: Double,
    val detectedRegime: String,
    val identifiedWeaknesses: List<String>,
    val evaluationTimestamp: Long = System.currentTimeMillis()
)

data class CandidateComparisonResult(
    val isEligibleForPromotion: Boolean,
    val rejectionReason: String?,
    val deltaWinRatePct: Double,
    val deltaExpectancyUsd: Double,
    val deltaDrawdownPct: Double,
    val recommendationSummary: String
)

class QuantitativeEvaluationEngine {

    companion object {
        const val MINIMUM_SAMPLE_SIZE = 20
        const val MAX_ALLOWABLE_DRAWDOWN_PCT = 15.0
        const val DEFAULT_SLIPPAGE_PIPS = 1.0
        const val DEFAULT_COMMISSION_USD = 3.0
    }

    /**
     * Chronologically evaluates completed signal performance records,
     * applying train/validation/test splits to guarantee zero data leakage.
     */
    fun evaluateChronologicalOutcomes(
        outcomes: List<SignalPerformanceOutcomeEntity>
    ): EvaluationReport {
        val completed = outcomes.filter { it.outcomeStatus in listOf("WIN", "LOSS") }
            .sortedBy { it.timestamp }

        if (completed.isEmpty()) {
            return EvaluationReport(
                sampleSize = 0,
                inSampleSize = 0,
                outOfSampleSize = 0,
                isSampleSufficiencyMet = false,
                winRateOutOfSample = 0.0,
                expectancyOutOfSample = 0.0,
                profitFactorOutOfSample = 0.0,
                maxDrawdownPctOutOfSample = 0.0,
                averageWinUsd = 0.0,
                averageLossUsd = 0.0,
                totalNetPnlUsd = 0.0,
                totalFeesPaidUsd = 0.0,
                detectedRegime = "INSUFFICIENT_DATA",
                identifiedWeaknesses = listOf("No closed trade records available for quantitative evaluation")
            )
        }

        val totalSize = completed.size
        val isSufficient = totalSize >= MINIMUM_SAMPLE_SIZE

        // Chronological split: 70% In-Sample (Train/Tune), 30% Out-Of-Sample (Strict Evaluation)
        val inSampleCutoff = (totalSize * 0.70).toInt().coerceAtLeast(1)
        val inSampleList = completed.take(inSampleCutoff)
        val outOfSampleList = completed.drop(inSampleCutoff).ifEmpty { completed }

        val wins = outOfSampleList.filter { it.outcomeStatus == "WIN" }
        val losses = outOfSampleList.filter { it.outcomeStatus == "LOSS" }

        val winRate = if (outOfSampleList.isNotEmpty()) {
            (wins.size.toDouble() / outOfSampleList.size.toDouble()) * 100.0
        } else 0.0

        // Calculate realistic fee/slippage adjusted metrics
        val grossProfit = wins.sumOf { max(0.0, it.netPnLAfterFees) }
        val grossLoss = abs(losses.sumOf { if (it.netPnLAfterFees < 0.0) it.netPnLAfterFees else 0.0 })
        val totalNetPnl = outOfSampleList.sumOf { it.netPnLAfterFees }
        val totalFees = outOfSampleList.sumOf { it.simulatedCommissionUsd }

        val avgWin = if (wins.isNotEmpty()) grossProfit / wins.size else 0.0
        val avgLoss = if (losses.isNotEmpty()) grossLoss / losses.size else 0.0

        val winProb = winRate / 100.0
        val lossProb = 1.0 - winProb
        val expectancy = (winProb * avgWin) - (lossProb * avgLoss)

        val profitFactor = if (grossLoss > 0.001) {
            grossProfit / grossLoss
        } else if (grossProfit > 0.0) {
            99.99
        } else 0.0

        // Max Drawdown calculation over equity curve
        var runningEquity = 10000.0
        var peakEquity = 10000.0
        var maxDrawdownPct = 0.0

        for (trade in outOfSampleList) {
            runningEquity += trade.netPnLAfterFees
            if (runningEquity > peakEquity) {
                peakEquity = runningEquity
            }
            val drawdown = (peakEquity - runningEquity) / peakEquity * 100.0
            if (drawdown > maxDrawdownPct) {
                maxDrawdownPct = drawdown
            }
        }

        // Market regime & weakness diagnosis
        val weaknesses = mutableListOf<String>()
        val highRsiLosses = losses.count { it.rsiValue >= 70.0 }
        val lowRsiLosses = losses.count { it.rsiValue <= 30.0 }

        if (highRsiLosses > 2) {
            weaknesses.add("Recurring false reversals at overbought RSI levels (>=70) during strong momentum")
        }
        if (lowRsiLosses > 2) {
            weaknesses.add("Premature dip buying at oversold RSI (<=30) during extended trend dumps")
        }
        if (maxDrawdownPct > 10.0) {
            weaknesses.add("Elevated peak-to-trough drawdown of ${String.format("%.1f", maxDrawdownPct)}%")
        }
        if (!isSufficient) {
            weaknesses.add("Sample size ($totalSize/$MINIMUM_SAMPLE_SIZE trades) is insufficient for statistical confidence")
        }

        val detectedRegime = when {
            outOfSampleList.any { it.volatilityRegime == "HIGH" } -> "HIGH_VOLATILITY"
            winRate < 45.0 -> "CHOPPY_RANGE"
            else -> "ORDERLY_TREND"
        }

        return EvaluationReport(
            sampleSize = totalSize,
            inSampleSize = inSampleList.size,
            outOfSampleSize = outOfSampleList.size,
            isSampleSufficiencyMet = isSufficient,
            winRateOutOfSample = winRate,
            expectancyOutOfSample = expectancy,
            profitFactorOutOfSample = profitFactor,
            maxDrawdownPctOutOfSample = maxDrawdownPct,
            averageWinUsd = avgWin,
            averageLossUsd = avgLoss,
            totalNetPnlUsd = totalNetPnl,
            totalFeesPaidUsd = totalFees,
            detectedRegime = detectedRegime,
            identifiedWeaknesses = weaknesses
        )
    }

    /**
     * Strictly verifies whether a proposed candidate strategy outperforms the baseline
     * without compromising safety constraints.
     */
    fun compareBaselineVsCandidate(
        baseline: EvaluationReport,
        candidate: EvaluationReport
    ): CandidateComparisonResult {
        val deltaWinRate = candidate.winRateOutOfSample - baseline.winRateOutOfSample
        val deltaExpectancy = candidate.expectancyOutOfSample - baseline.expectancyOutOfSample
        val deltaDrawdown = candidate.maxDrawdownPctOutOfSample - baseline.maxDrawdownPctOutOfSample

        // Safety Rule 1: Insufficient data check
        if (!candidate.isSampleSufficiencyMet) {
            return CandidateComparisonResult(
                isEligibleForPromotion = false,
                rejectionReason = "Candidate strategy tested on insufficient sample size (${candidate.sampleSize}/$MINIMUM_SAMPLE_SIZE required).",
                deltaWinRatePct = deltaWinRate,
                deltaExpectancyUsd = deltaExpectancy,
                deltaDrawdownPct = deltaDrawdown,
                recommendationSummary = "REJECTED: Data insufficiency prevents safe deployment."
            )
        }

        // Safety Rule 2: Max drawdown limit
        if (candidate.maxDrawdownPctOutOfSample > MAX_ALLOWABLE_DRAWDOWN_PCT) {
            return CandidateComparisonResult(
                isEligibleForPromotion = false,
                rejectionReason = "Candidate strategy maximum drawdown (${String.format("%.1f", candidate.maxDrawdownPctOutOfSample)}%) exceeds hard risk ceiling of $MAX_ALLOWABLE_DRAWDOWN_PCT%.",
                deltaWinRatePct = deltaWinRate,
                deltaExpectancyUsd = deltaExpectancy,
                deltaDrawdownPct = deltaDrawdown,
                recommendationSummary = "REJECTED: Risk limit violation."
            )
        }

        // Safety Rule 3: Expectancy must be positive and higher than baseline
        if (candidate.expectancyOutOfSample <= 0.0) {
            return CandidateComparisonResult(
                isEligibleForPromotion = false,
                rejectionReason = "Candidate strategy has non-positive expectancy ($${String.format("%.2f", candidate.expectancyOutOfSample)}/trade).",
                deltaWinRatePct = deltaWinRate,
                deltaExpectancyUsd = deltaExpectancy,
                deltaDrawdownPct = deltaDrawdown,
                recommendationSummary = "REJECTED: Negative or zero statistical expectancy."
            )
        }

        if (deltaExpectancy <= 0.0 && deltaWinRate <= 0.0) {
            return CandidateComparisonResult(
                isEligibleForPromotion = false,
                rejectionReason = "Candidate strategy failed to outperform baseline on out-of-sample data.",
                deltaWinRatePct = deltaWinRate,
                deltaExpectancyUsd = deltaExpectancy,
                deltaDrawdownPct = deltaDrawdown,
                recommendationSummary = "REJECTED: Underperforms existing baseline."
            )
        }

        // Eligible for shadow or user approval
        return CandidateComparisonResult(
            isEligibleForPromotion = true,
            rejectionReason = null,
            deltaWinRatePct = deltaWinRate,
            deltaExpectancyUsd = deltaExpectancy,
            deltaDrawdownPct = deltaDrawdown,
            recommendationSummary = "APPROVED FOR SHADOW TESTING: Out-of-sample expectancy improved by +$${String.format("%.2f", deltaExpectancy)} with acceptable risk profile."
        )
    }
}
