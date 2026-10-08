package com.smsrelay.app

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.UUID

internal data class ChatEntry(val id: String = UUID.randomUUID().toString(), val fromUser: Boolean, val text: String, val sources: List<SmsItem> = emptyList())

internal class AssistantSession {
    var history by mutableStateOf(listOf(ChatEntry(fromUser = false,
        text = "Hello. I can brief collected messages and read them aloud. Try “Summarize today” or “Show important messages”.")))
    var question by mutableStateOf("")
    var readReplies by mutableStateOf(true)
}

@Composable
internal fun AssistantScreen(messages: List<SmsItem>, spokenAlerts: Boolean, session: AssistantSession, onSources: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var history by session::history
    var question by session::question
    var readReplies by session::readReplies
    var status by remember { mutableStateOf<String?>(null) }
    var selectedMessage by remember { mutableStateOf<SmsItem?>(null) }
    var voiceReady by remember { mutableStateOf(false) }
    var voice by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { result -> voiceReady = result == TextToSpeech.SUCCESS }
        voice = engine
        onDispose { engine.stop(); engine.shutdown(); voice = null }
    }
    DisposableEffect(lifecycle, voice) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice?.stop() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    fun ask(text: String) {
        if (text.isBlank()) return
        val reply = MessageAssistant.answer(text, messages)
        history = (history + ChatEntry(fromUser = true, text = text) +
            ChatEntry(fromUser = false, text = reply.text, sources = reply.sources)).takeLast(40)
        question = ""
        status = null
        if (readReplies && voiceReady) voice?.speak(reply.text.take(3500), TextToSpeech.QUEUE_FLUSH, null, "reply")
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { ask(it) }
        } else status = "Voice input was cancelled. You can type your question."
    }
    fun startVoice() {
        voice?.stop()
        try {
            speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about your messages")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            })
        } catch (_: ActivityNotFoundException) {
            status = "Speech recognition is not available on this device. Type your question instead."
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoice() else status = "Microphone access was denied. You can still type your questions."
    }
    var knownIds by remember { mutableStateOf(messages.map { it.id }.toSet()) }
    LaunchedEffect(messages) {
        val fresh = messages.filter { it.id !in knownIds }
        knownIds = messages.map { it.id }.toSet()
        if (fresh.isNotEmpty() && spokenAlerts && voiceReady &&
            lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            val alert = "You have ${fresh.size} new message previews. Ask me for a brief."
            history = (history + ChatEntry(fromUser = false, text = alert)).takeLast(40)
            voice?.speak(alert, TextToSpeech.QUEUE_ADD, null, "new-messages")
        }
    }
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your message assistant", style = MaterialTheme.typography.headlineSmall)
        PetControls()
        Text("${messages.size} available previews · On-device brief", style = MaterialTheme.typography.bodySmall, color = colors.secondary)
        if (messages.isEmpty()) OutlinedButton(onClick = onSources) { Text("Connect your sources") }
        Text("Briefs use short extracts from captured text. This version does not use a cloud AI model.",
            style = MaterialTheme.typography.bodySmall, color = colors.secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { ask("Summarize today") }, modifier = Modifier.weight(1f)) { Text("Today") }
            OutlinedButton(onClick = { ask("Show important messages") }, modifier = Modifier.weight(1f)) { Text("Important") }
        }
        LazyColumn(modifier = Modifier.weight(1f), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(history.asReversed(), key = { it.id }) { entry ->
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (entry.fromUser) colors.primary else colors.surface),
                    border = if (entry.fromUser) null else BorderStroke(1.dp, colors.outlineVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (entry.fromUser) "You" else "Assistant", style = MaterialTheme.typography.labelSmall,
                            color = if (entry.fromUser) colors.onPrimary else colors.secondary)
                        Text(entry.text, color = if (entry.fromUser) colors.onPrimary else colors.onSurface)
                        entry.sources.forEach { source ->
                            TextButton(onClick = { selectedMessage = source }) {
                                Text("View " + source.source + " · " + source.sender)
                            }
                        }
                    }
                }
            }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.secondary) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(question, { question = it }, placeholder = { Text("Ask about your messages") },
                modifier = Modifier.weight(1f), maxLines = 3, shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { ask(question) }))
            Button(onClick = { ask(question) }, enabled = question.isNotBlank()) { Text("Ask") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    startVoice() else permission.launch(Manifest.permission.RECORD_AUDIO)
            }) { Text("Tap to talk") }
            TextButton(onClick = { voice?.stop() }) { Text("Stop voice") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Checkbox(checked = readReplies, onCheckedChange = { readReplies = it; if (!it) voice?.stop() })
            Text("Read answers aloud", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
        }
        if (!voiceReady) Text("Spoken replies need an available text-to-speech engine.", style = MaterialTheme.typography.bodySmall)
    }
    selectedMessage?.let { source ->
        AlertDialog(onDismissRequest = { selectedMessage = null },
            title = { Text(source.sender) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(source.source + " · " + source.receivedAt, style = MaterialTheme.typography.labelMedium)
                    Text(source.body)
                }
            },
            confirmButton = { TextButton(onClick = { selectedMessage = null }) { Text("Close") } },
            dismissButton = {
                if (source.sourcePackage.isNotBlank()) TextButton(onClick = {
                    val launch = context.packageManager.getLaunchIntentForPackage(source.sourcePackage)
                    if (launch == null) status = "The source app is not available."
                    else try { context.startActivity(launch) }
                    catch (_: ActivityNotFoundException) { status = "Unable to open the source app." }
                    selectedMessage = null
                }) { Text("Open source app") }
            })
    }

}
