package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

val STORE_CATEGORIES = listOf(
    "Games",
    "Tools",
    "AI Apps",
    "Entertainment",
    "Productivity",
    "Education",
    "Utilities",
    "Business",
    "Social & Communication",
    "Other"
)

enum class SortOption(val label: String) {
    RELEVANCE("Relevance"),
    POPULARITY("Most Popular"),
    HIGHEST_RATED("Highest Rated"),
    RECENTLY_UPDATED("Recently Updated"),
    RECENTLY_ADDED("Recently Added"),
    ALPHABETICAL("Alphabetical (A-Z)"),
    FILE_SIZE("Smallest Size")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

@Keep
@IgnoreExtraProperties
data class StoreApp(
    @DocumentId val docId: String = "",
    val appId: String = "",
    val appName: String = "",
    val packageName: String = "",
    val category: String = "Tools",
    val developerName: String = "Zayaan Apps Studio",
    val description: String = "",
    val shortDescription: String = "",
    val iconUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val versionName: String = "1.0.0.1",
    val versionCode: Long = 1L,
    val apkDownloadUrl: String = "",
    val apkFileSize: String = "12.0 MB",
    val releaseNotes: String = "",
    val featured: Boolean = false,
    val editorsChoice: Boolean = false,
    val essential: Boolean = false,
    val published: Boolean = true,
    val downloadCount: Long = 0L,
    val ratingSum: Long = 0L,
    val reviewCount: Long = 0L,
    val sha256Checksum: String = "",
    val minAndroidSdk: Long = 24L,
    val targetAndroidSdk: Long = 36L,
    val architecture: String = "arm64-v8a, armeabi-v7a, universal",
    val permissionsList: List<String> = listOf("INTERNET", "ACCESS_NETWORK_STATE"),
    val privacyPolicyUrl: String = "https://zayaanapps.github.io/zayaan-apps/",
    val websiteUrl: String = "https://zayaanapps.github.io/zayaan-apps/",
    val supportEmail: String = "zayaanapps@gmail.com",
    val githubRepoUrl: String = "https://github.com/zayaanapps/zayaan-apps",
    val promoVideoUrl: String = "",
    val offlineCapable: Boolean = true,
    val pageViews: Long = 0L,
    val authorUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val averageRating: Double
        get() = if (reviewCount > 0L) (ratingSum.toDouble() / reviewCount.toDouble()).coerceIn(0.0, 5.0) else 0.0
}

@Keep
@IgnoreExtraProperties
data class StoreCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val displayOrder: Long = 0L,
    val authorUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val docId: String
        get() = id
}

@Keep
@IgnoreExtraProperties
data class UserPreference(
    @DocumentId val id: String = "",
    val userId: String = "",
    val darkMode: Boolean = true,
    val notifyUpdates: Boolean = true,
    val bookmarkedAppIds: List<String> = emptyList(),
    val favoriteAppIds: List<String> = emptyList(),
    val recentlyViewedAppIds: List<String> = emptyList(),
    val downloadedAppIds: List<String> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppReview(
    @DocumentId val id: String = "",
    val appId: String = "",
    val appName: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val verifiedInstall: Boolean = false,
    val helpfulCount: Long = 0L,
    val developerReply: String = "",
    val flagged: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppCollection(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val appIds: List<String> = emptyList(),
    val authorUid: String = "",
    @get:PropertyName("isPublic") @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val featured: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeveloperSubmission(
    @DocumentId val id: String = "",
    val appId: String = "",
    val appName: String = "",
    val packageName: String = "",
    val category: String = "Tools",
    val developerName: String = "",
    val description: String = "",
    val versionName: String = "1.0.0.1",
    val versionCode: Long = 1L,
    val apkDownloadUrl: String = "",
    val apkFileSize: String = "12.0 MB",
    val sha256Checksum: String = "",
    val releaseNotes: String = "",
    val privacyPolicyUrl: String = "https://zayaanapps.github.io/zayaan-apps/",
    val supportEmail: String = "zayaanapps@gmail.com",
    val status: String = "PENDING",
    val reviewNote: String = "",
    val submitterUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoreNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "ANNOUNCEMENT",
    val targetAppId: String = "",
    val published: Boolean = true,
    val authorUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppReport(
    @DocumentId val id: String = "",
    val targetId: String = "",
    val targetType: String = "APP",
    val reason: String = "",
    val details: String = "",
    val reporterUid: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppVersionRecord(
    @DocumentId val id: String = "",
    val appId: String = "",
    val versionName: String = "1.0.0.1",
    val versionCode: Long = 1L,
    val apkDownloadUrl: String = "",
    val apkFileSize: String = "12.0 MB",
    val sha256Checksum: String = "",
    val changelog: String = "",
    val authorUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "admin",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLogRecord(
    @DocumentId val id: String = "",
    val action: String = "",
    val targetType: String = "APP",
    val targetId: String = "",
    val summary: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DeveloperProfileSummary(
    val developerName: String,
    val supportEmail: String,
    val websiteUrl: String,
    val githubRepoUrl: String,
    val privacyPolicyUrl: String,
    val apps: List<StoreApp>,
    val totalDownloads: Long,
    val totalReviews: Long,
    val averageRating: Double
)

data class ApkArchiveVerificationInfo(
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sha256Checksum: String,
    val signingCertSha256: String,
    val isInstalledOnDevice: Boolean,
    val signatureMatchesInstalled: Boolean
)

data class ResolvedGitHubReleaseAsset(
    val owner: String,
    val repo: String,
    val tagName: String,
    val versionCode: Long,
    val assetName: String,
    val browserDownloadUrl: String,
    val fileSizeFormatted: String,
    val releaseNotes: String,
    val releaseHtmlUrl: String
)

data class InstalledAppStatus(
    val packageName: String,
    val installed: Boolean,
    val installedVersionName: String? = null,
    val installedVersionCode: Long = 0L,
    val updateAvailable: Boolean = false
) {
    val isInstalled: Boolean
        get() = installed
}

typealias InstalledPackageStatus = InstalledAppStatus

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val appId: String,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState
    data class Paused(
        val appId: String,
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadState
    data class ReadyToInstall(
        val appId: String,
        val apkFilePath: String,
        val versionName: String = "",
        val versionCode: Long = 0L,
        val computedSha256: String = "",
        val checksumVerified: Boolean = true,
        val signingCertSha256: String = "",
        val signatureMatchesInstalled: Boolean = true
    ) : DownloadState
    data class Failed(val appId: String, val message: String) : DownloadState {
        val errorMessage: String
            get() = message
    }
}
