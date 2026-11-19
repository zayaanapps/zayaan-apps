package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
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
            "Latest Release"
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

        // 2. App Identity Header Card (Compact & Bordered)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val detailIconModel: Any = if (app.packageName == "com.zayaanapps.store") {
                            R.drawable.zayaan_logo_badge
                        } else {
                            app.iconUrl
                        }
                        AsyncImage(
                            model = detailIconModel,
                            contentDescription = "${app.appName} icon",
                            placeholder = painterResource(id = R.drawable.zayaan_logo_badge),
                            error = painterResource(id = R.drawable.zayaan_logo_badge),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (app.editorsChoice) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Editor's Choice",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = app.developerName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = app.packageName,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(7.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = app.category,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (app.editorsChoice) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(7.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WorkspacePremium,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "Editor's Choice",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (app.offlineCapable) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(7.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OfflineBolt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "Offline Ready",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (app.shortDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = app.shortDescription,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                WhatsNewCompactCard(
                    versionName = app.versionName,
                    updatedDateLabel = formattedUpdatedDate,
                    releaseNotes = app.releaseNotes
                )
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

        // 5. About This Application (Introduction + Package, Version, Size & Specs)
        item {
            AboutApplicationCompactCard(
                app = app
            )
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

        // 7. Technical Specifications, Android Compatibility & Permissions (Compact & Bordered)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Package & Compatibility",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f))
                        ) {
                            Text(
                                text = "API ${app.minAndroidSdk}+",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            SpecKeyValueRow("Package ID", app.packageName, Icons.Default.Inventory2)
                            SpecRowDivider()
                            SpecKeyValueRow("Version", "v${app.versionName} (Code ${app.versionCode})", Icons.Default.NewReleases)
                            SpecRowDivider()
                            SpecKeyValueRow("APK Size", app.apkFileSize, Icons.Default.SdStorage)
                            SpecRowDivider()
                            SpecKeyValueRow("Android OS", "API ${app.minAndroidSdk}+ (Target ${app.targetAndroidSdk})", Icons.Default.PhoneAndroid)
                            SpecRowDivider()
                            SpecKeyValueRow("Architecture", app.architecture, Icons.Default.Memory)
                            SpecRowDivider()
                            SpecKeyValueRow("Updated / Published", "$formattedUpdatedDate • $formattedPublishedDate", Icons.Default.CalendarToday)
                            SpecRowDivider()
                            SpecKeyValueRow(
                                "Connectivity",
                                if (app.offlineCapable) "Works Offline" else "Requires Internet",
                                if (app.offlineCapable) Icons.Default.OfflineBolt else Icons.Default.Wifi
                            )
                            if (app.sha256Checksum.isNotBlank()) {
                                SpecRowDivider()
                                SpecKeyValueRow("SHA-256", "${app.sha256Checksum.take(20)}...", Icons.Default.Fingerprint)
                            }
                        }
                    }

                    if (app.permissionsList.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Declared Permissions (${app.permissionsList.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            app.permissionsList.forEach { perm ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = perm,
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Developer Information & Official Links (Compact & Bordered)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Developer & Distribution Info",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            SpecKeyValueRow("Developer", app.developerName, Icons.Default.Verified)
                            SpecRowDivider()
                            SpecKeyValueRow("Support", app.supportEmail, Icons.Default.Email)
                            SpecRowDivider()
                            SpecKeyValueRow("Source", "GitHub Releases APK", Icons.Default.CloudDone)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactActionChip(
                            label = "Developer Profile",
                            icon = Icons.Default.Verified,
                            onClick = { showDeveloperProfile.value = true }
                        )
                        if (app.websiteUrl.isNotBlank()) {
                            CompactActionChip(
                                label = "Website",
                                icon = Icons.Default.Language,
                                onClick = {
                                    onOpenBrowserDownload(context, app.copy(apkDownloadUrl = app.websiteUrl))
                                }
                            )
                        }
                        if (app.githubRepoUrl.isNotBlank()) {
                            CompactActionChip(
                                label = "GitHub Repo",
                                icon = Icons.Default.Code,
                                onClick = {
                                    onOpenBrowserDownload(context, app.copy(apkDownloadUrl = app.githubRepoUrl))
                                }
                            )
                        }
                        if (app.privacyPolicyUrl.isNotBlank()) {
                            CompactActionChip(
                                label = "Privacy Policy",
                                icon = Icons.Default.Policy,
                                onClick = {
                                    onOpenBrowserDownload(context, app.copy(apkDownloadUrl = app.privacyPolicyUrl))
                                }
                            )
                        }
                        CompactActionChip(
                            label = "Report Issue",
                            icon = Icons.Default.Security,
                            onClick = { showReportDialog.value = true }
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
            val relatedIconModel: Any = if (app.packageName == "com.zayaanapps.store") {
                R.drawable.zayaan_logo_badge
            } else {
                app.iconUrl
            }
            AsyncImage(
                model = relatedIconModel,
                contentDescription = "${app.appName} icon",
                placeholder = painterResource(id = R.drawable.zayaan_logo_badge),
                error = painterResource(id = R.drawable.zayaan_logo_badge),
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
    value: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpecRowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    )
}

@Composable
private fun CompactActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
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

private data class ExtractedUrlLink(
    val label: String,
    val url: String
)

private val RICH_TEXT_TOKEN_REGEX = Regex(
    """\[([^\]]+)]\((https?://[^\s)]+)\)|https?://[^\s<>)\]]+|\*\*([^*]+)\*\*|`([^`]+)`"""
)

private val EXTRACT_URL_REGEX = Regex("""\[([^\]]+)]\((https?://[^\s)]+)\)|(https?://[^\s<>)\]]+)""")

private fun extractLinksFromRichText(rawText: String): List<ExtractedUrlLink> {
    val links = mutableListOf<ExtractedUrlLink>()
    val seenUrls = mutableSetOf<String>()
    for (rawLine in rawText.lines()) {
        val line = rawLine.trim()
        if (line.isBlank()) continue
        for (match in EXTRACT_URL_REGEX.findAll(line)) {
            val mdLabel = match.groupValues[1].trim()
            val mdUrl = match.groupValues[2].trim()
            val bareUrl = match.groupValues[3].trimEnd('.', ',', ';', ')')
            val url = mdUrl.ifBlank { bareUrl }
            if (url.isBlank() || !seenUrls.add(url)) continue

            val derivedLabel = if (mdLabel.isNotBlank()) {
                mdLabel
            } else {
                val beforeUrl = line.substring(0, match.range.first)
                    .replace(Regex("""^[\-*•#>\s]+"""), "")
                    .replace("**", "")
                    .replace("`", "")
                    .trim()
                    .trimEnd(':', '-', ' ')
                when {
                    beforeUrl.isNotBlank() -> beforeUrl
                    url.contains("/compare/", ignoreCase = true) -> "Full Changelog"
                    url.contains("github.io", ignoreCase = true) -> "Web Portal"
                    url.contains("github.com", ignoreCase = true) -> "GitHub"
                    else -> "Open Link"
                }
            }
            links.add(ExtractedUrlLink(label = derivedLabel, url = url))
        }
    }
    return links
}

private fun buildFormattedRichText(
    rawText: String,
    headingColor: Color,
    bodyColor: Color,
    linkColor: Color,
    codeBgColor: Color
): AnnotatedString {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    )
    val lines = rawText.lines()
    return buildAnnotatedString {
        var appendedAny = false
        var lastWasBlank = false
        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                if (appendedAny && !lastWasBlank) {
                    append('\n')
                    lastWasBlank = true
                }
                continue
            }
            if (appendedAny && !lastWasBlank) {
                append('\n')
            }
            appendedAny = true
            lastWasBlank = false

            val isHeading = trimmed.startsWith("#")
            val normalizedLine = when {
                isHeading -> trimmed.trimStart('#').trim()
                trimmed.startsWith("- ") -> "• ${trimmed.removePrefix("- ").trim()}"
                trimmed.startsWith("* ") -> "• ${trimmed.removePrefix("* ").trim()}"
                else -> trimmed
            }

            if (isHeading) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = headingColor)) {
                    appendFormattedInlineTokens(normalizedLine, bodyColor, codeBgColor, linkStyle)
                }
            } else {
                appendFormattedInlineTokens(normalizedLine, bodyColor, codeBgColor, linkStyle)
            }
        }
    }
}

