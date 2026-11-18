package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AndroidPackageController
import com.example.data.DownloadState
import com.example.data.InstalledAppStatus
import com.example.data.StoreApp
import com.example.data.StoreCategory
import com.example.data.StoreRepository
import com.example.data.UserPreference
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class StoreViewModel(
    private val repository: StoreRepository,
    private val packageController: AndroidPackageController,
    val currentUserId: String,
    val currentUserEmail: String?
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _installedStatuses = MutableStateFlow<Map<String, InstalledAppStatus>>(emptyMap())
    val installedStatuses: StateFlow<Map<String, InstalledAppStatus>> = _installedStatuses.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _storeSelfUpdateApp = MutableStateFlow<StoreApp?>(null)
    val storeSelfUpdateApp: StateFlow<StoreApp?> = _storeSelfUpdateApp.asStateFlow()

    private var hasSeededCatalogInSession = false

    init {
        checkStoreSelfUpdateFromGitHub()
    }

    fun checkStoreSelfUpdateFromGitHub() {
        viewModelScope.launch {
            val githubRelease = packageController.fetchLatestGitHubStoreRelease()
            if (githubRelease != null) {
                val localSelfStatus = packageController.getInstalledStatus(
                    "com.zayaanapps.store",
                    githubRelease.versionCode
                )
                if (localSelfStatus.updateAvailable) {
                    _storeSelfUpdateApp.value = githubRelease
                }
            }
        }
    }

    val publishedAppsState: StateFlow<UiState<List<StoreApp>>> = repository.observePublishedApps()
        .map<List<StoreApp>, UiState<List<StoreApp>>> { apps ->
            // Scan installed statuses whenever catalog updates
            val statusMap = packageController.scanCatalogStatus(apps)
            _installedStatuses.value = statusMap

            // Check if Zayaan Apps Store (com.zayaanapps.store) has a newer version in Firestore
            val storeCatalogEntry = apps.firstOrNull { it.packageName == "com.zayaanapps.store" }
            if (storeCatalogEntry != null) {
                val selfStatus = statusMap["com.zayaanapps.store"]
                val currentCandidate = _storeSelfUpdateApp.value
                if (selfStatus?.updateAvailable == true) {
                    if (currentCandidate == null || storeCatalogEntry.versionCode >= currentCandidate.versionCode) {
                        _storeSelfUpdateApp.value = storeCatalogEntry
                    }
                }
            }

            if (apps.isEmpty() && !hasSeededCatalogInSession) {
                hasSeededCatalogInSession = true
                viewModelScope.launch {
                    repository.seedOfficialZayaanCatalog()
                }
            }
            UiState.Success(apps)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w("StoreVM", "Error observing published apps", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load application catalog"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val adminAppsState: StateFlow<UiState<List<StoreApp>>> = combine(
        repository.observePublishedApps(),
        repository.observeMyAuthoredApps(currentUserId)
    ) { published, authored ->
        val merged = (published + authored)
            .associateBy { it.appId.ifBlank { it.docId } }
            .values
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        UiState.Success(merged) as UiState<List<StoreApp>>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(UiState.Error(rootError.message ?: "Failed to load admin catalog"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val categoriesState: StateFlow<UiState<List<StoreCategory>>> = repository.observeCategories()
        .map<List<StoreCategory>, UiState<List<StoreCategory>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(UiState.Error(rootError.message ?: "Failed to load categories"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val userPreferencesState: StateFlow<UserPreference?> = repository.observeUserPreferences(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun refreshInstalledAppsStatus() {
        val currentState = publishedAppsState.value
        if (currentState is UiState.Success) {
            val statusMap = packageController.scanCatalogStatus(currentState.data)
            _installedStatuses.value = statusMap
            val updatable = currentState.data.filter { app ->
                statusMap[app.packageName]?.updateAvailable == true
            }
            if (updatable.isNotEmpty() && (userPreferencesState.value?.notifyUpdates != false)) {
                packageController.notifyAvailableUpdates(updatable)
            }
            _statusBannerMessage.value = "Scanned ${currentState.data.size} catalog packages: ${updatable.size} update(s) available."
        }
    }

    fun startApkDownload(app: StoreApp) {
        val appId = app.appId.ifBlank { app.docId }
        viewModelScope.launch {
            repository.incrementDownloadCount(appId)
            packageController.downloadApk(app).collect { state ->
                _downloadStates.value = _downloadStates.value.toMutableMap().apply {
                    put(appId, state)
                }
                if (state is DownloadState.ReadyToInstall) {
                    _statusBannerMessage.value = "Downloaded ${app.appName} v${app.versionName}. Tap 'Install APK' to launch Android Package Installer."
                } else if (state is DownloadState.Failed) {
                    _statusBannerMessage.value = state.message
                }
            }
        }
    }

    fun installDownloadedApk(activityContext: Context, apkFilePath: String) {
        val res = packageController.launchApkInstaller(activityContext, apkFilePath)
        if (res.isFailure) {
            _statusBannerMessage.value = res.exceptionOrNull()?.localizedMessage
                ?: "Unable to launch Android Package Installer."
        }
    }

    fun openInstalledApp(activityContext: Context, packageName: String) {
        val res = packageController.openInstalledApp(activityContext, packageName)
        if (res.isFailure) {
            _statusBannerMessage.value = "Could not open $packageName directly."
        }
    }

    fun openExternalDownloadUrl(activityContext: Context, app: StoreApp) {
        val appId = app.appId.ifBlank { app.docId }
        viewModelScope.launch {
            repository.incrementDownloadCount(appId)
        }
        val res = packageController.openDownloadUrlInBrowser(activityContext, app.apkDownloadUrl)
        if (res.isFailure) {
            _statusBannerMessage.value = "Unable to open browser for URL: ${app.apkDownloadUrl}"
        }
    }

    fun createOrUpdateApp(app: StoreApp, isEdit: Boolean, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = if (isEdit) {
                repository.updateApp(app)
            } else {
                repository.createApp(app).map { }
            }
            result.fold(
                onSuccess = {
                    _statusBannerMessage.value = if (isEdit) {
                        "Updated '${app.appName}' (v${app.versionName}) in Cloud Firestore."
                    } else {
                        "Published '${app.appName}' to Zayaan Apps Store (Android & Web)."
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    _statusBannerMessage.value = "Save failed: ${err.localizedMessage ?: "Permission or validation error"}"
                }
            )
        }
    }

    fun togglePublished(app: StoreApp) {
        val targetId = app.appId.ifBlank { app.docId }
        viewModelScope.launch {
            val newStatus = !app.published
            val res = repository.setAppPublished(targetId, newStatus)
            res.fold(
                onSuccess = {
                    _statusBannerMessage.value = if (newStatus) {
                        "'${app.appName}' is now Published on Android & Web."
                    } else {
                        "'${app.appName}' is now Unpublished (Draft)."
                    }
                },
                onFailure = { err ->
                    _statusBannerMessage.value = "Failed to change visibility: ${err.localizedMessage}"
                }
            )
        }
    }

    fun toggleFeatured(app: StoreApp) {
        val targetId = app.appId.ifBlank { app.docId }
        viewModelScope.launch {
            val newStatus = !app.featured
            val res = repository.setAppFeatured(targetId, newStatus)
            res.fold(
                onSuccess = {
                    _statusBannerMessage.value = if (newStatus) {
                        "'${app.appName}' added to Featured carousel."
                    } else {
                        "'${app.appName}' removed from Featured carousel."
                    }
                },
                onFailure = { err ->
                    _statusBannerMessage.value = "Failed to update featured status: ${err.localizedMessage}"
                }
            )
        }
    }

    fun deleteApp(app: StoreApp) {
        val targetId = app.appId.ifBlank { app.docId }
        viewModelScope.launch {
            val res = repository.deleteApp(targetId)
            res.fold(
                onSuccess = {
                    _statusBannerMessage.value = "Deleted '${app.appName}' from catalog."
                },
                onFailure = { err ->
                    _statusBannerMessage.value = "Delete failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun addCategory(name: String, description: String, order: Long) {
        viewModelScope.launch {
            val res = repository.addCategory(name, description, order)
            res.fold(
                onSuccess = {
                    _statusBannerMessage.value = "Category '$name' saved."
                },
                onFailure = { err ->
                    _statusBannerMessage.value = "Category save failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    fun seedCatalogNow() {
        viewModelScope.launch {
            val res = repository.seedOfficialZayaanCatalog()
            res.fold(
                onSuccess = { count ->
                    _statusBannerMessage.value = "Restored $count official Zayaan Apps to Cloud Firestore."
                },
                onFailure = { err ->
                    _statusBannerMessage.value = "Seed failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun updateDarkModePreference(darkMode: Boolean) {
        val current = userPreferencesState.value
        viewModelScope.launch {
            repository.saveUserPreferences(
                darkMode = darkMode,
                notifyUpdates = current?.notifyUpdates ?: true,
                bookmarkedAppIds = current?.bookmarkedAppIds ?: emptyList(),
                isExistingDoc = current != null
            )
        }
    }

    fun updateNotificationPreference(notifyUpdates: Boolean) {
        val current = userPreferencesState.value
        viewModelScope.launch {
            repository.saveUserPreferences(
                darkMode = current?.darkMode ?: true,
                notifyUpdates = notifyUpdates,
                bookmarkedAppIds = current?.bookmarkedAppIds ?: emptyList(),
                isExistingDoc = current != null
            )
        }
    }

    fun toggleBookmark(appId: String) {
        val current = userPreferencesState.value
        val existingList = current?.bookmarkedAppIds ?: emptyList()
        val updatedList = if (appId in existingList) {
            existingList - appId
        } else {
            (existingList + appId).take(50)
        }
        viewModelScope.launch {
            repository.saveUserPreferences(
                darkMode = current?.darkMode ?: true,
                notifyUpdates = current?.notifyUpdates ?: true,
                bookmarkedAppIds = updatedList,
                isExistingDoc = current != null
            )
        }
    }
}
