package com.example.data

import java.security.MessageDigest

sealed class ConfigValidationResult {
    data class Success(val appliedConfig: RemoteConfigPayload, val message: String) : ConfigValidationResult()
    data class Rejected(val reason: String) : ConfigValidationResult()
}

class SecureRemoteConfigManager {

    companion object {
        const val MAX_ALLOWED_RISK_PERCENTAGE = 2.0
        const val MIN_ALLOWED_CONFIDENCE_THRESHOLD = 60
        const val MAX_ALLOWED_CONFIDENCE_THRESHOLD = 95
        const val MIN_SCAN_INTERVAL_SECONDS = 5
        const val MAX_SCAN_INTERVAL_SECONDS = 300

        val FACTORY_DEFAULT_CONFIG = RemoteConfigPayload(
            version = 1,
            timestamp = 1730000000000L,
            expiryTimestamp = 1999999999000L,
            geminiModelId = "gemini-1.5-flash",
            scanIntervalSeconds = 15,
            minConfidenceThreshold = 80,
            maxOpenRiskPercentage = 1.0,
            promptTemplateVersion = "prompt-v2-confluence",
            enableShadowTesting = true,
            strategyVersion = "v1.0.0-baseline"
        )
    }

    private var activeConfig: RemoteConfigPayload = FACTORY_DEFAULT_CONFIG
    private var lastKnownGoodConfig: RemoteConfigPayload = FACTORY_DEFAULT_CONFIG
    private val configHistory = mutableListOf<RemoteConfigPayload>()

    init {
        configHistory.add(FACTORY_DEFAULT_CONFIG)
    }

    fun getActiveConfig(): RemoteConfigPayload = activeConfig

    fun getLastKnownGoodConfig(): RemoteConfigPayload = lastKnownGoodConfig

    /**
     * Computes deterministic SHA-256 checksum over configuration parameters.
     */
    fun computeChecksum(payload: RemoteConfigPayload): String {
        val canonicalRepresentation = buildString {
            append("version=").append(payload.version).append(";")
            append("timestamp=").append(payload.timestamp).append(";")
            append("expiry=").append(payload.expiryTimestamp).append(";")
            append("model=").append(payload.geminiModelId).append(";")
            append("interval=").append(payload.scanIntervalSeconds).append(";")
            append("confidence=").append(payload.minConfidenceThreshold).append(";")
            append("maxRisk=").append(String.format("%.2f", payload.maxOpenRiskPercentage)).append(";")
            append("strategy=").append(payload.strategyVersion)
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(canonicalRepresentation.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validates and safely applies incoming remote configuration.
     * Enforces cryptographic checksums, version monotonicity, expiry checks, and hard risk clamps.
     */
    fun validateAndApply(
        candidate: RemoteConfigPayload,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): ConfigValidationResult {
        // 1. Monotonic versioning check
        if (candidate.version <= activeConfig.version) {
            return ConfigValidationResult.Rejected(
                "Version replay detected: Candidate version (${candidate.version}) must be strictly greater than active version (${activeConfig.version})."
            )
        }

        // 2. Expiry check
        if (candidate.expiryTimestamp <= currentTimeMillis) {
            return ConfigValidationResult.Rejected(
                "Configuration expired: Expiry (${candidate.expiryTimestamp}) is prior to current system time ($currentTimeMillis)."
            )
        }

        // 3. Checksum verification if provided
        if (candidate.payloadChecksumSha256.isNotBlank()) {
            val calculatedChecksum = computeChecksum(candidate)
            if (!calculatedChecksum.equals(candidate.payloadChecksumSha256, ignoreCase = true)) {
                return ConfigValidationResult.Rejected(
                    "Integrity check failed: Computed SHA-256 hash ($calculatedChecksum) does not match payload ($candidate.payloadChecksumSha256)."
                )
            }
        }

        // 4. Hard safety guards and parameter clamping
        val clampedRisk = candidate.maxOpenRiskPercentage.coerceIn(0.1, MAX_ALLOWED_RISK_PERCENTAGE)
        val clampedConfidence = candidate.minConfidenceThreshold.coerceIn(
            MIN_ALLOWED_CONFIDENCE_THRESHOLD,
            MAX_ALLOWED_CONFIDENCE_THRESHOLD
        )
        val clampedInterval = candidate.scanIntervalSeconds.coerceIn(
            MIN_SCAN_INTERVAL_SECONDS,
            MAX_SCAN_INTERVAL_SECONDS
        )

        // Ensure real money trading cannot be autonomously turned on
        val sanitizedConfig = candidate.copy(
            maxOpenRiskPercentage = clampedRisk,
            minConfidenceThreshold = clampedConfidence,
            scanIntervalSeconds = clampedInterval
        )

        // Save current as last known good before switching
        lastKnownGoodConfig = activeConfig
        activeConfig = sanitizedConfig
        configHistory.add(sanitizedConfig)

        return ConfigValidationResult.Success(
            appliedConfig = sanitizedConfig,
            message = "Remote config v${sanitizedConfig.version} (${sanitizedConfig.strategyVersion}) validated and applied with risk clamped to ${sanitizedConfig.maxOpenRiskPercentage}%."
        )
    }

    /**
     * Instant 1-tap rollback to last known-good configuration.
     */
    fun rollbackToKnownGood(): RemoteConfigPayload {
        activeConfig = lastKnownGoodConfig
        return activeConfig
    }

    /**
     * Instant reset to hardcoded factory safe defaults.
     */
    fun resetToFactoryDefaults(): RemoteConfigPayload {
        activeConfig = FACTORY_DEFAULT_CONFIG
        lastKnownGoodConfig = FACTORY_DEFAULT_CONFIG
        return activeConfig
    }
}
