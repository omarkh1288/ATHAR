package com.athar.accessibilitymapping.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.athar.accessibilitymapping.data.AnalyticsRecordStatus
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.VolunteerAnalyticsSnapshot
import com.athar.accessibilitymapping.data.VolunteerAnalyticsEarnings
import com.athar.accessibilitymapping.data.VolunteerAnalyticsPerformance
import com.athar.accessibilitymapping.data.VolunteerAnalyticsReviews
import com.athar.accessibilitymapping.ui.profile.VolunteerReview
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide
import com.athar.accessibilitymapping.ui.profile.AtharVolunteerReviewsSection
import com.athar.accessibilitymapping.ui.theme.*

private val AnalyticsTitleStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Bold,
  fontSize = 28.sp,
  lineHeight = 34.sp
)

private val AnalyticsSectionStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 18.sp,
  lineHeight = 24.sp
)

private val AnalyticsBodyStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp
)

private val AnalyticsLabelStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Medium,
  fontSize = 12.sp,
  lineHeight = 16.sp
)

private val WarningBackground = Color(0xFFFFF8DC)
private val PendingBackground = Color(0xFFFEF3C7)
private val PendingText = Color(0xFFD97706)
private val SuccessBackground = Color(0xFFD1FAE5)

private enum class AnalyticsTab(val label: String, val icon: ImageVector) {
  Overview("Overview", Lucide.ChartColumnBig),
  Earnings("Earnings", Lucide.Wallet),
  Performance("Performance", Lucide.TrendingUp),
  History("History", Lucide.Calendar),
  Reviews("Reviews", Lucide.MessageSquare)
}

private enum class WithdrawalMethod { Bank, Wallet }
private enum class RecordStatus { Completed, Pending }

private data class MonthlyEarning(val month: String, val gross: Double, val net: Double, val fee: Double)
private data class WeeklyRequest(val day: String, val completed: Int)
private data class RequestTypeShare(val name: String, val value: Int, val color: Color)
private data class PaymentRecord(
  val id: String,
  val date: String,
  val user: String,
  val hours: Int,
  val gross: Double,
  val net: Double,
  val status: RecordStatus
)
private data class WithdrawalRecord(val id: String, val date: String, val amount: Double, val method: String, val status: RecordStatus)
private data class ChartPoint(val label: String, val value: Float)

private val monthlyEarnings = listOf(
  MonthlyEarning("Oct", 1200.0, 840.0, 360.0),
  MonthlyEarning("Nov", 1650.0, 1155.0, 495.0),
  MonthlyEarning("Dec", 900.0, 630.0, 270.0),
  MonthlyEarning("Jan", 2100.0, 1470.0, 630.0),
  MonthlyEarning("Feb", 1800.0, 1260.0, 540.0),
  MonthlyEarning("Mar", 2400.0, 1680.0, 720.0)
)

private val weeklyRequests = listOf(
  WeeklyRequest("Sat", 3), WeeklyRequest("Sun", 5), WeeklyRequest("Mon", 2), WeeklyRequest("Tue", 4),
  WeeklyRequest("Wed", 6), WeeklyRequest("Thu", 3), WeeklyRequest("Fri", 1)
)

private val requestTypes = listOf(
  RequestTypeShare("Wheelchair assist", 38, NavyPrimary),
  RequestTypeShare("Visual guidance", 25, AccentGold),
  RequestTypeShare("Hearing support", 18, NavyDark),
  RequestTypeShare("Mobility help", 12, AccentGoldDark),
  RequestTypeShare("Other", 7, TextLight)
)

private val paymentHistory = listOf(
  PaymentRecord("p1", "Mar 8, 2026", "Ahmed Al-Rashid", 2, 150.0, 105.0, RecordStatus.Completed),
  PaymentRecord("p2", "Mar 7, 2026", "Nora Said", 1, 100.0, 70.0, RecordStatus.Completed),
  PaymentRecord("p3", "Mar 6, 2026", "Hassan Mostafa", 3, 225.0, 157.5, RecordStatus.Completed),
  PaymentRecord("p4", "Mar 5, 2026", "Fatima Hassan", 1, 75.0, 52.5, RecordStatus.Completed),
  PaymentRecord("p5", "Mar 4, 2026", "Omar Khalil", 2, 200.0, 140.0, RecordStatus.Pending),
  PaymentRecord("p6", "Mar 3, 2026", "Layla Abdullah", 2, 150.0, 105.0, RecordStatus.Completed),
  PaymentRecord("p7", "Mar 1, 2026", "Youssef Adel", 4, 400.0, 280.0, RecordStatus.Completed)
)

