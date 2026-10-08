package com.smsrelay.app

import java.util.Calendar

internal data class AssistantReply(val text: String, val sources: List<SmsItem> = emptyList())

/** Deterministic first version: quote available previews, never invent missing content. */
internal object MessageAssistant {
    fun answer(question: String, messages: List<SmsItem>, now: Long = System.currentTimeMillis()): AssistantReply {
        val query = question.lowercase().trim()
        if (query in setOf("hi", "hello", "hey")) return AssistantReply("Hello. Ask me to brief your messages, show important updates, or read messages from a sender.")
        if (!Regex("summary|summari[sz]e|brief|read|message|email|gmail|whatsapp|instagram|discord|teams|telegram|signal|messenger|slack|snapchat|sms|today|yesterday|important|updates|what|show|latest|new").containsMatchIn(query))
            return AssistantReply("I can help with collected messages. Try “Summarize today”, “WhatsApp updates”, or “Messages from Rahul”.")
        val dayStart = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val yesterday = Calendar.getInstance().apply { timeInMillis = dayStart; add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        val source = messages.map { it.source }.distinct().sortedByDescending { it.length }
            .firstOrNull { it != "Imported" && query.contains(it.lowercase()) } ?: when {
            "instagram" in query -> "Instagram"
            "discord" in query -> "Discord"
            "teams" in query -> "Teams"
            "telegram" in query -> "Telegram"
            "signal" in query -> "Signal"
            "messenger" in query -> "Messenger"
            "slack" in query -> "Slack"
            "snapchat" in query -> "Snapchat"
            "whatsapp" in query -> "WhatsApp"
            "email" in query || "gmail" in query -> "Email"
            "sms" in query -> "SMS"
            else -> null
        }
        val sender = Regex("(?:from|by)\\s+(.+?)(?:\\s+(?:today|yesterday)|[?!.]|$)")
            .find(query)?.groupValues?.get(1)?.trim()
        val selected = messages.filter { message ->
            !MessageContent.isSensitive(message.body) &&
                (source == null || message.source == source) &&
                (sender == null || message.sender.contains(sender, true)) &&
                (!query.contains("important") || message.category in setOf("Important", "Financial")) &&
                (!query.contains("today") || message.capturedAt in dayStart..now) &&
                (!query.contains("yesterday") || message.capturedAt >= yesterday && message.capturedAt < dayStart)
        }.sortedByDescending { it.capturedAt }
        if (selected.isEmpty()) return AssistantReply("I have no collected messages matching that request. Enable your sources, check notification access, or change the sender or time filter. I cannot see hidden notification text or old chat history.")
        val limit = if ("read" in query) 3 else 5
        val shown = selected.take(limit)
        val lines = shown.mapIndexed { index, message ->
            val cleaned = message.body.replace(Regex("\\s+"), " ").trim()
            val excerpt = if ("read" in query) cleaned.take(500) else cleaned.take(160)
            "${index + 1}. ${message.source} — ${message.sender}: $excerpt${if (excerpt.length < cleaned.length) "…" else ""}"
        }
        val header = if ("read" in query) "Reading ${shown.size} of ${selected.size} matching message previews."
            else "You have ${selected.size} matching message previews. Here is a short brief:"
        val footer = if (selected.size > shown.size) "\n${selected.size - shown.size} more are in your inbox." else ""
        return AssistantReply(header + "\n\n" + lines.joinToString("\n\n") + footer, shown)
    }
}
