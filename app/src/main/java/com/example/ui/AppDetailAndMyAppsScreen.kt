package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppReview
import com.example.data.AppVersionRecord
import com.example.data.DownloadState
import com.example.data.InstalledPackageStatus
import com.example.data.StoreApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDetailScreen(
    app: StoreApp,
    allApps: List<StoreApp> = emptyList(),
    reviews: List<AppReview> = emptyList(),
    versionHistory: List<AppVersionRecord> = emptyList(),
    currentUserId: String = "",
    isAdmin: Boolean = false,
    installedStatus: InstalledPackageStatus?,
    downloadState: DownloadState,
    isBookmarked: Boolean,
    isFavorite: Boolean = false,
    isInComparison: Boolean = false,
    onBack: () -> Unit,
    onStartDownload: (StoreApp) -> Unit,
    onCancelDownload: (String) -> Unit = {},
    onInstallApk: (Context, String) -> Unit,
    onOpenInstalledApp: (Context, String) -> Unit,
    onOpenBrowserDownload: (Context, StoreApp) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onToggleCompare: (String) -> Unit = {},
    onShareApp: (Context, StoreApp) -> Unit = { _, _ -> },
    onSelectRelatedApp: (StoreApp) -> Unit = {},
    onSubmitReview: (Long, String) -> Unit = { _, _ -> },
    onVoteReviewHelpful: (AppReview) -> Unit = {},
    onFlagReview: (AppReview, Boolean) -> Unit = { _, _ -> },
    onAdminReplyReview: (AppReview, String) -> Unit = { _, _ -> },
    onDeleteReview: (AppReview) -> Unit = {},
    onReportApp: (String, String) -> Unit = { _, _ -> },
    onDownloadLatestGitHubRelease: (Context, StoreApp) -> Unit = { _, target -> onStartDownload(target) },
    onUninstallApp: (Context, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val appId = app.appId.ifBlank { app.docId }
    val showDeveloperProfile = remember { androidx.compose.runtime.mutableStateOf(false) }
    val showReportDialog = remember { androidx.compose.runtime.mutableStateOf(false) }

    val relatedCategoryApps = remember(allApps, app) {
        allApps.filter {
            it.category.equals(app.category, ignoreCase = true) &&
                (it.appId.ifBlank { it.docId }) != appId
        }.take(6)
    }

    val moreFromDeveloperApps = remember(allApps, app) {
        allApps.filter {
            it.developerName.equals(app.developerName, ignoreCase = true) &&
                (it.appId.ifBlank { it.docId }) != appId
        }.take(6)
    }

    val recommendedApps = remember(allApps, app) {
        allApps.filter {
            (it.featured || it.editorsChoice || it.essential) &&
                (it.appId.ifBlank { it.docId }) != appId
        }.take(6)
    }

    val formattedUpdatedDate = remember(app.updatedAtMillis) {
        if (app.updatedAtMillis > 0L) {
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(app.updatedAtMillis))
        } else {
            "Latest GitHub Release"
        }
    }

    val formattedPublishedDate = remember(app.createdAtMillis) {
        if (app.createdAtMillis > 0L) {
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(app.createdAtMillis))
        } else {
            "2025 Release"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Navigation Bar with Back, Compare, Favorite, Share & Wishlist Bookmark
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleCompare(appId) },
                        modifier = Modifier.testTag("detail_compare_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = if (isInComparison) "Remove from Comparison" else "Add to App Comparison",
                            tint = if (isInComparison) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { onToggleFavorite(appId) },
                        modifier = Modifier.testTag("detail_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { onShareApp(context, app) },
                        modifier = Modifier.testTag("detail_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share application link"
                        )
                    }
                    IconButton(
                        onClick = { onToggleBookmark(appId) },
                        modifier = Modifier.testTag("detail_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove from Wishlist" else "Add to Wishlist",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. App Identity Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val detailIconModel: Any = if (
                            app.packageName == "com.zayaanapps.store" &&
                            (app.iconUrl.isBlank() || app.iconUrl.contains("/web/icons/Icon-192.png"))
                        ) {
                            R.drawable.img_launcher_icon_1795021165006
                        } else {
                            app.iconUrl
                        }
                        AsyncImage(
                            model = detailIconModel,
                            contentDescription = "${app.appName} icon",
                            placeholder = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                            error = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                if (app.editorsChoice) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Editor's Choice",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = app.developerName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = app.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                if (app.editorsChoice) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = "Editor's Choice",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (app.offlineCapable) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OfflineBolt,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Offline Ready",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (app.shortDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = app.shortDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Compact Single-Line Telemetry Strip (Version, Size, Downloads, Rating with Icons)
                    val ratingSummaryText = if (reviews.isNotEmpty() || app.reviewCount > 0) {
                        val avg = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else app.averageRating
                        val count = maxOf(reviews.size.toLong(), app.reviewCount)
                        "${String.format(Locale.US, "%.1f", avg)} ($count)"
                    } else {
                        "Verified"
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactTelemetryItem(
                                icon = Icons.Default.NewReleases,
                                value = "v${app.versionName}",
                                contentDescription = "Version",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            VerticalDivider(
                                modifier = Modifier.height(14.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                            CompactTelemetryItem(
                                icon = Icons.Default.SdStorage,
                                value = app.apkFileSize,
                                contentDescription = "APK Size",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            VerticalDivider(
                                modifier = Modifier.height(14.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                            CompactTelemetryItem(
                                icon = Icons.Default.Download,
                                value = "${app.downloadCount}",
                                contentDescription = "Downloads",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            VerticalDivider(
                                modifier = Modifier.height(14.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                            CompactTelemetryItem(
                                icon = Icons.Default.Star,
                                value = ratingSummaryText,
                                contentDescription = "Rating",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Version Comparison Status Banner if Installed
                    if (installedStatus != null && installedStatus.isInstalled) {
                        Surface(
                            color = if (installedStatus.updateAvailable) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (installedStatus.updateAvailable) {
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (installedStatus.updateAvailable) {
                                        Icons.Default.SystemUpdate
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = "Installed status",
                                    tint = if (installedStatus.updateAvailable) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (installedStatus.updateAvailable) {
                                            "Update Available: Installed v${installedStatus.installedVersionName} (${installedStatus.installedVersionCode}) → Store v${app.versionName} (${app.versionCode})"
                                        } else {
                                            "Up to Date: Installed v${installedStatus.installedVersionName} (code ${installedStatus.installedVersionCode})"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Package Action Area: Install only (if not installed) or Uninstall + Open/Update (if installed)
                    val isAppInstalled = installedStatus?.isInstalled == true
                    val hasAppUpdate = installedStatus?.updateAvailable == true

                    when (downloadState) {
                        is DownloadState.Downloading -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Downloading APK (${downloadState.downloadedBytes / 1024} KB)...",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${downloadState.progressPercent}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = { onCancelDownload(appId) }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel Download",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { downloadState.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }

                        is DownloadState.Paused -> {
                            if (isAppInstalled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onUninstallApp(context, app.packageName) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("uninstall_app_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Uninstall",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Uninstall")
                                    }
                                    Button(
                                        onClick = { onStartDownload(app) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("resume_apk_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = "Resume",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Resume (${downloadState.progressPercent}%)")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onStartDownload(app) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("resume_apk_button")
                                ) {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Resume Install (${downloadState.progressPercent}%)")
                                }
                            }
                        }

                        is DownloadState.Failed -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Download failed: ${downloadState.errorMessage}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                if (isAppInstalled) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onUninstallApp(context, app.packageName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("uninstall_app_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Uninstall",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Uninstall")
                                        }
                                        Button(
                                            onClick = { onDownloadLatestGitHubRelease(context, app) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("retry_apk_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Retry",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Update")
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { onDownloadLatestGitHubRelease(context, app) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("retry_apk_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Install")
                                    }
                                }
                            }
                        }

                        is DownloadState.ReadyToInstall -> {
                            val apkVersionLabel = downloadState.versionName.ifBlank { app.versionName }
                            val hasSignatureConflict = isAppInstalled &&
                                downloadState.signingCertSha256.isNotBlank() &&
                                !downloadState.signatureMatchesInstalled
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (hasSignatureConflict) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Signing certificate differs from installed build. Tap Uninstall first, then tap Install.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                                if (isAppInstalled) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onUninstallApp(context, app.packageName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("uninstall_app_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Uninstall",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Uninstall")
                                        }
                                        Button(
                                            onClick = { onInstallApk(context, downloadState.apkFilePath) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("install_apk_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.InstallMobile,
                                                contentDescription = "Install Update",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Update (v$apkVersionLabel)")
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { onInstallApk(context, downloadState.apkFilePath) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("install_apk_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Install (v$apkVersionLabel)")
                                    }
                                }
                                if (downloadState.computedSha256.isNotBlank()) {
                                    Text(
                                        text = "APK SHA-256: ${downloadState.computedSha256.take(24)}... (${if (downloadState.checksumVerified) "Checksum Verified" else "Checksum Warning"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (downloadState.signingCertSha256.isNotBlank()) {
                                    Text(
                                        text = "Signing Cert SHA-256: ${downloadState.signingCertSha256.take(24)}... (${if (downloadState.signatureMatchesInstalled) "Signature Verified" else "Signature Differs"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (downloadState.signatureMatchesInstalled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }
                        }

                        else -> {
                            if (isAppInstalled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onUninstallApp(context, app.packageName) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("uninstall_app_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Uninstall",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Uninstall")
                                    }

                                    if (hasAppUpdate) {
                                        Button(
                                            onClick = { onDownloadLatestGitHubRelease(context, app) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("update_app_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SystemUpdate,
                                                contentDescription = "Update",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Update")
                                        }
                                    } else {
                                        Button(
                                            onClick = { onOpenInstalledApp(context, app.packageName) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("open_installed_app_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Open",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open")
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onDownloadLatestGitHubRelease(context, app) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("install_apk_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Install",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Install")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Screenshots Gallery
        if (app.screenshots.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Preview Screenshots",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(app.screenshots) { screenshotUrl ->
                            Card(
                                modifier = Modifier
                                    .width(248.dp)
                                    .height(152.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
                            ) {
                                AsyncImage(
                                    model = screenshotUrl,
                                    contentDescription = "${app.appName} screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. What's New / Release Notes & Changelog
        if (app.releaseNotes.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "What's New in v${app.versionName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Updated $formattedUpdatedDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = app.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 4B. Multi-Version Release History & Changelogs (Phase 3)
        if (versionHistory.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Release History & Previous Versions (${versionHistory.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        versionHistory.take(5).forEach { ver ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "v${ver.versionName} (Build ${ver.versionCode}) • ${ver.apkFileSize}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                onStartDownload(
                                                    app.copy(
                                                        versionName = ver.versionName,
                                                        versionCode = ver.versionCode,
                                                        apkDownloadUrl = ver.apkDownloadUrl.ifBlank { app.apkDownloadUrl },
                                                        apkFileSize = ver.apkFileSize.ifBlank { app.apkFileSize },
                                                        sha256Checksum = ver.sha256Checksum
                                                    )
                                                )
                                            }
                                        ) {
                                            Text("Download APK")
                                        }
                                    }
                                    if (ver.sha256Checksum.isNotBlank()) {
                                        Text(
                                            text = "SHA-256: ${ver.sha256Checksum.take(24)}...",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (ver.changelog.isNotBlank()) {
                                        Text(
                                            text = ver.changelog,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. About This Application (Full Description)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                }
            }
        }

        // 6. Ratings, Written Reviews, Helpful Votes & Moderation (Phase 2)
        item {
            RatingsAndReviewsSection(
                app = app,
                reviews = reviews,
                currentUserId = currentUserId,
                isAdmin = isAdmin,
                onSubmitReview = onSubmitReview,
                onVoteHelpful = onVoteReviewHelpful,
                onFlagReview = onFlagReview,
                onAdminReply = onAdminReplyReview,
                onDeleteReview = onDeleteReview
            )
        }

        // 7. Technical Specifications, Android Compatibility & Permissions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Package & Compatibility Specifications",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    SpecKeyValueRow("Package ID", app.packageName)
                    SpecKeyValueRow("Version", "v${app.versionName} (Build Code ${app.versionCode})")
                    SpecKeyValueRow("File Size", app.apkFileSize)
                    SpecKeyValueRow("Supported Android", "Android API ${app.minAndroidSdk}+ (Target API ${app.targetAndroidSdk})")
                    SpecKeyValueRow("CPU Architecture", app.architecture)
                    SpecKeyValueRow("Last Updated", formattedUpdatedDate)
                    SpecKeyValueRow("First Published", formattedPublishedDate)
                    SpecKeyValueRow("Offline Support", if (app.offlineCapable) "Yes (Works Offline)" else "Requires Internet")
                    if (app.sha256Checksum.isNotBlank()) {
                        SpecKeyValueRow("SHA-256 Checksum", app.sha256Checksum.take(24) + "...")
                    }

                    if (app.permissionsList.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            text = "Declared Android Permissions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            app.permissionsList.forEach { perm ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                                ) {
                                    Text(
                                        text = perm,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Developer Information & Official Links
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Developer & Distribution Info",
                        style = MaterialTheme.typography.titleMedium
                    )
                    SpecKeyValueRow("Developer", app.developerName)
                    SpecKeyValueRow("Support Contact", app.supportEmail)
                    SpecKeyValueRow("Distribution Source", "GitHub Releases APK")

                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { showDeveloperProfile.value = true },
                            label = { Text("Developer Profile") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        if (app.websiteUrl.isNotBlank()) {
                            AssistChip(
                                onClick = {
                                    onOpenBrowserDownload(context, app.copy(apkDownloadUrl = app.websiteUrl))
                                },
                                label = { Text("Website") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        if (app.githubRepoUrl.isNotBlank()) {
                            AssistChip(
                                onClick = {
                                    onOpenBrowserDownload(context, app.copy(apkDownloadUrl = app.githubRepoUrl))
                                },
                                label = { Text("GitHub Repository") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        if (app.privacyPolicyUrl.isNotBlank()) {
                            AssistChip(
                                onClick = {
                                    onOpenBrowserDownload(context, app.copy(apkDownloadUrl = app.privacyPolicyUrl))
                                },
                                label = { Text("Privacy Policy") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Policy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        AssistChip(
                            onClick = { showReportDialog.value = true },
                            label = { Text("Report App / Security") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // 9. Similar / Related Applications Carousel
        if (relatedCategoryApps.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Similar Applications in ${app.category}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Related tools & apps in the same category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(relatedCategoryApps, key = { "rel_${it.appId.ifBlank { it.docId }}" }) { related ->
                            RelatedAppMiniCard(
                                app = related,
                                onClick = { onSelectRelatedApp(related) }
                            )
                        }
                    }
                }
            }
        }

        // 10. More from this Developer Carousel
        if (moreFromDeveloperApps.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "More by ${app.developerName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(moreFromDeveloperApps, key = { "dev_${it.appId.ifBlank { it.docId }}" }) { devApp ->
                            RelatedAppMiniCard(
                                app = devApp,
                                onClick = { onSelectRelatedApp(devApp) }
                            )
                        }
                    }
                }
            }
        }

        // 11. Recommended for You Carousel
        if (recommendedApps.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recommended for You",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recommendedApps, key = { "rec_det_${it.appId.ifBlank { it.docId }}" }) { recApp ->
                            RelatedAppMiniCard(
                                app = recApp,
                                onClick = { onSelectRelatedApp(recApp) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeveloperProfile.value) {
        DeveloperProfileDialog(
            developerName = app.developerName,
            allApps = allApps,
            onDismiss = { showDeveloperProfile.value = false },
            onSelectApp = onSelectRelatedApp
        )
    }

    if (showReportDialog.value) {
        val reportReason = remember { androidx.compose.runtime.mutableStateOf("Broken Download Link / Checksum Verification") }
        val reportDetails = remember { androidx.compose.runtime.mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReportDialog.value = false },
            title = { Text("Report ${app.appName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = reportReason.value,
                        onValueChange = { reportReason.value = it },
                        label = { Text("Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = reportDetails.value,
                        onValueChange = { reportDetails.value = it },
                        label = { Text("Details") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportApp(reportReason.value, reportDetails.value)
                        showReportDialog.value = false
                    }
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showReportDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RelatedAppMiniCard(
    app: StoreApp,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(168.dp)
            .clickable(onClick = onClick)
            .testTag("related_app_card_${app.appId.ifBlank { app.docId }}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val relatedIconModel: Any = if (
                app.packageName == "com.zayaanapps.store" &&
                (app.iconUrl.isBlank() || app.iconUrl.contains("/web/icons/Icon-192.png"))
            ) {
                R.drawable.img_launcher_icon_1795021165006
            } else {
                app.iconUrl
            }
            AsyncImage(
                model = relatedIconModel,
                contentDescription = "${app.appName} icon",
                placeholder = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                error = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "v${app.versionName} • ${app.apkFileSize}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SpecKeyValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactTelemetryItem(
    icon: ImageVector,
    value: String,
    contentDescription: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MyAppsScreen(
    appsState: UiState<List<StoreApp>>,
    installedStatuses: Map<String, InstalledPackageStatus>,
    downloadStates: Map<String, DownloadState> = emptyMap(),
    downloadedApkFiles: List<File> = emptyList(),
    bookmarkedIds: List<String>,
    favoriteIds: List<String> = emptyList(),
    downloadedAppIds: List<String> = emptyList(),
    comparisonAppIds: List<String> = emptyList(),
    onRefreshScan: () -> Unit,
    onAppClick: (StoreApp) -> Unit,
    onUpdateOrDownloadClick: (StoreApp) -> Unit,
    onUpdateAllClick: (Context, List<StoreApp>) -> Unit = { _, _ -> },
    onInstallCachedApk: (Context, String) -> Unit = { _, _ -> },
    onDeleteCachedApk: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onRemoveFromCompare: (String) -> Unit = {},
    onClearComparison: () -> Unit = {},
    onOpenInstalledApp: (Context, String) -> Unit
) {
    val context = LocalContext.current
    val allApps = (appsState as? UiState.Success)?.data ?: emptyList()

    val updatesAvailableApps = allApps.filter { app ->
        installedStatuses[app.packageName]?.updateAvailable == true
    }
    val installedApps = allApps.filter { app ->
        installedStatuses[app.packageName]?.isInstalled == true
    }
    val favoriteApps = allApps.filter { app ->
        (app.appId.ifBlank { app.docId }) in favoriteIds
    }
    val bookmarkedApps = allApps.filter { app ->
        (app.appId.ifBlank { app.docId }) in bookmarkedIds
    }
    val previouslyDownloadedApps = allApps.filter { app ->
        (app.appId.ifBlank { app.docId }) in downloadedAppIds
    }
    val comparedApps = allApps.filter { app ->
        (app.appId.ifBlank { app.docId }) in comparisonAppIds
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("my_apps_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Library, Downloads & Updates",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Download Manager, Update All, Wishlist, Favorites & App Comparison",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onRefreshScan,
                    modifier = Modifier.testTag("scan_updates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan for app updates",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan")
                }
            }
        }

        // Section 1: Available Updates & Update All Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Updates (${updatesAvailableApps.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (updatesAvailableApps.isNotEmpty()) {
                    Button(
                        onClick = { onUpdateAllClick(context, updatesAvailableApps) },
                        modifier = Modifier.testTag("update_all_apps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update All (${updatesAvailableApps.size})")
                    }
                }
            }
        }

        if (updatesAvailableApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "All apps up to date",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "All installed Zayaan Apps packages are running the latest version.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(updatesAvailableApps, key = { "upd_${it.appId}" }) { app ->
                val status = installedStatuses[app.packageName]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Installed: v${status?.installedVersionName} (${status?.installedVersionCode}) → Latest: v${app.versionName} (${app.versionCode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Button(onClick = { onUpdateOrDownloadClick(app) }) {
                            Text("Update APK")
                        }
                    }
                }
            }
        }

        // Section 2: Download Manager (Active Downloads, Progress, Retry & Cached APK Files)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("download_manager_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "APK Download Manager & Local Cache",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Track live progress, retry interrupted transfers & install cached APKs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val activeEntries = downloadStates.entries.filter { it.value !is DownloadState.Idle }
                    if (activeEntries.isNotEmpty()) {
                        activeEntries.forEach { (appId, state) ->
                            val matchedApp = allApps.firstOrNull { (it.appId.ifBlank { it.docId }) == appId }
                            val displayTitle = matchedApp?.appName ?: appId
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    when (state) {
                                        is DownloadState.Downloading -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$displayTitle (${state.progressPercent}%)",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                                IconButton(onClick = { onCancelDownload(appId) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Cancel download",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            LinearProgressIndicator(
                                                progress = { state.progressPercent / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                        }

                                        is DownloadState.Paused -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$displayTitle — Paused (${state.progressPercent}%)",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                if (matchedApp != null) {
                                                    FilledTonalButton(onClick = { onUpdateOrDownloadClick(matchedApp) }) {
                                                        Text("Resume")
                                                    }
                                                }
                                            }
                                        }

                                        is DownloadState.Failed -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "$displayTitle — Download Failed",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                    Text(
                                                        text = state.errorMessage,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (matchedApp != null) {
                                                    Button(onClick = { onUpdateOrDownloadClick(matchedApp) }) {
                                                        Text("Retry")
                                                    }
                                                }
                                            }
                                        }

                                        is DownloadState.ReadyToInstall -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "$displayTitle — Ready to Install",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                    if (state.computedSha256.isNotBlank()) {
                                                        Text(
                                                            text = "SHA-256: ${state.computedSha256.take(16)}...",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                                Button(onClick = { onInstallCachedApk(context, state.apkFilePath) }) {
                                                    Text("Install APK")
                                                }
                                            }
                                        }

                                        else -> {}
                                    }
                                }
                            }
                        }
                    }

                    if (downloadedApkFiles.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Cached APK Packages on Device (${downloadedApkFiles.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        downloadedApkFiles.forEach { file ->
                            val sizeMb = String.format(Locale.US, "%.2f MB", file.length().toDouble() / (1024.0 * 1024.0))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = sizeMb,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilledTonalButton(
                                        onClick = { onInstallCachedApk(context, file.absolutePath) }
                                    ) {
                                        Text("Install", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(
                                        onClick = { onDeleteCachedApk(file.absolutePath) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete cached APK",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (activeEntries.isEmpty()) {
                        Text(
                            text = "No active or cached APK downloads in local storage yet. Start any download from the Store catalog to track progress here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 3: Side-by-Side Application Comparison
        item {
            AppComparisonSectionCard(
                comparedApps = comparedApps,
                installedStatuses = installedStatuses,
                onSelectApp = onAppClick,
                onRemoveFromCompare = onRemoveFromCompare,
                onClearAll = onClearComparison,
                onDownloadOrUpdate = onUpdateOrDownloadClick
            )
        }

        // Section 4: Installed Packages on This Device
        item {
            Text(
                text = "Installed on This Device (${installedApps.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (installedApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No catalog packages detected as installed yet. Download any APK from the Store tab to install.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(installedApps, key = { "inst_${it.appId}" }) { app ->
                val status = installedStatuses[app.packageName]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${app.packageName} • v${status?.installedVersionName ?: app.versionName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { onOpenInstalledApp(context, app.packageName) }) {
                            Text("Open")
                        }
                    }
                }
            }
        }

        // Section 5: Personal Library — Favorite Apps
        item {
            Text(
                text = "Favorite Apps (${favoriteApps.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (favoriteApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Tap the heart icon on any application details page to mark it as a Favorite (synchronized across Android and Web).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(favoriteApps, key = { "fav_${it.appId}" }) { app ->
                StoreAppListRowCard(
                    app = app,
                    installedStatus = installedStatuses[app.packageName],
                    onClick = { onAppClick(app) }
                )
            }
        }

        // Section 6: Personal Library — Saved / Wishlist Apps
        item {
            Text(
                text = "Wishlist / Saved Apps (${bookmarkedApps.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (bookmarkedApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Tap the bookmark icon on any application details page to save it to your Wishlist.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(bookmarkedApps, key = { "bm_${it.appId}" }) { app ->
                StoreAppListRowCard(
                    app = app,
                    installedStatus = installedStatuses[app.packageName],
                    onClick = { onAppClick(app) }
                )
            }
        }

        // Section 7: Personal Library — Previously Downloaded History
        item {
            Text(
                text = "Previously Downloaded History (${previouslyDownloadedApps.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (previouslyDownloadedApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Applications you download are automatically recorded in your cloud-synced download history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(previouslyDownloadedApps, key = { "dl_hist_${it.appId}" }) { app ->
                StoreAppListRowCard(
                    app = app,
                    installedStatus = installedStatuses[app.packageName],
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}
