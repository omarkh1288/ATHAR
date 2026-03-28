package com.athar.accessibilitymapping.data.volunteer

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class AtharVolunteerDashboardAssembler(
  private val clock: Clock = Clock.systemDefaultZone(),
  private val zoneId: ZoneId = ZoneId.systemDefault(),
  private val locale: Locale = Locale.getDefault()
) {
  fun assemble(bundle: AtharVolunteerEndpointBundle): VolunteerDashboardUiModel {
    val history = bundle.history ?: AtharVolunteerHistoryDto()
    val earnings = bundle.earnings ?: AtharVolunteerEarningsDto()
    val performance = bundle.performance ?: AtharVolunteerPerformanceDto()
    val reviews = bundle.reviews ?: AtharVolunteerReviewsDto()
    val impact = bundle.impact ?: AtharVolunteerImpactDto()

    val weeklyActivity = buildWeeklyActivity(history.data)
    val requestTypes = buildRequestTypes(history.data)
    val historyThisWeekNet = deriveCurrentWeekNet(history.data)
    val historyThisWeekCount = history.summary.requestsThisWeek ?: deriveCurrentWeekCount(history.data)
    val currentMonthNet = history.summary.thisMonthNetEarnings ?: earnings.summary.netEarnings ?: 0.0

    val monthlyNetEarnings = earnings.monthlyNetEarnings.mapNotNull { item ->
      val netAmount = item.netAmount ?: return@mapNotNull null
      VolunteerMonthlyNetUiModel(
        monthLabel = item.monthLabel.orEmpty().ifBlank { "Month" },
        netAmount = netAmount
      )
    }

    val reviewSummaryAverage = reviews.summary.averageRating
      ?: performance.averageRating?.toDouble()
      ?: impact.averageRating
      ?: 0.0
    val reviewSummaryTotal = reviews.summary.totalReviews
      ?: performance.totalReviews
      ?: 0

    return VolunteerDashboardUiModel(
      totalNetEarnings = earnings.summary.netEarnings ?: monthlyNetEarnings.sumOf { it.netAmount },
      currentMonthNet = currentMonthNet,
      currentMonthLabel = YearMonth.now(clock.withZone(zoneId))
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
      monthlyChangePercent = earnings.summary.monthlyChangePercent ?: deriveMonthlyChange(monthlyNetEarnings),
      completedCount = performance.completed ?: impact.totalAssists ?: 0,
      pendingCount = performance.pending ?: 0,
      averageRating = reviewSummaryAverage,
      reviewCount = reviewSummaryTotal,
      historyThisWeekCount = historyThisWeekCount,
      historyThisWeekNet = historyThisWeekNet,
      weeklyActivity = weeklyActivity,
      requestTypes = requestTypes,
      availableFunds = earnings.summary.clearedEarnings ?: 0.0,
      pendingBalance = earnings.summary.pendingBalance ?: 0.0,
      totalGross = earnings.summary.grossEarnings ?: 0.0,
      totalFees = earnings.summary.totalFees ?: 0.0,
      monthlyNetEarnings = monthlyNetEarnings,
      withdrawalHistory = earnings.withdrawalHistory.mapNotNull { item ->
        val amount = item.amount ?: return@mapNotNull null
        VolunteerWithdrawalHistoryItemUiModel(
          id = item.id.orEmpty(),
          dateLabel = formatDisplayDate(item.dateTime),
          amount = amount,
          method = item.method.orEmpty(),
          status = item.status.orEmpty()
        )
      },
      paymentHistory = earnings.paymentHistory.mapNotNull { item ->
        val amount = item.amount ?: return@mapNotNull null
        VolunteerPaymentHistoryItemUiModel(
          id = item.id.orEmpty(),
          dateLabel = formatDisplayDate(item.dateTime),
          amount = amount,
          netAmount = item.netAmount ?: amount,
          status = item.status.orEmpty(),
          userName = item.userName.orEmpty(),
          hours = item.hours ?: 0
        )
      },
      performance = VolunteerPerformanceUiModel(
        grade = performance.grade.orEmpty(),
        headline = performance.headline.orEmpty(),
        percentile = performance.percentile ?: 0,
        responseRate = performance.responseRate ?: 0f,
        completionRate = performance.completionRate ?: 0f,
        averageRating = performance.averageRating ?: 0f,
        onTimeRate = performance.onTimeRate ?: 0f,
        completed = performance.completed ?: impact.totalAssists ?: 0,
        pending = performance.pending ?: 0,
        usersHelped = performance.usersHelped ?: 0,
        positiveReviews = performance.positiveReviews ?: 0,
        fiveStarRatings = performance.fiveStarRatings ?: 0,
        totalReviews = performance.totalReviews ?: reviewSummaryTotal,
        badges = performance.badges.filter { it.isNotBlank() }
      ),
      reviews = VolunteerReviewsUiModel(
        averageRating = reviewSummaryAverage,
        totalReviews = reviewSummaryTotal,
        reviews = reviews.reviews.map { item ->
          VolunteerReviewItemUiModel(
            id = item.id.orEmpty(),
            userName = item.userName.orEmpty(),
            rating = item.rating ?: 0,
            comment = item.comment.orEmpty(),
            dateLabel = formatDisplayDate(item.dateTime),
            issues = item.issues
          )
        }
      ),
      impact = VolunteerImpactUiModel(
        totalAssists = impact.totalAssists ?: performance.completed ?: 0,
        averageRating = impact.averageRating ?: reviewSummaryAverage,
        thisWeekCount = impact.thisWeekCount ?: historyThisWeekCount
      )
    )
  }

  private fun buildWeeklyActivity(historyItems: List<AtharVolunteerHistoryItemDto>): List<VolunteerWeeklyActivityPointUiModel> {
    val today = LocalDate.now(clock.withZone(zoneId))
    val activityByDate = historyItems
      .mapNotNull { item ->
        val date = parseLocalDate(item.eventDateTime ?: item.completedAt ?: item.createdAt ?: item.updatedAt)
          ?: return@mapNotNull null
        date to item
      }
      .groupBy({ it.first }, { it.second })

    return (6L downTo 0L).map { daysAgo ->
      val targetDate = today.minusDays(daysAgo)
      val items = activityByDate[targetDate].orEmpty()
      VolunteerWeeklyActivityPointUiModel(
        dateLabel = targetDate.format(DateTimeFormatter.ofPattern("EEE", locale)),
        requestsCount = items.size,
        netAmount = items.sumOf { it.netAmount ?: it.grossAmount ?: 0.0 }
      )
    }
  }

  private fun buildRequestTypes(historyItems: List<AtharVolunteerHistoryItemDto>): List<VolunteerRequestTypeUiModel> {
    if (historyItems.isEmpty()) return emptyList()
    val grouped = historyItems
      .groupingBy {
        it.assistanceType?.takeIf { value -> value.isNotBlank() }
          ?: it.status?.replace('_', ' ')?.replaceFirstChar { ch -> ch.titlecase(locale) }
          ?: "Other"
      }
      .eachCount()
      .toList()
      .sortedByDescending { it.second }
    val total = grouped.sumOf { it.second }.coerceAtLeast(1)
    return grouped.map { (type, count) ->
      VolunteerRequestTypeUiModel(
        type = type,
        count = count,
        percent = count * 100.0 / total
      )
    }
  }

  private fun deriveCurrentWeekNet(historyItems: List<AtharVolunteerHistoryItemDto>): Double {
    val (start, end) = currentWeekRange()
    return historyItems.sumOf { item ->
      val date = parseLocalDate(item.eventDateTime ?: item.completedAt ?: item.createdAt ?: item.updatedAt)
        ?: return@sumOf 0.0
      if (date in start..end) item.netAmount ?: item.grossAmount ?: 0.0 else 0.0
    }
  }

  private fun deriveCurrentWeekCount(historyItems: List<AtharVolunteerHistoryItemDto>): Int {
    val (start, end) = currentWeekRange()
    return historyItems.count { item ->
      val date = parseLocalDate(item.eventDateTime ?: item.completedAt ?: item.createdAt ?: item.updatedAt)
        ?: return@count false
      date in start..end
    }
  }

  private fun currentWeekRange(): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now(clock.withZone(zoneId))
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val start = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    return start to start.plusDays(6)
  }

  private fun deriveMonthlyChange(monthlyNetEarnings: List<VolunteerMonthlyNetUiModel>): Double {
    val current = monthlyNetEarnings.lastOrNull()?.netAmount ?: return 0.0
    val previous = monthlyNetEarnings.dropLast(1).lastOrNull()?.netAmount ?: return if (current == 0.0) 0.0 else 100.0
    if (previous == 0.0) return if (current == 0.0) 0.0 else 100.0
    return ((current - previous) / previous) * 100.0
  }

  private fun formatDisplayDate(rawValue: String?): String {
    val date = parseLocalDate(rawValue) ?: return rawValue.orEmpty()
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", locale))
  }

  private fun parseLocalDate(rawValue: String?): LocalDate? {
    val value = rawValue?.trim().orEmpty()
    if (value.isBlank()) return null
    return try {
      Instant.parse(value).atZone(zoneId).toLocalDate()
    } catch (_: DateTimeParseException) {
      try {
        java.time.OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalDate()
      } catch (_: DateTimeParseException) {
        try {
          LocalDate.parse(value)
        } catch (_: DateTimeParseException) {
          null
        }
      }
    }
  }
}

