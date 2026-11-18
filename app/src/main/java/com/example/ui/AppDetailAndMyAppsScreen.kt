package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DownloadState
import com.example.data.InstalledAppStatus
import com.example.data.StoreApp

@Composable
fun AppDetailScreen(
    app: StoreApp,
    installedStatus: InstalledAppStatus?,
    downloadState: DownloadState,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onStartDownload: (StoreApp) -> Unit,
    onInstallApk: (Context, String) -> Unit,
    onOpenInstalledApp: (Context, String) -> Unit,
    onOpenBrowserDownload: (Context, StoreApp) -> Unit,
    onToggleBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val appId = app.appId.ifBlank { app.docId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("app_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Store"
                        )
                    }
                    Text(
                        text = "Application Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                IconButton(
                    onClick = { onToggleBookmark(appId) },
                    modifier = Modifier.testTag("bookmark_app_button")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark App",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // App Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppEmblemAvatar(app = app, sizeDp = 76)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = app.developerName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatMetricItem(label = "VERSION", value = "v${app.versionName} (${app.versionCode})")
                        StatMetricItem(label = "APK SIZE", value = app.apkFileSize)
                        StatMetricItem(label = "CATEGORY", value = app.category)
                        StatMetricItem(label = "DOWNLOADS", value = "${app.downloadCount}")
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Installed Version Comparison Banner
                    if (installedStatus?.installed == true) {
                        Surface(
                            color = if (installedStatus.updateAvailable) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (installedStatus.updateAvailable) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (installedStatus.updateAvailable) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (installedStatus.updateAvailable) {
                                            "Update Available on Device"
                                        } else {
                                            "Installed & Up to Date on Device"
                                        },
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "Installed: v${installedStatus.installedVersionName} (code ${installedStatus.installedVersionCode}) • Store: v${app.versionName} (code ${app.versionCode})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Download & Package Installer Action Area
                    when (downloadState) {
                        is DownloadState.Downloading -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Downloading APK from GitHub Releases...",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${downloadState.progressPercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { downloadState.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                )
                            }
                        }

                        is DownloadState.ReadyToInstall -> {
                            Button(
                                onClick = { onInstallApk(context, downloadState.apkFilePath) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("install_downloaded_apk_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Launch Android Package Installer")
                            }
                        }

                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onStartDownload(app) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("download_apk_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = if (installedStatus?.updateAvailable == true) {
                                            Icons.Default.SystemUpdate
                                        } else {
                                            Icons.Default.CloudDownload
                                        },
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            installedStatus?.updateAvailable == true -> "Update APK (v${app.versionName})"
                                            installedStatus?.installed == true -> "Re-download APK"
                                            else -> "Download & Install APK"
                                        }
                                    )
                                }

                                if (installedStatus?.installed == true) {
                                    FilledTonalButton(
                                        onClick = { onOpenInstalledApp(context, app.packageName) },
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("open_installed_app_button"),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { onOpenBrowserDownload(context, app) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("browser_download_apk_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Directly via GitHub Releases Browser")
                    }
                }
            }
        }

        // Release Notes / What's New
        if (app.releaseNotes.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "What's New in v${app.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = app.releaseNotes,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        // Full Description & Metadata
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "About this application",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = app.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    MetadataKeyVal("Application ID", appId)
                    MetadataKeyVal("Package Name", app.packageName)
                    MetadataKeyVal("Version Code", app.versionCode.toString())
                    MetadataKeyVal("GitHub APK Asset URL", app.apkDownloadUrl)
                    MetadataKeyVal("Android Security Policy", "Standard Android Package Installer with user confirmation")
                }
            }
        }
    }
}

@Composable
private fun StatMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetadataKeyVal(key: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = key,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MyAppsScreen(
    appsState: UiState<List<StoreApp>>,
    installedStatuses: Map<String, InstalledAppStatus>,
    bookmarkedIds: List<String>,
    onRefreshScan: () -> Unit,
    onAppClick: (StoreApp) -> Unit,
    onUpdateOrDownloadClick: (StoreApp) -> Unit,
    onOpenInstalledApp: (Context, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_apps_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Android Package & Update Scanner",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Detects installed Zayaan packages via Android PackageManager and compares local versionCode with Cloud Firestore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = onRefreshScan,
                        modifier = Modifier.testTag("scan_updates_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check")
                    }
                }
            }
        }

        if (appsState is UiState.Success) {
            val allApps = appsState.data
            val installedApps = allApps.filter { installedStatuses[it.packageName]?.installed == true }
            val updatableApps = installedApps.filter { installedStatuses[it.packageName]?.updateAvailable == true }
            val bookmarkedApps = allApps.filter { (it.appId.ifBlank { it.docId }) in bookmarkedIds }

            // 1. Available Updates Section
            item {
                Text(
                    text = "Available Updates (${updatableApps.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (updatableApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "All detected packages are up to date!",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            } else {
                items(
                    items = updatableApps,
                    key = { "update_${it.appId.ifBlank { it.docId }}" }
                ) { app ->
                    val status = installedStatuses[app.packageName]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppEmblemAvatar(app = app, sizeDp = 50)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Installed: v${status?.installedVersionName} (${status?.installedVersionCode}) → New: v${app.versionName} (${app.versionCode})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(
                                    onClick = { onUpdateOrDownloadClick(app) },
                                    modifier = Modifier.testTag("update_btn_${app.appId}")
                                ) {
                                    Text("Update")
                                }
                            }
                            if (app.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Release Notes: ${app.releaseNotes}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Installed Applications on Device
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Installed on This Device (${installedApps.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(
                items = installedApps,
                key = { "installed_${it.appId.ifBlank { it.docId }}" }
            ) { app ->
                val status = installedStatuses[app.packageName]
                CatalogAppRowCard(
                    app = app,
                    installedStatus = status,
                    onClick = { onAppClick(app) }
                )
            }

            // 3. Saved / Bookmarked Applications
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bookmarked Applications (${bookmarkedApps.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (bookmarkedApps.isEmpty()) {
                item {
                    Text(
                        text = "Tap the bookmark icon on any application detail screen to save it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = bookmarkedApps,
                    key = { "bookmarked_${it.appId.ifBlank { it.docId }}" }
                ) { app ->
                    CatalogAppRowCard(
                        app = app,
                        installedStatus = installedStatuses[app.packageName],
                        onClick = { onAppClick(app) }
                    )
                }
            }
        }
    }
}
