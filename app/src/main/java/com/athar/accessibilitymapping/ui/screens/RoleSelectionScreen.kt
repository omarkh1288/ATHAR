package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.R
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.AuthOperationResult
import com.athar.accessibilitymapping.data.AuthSession
import com.athar.accessibilitymapping.data.EmailVerificationChallenge
import com.athar.accessibilitymapping.data.RegistrationOperationResult
import com.athar.accessibilitymapping.data.UserRegistrationPayload
import com.athar.accessibilitymapping.data.UserRole
import com.athar.accessibilitymapping.data.VolunteerRegistrationPayload
import com.athar.accessibilitymapping.ui.components.PrimaryButton
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

private sealed interface PendingVerificationLaunch {
  val email: String
  val role: UserRole
  val isStarting: Boolean
  val errorMessage: String?

  data class User(
    val payload: UserRegistrationPayload,
    override val isStarting: Boolean = true,
    override val errorMessage: String? = null
  ) : PendingVerificationLaunch {
    override val email: String get() = payload.email.trim()
    override val role: UserRole get() = UserRole.User
  }

  data class Volunteer(
    val payload: VolunteerRegistrationPayload,
    override val isStarting: Boolean = true,
    override val errorMessage: String? = null
  ) : PendingVerificationLaunch {
    override val email: String get() = payload.email.trim()
    override val role: UserRole get() = UserRole.Volunteer
  }
}

@Composable
fun RoleSelectionScreen(
  onComplete: (AuthSession) -> Unit,
  onBack: () -> Unit,
  onRegisterUser: suspend (UserRegistrationPayload) -> RegistrationOperationResult,
  onRegisterVolunteer: suspend (VolunteerRegistrationPayload) -> RegistrationOperationResult,
  onVerifyEmail: suspend (String, String) -> AuthOperationResult,
  onResendEmailChallenge: suspend (String) -> ApiCallResult<EmailVerificationChallenge>
) {
  val coroutineScope = rememberCoroutineScope()
  var showRegister by remember { mutableStateOf<UserRole?>(null) }
  var pendingVerification by remember { mutableStateOf<EmailVerificationChallenge?>(null) }
  var pendingVerificationLaunch by remember { mutableStateOf<PendingVerificationLaunch?>(null) }

  suspend fun startVerification(launchState: PendingVerificationLaunch) {
    when (launchState) {
      is PendingVerificationLaunch.User -> {
        when (val result = onRegisterUser(launchState.payload)) {
          is RegistrationOperationResult.VerificationRequired -> {
            pendingVerificationLaunch = null
            pendingVerification = result.challenge
          }
          is RegistrationOperationResult.Authenticated -> {
            pendingVerificationLaunch = launchState.copy(
              isStarting = false,
              errorMessage = "We couldn't open email verification. Please try again."
            )
          }
          is RegistrationOperationResult.Error -> {
            pendingVerificationLaunch = launchState.copy(
              isStarting = false,
              errorMessage = result.message
            )
          }
        }
      }
      is PendingVerificationLaunch.Volunteer -> {
        when (val result = onRegisterVolunteer(launchState.payload)) {
          is RegistrationOperationResult.VerificationRequired -> {
            pendingVerificationLaunch = null
            pendingVerification = result.challenge
          }
          is RegistrationOperationResult.Authenticated -> {
            pendingVerificationLaunch = launchState.copy(
              isStarting = false,
              errorMessage = "We couldn't open email verification. Please try again."
            )
          }
          is RegistrationOperationResult.Error -> {
            pendingVerificationLaunch = launchState.copy(
              isStarting = false,
              errorMessage = result.message
            )
          }
        }
      }
    }
  }

  val launchState = pendingVerificationLaunch
  if (launchState != null) {
    EmailVerificationStartScreen(
      email = launchState.email,
      role = launchState.role,
      isStarting = launchState.isStarting,
      errorMessage = launchState.errorMessage,
      onBack = {
        pendingVerificationLaunch = null
      },
      onRetry = {
        pendingVerificationLaunch = when (launchState) {
          is PendingVerificationLaunch.User -> launchState.copy(isStarting = true, errorMessage = null)
          is PendingVerificationLaunch.Volunteer -> launchState.copy(isStarting = true, errorMessage = null)
        }
        coroutineScope.launch {
          startVerification(
            when (launchState) {
              is PendingVerificationLaunch.User -> launchState.copy(isStarting = true, errorMessage = null)
              is PendingVerificationLaunch.Volunteer -> launchState.copy(isStarting = true, errorMessage = null)
            }
          )
        }
      }
    )
    return
  }

  val challenge = pendingVerification
  if (challenge != null) {
    EmailVerificationScreen(
      challenge = challenge,
      onBack = { pendingVerification = null },
      onChallengeUpdated = { pendingVerification = it },
      onVerify = { code ->
        when (val result = onVerifyEmail(challenge.challengeId, code)) {
          is AuthOperationResult.Success -> {
            onComplete(result.session)
            null
          }
          is AuthOperationResult.Error -> result.message
        }
      },
      onResend = {
        onResendEmailChallenge(challenge.challengeId)
      }
    )
    return
  }

  if (showRegister == UserRole.User) {
    RegisterUserScreen(
      onBack = { showRegister = null },
      onComplete = { payload ->
        val launchState = PendingVerificationLaunch.User(payload = payload)
        pendingVerificationLaunch = launchState
        startVerification(launchState)
        null
      }
    )
    return
  }

  if (showRegister == UserRole.Volunteer) {
    RegisterVolunteerScreen(
      onBack = { showRegister = null },
      onComplete = { payload ->
        val launchState = PendingVerificationLaunch.Volunteer(payload = payload)
        pendingVerificationLaunch = launchState
        startVerification(launchState)
        null
      }
    )
    return
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyPrimary)
      .padding(16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable { onBack() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Box(
        modifier = Modifier
          .size(96.dp)
          .clip(CircleShape)
          .background(Color.White),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.athar_logo),
          contentDescription = "Athar logo",
          modifier = Modifier.size(60.dp),
          contentScale = ContentScale.Fit
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Join Athar",
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White
      )
      Text(
        text = "Choose your role to get started",
        color = Color(0xFFD6E4F5)
      )

      Spacer(modifier = Modifier.height(20.dp))

      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RoleCard(
          title = "I Need Help",
          description = "Find accessible places and get assistance when needed",
          icon = Icons.Outlined.Person,
          iconBackground = NavyPrimary,
          onClick = { showRegister = UserRole.User },
          items = listOf(
            "Search accessible places",
            "Request volunteer assistance",
            "Rate locations & volunteers"
          )
        )

        RoleCard(
          title = "I Want to Help",
          description = "Become a volunteer and assist people in your community",
          icon = Icons.Outlined.Favorite,
          iconBackground = AccentGold,
          onClick = { showRegister = UserRole.Volunteer },
          items = listOf(
            "Accept assistance requests",
            "Go live when available",
            "Build your volunteer profile"
          )
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Already have an account? ",
          color = Color(0xFFD6E4F5)
        )
        Text(
          text = "Sign in",
          color = AccentGold,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.clickable { onBack() }
        )
      }
    }
  }
}