internal object AtharVolunteerPayloadParser {
  fun parseImpact(element: JsonElement): AtharVolunteerImpactDto {
    val payload = unwrapPayloadObject(element)
    val impactObject = payload.obj("impact") ?: payload
    return AtharVolunteerImpactDto(
      totalAssists = impactObject.int("total_assists", "totalAssists", "completed"),
      averageRating = impactObject.double("average_rating", "averageRating", "avg_rating", "avgRating"),
      thisWeekCount = impactObject.int("this_week", "thisWeek", "requests_this_week")
    )
  }

  fun parseHistory(element: JsonElement): AtharVolunteerHistoryDto {
    val payload = unwrapPayloadObject(element)
    val summarySource = payload.obj("summary") ?: payload.obj("impact") ?: payload
    val items = payload.array("data")
      ?: payload.array("requests")
      ?: payload.obj("history")?.array("data")
      ?: emptyList()

    return AtharVolunteerHistoryDto(
      summary = AtharVolunteerHistorySummaryDto(
        thisMonthNetEarnings = summarySource.double("this_month_net_earnings", "this_month_net", "current_month_net"),
        requestsThisWeek = summarySource.int("requests_this_week", "this_week", "thisWeek")
      ),
      data = items.map { item ->
        AtharVolunteerHistoryItemDto(
          id = item.string("id"),
          assistanceType = item.string("assistance_type", "help_type", "helpType", "type"),
          status = item.string("status"),
          eventDateTime = item.string("completed_at", "completedAt", "request_date", "requestDate", "date"),
          createdAt = item.string("created_at", "createdAt", "request_time", "requestTime"),
          completedAt = item.string("completed_at", "completedAt"),
          updatedAt = item.string("updated_at", "updatedAt"),
          netAmount = item.double("net_amount", "net_earnings", "net_amount_egp", "this_month_net_earnings"),
          grossAmount = item.double("amount", "gross_amount", "total_amount", "total_amount_egp", "service_fee"),
          hours = item.int("hours"),
          userName = item.string("user_name", "userName", "name")
        )
      }
    )
  }

