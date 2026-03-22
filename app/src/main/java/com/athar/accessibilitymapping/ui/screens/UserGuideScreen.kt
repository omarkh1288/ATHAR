package com.athar.accessibilitymapping.ui.screens

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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.ui.components.ScreenHeader

private data class GuideItem(
  val title: String,
  val content: String
)

private data class GuideSection(
  val id: String,
  val title: String,
  val icon: ImageVector,
  val iconBackground: Color,
  val iconTint: Color,
  val items: List<GuideItem>
)

@Composable
fun UserGuideScreen(onBack: () -> Unit) {
  var expandedSection by remember { mutableStateOf("getting-started") }

  val sections = remember {
    listOf(
      GuideSection(
        id = "getting-started",
        title = "Getting Started",
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        iconBackground = Color(0xFFD6E4F5),
        iconTint = Color(0xFF1F3C5B),
        items = listOf(
          GuideItem(
            title = "Welcome to Athar",
            content = "Athar is an accessibility mapping and volunteer assistance app designed for people with disabilities. The platform connects users who need help with volunteers ready to assist."
          ),
          GuideItem(
            title = "Choosing Your Role",
            content = "During registration you choose User or Volunteer. This role is permanent and controls which features you can use. Users can request help and rate locations, while volunteers respond to assistance requests."
          ),
          GuideItem(
            title = "Navigating the App",
            content = "The app uses bottom tab navigation. Users get Map/Requests/Profile experiences, and volunteers get dashboard-focused flows. Buttons and text are intentionally large for accessibility."
          )
        )
      ),
      GuideSection(
        id = "for-users",
        title = "For Users (People with Disabilities)",
        icon = Icons.Outlined.Person,
        iconBackground = Color(0xFFDBEAFE),
        iconTint = Color(0xFF2563EB),
        items = listOf(
          GuideItem(
            title = "Requesting Assistance",
            content = "Use Request Help to create an assistance request. Nearby volunteers are notified and you can coordinate once a volunteer accepts."
          ),
          GuideItem(
            title = "Rating Locations",
            content = "Rate places for ramps, elevators, accessible restrooms, parking, and braille signage. Community ratings improve map reliability for everyone."
          ),
          GuideItem(
            title = "Using the Map",
            content = "Use map filters to find places with the accessibility features you need. Tap markers to inspect ratings and details."
          ),
          GuideItem(
            title = "Managing Requests",
            content = "Track pending, active, and completed requests. Cancel pending requests anytime and complete active requests after support is received."
          )
        )
      ),
      GuideSection(
        id = "for-volunteers",
        title = "For Volunteers",
        icon = Icons.Outlined.Handyman,
        iconBackground = Color(0xFFFFEDD5),
        iconTint = Color(0xFFEA580C),
        items = listOf(
          GuideItem(
            title = "Responding to Requests",
            content = "Review request details and accept requests you can fulfill. After acceptance, communicate with users to coordinate assistance."
          ),
          GuideItem(
            title = "Managing Availability",
            content = "Update availability status so notifications only arrive when you are ready to help."
          ),
          GuideItem(
            title = "Building Your Reputation",
            content = "Reliable support improves your rating and completion stats, which are visible in volunteer-facing surfaces."
          ),
          GuideItem(
            title = "Using the Dashboard",
            content = "Dashboard views show pending items, active sessions, and contribution metrics so volunteers can track their impact."
          )
        )
      ),
      GuideSection(
        id = "map-features",
        title = "Map Features",
        icon = Icons.Outlined.Map,
        iconBackground = Color(0xFFECFCCB),
        iconTint = Color(0xFF65A30D),
        items = listOf(
          GuideItem(
            title = "Viewing Locations",
            content = "Map markers represent locations with accessibility data. Distinct marker styles help separate rated and unrated places."
          ),
          GuideItem(
            title = "Using Filters",
            content = "Filter results by accessibility features such as ramps, elevators, restrooms, parking, and braille signage."
          ),
          GuideItem(
            title = "Location Details",
            content = "Tapping a marker opens details like ratings, feature-level information, and community feedback."
          ),
          GuideItem(
            title = "Adding New Locations",
            content = "Users can submit new places with accessibility details to expand coverage and help the community."
          )
        )
      ),
      GuideSection(
        id = "account",
        title = "Account Management",
        icon = Icons.Outlined.Settings,
        iconBackground = Color(0xFFF3E8FF),
        iconTint = Color(0xFF9333EA),
        items = listOf(
          GuideItem(
            title = "Editing Your Profile",
            content = "Update profile details such as name, photo, phone, and bio. Users can include accessibility needs; volunteers can include skills."
          ),
          GuideItem(
            title = "Privacy Settings",
            content = "Control location sharing, visibility, and notifications in Privacy & Security settings."
          ),
          GuideItem(
            title = "Notification Settings",
            content = "Choose which alerts you receive, including request updates, messages, and announcements."
          ),
          GuideItem(
            title = "Changing Password",
            content = "Use Change Password in security settings to update credentials with strong-password requirements."
          )
        )
      )
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
  ) {
    ScreenHeader(
      title = "User Guide",
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
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFD6E4F5), RoundedCornerShape(12.dp))
          .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
          .padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = Color(0xFF1F3C5B),
            modifier = Modifier
              .size(24.dp)
              .padding(top = 2.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "How to Use Athar",
              style = MaterialTheme.typography.titleLarge,
              color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "This guide helps you get the most out of Athar. Tap any section below to expand detailed instructions.",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF374151)
            )
          }
        }
      }

      sections.forEach { section ->
        val isExpanded = expandedSection == section.id

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                expandedSection = if (isExpanded) "" else section.id
              }
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .background(section.iconBackground, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = section.iconTint,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF111827)
              )
              Text(
                text = "${section.items.size} topics",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4B5563)
              )
            }

            Icon(
              imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
              contentDescription = null,
              tint = Color(0xFF9CA3AF),
              modifier = Modifier.size(24.dp)
            )
          }

          if (isExpanded) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              section.items.forEach { item ->
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF111827)
                  )
                  Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF374151)
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
            }
          }
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
          .border(2.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Quick Tips",
          style = MaterialTheme.typography.titleLarge,
          color = Color(0xFF111827)
        )
        GuideTip("Keep location services enabled for the best assistance matching experience.")
        GuideTip("Be respectful and honest in all user and volunteer interactions.")
        GuideTip("Report issues or inappropriate behavior using Help & Support.")
        GuideTip("Keep your profile up to date so others can support you effectively.")
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White, RoundedCornerShape(16.dp))
          .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "Still Need Help?",
          style = MaterialTheme.typography.titleLarge,
          color = Color(0xFF111827)
        )
        Text(
          text = "If you could not find what you were looking for, check video tutorials or contact support.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF374151)
        )
      }

      Text(
        text = "Last updated: November 24, 2025",
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
private fun GuideTip(text: String) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier = Modifier
        .padding(top = 5.dp)
        .size(8.dp)
        .background(Color(0xFFD97706), CircleShape)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = Color(0xFF374151)
    )
  }
}


