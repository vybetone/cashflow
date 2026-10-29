package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

data class AppReleaseInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSha256Checksum: String,
    val minSupportedSdk: Int = 26,
    val isCriticalSecurityUpdate: Boolean = false,
    val stagedRolloutPercentage: Int = 100,
    val isSuspendedDueToRegression: Boolean = false
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val releaseInfo: AppReleaseInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersionCode: Int) : UpdateCheckResult()
    data class RollbackRequired(val targetRollbackVersion: Int, val reason: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class AppUpdateManager(private val context: Context) {

    companion object {
        const val REPO_RELEASES_URL = "https://github.com/vybetone/cashflow/releases"
    }

    /**
     * Compares installed version with latest published release metadata.
     */
    fun evaluateRelease(
        currentVersionCode: Int,
        latestRelease: AppReleaseInfo,
        userDeviceHashMod: Int = 50 // 0-99 for staged rollout sampling
    ): UpdateCheckResult {
        if (latestRelease.isSuspendedDueToRegression) {
            return UpdateCheckResult.RollbackRequired(
                targetRollbackVersion = currentVersionCode - 1,
                reason = "Latest release suspended upstream due to detected runtime telemetry regression."
            )
        }

        if (latestRelease.versionCode > currentVersionCode) {
            // Check staged rollout threshold
            if (userDeviceHashMod < latestRelease.stagedRolloutPercentage || latestRelease.isCriticalSecurityUpdate) {
                return UpdateCheckResult.UpdateAvailable(latestRelease)
            } else {
                return UpdateCheckResult.UpToDate(currentVersionCode)
            }
        }

        return UpdateCheckResult.UpToDate(currentVersionCode)
    }

    /**
     * Cryptographically verifies the downloaded APK binary against the declared SHA-256 hash.
     */
    fun verifyApkChecksum(apkBytes: ByteArray, expectedSha256Hex: String): Boolean {
        if (apkBytes.isEmpty() || expectedSha256Hex.isBlank()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        val calculated = digest.digest(apkBytes).joinToString("") { "%02x".format(it) }
        return calculated.equals(expectedSha256Hex.trim(), ignoreCase = true)
    }

    /**
     * Constructs a secure Android PackageInstaller intent with proper FileProvider URI.
     */
    fun createInstallIntent(apkFile: File): Intent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val apkUri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } catch (e: Exception) {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        return intent
    }
}
