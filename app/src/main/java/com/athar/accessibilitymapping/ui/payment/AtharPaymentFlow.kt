package com.athar.accessibilitymapping.ui.payment
import com.athar.accessibilitymapping.ui.theme.ssp

import com.athar.accessibilitymapping.ui.theme.sdp

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ATHAR - Payment Flow with Paymob Gateway (Jetpack Compose)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Pixel-perfect Kotlin translation of the web PaymentFlow.tsx.
 *
 * The payment screen is READ-ONLY — hours and price were already set by
 * the user when creating the help request via AtharHelpRequestForm.
 * The volunteer sees all details before accepting. After acceptance,
 * the user just reviews the summary and confirms payment via Paymob.
 *
 * Three-step flow:
 *   1. Volunteer Accepted notification (with all details + amount summary)
 *   2. Read-only Order Summary → "Pay with Paymob" redirect button
 *   3. Payment Confirmation with booking reference + CTAs
 *
 * ── PLACEMENT ──
 *   app/src/main/java/com/athar/app/ui/payment/AtharPaymentFlow.kt
 *
 * ── DEPENDENCIES (build.gradle.kts :app) ──
 *   implementation(platform("androidx.compose:compose-bom:2024.02.00"))
 *   implementation("androidx.compose.ui:ui")
 *   implementation("androidx.compose.ui:ui-graphics")
 *   implementation("androidx.compose.ui:ui-tooling-preview")
 *   implementation("androidx.compose.material3:material3")
 *   implementation("androidx.compose.material:material-icons-extended")
 *   implementation("androidx.compose.foundation:foundation")
 *   implementation("androidx.compose.animation:animation")
 *
 * ── USAGE ──
 *   AtharPaymentFlow(
 *       volunteerName = "Sara Mohammed",
 *       serviceName = "Mobility Assistance",
 *       location = "Central Mall entrance",
 *       destination = "Central Mall - Level 2",
 *       date = "Mar 6, 2026",
 *       hours = 2,
 *       pricePerHour = 75,
 *       onComplete = { },
 *       onTrackVolunteer = { },
 *       onBackToHome = { },
 *       onClose = { },
 *   )
 *
 * ── PAYMOB NOTE ──
 *   In production, the "Pay with Paymob" button should:
 *   1. Call your backend → Paymob auth token → order → payment key
 *   2. Launch Paymob's hosted checkout via Android Custom Tabs / WebView
 *   3. Handle callback/redirect to confirm payment
 *   Currently simulates the redirect with a 2.5s delay for demo purposes.
 */

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.athar.accessibilitymapping.data.ApiActionResult
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.ApiPayRequestResponse
import com.athar.accessibilitymapping.data.ApiPaymentStatus
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    val Success = Color(0xFF10B981)
    val SuccessLight = Color(0xFFD1FAE5)
    val SuccessText = Color(0xFF065F46)
    val Gray100 = Color(0xFFF1F5F9)
    val Gray200 = Color(0xFFE2E8F0)
    val WarningBg = Color(0xFFFFF8DC)
    val White = Color.White
}

