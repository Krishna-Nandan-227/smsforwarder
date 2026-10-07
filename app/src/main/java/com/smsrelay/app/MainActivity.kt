package com.smsrelay.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily

class MainActivity : ComponentActivity() {
    private var sharedSms by mutableStateOf<SmsItem?>(null)
    private var shareSequence by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        sharedSms = intent.toSharedSms()

        setContent {
            MaterialTheme(colorScheme = SmsRelayColors, typography = SmsRelayTypography) {
                AuthenticationGate { accountEmail, onSignOut ->
                    WorkspaceScreen(
                        sharedSms = sharedSms,
                        shareSequence = shareSequence,
                        onClearSharedSms = {
                            sharedSms = null
                            // Avoid importing the same launch intent again after rotation.
                            setIntent(Intent(this@MainActivity, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
                        },
                        accountEmail = accountEmail,
                        onSignOut = onSignOut,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedSms = intent.toSharedSms()
        shareSequence++
    }
}

private val Ink = Color(0xFF06141B)
private val DeepSlate = Color(0xFF11212D)
private val Slate = Color(0xFF253745)
private val Steel = Color(0xFF4A5C6A)
private val Mist = Color(0xFF9BA8AB)
private val Cloud = Color(0xFFCCD0CF)
private val Paper = Color(0xFFFAFAF7)
private val AppBackground = Color(0xFFF4F3EE)
private val Hairline = Color(0xFFD9DDDB)

private val SmsRelayTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 44.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 38.sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
)

private val SmsRelayColors = lightColorScheme(
    primary = Slate,
    onPrimary = Paper,
    primaryContainer = Mist,
    onPrimaryContainer = Ink,
    secondary = Slate,
    onSecondary = Cloud,
    background = AppBackground,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Cloud,
    onSurfaceVariant = DeepSlate,
    outline = Steel,
    outlineVariant = Hairline,
    error = Color(0xFF823B3B),
)

internal data class SmsItem(
    val sender: String,
    val category: String,
    val receivedAt: String,
    val body: String,
)

private fun Intent.toSharedSms(): SmsItem? {
    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SENDTO) {
        return null
    }

    val sharedText = when (action) {
        Intent.ACTION_SENDTO ->
            data?.getQueryParameter("body") ?: getStringExtra("sms_body")

        else -> getStringExtra(Intent.EXTRA_TEXT)
    }?.trim().orEmpty()

    if (sharedText.isBlank()) return null

    val sharedSubject = getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
    val body = if (sharedSubject.isNotBlank() && !sharedText.contains(sharedSubject)) {
        "$sharedSubject\n$sharedText"
    } else {
        sharedText
    }

    return SmsItem(
        sender = data?.schemeSpecificPart
            ?.substringBefore('?')
            ?.takeIf { it.isNotBlank() }
            ?: "Shared message",
        category = "User shared",
        receivedAt = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT,
        ).format(java.util.Date()),
        body = body,
    )
}


@Composable
internal fun LoadingScreen() {
    val animation = rememberInfiniteTransition(label = "loading-mark")

    val firstScale by animation.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-dot-one",
    )

    val secondScale by animation.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, delayMillis = 160),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-dot-two",
    )

    val thirdScale by animation.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, delayMillis = 320),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-dot-three",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .size(width = 72.dp, height = 56.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                        bottomStart = 6.dp,
                    )
                )
                .background(Slate),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(firstScale, secondScale, thirdScale).forEach { scale ->
                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(8.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                        )
                        .clip(CircleShape)
                        .background(Cloud)
                )
            }
        }
    }
}

@Composable
internal fun InboxScreen(
    messages: List<SmsItem>,
    forwardingEnabled: Boolean,
    onEmailShare: (SmsItem) -> Unit,
    search: String,
    onSearchChange: (String) -> Unit,
    hasImports: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(20.dp),
    ) {
        Text(
            text = "Your messages",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (forwardingEnabled) "Ready to share" else "Sharing is paused",
            style = MaterialTheme.typography.bodyMedium,
            color = Steel,
        )
        Text(
            text = if (hasImports) "Imported messages and sample previews" else "Sample inbox · Share SMS text into this app to import it",
            style = MaterialTheme.typography.bodyMedium,
            color = Steel,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = search, onValueChange = onSearchChange,
            placeholder = { Text("Search messages or senders") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (messages.isEmpty()) {
            Text("No messages match your search or rules.", color = Steel)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(messages) { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Hairline),
                    colors = CardDefaults.cardColors(containerColor = Paper),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = message.sender,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink,
                            )
                            Surface(shape = RoundedCornerShape(8.dp), color = AppBackground) {
                                Text(
                                    text = message.category,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepSlate,
                                )
                            }
                        }
                        Text(
                            text = message.receivedAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = Steel,
                        )
                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { onEmailShare(message) },
                            enabled = forwardingEnabled,
                        ) {
                            Text("Share message")
                        }
                    }
                }
            }
        }
    }
}
