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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
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
fun TermsOfServiceScreen(onBack: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
  ) {
    ScreenHeader(
      title = "Terms of Service",
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
      InfoBanner(
        background = Color(0xFFD6E4F5),
        border = Color(0xFFE2E8F0),
        icon = Icons.Outlined.Description,
        iconTint = Color(0xFF1F3C5B),
        text = "Last Updated: November 24, 2025"
      )

      LegalIntroCard(
        title = "Terms and Conditions",
        paragraphs = listOf(
          "Welcome to Athar. These Terms of Service govern your use of the Athar application and services. By creating an account and using Athar, you agree to be bound by these Terms.",
          "Please read these Terms carefully. If you do not agree with any part of these Terms, you should not use our services."
        )
      )

      LegalSectionCard(
        title = "1. Acceptance of Terms",
        icon = Icons.Outlined.Description,
        iconBackground = Color(0xFFDBEAFE),
        iconTint = Color(0xFF2563EB),
        body = listOf(
          "By accessing or using Athar, you acknowledge that you have read, understood, and agree to be bound by these Terms and our Privacy Policy. These Terms apply to all users, including both people with disabilities seeking assistance and volunteers providing help."
        )
      )

      LegalSectionCard(
        title = "2. Eligibility",
        icon = Icons.Outlined.Person,
        iconBackground = Color(0xFFD6E4F5),
        iconTint = Color(0xFF1F3C5B),
        body = listOf("To use Athar, you must:"),
        bullets = listOf(
          "Be at least 18 years old",
          "Provide accurate and complete registration information",
          "Maintain the security of your account credentials",
          "Have the legal capacity to enter into binding agreements"
        )
      )

      LegalSectionCard(
        title = "3. User Accounts and Roles",
        icon = Icons.Outlined.Shield,
        iconBackground = Color(0xFFF3E8FF),
        iconTint = Color(0xFF9333EA),
        blocks = listOf(
          "Role Assignment" to listOf(
            "Upon registration, you must choose either a User role (person with disabilities) or Volunteer role. This role is permanent and cannot be changed. Each role has specific capabilities and responsibilities."
          ),
          "User Role" to listOf(
            "Request assistance from volunteers",
            "Rate and review locations for accessibility",
            "View accessibility maps and features",
            "Cannot provide volunteer assistance"
          ),
          "Volunteer Role" to listOf(
            "Respond to and fulfill assistance requests",
            "View accessibility maps and features",
            "Cannot request assistance or rate locations"
          ),
          "Account Security" to listOf(
            "You are responsible for maintaining the confidentiality of your account credentials. Notify us immediately of any unauthorized access or security breaches."
          )
        )
      )

      LegalSectionCard(
        title = "4. Acceptable Use",
        icon = Icons.Outlined.Shield,
        iconBackground = Color(0xFFDCFCE7),
        iconTint = Color(0xFF16A34A),
        body = listOf("You agree to use Athar responsibly and:"),
        bullets = listOf(
          "Treat all users with respect and dignity",
          "Provide accurate information in assistance requests and responses",
          "Honor commitments made to other users",
          "Provide honest and constructive location ratings",
          "Respect others' privacy and personal information",
          "Follow local laws and regulations"
        )
      )

      LegalSectionCard(
        title = "5. Prohibited Conduct",
        icon = Icons.Outlined.Block,
        iconBackground = Color(0xFFFEE2E2),
        iconTint = Color(0xFFDC2626),
        body = listOf("You must NOT:"),
        bullets = listOf(
          "Harass, abuse, or discriminate against any user",
          "Create fake accounts or impersonate others",
          "Submit false assistance requests or location ratings",
          "Use the service for commercial purposes without authorization",
          "Share inappropriate, offensive, or illegal content",
          "Attempt to hack, disrupt, or compromise the platform",
          "Spam or solicit users for unrelated purposes"
        )
      )

      LegalSectionCard(
        title = "6. Service Availability",
        icon = Icons.Outlined.Notifications,
        iconBackground = Color(0xFFFEF3C7),
        iconTint = Color(0xFFD97706),
        body = listOf(
          "We strive to provide reliable and continuous service, but we cannot guarantee:",
        ),
        bullets = listOf(
          "Uninterrupted or error-free operation",
          "Availability of volunteers at all times",
          "Specific response times for assistance requests",
          "Accuracy of all location accessibility information"
        ),
        trailingParagraph = "We reserve the right to modify, suspend, or discontinue any part of our service with or without notice."
      )

      LegalSectionCard(
        title = "7. Disclaimer of Warranties",
        icon = Icons.Outlined.WarningAmber,
        iconBackground = Color(0xFFFFEDD5),
        iconTint = Color(0xFFEA580C),
        body = listOf(
          "Athar is provided \"AS IS\" and \"AS AVAILABLE\" without warranties of any kind. We are a platform connecting users and volunteers but do not:"
        ),
        bullets = listOf(
          "Employ, supervise, or control volunteers",
          "Guarantee the quality or safety of assistance provided",
          "Verify the accuracy of user-submitted location ratings",
          "Assume liability for interactions between users"
        )
      )

      LegalSectionCard(
        title = "8. Limitation of Liability",
        icon = Icons.Outlined.Gavel,
        iconBackground = Color(0xFFE0E7FF),
        iconTint = Color(0xFF4F46E5),
        body = listOf(
          "To the maximum extent permitted by law, Athar and its operators shall not be liable for:"
        ),
        bullets = listOf(
          "Indirect, incidental, or consequential damages",
          "Loss of data, profits, or opportunities",
          "Conduct or actions of any user or volunteer",
          "Damage arising from use or inability to use the service"
        )
      )

      LegalSectionCard(
        title = "9. User Content and Ratings",
        icon = Icons.Outlined.Description,
        iconBackground = Color(0xFFFEF9C3),
        iconTint = Color(0xFFCA8A04),
        body = listOf(
          "By submitting location ratings, reviews, or other content, you grant Athar a worldwide, non-exclusive, royalty-free license to use, display, and distribute your content within the platform.",
          "You are responsible for submitted content and must ensure it is accurate, respectful, and lawful."
        )
      )

      LegalSectionCard(
        title = "10. Termination",
        icon = Icons.Outlined.Block,
        iconBackground = Color(0xFFFEE2E2),
        iconTint = Color(0xFFDC2626),
        body = listOf("We reserve the right to suspend or terminate your account at any time for:"),
        bullets = listOf(
          "Violation of these Terms",
          "Fraudulent or illegal activity",
          "Repeated complaints or misconduct",
          "Any reason necessary to protect our community"
        ),
        trailingParagraph = "You may also delete your account at any time through the app settings."
      )

      LegalSectionCard(
        title = "11. Governing Law",
        icon = Icons.Outlined.Gavel,
        iconBackground = Color(0xFFF3F4F6),
        iconTint = Color(0xFF4B5563),
        body = listOf(
          "These Terms are governed by the laws of Egypt. Any disputes arising from these Terms or your use of Athar shall be resolved in the courts of Cairo, Egypt."
        )
      )

      LegalIntroCard(
        title = "12. Changes to Terms",
        paragraphs = listOf(
          "We may update these Terms from time to time. We will notify you of significant changes through the app or by email. Continued use of Athar after changes constitutes acceptance of the updated Terms."
        )
      )

      LegalIntroCard(
        title = "Contact Us",
        background = Color(0xFFD6E4F5),
        border = Color(0xFFE2E8F0),
        paragraphs = listOf(
          "If you have questions about these Terms of Service, please contact us:",
          "Email: legal@athar.app",
          "Address: Cairo, Egypt",
          "Phone: +20 XXX XXX XXXX"
        )
      )

      LegalIntroCard(
        title = "Agreement",
        background = Color(0xFFFFFBEB),
        border = Color(0xFFFDE68A),
        paragraphs = listOf(
          "By using Athar, you acknowledge that you have read, understood, and agree to be bound by these Terms of Service and our Privacy Policy."
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
private fun InfoBanner(
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
private fun LegalIntroCard(
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
private fun LegalSectionCard(
  title: String,
  icon: ImageVector,
  iconBackground: Color,
  iconTint: Color,
  body: List<String> = emptyList(),
  bullets: List<String> = emptyList(),
  blocks: List<Pair<String, List<String>>> = emptyList(),
  trailingParagraph: String? = null
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, RoundedCornerShape(16.dp))
      .border(2.dp, if (iconTint == Color(0xFFDC2626)) Color(0xFFFCA5A5) else Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
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

    if (trailingParagraph != null) {
      Text(
        text = trailingParagraph,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF374151),
        modifier = Modifier.padding(start = 52.dp)
      )
    }
  }
}


