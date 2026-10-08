package com.smsrelay.app

import org.junit.Assert.*
import org.junit.Test

class NotificationFilterTest {
    private fun allowed(sender: String = "Rahul", body: String = "Meeting tomorrow",
        source: String = "WhatsApp", selected: Set<String> = setOf("WhatsApp"),
        finance: Boolean = false, senders: List<String> = emptyList()) =
        MessageContent.permits(sender, body, source, selected, finance, senders)

    @Test fun unselectedAppsAreRejected() {
        assertFalse(allowed(selected = emptySet()))
        assertFalse(allowed(source = "Email"))
        assertTrue(allowed())
    }
    @Test fun financialMessagesRequireOptIn() {
        assertFalse(allowed(body = "Your account was debited INR 500"))
        assertTrue(allowed(body = "Your account was debited INR 500", finance = true))
    }
    @Test fun codesRemainExcludedEvenWithFinancialOptIn() {
        assertFalse(allowed(body = "Your bank verification code is 123456", finance = true))
    }
    @Test fun senderMatchingIsExactAndCaseInsensitive() {
        assertTrue(allowed(senders = listOf("rahul")))
        assertFalse(allowed(sender = "Rahul Kumar", senders = listOf("Rahul")))
    }
    @Test fun duplicateNotificationsAreNotAppendedTwice() {
        val message = SmsItem("Rahul", "Message", "Today", "Hello", "same-id", capturedAt = 1)
        val result = mergeCollectedMessages(listOf(message), listOf(message, message))
        assertEquals(1, result.size)
    }
    @Test fun newlyCollectedMessagesAreOrderedByTimestamp() {
        val older = SmsItem("A", "Message", "Today", "A", "a", capturedAt = 10)
        val newer = SmsItem("B", "Message", "Today", "B", "b", capturedAt = 20)
        assertEquals(listOf(newer, older), mergeCollectedMessages(listOf(older), listOf(newer)))
    }
}
