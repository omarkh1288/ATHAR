package com.athar.accessibilitymapping.data.volunteer

import kotlinx.serialization.Serializable

sealed interface AtharDataResult<out T> {
  data object Loading : AtharDataResult<Nothing>
  data class Success<T>(
    val data: T,
    val warnings: List<String> = emptyList()
  ) : AtharDataResult<T>
  data class Error(
    val message: String
  ) : AtharDataResult<Nothing>
}

data class VolunteerDashboardUiModel(
  val totalNetEarnings: Double = 0.0,
  val currentMonthNet: Double = 0.0,
  val currentMonthLabel: String = "",
  val monthlyChangePercent: Double = 0.0,
  val completedCount: Int = 0,
  val pendingCount: Int = 0,
  val averageRating: Double = 0.0,
  val reviewCount: Int = 0,
  val historyThisWeekCount: Int = 0,
  val historyThisWeekNet: Double = 0.0,
  val weeklyActivity: List<VolunteerWeeklyActivityPointUiModel> = emptyList(),
  val requestTypes: List<VolunteerRequestTypeUiModel> = emptyList(),
  val availableFunds: Double = 0.0,
  val pendingBalance: Double = 0.0,
  val totalGross: Double = 0.0,
  val totalFees: Double = 0.0,
  val monthlyNetEarnings: List<VolunteerMonthlyNetUiModel> = emptyList(),
  val withdrawalHistory: List<VolunteerWithdrawalHistoryItemUiModel> = emptyList(),
  val paymentHistory: List<VolunteerPaymentHistoryItemUiModel> = emptyList(),
  val performance: VolunteerPerformanceUiModel = VolunteerPerformanceUiModel(),
  val reviews: VolunteerReviewsUiModel = VolunteerReviewsUiModel(),
  val impact: VolunteerImpactUiModel = VolunteerImpactUiModel()
) {
  fun isMeaningfullyEmpty(): Boolean {
    return totalNetEarnings == 0.0 &&
      currentMonthNet == 0.0 &&
      completedCount == 0 &&
      pendingCount == 0 &&
      averageRating == 0.0 &&
      reviewCount == 0 &&
      historyThisWeekCount == 0 &&
      historyThisWeekNet == 0.0 &&
      weeklyActivity.all { it.requestsCount == 0 && it.netAmount == 0.0 } &&
      requestTypes.isEmpty() &&
      availableFunds == 0.0 &&
      monthlyNetEarnings.all { it.netAmount == 0.0 } &&
      withdrawalHistory.isEmpty() &&
      paymentHistory.isEmpty()
  }
}

data class VolunteerWeeklyActivityPointUiModel(
  val dateLabel: String,
  val requestsCount: Int,
  val netAmount: Double
)

data class VolunteerRequestTypeUiModel(
  val type: String,
  val count: Int,
  val percent: Double
)

data class VolunteerMonthlyNetUiModel(
  val monthLabel: String,
  val netAmount: Double
)

data class VolunteerWithdrawalHistoryItemUiModel(
  val id: String,
  val dateLabel: String,
  val amount: Double,
  val method: String,
  val status: String
)

data class VolunteerPaymentHistoryItemUiModel(
  val id: String,
  val dateLabel: String,
  val amount: Double,
  val netAmount: Double,
  val status: String,
  val userName: String,
  val hours: Int
)

data class VolunteerPerformanceUiModel(
  val grade: String = "",
  val headline: String = "",
  val percentile: Int = 0,
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
  val badges: List<String> = emptyList()
)

@Serializable
data class AtharVolunteerPerformanceWeeklyActivityDto(
  val dayLabel: String? = null,
  val completedCount: Int? = null
)

@Serializable
data class AtharVolunteerPerformanceRequestTypeDto(
  val type: String? = null,
  val count: Int? = null
)

data class VolunteerReviewsUiModel(
  val averageRating: Double = 0.0,
  val totalReviews: Int = 0,
  val reviews: List<VolunteerReviewItemUiModel> = emptyList()
)

data class VolunteerReviewItemUiModel(
  val id: String,
  val userName: String,
  val rating: Int,
  val comment: String,
  val dateLabel: String,
  val issues: List<String> = emptyList()
)

data class VolunteerImpactUiModel(
  val totalAssists: Int = 0,
  val averageRating: Double = 0.0,
  val thisWeekCount: Int = 0
)

@Serializable
data class AtharVolunteerImpactDto(
  val totalAssists: Int? = null,
  val averageRating: Double? = null,
  val thisWeekCount: Int? = null
)

