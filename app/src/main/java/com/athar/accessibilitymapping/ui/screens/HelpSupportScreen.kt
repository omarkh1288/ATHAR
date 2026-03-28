package com.athar.accessibilitymapping.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athar.accessibilitymapping.data.ApiCallResult
import com.athar.accessibilitymapping.data.AtharRepository
import com.athar.accessibilitymapping.ui.components.ScreenHeader
import com.athar.accessibilitymapping.ui.theme.AccentGold
import com.athar.accessibilitymapping.ui.theme.AccentGoldDark
import com.athar.accessibilitymapping.ui.theme.BluePrimary
import com.athar.accessibilitymapping.ui.theme.BlueSecondary
import com.athar.accessibilitymapping.ui.theme.NavyDark
import com.athar.accessibilitymapping.ui.theme.NavyPrimary
import com.athar.accessibilitymapping.ui.theme.sdp
import com.athar.accessibilitymapping.ui.theme.ssp
import kotlinx.coroutines.launch

private data class FaqItem(
  val question: String,
  val answer: String
)

@Composable
fun HelpSupportScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  var showContactForm by remember { mutableStateOf(false) }
  var showUserGuide by remember { mutableStateOf(false) }
  var showVideoTutorials by remember { mutableStateOf(false) }
  var showDocumentation by remember { mutableStateOf(false) }

  if (showContactForm) {
    ContactSupportScreen(onBack = { showContactForm = false })
    return
  }
  if (showUserGuide) {
    UserGuideScreen(onBack = { showUserGuide = false })
    return
  }
  if (showVideoTutorials) {
    VideoTutorialsScreen(onBack = { showVideoTutorials = false })
    return
  }
  if (showDocumentation) {
    DocumentationScreen(onBack = { showDocumentation = false })
    return
  }

  val faqItems = listOf(
    FaqItem(
      "How do I request volunteer assistance?",
      "Tap the Request Help button on the Map screen, fill in your location and needs, and we'll match you with a nearby volunteer."
    ),
    FaqItem(
      "How do I become a volunteer?",
      "Contact our support team to switch your account to volunteer mode. You'll need to complete verification before you can accept requests."
    ),
    FaqItem(
      "How is my data protected?",
      "All your data is encrypted and stored securely. We never share your personal information without your consent."
    ),
    FaqItem(
      "Can I rate locations and volunteers?",
      "Yes! After visiting a location or receiving help, you can rate and review to help others in the community."
    ),
    FaqItem(
      "What if no volunteer accepts my request?",
      "If no volunteer is available within 3 minutes, we'll expand the search radius and notify more volunteers."
    )
  )
  val expandedFaq = remember { mutableStateListOf(*Array(faqItems.size) { false }) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
  ) {
    ScreenHeader(title = "Help & Support", onBack = onBack, background = NavyPrimary)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(BluePrimary)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      HelpSection(title = "Get in Touch") {
        HelpRow(
          icon = Icons.AutoMirrored.Outlined.Chat,
          iconBackground = NavyPrimary,
          iconBorder = NavyDark,
          title = "Contact Support",
          subtitle = "Send us a message",
          showChevron = true
        ) {
          showContactForm = true
        }
        HelpRow(
          icon = Icons.Outlined.Email,
          iconBackground = Color(0xFF10B981),
          iconBorder = Color(0xFF059669),
          title = "Email Support",
          subtitle = "support@athar.app",
          showChevron = false
        ) {
          openSupportEmail(context, "support@athar.app")
        }
        HelpRow(
          icon = Icons.Outlined.Phone,
          iconBackground = AccentGold,
          iconBorder = AccentGoldDark,
          title = "Call Support Hotline",
          subtitle = "+966 800 123 456",
          showChevron = false,
          showDivider = false
        ) {
          openSupportDialer(context, "+966800123456")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      HelpSection(title = "Resources") {
        HelpRow(
          icon = Icons.Outlined.Book,
          iconBackground = AccentGold,
          iconBorder = AccentGoldDark,
          title = "User Guide",
          subtitle = "Learn how to use Athar",
          showChevron = true
        ) {
          showUserGuide = true
        }
        HelpRow(
          icon = Icons.Outlined.VideoLibrary,
          iconBackground = Color(0xFFDC2626),
          iconBorder = Color(0xFF991B1B),
          title = "Video Tutorials",
          subtitle = "Watch step-by-step guides",
          showChevron = true
        ) {
          showVideoTutorials = true
        }
        HelpRow(
          icon = Icons.Outlined.Description,
          iconBackground = NavyPrimary,
          iconBorder = NavyDark,
          title = "Documentation",
          subtitle = "Detailed feature guides",
          showChevron = true,
          showDivider = false
        ) {
          showDocumentation = true
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White, RoundedCornerShape(16.dp))
          .border(2.dp, NavyPrimary, RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Text(
          text = "Frequently Asked Questions",
          color = NavyPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        faqItems.forEachIndexed { index, item ->
          Column {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(BlueSecondary, RoundedCornerShape(8.dp))
                .border(2.dp, NavyPrimary, RoundedCornerShape(8.dp))
                .clickable { expandedFaq[index] = !expandedFaq[index] }
                .padding(12.dp),
              verticalAlignment = Alignment.Top
            ) {
              Text(
                text = item.question,
                color = NavyPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = NavyPrimary,
                modifier = Modifier
                  .size(20.dp)
                  .rotate(if (expandedFaq[index]) 90f else 0f)
              )
            }
            AnimatedVisibility(visible = expandedFaq[index]) {
              Text(
                text = item.answer,
                color = NavyPrimary.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(text = "Athar v1.0.0 (MVP)", color = NavyPrimary.copy(alpha = 0.6f), fontSize = 14.sp)
        Text(text = "(c) 2024 Athar. All rights reserved.", color = NavyPrimary.copy(alpha = 0.6f), fontSize = 14.sp)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun ContactSupportScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val repository = remember(context) { AtharRepository(context) }
  val coroutineScope = rememberCoroutineScope()

  var subject by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  val canSubmit = subject.isNotBlank() && message.isNotBlank() && !isSubmitting

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
  ) {
    ScreenHeader(title = "Contact Support", onBack = onBack, background = NavyPrimary)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(BluePrimary)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(BlueSecondary, RoundedCornerShape(12.dp))
          .border(2.dp, NavyPrimary, RoundedCornerShape(12.dp))
          .padding(16.dp)
      ) {
        Text(
          text = "Our support team typically responds within 24 hours. For urgent issues, please call our hotline.",
          color = NavyPrimary,
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text("Subject *", color = NavyPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = subject,
        onValueChange = {
          subject = it
          errorMessage = null
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("What do you need help with?", color = NavyPrimary.copy(alpha = 0.6f), fontSize = 16.sp) },
        shape = RoundedCornerShape(8.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = NavyPrimary),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = NavyPrimary,
          unfocusedBorderColor = NavyPrimary,
          focusedContainerColor = Color.White,
          unfocusedContainerColor = Color.White,
          cursorColor = NavyPrimary
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text("Message *", color = NavyPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedTextField(
        value = message,
        onValueChange = {
          message = it
          errorMessage = null
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp),
        placeholder = { Text("Describe your issue or question in detail...", color = NavyPrimary.copy(alpha = 0.6f), fontSize = 16.sp) },
        shape = RoundedCornerShape(8.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = NavyPrimary),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = NavyPrimary,
          unfocusedBorderColor = NavyPrimary,
          focusedContainerColor = Color.White,
          unfocusedContainerColor = Color.White,
          cursorColor = NavyPrimary
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          errorMessage = null
          successMessage = null
          if (subject.isBlank() || message.isBlank()) {
            errorMessage = "Please fill in all fields."
            return@Button
          }

          isSubmitting = true
          coroutineScope.launch {
            when (val result = repository.sendSupportMessage(subject.trim(), message.trim())) {
              is ApiCallResult.Success -> {
                successMessage = result.data.message
                subject = ""
                message = ""
              }

              is ApiCallResult.Failure -> {
                errorMessage = result.message
              }
            }
            isSubmitting = false
          }
        },
        enabled = canSubmit,
        colors = ButtonDefaults.buttonColors(
          containerColor = NavyPrimary,
          contentColor = Color.White,
          disabledContainerColor = Color(0xFFD1D5DB),
          disabledContentColor = Color(0xFF6B7280)
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, NavyDark),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
      ) {
        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isSubmitting) "Sending..." else "Send Message",
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(errorMessage ?: "", color = Color(0xFFB91C1C), fontSize = 14.sp)
      }
      if (successMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(successMessage ?: "", color = Color(0xFF166534), fontSize = 14.sp)
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, RoundedCornerShape(16.dp))
      .border(2.dp, NavyPrimary, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Text(text = title, color = NavyPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(12.dp))
    content()
  }
}

@Composable
private fun HelpRow(
  icon: ImageVector,
  iconBackground: Color,
  iconBorder: Color,
  title: String,
  subtitle: String,
  showChevron: Boolean,
  showDivider: Boolean = true,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(BlueSecondary, RoundedCornerShape(8.dp))
      .border(2.dp, NavyPrimary, RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .background(iconBackground, CircleShape)
        .border(2.dp, iconBorder, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, color = NavyPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
      Text(text = subtitle, color = NavyPrimary.copy(alpha = 0.8f), fontSize = 14.sp)
    }
    if (showChevron) {
      Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
    }
  }
  if (showDivider) {
    Spacer(modifier = Modifier.height(12.dp))
  }
}

private fun openSupportEmail(context: Context, email: String) {
  val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
  if (intent.resolveActivity(context.packageManager) != null) {
    context.startActivity(intent)
  }
}

private fun openSupportDialer(context: Context, phoneNumber: String) {
  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
  if (intent.resolveActivity(context.packageManager) != null) {
    context.startActivity(intent)
  }
}

