package com.athar.accessibilitymapping.data.volunteer

import android.content.Context
import android.util.Log
import com.athar.accessibilitymapping.BuildConfig
import com.athar.accessibilitymapping.data.AuthSessionStore
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Response
import retrofit2.Retrofit

class AtharVolunteerDashboardRepository(
  context: Context,
  private val sessionStore: AuthSessionStore = AuthSessionStore(context.applicationContext),
  private val remoteSource: AtharVolunteerRemoteSource = RetrofitAtharVolunteerRemoteSource(),
  private val assembler: AtharVolunteerDashboardAssembler = AtharVolunteerDashboardAssembler(),
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  suspend fun loadDashboard(
    request: VolunteerAnalyticsRequest = VolunteerAnalyticsRequest(),
    forceRefresh: Boolean = false
  ): AtharDataResult<VolunteerDashboardUiModel> = withContext(ioDispatcher) {
    val cacheKey = buildCacheKey(request)
    if (!forceRefresh) {
      cachedDashboard(cacheKey)?.let { return@withContext it }
    }
    val accessToken = sessionStore.getAccessToken()
      ?: return@withContext AtharDataResult.Error("You are not logged in.")
    if (accessToken.startsWith("local-")) {
      return@withContext AtharDataResult.Error("Volunteer analytics require a live authenticated backend session.")
    }
    val result = AtharVolunteerDashboardUseCase(
      remoteSource = remoteSource,
      assembler = assembler,
      ioDispatcher = ioDispatcher
    ).load(accessToken, request)
    if (result is AtharDataResult.Success) {
      dashboardCache[cacheKey] = CachedVolunteerDashboard(result)
    }
    result
  }

  fun peekCachedDashboard(
    request: VolunteerAnalyticsRequest = VolunteerAnalyticsRequest()
  ): AtharDataResult.Success<VolunteerDashboardUiModel>? {
    return cachedDashboard(buildCacheKey(request))
  }

  fun invalidateDashboardCache(
    request: VolunteerAnalyticsRequest? = null
  ) {
    if (request == null) {
      dashboardCache.clear()
    } else {
      dashboardCache.remove(buildCacheKey(request))
    }
  }

  private fun cachedDashboard(cacheKey: String): AtharDataResult.Success<VolunteerDashboardUiModel>? {
    val cached = dashboardCache[cacheKey] ?: return null
    return if (System.currentTimeMillis() - cached.cachedAtEpochMillis <= ANALYTICS_CACHE_TTL_MS) {
      cached.result
    } else {
      dashboardCache.remove(cacheKey)
      null
    }
  }

  private fun buildCacheKey(request: VolunteerAnalyticsRequest): String {
    return request.cacheKey()
  }

  private data class CachedVolunteerDashboard(
    val result: AtharDataResult.Success<VolunteerDashboardUiModel>,
    val cachedAtEpochMillis: Long = System.currentTimeMillis()
  )

  companion object {
    // Keep analytics hot during normal navigation, but expire stale snapshots automatically.
    private const val ANALYTICS_CACHE_TTL_MS = 5 * 60 * 1000L
    private val dashboardCache = ConcurrentHashMap<String, CachedVolunteerDashboard>()

    fun clearCachedDashboards() {
      dashboardCache.clear()
    }
  }
}

class AtharVolunteerDashboardUseCase(
  private val remoteSource: AtharVolunteerRemoteSource,
  private val assembler: AtharVolunteerDashboardAssembler,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  companion object {
    private const val TAG = "AtharDashUseCase"
  }

  suspend fun load(
    accessToken: String,
    request: VolunteerAnalyticsRequest = VolunteerAnalyticsRequest()
  ): AtharDataResult<VolunteerDashboardUiModel> = withContext(ioDispatcher) {
    val authorization = "Bearer $accessToken"
    Log.d(TAG, "load: fetching all analytics endpoints...")
    supervisorScope {
      val impactDeferred = async { remoteSource.getImpact(authorization) }
      val historyDeferred = async { remoteSource.getHistory(authorization) }
      val earningsDeferred = async { remoteSource.getEarnings(authorization) }
      val performanceDeferred = async { remoteSource.getPerformance(authorization) }
      val reviewsDeferred = async {
        remoteSource.getReviews(
          authorization = authorization,
          page = request.page,
          perPage = request.perPage,
          rating = request.rating
        )
      }

      val impact = impactDeferred.await()
      val history = historyDeferred.await()
      val earnings = earningsDeferred.await()
      val performance = performanceDeferred.await()
      val reviews = reviewsDeferred.await()

      Log.d(TAG, "load: impact=${if (impact is EndpointResult.Success) "OK" else "FAIL(${(impact as? EndpointResult.Error)?.message})"}, " +
        "history=${if (history is EndpointResult.Success) "OK" else "FAIL(${(history as? EndpointResult.Error)?.message})"}, " +
        "earnings=${if (earnings is EndpointResult.Success) "OK" else "FAIL(${(earnings as? EndpointResult.Error)?.message})"}, " +
        "performance=${if (performance is EndpointResult.Success) "OK" else "FAIL(${(performance as? EndpointResult.Error)?.message})"}, " +
        "reviews=${if (reviews is EndpointResult.Success) "OK" else "FAIL(${(reviews as? EndpointResult.Error)?.message})"}")

      val allResults = listOf(impact, history, earnings, performance, reviews)

      val warnings = allResults
        .mapNotNull { (it as? EndpointResult.Error)?.message }
        .distinct()

      if (allResults.all { it is EndpointResult.Error }) {
        Log.e(TAG, "load: ALL endpoints failed: $warnings")
        return@supervisorScope AtharDataResult.Error(
          warnings.joinToString(separator = "\n").ifBlank { "Unable to load volunteer analytics." }
        )
      }

      val bundle = AtharVolunteerEndpointBundle(
        impact = (impact as? EndpointResult.Success)?.data as? AtharVolunteerImpactDto,
        history = (history as? EndpointResult.Success)?.data as? AtharVolunteerHistoryDto,
        earnings = (earnings as? EndpointResult.Success)?.data as? AtharVolunteerEarningsDto,
        performance = (performance as? EndpointResult.Success)?.data as? AtharVolunteerPerformanceDto,
        reviews = (reviews as? EndpointResult.Success)?.data as? AtharVolunteerReviewsDto
      )
      Log.d(TAG, "load: assembling bundle - performance weeklyActivity=${bundle.performance?.weeklyActivity?.size ?: "null"}")

      val model = assembler.assemble(bundle)
      Log.d(TAG, "load: assembled model - weeklyActivity=${model.weeklyActivity.size}, isMeaningfullyEmpty=${model.isMeaningfullyEmpty()}")

      AtharDataResult.Success(
        data = model,
        warnings = warnings
      )
    }
  }
}