  fun parseEarnings(element: JsonElement): AtharVolunteerEarningsDto {
    val payload = unwrapPayloadObject(element)
    val summary = payload.obj("summary") ?: payload
    return AtharVolunteerEarningsDto(
      summary = AtharVolunteerEarningsSummaryDto(
        clearedEarnings = summary.double("cleared_earnings", "available_balance", "available_funds"),
        netEarnings = summary.double("net_earnings", "total_net"),
        pendingBalance = summary.double("pending_balance", "pending_earnings"),
        grossEarnings = summary.double("gross_earnings", "total_gross"),
        totalFees = summary.double("total_fees", "fees"),
        monthlyChangePercent = summary.double("monthly_change_percent")
      ),
      monthlyNetEarnings = (payload.array("monthly_net_earnings") ?: payload.array("monthly_earnings") ?: emptyList()).map { item ->
        AtharVolunteerMonthlyNetDto(
          monthLabel = item.string("month", "label", "month_label"),
          netAmount = item.double("net", "net_earnings", "amount")
        )
      },
      withdrawalHistory = (payload.array("withdrawal_history") ?: emptyList()).map { item ->
        AtharVolunteerWithdrawalHistoryDto(
          id = item.string("id"),
          dateTime = item.string("date", "created_at", "createdAt"),
          amount = item.double("amount"),
          method = item.string("method"),
          status = item.string("status")
        )
      },
      paymentHistory = (payload.array("payment_history") ?: emptyList()).map { item ->
        AtharVolunteerPaymentHistoryDto(
          id = item.string("id"),
          dateTime = item.string("date", "created_at", "createdAt"),
          amount = item.double("gross", "amount"),
          netAmount = item.double("net", "net_amount"),
          status = item.string("status"),
          userName = item.string("user", "user_name", "userName"),
          hours = item.int("hours")
        )
      }
    )
  }

