package com.athar.accessibilitymapping.data.volunteer

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtharVolunteerDashboardAssemblerTest {
  private val fixedClock = Clock.fixed(Instant.parse("2026-03-28T12:00:00Z"), ZoneId.of("UTC"))

  @Test
  fun `maps successful payloads with required source priorities`() {
    val assembler = AtharVolunteerDashboardAssembler(
      clock = fixedClock,
      zoneId = ZoneId.of("UTC")
    )

    val model = assembler.assemble(
      AtharVolunteerEndpointBundle(
        history = AtharVolunteerHistoryDto(
          summary = AtharVolunteerHistorySummaryDto(
            thisMonthNetEarnings = 420.0,
            requestsThisWeek = 3
          ),
          data = listOf(
            AtharVolunteerHistoryItemDto(id = "1", assistanceType = "Navigation", eventDateTime = "2026-03-28T10:00:00Z", netAmount = 120.0),
            AtharVolunteerHistoryItemDto(id = "2", assistanceType = "Navigation", eventDateTime = "2026-03-27T10:00:00Z", netAmount = 80.0),
            AtharVolunteerHistoryItemDto(id = "3", assistanceType = "Reading", eventDateTime = "2026-03-25T10:00:00Z", netAmount = 60.0)
          )
        ),
        earnings = AtharVolunteerEarningsDto(
          summary = AtharVolunteerEarningsSummaryDto(
            clearedEarnings = 300.0,
            netEarnings = 700.0,
            grossEarnings = 1000.0,
            totalFees = 300.0
          ),
          monthlyNetEarnings = listOf(
            AtharVolunteerMonthlyNetDto("Feb", 200.0),
            AtharVolunteerMonthlyNetDto("Mar", 420.0)
          )
        ),
        performance = AtharVolunteerPerformanceDto(
          completed = 9,
          pending = 2,
          responseRate = 95f,
          completionRate = 85f,
          onTimeRate = 90f,
          grade = "A"
        ),
        reviews = AtharVolunteerReviewsDto(
          summary = AtharVolunteerReviewsSummaryDto(
            averageRating = 4.6,
            totalReviews = 5
          )
        )
      )
    )

    assertEquals(420.0, model.currentMonthNet, 0.0)
    assertEquals(3, model.historyThisWeekCount)
    assertEquals(260.0, model.historyThisWeekNet, 0.0)
    assertEquals(300.0, model.availableFunds, 0.0)
    assertEquals(2, model.requestTypes.size)
    assertEquals("Navigation", model.requestTypes.first().type)
    assertEquals(66.666, model.requestTypes.first().percent, 0.5)
    assertEquals(4.6, model.averageRating, 0.0)
    assertEquals(5, model.reviewCount)
  }

  @Test
  fun `prefers dedicated analytics payloads for weekly activity and history values`() {
    val assembler = AtharVolunteerDashboardAssembler(
      clock = fixedClock,
      zoneId = ZoneId.of("UTC")
    )

    val model = assembler.assemble(
      AtharVolunteerEndpointBundle(
        history = AtharVolunteerHistoryDto(
          data = listOf(
            AtharVolunteerHistoryItemDto(
              id = "history-1",
              assistanceType = "Navigation",
              requestTimeLabel = "Mar 20, 2026",
              status = "completed",
              grossAmount = 200.0,
              netAmount = 140.0,
              userName = "Layla",
              location = "Downtown"
            )
          )
        ),
        earnings = AtharVolunteerEarningsDto(
          summary = AtharVolunteerEarningsSummaryDto(
            thisWeekNet = 175.0,
            currentMonthLabel = "March 2026",
            currentMonthNet = 350.0,
            netEarnings = 700.0
          )
        ),
        performance = AtharVolunteerPerformanceDto(
          weeklyActivity = listOf(
            AtharVolunteerPerformanceWeeklyActivityDto(dayLabel = "Mon", completedCount = 2),
            AtharVolunteerPerformanceWeeklyActivityDto(dayLabel = "Tue", completedCount = 1)
          ),
          requestTypes = listOf(
            AtharVolunteerPerformanceRequestTypeDto(type = "Navigation", count = 3)
          )
        )
      )
    )

    assertEquals(175.0, model.historyThisWeekNet, 0.0)
    assertEquals(350.0, model.currentMonthNet, 0.0)
    assertEquals("March 2026", model.currentMonthLabel)
    assertEquals(2, model.weeklyActivity.first().requestsCount)
    assertEquals(1, model.historyRecords.size)
    assertEquals("Layla", model.historyRecords.first().userName)
  }

  @Test
  fun `parser supports backend analytics field names for history and performance payloads`() {
    val history = AtharVolunteerPayloadParser.parseHistory(
      Json.parseToJsonElement(
        """
          {
            "counts": { "incoming": 0, "active": 0, "history": 1 },
            "impact": { "totalAssists": 1, "avgRating": 4.5, "thisWeek": 1 },
            "requests": [
              {
                "id": "req-1",
                "userName": "Sara",
                "location": "Nasr City",
                "helpType": "Navigation",
                "requestTime": "Mar 28, 2026",
                "status": "completed",
                "hours": 2,
                "price_per_hour": 50,
                "total_amount_egp": 100.0
              }
            ]
          }
        """.trimIndent()
      )
    )
    val performance = AtharVolunteerPayloadParser.parsePerformance(
      Json.parseToJsonElement(
        """
          {
            "weekly_activity": [
              { "day": "Mon", "completed": 2 },
              { "day": "Tue", "completed": 1 }
            ],
            "request_types": [
              { "name": "Navigation", "value": 3 }
            ]
          }
        """.trimIndent()
      )
    )

    assertEquals(1, history.data.size)
    assertEquals("Navigation", history.data.first().assistanceType)
    assertEquals("Sara", history.data.first().userName)
    assertEquals(100.0, history.data.first().grossAmount ?: 0.0, 0.0)
    assertEquals("Mon", performance.weeklyActivity.first().dayLabel)
    assertEquals(2, performance.weeklyActivity.first().completedCount)
    assertEquals("Navigation", performance.requestTypes.first().type)
  }

  @Test
  fun `parser supports both weekly activity payload variants`() {
    val kotlinStyle = AtharVolunteerPayloadParser.parsePerformance(
      Json.parseToJsonElement(
        """
          {
            "weekly_activity": [
              { "day": "Mon", "completed": 3 },
              { "day": "Tue", "completed": 1 }
            ]
          }
        """.trimIndent()
      )
    )
    val laravelStyle = AtharVolunteerPayloadParser.parsePerformance(
      Json.parseToJsonElement(
        """
          {
            "weekly_activity": [
              { "day": "Mon", "completed_requests": 3, "count": 3 },
              { "day": "Tue", "completed_requests": 1, "count": 1 }
            ]
          }
        """.trimIndent()
      )
    )

    assertEquals(2, kotlinStyle.weeklyActivity.size)
    assertEquals(3, kotlinStyle.weeklyActivity.first().completedCount)
    assertEquals(2, laravelStyle.weeklyActivity.size)
    assertEquals(3, laravelStyle.weeklyActivity.first().completedCount)
    assertEquals(1, laravelStyle.weeklyActivity[1].completedCount)
  }

  @Test
  fun `builds weekly activity from history relative time labels when analytics payload is empty`() {
    val assembler = AtharVolunteerDashboardAssembler(
      clock = fixedClock,
      zoneId = ZoneId.of("UTC")
    )

    val model = assembler.assemble(
      AtharVolunteerEndpointBundle(
        history = AtharVolunteerHistoryDto(
          data = listOf(
            AtharVolunteerHistoryItemDto(id = "1", requestTimeLabel = "2h ago", netAmount = 80.0),
            AtharVolunteerHistoryItemDto(id = "2", requestTimeLabel = "1d ago", netAmount = 40.0)
          )
        ),
        performance = AtharVolunteerPerformanceDto()
      )
    )

    assertEquals(7, model.weeklyActivity.size)
    assertTrue(model.weeklyActivity.sumOf { it.requestsCount } >= 2)
    assertTrue(model.weeklyActivity.sumOf { it.netAmount } >= 120.0)
  }

  @Test
  fun `serializable weekly activity data class resolves Laravel payload via effectiveCompleted`() {
    val json = Json { ignoreUnknownKeys = true }
    // Kotlin backend style: { "day": "Mon", "completed": 3 }
    val kotlinItem = json.decodeFromString<com.athar.accessibilitymapping.data.ApiVolunteerAnalyticsWeeklyActivity>(
      """{ "day": "Mon", "completed": 3 }"""
    )
    assertEquals("Mon", kotlinItem.effectiveDay)
    assertEquals(3, kotlinItem.effectiveCompleted)

    // Laravel backend style: { "day": "Mon", "completed_requests": 3, "count": 3 }
    val laravelItem = json.decodeFromString<com.athar.accessibilitymapping.data.ApiVolunteerAnalyticsWeeklyActivity>(
      """{ "day": "Mon", "completed_requests": 3, "count": 3 }"""
    )
    assertEquals("Mon", laravelItem.effectiveDay)
    assertEquals(3, laravelItem.effectiveCompleted)

    // Edge case: only "count" present
    val countOnly = json.decodeFromString<com.athar.accessibilitymapping.data.ApiVolunteerAnalyticsWeeklyActivity>(
      """{ "day": "Wed", "count": 5 }"""
    )
    assertEquals(5, countOnly.effectiveCompleted)

    // Edge case: "label" instead of "day"
    val labelDay = json.decodeFromString<com.athar.accessibilitymapping.data.ApiVolunteerAnalyticsWeeklyActivity>(
      """{ "label": "Thu", "value": 2 }"""
    )
    assertEquals("Thu", labelDay.effectiveDay)
    assertEquals(2, labelDay.effectiveCompleted)
  }

  @Test
  fun `uses earnings fallback when history current month is missing`() {
    val assembler = AtharVolunteerDashboardAssembler(
      clock = fixedClock,
      zoneId = ZoneId.of("UTC")
    )

    val model = assembler.assemble(
      AtharVolunteerEndpointBundle(
        history = AtharVolunteerHistoryDto(),
        earnings = AtharVolunteerEarningsDto(
          summary = AtharVolunteerEarningsSummaryDto(netEarnings = 155.0)
        )
      )
    )

    assertEquals(155.0, model.currentMonthNet, 0.0)
    assertTrue(model.requestTypes.isEmpty())
    assertTrue(model.weeklyActivity.all { it.requestsCount == 0 })
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AtharVolunteerDashboardUseCaseTest {
  private val dispatcher = StandardTestDispatcher()

  @Test
  fun `returns success with warnings when some endpoints fail`() = runTest(dispatcher) {
    val useCase = AtharVolunteerDashboardUseCase(
      remoteSource = object : AtharVolunteerRemoteSource {
        override suspend fun getImpact(authorization: String) = EndpointResult.Error("impact failed")
        override suspend fun getHistory(authorization: String) = EndpointResult.Success(
          AtharVolunteerHistoryDto(summary = AtharVolunteerHistorySummaryDto(thisMonthNetEarnings = 250.0))
        )
        override suspend fun getEarnings(authorization: String) = EndpointResult.Error("earnings failed")
        override suspend fun getPerformance(authorization: String) = EndpointResult.Success(
          AtharVolunteerPerformanceDto(completed = 2)
        )
        override suspend fun getReviews(authorization: String, page: Int, perPage: Int, rating: Int?) =
          EndpointResult.Success(AtharVolunteerReviewsDto())
      },
      assembler = AtharVolunteerDashboardAssembler(),
      ioDispatcher = dispatcher
    )

    val result = useCase.load("token")

    assertTrue(result is AtharDataResult.Success)
    val success = result as AtharDataResult.Success
    assertEquals(250.0, success.data.currentMonthNet, 0.0)
    assertEquals(2, success.data.completedCount)
    assertTrue(success.warnings.contains("impact failed"))
    assertTrue(success.warnings.contains("earnings failed"))
  }
}
