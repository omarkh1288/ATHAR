package com.athar.accessibilitymapping.ui.screens

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ATHAR - Volunteer Dashboard (Jetpack Compose)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * A pixel-perfect Kotlin translation of the web VolunteerDashboard.tsx.
 * Self-contained — all colors, data models, mock data, and composables
 * are included in this single file.
 *
 * ── PLACEMENT ──
 *   app/src/main/java/com/athar/app/ui/dashboard/AtharVolunteerDashboard.kt
 *
 * ── DEPENDENCIES (build.gradle.kts :app) ──
 *   // Compose BOM
 *   implementation(platform("androidx.compose:compose-bom:2024.02.00"))
 *   implementation("androidx.compose.ui:ui")
 *   implementation("androidx.compose.ui:ui-graphics")
 *   implementation("androidx.compose.ui:ui-tooling-preview")
 *   implementation("androidx.compose.material3:material3")
 *   implementation("androidx.compose.material:material-icons-extended")
 *   implementation("androidx.compose.foundation:foundation")
 *   implementation("androidx.compose.animation:animation")
 *   implementation("androidx.activity:activity-compose:1.8.2")
 *   implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
 *
 * ── USAGE ──
 *   AtharVolunteerDashboard(
 *       isVolunteerLive = true,    // from your state/viewmodel
 *       userName = "Sara Mohammed", // from your auth context
 *       onNavigateToMap = { },      // callback to switch to map tab
 *   )
 */

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.AssistanceRequest as DomainAssistanceRequest
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.RequestStatus
import com.athar.accessibilitymapping.data.VolunteerDashboardCounts
import com.athar.accessibilitymapping.data.VolunteerImpactDashboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════
// ATHAR COLOR PALETTE
// ═══════════════════════════════════════════════════════════════════════════

private object VolunteerDashboardColors {
    // Primary - Light Sky Blue
    val Primary = Color(0xFFEAF2FB)
    val PrimaryGradient = Color(0xFFD6E4F5)   // Soft Blue / bg-secondary

    // Secondary - Navy Blue
    val Secondary = Color(0xFF1F3C5B)
    val SecondaryDark = Color(0xFF2C4F73)

    // Accent - Warm Gold
    val Accent = Color(0xFFC9A24D)
    val AccentLight = Color(0xFFD9B76D)
    val AccentDark = Color(0xFFB38F3D)

    // Text
    val TextPrimary = Color(0xFF1F3C5B)
    val TextSecondary = Color(0xFF2C4F73)
    val TextLight = Color(0xFF5B7A99)

    // Success
    val Success = Color(0xFF10B981)
    val SuccessDark = Color(0xFF059669)
    val SuccessLight = Color(0xFFD1FAE5)
    val SuccessText = Color(0xFF065F46)
    val SuccessPulse = Color(0xFFA7F3D0)

    // Error / High urgency
    val Error = Color(0xFFDC2626)
    val ErrorBg = Color(0xFFFEF2F2)
    val ErrorBorder = Color(0xFFFECACA)

    // Warning / Medium urgency (gold tint)
    val WarningBg = Color(0xFFFFF8E6)

    // Neutrals
    val Gray50 = Color(0xFFF8FAFB)
    val Gray100 = Color(0xFFF1F5F9)
    val Gray200 = Color(0xFFE2E8F0)
    val Gray600 = Color(0xFF475569)
    val White = Color.White
}

// ═══════════════════════════════════════════════════════════════════════════
// DATA MODELS
// ═══════════════════════════════════════════════════════════════════════════

private enum class DashboardTab(val label: String) {
    INCOMING("Incoming"),
    ACCEPTED("Active"),
    HISTORY("History")
}

private enum class Urgency { LOW, MEDIUM, HIGH }

private data class AssistanceRequest(
    val id: String,
    val userName: String,
    val userType: String,
    val location: String,
    val destination: String,
    val distance: String,
    val urgency: Urgency,
    val helpType: String,
    val requestTime: String,
    val hours: Int,
    val pricePerHour: Int,
)

private enum class HistoryOutcome { COMPLETED, CANCELLED }

private data class HistoryItem(
    val id: String,
    val userName: String,
    val userType: String,
    val location: String,
    val helpType: String,
    val completedTime: String,
    val rating: Int,
    val outcome: HistoryOutcome,
)

private data class VolunteerStats(
    val totalAssists: Int,
    val thisWeek: Int,
    val avgRating: Float,
    val streak: Int,
)

private data class UrgencyStyle(
    val bg: Color,
    val text: Color,
    val border: Color,
    val icon: ImageVector,
)

