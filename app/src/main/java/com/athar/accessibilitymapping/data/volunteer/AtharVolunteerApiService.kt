package com.athar.accessibilitymapping.data.volunteer

import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AtharVolunteerApiService {
  @GET("api/volunteer/impact")
  suspend fun getImpact(
    @Header("Authorization") authorization: String
  ): Response<JsonElement>

  @GET("api/volunteer/history")
  suspend fun getHistory(
    @Header("Authorization") authorization: String,
    @Query("page") page: Int = 1,
    @Query("per_page") perPage: Int = 100
  ): Response<JsonElement>

  @GET("api/volunteer/analytics/earnings")
  suspend fun getEarnings(
    @Header("Authorization") authorization: String
  ): Response<JsonElement>

  @GET("api/volunteer/analytics/performance")
  suspend fun getPerformance(
    @Header("Authorization") authorization: String
  ): Response<JsonElement>

  @GET("api/volunteer/analytics/reviews")
  suspend fun getReviews(
    @Header("Authorization") authorization: String,
    @Query("page") page: Int = 1,
    @Query("per_page") perPage: Int = 100
  ): Response<JsonElement>
}
