package com.athar.accessibilitymapping.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.EmailVerificationChallenge
import com.athar.accessibilitymapping.data.UserRole
import com.athar.accessibilitymapping.ui.components.OutlineActionButton
import com.athar.accessibilitymapping.ui.components.PrimaryButton
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.ErrorRed
import com.athar.accessibilitymapping.ui.theme.Gray100
import com.athar.accessibilitymapping.ui.theme.Gray200
import com.athar.accessibilitymapping.ui.theme.Gray600
import com.athar.accessibilitymapping.ui.theme.Gray700
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.SuccessGreen
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp
import java.time.Instant
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
  challenge: EmailVerificationChallenge,
  onBack: () -> Unit,
  onChallengeUpdated: (EmailVerificationChallenge) -> Unit,
  onVerify: suspend (String) -> String?,
  onResend: suspend () -> ApiCallResult<EmailVerificationChallenge>,
  isPreparingChallenge: Boolean = false,
  challengeLoadError: String? = null,
  onRetryChallengeLoad: (() -> Unit)? = null
) {
  val coroutineScope = rememberCoroutineScope()
  var code by remember(challenge.challengeId) { mutableStateOf("") }
  var errorMessage by remember(challenge.challengeId) { mutableStateOf<String?>(null) }
  var statusMessage by remember(challenge.challengeId) { mutableStateOf<String?>(challenge.message) }
  var isVerifying by remember(challenge.challengeId) { mutableStateOf(false) }
  var isResending by remember(challenge.challengeId) { mutableStateOf(false) }
  var currentEpochSeconds by remember { mutableStateOf(Instant.now().epochSecond) }
  val challengeReady = challenge.challengeId.isNotBlank()
  val isChallengePending = isPreparingChallenge || !challengeReady

  LaunchedEffect(challenge.challengeId, challenge.resendAvailableAtEpochSeconds, challenge.expiresAtEpochSeconds) {
    while (true) {
      currentEpochSeconds = Instant.now().epochSecond
      delay(1000)
    }
  }

  val resendSecondsRemaining = max(0L, challenge.resendAvailableAtEpochSeconds - currentEpochSeconds)
  val expirySecondsRemaining = max(0L, challenge.expiresAtEpochSeconds - currentEpochSeconds)
  val verificationExpired = expirySecondsRemaining <= 0L
  val hasCompleteCode = code.length == challenge.codeLength
  val heroAccent = if (challenge.role == UserRole.Volunteer) AccentGold else Color(0xFFD9B76D)
  val roleLabel = if (challenge.role == UserRole.Volunteer) "Volunteer" else "User"
  val subtitle = "Enter the verification code we sent to your email so your Athar account can be activated securely."
  val instructions = "Check your inbox and spam folder, then enter the ${challenge.codeLength}-digit code below."

  val pulseTransition = rememberInfiniteTransition()
  val pulseScale by pulseTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600),
      repeatMode = RepeatMode.Reverse
    )
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0C2136), NavyPrimary, NavyDark, Color(0xFF16314B))
        )
      )
  ) {
    DecorativeOrb(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset(x = 68.sdp, y = (-36).sdp),
      size = 220.sdp,
      color = heroAccent.copy(alpha = 0.13f)
    )
    DecorativeOrb(
      modifier = Modifier
        .align(Alignment.CenterStart)
        .offset(x = (-92).sdp, y = 24.sdp),
      size = 170.sdp,
      color = Color.White.copy(alpha = 0.05f)
    )
    DecorativeOrb(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .offset(y = 72.sdp),
      size = 280.sdp,
      color = BluePrimary.copy(alpha = 0.10f)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.sdp, vertical = 18.sdp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.sdp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(44.sdp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(enabled = !isVerifying && !isResending && !isPreparingChallenge, onClick = onBack),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(22.sdp)
          )
        }

        TopStatusChip(
          text = "Email Security",
          accentColor = heroAccent
        )
      }

      Spacer(modifier = Modifier.height(26.sdp))

      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(contentAlignment = Alignment.Center) {
          Box(
            modifier = Modifier
              .size(118.sdp)
              .graphicsLayer(
                scaleX = pulseScale,
                scaleY = pulseScale
              )
              .clip(CircleShape)
              .background(heroAccent.copy(alpha = 0.12f))
          )
          Box(
            modifier = Modifier
              .size(92.sdp)
              .shadow(14.sdp, CircleShape)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(
                    heroAccent.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.16f)
                  )
                )
              )
              .border(2.sdp, Color.White.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.VerifiedUser,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(38.sdp)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.sdp))

        Text(
          text = "Verify Your Email",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 30.ssp,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.sdp))
        Text(
          text = subtitle,
          color = Color(0xFFD9E7F5),
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.sdp))

        Card(
          shape = RoundedCornerShape(24.sdp),
          colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
          border = BorderStroke(1.sdp, Color.White.copy(alpha = 0.12f))
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.sdp, vertical = 14.sdp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Code sent to ${maskEmail(challenge.email)}",
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.ssp,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.sdp))
            Text(
              text = "Signing up as $roleLabel",
              color = Color(0xFFBED0E4),
              style = MaterialTheme.typography.bodySmall,
              textAlign = TextAlign.Center
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(22.sdp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.sdp)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(10.sdp)
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    NavyPrimary,
                    heroAccent,
                    NavyPrimary.copy(alpha = 0.85f)
                  )
                )
              )
          )

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.sdp, vertical = 22.sdp),
            verticalArrangement = Arrangement.spacedBy(16.sdp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.sdp)
            ) {
              VerificationMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Email,
                label = "Account type",
                value = roleLabel,
                accent = NavyPrimary
              )
              VerificationMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Refresh,
                label = "Code status",
                value = when {
                  !challengeLoadError.isNullOrBlank() -> "Retry"
                  isChallengePending -> "Loading"
                  verificationExpired -> "Expired"
                  else -> formatCountdown(expirySecondsRemaining)
                },
                accent = heroAccent
              )
            }

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.sdp))
                .background(BluePrimary)
                .padding(18.sdp),
              verticalArrangement = Arrangement.spacedBy(10.sdp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = "Verification code",
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.ssp
                  )
                  Spacer(modifier = Modifier.height(4.sdp))
                  Text(
                    text = instructions,
                    color = Gray600,
                    style = MaterialTheme.typography.bodyMedium
                  )
                }
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.sdp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 12.sdp, vertical = 8.sdp)
                ) {
                  Text(
                    text = "${challenge.codeLength} digits",
                    color = NavyPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                  )
                }
              }

              OtpCodeField(
                code = code,
                codeLength = challenge.codeLength,
                accentColor = heroAccent,
                isError = errorMessage != null || !challengeLoadError.isNullOrBlank(),
                enabled = challengeReady && !verificationExpired && !isVerifying && !isPreparingChallenge,
                onCodeChange = {
                  code = it
                  errorMessage = null
                }
              )
            }

            if (!challengeLoadError.isNullOrBlank()) {
              StatusBanner(
                text = challengeLoadError.orEmpty(),
                background = Color(0xFFFEECEC),
                border = ErrorRed.copy(alpha = 0.22f),
                textColor = ErrorRed
              )
            } else if (isChallengePending) {
              StatusBanner(
                text = challenge.message,
                background = Gray100,
                border = Gray200,
                textColor = NavyPrimary
              )
            } else if (!statusMessage.isNullOrBlank() && errorMessage.isNullOrBlank()) {
              StatusBanner(
                text = statusMessage.orEmpty(),
                background = Color(0xFFE7F8EE),
                border = SuccessGreen.copy(alpha = 0.28f),
                textColor = Color(0xFF166534)
              )
            }

            if (!errorMessage.isNullOrBlank()) {
              StatusBanner(
                text = errorMessage.orEmpty(),
                background = Color(0xFFFEECEC),
                border = ErrorRed.copy(alpha = 0.22f),
                textColor = ErrorRed
              )
            }

            PrimaryButton(
              text = when {
                isPreparingChallenge -> "Loading..."
                isVerifying -> "Verifying..."
                else -> "Verify Email"
              },
              onClick = {
                if (!challengeReady) {
                  errorMessage = challengeLoadError ?: "We're still preparing your verification request. Please wait."
                  return@PrimaryButton
                }
                if (verificationExpired) {
                  errorMessage = "This code expired. Go back and register again."
                  return@PrimaryButton
                }
                if (code.length < challenge.codeLength) {
                  errorMessage = "Enter the full ${challenge.codeLength}-digit verification code."
                  return@PrimaryButton
                }
                isVerifying = true
                statusMessage = null
                coroutineScope.launch {
                  errorMessage = onVerify(code)
                  isVerifying = false
                }
              },
              enabled = challengeReady && hasCompleteCode && !isVerifying && !verificationExpired && !isPreparingChallenge,
              background = NavyPrimary,
              modifier = Modifier.fillMaxWidth()
            )

            if (!challengeLoadError.isNullOrBlank() && onRetryChallengeLoad != null) {
              OutlineActionButton(
                text = if (isPreparingChallenge) "Retrying..." else "Retry Setup",
                onClick = onRetryChallengeLoad,
                borderColor = heroAccent,
                contentColor = NavyPrimary,
                modifier = Modifier.fillMaxWidth()
              )
            }

            OutlineActionButton(
              text = when {
                isChallengePending -> "Preparing..."
                isResending -> "Sending New Code..."
                resendSecondsRemaining > 0L -> "Resend in ${formatCountdown(resendSecondsRemaining)}"
                else -> "Resend Code"
              },
              onClick = {
                if (isChallengePending || resendSecondsRemaining > 0L || isResending) return@OutlineActionButton
                isResending = true
                errorMessage = null
                coroutineScope.launch {
                  when (val result = onResend()) {
                    is ApiCallResult.Success -> {
                      onChallengeUpdated(result.data)
                      code = ""
                      statusMessage = result.data.message
                    }
                    is ApiCallResult.Failure -> {
                      errorMessage = result.message
                    }
                  }
                  isResending = false
                }
              },
              borderColor = if (isChallengePending || resendSecondsRemaining > 0L || isResending) Gray200 else heroAccent,
              contentColor = if (isChallengePending || resendSecondsRemaining > 0L || isResending) Gray600 else NavyPrimary,
              modifier = Modifier.fillMaxWidth()
            )

            SupportInfoCard(
              maskedEmail = maskEmail(challenge.email),
              isResending = isResending
            )

            if (isResending) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.sdp),
                  color = NavyPrimary,
                  strokeWidth = 2.sdp
                )
                Spacer(modifier = Modifier.width(10.sdp))
                Text(
                  text = "Sending a fresh code to your email...",
                  color = Gray700,
                  style = MaterialTheme.typography.bodyMedium
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DecorativeOrb(
  modifier: Modifier = Modifier,
  size: androidx.compose.ui.unit.Dp,
  color: Color
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(color)
  )
}