// ═══════════════════════════════════════════════════════════════════════════
// MOCK DATA
// ═══════════════════════════════════════════════════════════════════════════

private val defaultVolunteerStats = VolunteerStats(
    totalAssists = 47,
    thisWeek = 5,
    avgRating = 4.8f,
    streak = 3,
)

// ═══════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

private fun getUrgencyStyle(urgency: Urgency): UrgencyStyle = when (urgency) {
    Urgency.HIGH -> UrgencyStyle(
        bg = VolunteerDashboardColors.ErrorBg,
        text = VolunteerDashboardColors.Error,
        border = VolunteerDashboardColors.ErrorBorder,
        icon = Icons.Filled.Warning,
    )
    Urgency.MEDIUM -> UrgencyStyle(
        bg = VolunteerDashboardColors.WarningBg,
        text = VolunteerDashboardColors.AccentDark,
        border = VolunteerDashboardColors.AccentLight,
        icon = Icons.Filled.Schedule,
    )
    Urgency.LOW -> UrgencyStyle(
        bg = VolunteerDashboardColors.PrimaryGradient,
        text = VolunteerDashboardColors.Secondary,
        border = VolunteerDashboardColors.Secondary,
        icon = Icons.Filled.FavoriteBorder,
    )
}

private fun getUserTypeIcon(userType: String): ImageVector {
    val lower = userType.lowercase()
    return when {
        "wheelchair" in lower || "mobility" in lower -> Icons.Filled.Accessible
        "visual" in lower -> Icons.Filled.Visibility
        "hearing" in lower -> Icons.Filled.HearingDisabled
        else -> Icons.Outlined.Person
    }
}

private fun String.toUrgency(): Urgency {
    return when (lowercase()) {
        "high", "urgent" -> Urgency.HIGH
        "medium", "normal" -> Urgency.MEDIUM
        else -> Urgency.LOW
    }
}

private fun DomainAssistanceRequest.toDashboardRequest(): AssistanceRequest {
    return AssistanceRequest(
        id = id,
        userName = userName,
        userType = userType,
        location = location,
        destination = destination,
        distance = distance,
        urgency = urgency.toUrgency(),
        helpType = helpType,
        requestTime = requestTime,
        hours = hours,
        pricePerHour = pricePerHour
    )
}

private fun DomainAssistanceRequest.toHistoryItem(): HistoryItem {
    return HistoryItem(
        id = id,
        userName = userName,
        userType = userType,
        location = location,
        helpType = helpType,
        completedTime = requestTime,
        rating = 0,
        outcome = if (
            status == RequestStatus.Completed ||
            status == RequestStatus.Rated ||
            status == RequestStatus.Archived
        ) HistoryOutcome.COMPLETED else HistoryOutcome.CANCELLED
    )
}

private fun AssistanceRequest.toCompletedHistoryItem(): HistoryItem {
    return HistoryItem(
        id = id,
        userName = userName,
        userType = userType,
        location = location,
        helpType = helpType,
        completedTime = "Just now",
        rating = 0,
        outcome = HistoryOutcome.COMPLETED
    )
}

private fun mergeHistoryItems(
    localItems: List<HistoryItem>,
    remoteItems: List<HistoryItem>
): List<HistoryItem> {
    return (localItems + remoteItems).distinctBy { it.id }
}

private fun openNavigation(context: Context, destinationLabel: String) {
    val destination = destinationLabel.trim()
    if (destination.isBlank()) {
        Toast.makeText(context, "No destination available for navigation.", Toast.LENGTH_SHORT).show()
        return
    }

    val encodedDestination = Uri.encode(destination)
    val navigationIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("google.navigation:q=$encodedDestination&mode=w")
    ).apply {
        setPackage("com.google.android.apps.maps")
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val browserIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encodedDestination&travelmode=walking")
    ).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    when {
        navigationIntent.resolveActivity(context.packageManager) != null -> context.startActivity(navigationIntent)
        browserIntent.resolveActivity(context.packageManager) != null -> context.startActivity(browserIntent)
        else -> Toast.makeText(context, "No maps app available on this device.", Toast.LENGTH_SHORT).show()
    }
}