private val withdrawalHistory = listOf(
  WithdrawalRecord("w1", "Mar 1, 2026", 1260.0, "Bank Transfer", RecordStatus.Completed),
  WithdrawalRecord("w2", "Feb 15, 2026", 840.0, "Paymob Wallet", RecordStatus.Completed),
  WithdrawalRecord("w3", "Feb 1, 2026", 630.0, "Bank Transfer", RecordStatus.Completed)
)

@Composable
fun VolunteerAnalyticsScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val repository = remember(context) { AtharRepository(context) }
  var activeTab by rememberSaveable { mutableStateOf(AnalyticsTab.Overview) }
  var showWithdrawDialog by rememberSaveable { mutableStateOf(false) }
  var showFeeInfo by rememberSaveable { mutableStateOf(false) }
  var withdrawAmount by rememberSaveable { mutableStateOf("") }
  var withdrawMethod by rememberSaveable { mutableStateOf(WithdrawalMethod.Bank) }
  var expandedPaymentId by rememberSaveable { mutableStateOf<String?>(null) }
  var analyticsSnapshot by remember { mutableStateOf(VolunteerAnalyticsSnapshot()) }
  var isLoading by remember { mutableStateOf(true) }

  val analyticsEarnings = analyticsSnapshot.earnings
  val analyticsPerformance = analyticsSnapshot.performance
  val analyticsReviews = analyticsSnapshot.reviews
  val monthlyEarningsData = analyticsEarnings.monthlyEarnings.map { MonthlyEarning(it.month, it.gross, it.net, it.fee) }
  val weeklyRequestsData = analyticsPerformance.weeklyActivity.map { WeeklyRequest(it.day, it.completed) }
  val requestTypesData = analyticsPerformance.requestTypes.mapIndexed { index, item ->
    RequestTypeShare(item.name, item.value, requestTypeColor(index))
  }
  val paymentHistoryData = analyticsEarnings.paymentHistory.map { record ->
    PaymentRecord(
      id = record.id,
      date = record.date,
      user = record.user,
      hours = record.hours,
      gross = record.gross,
      net = record.net,
      status = if (record.status == AnalyticsRecordStatus.Completed) RecordStatus.Completed else RecordStatus.Pending
    )
  }
  val withdrawalHistoryData = analyticsEarnings.withdrawalHistory.map { record ->
    WithdrawalRecord(
      id = record.id,
      date = record.date,
      amount = record.amount,
      method = record.method,
      status = if (record.status == AnalyticsRecordStatus.Completed) RecordStatus.Completed else RecordStatus.Pending
    )
  }
  val reviewItems = analyticsReviews.reviews.map { review ->
    VolunteerReview(
      id = review.id,
      userName = review.userName,
      rating = review.rating,
      comment = review.comment,
      date = review.date,
      issues = review.issues
    )
  }

  LaunchedEffect(Unit) {
    isLoading = true
    analyticsSnapshot = repository.getVolunteerAnalyticsSnapshot(perPage = 100)
    isLoading = false
  }

  LaunchedEffect(analyticsSnapshot.warningMessage) {
    val warning = analyticsSnapshot.warningMessage?.trim().orEmpty()
    if (warning.isNotBlank()) {
      Toast.makeText(context, warning, Toast.LENGTH_LONG).show()
    }
  }

  Box(Modifier.fillMaxSize().background(BluePrimary)) {
    LazyColumn(Modifier.fillMaxSize()) {
      item { AnalyticsHeader(onBack) }
      item { AnalyticsTabs(activeTab = activeTab, onSelect = { activeTab = it }) }
      item { Spacer(Modifier.height(16.dp)) }
      item {
        Column(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          when (activeTab) {
            AnalyticsTab.Overview -> OverviewTab(
              totalNet = analyticsEarnings.totalNet,
              currentMonthNet = analyticsEarnings.currentMonthNet,
              currentMonthLabel = analyticsEarnings.currentMonthLabel,
              monthlyChangePercent = analyticsEarnings.monthlyChangePercent,
              completedCount = analyticsPerformance.completed,
              averageRating = analyticsPerformance.averageRating,
              reviewCount = analyticsPerformance.totalReviews,
              pendingCount = analyticsPerformance.pending,
              weeklyRequests = weeklyRequestsData,
              requestTypes = requestTypesData
            )
            AnalyticsTab.Earnings -> EarningsTab(
              availableBalance = analyticsEarnings.availableBalance,
              pendingBalance = analyticsEarnings.pendingBalance,
              totalGross = analyticsEarnings.totalGross,
              totalFees = analyticsEarnings.totalFees,
              totalNet = analyticsEarnings.totalNet,
              monthlyEarnings = monthlyEarningsData,
              withdrawalHistory = withdrawalHistoryData,
              showFeeInfo = showFeeInfo,
              onToggleFeeInfo = { showFeeInfo = !showFeeInfo },
              onWithdraw = { showWithdrawDialog = true }
            )
            AnalyticsTab.Performance -> PerformanceTab(analyticsPerformance)
            AnalyticsTab.History -> HistoryTab(
              thisWeekCompleted = analyticsPerformance.weeklyActivity.sumOf { it.completed },
              thisMonthNet = analyticsEarnings.currentMonthNet,
              paymentHistory = paymentHistoryData,
              expandedPaymentId = expandedPaymentId
            ) { expandedPaymentId = if (expandedPaymentId == it) null else it }
            AnalyticsTab.Reviews -> AtharVolunteerReviewsSection(reviews = reviewItems, isOwnProfile = true)
          }
        }
      }
      item { Spacer(Modifier.height(24.dp)) }
    }

    if (isLoading) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentGold)
      }
    }

    if (showWithdrawDialog) {
      WithdrawDialog(
        availableBalance = analyticsEarnings.availableBalance,
        amount = withdrawAmount,
        method = withdrawMethod,
        onAmountChange = { withdrawAmount = it.filter(Char::isDigit) },
        onMethodChange = { withdrawMethod = it },
        onDismiss = { showWithdrawDialog = false; withdrawAmount = "" },
        onConfirm = { showWithdrawDialog = false; withdrawAmount = "" }
      )
    }
  }
}

