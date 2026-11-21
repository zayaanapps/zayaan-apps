package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Compact, Theme-Adaptive Admin Header & Action Console
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.zayaan_logo_badge),
                            contentDescription = "Zayaan Apps Logo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "Zayaan Apps Admin Console",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentUserEmail ?: "Open Store Mode"} • ${if (isTrustedAdmin) "RBAC Verified" else "Developer Portal"}",
                                fontSize = 11.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Compact bordered 5-metric telemetry strip
                    Surface(
                        color = cs.surface.copy(alpha = if (isDark) 0.65f else 0.90f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AdminCompactMetric("APPS", "${apps.size}")
                            VerticalDivider(modifier = Modifier.height(18.dp), color = cs.outline.copy(alpha = 0.22f))
                            AdminCompactMetric("LIVE", "$publishedCount")
                            VerticalDivider(modifier = Modifier.height(18.dp), color = cs.outline.copy(alpha = 0.22f))
                            AdminCompactMetric("FEAT", "$featuredCount")
                            VerticalDivider(modifier = Modifier.height(18.dp), color = cs.outline.copy(alpha = 0.22f))
                            AdminCompactMetric("DLs", "$totalDownloads")
                            VerticalDivider(modifier = Modifier.height(18.dp), color = cs.outline.copy(alpha = 0.22f))
                            AdminCompactMetric("VIEWS", "$totalPageViews")
                        }
                    }

                    // Row 1: Primary App CRUD, Categories, Collections & Banners
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                editingApp = StoreApp()
                                isCreatingNew = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .height(34.dp)
                                .testTag("admin_add_app_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Publish", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        AdminActionPillButton(
                            label = "Categories",
                            icon = Icons.Default.Category,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_manage_categories_button"),
                            onClick = { showCategoryDialog = true }
                        )

                        AdminActionPillButton(
                            label = "Banners (${collections.size})",
                            icon = Icons.Default.CollectionsBookmark,
                            modifier = Modifier
                                .weight(1.05f)
                                .testTag("admin_manage_collections_button"),
                            onClick = { showCollectionsDialog = true }
                        )
                    }

                    // Row 2: Developer Submissions Queue & Multi-Version / SHA-256 Checksum Manager
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AdminActionPillButton(
                            label = if (pendingSubmissionsCount > 0) {
                                "Submissions ($pendingSubmissionsCount)"
                            } else {
                                "Portal (${submissions.size})"
                            },
                            icon = Icons.Default.Publish,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_submissions_button"),
                            onClick = { showSubmissionsDialog = true }
                        )

                        AdminActionPillButton(
                            label = "Versions & SHA-256",
                            icon = Icons.Default.Security,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_version_history_button"),
                            onClick = { showVersionsDialog = true }
                        )
                    }

                    // Row 3: Review Moderation, Security Reports & Store Announcements
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val flaggedCount = allReviews.count { it.flagged }
                        AdminActionPillButton(
                            label = if (flaggedCount > 0) "Reviews ($flaggedCount)" else "Reviews (${allReviews.size})",
                            icon = Icons.Default.RateReview,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_review_moderation_button"),
                            onClick = { showModerationDialog = true }
                        )

                        AdminActionPillButton(
                            label = if (openReportsCount > 0) "Reports ($openReportsCount)" else "Reports (${reports.size})",
                            icon = Icons.Default.Report,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_reports_button"),
                            onClick = { showReportsDialog = true }
                        )

                        AdminActionPillButton(
                            label = "Alerts (${notifications.size})",
                            icon = Icons.Default.Notifications,
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("admin_broadcast_notification_button"),
                            onClick = { showNotificationDialog = true }
                        )
                    }

                    // Row 4: Users/Developers/RBAC Roles & Analytics/Audit Logs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AdminActionPillButton(
                            label = "Developers & Roles",
                            icon = Icons.Default.People,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_users_roles_button"),
                            onClick = { showUsersRolesDialog = true }
                        )

                        AdminActionPillButton(
                            label = "Analytics (${auditLogs.size})",
                            icon = Icons.Default.Analytics,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_analytics_audit_button"),
                            onClick = { showAnalyticsAuditDialog = true }
                        )
                    }

                    OutlinedButton(
                        onClick = onSeedOfficialCatalog,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sync / Restore Default Zayaan Ecosystem Apps",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Centralized GitHub Releases & Multi-Platform Sync Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.26f)),
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
                        color = cs.primaryContainer,
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.28f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real-Time GitHub Releases & Cloud Sync",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Releases, changelogs, collections & SHA-256 sync instantly across Android & Web.",
                            fontSize = 11.sp,
                            lineHeight = 14.5.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Managed Applications Catalog List Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = cs.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.28f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier
                            .padding(5.dp)
                            .size(15.dp)
                    )
                }
                Text(
                    text = "Managed Store Catalog",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = cs.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.22f))
                ) {
                    Text(
                        text = "${apps.size}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }

        items(
            items = apps,
            key = { "admin_${it.appId.ifBlank { it.docId }}" }
        ) { app ->
            val appVersionsCount = versionHistory.count { it.appId == (app.appId.ifBlank { app.docId }) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.26f)),
                colors = CardDefaults.cardColors(containerColor = cs.surface)
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppEmblemAvatar(app = app, sizeDp = 44)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Surface(
                                    color = if (app.published) cs.primaryContainer else cs.errorContainer,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (app.published) cs.primary.copy(alpha = 0.28f) else cs.error.copy(alpha = 0.28f)
                                    )
                                ) {
                                    Text(
                                        text = if (app.published) "PUBLISHED" else "DRAFT",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (app.published) cs.onPrimaryContainer else cs.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${app.packageName} • v${app.versionName} (${app.versionCode})",
                                fontSize = 11.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${app.category} • ${app.apkFileSize} • ${app.downloadCount} DLs • ${app.pageViews} views" +
                                    if (appVersionsCount > 0) " • $appVersionsCount vers" else "",
                                fontSize = 10.5.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (app.sha256Checksum.isNotBlank()) {
                                Text(
                                    text = "SHA-256: ${app.sha256Checksum.take(20)}...",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = cs.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = cs.outline.copy(alpha = 0.18f))

                    // Compact bordered curation & publishing pills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        AdminTogglePill(
                            label = "Featured",
                            selected = app.featured,
                            icon = if (app.featured) Icons.Default.Star else Icons.Default.StarBorder,
                            modifier = Modifier.testTag("admin_toggle_featured_${app.appId}"),
                            onClick = { onToggleFeatured(app) }
                        )
                        AdminTogglePill(
                            label = "Editor's",
                            selected = app.editorsChoice,
                            icon = Icons.Default.WorkspacePremium,
                            onClick = { onToggleEditorsChoice(app) }
                        )
                        AdminTogglePill(
                            label = "Essential",
                            selected = app.essential,
                            icon = Icons.Default.CheckCircle,
                            onClick = { onToggleEssential(app) }
                        )
                        AdminTogglePill(
                            label = if (app.published) "Published" else "Draft",
                            selected = app.published,
                            icon = if (app.published) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            modifier = Modifier.testTag("admin_toggle_publish_${app.appId}"),
                            onClick = { onTogglePublished(app) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingApp = app
                                isCreatingNew = false
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(9.dp),
                            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.38f)),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("admin_edit_app_${app.appId}")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit / Bump Version", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = cs.errorContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(9.dp),
                            border = BorderStroke(1.dp, cs.error.copy(alpha = 0.28f)),
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { onDeleteApp(app) }
                                .testTag("admin_delete_app_${app.appId}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete App",
                                    tint = cs.error,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
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
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .size(sizeDp.dp)
            .border(1.dp, cs.outline.copy(alpha = 0.26f), RoundedCornerShape(11.dp)),
        shape = RoundedCornerShape(11.dp),
        color = cs.primaryContainer
    ) {
        if (app.iconUrl.isNotBlank() || app.packageName == "com.zayaanapps.store") {
            val emblemModel: Any = if (app.packageName == "com.zayaanapps.store") {
                com.example.R.drawable.zayaan_logo_badge
            } else {
                app.iconUrl
            }
            coil.compose.AsyncImage(
                model = emblemModel,
                contentDescription = "${app.appName} icon",
                placeholder = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.zayaan_logo_badge),
                error = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.zayaan_logo_badge),
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AdminCompactMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun AdminActionPillButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = cs.surface,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.28f)),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminTogglePill(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = if (selected) cs.primaryContainer else cs.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) cs.primary.copy(alpha = 0.45f) else cs.outline.copy(alpha = 0.26f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    var appIdManuallyEdited by remember { mutableStateOf(!isNew && initialApp.appId.isNotBlank()) }
    var packageManuallyEdited by remember { mutableStateOf(!isNew && initialApp.packageName.isNotBlank()) }
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
    var showOptionalDetails by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val cs = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isNew) "Publish New Android App" else "Update '${initialApp.appName}'",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isNew) {
                        "Just enter the essentials below — extra metadata uses smart defaults."
                    } else {
                        "Update release details or expand optional metadata."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = githubRepoUrl,
                    onValueChange = { githubRepoUrl = it },
                    label = { Text("GitHub Repo or Release URL *") },
                    placeholder = { Text("https://github.com/owner/repo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = {
                            val url = githubRepoUrl.trim()
                            if (url.isNotBlank()) {
                                validationError = "Fetching release info from GitHub..."
                            } else {
                                validationError = "Please enter a GitHub repo or release URL first."
                            }
                        }) {
                            Text("Fetch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )

                OutlinedTextField(
                    value = appName,
                    onValueChange = { next ->
                        appName = next
                        validationError = null
                        if (isNew) {
                            val slug = next.lowercase()
                                .replace(Regex("[^a-z0-9]+"), "-")
                                .trim('-')
                            if (!appIdManuallyEdited) {
                                appId = slug
                            }
                            if (!packageManuallyEdited) {
                                val pkgSuffix = slug.replace("-", "")
                                packageName = if (pkgSuffix.isNotEmpty()) {
                                    "com.zayaanapps.$pkgSuffix"
                                } else {
                                    ""
                                }
                            }
                        }
                    },
                    label = { Text("App Name *") },
                    placeholder = { Text("e.g. Zayaan Notes") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_app_name_input")
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = {
                        packageName = it
                        packageManuallyEdited = true
                    },
                    label = { Text("Android Package Name *") },
                    placeholder = { Text("com.zayaanapps.myapp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(STORE_CATEGORIES) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("Version") },
                        placeholder = { Text("1.0.0.1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = apkFileSize,
                        onValueChange = { apkFileSize = it },
                        label = { Text("APK Size") },
                        placeholder = { Text("12.0 MB") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = apkDownloadUrl,
                    onValueChange = {
                        apkDownloadUrl = it
                        validationError = null
                    },
                    label = { Text("APK Download URL *") },
                    placeholder = { Text("https://github.com/.../releases/download/v1.0/app.apk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description (optional)") },
                    placeholder = { Text("What does this app do?") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = cs.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { published = !published }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Publish Live in Store",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (published) "Visible to all users immediately" else "Saved as hidden draft",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = published,
                            onCheckedChange = { published = it }
                        )
                    }
                }

                // Collapsible Optional Details & Badges Section
                Surface(
                    color = cs.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.26f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOptionalDetails = !showOptionalDetails }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "More Details & Badges (Optional)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.primary
                                )
                                Text(
                                    text = "Release notes, icon URL, screenshots, SHA-256 & featured flags",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (showOptionalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showOptionalDetails) "Collapse optional details" else "Expand optional details",
                                tint = cs.primary
                            )
                        }

                        AnimatedVisibility(visible = showOptionalDetails) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = cs.outline.copy(alpha = 0.18f))

                                OutlinedTextField(
                                    value = releaseNotes,
                                    onValueChange = { releaseNotes = it },
                                    label = { Text("What's New / Release Notes") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = appId,
                                        onValueChange = {
                                            if (isNew) {
                                                appId = it
                                                appIdManuallyEdited = true
                                            }
                                        },
                                        enabled = isNew,
                                        label = { Text("App ID (slug)") },
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

                                OutlinedTextField(
                                    value = developerName,
                                    onValueChange = { developerName = it },
                                    label = { Text("Developer / Studio") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = shortDescription,
                                    onValueChange = { shortDescription = it },
                                    label = { Text("Short Tagline (auto from description if blank)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = iconUrl,
                                    onValueChange = { iconUrl = it },
                                    label = { Text("App Icon Image URL") },
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
                                    value = sha256Checksum,
                                    onValueChange = { sha256Checksum = it.trim() },
                                    label = { Text("APK SHA-256 Checksum") },
                                    singleLine = true,
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

                                Text(
                                    text = "Storefront Badges",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cs.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = featured,
                                        onClick = { featured = !featured },
                                        label = { Text("Featured") }
                                    )
                                    FilterChip(
                                        selected = editorsChoice,
                                        onClick = { editorsChoice = !editorsChoice },
                                        label = { Text("Editor's Choice") }
                                    )
                                    FilterChip(
                                        selected = essential,
                                        onClick = { essential = !essential },
                                        label = { Text("Essential") }
                                    )
                                    FilterChip(
                                        selected = offlineCapable,
                                        onClick = { offlineCapable = !offlineCapable },
                                        label = { Text("Offline Ready") }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = appName.trim()
                    if (cleanName.isBlank()) {
                        validationError = "Please enter an App Name."
                        return@Button
                    }
                    val cleanUrl = apkDownloadUrl.trim()
                    if (cleanUrl.isBlank()) {
                        validationError = "Please paste the APK Download URL."
                        return@Button
                    }
                    val sanitizedSlug = appId
                        .ifBlank { cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "-") }
                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "-")
                        .trim('-')
                        .ifBlank { "zayaan-app-${System.currentTimeMillis()}" }
                    val cleanVersion = versionName.trim().ifBlank { "1.0.0.1" }
                    val parsedCode = versionCodeText.toLongOrNull()?.coerceAtLeast(1L)
                        ?: cleanVersion.split('.').lastOrNull()?.toLongOrNull()?.coerceAtLeast(1L)
                        ?: 1L
                    val cleanDesc = description.trim()
                        .ifBlank { "$cleanName — Official Android release published on Zayaan Apps." }
                    val cleanShort = shortDescription.trim()
                        .ifBlank { cleanDesc.lineSequence().firstOrNull().orEmpty().take(180) }
                    val cleanPkg = packageName.trim()
                        .ifBlank { "com.zayaanapps.${sanitizedSlug.replace("-", "")}" }
                    val parsedScreenshots = screenshotsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .take(10)
                    onSave(
                        initialApp.copy(
                            appId = sanitizedSlug,
                            appName = cleanName,
                            packageName = cleanPkg,
                            category = category,
                            developerName = developerName.trim().ifBlank { "Zayaan Apps Studio" },
                            shortDescription = cleanShort,
                            versionName = cleanVersion,
                            versionCode = parsedCode,
                            apkFileSize = apkFileSize.trim().ifBlank { "12.0 MB" },
                            apkDownloadUrl = cleanUrl,
                            sha256Checksum = sha256Checksum.trim(),
                            iconUrl = iconUrl.trim(),
                            screenshots = parsedScreenshots,
                            description = cleanDesc,
                            releaseNotes = releaseNotes.trim().ifBlank {
                                if (isNew) "Initial release v$cleanVersion." else "Updated release v$cleanVersion."
                            },
                            supportEmail = supportEmail.trim().ifBlank { "zayaanapps@gmail.com" },
                            githubRepoUrl = githubRepoUrl.trim().ifBlank { "https://github.com/zayaanapps/zayaan-apps" },
                            privacyPolicyUrl = privacyPolicyUrl.trim().ifBlank { "https://zayaanapps.github.io/zayaan-apps/" },
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
                Text(if (isNew) "Publish App" else "Save Update")
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
