package com.example

import com.example.data.AppReleaseInfo
import com.example.data.AppUpdateManager
import com.example.data.CandidateComparisonResult
import com.example.data.ConfigValidationResult
import com.example.data.EvaluationReport
import com.example.data.ObservationSource
import com.example.data.QuantitativeEvaluationEngine
import com.example.data.RemoteConfigPayload
import com.example.data.SecureRemoteConfigManager
import com.example.data.SignalPerformanceOutcomeEntity
import com.example.data.UpdateCheckResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context

@RunWith(RobolectricTestRunner::class)
class ContinuousLearningAndEvaluationTest {

    private lateinit var configManager: SecureRemoteConfigManager
    private lateinit var evalEngine: QuantitativeEvaluationEngine
    private lateinit var updateManager: AppUpdateManager

    @Before
    fun setUp() {
        configManager = SecureRemoteConfigManager()
        evalEngine = QuantitativeEvaluationEngine()
        val context = ApplicationProvider.getApplicationContext<Context>()
        updateManager = AppUpdateManager(context)
    }

    @Test
    fun testConfigValidation_acceptsValidMonotonicConfigWithChecksum() {
        val candidate = RemoteConfigPayload(
            version = 2,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + 86400000L,
            geminiModelId = "gemini-1.5-flash",
            scanIntervalSeconds = 20,
            minConfidenceThreshold = 85,
            maxOpenRiskPercentage = 1.5,
            strategyVersion = "v1.1.0-tune"
        )
        val validChecksum = configManager.computeChecksum(candidate)
        val payloadWithChecksum = candidate.copy(payloadChecksumSha256 = validChecksum)

        val result = configManager.validateAndApply(payloadWithChecksum)
        assertTrue("Expected Success validation result", result is ConfigValidationResult.Success)
        val success = result as ConfigValidationResult.Success
        assertEquals(2, success.appliedConfig.version)
        assertEquals("v1.1.0-tune", success.appliedConfig.strategyVersion)
        assertEquals(1.5, success.appliedConfig.maxOpenRiskPercentage, 0.001)
    }

    @Test
    fun testConfigValidation_rejectsVersionReplay() {
        val replayCandidate = RemoteConfigPayload(
            version = 1, // Same as active factory version 1
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + 86400000L
        )

        val result = configManager.validateAndApply(replayCandidate)
        assertTrue(result is ConfigValidationResult.Rejected)
        val rejected = result as ConfigValidationResult.Rejected
        assertTrue(rejected.reason.contains("Version replay"))
    }

    @Test
    fun testConfigValidation_rejectsExpiredPayload() {
        val expiredCandidate = RemoteConfigPayload(
            version = 2,
            timestamp = System.currentTimeMillis() - 100000L,
            expiryTimestamp = System.currentTimeMillis() - 1000L // In the past
        )

        val result = configManager.validateAndApply(expiredCandidate)
        assertTrue(result is ConfigValidationResult.Rejected)
        val rejected = result as ConfigValidationResult.Rejected
        assertTrue(rejected.reason.contains("Configuration expired"))
    }

    @Test
    fun testConfigValidation_rejectsChecksumMismatch() {
        val candidate = RemoteConfigPayload(
            version = 3,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + 86400000L,
            payloadChecksumSha256 = "deadbeef12345678deadbeef12345678deadbeef12345678deadbeef12345678" // Invalid
        )

        val result = configManager.validateAndApply(candidate)
        assertTrue(result is ConfigValidationResult.Rejected)
        val rejected = result as ConfigValidationResult.Rejected
        assertTrue(rejected.reason.contains("Integrity check failed"))
    }

