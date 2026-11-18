package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

val STORE_CATEGORIES = listOf(
    "Games",
    "Tools",
    "AI Apps",
    "Entertainment",
    "Productivity",
    "Education",
    "Utilities",
    "Other"
)

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
    val iconUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val versionName: String = "1.0.0",
    val versionCode: Long = 1L,
    val apkDownloadUrl: String = "",
    val apkFileSize: String = "12.0 MB",
    val releaseNotes: String = "",
    val featured: Boolean = false,
    val published: Boolean = true,
    val downloadCount: Long = 0L,
    val authorUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

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
)

@Keep
@IgnoreExtraProperties
data class UserPreference(
    @DocumentId val id: String = "",
    val userId: String = "",
    val darkMode: Boolean = true,
    val notifyUpdates: Boolean = true,
    val bookmarkedAppIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class InstalledAppStatus(
    val packageName: String,
    val installed: Boolean,
    val installedVersionName: String? = null,
    val installedVersionCode: Long = 0L,
    val updateAvailable: Boolean = false
)

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val appId: String, val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState
    data class ReadyToInstall(val appId: String, val apkFilePath: String) : DownloadState
    data class Failed(val appId: String, val message: String) : DownloadState
}
