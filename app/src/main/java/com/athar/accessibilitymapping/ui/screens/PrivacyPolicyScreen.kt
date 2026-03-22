package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.ui.components.ScreenHeader

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
  ) {
    ScreenHeader(
      title = "Privacy Policy",
      onBack = onBack,
      background = Color(0xFF1F3C5B)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      PrivacyBanner(
        background = Color(0xFFD6E4F5),
        border = Color(0xFFE2E8F0),
        icon = Icons.Outlined.Description,
        iconTint = Color(0xFF1F3C5B),
        text = "Last Updated: November 24, 2025"
      )

      PrivacyIntroCard(
        title = "Welcome to Athar",
        paragraphs = listOf(
          "At Athar, we are committed to protecting your privacy and keeping your personal information secure. This Privacy Policy explains how we collect, use, share, and safeguard your data while you use our accessibility mapping and volunteer assistance platform.",
          "By using Athar, you agree to the terms outlined in this policy. Please read it carefully."
        )
      )

      PrivacySectionCard(
        title = "1. Information We Collect",
        icon = Icons.Outlined.Storage,
        iconBackground = Color(0xFFDBEAFE),
        iconTint = Color(0xFF2563EB),
        blocks = listOf(
          "Personal Information" to listOf(
            "Name, email address, and phone number",
            "Profile photo (optional)",
            "Disability type and accessibility needs (for users)",
            "Volunteer skills and availability (for volunteers)"
          ),
          "Location Data" to listOf(
            "Real-time location when using assistance requests",
            "Location ratings and accessibility reports",
            "Approximate location for nearby volunteer matching"
          ),
          "Usage Information" to listOf(
            "Assistance requests and volunteer responses",
            "Ratings and reviews you provide",
            "App usage patterns and preferences"
          )
        )
      )

      PrivacySectionCard(
        title = "2. How We Use Your Information",
        icon = Icons.Outlined.Visibility,
        iconBackground = Color(0xFFD6E4F5),
        iconTint = Color(0xFF1F3C5B),
        body = listOf("We use your information to:"),
        bullets = listOf(
          "Connect users with nearby volunteers for assistance",
          "Display accessibility features on the interactive map",
          "Enable location ratings and community reviews",
          "Improve services and user experience",
          "Send notifications about requests and updates",
          "Maintain platform security and integrity"
        )
      )

      PrivacySectionCard(
        title = "3. Information Sharing",
        icon = Icons.Outlined.People,
        iconBackground = Color(0xFFFFEDD5),
        iconTint = Color(0xFFEA580C),
        blocks = listOf(
          "With Volunteers" to listOf(
            "When you request assistance, we share your name, location, and assistance needs with volunteers who can help."
          ),
          "With Other Users" to listOf(
            "Profile information (name/photo) and location ratings can be visible to users to support trust and transparency."
          ),
          "We Never Share" to listOf(
            "Personal contact information without consent",
            "Data with advertisers for third-party marketing",
            "Sensitive disability information publicly"
          )
        )
      )

      PrivacySectionCard(
        title = "4. Data Security",
        icon = Icons.Outlined.Lock,
        iconBackground = Color(0xFFDCFCE7),
        iconTint = Color(0xFF16A34A),
        body = listOf("We implement industry-standard security measures to protect your data:"),
        bullets = listOf(
          "Encryption for sensitive information",
          "Secure infrastructure with regular security reviews",
          "Optional two-factor authentication",
          "Regular backups and recovery procedures",
          "Restricted internal access to personal data"
        )
      )

      PrivacySectionCard(
        title = "5. Your Rights",
        icon = Icons.Outlined.Security,
        iconBackground = Color(0xFFF3E8FF),
        iconTint = Color(0xFF9333EA),
        body = listOf("You have the right to:"),
        bullets = listOf(
          "Access your personal data",
          "Correct inaccurate information",
          "Request deletion of your account and eligible data",
          "Control location sharing and notifications",
          "Request data portability exports"
        )
      )

      PrivacySectionCard(
        title = "6. Data Retention",
        icon = Icons.Outlined.Storage,
        iconBackground = Color(0xFFFEF3C7),
        iconTint = Color(0xFFD97706),
        body = listOf(
          "We retain data only as long as needed to provide services and satisfy legal obligations:"
        ),
        bullets = listOf(
          "Active account data while your account remains active",
          "Location ratings retained for community trust",
          "Deleted account data removed within policy windows",
          "Certain records retained where legally required"
        )
      )

      PrivacySectionCard(
        title = "7. Children's Privacy",
        icon = Icons.Outlined.Security,
        iconBackground = Color(0xFFFEE2E2),
        iconTint = Color(0xFFDC2626),
        body = listOf(
          "Athar is intended for users aged 18 and older. We do not knowingly collect personal information from children under 18."
        )
      )

      PrivacySectionCard(
        title = "8. International Users",
        icon = Icons.Outlined.Language,
        iconBackground = Color(0xFFE0E7FF),
        iconTint = Color(0xFF4F46E5),
        body = listOf(
          "Athar is based in Egypt. If you access Athar from outside Egypt, your information may be processed in Egypt subject to applicable data protection obligations."
        )
      )

      PrivacyIntroCard(
        title = "9. Changes to This Policy",
        paragraphs = listOf(
          "We may update this Privacy Policy over time. Significant changes are communicated through the app or by email. Continued use after updates means you accept the revised policy."
        )
      )

      PrivacyIntroCard(
        title = "Contact Us",
        background = Color(0xFFD6E4F5),
        border = Color(0xFFE2E8F0),
        paragraphs = listOf(
          "If you have questions about this Privacy Policy or data handling, please contact us:",
          "Email: privacy@athar.app",
          "Address: Cairo, Egypt",
          "Phone: +20 XXX XXX XXXX"
        )
      )

      Text(
        text = "© 2025 Athar. All rights reserved.",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF6B7280),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
      )
    }
  }
}

