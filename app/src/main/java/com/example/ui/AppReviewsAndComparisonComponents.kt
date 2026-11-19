package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        mutableLongStateOf(5L)
    }
    var reviewComment by remember(existingUserReview) {
        mutableStateOf("")
    }
    var showEditReviewDialog by remember { mutableStateOf(false) }
    var editStars by remember(existingUserReview) {
        mutableLongStateOf(existingUserReview?.rating ?: 5L)
    }
    var editComment by remember(existingUserReview) {
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Ratings & Reviews",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Verified installs, helpful votes & replies",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (totalReviews > 0 || app.reviewCount > 0) {
                                "${String.format(Locale.US, "%.1f", calculatedAverage)} (${maxOf(totalReviews.toLong(), app.reviewCount)})"
                            } else {
                                "5.0 New"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 5-Star Histogram Breakdown
            if (reviews.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (star in 5 downTo 1) {
                            val countForStar = reviews.count { it.rating == star.toLong() }
                            val ratio = countForStar.toFloat() / reviews.size.coerceAtLeast(1).toFloat()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$star ★",
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(26.dp)
                                )
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$countForStar",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Review Submission Form (hidden once submitted, replaced by compact Edit option)
            if (existingUserReview != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submitted_review_summary_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Your review (${existingUserReview.rating}/5 ★)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (existingUserReview.comment.isNotBlank()) {
                                Text(
                                    text = existingUserReview.comment,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                editStars = existingUserReview.rating
                                editComment = existingUserReview.comment
                                showEditReviewDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_edit_review_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit review",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Edit", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rate & Review ${app.appName}",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "$selectedStars / 5 ★",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                for (star in 1L..5L) {
                                    IconButton(
                                        onClick = { selectedStars = star },
                                        modifier = Modifier.testTag("star_rating_button_$star")
                                    ) {
                                        Icon(
                                            imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Rate $star stars",
                                            tint = if (star <= selectedStars) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            label = { Text("Share your experience...", fontSize = 12.sp) },
                            minLines = 1,
                            maxLines = 3,
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
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Post Review",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Community Reviews List
            if (reviews.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "No written reviews yet. Be the first to rate this release!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        onEditClick = {
                            editStars = review.rating
                            editComment = review.comment
                            showEditReviewDialog = true
                        },
                        onDeleteClick = { onDeleteReview(review) }
                    )
                }
            }
        }
    }

    if (showEditReviewDialog && existingUserReview != null) {
        AlertDialog(
            onDismissRequest = { showEditReviewDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Edit Your Review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                        ) {
                            Text(
                                text = "$editStars / 5 ★",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1L..5L) {
                            IconButton(
                                onClick = { editStars = star },
                                modifier = Modifier.testTag("edit_star_rating_button_$star")
                            ) {
                                Icon(
                                    imageVector = if (star <= editStars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Rate $star stars",
                                    tint = if (star <= editStars) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editComment,
                        onValueChange = { editComment = it },
                        label = { Text("Update your review...") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_review_comment_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitReview(editStars, editComment)
                        showEditReviewDialog = false
                    },
                    modifier = Modifier.testTag("save_edited_review_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditReviewDialog = false },
                    modifier = Modifier.testTag("cancel_edit_review_button")
                ) {
                    Text("Cancel")
                }
            }
        )
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
    onEditClick: () -> Unit,
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
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
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
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
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
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
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            if (review.developerReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
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
                    if (isOwnReview) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.testTag("edit_review_icon_button_${review.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit review",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_comparison_section"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.28f)),
        colors = CardDefaults.cardColors(containerColor = cs.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = cs.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Side-by-Side Comparison (${comparedApps.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Versions, ratings, APK sizes & target SDKs",
                            fontSize = 11.sp,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (comparedApps.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .clickable(onClick = onClearAll)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("clear_comparison_button")
                    )
                }
            }

            if (comparedApps.isEmpty()) {
                Surface(
                    color = cs.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.20f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tap Compare on any app details screen to compare versions, sizes, and SDK specs side-by-side.",
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(comparedApps, key = { "cmp_${it.appId.ifBlank { it.docId }}" }) { app ->
                        val appId = app.appId.ifBlank { app.docId }
                        val status = installedStatuses[app.packageName]
                        Surface(
                            color = cs.surfaceVariant.copy(alpha = 0.38f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.26f)),
                            modifier = Modifier.width(214.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(11.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onSelectApp(app) }
                                ) {
                                    val iconModel: Any = if (app.packageName == "com.zayaanapps.store") {
                                        R.drawable.zayaan_logo_badge
                                    } else {
                                        app.iconUrl
                                    }
                                    AsyncImage(
                                        model = iconModel,
                                        contentDescription = "${app.appName} icon",
                                        placeholder = painterResource(id = R.drawable.zayaan_logo_badge),
                                        error = painterResource(id = R.drawable.zayaan_logo_badge),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                1.dp,
                                                cs.outline.copy(alpha = 0.25f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .background(cs.surface),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = app.category,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = cs.primary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                HorizontalDivider(color = cs.outline.copy(alpha = 0.20f))

                                CompareMetricLine("Version", "v${app.versionName} (${app.versionCode})", Icons.Default.NewReleases)
                                CompareMetricLine(
                                    "Status",
                                    when {
                                        status?.updateAvailable == true -> "Update (${status.installedVersionName})"
                                        status?.isInstalled == true -> "Installed v${status.installedVersionName}"
                                        else -> "Not Installed"
                                    },
                                    Icons.Default.Verified
                                )
                                CompareMetricLine("APK Size", app.apkFileSize, Icons.Default.SdStorage)
                                CompareMetricLine(
                                    "Rating",
                                    if (app.reviewCount > 0) {
                                        "${String.format(Locale.US, "%.1f", app.averageRating)} (${app.reviewCount})"
                                    } else {
                                        "Verified"
                                    },
                                    Icons.Default.Star
                                )
                                CompareMetricLine("Downloads", "${app.downloadCount}", Icons.Default.Download)
                                CompareMetricLine("Android", "API ${app.minAndroidSdk}+ → ${app.targetAndroidSdk}", Icons.Default.PhoneAndroid)
                                CompareMetricLine("Offline", if (app.offlineCapable) "Ready" else "Online", Icons.Default.OfflineBolt)
                                CompareMetricLine("Permissions", "${app.permissionsList.size} declared", Icons.Default.Security)

                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(
                                        onClick = { onDownloadOrUpdate(app) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(9.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                    ) {
                                        Text(
                                            text = if (status?.updateAvailable == true) "Update" else "Get APK",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onRemoveFromCompare(appId) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(9.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Remove", fontSize = 11.sp)
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
private fun CompareMetricLine(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
