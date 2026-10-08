package com.smsrelay.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun SourcesScreen(selected: Set<String>, onChange: (Set<String>) -> Unit,
    assistantAlerts: Boolean, onAlertsChange: (Boolean) -> Unit,
    onDestinations: () -> Unit, onClear: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var access by remember { mutableStateOf(NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onAlertsChange(granted)
        if (!granted) error = "Assistant notifications are off. Collected previews will still appear in the app."
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) access = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your sources", style = MaterialTheme.typography.headlineSmall)
        Text("Choose the apps your assistant can collect notification previews from.")
        WorkspaceCard {
            Text(if (access) "Notification access enabled" else "Connect notification access", style = MaterialTheme.typography.titleMedium)
            Text("Enable SMSForwarder in Android's notification access settings. Collection only uses the sources you select below.")
            OutlinedButton(onClick = {
                try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                catch (_: ActivityNotFoundException) { error = "Notification access settings are unavailable on this device." }
            }) { Text(if (access) "Manage access" else "Enable access") }
        }
        NotificationSources.known.forEach { source ->
            RuleToggle(source.key, source.detail, source.key in selected) { enabled ->
                onChange(if (enabled) selected + source.key else selected - source.key)
            }
        }
        var showOtherApps by remember { mutableStateOf(false) }
        var appSearch by remember { mutableStateOf("") }
        val otherApps = remember(context) { NotificationSources.otherApps(context) }
        OutlinedButton(onClick = { showOtherApps = !showOtherApps }) {
            Text(if (showOtherApps) "Hide other apps" else "Add another messaging app")
        }
        if (showOtherApps) {
            Text("Choose an installed app. Its visible notifications will be collected, including updates that may not be messages.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(appSearch, { appSearch = it }, label = { Text("Find an app") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            val matches = otherApps.filter { appSearch.isBlank() || it.second.contains(appSearch, true) }
            if (matches.isEmpty()) Text("No matching installed apps.")
            matches.forEach { (key, label) ->
                RuleToggle(label, "Notification previews from this app.", key in selected) { enabled ->
                    onChange(if (enabled) selected + key else selected - key)
                }
            }
        }
        RuleToggle("Assistant notifications", "Show a discreet alert when a new preview is collected.", assistantAlerts) { enabled ->
            if (!enabled) onAlertsChange(false)
            else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else onAlertsChange(true)
        }
        Text("New notifications are collected while you are signed in. Hidden previews, old histories, and notifications that were never shown cannot be recovered. Recognized verification-code previews are skipped.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        WorkspaceCard {
            Text("Share your messages", style = MaterialTheme.typography.titleMedium)
            Text("Your saved email destinations are still available for sharing a draft from Inbox.")
            TextButton(onClick = onDestinations) { Text("Manage email destinations") }
        }
        OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear collected messages") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("Clear this inbox?") },
        text = { Text("This removes collected previews and imported messages from this device. Messages in the original apps are kept.") },
        confirmButton = { TextButton(onClick = { onClear(); confirmClear = false }) { Text("Clear messages") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } })
}