interface AtharVolunteerRemoteSource {
  suspend fun getImpact(authorization: String): EndpointResult<AtharVolunteerImpactDto>
  suspend fun getHistory(authorization: String): EndpointResult<AtharVolunteerHistoryDto>
  suspend fun getEarnings(authorization: String): EndpointResult<AtharVolunteerEarningsDto>
  suspend fun getPerformance(authorization: String): EndpointResult<AtharVolunteerPerformanceDto>
  suspend fun getReviews(
    authorization: String,
    page: Int = 1,
    perPage: Int = 100,
    rating: Int? = null
  ): EndpointResult<AtharVolunteerReviewsDto>
}

class RetrofitAtharVolunteerRemoteSource(
  private val service: AtharVolunteerApiService = buildService()
) : AtharVolunteerRemoteSource {
  override suspend fun getImpact(authorization: String): EndpointResult<AtharVolunteerImpactDto> {
    return parseResponse("impact", service.getImpact(authorization), AtharVolunteerPayloadParser::parseImpact)
  }

  override suspend fun getHistory(authorization: String): EndpointResult<AtharVolunteerHistoryDto> {
    return parseResponse("history", service.getHistory(authorization), AtharVolunteerPayloadParser::parseHistory)
  }

  override suspend fun getEarnings(authorization: String): EndpointResult<AtharVolunteerEarningsDto> {
    return parseResponse("earnings", service.getEarnings(authorization), AtharVolunteerPayloadParser::parseEarnings)
  }

  override suspend fun getPerformance(authorization: String): EndpointResult<AtharVolunteerPerformanceDto> {
    return parseResponse("performance", service.getPerformance(authorization), AtharVolunteerPayloadParser::parsePerformance)
  }

  override suspend fun getReviews(
    authorization: String,
    page: Int,
    perPage: Int,
    rating: Int?
  ): EndpointResult<AtharVolunteerReviewsDto> {
    return parseResponse(
      "reviews",
      service.getReviews(
        authorization = authorization,
        page = page,
        perPage = perPage,
        rating = rating
      ),
      AtharVolunteerPayloadParser::parseReviews
    )
  }

  private fun <T> parseResponse(
    endpointName: String,
    response: Response<JsonElement>,
    parser: (JsonElement) -> T
  ): EndpointResult<T> {
    if (!response.isSuccessful) {
      val errorText = runCatching { response.errorBody()?.string()?.trim().orEmpty() }.getOrDefault("")
      val message = buildString {
        append("Request failed with HTTP ${response.code()}.")
        if (errorText.isNotBlank()) {
          append(' ')
          append(errorText)
        }
      }
      Log.e(TAG, "parseResponse[$endpointName]: HTTP ${response.code()} - $errorText")
      return EndpointResult.Error(message)
    }
    val body = response.body()
    if (body == null) {
      Log.e(TAG, "parseResponse[$endpointName]: empty body")
      return EndpointResult.Error("Server returned an empty response body.")
    }
    Log.d(TAG, "parseResponse[$endpointName]: HTTP 200 body=${body.toString().take(300)}")
    return runCatching { parser(body) }
      .fold(
        onSuccess = {
          Log.d(TAG, "parseResponse[$endpointName]: parsed OK")
          EndpointResult.Success(it)
        },
        onFailure = {
          Log.e(TAG, "parseResponse[$endpointName]: parse FAILED - ${it.message}", it)
          EndpointResult.Error(it.message ?: "Failed to parse server response.")
        }
      )
  }

  companion object {
    private const val TAG = "AtharRemoteSource"

    private fun buildService(): AtharVolunteerApiService {
      val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
      }
      return Retrofit.Builder()
        .baseUrl(normalizeBaseUrl(BuildConfig.BACKEND_BASE_URL))
        .client(
          OkHttpClient.Builder()
            .addInterceptor { chain ->
              val request: Request = chain.request()
                .newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
              chain.proceed(request)
            }
            .build()
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AtharVolunteerApiService::class.java)
    }

    private fun normalizeBaseUrl(rawBaseUrl: String): String {
      val trimmed = rawBaseUrl.trim().trimEnd('/')
      val withoutApi = if (trimmed.lowercase(Locale.US).endsWith("/api")) trimmed.dropLast(4) else trimmed
      return "$withoutApi/"
    }
  }
}

data class VolunteerAnalyticsRequest(
  val page: Int = 1,
  val perPage: Int = 100,
  val rating: Int? = null
) {
  fun cacheKey(): String = "page=$page|perPage=$perPage|rating=${rating ?: "all"}"
}