@Serializable
data class AtharVolunteerHistoryDto(
  val summary: AtharVolunteerHistorySummaryDto = AtharVolunteerHistorySummaryDto(),
  val data: List<AtharVolunteerHistoryItemDto> = emptyList()
)

@Serializable
data class AtharVolunteerHistorySummaryDto(
  val thisMonthNetEarnings: Double? = null,
  val requestsThisWeek: Int? = null
)

@Serializable
data class AtharVolunteerHistoryItemDto(
  val id: String? = null,
  val assistanceType: String? = null,
  val status: String? = null,
  val eventDateTime: String? = null,
  val createdAt: String? = null,
  val completedAt: String? = null,
  val updatedAt: String? = null,
  val netAmount: Double? = null,
  val grossAmount: Double? = null,
  val hours: Int? = null,
  val userName: String? = null
)

@Serializable
data class AtharVolunteerEarningsDto(
  val summary: AtharVolunteerEarningsSummaryDto = AtharVolunteerEarningsSummaryDto(),
  val monthlyNetEarnings: List<AtharVolunteerMonthlyNetDto> = emptyList(),
  val withdrawalHistory: List<AtharVolunteerWithdrawalHistoryDto> = emptyList(),
  val paymentHistory: List<AtharVolunteerPaymentHistoryDto> = emptyList()
)

@Serializable
data class AtharVolunteerEarningsSummaryDto(
  val clearedEarnings: Double? = null,
  val netEarnings: Double? = null,
  val pendingBalance: Double? = null,
  val grossEarnings: Double? = null,
  val totalFees: Double? = null,
  val monthlyChangePercent: Double? = null
)

@Serializable
data class AtharVolunteerMonthlyNetDto(
  val monthLabel: String? = null,
  val netAmount: Double? = null
)

@Serializable
data class AtharVolunteerWithdrawalHistoryDto(
  val id: String? = null,
  val dateTime: String? = null,
  val amount: Double? = null,
  val method: String? = null,
  val status: String? = null
)

@Serializable
data class AtharVolunteerPaymentHistoryDto(
  val id: String? = null,
  val dateTime: String? = null,
  val amount: Double? = null,
  val netAmount: Double? = null,
  val status: String? = null,
  val userName: String? = null,
  val hours: Int? = null
)

@Serializable
data class AtharVolunteerPerformanceDto(
  val grade: String? = null,
  val headline: String? = null,
  val percentile: Int? = null,
  val responseRate: Float? = null,
  val completionRate: Float? = null,
  val averageRating: Float? = null,
  val onTimeRate: Float? = null,
  val completed: Int? = null,
  val pending: Int? = null,
  val usersHelped: Int? = null,
  val positiveReviews: Int? = null,
  val fiveStarRatings: Int? = null,
  val totalReviews: Int? = null,
  val badges: List<String> = emptyList(),
  val weeklyActivity: List<AtharVolunteerPerformanceWeeklyActivityDto> = emptyList(),
  val requestTypes: List<AtharVolunteerPerformanceRequestTypeDto> = emptyList()
)

@Serializable
data class AtharVolunteerReviewsDto(
  val summary: AtharVolunteerReviewsSummaryDto = AtharVolunteerReviewsSummaryDto(),
  val reviews: List<AtharVolunteerReviewDto> = emptyList()
)

@Serializable
data class AtharVolunteerReviewsSummaryDto(
  val averageRating: Double? = null,
  val totalReviews: Int? = null
)

@Serializable
data class AtharVolunteerReviewDto(
  val id: String? = null,
  val userName: String? = null,
  val rating: Int? = null,
  val comment: String? = null,
  val dateTime: String? = null,
  val issues: List<String> = emptyList()
)

data class AtharVolunteerEndpointBundle(
  val impact: AtharVolunteerImpactDto? = null,
  val history: AtharVolunteerHistoryDto? = null,
  val earnings: AtharVolunteerEarningsDto? = null,
  val performance: AtharVolunteerPerformanceDto? = null,
  val reviews: AtharVolunteerReviewsDto? = null
)

sealed interface EndpointResult<out T> {
  data class Success<T>(val data: T) : EndpointResult<T>
  data class Error(val message: String) : EndpointResult<Nothing>
}

sealed interface VolunteerAnalyticsUiState {
  data object Loading : VolunteerAnalyticsUiState
  data class Content(
    val model: VolunteerDashboardUiModel,
    val warnings: List<String> = emptyList()
  ) : VolunteerAnalyticsUiState
  data class Empty(
    val model: VolunteerDashboardUiModel,
    val message: String,
    val warnings: List<String> = emptyList()
  ) : VolunteerAnalyticsUiState
  data class Error(
    val message: String
  ) : VolunteerAnalyticsUiState
}
