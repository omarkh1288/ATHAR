package com.athar.accessibilitymapping.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athar.accessibilitymapping.ui.components.ScreenHeader

private data class TutorialCategory(
  val id: String,
  val name: String,
  val count: Int
)

private data class TutorialItem(
  val id: String,
  val title: String,
  val description: String,
  val duration: String,
  val category: String,
  val thumbnailColor: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoTutorialsScreen(onBack: () -> Unit) {
  var selectedCategory by remember { mutableStateOf("all") }
  var searchQuery by remember { mutableStateOf("") }

  val categories = remember {
    listOf(
      TutorialCategory("all", "All Videos", 12),
      TutorialCategory("getting-started", "Getting Started", 3),
      TutorialCategory("map", "Map Features", 2),
      TutorialCategory("assistance", "Assistance Requests", 3),
      TutorialCategory("rating", "Location Rating", 2),
      TutorialCategory("settings", "Settings & Privacy", 2)
    )
  }

  val tutorials = remember {
    listOf(
      TutorialItem(
        id = "1",
        title = "Welcome to Athar",
        description = "Introduction to Athar and how it connects people with disabilities and volunteers.",
        duration = "3:45",
        category = "getting-started",
        thumbnailColor = "teal"
      ),
      TutorialItem(
        id = "2",
        title = "Creating Your Account",
        description = "Step-by-step registration guide for User and Volunteer account flows.",
        duration = "5:20",
        category = "getting-started",
        thumbnailColor = "blue"
      ),
      TutorialItem(
        id = "3",
        title = "Understanding User Roles",
        description = "Learn differences between User and Volunteer capabilities.",
        duration = "4:10",
        category = "getting-started",
        thumbnailColor = "purple"
      ),
      TutorialItem(
        id = "4",
        title = "Using the Interactive Map",
        description = "Navigate the accessibility map, markers, and place details.",
        duration = "6:30",
        category = "map",
        thumbnailColor = "lime"
      ),
      TutorialItem(
        id = "5",
        title = "Filtering Locations by Features",
        description = "Find locations with specific features like ramps or elevators.",
        duration = "4:15",
        category = "map",
        thumbnailColor = "green"
      ),
      TutorialItem(
        id = "6",
        title = "Requesting Help (For Users)",
        description = "Create assistance requests and get matched with nearby volunteers.",
        duration = "5:45",
        category = "assistance",
        thumbnailColor = "blue"
      ),
      TutorialItem(
        id = "7",
        title = "Responding to Requests (For Volunteers)",
        description = "Accept and fulfill requests from users who need assistance.",
        duration = "6:00",
        category = "assistance",
        thumbnailColor = "orange"
      ),
      TutorialItem(
        id = "8",
        title = "Managing Active Requests",
        description = "Track pending and active sessions and complete requests correctly.",
        duration = "4:30",
        category = "assistance",
        thumbnailColor = "teal"
      ),
      TutorialItem(
        id = "9",
        title = "Rating Locations for Accessibility",
        description = "Submit feature ratings to improve community accessibility data.",
        duration = "5:10",
        category = "rating",
        thumbnailColor = "yellow"
      ),
      TutorialItem(
        id = "10",
        title = "Writing Helpful Reviews",
        description = "Best practices for clear, respectful, and useful accessibility reviews.",
        duration = "3:55",
        category = "rating",
        thumbnailColor = "orange"
      ),
      TutorialItem(
        id = "11",
        title = "Customizing Your Profile",
        description = "Update profile information and account settings effectively.",
        duration = "4:40",
        category = "settings",
        thumbnailColor = "purple"
      ),
      TutorialItem(
        id = "12",
        title = "Privacy and Security Settings",
        description = "Control visibility, location sharing, and account security options.",
        duration = "5:25",
        category = "settings",
        thumbnailColor = "red"
      )
    )
  }

  val normalizedQuery = searchQuery.trim().lowercase()
  val filteredTutorials = tutorials.filter { tutorial ->
    val matchesCategory = selectedCategory == "all" || tutorial.category == selectedCategory
    val matchesQuery = normalizedQuery.isBlank() ||
      tutorial.title.lowercase().contains(normalizedQuery) ||
      tutorial.description.lowercase().contains(normalizedQuery)
    matchesCategory && matchesQuery
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
  ) {
    ScreenHeader(
      title = "Video Tutorials",
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
          .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
          .border(2.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
          .padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = Color(0xFFB91C1C),
            modifier = Modifier
              .size(24.dp)
              .padding(top = 2.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Learn by Watching",
              style = MaterialTheme.typography.titleLarge,
              color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Watch step-by-step guides to learn all Athar features. Great for visual learners.",
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
        placeholder = { Text("Search tutorials...") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Color(0xFF1F3C5B),
          unfocusedBorderColor = Color(0xFFE5E7EB)
        )
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.White, RoundedCornerShape(16.dp))
          .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.FilterList,
            contentDescription = null,
            tint = Color(0xFF4B5563),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Categories",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF111827)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          categories.forEach { category ->
            val selected = selectedCategory == category.id
            Box(
              modifier = Modifier
                .background(
                  color = if (selected) Color(0xFF1F3C5B) else Color(0xFFF3F4F6),
                  shape = RoundedCornerShape(8.dp)
                )
                .clickable { selectedCategory = category.id }
                .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
              Text(
                text = "${category.name} (${category.count})",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color.White else Color(0xFF374151)
              )
            }
          }
        }
      }

      if (normalizedQuery.isNotBlank()) {
        Text(
          text = "Found ${filteredTutorials.size} tutorial${if (filteredTutorials.size == 1) "" else "s"}",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF4B5563)
        )
      }

      if (filteredTutorials.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "No Tutorials Found",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF111827)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Try adjusting your search or category filter.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4B5563)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          filteredTutorials.forEach { tutorial ->
            val colorSet = tutorialColors(tutorial.thumbnailColor)
            val categoryName = categories.firstOrNull { it.id == tutorial.category }?.name ?: tutorial.category

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                .clickable { }
                .padding(16.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(width = 96.dp, height = 96.dp)
                  .background(colorSet.first, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Outlined.PlayArrow,
                  contentDescription = null,
                  tint = colorSet.second,
                  modifier = Modifier.size(40.dp)
                )
                Box(
                  modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = tutorial.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                  )
                }
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = tutorial.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = Color(0xFF111827)
                )
                Text(
                  text = tutorial.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF4B5563)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Outlined.Schedule,
                      contentDescription = null,
                      tint = Color(0xFF6B7280),
                      modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = tutorial.duration,
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFF6B7280)
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
          .border(2.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
          .padding(16.dp)
      ) {
        Text(
          text = "More Videos Coming Soon",
          style = MaterialTheme.typography.titleLarge,
          color = Color(0xFF111827)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "New tutorial videos are added regularly. Check back for advanced features and tips.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF374151)
        )
      }

      Text(
        text = "Tap any video card to watch the tutorial",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF6B7280),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
      )
    }
  }
}

private fun tutorialColors(key: String): Pair<Color, Color> {
  return when (key) {
    "teal" -> Color(0xFFD6E4F5) to Color(0xFF1F3C5B)
    "blue" -> Color(0xFFDBEAFE) to Color(0xFF2563EB)
    "purple" -> Color(0xFFF3E8FF) to Color(0xFF9333EA)
    "lime" -> Color(0xFFECFCCB) to Color(0xFF65A30D)
    "green" -> Color(0xFFD6E4F5) to Color(0xFF1F3C5B)
    "orange" -> Color(0xFFFFEDD5) to Color(0xFFEA580C)
    "yellow" -> Color(0xFFFEF9C3) to Color(0xFFCA8A04)
    "red" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
    else -> Color(0xFFD6E4F5) to Color(0xFF1F3C5B)
  }
}


