package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class AndroidPackageController(private val context: Context) {

    private val appContext = context.applicationContext
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val UPDATE_CHANNEL_ID = "zayaan_app_updates"
    }

    init {
        createUpdateNotificationChannel()
    }

    private fun createUpdateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "Zayaan App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when installed Zayaan Apps have newer versions available."
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Inspects the Android device's PackageManager for the given packageName and compares
     * its installed versionCode against the store catalog's latest versionCode.
     */
    fun getInstalledStatus(packageName: String, storeVersionCode: Long): InstalledAppStatus {
        if (packageName.isBlank()) {
            return InstalledAppStatus(packageName = "", installed = false)
        }
        return try {
            val pm = appContext.packageManager
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            val installedCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val installedName = packageInfo.versionName ?: "1.0"
            InstalledAppStatus(
                packageName = packageName,
                installed = true,
                installedVersionName = installedName,
                installedVersionCode = installedCode,
                updateAvailable = storeVersionCode > installedCode
            )
        } catch (_: PackageManager.NameNotFoundException) {
            InstalledAppStatus(
                packageName = packageName,
                installed = false,
                installedVersionName = null,
                installedVersionCode = 0L,
                updateAvailable = false
            )
        } catch (_: Exception) {
            InstalledAppStatus(
                packageName = packageName,
                installed = false
            )
        }
    }

    /**
     * Scans a list of store apps and returns a map of packageName -> InstalledAppStatus.
     */
    fun scanCatalogStatus(apps: List<StoreApp>): Map<String, InstalledAppStatus> {
        val result = mutableMapOf<String, InstalledAppStatus>()
        for (app in apps) {
            if (app.packageName.isNotBlank()) {
                result[app.packageName] = getInstalledStatus(app.packageName, app.versionCode)
            }
        }
        return result
    }

    /**
     * Posts a local system notification if updates are available and POST_NOTIFICATIONS is granted.
     */
    fun notifyAvailableUpdates(updatableApps: List<StoreApp>) {
        if (updatableApps.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val title = if (updatableApps.size == 1) {
            "Update available for ${updatableApps.first().appName}"
        } else {
            "${updatableApps.size} app updates available in Zayaan Apps"
        }
        val body = updatableApps.joinToString(", ") { "${it.appName} (v${it.versionName})" }

        val notification = NotificationCompat.Builder(appContext, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }

    /**
     * Downloads an APK file from the given GitHub Releases URL with real-time byte progress.
     */
    fun downloadApk(app: StoreApp): Flow<DownloadState> = flow {
        val appId = app.appId.ifBlank { app.docId }
        val url = app.apkDownloadUrl.trim()
        if (url.isBlank() || (!url.startsWith("https://") && !url.startsWith("http://"))) {
            emit(DownloadState.Failed(appId, "Invalid APK download URL: must start with https://"))
            return@flow
        }

        emit(DownloadState.Downloading(appId, 0, 0L, 0L))

        val apkDir = File(appContext.cacheDir, "apks").apply {
            if (!exists()) mkdirs()
        }
        val safeFileName = "${appId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}-v${app.versionCode}.apk"
        val targetFile = File(apkDir, safeFileName)

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ZayaanAppsStore-Android/2.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(
                        DownloadState.Failed(
                            appId,
                            "GitHub Release returned HTTP ${response.code}. Verify the release asset URL exists or open in browser."
                        )
                    )
                    return@flow
                }

                val body = response.body
                if (body == null) {
                    emit(DownloadState.Failed(appId, "Empty response body from release server."))
                    return@flow
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val buffer = ByteArray(8 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        var bytesRead: Int
                        var lastEmittedPercent = -1
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                50
                            }
                            if (percent != lastEmittedPercent) {
                                lastEmittedPercent = percent
                                emit(DownloadState.Downloading(appId, percent, downloadedBytes, totalBytes))
                            }
                        }
                        output.flush()
                    }
                }
            }

            emit(DownloadState.ReadyToInstall(appId, targetFile.absolutePath))
        } catch (e: Exception) {
            emit(
                DownloadState.Failed(
                    appId,
                    e.localizedMessage ?: "Network error while downloading APK from GitHub Releases."
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Launches the standard Android system package installer confirmation dialog for a downloaded APK file.
     * Respects Android security restrictions and user confirmation requirements.
     */
    fun launchApkInstaller(activityContext: Context, apkFilePath: String): Result<Unit> {
        return try {
            val file = File(apkFilePath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("Downloaded APK file not found at $apkFilePath"))
            }
            val apkUri: Uri = FileProvider.getUriForFile(
                activityContext,
                "${activityContext.packageName}.fileprovider",
                file
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Opens an already installed application via its launcher Intent.
     */
    fun openInstalledApp(activityContext: Context, packageName: String): Result<Unit> {
        return try {
            val launchIntent = activityContext.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activityContext.startActivity(launchIntent)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("No launch activity found for $packageName"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Opens a GitHub Releases APK download URL directly in the external browser.
     */
    fun openDownloadUrlInBrowser(activityContext: Context, url: String): Result<Unit> {
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(browserIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queries https://api.github.com/repos/zayaanapps/zayaan-apps/releases/latest
     * to check if a newer 1.0.0.X release tag of Zayaan Apps Store itself was built on GitHub Actions.
     */
    suspend fun fetchLatestGitHubStoreRelease(): StoreApp? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/zayaanapps/zayaan-apps/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ZayaanAppsStore-Android/1.0.0.1")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val jsonStr = response.body?.string() ?: return@withContext null
                val json = org.json.JSONObject(jsonStr)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                if (tagName.isEmpty()) return@withContext null

                // Extract build number from 4-part version 1.0.0.X (last segment)
                val lastSegment = tagName.substringAfterLast('.', "").toLongOrNull() ?: 1L
                val releaseNotes = json.optString("body", "Automated GitHub Actions release v$tagName")

                var downloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk"
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val assetUrl = asset.optString("browser_download_url", "")
                        if (assetUrl.endsWith(".apk")) {
                            downloadUrl = assetUrl
                            break
                        }
                    }
                }

                StoreApp(
                    appId = "zayaan-apps-store",
                    appName = "Zayaan Apps Store",
                    packageName = "com.zayaanapps.store",
                    category = "Tools",
                    developerName = "Zayaan Apps Official",
                    description = "Official Zayaan Apps Store self-update package from GitHub Releases.",
                    versionName = tagName,
                    versionCode = lastSegment,
                    apkDownloadUrl = downloadUrl,
                    apkFileSize = "16.8 MB",
                    releaseNotes = releaseNotes,
                    featured = true,
                    published = true
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
