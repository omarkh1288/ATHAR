package com.athar.accessibilitymapping.data

enum class UserRole {
  User,
  Volunteer
}

enum class RequestStatus {
  Created,
  Broadcasted,
  Accepted,
  InProgress,
  Completed,
  Rated,
  Archived,
  Cancelled,
  NoVolunteer
}

data class LocationFeatures(
  val ramp: Boolean,
  val elevator: Boolean,
  val accessibleToilet: Boolean,
  val accessibleParking: Boolean,
  val wideEntrance: Boolean,
  val brailleSignage: Boolean
)

data class Location(
  val id: String,
  val name: String,
  val category: String,
  val lat: Double,
  val lng: Double,
  val rating: Double,
  val totalRatings: Int,
  val features: LocationFeatures,
  val recentReports: List<String>,
  val distance: String
)

data class VolunteerRequest(
  val id: String,
  val userId: String,
  val userName: String,
  val userType: String,
  val location: String,
  val requestTime: String,
  val status: String,
  val volunteerName: String?,
  val description: String,
  val hours: Int = 1,
  val pricePerHour: Int = 50,
  val totalAmountEgp: Int? = null
)

data class UserProfile(
  val name: String,
  val email: String,
  val phone: String,
  val disabilityType: String,
  val memberSince: String,
  val contributionStats: ContributionStats
)

data class ContributionStats(
  val ratingsSubmitted: Int,
  val reportsSubmitted: Int,
  val helpfulVotes: Int
)

data class AssistanceRequest(
  val id: String,
  val userName: String,
  val userType: String,
  val location: String,
  val destination: String,
  val distance: String,
  val urgency: String,
  val helpType: String,
  val requestTime: String,
  val status: RequestStatus,
  val hours: Int = 1,
  val pricePerHour: Int = 50,
  val totalAmountEgp: Int? = null
)

data class VolunteerDashboardCounts(
  val incoming: Int = 0,
  val active: Int = 0,
  val history: Int = 0
)

data class VolunteerIncomingAlert(
  val count: Int = 0,
  val message: String = ""
)

data class VolunteerImpact(
  val totalAssists: Int = 0,
  val avgRating: Float = 0f,
  val thisWeek: Int = 0
)

data class VolunteerIncomingDashboard(
  val counts: VolunteerDashboardCounts = VolunteerDashboardCounts(),
  val incomingAlert: VolunteerIncomingAlert = VolunteerIncomingAlert(),
  val requests: List<AssistanceRequest> = emptyList()
)

data class VolunteerActiveDashboard(
  val counts: VolunteerDashboardCounts = VolunteerDashboardCounts(),
  val statusBanner: String = "Assistance in Progress",
  val requests: List<AssistanceRequest> = emptyList()
)

data class VolunteerHistoryDashboard(
  val counts: VolunteerDashboardCounts = VolunteerDashboardCounts(),
  val impact: VolunteerImpact = VolunteerImpact(),
  val requests: List<AssistanceRequest> = emptyList()
)

data class VolunteerImpactDashboard(
  val counts: VolunteerDashboardCounts = VolunteerDashboardCounts(),
  val impact: VolunteerImpact = VolunteerImpact()
)

enum class VolunteerApprovalStatus {
  Approved,
  PendingApproval,
  Rejected,
  Inactive,
  Unknown
}

data class VolunteerAnalyticsVolunteerSummary(
  val id: String = "",
  val fullName: String = "",
  val email: String = "",
  val phone: String = "",
  val location: String = "",
  val memberSince: String = "",
  val volunteerLive: Boolean = false,
  val volunteerStatus: String? = null,
  val roleVerifiedAt: String? = null,
  val approvalStatus: VolunteerApprovalStatus = VolunteerApprovalStatus.Unknown,
  val isActive: Boolean = true
)

enum class AnalyticsRecordStatus {
  Completed,
  Pending
}

data class VolunteerAnalyticsMonthlyEarning(
  val month: String,
  val gross: Double,
  val net: Double,
  val fee: Double
)

data class VolunteerAnalyticsWeeklyActivity(
  val day: String,
  val completed: Int
)

data class VolunteerAnalyticsRequestTypeShare(
  val name: String,
  val value: Int
)

data class VolunteerAnalyticsPaymentRecord(
  val id: String,
  val date: String,
  val user: String,
  val hours: Int,
  val gross: Double,
  val net: Double,
  val status: AnalyticsRecordStatus
)

data class VolunteerAnalyticsWithdrawalRecord(
  val id: String,
  val date: String,
  val amount: Double,
  val method: String,
  val status: AnalyticsRecordStatus
)

data class VolunteerAnalyticsEarnings(
  val availableBalance: Double = 0.0,
  val pendingBalance: Double = 0.0,
  val totalGross: Double = 0.0,
  val totalFees: Double = 0.0,
  val totalNet: Double = 0.0,
  val thisWeekNet: Double = 0.0,
  val currentMonthLabel: String = "",
  val currentMonthNet: Double = 0.0,
  val lastMonthNet: Double = 0.0,
  val monthlyChangePercent: Double = 0.0,
  val monthlyEarnings: List<VolunteerAnalyticsMonthlyEarning> = emptyList(),
  val withdrawalHistory: List<VolunteerAnalyticsWithdrawalRecord> = emptyList(),
  val paymentHistory: List<VolunteerAnalyticsPaymentRecord> = emptyList()
)

data class VolunteerAnalyticsPerformance(
  val grade: String = "C",
  val headline: String = "",
  val percentile: Int = 50,
  val responseRate: Float = 0f,
  val completionRate: Float = 0f,
  val averageRating: Float = 0f,
  val onTimeRate: Float = 0f,
  val completed: Int = 0,
  val pending: Int = 0,
  val usersHelped: Int = 0,
  val positiveReviews: Int = 0,
  val fiveStarRatings: Int = 0,
  val totalReviews: Int = 0,
  val badges: List<String> = emptyList(),
  val weeklyActivity: List<VolunteerAnalyticsWeeklyActivity> = emptyList(),
  val requestTypes: List<VolunteerAnalyticsRequestTypeShare> = emptyList()
)

data class VolunteerAnalyticsReview(
  val id: String,
  val userName: String,
  val rating: Int,
  val comment: String,
  val date: String,
  val issues: List<String> = emptyList()
)

data class VolunteerAnalyticsReviews(
  val reviews: List<VolunteerAnalyticsReview> = emptyList(),
  val page: Int = 1,
  val perPage: Int = 10,
  val total: Int = 0,
  val averageRating: Double = 0.0
)

data class VolunteerAnalyticsSnapshot(
  val volunteer: VolunteerAnalyticsVolunteerSummary = VolunteerAnalyticsVolunteerSummary(),
  val impact: VolunteerImpactDashboard = VolunteerImpactDashboard(),
  val earnings: VolunteerAnalyticsEarnings = VolunteerAnalyticsEarnings(),
  val performance: VolunteerAnalyticsPerformance = VolunteerAnalyticsPerformance(),
  val reviews: VolunteerAnalyticsReviews = VolunteerAnalyticsReviews(),
  val isRemoteConnected: Boolean = false,
  val warningMessage: String? = null
)
