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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

private data class DocumentationArticle(
  val title: String,
  val content: List<String>
)

private data class DocumentationSection(
  val id: String,
  val title: String,
  val icon: ImageVector,
  val iconBackground: Color,
  val iconTint: Color,
  val articles: List<DocumentationArticle>
)

@Composable
fun DocumentationScreen(onBack: () -> Unit) {
  var expandedSection by remember { mutableStateOf("features") }
  var searchQuery by remember { mutableStateOf("") }

  val sections = remember {
    listOf(
      DocumentationSection(
        id = "features",
        title = "Features Overview",
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        iconBackground = Color(0xFFD6E4F5),
        iconTint = Color(0xFF1F3C5B),
        articles = listOf(
          DocumentationArticle(
            title = "Core Features",
            content = listOf(
              "Athar provides three core features: interactive accessibility mapping, real-time volunteer assistance matching, and a community-driven location rating system.",
              "The platform uses role-based access control with strict separation between Users (people with disabilities) and Volunteers (assistance providers).",
              "All features are designed with accessibility in mind, following WCAG 2.1 AA standards for color contrast, text size, and interactive elements."
            )
          ),
          DocumentationArticle(
            title = "User Interface Design",
            content = listOf(
              "The app uses a mobile-first approach with bottom tab navigation for easy thumb access.",
              "High-contrast color palette keeps critical actions clear and readable.",
              "Large buttons (minimum 44x44 touch targets) and readable text sizes are optimized for older users and people with visual impairments."
            )
          )
        )
      ),
      DocumentationSection(
        id = "accessibility",
        title = "Accessibility Features",
        icon = Icons.Outlined.Shield,
        iconBackground = Color(0xFFDBEAFE),
        iconTint = Color(0xFF2563EB),
        articles = listOf(
          DocumentationArticle(
            title = "Map Accessibility Indicators",
            content = listOf(
              "Locations are rated on key accessibility features: wheelchair ramps, elevators, accessible restrooms, parking, and braille signage.",
              "Each feature uses a 5-star model and average ratings are calculated from user submissions.",
              "Markers separate known accessibility-ready places from unrated places so users can quickly scan confidence levels."
            )
          ),
          DocumentationArticle(
            title = "Screen Reader Support",
            content = listOf(
              "Interactive elements include descriptive labels for screen reader compatibility.",
              "Visual assets include descriptive labels where needed.",
              "Form inputs use clear labels and error feedback."
            )
          ),
          DocumentationArticle(
            title = "High Contrast Mode",
            content = listOf(
              "The interface keeps strong foreground/background contrast by default.",
              "Text remains readable across cards, headers, and controls.",
              "Inputs and buttons maintain visible focus states for keyboard and assistive navigation."
            )
          )
        )
      ),
      DocumentationSection(
        id = "volunteer-system",
        title = "Volunteer Assistance System",
        icon = Icons.Outlined.VolunteerActivism,
        iconBackground = Color(0xFFFFEDD5),
        iconTint = Color(0xFFEA580C),
        articles = listOf(
          DocumentationArticle(
            title = "Request Matching Algorithm",
            content = listOf(
              "When a user creates a request, the system matches volunteers by proximity, availability, and skills.",
              "Nearby volunteers receive request notifications in real time.",
              "Matching can consider volunteer reliability signals such as rating and completion history."
            )
          ),
          DocumentationArticle(
            title = "Request Lifecycle",
            content = listOf(
              "Requests move through Pending, Active, Completed, or Cancelled states.",
              "Users can cancel pending requests. Active requests are completed after confirmation.",
              "After completion, users can rate support quality and share feedback."
            )
          ),
          DocumentationArticle(
            title = "Volunteer Dashboard",
            content = listOf(
              "Volunteers can track completed assists, rating, and recent activity.",
              "The dashboard highlights pending requests and active sessions.",
              "Availability controls determine when new requests are delivered."
            )
          )
        )
      ),
      DocumentationSection(
        id = "map-usage",
        title = "Map Usage & Navigation",
        icon = Icons.Outlined.Place,
        iconBackground = Color(0xFFECFCCB),
        iconTint = Color(0xFF65A30D),
        articles = listOf(
          DocumentationArticle(
            title = "Interactive Map Technology",
            content = listOf(
              "Athar supports interactive location browsing with map overlays and location markers.",
              "Markers represent accessibility-rated places across the configured region.",
              "Users can pan, zoom, and inspect place-level details quickly."
            )
          ),
          DocumentationArticle(
            title = "Location Filtering",
            content = listOf(
              "Filter by feature types like ramps, elevators, parking, and braille.",
              "Multiple filters can be combined to narrow results precisely.",
              "Filters remain active during the session for faster repeat searches."
            )
          ),
          DocumentationArticle(
            title = "Adding & Rating Locations",
            content = listOf(
              "Users can submit ratings and accessibility updates to improve map quality.",
              "When adding a location, users share the name, address, and accessibility details.",
              "New ratings are merged with previous submissions to keep results community-driven."
            )
          )
        )
      ),
      DocumentationSection(
        id = "privacy",
        title = "Privacy & Security",
        icon = Icons.Outlined.Lock,
        iconBackground = Color(0xFFF3E8FF),
        iconTint = Color(0xFF9333EA),
        articles = listOf(
          DocumentationArticle(
            title = "Data Collection",
            content = listOf(
              "Athar may collect personal profile fields, location signals, and in-app activity records needed for platform operation.",
              "Location sharing is tied to assistance flows and map features.",
              "Sensitive data transport is protected in transit and at rest."
            )
          ),
          DocumentationArticle(
            title = "Privacy Controls",
            content = listOf(
              "Users can manage profile visibility, location behavior, and notification preferences.",
              "Two-factor and session controls are available for stronger account protection.",
              "Active sessions can be reviewed and revoked from security settings."
            )
          ),
          DocumentationArticle(
            title = "Data Rights",
            content = listOf(
              "Users can request access, correction, export, or deletion of eligible data.",
              "Data export supports common formats.",
              "Deletion requests are processed according to policy and legal requirements."
            )
          )
        )
      ),
      DocumentationSection(
        id = "troubleshooting",
        title = "Troubleshooting",
        icon = Icons.Outlined.ReportProblem,
        iconBackground = Color(0xFFFEE2E2),
        iconTint = Color(0xFFDC2626),
        articles = listOf(
          DocumentationArticle(
            title = "Common Issues",
            content = listOf(
              "Location not updating: verify device location services and app permissions.",
              "No volunteers available: availability can vary by area and time.",
              "Map not loading: check network connectivity and retry."
            )
          ),
          DocumentationArticle(
            title = "Account Issues",
            content = listOf(
              "Login failures: verify credentials and reset password when needed.",
              "Role change requests: account role is intentionally fixed for platform integrity.",
              "Profile save issues: retry after refresh or re-authentication."
            )
          ),
          DocumentationArticle(
            title = "Getting Additional Help",
            content = listOf(
              "Use Help & Support for direct assistance and feedback.",
              "Include device model, OS version, and repro steps in bug reports.",
              "Review FAQ and quick guides before contacting support for faster resolution."
            )
          )
        )
      )
    )
  }

  val normalizedQuery = searchQuery.trim().lowercase()
  val filteredSections = sections.filter { section ->
    if (normalizedQuery.isBlank()) return@filter true
    section.title.lowercase().contains(normalizedQuery) || section.articles.any { article ->
      article.title.lowercase().contains(normalizedQuery) ||
        article.content.any { paragraph -> paragraph.lowercase().contains(normalizedQuery) }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
  ) {
    ScreenHeader(
      title = "Documentation",
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
          .background(Color(0xFFEEF2FF), RoundedCornerShape(12.dp))
          .border(2.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp))
          .padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = Color(0xFF4338CA),
            modifier = Modifier
              .size(24.dp)
              .padding(top = 2.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Comprehensive Documentation",
              style = MaterialTheme.typography.titleLarge,
              color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Detailed technical documentation covering all features, systems, and best practices for using Athar effectively.",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF374151)
            )
          }
        }
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = {
          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Color(0xFF9CA3AF)
          )
        },
        placeholder = { Text("Search documentation...") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color(0xFF1F3C5B),
          unfocusedBorderColor = Color(0xFFE5E7EB)
        )
      )

      if (normalizedQuery.isNotBlank()) {
        Text(
          text = "Found ${filteredSections.size} section${if (filteredSections.size == 1) "" else "s"}",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF4B5563)
        )
      }

      if (filteredSections.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "No Results Found",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF111827)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Try a different search term to find what you are looking for.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4B5563)
          )
        }
      } else {
        filteredSections.forEach { section ->
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
                  text = "${section.articles.size} article${if (section.articles.size == 1) "" else "s"}",
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
                  .padding(horizontal = 16.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                section.articles.forEach { article ->
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                      .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Text(
                      text = article.title,
                      style = MaterialTheme.typography.titleSmall,
                      color = Color(0xFF111827)
                    )
                    article.content.forEach { paragraph ->
                      Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF374151)
                      )
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
              }
            }
          }
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFD6E4F5), RoundedCornerShape(12.dp))
          .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Quick Reference",
          style = MaterialTheme.typography.titleLarge,
          color = Color(0xFF111827)
        )

        QuickRefLine("User Role:", "Request assistance, rate locations, view map")
        QuickRefLine("Volunteer Role:", "Provide assistance, view dashboard, view map")
        QuickRefLine("Support Email:", "support@athar.app")
        QuickRefLine("App Version:", "1.0.0 (MVP)")
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
          .border(2.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
          .padding(16.dp)
      ) {
        Text(
          text = "Developer Resources",
          style = MaterialTheme.typography.titleLarge,
          color = Color(0xFF111827)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Looking for API documentation or developer integration guides? These will be available in future releases as platform capabilities expand.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF374151)
        )
      }

      Text(
        text = "Documentation last updated: November 24, 2025",
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
private fun QuickRefLine(label: String, value: String) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier = Modifier
        .padding(top = 5.dp)
        .size(8.dp)
        .background(Color(0xFF1F3C5B), CircleShape)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
      color = Color(0xFF374151)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = Color(0xFF374151)
    )
  }
}


