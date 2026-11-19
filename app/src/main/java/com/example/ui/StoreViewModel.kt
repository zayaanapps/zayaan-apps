package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AdminRecord
import com.example.data.AndroidPackageController
import com.example.data.ApkArchiveVerificationInfo
import com.example.data.AppCollection
import com.example.data.AppReport
import com.example.data.AppReview
import com.example.data.AppThemeMode
import com.example.data.AppVersionRecord
import com.example.data.AuditLogRecord
import com.example.data.DeveloperSubmission
import com.example.data.DownloadState
import com.example.data.InstalledPackageStatus
import com.example.data.SortOption
import com.example.data.StoreApp
import com.example.data.StoreCategory
import com.example.data.StoreNotification
import com.example.data.StoreRepository
import com.example.data.UserPreference
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class StoreViewModel(
    private val repository: StoreRepository,
    private val packageController: AndroidPackageController,
    private val currentUserId: String,
    private val currentUserEmail: String?,
    private val isAuthenticated: Boolean = true
) : ViewModel() {

    private val isBootstrappedAdmin: Boolean =
        isAuthenticated && currentUserEmail?.equals("zayaanapps@gmail.com", ignoreCase = true) == true

    private val _publishedAppsState = MutableStateFlow<UiState<List<StoreApp>>>(UiState.Loading)
    val publishedAppsState: StateFlow<UiState<List<StoreApp>>> = _publishedAppsState.asStateFlow()

    private val _adminAppsState = MutableStateFlow<UiState<List<StoreApp>>>(UiState.Loading)
    val adminAppsState: StateFlow<UiState<List<StoreApp>>> = _adminAppsState.asStateFlow()

    private val _categoriesState = MutableStateFlow<UiState<List<StoreCategory>>>(UiState.Loading)
    val categoriesState: StateFlow<UiState<List<StoreCategory>>> = _categoriesState.asStateFlow()

    private val _allReviewsState = MutableStateFlow<List<AppReview>>(emptyList())
    val allReviewsState: StateFlow<List<AppReview>> = _allReviewsState.asStateFlow()

    private val _notificationsState = MutableStateFlow<List<StoreNotification>>(emptyList())
    val notificationsState: StateFlow<List<StoreNotification>> = _notificationsState.asStateFlow()

    private val _collectionsState = MutableStateFlow<List<AppCollection>>(emptyList())
    val collectionsState: StateFlow<List<AppCollection>> = _collectionsState.asStateFlow()

    private val _submissionsState = MutableStateFlow<List<DeveloperSubmission>>(emptyList())
    val submissionsState: StateFlow<List<DeveloperSubmission>> = _submissionsState.asStateFlow()

    private val _reportsState = MutableStateFlow<List<AppReport>>(emptyList())
    val reportsState: StateFlow<List<AppReport>> = _reportsState.asStateFlow()

    private val _versionHistoryState = MutableStateFlow<List<AppVersionRecord>>(emptyList())
    val versionHistoryState: StateFlow<List<AppVersionRecord>> = _versionHistoryState.asStateFlow()

    private val _adminRecordsState = MutableStateFlow<List<AdminRecord>>(emptyList())
    val adminRecordsState: StateFlow<List<AdminRecord>> = _adminRecordsState.asStateFlow()

    private val _auditLogsState = MutableStateFlow<List<AuditLogRecord>>(emptyList())
    val auditLogsState: StateFlow<List<AuditLogRecord>> = _auditLogsState.asStateFlow()

    private val _isTrustedAdmin = MutableStateFlow(isBootstrappedAdmin)
    val isTrustedAdmin: StateFlow<Boolean> = _isTrustedAdmin.asStateFlow()

    private val _userPreferencesState = MutableStateFlow<UserPreference?>(null)
    val userPreferencesState: StateFlow<UserPreference?> = _userPreferencesState.asStateFlow()

    private val _themeMode = MutableStateFlow(packageController.getSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedSubcategory = MutableStateFlow("All")
    val selectedSubcategory: StateFlow<String> = _selectedSubcategory.asStateFlow()

    private val _selectedSortOption = MutableStateFlow(SortOption.RELEVANCE)
    val selectedSortOption: StateFlow<SortOption> = _selectedSortOption.asStateFlow()

    private val _installedStatuses = MutableStateFlow<Map<String, InstalledPackageStatus>>(emptyMap())
    val installedStatuses: StateFlow<Map<String, InstalledPackageStatus>> = _installedStatuses.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _downloadedApkFiles = MutableStateFlow<List<File>>(emptyList())
    val downloadedApkFiles: StateFlow<List<File>> = _downloadedApkFiles.asStateFlow()

    private val _cachedApkVerifications = MutableStateFlow<List<ApkArchiveVerificationInfo>>(emptyList())
    val cachedApkVerifications: StateFlow<List<ApkArchiveVerificationInfo>> = _cachedApkVerifications.asStateFlow()

    private val _comparisonAppIds = MutableStateFlow<List<String>>(emptyList())
    val comparisonAppIds: StateFlow<List<String>> = _comparisonAppIds.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _storeSelfUpdateApp = MutableStateFlow<StoreApp?>(null)
    val storeSelfUpdateApp: StateFlow<StoreApp?> = _storeSelfUpdateApp.asStateFlow()

    private var latestGitHubStoreRelease: StoreApp? = null
    private var hasAutoSeededEmptyStore = false
    private var notifiedUpdatePackages = mutableSetOf<String>()
    private var notifiedAnnouncementIds = mutableSetOf<String>()

    init {
        if (isAuthenticated) {
            observeStoreStreams()
        } else {
            loadGuestPreviewStore()
        }
        checkGitHubSelfUpdate()
        refreshCachedApks()
    }

    private fun enrichCatalogWithLiveStoreRelease(apps: List<StoreApp>): List<StoreApp> {
        val remote = latestGitHubStoreRelease
        val installedBuildCode = BuildConfig.VERSION_CODE.toLong()
        val installedBuildName = BuildConfig.VERSION_NAME
        val validStoreIconUrl =
            "https://raw.githubusercontent.com/zayaanapps/zayaan-apps/main/app/src/main/res/drawable/img_launcher_icon_1795021165006.jpg"

        return apps.map { app ->
            if (app.packageName != "com.zayaanapps.store" && app.appId != "zayaan-apps-store") {
                app
            } else {
                val fixedIconUrl = if (app.iconUrl.isBlank() || app.iconUrl.contains("/web/icons/Icon-192.png")) {
                    validStoreIconUrl
                } else {
                    app.iconUrl
                }
                when {
                    remote != null && remote.versionCode >= maxOf(app.versionCode, installedBuildCode) -> {
                        app.copy(
                            iconUrl = fixedIconUrl,
                            versionName = remote.versionName.ifBlank { app.versionName },
                            versionCode = remote.versionCode,
                            apkDownloadUrl = remote.apkDownloadUrl.ifBlank { app.apkDownloadUrl },
                            apkFileSize = remote.apkFileSize.ifBlank { app.apkFileSize },
                            releaseNotes = remote.releaseNotes.ifBlank { app.releaseNotes }
                        )
                    }
                    installedBuildCode > app.versionCode -> {
                        app.copy(
                            iconUrl = fixedIconUrl,
                            versionName = installedBuildName,
                            versionCode = installedBuildCode
                        )
                    }
                    else -> app.copy(iconUrl = fixedIconUrl)
                }
            }
        }
    }

    private fun applyLocalReviewAggregates(apps: List<StoreApp>, reviews: List<AppReview>): List<StoreApp> {
        if (reviews.isEmpty()) return apps
        val grouped = reviews.groupBy { it.appId }
        return apps.map { app ->
            val appId = app.appId.ifBlank { app.docId }
            val appReviews = grouped[appId].orEmpty()
            if (appReviews.isEmpty()) {
                app
            } else {
                app.copy(
                    ratingSum = appReviews.sumOf { it.rating },
                    reviewCount = appReviews.size.toLong()
                )
            }
        }
    }

    private fun refreshGuestCatalogAggregates(reviews: List<AppReview>) {
        val currentPub = (_publishedAppsState.value as? UiState.Success)?.data
        if (currentPub != null) {
            _publishedAppsState.value = UiState.Success(applyLocalReviewAggregates(currentPub, reviews))
        }
        val currentAdmin = (_adminAppsState.value as? UiState.Success)?.data
        if (currentAdmin != null) {
            _adminAppsState.value = UiState.Success(applyLocalReviewAggregates(currentAdmin, reviews))
        }
    }

    private fun loadGuestPreviewStore() {
        val localReviews = packageController.loadLocalReviewsBackup()
        _allReviewsState.value = localReviews
        val starterApps = applyLocalReviewAggregates(
            enrichCatalogWithLiveStoreRelease(StoreRepository.officialStarterApps()),
            localReviews
        )
        _publishedAppsState.value = UiState.Success(starterApps)
        _adminAppsState.value = UiState.Success(starterApps)
        _categoriesState.value = UiState.Success(StoreRepository.officialDefaultCategories())
        _userPreferencesState.value = UserPreference(userId = currentUserId, darkMode = true, notifyUpdates = true)
        refreshInstalledPackages(starterApps)
    }

    private fun syncPendingLocalReviewsToCloud(catalogApps: List<StoreApp>) {
        val pending = packageController.loadLocalReviewsBackup()
        if (pending.isEmpty()) return
        viewModelScope.launch {
            val displayName = currentUserEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: "Zayaan User"
            var allSynced = true
            pending.forEach { localReview ->
                val targetApp = catalogApps.firstOrNull {
                    (it.appId.ifBlank { it.docId }) == localReview.appId
                } ?: StoreApp(
                    docId = localReview.appId,
                    appId = localReview.appId,
                    appName = localReview.appName
                )
                val result = repository.submitOrUpdateReview(
                    app = targetApp,
                    userId = currentUserId,
                    userName = displayName,
                    rating = localReview.rating,
                    comment = localReview.comment,
                    verifiedInstall = localReview.verifiedInstall
                )
                if (result.isFailure) {
                    allSynced = false
                }
            }
            if (allSynced) {
                packageController.clearLocalReviewsBackup()
            }
        }
    }

    private fun observeStoreStreams() {
        viewModelScope.launch {
            repository.observePublishedApps()
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing published apps", e)
                    _publishedAppsState.value = UiState.Error(e.localizedMessage ?: "Failed to load catalog")
                }
                .collect { result ->
                    result.onSuccess { apps ->
                        val enriched = enrichCatalogWithLiveStoreRelease(apps)
                        _publishedAppsState.value = UiState.Success(enriched)
                        refreshInstalledPackages(enriched)
                        if (apps.isEmpty() && !hasAutoSeededEmptyStore) {
                            hasAutoSeededEmptyStore = true
                            seedInitialCatalogIfEmpty()
                        } else if (apps.isNotEmpty()) {
                            syncPendingLocalReviewsToCloud(enriched)
                        }
                    }.onFailure { e ->
                        _publishedAppsState.value = UiState.Error(e.localizedMessage ?: "Failed to load catalog")
                    }
                }
        }

        viewModelScope.launch {
            repository.observeAllAppsForAdmin()
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing admin apps", e)
                }
                .collect { result ->
                    result.onSuccess { apps ->
                        _adminAppsState.value = UiState.Success(enrichCatalogWithLiveStoreRelease(apps))
                    }.onFailure { e ->
                        _adminAppsState.value = UiState.Error(e.localizedMessage ?: "Admin access restricted")
                    }
                }
        }

        viewModelScope.launch {
            repository.observeCategories()
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing categories", e)
                }
                .collect { result ->
                    result.onSuccess { cats ->
                        _categoriesState.value = UiState.Success(cats)
                    }
                }
        }

        viewModelScope.launch {
            repository.observeAllReviews()
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing reviews", e)
                }
                .collect { result ->
                    result.onSuccess { reviews ->
                        _allReviewsState.value = reviews
                    }
                }
        }

        viewModelScope.launch {
            repository.observeNotifications()
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing notifications", e)
                }
                .collect { result ->
                    result.onSuccess { notifications ->
                        _notificationsState.value = notifications
                        val notifyEnabled = _userPreferencesState.value?.notifyUpdates ?: true
                        if (notifyEnabled) {
                            notifications.take(2).forEach { notif ->
                                if (notif.id.isNotBlank() && notif.id !in notifiedAnnouncementIds) {
                                    notifiedAnnouncementIds.add(notif.id)
                                    packageController.postStoreAnnouncementNotification(notif)
                                }
                            }
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.observeCollections(isAdmin = isBootstrappedAdmin)
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing collections", e)
                }
                .collect { result ->
                    result.onSuccess { cols ->
                        _collectionsState.value = cols
                    }
                }
        }

        viewModelScope.launch {
            repository.observeDeveloperSubmissions(currentUserId, isBootstrappedAdmin)
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing submissions", e)
                }
                .collect { result ->
                    result.onSuccess { subs ->
                        _submissionsState.value = subs
                    }
                }
        }

        viewModelScope.launch {
            repository.observeReports(currentUserId, isBootstrappedAdmin)
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing reports", e)
                }
                .collect { result ->
                    result.onSuccess { reps ->
                        _reportsState.value = reps
                    }
                }
        }

        viewModelScope.launch {
            repository.observeVersionHistory()
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing version history", e)
                }
                .collect { result ->
                    result.onSuccess { versions ->
                        _versionHistoryState.value = versions
                    }
                }
        }

        viewModelScope.launch {
            repository.observeAdminRecords(currentUserId, isBootstrappedAdmin)
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing admin records", e)
                }
                .collect { result ->
                    result.onSuccess { records ->
                        _adminRecordsState.value = records
                        _isTrustedAdmin.value = isBootstrappedAdmin || records.any { it.userId == currentUserId }
                    }
                }
        }

        viewModelScope.launch {
            repository.observeAuditLogs(currentUserId, isBootstrappedAdmin)
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing audit logs", e)
                }
                .collect { result ->
                    result.onSuccess { logs ->
                        _auditLogsState.value = logs
                    }
                }
        }

        viewModelScope.launch {
            repository.observeUserPreferences(currentUserId)
                .catch { e ->
                    Log.e("StoreViewModel", "Error observing user prefs", e)
                }
                .collect { result ->
                    result.onSuccess { pref ->
                        _userPreferencesState.value = pref
                    }
                }
        }
    }

    private fun logAudit(action: String, targetType: String, targetId: String, summary: String) {
        viewModelScope.launch {
            repository.recordAuditLog(
                action = action,
                targetType = targetType,
                targetId = targetId,
                summary = summary,
                actorUid = currentUserId,
                actorEmail = currentUserEmail ?: ""
            )
        }
    }

    private fun seedInitialCatalogIfEmpty() {
        viewModelScope.launch {
            repository.seedOfficialZayaanCatalog(currentUserId)
                .onSuccess { count ->
                    if (count > 0) {
                        _statusBannerMessage.value = "Loaded $count official Zayaan Apps into the store."
                        logAudit("CATALOG_SEEDED", "CATALOG", "starter-catalog", "Auto-seeded $count starter apps.")
                    }
                }
                .onFailure { e ->
                    Log.w("StoreViewModel", "Auto-seed skipped: ${e.message}")
                }
        }
    }

    fun refreshCachedApks() {
        viewModelScope.launch {
            val (files, verifications) = withContext(Dispatchers.IO) {
                val apkFiles = packageController.listDownloadedApkFiles()
                val infos = apkFiles.map { packageController.inspectApkArchiveInfo(it) }
                apkFiles to infos
            }
            _downloadedApkFiles.value = files
            _cachedApkVerifications.value = verifications
        }
    }

    fun deleteCachedApk(apkFilePath: String) {
        if (packageController.deleteDownloadedApkFile(apkFilePath)) {
            refreshCachedApks()
            _statusBannerMessage.value = "Removed cached APK package from local storage."
        }
    }

    fun cancelActiveDownload(appId: String) {
        packageController.cancelDownload(appId)
    }

    private fun syncCurrentPreferences(updatedPref: UserPreference) {
        _userPreferencesState.value = updatedPref
        if (!isAuthenticated) return
        viewModelScope.launch {
            repository.saveUserPreferences(
                userId = currentUserId,
                darkMode = updatedPref.darkMode,
                notifyUpdates = updatedPref.notifyUpdates,
                bookmarkedAppIds = updatedPref.bookmarkedAppIds,
                favoriteAppIds = updatedPref.favoriteAppIds,
                recentlyViewedAppIds = updatedPref.recentlyViewedAppIds,
                downloadedAppIds = updatedPref.downloadedAppIds,
                searchHistory = updatedPref.searchHistory
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitSearchQuery(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        if (trimmed.length >= 2) {
            recordSearchQuery(trimmed)
        }
    }

    fun recordSearchQuery(query: String) {
        val trimmed = query.trim().take(60)
        if (trimmed.isBlank()) return
        val currentPref = _userPreferencesState.value ?: UserPreference(userId = currentUserId)
        val updatedHistory = (listOf(trimmed) + currentPref.searchHistory.filterNot {
            it.equals(trimmed, ignoreCase = true)
        }).take(20)
        syncCurrentPreferences(currentPref.copy(searchHistory = updatedHistory))
    }

    fun clearSearchHistory() {
        val currentPref = _userPreferencesState.value ?: UserPreference(userId = currentUserId)
        syncCurrentPreferences(currentPref.copy(searchHistory = emptyList()))
    }

    fun recordRecentlyViewedApp(appId: String) {
        if (appId.isBlank()) return
        val currentPref = _userPreferencesState.value ?: UserPreference(userId = currentUserId)
        val updatedRecent = (listOf(appId) + currentPref.recentlyViewedAppIds.filterNot { it == appId }).take(30)
        syncCurrentPreferences(currentPref.copy(recentlyViewedAppIds = updatedRecent))

        val published = (_publishedAppsState.value as? UiState.Success)?.data ?: emptyList()
        val matchedApp = published.firstOrNull { (it.appId.ifBlank { it.docId }) == appId }
        if (isAuthenticated && matchedApp != null && matchedApp.published) {
            viewModelScope.launch {
                repository.incrementPageViewCount(matchedApp.docId.ifBlank { matchedApp.appId }, matchedApp.pageViews)
            }
        }
    }

    private fun recordDownloadedAppId(appId: String) {
        if (appId.isBlank()) return
        val currentPref = _userPreferencesState.value ?: UserPreference(userId = currentUserId)
        val updatedDownloaded = (listOf(appId) + currentPref.downloadedAppIds.filterNot { it == appId }).take(50)
        syncCurrentPreferences(currentPref.copy(downloadedAppIds = updatedDownloaded))
    }

    fun toggleBookmark(appId: String) {
        val currentPref = _userPreferencesState.value ?: UserPreference(userId = currentUserId)
        val updatedBookmarks = if (appId in currentPref.bookmarkedAppIds) {
            currentPref.bookmarkedAppIds - appId
        } else {
            (listOf(appId) + currentPref.bookmarkedAppIds).distinct().take(50)
        }
        syncCurrentPreferences(currentPref.copy(bookmarkedAppIds = updatedBookmarks))
    }

    fun toggleFavorite(appId: String) {
        val currentPref = _userPreferencesState.value ?: UserPreference(userId = currentUserId)
        val updatedFavorites = if (appId in currentPref.favoriteAppIds) {
            currentPref.favoriteAppIds - appId
        } else {
            (listOf(appId) + currentPref.favoriteAppIds).distinct().take(50)
        }
        syncCurrentPreferences(currentPref.copy(favoriteAppIds = updatedFavorites))
    }

    fun toggleCompareApp(appId: String) {
        if (appId.isBlank()) return
        val current = _comparisonAppIds.value
        _comparisonAppIds.value = if (appId in current) {
            current - appId
        } else {
            if (current.size >= 4) {
                _statusBannerMessage.value = "Comparing up to 4 applications side-by-side."
                (current.drop(1) + appId).distinct()
            } else {
                (current + appId).distinct()
            }
        }
    }

    fun clearComparisonApps() {
        _comparisonAppIds.value = emptyList()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectSubcategory(subcategory: String) {
        _selectedSubcategory.value = subcategory
    }

    fun selectSortOption(sortOption: SortOption) {
        _selectedSortOption.value = sortOption
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "All"
        _selectedSubcategory.value = "All"
        _selectedSortOption.value = SortOption.RELEVANCE
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun refreshInstalledAppsStatus() {
        val currentApps = (_publishedAppsState.value as? UiState.Success)?.data ?: emptyList()
        refreshInstalledPackages(currentApps)
        checkGitHubSelfUpdate()
        refreshCachedApks()
        _statusBannerMessage.value = "Scanned device packages & checked GitHub for store updates."
    }

    fun rescanInstalledPackagesSilently() {
        val currentApps = (_publishedAppsState.value as? UiState.Success)?.data ?: return
        refreshInstalledPackages(currentApps)
    }

    private fun checkGitHubSelfUpdate() {
        viewModelScope.launch {
            val remoteRelease = packageController.fetchLatestGitHubStoreRelease()
            if (remoteRelease != null) {
                latestGitHubStoreRelease = remoteRelease

                val currentPublished = (_publishedAppsState.value as? UiState.Success)?.data
                if (currentPublished != null) {
                    val enrichedPublished = enrichCatalogWithLiveStoreRelease(currentPublished)
                    _publishedAppsState.value = UiState.Success(enrichedPublished)
                    refreshInstalledPackages(enrichedPublished)
                }

                val currentAdmin = (_adminAppsState.value as? UiState.Success)?.data
                if (currentAdmin != null) {
                    _adminAppsState.value = UiState.Success(enrichCatalogWithLiveStoreRelease(currentAdmin))
                }

                val selfStatus = packageController.inspectPackage(
                    packageName = remoteRelease.packageName,
                    remoteVersionCode = remoteRelease.versionCode,
                    remoteVersionName = remoteRelease.versionName
                )
                if (selfStatus.updateAvailable) {
                    _storeSelfUpdateApp.value = remoteRelease
                    if ("com.zayaanapps.store" !in notifiedUpdatePackages) {
                        notifiedUpdatePackages.add("com.zayaanapps.store")
                        packageController.postUpdateNotification(remoteRelease)
                    }
                }
            }
        }
    }

    private fun refreshInstalledPackages(apps: List<StoreApp>) {
        viewModelScope.launch {
            val statusMap = withContext(Dispatchers.IO) {
                packageController.inspectCatalogPackages(apps)
            }
            _installedStatuses.value = statusMap

            val selfApp = apps.firstOrNull { it.packageName == "com.zayaanapps.store" }
            val selfStatus = statusMap["com.zayaanapps.store"]
            if (selfApp != null && selfStatus?.updateAvailable == true) {
                _storeSelfUpdateApp.value = selfApp
            }

            val notifyEnabled = _userPreferencesState.value?.notifyUpdates ?: true
            if (notifyEnabled) {
                val updatable = apps.filter { app ->
                    val status = statusMap[app.packageName]
                    status != null && status.updateAvailable && app.packageName !in notifiedUpdatePackages
                }
                if (updatable.isNotEmpty()) {
                    updatable.forEach { notifiedUpdatePackages.add(it.packageName) }
                    packageController.notifyAvailableUpdates(updatable)
                }
            }
        }
    }

    /**
     * Queries the GitHub Releases API (`releases/latest`) for the application's repository,
     * discovers the exact `.apk` asset URL, version tag, and file size, and immediately downloads it.
     */
    fun downloadLatestGitHubReleaseApk(
        app: StoreApp,
        context: Context? = null
    ) {
        val key = app.appId.ifBlank { app.docId }
        viewModelScope.launch {
            _statusBannerMessage.value = "Checking GitHub Releases for latest ${app.appName} APK..."
            val repoCandidate = app.githubRepoUrl.ifBlank {
                if (app.apkDownloadUrl.contains("github.com/", ignoreCase = true)) {
                    app.apkDownloadUrl
                } else {
                    "https://github.com/zayaanapps/zayaan-apps"
                }
            }
            val resolved = packageController.resolveLatestGitHubReleaseAsset(repoCandidate)
                ?: packageController.resolveLatestGitHubReleaseAsset(app.apkDownloadUrl)
                ?: packageController.resolveLatestGitHubReleaseAsset("https://github.com/zayaanapps/zayaan-apps")

            if (resolved != null) {
                val updatedApp = app.copy(
                    versionName = resolved.tagName.ifBlank { app.versionName },
                    versionCode = maxOf(app.versionCode, resolved.versionCode),
                    apkDownloadUrl = resolved.browserDownloadUrl,
                    apkFileSize = resolved.fileSizeFormatted.ifBlank { app.apkFileSize },
                    releaseNotes = resolved.releaseNotes.ifBlank { app.releaseNotes }
                )
                if (updatedApp.packageName == "com.zayaanapps.store" || updatedApp.appId == "zayaan-apps-store") {
                    latestGitHubStoreRelease = updatedApp
                }
                val matchKey = updatedApp.appId.ifBlank { updatedApp.docId }
                val currentPublished = (_publishedAppsState.value as? UiState.Success)?.data
                if (currentPublished != null) {
                    val refreshed = currentPublished.map {
                        if ((it.appId.ifBlank { it.docId }) == matchKey) updatedApp else it
                    }
                    _publishedAppsState.value = UiState.Success(refreshed)
                    refreshInstalledPackages(refreshed)
                }
                val currentAdmin = (_adminAppsState.value as? UiState.Success)?.data
                if (currentAdmin != null) {
                    _adminAppsState.value = UiState.Success(
                        currentAdmin.map { if ((it.appId.ifBlank { it.docId }) == matchKey) updatedApp else it }
                    )
                }
                _statusBannerMessage.value = "Found latest GitHub Release v${updatedApp.versionName} (${resolved.assetName}). Downloading APK..."
                startApkDownload(updatedApp, autoPromptInstallContext = context, forceRedownload = true)
            } else {
                // Fallback: try direct download from app.apkDownloadUrl, or open GitHub Releases page in browser
                val fallbackReleasesPage = if (repoCandidate.contains("github.com/", ignoreCase = true)) {
                    val match = Regex("""https?://github\.com/[^/]+/[^/]+""", RegexOption.IGNORE_CASE).find(repoCandidate)
                    if (match != null) "${match.value.removeSuffix(".git")}/releases/latest" else app.apkDownloadUrl
                } else {
                    "https://github.com/zayaanapps/zayaan-apps/releases/latest"
                }
                if (app.apkDownloadUrl.endsWith(".apk", ignoreCase = true)) {
                    _statusBannerMessage.value = "Downloading latest APK from configured release URL..."
                    startApkDownload(app, autoPromptInstallContext = context, forceRedownload = true)
                } else if (context != null) {
                    _statusBannerMessage.value = "Opening GitHub Releases page ($fallbackReleasesPage)..."
                    packageController.openDownloadUrlInBrowser(context, fallbackReleasesPage)
                } else {
                    startApkDownload(app, forceRedownload = true)
                }
            }
        }
    }

    fun startApkDownload(
        app: StoreApp,
        autoPromptInstallContext: Context? = null,
        forceRedownload: Boolean = false
    ) {
        val key = app.appId.ifBlank { app.docId }
        recordDownloadedAppId(key)
        viewModelScope.launch {
            if (isAuthenticated) {
                repository.incrementDownloadCount(app.docId.ifBlank { app.appId }, app.downloadCount)
            }
            packageController.downloadApkWithProgress(app, forceRedownload = forceRedownload).collect { state ->
                _downloadStates.value = _downloadStates.value + (key to state)
                if (state is DownloadState.ReadyToInstall) {
                    refreshCachedApks()
                    val resolvedVer = state.versionName.ifBlank { app.versionName }
                    val verifyNote = buildString {
                        if (state.checksumVerified) append(" • SHA-256 Verified")
                        if (state.signingCertSha256.isNotBlank() && state.signatureMatchesInstalled) {
                            append(" • Cert Signature Verified")
                        } else if (state.signingCertSha256.isNotBlank()) {
                            append(" • Signature Differs (Uninstall old build first)")
                        }
                    }
                    _statusBannerMessage.value = "${app.appName} v$resolvedVer downloaded$verifyNote."
                    if (autoPromptInstallContext != null) {
                        installDownloadedApk(autoPromptInstallContext, state.apkFilePath)
                    }
                } else if (state is DownloadState.Failed) {
                    _statusBannerMessage.value = state.errorMessage
                }
            }
        }
    }

    fun updateAllAvailableApps(context: Context, updatableApps: List<StoreApp>) {
        if (updatableApps.isEmpty()) {
            _statusBannerMessage.value = "All installed applications are already up to date."
            return
        }
        viewModelScope.launch {
            _statusBannerMessage.value = "Update All started for ${updatableApps.size} app(s)..."
            for (app in updatableApps) {
                val key = app.appId.ifBlank { app.docId }
                recordDownloadedAppId(key)
                if (isAuthenticated) {
                    repository.incrementDownloadCount(app.docId.ifBlank { app.appId }, app.downloadCount)
                }
                packageController.downloadApkWithProgress(app).collect { state ->
                    _downloadStates.value = _downloadStates.value + (key to state)
                    if (state is DownloadState.ReadyToInstall) {
                        refreshCachedApks()
                        installDownloadedApk(context, state.apkFilePath)
                    } else if (state is DownloadState.Failed) {
                        _statusBannerMessage.value = "Failed updating ${app.appName}: ${state.errorMessage}"
                    }
                }
            }
            _statusBannerMessage.value = "Downloaded ${updatableApps.size} update(s). Confirm installation in the Android system dialog."
        }
    }

    fun installDownloadedApk(context: Context, apkFilePath: String) {
        if (!packageController.canRequestPackageInstalls()) {
            _statusBannerMessage.value = "Grant 'Install Unknown Apps' permission in Android Settings to install APK updates."
            packageController.openManageUnknownAppSourcesSettings(context)
            return
        }
        packageController.launchApkInstaller(context, apkFilePath)
            .onSuccess {
                if (_statusBannerMessage.value?.contains("Install Unknown Apps", ignoreCase = true) == true) {
                    _statusBannerMessage.value = null
                }
            }
            .onFailure { e ->
                _statusBannerMessage.value = "Installer error: ${e.localizedMessage}"
            }
    }

    fun uninstallInstalledApp(context: Context, packageName: String) {
        packageController.uninstallPackage(context, packageName)
            .onFailure { e ->
                _statusBannerMessage.value = "Uninstall error: ${e.localizedMessage}"
            }
    }

    fun openInstalledApp(context: Context, packageName: String) {
        val targetPkg = packageName.trim()
        if (targetPkg == context.packageName) {
            android.widget.Toast.makeText(
                context,
                "Zayaan Apps Store is already open.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        packageController.openInstalledApp(context, targetPkg)
            .onFailure {
                val msg = "Could not launch $targetPkg on this device."
                _statusBannerMessage.value = msg
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    fun openExternalDownloadUrl(context: Context, app: StoreApp) {
        val key = app.appId.ifBlank { app.docId }
        if (key != "portal") {
            recordDownloadedAppId(key)
            if (isAuthenticated) {
                viewModelScope.launch {
                    repository.incrementDownloadCount(app.docId.ifBlank { app.appId }, app.downloadCount)
                }
            }
        }
        packageController.openDownloadUrlInBrowser(context, app.apkDownloadUrl)
            .onFailure { e ->
                _statusBannerMessage.value = "Could not open URL: ${e.localizedMessage}"
            }
    }

    fun shareApp(context: Context, app: StoreApp) {
        packageController.shareApplicationLink(context, app)
    }

    fun updateThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        packageController.saveThemeMode(mode)
        if (mode != AppThemeMode.SYSTEM) {
            val currentPref = _userPreferencesState.value ?: UserPreference(userId = currentUserId)
            syncCurrentPreferences(currentPref.copy(darkMode = mode == AppThemeMode.DARK))
        }
    }

    fun updateDarkModePreference(enabled: Boolean) {
        updateThemeMode(if (enabled) AppThemeMode.DARK else AppThemeMode.LIGHT)
    }

    fun updateNotificationPreference(enabled: Boolean) {
        val currentPref = _userPreferencesState.value ?: UserPreference(userId = currentUserId)
        syncCurrentPreferences(currentPref.copy(notifyUpdates = enabled))
    }

    // Ratings, Reviews & Moderation
    fun submitAppReview(app: StoreApp, rating: Long, comment: String) {
        val isVerifiedInstall = _installedStatuses.value[app.packageName]?.isInstalled == true
        val displayName = currentUserEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Zayaan User"
        val appId = app.appId.ifBlank { app.docId }
        val existing = _allReviewsState.value.firstOrNull {
            it.appId == appId && it.userId == currentUserId
        }
        val coercedRating = rating.coerceIn(1L, 5L)
        val cleanComment = comment.trim().take(1500).ifBlank { "Rated $coercedRating stars." }

        if (!isAuthenticated) {
            val nowTs = Timestamp(Date())
            val localReview = AppReview(
                id = "${appId}_${currentUserId}",
                appId = appId,
                appName = app.appName,
                userId = currentUserId,
                userName = displayName,
                rating = coercedRating,
                comment = cleanComment,
                verifiedInstall = isVerifiedInstall,
                helpfulCount = existing?.helpfulCount ?: 0L,
                developerReply = existing?.developerReply ?: "",
                flagged = existing?.flagged ?: false,
                createdAt = existing?.createdAt ?: nowTs,
                updatedAt = nowTs
            )
            val updatedList = listOf(localReview) + _allReviewsState.value.filterNot { it.id == localReview.id }
            _allReviewsState.value = updatedList
            packageController.saveLocalReviewsBackup(updatedList)
            refreshGuestCatalogAggregates(updatedList)
            _statusBannerMessage.value = if (existing != null) {
                "Updated your $coercedRating★ review (saved locally; auto-syncs to Cloud when Sign-In is enabled)."
            } else {
                "Saved your $coercedRating★ review locally (auto-syncs to Cloud when Sign-In is enabled)."
            }
            return
        }
        viewModelScope.launch {
            repository.submitOrUpdateReview(
                app = app,
                userId = currentUserId,
                userName = displayName,
                rating = coercedRating,
                comment = cleanComment,
                verifiedInstall = isVerifiedInstall
            ).onSuccess {
                _statusBannerMessage.value = if (existing != null) {
                    "Updated your $coercedRating★ review for ${app.appName} in Cloud."
                } else {
                    "Published your $coercedRating★ review for ${app.appName} to Cloud."
                }
            }.onFailure { e ->
                _statusBannerMessage.value = "Could not save review to Cloud: ${e.localizedMessage}"
            }
        }
    }

    fun voteReviewHelpful(review: AppReview) {
        if (!isAuthenticated) {
            val updatedList = _allReviewsState.value.map {
                if (it.id == review.id) it.copy(helpfulCount = it.helpfulCount + 1) else it
            }
            _allReviewsState.value = updatedList
            packageController.saveLocalReviewsBackup(updatedList)
            _statusBannerMessage.value = "Marked ${review.userName}'s review as helpful."
            return
        }
        viewModelScope.launch {
            repository.voteReviewHelpful(review)
                .onSuccess {
                    _statusBannerMessage.value = "Marked ${review.userName}'s review as helpful (+1) in Cloud."
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Vote failed: ${e.localizedMessage}"
                }
        }
    }

    fun flagReviewForModeration(review: AppReview, flagged: Boolean = true) {
        if (!isAuthenticated) {
            val updatedList = _allReviewsState.value.map {
                if (it.id == review.id) it.copy(flagged = flagged, updatedAt = Timestamp(Date())) else it
            }
            _allReviewsState.value = updatedList
            packageController.saveLocalReviewsBackup(updatedList)
            _statusBannerMessage.value = if (flagged) {
                "Review flagged locally for moderation."
            } else {
                "Cleared moderation flag on review."
            }
            return
        }
        viewModelScope.launch {
            repository.setReviewFlagged(review, flagged)
                .onSuccess {
                    _statusBannerMessage.value = if (flagged) {
                        "Review flagged for moderator review in Cloud."
                    } else {
                        "Cleared moderation flag on review in Cloud."
                    }
                    if (!flagged) {
                        logAudit("REVIEW_APPROVED", "REVIEW", review.id, "Cleared moderation flag on review by ${review.userName} for ${review.appName}.")
                    }
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Moderation action failed: ${e.localizedMessage}"
                }
        }
    }

    fun replyToReviewAsAdmin(review: AppReview, replyText: String) {
        if (!isAuthenticated) {
            val updatedList = _allReviewsState.value.map {
                if (it.id == review.id) {
                    it.copy(
                        developerReply = replyText.trim().take(1000),
                        flagged = false,
                        updatedAt = Timestamp(Date())
                    )
                } else it
            }
            _allReviewsState.value = updatedList
            packageController.saveLocalReviewsBackup(updatedList)
            _statusBannerMessage.value = "Saved developer reply locally."
            return
        }
        viewModelScope.launch {
            repository.replyToReviewAsAdmin(review, replyText)
                .onSuccess {
                    _statusBannerMessage.value = "Posted developer reply to ${review.userName} in Cloud."
                    logAudit("REVIEW_REPLIED", "REVIEW", review.id, "Posted official developer reply on ${review.appName}.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Reply failed: ${e.localizedMessage}"
                }
        }
    }

    fun deleteReview(review: AppReview) {
        if (!isAuthenticated) {
            val updatedList = _allReviewsState.value.filterNot { it.id == review.id }
            _allReviewsState.value = updatedList
            packageController.saveLocalReviewsBackup(updatedList)
            refreshGuestCatalogAggregates(updatedList)
            _statusBannerMessage.value = "Deleted review by ${review.userName}."
            return
        }
        viewModelScope.launch {
            repository.deleteReview(review.id)
                .onSuccess {
                    _statusBannerMessage.value = "Deleted review by ${review.userName} from Cloud."
                    logAudit("REVIEW_DELETED", "REVIEW", review.id, "Deleted review by ${review.userName} on ${review.appName}.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Delete review failed: ${e.localizedMessage}"
                }
        }
    }

    // Store Release & Update Announcements
    fun publishStoreAnnouncement(title: String, body: String, category: String, targetAppId: String) {
        if (!isAuthenticated) {
            _statusBannerMessage.value = "Broadcast saved locally (cloud push activates when Sign-In is re-enabled)."
            return
        }
        viewModelScope.launch {
            repository.createStoreNotification(
                title = title,
                body = body,
                category = category,
                targetAppId = targetAppId,
                authorUid = currentUserId
            ).onSuccess {
                _statusBannerMessage.value = "Broadcasted store notification: '$title'."
                logAudit("NOTIFICATION_BROADCAST", "NOTIFICATION", targetAppId.ifBlank { "store" }, "Broadcasted [$category] '$title'.")
            }.onFailure { e ->
                _statusBannerMessage.value = "Failed to broadcast notification: ${e.localizedMessage}"
            }
        }
    }

    fun deleteStoreAnnouncement(notificationId: String) {
        viewModelScope.launch {
            repository.deleteStoreNotification(notificationId)
                .onSuccess {
                    _statusBannerMessage.value = "Removed announcement."
                    logAudit("NOTIFICATION_DELETED", "NOTIFICATION", notificationId, "Deleted store notification $notificationId.")
                }
        }
    }

    // Curated Collections & Banners
    fun saveCollection(
        collectionId: String,
        title: String,
        subtitle: String,
        appIds: List<String>,
        isPublic: Boolean,
        featured: Boolean
    ) {
        if (!isAuthenticated) {
            _statusBannerMessage.value = "Collection '$title' saved locally (cloud sync resumes when Sign-In is re-enabled)."
            return
        }
        viewModelScope.launch {
            repository.upsertCollection(
                collectionId = collectionId,
                title = title,
                subtitle = subtitle,
                appIds = appIds,
                isPublic = isPublic,
                featured = featured,
                authorUid = currentUserId
            ).onSuccess {
                _statusBannerMessage.value = "Saved curated collection '$title'."
                logAudit("COLLECTION_SAVED", "COLLECTION", title, "Saved collection '$title' (${appIds.size} apps).")
            }.onFailure { e ->
                _statusBannerMessage.value = "Failed to save collection: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
                .onSuccess {
                    _statusBannerMessage.value = "Deleted collection."
                    logAudit("COLLECTION_DELETED", "COLLECTION", collectionId, "Deleted collection $collectionId.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to delete collection: ${e.localizedMessage}"
                }
        }
    }

    // Developer Submissions & Approval Workflow
    fun submitDeveloperApplication(submission: DeveloperSubmission) {
        if (!isAuthenticated) {
            _submissionsState.value = listOf(submission.copy(id = submission.appId.ifBlank { submission.appName })) + _submissionsState.value
            _statusBannerMessage.value = "Saved developer submission '${submission.appName}' locally (sign in later to submit to cloud)."
            return
        }
        viewModelScope.launch {
            repository.createDeveloperSubmission(submission, currentUserId)
                .onSuccess {
                    _statusBannerMessage.value = "Submitted '${submission.appName}' (v${submission.versionName}) to the Zayaan Developer Portal for review."
                    logAudit("SUBMISSION_CREATED", "SUBMISSION", submission.appId.ifBlank { submission.appName }, "Developer submitted '${submission.appName}' v${submission.versionName}.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Submission failed: ${e.localizedMessage}"
                }
        }
    }

    fun reviewDeveloperSubmission(submission: DeveloperSubmission, newStatus: String, reviewNote: String) {
        viewModelScope.launch {
            repository.reviewDeveloperSubmission(submission, newStatus, reviewNote, currentUserId)
                .onSuccess {
                    _statusBannerMessage.value = if (newStatus == "APPROVED") {
                        "Approved '${submission.appName}' (v${submission.versionName}) and published to Zayaan Apps catalog!"
                    } else {
                        "Marked submission '${submission.appName}' as $newStatus."
                    }
                    logAudit(
                        action = "SUBMISSION_$newStatus",
                        targetType = "SUBMISSION",
                        targetId = submission.id,
                        summary = "$newStatus submission '${submission.appName}' v${submission.versionName}. Note: $reviewNote"
                    )
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Review action failed: ${e.localizedMessage}"
                }
        }
    }

    fun deleteDeveloperSubmission(submissionId: String) {
        viewModelScope.launch {
            repository.deleteDeveloperSubmission(submissionId)
                .onSuccess {
                    _statusBannerMessage.value = "Removed submission entry."
                    logAudit("SUBMISSION_DELETED", "SUBMISSION", submissionId, "Deleted submission $submissionId.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Could not delete submission: ${e.localizedMessage}"
                }
        }
    }

    // Security & Content Reports
    fun submitReport(targetId: String, targetType: String, reason: String, details: String) {
        if (!isAuthenticated) {
            _statusBannerMessage.value = "Report noted locally (cloud submission resumes when Sign-In is re-enabled)."
            return
        }
        viewModelScope.launch {
            repository.submitReport(targetId, targetType, reason, details, currentUserId)
                .onSuccess {
                    _statusBannerMessage.value = "Submitted report ($reason). Store administrators will review it."
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Could not submit report: ${e.localizedMessage}"
                }
        }
    }

    fun updateReportStatus(report: AppReport, newStatus: String) {
        viewModelScope.launch {
            repository.updateReportStatus(report.id, newStatus)
                .onSuccess {
                    _statusBannerMessage.value = "Updated report status to $newStatus."
                    logAudit("REPORT_$newStatus", "REPORT", report.id, "Marked report on ${report.targetType}:${report.targetId} as $newStatus.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to update report: ${e.localizedMessage}"
                }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
                .onSuccess {
                    _statusBannerMessage.value = "Deleted report."
                    logAudit("REPORT_DELETED", "REPORT", reportId, "Deleted report $reportId.")
                }
        }
    }

    // Multi-Version & Release History
    fun addVersionReleaseRecord(
        appId: String,
        versionName: String,
        versionCode: Long,
        apkDownloadUrl: String,
        apkFileSize: String,
        sha256Checksum: String,
        changelog: String
    ) {
        if (!isAuthenticated) {
            _statusBannerMessage.value = "Recorded release v$versionName locally."
            return
        }
        viewModelScope.launch {
            repository.recordAppVersion(
                appId = appId,
                versionName = versionName,
                versionCode = versionCode,
                apkDownloadUrl = apkDownloadUrl,
                apkFileSize = apkFileSize,
                sha256Checksum = sha256Checksum,
                changelog = changelog,
                authorUid = currentUserId
            ).onSuccess {
                _statusBannerMessage.value = "Recorded release v$versionName (code $versionCode) for '$appId'."
                logAudit("VERSION_RECORDED", "APP", appId, "Added release history v$versionName (code $versionCode) for $appId.")
            }.onFailure { e ->
                _statusBannerMessage.value = "Failed to save version record: ${e.localizedMessage}"
            }
        }
    }

    fun deleteVersionReleaseRecord(versionId: String) {
        viewModelScope.launch {
            repository.deleteVersionRecord(versionId)
                .onSuccess {
                    _statusBannerMessage.value = "Removed version history record."
                    logAudit("VERSION_DELETED", "APP", versionId, "Deleted version history record $versionId.")
                }
        }
    }

    // Admin Role Management (RBAC)
    fun grantOrUpdateAdminRole(targetUserId: String, role: String) {
        viewModelScope.launch {
            repository.upsertAdminRole(targetUserId, role)
                .onSuccess {
                    _statusBannerMessage.value = "Assigned role '$role' to UID '$targetUserId'."
                    logAudit("ADMIN_ROLE_GRANTED", "ADMIN", targetUserId, "Granted '$role' role to UID $targetUserId.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to assign admin role: ${e.localizedMessage}"
                }
        }
    }

    fun revokeAdminRole(targetUserId: String) {
        viewModelScope.launch {
            repository.removeAdminRole(targetUserId)
                .onSuccess {
                    _statusBannerMessage.value = "Revoked administrator role for UID '$targetUserId'."
                    logAudit("ADMIN_ROLE_REVOKED", "ADMIN", targetUserId, "Revoked administrator privileges for UID $targetUserId.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to revoke admin role: ${e.localizedMessage}"
                }
        }
    }

    // Admin Catalog CRUD
    fun createOrUpdateApp(app: StoreApp, isEditing: Boolean) {
        if (!isAuthenticated) {
            val id = app.appId.ifBlank { app.docId.ifBlank { app.appName.lowercase().replace(" ", "-") } }
            val saved = app.copy(appId = id, docId = id)
            val currentAdmin = (_adminAppsState.value as? UiState.Success)?.data ?: emptyList()
            val updatedAdmin = if (isEditing) {
                currentAdmin.map { if ((it.appId.ifBlank { it.docId }) == id) saved else it }
            } else {
                listOf(saved) + currentAdmin
            }
            _adminAppsState.value = UiState.Success(updatedAdmin)
            _publishedAppsState.value = UiState.Success(updatedAdmin.filter { it.published })
            _statusBannerMessage.value = "Saved '${saved.appName}' locally (sign in later to sync to Firestore)."
            return
        }
        viewModelScope.launch {
            val result = if (isEditing && (app.docId.isNotBlank() || app.appId.isNotBlank())) {
                repository.updateApp(app, currentUserId)
            } else {
                repository.createApp(app, currentUserId)
            }
            result.onSuccess {
                _statusBannerMessage.value = if (isEditing) {
                    "Updated '${app.appName}' (v${app.versionName}) in Zayaan Apps."
                } else {
                    "Published '${app.appName}' (v${app.versionName}) to Zayaan Apps."
                }
                logAudit(
                    action = if (isEditing) "APP_UPDATED" else "APP_CREATED",
                    targetType = "APP",
                    targetId = app.appId.ifBlank { app.docId.ifBlank { app.appName } },
                    summary = "${if (isEditing) "Updated" else "Created"} '${app.appName}' v${app.versionName} (code ${app.versionCode})."
                )
            }.onFailure { e ->
                _statusBannerMessage.value = "Save failed: ${e.localizedMessage}"
            }
        }
    }

    fun togglePublished(app: StoreApp) {
        if (!isAuthenticated) {
            val nextPublished = !app.published
            val id = app.appId.ifBlank { app.docId }
            val updated = ((_adminAppsState.value as? UiState.Success)?.data ?: emptyList())
                .map { if ((it.appId.ifBlank { it.docId }) == id) it.copy(published = nextPublished) else it }
            _adminAppsState.value = UiState.Success(updated)
            _publishedAppsState.value = UiState.Success(updated.filter { it.published })
            _statusBannerMessage.value = "${if (nextPublished) "Published" else "Unpublished"} '${app.appName}' locally."
            return
        }
        viewModelScope.launch {
            val nextPublished = !app.published
            repository.updateApp(app.copy(published = nextPublished))
                .onSuccess {
                    val stateLabel = if (nextPublished) "Published" else "Unpublished"
                    _statusBannerMessage.value = "$stateLabel '${app.appName}'."
                    logAudit("APP_PUBLISH_TOGGLED", "APP", app.appId.ifBlank { app.docId }, "$stateLabel '${app.appName}'.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to toggle publish state: ${e.localizedMessage}"
                }
        }
    }

    fun toggleFeatured(app: StoreApp) {
        if (!isAuthenticated) {
            val nextFeatured = !app.featured
            val id = app.appId.ifBlank { app.docId }
            val updated = ((_adminAppsState.value as? UiState.Success)?.data ?: emptyList())
                .map { if ((it.appId.ifBlank { it.docId }) == id) it.copy(featured = nextFeatured) else it }
            _adminAppsState.value = UiState.Success(updated)
            _publishedAppsState.value = UiState.Success(updated.filter { it.published })
            return
        }
        viewModelScope.launch {
            val nextFeatured = !app.featured
            repository.updateApp(app.copy(featured = nextFeatured))
                .onSuccess {
                    val stateLabel = if (nextFeatured) "Featured" else "Removed from Featured"
                    _statusBannerMessage.value = "$stateLabel '${app.appName}'."
                    logAudit("APP_FEATURED_TOGGLED", "APP", app.appId.ifBlank { app.docId }, "$stateLabel '${app.appName}'.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to toggle featured status: ${e.localizedMessage}"
                }
        }
    }

    fun toggleEditorsChoice(app: StoreApp) {
        if (!isAuthenticated) {
            val nextChoice = !app.editorsChoice
            val id = app.appId.ifBlank { app.docId }
            val updated = ((_adminAppsState.value as? UiState.Success)?.data ?: emptyList())
                .map { if ((it.appId.ifBlank { it.docId }) == id) it.copy(editorsChoice = nextChoice) else it }
            _adminAppsState.value = UiState.Success(updated)
            _publishedAppsState.value = UiState.Success(updated.filter { it.published })
            return
        }
        viewModelScope.launch {
            val nextChoice = !app.editorsChoice
            repository.updateApp(app.copy(editorsChoice = nextChoice))
                .onSuccess {
                    val stateLabel = if (nextChoice) "Marked as Editor's Choice" else "Removed Editor's Choice"
                    _statusBannerMessage.value = "$stateLabel: '${app.appName}'."
                    logAudit("APP_EDITORS_CHOICE_TOGGLED", "APP", app.appId.ifBlank { app.docId }, "$stateLabel for '${app.appName}'.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Update failed: ${e.localizedMessage}"
                }
        }
    }

    fun toggleEssential(app: StoreApp) {
        if (!isAuthenticated) {
            val nextEssential = !app.essential
            val id = app.appId.ifBlank { app.docId }
            val updated = ((_adminAppsState.value as? UiState.Success)?.data ?: emptyList())
                .map { if ((it.appId.ifBlank { it.docId }) == id) it.copy(essential = nextEssential) else it }
            _adminAppsState.value = UiState.Success(updated)
            _publishedAppsState.value = UiState.Success(updated.filter { it.published })
            return
        }
        viewModelScope.launch {
            val nextEssential = !app.essential
            repository.updateApp(app.copy(essential = nextEssential))
                .onSuccess {
                    val stateLabel = if (nextEssential) "Added to Essential Apps" else "Removed from Essential Apps"
                    _statusBannerMessage.value = "$stateLabel: '${app.appName}'."
                    logAudit("APP_ESSENTIAL_TOGGLED", "APP", app.appId.ifBlank { app.docId }, "$stateLabel for '${app.appName}'.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Update failed: ${e.localizedMessage}"
                }
        }
    }

    fun deleteApp(app: StoreApp) {
        val docId = app.docId.ifBlank { app.appId }
        if (!isAuthenticated) {
            val updated = ((_adminAppsState.value as? UiState.Success)?.data ?: emptyList())
                .filterNot { (it.appId.ifBlank { it.docId }) == docId }
            _adminAppsState.value = UiState.Success(updated)
            _publishedAppsState.value = UiState.Success(updated.filter { it.published })
            _statusBannerMessage.value = "Removed '${app.appName}' locally."
            return
        }
        viewModelScope.launch {
            repository.deleteApp(docId)
                .onSuccess {
                    _statusBannerMessage.value = "Deleted '${app.appName}' from catalog."
                    logAudit("APP_DELETED", "APP", docId, "Deleted application '${app.appName}' ($docId) from catalog.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Delete failed: ${e.localizedMessage}"
                }
        }
    }

    fun addCategory(name: String, description: String, order: Long) {
        if (!isAuthenticated) {
            val current = (_categoriesState.value as? UiState.Success)?.data ?: emptyList()
            val id = name.lowercase().replace(" ", "-")
            val updated = (current.filterNot { it.id == id } + StoreCategory(id, name, description, order)).sortedBy { it.displayOrder }
            _categoriesState.value = UiState.Success(updated)
            _statusBannerMessage.value = "Saved category '$name' locally."
            return
        }
        viewModelScope.launch {
            repository.upsertCategory(name, description, order, currentUserId)
                .onSuccess {
                    _statusBannerMessage.value = "Saved category '$name'."
                    logAudit("CATEGORY_SAVED", "CATEGORY", name, "Saved category '$name' (order $order).")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to save category: ${e.localizedMessage}"
                }
        }
    }

    fun deleteCategory(docId: String) {
        if (!isAuthenticated) {
            val current = (_categoriesState.value as? UiState.Success)?.data ?: emptyList()
            _categoriesState.value = UiState.Success(current.filterNot { it.id == docId })
            _statusBannerMessage.value = "Category removed locally."
            return
        }
        viewModelScope.launch {
            repository.deleteCategory(docId)
                .onSuccess {
                    _statusBannerMessage.value = "Category removed."
                    logAudit("CATEGORY_DELETED", "CATEGORY", docId, "Deleted category '$docId'.")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to delete category: ${e.localizedMessage}"
                }
        }
    }

    fun seedCatalogNow() {
        if (!isAuthenticated) {
            loadGuestPreviewStore()
            _statusBannerMessage.value = "Official 8 Zayaan Apps catalog loaded."
            return
        }
        viewModelScope.launch {
            repository.seedOfficialZayaanCatalog(currentUserId)
                .onSuccess { count ->
                    _statusBannerMessage.value = if (count > 0) {
                        "Seeded $count official Zayaan Apps into Firestore."
                    } else {
                        "All starter Zayaan Apps are already present in the catalog."
                    }
                    logAudit("CATALOG_SYNCED", "CATALOG", "official-zayaan-catalog", "Synced official Zayaan Apps catalog ($count new apps added).")
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Failed to seed catalog: ${e.localizedMessage}"
                }
        }
    }
}
