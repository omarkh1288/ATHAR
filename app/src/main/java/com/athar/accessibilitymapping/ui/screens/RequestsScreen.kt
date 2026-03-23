package com.athar.accessibilitymapping.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.data.UserRole
import com.athar.accessibilitymapping.data.VolunteerRequest
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.AccentGoldDark
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.BlueSecondary
import com.athar.accessibilitymapping.ui.theme.Gray200
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.payment.AtharPaymentFlow
import com.athar.accessibilitymapping.ui.review.AtharEndOfRideReview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RequestsHeaderStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Bold,
  fontSize = 30.sp,
  lineHeight = 36.sp
)

private val RequestsTabStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Medium,
  fontSize = 16.sp,
  lineHeight = 20.sp
)

private val RequestsNameStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.SemiBold,
  fontSize = 16.sp,
  lineHeight = 22.sp
)

private val RequestsBodyStyle = TextStyle(
  fontFamily = FontFamily.SansSerif,
  fontWeight = FontWeight.Normal,
  fontSize = 14.sp,
  lineHeight = 20.sp
)

@Composable
fun RequestsScreen(
  userRole: UserRole,
  isVolunteerLive: Boolean,
  userName: String
) {
  if (userRole == UserRole.Volunteer) {
    VolunteerDashboardScreen(
      isVolunteerLive = isVolunteerLive,
      userName = userName
    )
    return
  }

  val context = LocalContext.current
  val repository = remember(context) { AtharRepository(context) }
  val coroutineScope = rememberCoroutineScope()

  val requestsViewModel: RequestsViewModel = viewModel()
  requestsViewModel.startPollingIfNeeded()

  val requests by requestsViewModel.requests.collectAsState()
  var activeTab by remember { mutableStateOf("all") }
  var infoMessage by remember { mutableStateOf<String?>(null) }

  var showPaymentFlow by remember { mutableStateOf(false) }
  var showReviewFlow by remember { mutableStateOf(false) }
  var activeRequestForFlow by remember { mutableStateOf<VolunteerRequest?>(null) }

  BackHandler(enabled = showPaymentFlow || showReviewFlow) {
    showPaymentFlow = false
    showReviewFlow = false
  }

  val pendingCount = requests.count { normalizeRequestStatus(it.status) == "pending" }
  val activeCount = requests.count { isActiveRequestStatus(it.status) }
  val historyCount = requests.count {
    val status = normalizeRequestStatus(it.status)
    status == "completed" || status == "cancelled"
  }

  val filteredRequests = requests.filter { request ->
    when (activeTab) {
      "pending" -> normalizeRequestStatus(request.status) == "pending"
      "active" -> isActiveRequestStatus(request.status)
      "history" -> {
        val status = normalizeRequestStatus(request.status)
        status == "completed" || status == "cancelled"
      }
      else -> true
    }
  }

  val tabScrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BluePrimary)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(8.dp)
        .background(NavyPrimary)
        .statusBarsPadding()
        .padding(top = 20.dp, bottom = 20.dp)
    ) {
      Text(
        text = "Volunteer Requests",
        color = Color.White,
        style = RequestsHeaderStyle,
        modifier = Modifier.padding(horizontal = 16.dp)
      )
      if (infoMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = infoMessage ?: "",
          color = Color.White.copy(alpha = 0.9f),
          style = RequestsBodyStyle.copy(fontSize = 13.sp),
          modifier = Modifier.padding(horizontal = 16.dp)
        )
      }
      Spacer(modifier = Modifier.height(if (infoMessage == null) 16.dp else 10.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(tabScrollState)
          .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          Triple("all", "All", 0),
          Triple("pending", "Pending", pendingCount),
          Triple("active", "Active", activeCount),
          Triple("history", "History", historyCount)
        ).forEach { (id, label, count) ->
          val selected = activeTab == id
          Box(
            modifier = Modifier
              .shadow(4.dp, RoundedCornerShape(10.dp))
              .background(if (selected) Color.White else NavyDark, RoundedCornerShape(10.dp))
              .widthIn(min = 64.dp)
              .height(44.dp)
              .clickable { activeTab = id }
              .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Text(
                text = label,
                color = if (selected) NavyPrimary else Color.White,
                style = RequestsTabStyle,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
              )
              if (id != "all") {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .background(AccentGold, RoundedCornerShape(999.dp))
                    .border(1.dp, AccentGoldDark, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = count.toString(),
                    color = Color.White,
                    style = RequestsTabStyle.copy(
                      fontSize = 12.sp,
                      lineHeight = 14.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  )
                }
              }
            }
          }
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      if (filteredRequests.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .background(BlueSecondary, CircleShape)
              .border(2.dp, NavyPrimary, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Chat,
              contentDescription = null,
              tint = NavyPrimary,
              modifier = Modifier.size(40.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "No Requests",
            color = NavyPrimary,
            fontSize = 20.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No requests yet. Create your first request from the Map view.",
            color = NavyPrimary.copy(alpha = 0.8f),
            fontSize = 16.sp,
            lineHeight = 26.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 384.dp)
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Gray200)
            )
          }
          itemsIndexed(filteredRequests, key = { _, item -> item.id }) { _, request ->
            val normalizedStatus = normalizeRequestStatus(request.status)
            val cardInteractionSource = remember(request.id) { MutableInteractionSource() }
            val isCardHovered by cardInteractionSource.collectIsHoveredAsState()

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(if (isCardHovered) BlueSecondary else Color.White)
                .hoverable(interactionSource = cardInteractionSource)
                .padding(16.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(48.dp)
                      .background(NavyPrimary, CircleShape)
                      .border(2.dp, NavyDark, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.PersonOutline,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = request.userName,
                      color = NavyPrimary,
                      style = RequestsNameStyle,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = request.userType,
                      color = NavyPrimary.copy(alpha = 0.8f),
                      style = RequestsBodyStyle,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                val moreInteraction = remember(request.id + "_more") { MutableInteractionSource() }
                val isMoreHovered by moreInteraction.collectIsHoveredAsState()
                Box(
                  modifier = Modifier
                    .background(
                      if (isMoreHovered) BlueSecondary else Color.Transparent,
                      RoundedCornerShape(8.dp)
                    )
                    .hoverable(interactionSource = moreInteraction)
                    .clickable(
                      interactionSource = moreInteraction,
                      indication = null
                    ) {}
                    .padding(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.LocationOn,
                  contentDescription = null,
                  tint = NavyPrimary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = request.location,
                  color = NavyPrimary,
                  style = RequestsBodyStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.AccessTime,
                  contentDescription = null,
                  tint = NavyPrimary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = request.requestTime,
                  color = NavyPrimary.copy(alpha = 0.8f),
                  style = RequestsBodyStyle,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Text(
                text = request.description,
                color = NavyPrimary,
                style = RequestsBodyStyle.copy(
                  fontSize = 15.sp,
                  lineHeight = 22.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusBadge(status = request.status)
                if (normalizedStatus == "pending") {
                  val cancelInteraction = remember(request.id + "_cancel") { MutableInteractionSource() }
                  val isCancelHovered by cancelInteraction.collectIsHoveredAsState()
                  Button(
                    onClick = {
                      coroutineScope.launch {
                        repository.cancelRequest(request.id)
                        requestsViewModel.refreshNow()
                      }
                    },
                    interactionSource = cancelInteraction,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isCancelHovered) Color(0xFFFEE2E2) else Color.White,
                      contentColor = Color(0xFFDC2626)
                    ),
                    border = BorderStroke(2.dp, Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp)
                  ) {
                    Text(
                      text = "Cancel",
                      color = Color(0xFFDC2626),
                      style = RequestsBodyStyle,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }

              if (request.volunteerName != null) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(2.dp)
                      .background(Gray200)
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Outlined.CheckCircle,
                      contentDescription = null,
                      tint = Color(0xFF10B981),
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Volunteer: ",
                      color = NavyPrimary,
                      style = RequestsBodyStyle
                    )
                    Text(
                      text = request.volunteerName,
                      color = NavyPrimary,
                      style = RequestsBodyStyle,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }

                  if (requiresPayment(request.status) && request.paymentMethod.lowercase() != "cash") {
                    val contactInteraction = remember(request.id + "_contact") { MutableInteractionSource() }
                    val isContactHovered by contactInteraction.collectIsHoveredAsState()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Button(
                        onClick = {
                          activeRequestForFlow = request
                          showPaymentFlow = true
                        },
                        interactionSource = contactInteraction,
                        colors = ButtonDefaults.buttonColors(
                          containerColor = AccentGold,
                          contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, AccentGoldDark),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                          .weight(1f)
                          .height(56.dp)
                          .shadow(6.dp, RoundedCornerShape(10.dp))
                      ) {
                        Text(
                          text = "Pay for Service",
                          style = RequestsBodyStyle.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                          ),
                          fontWeight = FontWeight.SemiBold,
                        )
                      }
                      Button(
                        onClick = {
                          infoMessage = "Volunteer contact details are private in this MVP."
                        },
                        colors = ButtonDefaults.buttonColors(
                          containerColor = NavyPrimary,
                          contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, NavyDark),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier
                          .weight(1f)
                          .height(56.dp)
                          .shadow(6.dp, RoundedCornerShape(10.dp))
                      ) {
                        Text(
                          text = "Contact",
                          style = RequestsBodyStyle.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                          ),
                          fontWeight = FontWeight.SemiBold,
                        )
                      }
                    }
                  }

                  if (normalizedStatus == "completed") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                      onClick = {
                        activeRequestForFlow = request
                        showReviewFlow = true
                      },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = NavyPrimary
                      ),
                      border = BorderStroke(2.dp, NavyPrimary),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                    ) {
                      Text(
                        text = "Rate Experience",
                        style = RequestsBodyStyle.copy(
                          fontSize = 16.sp,
                          lineHeight = 22.sp
                        ),
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }
                }
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Gray200)
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(BlueSecondary)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .background(NavyPrimary)
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.PersonOutline,
          contentDescription = null,
          tint = NavyPrimary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Want to help?",
          color = NavyPrimary,
          style = RequestsBodyStyle.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp
          ),
          fontWeight = FontWeight.SemiBold,
          maxLines = 1
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Enable volunteer mode",
          color = AccentGold,
          style = RequestsBodyStyle.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp
          ),
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .clickable {
              infoMessage = "Enable volunteer mode from Account Settings."
            }
        )
      }
    }
  }

  if (showPaymentFlow && activeRequestForFlow != null) {
    val request = activeRequestForFlow!!
    AtharPaymentFlow(
      volunteerName = request.volunteerName ?: "Sara Mohammed",
      serviceName = if (request.description.isBlank()) "Assistance" else request.description,
      location = request.location,
      date = "Mar 6, 2026",
      initialPricePerHour = request.pricePerHour,
      initialHours = request.hours,
      initialTotalAmountEgp = request.totalAmountEgp,
      onSubmitPayment = { method ->
        val amountEgp = request.totalAmountEgp?.coerceAtLeast(1)
          ?: (request.hours * request.pricePerHour).coerceAtLeast(1)
        val result = repository.payRequest(request.id, method, amountEgp)
        requestsViewModel.refreshNow()
        when (result) {
          is ApiCallResult.Success -> infoMessage = result.data.message
          is ApiCallResult.Failure -> infoMessage = result.message
        }
        result
      },
      onConfirmPaymobPayment = { paymentId ->
        val result = repository.confirmPaymobPayment(paymentId)
        requestsViewModel.refreshNow()
        when (result) {
          is ApiCallResult.Success -> infoMessage = result.data.message
          is ApiCallResult.Failure -> infoMessage = result.message
        }
        result
      },
      onGetPaymentStatus = { paymentId ->
        val result = repository.getPaymentStatus(paymentId)
        requestsViewModel.refreshNow()
        result
      },
      onRefreshPaymentStatus = { paymentId ->
        val result = repository.refreshPayment(paymentId)
        requestsViewModel.refreshNow()
        result
      },
      skipNotification = true,
      onComplete = { showPaymentFlow = false },
      onTrackVolunteer = { showPaymentFlow = false },
      onBackToHome = { showPaymentFlow = false },
      onClose = { showPaymentFlow = false }
    )
  }

  if (showReviewFlow && activeRequestForFlow != null) {
    AtharEndOfRideReview(
      volunteerName = activeRequestForFlow?.volunteerName ?: "Sara Mohammed",
      onSubmit = { rating, comment, issues, reportText ->
        val requestId = activeRequestForFlow?.id
        showReviewFlow = false
        if (requestId == null) {
          infoMessage = "Unable to submit review right now."
        } else {
          coroutineScope.launch {
            when (
              val result = repository.rateVolunteerRequest(
                requestId = requestId,
                rating = rating,
                comment = comment.ifBlank { reportText.ifBlank { null } },
                issues = issues
              )
            ) {
              is ApiCallResult.Success -> {
                requestsViewModel.refreshNow()
                infoMessage = result.data.message.ifBlank { "Thank you for rating $rating stars!" }
              }
              is ApiCallResult.Failure -> {
                infoMessage = result.message
              }
            }
          }
        }
      },
      onSkip = { showReviewFlow = false },
      onBackToHome = { showReviewFlow = false }
    )
  }
}

