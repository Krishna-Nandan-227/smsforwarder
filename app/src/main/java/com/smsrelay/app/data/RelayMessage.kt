package com.smsrelay.app.data

import java.time.Instant

/** A user-approved SMS payload ready for delivery. */
data class RelayMessage(
    val id: String,
    val sender: String,
    val body: String,
    val receivedAt: Instant,
    val category: MessageCategory = MessageCategory.OTHER,
)

enum class MessageCategory {
    FINANCIAL,
    DELIVERY,
    APPOINTMENT,
    OTHER,
}

