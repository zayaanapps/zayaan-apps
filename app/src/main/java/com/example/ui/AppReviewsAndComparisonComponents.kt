package com.example.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AppReview
import com.example.data.InstalledPackageStatus
import com.example.data.StoreApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RatingsAndReviewsSection(
    app: StoreApp,
    reviews: List<AppReview>,
    currentUserId: String,
    isAdmin: Boolean,
    onSubmitReview: (Long, String) -> Unit,
    onVoteHelpful: (AppReview) -> Unit,
    onFlagReview: (AppReview, Boolean) -> Unit,
    onAdminReply: (AppReview, String) -> Unit,
    onDeleteReview: (AppReview) -> Unit
) {
    val existingUserReview = remember(reviews, currentUserId) {
        reviews.firstOrNull { it.userId == currentUserId }
    }
    var selectedStars by remember(existingUserReview) {
        mutableLongStateOf(existingUserReview?.rating ?: 5L)
    }
    var reviewComment by remember(existingUserReview) {
        mutableStateOf(existingUserReview?.comment ?: "")
    }
    var replyingToReview by remember { mutableStateOf<AppReview?>(null) }
    var replyDraft by remember { mutableStateOf("") }

    val totalReviews = reviews.size
    val calculatedAverage = remember(reviews, app.averageRating) {
        if (reviews.isNotEmpty()) {
            reviews.map { it.rating }.average()
        } else {
            app.averageRating
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ratings_and_reviews_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ratings & Community Reviews",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Verified installs, helpful votes & developer replies",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (totalReviews > 0 || app.reviewCount > 0) {
                                "${String.format(Locale.US, "%.1f", calculatedAverage)} (${maxOf(totalReviews.toLong(), app.reviewCount)})"
                            } else {
                                "5.0 ★ New"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 5-Star Histogram Breakdown
            if (reviews.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (star in 5 downTo 1) {
                        val countForStar = reviews.count { it.rating == star.toLong() }
                        val ratio = countForStar.toFloat() / reviews.size.coerceAtLeast(1).toFloat()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$star ★",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(28.dp)
                            )
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$countForStar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(22.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Write or Update Your Review Form
            Text(
                text = if (existingUserReview != null) "Update Your Review" else "Rate & Review ${app.appName}",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (star in 1L..5L) {
                    IconButton(
                        onClick = { selectedStars = star },
                        modifier = Modifier.testTag("star_rating_button_$star")
                    ) {
                        Icon(
                            imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Rate $star stars",
                            tint = if (star <= selectedStars) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "$selectedStars / 5 Stars",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = reviewComment,
                onValueChange = { reviewComment = it },
                label = { Text("Share your experience with this APK release...") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_comment_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        onSubmitReview(selectedStars, reviewComment)
                    },
                    modifier = Modifier.testTag("submit_review_button")
                ) {
                    Text(if (existingUserReview != null) "Update Review" else "Post Review")
                }
            }

            // Community Reviews List
            if (reviews.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No written reviews yet for ${app.appName}. Be the first to rate this release!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                HorizontalDivider()
                reviews.forEach { review ->
                    ReviewItemCard(
                        review = review,
                        isOwnReview = review.userId == currentUserId,
                        isAdmin = isAdmin,
                        onVoteHelpful = { onVoteHelpful(review) },
                        onFlagToggle = { onFlagReview(review, !review.flagged) },
                        onReplyClick = {
                            replyingToReview = review
                            replyDraft = review.developerReply
                        },
                        onDeleteClick = { onDeleteReview(review) }
                    )
                }
            }
        }
    }

    if (replyingToReview != null) {
        val target = replyingToReview!!
        AlertDialog(
            onDismissRequest = { replyingToReview = null },
            title = { Text("Developer Reply to ${target.userName}") },
            text = {
                OutlinedTextField(
                    value = replyDraft,
                    onValueChange = { replyDraft = it },
                    label = { Text("Official Developer / Admin Response") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdminReply(target, replyDraft)
                        replyingToReview = null
                    }
                ) {
                    Text("Save Reply")
                }
            },
            dismissButton = {
                TextButton(onClick = { replyingToReview = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReviewItemCard(
    review: AppReview,
    isOwnReview: Boolean,
    isAdmin: Boolean,
    onVoteHelpful: () -> Unit,
    onFlagToggle: () -> Unit,
    onReplyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateStr = remember(review.updatedAt, review.createdAt) {
        val ts = review.updatedAt ?: review.createdAt
        if (ts != null) {
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(ts.toDate())
        } else {
            "Verified Review"
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (review.verifiedInstall) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Device Install",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Verified Install",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    if (review.flagged) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Flagged",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${"★".repeat(review.rating.toInt().coerceIn(1, 5))} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium
            )

            if (review.developerReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Response from Zayaan Apps Team",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = review.developerReply,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onVoteHelpful,
                    label = { Text("Helpful (${review.helpfulCount})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Vote Helpful",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.testTag("helpful_vote_chip_${review.id}")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        TextButton(onClick = onReplyClick) {
                            Text("Reply", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = onFlagToggle,
                        modifier = Modifier.testTag("flag_review_button_${review.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = if (review.flagged) "Unflag review" else "Report review for moderation",
                            tint = if (review.flagged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isOwnReview || isAdmin) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.testTag("delete_review_button_${review.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete review",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppComparisonSectionCard(
    comparedApps: List<StoreApp>,
    installedStatuses: Map<String, InstalledPackageStatus>,
    onSelectApp: (StoreApp) -> Unit,
    onRemoveFromCompare: (String) -> Unit,
    onClearAll: () -> Unit,
    onDownloadOrUpdate: (StoreApp) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_comparison_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Side-by-Side Application Comparison (${comparedApps.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Compare versions, ratings, APK sizes, SDKs & offline capability",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (comparedApps.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.testTag("clear_comparison_button")
                    ) {
                        Text("Clear")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (comparedApps.isEmpty()) {
                Text(
                    text = "Tap the 'Compare' button on any two or more application detail screens to compare their features, ratings, APK sizes, target Android SDKs, and installed versions side-by-side.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(comparedApps, key = { "cmp_${it.appId.ifBlank { it.docId }}" }) { app ->
                        val appId = app.appId.ifBlank { app.docId }
                        val status = installedStatuses[app.packageName]
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(230.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onSelectApp(app) }
                                ) {
                                    AsyncImage(
                                        model = app.iconUrl,
                                        contentDescription = "${app.appName} icon",
                                        placeholder = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                                        error = painterResource(id = R.drawable.img_launcher_icon_1795021165006),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                HorizontalDivider()

                                CompareMetricLine("Store Version", "v${app.versionName} (${app.versionCode})")
                                CompareMetricLine(
                                    "Device Status",
                                    when {
                                        status?.updateAvailable == true -> "Update (${status.installedVersionName})"
                                        status?.isInstalled == true -> "Installed (v${status.installedVersionName})"
                                        else -> "Not Installed"
                                    }
                                )
                                CompareMetricLine("APK Size", app.apkFileSize)
                                CompareMetricLine(
                                    "Rating",
                                    if (app.reviewCount > 0) {
                                        "★ ${String.format(Locale.US, "%.1f", app.averageRating)} (${app.reviewCount})"
                                    } else {
                                        "Verified Release"
                                    }
                                )
                                CompareMetricLine("Downloads", "${app.downloadCount}")
                                CompareMetricLine("Android API", "API ${app.minAndroidSdk}+ (Target ${app.targetAndroidSdk})")
                                CompareMetricLine("Offline Ready", if (app.offlineCapable) "Yes" else "Online Only")
                                CompareMetricLine("Permissions", "${app.permissionsList.size} declared")

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(
                                        onClick = { onDownloadOrUpdate(app) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (status?.updateAvailable == true) "Update" else "Get APK",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onRemoveFromCompare(appId) }
                                    ) {
                                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareMetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
