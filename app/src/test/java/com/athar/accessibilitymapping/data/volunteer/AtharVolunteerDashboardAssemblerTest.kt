package com.athar.accessibilitymapping.data.volunteer

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
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
        override suspend fun getReviews(authorization: String) = EndpointResult.Success(AtharVolunteerReviewsDto())
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
