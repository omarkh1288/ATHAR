package com.athar.accessibilitymapping.ui.review

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ATHAR - End of Ride Review Bottom Sheet (Jetpack Compose)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * A pixel-perfect Kotlin translation of the web EndOfRideReview.tsx.
 * Half-screen bottom sheet with:
 *   • Drag handle + close button
 *   • Volunteer info card with "Completed" badge
 *   • Star rating (1–5) with emoji labels
 *   • Optional comment box
 *   • Collapsible "Report an issue" section with 9 issue chips
 *   • Optional issue detail text field
 *   • Submit / Skip buttons
 *   • Success confirmation screen
 *
 * ── PLACEMENT ──
 *   app/src/main/java/com/athar/accessibilitymapping/ui/review/AtharEndOfRideReview.kt
 *
 * ── USAGE ──
 *   AtharEndOfRideReview(
 *       volunteerName = "Sara Mohammed",
 *       onSubmit = { rating, comment, issues, reportText -> },
 *       onSkip = { /* dismiss */ },
 *       onBackToHome = { /* navigate home */ },
 *   )
 */

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════
// ATHAR COLOR PALETTE
// ═══════════════════════════════════════════════════════════════════════════

private object AtharColors {
    val Primary = Color(0xFFEAF2FB)
    val PrimaryGradient = Color(0xFFD6E4F5)
    val Secondary = Color(0xFF1F3C5B)
    val SecondaryDark = Color(0xFF2C4F73)
    val Accent = Color(0xFFC9A24D)
    val AccentLight = Color(0xFFD9B76D)
    val AccentDark = Color(0xFFB38F3D)
    val TextPrimary = Color(0xFF1F3C5B)
    val TextLight = Color(0xFF5B7A99)
    val Success = Color(0xFF10B981)
    val SuccessLight = Color(0xFFD1FAE5)
    val SuccessGreen = Color(0xFF059669)
    val SuccessText = Color(0xFF065F46)
    val WarningBg = Color(0xFFFFF8E6)
    val Gray100 = Color(0xFFF1F5F9)
    val Gray200 = Color(0xFFE2E8F0)
    val White = Color.White
}

// ═══════════════════════════════════════════════════════════════════════════
// ISSUE CHIP DATA
// ═══════════════════════════════════════════════════════════════════════════

private data class IssueChip(
    val label: String,
    val icon: ImageVector,
)

private val issueChips = listOf(
    IssueChip("Late arrival", Icons.Filled.Schedule),
    IssueChip("Unprofessional behavior", Icons.Filled.SentimentDissatisfied),
    IssueChip("Route issue", Icons.Filled.Route),
    IssueChip("Communication problem", Icons.Filled.HearingDisabled),
    IssueChip("Safety concern", Icons.Filled.GppBad),
    IssueChip("Wrong location", Icons.Filled.WrongLocation),
    IssueChip("Accessibility needs not met", Icons.Filled.NotAccessible),
    IssueChip("Uncomfortable experience", Icons.Filled.Warning),
    IssueChip("Other", Icons.Filled.HelpOutline),
)

