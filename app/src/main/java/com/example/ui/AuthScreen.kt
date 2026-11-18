package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.security.MessageDigest

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun resolveDefaultWebClientId(context: Context): String? {
    val pkgResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val fallbackResId = if (pkgResId != 0) pkgResId else context.resources.getIdentifier("default_web_client_id", "string", "com.example")
    if (fallbackResId == 0) return null
    return try {
        context.getString(fallbackResId).takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

private fun getAppSigningSha1(context: Context): String {
    return try {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return "unknown"
        val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
        digest.joinToString(":") { "%02X".format(it) }
    } catch (_: Exception) {
        "unknown"
    }
}

private fun formatCredentialDiagnostic(context: Context, e: GetCredentialException): String {
    val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: e.message ?: e.type
    val sha1 = getAppSigningSha1(context)
    return "$detail\n(${e.type} • ${context.packageName} • SHA1: $sha1)"
}

private suspend fun exchangeCredentialWithFirebase(
    cred: Credential,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        withTimeout(20_000L) {
            Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        }
        onAuthSuccess()
    } else {
        onAuthError("Unexpected credential type returned")
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
): Job {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return Job().apply { complete() }
    }
    val clientId = resolveDefaultWebClientId(context) ?: run {
        onUnauthenticated()
        return Job().apply { complete() }
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    val activityOrContext = context.findActivity() ?: context
    return scope.launch {
        try {
            val cred = withTimeout(12_000L) {
                credentialManager.getCredential(activityOrContext, request).credential
            }
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                withTimeout(15_000L) {
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                }
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: TimeoutCancellationException) {
            Log.w("Auth", "Auto sign-in timed out: ${e.message}")
            onUnauthenticated()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: (String?) -> Unit = {},
    previousJob: Job? = null
): Job {
    val clientId = resolveDefaultWebClientId(context) ?: run {
        onAuthError("Google Sign-In configuration missing: add app/google-services.json and rebuild.")
        return Job().apply { complete() }
    }
    val activity = context.findActivity() ?: run {
        onAuthError("Activity context required for interactive Google Sign-In")
        return Job().apply { complete() }
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    return scope.launch {
        previousJob?.cancelAndJoin()
        try {
            val cred = withTimeout(45_000L) {
                credentialManager.getCredential(activity, primaryRequest).credential
            }
            exchangeCredentialWithFirebase(cred, onAuthSuccess, onAuthError)
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled(formatCredentialDiagnostic(context, e))
        } catch (e: TimeoutCancellationException) {
            Log.w("Auth", "Primary Google Sign-In timed out, trying bottom-sheet fallback", e)
            runBottomSheetFallback(activity, credentialManager, clientId, onAuthSuccess, onAuthError, onAuthCancelled)
        } catch (e: GetCredentialException) {
            Log.w("Auth", "Primary Google Sign-In failed (${e.type}), trying bottom-sheet fallback", e)
            runBottomSheetFallback(activity, credentialManager, clientId, onAuthSuccess, onAuthError, onAuthCancelled)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

fun onGoogleBottomSheetSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: (String?) -> Unit = {},
    previousJob: Job? = null
): Job {
    val clientId = resolveDefaultWebClientId(context) ?: run {
        onAuthError("Google Sign-In configuration missing: add app/google-services.json and rebuild.")
        return Job().apply { complete() }
    }
    val activity = context.findActivity() ?: run {
        onAuthError("Activity context required for interactive Google Sign-In")
        return Job().apply { complete() }
    }
    return scope.launch {
        previousJob?.cancelAndJoin()
        runBottomSheetFallback(activity, credentialManager, clientId, onAuthSuccess, onAuthError, onAuthCancelled)
    }
}

fun onGoogleBrowserOAuthClicked(
    context: Context,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    previousJob: Job? = null
): Job {
    val activity = context.findActivity() ?: run {
        onAuthError("Activity context required for browser Google Sign-In")
        return Job().apply { complete() }
    }
    return scope.launch {
        previousJob?.cancelAndJoin()
        try {
            val provider = OAuthProvider.newBuilder("google.com")
                .addCustomParameter("prompt", "select_account")
                .build()
            val pending = Firebase.auth.pendingAuthResult
            if (pending != null) {
                pending.await()
            } else {
                Firebase.auth.startActivityForSignInWithProvider(activity, provider).await()
            }
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Browser Google OAuth failed", e)
            onAuthError(e.localizedMessage ?: "Google Web Sign-In failed. Please try again.")
        }
    }
}

private suspend fun runBottomSheetFallback(
    activity: Activity,
    credentialManager: CredentialManager,
    clientId: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: (String?) -> Unit
) {
    try {
        val bottomSheetOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .build()
        val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(bottomSheetOption)
            .build()
        val cred = withTimeout(45_000L) {
            credentialManager.getCredential(activity, fallbackRequest).credential
        }
        exchangeCredentialWithFirebase(cred, onAuthSuccess, onAuthError)
    } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Google bottom-sheet Sign-In cancelled: ${e.message}", e)
        onAuthCancelled(formatCredentialDiagnostic(activity, e))
    } catch (e: TimeoutCancellationException) {
        Log.e("Auth", "Google Sign-In timed out", e)
        onAuthError("Google Sign-In timed out waiting for Play Services. Try 'Sign in via Google Web (Browser)' below.")
    } catch (e: GetCredentialException) {
        Log.e("Auth", "Google bottom-sheet Sign-In failed", e)
        onAuthError(formatCredentialDiagnostic(activity, e))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("Auth", "Google bottom-sheet Sign-In failed", e)
        onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
    }
}

fun signOutFromZayaanStore(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeAuthJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            activeAuthJob?.cancel()
        }
    }

    LaunchedEffect(Unit) {
        val pending = Firebase.auth.pendingAuthResult
        if (pending != null) {
            isLoading = true
            try {
                pending.await()
                isLoading = false
                onAuthSuccess()
                return@LaunchedEffect
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage ?: "Pending Google sign-in failed."
            }
        }
        val prior = activeAuthJob
        activeAuthJob = scope.launch {
            prior?.cancelAndJoin()
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = onAuthSuccess,
                onUnauthenticated = {},
                scope = scope
            ).join()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_store_hero_banner_1795021164708),
                            contentDescription = "Zayaan Apps Store Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "ZAYAAN APPS",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Android APK Store & GitHub Pages Web Hub",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Official Digital Application Store",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sign in with Google to browse the live Firestore catalog, download & install verified GitHub Release APKs, check installed app updates, and access the Admin Dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    FeatureBulletRow(
                        icon = Icons.Default.Verified,
                        title = "Android APK Installer & Update Checker",
                        subtitle = "Direct GitHub Releases download with versionCode comparison"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureBulletRow(
                        icon = Icons.Default.CloudSync,
                        title = "Shared Cloud Firestore + Flutter Web",
                        subtitle = "Real-time sync with zayaanapps.github.io/zayaan-apps/"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureBulletRow(
                        icon = Icons.Default.Security,
                        title = "Zero-Trust Admin & CI/CD Hub",
                        subtitle = "Publish apps & inspect complete Flutter/GitHub Actions code"
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val prior = activeAuthJob
                            errorMessage = null
                            isLoading = true
                            activeAuthJob = onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = { diagnostic ->
                                    isLoading = false
                                    errorMessage = buildString {
                                        append("Google Sign-In was cancelled before permission completed. Try 'Quick Sign-In' or 'Google Web (Browser)' below.")
                                        if (!diagnostic.isNullOrBlank()) {
                                            append("\nDetails: ")
                                            append(diagnostic)
                                        }
                                    }
                                },
                                previousJob = prior
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val prior = activeAuthJob
                            errorMessage = null
                            isLoading = true
                            activeAuthJob = onGoogleBottomSheetSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = { diagnostic ->
                                    isLoading = false
                                    errorMessage = buildString {
                                        append("Google bottom-sheet sign-in was dismissed. Try 'Sign in via Google Web (Browser)' below.")
                                        if (!diagnostic.isNullOrBlank()) {
                                            append("\nDetails: ")
                                            append(diagnostic)
                                        }
                                    }
                                },
                                previousJob = prior
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_quick_sheet_sign_in_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Quick Sign-In (Google Bottom Sheet)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val prior = activeAuthJob
                            errorMessage = null
                            isLoading = true
                            activeAuthJob = onGoogleBrowserOAuthClicked(
                                context = context,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                previousJob = prior
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_browser_oauth_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Sign in via Google Web (Browser)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                activeAuthJob?.cancel()
                                activeAuthJob = null
                                isLoading = false
                                errorMessage = "Sign-in cancelled. Try 'Quick Sign-In' or 'Sign in via Google Web (Browser)'."
                            },
                            modifier = Modifier.testTag("cancel_google_sign_in_button")
                        ) {
                            Text("Cancel & Reset")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureBulletRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
