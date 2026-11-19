package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AndroidPackageController
import com.example.data.AppThemeMode
import com.example.data.DownloadState
import com.example.data.StoreApp
import com.example.data.StoreRepository
import com.example.data.isAdminEmail
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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
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
fun ZayaanStoreRoot(auth: FirebaseAuth? = null) {
    val context = LocalContext.current
    val isFirebaseInitialized = remember { FirebaseApp.getApps(context).isNotEmpty() }
    val resolvedAuth = auth ?: if (isFirebaseInitialized) Firebase.auth else null
    var currentUser by remember(resolvedAuth) { mutableStateOf(resolvedAuth?.currentUser) }

    DisposableEffect(resolvedAuth) {
        if (resolvedAuth == null) {
            onDispose {}
        } else {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUser = firebaseAuth.currentUser
            }
            resolvedAuth.addAuthStateListener(listener)
            onDispose { resolvedAuth.removeAuthStateListener(listener) }
        }
    }

    val user = currentUser
    if (user == null) {
        val localController = remember(context) { AndroidPackageController(context) }
        val savedThemeMode = remember { localController.getSavedThemeMode() }
        val systemDark = isSystemInDarkTheme()
        val authDark = when (savedThemeMode) {
            AppThemeMode.SYSTEM -> systemDark
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }
        ZayaanAppsTheme(darkTheme = authDark) {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = resolvedAuth?.currentUser
                }
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedStoreShell(
                currentUserId = user.uid,
                currentUserEmail = user.email,
                isAuthenticated = true,
                isFirebaseInitialized = isFirebaseInitialized
            )
        }
    }
}