@Composable
private fun PrivacyBanner(
  background: Color,
  border: Color,
  icon: ImageVector,
  iconTint: Color,
  text: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(background, RoundedCornerShape(12.dp))
      .border(2.dp, border, RoundedCornerShape(12.dp))
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
      color = iconTint
    )
  }
}

@Composable
private fun PrivacyIntroCard(
  title: String,
  paragraphs: List<String>,
  background: Color = Color.White,
  border: Color = Color(0xFFE5E7EB)
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(background, RoundedCornerShape(16.dp))
      .border(2.dp, border, RoundedCornerShape(16.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = Color(0xFF111827)
    )
    paragraphs.forEach { paragraph ->
      Text(
        text = paragraph,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF374151)
      )
    }
  }
}

@Composable
private fun PrivacySectionCard(
  title: String,
  icon: ImageVector,
  iconBackground: Color,
  iconTint: Color,
  body: List<String> = emptyList(),
  bullets: List<String> = emptyList(),
  blocks: List<Pair<String, List<String>>> = emptyList()
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, RoundedCornerShape(16.dp))
      .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(iconBackground, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = Color(0xFF111827)
      )
    }

    if (body.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 52.dp)) {
        body.forEach { paragraph ->
          Text(
            text = paragraph,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF374151)
          )
        }
      }
    }

    if (bullets.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 52.dp)) {
        bullets.forEach { bullet ->
          Row(verticalAlignment = Alignment.Top) {
            Box(
              modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(iconTint.copy(alpha = 0.9f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = bullet,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF374151)
            )
          }
        }
      }
    }

    if (blocks.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(start = 52.dp)) {
        blocks.forEach { (heading, lines) ->
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = heading,
              style = MaterialTheme.typography.titleSmall,
              color = Color(0xFF111827)
            )
            lines.forEach { line ->
              Row(verticalAlignment = Alignment.Top) {
                Box(
                  modifier = Modifier
                    .padding(top = 6.dp)
                    .size(6.dp)
                    .background(iconTint.copy(alpha = 0.9f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = line,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF374151)
                )
              }
            }
          }
        }
      }
    }
  }
}


