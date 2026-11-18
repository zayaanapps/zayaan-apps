package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AndroidPackageController
import com.example.data.DownloadState
import com.example.data.StoreApp
import com.example.data.StoreRepository
import com.example.ui.AdminDashboardScreen
import com.example.ui.AppDetailScreen
import com.example.ui.AuthScreen
import com.example.ui.MyAppsScreen
import com.example.ui.StoreHomeScreen
import com.example.ui.StoreViewModel
import com.example.ui.UiState
import com.example.ui.WebDevOpsAndSettingsScreen
import com.example.ui.signOutFromZayaanStore
import com.example.ui.theme.ZayaanAppsTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZayaanStoreRoot()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

enum class ZayaanTab(val label: String, val icon: ImageVector, val testTag: String) {
    STORE("Store", Icons.Default.Shop, "nav_tab_store"),
    MY_APPS("My Apps", Icons.Default.Apps, "nav_tab_my_apps"),
    ADMIN("Admin", Icons.Default.AdminPanelSettings, "nav_tab_admin"),
    WEB_DEVOPS("Web & DevOps", Icons.Default.Public, "nav_tab_web_devops")
}

@Composable
fun ZayaanStoreRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        ZayaanAppsTheme(darkTheme = true) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedStoreShell(
                currentUserId = user.uid,
                currentUserEmail = user.email
            )
        }
    }
}