private fun AnnotatedString.Builder.appendFormattedInlineTokens(
    text: String,
    bodyColor: Color,
    codeBgColor: Color,
    linkStyle: TextLinkStyles
) {
    var cursor = 0
    for (match in RICH_TEXT_TOKEN_REGEX.findAll(text)) {
        if (match.range.first > cursor) {
            withStyle(SpanStyle(color = bodyColor)) {
                append(text.substring(cursor, match.range.first))
            }
        }
        val mdLabel = match.groupValues[1]
        val mdUrl = match.groupValues[2]
        val boldText = match.groupValues[3]
        val codeText = match.groupValues[4]
        when {
            mdUrl.isNotBlank() -> {
                withLink(LinkAnnotation.Url(url = mdUrl, styles = linkStyle)) {
                    append(mdLabel.ifBlank { mdUrl })
                }
            }
            boldText.isNotBlank() -> {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = bodyColor)) {
                    append(boldText)
                }
            }
            codeText.isNotBlank() -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        background = codeBgColor,
                        color = bodyColor
                    )
                ) {
                    append(codeText)
                }
            }
            else -> {
                val rawUrl = match.value
                val cleanUrl = rawUrl.trimEnd('.', ',', ';', ')')
                val trailing = rawUrl.removePrefix(cleanUrl)
                withLink(LinkAnnotation.Url(url = cleanUrl, styles = linkStyle)) {
                    append(cleanUrl)
                }
                if (trailing.isNotEmpty()) {
                    withStyle(SpanStyle(color = bodyColor)) {
                        append(trailing)
                    }
                }
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        withStyle(SpanStyle(color = bodyColor)) {
            append(text.substring(cursor))
        }
    }
}

