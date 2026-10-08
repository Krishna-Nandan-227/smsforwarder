package com.smsrelay.app

import android.content.Context
import android.content.Intent
import android.provider.Telephony

internal data class MessageSource(val key: String, val packages: Set<String>, val detail: String)
internal object NotificationSources {
    val known = listOf(
        MessageSource("SMS", setOf("com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms", "com.android.messaging"), "Messaging app notification previews."),
        MessageSource("WhatsApp", setOf("com.whatsapp", "com.whatsapp.w4b"), "WhatsApp and WhatsApp Business previews."),
        MessageSource("Email", setOf("com.google.android.gm", "com.microsoft.office.outlook"), "Gmail and Outlook previews."),
        MessageSource("Instagram", setOf("com.instagram.android", "com.instagram.lite"), "Instagram notifications, including available message previews."),
        MessageSource("Discord", setOf("com.discord"), "Discord direct-message and channel previews."),
        MessageSource("Teams", setOf("com.microsoft.teams"), "Microsoft Teams notification previews."),
        MessageSource("Telegram", setOf("org.telegram.messenger", "org.telegram.messenger.web"), "Telegram message previews."),
        MessageSource("Signal", setOf("org.thoughtcrime.securesms"), "Signal previews when enabled in Signal settings."),
        MessageSource("Messenger", setOf("com.facebook.orca", "com.facebook.mlite"), "Messenger notification previews."),
        MessageSource("Slack", setOf("com.Slack"), "Slack message and channel previews."),
        MessageSource("Snapchat", setOf("com.snapchat.android"), "Available Snapchat notification text.")
    )
    fun resolve(context: Context, pkg: String): Pair<String, String>? {
        known.firstOrNull { pkg in it.packages }?.let { return it.key to it.key }
        if (pkg == Telephony.Sms.getDefaultSmsPackage(context)) return "SMS" to "SMS"
        val label = try { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }
            catch (_: android.content.pm.PackageManager.NameNotFoundException) { pkg }
        return "app:$pkg" to label
    }
    fun otherApps(context: Context): List<Pair<String, String>> {
        val knownPackages = known.flatMap { it.packages }.toSet()
        return context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != context.packageName && it.activityInfo.packageName !in knownPackages }
            .map { "app:" + it.activityInfo.packageName to it.loadLabel(context.packageManager).toString() }
            .distinctBy { it.first }.sortedBy { it.second.lowercase() }
    }
}