  fun parsePerformance(element: JsonElement): AtharVolunteerPerformanceDto {
    val payload = unwrapPayloadObject(element)
    val summary = payload.obj("summary") ?: payload
    return AtharVolunteerPerformanceDto(
      grade = summary.string("grade"),
      headline = summary.string("headline"),
      percentile = summary.int("percentile"),
      responseRate = summary.float("response_rate", "responseRate"),
      completionRate = summary.float("completion_rate", "completionRate"),
      averageRating = summary.float("average_rating", "averageRating"),
      onTimeRate = summary.float("on_time_rate", "onTimeRate"),
      completed = summary.int("completed"),
      pending = summary.int("pending"),
      usersHelped = summary.int("users_helped", "usersHelped"),
      positiveReviews = summary.int("positive_reviews", "positiveReviews"),
      fiveStarRatings = summary.int("five_star_ratings", "fiveStarRatings"),
      totalReviews = summary.int("total_reviews", "totalReviews"),
      badges = summary.array("badges").orEmpty().mapNotNull { it.primitiveContent() ?: it.string("value") }
    )
  }

  fun parseReviews(element: JsonElement): AtharVolunteerReviewsDto {
    val payload = unwrapPayloadObject(element)
    val summary = payload.obj("summary") ?: payload
    val reviewItems = payload.array("data") ?: payload.array("reviews") ?: emptyList()
    return AtharVolunteerReviewsDto(
      summary = AtharVolunteerReviewsSummaryDto(
        averageRating = summary.double("average_rating", "averageRating"),
        totalReviews = summary.int("total_reviews", "total", "count")
      ),
      reviews = reviewItems.map { item ->
        AtharVolunteerReviewDto(
          id = item.string("id"),
          userName = item.string("user_name", "userName", "name"),
          rating = item.int("rating"),
          comment = item.string("comment", "review"),
          dateTime = item.string("date", "created_at", "createdAt"),
          issues = item.array("issues").orEmpty().mapNotNull { issue -> issue.primitiveContent() }
        )
      }
    )
  }

  private fun unwrapPayloadObject(element: JsonElement): JsonObject {
    val root = element as? JsonObject ?: return JsonObject(emptyMap())
    val hasWrapper = root["success"]?.jsonPrimitive?.booleanOrNull != null || root.containsKey("message")
    val wrappedData = root["data"] as? JsonObject
    return if (hasWrapper && wrappedData != null) wrappedData else root
  }

  private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

  private fun JsonObject.array(key: String): List<JsonObject>? {
    val value = this[key] as? JsonArray ?: return null
    return value.mapNotNull { it as? JsonObject }
  }

  private fun JsonObject.string(vararg keys: String): String? {
    keys.forEach { key ->
      val value = primitive(key)?.contentOrNull?.trim()
      if (!value.isNullOrBlank()) return value
    }
    return null
  }

  private fun JsonObject.int(vararg keys: String): Int? {
    keys.forEach { key ->
      val primitive = primitive(key) ?: return@forEach
      primitive.intOrNull?.let { return it }
      primitive.longOrNull?.toInt()?.let { return it }
      primitive.doubleOrNull?.toInt()?.let { return it }
      primitive.contentOrNull?.toIntOrNull()?.let { return it }
    }
    return null
  }

  private fun JsonObject.float(vararg keys: String): Float? = double(*keys)?.toFloat()

  private fun JsonObject.double(vararg keys: String): Double? {
    keys.forEach { key ->
      val primitive = primitive(key) ?: return@forEach
      primitive.doubleOrNull?.let { return it }
      primitive.longOrNull?.toDouble()?.let { return it }
      primitive.intOrNull?.toDouble()?.let { return it }
      primitive.contentOrNull?.replace(",", "")?.toDoubleOrNull()?.let { return it }
    }
    return null
  }

  private fun JsonObject.primitive(key: String): JsonPrimitive? {
    val value = this[key]
    return if (value == null || value == JsonNull) null else value as? JsonPrimitive
  }

  private fun JsonElement.primitiveContent(): String? = (this as? JsonPrimitive)?.contentOrNull
}
