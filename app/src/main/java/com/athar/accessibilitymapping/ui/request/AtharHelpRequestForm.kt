package com.athar.accessibilitymapping.ui.request

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
    val Secondary = Color(0xFF1F3C5B)
    val SecondaryDark = Color(0xFF2C4F73)
    val Accent = Color(0xFFC9A24D)
    val AccentLight = Color(0xFFD9B76D)
    val AccentDark = Color(0xFFB38F3D)
    val TextLight = Color(0xFF5B7A99)
    val Success = Color(0xFF10B981)
    val SuccessLight = Color(0xFFD1FAE5)
    val SuccessText = Color(0xFF065F46)
    val Error = Color(0xFFDC2626)
    val ErrorBg = Color(0xFFFEF2F2)
    val WarningBg = Color(0xFFFFF8DC)
    val Gray100 = Color(0xFFF1F5F9)
    val Gray200 = Color(0xFFE2E8F0)
    val White = Color.White
}

// ═══════════════════════════════════════════════════════════════════════════
// DATA MODEL
// ═══════════════════════════════════════════════════════════════════════════

data class HelpRequestData(
    val location: String,
    val destination: String,
    val helpType: String,
    val urgency: String,
    val paymentMethod: String,
    val hours: Int,
    val pricePerHour: Int,
    val total: Int,
    val description: String,
)

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtharHelpRequestForm(
    userDisabilityType: String = "Wheelchair user",
    isSubmitting: Boolean = false,
    submitted: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (HelpRequestData) -> Unit = {},
    onClose: () -> Unit = {},
) {
    var location by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var helpType by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("medium") }
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("cash") }
    var hours by remember { mutableIntStateOf(1) }
    var pricePerHour by remember { mutableIntStateOf(50) }
    var showHelpTypeDropdown by remember { mutableStateOf(false) }

    val priceSteps = listOf(50, 75, 100, 125, 150, 175, 200)
    val maxHours = 8
    val total by remember { derivedStateOf { pricePerHour * hours } }

    val helpTypes = listOf(
        "Navigation assistance",
        "Finding accessible entrance",
        "Help with accessible transportation",
        "Guide to specific location",
        "Reading assistance",
        "Other assistance",
    )

    val canSubmit = location.isNotBlank() && destination.isNotBlank() && helpType.isNotBlank()

    LaunchedEffect(submitted) {
        if (submitted) {
            delay(2000)
            onClose()
        }
    }

    Dialog(
        onDismissRequest = onClose,
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
                    onClick = onClose,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (submitted) {
                SuccessScreen(hours = hours, pricePerHour = pricePerHour, total = total)
            } else {
                RequestFormScreen(
                    userDisabilityType = userDisabilityType,
                    location = location,
                    destination = destination,
                    helpType = helpType,
                    urgency = urgency,
                    paymentMethod = paymentMethod,
                    description = description,
                    hours = hours,
                    pricePerHour = pricePerHour,
                    total = total,
                    priceSteps = priceSteps,
                    maxHours = maxHours,
                    helpTypes = helpTypes,
                    canSubmit = canSubmit,
                    isSubmitting = isSubmitting,
                    errorMessage = errorMessage,
                    showHelpTypeDropdown = showHelpTypeDropdown,
                    onLocationChange = { location = it },
                    onDestinationChange = { destination = it },
                    onHelpTypeChange = { helpType = it; showHelpTypeDropdown = false },
                    onUrgencyChange = { urgency = it },
                    onPaymentMethodChange = { paymentMethod = it },
                    onDescriptionChange = { description = it },
                    onHoursChange = { hours = it },
                    onPriceChange = { pricePerHour = it },
                    onToggleDropdown = { showHelpTypeDropdown = !showHelpTypeDropdown },
                    onSubmit = {
                        if (canSubmit && !isSubmitting) {
                            onSubmit(
                                HelpRequestData(
                                    location = location,
                                    destination = destination,
                                    helpType = helpType,
                                    urgency = urgency,
                                    paymentMethod = paymentMethod,
                                    hours = hours,
                                    pricePerHour = pricePerHour,
                                    total = total,
                                    description = description,
                                )
                            )
                        }
                    },
                    onClose = onClose,
                    onUseCurrentLocation = { location = "Central Mall, Main Entrance" },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REQUEST FORM SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestFormScreen(
    userDisabilityType: String,
    location: String,
    destination: String,
    helpType: String,
    urgency: String,
    paymentMethod: String,
    description: String,
    hours: Int,
    pricePerHour: Int,
    total: Int,
    priceSteps: List<Int>,
    maxHours: Int,
    helpTypes: List<String>,
    canSubmit: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    showHelpTypeDropdown: Boolean,
    onLocationChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onHelpTypeChange: (String) -> Unit,
    onUrgencyChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onHoursChange: (Int) -> Unit,
    onPriceChange: (Int) -> Unit,
    onToggleDropdown: () -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onUseCurrentLocation: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
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
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AtharColors.Secondary)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Request Volunteer Help",
                    color = AtharColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = AtharColors.White,
                    )
                }
            }

            // ── Scrollable Content ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // User Info Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AtharColors.Primary)
                        .border(2.dp, AtharColors.Secondary, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = AtharColors.Secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Requesting as: ",
                        color = AtharColors.Secondary,
                        fontSize = 13.sp,
                    )
                    Text(
                        userDisabilityType,
                        color = AtharColors.Secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // ── Current Location ──
                FormField(
                    label = "Your Current Location *",
                    icon = Icons.Filled.LocationOn,
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = onLocationChange,
                        placeholder = { Text("e.g., Central Mall entrance", color = AtharColors.TextLight, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                        leadingIcon = {
                            Icon(Icons.Filled.LocationOn, null, tint = AtharColors.TextLight, modifier = Modifier.size(20.dp))
                        },
                    )
                    TextButton(
                        onClick = onUseCurrentLocation,
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Text(
                            "📍 Use my current location",
                            color = AtharColors.AccentDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // ── Destination ──
                FormField(
                    label = "Where do you need to go? *",
                    icon = Icons.Filled.Navigation,
                ) {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = onDestinationChange,
                        placeholder = { Text("e.g., Central Mall - Level 2, Store 45", color = AtharColors.TextLight, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                        leadingIcon = {
                            Icon(Icons.Filled.Navigation, null, tint = AtharColors.TextLight, modifier = Modifier.size(20.dp))
                        },
                    )
                }

                // ── Help Type ──
                FormField(
                    label = "Type of Assistance Needed *",
                    icon = Icons.Filled.MedicalServices,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AtharColors.White)
                                .border(2.dp, AtharColors.SecondaryDark, RoundedCornerShape(12.dp))
                                .clickable { onToggleDropdown() }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = helpType.ifEmpty { "Select help type..." },
                                color = if (helpType.isEmpty()) AtharColors.TextLight else AtharColors.Secondary,
                                fontSize = 15.sp,
                                fontWeight = if (helpType.isEmpty()) FontWeight.Normal else FontWeight.Medium,
                            )
                            Icon(
                                imageVector = if (showHelpTypeDropdown) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AtharColors.TextLight,
                            )
                        }
                        AnimatedVisibility(visible = showHelpTypeDropdown) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AtharColors.White)
                                    .border(2.dp, AtharColors.Gray200, RoundedCornerShape(14.dp))
                            ) {
                                helpTypes.forEachIndexed { index, type ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onHelpTypeChange(type) }
                                            .padding(horizontal = 16.dp, vertical = 18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = type,
                                            color = AtharColors.Secondary,
                                            fontSize = 15.sp,
                                            fontWeight = if (helpType == type) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                        if (helpType == type) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = AtharColors.AccentDark,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                    if (index < helpTypes.lastIndex) {
                                        HorizontalDivider(color = AtharColors.Gray100, thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Urgency Level ──
                FormField(
                    label = "Urgency Level",
                    icon = Icons.Filled.PriorityHigh,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        listOf(
                            Triple("low", "Low", AtharColors.Secondary),
                            Triple("medium", "Medium", AtharColors.AccentDark),
                            Triple("high", "High", AtharColors.Error),
                        ).forEach { (level, label, color) ->
                            val selected = urgency == level
                            val bg = when {
                                !selected -> AtharColors.White
                                level == "low" -> AtharColors.Primary
                                level == "medium" -> AtharColors.WarningBg
                                else -> AtharColors.ErrorBg
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(
                                        2.dp,
                                        if (selected) color else AtharColors.Gray200,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable { onUrgencyChange(level) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label,
                                    color = if (selected) color else AtharColors.TextLight,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // PAYMENT SECTION — Hours & Price Selection
                // ═══════════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, AtharColors.Accent, RoundedCornerShape(16.dp)),
                ) {
                    // Payment header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AtharColors.WarningBg)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.CreditCard,
                                contentDescription = null,
                                tint = AtharColors.AccentDark,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "Payment Details",
                                color = AtharColors.Secondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                        }
                        Text(
                            "Pay after volunteer accepts",
                            color = AtharColors.TextLight,
                            fontSize = 11.sp,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AtharColors.White)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "Payment Method",
                            color = AtharColors.Secondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PaymentMethodOption(
                                modifier = Modifier.weight(1f),
                                label = "Pay in Cash",
                                caption = "Auto confirmed after volunteer accepts",
                                icon = Icons.Filled.MonetizationOn,
                                selected = paymentMethod == "cash",
                                onClick = { onPaymentMethodChange("cash") },
                            )
                            PaymentMethodOption(
                                modifier = Modifier.weight(1f),
                                label = "Visa",
                                caption = "Opens Paymob after volunteer accepts",
                                icon = Icons.Filled.CreditCard,
                                selected = paymentMethod == "card",
                                onClick = { onPaymentMethodChange("card") },
                            )
                        }

                        HorizontalDivider(color = AtharColors.Gray200, thickness = 1.dp)

                        // ── Hours Selector ──
                        Text(
                            "Number of Hours",
                            color = AtharColors.Secondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StepperButton(
                                icon = Icons.Filled.Remove,
                                enabled = hours > 1,
                                color = AtharColors.Secondary,
                                onClick = { if (hours > 1) onHoursChange(hours - 1) },
                            )
                            Spacer(Modifier.width(20.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.widthIn(min = 80.dp),
                            ) {
                                Text(
                                    "$hours",
                                    color = AtharColors.Secondary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    if (hours == 1) "hour" else "hours",
                                    color = AtharColors.TextLight,
                                    fontSize = 13.sp,
                                )
                            }
                            Spacer(Modifier.width(20.dp))
                            StepperButton(
                                icon = Icons.Filled.Add,
                                enabled = hours < maxHours,
                                color = AtharColors.Secondary,
                                onClick = { if (hours < maxHours) onHoursChange(hours + 1) },
                            )
                        }

                        HorizontalDivider(color = AtharColors.Gray200, thickness = 1.dp)

                        // ── Price Per Hour ──
                        Text(
                            "Price Per Hour (EGP)",
                            color = AtharColors.Secondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val idx = priceSteps.indexOf(pricePerHour)
                            StepperButton(
                                icon = Icons.Filled.Remove,
                                enabled = idx > 0,
                                color = AtharColors.AccentDark,
                                onClick = { if (idx > 0) onPriceChange(priceSteps[idx - 1]) },
                            )
                            Spacer(Modifier.width(20.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.widthIn(min = 100.dp),
                            ) {
                                Text(
                                    "$pricePerHour",
                                    color = AtharColors.Accent,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "EGP/hr",
                                    color = AtharColors.TextLight,
                                    fontSize = 13.sp,
                                )
                            }
                            Spacer(Modifier.width(20.dp))
                            StepperButton(
                                icon = Icons.Filled.Add,
                                enabled = idx < priceSteps.size - 1,
                                color = AtharColors.AccentDark,
                                onClick = { if (idx < priceSteps.size - 1) onPriceChange(priceSteps[idx + 1]) },
                            )
                        }

                        // Price chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            priceSteps.forEach { p ->
                                val selected = p == pricePerHour
                                Text(
                                    "$p",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .then(
                                            if (selected)
                                                Modifier
                                                    .background(AtharColors.Accent)
                                                    .border(2.dp, AtharColors.AccentDark, RoundedCornerShape(50))
                                            else
                                                Modifier
                                                    .background(AtharColors.White)
                                                    .border(2.dp, AtharColors.Gray200, RoundedCornerShape(50))
                                        )
                                        .clickable { onPriceChange(p) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (selected) AtharColors.White else AtharColors.Secondary,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                )
                            }
                        }

                        // Live Total
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AtharColors.Primary)
                                .border(2.dp, AtharColors.Secondary, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "$hours ${if (hours == 1) "hour" else "hours"} × $pricePerHour EGP",
                                color = AtharColors.Secondary,
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$total",
                                    color = AtharColors.Accent,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "EGP",
                                    color = AtharColors.Accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                            }
                        }
                    }
                }

                // ── Additional Details ──
                FormField(
                    label = "Additional Details (Optional)",
                    icon = Icons.Filled.Notes,
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        placeholder = { Text("Any additional information that might help the volunteer...", color = AtharColors.TextLight, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                    )
                }

                // ── Info Notice ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AtharColors.WarningBg)
                        .border(2.dp, AtharColors.AccentLight, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = AtharColors.AccentDark,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    )
                    Text(
                        if (paymentMethod == "cash") {
                            "Your request will be broadcast to verified volunteers within 5 km. Cash payments are confirmed automatically when the volunteer accepts."
                        } else {
                            "Your request will be broadcast to verified volunteers within 5 km. After a volunteer accepts, you will complete payment through Paymob."
                        },
                        color = AtharColors.Secondary,
                        fontSize = 13.sp,
                    )
                }

                // ── Submit Button ──
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit && !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AtharColors.Accent,
                        disabledContainerColor = AtharColors.Gray200,
                        disabledContentColor = AtharColors.TextLight,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (canSubmit) 6.dp else 0.dp),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AtharColors.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Broadcasting Request...",
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
                            "Send Request — $total EGP",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AtharColors.ErrorBg)
                            .border(1.dp, AtharColors.Error, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = AtharColors.Error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = errorMessage,
                            color = AtharColors.Error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SUCCESS SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SuccessScreen(hours: Int, pricePerHour: Int, total: Int) {
    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AtharColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AtharColors.SuccessLight)
                    .border(3.dp, AtharColors.Success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Request Broadcasted!",
                color = AtharColors.Secondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Searching for nearby volunteers. Average response time is under 2 minutes.",
                color = AtharColors.TextLight,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AtharColors.Primary)
                    .border(2.dp, AtharColors.Gray200, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Your offer", color = AtharColors.TextLight, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$total EGP",
                        color = AtharColors.Accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "(${hours}h × $pricePerHour EGP)",
                        color = AtharColors.TextLight,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Payment will be collected after a volunteer accepts.",
                    color = AtharColors.TextLight,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PaymentMethodOption(
    modifier: Modifier = Modifier,
    label: String,
    caption: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AtharColors.Primary else AtharColors.White)
            .border(
                2.dp,
                if (selected) AtharColors.Accent else AtharColors.Gray200,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) AtharColors.Accent else AtharColors.Gray100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AtharColors.White else AtharColors.Secondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            color = AtharColors.Secondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = caption,
            color = AtharColors.TextLight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FormField(
    label: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Icon(icon, null, tint = AtharColors.AccentDark, modifier = Modifier.size(16.dp))
            Text(
                label,
                color = AtharColors.Secondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        content()
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(
                2.dp,
                if (enabled) color else AtharColors.Gray200,
                CircleShape,
            )
            .background(AtharColors.White)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) color else AtharColors.Gray200,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AtharColors.Secondary,
    unfocusedBorderColor = AtharColors.Gray200,
    focusedTextColor = AtharColors.Secondary,
    unfocusedTextColor = AtharColors.Secondary,
    cursorColor = AtharColors.Accent,
    unfocusedContainerColor = AtharColors.Gray100,
    focusedContainerColor = AtharColors.White,
)

// ═══════════════════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewAtharHelpRequestForm() {
    AtharHelpRequestForm(
        userDisabilityType = "Wheelchair user",
        onSubmit = {},
        onClose = {},
    )
}
