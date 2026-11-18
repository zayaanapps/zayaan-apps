package com.example.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.STORE_CATEGORIES
import com.example.data.StoreApp
import com.example.data.StoreCategory

@Composable
fun AdminDashboardScreen(
    adminAppsState: UiState<List<StoreApp>>,
    categoriesState: UiState<List<StoreCategory>>,
    currentUserEmail: String?,
    onSaveApp: (StoreApp, Boolean) -> Unit,
    onTogglePublished: (StoreApp) -> Unit,
    onToggleFeatured: (StoreApp) -> Unit,
    onDeleteApp: (StoreApp) -> Unit,
    onAddCategory: (String, String, Long) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSeedOfficialCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingApp by remember { mutableStateOf<StoreApp?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val apps = (adminAppsState as? UiState.Success)?.data ?: emptyList()
    val totalDownloads = apps.sumOf { it.downloadCount }
    val publishedCount = apps.count { it.published }
    val featuredCount = apps.count { it.featured }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Header & Analytics Summary
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
                        Column {
                            Text(
                                text = "Zayaan Apps Admin Dashboard",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Signed in as: ${currentUserEmail ?: "Authenticated Administrator"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AdminMetricBox("TOTAL APPS", "${apps.size}")
                        AdminMetricBox("PUBLISHED", "$publishedCount")
                        AdminMetricBox("FEATURED", "$featuredCount")
                        AdminMetricBox("DOWNLOADS", "$totalDownloads")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                editingApp = StoreApp()
                                isCreatingNew = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_add_app_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Publish New App")
                        }

                        FilledTonalButton(
                            onClick = { showCategoryDialog = true },
                            modifier = Modifier.testTag("admin_manage_categories_button")
                        ) {
                            Icon(imageVector = Icons.Default.Category, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Categories")
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

        // Centralized Distribution Info
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
                            imageVector = Icons.Default.Publish,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Real-Time Multi-Platform Sync Active",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Any application added or updated here immediately appears in both the Android APK Store and your responsive website at https://zayaanapps.github.io/zayaan-apps/ without rebuilding Zayaan Apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Managed Applications List
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
                        AppEmblemAvatar(app = app, sizeDp = 50)
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
                                text = "${app.category} • ${app.apkFileSize} • ${app.downloadCount} downloads",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleFeatured(app) },
                            modifier = Modifier.testTag("admin_toggle_featured_${app.appId}")
                        ) {
                            Icon(
                                imageVector = if (app.featured) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Toggle Featured",
                                tint = if (app.featured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { onTogglePublished(app) },
                            modifier = Modifier.testTag("admin_toggle_publish_${app.appId}")
                        ) {
                            Icon(
                                imageVector = if (app.published) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Published",
                                tint = if (app.published) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                editingApp = app
                                isCreatingNew = false
                            },
                            modifier = Modifier.testTag("admin_edit_app_${app.appId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit App / Release Version"
                            )
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
}

@Composable
private fun AdminMetricBox(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
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
    var versionName by remember { mutableStateOf(initialApp.versionName) }
    var versionCodeText by remember { mutableStateOf(initialApp.versionCode.toString()) }
    var apkFileSize by remember { mutableStateOf(initialApp.apkFileSize) }
    var apkDownloadUrl by remember { mutableStateOf(initialApp.apkDownloadUrl) }
    var iconUrl by remember { mutableStateOf(initialApp.iconUrl) }
    var screenshotsText by remember { mutableStateOf(initialApp.screenshots.joinToString(", ")) }
    var description by remember { mutableStateOf(initialApp.description) }
    var releaseNotes by remember { mutableStateOf(initialApp.releaseNotes) }
    var featured by remember { mutableStateOf(initialApp.featured) }
    var published by remember { mutableStateOf(initialApp.published) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "Publish New Application" else "Update '${initialApp.appName}'")
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

                OutlinedTextField(
                    value = appId,
                    onValueChange = { if (isNew) appId = it },
                    enabled = isNew,
                    label = { Text("App ID (slug)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Android Package Name (e.g. com.zayaanapps.calc)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        value = developerName,
                        onValueChange = { developerName = it },
                        label = { Text("Developer") },
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
                    placeholder = { Text("https://github.com/zayaanapps/.../releases/download/v1.0/app-release.apk") },
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
                    label = { Text("Release Notes (What's New)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Featured on Store Home")
                    Spacer(modifier = Modifier.width(12.dp))
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
                            versionName = versionName.ifBlank { "1.0.0" },
                            versionCode = parsedCode,
                            apkFileSize = apkFileSize.ifBlank { "12.0 MB" },
                            apkDownloadUrl = apkDownloadUrl.ifBlank {
                                "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/app-release.apk"
                            },
                            iconUrl = iconUrl,
                            screenshots = parsedScreenshots,
                            description = description.ifBlank { "Official Zayaan Apps distribution package." },
                            releaseNotes = releaseNotes,
                            featured = featured,
                            published = published
                        )
                    )
                },
                modifier = Modifier.testTag("editor_save_button")
            ) {
                Text("Save to Firestore")
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
