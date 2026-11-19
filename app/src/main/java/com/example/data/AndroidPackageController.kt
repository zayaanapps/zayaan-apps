package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.google.firebase.Timestamp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AndroidPackageController(private val context: Context) {

    private val appContext = context.applicationContext
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val cancelledDownloads = ConcurrentHashMap<String, Boolean>()

    companion object {
        private const val UPDATE_CHANNEL_ID = "zayaan_app_updates"
        private const val PREFS_NAME = "zayaan_store_local_cache"
        private const val KEY_PENDING_REVIEWS_JSON = "pending_reviews_json"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_PENDING_INSTALL_APKS = "pending_install_apks_json"
    }

    init {
        createUpdateNotificationChannel()
    }

    private fun createUpdateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "Zayaan App Updates & Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for available updates, new releases, and completed APK downloads."
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun cancelDownload(appId: String) {
        cancelledDownloads[appId] = true
    }

    /**
     * Checks if notification permission is granted (required on Android 13+).
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Checks if the user has granted Android's "Install Unknown Apps" permission for Zayaan Apps.
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                appContext.packageManager.canRequestPackageInstalls()
            } catch (_: Exception) {
                true
            }
        } else {
            true
        }
    }

    /**
     * Opens the official Android system settings screen to allow installing unknown apps from Zayaan Apps.
     */
    fun buildManageUnknownAppSourcesIntent(activityContext: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activityContext.packageName}")
            )
        } else {
            null
        }
    }

    fun openManageUnknownAppSourcesSettings(activityContext: Context): Result<Unit> {
        return try {
            val intent = buildManageUnknownAppSourcesIntent(activityContext)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activityContext.startActivity(intent)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Computes the real SHA-256 hex digest of a downloaded APK file for pre-installation verification.
     */
    fun computeFileSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    private fun computeBytesSha256(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(bytes)
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Extracts the SHA-256 fingerprint of a PackageInfo's signing certificate.
     */
    private fun extractSigningCertificateSha256(packageInfo: PackageInfo?): String {
        if (packageInfo == null) return ""
        return try {
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                val sigs = if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else null
                sigs?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.firstOrNull()?.toByteArray()
            }
            if (certBytes != null) computeBytesSha256(certBytes) else ""
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Inspects an APK file on disk using PackageManager.getPackageArchiveInfo to extract its
     * package name, versionName, versionCode, SHA-256 file digest, signing certificate SHA-256,
     * and checks whether its signing certificate matches the currently installed package on device.
     */
    fun inspectApkArchiveInfo(file: File): ApkArchiveVerificationInfo {
        val pm = appContext.packageManager
        val fileSha = if (file.exists()) computeFileSha256(file) else ""
        val archiveInfo: PackageInfo? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    file.absolutePath,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
            }
        } catch (_: Exception) {
            null
        }

        val pkgName = archiveInfo?.packageName ?: ""
        val verName = archiveInfo?.versionName ?: ""
        val verCode = if (archiveInfo != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.versionCode.toLong()
            }
        } else 0L
        val archiveCertSha = extractSigningCertificateSha256(archiveInfo)

        var installedOnDevice = false
        var signatureMatches = true
        if (pkgName.isNotBlank()) {
            try {
                val installedInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(
                        pkgName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES)
                }
                installedOnDevice = true
                val installedCertSha = extractSigningCertificateSha256(installedInfo)
                if (archiveCertSha.isNotBlank() && installedCertSha.isNotBlank()) {
                    signatureMatches = archiveCertSha.equals(installedCertSha, ignoreCase = true)
                }
            } catch (_: Exception) {
                installedOnDevice = false
            }
        }

        return ApkArchiveVerificationInfo(
            filePath = file.absolutePath,
            fileName = file.name,
            fileSizeBytes = if (file.exists()) file.length() else 0L,
            packageName = pkgName,
            versionName = verName,
            versionCode = verCode,
            sha256Checksum = fileSha,
            signingCertSha256 = archiveCertSha,
            isInstalledOnDevice = installedOnDevice,
            signatureMatchesInstalled = signatureMatches
        )
    }

    private fun apkStorageDirectories(): List<File> = listOfNotNull(
        File(appContext.cacheDir, "apks"),
        File(appContext.filesDir, "apks"),
        appContext.externalCacheDir?.let { File(it, "apks") }
    )

    /**
     * Lists all locally downloaded APK files stored in the cache/internal APK directories.
     */
    fun listDownloadedApkFiles(): List<File> {
        return apkStorageDirectories()
            .filter { it.exists() }
            .flatMap { dir ->
                dir.listFiles { f -> f.isFile && f.name.endsWith(".apk", ignoreCase = true) }?.toList().orEmpty()
            }
            .sortedByDescending { it.lastModified() }
    }

    fun deleteDownloadedApkFile(apkFilePath: String): Boolean {
        return try {
            val file = File(apkFilePath)
            val partFile = File("$apkFilePath.part")
            val partDeleted = if (partFile.exists()) partFile.delete() else false
            val fileDeleted = if (file.exists()) file.delete() else false
            removePendingInstallByPath(apkFilePath)
            fileDeleted || partDeleted
        } catch (_: Exception) {
            false
        }
    }

    private fun recordPendingApkInstall(
        packageName: String,
        appId: String,
        versionCode: Long,
        apkFilePath: String
    ) {
        if (packageName.isBlank() && appId.isBlank()) return
        try {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PENDING_INSTALL_APKS, "{}") ?: "{}"
            val root = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            val key = packageName.ifBlank { appId }
            val entry = JSONObject().apply {
                put("packageName", packageName)
                put("appId", appId)
                put("versionCode", versionCode)
                put("apkFilePath", apkFilePath)
                put("recordedAt", System.currentTimeMillis())
            }
            root.put(key, entry)
            prefs.edit().putString(KEY_PENDING_INSTALL_APKS, root.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun removePendingInstallByPath(apkFilePath: String) {
        try {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PENDING_INSTALL_APKS, null) ?: return
            val root = runCatching { JSONObject(raw) }.getOrElse { return }
            val keysToRemove = mutableListOf<String>()
            val iter = root.keys()
            while (iter.hasNext()) {
                val key = iter.next()
                val obj = root.optJSONObject(key)
                if (obj == null || obj.optString("apkFilePath") == apkFilePath) {
                    keysToRemove.add(key)
                }
            }
            if (keysToRemove.isNotEmpty()) {
                keysToRemove.forEach { root.remove(it) }
                prefs.edit().putString(KEY_PENDING_INSTALL_APKS, root.toString()).apply()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Automatically deletes downloaded APK packages from local storage as soon as the corresponding
     * package & version (or newer) is installed on the device. Returns package names cleaned up.
     */
    fun cleanupInstalledApkPackages(): List<String> {
        val cleanedPackages = mutableListOf<String>()

        // 1. Inspect every .apk file currently sitting in APK directories
        for (file in listDownloadedApkFiles()) {
            val archiveInfo = inspectApkArchiveInfo(file)
            if (archiveInfo.packageName.isNotBlank()) {
                val installed = getInstalledStatus(archiveInfo.packageName, archiveInfo.versionCode)
                val targetCode = archiveInfo.versionCode
                if (installed.isInstalled && (targetCode <= 0L || installed.installedVersionCode >= targetCode)) {
                    if (deleteDownloadedApkFile(file.absolutePath)) {
                        cleanedPackages.add(archiveInfo.packageName)
                    }
                }
            }
        }

        // 2. Check tracked pending installs (survives process restart after store self-update)
        try {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PENDING_INSTALL_APKS, null)
            if (!raw.isNullOrBlank()) {
                val root = JSONObject(raw)
                val keysToRemove = mutableListOf<String>()
                val iter = root.keys()
                while (iter.hasNext()) {
                    val key = iter.next()
                    val obj = root.optJSONObject(key) ?: continue
                    val pkg = obj.optString("packageName", "")
                    val verCode = obj.optLong("versionCode", 0L)
                    val path = obj.optString("apkFilePath", "")
                    val file = if (path.isNotBlank()) File(path) else null
                    if (pkg.isNotBlank()) {
                        val installed = getInstalledStatus(pkg, verCode)
                        if (installed.isInstalled && (verCode <= 0L || installed.installedVersionCode >= verCode)) {
                            if (path.isNotBlank() && deleteDownloadedApkFile(path)) {
                                cleanedPackages.add(pkg)
                            }
                            keysToRemove.add(key)
                        } else if (file == null || !file.exists()) {
                            keysToRemove.add(key)
                        }
                    } else if (file == null || !file.exists()) {
                        keysToRemove.add(key)
                    }
                }
                if (keysToRemove.isNotEmpty()) {
                    keysToRemove.forEach { root.remove(it) }
                    prefs.edit().putString(KEY_PENDING_INSTALL_APKS, root.toString()).apply()
                }
            }
        } catch (_: Exception) {
        }

        return cleanedPackages.distinct()
    }

    /**
     * Deletes downloaded APK files belonging to a specific installed/updated package immediately
     * when Android broadcasts ACTION_PACKAGE_ADDED or ACTION_PACKAGE_REPLACED.
     */
    fun deleteApksForInstalledPackage(packageName: String, catalogApps: List<StoreApp> = emptyList()): List<String> {
        if (packageName.isBlank()) return emptyList()
        val deletedPaths = mutableListOf<String>()
        val installed = getInstalledStatus(packageName, 0L)
        if (!installed.isInstalled) return emptyList()

        val matchingAppIds = catalogApps
            .filter { it.packageName.equals(packageName, ignoreCase = true) }
            .map { it.appId.ifBlank { it.docId }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_") }
            .filter { it.isNotBlank() }
            .toSet()

        for (file in listDownloadedApkFiles()) {
            val archiveInfo = inspectApkArchiveInfo(file)
            val matchesPkg = archiveInfo.packageName.equals(packageName, ignoreCase = true) &&
                (archiveInfo.versionCode <= 0L || installed.installedVersionCode >= archiveInfo.versionCode)
            val matchesPrefix = matchingAppIds.any { file.name.startsWith("$it-v") }
            if (matchesPkg || matchesPrefix) {
                if (deleteDownloadedApkFile(file.absolutePath)) {
                    deletedPaths.add(file.absolutePath)
                }
            }
        }

        // Also clean any tracked pending install entry for this package
        try {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PENDING_INSTALL_APKS, null)
            if (!raw.isNullOrBlank()) {
                val root = JSONObject(raw)
                val obj = root.optJSONObject(packageName)
                if (obj != null) {
                    val path = obj.optString("apkFilePath", "")
                    if (path.isNotBlank() && deleteDownloadedApkFile(path)) {
                        deletedPaths.add(path)
                    }
                    root.remove(packageName)
                    prefs.edit().putString(KEY_PENDING_INSTALL_APKS, root.toString()).apply()
                }
            }
        } catch (_: Exception) {
        }

        return deletedPaths.distinct()
    }

    fun postStoreAnnouncementNotification(notification: StoreNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val androidNotification = NotificationCompat.Builder(appContext, UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(notification.id.hashCode(), androidNotification)
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
            val installedName = packageInfo.versionName ?: "1.0.0.1"
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

    fun inspectPackage(
        packageName: String,
        remoteVersionCode: Long,
        remoteVersionName: String = ""
    ): InstalledAppStatus = getInstalledStatus(packageName, remoteVersionCode)

    fun scanCatalogStatus(apps: List<StoreApp>): Map<String, InstalledAppStatus> {
        val result = mutableMapOf<String, InstalledAppStatus>()
        for (app in apps) {
            if (app.packageName.isNotBlank()) {
                result[app.packageName] = getInstalledStatus(app.packageName, app.versionCode)
            }
        }
        return result
    }

    fun inspectCatalogPackages(apps: List<StoreApp>): Map<String, InstalledAppStatus> =
        scanCatalogStatus(apps)

    fun postUpdateNotification(app: StoreApp) {
        notifyAvailableUpdates(listOf(app))
    }

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

    fun downloadApkWithProgress(
        app: StoreApp,
        allowResume: Boolean = true,
        forceRedownload: Boolean = false
    ): Flow<DownloadState> = downloadApk(app, allowResume, forceRedownload)

    /**
     * Downloads an APK file from GitHub Releases with HTTP Range resume support,
     * cancellation support, duplicate detection, and SHA-256 checksum verification.
     */
    fun downloadApk(
        app: StoreApp,
        allowResume: Boolean = true,
        forceRedownload: Boolean = false
    ): Flow<DownloadState> = flow {
        val appId = app.appId.ifBlank { app.docId }
        cancelledDownloads[appId] = false

        val url = app.apkDownloadUrl.trim()
        if (url.isBlank() || (!url.startsWith("https://") && !url.startsWith("http://"))) {
            emit(DownloadState.Failed(appId, "Invalid APK download URL: must start with https://"))
            return@flow
        }

        val apkDir = File(appContext.cacheDir, "apks").apply {
            if (!exists()) mkdirs()
        }
        val safeAppPrefix = appId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val safeFileName = "$safeAppPrefix-v${app.versionCode}.apk"
        val targetFile = File(apkDir, safeFileName)
        val partFile = File(apkDir, "$safeFileName.part")

        // Delete any older cached APK files for this same app before downloading a new build
        apkDir.listFiles { f ->
            f.isFile && f.name.startsWith("$safeAppPrefix-v") && f.name != safeFileName && f.name != "$safeFileName.part"
        }?.forEach { staleFile ->
            staleFile.delete()
        }

        val shouldBypassCache = forceRedownload || url.contains("/releases/latest/", ignoreCase = true)
        if (shouldBypassCache) {
            if (targetFile.exists()) targetFile.delete()
            if (partFile.exists()) partFile.delete()
        }

        // Duplicate download detection: if verified APK already exists on disk, skip re-downloading
        if (!shouldBypassCache && targetFile.exists() && targetFile.length() > 1024L) {
            val archiveVer = inspectApkArchiveInfo(targetFile)
            val expectedSha = app.sha256Checksum.trim().lowercase()
            val verified = expectedSha.isEmpty() || expectedSha == archiveVer.sha256Checksum.lowercase()
            emit(
                DownloadState.ReadyToInstall(
                    appId = appId,
                    apkFilePath = targetFile.absolutePath,
                    versionName = archiveVer.versionName.ifBlank { app.versionName },
                    versionCode = archiveVer.versionCode.takeIf { it > 0L } ?: app.versionCode,
                    computedSha256 = archiveVer.sha256Checksum,
                    checksumVerified = verified,
                    signingCertSha256 = archiveVer.signingCertSha256,
                    signatureMatchesInstalled = archiveVer.signatureMatchesInstalled
                )
            )
            return@flow
        }

        val existingBytes = if (allowResume && partFile.exists()) partFile.length() else 0L
        emit(DownloadState.Downloading(appId, 0, existingBytes, 0L))

        try {
            var resolvedDownloadUrl = url
            // If URL is a GitHub repository root or /releases/latest page without a direct .apk filename, resolve via GitHub Releases API first
            if (url.contains("github.com/", ignoreCase = true) && !url.endsWith(".apk", ignoreCase = true)) {
                val resolvedAsset = resolveLatestGitHubReleaseAsset(url)
                    ?: resolveLatestGitHubReleaseAsset(app.githubRepoUrl)
                if (resolvedAsset != null) {
                    resolvedDownloadUrl = resolvedAsset.browserDownloadUrl
                }
            }

            fun buildDownloadRequest(targetUrl: String, rangeStart: Long): Request {
                val builder = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "ZayaanAppsStore-Android/1.0.0.2")
                    .header("Cache-Control", "no-cache")
                if (rangeStart > 0L) {
                    builder.header("Range", "bytes=$rangeStart-")
                }
                return builder.build()
            }

            var initialCall = httpClient.newCall(buildDownloadRequest(resolvedDownloadUrl, existingBytes)).execute()

            // Automatic GitHub Releases Asset Discovery Fallback if the hardcoded filename returned 404
            if (!initialCall.isSuccessful && initialCall.code != 206 &&
                (resolvedDownloadUrl.contains("github.com/", ignoreCase = true) || app.githubRepoUrl.contains("github.com/", ignoreCase = true))
            ) {
                initialCall.close()
                val discovered = resolveLatestGitHubReleaseAsset(resolvedDownloadUrl)
                    ?: resolveLatestGitHubReleaseAsset(app.githubRepoUrl)
                    ?: resolveLatestGitHubReleaseAsset("https://github.com/zayaanapps/zayaan-apps")
                if (discovered != null && discovered.browserDownloadUrl != resolvedDownloadUrl) {
                    resolvedDownloadUrl = discovered.browserDownloadUrl
                    initialCall = httpClient.newCall(buildDownloadRequest(resolvedDownloadUrl, existingBytes)).execute()
                }
            }

            initialCall.use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    emit(
                        DownloadState.Failed(
                            appId,
                            "GitHub Release returned HTTP ${response.code}. Tap 'Fetch Latest Release APK' or 'Download via Browser' to open GitHub Releases."
                        )
                    )
                    return@flow
                }

                val body = response.body
                if (body == null) {
                    emit(DownloadState.Failed(appId, "Empty response body from release server."))
                    return@flow
                }

                val isPartialContent = response.code == 206
                val contentLength = body.contentLength()
                val totalBytes = if (isPartialContent && contentLength > 0) {
                    existingBytes + contentLength
                } else {
                    contentLength
                }
                var downloadedBytes = if (isPartialContent) existingBytes else 0L
                val buffer = ByteArray(8 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(partFile, isPartialContent).use { output ->
                        var bytesRead: Int
                        var lastEmittedPercent = -1
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            if (cancelledDownloads[appId] == true) {
                                output.flush()
                                emit(
                                    DownloadState.Paused(
                                        appId = appId,
                                        progressPercent = lastEmittedPercent.coerceAtLeast(0),
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                )
                                return@flow
                            }
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

            if (targetFile.exists()) targetFile.delete()
            partFile.renameTo(targetFile)

            val archiveVer = inspectApkArchiveInfo(targetFile)
            val expectedSha = app.sha256Checksum.trim().lowercase()
            val verified = expectedSha.isEmpty() || expectedSha == archiveVer.sha256Checksum.lowercase()

            emit(
                DownloadState.ReadyToInstall(
                    appId = appId,
                    apkFilePath = targetFile.absolutePath,
                    versionName = archiveVer.versionName.ifBlank { app.versionName },
                    versionCode = archiveVer.versionCode.takeIf { it > 0L } ?: app.versionCode,
                    computedSha256 = archiveVer.sha256Checksum,
                    checksumVerified = verified,
                    signingCertSha256 = archiveVer.signingCertSha256,
                    signatureMatchesInstalled = archiveVer.signatureMatchesInstalled
                )
            )
        } catch (e: Exception) {
            emit(
                DownloadState.Failed(
                    appId,
                    e.localizedMessage ?: "Network interruption while downloading APK from GitHub Releases."
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Builds the system APK installation Intent and records the APK as pending install so it is
     * automatically deleted immediately after installation completes.
     */
    fun buildApkInstallIntent(
        activityContext: Context,
        apkFilePath: String,
        appId: String = "",
        forActivityResult: Boolean = false
    ): Result<Intent> {
        return try {
            val file = File(apkFilePath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("Downloaded APK file not found at $apkFilePath"))
            }
            val archiveInfo = inspectApkArchiveInfo(file)
            recordPendingApkInstall(
                packageName = archiveInfo.packageName,
                appId = appId,
                versionCode = archiveInfo.versionCode,
                apkFilePath = file.absolutePath
            )
            val apkUri: Uri = FileProvider.getUriForFile(
                activityContext,
                "${activityContext.packageName}.fileprovider",
                file
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (!forActivityResult) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            Result.success(installIntent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launches the standard Android system package installer confirmation dialog for a downloaded APK file.
     * Respects Android security restrictions and user confirmation requirements.
     */
    fun launchApkInstaller(activityContext: Context, apkFilePath: String, appId: String = ""): Result<Unit> {
        return buildApkInstallIntent(activityContext, apkFilePath, appId, forActivityResult = false).mapCatching { installIntent ->
            activityContext.startActivity(installIntent)
        }
    }

    fun openInstalledApp(activityContext: Context, packageName: String): Result<Unit> {
        val targetPkg = packageName.trim()
        if (targetPkg.isBlank()) {
            return Result.failure(IllegalArgumentException("Package name cannot be blank"))
        }
        return try {
            val pm = activityContext.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
                ?: pm.getLeanbackLaunchIntentForPackage(targetPkg)
                ?: run {
                    val mainQuery = Intent(Intent.ACTION_MAIN).apply {
                        setPackage(targetPkg)
                    }
                    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.queryIntentActivities(mainQuery, PackageManager.ResolveInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.queryIntentActivities(mainQuery, 0)
                    }
                    val targetActivity = resolved.firstOrNull { it.activityInfo?.exported != false }?.activityInfo
                        ?: resolved.firstOrNull()?.activityInfo
                    if (targetActivity != null) {
                        Intent(Intent.ACTION_MAIN).apply {
                            setClassName(targetActivity.packageName, targetActivity.name)
                            addCategory(Intent.CATEGORY_LAUNCHER)
                        }
                    } else {
                        null
                    }
                }
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                activityContext.startActivity(launchIntent)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("No launch activity found for $targetPkg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uninstallPackage(activityContext: Context, packageName: String): Result<Unit> {
        return try {
            val uninstallIntent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(uninstallIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    fun shareApplicationLink(activityContext: Context, app: StoreApp): Result<Unit> {
        return try {
            val shareText = buildString {
                append("Check out ${app.appName} (v${app.versionName}) by ${app.developerName} on Zayaan Apps!\n")
                append("Web Store: https://zayaanapps.github.io/zayaan-apps/\n")
                append("Direct APK: ${app.apkDownloadUrl}")
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "${app.appName} on Zayaan Apps")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share ${app.appName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractSha256FromReleaseBody(rawBody: String): String {
        val match = Regex("""SHA-?256[^0-9a-fA-F]*([0-9a-fA-F]{64})""", RegexOption.IGNORE_CASE).find(rawBody)
        return match?.groupValues?.getOrNull(1)?.lowercase() ?: ""
    }

    /**
     * Filters out technical build metadata blocks (`Package Name`, `Version Code`, `SHA-256`,
     * `<details>...</details>`, etc.) so "What's New" always highlights actual updates, fixes, and changes.
     */
    private fun cleanGitHubReleaseNotes(rawBody: String, tagName: String): String {
        val withoutDetails = rawBody.replace(
            Regex("""<details>[\s\S]*?</details>""", RegexOption.IGNORE_CASE),
            ""
        )
        val metadataLinePrefixes = listOf(
            "### zayaan apps official store release",
            "package name:",
            "architecture:",
            "version name:",
            "version code:",
            "commit:",
            "apk sha-256 checksum:",
            "responsive web portal:",
            "direct apk assets:"
        )
        val keptLines = mutableListOf<String>()
        var compareLinkLine = ""

        for (rawLine in withoutDetails.lines()) {
            val trimmed = rawLine.trim()
            val normalizedForCheck = trimmed
                .removePrefix("- ")
                .removePrefix("* ")
                .replace("**", "")
                .replace("`", "")
                .trim()
                .lowercase()
            if (normalizedForCheck.startsWith("full changelog:") || trimmed.contains("/compare/", ignoreCase = true)) {
                compareLinkLine = trimmed
                continue
            }
            val isMetadataBoilerplate = metadataLinePrefixes.any { prefix ->
                normalizedForCheck.startsWith(prefix)
            }
            if (!isMetadataBoilerplate) {
                keptLines.add(rawLine)
            }
        }

        val cleanedContent = keptLines.joinToString("\n").trim()
        return buildString {
            if (cleanedContent.isNotBlank()) {
                append(cleanedContent)
            } else {
                append("### Latest Updates, Fixes & Changes (v$tagName)\n")
                append("- Latest GitHub Release update synced to v$tagName.\n")
                append("- Performance, stability, and UI improvements across Zayaan Apps.")
            }
            if (compareLinkLine.isNotBlank()) {
                append("\n\n")
                append(compareLinkLine)
            }
        }.trim()
    }

    /**
     * Resolves the latest `.apk` asset download URL, version tag, file size, SHA-256, and cleaned release notes
     * from any GitHub repository URL or GitHub Releases URL (`https://github.com/<owner>/<repo>...`).
     */
    suspend fun resolveLatestGitHubReleaseAsset(repoOrReleaseUrl: String): ResolvedGitHubReleaseAsset? = withContext(Dispatchers.IO) {
        try {
            val regex = Regex("""github\.com/([^/]+)/([^/]+)""", RegexOption.IGNORE_CASE)
            val match = regex.find(repoOrReleaseUrl) ?: return@withContext null
            val owner = match.groupValues[1].trim()
            val repo = match.groupValues[2].removeSuffix(".git").trim()
            if (owner.isBlank() || repo.isBlank()) return@withContext null

            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "ZayaanAppsStore-Android/1.0.0.2")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val jsonStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(jsonStr)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/$owner/$repo/releases/latest")
                val rawBody = json.optString("body", "")
                val releaseNotes = cleanGitHubReleaseNotes(rawBody, tagName.ifBlank { "1.0.0.1" })
                val parsedSha256 = extractSha256FromReleaseBody(rawBody)
                val assets = json.optJSONArray("assets")

                var apkUrl = ""
                var apkName = ""
                var sizeBytes = 0L

                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        val browserUrl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) || browserUrl.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = browserUrl
                            apkName = name
                            sizeBytes = asset.optLong("size", 0L)
                            if (name.contains("release", ignoreCase = true)) break
                        }
                    }
                }

                if (apkUrl.isBlank()) return@withContext null

                val lastSegment = tagName.substringAfterLast('.', "").filter { it.isDigit() }.toLongOrNull()
                    ?: tagName.filter { it.isDigit() }.toLongOrNull()
                    ?: 1L
                val formattedSize = if (sizeBytes > 0L) {
                    "%.1f MB".format(sizeBytes.toDouble() / (1024.0 * 1024.0))
                } else {
                    "16.8 MB"
                }

                ResolvedGitHubReleaseAsset(
                    owner = owner,
                    repo = repo,
                    tagName = tagName.ifBlank { "1.0.0.1" },
                    versionCode = lastSegment.coerceAtLeast(1L),
                    assetName = apkName,
                    browserDownloadUrl = apkUrl,
                    fileSizeFormatted = formattedSize,
                    releaseNotes = releaseNotes,
                    releaseHtmlUrl = htmlUrl,
                    sha256Checksum = parsedSha256
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Queries https://api.github.com/repos/zayaanapps/zayaan-apps/releases/latest
     * to check if a newer 1.0.0.X release tag of Zayaan Apps Store itself was built on GitHub Actions.
     */
    suspend fun fetchLatestGitHubStoreRelease(): StoreApp? = withContext(Dispatchers.IO) {
        val resolved = resolveLatestGitHubReleaseAsset("https://github.com/zayaanapps/zayaan-apps")
            ?: return@withContext null
        StoreApp(
            appId = "zayaan-apps-store",
            appName = "Zayaan Apps Store",
            packageName = "com.zayaanapps.store",
            category = "Tools",
            developerName = "Zayaan Apps Official",
            description = "The official Zayaan Apps Android App Store & Web Distribution client. Discover, download, verify, and update official Zayaan Android applications directly from GitHub Releases with real-time version tracking.",
            shortDescription = "Official Zayaan Apps Android marketplace & GitHub Releases distribution client.",
            versionName = resolved.tagName,
            versionCode = resolved.versionCode,
            apkDownloadUrl = resolved.browserDownloadUrl,
            apkFileSize = resolved.fileSizeFormatted,
            releaseNotes = resolved.releaseNotes,
            sha256Checksum = resolved.sha256Checksum,
            githubRepoUrl = "https://github.com/${resolved.owner}/${resolved.repo}",
            featured = true,
            editorsChoice = true,
            essential = true,
            published = true
        )
    }

    fun saveLocalReviewsBackup(reviews: List<AppReview>) {
        try {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray()
            reviews.forEach { review ->
                val obj = JSONObject().apply {
                    put("id", review.id)
                    put("appId", review.appId)
                    put("appName", review.appName)
                    put("userId", review.userId)
                    put("userName", review.userName)
                    put("rating", review.rating)
                    put("comment", review.comment)
                    put("verifiedInstall", review.verifiedInstall)
                    put("helpfulCount", review.helpfulCount)
                    put("developerReply", review.developerReply)
                    put("flagged", review.flagged)
                    put("createdAtMillis", review.createdAt?.toDate()?.time ?: System.currentTimeMillis())
                    put("updatedAtMillis", review.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_PENDING_REVIEWS_JSON, arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun loadLocalReviewsBackup(): List<AppReview> {
        return try {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PENDING_REVIEWS_JSON, null) ?: return emptyList()
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    val appId = obj.optString("appId", "")
                    if (id.isBlank() || appId.isBlank()) continue
                    val createdMillis = obj.optLong("createdAtMillis", 0L)
                    val updatedMillis = obj.optLong("updatedAtMillis", 0L)
                    add(
                        AppReview(
                            id = id,
                            appId = appId,
                            appName = obj.optString("appName", appId),
                            userId = obj.optString("userId", "guest-preview"),
                            userName = obj.optString("userName", "Zayaan User"),
                            rating = obj.optLong("rating", 5L).coerceIn(1L, 5L),
                            comment = obj.optString("comment", ""),
                            verifiedInstall = obj.optBoolean("verifiedInstall", false),
                            helpfulCount = obj.optLong("helpfulCount", 0L).coerceAtLeast(0L),
                            developerReply = obj.optString("developerReply", ""),
                            flagged = obj.optBoolean("flagged", false),
                            createdAt = if (createdMillis > 0L) Timestamp(Date(createdMillis)) else null,
                            updatedAt = if (updatedMillis > 0L) Timestamp(Date(updatedMillis)) else null
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearLocalReviewsBackup() {
        try {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PENDING_REVIEWS_JSON)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun getSavedThemeMode(): AppThemeMode {
        return try {
            val raw = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_APP_THEME_MODE, AppThemeMode.SYSTEM.name)
            AppThemeMode.entries.firstOrNull { it.name == raw } ?: AppThemeMode.SYSTEM
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun saveThemeMode(mode: AppThemeMode) {
        try {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_APP_THEME_MODE, mode.name)
                .apply()
        } catch (_: Exception) {
        }
    }
}