@Composable
private fun EmailVerificationStartScreen(
  email: String,
  role: UserRole,
  isStarting: Boolean,
  errorMessage: String?,
  onBack: () -> Unit,
  onRetry: () -> Unit
) {
  val accent = if (role == UserRole.Volunteer) AccentGold else Color(0xFFD9B76D)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyPrimary)
      .padding(20.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(enabled = !isStarting, onClick = onBack),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(48.dp))

      Box(
        modifier = Modifier
          .size(92.dp)
          .clip(CircleShape)
          .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
      ) {
        if (isStarting) {
          CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 3.dp,
            modifier = Modifier.size(34.dp)
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.Email,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(34.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Email Verification",
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = if (isStarting) {
          "Opening the OTP screen for ${email.trim()}..."
        } else {
          errorMessage ?: "We couldn't start email verification."
        },
        color = Color(0xFFD6E4F5)
      )

      Spacer(modifier = Modifier.height(28.dp))

      if (!isStarting) {
        PrimaryButton(
          text = "Try Again",
          onClick = onRetry,
          background = accent,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(
          text = "Back",
          onClick = onBack,
          background = Color.White,
          contentColor = NavyPrimary,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun RoleCard(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconBackground: Color,
  onClick: () -> Unit,
  items: List<String>
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
  ) {
    Column(modifier = Modifier.padding(28.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .background(iconBackground, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
          Text(title, color = NavyPrimary, style = MaterialTheme.typography.titleMedium)
          Text(description, color = NavyPrimary.copy(alpha = 0.7f))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(item, color = NavyPrimary.copy(alpha = 0.8f))
          }
        }
      }
    }
  }
}
