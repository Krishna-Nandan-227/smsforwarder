package com.smsrelay.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class WorkspaceTab { Inbox, Accounts, Rules, Profile }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkspaceScreen(
    sharedSms: SmsItem?, shareSequence: Int, onClearSharedSms: () -> Unit,
    accountEmail: String, onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(accountEmail) { WorkspaceStore(context.applicationContext, accountEmail) }
    var accounts by remember(store) { mutableStateOf(store.accounts()) }
    var imports by remember(store) { mutableStateOf(store.messages()) }
    var sharingEnabled by remember(store) { mutableStateOf(store.sharingEnabled) }
    var includeFinancial by remember(store) { mutableStateOf(store.includeFinancial) }
    var allowedSenders by remember(store) { mutableStateOf(store.allowedSenders) }
    var profileName by remember(store) { mutableStateOf(store.profileName) }
    var aboutMe by remember(store) { mutableStateOf(store.aboutMe) }
    var tab by rememberSaveable { mutableStateOf(WorkspaceTab.Inbox) }
    var search by rememberSaveable { mutableStateOf("") }
    var sharingMessage by remember { mutableStateOf<SmsItem?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    BackHandler(enabled = tab != WorkspaceTab.Inbox && sharingMessage == null) { tab = WorkspaceTab.Inbox }
    val sampleMessages = remember {
        listOf(
            SmsItem("ParcelDesk", "Delivery", "Sample · 10:42 AM", "Your parcel is out for delivery. Tracking: PD48291."),
            SmsItem("City Clinic", "Appointment", "Sample · 9:15 AM", "Reminder: appointment tomorrow at 11:30 AM."),
            SmsItem("Bank ABC", "Financial", "Sample · Yesterday", "A transaction alert would appear here. Financial messages are hidden by default."),
        )
    }
    LaunchedEffect(sharedSms, shareSequence) {
        if (sharedSms != null) {
            imports = listOf(sharedSms) + imports
            store.saveMessages(imports)
            onClearSharedSms()
            search = ""
            tab = WorkspaceTab.Inbox
        }
    }
    val senderAllowlist = allowedSenders.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val visibleMessages = (imports + sampleMessages).filter { item ->
        (includeFinancial || item.category != "Financial") &&
            (senderAllowlist.isEmpty() || senderAllowlist.any { it.equals(item.sender, ignoreCase = true) }) &&
            (search.isBlank() || listOf(item.sender, item.body, item.category).any { it.contains(search, ignoreCase = true) })
    }

    fun openEmail(item: SmsItem, account: ShareAccount) {
        if (!sharingEnabled) return
        val subject = "SMS from ${item.sender}"
        val body = "Sender: ${item.sender}\nCategory: ${item.category}\nReceived: ${item.receivedAt}\n\n${item.body}"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.fromParts("mailto", account.email, null).buildUpon()
                .appendQueryParameter("subject", subject).appendQueryParameter("body", body).build()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(account.email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        sharingMessage = null
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar("Install an email app to share messages.") }
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(title = { Text("SMSForwarder", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background))
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                HorizontalDivider(color = colors.outlineVariant)
                NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                    WorkspaceTab.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = tab == destination, onClick = { tab = destination },
                            icon = { Icon(when (destination) {
                                WorkspaceTab.Inbox -> Icons.Outlined.Home
                                WorkspaceTab.Accounts -> Icons.Outlined.Email
                                WorkspaceTab.Rules -> Icons.Outlined.Settings
                                WorkspaceTab.Profile -> Icons.Outlined.AccountCircle
                            }, contentDescription = null) },
                            label = { Text(destination.name, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = colors.background,
                                selectedIconColor = colors.primary, selectedTextColor = colors.primary),
                        )
                    }
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            WorkspaceTab.Inbox -> InboxScreen(visibleMessages, sharingEnabled,
                onEmailShare = { sharingMessage = it }, search = search, onSearchChange = { search = it },
                hasImports = imports.isNotEmpty(), modifier = modifier)
            WorkspaceTab.Accounts -> AccountsScreen(accounts, onSave = { updated ->
                accounts = updated
                store.saveAccounts(updated)
            }, modifier = modifier)
            WorkspaceTab.Rules -> RulesScreen(sharingEnabled, { sharingEnabled = it; store.sharingEnabled = it },
                includeFinancial, { includeFinancial = it; store.includeFinancial = it }, allowedSenders,
                { allowedSenders = it; store.allowedSenders = it }, modifier)
            WorkspaceTab.Profile -> ProfileScreen(accountEmail, profileName, aboutMe, accounts.size, imports.size,
                onSave = { name, bio -> profileName = name; aboutMe = bio; store.profileName = name; store.aboutMe = bio },
                onSignOut = onSignOut, modifier = modifier)
        }
    }

    sharingMessage?.let { item ->
        AlertDialog(
            onDismissRequest = { sharingMessage = null },
            title = { Text("Share message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose an email destination. Your email app will open a draft for you to send.")
                    if (accounts.isEmpty()) Text("Add an email destination in Accounts first.")
                    LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accounts, key = { it.id }) { account ->
                            OutlinedButton(onClick = { openEmail(item, account) }, modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(account.name, style = MaterialTheme.typography.titleMedium)
                                    Text(account.email, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sharingMessage = null; tab = WorkspaceTab.Accounts }) {
                Text("Manage accounts")
            } },
            dismissButton = { TextButton(onClick = { sharingMessage = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WorkspaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun AccountsScreen(accounts: List<ShareAccount>, onSave: (List<ShareAccount>) -> Unit, modifier: Modifier) {
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShareAccount?>(null) }
    var removing by remember { mutableStateOf<ShareAccount?>(null) }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Your accounts", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Save the email addresses where you want to share messages.", color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(20.dp))
            Button(onClick = { editing = null; showEditor = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp)) { Text("Add email account") }
        }
        if (accounts.isEmpty()) item {
            WorkspaceCard {
                Icon(Icons.Outlined.Email, null, modifier = Modifier.size(32.dp))
                Text("A home for your updates", style = MaterialTheme.typography.titleMedium)
                Text("Add a personal, work, or family email address. Then choose it when sharing from Inbox.")
            }
        }
        items(accounts, key = { it.id }) { account ->
            WorkspaceCard {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(account.email, style = MaterialTheme.typography.bodyMedium)
                Text("Email destination", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editing = account; showEditor = true }) { Text("Edit") }
                    TextButton(onClick = { removing = account }) { Text("Remove") }
                }
            }
        }
        item {
            WorkspaceCard {
                Text("More places, later", style = MaterialTheme.typography.titleMedium)
                Text("Google Chat and WhatsApp connections are planned. Email opens a draft in your email app.",
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
    if (showEditor) AccountEditor(editing, accounts, onDismiss = { showEditor = false }, onSave = { account ->
        onSave(if (editing == null) accounts + account else accounts.map { if (it.id == account.id) account else it })
        showEditor = false
    })
    removing?.let { account ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("Remove account?") },
            text = { Text("Remove ${account.name} from your sharing destinations?") },
            confirmButton = { TextButton(onClick = { onSave(accounts.filter { it.id != account.id }); removing = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } })
    }
}

@Composable
private fun AccountEditor(account: ShareAccount?, accounts: List<ShareAccount>, onDismiss: () -> Unit, onSave: (ShareAccount) -> Unit) {
    var name by remember { mutableStateOf(account?.name.orEmpty()) }
    var email by remember { mutableStateOf(account?.email.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (account == null) "Add email account" else "Edit account") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This is a sharing destination. No email password is needed.")
                OutlinedTextField(name, { name = it; error = null }, label = { Text("Name, e.g. Work") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it; error = null }, label = { Text("Email address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = {
            val address = email.trim()
            error = when {
                name.isBlank() -> "Give this account a name."
                !Patterns.EMAIL_ADDRESS.matcher(address).matches() -> "Enter a valid email address."
                accounts.any { it.id != account?.id && it.email.equals(address, true) } -> "This email is already saved."
                else -> null
            }
            if (error == null) onSave(ShareAccount(account?.id ?: java.util.UUID.randomUUID().toString(), name.trim(), address))
        }) { Text("Save account") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun RulesScreen(sharingEnabled: Boolean, onSharingChange: (Boolean) -> Unit,
    includeFinancial: Boolean, onFinancialChange: (Boolean) -> Unit,
    allowedSenders: String, onSendersChange: (String) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Your rules", style = MaterialTheme.typography.headlineSmall)
        Text("Choose what appears in your inbox and when sharing is available.", color = MaterialTheme.colorScheme.secondary)
        RuleToggle("Allow message sharing", "Turn off to pause sharing from the inbox.", sharingEnabled, onSharingChange)
        RuleToggle("Show financial messages", "Messages labeled Financial are hidden by default.", includeFinancial, onFinancialChange)
        WorkspaceCard {
            Text("Specific senders", style = MaterialTheme.typography.titleMedium)
            Text("Leave blank for all senders. Separate exact sender names or phone numbers with commas.")
            OutlinedTextField(allowedSenders, onSendersChange, label = { Text("Allowed senders") },
                placeholder = { Text("ParcelDesk, City Clinic") }, modifier = Modifier.fillMaxWidth())
            Text("Changes are saved automatically.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary)
        }
        Text("Rules use each message's label. Imported texts are labeled User shared. Live SMS collection and automatic forwarding are not connected yet.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun RuleToggle(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    WorkspaceCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Switch(checked, onChange)
        }
    }
}

@Composable
private fun ProfileScreen(email: String, name: String, bio: String, accounts: Int, messages: Int,
    onSave: (String, String) -> Unit, onSignOut: () -> Unit, modifier: Modifier) {
    var editing by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val displayName = name.ifBlank { email.substringBefore('@') }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Your profile", style = MaterialTheme.typography.headlineSmall)
        WorkspaceCard {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge)
                }
            }
            Text(displayName, style = MaterialTheme.typography.titleLarge)
            Text(email, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
            Text(bio.ifBlank { "Add a few words about yourself." }, color = MaterialTheme.colorScheme.secondary)
            OutlinedButton(onClick = { editing = true }) { Text("Edit profile") }
        }
        WorkspaceCard {
            Text("Your workspace", style = MaterialTheme.typography.titleMedium)
            Text("$accounts saved email destinations")
            Text("$messages imported messages")
            Text("Your destinations, profile, rules, and imported messages are saved on this device.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        WorkspaceCard {
            Text("About SMSForwarder", style = MaterialTheme.typography.titleMedium)
            Text("Keep important updates together and share them with the people or accounts you choose.")
            Text("Version 0.1.0 · Email sharing", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary)
        }
        OutlinedButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
    if (editing) ProfileEditor(name, bio, onDismiss = { editing = false }, onSave = { updatedName, updatedBio ->
        onSave(updatedName, updatedBio); editing = false
    })
    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false }, title = { Text("Sign out?") },
        text = { Text("Your saved workspace will be here when you sign in again on this device.") },
        confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sign out") } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } })
}

@Composable
private fun ProfileEditor(name: String, bio: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var draftName by remember { mutableStateOf(name) }
    var draftBio by remember { mutableStateOf(bio) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit profile") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draftName, { draftName = it.take(80) }, label = { Text("Display name") }, singleLine = true)
            OutlinedTextField(draftBio, { draftBio = it.take(300) }, label = { Text("About me") }, minLines = 3, maxLines = 5)
        }
    }, confirmButton = { TextButton(onClick = { onSave(draftName.trim(), draftBio.trim()) }) { Text("Save profile") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
