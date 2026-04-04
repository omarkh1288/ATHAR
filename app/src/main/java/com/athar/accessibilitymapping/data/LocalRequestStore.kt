package com.athar.accessibilitymapping.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private const val LOCAL_REQUESTS_STORE_NAME = "local_requests_store"
private val Context.localRequestsDataStore by preferencesDataStore(name = LOCAL_REQUESTS_STORE_NAME)

@Serializable
private data class LocalAssistanceRequestRecord(
  val id: String,
  val userId: String,
  val userName: String,
  val userType: String,
  val location: String,
  val destination: String,
  val distance: String,
  val urgency: String,
  val helpType: String,
  val description: String,
  val hours: Int = 1,
  val pricePerHour: Int = 50,
  val totalAmountEgp: Int? = null,
  val createdAtEpochSeconds: Long,
  val status: String = "pending",
  val volunteerId: String? = null,
  val volunteerName: String? = null,
  val declinedVolunteerIds: List<String> = emptyList()
)

class LocalRequestStore(private val context: Context) {
  private val requestsKey = stringPreferencesKey("requests_json")
  private val volunteerLiveKey = stringPreferencesKey("volunteer_live_json")
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  suspend fun setVolunteerLive(volunteerId: String, isLive: Boolean) {
    val liveStates = readVolunteerLiveStates().toMutableMap()
    liveStates[volunteerId] = isLive
    context.localRequestsDataStore.edit { prefs ->
      prefs[volunteerLiveKey] = json.encodeToString(
        MapSerializer(String.serializer(), Boolean.serializer()),
        liveStates
      )
    }
  }

  suspend fun isVolunteerLive(volunteerId: String): Boolean {
    return readVolunteerLiveStates()[volunteerId] == true
  }

  suspend fun createRequest(
    userSession: AuthSession,
    request: ApiCreateRequest
  ): ApiVolunteerRequest {
    val now = System.currentTimeMillis() / 1000
    val normalizedHours = request.hours.coerceAtLeast(1)
    val normalizedPricePerHour = request.pricePerHour.coerceAtLeast(1)
    val normalizedTotalAmount = request.serviceFee
      .takeIf { it > 0.0 }
      ?.roundToInt()
      ?: (normalizedHours * normalizedPricePerHour)
    val record = LocalAssistanceRequestRecord(
      id = "local-req-${UUID.randomUUID()}",
      userId = userSession.userId,
      userName = userSession.fullName,
      userType = request.userType.ifBlank { userSession.disabilityType ?: "User" },
      location = request.location,
      destination = request.destination,
      distance = request.distance.ifBlank { "Unknown" },
      urgency = request.urgency.ifBlank { "medium" },
      helpType = request.helpType,
      description = request.description.ifBlank { request.helpType },
      hours = normalizedHours,
      pricePerHour = normalizedPricePerHour,
      totalAmountEgp = normalizedTotalAmount,
      createdAtEpochSeconds = now
    )
    val requests = readRequests().toMutableList()
    requests += record
    writeRequests(requests)
    return record.toVolunteerRequestDto()
  }

  suspend fun getUserRequests(userId: String): List<ApiVolunteerRequest> {
    return readRequests()
      .filter { it.userId == userId }
      .sortedByDescending { it.createdAtEpochSeconds }
      .map { it.toVolunteerRequestDto() }
  }

  suspend fun getIncomingRequests(volunteerId: String): List<ApiAssistanceRequest> {
    if (!isVolunteerLive(volunteerId)) {
      return emptyList()
    }
    return readRequests()
      .filter { it.status == "pending" && it.volunteerId == null && !it.declinedVolunteerIds.contains(volunteerId) }
      .sortedByDescending { it.createdAtEpochSeconds }
      .map { it.toAssistanceRequestDto() }
  }

  suspend fun acceptRequest(
    volunteerSession: AuthSession,
    requestId: String
  ): ApiCallResult<ApiActionResult> {
    val requests = readRequests().toMutableList()
    val index = requests.indexOfFirst { it.id == requestId }
    if (index < 0) {
      return ApiCallResult.Failure("Request not found.", 404)
    }
    val request = requests[index]
    if (request.status != "pending" || request.volunteerId != null) {
      return ApiCallResult.Failure("Request is no longer available.", 409)
    }
    requests[index] = request.copy(
      status = "accepted",
      volunteerId = volunteerSession.userId,
      volunteerName = volunteerSession.fullName
    )
    writeRequests(requests)
    return ApiCallResult.Success(ApiActionResult(success = true, message = "Request accepted successfully."))
  }