@Composable
fun AuthenticatedStoreShell(
    currentUserId: String,
    currentUserEmail: String?
) {
    val viewModel: StoreViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoreViewModel(
                    repository = StoreRepository(db),
                    packageController = AndroidPackageController(app),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail
                )
            }
        }
    )

    val publishedAppsState by viewModel.publishedAppsState.collectAsStateWithLifecycle()
    val adminAppsState by viewModel.adminAppsState.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val allReviews by viewModel.allReviewsState.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsState.collectAsStateWithLifecycle()
    val collections by viewModel.collectionsState.collectAsStateWithLifecycle()
    val submissions by viewModel.submissionsState.collectAsStateWithLifecycle()
    val reports by viewModel.reportsState.collectAsStateWithLifecycle()
    val versionHistory by viewModel.versionHistoryState.collectAsStateWithLifecycle()
    val adminRecords by viewModel.adminRecordsState.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val cachedApkVerifications by viewModel.cachedApkVerifications.collectAsStateWithLifecycle()
    val isTrustedAdmin by viewModel.isTrustedAdmin.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferencesState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSubcategory by viewModel.selectedSubcategory.collectAsStateWithLifecycle()
    val selectedSortOption by viewModel.selectedSortOption.collectAsStateWithLifecycle()
    val installedStatuses by viewModel.installedStatuses.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val downloadedApkFiles by viewModel.downloadedApkFiles.collectAsStateWithLifecycle()
    val comparisonAppIds by viewModel.comparisonAppIds.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val storeSelfUpdateApp by viewModel.storeSelfUpdateApp.collectAsStateWithLifecycle()
    var selfUpdateDismissed by remember { mutableStateOf(false) }

    val isDarkMode = userPrefs?.darkMode ?: true
    val notifyUpdates = userPrefs?.notifyUpdates ?: true
    val bookmarkedIds = userPrefs?.bookmarkedAppIds ?: emptyList()
    val favoriteIds = userPrefs?.favoriteAppIds ?: emptyList()
    val downloadedAppIds = userPrefs?.downloadedAppIds ?: emptyList()
    val searchHistory = userPrefs?.searchHistory ?: emptyList()
    val recentlyViewedIds = userPrefs?.recentlyViewedAppIds ?: emptyList()
    val isAdminUser = isTrustedAdmin || currentUserEmail.equals("zayaanapps@gmail.com", ignoreCase = true)

    var currentTab by remember { mutableStateOf(ZayaanTab.STORE) }
    var selectedAppIdForDetail by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Count available updates for badge on My Apps tab
    val availableUpdatesCount = installedStatuses.values.count { it.updateAvailable }

    BackHandler(enabled = selectedAppIdForDetail != null || currentTab != ZayaanTab.STORE) {
        if (selectedAppIdForDetail != null) {
            selectedAppIdForDetail = null
        } else {
            currentTab = ZayaanTab.STORE
        }
    }

    ZayaanAppsTheme(darkTheme = isDarkMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 700.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar {
                            ZayaanTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab && selectedAppIdForDetail == null,
                                    onClick = {
                                        selectedAppIdForDetail = null
                                        currentTab = tab
                                    },
                                    icon = {
                                        if (tab == ZayaanTab.MY_APPS && availableUpdatesCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge { Text("$availableUpdatesCount") }
                                                }
                                            ) {
                                                Icon(imageVector = tab.icon, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(imageVector = tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            ZayaanTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentTab == tab && selectedAppIdForDetail == null,
                                    onClick = {
                                        selectedAppIdForDetail = null
                                        currentTab = tab
                                    },
                                    icon = {
                                        if (tab == ZayaanTab.MY_APPS && availableUpdatesCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge { Text("$availableUpdatesCount") }
                                                }
                                            ) {
                                                Icon(imageVector = tab.icon, contentDescription = tab.label)
                                            }
                                        } else {
                                            Icon(imageVector = tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Zayaan Apps Store Self-Update Prompt Banner
                        val selfUpdate = storeSelfUpdateApp
                        if (selfUpdate != null && !selfUpdateDismissed) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("store_self_update_banner"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Zayaan Apps Store Update Available (v${selfUpdate.versionName})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Tap Update to download & install the latest store APK (build ${selfUpdate.versionCode}).",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            selectedAppIdForDetail = selfUpdate.appId.ifBlank { "zayaan-apps-store" }
                                            viewModel.startApkDownload(selfUpdate)
                                        },
                                        modifier = Modifier.testTag("store_self_update_button")
                                    ) {
                                        Text("Update")
                                    }
                                    IconButton(onClick = { selfUpdateDismissed = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss self-update prompt"
                                        )
                                    }
                                }
                            }
                        }

                        // Dismissible Status Banner
                        if (bannerMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bannerMessage!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.clearBannerMessage() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss notification"
                                        )
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            val publishedList = (publishedAppsState as? UiState.Success)?.data ?: emptyList()
                            val activeDetailApp: StoreApp? = if (selectedAppIdForDetail != null) {
                                val allAvailable = publishedList +
                                    ((adminAppsState as? UiState.Success)?.data ?: emptyList())
                                allAvailable.firstOrNull {
                                    (it.appId.ifBlank { it.docId }) == selectedAppIdForDetail
                                }
                            } else null

                            if (activeDetailApp != null) {
                                val appId = activeDetailApp.appId.ifBlank { activeDetailApp.docId }
                                val appReviews = allReviews.filter { it.appId == appId }
                                val appVersions = versionHistory.filter { it.appId == appId }
                                AppDetailScreen(
                                    app = activeDetailApp,
                                    allApps = publishedList,
                                    reviews = appReviews,
                                    versionHistory = appVersions,
                                    currentUserId = currentUserId,
                                    isAdmin = isAdminUser,
                                    installedStatus = installedStatuses[activeDetailApp.packageName],
                                    downloadState = downloadStates[appId] ?: DownloadState.Idle,
                                    isBookmarked = appId in bookmarkedIds,
                                    isFavorite = appId in favoriteIds,
                                    isInComparison = appId in comparisonAppIds,
                                    onBack = { selectedAppIdForDetail = null },
                                    onStartDownload = { viewModel.startApkDownload(it) },
                                    onCancelDownload = { viewModel.cancelActiveDownload(it) },
                                    onInstallApk = { ctx, path -> viewModel.installDownloadedApk(ctx, path) },
                                    onOpenInstalledApp = { ctx, pkg -> viewModel.openInstalledApp(ctx, pkg) },
                                    onOpenBrowserDownload = { ctx, app -> viewModel.openExternalDownloadUrl(ctx, app) },
                                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onToggleCompare = { viewModel.toggleCompareApp(it) },
                                    onShareApp = { ctx, app -> viewModel.shareApp(ctx, app) },
                                    onSelectRelatedApp = { related ->
                                        val relId = related.appId.ifBlank { related.docId }
                                        selectedAppIdForDetail = relId
                                        viewModel.recordRecentlyViewedApp(relId)
                                    },
                                    onSubmitReview = { rating, comment ->
                                        viewModel.submitAppReview(activeDetailApp, rating, comment)
                                    },
                                    onVoteReviewHelpful = { viewModel.voteReviewHelpful(it) },
                                    onFlagReview = { rev, flagged ->
                                        viewModel.flagReviewForModeration(rev, flagged)
                                    },
                                    onAdminReplyReview = { rev, replyText ->
                                        viewModel.replyToReviewAsAdmin(rev, replyText)
                                    },
                                    onDeleteReview = { viewModel.deleteReview(it) },
                                    onReportApp = { reason, details ->
                                        viewModel.submitReport(appId, "APP", reason, details)
                                    },
                                    onDownloadLatestGitHubRelease = { ctx, app ->
                                        viewModel.downloadLatestGitHubReleaseApk(app, ctx)
                                    }
                                )
                            } else {
                                when (currentTab) {
                                    ZayaanTab.STORE -> {
                                        StoreHomeScreen(
                                            appsState = publishedAppsState,
                                            searchQuery = searchQuery,
                                            selectedCategory = selectedCategory,
                                            selectedSubcategory = selectedSubcategory,
                                            selectedSortOption = selectedSortOption,
                                            searchHistory = searchHistory,
                                            recentlyViewedAppIds = recentlyViewedIds,
                                            favoriteAppIds = favoriteIds,
                                            bookmarkedAppIds = bookmarkedIds,
                                            downloadedAppIds = downloadedAppIds,
                                            notifications = notifications,
                                            customCollections = collections,
                                            installedStatuses = installedStatuses,
                                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                            onSearchSubmit = { viewModel.submitSearchQuery(it) },
                                            onClearSearchHistory = { viewModel.clearSearchHistory() },
                                            onCategorySelect = { viewModel.selectCategory(it) },
                                            onSubcategorySelect = { viewModel.selectSubcategory(it) },
                                            onSortOptionSelect = { viewModel.selectSortOption(it) },
                                            onResetFilters = { viewModel.resetFilters() },
                                            onAppClick = { app ->
                                                val id = app.appId.ifBlank { app.docId }
                                                selectedAppIdForDetail = id
                                                viewModel.recordRecentlyViewedApp(id)
                                            },
                                            onSeedDefaultCatalog = { viewModel.seedCatalogNow() }
                                        )
                                    }

                                    ZayaanTab.MY_APPS -> {
                                        MyAppsScreen(
                                            appsState = publishedAppsState,
                                            installedStatuses = installedStatuses,
                                            downloadStates = downloadStates,
                                            downloadedApkFiles = downloadedApkFiles,
                                            bookmarkedIds = bookmarkedIds,
                                            favoriteIds = favoriteIds,
                                            downloadedAppIds = downloadedAppIds,
                                            comparisonAppIds = comparisonAppIds,
                                            onRefreshScan = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                                viewModel.refreshInstalledAppsStatus()
                                            },
                                            onAppClick = { app ->
                                                val id = app.appId.ifBlank { app.docId }
                                                selectedAppIdForDetail = id
                                                viewModel.recordRecentlyViewedApp(id)
                                            },
                                            onUpdateOrDownloadClick = { app ->
                                                selectedAppIdForDetail = app.appId.ifBlank { app.docId }
                                                viewModel.startApkDownload(app)
                                            },
                                            onUpdateAllClick = { ctx, updatableList ->
                                                viewModel.updateAllAvailableApps(ctx, updatableList)
                                            },
                                            onInstallCachedApk = { ctx, path ->
                                                viewModel.installDownloadedApk(ctx, path)
                                            },
                                            onDeleteCachedApk = { path ->
                                                viewModel.deleteCachedApk(path)
                                            },
                                            onCancelDownload = { id ->
                                                viewModel.cancelActiveDownload(id)
                                            },
                                            onRemoveFromCompare = { id ->
                                                viewModel.toggleCompareApp(id)
                                            },
                                            onClearComparison = {
                                                viewModel.clearComparisonApps()
                                            },
                                            onOpenInstalledApp = { ctx, pkg ->
                                                viewModel.openInstalledApp(ctx, pkg)
                                            }
                                        )
                                    }

                                    ZayaanTab.ADMIN -> {
                                        AdminDashboardScreen(
                                            adminAppsState = adminAppsState,
                                            categoriesState = categoriesState,
                                            allReviews = allReviews,
                                            notifications = notifications,
                                            collections = collections,
                                            submissions = submissions,
                                            reports = reports,
                                            versionHistory = versionHistory,
                                            adminRecords = adminRecords,
                                            auditLogs = auditLogs,
                                            cachedApkVerifications = cachedApkVerifications,
                                            currentUserId = currentUserId,
                                            currentUserEmail = currentUserEmail,
                                            isTrustedAdmin = isAdminUser,
                                            onSaveApp = { app, isEdit ->
                                                viewModel.createOrUpdateApp(app, isEdit)
                                            },
                                            onTogglePublished = { viewModel.togglePublished(it) },
                                            onToggleFeatured = { viewModel.toggleFeatured(it) },
                                            onToggleEditorsChoice = { viewModel.toggleEditorsChoice(it) },
                                            onToggleEssential = { viewModel.toggleEssential(it) },
                                            onDeleteApp = { viewModel.deleteApp(it) },
                                            onAddCategory = { name, desc, order ->
                                                viewModel.addCategory(name, desc, order)
                                            },
                                            onDeleteCategory = { viewModel.deleteCategory(it) },
                                            onReplyToReview = { rev, replyText ->
                                                viewModel.replyToReviewAsAdmin(rev, replyText)
                                            },
                                            onClearReviewFlag = { rev ->
                                                viewModel.flagReviewForModeration(rev, false)
                                            },
                                            onDeleteReview = { rev ->
                                                viewModel.deleteReview(rev)
                                            },
                                            onBroadcastNotification = { title, body, category, targetAppId ->
                                                viewModel.publishStoreAnnouncement(title, body, category, targetAppId)
                                            },
                                            onDeleteNotification = { id ->
                                                viewModel.deleteStoreAnnouncement(id)
                                            },
                                            onSaveCollection = { colId, title, subtitle, appIds, isPublic, featured ->
                                                viewModel.saveCollection(colId, title, subtitle, appIds, isPublic, featured)
                                            },
                                            onDeleteCollection = { colId ->
                                                viewModel.deleteCollection(colId)
                                            },
                                            onSubmitDeveloperApp = { sub ->
                                                viewModel.submitDeveloperApplication(sub)
                                            },
                                            onReviewDeveloperSubmission = { sub, status, note ->
                                                viewModel.reviewDeveloperSubmission(sub, status, note)
                                            },
                                            onDeleteDeveloperSubmission = { subId ->
                                                viewModel.deleteDeveloperSubmission(subId)
                                            },
                                            onSubmitReport = { targetId, targetType, reason, details ->
                                                viewModel.submitReport(targetId, targetType, reason, details)
                                            },
                                            onUpdateReportStatus = { rep, status ->
                                                viewModel.updateReportStatus(rep, status)
                                            },
                                            onDeleteReport = { repId ->
                                                viewModel.deleteReport(repId)
                                            },
                                            onAddVersionRecord = { appId, vName, vCode, url, size, sha, log ->
                                                viewModel.addVersionReleaseRecord(appId, vName, vCode, url, size, sha, log)
                                            },
                                            onDeleteVersionRecord = { verId ->
                                                viewModel.deleteVersionReleaseRecord(verId)
                                            },
                                            onGrantAdminRole = { uid, role ->
                                                viewModel.grantOrUpdateAdminRole(uid, role)
                                            },
                                            onRevokeAdminRole = { uid ->
                                                viewModel.revokeAdminRole(uid)
                                            },
                                            onSeedOfficialCatalog = { viewModel.seedCatalogNow() }
                                        )
                                    }

                                    ZayaanTab.WEB_DEVOPS -> {
                                        WebDevOpsAndSettingsScreen(
                                            darkMode = isDarkMode,
                                            notifyUpdates = notifyUpdates,
                                            currentUserEmail = currentUserEmail,
                                            onDarkModeChange = { viewModel.updateDarkModePreference(it) },
                                            onNotifyUpdatesChange = { viewModel.updateNotificationPreference(it) },
                                            onRequestNotificationPermission = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            },
                                            onOpenUrl = { ctx, url ->
                                                val dummyApp = StoreApp(appId = "portal", apkDownloadUrl = url)
                                                viewModel.openExternalDownloadUrl(ctx, dummyApp)
                                            },
                                            onSignOut = {
                                                signOutFromZayaanStore(credentialManager, scope)
                                            },
                                            onShowCopiedMessage = { _ ->
                                                viewModel.clearBannerMessage()
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
    }
}
