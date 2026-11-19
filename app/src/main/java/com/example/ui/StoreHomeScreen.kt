package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                contentPadding = PaddingValues(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Promotional Hero Banner
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

                // 2. Advanced Search Bar + Instant Suggestions + Search History
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("store_search_input"),
                            placeholder = {
                                Text("Search apps, developers, categories, keywords...")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search applications"
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = { onSearchSubmit(searchQuery) },
                                            modifier = Modifier.testTag("search_submit_button")
                                        ) {
                                            Text("Save")
                                        }
                                        IconButton(
                                            onClick = { onSearchQueryChange("") },
                                            modifier = Modifier.testTag("clear_search_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search query"
                                            )
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Instant Search Suggestions
                        if (instantSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Instant Suggestions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(instantSuggestions) { suggestion ->
                                    AssistChip(
                                        onClick = {
                                            onSearchQueryChange(suggestion)
                                            onSearchSubmit(suggestion)
                                        },
                                        label = { Text(suggestion) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Recent Search History + Popular Searches
                        if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = onClearSearchHistory,
                                    modifier = Modifier.testTag("clear_search_history_button")
                                ) {
                                    Text("Clear History", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchHistory) { historyItem ->
                                    AssistChip(
                                        onClick = { onSearchQueryChange(historyItem) },
                                        label = { Text(historyItem) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Primary Category Filter Bar + Subcategory & Sort Bar
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = category.equals(selectedCategory, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onCategorySelect(category) },
                                    label = { Text(category) },
                                    modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        // Subcategories & Collection Filters
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(MARKETPLACE_SUBCATEGORIES) { subcat ->
                                val isSelected = subcat.equals(selectedSubcategory, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSubcategorySelect(subcat) },
                                    label = { Text(subcat) },
                                    modifier = Modifier.testTag("subcat_chip_${subcat.lowercase().replace(" ", "_")}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }

                        // Sort Options Bar
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort by",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sort:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(SortOption.entries) { option ->
                                val isSelected = option == selectedSortOption
                                AssistChip(
                                    onClick = { onSortOptionSelect(option) },
                                    label = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("sort_chip_${option.name.lowercase()}"),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        labelColor = if (isSelected) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
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
                                                .width(270.dp)
                                                .clickable {
                                                    val target = allApps.firstOrNull {
                                                        (it.appId.ifBlank { it.docId }) == notif.targetAppId
                                                    }
                                                    if (target != null) onAppClick(target)
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = notif.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = notif.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = notif.body,
                                                    style = MaterialTheme.typography.bodySmall,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedCategory == "All") {
                                    "All Applications (${filteredApps.size})"
                                } else {
                                    "$selectedCategory (${filteredApps.size})"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Subcategory: $selectedSubcategory • Sorted by ${selectedSortOption.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isDefaultDiscoveryView) {
                            OutlinedButton(
                                onClick = onResetFilters,
                                modifier = Modifier.testTag("reset_filters_button")
                            ) {
                                Text("Reset Filters")
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
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No matching apps found",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try adjusting your search query, category, subcategory, or seed the official Zayaan Apps starter catalog.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Composable
private fun SectionHeaderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StoreHeroBanner(
    totalAppsCount: Int,
    totalDownloads: Long,
    storeVersionName: String = BuildConfig.VERSION_NAME
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(176.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_store_hero_banner_1795021164708),
                contentDescription = "Zayaan Apps Store Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEE070B14),
                                Color(0xBB0F172A),
                                Color(0x550F172A)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "GITHUB RELEASES MARKETPLACE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF67E8F9),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "v$storeVersionName",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Zayaan Apps",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "$totalAppsCount Verified Android Packages • $totalDownloads Total Downloads • Android & Web Parity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun CuratedCollectionCard(
    collection: CuratedCollectionBanner,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .width(265.dp)
            .clickable(onClick = onClick)
            .testTag("collection_card_${collection.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = collection.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = collection.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = collection.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
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
    ElevatedCard(
        modifier = Modifier
            .width(288.dp)
            .clickable(onClick = onClick)
            .testTag("featured_card_${app.appId.ifBlank { app.docId }}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.developerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = app.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (app.editorsChoice) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Editor's Choice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = app.shortDescription.ifBlank { app.description },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "v${app.versionName} • ${app.apkFileSize} • ${app.downloadCount} DLs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                PackageInstallStatusBadge(installedStatus = installedStatus)
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
            .width(176.dp)
            .clickable(onClick = onClick)
            .testTag("compact_app_card_${app.appId.ifBlank { app.docId }}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                PackageInstallStatusBadge(installedStatus = installedStatus)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.developerName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                )
                Text(
                    text = app.apkFileSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.editorsChoice) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Editor's Choice",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (app.offlineCapable) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = "Offline Ready",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${app.developerName} • ${app.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.shortDescription.isNotBlank()) {
                    Text(
                        text = app.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "v${app.versionName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "• ${app.apkFileSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "• ${app.downloadCount} DLs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.reviewCount > 0) {
                        Text(
                            text = "• ★ ${String.format(Locale.US, "%.1f", app.averageRating)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            softWrap = false
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
fun PackageInstallStatusBadge(installedStatus: InstalledPackageStatus?) {
    when {
        installedStatus == null || !installedStatus.isInstalled -> {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Get APK",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Get APK",
                        style = MaterialTheme.typography.labelMedium,
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
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update available",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Update",
                        style = MaterialTheme.typography.labelMedium,
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
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Installed",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Installed",
                        style = MaterialTheme.typography.labelMedium,
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