@Composable
private fun StatusBadge(status: String) {
  val normalizedStatus = normalizeRequestStatus(status)
  val background = when (normalizedStatus) {
    "pending" -> Color(0xFFFFF8DC)
    "accepted" -> BlueSecondary
    "active" -> Color(0xFFD1FAE5)
    "completed" -> Color(0xFFD1FAE5)
    "cancelled" -> Color(0xFFF3F4F6)
    else -> Color(0xFFF3F4F6)
  }
  val border = when (normalizedStatus) {
    "pending" -> AccentGold
    "accepted" -> NavyPrimary
    "active" -> Color(0xFF10B981)
    "completed" -> Color(0xFF10B981)
    "cancelled" -> Gray200
    else -> Gray200
  }
  val textColor = when (normalizedStatus) {
    "pending" -> AccentGoldDark
    "accepted" -> NavyPrimary
    "active" -> Color(0xFF065F46)
    "completed" -> Color(0xFF065F46)
    "cancelled" -> NavyPrimary
    else -> NavyPrimary
  }
  val iconColor = when (normalizedStatus) {
    "active",
    "completed" -> Color(0xFF10B981)
    else -> textColor
  }

  Row(
    modifier = Modifier
      .background(background, RoundedCornerShape(8.dp))
      .border(2.dp, border, RoundedCornerShape(8.dp))
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val icon = when (normalizedStatus) {
      "pending" -> Icons.Outlined.AccessTime
      "accepted" -> Icons.Outlined.PersonOutline
      "active" -> Icons.Outlined.CheckCircle
      "completed" -> Icons.Outlined.CheckCircle
      else -> Icons.Outlined.Close
    }
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconColor,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = normalizedStatus.replaceFirstChar { it.uppercase() },
      color = textColor,
      style = RequestsBodyStyle.copy(
        lineHeight = 18.sp
      ),
      fontWeight = FontWeight.SemiBold,
    )
  }
}

private fun normalizeRequestStatus(raw: String): String {
  return when (raw.lowercase()) {
    "created", "broadcasted", "pending" -> "pending"
    "accepted", "pending_payment" -> "accepted"
    "active", "inprogress", "in_progress", "confirmed" -> "active"
    "completed", "rated", "archived" -> "completed"
    "cancelled", "novolunteer", "no_volunteer" -> "cancelled"
    else -> raw.lowercase()
  }
}

private fun isActiveRequestStatus(raw: String): Boolean {
  return normalizeRequestStatus(raw) in setOf("accepted", "active")
}

private fun requiresPayment(raw: String): Boolean {
  return normalizeRequestStatus(raw) == "accepted"
}