private enum class PaymentStep { NOTIFICATION, PAYMENT, REDIRECTING, CONFIRMATION }

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AtharPaymentFlow(
    volunteerName: String,
    serviceName: String,
    location: String,
    destination: String = "",
    date: String,
    initialHours: Int = 1,
    initialPricePerHour: Int = 50,
    initialTotalAmountEgp: Int? = null,
    onSubmitPayment: suspend (method: String) -> ApiCallResult<ApiPayRequestResponse>,
    onConfirmPaymobPayment: suspend (paymentId: String) -> ApiCallResult<ApiActionResult>,
    onGetPaymentStatus: suspend (paymentId: String) -> ApiCallResult<ApiPaymentStatus>,
    onRefreshPaymentStatus: suspend (paymentId: String) -> ApiCallResult<ApiPaymentStatus>,
    onComplete: () -> Unit,
    onTrackVolunteer: () -> Unit,
    onBackToHome: () -> Unit,
    onClose: () -> Unit,
    skipNotification: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var step by remember { mutableStateOf(if (skipNotification) PaymentStep.PAYMENT else PaymentStep.NOTIFICATION) }
    var selectedPaymentLabel by remember { mutableStateOf("Paymob") }
    var paymentMessage by remember { mutableStateOf<String?>(null) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var activePaymentId by remember { mutableStateOf<String?>(null) }
    var activeCheckoutUrl by remember { mutableStateOf<String?>(null) }
    var activePaymentStatus by remember { mutableStateOf<ApiPaymentStatus?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var hasOpenedCheckout by remember { mutableStateOf(false) }
    val hours = initialHours.coerceAtLeast(1)
    val pricePerHour = normalizePaymentPricePerHour(hours, initialPricePerHour)
    val total = normalizePaymentTotalAmount(hours, pricePerHour, initialTotalAmountEgp)
        ?: (hours * pricePerHour).coerceAtLeast(1)
    val bookingRef = remember {
        "ATH-" + System.currentTimeMillis().toString(36).uppercase().takeLast(8)
    }

    LaunchedEffect(step, activeCheckoutUrl, hasOpenedCheckout) {
        if (step == PaymentStep.REDIRECTING && !hasOpenedCheckout) {
            activeCheckoutUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { checkoutUrl ->
                    hasOpenedCheckout = true
                    runCatching { uriHandler.openUri(checkoutUrl) }
                }
        }
    }

    fun openCheckout() {
        val checkoutUrl = activeCheckoutUrl
        if (checkoutUrl.isNullOrBlank()) {
            paymentError = "Paymob checkout link is unavailable."
            return
        }
        paymentError = null
        paymentMessage = "Opening Paymob checkout."
        runCatching { uriHandler.openUri(checkoutUrl) }
            .onFailure { paymentError = "Unable to open Paymob checkout on this device." }
    }

    fun submitPayment(method: String) {
        if (isSubmitting) return
        coroutineScope.launch {
            isSubmitting = true
            paymentError = null
            paymentMessage = null
            if (method == "CARD") {
                activePaymentId = null
                activeCheckoutUrl = null
                activePaymentStatus = null
                hasOpenedCheckout = false
            }

            when (val result = onSubmitPayment(method)) {
                is ApiCallResult.Success -> {
                    val payment = result.data
                    selectedPaymentLabel = paymentLabelFor(payment.paymentMethod)
                    paymentMessage = payment.message.ifBlank { null }
                    activePaymentId = payment.paymentId
                    activeCheckoutUrl = payment.checkoutUrl
                    activePaymentStatus = null
                    hasOpenedCheckout = false

                    if (method == "CASH") {
                        step = PaymentStep.CONFIRMATION
                    } else {
                        val paymentId = payment.paymentId
                        val checkoutUrl = payment.checkoutUrl
                        if (checkoutUrl.isNullOrBlank()) {
                            paymentError = "Paymob checkout link is unavailable."
                            step = PaymentStep.PAYMENT
                        } else {
                            if (!paymentId.isNullOrBlank()) {
                                when (val statusResult = onGetPaymentStatus(paymentId)) {
                                    is ApiCallResult.Success -> {
                                        activePaymentStatus = statusResult.data
                                        paymentMessage = paymentStatusSummary(statusResult.data)
                                        val gatewayAmount = statusResult.data.amount
                                        if (gatewayAmount > 0.0 && abs(gatewayAmount - total.toDouble()) > 0.99) {
                                            paymentError = "Gateway amount mismatch. Expected $total EGP, got ${gatewayAmount.toInt()} EGP."
                                            step = PaymentStep.PAYMENT
                                        } else if (statusResult.data.success) {
                                            step = PaymentStep.CONFIRMATION
                                        } else {
                                            step = PaymentStep.REDIRECTING
                                        }
                                    }
                                    is ApiCallResult.Failure -> {
                                        step = PaymentStep.REDIRECTING
                                        paymentError = statusResult.message
                                    }
                                }
                            } else {
                                step = PaymentStep.REDIRECTING
                                paymentMessage = paymentMessage
                                    ?: "Complete payment in Paymob, then return to the app."
                            }
                        }
                    }
                }
                is ApiCallResult.Failure -> {
                    paymentError = result.message
                }
            }
            isSubmitting = false
        }
    }

    fun refreshPaymentStatus() {
        val paymentId = activePaymentId
        if (paymentId.isNullOrBlank() || isSubmitting) return
        coroutineScope.launch {
            isSubmitting = true
            paymentError = null
            when (val result = onRefreshPaymentStatus(paymentId)) {
                is ApiCallResult.Success -> {
                    activePaymentStatus = result.data
                    paymentMessage = paymentStatusSummary(result.data)
                    if (result.data.success) {
                        step = PaymentStep.CONFIRMATION
                    }
                }
                is ApiCallResult.Failure -> {
                    paymentError = result.message
                }
            }
            isSubmitting = false
        }
    }

    fun confirmPaymobPayment() {
        val paymentId = activePaymentId
        if (paymentId.isNullOrBlank() || isSubmitting) return
        coroutineScope.launch {
            isSubmitting = true
            paymentError = null
            when (val confirmResult = onConfirmPaymobPayment(paymentId)) {
                is ApiCallResult.Success -> {
                    paymentMessage = confirmResult.data.message.ifBlank { "Payment updated." }
                    when (val statusResult = onRefreshPaymentStatus(paymentId)) {
                        is ApiCallResult.Success -> {
                            activePaymentStatus = statusResult.data
                            paymentMessage = paymentStatusSummary(statusResult.data)
                            if (statusResult.data.success) {
                                step = PaymentStep.CONFIRMATION
                            } else {
                                paymentError = "Payment is still pending in Paymob."
                            }
                        }
                        is ApiCallResult.Failure -> {
                            paymentError = statusResult.message
                        }
                    }
                }
                is ApiCallResult.Failure -> {
                    paymentError = confirmResult.message
                }
            }
            isSubmitting = false
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onClose,
                ),
            contentAlignment = if (step == PaymentStep.NOTIFICATION) Alignment.Center else Alignment.BottomCenter,
        ) {
            when (step) {
                PaymentStep.NOTIFICATION -> NotificationScreen(
                    volunteerName, serviceName, location, destination, date,
                    hours, pricePerHour, total,
                    onProceed = { step = PaymentStep.PAYMENT },
                    onClose = onClose,
                )
                PaymentStep.PAYMENT, PaymentStep.REDIRECTING -> ReadOnlyPaymentScreen(
                    volunteerName, serviceName, location, destination, date,
                    hours, pricePerHour, total,
                    isRedirecting = step == PaymentStep.REDIRECTING,
                    isSubmitting = isSubmitting,
                    paymentMessage = paymentMessage,
                    paymentError = paymentError,
                    paymentStatus = activePaymentStatus,
                    paymentId = activePaymentId,
                    onPayWithPaymob = { submitPayment("CARD") },
                    onOpenPaymobCheckout = ::openCheckout,
                    onConfirmPaymobPayment = ::confirmPaymobPayment,
                    onRefreshPaymentStatus = ::refreshPaymentStatus,
                    onBack = {
                        paymentError = null
                        step = if (step == PaymentStep.REDIRECTING) PaymentStep.PAYMENT else PaymentStep.NOTIFICATION
                    },
                    onClose = onClose,
                )
                PaymentStep.CONFIRMATION -> ConfirmationScreen(
                    volunteerName, location, destination, date,
                    hours, pricePerHour, total, bookingRef, selectedPaymentLabel,
                    onTrackVolunteer = {
                        onComplete()
                        onTrackVolunteer()
                    },
                    onBackToHome = {
                        onComplete()
                        onBackToHome()
                    },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN 1: VOLUNTEER ACCEPTED — NOTIFICATION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NotificationScreen(
    volunteerName: String, serviceName: String, location: String,
    destination: String, date: String, hours: Int, pricePerHour: Int, total: Int,
    onProceed: () -> Unit, onClose: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 24.sdp)
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null) {},
        shape = RoundedCornerShape(20.sdp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.sdp),
    ) {
        Column {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AtharColors.Secondary)
                    .padding(horizontal = 20.sdp, vertical = 24.sdp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.sdp)
                        .clip(CircleShape)
                        .background(AtharColors.Accent)
                        .border(3.sdp, AtharColors.AccentLight, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = AtharColors.White, modifier = Modifier.size(32.sdp))
                }
                Spacer(Modifier.height(12.sdp))
                Text("Volunteer Accepted!", color = AtharColors.White, fontSize = 20.ssp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.sdp))
                Text("Please complete payment to confirm your booking.", color = AtharColors.Primary, fontSize = 14.ssp, textAlign = TextAlign.Center)
            }

            // Body
            Column(
                modifier = Modifier.padding(20.sdp),
                verticalArrangement = Arrangement.spacedBy(14.sdp),
            ) {
                // Volunteer info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.sdp))
                        .background(AtharColors.Primary)
                        .border(2.sdp, AtharColors.Secondary, RoundedCornerShape(12.sdp))
                        .padding(14.sdp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.sdp),
                ) {
                    Box(
                        modifier = Modifier.size(44.sdp).clip(CircleShape).background(AtharColors.Secondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, null, tint = AtharColors.White, modifier = Modifier.size(22.sdp))
                    }
                    Column {
                        Text("Your Volunteer", color = AtharColors.TextLight, fontSize = 12.ssp)
                        Text(volunteerName, color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                    }
                }

                DetailRow(Icons.Filled.Schedule, "Service", serviceName)
                DetailRow(Icons.Filled.CalendarToday, "Date", date)
                DetailRow(Icons.Filled.LocationOn, "From", location)
                if (destination.isNotBlank()) DetailRow(Icons.Filled.Navigation, "To", destination)

                // Amount summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.sdp))
                        .background(AtharColors.Primary)
                        .border(2.sdp, AtharColors.Secondary, RoundedCornerShape(12.sdp))
                        .padding(14.sdp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("$hours ${if (hours == 1) "hour" else "hours"} × $pricePerHour EGP", color = AtharColors.Secondary, fontSize = 13.ssp)
                    Spacer(Modifier.height(4.sdp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$total", color = AtharColors.Accent, fontSize = 30.ssp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.sdp))
                        Text("EGP", color = AtharColors.Accent, fontSize = 16.ssp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.sdp))
                    }
                }

                Button(
                    onClick = onProceed,
                    modifier = Modifier.fillMaxWidth().height(52.sdp),
                    colors = ButtonDefaults.buttonColors(containerColor = AtharColors.Accent),
                    shape = RoundedCornerShape(14.sdp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.sdp),
                ) {
                    Text("Proceed to Payment", fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                }

                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Remind me later", color = AtharColors.TextLight, fontSize = 14.ssp)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.sdp)) {
        Icon(icon, null, tint = AtharColors.Accent, modifier = Modifier.size(20.sdp).padding(top = 2.sdp))
        Column {
            Text(label, color = AtharColors.TextLight, fontSize = 12.ssp)
            Text(value, color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 14.ssp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN 2: READ-ONLY ORDER SUMMARY + PAY WITH PAYMOB
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ReadOnlyPaymentScreen(
    volunteerName: String, serviceName: String, location: String,
    destination: String, date: String, hours: Int, pricePerHour: Int, total: Int,
    isRedirecting: Boolean,
    isSubmitting: Boolean,
    paymentMessage: String?,
    paymentError: String?,
    paymentStatus: ApiPaymentStatus?,
    paymentId: String?,
    onPayWithPaymob: () -> Unit,
    onOpenPaymobCheckout: () -> Unit,
    onConfirmPaymobPayment: () -> Unit,
    onRefreshPaymentStatus: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .clickable(remember { MutableInteractionSource() }, null) {},
        shape = RoundedCornerShape(topStart = 24.sdp, topEnd = 24.sdp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.sdp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(start = 24.sdp, end = 24.sdp, top = 16.sdp, bottom = 12.sdp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.sdp).height(5.sdp)
                        .clip(RoundedCornerShape(3.sdp))
                        .background(AtharColors.Gray200),
                )
                Spacer(Modifier.height(16.sdp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.sdp)) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.sdp).clip(CircleShape).background(AtharColors.Gray100),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AtharColors.Secondary, modifier = Modifier.size(20.sdp))
                        }
                        Text("Confirm Payment", color = AtharColors.Secondary, fontSize = 20.ssp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.sdp).clip(CircleShape).background(AtharColors.Gray100),
                    ) {
                        Icon(Icons.Filled.Close, "Close", tint = AtharColors.Secondary, modifier = Modifier.size(20.sdp))
                    }
                }
            }

            HorizontalDivider(color = AtharColors.Gray200, thickness = 2.sdp)

            // Scrollable READ-ONLY content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(24.sdp),
                verticalArrangement = Arrangement.spacedBy(20.sdp),
            ) {
                // Volunteer badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.sdp))
                        .background(AtharColors.Primary)
                        .border(2.sdp, AtharColors.Gray200, RoundedCornerShape(16.sdp))
                        .padding(16.sdp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.sdp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AtharColors.Secondary, AtharColors.SecondaryDark)))
                            .border(2.sdp, AtharColors.SecondaryDark, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, null, tint = AtharColors.White, modifier = Modifier.size(24.sdp))
                    }
                    Spacer(Modifier.width(12.sdp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(volunteerName, color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                        Text(serviceName, color = AtharColors.TextLight, fontSize = 13.ssp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.sdp))
                    Text(
                        if (isRedirecting) "Paymob" else "Accepted",
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AtharColors.SuccessLight)
                            .border(2.sdp, AtharColors.Success, RoundedCornerShape(50))
                            .padding(horizontal = 12.sdp, vertical = 4.sdp),
                        color = AtharColors.SuccessText, fontSize = 11.ssp, fontWeight = FontWeight.SemiBold,
                    )
                }

                // ── Read-Only Order Details ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.sdp))
                        .border(2.sdp, AtharColors.Gray200, RoundedCornerShape(16.sdp))
                        .padding(16.sdp),
                    verticalArrangement = Arrangement.spacedBy(12.sdp),
                ) {
                    Text("Order Details", color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 14.ssp)
                    SummaryRow("From", location)
                    if (destination.isNotBlank()) SummaryRow("To", destination)
                    SummaryRow("Service", serviceName)
                    SummaryRow("Date", date)
                    SummaryRow("Hours", "$hours ${if (hours == 1) "hour" else "hours"}")
                    SummaryRow("Rate", "$pricePerHour EGP/hr")
                }

                // ── Read-Only Total ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.sdp))
                        .background(AtharColors.Primary)
                        .border(2.sdp, AtharColors.Secondary, RoundedCornerShape(16.sdp))
                        .padding(20.sdp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Total Amount", color = AtharColors.TextLight, fontSize = 13.ssp)
                    Spacer(Modifier.height(4.sdp))
                    Text("$hours ${if (hours == 1) "hour" else "hours"} × $pricePerHour EGP", color = AtharColors.Secondary, fontSize = 14.ssp)
                    Spacer(Modifier.height(8.sdp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$total", color = AtharColors.Accent, fontSize = 36.ssp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.sdp))
                        Text("EGP", color = AtharColors.Accent, fontSize = 18.ssp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.sdp))
                    }
                }

                // ── Paymob Security Badge ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.sdp))
                        .background(AtharColors.Gray100)
                        .border(2.sdp, AtharColors.Gray200, RoundedCornerShape(16.sdp))
                        .padding(14.sdp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.sdp),
                ) {
                    Box(
                        modifier = Modifier.size(40.sdp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF0A2540), Color(0xFF1A3A5C)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = AtharColors.White, modifier = Modifier.size(20.sdp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Secured by Paymob", color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 14.ssp)
                        Text(
                            if (isRedirecting) "Open the checkout, finish the payment, then confirm it below."
                            else "You'll be redirected to Paymob's secure checkout.",
                            color = AtharColors.TextLight,
                            fontSize = 11.ssp
                        )
                    }
                    Icon(Icons.Filled.Lock, null, tint = AtharColors.TextLight, modifier = Modifier.size(16.sdp))
                }

                paymentMessage?.let { message ->
                    PaymentFeedbackCard(
                        text = message,
                        background = AtharColors.SuccessLight,
                        border = AtharColors.Success,
                        textColor = AtharColors.SuccessText,
                        icon = Icons.Filled.CheckCircle
                    )
                }

                paymentError?.let { error ->
                    PaymentFeedbackCard(
                        text = error,
                        background = Color(0xFFFFE4E6),
                        border = Color(0xFFE11D48),
                        textColor = Color(0xFF9F1239),
                        icon = Icons.Filled.Error
                    )
                }

                if (paymentStatus != null || !paymentId.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.sdp))
                            .background(AtharColors.White)
                            .border(2.sdp, AtharColors.Gray200, RoundedCornerShape(16.sdp))
                            .padding(16.sdp),
                        verticalArrangement = Arrangement.spacedBy(8.sdp),
                    ) {
                        Text("Payment Status", color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 14.ssp)
                        paymentId?.takeIf { it.isNotBlank() }?.let { SummaryRow("Payment ID", it) }
                        paymentStatus?.let { status ->
                            SummaryRow("Status", status.status.replaceFirstChar { it.uppercase() })
                            SummaryRow("Amount", "${status.amount.toInt()} ${status.currency}")
                        }
                    }
                }

                // ── Pay with Paymob Button ──
                Button(
                    onClick = if (isRedirecting) onOpenPaymobCheckout else onPayWithPaymob,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(56.sdp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRedirecting) AtharColors.SecondaryDark else AtharColors.Accent,
                        disabledContainerColor = AtharColors.Secondary,
                        disabledContentColor = AtharColors.White,
                    ),
                    shape = RoundedCornerShape(16.sdp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.sdp),
                ) {
                    when {
                        isSubmitting -> {
                            CircularProgressIndicator(modifier = Modifier.size(20.sdp), color = AtharColors.White, strokeWidth = 2.sdp)
                            Spacer(Modifier.width(10.sdp))
                            Text("Processing payment...", fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                        }
                        isRedirecting -> {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(20.sdp))
                            Spacer(Modifier.width(10.sdp))
                            Text("Open Paymob Checkout", fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                        }
                        else -> {
                            Icon(Icons.Filled.CreditCard, null, modifier = Modifier.size(20.sdp))
                            Spacer(Modifier.width(10.sdp))
                            Text("Pay $total EGP with Paymob", fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                            Spacer(Modifier.width(6.sdp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.sdp), tint = AtharColors.White.copy(alpha = 0.7f))
                        }
                    }
                }

                if (isRedirecting) {
                    Button(
                        onClick = onConfirmPaymobPayment,
                        enabled = !isSubmitting && !paymentId.isNullOrBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.sdp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AtharColors.Accent,
                            disabledContainerColor = AtharColors.Gray200,
                            disabledContentColor = AtharColors.TextLight,
                        ),
                        shape = RoundedCornerShape(16.sdp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.sdp),
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(20.sdp))
                        Spacer(Modifier.width(10.sdp))
                        Text("I've Completed Payment", fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                    }

                    OutlinedButton(
                        onClick = onRefreshPaymentStatus,
                        enabled = !isSubmitting && !paymentId.isNullOrBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.sdp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AtharColors.Secondary,
                            disabledContentColor = AtharColors.TextLight,
                        ),
                        border = BorderStroke(2.sdp, AtharColors.Secondary),
                        shape = RoundedCornerShape(16.sdp),
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(20.sdp))
                        Spacer(Modifier.width(10.sdp))
                        Text("Refresh Payment Status", fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                    }
                }

                // Payment method badges
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("VISA", modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A1F71)).padding(horizontal = 6.dp, vertical = 2.dp), color = AtharColors.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(Modifier.width(6.sdp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.sdp)).background(Color(0xFFEB001B)).padding(horizontal = 6.sdp, vertical = 2.sdp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(12.sdp).clip(CircleShape).background(Color(0xFFFF5F00).copy(alpha = 0.8f)))
                    }
                    Spacer(Modifier.width(6.sdp))
                    Text("meeza", modifier = Modifier.clip(RoundedCornerShape(4.sdp)).background(Color(0xFF003087)).padding(horizontal = 6.sdp, vertical = 2.sdp), color = Color(0xFF009CDE), fontSize = 7.ssp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.sdp))
                    Text("& more payment methods", color = AtharColors.TextLight, fontSize = 11.ssp)
                }

                Spacer(Modifier.height(8.sdp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SCREEN 3: PAYMENT CONFIRMATION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ConfirmationScreen(
    volunteerName: String, location: String, destination: String, date: String,
    hours: Int, pricePerHour: Int, total: Int, bookingRef: String, paymentMethodLabel: String,
    onTrackVolunteer: () -> Unit, onBackToHome: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null) {},
        shape = RoundedCornerShape(topStart = 24.sdp, topEnd = 24.sdp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.sdp),
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Spacer(Modifier.height(16.sdp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.sdp).height(5.sdp)
                    .clip(RoundedCornerShape(3.sdp))
                    .background(AtharColors.Gray200),
            )

            // Success
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.sdp, vertical = 24.sdp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(96.sdp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AtharColors.SuccessLight, Color(0xFFA7F3D0)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(48.sdp))
                }
                Spacer(Modifier.height(16.sdp))
                Text("Payment Confirmed!", color = AtharColors.Secondary, fontSize = 22.ssp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.sdp))
                Text("Your volunteer is on the way.", color = AtharColors.TextLight, fontSize = 14.ssp)
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.sdp),
                verticalArrangement = Arrangement.spacedBy(16.sdp),
            ) {
                // Booking reference
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.sdp)).background(AtharColors.Primary).padding(12.sdp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Booking Reference", color = AtharColors.TextLight, fontSize = 11.ssp)
                    Spacer(Modifier.height(4.sdp))
                    Text(bookingRef, color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 14.ssp, fontFamily = FontFamily.Monospace, letterSpacing = 1.ssp)
                }

                // Summary
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.sdp)).border(2.sdp, AtharColors.Gray200, RoundedCornerShape(16.sdp)).padding(16.sdp),
                    verticalArrangement = Arrangement.spacedBy(12.sdp),
                ) {
                    SummaryRow("Volunteer", volunteerName)
                    SummaryRow("Date", date)
                    SummaryRow("From", location)
                    if (destination.isNotBlank()) SummaryRow("To", destination)
                    SummaryRow("Hours", "$hours ${if (hours == 1) "hour" else "hours"}")
                    SummaryRow("Rate", "$pricePerHour EGP/hr")
                    SummaryRow("Payment", paymentMethodLabel)
                    HorizontalDivider(color = AtharColors.Gray200, thickness = 2.sdp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Paid", color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                        Text("$total EGP", color = AtharColors.Accent, fontWeight = FontWeight.Bold, fontSize = 20.ssp)
                    }
                }

                // Info note
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.sdp)).background(AtharColors.WarningBg).padding(12.sdp),
                    verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.sdp),
                ) {
                    Icon(Icons.Filled.Shield, null, tint = AtharColors.AccentDark, modifier = Modifier.size(20.sdp).padding(top = 2.sdp))
                    Text("Your volunteer has been notified and is heading to your location now.", color = AtharColors.Secondary, fontSize = 13.ssp)
                }

                // Action buttons
                Button(
                    onClick = onTrackVolunteer,
                    modifier = Modifier.fillMaxWidth().height(52.sdp),
                    colors = ButtonDefaults.buttonColors(containerColor = AtharColors.Secondary),
                    shape = RoundedCornerShape(16.sdp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.sdp),
                ) {
                    Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(20.sdp))
                    Spacer(Modifier.width(8.sdp))
                    Text("Track Volunteer", fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                }

                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth().height(52.sdp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AtharColors.Secondary),
                    border = BorderStroke(2.sdp, AtharColors.Secondary),
                    shape = RoundedCornerShape(16.sdp),
                ) {
                    Icon(Icons.Filled.Home, null, modifier = Modifier.size(20.sdp))
                    Spacer(Modifier.width(8.sdp))
                    Text("Back to Home", fontWeight = FontWeight.SemiBold, fontSize = 16.ssp)
                }

                Spacer(Modifier.height(16.sdp))
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AtharColors.TextLight, fontSize = 13.ssp)
        Text(value, color = AtharColors.Secondary, fontWeight = FontWeight.SemiBold, fontSize = 13.ssp)
    }
}