@Composable
private fun TopStatusChip(
  text: String,
  accentColor: Color
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(20.sdp))
      .background(Color.White.copy(alpha = 0.10f))
      .border(1.sdp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(20.sdp))
      .padding(horizontal = 12.sdp, vertical = 8.sdp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(8.sdp)
        .clip(CircleShape)
        .background(accentColor)
    )
    Spacer(modifier = Modifier.width(8.sdp))
    Text(
      text = text,
      color = Color.White,
      fontWeight = FontWeight.SemiBold,
      style = MaterialTheme.typography.labelMedium
    )
  }
}

@Composable
private fun VerificationMetricCard(
  modifier: Modifier = Modifier,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  accent: Color
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(20.sdp),
    colors = CardDefaults.cardColors(containerColor = Gray100)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.sdp, vertical = 14.sdp),
      verticalArrangement = Arrangement.spacedBy(10.sdp)
    ) {
      Box(
        modifier = Modifier
          .size(38.sdp)
          .clip(CircleShape)
          .background(Color.White),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(18.sdp)
        )
      }
      Text(
        text = label,
        color = Gray600,
        style = MaterialTheme.typography.bodySmall
      )
      Text(
        text = value,
        color = NavyPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 16.ssp
      )
    }
  }
}

@Composable
private fun SupportInfoCard(
  maskedEmail: String,
  isResending: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.sdp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFE)),
    border = BorderStroke(1.sdp, Gray200)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.sdp, vertical = 14.sdp),
      verticalArrangement = Arrangement.spacedBy(6.sdp)
    ) {
      Text(
        text = "Need help?",
        color = NavyPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.ssp
      )
      Text(
        text = if (isResending) {
          "We are generating a fresh code for $maskedEmail. Stay on this screen for a moment."
        } else {
          "If the code does not arrive, wait for the timer and request a new one. You can also go back and confirm the email address."
        },
        color = Gray600,
        style = MaterialTheme.typography.bodyMedium
      )
    }
  }
}