private fun sanitizeWhatsNewNotes(rawNotes: String, versionName: String): String {
    val withoutDetails = rawNotes.replace(
        Regex("""<details>[\s\S]*?</details>""", RegexOption.IGNORE_CASE),
        ""
    )
    val metadataLinePrefixes = listOf(
        "### zayaan apps official store release",
        "package name:",
        "architecture:",
        "version name:",
        "version code:",
        "commit:",
        "apk sha-256 checksum:",
        "responsive web portal:",
        "direct apk assets:"
    )
    val kept = mutableListOf<String>()
    var compareLine = ""
    for (rawLine in withoutDetails.lines()) {
        val trimmed = rawLine.trim()
        val normalized = trimmed
            .removePrefix("- ")
            .removePrefix("* ")
            .replace("**", "")
            .replace("`", "")
            .trim()
            .lowercase()
        if (normalized.startsWith("full changelog:") || trimmed.contains("/compare/", ignoreCase = true)) {
            compareLine = trimmed
            continue
        }
        if (metadataLinePrefixes.none { normalized.startsWith(it) }) {
            kept.add(rawLine)
        }
    }
    val cleanedBody = kept.joinToString("\n").trim()
    return buildString {
        if (cleanedBody.isNotBlank()) {
            append(cleanedBody)
        } else {
            append("### Latest Updates, Fixes & Changes (v$versionName)\n")
            append("• Updated release v$versionName with latest fixes, performance & UI refinements.")
        }
        if (compareLine.isNotBlank()) {
            append("\n\n")
            append(compareLine)
        }
    }.trim()
}

