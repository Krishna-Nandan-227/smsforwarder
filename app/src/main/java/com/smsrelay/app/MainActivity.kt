package com.smsrelay.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
                    SmsRelayApp(
                        sharedSms = sharedSms,
                        shareSequence = shareSequence,
                        onClearSharedSms = { sharedSms = null },
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

private data class SmsItem(
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
        receivedAt = "Just now",
        body = body,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsRelayApp(
    sharedSms: SmsItem?,
    shareSequence: Int,
    onClearSharedSms: () -> Unit,
    accountEmail: String,
    onSignOut: () -> Unit,
) {
    var forwardingEnabled by remember { mutableStateOf(false) }
    var includeFinancial by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Inbox") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val messages = remember {
        listOf(
            SmsItem(
                "ParcelDesk",
                "Delivery",
                "Today · 10:42 AM",
                "Your parcel is out for delivery. Tracking: PD48291.",
            ),
            SmsItem(
                "City Clinic",
                "Appointment",
                "Today · 9:15 AM",
                "Reminder: appointment tomorrow at 11:30 AM.",
            ),
            SmsItem(
                "Bank ABC",
                "Financial",
                "Yesterday · 7:06 PM",
                "A transaction alert would appear here. Financial forwarding is off by default.",
            ),
        )
    }


    LaunchedEffect(shareSequence) {
        if (shareSequence > 0) selectedTab = "Inbox"
    }

    run {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "SMSForwarder",
                            color = Ink,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppBackground,
                        titleContentColor = Ink,
                    ),
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (selectedTab == "Inbox") {
                        Button(
                            onClick = { selectedTab = "Inbox" },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Inbox")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedTab = "Inbox" },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Inbox")
                        }
                    }

                    if (selectedTab == "Settings") {
                        Button(
                            onClick = { selectedTab = "Settings" },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Settings")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedTab = "Settings" },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Settings")
                        }
                    }
                }
            },
        ) { padding ->
            if (selectedTab == "Inbox") {
                InboxScreen(
                    messages = if (sharedSms == null) {
                        messages
                    } else {
                        listOf(sharedSms) + messages
                    },
                    forwardingEnabled = forwardingEnabled,
                    onEmailShare = { item ->
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "SMS from ${item.sender}",
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${item.sender} · ${item.receivedAt}\n\n${item.body}",
                            )
                        }

                        try {
                            context.startActivity(emailIntent)
                        } catch (_: android.content.ActivityNotFoundException) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "No email app is available on this device."
                                )
                            }
                        }

                        onClearSharedSms()
                    },
                    modifier = Modifier.padding(padding),
                )
            } else {
                SettingsScreen(
                    accountEmail = accountEmail,
                    onSignOut = onSignOut,
                    forwardingEnabled = forwardingEnabled,
                    onForwardingChange = { forwardingEnabled = it },
                    includeFinancial = includeFinancial,
                    onFinancialChange = { includeFinancial = it },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
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
private fun InboxScreen(
    messages: List<SmsItem>,
    forwardingEnabled: Boolean,
    onEmailShare: (SmsItem) -> Unit,
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
            text = if (forwardingEnabled) "Forwarding is on" else "Forwarding is paused",
            style = MaterialTheme.typography.bodyMedium,
            color = Steel,
        )
        Text(
            text = "Sample inbox",
            style = MaterialTheme.typography.bodyMedium,
            color = Steel,
        )
        Spacer(Modifier.height(16.dp))

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
                        ) {
                            Text("Share by email", color = Slate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    accountEmail: String,
    onSignOut: () -> Unit,
    forwardingEnabled: Boolean,
    onForwardingChange: (Boolean) -> Unit,
    includeFinancial: Boolean,
    onFinancialChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink,
        )

        Text(accountEmail, style = MaterialTheme.typography.bodyMedium, color = Steel)
        OutlinedButton(onClick = onSignOut) { Text("Sign out") }

        PreferenceRow(
            title = "Forward selected messages",
            detail = "Collection is not connected yet",
            checked = forwardingEnabled,
            onCheckedChange = onForwardingChange,
        )

        PreferenceRow(
            title = "Include financial SMS",
            detail = "Off by default; enable only if you want these forwarded",
            checked = includeFinancial,
            onCheckedChange = onFinancialChange,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Hairline),
            colors = CardDefaults.cardColors(containerColor = Paper),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Delivery",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Text(
                    text = "Email sharing is the first delivery option. Google Chat and WhatsApp can be added as separate channels.",
                    color = DeepSlate,
                )
            }
        }

        Text(
            text = "Sample previews only. Live SMS access and delivery are not connected yet.",
            style = MaterialTheme.typography.bodySmall,
            color = Steel,
        )
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Hairline),
        colors = CardDefaults.cardColors(containerColor = Paper),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepSlate,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
