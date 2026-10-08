package com.smsrelay.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat

@Composable
internal fun PetControls() {
    val context = LocalContext.current
    val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: return
    val store = remember(email) { WorkspaceStore(context, email) }
    var enabled by remember { mutableStateOf(store.petEnabled) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(store) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "pet_enabled") enabled = store.petEnabled
        }
        store.listen(listener)
        onDispose { store.stopListening(listener) }
    }
    fun enable() {
        if (!Settings.canDrawOverlays(context)) { error = "Allow display over other apps, then tap Show pet again."; return }
        if (context.getSystemService(android.app.ActivityManager::class.java).isLowRamDevice &&
            android.os.Build.VERSION.SDK_INT <= 29) {
            error = "This device does not support notification-listener collection."; return
        }
        if (context.packageName !in NotificationManagerCompat.getEnabledListenerPackages(context)) {
            error = "Enable notification access in Sources first."; return
        }
        store.petEnabled = true; enabled = true; error = null
        NotificationListenerService.requestRebind(ComponentName(context, MessageNotificationListener::class.java))
    }
    val overlay = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { enable() }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            if (enabled) { store.petEnabled = false; enabled = false }
            else if (Settings.canDrawOverlays(context)) enable()
            else try {
                overlay.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)))
            } catch (_: android.content.ActivityNotFoundException) { error = "Overlay settings are unavailable." }
        }) { Text(if (enabled) "Hide phone pet" else "Show phone pet") }
    }
    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
}
