package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.AssistantChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for formatting, deduplicating, redacting sensitive tokens,
 * and performing native Android clipboard copy & intent share for AI chat messages.
 */
object ChatExportHelper {

    // Regex patterns for sensitive credentials & tokens
    private val GEMINI_KEY_REGEX = Regex("AIza[0-9A-Za-z\\-_]{35}")
    private val BEARER_TOKEN_REGEX = Regex("(?i)Bearer\\s+([A-Za-z0-9_\\-\\.]+)")
    private val API_KEY_PAIR_REGEX = Regex("(?i)(api[_-]?key|access[_-]?token|secret[_-]?key|client[_-]?secret|private[_-]?key)[\"']?\\s*[:=]\\s*[\"']?([A-Za-z0-9_\\-\\.]+)")
    private val PASSWORD_PAIR_REGEX = Regex("(?i)(password|passwd|passphrase|investor[_-]?password)[\"']?\\s*[:=]\\s*[\"']?([^\\s\"'`]+)")
    private val ACCOUNT_ID_REGEX = Regex("(?i)(account[_-]?number|account[_-]?id|broker[_-]?login|login[_-]?id|acc\\s*#)[\"']?\\s*[:=]\\s*[\"']?(\\d{5,})")
    private val ANSI_CONTROL_REGEX = Regex("\u001B\\[[;?0-9]*[a-zA-Z]")

    /**
     * Redacts known sensitive fields (API keys, bearer tokens, passwords, broker credentials)
     * and strips non-printable control characters.
     */
    fun redactSensitiveData(text: String): String {
        if (text.isBlank()) return text

        var sanitized = text.replace(ANSI_CONTROL_REGEX, "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        sanitized = sanitized.replace(GEMINI_KEY_REGEX, "[REDACTED_API_KEY]")
        sanitized = sanitized.replace(BEARER_TOKEN_REGEX, "Bearer [REDACTED_TOKEN]")
        sanitized = API_KEY_PAIR_REGEX.replace(sanitized) { matchResult ->
            "${matchResult.groupValues[1]}: [REDACTED_KEY]"
        }
        sanitized = PASSWORD_PAIR_REGEX.replace(sanitized) { matchResult ->
            "${matchResult.groupValues[1]}: [REDACTED_PASSWORD]"
        }
        sanitized = ACCOUNT_ID_REGEX.replace(sanitized) { matchResult ->
            "${matchResult.groupValues[1]}: [REDACTED_ACCOUNT]"
        }

        return sanitized.trim()
    }

    /**
     * Deduplicates messages by id and consecutive identical content,
     * while guaranteeing ascending chronological order.
     */
    fun deduplicateAndSort(messages: List<AssistantChatMessage>): List<AssistantChatMessage> {
        if (messages.isEmpty()) return emptyList()

        val sorted = messages.sortedBy { it.timestamp }
        val seenIds = mutableSetOf<String>()
        val result = mutableListOf<AssistantChatMessage>()

        for (msg in sorted) {
            if (msg.id.isNotBlank() && !seenIds.add(msg.id)) {
                continue
            }

            // Deduplicate consecutive identical messages within 2 seconds
            val last = result.lastOrNull()
            if (last != null &&
                last.isUser == msg.isUser &&
                last.text.trim() == msg.text.trim() &&
                kotlin.math.abs(last.timestamp - msg.timestamp) < 2000
            ) {
                continue
            }

            result.add(msg)
        }

        return result
    }

    /**
     * Formats a single message for individual copy/share.
     * Includes clear role label and sanitization.
     */
    fun formatMessageForExport(message: AssistantChatMessage, includeRoleLabel: Boolean = true): String {
        val sanitizedText = redactSensitiveData(message.text)
        if (!includeRoleLabel) {
            return sanitizedText
        }

        val role = if (message.isUser) "User" else "CASH FLOW AI Assistant"
        val metadataBadge = buildString {
            if (!message.associatedSymbol.isNullOrBlank()) {
                append(" [${message.associatedSymbol}")
                if (!message.signalAction.isNullOrBlank()) {
                    append(" | ${message.signalAction}")
                }
                if (message.confidence != null && message.confidence > 0) {
                    append(" (${message.confidence}%)")
                }
                append("]")
            }
        }

        return "$role$metadataBadge:\n$sanitizedText"
    }

    /**
     * Formats the entire conversation in chronological order with clear labels,
     * timestamps, and metadata header/footer.
     * Returns empty string if the conversation is empty.
     */
    fun formatConversationForExport(
        messages: List<AssistantChatMessage>,
        title: String = "CASH FLOW AI — CHAT CONVERSATION EXPORT"
    ): String {
        val cleanList = deduplicateAndSort(messages)
        if (cleanList.isEmpty()) {
            return ""
        }

        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }

        val exportTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date())

        val sb = StringBuilder()
        sb.appendLine("========================================")
        sb.appendLine(title)
        sb.appendLine("Exported: $exportTimestamp")
        sb.appendLine("Total Messages: ${cleanList.size}")
        sb.appendLine("========================================")
        sb.appendLine()

        cleanList.forEachIndexed { index, msg ->
            val formattedTime = try {
                timeFormatter.format(Date(msg.timestamp))
            } catch (e: Exception) {
                ""
            }

            val role = if (msg.isUser) "User" else "CASH FLOW AI Assistant"
            val badge = buildString {
                if (!msg.associatedSymbol.isNullOrBlank()) {
                    append(" [${msg.associatedSymbol}")
                    if (!msg.signalAction.isNullOrBlank()) {
                        append(" | ${msg.signalAction}")
                    }
                    if (msg.confidence != null && msg.confidence > 0) {
                        append(" (${msg.confidence}%)")
                    }
                    append("]")
                }
            }

            val timePrefix = if (formattedTime.isNotBlank()) "[$formattedTime] " else ""
            sb.appendLine("$timePrefix$role$badge:")
            sb.appendLine(redactSensitiveData(msg.text))

            if (index < cleanList.size - 1) {
                sb.appendLine()
            }
        }

        sb.appendLine()
        sb.appendLine("========================================")
        sb.appendLine("END OF CONVERSATION")
        sb.appendLine("========================================")

        return sb.toString().trimEnd()
    }

    /**
     * Copies text to the Android Clipboard with Toast confirmation.
     */
    fun copyTextToClipboard(
        context: Context,
        text: String,
        label: String = "Cash Flow AI Chat",
        feedbackMessage: String = "Copied to clipboard"
    ): Boolean {
        if (text.isBlank()) return false
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, feedbackMessage, Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shares plain text via the native Android Intent.ACTION_SEND chooser.
     */
    fun shareText(
        context: Context,
        text: String,
        subject: String = "CASH FLOW AI Chat Export",
        chooserTitle: String = "Share Conversation"
    ): Boolean {
        if (text.isBlank()) return false
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }
}
