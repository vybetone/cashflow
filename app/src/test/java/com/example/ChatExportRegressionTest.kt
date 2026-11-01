package com.example

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssistantChatMessage
import com.example.ui.ChatExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ChatExportRegressionTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun testRedaction_googleApiKey_isRedacted() {
        val raw = "Here is my key: AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q and it works"
        val sanitized = ChatExportHelper.redactSensitiveData(raw)

        assertFalse("Must not contain raw Gemini API key", sanitized.contains("AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q"))
        assertTrue("Must contain [REDACTED_API_KEY]", sanitized.contains("[REDACTED_API_KEY]"))
    }

    @Test
    fun testRedaction_bearerToken_isRedacted() {
        val raw = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz123"
        val sanitized = ChatExportHelper.redactSensitiveData(raw)

        assertFalse("Must not contain raw token", sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz123"))
        assertTrue("Must contain Bearer [REDACTED_TOKEN]", sanitized.contains("Bearer [REDACTED_TOKEN]"))
    }

    @Test
    fun testRedaction_passwordsAndBrokerSecrets_areRedacted() {
        val raw = "Connect with password: SecretMasterPass123! and investor_password: ReadOnlyPass999"
        val sanitized = ChatExportHelper.redactSensitiveData(raw)

        assertFalse("Must not contain SecretMasterPass123!", sanitized.contains("SecretMasterPass123!"))
        assertFalse("Must not contain ReadOnlyPass999", sanitized.contains("ReadOnlyPass999"))
        assertTrue("Must contain [REDACTED_PASSWORD]", sanitized.contains("[REDACTED_PASSWORD]"))
    }

    @Test
    fun testRedaction_brokerAccountNumber_isRedacted() {
        val raw = "Trading account: account_number: 99482710 connected to MT5"
        val sanitized = ChatExportHelper.redactSensitiveData(raw)

        assertFalse("Must not contain raw account number", sanitized.contains("99482710"))
        assertTrue("Must contain [REDACTED_ACCOUNT]", sanitized.contains("[REDACTED_ACCOUNT]"))
    }

    @Test
    fun testDeduplicationAndSorting_outOfOrderMessages_sortedChronologically() {
        val msg1 = AssistantChatMessage(id = "m1", isUser = true, text = "First message", timestamp = 1000L)
        val msg2 = AssistantChatMessage(id = "m2", isUser = false, text = "Second message", timestamp = 2000L)
        val msg3 = AssistantChatMessage(id = "m3", isUser = true, text = "Third message", timestamp = 3000L)

        // Pass out of order
        val result = ChatExportHelper.deduplicateAndSort(listOf(msg3, msg1, msg2))

        assertEquals(3, result.size)
        assertEquals("m1", result[0].id)
        assertEquals("m2", result[1].id)
        assertEquals("m3", result[2].id)
    }

    @Test
    fun testDeduplication_duplicateIds_areFilteredOut() {
        val msg1 = AssistantChatMessage(id = "dup_id", isUser = true, text = "Query", timestamp = 1000L)
        val msg2 = AssistantChatMessage(id = "dup_id", isUser = true, text = "Query", timestamp = 1000L)
        val msg3 = AssistantChatMessage(id = "other_id", isUser = false, text = "Response", timestamp = 2000L)

        val result = ChatExportHelper.deduplicateAndSort(listOf(msg1, msg2, msg3))

        assertEquals(2, result.size)
        assertEquals("dup_id", result[0].id)
        assertEquals("other_id", result[1].id)
    }

    @Test
    fun testFormatMessage_userAndAiAttribution() {
        val userMsg = AssistantChatMessage(isUser = true, text = "Is EUR/USD bullish?")
        val formattedUser = ChatExportHelper.formatMessageForExport(userMsg)
        assertTrue("User message must start with User:", formattedUser.startsWith("User:\n"))
        assertTrue("Contains original text", formattedUser.contains("Is EUR/USD bullish?"))

        val aiMsg = AssistantChatMessage(
            isUser = false,
            text = "EUR/USD is breaking out above resistance.",
            associatedSymbol = "EUR/USD",
            signalAction = "BUY",
            confidence = 88
        )
        val formattedAi = ChatExportHelper.formatMessageForExport(aiMsg)
        assertTrue("AI message must start with AI label", formattedAi.startsWith("CASH FLOW AI Assistant"))
        assertTrue("Includes symbol and confidence badge", formattedAi.contains("[EUR/USD | BUY (88%)]"))
        assertTrue("Contains response text", formattedAi.contains("EUR/USD is breaking out above resistance."))
    }

    @Test
    fun testFormatConversation_emptyList_returnsEmptyString() {
        val formatted = ChatExportHelper.formatConversationForExport(emptyList())
        assertEquals("", formatted)
    }

    @Test
    fun testFormatConversation_fullConversation_hasHeadersAndChronologicalOrder() {
        val messages = listOf(
            AssistantChatMessage(id = "1", isUser = true, text = "Evaluate GBP/USD", timestamp = 100000L),
            AssistantChatMessage(
                id = "2",
                isUser = false,
                text = "Strong support at 1.2900 with API key AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q",
                timestamp = 105000L,
                associatedSymbol = "GBP/USD",
                signalAction = "STRONG_BUY",
                confidence = 92
            )
        )

        val export = ChatExportHelper.formatConversationForExport(messages)

        assertTrue(export.contains("CASH FLOW AI — CHAT CONVERSATION EXPORT"))
        assertTrue(export.contains("Total Messages: 2"))
        assertTrue(export.contains("User:\nEvaluate GBP/USD"))
        assertTrue(export.contains("CASH FLOW AI Assistant [GBP/USD | STRONG_BUY (92%)]:"))
        assertTrue(export.contains("Strong support at 1.2900 with API key [REDACTED_API_KEY]"))
        assertFalse("Must never leak raw Gemini API key in export", export.contains("AIzaSyA1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q"))
        assertTrue(export.contains("END OF CONVERSATION"))
    }

    @Test
    fun testCopyToClipboard_withValidText_copiesAndReturnsTrue() {
        val success = ChatExportHelper.copyTextToClipboard(
            context = context,
            text = "Test chat content to copy",
            label = "Test Label"
        )

        assertTrue(success)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        assertNotNull(clip)
        assertEquals("Test chat content to copy", clip!!.getItemAt(0).text.toString())
    }

    @Test
    fun testCopyToClipboard_withBlankText_returnsFalse() {
        val success = ChatExportHelper.copyTextToClipboard(context, "   ")
        assertFalse(success)
    }

    @Test
    fun testShareText_withValidText_triggersActionSendChooser() {
        val success = ChatExportHelper.shareText(
            context = context,
            text = "Exported trade conversation",
            subject = "Trade Analysis",
            chooserTitle = "Share Conversation"
        )

        assertTrue(success)
        val shadowApp = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
        val nextIntent = shadowApp.nextStartedActivity
        assertNotNull("Must launch chooser intent", nextIntent)
        assertEquals(Intent.ACTION_CHOOSER, nextIntent.action)

        val targetIntent = nextIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent!!.action)
        assertEquals("text/plain", targetIntent.type)
        assertEquals("Exported trade conversation", targetIntent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals("Trade Analysis", targetIntent.getStringExtra(Intent.EXTRA_SUBJECT))
    }

    @Test
    fun testShareText_withBlankText_returnsFalse() {
        val success = ChatExportHelper.shareText(context, "")
        assertFalse(success)
    }
}
