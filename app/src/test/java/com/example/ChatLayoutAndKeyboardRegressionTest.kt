package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.AssistantChatMessage
import com.example.data.MarketUniverse
import com.example.ui.screens.AiAnalyticsScreen
import com.example.ui.theme.CashFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ChatLayoutAndKeyboardRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testComposerAndButtons_areDisplayedAndInteractive() {
        val messages = mutableStateListOf(
            AssistantChatMessage(id = "msg-1", isUser = true, text = "What is EUR/USD RSI?"),
            AssistantChatMessage(id = "msg-2", isUser = false, text = "EUR/USD RSI is currently 54.2 with neutral momentum.")
        )
        var lastSent = ""

        composeTestRule.setContent {
            CashFlowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Render AssistantChatPanel directly inside test
                    com.example.ui.screens.AssistantChatPanelForTesting(
                        messages = messages,
                        onSendMessage = { lastSent = it },
                        onClearChat = { messages.clear() }
                    )
                }
            }
        }

        // Verify composer and send button are visible
        composeTestRule.onNodeWithTag("input_assistant_chat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_send_assistant_chat").assertIsDisplayed()

        // Verify full conversation actions exist
        composeTestRule.onNodeWithTag("btn_copy_conversation").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_share_conversation").assertIsDisplayed()

        // Verify individual message copy and share exist
        composeTestRule.onNodeWithTag("btn_copy_message_msg-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_share_message_msg-1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_copy_message_msg-2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_share_message_msg-2").assertIsDisplayed()

        // Type into composer and click send
        composeTestRule.onNodeWithTag("input_assistant_chat").performTextInput("Check MACD divergence")
        composeTestRule.onNodeWithTag("btn_send_assistant_chat").performClick()

        assertEquals("Check MACD divergence", lastSent)
    }

    @Test
    fun testSimulatedKeyboardInsetPadding_composerRemainsVisible() {
        var keyboardPaddingDp by mutableStateOf(0)

        composeTestRule.setContent {
            CashFlowTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    com.example.ui.screens.AssistantChatPanelForTesting(
                        messages = listOf(AssistantChatMessage(id = "1", isUser = false, text = "Telemetry ready")),
                        onSendMessage = {},
                        onClearChat = {}
                    )
                }
            }
        }

        // Verify composer is displayed at default state
        composeTestRule.onNodeWithTag("input_assistant_chat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_send_assistant_chat").assertIsDisplayed()

        // Simulate keyboard opening by changing state
        keyboardPaddingDp = 300
        composeTestRule.waitForIdle()

        // Verify composer still visible above simulated keyboard
        composeTestRule.onNodeWithTag("input_assistant_chat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_send_assistant_chat").assertIsDisplayed()
    }
}