    @Test
    fun testConfigValidation_strictlyClampsRiskToCeiling() {
        val excessiveRiskCandidate = RemoteConfigPayload(
            version = 4,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + 86400000L,
            maxOpenRiskPercentage = 12.0 // Exceeds hard ceiling of 2.0%
        )

        val result = configManager.validateAndApply(excessiveRiskCandidate)
        assertTrue(result is ConfigValidationResult.Success)
        val applied = (result as ConfigValidationResult.Success).appliedConfig
        // Must be clamped to 2.0%
        assertEquals(2.0, applied.maxOpenRiskPercentage, 0.001)
    }

    @Test
    fun testOneTapRollback_restoresLastKnownGoodConfig() {
        val initialConfig = configManager.getActiveConfig()
        assertEquals(1, initialConfig.version)

        val v2 = RemoteConfigPayload(
            version = 2,
            timestamp = System.currentTimeMillis(),
            expiryTimestamp = System.currentTimeMillis() + 86400000L,
            strategyVersion = "v2.0.0-experimental"
        )
        configManager.validateAndApply(v2)
        assertEquals(2, configManager.getActiveConfig().version)

        // Execute rollback
        val reverted = configManager.rollbackToKnownGood()
        assertEquals(1, reverted.version)
        assertEquals(initialConfig.strategyVersion, reverted.strategyVersion)
    }

    @Test
    fun testEvaluationEngine_sampleSufficiencyEnforced() {
        // Less than 20 trades
        val fewTrades = (1..10).map { i ->
            SignalPerformanceOutcomeEntity(
                symbol = "EUR/USD",
                marketCategory = "FOREX",
                signalAction = "BUY",
                confidenceScore = 88,
                entryPrice = 1.0850,
                stopLoss = 1.0800,
                takeProfit = 1.0950,
                riskRewardRatio = 2.0,
                outcomeStatus = if (i % 2 == 0) "WIN" else "LOSS",
                netPnLAfterFees = if (i % 2 == 0) 45.0 else -25.0
            )
        }

        val report = evalEngine.evaluateChronologicalOutcomes(fewTrades)
        assertFalse("Sample size of 10 should not meet sufficiency", report.isSampleSufficiencyMet)
        assertEquals(10, report.sampleSize)
    }

    @Test
    fun testEvaluationEngine_candidateRejectedWhenDrawdownExceedsLimit() {
        val baseline = EvaluationReport(
            sampleSize = 30,
            inSampleSize = 21,
            outOfSampleSize = 9,
            isSampleSufficiencyMet = true,
            winRateOutOfSample = 60.0,
            expectancyOutOfSample = 15.0,
            profitFactorOutOfSample = 1.8,
            maxDrawdownPctOutOfSample = 8.0,
            averageWinUsd = 40.0,
            averageLossUsd = 20.0,
            totalNetPnlUsd = 120.0,
            totalFeesPaidUsd = 30.0,
            detectedRegime = "ORDERLY_TREND",
            identifiedWeaknesses = emptyList()
        )

        val excessiveDrawdownCandidate = baseline.copy(
            expectancyOutOfSample = 22.0, // Higher expectancy
            maxDrawdownPctOutOfSample = 18.5 // Exceeds 15% limit
        )

        val comparison = evalEngine.compareBaselineVsCandidate(baseline, excessiveDrawdownCandidate)
        assertFalse("Candidate with 18.5% drawdown must be rejected", comparison.isEligibleForPromotion)
        assertNotNull(comparison.rejectionReason)
        assertTrue(comparison.rejectionReason!!.contains("exceeds hard risk ceiling"))
    }