private val ratingLabels = listOf("", "Poor", "Fair", "Good", "Great", "Excellent!")
private val ratingEmojis = listOf("", "😞", "😐", "🙂", "😊", "🤩")

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AtharEndOfRideReview(
    volunteerName: String,
    onSubmit: (rating: Int, comment: String, issues: List<String>, reportText: String) -> Unit,
    onSkip: () -> Unit,
    onBackToHome: () -> Unit,
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var selectedIssues by remember { mutableStateOf(listOf<String>()) }
    var reportText by remember { mutableStateOf("") }
    var showIssues by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    // Handle submission animation
    LaunchedEffect(isSubmitting) {
        if (isSubmitting) {
            delay(1000)
            isSubmitting = false
            submitted = true
            onSubmit(rating, comment, selectedIssues, reportText)
        }
    }

    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSkip,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (submitted) {
                SuccessScreen(
                    onBackToHome = onBackToHome,
                )
            } else {
                ReviewScreen(
                    volunteerName = volunteerName,
                    rating = rating,
                    comment = comment,
                    selectedIssues = selectedIssues,
                    reportText = reportText,
                    showIssues = showIssues,
                    isSubmitting = isSubmitting,
                    onRatingChange = { rating = it },
                    onCommentChange = { comment = it },
                    onToggleIssue = { issue ->
                        selectedIssues = if (selectedIssues.contains(issue)) {
                            selectedIssues - issue
                        } else {
                            selectedIssues + issue
                        }
                    },
                    onReportTextChange = { reportText = it },
                    onToggleShowIssues = { showIssues = !showIssues },
                    onSubmit = { if (rating > 0) isSubmitting = true },
                    onSkip = onSkip,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REVIEW SCREEN (Main half-screen bottom sheet)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ReviewScreen(
    volunteerName: String,
    rating: Int,
    comment: String,
    selectedIssues: List<String>,
    reportText: String,
    showIssues: Boolean,
    isSubmitting: Boolean,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onToggleIssue: (String) -> Unit,
    onReportTextChange: (String) -> Unit,
    onToggleShowIssues: () -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Drag Handle + Header ──
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AtharColors.Gray200),
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Rate your experience",
                        color = AtharColors.Secondary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AtharColors.Gray100),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = AtharColors.Secondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // ── Scrollable Content ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── Volunteer Card ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AtharColors.Primary)
                        .border(2.dp, AtharColors.Gray200, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AtharColors.Secondary, AtharColors.SecondaryDark)
                                )
                            )
                            .border(2.dp, AtharColors.SecondaryDark, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = AtharColors.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            volunteerName,
                            color = AtharColors.Secondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                        Text(
                            "Your volunteer assistant",
                            color = AtharColors.TextLight,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Completed",
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AtharColors.SuccessLight)
                            .border(2.dp, AtharColors.Success, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = AtharColors.SuccessText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // ── Star Rating ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "How would you rate this assistance?",
                        color = AtharColors.TextLight,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { onRatingChange(star) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "Rate $star star${if (star > 1) "s" else ""}",
                                    tint = if (star <= rating) AtharColors.Accent else AtharColors.Gray200,
                                    modifier = Modifier.size(44.dp),
                                )
                            }
                        }
                    }

                    // Rating label with emoji
                    AnimatedVisibility(
                        visible = rating > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Text(
                            "${ratingEmojis[rating]}  ${ratingLabels[rating]}",
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(AtharColors.Primary)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            color = AtharColors.AccentDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }

                // ── Comment Box ──
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    placeholder = {
                        Text(
                            "Share your experience... (optional)",
                            color = AtharColors.TextLight,
                            fontSize = 14.sp,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtharColors.Secondary,
                        unfocusedBorderColor = AtharColors.Gray200,
                        focusedTextColor = AtharColors.Secondary,
                        unfocusedTextColor = AtharColors.Secondary,
                        cursorColor = AtharColors.Accent,
                        unfocusedContainerColor = AtharColors.Gray100,
                        focusedContainerColor = AtharColors.White,
                    ),
                )

                // ── Report Issue Toggle Button ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (showIssues || selectedIssues.isNotEmpty()) AtharColors.WarningBg
                            else AtharColors.White
                        )
                        .border(
                            2.dp,
                            if (showIssues || selectedIssues.isNotEmpty()) AtharColors.Accent
                            else AtharColors.Gray200,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable(onClick = onToggleShowIssues)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.ReportProblem,
                        contentDescription = null,
                        tint = AtharColors.AccentDark,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Report an issue",
                            color = AtharColors.Secondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Text(
                            if (selectedIssues.isNotEmpty())
                                "${selectedIssues.size} issue${if (selectedIssues.size > 1) "s" else ""} selected"
                            else "Did anything go wrong?",
                            color = AtharColors.TextLight,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(
                        if (showIssues) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = AtharColors.Secondary,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AtharColors.Primary)
                            .padding(2.dp),
                    )
                }

                // ── Issue Chips (collapsible) ──
                AnimatedVisibility(
                    visible = showIssues,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AtharColors.Gray100)
                            .border(2.dp, AtharColors.Gray200, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Chips in a flow layout
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            issueChips.forEach { chip ->
                                val selected = selectedIssues.contains(chip.label)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .then(
                                            if (selected)
                                                Modifier
                                                    .background(AtharColors.Secondary)
                                                    .border(
                                                        2.dp,
                                                        AtharColors.SecondaryDark,
                                                        RoundedCornerShape(50),
                                                    )
                                            else
                                                Modifier
                                                    .background(AtharColors.White)
                                                    .border(
                                                        2.dp,
                                                        AtharColors.Gray200,
                                                        RoundedCornerShape(50),
                                                    )
                                        )
                                        .clickable { onToggleIssue(chip.label) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        chip.icon,
                                        contentDescription = null,
                                        tint = if (selected) AtharColors.White else AtharColors.Secondary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        chip.label,
                                        color = if (selected) AtharColors.White else AtharColors.Secondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }

                        // Report text field (visible when issues are selected)
                        AnimatedVisibility(
                            visible = selectedIssues.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            OutlinedTextField(
                                value = reportText,
                                onValueChange = onReportTextChange,
                                placeholder = {
                                    Text(
                                        "Describe the issue in more detail (optional)...",
                                        color = AtharColors.TextLight,
                                        fontSize = 13.sp,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AtharColors.Secondary,
                                    unfocusedBorderColor = AtharColors.Gray200,
                                    focusedTextColor = AtharColors.Secondary,
                                    unfocusedTextColor = AtharColors.Secondary,
                                    cursorColor = AtharColors.Accent,
                                    unfocusedContainerColor = AtharColors.White,
                                    focusedContainerColor = AtharColors.White,
                                ),
                            )
                        }
                    }
                }

                // ── Submit Button ──
                Button(
                    onClick = onSubmit,
                    enabled = rating > 0 && !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AtharColors.Accent,
                        disabledContainerColor = AtharColors.Gray200,
                        disabledContentColor = AtharColors.TextLight,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (rating > 0) 6.dp else 0.dp,
                    ),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AtharColors.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Submitting...",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    } else {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Submit Review",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }

                // ── Skip Link ──
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        "Skip for now",
                        color = AtharColors.TextLight,
                        fontSize = 14.sp,
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SUCCESS CONFIRMATION SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SuccessScreen(
    onBackToHome: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AtharColors.Gray200),
            )
            Spacer(Modifier.height(32.dp))

            // Success icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AtharColors.SuccessLight, Color(0xFFA7F3D0))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AtharColors.SuccessGreen,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Thank you for your feedback!",
                color = AtharColors.Secondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your review has been submitted successfully. It helps us improve the experience for everyone.",
                color = AtharColors.TextLight,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AtharColors.Secondary,
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Back to Home",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewAtharEndOfRideReview() {
    AtharEndOfRideReview(
        volunteerName = "Sara Mohammed",
        onSubmit = { _, _, _, _ -> },
        onSkip = {},
        onBackToHome = {},
    )
}
