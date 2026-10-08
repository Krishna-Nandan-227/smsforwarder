package com.smsrelay.app

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal fun applyPetVoice(engine: TextToSpeech, store: WorkspaceStore) {
    engine.setSpeechRate(store.petSpeechRate)
    engine.setPitch(store.petPitch)
    val selected = engine.voices?.firstOrNull { it.name == store.petVoiceName &&
        !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() }
    if (selected != null) engine.voice = selected
    else engine.defaultVoice?.let { engine.voice = it }
}

@Composable
internal fun PetVoiceSettings(store: WorkspaceStore) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var voices by remember { mutableStateOf(emptyList<Voice>()) }
    var ready by remember { mutableStateOf(false) }
    var selected by remember(store) { mutableStateOf(store.petVoiceName) }
    var rate by remember(store) { mutableFloatStateOf(store.petSpeechRate) }
    var pitch by remember(store) { mutableFloatStateOf(store.petPitch) }
    var expanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { result ->
            ready = result == TextToSpeech.SUCCESS
            if (ready) {
                voices = tts?.voices.orEmpty().filter { !it.isNetworkConnectionRequired &&
                    "notInstalled" !in it.features.orEmpty() }
                    .sortedWith(compareBy({ it.locale.displayLanguage }, { it.name }))
            } else status = "No text-to-speech engine is available."
        }
        engine = tts
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) tts?.stop() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); tts?.stop(); tts?.shutdown(); engine = null }
    }
    WorkspaceCard {
        Text("A voice for your companion", style = MaterialTheme.typography.titleMedium)
        Text("Choose a downloaded phone voice. Your pet speaks when you ask for a brief.",
            style = MaterialTheme.typography.bodySmall)
        val chosen = voices.firstOrNull { it.name == selected }
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = ready) {
                Text(chosen?.let { voiceLabel(it) } ?: "Device default")
            }
            DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                DropdownMenuItem(text = { Text("Device default") }, onClick = {
                    selected = ""; store.petVoiceName = ""; expanded = false
                })
                voices.forEach { voice ->
                    DropdownMenuItem(text = { Text(voiceLabel(voice)) }, onClick = {
                        selected = voice.name; store.petVoiceName = selected; expanded = false
                    })
                }
            }
        }
        Text("Speed · %.1f×".format(rate))
        Slider(rate, { rate = it; store.petSpeechRate = it }, valueRange = .6f..1.4f)
        Text("Pitch · %.1f×".format(pitch))
        Slider(pitch, { pitch = it; store.petPitch = it }, valueRange = .7f..1.5f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                engine?.let {
                    applyPetVoice(it, store)
                    val result = it.speak("Hello, I'm " + store.petName.ifBlank { "your companion" } +
                        ". Tap me for a brief of your messages.", TextToSpeech.QUEUE_FLUSH, null, "pet-preview")
                    status = if (result == TextToSpeech.ERROR) "Unable to play this voice. Try the device default." else null
                }
            }, enabled = ready) { Text("Preview voice") }
            TextButton(onClick = { engine?.stop() }) { Text("Stop") }
        }
        if (voices.isEmpty() && ready) Text("No downloaded voices found. Using device default.",
            style = MaterialTheme.typography.bodySmall)
        status?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
private fun voiceLabel(voice: Voice) = voice.locale.displayName + " · " + voice.name