@Composable
private fun AnalyticsHeader(onBack: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth().background(NavyPrimary).statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 22.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(NavyDark).clickable(onClick = onBack),
        contentAlignment = Alignment.Center
      ) {
        Icon(Lucide.ArrowLeft, "Go back", tint = Color.White, modifier = Modifier.size(20.dp))
      }
      Column {
        Text("Analytics Dashboard", color = Color.White, style = AnalyticsTitleStyle)
        Text("Track your performance and earnings", color = Color.White.copy(alpha = 0.7f), style = AnalyticsBodyStyle)
      }
    }
  }
}

@Composable
private fun AnalyticsTabs(activeTab: AnalyticsTab, onSelect: (AnalyticsTab) -> Unit) {
  Card(
    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(2.dp, Gray200),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
  ) {
    Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      AnalyticsTab.entries.forEach { tab ->
        val selected = tab == activeTab
        Column(
          modifier = Modifier
            .weight(analyticsTabWeight(tab))
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) NavyPrimary else Color.Transparent)
            .clickable { onSelect(tab) }
            .padding(horizontal = 2.dp, vertical = if (selected) 10.dp else 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Icon(tab.icon, tab.label, tint = if (selected) Color.White else TextLight, modifier = Modifier.size(if (selected) 15.dp else 14.dp))
          Text(
            tab.label,
            color = if (selected) Color.White else TextLight,
            style = AnalyticsLabelStyle.copy(fontSize = if (selected) 11.sp else 10.sp, lineHeight = 12.sp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(2.dp, Gray200),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp), content = content)
  }
}

private fun formatAmount(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
private fun formatFloat(value: Float): String = if (value % 1f == 0f) value.toInt().toString() else String.format("%.1f", value)
private fun requestTypeColor(index: Int): Color = listOf(NavyPrimary, AccentGold, NavyDark, AccentGoldDark, TextLight)[index % 5]
private fun analyticsTabWeight(tab: AnalyticsTab): Float = when (tab) {
  AnalyticsTab.Performance -> 1.3f
  AnalyticsTab.Overview -> 1.05f
  AnalyticsTab.Earnings -> 1f
  AnalyticsTab.History -> 0.85f
  AnalyticsTab.Reviews -> 0.9f
}

@Composable
private fun OverviewTab(
  totalNet: Double,
  currentMonthNet: Double,
  currentMonthLabel: String,
  monthlyChangePercent: Double,
  completedCount: Int,
  averageRating: Float,
  reviewCount: Int,
  pendingCount: Int,
  weeklyRequests: List<WeeklyRequest>,
  requestTypes: List<RequestTypeShare>
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    TwoColumnRow(
      { StatCard(Lucide.DollarSign, "Net Earnings", "${formatAmount(totalNet)} EGP", "All time", AccentGold) },
      { StatCard(Lucide.CircleCheck, "Completed", completedCount.toString(), "Requests", SuccessGreen) }
    )
    TwoColumnRow(
      { StatCard(Lucide.Star, "Avg Rating", formatFloat(averageRating), "$reviewCount reviews", AccentGoldDark) },
      { StatCard(Lucide.Clock, "Pending", pendingCount.toString(), "Requests", WarningGold) }
    )

    SectionCard {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("This Month", color = NavyPrimary, style = AnalyticsSectionStyle)
        Surface(shape = RoundedCornerShape(999.dp), color = BlueSecondary) {
          Text(currentMonthLabel.ifBlank { "Current Month" }, color = NavyPrimary, style = AnalyticsLabelStyle, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
        }
      }
      Spacer(Modifier.height(12.dp))
      Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatAmount(currentMonthNet), color = NavyPrimary, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp)
        Text("EGP net", color = TextLight, style = AnalyticsBodyStyle, modifier = Modifier.padding(bottom = 4.dp))
      }
      Spacer(Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val isPositive = monthlyChangePercent >= 0
        Icon(if (isPositive) Lucide.TrendingUp else Lucide.TrendingDown, null, tint = if (isPositive) SuccessGreen else ErrorRed, modifier = Modifier.size(16.dp))
        Text(
          "${if (isPositive) "+" else ""}${formatAmount(monthlyChangePercent)}% vs last month",
          color = if (isPositive) SuccessGreen else ErrorRed,
          style = AnalyticsBodyStyle
        )
      }
    }

    SectionCard {
      Text("Weekly Activity", color = NavyPrimary, style = AnalyticsSectionStyle)
      Spacer(Modifier.height(12.dp))
      if (weeklyRequests.isEmpty()) {
        Text("No weekly activity yet.", color = TextLight, style = AnalyticsBodyStyle)
      } else {
        BarChart(weeklyRequests.map { ChartPoint(it.day, it.completed.toFloat()) }, NavyPrimary, 160.dp, "")
      }
    }

    SectionCard {
      Text(
        "Request Types",
        color = NavyPrimary,
        style = AnalyticsSectionStyle.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
      )
      Spacer(Modifier.height(18.dp))
      if (requestTypes.isEmpty()) {
        Text("No request type data yet.", color = TextLight, style = AnalyticsBodyStyle)
      } else {
        DonutChart(requestTypes)
      }
    }
  }
}

