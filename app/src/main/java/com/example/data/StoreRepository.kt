package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.trim().lowercase()
            .replace(Regex("[^a-z0-9_\\-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
        return if (cleaned.isNotEmpty()) cleaned.take(100) else "app-${System.currentTimeMillis()}"
    }

    /**
     * Real-time stream of published applications in the Zayaan Apps store.
     */
    fun observePublishedApps(): Flow<List<StoreApp>> = flow {
        val uid = requireUserId()
        val path = "apps"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StoreApp::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Real-time stream of apps authored by the current admin/developer (includes unpublished drafts).
     */
    fun observeMyAuthoredApps(userId: String): Flow<List<StoreApp>> = flow {
        val uid = requireUserId()
        val path = "apps"
        emitAll(
            db.collection(path)
                .whereEqualTo("authorUid", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StoreApp::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Real-time stream of store categories.
     */
    fun observeCategories(): Flow<List<StoreCategory>> = flow {
        val uid = requireUserId()
        val path = "categories"
        emitAll(
            db.collection(path)
                .whereGreaterThanOrEqualTo("displayOrder", 0)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(StoreCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayOrder }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Real-time stream of user preferences and bookmarked apps.
     */
    fun observeUserPreferences(userId: String): Flow<UserPreference?> = flow {
        val uid = requireUserId()
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserPreference::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Creates a new application in the Zayaan Apps catalog.
     */
    suspend fun createApp(app: StoreApp): Result<String> {
        var docPath = "apps"
        return try {
            val uid = requireUserId()
            val safeId = sanitizeId(app.appId.ifBlank { app.appName })
            val validCategory = if (app.category in STORE_CATEGORIES) app.category else "Other"
            val docRef = db.collection("apps").document(safeId)
            docPath = docRef.path

            val payload = mapOf(
                "appId" to safeId,
                "appName" to app.appName.trim().take(80).ifEmpty { "Untitled App" },
                "packageName" to app.packageName.trim().take(150).ifEmpty { "com.zayaanapps.$safeId" },
                "category" to validCategory,
                "developerName" to app.developerName.trim().take(80).ifEmpty { "Zayaan Apps Studio" },
                "description" to app.description.trim().take(4000).ifEmpty { "Official application distributed via Zayaan Apps." },
                "iconUrl" to app.iconUrl.trim().take(1000),
                "screenshots" to app.screenshots.map { it.trim().take(1000) }.filter { it.isNotEmpty() }.take(10),
                "versionName" to app.versionName.trim().take(40).ifEmpty { "1.0.0" },
                "versionCode" to app.versionCode.coerceIn(1L, 2000000000L),
                "apkDownloadUrl" to app.apkDownloadUrl.trim().take(1000).ifEmpty {
                    "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/$safeId.apk"
                },
                "apkFileSize" to app.apkFileSize.trim().take(40).ifEmpty { "14.5 MB" },
                "releaseNotes" to app.releaseNotes.trim().take(2000),
                "featured" to app.featured,
                "published" to app.published,
                "downloadCount" to app.downloadCount.coerceAtLeast(0L),
                "authorUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(safeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing application in the Zayaan Apps catalog (preserves createdAt, appId, authorUid).
     */
    suspend fun updateApp(app: StoreApp): Result<Unit> {
        val targetId = app.appId.ifBlank { app.docId }
        var docPath = "apps/$targetId"
        return try {
            requireUserId()
            require(targetId.isNotBlank()) { "App ID must not be blank for update" }
            val validCategory = if (app.category in STORE_CATEGORIES) app.category else "Other"
            val docRef = db.collection("apps").document(targetId)
            docPath = docRef.path

            val updatePayload = mapOf(
                "appName" to app.appName.trim().take(80).ifEmpty { "Untitled App" },
                "packageName" to app.packageName.trim().take(150).ifEmpty { "com.zayaanapps.$targetId" },
                "category" to validCategory,
                "developerName" to app.developerName.trim().take(80).ifEmpty { "Zayaan Apps Studio" },
                "description" to app.description.trim().take(4000).ifEmpty { "Official application distributed via Zayaan Apps." },
                "iconUrl" to app.iconUrl.trim().take(1000),
                "screenshots" to app.screenshots.map { it.trim().take(1000) }.filter { it.isNotEmpty() }.take(10),
                "versionName" to app.versionName.trim().take(40).ifEmpty { "1.0.0" },
                "versionCode" to app.versionCode.coerceIn(1L, 2000000000L),
                "apkDownloadUrl" to app.apkDownloadUrl.trim().take(1000).ifEmpty {
                    "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/$targetId.apk"
                },
                "apkFileSize" to app.apkFileSize.trim().take(40).ifEmpty { "14.5 MB" },
                "releaseNotes" to app.releaseNotes.trim().take(2000),
                "featured" to app.featured,
                "published" to app.published,
                "downloadCount" to app.downloadCount.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Toggles published/unpublished visibility for an application.
     */
    suspend fun setAppPublished(appId: String, published: Boolean): Result<Unit> {
        val docPath = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank()) { "App ID must not be blank" }
            db.collection("apps").document(appId).update(
                mapOf(
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Toggles featured badge for an application.
     */
    suspend fun setAppFeatured(appId: String, featured: Boolean): Result<Unit> {
        val docPath = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank()) { "App ID must not be blank" }
            db.collection("apps").document(appId).update(
                mapOf(
                    "featured" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Increments the download counter (+1) when a user downloads an APK.
     */
    suspend fun incrementDownloadCount(appId: String): Result<Unit> {
        val docPath = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank()) { "App ID must not be blank" }
            db.collection("apps").document(appId).update(
                mapOf(
                    "downloadCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Deletes an application from the store catalog.
     */
    suspend fun deleteApp(appId: String): Result<Unit> {
        val docPath = "apps/$appId"
        return try {
            requireUserId()
            require(appId.isNotBlank()) { "App ID must not be blank" }
            db.collection("apps").document(appId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Creates or updates a store category.
     */
    suspend fun addCategory(name: String, description: String, displayOrder: Long): Result<Unit> {
        val catId = sanitizeId(name)
        val docPath = "categories/$catId"
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "name" to name.trim().take(50).ifEmpty { "General" },
                "description" to description.trim().take(300).ifEmpty { "Curated applications in $name." },
                "displayOrder" to displayOrder.coerceIn(0L, 1000L),
                "authorUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("categories").document(catId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val docPath = "categories/$categoryId"
        return try {
            requireUserId()
            require(categoryId.isNotBlank()) { "Category ID must not be blank" }
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Saves user preferences (theme, update notifications, bookmarked app IDs).
     */
    suspend fun saveUserPreferences(
        darkMode: Boolean,
        notifyUpdates: Boolean,
        bookmarkedAppIds: List<String>,
        isExistingDoc: Boolean
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val safeBookmarks = bookmarkedAppIds.map { it.trim().take(100) }.filter { it.isNotEmpty() }.distinct().take(50)

            if (isExistingDoc) {
                docRef.update(
                    mapOf(
                        "darkMode" to darkMode,
                        "notifyUpdates" to notifyUpdates,
                        "bookmarkedAppIds" to safeBookmarks,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "darkMode" to darkMode,
                        "notifyUpdates" to notifyUpdates,
                        "bookmarkedAppIds" to safeBookmarks,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Seeds the initial official Zayaan Apps ecosystem catalog so the store is immediately rich and functional.
     */
    suspend fun seedOfficialZayaanCatalog(): Result<Int> {
        return try {
            requireUserId()
            val seedApps = listOf(
                StoreApp(
                    appId = "zayaan-apps-store",
                    appName = "Zayaan Apps Store",
                    packageName = "com.zayaanapps.store",
                    category = "Tools",
                    developerName = "Zayaan Apps Official",
                    description = "The official Zayaan Apps digital application marketplace and APK updater for Android & Web. Discover, download, and keep all Zayaan ecosystem apps up to date directly from GitHub Releases and Cloud Firestore.",
                    iconUrl = "https://zayaanapps.github.io/zayaan-apps/icons/store.png",
                    screenshots = listOf(
                        "https://zayaanapps.github.io/zayaan-apps/screenshots/store-home.png",
                        "https://zayaanapps.github.io/zayaan-apps/screenshots/store-admin.png"
                    ),
                    versionName = "2.0.0",
                    versionCode = 2L, // Set to 2 so current v1 (versionCode 1) shows an available update in My Apps!
                    apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps-release.apk",
                    apkFileSize = "16.8 MB",
                    releaseNotes = "v2.0.0: Added centralized GitHub Releases webhook sync, Flutter Web GitHub Pages responsive portal, and one-tap APK updater.",
                    featured = true,
                    published = true,
                    downloadCount = 1280L
                ),
                StoreApp(
                    appId = "zayaan-calculator",
                    appName = "Zayaan Calculator Pro",
                    packageName = "com.zayaanapps.calculator",
                    category = "Utilities",
                    developerName = "Zayaan Apps Official",
                    description = "Precision scientific, graphing, and multi-currency financial calculator crafted with Material 3 expressive motion. Includes real-time unit conversions, equation history tape, and offline split-bill tools.",
                    iconUrl = "https://zayaanapps.github.io/zayaan-apps/icons/calculator.png",
                    screenshots = listOf(
                        "https://zayaanapps.github.io/zayaan-apps/screenshots/calc-1.png",
                        "https://zayaanapps.github.io/zayaan-apps/screenshots/calc-2.png"
                    ),
                    versionName = "1.4.2",
                    versionCode = 14L,
                    apkDownloadUrl = "https://github.com/zayaanapps/zayaan-calculator/releases/latest/download/zayaan-calculator.apk",
                    apkFileSize = "11.2 MB",
                    releaseNotes = "Added engineering constants library, live solver preview, and tablet split-screen keypad.",
                    featured = true,
                    published = true,
                    downloadCount = 845L
                ),
                StoreApp(
                    appId = "zayaan-notes",
                    appName = "Zayaan Notes & Markdown",
                    packageName = "com.zayaanapps.notes",
                    category = "Productivity",
                    developerName = "Zayaan Apps Official",
                    description = "Fast, encrypted markdown notebook and voice memo organizer with real-time cloud synchronization, LaTeX math blocks, checklist widgets, and instant PDF export.",
                    iconUrl = "https://zayaanapps.github.io/zayaan-apps/icons/notes.png",
                    screenshots = listOf(
                        "https://zayaanapps.github.io/zayaan-apps/screenshots/notes-1.png"
                    ),
                    versionName = "2.1.0",
                    versionCode = 21L,
                    apkDownloadUrl = "https://github.com/zayaanapps/zayaan-notes/releases/latest/download/zayaan-notes.apk",
                    apkFileSize = "14.6 MB",
                    releaseNotes = "Introduced bi-directional note linking, code syntax highlighting, and dark AMOLED theme.",
                    featured = true,
                    published = true,
                    downloadCount = 690L
                ),
                StoreApp(
                    appId = "zayaan-ai-tools",
                    appName = "Zayaan AI Studio Companion",
                    packageName = "com.zayaanapps.aitools",
                    category = "AI Apps",
                    developerName = "Zayaan AI Labs",
                    description = "All-in-one prompt engineering toolkit, document summarizer, code formatter, and voice-to-structured-JSON assistant built for creators and mobile developers.",
                    iconUrl = "https://zayaanapps.github.io/zayaan-apps/icons/aitools.png",
                    screenshots = listOf(
                        "https://zayaanapps.github.io/zayaan-apps/screenshots/ai-1.png"
                    ),
                    versionName = "1.8.0",
                    versionCode = 18L,
                    apkDownloadUrl = "https://github.com/zayaanapps/zayaan-ai-tools/releases/latest/download/zayaan-ai-tools.apk",
                    apkFileSize = "19.4 MB",
                    releaseNotes = "Added prompt template library, JSON schema validator, and offline token counter.",
                    featured = true,
                    published = true,
                    downloadCount = 1540L
                ),
                StoreApp(
                    appId = "zayaan-utilities",
                    appName = "Zayaan System Utilities",
                    packageName = "com.zayaanapps.utilities",
                    category = "Tools",
                    developerName = "Zayaan Apps Official",
                    description = "Essential Android device diagnostics, network ping & DNS inspector, QR/barcode studio, battery health monitor, and APK checksum (SHA-256) verifier.",
                    iconUrl = "https://zayaanapps.github.io/zayaan-apps/icons/utilities.png",
                    screenshots = emptyList(),
                    versionName = "1.3.1",
                    versionCode = 9L,
                    apkDownloadUrl = "https://github.com/zayaanapps/zayaan-utilities/releases/latest/download/zayaan-utilities.apk",
                    apkFileSize = "9.8 MB",
                    releaseNotes = "Added SHA-256 APK signature inspector and Wi-Fi channel analyzer.",
                    featured = false,
                    published = true,
                    downloadCount = 520L
                ),
                StoreApp(
                    appId = "zayaan-academy",
                    appName = "Zayaan Dart & Flutter Academy",
                    packageName = "com.zayaanapps.academy",
                    category = "Education",
                    developerName = "Zayaan Education",
                    description = "Interactive beginner-to-senior learning tracks for Flutter, Dart, Firebase Firestore Security Rules, and GitHub Actions DevOps right from your Android smartphone.",
                    iconUrl = "https://zayaanapps.github.io/zayaan-apps/icons/academy.png",
                    screenshots = emptyList(),
                    versionName = "1.1.0",
                    versionCode = 5L,
                    apkDownloadUrl = "https://github.com/zayaanapps/zayaan-academy/releases/latest/download/zayaan-academy.apk",
                    apkFileSize = "15.1 MB",
                    releaseNotes = "New interactive quizzes for Flutter Web GitHub Pages deployment.",
                    featured = false,
                    published = true,
                    downloadCount = 410L
                ),
                StoreApp(
                    appId = "zayaan-pixel-quest",
                    appName = "Zayaan Cyber Runner",
                    packageName = "com.zayaanapps.pixelquest",
                    category = "Games",
                    developerName = "Zayaan Interactive",
                    description = "High-framerate 60fps neon synthwave arcade puzzle runner built with Flutter Flame engine. Compete on global cloud leaderboards with zero ads.",
                    iconUrl = "https://zayaanapps.github.io/zayaan-apps/icons/game.png",
                    screenshots = emptyList(),
                    versionName = "1.0.5",
                    versionCode = 6L,
                    apkDownloadUrl = "https://github.com/zayaanapps/zayaan-cyber-runner/releases/latest/download/zayaan-cyber-runner.apk",
                    apkFileSize = "28.4 MB",
                    releaseNotes = "Added 12 new neon sector levels and gamepad controller support.",
                    featured = true,
                    published = true,
                    downloadCount = 980L
                )
            )

            var count = 0
            for (app in seedApps) {
                val res = createApp(app)
                if (res.isSuccess) count++
            }

            STORE_CATEGORIES.forEachIndexed { index, catName ->
                addCategory(
                    name = catName,
                    description = "Explore official and community $catName applications on Zayaan Apps.",
                    displayOrder = index.toLong()
                )
            }

            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
