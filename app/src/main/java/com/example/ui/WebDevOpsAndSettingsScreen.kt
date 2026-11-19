package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.AppThemeMode

@Composable
fun WebDevOpsAndSettingsScreen(
    themeMode: AppThemeMode,
    notifyUpdates: Boolean,
    currentUserEmail: String?,
    isAdminUser: Boolean = false,
    canInstallUnknownApps: Boolean = true,
    notificationsPermissionGranted: Boolean = true,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onNotifyUpdatesChange: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestInstallUnknownAppsPermission: () -> Unit = {},
    onOpenUrl: (Context, String) -> Unit,
    onSignOut: () -> Unit,
    onShowCopiedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
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
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Compact, Theme-Adaptive Web & GitHub DevOps Header Card
        item {
            val headerShape = RoundedCornerShape(16.dp)
            val headerBrush = if (isDark) {
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0C2235), Color(0xFF092A27))
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color(0xFFEAF8F1), Color(0xFFF1F7FC))
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = headerShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(
                    width = 1.dp,
                    color = cs.primary.copy(alpha = if (isDark) 0.34f else 0.24f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBrush)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.zayaan_logo_badge),
                            contentDescription = "Zayaan Apps Logo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zayaan Apps Web & DevOps",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "https://zayaanapps.github.io/zayaan-apps/",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = cs.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    onOpenUrl(context, "https://zayaanapps.github.io/zayaan-apps/")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Surface(
                        color = cs.surface.copy(alpha = if (isDark) 0.62f else 0.88f),
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Build v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • ${BuildConfig.APPLICATION_ID}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Android APK & responsive Flutter Web share the live Firestore 'apps' catalog and GitHub Actions CI/CD releases.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = cs.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onOpenUrl(context, "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk")
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("direct_latest_release_apk_button")
                        ) {
                            Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Latest Release APK", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                onOpenUrl(context, "https://github.com/zayaanapps/zayaan-apps/releases/latest")
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("GitHub Releases", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                onOpenUrl(context, "https://zayaanapps.github.io/zayaan-apps/")
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Open Web Store", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                onOpenUrl(context, "https://github.com/zayaanapps/zayaan-apps")
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.32f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("GitHub Repo", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // 2. Compact Store Settings & Preferences Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.24f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingLeadingIconBadge(
                            icon = Icons.Default.Settings,
                            tint = cs.primary
                        )
                        Text(
                            text = "Store Settings & Preferences",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingLeadingIconBadge(
                                icon = when (themeMode) {
                                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                                    AppThemeMode.DARK -> Icons.Default.DarkMode
                                },
                                tint = cs.primary
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Column {
                                Text(
                                    text = "App Theme (Material 3)",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Follows System by default; switch to Light or Dark anytime",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("theme_mode_selector")
                        ) {
                            AppThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = AppThemeMode.entries.size
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = when (mode) {
                                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = cs.outline.copy(alpha = 0.18f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            SettingLeadingIconBadge(
                                icon = Icons.Default.Notifications,
                                tint = cs.primary
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Column {
                                Text(
                                    text = "Available Update Notifications",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Notify when installed Zayaan apps have newer versionCode",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = notifyUpdates,
                            onCheckedChange = { enabled ->
                                onNotifyUpdatesChange(enabled)
                                if (enabled) onRequestNotificationPermission()
                            },
                            modifier = Modifier.testTag("notify_updates_switch")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = cs.outline.copy(alpha = 0.18f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            SettingLeadingIconBadge(
                                icon = Icons.Default.InstallMobile,
                                tint = cs.primary
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Column {
                                Text(
                                    text = "Install Unknown Apps Permission",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = if (canInstallUnknownApps) {
                                        "Granted — APK updates auto-delete after install"
                                    } else {
                                        "Required to install & update APKs"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        if (canInstallUnknownApps) {
                            Surface(
                                color = cs.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.30f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Granted",
                                        tint = cs.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Granted",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = cs.primary
                                    )
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onRequestInstallUnknownAppsPermission,
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Grant", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (!notificationsPermissionGranted) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = cs.outline.copy(alpha = 0.18f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                SettingLeadingIconBadge(
                                    icon = Icons.Default.Notifications,
                                    tint = cs.tertiary
                                )
                                Spacer(modifier = Modifier.width(9.dp))
                                Column {
                                    Text(
                                        text = "Notification Permission",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = "Tap Grant to receive app update & install alerts",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            FilledTonalButton(
                                onClick = onRequestNotificationPermission,
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Grant", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = cs.outline.copy(alpha = 0.18f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            SettingLeadingIconBadge(
                                icon = Icons.Default.AccountCircle,
                                tint = cs.secondary
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Column {
                                Text(
                                    text = "Account Session",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = currentUserEmail?.let {
                                        "$it • ${if (isAdminUser) "Administrator" else "Standard User"}"
                                    } ?: "Not signed in",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (currentUserEmail != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(1.dp, cs.error.copy(alpha = 0.35f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.error),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // 3. Compact Blueprint & Code Inspector Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingLeadingIconBadge(
                        icon = Icons.Default.Code,
                        tint = cs.primary
                    )
                    Text(
                        text = "Flutter + Web + GitHub Actions Blueprints",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    blueprintTabs.forEach { tab ->
                        val selected = selectedBlueprintTab == tab
                        Surface(
                            onClick = { selectedBlueprintTab = tab },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) cs.primary.copy(alpha = 0.14f) else cs.surface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) cs.primary.copy(alpha = 0.55f) else cs.outline.copy(alpha = 0.24f)
                            )
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (selected) cs.primary else cs.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.24f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedBlueprintTab,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = cs.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText(selectedBlueprintTab, contentText))
                                onShowCopiedMessage("Copied '$selectedBlueprintTab' to clipboard!")
                            },
                            shape = RoundedCornerShape(9.dp),
                            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = cs.surfaceVariant.copy(alpha = if (isDark) 0.42f else 0.32f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = contentText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp
                            ),
                            color = cs.onSurface,
                            modifier = Modifier.padding(11.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingLeadingIconBadge(
    icon: ImageVector,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
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
# File: .github/workflows/deploy.yml (Native Android Kotlin APK + Flutter Web)
name: Zayaan Apps — Automated Android APK Release & GitHub Pages Web Deployment

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

permissions:
  contents: write
  pages: write
  id-token: write

jobs:
  build-android-apk:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
      - uses: android-actions/setup-android@v4
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '9.3.1'
      - name: Compile Native Jetpack Compose APK
        run: gradle :app:assembleDebug --no-daemon
      - name: Publish Native Android APK to GitHub Releases
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v1.0.0.${'$'}{{ steps.version_info.outputs.build_code }}
          make_latest: true
          files: |
            release_assets/zayaan-apps-release.apk
            release_assets/zayaan-apps.apk
            release_assets/app-release.apk

  build-and-deploy-web:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: flutter_zayaan_apps
    steps:
      - uses: actions/checkout@v4
      - uses: subosito/flutter-action@v2
        with:
          channel: 'stable'
          cache: true
      - run: |
          flutter pub get
          flutter build web --release --base-href "/zayaan-apps/"
          cp build/web/index.html build/web/404.html
          touch build/web/.nojekyll
      - uses: actions/upload-pages-artifact@v3
        with:
          path: flutter_zayaan_apps/build/web
      - uses: actions/deploy-pages@v4
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
