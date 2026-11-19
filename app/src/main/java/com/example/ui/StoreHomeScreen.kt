package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.AppCollection
import com.example.data.InstalledPackageStatus
import com.example.data.STORE_CATEGORIES
import com.example.data.SortOption
import com.example.data.StoreApp
import com.example.data.StoreNotification
import java.util.Locale

val MARKETPLACE_SUBCATEGORIES = listOf(
    "All",
    "Editor's Choice",
    "Essential Apps",
    "Offline Ready",
    "Under 15 MB",
    "Latest v1.0.0.x"
)

data class CuratedCollectionBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val targetCategory: String,
    val targetSubcategory: String
)

val CURATED_MARKETPLACE_COLLECTIONS = listOf(
    CuratedCollectionBanner(
        id = "essential-new-phone",
        title = "Essential Apps for New Phones",
        subtitle = "Must-have verified Zayaan tools, package managers & daily utilities.",
        badge = "CURATED COLLECTION",
        targetCategory = "All",
        targetSubcategory = "Essential Apps"
    ),
    CuratedCollectionBanner(
        id = "editors-choice-spotlight",
        title = "Editor's Choice Spotlight",
        subtitle = "Hand-picked flagship Android apps with verified GitHub Releases APKs.",
        badge = "EDITOR'S CHOICE",
        targetCategory = "All",
        targetSubcategory = "Editor's Choice"
    ),
    CuratedCollectionBanner(
        id = "ai-productivity-suite",
        title = "AI & Deep-Work Suite",
        subtitle = "Multimodal AI coding assistants and distraction-free productivity timers.",
        badge = "AI & WORKFLOW",
        targetCategory = "AI Apps",
        targetSubcategory = "All"
    ),
    CuratedCollectionBanner(
        id = "best-offline-ready",
        title = "Best Offline-Ready Apps",
        subtitle = "Full capability even without an active network connection.",
        badge = "OFFLINE READY",
        targetCategory = "All",
        targetSubcategory = "Offline Ready"
    )
)