@Composable
private fun EarningsTab(
  availableBalance: Double,
  pendingBalance: Double,
  totalGross: Double,
  totalFees: Double,
  totalNet: Double,
  monthlyEarnings: List<MonthlyEarning>,
  withdrawalHistory: List<WithdrawalRecord>,
  showFeeInfo: Boolean,
  onToggleFeeInfo: () -> Unit,
  onWithdraw: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(2.dp, NavyDark),
      colors = CardDefaults.cardColors(containerColor = NavyPrimary),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(Modifier.padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Available Balance", color = Color.White.copy(alpha = 0.8f), style = AnalyticsBodyStyle)
          Icon(Lucide.Wallet, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(formatAmount(availableBalance), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp)
          Text("EGP", color = Color.White.copy(alpha = 0.7f), style = AnalyticsBodyStyle, modifier = Modifier.padding(bottom = 4.dp))
        }
        if (pendingBalance > 0) {
          Spacer(Modifier.height(8.dp))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Lucide.Clock, null, tint = AccentGoldLight, modifier = Modifier.size(14.dp))
            Text("${formatAmount(pendingBalance)} EGP pending clearance", color = AccentGoldLight, style = AnalyticsBodyStyle)
          }
        }
        Spacer(Modifier.height(16.dp))
        FilledActionButton("Withdraw Funds", Lucide.ArrowDownToLine, AccentGold, AccentGold, onWithdraw)
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth().animateContentSize(),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(3.dp, AccentGoldDark),
      colors = CardDefaults.cardColors(containerColor = WarningBackground),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
      Column(Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleFeeInfo),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(Modifier.size(40.dp).clip(CircleShape).background(AccentGold), contentAlignment = Alignment.Center) {
            Icon(Lucide.Percent, null, tint = Color.White, modifier = Modifier.size(20.dp))
          }
          Column(Modifier.weight(1f)) {
            Text("Athar Service Fee: 30%", color = NavyPrimary, style = AnalyticsSectionStyle)
            Text("Tap to learn how fees work", color = TextLight, style = AnalyticsBodyStyle)
          }
          Icon(if (showFeeInfo) Lucide.ChevronUp else Lucide.ChevronDown, null, tint = NavyPrimary)
        }
        AnimatedVisibility(visible = showFeeInfo) {
          Column {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = AccentGoldLight, thickness = 2.dp)
            Spacer(Modifier.height(12.dp))
            InfoRow(Lucide.Info, "Athar retains 30% of every completed payment to sustain the platform, cover processing fees, and continuously improve accessibility services.", NavyPrimary)
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.6f)) {
              Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Example Breakdown:", color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.SemiBold))
                BreakdownRow("User pays (2hrs x 75 EGP)", "150 EGP", NavyDark)
                BreakdownRow("Athar fee (30%)", "-45 EGP", ErrorRed)
                HorizontalDivider(color = AccentGoldLight, thickness = 1.dp)
                BreakdownRow("You receive", "105 EGP", SuccessGreenDark, true)
              }
            }
            Spacer(Modifier.height(12.dp))
            InfoRow(Lucide.TriangleAlert, "Fees are deducted before earnings are added to your balance. All displayed earnings are net amounts after the 30% deduction.", TextLight)
          }
        }
      }
    }

    SectionCard {
      Text("Earnings Breakdown", color = NavyPrimary, style = AnalyticsSectionStyle)
      Spacer(Modifier.height(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconAmountRow(Lucide.Banknote, "Total Gross Earnings", "${formatAmount(totalGross)} EGP", NavyPrimary, NavyPrimary)
        IconAmountRow(Lucide.Percent, "Athar Fees (30%)", "-${formatAmount(totalFees)} EGP", ErrorRed, ErrorRed)
        HorizontalDivider(color = Gray200, thickness = 2.dp)
        IconAmountRow(Lucide.DollarSign, "Net Earnings (You Keep)", "${formatAmount(totalNet)} EGP", SuccessGreen, SuccessGreenDark, true)
      }
    }

    SectionCard {
      Text("Monthly Net Earnings", color = NavyPrimary, style = AnalyticsSectionStyle)
      Spacer(Modifier.height(12.dp))
      if (monthlyEarnings.isEmpty()) {
        Text("No monthly earnings yet.", color = TextLight, style = AnalyticsBodyStyle)
      } else {
        BarChart(monthlyEarnings.map { ChartPoint(it.month, it.net.toFloat()) }, SuccessGreen, 180.dp, " EGP")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(SuccessGreen))
          Spacer(Modifier.width(8.dp))
          Text("Net earnings (after 30% fee)", color = TextLight, style = AnalyticsLabelStyle)
        }
      }
    }

    SectionCard {
      Text("Withdrawal History", color = NavyPrimary, style = AnalyticsSectionStyle)
      Spacer(Modifier.height(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (withdrawalHistory.isEmpty()) {
          Text("No withdrawals yet.", color = TextLight, style = AnalyticsBodyStyle)
        } else {
          withdrawalHistory.forEach { record ->
            val bg = if (record.status == RecordStatus.Completed) SuccessBackground else PendingBackground
            val fg = if (record.status == RecordStatus.Completed) SuccessGreenDark else PendingText
            Row(
              modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BlueSecondary).border(2.dp, Gray200, RoundedCornerShape(12.dp)).padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(Modifier.weight(1f)) {
                Text("${formatAmount(record.amount)} EGP", color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.SemiBold))
                Text("${record.date} - ${record.method}", color = TextLight, style = AnalyticsLabelStyle)
              }
              StatusChip(if (record.status == RecordStatus.Completed) "Completed" else "Processing", bg, fg)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PerformanceTab(performance: VolunteerAnalyticsPerformance) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    SectionCard {
      Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(BlueSecondary).border(4.dp, AccentGold, CircleShape), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(performance.grade.ifBlank { "-" }, color = NavyPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text("Grade", color = TextLight, style = AnalyticsLabelStyle)
          }
        }
        Spacer(Modifier.height(12.dp))
        Text(
          performance.headline.ifBlank { "No performance data yet" },
          color = NavyPrimary,
          style = AnalyticsSectionStyle,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
          if (performance.completed > 0 || performance.pending > 0) {
            "You're in the top ${performance.percentile}% of Athar volunteers"
          } else {
            "Complete requests to unlock your volunteer performance score"
          },
          color = TextLight,
          style = AnalyticsBodyStyle,
          textAlign = TextAlign.Center
        )
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      MetricCard("Response Rate", performance.responseRate, 100f, "%", SuccessGreen)
      MetricCard("Completion Rate", performance.completionRate, 100f, "%", NavyPrimary)
      MetricCard("Average Rating", performance.averageRating, 5f, "/5", AccentGold)
      MetricCard("On-Time Rate", performance.onTimeRate, 100f, "%", SuccessGreen)
    }
    SectionCard {
      Text("Detailed Stats", color = NavyPrimary, style = AnalyticsSectionStyle)
      Spacer(Modifier.height(12.dp))
      TwoColumnRow(
        { MiniStat(Lucide.CircleCheck, "Completed", performance.completed.toString(), SuccessGreen) },
        { MiniStat(Lucide.Clock, "Pending", performance.pending.toString(), WarningGold) }
      )
      Spacer(Modifier.height(12.dp))
      TwoColumnRow(
        { MiniStat(Lucide.Users, "Users Helped", performance.usersHelped.toString(), NavyPrimary) },
        { MiniStat(Lucide.ThumbsUp, "Positive Reviews", performance.positiveReviews.toString(), AccentGold) }
      )
      Spacer(Modifier.height(12.dp))
      TwoColumnRow(
        { MiniStat(Lucide.Star, "5-Star Ratings", performance.fiveStarRatings.toString(), AccentGoldDark) },
        { MiniStat(Lucide.Award, "Badges Earned", performance.badges.size.toString(), NavyPrimary) }
      )
    }
    SectionCard {
      Text("Earned Badges", color = NavyPrimary, style = AnalyticsSectionStyle)
      Spacer(Modifier.height(12.dp))
      if (performance.badges.isEmpty()) {
        Text("No badges earned yet.", color = TextLight, style = AnalyticsBodyStyle)
      } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          performance.badges.forEach { badge ->
            Surface(shape = RoundedCornerShape(999.dp), border = BorderStroke(2.dp, AccentGold), color = BlueSecondary) {
              Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Lucide.Award, null, tint = AccentGold, modifier = Modifier.size(14.dp))
                Text(badge, color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Medium))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HistoryTab(
  thisWeekCompleted: Int,
  thisMonthNet: Double,
  paymentHistory: List<PaymentRecord>,
  expandedPaymentId: String?,
  onToggle: (String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    TwoColumnRow(
      { SummaryCard(thisWeekCompleted.toString(), "This Week", NavyPrimary) },
      { SummaryCard("${formatAmount(thisMonthNet)} EGP", "This Month Net", AccentGold) }
    )
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(2.dp, Gray200),
      colors = CardDefaults.cardColors(containerColor = Color.White),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
      Column {
        Text("Payment History", color = NavyPrimary, style = AnalyticsSectionStyle, modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        HorizontalDivider(color = Gray200, thickness = 2.dp)
        if (paymentHistory.isEmpty()) {
          Text(
            "No payment history yet.",
            color = TextLight,
            style = AnalyticsBodyStyle,
            modifier = Modifier.padding(16.dp)
          )
        } else {
          paymentHistory.forEachIndexed { index, payment ->
            PaymentRow(payment = payment, expanded = expandedPaymentId == payment.id, onToggle = { onToggle(payment.id) })
            if (index < paymentHistory.lastIndex) HorizontalDivider(color = Gray200, thickness = 2.dp)
          }
        }
      }
    }
  }
}

@Composable
private fun TwoColumnRow(first: @Composable () -> Unit, second: @Composable () -> Unit) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Box(Modifier.weight(1f)) { first() }
    Box(Modifier.weight(1f)) { second() }
  }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, subtitle: String, color: Color) {
  SectionCard {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
      }
      Text(label, color = TextLight, style = AnalyticsLabelStyle)
    }
    Spacer(Modifier.height(10.dp))
    Text(value, color = NavyPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
    Text(subtitle, color = TextLight, style = AnalyticsLabelStyle)
  }
}

@Composable
private fun SummaryCard(value: String, label: String, valueColor: Color) {
  SectionCard {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp)
      Text(label, color = TextLight, style = AnalyticsBodyStyle.copy(fontSize = 14.sp, lineHeight = 20.sp))
    }
  }
}