private fun openSmsComposer(context: Context, rawPhoneNumber: String?, messageBody: String) {
    val phoneNumber = rawPhoneNumber.orEmpty().trim()
    if (phoneNumber.isBlank()) {
        Toast.makeText(context, "No phone number available for messaging.", Toast.LENGTH_SHORT).show()
        return
    }

    val smsIntent = Intent(
        Intent.ACTION_SENDTO,
        Uri.parse("smsto:${Uri.encode(phoneNumber)}")
    ).apply {
        putExtra("sms_body", messageBody)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (smsIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(smsIntent)
    } else {
        Toast.makeText(context, "No messaging app available on this device.", Toast.LENGTH_SHORT).show()
    }
}

private fun openDialer(context: Context, rawPhoneNumber: String?) {
    val phoneNumber = rawPhoneNumber.orEmpty().trim()
    if (phoneNumber.isBlank()) {
        Toast.makeText(context, "No phone number available for this request.", Toast.LENGTH_SHORT).show()
        return
    }

    val dialIntent = Intent(
        Intent.ACTION_DIAL,
        Uri.parse("tel:${Uri.encode(phoneNumber)}")
    ).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (dialIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(dialIntent)
    } else {
        Toast.makeText(context, "No dialer app available on this device.", Toast.LENGTH_SHORT).show()
    }
}

private fun extractPhoneNumber(vararg candidates: String): String? {
    val phoneRegex = Regex("""(?:\+|00)?[\d\s().-]{8,}""")
    candidates.forEach { candidate ->
        phoneRegex.findAll(candidate).forEach { match ->
            val value = match.value.trim()
            if (value.count { it.isDigit() } >= 8) {
                return value.filter { it.isDigit() || it == '+' }
            }
        }
    }
    return null
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AtharVolunteerDashboard(
    isVolunteerLive: Boolean,
    userName: String = "Sara Mohammed",
    onNavigateToMap: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember(context) { AtharRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(DashboardTab.INCOMING) }
    var activeRequests by remember { mutableStateOf<List<AssistanceRequest>>(emptyList()) }
    var acceptedRequest by remember { mutableStateOf<AssistanceRequest?>(null) }
    var historyItems by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var localCompletedItems by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var tabCounts by remember { mutableStateOf(VolunteerDashboardCounts()) }
    var incomingAlertMessage by remember { mutableStateOf("No incoming requests nearby.") }
    var activeStatusBanner by remember { mutableStateOf("Assistance in Progress") }
    var volunteerStats by remember { mutableStateOf(defaultVolunteerStats) }

    suspend fun refreshIncomingData() {
        val incoming = repository.getVolunteerIncomingDashboard(perPage = 50)
        val mappedIncomingRequests = incoming.requests.map { it.toDashboardRequest() }
        tabCounts = incoming.counts
        val alertCount = if (incoming.incomingAlert.count > 0) {
            incoming.incomingAlert.count
        } else {
            mappedIncomingRequests.size
        }
        incomingAlertMessage = incoming.incomingAlert.message.ifBlank {
            if (alertCount > 0) {
                "$alertCount ${if (alertCount == 1) "person needs" else "people need"} your help nearby"
            } else {
                "No incoming requests nearby."
            }
        }
        activeRequests = mappedIncomingRequests.filter { request -> request.id != acceptedRequest?.id }
    }

    suspend fun refreshActiveData() {
        val active = repository.getVolunteerActiveDashboard(perPage = 50)
        val mappedActiveRequests = active.requests.map { it.toDashboardRequest() }
        tabCounts = active.counts
        activeStatusBanner = active.statusBanner.ifBlank { "Assistance in Progress" }
        acceptedRequest = acceptedRequest?.let { current ->
            mappedActiveRequests.firstOrNull { request -> request.id == current.id } ?: current
        } ?: mappedActiveRequests.firstOrNull()
    }

    suspend fun refreshHistoryData() {
        val history = repository.getVolunteerHistoryDashboard(perPage = 50)
        val remoteHistory = history.requests.map { it.toHistoryItem() }
        tabCounts = history.counts
        volunteerStats = volunteerStats.copy(
            totalAssists = history.impact.totalAssists,
            avgRating = history.impact.avgRating,
            thisWeek = history.impact.thisWeek
        )
        historyItems = mergeHistoryItems(localCompletedItems, remoteHistory)
    }

    suspend fun refreshImpactData() {
        val impact: VolunteerImpactDashboard = repository.getVolunteerImpactDashboard()
        tabCounts = impact.counts
        volunteerStats = volunteerStats.copy(
            totalAssists = impact.impact.totalAssists,
            avgRating = impact.impact.avgRating,
            thisWeek = impact.impact.thisWeek
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshImpactData()
            delay(30000L)
        }
    }

    LaunchedEffect(isVolunteerLive, activeTab) {
        actionError = null
        if (!isVolunteerLive) {
            activeRequests = emptyList()
            acceptedRequest = null
            historyItems = emptyList()
            localCompletedItems = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            when (activeTab) {
                DashboardTab.INCOMING -> refreshIncomingData()
                DashboardTab.ACCEPTED -> refreshActiveData()
                DashboardTab.HISTORY -> refreshHistoryData()
            }
            val pollDelayMs = when (activeTab) {
                DashboardTab.INCOMING -> 15000L
                DashboardTab.ACCEPTED -> 20000L
                DashboardTab.HISTORY -> 30000L
            }
            delay(pollDelayMs)
        }
    }

    val onAccept: (AssistanceRequest) -> Unit = { request ->
        coroutineScope.launch {
            actionError = null
            when (val result = repository.acceptRequest(request.id)) {
                is ApiCallResult.Success -> {
                    acceptedRequest = request
                    activeTab = DashboardTab.ACCEPTED
                    refreshActiveData()
                    refreshIncomingData()
                }
                is ApiCallResult.Failure -> {
                    actionError = result.message
                }
            }
        }
    }

    val onDecline: (String) -> Unit = { requestId ->
        coroutineScope.launch {
            actionError = null
            when (val result = repository.declineRequest(requestId)) {
                is ApiCallResult.Success -> refreshIncomingData()
                is ApiCallResult.Failure -> {
                    actionError = result.message
                }
            }
        }
    }

    val onComplete: () -> Unit = {
        acceptedRequest?.let { completedRequest ->
            val requestId = completedRequest.id
            coroutineScope.launch {
                actionError = null
                when (val result = repository.completeRequest(requestId)) {
                    is ApiCallResult.Success -> {
                        localCompletedItems = listOf(completedRequest.toCompletedHistoryItem()) +
                            localCompletedItems.filterNot { existing -> existing.id == completedRequest.id }
                        acceptedRequest = null
                        activeTab = DashboardTab.HISTORY
                        historyItems = mergeHistoryItems(localCompletedItems, historyItems)
                        refreshHistoryData()
                        refreshActiveData()
                        refreshImpactData()
                    }
                    is ApiCallResult.Failure -> {
                        actionError = result.message
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = FontFamily.SansSerif)) {
        if (!isVolunteerLive) {
            OfflineView(
                userName = userName,
                onNavigateToMap = onNavigateToMap,
                stats = volunteerStats
            )
        } else {
            LiveDashboard(
                activeTab = activeTab,
                onTabSelected = { activeTab = it },
                tabCounts = tabCounts,
                stats = volunteerStats,
                incomingAlertMessage = incomingAlertMessage,
                activeStatusBanner = activeStatusBanner,
                activeRequests = activeRequests,
                acceptedRequest = acceptedRequest,
                onAccept = onAccept,
                onDecline = onDecline,
                onComplete = onComplete,
                historyItems = historyItems,
                actionError = actionError,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// OFFLINE VIEW
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun OfflineView(
    userName: String,
    onNavigateToMap: () -> Unit,
    stats: VolunteerStats,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerDashboardColors.Primary)
    ) {
        // Header
        DashboardHeader(title = "Volunteer Dashboard", subtitle = "Welcome back, $userName")

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // Avatar circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .border(3.dp, VolunteerDashboardColors.Secondary, CircleShape)
                    .background(VolunteerDashboardColors.PrimaryGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = VolunteerDashboardColors.Secondary,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "You're Offline",
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerDashboardColors.Secondary,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Go live from the Map view to start receiving assistance requests from people nearby.",
                fontSize = 16.sp,
                color = VolunteerDashboardColors.TextSecondary.copy(alpha = 0.8f),
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )

            Spacer(Modifier.height(24.dp))

            // "Tap Go Live" hint
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, VolunteerDashboardColors.Accent, RoundedCornerShape(12.dp))
                    .background(VolunteerDashboardColors.PrimaryGradient)
                    .clickable { onNavigateToMap() }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.NearMe, contentDescription = null, tint = VolunteerDashboardColors.AccentDark, modifier = Modifier.size(20.dp))
                Text("Tap \"Go Live\" on the Map", fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.AccentDark, fontSize = 15.sp)
            }

            Spacer(Modifier.height(32.dp))

            // Quick stats grid
            StatsGrid(stats = stats)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIVE DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LiveDashboard(
    activeTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    tabCounts: VolunteerDashboardCounts,
    stats: VolunteerStats,
    incomingAlertMessage: String,
    activeStatusBanner: String,
    activeRequests: List<AssistanceRequest>,
    acceptedRequest: AssistanceRequest?,
    onAccept: (AssistanceRequest) -> Unit,
    onDecline: (String) -> Unit,
    onComplete: () -> Unit,
    historyItems: List<HistoryItem>,
    actionError: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VolunteerDashboardColors.Primary)
    ) {
        // ─── Header ───
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp)
                .background(VolunteerDashboardColors.Secondary)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 22.dp)
        ) {
            // Top row: LIVE badge + stats + avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    // LIVE indicator
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulsingDot(color = VolunteerDashboardColors.Success, size = 10)
                        Text("LIVE", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = VolunteerDashboardColors.SuccessPulse)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = VolunteerDashboardColors.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Rating + assists count
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = VolunteerDashboardColors.Accent, modifier = Modifier.size(14.dp))
                            Text("${stats.avgRating}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.Accent)
                        }
                        Text("${stats.totalAssists} assists", fontSize = 12.sp, color = VolunteerDashboardColors.PrimaryGradient.copy(alpha = 0.7f))
                    }

                    // Profile avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(2.dp, VolunteerDashboardColors.Accent, CircleShape)
                            .background(VolunteerDashboardColors.SecondaryDark, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = VolunteerDashboardColors.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (!actionError.isNullOrBlank()) {
                Text(
                    text = actionError,
                    color = VolunteerDashboardColors.ErrorBg,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            // ─── Tabs ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tabBadgeCounts = mapOf(
                    DashboardTab.INCOMING to tabCounts.incoming,
                    DashboardTab.ACCEPTED to tabCounts.active,
                    DashboardTab.HISTORY to tabCounts.history,
                )

                DashboardTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    val count = tabBadgeCounts[tab] ?: 0

                    val bgColor by animateColorAsState(
                        if (isSelected) VolunteerDashboardColors.White else VolunteerDashboardColors.SecondaryDark,
                        label = "tabBg"
                    )
                    val textColor by animateColorAsState(
                        if (isSelected) VolunteerDashboardColors.Secondary else VolunteerDashboardColors.White,
                        label = "tabText"
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .clickable { onTabSelected(tab) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(tab.label, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = textColor, fontSize = 14.sp)
                        if (count > 0) {
                            Text(
                                "$count",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VolunteerDashboardColors.White,
                                modifier = Modifier
                                    .background(VolunteerDashboardColors.Accent, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─── Tab Content ───
        when (activeTab) {
            DashboardTab.INCOMING -> IncomingRequestsTab(
                requests = activeRequests,
                incomingAlertMessage = incomingAlertMessage,
                onAccept = onAccept,
                onDecline = onDecline
            )
            DashboardTab.ACCEPTED -> AcceptedTab(
                acceptedRequest = acceptedRequest,
                statusBanner = activeStatusBanner,
                onComplete = onComplete
            )
            DashboardTab.HISTORY -> HistoryTab(historyItems = historyItems, stats = stats)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// INCOMING REQUESTS TAB
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun IncomingRequestsTab(
    requests: List<AssistanceRequest>,
    incomingAlertMessage: String,
    onAccept: (AssistanceRequest) -> Unit,
    onDecline: (String) -> Unit,
) {
    if (requests.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.CheckCircle,
            iconTint = VolunteerDashboardColors.Success,
            borderColor = VolunteerDashboardColors.Success,
            title = "All Caught Up!",
            message = "No assistance requests at the moment. You'll be notified when someone nearby needs help.",
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Alert banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, VolunteerDashboardColors.AccentLight, RoundedCornerShape(12.dp))
                .background(VolunteerDashboardColors.WarningBg, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = VolunteerDashboardColors.AccentDark, modifier = Modifier.size(16.dp))
            Text(
                incomingAlertMessage,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerDashboardColors.AccentDark,
            )
        }

        // Request cards
        requests.forEach { request ->
            RequestCard(request = request, onAccept = { onAccept(request) }, onDecline = { onDecline(request.id) })
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SINGLE REQUEST CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RequestCard(
    request: AssistanceRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val urgency = getUrgencyStyle(request.urgency)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, VolunteerDashboardColors.Gray200, RoundedCornerShape(16.dp))
            .background(VolunteerDashboardColors.White)
    ) {
        // ── Top section ──
        Column(modifier = Modifier.padding(16.dp)) {
            // User row + urgency badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Avatar + name
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, VolunteerDashboardColors.SecondaryDark, CircleShape)
                            .background(VolunteerDashboardColors.Secondary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = VolunteerDashboardColors.White, modifier = Modifier.size(24.dp))
                    }

                    Column {
                        Text(request.userName, fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.Secondary, fontSize = 16.sp)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(getUserTypeIcon(request.userType), contentDescription = null, tint = VolunteerDashboardColors.TextSecondary, modifier = Modifier.size(16.dp))
                            Text(request.userType, fontSize = 14.sp, color = VolunteerDashboardColors.TextSecondary)
                        }
                    }
                }

                // Urgency + time
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .border(2.dp, urgency.border, RoundedCornerShape(20.dp))
                            .background(urgency.bg, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(urgency.icon, contentDescription = null, tint = urgency.text, modifier = Modifier.size(12.dp))
                        Text(
                            request.urgency.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = urgency.text,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = VolunteerDashboardColors.TextLight, modifier = Modifier.size(14.dp))
                        Text(request.requestTime, fontSize = 12.sp, color = VolunteerDashboardColors.TextLight)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Location info
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = VolunteerDashboardColors.Secondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Column {
                    Text(
                        buildString {
                            append("From: ")
                            append(request.location)
                        },
                        fontSize = 14.sp,
                        color = VolunteerDashboardColors.TextSecondary,
                    )
                    Text(
                        buildString {
                            append("To: ")
                            append(request.destination)
                        },
                        fontSize = 14.sp,
                        color = VolunteerDashboardColors.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Help type + distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .background(VolunteerDashboardColors.PrimaryGradient, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = VolunteerDashboardColors.Secondary, modifier = Modifier.size(16.dp))
                    Text(request.helpType, fontSize = 14.sp, color = VolunteerDashboardColors.Secondary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${request.hours * request.pricePerHour} EGP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VolunteerDashboardColors.AccentDark
                    )
                    Text(
                        "${request.hours}h × ${request.pricePerHour} EGP",
                        fontSize = 11.sp,
                        color = VolunteerDashboardColors.TextLight
                    )
                }
            }
        }

        // ── Action buttons ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawTopBorder(VolunteerDashboardColors.Gray200)
                .background(VolunteerDashboardColors.Gray50)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Accept
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VolunteerDashboardColors.Success,
                    contentColor = VolunteerDashboardColors.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Accept", fontWeight = FontWeight.SemiBold)
            }

            // Decline
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VolunteerDashboardColors.TextSecondary),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).let {
                    androidx.compose.foundation.BorderStroke(2.dp, VolunteerDashboardColors.Gray200)
                },
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Decline", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ACCEPTED (ACTIVE) TAB
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AcceptedTab(
    acceptedRequest: AssistanceRequest?,
    statusBanner: String,
    onComplete: () -> Unit,
) {
    if (acceptedRequest == null) {
        EmptyState(
            icon = Icons.Filled.CheckCircle,
            iconTint = VolunteerDashboardColors.Success,
            borderColor = VolunteerDashboardColors.Secondary,
            title = "No Active Assistance",
            message = "Accept an incoming request to start helping someone.",
        )
        return
    }

    val urgency = getUrgencyStyle(acceptedRequest.urgency)
    val context = LocalContext.current
    val contactPhone = remember(acceptedRequest) {
        extractPhoneNumber(
            acceptedRequest.helpType,
            acceptedRequest.location,
            acceptedRequest.destination
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Active banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, VolunteerDashboardColors.Success, RoundedCornerShape(12.dp))
                .background(VolunteerDashboardColors.SuccessLight, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PulsingDot(color = VolunteerDashboardColors.Success, size = 12)
            Text(statusBanner, fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.SuccessText, fontSize = 15.sp)
        }

        // User info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, VolunteerDashboardColors.Gray200, RoundedCornerShape(16.dp))
                .background(VolunteerDashboardColors.White)
        ) {
            // Card header with sky-blue bg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VolunteerDashboardColors.PrimaryGradient)
                    .padding(20.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(2.dp, VolunteerDashboardColors.SecondaryDark, CircleShape)
                        .background(VolunteerDashboardColors.Secondary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = VolunteerDashboardColors.White, modifier = Modifier.size(32.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(acceptedRequest.userName, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.Secondary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(getUserTypeIcon(acceptedRequest.userType), contentDescription = null, tint = VolunteerDashboardColors.TextSecondary, modifier = Modifier.size(20.dp))
                        Text(acceptedRequest.userType, fontSize = 14.sp, color = VolunteerDashboardColors.TextSecondary)
                    }
                }

                // Urgency badge
                Row(
                    modifier = Modifier
                        .border(2.dp, urgency.border, RoundedCornerShape(20.dp))
                        .background(urgency.bg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(urgency.icon, contentDescription = null, tint = urgency.text, modifier = Modifier.size(12.dp))
                    Text(acceptedRequest.urgency.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = urgency.text)
                }
            }

            // Details section
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Current Location
                DetailRow(
                    icon = Icons.Filled.LocationOn,
                    label = "Current Location",
                    value = acceptedRequest.location,
                )

                // Destination
                DetailRow(
                    icon = Icons.Filled.Navigation,
                    label = "Destination",
                    value = acceptedRequest.destination,
                )

                // Help type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, VolunteerDashboardColors.AccentLight, RoundedCornerShape(12.dp))
                        .background(VolunteerDashboardColors.WarningBg, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = VolunteerDashboardColors.AccentDark, modifier = Modifier.size(20.dp))
                    Text(acceptedRequest.helpType, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = VolunteerDashboardColors.Secondary)
                }

                // Time + distance
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = VolunteerDashboardColors.TextLight, modifier = Modifier.size(16.dp))
                    Text(
                        "Requested ${acceptedRequest.requestTime} · ${acceptedRequest.distance} away",
                        fontSize = 14.sp,
                        color = VolunteerDashboardColors.TextLight,
                    )
                }
            }

            // Action buttons
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Navigate to User
                Button(
                    onClick = {
                        openNavigation(
                            context,
                            acceptedRequest.destination.ifBlank { acceptedRequest.location }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerDashboardColors.Secondary, contentColor = VolunteerDashboardColors.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Navigate to User", fontWeight = FontWeight.SemiBold)
                }

                // Message + Call
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            openSmsComposer(
                                context = context,
                                rawPhoneNumber = contactPhone,
                                messageBody = "Hi ${acceptedRequest.userName}, I'm your volunteer from Athar and I'm on my way."
                            )
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, VolunteerDashboardColors.Secondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VolunteerDashboardColors.Secondary),
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Message", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { openDialer(context, contactPhone) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, VolunteerDashboardColors.Secondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VolunteerDashboardColors.Secondary),
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Call", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Mark as Completed
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VolunteerDashboardColors.Success, contentColor = VolunteerDashboardColors.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as Completed", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HISTORY TAB
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryTab(
    historyItems: List<HistoryItem>,
    stats: VolunteerStats,
) {
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

    if (historyItems.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Schedule,
            iconTint = VolunteerDashboardColors.Secondary,
            borderColor = VolunteerDashboardColors.Secondary,
            title = "No History Yet",
            message = "Your completed assistance sessions will appear here.",
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Impact Summary Card ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, VolunteerDashboardColors.Gray200, RoundedCornerShape(16.dp))
                .background(VolunteerDashboardColors.White)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("YOUR IMPACT", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.TextLight, letterSpacing = 0.5.sp)
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = VolunteerDashboardColors.Accent, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Total Assists
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.totalAssists}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VolunteerDashboardColors.Secondary)
                    Text("Total Assists", fontSize = 12.sp, color = VolunteerDashboardColors.TextLight)
                }
                // Avg Rating
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = VolunteerDashboardColors.Accent, modifier = Modifier.size(16.dp))
                        Text("${stats.avgRating}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VolunteerDashboardColors.Accent)
                    }
                    Text("Avg Rating", fontSize = 12.sp, color = VolunteerDashboardColors.TextLight)
                }
                // This Week
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stats.thisWeek}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VolunteerDashboardColors.Success)
                    Text("This Week", fontSize = 12.sp, color = VolunteerDashboardColors.TextLight)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── History items ──
        historyItems.forEach { item ->
            HistoryRow(item = item, onClick = { selectedItem = item })
        }

        Spacer(Modifier.height(8.dp))
    }

    selectedItem?.let { item ->
        HistoryDetailDialog(
            item = item,
            onDismiss = { selectedItem = null }
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
) {
    val isCompleted = item.outcome == HistoryOutcome.COMPLETED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, VolunteerDashboardColors.Gray200, RoundedCornerShape(12.dp))
            .background(VolunteerDashboardColors.White)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Status icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(
                    2.dp,
                    if (isCompleted) VolunteerDashboardColors.Success else VolunteerDashboardColors.Gray200,
                    CircleShape
                )
                .background(
                    if (isCompleted) VolunteerDashboardColors.SuccessLight else VolunteerDashboardColors.Gray100,
                    CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (isCompleted) VolunteerDashboardColors.Success else VolunteerDashboardColors.Gray600,
                modifier = Modifier.size(20.dp),
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.userName,
                    fontWeight = FontWeight.SemiBold,
                    color = VolunteerDashboardColors.Secondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (isCompleted && item.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = VolunteerDashboardColors.Accent, modifier = Modifier.size(14.dp))
                        Text("${item.rating}.0", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.AccentDark)
                    }
                }
            }

            Text("${item.helpType} · ${item.location}", fontSize = 13.sp, color = VolunteerDashboardColors.TextLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(item.completedTime, fontSize = 12.sp, color = VolunteerDashboardColors.TextLight.copy(alpha = 0.7f))
        }

        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = VolunteerDashboardColors.Gray200, modifier = Modifier.size(20.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun HistoryDetailDialog(
    item: HistoryItem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = item.userName,
                fontWeight = FontWeight.SemiBold,
                color = VolunteerDashboardColors.Secondary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status: ${if (item.outcome == HistoryOutcome.COMPLETED) "Completed" else "Cancelled"}")
                Text("User Type: ${item.userType}")
                Text("Help Type: ${item.helpType}")
                Text("Location: ${item.location}")
                Text("Time: ${item.completedTime}")
                if (item.outcome == HistoryOutcome.COMPLETED && item.rating > 0) {
                    Text("Rating: ${item.rating}.0")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = VolunteerDashboardColors.White,
        tonalElevation = 0.dp,
    )
}

// REUSABLE SUB-COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════

/** Navy header bar with title + optional subtitle */
@Composable
private fun DashboardHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp)
            .background(VolunteerDashboardColors.Secondary)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp)
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = VolunteerDashboardColors.White)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, fontSize = 14.sp, color = VolunteerDashboardColors.PrimaryGradient.copy(alpha = 0.8f))
        }
    }
}

