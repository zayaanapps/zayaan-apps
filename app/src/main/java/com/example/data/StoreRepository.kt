package com.example.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StoreRepository(
    private val firestoreDb: FirebaseFirestore?
) {
    private val db: FirebaseFirestore
        get() = checkNotNull(firestoreDb) { "Cloud Firestore requires Firebase initialization." }
    /**
     * Real-time stream of published applications for the public Zayaan Apps storefront.
     */
    fun observePublishedApps(): Flow<Result<List<StoreApp>>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("apps")
            .whereEqualTo("published", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents
                    ?.mapNotNull { it.toStoreAppOrNull() }
                    ?.sortedWith(
                        compareByDescending<StoreApp> { it.featured }
                            .thenByDescending { it.downloadCount }
                            .thenBy { it.appName.lowercase() }
                    )
                    ?: emptyList()
                trySend(Result.success(apps))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of ALL applications (published + unpublished drafts) for the Admin Dashboard.
     */
    fun observeAllAppsForAdmin(): Flow<Result<List<StoreApp>>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("apps")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents
                    ?.mapNotNull { it.toStoreAppOrNull() }
                    ?.sortedWith(
                        compareByDescending<StoreApp> { it.featured }
                            .thenBy { it.appName.lowercase() }
                    )
                    ?: emptyList()
                trySend(Result.success(apps))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of store categories.
     */
    fun observeCategories(): Flow<Result<List<StoreCategory>>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        StoreCategory(
                            id = doc.id,
                            name = name,
                            description = doc.getString("description") ?: "",
                            displayOrder = doc.getLong("displayOrder") ?: 0L
                        )
                    }
                    ?.sortedBy { it.displayOrder }
                    ?: emptyList()
                trySend(Result.success(categories))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of the current user's preferences.
     */
    fun observeUserPreferences(userId: String): Flow<Result<UserPreference>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(Result.success(UserPreference(userId = userId)))
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val bookmarked = (snapshot.get("bookmarkedAppIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val favorites = (snapshot.get("favoriteAppIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val recentlyViewed = (snapshot.get("recentlyViewedAppIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val downloaded = (snapshot.get("downloadedAppIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val history = (snapshot.get("searchHistory") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    val pref = UserPreference(
                        userId = snapshot.getString("userId") ?: userId,
                        darkMode = snapshot.getBoolean("darkMode") ?: true,
                        notifyUpdates = snapshot.getBoolean("notifyUpdates") ?: true,
                        bookmarkedAppIds = bookmarked,
                        favoriteAppIds = favorites,
                        recentlyViewedAppIds = recentlyViewed,
                        downloadedAppIds = downloaded,
                        searchHistory = history
                    )
                    trySend(Result.success(pref))
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveUserPreferences(
        userId: String,
        darkMode: Boolean,
        notifyUpdates: Boolean,
        bookmarkedAppIds: List<String>,
        favoriteAppIds: List<String> = emptyList(),
        recentlyViewedAppIds: List<String> = emptyList(),
        downloadedAppIds: List<String> = emptyList(),
        searchHistory: List<String> = emptyList()
    ): Result<Unit> = runCatching {
        val docRef = db.collection("users").document(userId)
        val snap = docRef.get().await()
        val payload = hashMapOf<String, Any>(
            "userId" to userId,
            "darkMode" to darkMode,
            "notifyUpdates" to notifyUpdates,
            "bookmarkedAppIds" to bookmarkedAppIds.distinct().take(50),
            "favoriteAppIds" to favoriteAppIds.distinct().take(50),
            "recentlyViewedAppIds" to recentlyViewedAppIds.distinct().take(30),
            "downloadedAppIds" to downloadedAppIds.distinct().take(50),
            "searchHistory" to searchHistory.distinct().take(20),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snap.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.set(payload, SetOptions.merge()).await()
        }
    }

    /**
     * Real-time stream of community ratings & written reviews for a specific application.
     */
    fun observeReviewsForApp(appId: String): Flow<Result<List<AppReview>>> = callbackFlow {
        if (appId.isBlank()) {
            trySend(Result.success(emptyList()))
            awaitClose {}
            return@callbackFlow
        }
        val registration: ListenerRegistration = db.collection("reviews")
            .whereEqualTo("appId", appId)
            .whereGreaterThanOrEqualTo("rating", 1L)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val reviews = snapshot?.documents
                    ?.mapNotNull { it.toAppReviewOrNull() }
                    ?.sortedWith(
                        compareByDescending<AppReview> { it.helpfulCount }
                            .thenByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                    )
                    ?: emptyList()
                trySend(Result.success(reviews))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of all reviews across the store for Admin moderation & helpful vote insights.
     */
    fun observeAllReviews(): Flow<Result<List<AppReview>>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("reviews")
            .whereGreaterThanOrEqualTo("rating", 1L)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val reviews = snapshot?.documents
                    ?.mapNotNull { it.toAppReviewOrNull() }
                    ?.sortedWith(
                        compareByDescending<AppReview> { it.flagged }
                            .thenByDescending { it.helpfulCount }
                            .thenByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                    )
                    ?: emptyList()
                trySend(Result.success(reviews))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Submits or updates a user's 1..5 star rating and written review for an app,
     * and updates the aggregate `ratingSum` and `reviewCount` on the `/apps/{appId}` document.
     */
    suspend fun submitOrUpdateReview(
        app: StoreApp,
        userId: String,
        userName: String,
        rating: Long,
        comment: String,
        verifiedInstall: Boolean
    ): Result<Unit> = runCatching {
        val targetAppId = app.appId.ifBlank { app.docId }
        val appDocId = app.docId.ifBlank { app.appId }
        require(targetAppId.isNotBlank()) { "App ID cannot be blank" }
        require(userId.isNotBlank()) { "User must be signed in to post a review" }
        val coercedRating = rating.coerceIn(1L, 5L)
        val cleanComment = comment.trim().take(1500).ifBlank { "Rated $coercedRating stars." }
        val cleanUserName = userName.trim().take(80).ifBlank { "Zayaan Community Member" }
        val reviewDocId = "${targetAppId}_${userId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

        val reviewRef = db.collection("reviews").document(reviewDocId)
        val existingSnap = reviewRef.get().await()
        val previousRating = if (existingSnap.exists()) {
            existingSnap.getLong("rating") ?: 0L
        } else {
            0L
        }

        if (!existingSnap.exists()) {
            val payload = hashMapOf<String, Any>(
                "appId" to targetAppId.take(100),
                "appName" to app.appName.trim().take(80).ifBlank { targetAppId },
                "userId" to userId,
                "userName" to cleanUserName,
                "rating" to coercedRating,
                "comment" to cleanComment,
                "verifiedInstall" to verifiedInstall,
                "helpfulCount" to 0L,
                "developerReply" to "",
                "flagged" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            reviewRef.set(payload).await()
        } else {
            val updates = mapOf<String, Any>(
                "rating" to coercedRating,
                "comment" to cleanComment,
                "userName" to cleanUserName,
                "verifiedInstall" to verifiedInstall,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            reviewRef.update(updates).await()
        }

        // Update aggregate ratingSum and reviewCount on the published StoreApp
        if (appDocId.isNotBlank()) {
            val appRef = db.collection("apps").document(appDocId)
            val newReviewCount = if (existingSnap.exists()) {
                app.reviewCount.coerceAtLeast(1L)
            } else {
                app.reviewCount + 1L
            }
            val newRatingSum = (app.ratingSum - previousRating + coercedRating).coerceAtLeast(coercedRating)
            try {
                appRef.update(
                    mapOf(
                        "ratingSum" to newRatingSum,
                        "reviewCount" to newReviewCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {
                // Ignore if app document is not yet persisted in Firestore
            }
        }
    }

    /**
     * Increments the `helpfulCount` on a community review by +1.
     */
    suspend fun voteReviewHelpful(review: AppReview): Result<Unit> = runCatching {
        require(review.id.isNotBlank()) { "Review ID cannot be blank" }
        db.collection("reviews").document(review.id).update(
            mapOf(
                "helpfulCount" to (review.helpfulCount + 1L),
                "flagged" to review.flagged,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Flags or unflags a review for administrator moderation.
     */
    suspend fun setReviewFlagged(review: AppReview, flagged: Boolean): Result<Unit> = runCatching {
        require(review.id.isNotBlank()) { "Review ID cannot be blank" }
        db.collection("reviews").document(review.id).update(
            mapOf(
                "helpfulCount" to review.helpfulCount,
                "flagged" to flagged,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Posts an official developer/administrator reply to a review and clears any moderation flag.
     */
    suspend fun replyToReviewAsAdmin(review: AppReview, replyText: String): Result<Unit> = runCatching {
        require(review.id.isNotBlank()) { "Review ID cannot be blank" }
        db.collection("reviews").document(review.id).update(
            mapOf(
                "developerReply" to replyText.trim().take(1000),
                "flagged" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Deletes a review (allowed for the review author or store administrator).
     */
    suspend fun deleteReview(reviewId: String): Result<Unit> = runCatching {
        require(reviewId.isNotBlank()) { "Review ID cannot be blank" }
        db.collection("reviews").document(reviewId).delete().await()
    }

    /**
     * Real-time stream of Store Notifications & Release Announcements (`/notifications/{notificationId}`).
     */
    fun observeNotifications(): Flow<Result<List<StoreNotification>>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("notifications")
            .whereEqualTo("published", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val title = doc.getString("title") ?: return@mapNotNull null
                        StoreNotification(
                            id = doc.id,
                            title = title,
                            body = doc.getString("body") ?: "",
                            category = doc.getString("category") ?: "ANNOUNCEMENT",
                            targetAppId = doc.getString("targetAppId") ?: "",
                            published = doc.getBoolean("published") ?: true,
                            authorUid = doc.getString("authorUid") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                    ?.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    ?: emptyList()
                trySend(Result.success(list))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Publishes a new release, update, or store announcement notification.
     */
    suspend fun createStoreNotification(
        title: String,
        body: String,
        category: String,
        targetAppId: String,
        authorUid: String
    ): Result<Unit> = runCatching {
        val validCategories = setOf("NEW_RELEASE", "APP_UPDATE", "SECURITY", "FEATURED", "COLLECTION", "ANNOUNCEMENT")
        val normalizedCat = if (category in validCategories) category else "ANNOUNCEMENT"
        val docId = "notif_${System.currentTimeMillis()}"
        val payload = mapOf<String, Any>(
            "title" to title.trim().take(120).ifBlank { "Zayaan Apps Update" },
            "body" to body.trim().take(1000).ifBlank { "New application release available." },
            "category" to normalizedCat,
            "targetAppId" to targetAppId.trim().take(100),
            "published" to true,
            "authorUid" to authorUid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("notifications").document(docId).set(payload).await()
    }

    suspend fun deleteStoreNotification(notificationId: String): Result<Unit> = runCatching {
        db.collection("notifications").document(notificationId).delete().await()
    }

    /**
     * Creates a new application document in `/apps/{appId}` adhering strictly to `firestore.rules`,
     * and records the initial release in `/version_history/{versionId}`.
     */
    suspend fun createApp(app: StoreApp, authorUid: String): Result<Unit> = runCatching {
        val slug = app.appId.ifBlank {
            app.appName.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "app-${System.currentTimeMillis()}" }
        }.replace(Regex("[^a-zA-Z0-9_\\-]"), "-").take(100)
        val docRef = db.collection("apps").document(slug)
        val payload = hashMapOf<String, Any>(
            "appId" to slug,
            "appName" to app.appName.trim().take(80).ifBlank { slug },
            "packageName" to app.packageName.trim().take(150).ifBlank { "com.zayaanapps.custom" },
            "category" to if (app.category in STORE_CATEGORIES) app.category else "Other",
            "developerName" to app.developerName.trim().take(80).ifBlank { "Zayaan Apps Studio" },
            "shortDescription" to app.shortDescription.trim().take(200),
            "description" to app.description.trim().take(4000).ifBlank { "Official Zayaan Apps package." },
            "iconUrl" to app.iconUrl.trim().take(1000),
            "screenshots" to app.screenshots.map { it.trim() }.filter { it.isNotEmpty() }.take(10),
            "versionName" to app.versionName.trim().take(40).ifBlank { "1.0.0.1" },
            "versionCode" to app.versionCode.coerceIn(1L, 2_000_000_000L),
            "apkDownloadUrl" to app.apkDownloadUrl.trim().take(1000).ifBlank {
                "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk"
            },
            "apkFileSize" to app.apkFileSize.trim().take(40).ifBlank { "12.0 MB" },
            "releaseNotes" to app.releaseNotes.trim().take(2000),
            "featured" to app.featured,
            "editorsChoice" to app.editorsChoice,
            "essential" to app.essential,
            "published" to app.published,
            "downloadCount" to app.downloadCount.coerceAtLeast(0L),
            "ratingSum" to app.ratingSum.coerceAtLeast(0L),
            "reviewCount" to app.reviewCount.coerceAtLeast(0L),
            "sha256Checksum" to app.sha256Checksum.trim().take(128),
            "minAndroidSdk" to app.minAndroidSdk.coerceIn(21L, 40L),
            "targetAndroidSdk" to app.targetAndroidSdk.coerceIn(21L, 40L),
            "architecture" to app.architecture.trim().take(80),
            "permissionsList" to app.permissionsList.map { it.trim() }.filter { it.isNotEmpty() }.take(15),
            "privacyPolicyUrl" to app.privacyPolicyUrl.trim().take(500),
            "websiteUrl" to app.websiteUrl.trim().take(500),
            "supportEmail" to app.supportEmail.trim().take(150),
            "githubRepoUrl" to app.githubRepoUrl.trim().take(500),
            "promoVideoUrl" to app.promoVideoUrl.trim().take(500),
            "offlineCapable" to app.offlineCapable,
            "pageViews" to app.pageViews.coerceAtLeast(0L),
            "authorUid" to authorUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()

        // Automatically record release entry in /version_history
        try {
            recordAppVersion(
                appId = slug,
                versionName = app.versionName.ifBlank { "1.0.0.1" },
                versionCode = app.versionCode.coerceAtLeast(1L),
                apkDownloadUrl = app.apkDownloadUrl.ifBlank {
                    "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk"
                },
                apkFileSize = app.apkFileSize.ifBlank { "12.0 MB" },
                sha256Checksum = app.sha256Checksum,
                changelog = app.releaseNotes.ifBlank { "Initial catalog release v${app.versionName}." },
                authorUid = authorUid
            )
        } catch (_: Exception) {
            // Best-effort version history logging
        }
    }

    /**
     * Updates an existing application document in `/apps/{docId}` preserving immutable fields (`appId`, `authorUid`, `createdAt`).
     */
    suspend fun updateApp(app: StoreApp, actorUid: String = ""): Result<Unit> = runCatching {
        val docId = app.docId.ifBlank { app.appId }
        require(docId.isNotBlank()) { "Document ID cannot be blank when updating an app." }
        val docRef = db.collection("apps").document(docId)
        val updates = mapOf<String, Any>(
            "appName" to app.appName.trim().take(80),
            "packageName" to app.packageName.trim().take(150),
            "category" to if (app.category in STORE_CATEGORIES) app.category else "Other",
            "developerName" to app.developerName.trim().take(80),
            "shortDescription" to app.shortDescription.trim().take(200),
            "description" to app.description.trim().take(4000),
            "iconUrl" to app.iconUrl.trim().take(1000),
            "screenshots" to app.screenshots.map { it.trim() }.filter { it.isNotEmpty() }.take(10),
            "versionName" to app.versionName.trim().take(40),
            "versionCode" to app.versionCode.coerceIn(1L, 2_000_000_000L),
            "apkDownloadUrl" to app.apkDownloadUrl.trim().take(1000),
            "apkFileSize" to app.apkFileSize.trim().take(40),
            "releaseNotes" to app.releaseNotes.trim().take(2000),
            "featured" to app.featured,
            "editorsChoice" to app.editorsChoice,
            "essential" to app.essential,
            "published" to app.published,
            "downloadCount" to app.downloadCount.coerceAtLeast(0L),
            "sha256Checksum" to app.sha256Checksum.trim().take(128),
            "minAndroidSdk" to app.minAndroidSdk.coerceIn(21L, 40L),
            "targetAndroidSdk" to app.targetAndroidSdk.coerceIn(21L, 40L),
            "architecture" to app.architecture.trim().take(80),
            "permissionsList" to app.permissionsList.map { it.trim() }.filter { it.isNotEmpty() }.take(15),
            "privacyPolicyUrl" to app.privacyPolicyUrl.trim().take(500),
            "websiteUrl" to app.websiteUrl.trim().take(500),
            "supportEmail" to app.supportEmail.trim().take(150),
            "githubRepoUrl" to app.githubRepoUrl.trim().take(500),
            "promoVideoUrl" to app.promoVideoUrl.trim().take(500),
            "offlineCapable" to app.offlineCapable,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.update(updates).await()

        if (actorUid.isNotBlank()) {
            try {
                recordAppVersion(
                    appId = app.appId.ifBlank { docId },
                    versionName = app.versionName,
                    versionCode = app.versionCode.coerceAtLeast(1L),
                    apkDownloadUrl = app.apkDownloadUrl,
                    apkFileSize = app.apkFileSize,
                    sha256Checksum = app.sha256Checksum,
                    changelog = app.releaseNotes.ifBlank { "Updated release v${app.versionName}." },
                    authorUid = actorUid
                )
            } catch (_: Exception) {
                // Best-effort version history logging
            }
        }
    }

    /**
     * Deletes an application document from `/apps/{docId}`.
     */
    suspend fun deleteApp(docId: String): Result<Unit> = runCatching {
        db.collection("apps").document(docId).delete().await()
    }

    /**
     * Atomically increments `downloadCount` by +1 using the public counter rule in `firestore.rules`.
     */
    suspend fun incrementDownloadCount(docId: String, currentCount: Long): Result<Unit> = runCatching {
        val docRef = db.collection("apps").document(docId)
        docRef.update(
            mapOf(
                "downloadCount" to (currentCount + 1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Increments `pageViews` by +1 when a user inspects an application's details page.
     */
    suspend fun incrementPageViewCount(docId: String, currentViews: Long): Result<Unit> = runCatching {
        if (docId.isBlank()) return@runCatching
        db.collection("apps").document(docId).update(
            mapOf(
                "pageViews" to (currentViews + 1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Adds or updates a category in `/categories/{categoryId}`.
     */
    suspend fun upsertCategory(
        name: String,
        description: String,
        displayOrder: Long,
        authorUid: String = ""
    ): Result<Unit> = runCatching {
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val docRef = db.collection("categories").document(slug)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            docRef.set(
                mapOf(
                    "name" to name.trim().take(50),
                    "description" to description.trim().take(300).ifBlank { "$name applications" },
                    "displayOrder" to displayOrder.coerceIn(0L, 1000L),
                    "authorUid" to authorUid.ifBlank { "admin" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "name" to name.trim().take(50),
                    "description" to description.trim().take(300).ifBlank { "$name applications" },
                    "displayOrder" to displayOrder.coerceIn(0L, 1000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun deleteCategory(docId: String): Result<Unit> = runCatching {
        db.collection("categories").document(docId).delete().await()
    }

    // =========================================================================
    // PHASE 3: COLLECTIONS, SUBMISSIONS, REPORTS, VERSION HISTORY, RBAC & AUDIT
    // =========================================================================

    /**
     * Real-time stream of curated & featured collections (`/collections/{collectionId}`).
     */
    fun observeCollections(isAdmin: Boolean = false): Flow<Result<List<AppCollection>>> = callbackFlow {
        val query = if (isAdmin) {
            db.collection("collections")
        } else {
            db.collection("collections").whereEqualTo("isPublic", true)
        }
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            val list = snapshot?.documents
                ?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val appIds = (doc.get("appIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    AppCollection(
                        id = doc.id,
                        title = title,
                        subtitle = doc.getString("subtitle") ?: "",
                        appIds = appIds,
                        authorUid = doc.getString("authorUid") ?: "",
                        isPublic = doc.getBoolean("isPublic") ?: true,
                        featured = doc.getBoolean("featured") ?: true,
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                }
                ?.sortedByDescending { it.featured }
                ?: emptyList()
            trySend(Result.success(list))
        }
        awaitClose { registration.remove() }
    }

    suspend fun upsertCollection(
        collectionId: String,
        title: String,
        subtitle: String,
        appIds: List<String>,
        isPublic: Boolean,
        featured: Boolean,
        authorUid: String
    ): Result<Unit> = runCatching {
        val cleanId = collectionId.ifBlank {
            "col_${title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}_${System.currentTimeMillis() % 10000}"
        }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
        val docRef = db.collection("collections").document(cleanId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            docRef.set(
                mapOf(
                    "title" to title.trim().take(80).ifBlank { "Curated Collection" },
                    "subtitle" to subtitle.trim().take(250),
                    "appIds" to appIds.map { it.trim().take(100) }.filter { it.isNotEmpty() }.distinct().take(30),
                    "authorUid" to authorUid,
                    "isPublic" to isPublic,
                    "featured" to featured,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "title" to title.trim().take(80).ifBlank { "Curated Collection" },
                    "subtitle" to subtitle.trim().take(250),
                    "appIds" to appIds.map { it.trim().take(100) }.filter { it.isNotEmpty() }.distinct().take(30),
                    "isPublic" to isPublic,
                    "featured" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun deleteCollection(collectionId: String): Result<Unit> = runCatching {
        db.collection("collections").document(collectionId).delete().await()
    }

    /**
     * Real-time stream of Developer Portal application submissions (`/submissions/{submissionId}`).
     */
    fun observeDeveloperSubmissions(currentUserId: String, isAdmin: Boolean): Flow<Result<List<DeveloperSubmission>>> = callbackFlow {
        val query = if (isAdmin) {
            db.collection("submissions")
        } else {
            db.collection("submissions").whereEqualTo("submitterUid", currentUserId)
        }
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            val list = snapshot?.documents
                ?.mapNotNull { doc ->
                    val appName = doc.getString("appName") ?: return@mapNotNull null
                    DeveloperSubmission(
                        id = doc.id,
                        appId = doc.getString("appId") ?: doc.id,
                        appName = appName,
                        packageName = doc.getString("packageName") ?: "",
                        category = doc.getString("category") ?: "Tools",
                        developerName = doc.getString("developerName") ?: "",
                        description = doc.getString("description") ?: "",
                        versionName = doc.getString("versionName") ?: "1.0.0.1",
                        versionCode = doc.getLong("versionCode") ?: 1L,
                        apkDownloadUrl = doc.getString("apkDownloadUrl") ?: "",
                        apkFileSize = doc.getString("apkFileSize") ?: "12.0 MB",
                        sha256Checksum = doc.getString("sha256Checksum") ?: "",
                        releaseNotes = doc.getString("releaseNotes") ?: "",
                        privacyPolicyUrl = doc.getString("privacyPolicyUrl") ?: "",
                        supportEmail = doc.getString("supportEmail") ?: "",
                        status = doc.getString("status") ?: "PENDING",
                        reviewNote = doc.getString("reviewNote") ?: "",
                        submitterUid = doc.getString("submitterUid") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                }
                ?.sortedWith(
                    compareBy<DeveloperSubmission> { it.status != "PENDING" }
                        .thenByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                )
                ?: emptyList()
            trySend(Result.success(list))
        }
        awaitClose { registration.remove() }
    }

    suspend fun createDeveloperSubmission(
        submission: DeveloperSubmission,
        submitterUid: String
    ): Result<Unit> = runCatching {
        val slug = submission.appId.ifBlank {
            submission.appName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }.replace(Regex("[^a-zA-Z0-9_\\-]"), "-").take(100).ifBlank { "app-${System.currentTimeMillis()}" }
        val docId = "sub_${slug}_${System.currentTimeMillis() % 100000}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val payload = mapOf<String, Any>(
            "appId" to slug,
            "appName" to submission.appName.trim().take(80).ifBlank { slug },
            "packageName" to submission.packageName.trim().take(150).ifBlank { "com.zayaanapps.$slug" },
            "category" to if (submission.category in STORE_CATEGORIES) submission.category else "Tools",
            "developerName" to submission.developerName.trim().take(80).ifBlank { "Community Developer" },
            "description" to submission.description.trim().take(4000).ifBlank { "Submitted via Zayaan Developer Portal." },
            "versionName" to submission.versionName.trim().take(40).ifBlank { "1.0.0.1" },
            "versionCode" to submission.versionCode.coerceIn(1L, 2_000_000_000L),
            "apkDownloadUrl" to submission.apkDownloadUrl.trim().take(1000).ifBlank {
                "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk"
            },
            "apkFileSize" to submission.apkFileSize.trim().take(40).ifBlank { "12.0 MB" },
            "sha256Checksum" to submission.sha256Checksum.trim().take(128),
            "releaseNotes" to submission.releaseNotes.trim().take(2000),
            "privacyPolicyUrl" to submission.privacyPolicyUrl.trim().take(500),
            "supportEmail" to submission.supportEmail.trim().take(150),
            "status" to "PENDING",
            "reviewNote" to "",
            "submitterUid" to submitterUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("submissions").document(docId).set(payload).await()
    }

    /**
     * Approves or rejects a Developer Submission. When `newStatus == "APPROVED"`, automatically
     * creates or updates the application in `/apps/{appId}` and records the version in `/version_history`.
     */
    suspend fun reviewDeveloperSubmission(
        submission: DeveloperSubmission,
        newStatus: String,
        reviewNote: String,
        adminUid: String
    ): Result<Unit> = runCatching {
        require(submission.id.isNotBlank()) { "Submission ID cannot be blank" }
        val normalizedStatus = if (newStatus in setOf("PENDING", "APPROVED", "REJECTED")) newStatus else "PENDING"
        db.collection("submissions").document(submission.id).update(
            mapOf(
                "status" to normalizedStatus,
                "reviewNote" to reviewNote.trim().take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        if (normalizedStatus == "APPROVED") {
            val targetSlug = submission.appId.ifBlank {
                submission.appName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            }.replace(Regex("[^a-zA-Z0-9_\\-]"), "-").take(100)
            val existingAppSnap = db.collection("apps").document(targetSlug).get().await()
            val storeApp = StoreApp(
                docId = targetSlug,
                appId = targetSlug,
                appName = submission.appName,
                packageName = submission.packageName,
                category = submission.category,
                developerName = submission.developerName,
                description = submission.description,
                shortDescription = submission.description.lineSequence().firstOrNull()?.take(160) ?: "",
                versionName = submission.versionName,
                versionCode = submission.versionCode,
                apkDownloadUrl = submission.apkDownloadUrl,
                apkFileSize = submission.apkFileSize,
                sha256Checksum = submission.sha256Checksum,
                releaseNotes = submission.releaseNotes,
                privacyPolicyUrl = submission.privacyPolicyUrl,
                supportEmail = submission.supportEmail,
                published = true,
                featured = false
            )
            if (existingAppSnap.exists()) {
                updateApp(storeApp, adminUid).getOrThrow()
            } else {
                createApp(storeApp, adminUid).getOrThrow()
            }
        }
    }

    suspend fun deleteDeveloperSubmission(submissionId: String): Result<Unit> = runCatching {
        db.collection("submissions").document(submissionId).delete().await()
    }

    /**
     * Real-time stream of user-submitted security & moderation reports (`/reports/{reportId}`).
     */
    fun observeReports(currentUserId: String, isAdmin: Boolean): Flow<Result<List<AppReport>>> = callbackFlow {
        val query = if (isAdmin) {
            db.collection("reports")
        } else {
            db.collection("reports").whereEqualTo("reporterUid", currentUserId)
        }
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            val list = snapshot?.documents
                ?.mapNotNull { doc ->
                    val targetId = doc.getString("targetId") ?: return@mapNotNull null
                    AppReport(
                        id = doc.id,
                        targetId = targetId,
                        targetType = doc.getString("targetType") ?: "APP",
                        reason = doc.getString("reason") ?: "",
                        details = doc.getString("details") ?: "",
                        reporterUid = doc.getString("reporterUid") ?: "",
                        status = doc.getString("status") ?: "OPEN",
                        createdAt = doc.getTimestamp("createdAt"),
                        updatedAt = doc.getTimestamp("updatedAt")
                    )
                }
                ?.sortedWith(
                    compareBy<AppReport> { it.status != "OPEN" }
                        .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                )
                ?: emptyList()
            trySend(Result.success(list))
        }
        awaitClose { registration.remove() }
    }

    suspend fun submitReport(
        targetId: String,
        targetType: String,
        reason: String,
        details: String,
        reporterUid: String
    ): Result<Unit> = runCatching {
        val cleanType = if (targetType in setOf("APP", "REVIEW")) targetType else "APP"
        val docId = "rep_${System.currentTimeMillis()}"
        val payload = mapOf<String, Any>(
            "targetId" to targetId.trim().take(128).ifBlank { "unknown" },
            "targetType" to cleanType,
            "reason" to reason.trim().take(120).ifBlank { "General Report" },
            "details" to details.trim().take(1000),
            "reporterUid" to reporterUid,
            "status" to "OPEN",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("reports").document(docId).set(payload).await()
    }

    suspend fun updateReportStatus(reportId: String, newStatus: String): Result<Unit> = runCatching {
        val cleanStatus = if (newStatus in setOf("OPEN", "RESOLVED", "DISMISSED")) newStatus else "RESOLVED"
        db.collection("reports").document(reportId).update(
            mapOf(
                "status" to cleanStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteReport(reportId: String): Result<Unit> = runCatching {
        db.collection("reports").document(reportId).delete().await()
    }

    /**
     * Real-time stream of multi-version release history (`/version_history/{versionId}`).
     */
    fun observeVersionHistory(): Flow<Result<List<AppVersionRecord>>> = callbackFlow {
        val registration: ListenerRegistration = db.collection("version_history")
            .whereGreaterThanOrEqualTo("versionCode", 1L)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val appId = doc.getString("appId") ?: return@mapNotNull null
                        AppVersionRecord(
                            id = doc.id,
                            appId = appId,
                            versionName = doc.getString("versionName") ?: "1.0.0.1",
                            versionCode = doc.getLong("versionCode") ?: 1L,
                            apkDownloadUrl = doc.getString("apkDownloadUrl") ?: "",
                            apkFileSize = doc.getString("apkFileSize") ?: "12.0 MB",
                            sha256Checksum = doc.getString("sha256Checksum") ?: "",
                            changelog = doc.getString("changelog") ?: "",
                            authorUid = doc.getString("authorUid") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                    ?.sortedWith(
                        compareBy<AppVersionRecord> { it.appId }
                            .thenByDescending { it.versionCode }
                    )
                    ?: emptyList()
                trySend(Result.success(list))
            }
        awaitClose { registration.remove() }
    }

    suspend fun recordAppVersion(
        appId: String,
        versionName: String,
        versionCode: Long,
        apkDownloadUrl: String,
        apkFileSize: String,
        sha256Checksum: String,
        changelog: String,
        authorUid: String
    ): Result<Unit> = runCatching {
        val cleanAppId = appId.trim().take(100)
        require(cleanAppId.isNotBlank()) { "App ID cannot be blank" }
        val cleanCode = versionCode.coerceIn(1L, 2_000_000_000L)
        val docId = "ver_${cleanAppId}_$cleanCode".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val payload = mapOf<String, Any>(
            "appId" to cleanAppId,
            "versionName" to versionName.trim().take(40).ifBlank { "1.0.0.1" },
            "versionCode" to cleanCode,
            "apkDownloadUrl" to apkDownloadUrl.trim().take(1000).ifBlank {
                "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk"
            },
            "apkFileSize" to apkFileSize.trim().take(40).ifBlank { "12.0 MB" },
            "sha256Checksum" to sha256Checksum.trim().take(128),
            "changelog" to changelog.trim().take(2000),
            "authorUid" to authorUid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("version_history").document(docId).set(payload).await()
    }

    suspend fun deleteVersionRecord(versionId: String): Result<Unit> = runCatching {
        db.collection("version_history").document(versionId).delete().await()
    }

    /**
     * Real-time stream of Trusted Administrators Registry (`/admins/{adminUid}`).
     */
    fun observeAdminRecords(currentUserId: String, isAdmin: Boolean): Flow<Result<List<AdminRecord>>> = callbackFlow {
        if (!isAdmin) {
            val registration = db.collection("admins").document(currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val rec = AdminRecord(
                            id = snapshot.id,
                            userId = snapshot.getString("userId") ?: snapshot.id,
                            role = snapshot.getString("role") ?: "admin",
                            createdAt = snapshot.getTimestamp("createdAt")
                        )
                        trySend(Result.success(listOf(rec)))
                    } else {
                        trySend(Result.success(emptyList()))
                    }
                }
            awaitClose { registration.remove() }
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection("admins")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val userId = doc.getString("userId") ?: return@mapNotNull null
                        AdminRecord(
                            id = doc.id,
                            userId = userId,
                            role = doc.getString("role") ?: "admin",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                    ?: emptyList()
                trySend(Result.success(list))
            }
        awaitClose { registration.remove() }
    }

    suspend fun upsertAdminRole(targetUserId: String, role: String): Result<Unit> = runCatching {
        val cleanUid = targetUserId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(128)
        require(cleanUid.isNotBlank()) { "Administrator UID cannot be blank" }
        val cleanRole = if (role in setOf("admin", "superadmin")) role else "admin"
        val docRef = db.collection("admins").document(cleanUid)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            docRef.set(
                mapOf(
                    "userId" to cleanUid,
                    "role" to cleanRole,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(mapOf("role" to cleanRole)).await()
        }
    }

    suspend fun removeAdminRole(targetUserId: String): Result<Unit> = runCatching {
        db.collection("admins").document(targetUserId.trim()).delete().await()
    }

    /**
     * Real-time stream of Immutable Audit Logs (`/audit_logs/{logId}`).
     */
    fun observeAuditLogs(currentUserId: String, isAdmin: Boolean): Flow<Result<List<AuditLogRecord>>> = callbackFlow {
        val query = if (isAdmin) {
            db.collection("audit_logs")
        } else {
            db.collection("audit_logs").whereEqualTo("actorUid", currentUserId)
        }
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            val list = snapshot?.documents
                ?.mapNotNull { doc ->
                    val action = doc.getString("action") ?: return@mapNotNull null
                    AuditLogRecord(
                        id = doc.id,
                        action = action,
                        targetType = doc.getString("targetType") ?: "APP",
                        targetId = doc.getString("targetId") ?: "",
                        summary = doc.getString("summary") ?: "",
                        actorUid = doc.getString("actorUid") ?: "",
                        actorEmail = doc.getString("actorEmail") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
                ?.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                ?: emptyList()
            trySend(Result.success(list))
        }
        awaitClose { registration.remove() }
    }

    suspend fun recordAuditLog(
        action: String,
        targetType: String,
        targetId: String,
        summary: String,
        actorUid: String,
        actorEmail: String
    ): Result<Unit> = runCatching {
        if (actorUid.isBlank()) return@runCatching
        val validTypes = setOf("APP", "SUBMISSION", "CATEGORY", "COLLECTION", "REVIEW", "REPORT", "NOTIFICATION", "ADMIN", "CATALOG")
        val normalizedType = if (targetType in validTypes) targetType else "APP"
        val docId = "audit_${System.currentTimeMillis()}_${(100..999).random()}"
        val payload = mapOf<String, Any>(
            "action" to action.trim().take(80).ifBlank { "ADMIN_ACTION" },
            "targetType" to normalizedType,
            "targetId" to targetId.trim().take(128).ifBlank { "catalog" },
            "summary" to summary.trim().take(500).ifBlank { action },
            "actorUid" to actorUid.trim().take(128),
            "actorEmail" to actorEmail.trim().take(150),
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("audit_logs").document(docId).set(payload).await()
    }

    /**
     * Seeds the official Zayaan Apps starter catalog into Firestore when the catalog is empty
     * or when triggered by the admin.
     */
    suspend fun seedOfficialZayaanCatalog(authorUid: String): Result<Int> = runCatching {
        val starterApps = officialStarterApps()

        var seededCount = 0
        for (app in starterApps) {
            val docRef = db.collection("apps").document(app.appId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                createApp(app, authorUid).getOrThrow()
                seededCount++
            }
        }

        val defaultCategories = officialDefaultCategories()
        for (cat in defaultCategories) {
            try {
                upsertCategory(cat.name, cat.description, cat.displayOrder, authorUid)
            } catch (_: FirebaseFirestoreException) {
                // Non-admin users cannot write to categories; ignore gracefully
            }
        }
        seededCount
    }

    companion object {
        fun officialDefaultCategories(): List<StoreCategory> = listOf(
            StoreCategory("games", "Games", "Arcade, action, puzzle, and high-FPS Android gaming experiences.", 1),
            StoreCategory("tools", "Tools", "Essential system tools, package managers, and developer utilities.", 2),
            StoreCategory("ai-apps", "AI Apps", "Intelligent assistants, generative tools, and smart productivity workflows.", 3),
            StoreCategory("productivity", "Productivity", "Focus timers, note organizers, and workflow boosters.", 4),
            StoreCategory("utilities", "Utilities", "File managers, network analyzers, and device optimization apps.", 5),
            StoreCategory("education", "Education", "Interactive coding labs, tutorials, and learning companions.", 6),
            StoreCategory("entertainment", "Entertainment", "Media players, streaming companions, and creative audio/video apps.", 7),
            StoreCategory("business", "Business", "Invoicing, ledger tools, and mobile office suites.", 8),
            StoreCategory("social-communication", "Social & Communication", "Community forums, developer channels, and messaging tools.", 9)
        )

        fun officialStarterApps(): List<StoreApp> = listOf(
            StoreApp(
                appId = "zayaan-apps-store",
                docId = "zayaan-apps-store",
                appName = "Zayaan Apps Store",
                packageName = "com.zayaanapps.store",
                category = "Tools",
                developerName = "Zayaan Apps Official",
                shortDescription = "Official Zayaan Apps Android marketplace & GitHub Releases distribution client.",
                description = "The official Zayaan Apps Android App Store & Web Distribution client. Discover, download, verify, and update official Zayaan Android applications directly from GitHub Releases with real-time version tracking.",
                iconUrl = "https://raw.githubusercontent.com/zayaanapps/zayaan-apps/main/web/icons/Icon-192.png",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&q=80",
                    "https://images.unsplash.com/photo-1551650975-87deedd944c3?w=800&q=80",
                    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80"
                ),
                versionName = "1.0.0.1",
                versionCode = 1,
                apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk",
                apkFileSize = "14.2 MB",
                releaseNotes = "v1.0.0.1 Marketplace Release:\n• Google Play Store-inspired discovery sections & curated collections\n• Advanced search with instant suggestions, history, filters & sorting\n• Rich app detail sheets with related apps & developer specs\n• Automatic Store Self-Update notification & 1-tap APK upgrade",
                featured = true,
                editorsChoice = true,
                essential = true,
                published = true,
                downloadCount = 128,
                minAndroidSdk = 24,
                targetAndroidSdk = 35,
                architecture = "arm64-v8a, armeabi-v7a, x86_64",
                permissionsList = listOf("INTERNET", "REQUEST_INSTALL_PACKAGES", "POST_NOTIFICATIONS", "QUERY_ALL_PACKAGES"),
                privacyPolicyUrl = "https://zayaanapps.github.io/zayaan-apps/",
                websiteUrl = "https://zayaanapps.github.io/zayaan-apps/",
                supportEmail = "zayaanapps@gmail.com",
                githubRepoUrl = "https://github.com/zayaanapps/zayaan-apps",
                offlineCapable = false
            ),
            StoreApp(
                appId = "zayaan-ai-studio",
                docId = "zayaan-ai-studio",
                appName = "Zayaan AI Assistant",
                packageName = "com.zayaanapps.aistudio",
                category = "AI Apps",
                developerName = "Zayaan AI Lab",
                shortDescription = "Multimodal AI coding assistant, prompt library & voice reasoning companion.",
                description = "Next-generation multimodal AI assistant built for Android. Includes code synthesis, markdown export, offline prompt library, and instant voice-to-text reasoning workflows.",
                iconUrl = "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=256&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&q=80",
                    "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=800&q=80"
                ),
                versionName = "2.4.0.1",
                versionCode = 24,
                apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk",
                apkFileSize = "18.6 MB",
                releaseNotes = "v2.4.0.1:\n• Faster streaming responses\n• Added code syntax highlighting for Kotlin, Dart, and Python\n• Offline prompt templates",
                featured = true,
                editorsChoice = true,
                essential = true,
                published = true,
                downloadCount = 342,
                minAndroidSdk = 26,
                targetAndroidSdk = 35,
                architecture = "arm64-v8a",
                permissionsList = listOf("INTERNET", "RECORD_AUDIO"),
                privacyPolicyUrl = "https://zayaanapps.github.io/zayaan-apps/",
                websiteUrl = "https://zayaanapps.github.io/zayaan-apps/",
                supportEmail = "zayaanapps@gmail.com",
                githubRepoUrl = "https://github.com/zayaanapps/zayaan-apps",
                offlineCapable = true
            ),
            StoreApp(
                appId = "zayaan-file-commander",
                docId = "zayaan-file-commander",
                appName = "Zayaan File Commander",
                packageName = "com.zayaanapps.filecommander",
                category = "Utilities",
                developerName = "Zayaan Systems",
                shortDescription = "Dual-pane Android file explorer, SHA-256 APK verifier & archive manager.",
                description = "High-speed Android file manager, APK inspector, and archive extractor with Material 3 expressive design, storage heatmap analytics, and zero ads.",
                iconUrl = "https://images.unsplash.com/photo-1618401471353-b98afee0b2eb?w=256&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&q=80",
                    "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80"
                ),
                versionName = "1.8.2.1",
                versionCode = 18,
                apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk",
                apkFileSize = "9.4 MB",
                releaseNotes = "v1.8.2.1:\n• Added SHA-256 APK checksum verification tool\n• Dual-pane tablet and foldable layout support",
                featured = true,
                editorsChoice = false,
                essential = true,
                published = true,
                downloadCount = 215,
                minAndroidSdk = 24,
                targetAndroidSdk = 35,
                architecture = "arm64-v8a, armeabi-v7a",
                permissionsList = listOf("INTERNET"),
                privacyPolicyUrl = "https://zayaanapps.github.io/zayaan-apps/",
                websiteUrl = "https://zayaanapps.github.io/zayaan-apps/",
                supportEmail = "zayaanapps@gmail.com",
                githubRepoUrl = "https://github.com/zayaanapps/zayaan-apps",
                offlineCapable = true
            ),
            StoreApp(
                appId = "zayaan-cyber-runner",
                docId = "zayaan-cyber-runner",
                appName = "Cyber Grid Runner 3D",
                packageName = "com.zayaanapps.cyberrunner",
                category = "Games",
                developerName = "Zayaan Interactive",
                shortDescription = "120Hz synthwave arcade runner with gamepad support & offline procedural worlds.",
                description = "Fast-paced neon synthwave arcade runner featuring 120Hz display support, controller compatibility, and procedural obstacle courses.",
                iconUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=256&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80",
                    "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&q=80"
                ),
                versionName = "3.1.0.1",
                versionCode = 31,
                apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk",
                apkFileSize = "45.0 MB",
                releaseNotes = "v3.1.0.1:\n• New Cyber Horizon endless stage\n• Full Bluetooth gamepad support",
                featured = false,
                editorsChoice = true,
                essential = false,
                published = true,
                downloadCount = 519,
                minAndroidSdk = 26,
                targetAndroidSdk = 35,
                architecture = "arm64-v8a",
                permissionsList = listOf("INTERNET", "VIBRATE"),
                privacyPolicyUrl = "https://zayaanapps.github.io/zayaan-apps/",
                websiteUrl = "https://zayaanapps.github.io/zayaan-apps/",
                supportEmail = "zayaanapps@gmail.com",
                githubRepoUrl = "https://github.com/zayaanapps/zayaan-apps",
                offlineCapable = true
            ),
            StoreApp(
                appId = "zayaan-focus-flow",
                docId = "zayaan-focus-flow",
                appName = "Zayaan Focus & Pomodoro",
                packageName = "com.zayaanapps.focusflow",
                category = "Productivity",
                developerName = "Zayaan Apps Official",
                shortDescription = "Minimalist deep-work timer, Kanban task matrix & ambient soundscapes.",
                description = "Minimalist deep-work timer, task matrix, and daily productivity analytics with ambient soundscapes and cloud sync.",
                iconUrl = "https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=256&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?w=800&q=80"
                ),
                versionName = "1.5.0.1",
                versionCode = 15,
                apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk",
                apkFileSize = "11.1 MB",
                releaseNotes = "v1.5.0.1:\n• Added weekly streak heatmaps and Kanban task boards",
                featured = false,
                editorsChoice = false,
                essential = true,
                published = true,
                downloadCount = 176,
                minAndroidSdk = 24,
                targetAndroidSdk = 35,
                architecture = "arm64-v8a, armeabi-v7a",
                permissionsList = listOf("INTERNET", "POST_NOTIFICATIONS", "VIBRATE"),
                privacyPolicyUrl = "https://zayaanapps.github.io/zayaan-apps/",
                websiteUrl = "https://zayaanapps.github.io/zayaan-apps/",
                supportEmail = "zayaanapps@gmail.com",
                githubRepoUrl = "https://github.com/zayaanapps/zayaan-apps",
                offlineCapable = true
            ),
            StoreApp(
                appId = "zayaan-code-academy",
                docId = "zayaan-code-academy",
                appName = "Zayaan Code Academy",
                packageName = "com.zayaanapps.codeacademy",
                category = "Education",
                developerName = "Zayaan Education",
                shortDescription = "Interactive Kotlin, Jetpack Compose, Flutter & GitHub Actions labs.",
                description = "Interactive lessons, quizzes, and sandbox labs for learning Kotlin, Jetpack Compose, Flutter, and GitHub Actions automation on the go.",
                iconUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=256&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&q=80"
                ),
                versionName = "1.2.0.1",
                versionCode = 12,
                apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk",
                apkFileSize = "16.8 MB",
                releaseNotes = "v1.2.0.1:\n• New Jetpack Compose & CI/CD tracks added",
                featured = false,
                editorsChoice = true,
                essential = false,
                published = true,
                downloadCount = 143,
                minAndroidSdk = 24,
                targetAndroidSdk = 35,
                architecture = "arm64-v8a, armeabi-v7a",
                permissionsList = listOf("INTERNET"),
                privacyPolicyUrl = "https://zayaanapps.github.io/zayaan-apps/",
                websiteUrl = "https://zayaanapps.github.io/zayaan-apps/",
                supportEmail = "zayaanapps@gmail.com",
                githubRepoUrl = "https://github.com/zayaanapps/zayaan-apps",
                offlineCapable = true
            ),
            StoreApp(
                appId = "zayaan-invoice-pro",
                docId = "zayaan-invoice-pro",
                appName = "Zayaan Invoice & Ledger",
                packageName = "com.zayaanapps.invoicepro",
                category = "Business",
                developerName = "Zayaan Systems",
                shortDescription = "Offline-first PDF invoice generator, expense ledger & multi-currency billing.",
                description = "Professional invoice creator, client billing tracker, and PDF receipt generator designed for freelancers and small businesses with offline-first accuracy.",
                iconUrl = "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=256&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=800&q=80",
                    "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=800&q=80"
                ),
                versionName = "1.1.0.1",
                versionCode = 11,
                apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk",
                apkFileSize = "10.5 MB",
                releaseNotes = "v1.1.0.1:\n• Instant PDF export & customizable tax templates",
                featured = false,
                editorsChoice = false,
                essential = true,
                published = true,
                downloadCount = 94,
                minAndroidSdk = 24,
                targetAndroidSdk = 35,
                architecture = "arm64-v8a, armeabi-v7a",
                permissionsList = listOf("INTERNET"),
                privacyPolicyUrl = "https://zayaanapps.github.io/zayaan-apps/",
                websiteUrl = "https://zayaanapps.github.io/zayaan-apps/",
                supportEmail = "zayaanapps@gmail.com",
                githubRepoUrl = "https://github.com/zayaanapps/zayaan-apps",
                offlineCapable = true
            ),
            StoreApp(
                appId = "zayaan-connect-hub",
                docId = "zayaan-connect-hub",
                appName = "Zayaan Connect Hub",
                packageName = "com.zayaanapps.connecthub",
                category = "Social & Communication",
                developerName = "Zayaan Apps Official",
                shortDescription = "Developer community channels, release announcements & markdown discussions.",
                description = "Community hub for Zayaan Apps users and Android developers. Share release feedback, join beta testing channels, and discuss open-source Android tools.",
                iconUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=256&q=80",
                screenshots = listOf(
                    "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&q=80"
                ),
                versionName = "1.0.4.1",
                versionCode = 5,
                apkDownloadUrl = "https://github.com/zayaanapps/zayaan-apps/releases/latest/download/zayaan-apps.apk",
                apkFileSize = "12.8 MB",
                releaseNotes = "v1.0.4.1:\n• Added direct GitHub release discussion threads",
                featured = false,
                editorsChoice = false,
                essential = false,
                published = true,
                downloadCount = 87,
                minAndroidSdk = 24,
                targetAndroidSdk = 35,
                architecture = "arm64-v8a, armeabi-v7a",
                permissionsList = listOf("INTERNET", "POST_NOTIFICATIONS"),
                privacyPolicyUrl = "https://zayaanapps.github.io/zayaan-apps/",
                websiteUrl = "https://zayaanapps.github.io/zayaan-apps/",
                supportEmail = "zayaanapps@gmail.com",
                githubRepoUrl = "https://github.com/zayaanapps/zayaan-apps",
                offlineCapable = false
            )
        )
    }
}

private fun DocumentSnapshot.toAppReviewOrNull(): AppReview? {
    val appId = getString("appId") ?: return null
    val userId = getString("userId") ?: return null
    return AppReview(
        id = id,
        appId = appId,
        appName = getString("appName") ?: appId,
        userId = userId,
        userName = getString("userName") ?: "Zayaan User",
        rating = (getLong("rating") ?: 5L).coerceIn(1L, 5L),
        comment = getString("comment") ?: "",
        verifiedInstall = getBoolean("verifiedInstall") ?: false,
        helpfulCount = (getLong("helpfulCount") ?: 0L).coerceAtLeast(0L),
        developerReply = getString("developerReply") ?: "",
        flagged = getBoolean("flagged") ?: false,
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt")
    )
}

private fun DocumentSnapshot.toStoreAppOrNull(): StoreApp? {
    val appId = getString("appId") ?: id
    val appName = getString("appName") ?: return null
    val packageName = getString("packageName") ?: ""
    val category = getString("category") ?: "Other"
    val developerName = getString("developerName") ?: "Zayaan Apps"
    val description = getString("description") ?: ""
    val shortDescription = getString("shortDescription")?.takeIf { it.isNotBlank() }
        ?: description.lineSequence().firstOrNull()?.take(140)
        ?: ""
    val iconUrl = getString("iconUrl") ?: ""
    @Suppress("UNCHECKED_CAST")
    val screenshots = (get("screenshots") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val permissionsList = (get("permissionsList") as? List<*>)?.mapNotNull { it as? String }
        ?: listOf("INTERNET")
    val versionName = getString("versionName") ?: "1.0.0.1"
    val versionCode = getLong("versionCode") ?: 1L
    val apkDownloadUrl = getString("apkDownloadUrl") ?: ""
    val apkFileSize = getString("apkFileSize") ?: "10 MB"
    val releaseNotes = getString("releaseNotes") ?: ""
    val featured = getBoolean("featured") ?: false
    val editorsChoice = getBoolean("editorsChoice") ?: featured
    val essential = getBoolean("essential") ?: false
    val published = getBoolean("published") ?: true
    val downloadCount = getLong("downloadCount") ?: 0L
    val ratingSum = getLong("ratingSum") ?: 0L
    val reviewCount = getLong("reviewCount") ?: 0L
    val sha256Checksum = getString("sha256Checksum") ?: ""
    val minAndroidSdk = getLong("minAndroidSdk") ?: 24L
    val targetAndroidSdk = getLong("targetAndroidSdk") ?: 35L
    val architecture = getString("architecture") ?: "arm64-v8a, armeabi-v7a"
    val privacyPolicyUrl = getString("privacyPolicyUrl") ?: "https://zayaanapps.github.io/zayaan-apps/"
    val websiteUrl = getString("websiteUrl") ?: "https://zayaanapps.github.io/zayaan-apps/"
    val supportEmail = getString("supportEmail") ?: "zayaanapps@gmail.com"
    val githubRepoUrl = getString("githubRepoUrl") ?: "https://github.com/zayaanapps/zayaan-apps"
    val promoVideoUrl = getString("promoVideoUrl") ?: ""
    val offlineCapable = getBoolean("offlineCapable") ?: true
    val pageViews = getLong("pageViews") ?: 0L
    val authorUid = getString("authorUid") ?: ""
    val createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
    val updatedAtMillis = getTimestamp("updatedAt")?.toDate()?.time ?: 0L

    return StoreApp(
        docId = id,
        appId = appId,
        appName = appName,
        packageName = packageName,
        category = category,
        developerName = developerName,
        shortDescription = shortDescription,
        description = description,
        iconUrl = iconUrl,
        screenshots = screenshots,
        versionName = versionName,
        versionCode = versionCode,
        apkDownloadUrl = apkDownloadUrl,
        apkFileSize = apkFileSize,
        releaseNotes = releaseNotes,
        featured = featured,
        editorsChoice = editorsChoice,
        essential = essential,
        published = published,
        downloadCount = downloadCount,
        ratingSum = ratingSum,
        reviewCount = reviewCount,
        sha256Checksum = sha256Checksum,
        minAndroidSdk = minAndroidSdk,
        targetAndroidSdk = targetAndroidSdk,
        architecture = architecture,
        permissionsList = permissionsList,
        privacyPolicyUrl = privacyPolicyUrl,
        websiteUrl = websiteUrl,
        supportEmail = supportEmail,
        githubRepoUrl = githubRepoUrl,
        promoVideoUrl = promoVideoUrl,
        offlineCapable = offlineCapable,
        pageViews = pageViews,
        authorUid = authorUid,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}
