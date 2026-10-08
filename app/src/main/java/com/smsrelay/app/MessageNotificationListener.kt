package com.smsrelay.app

import android.app.Notification
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import java.text.DateFormat
import java.util.Date

internal object MessageContent {
    private val sensitive = Regex("\\b(otp|one[ -]?time|verification code|security code|authentication code|password reset)\\b", RegexOption.IGNORE_CASE)
    private val financial = Regex("\\b(bank|transaction|credited|debited|balance|payment|upi|inr|account statement)\\b", RegexOption.IGNORE_CASE)
    private val important = Regex("\\b(urgent|deadline|appointment|meeting|delivery|out for delivery|due|reminder|action required)\\b", RegexOption.IGNORE_CASE)
    fun isSensitive(text: String) = sensitive.containsMatchIn(text) ||
        (Regex("\\bcode\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) && Regex("\\b\\d{4,8}\\b").containsMatchIn(text))
    fun permits(sender: String, body: String, source: String, selected: Set<String>, includeFinancial: Boolean, allowlist: List<String>): Boolean =
        source in selected && !isSensitive("$sender $body") &&
            (includeFinancial || category("$sender $body") != "Financial") &&
            (allowlist.isEmpty() || allowlist.any { it.equals(sender, true) })

    fun category(text: String): String = when {
        financial.containsMatchIn(text) -> "Financial"
        important.containsMatchIn(text) -> "Important"
        else -> "Message"
    }
}

class MessageNotificationListener : NotificationListenerService() {
    private var pet: PhonePet? = null
    override fun onListenerConnected() {
        super.onListenerConnected()
        pet?.close()
        pet = PhonePet(this)
    }
    override fun onListenerDisconnected() {
        pet?.close(); pet = null
        super.onListenerDisconnected()
    }
    override fun onDestroy() {
        pet?.close(); pet = null
        super.onDestroy()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        if (sbn.packageName == packageName || notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 ||
            notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        val app = FirebaseApp.getApps(this).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME } ?: return
        val email = FirebaseAuth.getInstance(app).currentUser?.email ?: return
        val store = WorkspaceStore(applicationContext, email)
        val (sourceKey, source) = NotificationSources.resolve(this, sbn.packageName) ?: return
        if (!store.collectionEnabled || sourceKey !in store.selectedSources) return
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val entries = style?.messages?.mapNotNull { message ->
            val text = message.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) null else Triple(message.person?.name?.toString()
                ?: notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: source,
                text, message.timestamp)
        }.orEmpty().ifEmpty {
            val body = (notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: notification.extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
            if (body.isBlank()) emptyList() else listOf(Triple(
                notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: source,
                body, notification.`when`.takeIf { it > 0 } ?: sbn.postTime))
        }
        val allowlist = store.allowedSenders.split(',').map { it.trim() }.filter { it.isNotBlank() }
        val messages = entries.mapNotNull { (sender, body, sentAt) ->
            val text = "$sender $body"
            if (!MessageContent.permits(sender, body, sourceKey, store.selectedSources, store.includeFinancial, allowlist)) return@mapNotNull null
            val category = MessageContent.category(text)
            val id = MessageDigest.getInstance("SHA-256").digest(
                "${sbn.packageName}|${sbn.key}|$sentAt|$sender|$body".toByteArray())
                .joinToString("") { "%02x".format(it) }
            SmsItem(sender, category, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(sentAt)),
                body, id, source, sbn.packageName, sentAt)
        }
        val added = store.appendMessages(messages)
        if (store.assistantAlerts) AssistantAlerts.publish(this, added)
    }

}
