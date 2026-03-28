package com.athar.accessibilitymapping.data.volunteer

import android.content.Context
import com.athar.accessibilitymapping.BuildConfig
import com.athar.accessibilitymapping.data.AuthSessionStore
import java.util.Locale
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
  suspend fun loadDashboard(): AtharDataResult<VolunteerDashboardUiModel> = withContext(ioDispatcher) {
    val accessToken = sessionStore.getAccessToken()
      ?: return@withContext AtharDataResult.Error("You are not logged in.")
    if (accessToken.startsWith("local-")) {
      return@withContext AtharDataResult.Error("Volunteer analytics require a live authenticated backend session.")
    }
    AtharVolunteerDashboardUseCase(
      remoteSource = remoteSource,
      assembler = assembler,
      ioDispatcher = ioDispatcher
    ).load(accessToken)
  }
}

class AtharVolunteerDashboardUseCase(
  private val remoteSource: AtharVolunteerRemoteSource,
  private val assembler: AtharVolunteerDashboardAssembler,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  suspend fun load(accessToken: String): AtharDataResult<VolunteerDashboardUiModel> = withContext(ioDispatcher) {
    val authorization = "Bearer $accessToken"
    supervisorScope {
      val impactDeferred = async { remoteSource.getImpact(authorization) }
      val historyDeferred = async { remoteSource.getHistory(authorization) }
      val earningsDeferred = async { remoteSource.getEarnings(authorization) }
      val performanceDeferred = async { remoteSource.getPerformance(authorization) }
      val reviewsDeferred = async { remoteSource.getReviews(authorization) }

      val impact = impactDeferred.await()
      val history = historyDeferred.await()
      val earnings = earningsDeferred.await()
      val performance = performanceDeferred.await()
      val reviews = reviewsDeferred.await()
      val allResults = listOf(impact, history, earnings, performance, reviews)

      val warnings = allResults
        .mapNotNull { (it as? EndpointResult.Error)?.message }
        .distinct()

      if (allResults.all { it is EndpointResult.Error }) {
        return@supervisorScope AtharDataResult.Error(
          warnings.joinToString(separator = "\n").ifBlank { "Unable to load volunteer analytics." }
        )
      }

      AtharDataResult.Success(
        data = assembler.assemble(
          AtharVolunteerEndpointBundle(
            impact = (impact as? EndpointResult.Success)?.data as? AtharVolunteerImpactDto,
            history = (history as? EndpointResult.Success)?.data as? AtharVolunteerHistoryDto,
            earnings = (earnings as? EndpointResult.Success)?.data as? AtharVolunteerEarningsDto,
            performance = (performance as? EndpointResult.Success)?.data as? AtharVolunteerPerformanceDto,
            reviews = (reviews as? EndpointResult.Success)?.data as? AtharVolunteerReviewsDto
          )
        ),
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
  suspend fun getReviews(authorization: String): EndpointResult<AtharVolunteerReviewsDto>
}

class RetrofitAtharVolunteerRemoteSource(
  private val service: AtharVolunteerApiService = buildService()
) : AtharVolunteerRemoteSource {
  override suspend fun getImpact(authorization: String): EndpointResult<AtharVolunteerImpactDto> {
    return parseResponse(service.getImpact(authorization), AtharVolunteerPayloadParser::parseImpact)
  }

  override suspend fun getHistory(authorization: String): EndpointResult<AtharVolunteerHistoryDto> {
    return parseResponse(service.getHistory(authorization), AtharVolunteerPayloadParser::parseHistory)
  }

  override suspend fun getEarnings(authorization: String): EndpointResult<AtharVolunteerEarningsDto> {
    return parseResponse(service.getEarnings(authorization), AtharVolunteerPayloadParser::parseEarnings)
  }

  override suspend fun getPerformance(authorization: String): EndpointResult<AtharVolunteerPerformanceDto> {
    return parseResponse(service.getPerformance(authorization), AtharVolunteerPayloadParser::parsePerformance)
  }

  override suspend fun getReviews(authorization: String): EndpointResult<AtharVolunteerReviewsDto> {
    return parseResponse(service.getReviews(authorization), AtharVolunteerPayloadParser::parseReviews)
  }

  private fun <T> parseResponse(
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
      return EndpointResult.Error(message)
    }
    val body = response.body() ?: return EndpointResult.Error("Server returned an empty response body.")
    return runCatching { parser(body) }
      .fold(
        onSuccess = { EndpointResult.Success(it) },
        onFailure = { EndpointResult.Error(it.message ?: "Failed to parse server response.") }
      )
  }

  companion object {
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