@Composable
fun AuthenticatedStoreShell(
    currentUserId: String,
    currentUserEmail: String?,
    isAuthenticated: Boolean = true,
    isFirebaseInitialized: Boolean = true
) {
    val viewModel: StoreViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = if (isFirebaseInitialized) FirebaseFirestore.getInstance(databaseId) else null
                StoreViewModel(
                    repository = StoreRepository(db),
                    packageController = AndroidPackageController(app),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    isAuthenticated = isAuthenticated
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
    val pendingInstallPermission by viewModel.pendingInstallPermission.collectAsStateWithLifecycle()
    var selfUpdateDismissed by remember { mutableStateOf(false) }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val systemInDarkTheme = isSystemInDarkTheme()
    val effectiveDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    var showLaunchSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(280)
        showLaunchSplash = false
    }

    val notifyUpdates = userPrefs?.notifyUpdates ?: true
    val bookmarkedIds = userPrefs?.bookmarkedAppIds ?: emptyList()
    val favoriteIds = userPrefs?.favoriteAppIds ?: emptyList()
    val downloadedAppIds = userPrefs?.downloadedAppIds ?: emptyList()
    val searchHistory = userPrefs?.searchHistory ?: emptyList()
    val recentlyViewedIds = userPrefs?.recentlyViewedAppIds ?: emptyList()
    val isAdminUser = isAuthenticated && (isTrustedAdmin || isAdminEmail(currentUserEmail))
    val visibleTabs = remember(isAdminUser) {
        ZayaanTab.entries.filter { it != ZayaanTab.ADMIN || isAdminUser }
    }

    var currentTab by remember { mutableStateOf(ZayaanTab.STORE) }
    LaunchedEffect(isAdminUser) {
        if (!isAdminUser && currentTab == ZayaanTab.ADMIN) {
            currentTab = ZayaanTab.STORE
        }
    }
    var selectedAppIdForDetail by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var permissionVersionTick by remember { mutableStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissionVersionTick++
    }

    val unknownAppsSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        permissionVersionTick++
        viewModel.onAppResumed(context)
    }

    // Prompt notification permission once if needed on Android 13+
    LaunchedEffect(Unit) {
        delay(450)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.isNotificationPermissionGranted()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Automatically delete downloaded APK files right after Android completes package install/update
    DisposableEffect(context, viewModel) {
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
                    val installedPkg = intent.data?.schemeSpecificPart ?: return
                    viewModel.handlePackageInstalledOrReplaced(installedPkg)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            runCatching { context.unregisterReceiver(packageReceiver) }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionVersionTick++
                viewModel.onAppResumed(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Count available updates for badge on My Apps tab
    val availableUpdatesCount = installedStatuses.values.count { it.updateAvailable }

    BackHandler(enabled = selectedAppIdForDetail != null || currentTab != ZayaanTab.STORE) {
        if (selectedAppIdForDetail != null) {
            selectedAppIdForDetail = null
        } else {
            currentTab = ZayaanTab.STORE
        }
    }

    ZayaanAppsTheme(darkTheme = effectiveDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 700.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar {
                            visibleTabs.forEach { tab ->
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
                            visibleTabs.forEach { tab ->
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
                                            viewModel.downloadLatestGitHubReleaseApk(selfUpdate, context)
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
                                    onInstallApk = { ctx, path ->
                                        viewModel.installDownloadedApk(
                                            context = ctx,
                                            apkFilePath = path,
                                            appId = appId,
                                            appName = activeDetailApp.appName
                                        )
                                    },
                                    onOpenInstalledApp = { ctx, pkg ->
                                        if (pkg.trim() == ctx.packageName) {
                                            selectedAppIdForDetail = null
                                            currentTab = ZayaanTab.STORE
                                            Toast.makeText(ctx, "Zayaan Apps Store is open", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.openInstalledApp(ctx, pkg)
                                        }
                                    },
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
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !viewModel.isNotificationPermissionGranted()
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        viewModel.downloadLatestGitHubReleaseApk(app, ctx)
                                    },
                                    onUninstallApp = { ctx, pkg ->
                                        viewModel.uninstallInstalledApp(ctx, pkg)
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
                                                if (pkg.trim() == ctx.packageName) {
                                                    selectedAppIdForDetail = null
                                                    currentTab = ZayaanTab.STORE
                                                    Toast.makeText(ctx, "Zayaan Apps Store is open", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.openInstalledApp(ctx, pkg)
                                                }
                                            }
                                        )
                                    }

                                    ZayaanTab.ADMIN -> {
                                        if (isAdminUser) {
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
                                        } else {
                                            LaunchedEffect(Unit) { currentTab = ZayaanTab.STORE }
                                        }
                                    }

                                    ZayaanTab.WEB_DEVOPS -> {
                                        val canInstallUnknown = remember(permissionVersionTick) {
                                            viewModel.canRequestPackageInstalls()
                                        }
                                        val notificationsAllowed = remember(permissionVersionTick) {
                                            viewModel.isNotificationPermissionGranted()
                                        }
                                        WebDevOpsAndSettingsScreen(
                                            themeMode = themeMode,
                                            notifyUpdates = notifyUpdates,
                                            currentUserEmail = currentUserEmail,
                                            isAdminUser = isAdminUser,
                                            canInstallUnknownApps = canInstallUnknown,
                                            notificationsPermissionGranted = notificationsAllowed,
                                            onThemeModeChange = { viewModel.updateThemeMode(it) },
                                            onNotifyUpdatesChange = { viewModel.updateNotificationPreference(it) },
                                            onRequestNotificationPermission = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            },
                                            onRequestInstallUnknownAppsPermission = {
                                                val intent = viewModel.buildManageUnknownAppSourcesIntent(context)
                                                if (intent != null) {
                                                    unknownAppsSettingsLauncher.launch(intent)
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
            val pendingPerm = pendingInstallPermission
            if (pendingPerm != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissInstallPermissionPrompt() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text("Permission Required") },
                    text = {
                        Text(
                            "To install or update \"${pendingPerm.appName}\", Android requires the 'Install Unknown Apps' permission for Zayaan Apps. Grant it in Settings and return to automatically continue installation (downloaded APK files are automatically deleted right after install)."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.markAwaitingUnknownAppsPermissionGrant()
                                val intent = viewModel.buildManageUnknownAppSourcesIntent(context)
                                if (intent != null) {
                                    unknownAppsSettingsLauncher.launch(intent)
                                }
                            }
                        ) {
                            Text("Allow Permission")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissInstallPermissionPrompt() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            AnimatedVisibility(
                visible = showLaunchSplash,
                exit = fadeOut(animationSpec = tween(durationMillis = 260))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                            contentDescription = "Zayaan Apps",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Zayaan Apps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(120.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