@Composable
private fun MetricCard(label: String, value: Float, max: Float, suffix: String, color: Color) {
  val fill = if (max == 0f) 0f else (value / max).coerceIn(0f, 1f)
  SectionCard {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(label, color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Medium))
      Text("${formatFloat(value)}$suffix", color = color, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Bold))
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)).background(BlueSecondary)) {
      Box(Modifier.fillMaxWidth(fill).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(color))
    }
  }
}

@Composable
private fun MiniStat(icon: ImageVector, label: String, value: String, color: Color) {
  Row(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BlueSecondary).padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
      Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
    Column {
      Text(value, color = NavyPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      Text(label, color = TextLight, style = AnalyticsLabelStyle)
    }
  }
}

@Composable
private fun StatusChip(text: String, background: Color, textColor: Color) {
  Surface(shape = RoundedCornerShape(999.dp), color = background) {
    Text(text, color = textColor, style = AnalyticsLabelStyle.copy(fontSize = 11.sp, lineHeight = 13.sp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
  }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, color: Color) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
    Icon(icon, null, tint = AccentGoldDark, modifier = Modifier.padding(top = 2.dp).size(16.dp))
    Text(text, color = color, style = if (color == TextLight) AnalyticsLabelStyle else AnalyticsBodyStyle)
  }
}