@Composable
fun StoreHomeScreen(
    appsState: UiState<List<StoreApp>>,
    searchQuery: String,
    selectedCategory: String,
    selectedSubcategory: String = "All",
    selectedSortOption: SortOption = SortOption.RELEVANCE,
    searchHistory: List<String> = emptyList(),
    recentlyViewedAppIds: List<String> = emptyList(),
    favoriteAppIds: List<String> = emptyList(),
    bookmarkedAppIds: List<String> = emptyList(),
    downloadedAppIds: List<String> = emptyList(),
    notifications: List<StoreNotification> = emptyList(),
    customCollections: List<AppCollection> = emptyList(),
    installedStatuses: Map<String, InstalledPackageStatus>,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    onCategorySelect: (String) -> Unit,
    onSubcategorySelect: (String) -> Unit = {},
    onSortOptionSelect: (SortOption) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onAppClick: (StoreApp) -> Unit,
    onSeedDefaultCatalog: () -> Unit
) {
    when (appsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Syncing Zayaan Apps Marketplace...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unable to load store catalog",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = appsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is UiState.Success -> {
            val allApps = appsState.data

            val instantSuggestions by remember(allApps, searchQuery) {
                derivedStateOf {
                    val q = searchQuery.trim().lowercase()
                    if (q.isEmpty()) {
                        emptyList()
                    } else {
                        val nameMatches = allApps.map { it.appName }
                            .filter { it.lowercase().contains(q) && !it.equals(q, ignoreCase = true) }
                        val devMatches = allApps.map { it.developerName }.distinct()
                            .filter { it.lowercase().contains(q) && !it.equals(q, ignoreCase = true) }
                        val catMatches = STORE_CATEGORIES
                            .filter { it.lowercase().contains(q) && !it.equals(q, ignoreCase = true) }
                        (nameMatches + devMatches + catMatches).distinct().take(6)
                    }
                }
            }

            val filteredApps by remember(
                allApps,
                searchQuery,
                selectedCategory,
                selectedSubcategory,
                selectedSortOption
            ) {
                derivedStateOf {
                    val base = allApps.filter { app ->
                        val matchesCategory = selectedCategory == "All" ||
                            app.category.equals(selectedCategory, ignoreCase = true)

                        val matchesSubcategory = when (selectedSubcategory) {
                            "Editor's Choice" -> app.editorsChoice || app.featured
                            "Essential Apps" -> app.essential
                            "Offline Ready" -> app.offlineCapable
                            "Under 15 MB" -> parseSizeMegabytes(app.apkFileSize) <= 15.0
                            "Latest v1.0.0.x" -> app.versionName.split(".").size >= 4
                            else -> true
                        }

                        val q = searchQuery.trim().lowercase()
                        val matchesQuery = q.isEmpty() ||
                            app.appName.lowercase().contains(q) ||
                            app.shortDescription.lowercase().contains(q) ||
                            app.description.lowercase().contains(q) ||
                            app.developerName.lowercase().contains(q) ||
                            app.category.lowercase().contains(q) ||
                            app.packageName.lowercase().contains(q) ||
                            isTypoTolerantMatch(app.appName.lowercase(), q)

                        matchesCategory && matchesSubcategory && matchesQuery
                    }

                    when (selectedSortOption) {
                        SortOption.RELEVANCE -> base.sortedWith(
                            compareByDescending<StoreApp> { it.featured }
                                .thenByDescending { it.editorsChoice }
                                .thenByDescending { it.downloadCount }
                        )
                        SortOption.POPULARITY -> base.sortedByDescending { it.downloadCount }
                        SortOption.HIGHEST_RATED -> base.sortedWith(
                            compareByDescending<StoreApp> { it.averageRating }
                                .thenByDescending { it.downloadCount }
                        )
                        SortOption.RECENTLY_UPDATED -> base.sortedWith(
                            compareByDescending<StoreApp> { it.updatedAtMillis }
                                .thenByDescending { it.versionCode }
                        )
                        SortOption.RECENTLY_ADDED -> base.sortedWith(
                            compareByDescending<StoreApp> { it.createdAtMillis }
                                .thenByDescending { it.versionCode }
                        )
                        SortOption.ALPHABETICAL -> base.sortedBy { it.appName.lowercase() }
                        SortOption.FILE_SIZE -> base.sortedBy { parseSizeMegabytes(it.apkFileSize) }
                    }
                }
            }

            val featuredApps = remember(allApps) {
                allApps.filter { it.featured }.ifEmpty { allApps.take(3) }
            }
            val trendingApps = remember(allApps) {
                allApps.sortedByDescending { it.downloadCount }.take(6)
            }
            val editorsChoiceApps = remember(allApps) {
                allApps.filter { it.editorsChoice || it.essential }.ifEmpty { allApps.take(4) }
            }
            val recentlyUpdatedApps = remember(allApps) {
                allApps.sortedWith(
                    compareByDescending<StoreApp> { it.updatedAtMillis }
                        .thenByDescending { it.versionCode }
                ).take(6)
            }
            val newReleaseApps = remember(allApps) {
                allApps.sortedWith(
                    compareByDescending<StoreApp> { it.createdAtMillis }
                        .thenByDescending { it.downloadCount }
                ).take(5)
            }
            val recentlyViewedApps = remember(allApps, recentlyViewedAppIds) {
                recentlyViewedAppIds.mapNotNull { id ->
                    allApps.firstOrNull { (it.appId.ifBlank { it.docId }) == id }
                }
            }
            val personalizedRecommendedApps = remember(
                allApps,
                favoriteAppIds,
                bookmarkedAppIds,
                downloadedAppIds,
                recentlyViewedAppIds
            ) {
                val interactedIds = (favoriteAppIds + bookmarkedAppIds + downloadedAppIds + recentlyViewedAppIds).toSet()
                val preferredCategories = allApps
                    .filter { (it.appId.ifBlank { it.docId }) in interactedIds }
                    .map { it.category }
                    .toSet()
                val candidateApps = if (preferredCategories.isNotEmpty()) {
                    allApps.filter { it.category in preferredCategories }
                } else {
                    allApps.filter { it.featured || it.editorsChoice || it.essential }
                }
                candidateApps.sortedWith(
                    compareByDescending<StoreApp> { it.averageRating }
                        .thenByDescending { it.downloadCount }
                ).take(6)
            }

            val isDefaultDiscoveryView = searchQuery.isBlank() &&
                selectedCategory == "All" &&
                selectedSubcategory == "All" &&
                selectedSortOption == SortOption.RELEVANCE

            val categories = remember { listOf("All") + STORE_CATEGORIES }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("store_home_list"),
                contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Minimal & Compact Theme-Adaptive Header
                item {
                    val liveStoreVersion = allApps
                        .firstOrNull { it.packageName == "com.zayaanapps.store" || it.appId == "zayaan-apps-store" }
                        ?.versionName
                        ?.takeIf { it.isNotBlank() }
                        ?: BuildConfig.VERSION_NAME
                    StoreHeroBanner(
                        totalAppsCount = allApps.size,
                        totalDownloads = allApps.sumOf { it.downloadCount },
                        storeVersionName = liveStoreVersion
                    )
                }

                // 2. Compact Search Bar + Instant Suggestions + Search History
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        CompactStoreSearchBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            onSearchSubmit = onSearchSubmit
                        )

                        // Instant Search Suggestions
                        if (instantSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Suggestions",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(instantSuggestions) { suggestion ->
                                        CompactFilterPill(
                                            label = suggestion,
                                            selected = false,
                                            icon = Icons.Default.Search,
                                            onClick = {
                                                onSearchQueryChange(suggestion)
                                                onSearchSubmit(suggestion)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Recent Search History
                        if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Clear",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(onClick = onClearSearchHistory)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("clear_search_history_button")
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(searchHistory) { historyItem ->
                                    CompactFilterPill(
                                        label = historyItem,
                                        selected = false,
                                        icon = Icons.Default.History,
                                        onClick = { onSearchQueryChange(historyItem) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Minimal & Compact Category, Subcategory & Sort Filter Rows
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = category.equals(selectedCategory, ignoreCase = true)
                                CompactFilterPill(
                                    label = category,
                                    selected = isSelected,
                                    icon = categoryIconFor(category),
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                                    modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}"),
                                    onClick = { onCategorySelect(category) }
                                )
                            }
                        }

                        // Subcategories & Collection Filters
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(MARKETPLACE_SUBCATEGORIES) { subcat ->
                                val isSelected = subcat.equals(selectedSubcategory, ignoreCase = true)
                                CompactFilterPill(
                                    label = subcat,
                                    selected = isSelected,
                                    icon = subcategoryIconFor(subcat),
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.50f),
                                    modifier = Modifier.testTag("subcat_chip_${subcat.lowercase().replace(" ", "_")}"),
                                    onClick = { onSubcategorySelect(subcat) }
                                )
                            }
                        }

                        // Sort Options Bar
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(9.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = "Sort by",
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Sort",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            items(SortOption.entries) { option ->
                                val isSelected = option == selectedSortOption
                                CompactFilterPill(
                                    label = option.label,
                                    selected = isSelected,
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    selectedBorderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.50f),
                                    modifier = Modifier.testTag("sort_chip_${option.name.lowercase()}"),
                                    onClick = { onSortOptionSelect(option) }
                                )
                            }
                        }
                    }
                }

                // 4. Discovery Sections (shown on default Home discovery view)
                if (isDefaultDiscoveryView && allApps.isNotEmpty()) {
                    // Featured Applications Spotlight Carousel
                    if (featuredApps.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeaderRow(
                                    icon = Icons.Default.Star,
                                    iconTint = MaterialTheme.colorScheme.tertiary,
                                    title = "Featured Applications",
                                    subtitle = "Official flagship releases hosted on GitHub Releases"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(featuredApps, key = { "feat_${it.appId.ifBlank { it.docId }}" }) { app ->
                                        FeaturedAppSpotlightCard(
                                            app = app,
                                            installedStatus = installedStatuses[app.packageName],
                                            onClick = { onAppClick(app) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Store Release & Update Announcements Feed (Phase 2)
                    if (notifications.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeaderRow(
                                    icon = Icons.Default.NewReleases,
                                    iconTint = MaterialTheme.colorScheme.secondary,
                                    title = "Release & Update Announcements",
                                    subtitle = "Live store notifications for new releases & package updates"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(notifications.take(6), key = { "notif_${it.id}" }) { notif ->
                                        Card(
                                            modifier = Modifier
                                                .width(256.dp)
                                                .clickable {
                                                    val target = allApps.firstOrNull {
                                                        (it.appId.ifBlank { it.docId }) == notif.targetAppId
                                                    }
                                                    if (target != null) onAppClick(target)
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f)
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                                    shape = RoundedCornerShape(7.dp),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Campaign,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = notif.category,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = notif.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = notif.body,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Personalized Recommended for You Section (Phase 2)
                    if (personalizedRecommendedApps.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeaderRow(
                                    icon = Icons.Default.AutoAwesome,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "Recommended for You",
                                    subtitle = "Personalized based on your library, favorites & download history"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(personalizedRecommendedApps, key = { "pers_${it.appId.ifBlank { it.docId }}" }) { app ->
                                        CompactMarketplaceAppCard(
                                            app = app,
                                            installedStatus = installedStatuses[app.packageName],
                                            badgeText = "For You • ${app.category}",
                                            onClick = { onAppClick(app) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Featured Collections & Promotional Banners
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeaderRow(
                                icon = Icons.Default.AutoAwesome,
                                iconTint = MaterialTheme.colorScheme.primary,
                                title = "Featured Collections",
                                subtitle = "Curated Android bundles for productivity, AI & daily use"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(customCollections, key = { "dyn_col_${it.id}" }) { dynCol ->
                                    CuratedCollectionCard(
                                        collection = CuratedCollectionBanner(
                                            id = dynCol.id,
                                            title = dynCol.title,
                                            subtitle = dynCol.subtitle.ifBlank { "${dynCol.appIds.size} curated Zayaan applications" },
                                            badge = if (dynCol.featured) "ADMIN FEATURED" else "COLLECTION",
                                            targetCategory = "All",
                                            targetSubcategory = "All"
                                        ),
                                        onClick = {
                                            val firstApp = allApps.firstOrNull {
                                                (it.appId.ifBlank { it.docId }) in dynCol.appIds
                                            }
                                            if (firstApp != null) onAppClick(firstApp)
                                        }
                                    )
                                }
                                items(CURATED_MARKETPLACE_COLLECTIONS, key = { it.id }) { collection ->
                                    CuratedCollectionCard(
                                        collection = collection,
                                        onClick = {
                                            onCategorySelect(collection.targetCategory)
                                            onSubcategorySelect(collection.targetSubcategory)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Trending & Most Popular Apps Carousel
                    if (trendingApps.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeaderRow(
                                    icon = Icons.Default.TrendingUp,
                                    iconTint = MaterialTheme.colorScheme.secondary,
                                    title = "Trending & Most Popular",
                                    subtitle = "Ranked by verified community APK downloads"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(trendingApps, key = { "trend_${it.appId.ifBlank { it.docId }}" }) { app ->
                                        CompactMarketplaceAppCard(
                                            app = app,
                                            installedStatus = installedStatuses[app.packageName],
                                            badgeText = "${app.downloadCount} downloads",
                                            onClick = { onAppClick(app) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Editor's Choice & Essential Apps Carousel
                    if (editorsChoiceApps.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeaderRow(
                                    icon = Icons.Default.Verified,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "Editor's Choice & Essential",
                                    subtitle = "Verified architecture, clean Material 3 UI & zero ads"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(editorsChoiceApps, key = { "ed_${it.appId.ifBlank { it.docId }}" }) { app ->
                                        CompactMarketplaceAppCard(
                                            app = app,
                                            installedStatus = installedStatuses[app.packageName],
                                            badgeText = if (app.editorsChoice) "Editor's Choice" else "Essential",
                                            onClick = { onAppClick(app) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // New Releases & Recently Updated Carousel
                    if (recentlyUpdatedApps.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeaderRow(
                                    icon = Icons.Default.NewReleases,
                                    iconTint = MaterialTheme.colorScheme.tertiary,
                                    title = "New Releases & Recently Updated",
                                    subtitle = "Continuous 1.0.0.x builds deployed via GitHub Actions"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recentlyUpdatedApps, key = { "upd_${it.appId.ifBlank { it.docId }}" }) { app ->
                                        CompactMarketplaceAppCard(
                                            app = app,
                                            installedStatus = installedStatuses[app.packageName],
                                            badgeText = "v${app.versionName}",
                                            onClick = { onAppClick(app) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recently Viewed by User (if any)
                    if (recentlyViewedApps.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeaderRow(
                                    icon = Icons.Default.History,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "Recently Viewed by You",
                                    subtitle = "Jump right back into applications you explored"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recentlyViewedApps, key = { "rec_${it.appId.ifBlank { it.docId }}" }) { app ->
                                        CompactMarketplaceAppCard(
                                            app = app,
                                            installedStatus = installedStatuses[app.packageName],
                                            badgeText = app.category,
                                            onClick = { onAppClick(app) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Full Catalog Header & Active Filter Summary
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (selectedCategory == "All") {
                                            "All Applications (${filteredApps.size})"
                                        } else {
                                            "$selectedCategory (${filteredApps.size})"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$selectedSubcategory • ${selectedSortOption.label}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (!isDefaultDiscoveryView) {
                                OutlinedButton(
                                    onClick = onResetFilters,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("reset_filters_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }

                if (filteredApps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No matching apps found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Adjust your search or filter, or load the starter catalog.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (!isDefaultDiscoveryView) {
                                        OutlinedButton(onClick = onResetFilters) {
                                            Text("Clear Filters")
                                        }
                                    }
                                    Button(
                                        onClick = onSeedDefaultCatalog,
                                        modifier = Modifier.testTag("seed_default_catalog_button")
                                    ) {
                                        Text("Load Starter Zayaan Catalog")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredApps, key = { it.appId.ifBlank { it.docId } }) { app ->
                        StoreAppListRowCard(
                            app = app,
                            installedStatus = installedStatuses[app.packageName],
                            onClick = { onAppClick(app) }
                        )
                    }
                }
            }
        }
    }
}

private fun categoryIconFor(category: String): ImageVector = when (category) {
    "All" -> Icons.Default.Apps
    "Games" -> Icons.Default.SportsEsports
    "Tools" -> Icons.Default.Build
    "AI Apps" -> Icons.Default.AutoAwesome
    "Entertainment" -> Icons.Default.Movie
    "Productivity" -> Icons.Default.WorkOutline
    "Education" -> Icons.Default.School
    "Utilities" -> Icons.Default.Build
    "Business" -> Icons.Default.Storefront
    "Social & Communication" -> Icons.Default.Forum
    else -> Icons.Default.Category
}

private fun subcategoryIconFor(subcat: String): ImageVector = when (subcat) {
    "Editor's Choice" -> Icons.Default.WorkspacePremium
    "Essential Apps" -> Icons.Default.Verified
    "Offline Ready" -> Icons.Default.OfflineBolt
    "Under 15 MB" -> Icons.Default.SdStorage
    "Latest v1.0.0.x" -> Icons.Default.NewReleases
    else -> Icons.Default.Apps
}

@Composable
private fun SectionHeaderRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = iconTint.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, iconTint.copy(alpha = 0.30f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .padding(6.dp)
                    .size(17.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactStoreSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        color = cs.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFocused) cs.primary.copy(alpha = 0.78f) else cs.outline.copy(alpha = 0.30f)
        ),
        shadowElevation = if (isFocused) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search applications",
                tint = if (isFocused) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search apps, packages, developers...",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        color = cs.onSurface
                    ),
                    cursorBrush = SolidColor(cs.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(searchQuery) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag("store_search_input")
                )
            }
            if (searchQuery.isNotEmpty()) {
                Surface(
                    color = cs.primaryContainer,
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onSearchSubmit(searchQuery) }
                        .testTag("search_submit_button")
                ) {
                    Text(
                        text = "Save",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onSearchQueryChange("") }
                        .testTag("clear_search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search query",
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactFilterPill(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedBorderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = if (selected) selectedContainerColor else cs.surface,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) selectedBorderColor else cs.outline.copy(alpha = 0.26f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) selectedContentColor else cs.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) selectedContentColor else cs.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun StoreHeroBanner(
    totalAppsCount: Int,
    totalDownloads: Long,
    storeVersionName: String = BuildConfig.VERSION_NAME
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val cardBorderColor = if (isDark) {
        Color(0xFF6EDBA9).copy(alpha = 0.36f)
    } else {
        cs.primary.copy(alpha = 0.24f)
    }
    val gradientColors = if (isDark) {
        listOf(
            Color(0xEC071224),
            Color(0xD60C2235),
            Color(0xBA063327)
        )
    } else {
        listOf(
            Color(0xF5EBF8F2),
            Color(0xF2F3FAF7),
            Color(0xEFEAF3FB)
        )
    }
    val titleColor = if (isDark) Color.White else cs.onSurface
    val subtitleColor = if (isDark) Color(0xFFC9DAE8) else cs.onSurfaceVariant
    val verifiedTint = if (isDark) Color(0xFF6EDBA9) else cs.primary
    val versionChipBg = if (isDark) Color.White.copy(alpha = 0.12f) else cs.surface.copy(alpha = 0.92f)
    val versionChipBorder = if (isDark) Color(0xFF6EDBA9).copy(alpha = 0.35f) else cs.primary.copy(alpha = 0.25f)
    val versionTextColor = if (isDark) Color.White else cs.onSurface
    val metricsBg = if (isDark) Color.Black.copy(alpha = 0.26f) else cs.surface.copy(alpha = 0.88f)
    val metricsBorder = if (isDark) Color.White.copy(alpha = 0.16f) else cs.outline.copy(alpha = 0.20f)
    val metricTextColor = if (isDark) Color.White else cs.onSurface
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.20f) else cs.outline.copy(alpha = 0.20f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_store_hero_banner_1795021164708),
                contentDescription = "Zayaan Apps Store Banner",
                contentScale = ContentScale.Crop,
                alpha = if (isDark) 0.45f else 0.08f,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = gradientColors))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                                contentDescription = "Zayaan Apps Icon",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .border(1.dp, cardBorderColor, RoundedCornerShape(9.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Zayaan Apps",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = titleColor,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified GitHub Releases",
                                        tint = verifiedTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "Verified APKs • GitHub Releases & Cloud",
                                    fontSize = 11.sp,
                                    color = subtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = versionChipBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, versionChipBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF67E8F9) else cs.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "v$storeVersionName",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = versionTextColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Minimal bordered 3-metric telemetry strip
                    Surface(
                        color = metricsBg,
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, metricsBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroMetricPill(
                                icon = Icons.Default.Apps,
                                label = "$totalAppsCount Apps",
                                tint = if (isDark) Color(0xFF6EDBA9) else cs.primary,
                                textColor = metricTextColor
                            )
                            VerticalDivider(
                                modifier = Modifier.height(11.dp),
                                thickness = 1.dp,
                                color = dividerColor
                            )
                            HeroMetricPill(
                                icon = Icons.Default.Download,
                                label = "$totalDownloads DLs",
                                tint = if (isDark) Color(0xFF68D3FF) else cs.secondary,
                                textColor = metricTextColor
                            )
                            VerticalDivider(
                                modifier = Modifier.height(11.dp),
                                thickness = 1.dp,
                                color = dividerColor
                            )
                            HeroMetricPill(
                                icon = Icons.Default.Public,
                                label = "Android & Web",
                                tint = if (isDark) Color(0xFFADC6FF) else cs.tertiary,
                                textColor = metricTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    textColor: Color = Color.White
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun CuratedCollectionCard(
    collection: CuratedCollectionBanner,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(246.dp)
            .clickable(onClick = onClick)
            .testTag("collection_card_${collection.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = collection.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = collection.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = collection.subtitle,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeaturedAppSpotlightCard(
    app: StoreApp,
    installedStatus: InstalledPackageStatus?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(272.dp)
            .clickable(onClick = onClick)
            .testTag("featured_card_${app.appId.ifBlank { app.docId }}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val iconModel: Any = if (
                    app.packageName == "com.zayaanapps.store" &&
                    (app.iconUrl.isBlank() || app.iconUrl.contains("/web/icons/Icon-192.png"))
                ) {
                    R.drawable.img_launcher_icon_1795021165006
                } else {
                    app.iconUrl
                }
                AsyncImage(
                    model = iconModel,
                    contentDescription = "${app.appName} icon",
                    placeholder = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                    error = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(13.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.editorsChoice) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = app.developerName,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = categoryIconFor(app.category),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = app.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                        if (app.editorsChoice) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "Editor's",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = app.shortDescription.ifBlank { app.description },
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InlineHomeMetric(
                            icon = Icons.Default.NewReleases,
                            value = "v${app.versionName}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        InlineHomeMetric(
                            icon = Icons.Default.SdStorage,
                            value = app.apkFileSize,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        InlineHomeMetric(
                            icon = Icons.Default.Download,
                            value = "${app.downloadCount}",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    PackageInstallStatusBadge(installedStatus = installedStatus)
                }
            }
        }
    }
}

@Composable
private fun CompactMarketplaceAppCard(
    app: StoreApp,
    installedStatus: InstalledPackageStatus?,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick)
            .testTag("compact_app_card_${app.appId.ifBlank { app.docId }}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                val iconModel: Any = if (
                    app.packageName == "com.zayaanapps.store" &&
                    (app.iconUrl.isBlank() || app.iconUrl.contains("/web/icons/Icon-192.png"))
                ) {
                    R.drawable.img_launcher_icon_1795021165006
                } else {
                    app.iconUrl
                }
                AsyncImage(
                    model = iconModel,
                    contentDescription = "${app.appName} icon",
                    placeholder = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                    error = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.26f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                PackageInstallStatusBadge(installedStatus = installedStatus)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.developerName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(7.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    )
                    InlineHomeMetric(
                        icon = Icons.Default.SdStorage,
                        value = app.apkFileSize,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StoreAppListRowCard(
    app: StoreApp,
    installedStatus: InstalledPackageStatus?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
            .testTag("app_row_card_${app.appId.ifBlank { app.docId }}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModel: Any = if (
                app.packageName == "com.zayaanapps.store" &&
                (app.iconUrl.isBlank() || app.iconUrl.contains("/web/icons/Icon-192.png"))
            ) {
                R.drawable.img_launcher_icon_1795021165006
            } else {
                app.iconUrl
            }
            AsyncImage(
                model = iconModel,
                contentDescription = "${app.appName} icon",
                placeholder = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                error = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(13.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.editorsChoice) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Editor's Choice",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (app.offlineCapable) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "Offline Ready",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "${app.developerName} • ${app.category}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.shortDescription.isNotBlank()) {
                    Text(
                        text = app.shortDescription,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InlineHomeMetric(
                        icon = Icons.Default.NewReleases,
                        value = "v${app.versionName}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    InlineHomeMetric(
                        icon = Icons.Default.SdStorage,
                        value = app.apkFileSize,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    InlineHomeMetric(
                        icon = Icons.Default.Download,
                        value = "${app.downloadCount}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (app.reviewCount > 0) {
                        InlineHomeMetric(
                            icon = Icons.Default.Star,
                            value = String.format(Locale.US, "%.1f", app.averageRating),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            PackageInstallStatusBadge(installedStatus = installedStatus)
        }
    }
}

@Composable
private fun InlineHomeMetric(
    icon: ImageVector,
    value: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = value,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun PackageInstallStatusBadge(installedStatus: InstalledPackageStatus?) {
    when {
        installedStatus == null || !installedStatus.isInstalled -> {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Get APK",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Get APK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        installedStatus.updateAvailable -> {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update available",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Update",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        else -> {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Installed",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Installed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

private fun parseSizeMegabytes(sizeText: String): Double {
    val match = Regex("([0-9]+(?:\\.[0-9]+)?)").find(sizeText)
    return match?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 15.0
}

private fun isTypoTolerantMatch(target: String, query: String): Boolean {
    if (query.length < 4) return false
    val words = target.split(" ", "-", "_")
    return words.any { word ->
        if (kotlin.math.abs(word.length - query.length) > 1) return@any false
        var mismatches = 0
        val minLen = minOf(word.length, query.length)
        for (i in 0 until minLen) {
            if (word[i] != query[i]) mismatches++
            if (mismatches > 1) return@any false
        }
        true
    }
}