@Composable
private fun WhatsNewCompactCard(
    versionName: String,
    updatedDateLabel: String,
    releaseNotes: String
) {
    val uriHandler = LocalUriHandler.current
    val cleanedNotes = remember(releaseNotes, versionName) {
        sanitizeWhatsNewNotes(releaseNotes, versionName)
    }
    var expanded by remember(cleanedNotes) { mutableStateOf(false) }

    val extractedLinks = remember(cleanedNotes) { extractLinksFromRichText(cleanedNotes) }
    val nonEmptyLines = remember(cleanedNotes) {
        cleanedNotes.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
    val hasLongNotes = nonEmptyLines.size > 6 || cleanedNotes.length > 320

    val displayedRawNotes = remember(cleanedNotes, expanded, hasLongNotes) {
        if (expanded || !hasLongNotes) {
            cleanedNotes
        } else {
            nonEmptyLines
                .filterNot { line ->
                    val lower = line.lowercase()
                    (lower.contains("full changelog") || lower.contains("web portal")) &&
                        line.contains("http", ignoreCase = true)
                }
                .take(5)
                .joinToString("\n")
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    val formattedNotes = remember(displayedRawNotes, primaryColor, secondaryColor, onSurfaceColor, codeBgColor) {
        buildFormattedRichText(
            rawText = displayedRawNotes,
            headingColor = primaryColor,
            bodyColor = onSurfaceColor,
            linkColor = secondaryColor,
            codeBgColor = codeBgColor
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "What's New in v$versionName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = updatedDateLabel,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (formattedNotes.isNotEmpty()) {
                Text(
                    text = formattedNotes,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            if (extractedLinks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    extractedLinks.forEach { link ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runCatching { uriHandler.openUri(link.url) }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${link.label}:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = link.url,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textDecoration = TextDecoration.Underline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open ${link.label}",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (hasLongNotes) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                        modifier = Modifier.clickable { expanded = !expanded }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (expanded) "Show less" else "Show full notes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse notes" else "Expand notes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutApplicationCompactCard(
    app: StoreApp
) {
    val description = app.description.ifBlank { app.shortDescription }
    var expanded by remember(description) { mutableStateOf(false) }
    val isLongDescription = description.length > 260 || description.lines().size > 5

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val formattedDescription = remember(description, primaryColor, secondaryColor, onSurfaceVariantColor, codeBgColor) {
        buildFormattedRichText(
            rawText = description,
            headingColor = primaryColor,
            bodyColor = onSurfaceVariantColor,
            linkColor = secondaryColor,
            codeBgColor = codeBgColor
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "About this application",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "v${app.versionName}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (formattedDescription.isNotEmpty()) {
                Text(
                    text = formattedDescription,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    maxLines = if (expanded || !isLongDescription) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isLongDescription) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.clickable { expanded = !expanded }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (expanded) "Show less" else "Read more",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse description" else "Expand description",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Package, Version, Category, Size & OS Details right inside About App
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SpecKeyValueRow("Package", app.packageName, Icons.Default.Inventory2)
                    SpecRowDivider()
                    SpecKeyValueRow("Version", "v${app.versionName} (Build ${app.versionCode})", Icons.Default.NewReleases)
                    SpecRowDivider()
                    SpecKeyValueRow("Category", app.category, Icons.Default.Category)
                    SpecRowDivider()
                    SpecKeyValueRow("APK Size", app.apkFileSize, Icons.Default.SdStorage)
                    SpecRowDivider()
                    SpecKeyValueRow("Developer", app.developerName, Icons.Default.Storefront)
                    SpecRowDivider()
                    SpecKeyValueRow("Android OS", "API ${app.minAndroidSdk}+ (Target ${app.targetAndroidSdk})", Icons.Default.PhoneAndroid)
                    if (app.architecture.isNotBlank()) {
                        SpecRowDivider()
                        SpecKeyValueRow("Architecture", app.architecture, Icons.Default.Memory)
                    }
                    if (app.sha256Checksum.isNotBlank()) {
                        SpecRowDivider()
                        SpecKeyValueRow("SHA-256", "${app.sha256Checksum.take(20)}...", Icons.Default.Fingerprint)
                    }
                }
            }
        }
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
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
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
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact, Theme-Adaptive Library Header Card
        item {
            val headerGradient = if (isDark) {
                listOf(Color(0xFF0C2235), Color(0xFF092A27))
            } else {
                listOf(Color(0xFFEAF8F1), Color(0xFFF1F7FC))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, cs.primary.copy(alpha = if (isDark) 0.34f else 0.24f)),
                colors = CardDefaults.cardColors(containerColor = cs.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(headerGradient))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.zayaan_logo_badge),
                                contentDescription = "Zayaan Apps Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "Library, Downloads & Updates",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = cs.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Updates, downloads, installed & saved apps",
                                    fontSize = 11.sp,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = cs.surface.copy(alpha = if (isDark) 0.85f else 0.94f),
                            shape = RoundedCornerShape(9.dp),
                            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable(onClick = onRefreshScan)
                                .testTag("scan_updates_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Scan for app updates",
                                    tint = cs.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Scan",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.primary
                                )
                            }
                        }
                    }

                    // Compact bordered summary strip
                    Surface(
                        color = cs.surface.copy(alpha = if (isDark) 0.62f else 0.88f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MyAppsSummaryMetric(
                                icon = Icons.Default.SystemUpdate,
                                label = "${updatesAvailableApps.size} Updates",
                                tint = if (updatesAvailableApps.isNotEmpty()) cs.tertiary else cs.primary
                            )
                            VerticalDivider(modifier = Modifier.height(11.dp), color = cs.outline.copy(alpha = 0.22f))
                            MyAppsSummaryMetric(
                                icon = Icons.Default.PhoneAndroid,
                                label = "${installedApps.size} Installed",
                                tint = cs.primary
                            )
                            VerticalDivider(modifier = Modifier.height(11.dp), color = cs.outline.copy(alpha = 0.22f))
                            MyAppsSummaryMetric(
                                icon = Icons.Default.Favorite,
                                label = "${favoriteApps.size} Favs",
                                tint = cs.secondary
                            )
                            VerticalDivider(modifier = Modifier.height(11.dp), color = cs.outline.copy(alpha = 0.22f))
                            MyAppsSummaryMetric(
                                icon = Icons.Default.Bookmark,
                                label = "${bookmarkedApps.size} Saved",
                                tint = cs.tertiary
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Available Updates & Update All (shown only if multiple updates available)
        item {
            MyAppsSectionHeader(
                icon = Icons.Default.SystemUpdate,
                iconTint = cs.tertiary,
                title = "Available Updates",
                count = updatesAvailableApps.size,
                trailingContent = if (updatesAvailableApps.size > 1) {
                    {
                        Button(
                            onClick = { onUpdateAllClick(context, updatesAvailableApps) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("update_all_apps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Update All (${updatesAvailableApps.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else null
            )
        }

        if (updatesAvailableApps.isEmpty()) {
            item {
                MyAppsCompactEmptyCard(
                    icon = Icons.Default.CheckCircle,
                    iconTint = cs.primary,
                    borderColor = cs.primary.copy(alpha = 0.26f),
                    title = "All Apps Up to Date",
                    subtitle = "All installed Zayaan Apps packages are running the latest release."
                )
            }
        } else {
            items(updatesAvailableApps, key = { "upd_${it.appId}" }) { app ->
                val status = installedStatuses[app.packageName]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppClick(app) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, cs.tertiary.copy(alpha = 0.34f)),
                    colors = CardDefaults.cardColors(containerColor = cs.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MyAppsThumbnailIcon(app = app, size = 42.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = null,
                                    tint = cs.tertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "v${status?.installedVersionName} (${status?.installedVersionCode}) → v${app.versionName} (${app.versionCode})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = cs.tertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onUpdateOrDownloadClick(app) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Update", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section 2: Download Manager & Local Cache
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("download_manager_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.28f)),
                colors = CardDefaults.cardColors(containerColor = cs.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = cs.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "APK Download Manager & Cache",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Live transfers, retries & cached APK installers",
                                fontSize = 11.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val activeEntries = downloadStates.entries.filter { it.value !is DownloadState.Idle }
                    if (activeEntries.isNotEmpty()) {
                        activeEntries.forEach { (appId, state) ->
                            val matchedApp = allApps.firstOrNull { (it.appId.ifBlank { it.docId }) == appId }
                            val displayTitle = matchedApp?.appName ?: appId
                            Surface(
                                color = cs.surfaceVariant.copy(alpha = 0.42f),
                                shape = RoundedCornerShape(11.dp),
                                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.24f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    when (state) {
                                        is DownloadState.Downloading -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$displayTitle (${state.progressPercent}%)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .clickable { onCancelDownload(appId) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Cancel download",
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            LinearProgressIndicator(
                                                progress = { state.progressPercent / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
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
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (matchedApp != null) {
                                                    FilledTonalButton(
                                                        onClick = { onUpdateOrDownloadClick(matchedApp) },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text("Resume", fontSize = 11.sp)
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
                                                        text = "$displayTitle — Failed",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = cs.error
                                                    )
                                                    Text(
                                                        text = state.errorMessage,
                                                        fontSize = 11.sp,
                                                        color = cs.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (matchedApp != null) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = { onUpdateOrDownloadClick(matchedApp) },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text("Retry", fontSize = 11.sp)
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
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = cs.secondary
                                                    )
                                                    if (state.computedSha256.isNotBlank()) {
                                                        Text(
                                                            text = "SHA-256: ${state.computedSha256.take(16)}...",
                                                            fontSize = 10.5.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = cs.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { onInstallCachedApk(context, state.apkFilePath) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Install", fontSize = 11.sp)
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
                        HorizontalDivider(color = cs.outline.copy(alpha = 0.20f))
                        Text(
                            text = "Cached APK Packages (${downloadedApkFiles.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.primary
                        )
                        downloadedApkFiles.forEach { file ->
                            val sizeMb = String.format(Locale.US, "%.2f MB", file.length().toDouble() / (1024.0 * 1024.0))
                            Surface(
                                color = cs.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.22f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SdStorage,
                                            contentDescription = null,
                                            tint = cs.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = file.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = sizeMb,
                                                fontSize = 10.5.sp,
                                                color = cs.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { onInstallCachedApk(context, file.absolutePath) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Install", fontSize = 11.sp)
                                        }
                                        Surface(
                                            color = cs.errorContainer.copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, cs.error.copy(alpha = 0.25f)),
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onDeleteCachedApk(file.absolutePath) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete cached APK",
                                                    tint = cs.error,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (activeEntries.isEmpty()) {
                        Surface(
                            color = cs.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.20f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = cs.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "No active or cached APK downloads. Installed APKs are automatically cleaned up.",
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
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
            MyAppsSectionHeader(
                icon = Icons.Default.PhoneAndroid,
                iconTint = cs.primary,
                title = "Installed on This Device",
                count = installedApps.size
            )
        }

        if (installedApps.isEmpty()) {
            item {
                MyAppsCompactEmptyCard(
                    icon = Icons.Default.PhoneAndroid,
                    iconTint = cs.onSurfaceVariant,
                    borderColor = cs.outline.copy(alpha = 0.24f),
                    title = "No Store Apps Installed Yet",
                    subtitle = "Download any verified APK from the Store tab to track and open it here."
                )
            }
        } else {
            items(installedApps, key = { "inst_${it.appId}" }) { app ->
                val status = installedStatuses[app.packageName]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppClick(app) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.26f)),
                    colors = CardDefaults.cardColors(containerColor = cs.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MyAppsThumbnailIcon(app = app, size = 42.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Installed",
                                    tint = cs.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = "${app.packageName} • v${status?.installedVersionName ?: app.versionName}",
                                fontSize = 11.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onOpenInstalledApp(context, app.packageName) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(9.dp),
                            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.42f)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section 5: Personal Library — Favorite Apps
        item {
            MyAppsSectionHeader(
                icon = Icons.Default.Favorite,
                iconTint = cs.secondary,
                title = "Favorite Apps",
                count = favoriteApps.size
            )
        }

        if (favoriteApps.isEmpty()) {
            item {
                MyAppsCompactEmptyCard(
                    icon = Icons.Default.FavoriteBorder,
                    iconTint = cs.secondary,
                    borderColor = cs.outline.copy(alpha = 0.24f),
                    title = "No Favorites Yet",
                    subtitle = "Tap the heart icon on any app details screen to save favorites across Android & Web."
                )
            }
        } else {
            items(favoriteApps, key = { "fav_${it.appId}" }) { app ->
                MyAppsCompactRowCard(
                    app = app,
                    installedStatus = installedStatuses[app.packageName],
                    onClick = { onAppClick(app) }
                )
            }
        }

        // Section 6: Personal Library — Saved / Wishlist Apps
        item {
            MyAppsSectionHeader(
                icon = Icons.Default.Bookmark,
                iconTint = cs.tertiary,
                title = "Wishlist / Saved Apps",
                count = bookmarkedApps.size
            )
        }

        if (bookmarkedApps.isEmpty()) {
            item {
                MyAppsCompactEmptyCard(
                    icon = Icons.Default.BookmarkBorder,
                    iconTint = cs.tertiary,
                    borderColor = cs.outline.copy(alpha = 0.24f),
                    title = "Wishlist Empty",
                    subtitle = "Tap the bookmark icon on any app details page to save it for later."
                )
            }
        } else {
            items(bookmarkedApps, key = { "bm_${it.appId}" }) { app ->
                MyAppsCompactRowCard(
                    app = app,
                    installedStatus = installedStatuses[app.packageName],
                    onClick = { onAppClick(app) }
                )
            }
        }

        // Section 7: Personal Library — Previously Downloaded History
        item {
            MyAppsSectionHeader(
                icon = Icons.Default.History,
                iconTint = cs.primary,
                title = "Previously Downloaded History",
                count = previouslyDownloadedApps.size
            )
        }

        if (previouslyDownloadedApps.isEmpty()) {
            item {
                MyAppsCompactEmptyCard(
                    icon = Icons.Default.History,
                    iconTint = cs.onSurfaceVariant,
                    borderColor = cs.outline.copy(alpha = 0.24f),
                    title = "No Download History",
                    subtitle = "Apps you download are automatically synced to your cloud library."
                )
            }
        } else {
            items(previouslyDownloadedApps, key = { "dl_hist_${it.appId}" }) { app ->
                MyAppsCompactRowCard(
                    app = app,
                    installedStatus = installedStatuses[app.packageName],
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

@Composable
private fun MyAppsSummaryMetric(
    icon: ImageVector,
    label: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MyAppsSectionHeader(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    count: Int,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, iconTint.copy(alpha = 0.28f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(15.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Surface(
                color = cs.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.22f))
            ) {
                Text(
                    text = "$count",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
private fun MyAppsCompactEmptyCard(
    icon: ImageVector,
    iconTint: Color,
    borderColor: Color,
    title: String,
    subtitle: String
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = cs.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = iconTint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, iconTint.copy(alpha = 0.26f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 14.5.sp,
                    color = cs.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MyAppsThumbnailIcon(
    app: StoreApp,
    size: androidx.compose.ui.unit.Dp
) {
    val cs = MaterialTheme.colorScheme
    val iconModel: Any = if (app.packageName == "com.zayaanapps.store") {
        R.drawable.zayaan_logo_badge
    } else {
        app.iconUrl
    }
    AsyncImage(
        model = iconModel,
        contentDescription = "${app.appName} icon",
        placeholder = painterResource(id = R.drawable.zayaan_logo_badge),
        error = painterResource(id = R.drawable.zayaan_logo_badge),
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(11.dp))
            .border(1.dp, cs.outline.copy(alpha = 0.26f), RoundedCornerShape(11.dp))
            .background(cs.surfaceVariant),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun MyAppsCompactRowCard(
    app: StoreApp,
    installedStatus: InstalledPackageStatus?,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.26f)),
        colors = CardDefaults.cardColors(containerColor = cs.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MyAppsThumbnailIcon(app = app, size = 44.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.editorsChoice) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = cs.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = "${app.developerName} • ${app.category}",
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "v${app.versionName}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = cs.primary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = cs.secondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = app.apkFileSize,
                            fontSize = 10.5.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                    if (installedStatus?.isInstalled == true) {
                        Surface(
                            color = if (installedStatus.updateAvailable) {
                                cs.tertiaryContainer
                            } else {
                                cs.primaryContainer
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (installedStatus.updateAvailable) "Update" else "Installed",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (installedStatus.updateAvailable) {
                                    cs.onTertiaryContainer
                                } else {
                                    cs.onPrimaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open details",
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
