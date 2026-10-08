package com.smsrelay.app

import android.app.KeyguardManager
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import kotlin.math.abs

/** Window belongs to the system-bound notification listener; no always-on microphone. */
internal class PhonePet(private val context: Context) {
    private val windows = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var root: LinearLayout? = null
    private var pet: PetFace? = null
    private var panel: LinearLayout? = null
    private var label: TextView? = null
    private var title: TextView? = null
    private var nudge: TextView? = null
    private var store: WorkspaceStore? = null
    private var email: String? = null
    private var ready = false
    private var voice: TextToSpeech? = null
    private var expanded = false
    private var unseen = 0
    private var knownIds = emptySet<String>()
    private var screenOn = true
    private var summary: TextView? = null
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; x = 16; y = 240 }
    private val changes = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        handler.post {
            if (key == "messages") {
                val ids = eligible().map { it.id }.toSet()
                unseen += (ids - knownIds).size
                knownIds = ids
            }
            summary?.text = ""
            refresh()
        }
    }
    private val auth = FirebaseAuth.getInstance()
    private val authListener = FirebaseAuth.AuthStateListener {
        handler.post {
            val next = it.currentUser?.email
            if (next != email) {
                store?.stopListening(changes)
                hide()
                email = next
                store = next?.let { address -> WorkspaceStore(context, address) }
                store?.listen(changes)
                knownIds = eligible().map { message -> message.id }.toSet()
                unseen = 0
            }
            refresh()
        }
    }
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            screenOn = intent?.action != Intent.ACTION_SCREEN_OFF
            if (!screenOn) { voice?.stop(); hide() } else refresh()
        }
    }
    private val dayTick = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 60_000) }
    }
    init {
        auth.addAuthStateListener(authListener)
        ContextCompatReceiver.register(context, screen)
        handler.post(dayTick)
    }
    private fun dp(n: Int) = (n * context.resources.displayMetrics.density).toInt()
    private fun eligible(): List<SmsItem> {
        val s = store ?: return emptyList()
        val senders = s.allowedSenders.split(',').map { it.trim() }.filter { it.isNotBlank() }
        return s.messages().filter {
            !MessageContent.isSensitive(it.body) &&
                (s.includeFinancial || it.category != "Financial") &&
                (senders.isEmpty() || senders.any { sender -> sender.equals(it.sender, true) })
        }
    }
    private fun today(): List<SmsItem> {
        val now = System.currentTimeMillis()
        val start = Calendar.getInstance().apply {
            timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return eligible().filter { it.capturedAt in start..now }
    }
    private fun refresh() {
        val locked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        if (store?.petEnabled != true || !Settings.canDrawOverlays(context) || !screenOn || locked) {
            hide(); return
        }
        if (root == null) show()
        val count = today().size
        pet?.species = store?.petSpecies ?: "Cat"
        pet?.coat = store?.petColour ?: Color.rgb(247, 193, 140)
        pet?.petName = store?.petName ?: "Milo"
        pet?.badge = unseen
        pet?.invalidate()
        nudge?.text = if (unseen > 0) "$unseen new · Tap for a brief" else "$count today"
        title?.text = store?.petName?.ifBlank { "Your companion" } ?: "Your companion"
        label?.text = "$count collected today" + if (unseen > 0) "\n$unseen new · Want a brief?" else "\nTap Read brief to listen."
    }
    private fun background() = GradientDrawable().apply {
        setColor(Color.rgb(250, 250, 247)); cornerRadius = dp(20).toFloat()
        setStroke(dp(1), Color.rgb(155, 168, 171))
    }
    private fun text(value: String, size: Float = 15f) = TextView(context).apply {
        text = value; textSize = size; setTextColor(Color.rgb(17, 33, 45))
    }
    private fun button(value: String, action: () -> Unit) = Button(context).apply {
        text = value; isAllCaps = false; textSize = 14f
        setTextColor(if (value.startsWith("Read")) Color.WHITE else Color.rgb(37, 55, 69)); background = GradientDrawable().apply {
            setColor(if (value.startsWith("Read")) Color.rgb(37, 55, 69) else Color.rgb(237, 239, 234)); cornerRadius = dp(14).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(6) }
        setOnClickListener { action() }
    }
    private fun show() {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val face = PetFace(context)
        layout.addView(face, LinearLayout.LayoutParams(dp(76), dp(82)))
        nudge = text("", 12f).apply { background = background(); setPadding(dp(8), dp(4), dp(8), dp(4)) }
        layout.addView(nudge)
        val details = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; background = background()
            elevation = dp(8).toFloat()
            setPadding(dp(16), dp(16), dp(16), dp(12))
            visibility = View.GONE
        }
        title = text("", 22f).apply { typeface = Typeface.create("serif", Typeface.NORMAL) }
        details.addView(title)
        details.addView(text("YOUR DAILY COMPANION", 10f).apply { letterSpacing = .12f; setPadding(0, dp(4), 0, dp(12)) })
        label = text("")
        summary = text("Your updates, in one quiet place. Tap below for a brief.", 14f).apply { setLineSpacing(dp(3).toFloat(), 1f); setPadding(0, dp(12), 0, dp(12)) }
        details.addView(label)
        details.addView(ScrollView(context).apply {
            addView(summary)
        }, LinearLayout.LayoutParams(dp(240), dp(150)))
        details.addView(button("Read today's brief") {
            val reply = MessageAssistant.answer("Summarize today", today())
            summary?.text = reply.text
            unseen = 0; refresh()
            if (voice == null) {
                voice = TextToSpeech(context) { result ->
                    ready = result == TextToSpeech.SUCCESS
                    handler.post {
                        if (ready && root != null && screenOn) speak(reply.text)
                        else summary?.append("\nVoice unavailable. Read the brief above.")
                    }
                }
            } else if (ready) speak(reply.text)
        })
        details.addView(button("Stop voice") { voice?.stop() })
        details.addView(button("Close panel") { expanded = false; details.visibility = View.GONE; voice?.stop() })
        details.addView(button("Hide pet") { store?.petEnabled = false; hide() })
        layout.addView(details)
        root = layout; pet = face; panel = details; expanded = false
        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f; var moved = false
        face.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y; touchX = event.rawX; touchY = event.rawY; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX; val dy = event.rawY - touchY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) moved = true
                    if (moved) {
                        val bounds = context.resources.displayMetrics
                        params.x = (startX + dx.toInt()).coerceIn(0, (bounds.widthPixels - layout.width).coerceAtLeast(0))
                        params.y = (startY + dy.toInt()).coerceIn(dp(24), (bounds.heightPixels - layout.height - dp(24)).coerceAtLeast(dp(24)))
                        try { windows.updateViewLayout(layout, params) } catch (_: IllegalArgumentException) { hide() }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        view.performClick()
                        expanded = !expanded; details.visibility = if (expanded) View.VISIBLE else View.GONE
                        // Keep the expanded card within the screen after dragging to an edge.
                        params.x = params.x.coerceAtMost((context.resources.displayMetrics.widthPixels - dp(264)).coerceAtLeast(0))
                        params.y = params.y.coerceAtMost((context.resources.displayMetrics.heightPixels - dp(620)).coerceAtLeast(dp(24)))
                        try { windows.updateViewLayout(layout, params) } catch (_: IllegalArgumentException) { hide() }
                        refresh()
                    }; true
                }
                else -> true
            }
        }
        face.contentDescription = "Message pet. Drag to move. Tap for today's count and brief."
        try { windows.addView(layout, params) }
        catch (_: SecurityException) { root = null; pet = null; panel = null }
        catch (_: WindowManager.BadTokenException) { root = null; pet = null; panel = null }
    }
    private fun speak(value: String) {
        val engine = voice ?: return
        store?.let { applyPetVoice(engine, it) }
        voice?.speak(value.take(3500), TextToSpeech.QUEUE_FLUSH, null, "pet-brief")
    }
    private fun hide() {
        root?.let { try { windows.removeView(it) } catch (_: IllegalArgumentException) {} }
        root = null; pet = null; panel = null; label = null; title = null; summary = null; nudge = null
        voice?.stop()
    }
    fun close() {
        handler.removeCallbacksAndMessages(null)
        auth.removeAuthStateListener(authListener)
        store?.stopListening(changes)
        context.unregisterReceiver(screen)
        hide(); voice?.shutdown(); voice = null
    }
}

private object ContextCompatReceiver {
    fun register(context: Context, receiver: BroadcastReceiver) {
        androidx.core.content.ContextCompat.registerReceiver(context, receiver,
            IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT) },
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}

