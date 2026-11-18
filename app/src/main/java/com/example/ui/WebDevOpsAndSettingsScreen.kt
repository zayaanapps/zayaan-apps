package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WebDevOpsAndSettingsScreen(
    darkMode: Boolean,
    notifyUpdates: Boolean,
    currentUserEmail: String?,
    onDarkModeChange: (Boolean) -> Unit,
    onNotifyUpdatesChange: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenUrl: (Context, String) -> Unit,
    onSignOut: () -> Unit,
    onShowCopiedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedBlueprintTab by remember { mutableStateOf("Overview & Architecture") }

    val blueprintTabs = listOf(
        "Overview & Architecture",
        "GitHub Actions (.github/workflows/deploy.yml)",
        "Flutter Shared Code (flutter_zayaan_apps/lib/main.dart)",
        "Distributed App Webhook Sync",
        "About & Contact"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("web_devops_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Website & GitHub Repository Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Zayaan Apps Web & GitHub DevOps Hub",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "https://zayaanapps.github.io/zayaan-apps/",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Both your Android APK and responsive Flutter Web store share the exact same Cloud Firestore 'apps' collection. All Flutter/Dart files (flutter_zayaan_apps/) and GitHub Actions workflows (.github/workflows/deploy.yml) are generated in this workspace.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onOpenUrl(context, "https://zayaanapps.github.io/zayaan-apps/")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Web Store")
                        }

                        OutlinedButton(
                            onClick = {
                                onOpenUrl(context, "https://github.com/zayaanapps/zayaan-apps")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GitHub Repo")
                        }
                    }
                }
            }
        }

        // 2. App Settings (Theme, Notifications, Account)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Store Settings & Preferences",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Dark Theme (Material 3)", style = MaterialTheme.typography.labelLarge)
                                Text("Syncs preference to Cloud Firestore", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = onDarkModeChange,
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Available Update Notifications", style = MaterialTheme.typography.labelLarge)
                                Text("Notify when installed Zayaan apps have newer versionCode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = notifyUpdates,
                            onCheckedChange = { enabled ->
                                onNotifyUpdatesChange(enabled)
                                if (enabled) onRequestNotificationPermission()
                            },
                            modifier = Modifier.testTag("notify_updates_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Account Session", style = MaterialTheme.typography.labelLarge)
                            Text(currentUserEmail ?: "Authenticated Google Account", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        // 3. Interactive Blueprint & Code Inspector Selector
        item {
            Column {
                Text(
                    text = "Flutter + Web + GitHub Actions Blueprints",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    blueprintTabs.forEach { tab ->
                        FilterChip(
                            selected = selectedBlueprintTab == tab,
                            onClick = { selectedBlueprintTab = tab },
                            label = { Text(tab) }
                        )
                    }
                }
            }
        }

        // 4. Selected Blueprint Content
        item {
            val contentText = when (selectedBlueprintTab) {
                "Overview & Architecture" -> OVERVIEW_BLUEPRINT_TEXT
                "GitHub Actions (.github/workflows/deploy.yml)" -> DEPLOY_YML_PREVIEW_TEXT
                "Flutter Shared Code (flutter_zayaan_apps/lib/main.dart)" -> FLUTTER_MAIN_PREVIEW_TEXT
                "Distributed App Webhook Sync" -> DISTRIBUTED_SYNC_PREVIEW_TEXT
                else -> ABOUT_AND_CONTACT_TEXT
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedBlueprintTab,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText(selectedBlueprintTab, contentText))
                                onShowCopiedMessage("Copied '$selectedBlueprintTab' to clipboard!")
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = contentText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private val OVERVIEW_BLUEPRINT_TEXT = """
ZAYAAN APPS — DUAL-PRODUCT ARCHITECTURE SUMMARY

1. Why Both Native Android & Shared Flutter/Dart Are Included:
   • Google AI Studio's live streaming Android container compiles and previews Gradle/Kotlin APKs directly.
   • Therefore, you get an immediately runnable, installable Android APK right here in AI Studio (`com.zayaanapps.store`), PLUS the complete shared Flutter/Dart Android + Web project in `/flutter_zayaan_apps/` and `.github/workflows/deploy.yml` ready for your GitHub repository (`zayaanapps/zayaan-apps`).

2. Project Naming & URLs:
   • Application Name: Zayaan Apps
   • Android Package ID: com.zayaanapps.store
   • GitHub Repository: zayaanapps/zayaan-apps
   • GitHub Pages Website: https://zayaanapps.github.io/zayaan-apps/
   • Preferred Firebase Project ID: zayaan-apps (Note: Firebase project IDs must be globally unique and lowercase across all of Google Cloud; in AI Studio's sandbox, your provisioned project is wired automatically via google-services.json).

3. Free Tier Compliance (Firebase Spark + GitHub Free):
   • Cloud Firestore: Uses free daily read/write quota with real-time snapshot listeners.
   • Firebase Authentication: Google Sign-In & Email/Password are 100% free on the Spark plan.
   • APK Hosting & Bandwidth: Hosted free on GitHub Releases (avoids paid Firebase Storage bandwidth).
   • Website Hosting: Hosted free on GitHub Pages with `--base-href /zayaan-apps/` and 404.html SPA routing fallback.
   • FCM Push Notifications Note: Sending automated server-to-device FCM notifications via Cloud Functions requires the Firebase Blaze plan; as a 100% free Spark alternative, Zayaan Apps uses real-time Firestore versionCode comparison + local Android notification channels on app startup/check!
""".trimIndent()

private val DEPLOY_YML_PREVIEW_TEXT = """
# File: .github/workflows/deploy.yml (Also generated in workspace root)
name: Zayaan Apps — Build Android APK & Deploy Flutter Web

on:
  push:
    branches: [ main ]
  workflow_dispatch:

permissions:
  contents: write
  pages: write
  id-token: write

jobs:
  build-android-and-web:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: flutter_zayaan_apps
    steps:
      - name: Checkout zayaanapps/zayaan-apps
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Install Flutter SDK
        uses: subosito/flutter-action@v2
        with:
          channel: 'stable'
          cache: true

      - name: Resolve Dependencies & Run Static Analysis
        run: |
          flutter pub get
          flutter analyze

      - name: Build Android Release APK
        run: flutter build apk --release

      - name: Publish Release APK to GitHub Releases (Non-Destructive Tag)
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v1.0.${'$'}{{ github.run_number }}
          name: "Zayaan Apps Store v1.0.${'$'}{{ github.run_number }}"
          files: flutter_zayaan_apps/build/app/outputs/flutter-apk/app-release.apk
          generate_release_notes: true

      - name: Build Responsive Flutter Website for GitHub Pages
        run: |
          flutter build web --release --base-href "/zayaan-apps/"
          cp build/web/index.html build/web/404.html

      - name: Upload GitHub Pages Artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: flutter_zayaan_apps/build/web

  deploy-website:
    needs: build-android-and-web
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${'$'}{{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to https://zayaanapps.github.io/zayaan-apps/
        id: deployment
        uses: actions/deploy-pages@v4
""".trimIndent()

private val FLUTTER_MAIN_PREVIEW_TEXT = """
// Complete shared Flutter Android + Web codebase located in /flutter_zayaan_apps/
// Key architecture files generated in workspace:
// • flutter_zayaan_apps/pubspec.yaml
// • flutter_zayaan_apps/lib/main.dart
// • flutter_zayaan_apps/lib/models/store_app.dart
// • flutter_zayaan_apps/lib/services/firestore_service.dart
// • flutter_zayaan_apps/lib/services/installer_service.dart (Conditional export: Android vs Web)
// • flutter_zayaan_apps/lib/services/installer_service_android.dart
// • flutter_zayaan_apps/lib/services/installer_service_web.dart
// • flutter_zayaan_apps/lib/screens/store_shell_screen.dart
// • flutter_zayaan_apps/lib/screens/admin_panel_screen.dart
// • flutter_zayaan_apps/lib/screens/about_contact_screen.dart
// • flutter_zayaan_apps/web/index.html
""".trimIndent()

private val DISTRIBUTED_SYNC_PREVIEW_TEXT = """
CENTRALIZED APP DISTRIBUTION WITHOUT REBUILDING ZAYAAN APPS

Each individual app (e.g., zayaanapps/zayaan-calculator, zayaanapps/zayaan-notes) can have its own GitHub repository.
When you push a release tag in an individual app's repo, its GitHub Actions workflow builds the APK, uploads it to GitHub Releases, and patches the central Zayaan Apps Firestore `apps/{appId}` document via REST API—automatically updating both the Zayaan Apps Android Store and https://zayaanapps.github.io/zayaan-apps/ with zero rebuild of the store!

See `.github/workflows/publish-distributed-app-template.yml` in the workspace for the complete reusable workflow.
""".trimIndent()

private val ABOUT_AND_CONTACT_TEXT = """
ABOUT ZAYAAN APPS
Zayaan Apps is a complete digital application store and centralized APK distribution ecosystem built for Android smartphones, tablets, and desktop web browsers.

• Official Website: https://zayaanapps.github.io/zayaan-apps/
• GitHub Organization / Account: https://github.com/zayaanapps
• Repository: https://github.com/zayaanapps/zayaan-apps
• Administrator & Support Contact: zayaanapps@gmail.com
""".trimIndent()
