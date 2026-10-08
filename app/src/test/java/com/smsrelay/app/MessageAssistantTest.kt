package com.smsrelay.app

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class MessageAssistantTest {
    private val now = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 8, 12, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private fun message(sender: String = "Rahul", source: String = "WhatsApp", body: String = "Meeting today at 3 pm.",
        at: Long = now - 60_000, id: String = body) = SmsItem(sender, MessageContent.category(body), "Today", body, id, source, capturedAt = at)

    @Test fun briefIsGroundedAndFiltersSourceAndSender() {
        val wanted = message()
        val result = MessageAssistant.answer("Summarize WhatsApp messages from Rahul today",
            listOf(wanted, message(sender = "Ravi", body = "Dinner at 7."),
                message(source = "Email", body = "Invoice received.")), now)
        assertEquals(listOf(wanted), result.sources)
        assertTrue(result.text.contains("Meeting today at 3 pm."))
        assertFalse(result.text.contains("Invoice"))
    }
    @Test fun todayDoesNotIncludeYesterdayOrUndatedImports() {
        val result = MessageAssistant.answer("Summarize today", listOf(message(),
            message(body = "Old message", at = now - 86_400_000), message(body = "Undated", at = 0)), now)
        assertEquals(1, result.sources.size)
    }
    @Test fun verificationCodesNeverEnterReplies() {
        val result = MessageAssistant.answer("Read messages", listOf(message(body = "Your OTP is 123456.")), now)
        assertTrue(result.sources.isEmpty())
        assertFalse(result.text.contains("123456"))
    }
    @Test fun noMessagesDoesNotInventUpdates() {
        val result = MessageAssistant.answer("What is new today?", emptyList(), now)
        assertTrue(result.sources.isEmpty())
        assertTrue(result.text.contains("no collected messages"))
    }
    @Test fun importantUsesLabelsAndKeepsSourceReferences() {
        val reminder = message(body = "Appointment tomorrow at 10.")
        val result = MessageAssistant.answer("Show important messages",
            listOf(reminder, message(body = "Hello there")), now)
        assertEquals(listOf(reminder), result.sources)
    }
    @Test fun longBriefIsBoundedAndReportsRemainingCount() {
        val messages = (1..9).map { message(body = "Message $it " + "x".repeat(400), id = "$it") }
        val result = MessageAssistant.answer("Brief messages", messages, now)
        assertEquals(5, result.sources.size)
        assertTrue(result.text.contains("4 more"))
        assertTrue(result.text.length < 1600)
    }
    @Test fun unsupportedQuestionsExplainCurrentCapabilities() {
        val result = MessageAssistant.answer("Tell me a joke", listOf(message()), now)
        assertTrue(result.sources.isEmpty())
        assertTrue(result.text.contains("collected messages"))
    }
}