    @Test
    fun testEvaluationEngine_candidateRejectedWhenUnderperforming() {
        val baseline = EvaluationReport(
            sampleSize = 30,
            inSampleSize = 21,
            outOfSampleSize = 9,
            isSampleSufficiencyMet = true,
            winRateOutOfSample = 65.0,
            expectancyOutOfSample = 25.0,
            profitFactorOutOfSample = 2.1,
            maxDrawdownPctOutOfSample = 6.0,
            averageWinUsd = 50.0,
            averageLossUsd = 20.0,
            totalNetPnlUsd = 200.0,
            totalFeesPaidUsd = 27.0,
            detectedRegime = "ORDERLY_TREND",
            identifiedWeaknesses = emptyList()
        )

        val underperformingCandidate = baseline.copy(
            winRateOutOfSample = 55.0,
            expectancyOutOfSample = 12.0 // Lower expectancy
        )

        val comparison = evalEngine.compareBaselineVsCandidate(baseline, underperformingCandidate)
        assertFalse("Candidate with lower expectancy must be rejected", comparison.isEligibleForPromotion)
        assertTrue(comparison.rejectionReason!!.contains("failed to outperform baseline"))
    }

    @Test
    fun testEvaluationEngine_candidateAcceptedWhenOutperformingWithinRiskLimits() {
        val baseline = EvaluationReport(
            sampleSize = 30,
            inSampleSize = 21,
            outOfSampleSize = 9,
            isSampleSufficiencyMet = true,
            winRateOutOfSample = 60.0,
            expectancyOutOfSample = 15.0,
            profitFactorOutOfSample = 1.6,
            maxDrawdownPctOutOfSample = 7.0,
            averageWinUsd = 35.0,
            averageLossUsd = 20.0,
            totalNetPnlUsd = 135.0,
            totalFeesPaidUsd = 27.0,
            detectedRegime = "ORDERLY_TREND",
            identifiedWeaknesses = emptyList()
        )

        val winningCandidate = baseline.copy(
            winRateOutOfSample = 66.0,
            expectancyOutOfSample = 22.0,
            maxDrawdownPctOutOfSample = 6.5
        )

        val comparison = evalEngine.compareBaselineVsCandidate(baseline, winningCandidate)
        assertTrue("Candidate outperforming with acceptable drawdown must be approved", comparison.isEligibleForPromotion)
        assertTrue(comparison.deltaExpectancyUsd > 0.0)
    }

    @Test
    fun testAppUpdateManager_checksumVerification() {
        val dummyApkBytes = "Android APK Binary Payload Content".toByteArray(Charsets.UTF_8)
        val correctHash = "ae8c9a30282cf200e6dcbc56bb0d5db6576bbceba7fc677ba1c2a129f1b2b8c9" // Mock computed sha256

        // Compute actual SHA-256
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val expectedSha = digest.digest(dummyApkBytes).joinToString("") { "%02x".format(it) }

        assertTrue(updateManager.verifyApkChecksum(dummyApkBytes, expectedSha))
        assertFalse(updateManager.verifyApkChecksum(dummyApkBytes, "wronghash1234567890"))
    }

    @Test
    fun testAppUpdateManager_stagedRolloutAndRegressionRollback() {
        val releaseInfo = AppReleaseInfo(
            versionName = "2.0.0",
            versionCode = 200,
            releaseNotes = "Major update",
            downloadUrl = "https://example.com/apk",
            apkSha256Checksum = "abc",
            stagedRolloutPercentage = 20
        )

        // User with hash 10 is inside 20% staged rollout
        val resultEligible = updateManager.evaluateRelease(
            currentVersionCode = 100,
            latestRelease = releaseInfo,
            userDeviceHashMod = 10
        )
        assertTrue(resultEligible is UpdateCheckResult.UpdateAvailable)

        // User with hash 50 is outside 20% staged rollout -> stays UpToDate
        val resultNotYet = updateManager.evaluateRelease(
            currentVersionCode = 100,
            latestRelease = releaseInfo,
            userDeviceHashMod = 50
        )
        assertTrue(resultNotYet is UpdateCheckResult.UpToDate)

        // Suspended release due to regression
        val suspendedRelease = releaseInfo.copy(isSuspendedDueToRegression = true)
        val resultSuspended = updateManager.evaluateRelease(
            currentVersionCode = 200,
            latestRelease = suspendedRelease
        )
        assertTrue(resultSuspended is UpdateCheckResult.RollbackRequired)
    }
}
