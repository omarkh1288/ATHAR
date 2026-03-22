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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.R
import com.athar.accessibilitymapping.data.AuthOperationResult
import com.athar.accessibilitymapping.data.AuthSession
import com.athar.accessibilitymapping.data.UserRole
import com.athar.accessibilitymapping.data.UserRegistrationPayload
import com.athar.accessibilitymapping.data.VolunteerRegistrationPayload
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.SuccessGreen

@Composable
fun RoleSelectionScreen(
  onComplete: (AuthSession) -> Unit,
  onBack: () -> Unit,
  onRegisterUser: suspend (UserRegistrationPayload) -> AuthOperationResult,
  onRegisterVolunteer: suspend (VolunteerRegistrationPayload) -> AuthOperationResult
) {
  var showRegister by remember { mutableStateOf<UserRole?>(null) }

  if (showRegister == UserRole.User) {
    RegisterUserScreen(
      onBack = { showRegister = null },
      onComplete = { payload ->
        when (val result = onRegisterUser(payload)) {
          is AuthOperationResult.Success -> {
            onComplete(result.session)
            null
          }
          is AuthOperationResult.Error -> result.message
        }
      }
    )
    return
  }

  if (showRegister == UserRole.Volunteer) {
    RegisterVolunteerScreen(
      onBack = { showRegister = null },
      onComplete = { payload ->
        when (val result = onRegisterVolunteer(payload)) {
          is AuthOperationResult.Success -> {
            onComplete(result.session)
            null
          }
          is AuthOperationResult.Error -> result.message
        }
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
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier
            .clickable { onBack() }
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = NavyPrimary)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Back", color = NavyPrimary, fontWeight = FontWeight.Medium)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Box(
        modifier = Modifier
          .size(96.dp)
          .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.athar_logo),
          contentDescription = "Athar logo",
          modifier = Modifier.size(70.dp)
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