@Composable
private fun StatusBanner(
  text: String,
  background: Color,
  border: Color,
  textColor: Color
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.sdp))
      .background(background)
      .border(BorderStroke(1.sdp, border), RoundedCornerShape(18.sdp))
      .padding(horizontal = 14.sdp, vertical = 12.sdp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(10.sdp)
        .clip(CircleShape)
        .background(textColor)
    )
    Spacer(modifier = Modifier.width(10.sdp))
    Text(text = text, color = textColor, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun OtpCodeField(
  code: String,
  codeLength: Int,
  accentColor: Color,
  isError: Boolean,
  enabled: Boolean,
  onCodeChange: (String) -> Unit
) {
  BasicTextField(
    value = code,
    onValueChange = { next ->
      onCodeChange(next.filter(Char::isDigit).take(codeLength))
    },
    enabled = enabled,
    singleLine = true,
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.NumberPassword,
      imeAction = ImeAction.Done
    ),
    textStyle = TextStyle(color = Color.Transparent),
    cursorBrush = SolidColor(Color.Transparent),
    modifier = Modifier.fillMaxWidth(),
    decorationBox = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.sdp)
      ) {
        repeat(codeLength) { index ->
          val value = code.getOrNull(index)?.toString().orEmpty()
          val isActiveSlot = code.length == index || (code.length == codeLength && index == codeLength - 1)
          val borderColor by animateColorAsState(
            targetValue = when {
              isError -> ErrorRed
              value.isNotBlank() -> NavyPrimary
              isActiveSlot -> accentColor
              else -> Gray200
            }
          )
          val backgroundColor by animateColorAsState(
            targetValue = when {
              isError && value.isNotBlank() -> Color(0xFFFFF1F2)
              value.isNotBlank() -> Color.White
              isActiveSlot -> Color.White.copy(alpha = 0.98f)
              else -> Color(0xFFF8FBFE)
            }
          )
          val scale by animateFloatAsState(
            targetValue = if (isActiveSlot) 1.03f else 1f
          )

          Box(
            modifier = Modifier
              .weight(1f)
              .height(70.sdp)
              .graphicsLayer(
                scaleX = scale,
                scaleY = scale
              )
              .shadow(
                elevation = if (isActiveSlot || value.isNotBlank()) 10.sdp else 0.sdp,
                shape = RoundedCornerShape(20.sdp)
              )
              .clip(RoundedCornerShape(20.sdp))
              .background(backgroundColor)
              .border(2.sdp, borderColor, RoundedCornerShape(20.sdp)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = value,
              color = NavyPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 26.ssp,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }
  )
}

private fun maskEmail(email: String): String {
  val trimmed = email.trim()
  val atIndex = trimmed.indexOf('@')
  if (atIndex <= 1) return trimmed
  val name = trimmed.substring(0, atIndex)
  val domain = trimmed.substring(atIndex)
  val visiblePrefix = name.take(2)
  return "$visiblePrefix${"*".repeat(max(1, name.length - visiblePrefix.length))}$domain"
}

private fun formatCountdown(seconds: Long): String {
  val minutes = seconds / 60
  val remainingSeconds = seconds % 60
  return if (minutes > 0) {
    "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
  } else {
    "0:${remainingSeconds.toString().padStart(2, '0')}"
  }
}
