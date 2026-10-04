package com.smsrelay.app.delivery

import com.smsrelay.app.data.RelayMessage

/** Delivery destinations stay independent from SMS collection and filtering. */
interface DeliveryChannel {
    val id: String
    val displayName: String

    suspend fun deliver(message: RelayMessage): DeliveryResult
}

sealed interface DeliveryResult {
    data object Sent : DeliveryResult
    data class NeedsUserAction(val explanation: String) : DeliveryResult
    data class Failed(val explanation: String, val retryable: Boolean) : DeliveryResult
}

/** First MVP path: ask Android to open a mail app with a prefilled draft. */
class EmailShareChannel : DeliveryChannel {
    override val id: String = "email_share"
    override val displayName: String = "Email"

    override suspend fun deliver(message: RelayMessage): DeliveryResult =
        DeliveryResult.NeedsUserAction("Open an email draft for ${message.sender} and let the user send it.")
}