@Composable
private fun BreakdownRow(label: String, value: String, color: Color, bold: Boolean = false) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(label, color = if (bold) NavyPrimary else NavyDark, style = AnalyticsBodyStyle.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal))
    Text(value, color = color, style = AnalyticsBodyStyle.copy(fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium))
  }
}

@Composable
private fun IconAmountRow(icon: ImageVector, label: String, value: String, iconTint: Color, valueTint: Color, emphasized: Boolean = false) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
      Text(label, color = NavyDark, style = AnalyticsBodyStyle.copy(fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal))
    }
    Text(value, color = valueTint, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Bold, fontSize = if (emphasized) 18.sp else 14.sp), textAlign = TextAlign.End)
  }
}

@Composable
private fun PaymentRow(payment: PaymentRecord, expanded: Boolean, onToggle: () -> Unit) {
  val bg = if (expanded) BlueSecondary else Color.White
  val statusBg = if (payment.status == RecordStatus.Completed) SuccessBackground else PendingBackground
  val statusFg = if (payment.status == RecordStatus.Completed) SuccessGreenDark else PendingText
  val hourlyRate = if (payment.hours > 0) payment.gross / payment.hours else 0.0

  Column(
    modifier = Modifier.fillMaxWidth().background(bg).clickable(onClick = onToggle).animateContentSize().padding(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(payment.user, color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.SemiBold))
          StatusChip(payment.status.name.lowercase(), statusBg, statusFg)
        }
        Spacer(Modifier.height(4.dp))
        Text("${payment.date} - ${payment.hours}hr${if (payment.hours > 1) "s" else ""}", color = TextLight, style = AnalyticsLabelStyle)
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(horizontalAlignment = Alignment.End) {
          Text("+${formatAmount(payment.net)} EGP", color = SuccessGreen, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Bold))
          Text(
            "${formatAmount(payment.gross)} EGP",
            color = TextLight,
            style = AnalyticsLabelStyle.copy(textDecoration = TextDecoration.LineThrough)
          )
        }
        Icon(if (expanded) Lucide.ChevronUp else Lucide.ChevronDown, null, tint = TextLight)
      }
    }
    AnimatedVisibility(visible = expanded) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).background(BluePrimary).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        BreakdownRow("Gross Amount", "${formatAmount(payment.gross)} EGP", NavyPrimary)
        BreakdownRow("Athar Fee (30%)", "-${formatAmount(payment.gross - payment.net)} EGP", ErrorRed)
        HorizontalDivider(color = Gray200, thickness = 1.dp)
        BreakdownRow("Net (You Received)", "${formatAmount(payment.net)} EGP", SuccessGreenDark, true)
        BreakdownRow("Rate", "${formatAmount(hourlyRate)} EGP/hr x ${payment.hours}hr", TextLight)
      }
    }
  }
}

