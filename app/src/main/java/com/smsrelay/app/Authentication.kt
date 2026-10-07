package com.smsrelay.app

import android.util.Patterns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
internal fun AuthenticationGate(content: @Composable (String, () -> Unit) -> Unit) {
    val context = LocalContext.current
    val auth = remember {
        // A missing configuration must not crash the app or bypass sign-in.
        (FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(context))
            ?.let { FirebaseAuth.getInstance(it) }
    }
    var user by remember { mutableStateOf(auth?.currentUser) }
    var starting by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val credentials = remember { CredentialManager.create(context) }
    var signingOut by remember { mutableStateOf(false) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
    }
    LaunchedEffect(Unit) {
        delay(900)
        starting = false
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            starting || signingOut -> LoadingScreen()
            user != null -> content(user?.email ?: "Google account") {
                scope.launch {
                    signingOut = true
                    auth?.signOut()
                    try {
                        credentials.clearCredentialState(ClearCredentialStateRequest())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Firebase is signed out even if the account picker cannot be reset.
                    } finally {
                        signingOut = false
                    }
                }
            }
            else -> AuthenticationScreen(auth, credentials)
        }
    }
}

@Composable
private fun AuthenticationScreen(auth: FirebaseAuth?, credentials: CredentialManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var registering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords are never persisted in saved state or stored by the app.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    val clientId = remember {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) "" else context.getString(id)
    }

    fun changeMode() {
        registering = !registering
        password = ""
        confirmation = ""
        visible = false
        error = null
        notice = null
    }
    fun runAction(action: suspend () -> Unit) {
        if (busy) return
        error = null
        notice = null
        if (auth == null) {
            error = "Account sign-in is not available yet. Please try again later."
            return
        }
        busy = true
        scope.launch {
            try {
                action()
            } catch (_: GetCredentialCancellationException) {
                // Closing the Google picker simply returns to the form.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("SmsRelayAuth", "Authentication request failed", e)
                error = authenticationError(e)
            } finally {
                busy = false
            }
        }
    }
    BackHandler(enabled = registering && !busy) { changeMode() }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.size(64.dp, 50.dp).clip(
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 5.dp)
            ).background(colors.primary),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) {
                Box(Modifier.padding(horizontal = 3.dp).size(7.dp)
                    .clip(RoundedCornerShape(50)).background(colors.onPrimary))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("SMSForwarder", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Text(if (registering) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(if (registering) "Start managing your messages in one place."
            else "Sign in to continue to your messages.",
            style = MaterialTheme.typography.bodyMedium, color = colors.secondary)
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.background),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.4f)),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        runAction {
                            check(clientId.isNotBlank()) { "Google sign-in is not configured." }
                            val request = GetCredentialRequest.Builder().addCredentialOption(
                                GetSignInWithGoogleOption.Builder(clientId).build()
                            ).build()
                            val credential = credentials.getCredential(context, request).credential
                            check(credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                            auth!!.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = !busy && auth != null && clientId.isNotBlank(),
                ) { Text("Continue with Google") }
                if (clientId.isBlank()) {
                    Text(
                        "Google sign-in is currently unavailable. Use email below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = colors.outline.copy(alpha = 0.3f))
                    Text("  or use email  ", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
                    HorizontalDivider(Modifier.weight(1f), color = colors.outline.copy(alpha = 0.3f))
                }
                OutlinedTextField(
                    value = email, onValueChange = { email = it; error = null },
                    label = { Text("Email address") }, singleLine = true, enabled = !busy,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it; error = null },
                    label = { Text("Password") }, singleLine = true, enabled = !busy,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
                        imeAction = if (registering) ImeAction.Next else ImeAction.Done),
                    trailingIcon = { TextButton(onClick = { visible = !visible }, enabled = !busy) {
                        Text(if (visible) "Hide" else "Show")
                    } },
                )
                if (registering) {
                    OutlinedTextField(
                        value = confirmation, onValueChange = { confirmation = it; error = null },
                        label = { Text("Confirm password") }, singleLine = true, enabled = !busy,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    )
                    Text("Use at least 6 characters.", style = MaterialTheme.typography.bodySmall, color = colors.secondary)
                } else {
                    TextButton(onClick = {
                        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            error = "Enter your email address first."
                        } else runAction {
                            auth!!.sendPasswordResetEmail(email.trim()).await()
                            notice = "If an account exists for this email, you will receive a reset link."
                        }
                    }, enabled = !busy, modifier = Modifier.align(Alignment.End)) { Text("Forgot password?") }
                }
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error,
                    modifier = Modifier.fillMaxWidth()) }
                notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.secondary) }
                Button(
                    onClick = {
                        val emailValue = email.trim()
                        error = when {
                            !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches() -> "Enter a valid email address."
                            password.isEmpty() -> "Enter your password."
                            registering && password.length < 6 -> "Use a password with at least 6 characters."
                            registering && password != confirmation -> "The passwords do not match."
                            else -> null
                        }
                        if (error == null) runAction {
                            if (registering) auth!!.createUserWithEmailAndPassword(emailValue, password).await()
                            else auth!!.signInWithEmailAndPassword(emailValue, password).await()
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Please wait…")
                    } else Text(if (registering) "Create account" else "Sign in")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(if (registering) "Already have an account?" else "New to SMSForwarder?",
            style = MaterialTheme.typography.bodyMedium, color = colors.secondary)
        TextButton(onClick = { changeMode() }, enabled = !busy) {
            Text(if (registering) "Sign in" else "Create an account")
        }
    }
}

private fun authenticationError(error: Exception): String {
    // Firebase sometimes returns setup errors as a general FirebaseException.
    val detail = error.message.orEmpty()
    if (detail.contains("CONFIGURATION_NOT_FOUND")) {
        return "Account sign-in has not been enabled for this app yet."
    }
    if (detail.contains("API_KEY_INVALID") || detail.contains("API_KEY_SERVICE_BLOCKED")) {
        return "The account service configuration needs updating. Please try again later."
    }
    return when (error) {
    is FirebaseNetworkException -> "Unable to connect. Check your internet connection and try again."
    is FirebaseTooManyRequestsException -> "Too many attempts. Please wait and try again."
    is NoCredentialException -> "No Google account is available. Add one to your device or use email."
    is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "This email already has an account. Sign in instead."
        "ERROR_WEAK_PASSWORD" -> "Choose a stronger password."
        "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
        "ERROR_USER_DISABLED" -> "This account has been disabled."
        "ERROR_OPERATION_NOT_ALLOWED" -> "This sign-in method is currently unavailable."
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "Use the sign-in method originally used for this email."
        "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "The email or password is incorrect."
        else -> "Unable to sign in. Please try again."
    }
    else -> "Unable to complete sign-in. Please try again."
    }
}