@Composable
private fun PaymentFeedbackCard(
    text: String,
    background: Color,
    border: Color,
    textColor: Color,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.sdp))
            .background(background)
            .border(2.sdp, border, RoundedCornerShape(16.sdp))
            .padding(14.sdp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.sdp),
    ) {
        Icon(icon, null, tint = textColor, modifier = Modifier.size(20.sdp))
        Text(text, color = textColor, fontSize = 13.ssp)
    }
}

private fun paymentLabelFor(method: String): String {
    return when (method.trim().uppercase()) {
        "CARD" -> "Paymob"
        "CASH" -> "Cash"
        else -> method.ifBlank { "Payment" }
    }
}

private fun paymentStatusSummary(status: ApiPaymentStatus): String {
    return if (status.success) {
        "Payment captured successfully."
    } else {
        "Payment status: ${status.status.replaceFirstChar { it.uppercase() }}."
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewAtharPaymentFlow() {
    AtharPaymentFlow(
        volunteerName = "Sara Mohammed",
        serviceName = "Mobility Assistance",
        location = "Central Mall entrance",
        destination = "Central Mall - Level 2",
        date = "Mar 6, 2026",
        initialHours = 2,
        initialPricePerHour = 75,
        onSubmitPayment = {
            ApiCallResult.Success(
                ApiPayRequestResponse(
                    paymentMethod = it,
                    status = if (it == "CASH") "active" else "pending_payment",
                    message = "Preview payment response.",
                    checkoutUrl = "https://checkout.paymob.com/preview",
                    paymentId = "pay-preview"
                )
            )
        },
        onConfirmPaymobPayment = {
            ApiCallResult.Success(ApiActionResult(success = true, message = "Preview payment confirmed."))
        },
        onGetPaymentStatus = {
            ApiCallResult.Success(ApiPaymentStatus(id = it, status = "pending", amount = 150.0, currency = "EGP", success = false))
        },
        onRefreshPaymentStatus = {
            ApiCallResult.Success(ApiPaymentStatus(id = it, status = "captured", amount = 150.0, currency = "EGP", success = true))
        },
        onComplete = {},
        onTrackVolunteer = {},
        onBackToHome = {},
        onClose = {},
    )
}

private fun normalizePaymentPricePerHour(hours: Int, pricePerHour: Int): Int {
    // Valid EGP pricePerHour: 50-200. Piaster values start at 5000.
    return when {
        pricePerHour > 200 && pricePerHour % 100 == 0 -> (pricePerHour / 100).coerceAtLeast(1)
        else -> pricePerHour.coerceAtLeast(1)
    }
}

private fun normalizePaymentTotalAmount(hours: Int, pricePerHour: Int, totalAmountEgp: Int?): Int? {
    // Valid EGP total: max 200*8=1600. Piaster values start at 5000.
    return totalAmountEgp?.let { total ->
        when {
            total > 1600 && total % 100 == 0 -> (total / 100).coerceAtLeast(1)
            else -> total.coerceAtLeast(1)
        }
    }
}
