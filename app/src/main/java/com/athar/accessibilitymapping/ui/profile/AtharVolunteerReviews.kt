package com.athar.accessibilitymapping.ui.profile

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ATHAR - Volunteer Reviews Section (Jetpack Compose)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * A pixel-perfect Kotlin translation of the web VolunteerReviewsSection.tsx.
 * Displays on the volunteer's profile page:
 *   • Overall rating card with average score, star distribution bars
 *   • Individual user reviews with initials-based avatars
 *   • "Show All / Show Less" toggle
 *   • Reported issues summary (visible only to the volunteer themselves)
 *
 * Self-contained — all colors, data models, mock data, and composables
 * are included in this single file.
 *
 * ── PLACEMENT ──
 *   app/src/main/java/com/athar/accessibilitymapping/ui/profile/AtharVolunteerReviews.kt
 *
 * ── USAGE ──
 *   AtharVolunteerReviewsSection(
 *       reviews = listOf(...),         // or use built-in mock data
 *       isOwnProfile = true,           // shows reported issues section
 *   )
 */

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide

// ═══════════════════════════════════════════════════════════════════════════
// ATHAR COLOR PALETTE
// ═══════════════════════════════════════════════════════════════════════════

private object AtharColors {
    val Primary = Color(0xFFEAF2FB)
    val Secondary = Color(0xFF1F3C5B)
    val SecondaryDark = Color(0xFF2C4F73)
    val Accent = Color(0xFFC9A24D)
    val AccentLight = Color(0xFFD9B76D)
    val AccentDark = Color(0xFFB38F3D)
    val TextLight = Color(0xFF5B7A99)
    val TextSecondary = Color(0xFF2C4F73)
    val WarningBg = Color(0xFFFFF8E6)
    val Gray100 = Color(0xFFF1F5F9)
    val Gray200 = Color(0xFFE2E8F0)
    val White = Color.White
}

// ═══════════════════════════════════════════════════════════════════════════
// DATA MODELS
// ═══════════════════════════════════════════════════════════════════════════

data class VolunteerReview(
    val id: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val date: String,
    val issues: List<String> = emptyList(),
)

private data class ReportedIssueSummary(
    val issue: String,
    val count: Int,
)

// ═══════════════════════════════════════════════════════════════════════════
// MOCK DATA
// ═══════════════════════════════════════════════════════════════════════════

private val mockReviews = listOf(
    VolunteerReview("rev1", "Layla Abdullah", 5, "Sara was incredibly patient and helpful. She guided me through the entire mall with ease.", "Feb 28, 2026"),
    VolunteerReview("rev2", "Ahmed Al-Rashid", 5, "Excellent assistance! Very professional and kind.", "Feb 25, 2026"),
    VolunteerReview("rev3", "Fatima Hassan", 4, "Very helpful with finding the braille section. Would request again.", "Feb 20, 2026"),
    VolunteerReview("rev4", "Omar Khalil", 4, "Good communication and timely arrival.", "Feb 15, 2026", listOf("Route issue")),
    VolunteerReview("rev5", "Nora Said", 5, "Amazing support. Sara went above and beyond to help me at the medical center.", "Feb 10, 2026"),
    VolunteerReview("rev6", "Yusuf Ali", 3, "Service was okay, arrived a bit late.", "Feb 5, 2026", listOf("Late arrival")),
    VolunteerReview("rev7", "Sara Khan", 5, "Couldn't have navigated the garden paths without her help!", "Jan 30, 2026"),
    VolunteerReview("rev8", "Hassan Mostafa", 4, "Professional and caring. Highly recommended.", "Jan 25, 2026", listOf("Communication problem")),
)

private val reportedIssuesSummary = listOf(
    ReportedIssueSummary("Late arrival", 2),
    ReportedIssueSummary("Communication problem", 1),
    ReportedIssueSummary("Route issue", 1),
    ReportedIssueSummary("Accessibility needs not met", 1),
)

