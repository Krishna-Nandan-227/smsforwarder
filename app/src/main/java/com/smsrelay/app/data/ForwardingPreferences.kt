package com.smsrelay.app.data

/** User-controlled rules; an empty sender allowlist means no sender restriction. */
data class ForwardingPreferences(
    val enabled: Boolean = false,
    val allowedSenders: Set<String> = emptySet(),
    val includeFinancialMessages: Boolean = false,
) {
    fun permits(sender: String, category: MessageCategory): Boolean =
        enabled &&
            (allowedSenders.isEmpty() || sender in allowedSenders) &&
            (includeFinancialMessages || category != MessageCategory.FINANCIAL)
}

