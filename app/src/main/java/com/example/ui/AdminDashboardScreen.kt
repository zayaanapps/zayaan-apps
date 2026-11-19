package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AdminRecord
import com.example.data.ApkArchiveVerificationInfo
import com.example.data.AppCollection
import com.example.data.AppReport
import com.example.data.AppReview
import com.example.data.AppVersionRecord
import com.example.data.AuditLogRecord
import com.example.data.DeveloperSubmission
import com.example.data.STORE_CATEGORIES
import com.example.data.StoreApp
import com.example.data.StoreCategory
import com.example.data.StoreNotification

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    adminAppsState: UiState<List<StoreApp>>,
    categoriesState: UiState<List<StoreCategory>>,
    allReviews: List<AppReview> = emptyList(),
    notifications: List<StoreNotification> = emptyList(),
    collections: List<AppCollection> = emptyList(),
    submissions: List<DeveloperSubmission> = emptyList(),
    reports: List<AppReport> = emptyList(),
    versionHistory: List<AppVersionRecord> = emptyList(),
    adminRecords: List<AdminRecord> = emptyList(),
    auditLogs: List<AuditLogRecord> = emptyList(),
    cachedApkVerifications: List<ApkArchiveVerificationInfo> = emptyList(),
    currentUserId: String = "",
    currentUserEmail: String?,
    isTrustedAdmin: Boolean = true,
    onSaveApp: (StoreApp, Boolean) -> Unit,
    onTogglePublished: (StoreApp) -> Unit,
    onToggleFeatured: (StoreApp) -> Unit,
    onToggleEditorsChoice: (StoreApp) -> Unit = {},
    onToggleEssential: (StoreApp) -> Unit = {},
    onDeleteApp: (StoreApp) -> Unit,
    onAddCategory: (String, String, Long) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onReplyToReview: (AppReview, String) -> Unit = { _, _ -> },
    onClearReviewFlag: (AppReview) -> Unit = {},
    onDeleteReview: (AppReview) -> Unit = {},
    onBroadcastNotification: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteNotification: (String) -> Unit = {},
    onSaveCollection: (String, String, String, List<String>, Boolean, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteCollection: (String) -> Unit = {},
    onSubmitDeveloperApp: (DeveloperSubmission) -> Unit = {},
    onReviewDeveloperSubmission: (DeveloperSubmission, String, String) -> Unit = { _, _, _ -> },
    onDeleteDeveloperSubmission: (String) -> Unit = {},
    onSubmitReport: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onUpdateReportStatus: (AppReport, String) -> Unit = { _, _ -> },
    onDeleteReport: (String) -> Unit = {},
    onAddVersionRecord: (String, String, Long, String, String, String, String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteVersionRecord: (String) -> Unit = {},
    onGrantAdminRole: (String, String) -> Unit = { _, _ -> },
    onRevokeAdminRole: (String) -> Unit = {},
    onSeedOfficialCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingApp by remember { mutableStateOf<StoreApp?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showModerationDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showCollectionsDialog by remember { mutableStateOf(false) }
    var showSubmissionsDialog by remember { mutableStateOf(false) }
    var showVersionsDialog by remember { mutableStateOf(false) }
    var showReportsDialog by remember { mutableStateOf(false) }
    var showUsersRolesDialog by remember { mutableStateOf(false) }
    var showAnalyticsAuditDialog by remember { mutableStateOf(false) }

    val apps = (adminAppsState as? UiState.Success)?.data ?: emptyList()
    val totalDownloads = apps.sumOf { it.downloadCount }
    val totalPageViews = apps.sumOf { it.pageViews }
    val publishedCount = apps.count { it.published }
    val featuredCount = apps.count { it.featured }
    val pendingSubmissionsCount = submissions.count { it.status == "PENDING" }
    val openReportsCount = reports.count { it.status == "OPEN" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Admin Header, RBAC Status & Real-Time Marketplace Analytics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zayaan Apps Admin & Marketplace Console",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${currentUserEmail ?: "Open Store Mode (Local Session)"} • ${if (isTrustedAdmin) "Admin Console" else "Developer Portal Mode"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AdminMetricBox("APPS", "${apps.size}")
                        AdminMetricBox("LIVE", "$publishedCount")
                        AdminMetricBox("FEATURED", "$featuredCount")
                        AdminMetricBox("DOWNLOADS", "$totalDownloads")
                        AdminMetricBox("VIEWS", "$totalPageViews")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: Primary App CRUD, Categories, Collections & Banners
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                editingApp = StoreApp()
                                isCreatingNew = true
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("admin_add_app_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publish App")
                        }

                        FilledTonalButton(
                            onClick = { showCategoryDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_manage_categories_button")
                        ) {
                            Icon(imageVector = Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Categories")
                        }

                        FilledTonalButton(
                            onClick = { showCollectionsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_manage_collections_button")
                        ) {
                            Icon(imageVector = Icons.Default.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Banners (${collections.size})")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Developer Submissions Queue & Multi-Version / SHA-256 Checksum Manager
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showSubmissionsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_submissions_button")
                        ) {
                            Icon(imageVector = Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (pendingSubmissionsCount > 0) {
                                    "Submissions ($pendingSubmissionsCount Pending)"
                                } else {
                                    "Developer Portal (${submissions.size})"
                                }
                            )
                        }

                        FilledTonalButton(
                            onClick = { showVersionsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_version_history_button")
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Versions & SHA-256")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Review Moderation, Security Reports & Store Announcements
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val flaggedCount = allReviews.count { it.flagged }
                        FilledTonalButton(
                            onClick = { showModerationDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_review_moderation_button")
                        ) {
                            Text(
                                text = if (flaggedCount > 0) {
                                    "Reviews ($flaggedCount Flag)"
                                } else {
                                    "Reviews (${allReviews.size})"
                                }
                            )
                        }

                        FilledTonalButton(
                            onClick = { showReportsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_reports_button")
                        ) {
                            Text(
                                text = if (openReportsCount > 0) {
                                    "Reports ($openReportsCount Open)"
                                } else {
                                    "Reports (${reports.size})"
                                }
                            )
                        }

                        FilledTonalButton(
                            onClick = { showNotificationDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_broadcast_notification_button")
                        ) {
                            Text("Alerts (${notifications.size})")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 4: Users/Developers/RBAC Roles & Analytics/Audit Logs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showUsersRolesDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_users_roles_button")
                        ) {
                            Icon(imageVector = Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Developers & Roles")
                        }

                        FilledTonalButton(
                            onClick = { showAnalyticsAuditDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_analytics_audit_button")
                        ) {
                            Icon(imageVector = Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Analytics & Audit (${auditLogs.size})")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onSeedOfficialCatalog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync / Restore Default Zayaan Ecosystem Apps")
                    }
                }
            }
        }

        // 2. Centralized GitHub Releases & Multi-Platform Sync Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Real-Time Multi-Platform & GitHub Releases Sync",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Every application release, multi-version changelog, curated collection, and SHA-256 checksum saved here synchronizes in real time across the Android APK Store and https://zayaanapps.github.io/zayaan-apps/.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Managed Applications Catalog List
        item {
            Text(
                text = "Managed Store Catalog (${apps.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(
            items = apps,
            key = { "admin_${it.appId.ifBlank { it.docId }}" }
        ) { app ->
            val appVersionsCount = versionHistory.count { it.appId == (app.appId.ifBlank { app.docId }) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppEmblemAvatar(app = app, sizeDp = 52)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (app.published) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (app.published) "PUBLISHED" else "DRAFT",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${app.packageName} • v${app.versionName} (code ${app.versionCode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${app.category} • ${app.apkFileSize} • ${app.downloadCount} DLs • ${app.pageViews} views" +
                                    if (appVersionsCount > 0) " • $appVersionsCount versions" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (app.sha256Checksum.isNotBlank()) {
                                Text(
                                    text = "SHA-256: ${app.sha256Checksum.take(20)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Curation & Publishing Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = app.featured,
                            onClick = { onToggleFeatured(app) },
                            label = { Text("Featured") },
                            modifier = Modifier.testTag("admin_toggle_featured_${app.appId}")
                        )
                        FilterChip(
                            selected = app.editorsChoice,
                            onClick = { onToggleEditorsChoice(app) },
                            label = { Text("Editor's Choice") }
                        )
                        FilterChip(
                            selected = app.essential,
                            onClick = { onToggleEssential(app) },
                            label = { Text("Essential") }
                        )
                        FilterChip(
                            selected = app.published,
                            onClick = { onTogglePublished(app) },
                            label = { Text(if (app.published) "Published" else "Draft") },
                            modifier = Modifier.testTag("admin_toggle_publish_${app.appId}")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                editingApp = app
                                isCreatingNew = false
                            },
                            modifier = Modifier.testTag("admin_edit_app_${app.appId}")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit / Bump Version")
                        }

                        IconButton(
                            onClick = { onDeleteApp(app) },
                            modifier = Modifier.testTag("admin_delete_app_${app.appId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete App",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingApp != null) {
        AppEditorDialog(
            initialApp = editingApp!!,
            isNew = isCreatingNew,
            onDismiss = { editingApp = null },
            onSave = { updatedApp ->
                onSaveApp(updatedApp, !isCreatingNew)
                editingApp = null
            }
        )
    }

    if (showCategoryDialog) {
        val cats = (categoriesState as? UiState.Success)?.data ?: emptyList()
        CategoryManagerDialog(
            categories = cats,
            onDismiss = { showCategoryDialog = false },
            onAddCategory = onAddCategory,
            onDeleteCategory = onDeleteCategory
        )
    }

    if (showModerationDialog) {
        ReviewModerationDialog(
            reviews = allReviews,
            onDismiss = { showModerationDialog = false },
            onReply = onReplyToReview,
            onClearFlag = onClearReviewFlag,
            onDelete = onDeleteReview
        )
    }

    if (showNotificationDialog) {
        NotificationBroadcastDialog(
            notifications = notifications,
            apps = apps,
            onDismiss = { showNotificationDialog = false },
            onBroadcast = onBroadcastNotification,
            onDelete = onDeleteNotification
        )
    }

    if (showCollectionsDialog) {
        CollectionsAndBannersManagerDialog(
            collections = collections,
            apps = apps,
            onDismiss = { showCollectionsDialog = false },
            onSaveCollection = onSaveCollection,
            onDeleteCollection = onDeleteCollection
        )
    }

    if (showSubmissionsDialog) {
        DeveloperPortalAndSubmissionsDialog(
            submissions = submissions,
            isTrustedAdmin = isTrustedAdmin,
            currentUserEmail = currentUserEmail,
            onDismiss = { showSubmissionsDialog = false },
            onSubmitNewApp = onSubmitDeveloperApp,
            onReviewSubmission = onReviewDeveloperSubmission,
            onDeleteSubmission = onDeleteDeveloperSubmission
        )
    }

    if (showVersionsDialog) {
        VersionHistoryAndChecksumDialog(
            apps = apps,
            versionHistory = versionHistory,
            cachedApkVerifications = cachedApkVerifications,
            onDismiss = { showVersionsDialog = false },
            onAddVersionRecord = onAddVersionRecord,
            onPromoteVersionToCatalog = { app, ver ->
                onSaveApp(
                    app.copy(
                        versionName = ver.versionName,
                        versionCode = ver.versionCode,
                        apkDownloadUrl = ver.apkDownloadUrl,
                        apkFileSize = ver.apkFileSize,
                        sha256Checksum = ver.sha256Checksum,
                        releaseNotes = ver.changelog
                    ),
                    true
                )
            },
            onDeleteVersionRecord = onDeleteVersionRecord
        )
    }

    if (showReportsDialog) {
        ReportsAndSecurityModerationDialog(
            reports = reports,
            apps = apps,
            isTrustedAdmin = isTrustedAdmin,
            onDismiss = { showReportsDialog = false },
            onSubmitReport = onSubmitReport,
            onUpdateReportStatus = onUpdateReportStatus,
            onDeleteReport = onDeleteReport
        )
    }

    if (showUsersRolesDialog) {
        UsersDevelopersAndRolesDialog(
            apps = apps,
            adminRecords = adminRecords,
            currentUserId = currentUserId,
            currentUserEmail = currentUserEmail,
            isTrustedAdmin = isTrustedAdmin,
            onDismiss = { showUsersRolesDialog = false },
            onGrantAdminRole = onGrantAdminRole,
            onRevokeAdminRole = onRevokeAdminRole
        )
    }

    if (showAnalyticsAuditDialog) {
        AuditLogsAndAnalyticsDialog(
            apps = apps,
            reviews = allReviews,
            auditLogs = auditLogs,
            onDismiss = { showAnalyticsAuditDialog = false }
        )
    }
}

@Composable
private fun AppEmblemAvatar(app: StoreApp, sizeDp: Int) {
    Surface(
        modifier = Modifier.size(sizeDp.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (app.iconUrl.isNotBlank()) {
            coil.compose.AsyncImage(
                model = app.iconUrl,
                contentDescription = "${app.appName} icon",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AdminMetricBox(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppEditorDialog(
    initialApp: StoreApp,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (StoreApp) -> Unit
) {
    var appId by remember { mutableStateOf(initialApp.appId) }
    var appName by remember { mutableStateOf(initialApp.appName) }
    var packageName by remember { mutableStateOf(initialApp.packageName) }
    var category by remember { mutableStateOf(initialApp.category) }
    var developerName by remember { mutableStateOf(initialApp.developerName) }
    var shortDescription by remember { mutableStateOf(initialApp.shortDescription) }
    var versionName by remember { mutableStateOf(initialApp.versionName) }
    var versionCodeText by remember { mutableStateOf(initialApp.versionCode.toString()) }
    var apkFileSize by remember { mutableStateOf(initialApp.apkFileSize) }
    var apkDownloadUrl by remember { mutableStateOf(initialApp.apkDownloadUrl) }
    var sha256Checksum by remember { mutableStateOf(initialApp.sha256Checksum) }
    var iconUrl by remember { mutableStateOf(initialApp.iconUrl) }
    var screenshotsText by remember { mutableStateOf(initialApp.screenshots.joinToString(", ")) }
    var description by remember { mutableStateOf(initialApp.description) }
    var releaseNotes by remember { mutableStateOf(initialApp.releaseNotes) }
    var supportEmail by remember { mutableStateOf(initialApp.supportEmail) }
    var githubRepoUrl by remember { mutableStateOf(initialApp.githubRepoUrl) }
    var privacyPolicyUrl by remember { mutableStateOf(initialApp.privacyPolicyUrl) }
    var featured by remember { mutableStateOf(initialApp.featured) }
    var editorsChoice by remember { mutableStateOf(initialApp.editorsChoice) }
    var essential by remember { mutableStateOf(initialApp.essential) }
    var published by remember { mutableStateOf(initialApp.published) }
    var offlineCapable by remember { mutableStateOf(initialApp.offlineCapable) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "Publish New Application" else "Update '${initialApp.appName}' & Release Version")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = {
                        appName = it
                        if (isNew && appId.isBlank()) {
                            appId = it.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                            packageName = "com.zayaanapps.${appId.replace("-", "")}"
                        }
                    },
                    label = { Text("App Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_app_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = appId,
                        onValueChange = { if (isNew) appId = it },
                        enabled = isNew,
                        label = { Text("App ID (slug)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(STORE_CATEGORIES) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("Version Name (1.0.0.1)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = versionCodeText,
                        onValueChange = { versionCodeText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Version Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = developerName,
                        onValueChange = { developerName = it },
                        label = { Text("Developer / Studio") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = apkFileSize,
                        onValueChange = { apkFileSize = it },
                        label = { Text("APK Size") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = apkDownloadUrl,
                    onValueChange = { apkDownloadUrl = it },
                    label = { Text("GitHub Releases APK Download URL *") },
                    placeholder = { Text("https://github.com/zayaanapps/.../releases/download/v1.0.0.1/app-release.apk") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sha256Checksum,
                    onValueChange = { sha256Checksum = it.trim() },
                    label = { Text("APK SHA-256 Checksum (optional hex digest)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shortDescription,
                    onValueChange = { shortDescription = it },
                    label = { Text("Short Tagline / Summary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = iconUrl,
                    onValueChange = { iconUrl = it },
                    label = { Text("Icon URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = screenshotsText,
                    onValueChange = { screenshotsText = it },
                    label = { Text("Screenshot URLs (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Application Description *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("Release Notes (What's New in v$versionName)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = supportEmail,
                        onValueChange = { supportEmail = it },
                        label = { Text("Support Email") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = githubRepoUrl,
                        onValueChange = { githubRepoUrl = it },
                        label = { Text("GitHub Repo URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = privacyPolicyUrl,
                    onValueChange = { privacyPolicyUrl = it },
                    label = { Text("Privacy Policy URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured")
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(checked = editorsChoice, onCheckedChange = { editorsChoice = it })
                    Text("Editor's Choice")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = essential, onCheckedChange = { essential = it })
                    Text("Essential")
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(checked = offlineCapable, onCheckedChange = { offlineCapable = it })
                    Text("Offline Ready")
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(checked = published, onCheckedChange = { published = it })
                    Text("Published")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedCode = versionCodeText.toLongOrNull() ?: 1L
                    val parsedScreenshots = screenshotsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .take(10)
                    onSave(
                        initialApp.copy(
                            appId = appId.ifBlank { appName.lowercase().replace(Regex("[^a-z0-9]+"), "-") },
                            appName = appName.ifBlank { "Zayaan App" },
                            packageName = packageName.ifBlank { "com.zayaanapps.custom" },
                            category = category,
                            developerName = developerName.ifBlank { "Zayaan Apps Studio" },
                            shortDescription = shortDescription,
                            versionName = versionName.ifBlank { "1.0.0.1" },
                            versionCode = parsedCode,
                            apkFileSize = apkFileSize.ifBlank { "12.0 MB" },
                            apkDownloadUrl = apkDownloadUrl.ifBlank {
                                "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk"
                            },
                            sha256Checksum = sha256Checksum,
                            iconUrl = iconUrl,
                            screenshots = parsedScreenshots,
                            description = description.ifBlank { "Official Zayaan Apps distribution package." },
                            releaseNotes = releaseNotes,
                            supportEmail = supportEmail.ifBlank { "zayaanapps@gmail.com" },
                            githubRepoUrl = githubRepoUrl.ifBlank { "https://github.com/zayaanapps/zayaan-apps" },
                            privacyPolicyUrl = privacyPolicyUrl.ifBlank { "https://zayaanapps.github.io/zayaan-apps/" },
                            featured = featured,
                            editorsChoice = editorsChoice,
                            essential = essential,
                            offlineCapable = offlineCapable,
                            published = published
                        )
                    )
                },
                modifier = Modifier.testTag("editor_save_button")
            ) {
                Text("Save & Log Version")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CategoryManagerDialog(
    categories: List<StoreCategory>,
    onDismiss: () -> Unit,
    onAddCategory: (String, String, Long) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var newCatDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Store Categories") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newCatDesc,
                    onValueChange = { newCatDesc = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onAddCategory(newCatName, newCatDesc, categories.size.toLong())
                            newCatName = ""
                            newCatDesc = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Category")
                }

                Spacer(modifier = Modifier.height(8.dp))

                categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                cat.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteCategory(cat.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Category",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun ReviewModerationDialog(
    reviews: List<AppReview>,
    onDismiss: () -> Unit,
    onReply: (AppReview, String) -> Unit,
    onClearFlag: (AppReview) -> Unit,
    onDelete: (AppReview) -> Unit
) {
    var replyingReviewId by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Community Review Moderation (${reviews.size})") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (reviews.isEmpty()) {
                    Text(
                        text = "No community reviews have been submitted yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    reviews.forEach { rev ->
                        Surface(
                            color = if (rev.flagged) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${rev.appName} • ${rev.userName} (${rev.rating}★)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = rev.comment,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (rev.developerReply.isNotBlank()) {
                                    Text(
                                        text = "Developer Reply: ${rev.developerReply}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (replyingReviewId == rev.id) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        label = { Text("Official Developer Reply") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            onReply(rev, replyText)
                                            replyingReviewId = null
                                            replyText = ""
                                        }
                                    ) {
                                        Text("Post Reply")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = {
                                            replyingReviewId = rev.id
                                            replyText = rev.developerReply
                                        }
                                    ) {
                                        Text("Reply")
                                    }
                                    if (rev.flagged) {
                                        TextButton(onClick = { onClearFlag(rev) }) {
                                            Text("Approve / Unflag")
                                        }
                                    }
                                    TextButton(onClick = { onDelete(rev) }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun NotificationBroadcastDialog(
    notifications: List<StoreNotification>,
    apps: List<StoreApp>,
    onDismiss: () -> Unit,
    onBroadcast: (String, String, String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("NEW_RELEASE") }
    var targetAppId by remember { mutableStateOf(apps.firstOrNull()?.appId ?: "") }
    val notifCategories = listOf("NEW_RELEASE", "APP_UPDATE", "FEATURED", "ANNOUNCEMENT", "SECURITY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Release & Store Notifications") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Headline (e.g. Zayaan AI v2.4 Released)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Announcement Details") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(notifCategories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = targetAppId,
                    onValueChange = { targetAppId = it },
                    label = { Text("Target App ID (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onBroadcast(title, body, category, targetAppId)
                            title = ""
                            body = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Broadcast Notification")
                }

                Spacer(modifier = Modifier.height(6.dp))
                notifications.forEach { n ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${n.category}: ${n.title}", style = MaterialTheme.typography.labelLarge)
                            Text(n.body, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDelete(n.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete notification",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