@Composable
private fun BarChart(data: List<ChartPoint>, color: Color, height: Dp, suffix: String) {
  val maxValue = remember(data) { data.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f }
  val bottomAxisSpace = 24.dp
  val chartHeight = (height - bottomAxisSpace).coerceAtLeast(80.dp)

  Box(Modifier.fillMaxWidth().height(height)) {
    Column(
      Modifier
        .fillMaxWidth()
        .height(chartHeight)
        .padding(bottom = 4.dp)
        .align(Alignment.TopStart),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      repeat(4) { HorizontalDivider(color = Gray200, thickness = 1.dp) }
    }

    Row(
      Modifier
        .fillMaxSize()
        .padding(bottom = bottomAxisSpace),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      data.forEach { point ->
        val barHeight = if (maxValue == 0f) 0.dp else chartHeight * (point.value / maxValue).coerceIn(0f, 1f)
        Column(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Bottom
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(0.78f)
              .widthIn(max = 32.dp)
              .height(barHeight)
              .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
              .background(color)
          )
        }
      }
    }

    Row(
      Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      data.forEach { point ->
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
          Text(
            point.label,
            color = TextLight,
            style = AnalyticsLabelStyle.copy(fontSize = 11.sp, lineHeight = 13.sp),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@Composable
private fun DonutChart(data: List<RequestTypeShare>) {
  val total = data.sumOf { it.value }.coerceAtLeast(1)
  val centerLabelSize = 112.dp
  Column(
    Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    Box(Modifier.size(272.dp), contentAlignment = Alignment.Center) {
      Canvas(Modifier.fillMaxSize()) {
        val ringThickness = size.minDimension * 0.18f
        val arcInset = ringThickness / 2f
        val arcSize = Size(size.width - ringThickness, size.height - ringThickness)
        var startAngle = -90f
        data.forEach { slice ->
          val sweep = 360f * slice.value / total.toFloat()
          drawArc(
            color = slice.color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(arcInset, arcInset),
            size = arcSize,
            style = Stroke(width = ringThickness)
          )
          startAngle += sweep
        }
      }
      Box(Modifier.size(centerLabelSize).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
        Text("$total%", color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
      }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      data.forEach { slice ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
          Text("${slice.name} (${slice.value}%)", color = TextLight, style = AnalyticsLabelStyle.copy(fontSize = 12.sp, lineHeight = 14.sp))
        }
      }
    }
  }
}

@Composable
private fun FilledActionButton(
  text: String,
  icon: ImageVector,
  background: Color,
  borderColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentColor: Color = Color.White
) {
  Row(
    modifier = modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp)).background(background).border(2.dp, borderColor, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text, color = contentColor, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.SemiBold))
  }
}

@Composable
private fun OutlineActionButton(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
  Row(
    modifier = modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, Gray200, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text, color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.SemiBold))
  }
}

