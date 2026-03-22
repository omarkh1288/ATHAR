package com.athar.accessibilitymapping.data

val mockLocations = listOf(
  Location(
    id = "1",
    name = "City Stars Mall",
    category = "Shopping",
    lat = 30.0726,
    lng = 31.3498,
    rating = 4.5,
    totalRatings = 127,
    features = LocationFeatures(
      ramp = true,
      elevator = true,
      accessibleToilet = true,
      accessibleParking = true,
      wideEntrance = true,
      brailleSignage = false
    ),
    recentReports = listOf("Elevator working", "Clean accessible toilet"),
    distance = "0.5 km"
  ),
  Location(
    id = "2",
    name = "Giza Public Library",
    category = "Public Service",
    lat = 30.0131,
    lng = 31.2189,
    rating = 4.8,
    totalRatings = 89,
    features = LocationFeatures(
      ramp = true,
      elevator = true,
      accessibleToilet = true,
      accessibleParking = false,
      wideEntrance = true,
      brailleSignage = true
    ),
    recentReports = listOf("Excellent braille signage", "Staff very helpful"),
    distance = "1.2 km"
  ),
  Location(
    id = "3",
    name = "Orman Garden",
    category = "Recreation",
    lat = 30.0350,
    lng = 31.2136,
    rating = 3.9,
    totalRatings = 54,
    features = LocationFeatures(
      ramp = true,
      elevator = false,
      accessibleToilet = true,
      accessibleParking = true,
      wideEntrance = true,
      brailleSignage = false
    ),
    recentReports = listOf("Some paths uneven", "Accessible toilet needs repair"),
    distance = "2.1 km"
  ),
  Location(
    id = "4",
    name = "Giza Medical Center",
    category = "Healthcare",
    lat = 30.0089,
    lng = 31.2050,
    rating = 4.6,
    totalRatings = 203,
    features = LocationFeatures(
      ramp = true,
      elevator = true,
      accessibleToilet = true,
      accessibleParking = true,
      wideEntrance = true,
      brailleSignage = true
    ),
    recentReports = listOf("All facilities accessible", "Priority seating available"),
    distance = "0.8 km"
  ),
  Location(
    id = "5",
    name = "Nile View Cafe",
    category = "Restaurant",
    lat = 30.0195,
    lng = 31.2234,
    rating = 4.2,
    totalRatings = 76,
    features = LocationFeatures(
      ramp = true,
      elevator = false,
      accessibleToilet = true,
      accessibleParking = false,
      wideEntrance = true,
      brailleSignage = false
    ),
    recentReports = listOf("Ground floor accessible", "Limited parking"),
    distance = "1.5 km"
  )
)

val mockRequests = listOf(
  VolunteerRequest(
    id = "r1",
    userId = "u1",
    userName = "Ahmed Al-Rashid",
    userType = "Wheelchair user",
    location = "Central Mall entrance",
    requestTime = "10 mins ago",
    status = "pending",
    volunteerName = null,
    description = "Need assistance navigating to accessible entrance"
  ),
  VolunteerRequest(
    id = "r2",
    userId = "u2",
    userName = "Fatima Hassan",
    userType = "Visually impaired",
    location = "City Library",
    requestTime = "1 hour ago",
    status = "accepted",
    volunteerName = "Sara Mohammed",
    description = "Help finding braille section"
  ),
  VolunteerRequest(
    id = "r3",
    userId = "u3",
    userName = "Omar Khalil",
    userType = "Mobility challenges",
    location = "Medical Center",
    requestTime = "2 hours ago",
    status = "completed",
    volunteerName = "Yusuf Ali",
    description = "Assistance with check-in process"
  )
)

val userProfile = UserProfile(
  name = "Layla Abdullah",
  email = "layla.abdullah@email.com",
  phone = "+966 50 123 4567",
  disabilityType = "Wheelchair user",
  memberSince = "March 2024",
  contributionStats = ContributionStats(
    ratingsSubmitted = 12,
    reportsSubmitted = 8,
    helpfulVotes = 34
  )
)

val mockIncomingRequests = listOf(
  AssistanceRequest(
    id = "req1",
    userName = "Ahmed",
    userType = "Wheelchair user",
    location = "Central Mall entrance",
    destination = "Central Mall - Level 2",
    distance = "0.3 km",
    urgency = "medium",
    helpType = "Navigation assistance",
    requestTime = "2 mins ago",
    status = RequestStatus.Broadcasted
  ),
  AssistanceRequest(
    id = "req2",
    userName = "Fatima",
    userType = "Visually impaired",
    location = "City Library",
    destination = "City Library - Braille section",
    distance = "0.8 km",
    urgency = "low",
    helpType = "Finding location",
    requestTime = "5 mins ago",
    status = RequestStatus.Broadcasted
  )
)
