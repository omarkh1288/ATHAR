package com.athar.accessibilitymapping.data.volunteer

import android.util.Log
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
  companion object {
    private const val TAG = "AtharDashAssembler"
  }

  fun assemble(bundle: AtharVolunteerEndpointBundle): VolunteerDashboardUiModel {
    Log.d(TAG, "assemble: impact=${bundle.impact != null}, history=${bundle.history != null}, " +
      "earnings=${bundle.earnings != null}, performance=${bundle.performance != null}, reviews=${bundle.reviews != null}")

    val history = bundle.history ?: AtharVolunteerHistoryDto()
    val earnings = bundle.earnings ?: AtharVolunteerEarningsDto()
    val performance = bundle.performance ?: AtharVolunteerPerformanceDto()
    val reviews = bundle.reviews ?: AtharVolunteerReviewsDto()
    val impact = bundle.impact ?: AtharVolunteerImpactDto()

    Log.d(TAG, "assemble: performance.weeklyActivity.size=${performance.weeklyActivity.size}, " +
      "performance.badges=${performance.badges}, history.data.size=${history.data.size}")

    val historyWeeklyActivity = buildWeeklyActivity(history.data)
    val weeklyActivityFromPerformance = buildWeeklyActivityFromPerformance(performance)

    Log.d(TAG, "assemble: weeklyActivityFromPerformance.size=${weeklyActivityFromPerformance.size}, " +
      "historyWeeklyActivity.size=${historyWeeklyActivity.size}")

    val weeklyActivity = when {
      // Prefer the dedicated analytics endpoint because the volunteer history payload often
      // contains display labels like "3h ago" instead of raw timestamps.
      weeklyActivityFromPerformance.isNotEmpty() -> weeklyActivityFromPerformance
      historyWeeklyActivity.any { it.requestsCount > 0 || it.netAmount > 0.0 } -> historyWeeklyActivity
      else -> historyWeeklyActivity
    }
    Log.d(TAG, "assemble: chosen weeklyActivity.size=${weeklyActivity.size}, " +
      "values=${weeklyActivity.map { "${it.dateLabel}:${it.requestsCount}" }}")
    val requestTypesFromPerformance = buildRequestTypesFromPerformance(performance)
    val historyRequestTypes = buildRequestTypes(history.data)
    val requestTypes = if (requestTypesFromPerformance.isNotEmpty()) {
      requestTypesFromPerformance
    } else {
      historyRequestTypes
    }
    val historyThisWeekNet = earnings.summary.thisWeekNet ?: deriveCurrentWeekNet(history.data)
    val historyThisWeekCount = history.summary.requestsThisWeek ?: deriveCurrentWeekCount(history.data)
    val currentMonthNet = earnings.summary.currentMonthNet
      ?: history.summary.thisMonthNetEarnings
      ?: monthlyNetFallback(earnings)
    val historyRecords = history.data.map { item -> item.toHistoryRecordUiModel() }

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
      currentMonthLabel = earnings.summary.currentMonthLabel
        ?: YearMonth.now(clock.withZone(zoneId)).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
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
      paymentHistory = resolvePaymentHistory(earnings, history),
      historyRecords = historyRecords,
      performance = VolunteerPerformanceUiModel(
        grade = performance.grade.orEmpty().ifBlank {
          when {
            (performance.completed ?: impact.totalAssists ?: 0) >= 10 -> "A"
            (performance.completed ?: impact.totalAssists ?: 0) >= 5 -> "B"
            (performance.completed ?: impact.totalAssists ?: 0) > 0 -> "C"
            else -> "D"
          }
        },
        headline = performance.headline.orEmpty().ifBlank {
          if ((performance.completed ?: impact.totalAssists ?: 0) > 0) {
            "Performance summary"
          } else {
            "No performance data yet"
          }
        },
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

  private fun resolvePaymentHistory(
    earnings: AtharVolunteerEarningsDto,
    history: AtharVolunteerHistoryDto
  ): List<VolunteerPaymentHistoryItemUiModel> {
    val fromEarnings = earnings.paymentHistory.mapNotNull { item ->
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
      }
    if (fromEarnings.isNotEmpty()) return fromEarnings

    return history.data.mapNotNull { item ->
      VolunteerPaymentHistoryItemUiModel(
        id = item.id.orEmpty(),
        dateLabel = formatDisplayDate(item.eventDateTime ?: item.completedAt ?: item.createdAt ?: item.updatedAt)
          .ifBlank { item.requestTimeLabel.orEmpty() },
        amount = item.grossAmount ?: item.netAmount ?: 0.0,
        netAmount = item.netAmount ?: item.grossAmount ?: 0.0,
        status = item.status.orEmpty(),
        userName = item.userName.orEmpty(),
        hours = item.hours ?: 0
      )
    }
  }

  private fun buildWeeklyActivity(historyItems: List<AtharVolunteerHistoryItemDto>): List<VolunteerWeeklyActivityPointUiModel> {
    val today = LocalDate.now(clock.withZone(zoneId))
    val activityByDate = historyItems
      .mapNotNull { item ->
        val date = parseHistoryItemDate(item)
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

  private fun buildWeeklyActivityFromPerformance(
    performance: AtharVolunteerPerformanceDto
  ): List<VolunteerWeeklyActivityPointUiModel> {
    val apiItems = performance.weeklyActivity
    if (apiItems.isEmpty()) return emptyList()
    return apiItems.map { item ->
      VolunteerWeeklyActivityPointUiModel(
        dateLabel = item.dayLabel.orEmpty().ifBlank { "Day" },
        requestsCount = item.completedCount ?: 0,
        netAmount = 0.0
      )
    }
  }

  private fun buildRequestTypesFromPerformance(
    performance: AtharVolunteerPerformanceDto
  ): List<VolunteerRequestTypeUiModel> {
    val apiItems = performance.requestTypes
      .mapNotNull { item ->
        val count = item.count ?: return@mapNotNull null
        val type = item.type?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        type to count
      }
    if (apiItems.isEmpty()) return emptyList()
    val total = apiItems.sumOf { it.second }.coerceAtLeast(1)
    return apiItems.map { (type, count) ->
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
      val date = parseHistoryItemDate(item)
        ?: return@sumOf 0.0
      if (date in start..end) item.netAmount ?: item.grossAmount ?: 0.0 else 0.0
    }
  }

  private fun deriveCurrentWeekCount(historyItems: List<AtharVolunteerHistoryItemDto>): Int {
    val (start, end) = currentWeekRange()
    return historyItems.count { item ->
      val date = parseHistoryItemDate(item)
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

  private fun monthlyNetFallback(earnings: AtharVolunteerEarningsDto): Double {
    return earnings.monthlyNetEarnings.lastOrNull()?.netAmount
      ?: earnings.summary.netEarnings
      ?: 0.0
  }

  private fun AtharVolunteerHistoryItemDto.toHistoryRecordUiModel(): VolunteerHistoryRecordUiModel {
    return VolunteerHistoryRecordUiModel(
      id = id.orEmpty(),
      userName = userName.orEmpty(),
      helpType = assistanceType.orEmpty().ifBlank { "Assistance" },
      location = location.orEmpty(),
      dateLabel = formatDisplayDate(eventDateTime ?: completedAt ?: createdAt ?: updatedAt)
        .ifBlank { requestTimeLabel.orEmpty() },
      status = status.orEmpty(),
      hours = hours ?: 0,
      grossAmount = grossAmount ?: netAmount ?: 0.0,
      netAmount = netAmount ?: grossAmount ?: 0.0
    )
  }

  private fun parseHistoryItemDate(item: AtharVolunteerHistoryItemDto): LocalDate? {
    return parseLocalDate(item.eventDateTime ?: item.completedAt ?: item.createdAt ?: item.updatedAt)
      ?: parseRelativeDateLabel(item.requestTimeLabel)
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
          runCatching {
            LocalDate.parse(value, DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
          }.getOrNull()
        }
      }
    }
  }

  private fun parseRelativeDateLabel(rawValue: String?): LocalDate? {
    val value = rawValue?.trim().orEmpty().lowercase(locale)
    if (value.isBlank()) return null
    val today = LocalDate.now(clock.withZone(zoneId))
    if (value == "just now") return today

    val match = Regex("""^(\d+)\s*([mhdw])\s*ago$""").matchEntire(value) ?: return null
    val amount = match.groupValues[1].toLongOrNull() ?: return null
    return when (match.groupValues[2]) {
      "m", "h" -> today
      "d" -> today.minusDays(amount)
      "w" -> today.minusWeeks(amount)
      else -> null
    }
  }
}

internal object AtharVolunteerPayloadParser {
  private const val TAG = "AtharPayloadParser"

  fun parseImpact(element: JsonElement): AtharVolunteerImpactDto {
    Log.d(TAG, "parseImpact: raw=${element.toString().take(300)}")
    val payload = unwrapPayloadObject(element)
    val impactObject = payload.obj("impact") ?: payload
    val result = AtharVolunteerImpactDto(
      totalAssists = impactObject.int("total_assists", "totalAssists", "completed"),
      averageRating = impactObject.double("average_rating", "averageRating", "avg_rating", "avgRating"),
      thisWeekCount = impactObject.int("this_week", "thisWeek", "requests_this_week")
    )
    Log.d(TAG, "parseImpact: totalAssists=${result.totalAssists}, avgRating=${result.averageRating}, thisWeek=${result.thisWeekCount}")
    return result
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
        val grossAmount = item.double(
          "total_amount_egp",
          "totalAmountEgp",
          "amount",
          "gross_amount",
          "total_amount",
          "service_fee"
        )
        val pricePerHour = item.double("price_per_hour", "pricePerHour")
        val hours = item.int("hours")
        val fallbackGrossAmount = if (grossAmount != null) {
          grossAmount
        } else if (pricePerHour != null && hours != null) {
          pricePerHour * hours
        } else {
          null
        }
        AtharVolunteerHistoryItemDto(
          id = item.string("id"),
          assistanceType = item.string("assistance_type", "help_type", "helpType", "type"),
          status = item.string("status"),
          eventDateTime = item.string("completed_at", "completedAt", "request_date", "requestDate", "date"),
          requestTimeLabel = item.string("request_time", "requestTime", "date"),
          createdAt = item.string("created_at", "createdAt"),
          completedAt = item.string("completed_at", "completedAt"),
          updatedAt = item.string("updated_at", "updatedAt"),
          netAmount = item.double("net_amount", "net_earnings", "net_amount_egp", "this_month_net_earnings"),
          grossAmount = fallbackGrossAmount,
          hours = hours,
          userName = item.string("user_name", "userName", "name"),
          location = item.string("location")
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
        monthlyChangePercent = summary.double("monthly_change_percent"),
        thisWeekNet = summary.double("this_week_net"),
        currentMonthLabel = summary.string("current_month_label"),
        currentMonthNet = summary.double("current_month_net"),
        lastMonthNet = summary.double("last_month_net")
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
    Log.d(TAG, "parsePerformance: raw=${element.toString().take(500)}")
    val payload = unwrapPayloadObject(element)
    val summary = payload.obj("summary") ?: payload
    Log.d(TAG, "parsePerformance: payload keys=${payload.keys}, summary==payload: ${summary === payload}")

    val weeklyActivityArray = payload.array("weekly_activity") ?: summary.array("weekly_activity")
    Log.d(TAG, "parsePerformance: weeklyActivityArray size=${weeklyActivityArray?.size}, isNull=${weeklyActivityArray == null}")

    val result = AtharVolunteerPerformanceDto(
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
      badges = summary.primitiveArray("badges").orEmpty(),
      weeklyActivity = (weeklyActivityArray ?: emptyList()).map { item ->
        AtharVolunteerPerformanceWeeklyActivityDto(
          dayLabel = item.string("day", "label", "date"),
          completedCount = item.int("completed", "completed_requests", "count", "requests", "value")
        )
      },
      requestTypes = (payload.array("request_types") ?: summary.array("request_types") ?: emptyList()).map { item ->
        AtharVolunteerPerformanceRequestTypeDto(
          type = item.string("name", "type", "label", "assistance_type"),
          count = item.int("value", "count", "requests")
        )
      }
    )
    Log.d(TAG, "parsePerformance: grade=${result.grade}, completed=${result.completed}, " +
      "weeklyActivity.size=${result.weeklyActivity.size}, badges=${result.badges}, " +
      "requestTypes.size=${result.requestTypes.size}")
    return result
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
          issues = item.primitiveArray("issues").orEmpty()
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

  private fun JsonObject.primitiveArray(key: String): List<String>? {
    val value = this[key] as? JsonArray ?: return null
    return value.mapNotNull { element ->
      when (element) {
        is JsonPrimitive -> element.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        is JsonObject -> element.string("value", "name", "label")
        else -> null
      }
    }
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