@Composable
private fun WithdrawDialog(
  availableBalance: Double,
  amount: String,
  method: WithdrawalMethod,
  onAmountChange: (String) -> Unit,
  onMethodChange: (WithdrawalMethod) -> Unit,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  val amountValue = amount.toDoubleOrNull()
  val canSubmit = amountValue != null && amountValue >= 100.0 && amountValue <= availableBalance

  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.BottomCenter) {
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color.White).padding(24.dp)
      ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(48.dp).height(6.dp).clip(RoundedCornerShape(999.dp)).background(Gray200))
        Spacer(Modifier.height(16.dp))
        Text("Withdraw Funds", color = NavyPrimary, style = AnalyticsSectionStyle)
        Spacer(Modifier.height(4.dp))
        Text("Available balance: ${formatAmount(availableBalance)} EGP", color = TextLight, style = AnalyticsBodyStyle)
        Spacer(Modifier.height(16.dp))
        Text("Amount (EGP)", color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(8.dp))
        InputField(
          value = amount,
          onValueChange = onAmountChange,
          placeholder = "Enter amount",
          trailing = {
            Surface(shape = RoundedCornerShape(8.dp), color = BlueSecondary, modifier = Modifier.clickable { onAmountChange(availableBalance.toInt().toString()) }) {
              Text("MAX", color = NavyPrimary, style = AnalyticsLabelStyle, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
            }
          }
        )
        Spacer(Modifier.height(16.dp))
        Text("Withdrawal Method", color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Medium))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MethodButton("Bank Transfer", method == WithdrawalMethod.Bank, Modifier.weight(1f)) { onMethodChange(WithdrawalMethod.Bank) }
          MethodButton("Paymob Wallet", method == WithdrawalMethod.Wallet, Modifier.weight(1f)) { onMethodChange(WithdrawalMethod.Wallet) }
        }
        Spacer(Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BlueSecondary).padding(12.dp),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Lucide.Info, null, tint = TextLight, modifier = Modifier.padding(top = 2.dp).size(16.dp))
          Text("Minimum withdrawal: 100 EGP. Bank transfers take 2-3 business days. Paymob Wallet transfers are instant.", color = TextLight, style = AnalyticsLabelStyle)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlineActionButton(Modifier.weight(1f), "Cancel", onDismiss)
          FilledActionButton(
            text = "Withdraw",
            icon = Lucide.ArrowDownToLine,
            background = if (canSubmit) AccentGold else Gray200,
            borderColor = if (canSubmit) AccentGold else Gray200,
            contentColor = if (canSubmit) Color.White else TextLight,
            modifier = Modifier.weight(1f),
            onClick = onConfirm
          )
        }
      }
    }
  }
}

@Composable
private fun MethodButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
  Surface(
    modifier = modifier.clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(2.dp, if (selected) AccentGold else Gray200),
    color = if (selected) BlueSecondary else Color.White
  ) {
    Text(text, color = NavyPrimary, style = AnalyticsBodyStyle.copy(fontWeight = FontWeight.Medium), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp))
  }
}

@Composable
private fun InputField(value: String, onValueChange: (String) -> Unit, placeholder: String, trailing: @Composable (() -> Unit)? = null) {
  Box(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, Gray200, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      keyboardOptions = KeyboardOptions.Default,
      textStyle = TextStyle(color = NavyPrimary, fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp),
      cursorBrush = SolidColor(NavyPrimary),
      modifier = Modifier.fillMaxWidth(),
      decorationBox = { inner ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = TextLight, style = AnalyticsBodyStyle)
            inner()
          }
          trailing?.invoke()
        }
      }
    )
  }
}
