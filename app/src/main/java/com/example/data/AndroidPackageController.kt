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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
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
    fun openManageUnknownAppSourcesSettings(activityContext: Context): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activityContext.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
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

    /**
     * Lists all locally downloaded APK files stored in the cache directory.
     */
    fun listDownloadedApkFiles(): List<File> {
        val apkDir = File(appContext.cacheDir, "apks")
        if (!apkDir.exists()) return emptyList()
        return apkDir.listFiles { f -> f.isFile && f.name.endsWith(".apk") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteDownloadedApkFile(apkFilePath: String): Boolean {
        return try {
            val file = File(apkFilePath)
            val partFile = File("$apkFilePath.part")
            if (partFile.exists()) partFile.delete()
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
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
        val safeFileName = "${appId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}-v${app.versionCode}.apk"
        val targetFile = File(apkDir, safeFileName)
        val partFile = File(apkDir, "$safeFileName.part")

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

    /**
     * Resolves the latest `.apk` asset download URL, version tag, file size, and release notes
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
                val releaseNotes = json.optString("body", "Official GitHub Release v$tagName")
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
                    releaseHtmlUrl = htmlUrl
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
            description = "Official Zayaan Apps Store self-update package from GitHub Releases.",
            shortDescription = "Official Zayaan Apps Store self-updater (${resolved.assetName}).",
            versionName = resolved.tagName,
            versionCode = resolved.versionCode,
            apkDownloadUrl = resolved.browserDownloadUrl,
            apkFileSize = resolved.fileSizeFormatted,
            releaseNotes = resolved.releaseNotes,
            githubRepoUrl = "https://github.com/${resolved.owner}/${resolved.repo}",
            featured = true,
            editorsChoice = true,
            essential = true,
            published = true
        )
    }
}