private val issueIconMap: Map<String, ImageVector> = mapOf(
    "Late arrival" to Lucide.Clock,
    "Communication problem" to Lucide.Ear,
    "Route issue" to Lucide.Route,
    "Unprofessional behavior" to Lucide.Frown,
    "Safety concern" to Lucide.ShieldAlert,
    "Wrong location" to Lucide.MapPinOff,
    "Accessibility needs not met" to Lucide.Ban,
    "Uncomfortable experience" to Lucide.TriangleAlert,
    "Other" to Lucide.CircleQuestionMark,
)

private val avatarColors = listOf(
    Color(0xFF1F3C5B),
    Color(0xFF2C4F73),
    Color(0xFFC9A24D),
    Color(0xFFB38F3D),
    Color(0xFF5B7A99),
    Color(0xFF334155),
    Color(0xFF475569),
    Color(0xFF059669),
)

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AtharVolunteerReviewsSection(
    reviews: List<VolunteerReview> = mockReviews,
    isOwnProfile: Boolean = true,
) {
    var showAllReviews by remember { mutableStateOf(false) }
    val avgRating = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
    val avgRatingStr = "%.1f".format(avgRating)
    val reportedIssuesSummary = remember(reviews) { buildReportedIssuesSummary(reviews) }
    val displayedReviews = if (showAllReviews) reviews else reviews.take(3)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Overall Rating Card ──
        OverallRatingCard(reviews = reviews, avgRating = avgRating, avgRatingStr = avgRatingStr)

        // ── Individual Reviews ──
        IndividualReviewsCard(
            reviews = reviews,
            displayedReviews = displayedReviews,
            showAllReviews = showAllReviews,
            onToggle = { showAllReviews = !showAllReviews },
        )

        // ── Reported Issues (only for own profile) ──
        if (isOwnProfile && reportedIssuesSummary.isNotEmpty()) {
            ReportedIssuesCard(reportedIssuesSummary)
        }
    }
}

private fun buildReportedIssuesSummary(reviews: List<VolunteerReview>): List<ReportedIssueSummary> {
    return reviews
        .flatMap { it.issues }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .map { (issue, count) -> ReportedIssueSummary(issue = issue, count = count) }
        .sortedWith(compareByDescending<ReportedIssueSummary> { it.count }.thenBy { it.issue })
}