  suspend fun declineRequest(
    volunteerSession: AuthSession,
    requestId: String
  ): ApiCallResult<ApiActionResult> {
    val requests = readRequests().toMutableList()
    val index = requests.indexOfFirst { it.id == requestId }
    if (index < 0) {
      return ApiCallResult.Failure("Request not found.", 404)
    }
    val request = requests[index]
    if (request.status != "pending" || request.volunteerId != null) {
      return ApiCallResult.Failure("Request is no longer pending.", 409)
    }
    val declined = (request.declinedVolunteerIds + volunteerSession.userId).distinct()
    requests[index] = request.copy(declinedVolunteerIds = declined)
    writeRequests(requests)
    return ApiCallResult.Success(ApiActionResult(success = true, message = "Request declined."))
  }

  suspend fun cancelRequest(
    userSession: AuthSession,
    requestId: String
  ): ApiCallResult<ApiActionResult> {
    val requests = readRequests().toMutableList()
    val index = requests.indexOfFirst { it.id == requestId }
    if (index < 0) {
      return ApiCallResult.Failure("Request not found.", 404)
    }
    val request = requests[index]
    if (request.userId != userSession.userId) {
      return ApiCallResult.Failure("You can only cancel your own requests.", 403)
    }
    if (request.status !in setOf("pending", "accepted")) {
      return ApiCallResult.Failure("Only pending or accepted requests can be cancelled.", 409)
    }
    requests[index] = request.copy(status = "cancelled")
    writeRequests(requests)
    return ApiCallResult.Success(ApiActionResult(success = true, message = "Request cancelled."))
  }

  suspend fun completeRequest(
    actorSession: AuthSession,
    requestId: String
  ): ApiCallResult<ApiActionResult> {
    val requests = readRequests().toMutableList()
    val index = requests.indexOfFirst { it.id == requestId }
    if (index < 0) {
      return ApiCallResult.Failure("Request not found.", 404)
    }
    val request = requests[index]
    val isOwner = request.userId == actorSession.userId
    val isAssignedVolunteer = request.volunteerId == actorSession.userId
    if (!isOwner && !isAssignedVolunteer) {
      return ApiCallResult.Failure("You can only complete your own or assigned requests.", 403)
    }
    if (request.status !in setOf("accepted", "in_progress", "pending")) {
      return ApiCallResult.Failure("Request cannot be completed in its current state.", 409)
    }
    requests[index] = request.copy(status = "completed")
    writeRequests(requests)
    return ApiCallResult.Success(ApiActionResult(success = true, message = "Request marked as completed."))
  }

  private suspend fun readRequests(): List<LocalAssistanceRequestRecord> {
    val raw = context.localRequestsDataStore.data.first()[requestsKey].orEmpty()
    if (raw.isBlank()) return emptyList()
    return runCatching {
      json.decodeFromString(ListSerializer(LocalAssistanceRequestRecord.serializer()), raw)
    }.getOrElse { emptyList() }
  }

  private suspend fun writeRequests(requests: List<LocalAssistanceRequestRecord>) {
    context.localRequestsDataStore.edit { prefs ->
      prefs[requestsKey] = json.encodeToString(
        ListSerializer(LocalAssistanceRequestRecord.serializer()),
        requests
      )
    }
  }

  private suspend fun readVolunteerLiveStates(): Map<String, Boolean> {
    val raw = context.localRequestsDataStore.data.first()[volunteerLiveKey].orEmpty()
    if (raw.isBlank()) return emptyMap()
    return runCatching {
      json.decodeFromString(MapSerializer(String.serializer(), Boolean.serializer()), raw)
    }.getOrElse { emptyMap() }
  }

  private fun LocalAssistanceRequestRecord.toVolunteerRequestDto(): ApiVolunteerRequest {
    return ApiVolunteerRequest(
      id = id,
      userId = userId,
      userName = userName,
      userType = userType,
      location = location,
      requestTime = createdAtEpochSeconds.toRelativeTimeLabel(),
      status = status,
      volunteerName = volunteerName,
      description = description,
      hours = hours,
      pricePerHour = pricePerHour,
      totalAmountEgp = totalAmountEgp,
      paymentMethod = "cash",
      paymentStatus = null,
      isPaid = false
    )
  }

  private fun LocalAssistanceRequestRecord.toAssistanceRequestDto(): ApiAssistanceRequest {
    return ApiAssistanceRequest(
      id = id,
      userName = userName,
      userType = userType,
      location = location,
      destination = destination,
      distance = distance,
      urgency = urgency,
      helpType = helpType,
      requestTime = createdAtEpochSeconds.toRelativeTimeLabel(),
      status = status,
      hours = hours,
      pricePerHour = pricePerHour,
      totalAmountEgp = totalAmountEgp
    )
  }

  private fun Long.toRelativeTimeLabel(): String {
    val now = System.currentTimeMillis() / 1000
    val seconds = (now - this).coerceAtLeast(0)
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
      minutes < 1 -> "Just now"
      minutes < 60 -> "${minutes}m ago"
      hours < 24 -> "${hours}h ago"
      else -> "${hours / 24}d ago"
    }
  }
}