/** Pulsing green dot */
@Composable
private fun PulsingDot(color: Color, size: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

/** Detail row for accepted-request card */
@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(VolunteerDashboardColors.PrimaryGradient, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = VolunteerDashboardColors.Secondary, modifier = Modifier.size(16.dp))
        }

        Column {
            Text(label, fontSize = 12.sp, color = VolunteerDashboardColors.TextLight)
            Text(value, fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.Secondary, fontSize = 15.sp)
        }
    }
}

/** Stats grid (2x2) used in offline view */
@Composable
private fun StatsGrid(stats: VolunteerStats) {
    data class StatItem(val label: String, val value: String, val icon: ImageVector)

    val statItems = listOf(
        StatItem("Total Assists", "${stats.totalAssists}", Icons.Outlined.Person),
        StatItem("Avg Rating", "${stats.avgRating}", Icons.Outlined.StarOutline),
        StatItem("This Week", "${stats.thisWeek}", Icons.Outlined.Schedule),
        StatItem("Day Streak", "${stats.streak} days", Icons.Outlined.CheckCircle),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (rowIndex in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (colIndex in 0..1) {
                    val stat = statItems[rowIndex * 2 + colIndex]
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, VolunteerDashboardColors.Gray200, RoundedCornerShape(12.dp))
                            .background(VolunteerDashboardColors.White)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(stat.icon, contentDescription = null, tint = VolunteerDashboardColors.Accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stat.label,
                                fontSize = 12.sp,
                                color = VolunteerDashboardColors.TextLight,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stat.value,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VolunteerDashboardColors.Secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** Generic empty-state centered view */
@Composable
private fun EmptyState(
    icon: ImageVector,
    iconTint: Color,
    borderColor: Color,
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(2.dp, borderColor, CircleShape)
                .background(VolunteerDashboardColors.PrimaryGradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = VolunteerDashboardColors.Secondary)

        Spacer(Modifier.height(8.dp))

        Text(
            message,
            fontSize = 16.sp,
            color = VolunteerDashboardColors.TextSecondary.copy(alpha = 0.8f),
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MODIFIER EXTENSION - draw top border
// ═══════════════════════════════════════════════════════════════════════════

private fun Modifier.drawTopBorder(color: Color, width: Float = 2f): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = width,
        )
    }
)


// ═══════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═══════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, widthDp = 400, heightDp = 800, name = "Offline")
@Composable
private fun PreviewOffline() {
    MaterialTheme {
        AtharVolunteerDashboard(isVolunteerLive = false, userName = "Sara Mohammed")
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800, name = "Live - Incoming")
@Composable
private fun PreviewLive() {
    MaterialTheme {
        AtharVolunteerDashboard(isVolunteerLive = true, userName = "Sara Mohammed")
    }
}