// ═══════════════════════════════════════════════════════════════════════════
// OVERALL RATING CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun OverallRatingCard(
    reviews: List<VolunteerReview>,
    avgRating: Double,
    avgRatingStr: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, AtharColors.Gray200),
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AtharColors.Gray100)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = AtharColors.Accent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "Reviews & Ratings",
                    color = AtharColors.Secondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            HorizontalDivider(color = AtharColors.Gray200, thickness = 2.dp)

            // Body
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Big rating circle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AtharColors.Gray100, AtharColors.Primary)
                                )
                            )
                            .border(3.dp, AtharColors.Accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            avgRatingStr,
                            color = AtharColors.Secondary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        (1..5).forEach { star ->
                            RatingStar(
                                filled = star <= avgRating.toInt() + (if (avgRating % 1 >= 0.5) 1 else 0),
                                size = 14.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${reviews.size} reviews",
                        color = AtharColors.TextLight,
                        fontSize = 12.sp,
                    )
                }

                // Rating bars
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (5 downTo 1).forEach { starLevel ->
                        val count = reviews.count { it.rating == starLevel }
                        val pct = if (reviews.isNotEmpty()) count.toFloat() / reviews.size else 0f

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.width(28.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "$starLevel",
                                    color = AtharColors.TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = AtharColors.Accent,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AtharColors.Gray100),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(pct)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(AtharColors.Accent, AtharColors.AccentLight)
                                            )
                                        ),
                                )
                            }
                            Text(
                                "$count",
                                color = AtharColors.TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// INDIVIDUAL REVIEWS CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun IndividualReviewsCard(
    reviews: List<VolunteerReview>,
    displayedReviews: List<VolunteerReview>,
    showAllReviews: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, AtharColors.Gray200),
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AtharColors.Gray100)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Lucide.Eye,
                        contentDescription = null,
                        tint = AtharColors.Secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "User Reviews",
                        color = AtharColors.Secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
                Text(
                    "${reviews.size}",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AtharColors.Accent)
                        .wrapContentSize(Alignment.Center),
                    color = AtharColors.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = AtharColors.Gray200, thickness = 2.dp)

            // Reviews list
            if (displayedReviews.isEmpty()) {
                Text(
                    "No reviews yet.",
                    modifier = Modifier.padding(20.dp),
                    color = AtharColors.TextLight,
                    fontSize = 13.sp,
                )
            } else {
                displayedReviews.forEachIndexed { index, review ->
                    ReviewItem(review = review, colorIndex = index)
                    if (index < displayedReviews.lastIndex) {
                        HorizontalDivider(
                            color = AtharColors.Gray200,
                            thickness = 2.dp,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }

            // Show More / Show Less
            if (reviews.size > 3) {
                HorizontalDivider(color = AtharColors.Gray200, thickness = 2.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggle)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (showAllReviews) Lucide.ChevronUp else Lucide.ChevronDown,
                        contentDescription = null,
                        tint = AtharColors.AccentDark,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (showAllReviews) "Show Less" else "Show All ${reviews.size} Reviews",
                        color = AtharColors.AccentDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(review: VolunteerReview, colorIndex: Int) {
    val initials = review.userName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatar with initials
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColors[colorIndex % avatarColors.size])
                .border(2.dp, AtharColors.Gray100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                color = AtharColors.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    review.userName,
                    color = AtharColors.Secondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    review.date,
                    color = AtharColors.TextLight,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..5).forEach { star ->
                    RatingStar(filled = star <= review.rating, size = 14.dp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                review.comment,
                color = AtharColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun RatingStar(filled: Boolean, size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
        contentDescription = null,
        tint = if (filled) AtharColors.Accent else AtharColors.Gray200,
        modifier = Modifier.size(size),
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// REPORTED ISSUES CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ReportedIssuesCard(reportedIssuesSummary: List<ReportedIssueSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, AtharColors.Gray200),
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AtharColors.WarningBg)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Lucide.TriangleAlert,
                    contentDescription = null,
                    tint = AtharColors.AccentDark,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "Reported Issues",
                    color = AtharColors.Secondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            HorizontalDivider(color = AtharColors.Gray200, thickness = 2.dp)

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Privacy notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AtharColors.Gray100)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Lucide.ShieldAlert,
                        contentDescription = null,
                        tint = AtharColors.TextLight,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "This section is only visible to you and admins",
                        color = AtharColors.TextLight,
                        fontSize = 12.sp,
                    )
                }

                // Issue rows
                reportedIssuesSummary.forEach { item ->
                    val icon = issueIconMap[item.issue] ?: Lucide.TriangleAlert

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, AtharColors.Gray200, RoundedCornerShape(12.dp))
                            .background(AtharColors.White)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AtharColors.WarningBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = AtharColors.AccentDark,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            item.issue,
                            modifier = Modifier.weight(1f),
                            color = AtharColors.Secondary,
                            fontSize = 14.sp,
                        )
                        Text(
                            "${item.count}×",
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(AtharColors.WarningBg)
                                .border(
                                    1.5.dp,
                                    AtharColors.Accent,
                                    RoundedCornerShape(50),
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            color = AtharColors.AccentDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true)
@Composable
private fun PreviewAtharVolunteerReviews() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        AtharVolunteerReviewsSection(
            reviews = mockReviews,
            isOwnProfile = true,
        )
    }
}
