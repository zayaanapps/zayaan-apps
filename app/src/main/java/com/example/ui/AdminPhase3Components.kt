package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.DeveloperProfileSummary
import com.example.data.DeveloperSubmission
import com.example.data.STORE_CATEGORIES
import com.example.data.StoreApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeveloperPortalAndSubmissionsDialog(
    submissions: List<DeveloperSubmission>,
    isTrustedAdmin: Boolean,
    currentUserEmail: String?,
    onDismiss: () -> Unit,
    onSubmitNewApp: (DeveloperSubmission) -> Unit,
    onReviewSubmission: (DeveloperSubmission, String, String) -> Unit,
    onDeleteSubmission: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(if (submissions.isNotEmpty() && isTrustedAdmin) "QUEUE" else "SUBMIT") }
    var appName by remember { mutableStateOf("") }
    var appId by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var appIdManuallyEdited by remember { mutableStateOf(false) }
    var packageManuallyEdited by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Tools") }
    var developerName by remember {
        mutableStateOf(
            currentUserEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Zayaan Developer"
        )
    }
    var versionName by remember { mutableStateOf("1.0.0.1") }
    var versionCodeText by remember { mutableStateOf("1") }
    var apkFileSize by remember { mutableStateOf("12.0 MB") }
    var apkDownloadUrl by remember { mutableStateOf("") }
    var sha256Checksum by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("Initial Developer Portal submission.") }
    var supportEmail by remember { mutableStateOf(currentUserEmail ?: "zayaanapps@gmail.com") }
    var privacyPolicyUrl by remember { mutableStateOf("https://zayaanapps.github.io/zayaan-apps/") }
    var showOptionalFields by remember { mutableStateOf(false) }
    var reviewNotesById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Developer Portal & Submission Workflow")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeTab == "SUBMIT",
                        onClick = { activeTab = "SUBMIT" },
                        label = { Text("Submit Application") }
                    )
                    FilterChip(
                        selected = activeTab == "QUEUE",
                        onClick = { activeTab = "QUEUE" },
                        label = { Text("Approval Queue (${submissions.size})") }
                    )
                }

                if (activeTab == "SUBMIT") {
                    Text(
                        text = "Enter the essential app details below to submit for review. Extra fields use smart defaults.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { next ->
                            appName = next
                            val slug = next.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                            if (!appIdManuallyEdited) {
                                appId = slug
                            }
                            if (!packageManuallyEdited) {
                                val pkgSuffix = slug.replace("-", "")
                                packageName = if (pkgSuffix.isNotEmpty()) "com.zayaanapps.$pkgSuffix" else ""
                            }
                        },
                        label = { Text("Application Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                            label = { Text("Version (1.0.0.1)") },
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
                        label = { Text("APK Download URL *") },
                        placeholder = { Text("https://github.com/zayaanapps/.../releases/download/v1.0/app.apk") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Short Description (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showOptionalFields = !showOptionalFields }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Optional Details (SHA-256, Notes & Links)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (showOptionalFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = showOptionalFields) {
                                Column(
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = appId,
                                            onValueChange = {
                                                appId = it
                                                appIdManuallyEdited = true
                                            },
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
                                        value = sha256Checksum,
                                        onValueChange = { sha256Checksum = it.trim() },
                                        label = { Text("APK SHA-256 Checksum") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = releaseNotes,
                                        onValueChange = { releaseNotes = it },
                                        label = { Text("Release Notes / Changelog") },
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
                                            value = privacyPolicyUrl,
                                            onValueChange = { privacyPolicyUrl = it },
                                            label = { Text("Privacy Policy URL") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (appName.isNotBlank()) {
                                val slug = appId.ifBlank {
                                    appName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
                                }.ifBlank { "zayaan-sub-${System.currentTimeMillis()}" }
                                onSubmitNewApp(
                                    DeveloperSubmission(
                                        appId = slug,
                                        appName = appName.trim(),
                                        packageName = packageName.ifBlank { "com.zayaanapps.${slug.replace("-", "")}" },
                                        category = category,
                                        developerName = developerName.ifBlank { "Community Developer" },
                                        description = description.ifBlank { "Submitted via Zayaan Developer Portal." },
                                        versionName = versionName.ifBlank { "1.0.0.1" },
                                        versionCode = versionCodeText.toLongOrNull() ?: 1L,
                                        apkDownloadUrl = apkDownloadUrl.ifBlank {
                                            "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk"
                                        },
                                        apkFileSize = apkFileSize.ifBlank { "12.0 MB" },
                                        sha256Checksum = sha256Checksum,
                                        releaseNotes = releaseNotes,
                                        privacyPolicyUrl = privacyPolicyUrl,
                                        supportEmail = supportEmail,
                                        status = "PENDING"
                                    )
                                )
                                appName = ""
                                appId = ""
                                packageName = ""
                                apkDownloadUrl = ""
                                description = ""
                                appIdManuallyEdited = false
                                packageManuallyEdited = false
                                activeTab = "QUEUE"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_developer_app_button")
                    ) {
                        Text("Submit Application for Approval")
                    }
                } else {
                    if (submissions.isEmpty()) {
                        Text(
                            text = "No developer application submissions in the queue yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        submissions.forEach { sub ->
                            val statusColor = when (sub.status) {
                                "APPROVED" -> MaterialTheme.colorScheme.primaryContainer
                                "REJECTED" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${sub.appName} (v${sub.versionName} • code ${sub.versionCode})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = statusColor,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = sub.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${sub.packageName} • ${sub.category} • By ${sub.developerName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (sub.sha256Checksum.isNotBlank()) {
                                        Text(
                                            text = "SHA-256: ${sub.sha256Checksum.take(24)}...",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sub.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (sub.reviewNote.isNotBlank()) {
                                        Text(
                                            text = "Admin Review Note: ${sub.reviewNote}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (isTrustedAdmin) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val noteValue = reviewNotesById[sub.id] ?: sub.reviewNote
                                        OutlinedTextField(
                                            value = noteValue,
                                            onValueChange = {
                                                reviewNotesById = reviewNotesById + (sub.id to it)
                                            },
                                            label = { Text("Admin Verification / Approval Note") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    onReviewSubmission(
                                                        sub,
                                                        "APPROVED",
                                                        noteValue.ifBlank { "Verified & approved for Zayaan Apps Store." }
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Approve & Publish")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    onReviewSubmission(
                                                        sub,
                                                        "REJECTED",
                                                        noteValue.ifBlank { "Requires APK checksum or metadata updates." }
                                                    )
                                                }
                                            ) {
                                                Text("Reject")
                                            }
                                            IconButton(onClick = { onDeleteSubmission(sub.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete submission",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionsAndBannersManagerDialog(
    collections: List<AppCollection>,
    apps: List<StoreApp>,
    onDismiss: () -> Unit,
    onSaveCollection: (String, String, String, List<String>, Boolean, Boolean) -> Unit,
    onDeleteCollection: (String) -> Unit
) {
    var editingId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var selectedAppIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isPublic by remember { mutableStateOf(true) }
    var featured by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Curated Collections & Promotional Banners") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Collection / Banner Title *") },
                    placeholder = { Text("e.g. Must-Have AI & Developer Power Tools") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Promotional Subtitle") },
                    placeholder = { Text("Hand-picked releases verified for performance and offline support") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Select Applications (${selectedAppIds.size} selected)",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    apps.forEach { app ->
                        val id = app.appId.ifBlank { app.docId }
                        val isSelected = id in selectedAppIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedAppIds = if (isSelected) {
                                    selectedAppIds - id
                                } else {
                                    (selectedAppIds + id).distinct().take(30)
                                }
                            },
                            label = { Text(app.appName) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured Banner on Home")
                    Spacer(modifier = Modifier.width(10.dp))
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text("Public")
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveCollection(
                                editingId,
                                title,
                                subtitle,
                                selectedAppIds,
                                isPublic,
                                featured
                            )
                            editingId = ""
                            title = ""
                            subtitle = ""
                            selectedAppIds = emptyList()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingId.isBlank()) "Create Curated Collection" else "Update Collection")
                }

                HorizontalDivider()

                if (collections.isEmpty()) {
                    Text(
                        text = "No custom collections created yet. Create one above to highlight curated app bundles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    collections.forEach { col ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = col.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (col.subtitle.isNotBlank()) {
                                        Text(
                                            text = col.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${col.appIds.size} apps • ${if (col.featured) "Featured Banner" else "Standard"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        editingId = col.id
                                        title = col.title
                                        subtitle = col.subtitle
                                        selectedAppIds = col.appIds
                                        isPublic = col.isPublic
                                        featured = col.featured
                                    }
                                ) {
                                    Text("Edit")
                                }
                                IconButton(onClick = { onDeleteCollection(col.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete collection",
                                        tint = MaterialTheme.colorScheme.error
                                    )
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
fun VersionHistoryAndChecksumDialog(
    apps: List<StoreApp>,
    versionHistory: List<AppVersionRecord>,
    cachedApkVerifications: List<ApkArchiveVerificationInfo>,
    onDismiss: () -> Unit,
    onAddVersionRecord: (String, String, Long, String, String, String, String) -> Unit,
    onPromoteVersionToCatalog: (StoreApp, AppVersionRecord) -> Unit,
    onDeleteVersionRecord: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedAppId by remember {
        mutableStateOf(apps.firstOrNull()?.let { it.appId.ifBlank { it.docId } } ?: "zayaan-apps-store")
    }
    val selectedApp = apps.firstOrNull { (it.appId.ifBlank { it.docId }) == selectedAppId }
    var versionName by remember { mutableStateOf(selectedApp?.versionName ?: "1.0.0.2") }
    var versionCodeText by remember { mutableStateOf(((selectedApp?.versionCode ?: 1L) + 1L).toString()) }
    var apkFileSize by remember { mutableStateOf(selectedApp?.apkFileSize ?: "14.2 MB") }
    var apkDownloadUrl by remember {
        mutableStateOf(
            selectedApp?.apkDownloadUrl
                ?: "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk"
        )
    }
    var sha256Checksum by remember { mutableStateOf(selectedApp?.sha256Checksum ?: "") }
    var changelog by remember { mutableStateOf("") }

    val filteredHistory = versionHistory.filter { it.appId == selectedAppId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Multi-Version Releases & APK SHA-256 Verification") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Local APK Archive SHA-256 & Signing Certificate Inspector
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local APK Checksum & Signature Verifier",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (cachedApkVerifications.isEmpty()) {
                            Text(
                                text = "Download any APK from the store to automatically compute its SHA-256 file hash and verify its Android X.509 signing certificate fingerprint.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            cachedApkVerifications.forEach { info ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${info.fileName} (${info.packageName.ifBlank { "APK Archive" }})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    clipboard?.setPrimaryClip(ClipData.newPlainText("APK SHA-256", info.sha256Checksum))
                                                    sha256Checksum = info.sha256Checksum
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy & use SHA-256",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "v${info.versionName.ifBlank { "1.0" }} (code ${info.versionCode}) • ${info.fileSizeBytes / 1024} KB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "APK SHA-256: ${info.sha256Checksum}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (info.signingCertSha256.isNotBlank()) {
                                            Text(
                                                text = "Signing Cert SHA-256: ${info.signingCertSha256}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (info.isInstalledOnDevice) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (info.signatureMatchesInstalled) {
                                                        Icons.Default.Verified
                                                    } else {
                                                        Icons.Default.Warning
                                                    },
                                                    contentDescription = null,
                                                    tint = if (info.signatureMatchesInstalled) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.error
                                                    },
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (info.signatureMatchesInstalled) {
                                                        "Signature matches installed package (Upgrade-safe)"
                                                    } else {
                                                        "Warning: Signing certificate differs from installed package"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (info.signatureMatchesInstalled) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.error
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Select Application for Multi-Version Management
                Text(
                    text = "Select Application",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(apps) { app ->
                        val id = app.appId.ifBlank { app.docId }
                        FilterChip(
                            selected = selectedAppId == id,
                            onClick = {
                                selectedAppId = id
                                versionName = app.versionName
                                versionCodeText = (app.versionCode + 1L).toString()
                                apkFileSize = app.apkFileSize
                                apkDownloadUrl = app.apkDownloadUrl
                                sha256Checksum = app.sha256Checksum
                            },
                            label = { Text("${app.appName} (v${app.versionName})") }
                        )
                    }
                }

                // 3. Add New Version Release Record
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("Version Name") },
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
                        value = apkFileSize,
                        onValueChange = { apkFileSize = it },
                        label = { Text("APK Size") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sha256Checksum,
                        onValueChange = { sha256Checksum = it.trim() },
                        label = { Text("SHA-256 Checksum") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = apkDownloadUrl,
                    onValueChange = { apkDownloadUrl = it },
                    label = { Text("GitHub Releases APK URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = changelog,
                    onValueChange = { changelog = it },
                    label = { Text("Release Changelog / What's New") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (selectedAppId.isNotBlank() && versionName.isNotBlank()) {
                            onAddVersionRecord(
                                selectedAppId,
                                versionName,
                                versionCodeText.toLongOrNull() ?: 1L,
                                apkDownloadUrl,
                                apkFileSize,
                                sha256Checksum,
                                changelog.ifBlank { "Release v$versionName published via Zayaan Admin." }
                            )
                            changelog = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Record Version in Release History")
                }

                HorizontalDivider()

                Text(
                    text = "Release History for '$selectedAppId' (${filteredHistory.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (filteredHistory.isEmpty()) {
                    Text(
                        text = "No historical releases logged for this application yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    filteredHistory.forEach { ver ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "v${ver.versionName} (code ${ver.versionCode}) • ${ver.apkFileSize}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { onDeleteVersionRecord(ver.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete version record",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (ver.sha256Checksum.isNotBlank()) {
                                    Text(
                                        text = "SHA-256: ${ver.sha256Checksum}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (ver.changelog.isNotBlank()) {
                                    Text(
                                        text = ver.changelog,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (selectedApp != null && selectedApp.versionCode != ver.versionCode) {
                                    TextButton(
                                        onClick = { onPromoteVersionToCatalog(selectedApp, ver) }
                                    ) {
                                        Text("Set as Active Store Version (Rollback / Promote)")
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
fun ReportsAndSecurityModerationDialog(
    reports: List<AppReport>,
    apps: List<StoreApp>,
    isTrustedAdmin: Boolean,
    onDismiss: () -> Unit,
    onSubmitReport: (String, String, String, String) -> Unit,
    onUpdateReportStatus: (AppReport, String) -> Unit,
    onDeleteReport: (String) -> Unit
) {
    var targetId by remember { mutableStateOf(apps.firstOrNull()?.appId ?: "zayaan-apps-store") }
    var targetType by remember { mutableStateOf("APP") }
    var reason by remember { mutableStateOf("Broken Download Link / Checksum Mismatch") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Security & Content Reports (${reports.size})") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Submit or moderate security advisories, broken release asset links, or review abuse reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetType == "APP",
                        onClick = { targetType = "APP" },
                        label = { Text("Application") }
                    )
                    FilterChip(
                        selected = targetType == "REVIEW",
                        onClick = { targetType = "REVIEW" },
                        label = { Text("Community Review") }
                    )
                }
                OutlinedTextField(
                    value = targetId,
                    onValueChange = { targetId = it },
                    label = { Text("Target App ID or Review ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Additional Verification Details") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (targetId.isNotBlank() && reason.isNotBlank()) {
                            onSubmitReport(targetId, targetType, reason, details)
                            details = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Security / Moderation Report")
                }

                HorizontalDivider()

                if (reports.isEmpty()) {
                    Text(
                        text = "No open or historical reports found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    reports.forEach { rep ->
                        Surface(
                            color = when (rep.status) {
                                "OPEN" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                                "RESOLVED" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[${rep.targetType}] ${rep.targetId}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = rep.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = rep.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (rep.details.isNotBlank()) {
                                    Text(
                                        text = rep.details,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (isTrustedAdmin) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (rep.status != "RESOLVED") {
                                            TextButton(onClick = { onUpdateReportStatus(rep, "RESOLVED") }) {
                                                Text("Mark Resolved")
                                            }
                                        }
                                        if (rep.status != "DISMISSED") {
                                            TextButton(onClick = { onUpdateReportStatus(rep, "DISMISSED") }) {
                                                Text("Dismiss")
                                            }
                                        }
                                        TextButton(onClick = { onDeleteReport(rep.id) }) {
                                            Text("Delete", color = MaterialTheme.colorScheme.error)
                                        }
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
fun UsersDevelopersAndRolesDialog(
    apps: List<StoreApp>,
    adminRecords: List<AdminRecord>,
    currentUserId: String,
    currentUserEmail: String?,
    isTrustedAdmin: Boolean,
    onDismiss: () -> Unit,
    onGrantAdminRole: (String, String) -> Unit,
    onRevokeAdminRole: (String) -> Unit
) {
    var activeSection by remember { mutableStateOf("DEVELOPERS") }
    var targetUid by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("admin") }

    val developerProfiles: List<DeveloperProfileSummary> = remember(apps) {
        apps.groupBy { it.developerName.ifBlank { "Zayaan Apps Studio" } }
            .map { (devName, devApps) ->
                val first = devApps.first()
                val totalDl = devApps.sumOf { it.downloadCount }
                val totalRev = devApps.sumOf { it.reviewCount }
                val totalRatingSum = devApps.sumOf { it.ratingSum }
                val avgRating = if (totalRev > 0L) {
                    (totalRatingSum.toDouble() / totalRev.toDouble()).coerceIn(0.0, 5.0)
                } else 0.0
                DeveloperProfileSummary(
                    developerName = devName,
                    supportEmail = first.supportEmail,
                    websiteUrl = first.websiteUrl,
                    githubRepoUrl = first.githubRepoUrl,
                    privacyPolicyUrl = first.privacyPolicyUrl,
                    apps = devApps,
                    totalDownloads = totalDl,
                    totalReviews = totalRev,
                    averageRating = avgRating
                )
            }
            .sortedByDescending { it.totalDownloads }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Developers, Users & Administrator Roles") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeSection == "DEVELOPERS",
                        onClick = { activeSection = "DEVELOPERS" },
                        label = { Text("Developers (${developerProfiles.size})") }
                    )
                    FilterChip(
                        selected = activeSection == "ADMINS",
                        onClick = { activeSection = "ADMINS" },
                        label = { Text("Admin Roles (${adminRecords.size + 1})") }
                    )
                }

                if (activeSection == "DEVELOPERS") {
                    developerProfiles.forEach { profile ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.developerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${profile.apps.size} published app(s) • ${profile.totalDownloads} downloads" +
                                                if (profile.totalReviews > 0) " • ${"%.1f".format(profile.averageRating)}★" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Contact: ${profile.supportEmail} • GitHub: ${profile.githubRepoUrl}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Published Apps: ${profile.apps.joinToString(", ") { "${it.appName} (v${it.versionName})" }}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Current Session Identity",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Email: ${currentUserEmail ?: "Authenticated User"}\nUID: $currentUserId\nPrivilege Level: ${if (isTrustedAdmin) "Trusted Administrator (RBAC Active)" else "Standard User / Developer"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Bootstrapped Owner Record
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bootstrapped Store Owner (RBAC Verified)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Role: superadmin (Enforced by Firestore Rules email_verified check)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isTrustedAdmin) {
                        OutlinedTextField(
                            value = targetUid,
                            onValueChange = { targetUid = it.trim() },
                            label = { Text("Grant Admin Role to Firebase Auth UID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedRole == "admin",
                                onClick = { selectedRole = "admin" },
                                label = { Text("admin") }
                            )
                            FilterChip(
                                selected = selectedRole == "superadmin",
                                onClick = { selectedRole = "superadmin" },
                                label = { Text("superadmin") }
                            )
                        }
                        Button(
                            onClick = {
                                if (targetUid.isNotBlank()) {
                                    onGrantAdminRole(targetUid, selectedRole)
                                    targetUid = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant / Update Administrator Role")
                        }
                    }

                    adminRecords.forEach { rec ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "UID: ${rec.userId}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Role: ${rec.role}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isTrustedAdmin) {
                                    IconButton(onClick = { onRevokeAdminRole(rec.userId) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Revoke admin role",
                                            tint = MaterialTheme.colorScheme.error
                                        )
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
fun AuditLogsAndAnalyticsDialog(
    apps: List<StoreApp>,
    reviews: List<AppReview>,
    auditLogs: List<AuditLogRecord>,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf("ANALYTICS") }
    val totalDownloads = apps.sumOf { it.downloadCount }
    val totalPageViews = apps.sumOf { it.pageViews }
    val publishedCount = apps.count { it.published }
    val draftCount = apps.size - publishedCount
    val categoryBreakdown = remember(apps) {
        apps.groupBy { it.category }
            .mapValues { (_, list) -> list.size to list.sumOf { it.downloadCount } }
            .entries
            .sortedByDescending { it.value.second }
    }
    val topApps = remember(apps) {
        apps.sortedByDescending { it.downloadCount }.take(5)
    }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marketplace Analytics & Security Audit Logs") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = activeTab == "ANALYTICS",
                        onClick = { activeTab = "ANALYTICS" },
                        label = { Text("Download Analytics") }
                    )
                    FilterChip(
                        selected = activeTab == "AUDIT",
                        onClick = { activeTab = "AUDIT" },
                        label = { Text("Audit Logs (${auditLogs.size})") }
                    )
                }

                if (activeTab == "ANALYTICS") {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Store Telemetry Summary (Real Metrics Only)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Total Applications: ${apps.size} ($publishedCount Published, $draftCount Drafts)\n" +
                                    "• Total Verified APK Downloads: $totalDownloads\n" +
                                    "• Total App Detail Page Views: $totalPageViews\n" +
                                    "• Community Written Reviews: ${reviews.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Text(
                        text = "Top Downloaded Applications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    topApps.forEachIndexed { index, app ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${index + 1} ${app.appName} (v${app.versionName})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${app.category} • ${app.pageViews} page views",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${app.downloadCount} DLs",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "Category Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    categoryBreakdown.forEach { (cat, pair) ->
                        val (appCount, dlCount) = pair
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "$cat ($appCount apps)", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "$dlCount downloads",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    if (auditLogs.isEmpty()) {
                        Text(
                            text = "No administrative audit logs recorded yet. Every catalog edit, submission approval, review moderation, and role change is immutably logged here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        auditLogs.forEach { log ->
                            val formattedTime = log.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Just now"
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${log.action} [${log.targetType}]",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = formattedTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = log.summary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Target: ${log.targetId} • Actor: ${log.actorEmail.ifBlank { log.actorUid }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
fun DeveloperProfileDialog(
    developerName: String,
    allApps: List<StoreApp>,
    onDismiss: () -> Unit,
    onSelectApp: (StoreApp) -> Unit
) {
    val devApps = remember(developerName, allApps) {
        allApps.filter { it.developerName.equals(developerName, ignoreCase = true) }
    }
    val representative = devApps.firstOrNull()
    val totalDownloads = devApps.sumOf { it.downloadCount }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(developerName)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (representative != null) {
                    Text(
                        text = "Official Zayaan Apps Publisher • ${devApps.size} Published Application(s) • $totalDownloads Total Downloads",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Support Email: ${representative.supportEmail}\nWebsite: ${representative.websiteUrl}\nGitHub: ${representative.githubRepoUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                Text(
                    text = "Published Applications by $developerName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                devApps.forEach { app ->
                    Surface(
                        onClick = {
                            onDismiss()
                            onSelectApp(app)
                        },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${app.category} • v${app.versionName} • ${app.apkFileSize}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${app.downloadCount} DLs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
