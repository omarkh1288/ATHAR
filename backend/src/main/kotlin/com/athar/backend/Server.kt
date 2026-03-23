package com.athar.backend

import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class ApiPrincipal(
  val userId: String,
  val sessionId: String,
  val role: UserRole
) : Principal

fun main() {
  val port = System.getenv("ATHAR_BACKEND_PORT")?.toIntOrNull() ?: 8080
  embeddedServer(Netty, host = "0.0.0.0", port = port, module = Application::module)
    .start(wait = true)
}

fun Application.module() {
  val store = InMemoryStore()
  val authService = AuthService(store)
  val signInterpreter = EgyptianSignInterpreter()

  install(CallLogging)
  install(ContentNegotiation) {
    json(
      Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
      }
    )
  }
  install(Authentication) {
    jwt("auth-jwt") {
      realm = authService.realm()
      verifier(authService.verifier)
      validate { credential ->
        val userId = credential.payload.getClaim("uid").asString()
        val sessionId = credential.payload.getClaim("sid").asString()
        val roleName = credential.payload.getClaim("role").asString()
        if (userId.isNullOrBlank() || sessionId.isNullOrBlank() || roleName.isNullOrBlank()) {
          return@validate null
        }
        val role = runCatching { UserRole.valueOf(roleName) }.getOrNull() ?: return@validate null
        if (!authService.validateAccessPrincipal(userId, sessionId)) return@validate null
        ApiPrincipal(userId = userId, sessionId = sessionId, role = role)
      }
    }
  }

  routing {
    route("/api") {
      get("/health") {
        call.respond(ActionResultDto(success = true, message = "Athar backend is running"))
      }

      post("/sign-language/interpret") {
        val request = call.receiveOrNull<InterpretEgyptianSignRequest>()
          ?: return@post call.respondBadRequest("Invalid sign interpretation payload.")
        call.respondResult(signInterpreter.interpret(request))
      }

      route("/auth") {
        post("/register/user") {
          val request = call.receiveOrNull<RegisterUserRequest>() ?: return@post call.respondBadRequest("Invalid user registration payload.")
          call.respondResult(authService.registerUser(request))
        }
        post("/register-user") {
          val request = call.receiveOrNull<RegisterUserRequest>() ?: return@post call.respondBadRequest("Invalid user registration payload.")
          call.respondResult(authService.registerUser(request))
        }
        post("/register/volunteer") {
          val request = call.receiveOrNull<RegisterVolunteerRequest>() ?: return@post call.respondBadRequest("Invalid volunteer registration payload.")
          call.respondResult(authService.registerVolunteer(request))
        }
        post("/register-volunteer") {
          val request = call.receiveOrNull<RegisterVolunteerRequest>() ?: return@post call.respondBadRequest("Invalid volunteer registration payload.")
          call.respondResult(authService.registerVolunteer(request))
        }
        post("/login") {
          val request = call.receiveOrNull<LoginRequest>() ?: return@post call.respondBadRequest("Invalid login payload.")
          call.respondResult(authService.login(request))
        }
        post("/refresh") {
          val request = call.receiveOrNull<RefreshTokenRequest>() ?: return@post call.respondBadRequest("Invalid refresh payload.")
          call.respondResult(authService.refresh(request.refreshToken))
        }
        // Legacy/Mobile specific aliases
        post("/register-api") {
          call.handleApiRegister(authService)
        }
      }

      get("/locations") {
        call.respond(store.getLocations())
      }
      get("/locations/search") {
        val query = call.request.queryParameters["q"].orEmpty()
        call.respond(
          LocationSearchResponse(
            query = query,
            results = store.searchLocations(query)
          )
        )
      }

      post("/payments/paymob/callback") {
        val payload = call.receiveOrNull<JsonObject>() ?: return@post call.respondBadRequest("Invalid callback payload.")
        val paymentId = payload["id"]?.jsonPrimitive?.contentOrNull ?: return@post call.respondBadRequest("Payment id missing.")
        val success = payload["success"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
        call.respondResult(store.handlePaymobCallback(paymentId, success))
      }

      authenticate("auth-jwt") {
        post("/auth/logout") {
          val principal = call.currentPrincipal() ?: return@post call.respondUnauthorized()
          val request = call.receiveOrNull<LogoutRequest>() ?: LogoutRequest()
          call.respondResult(authService.logout(principal.userId, principal.sessionId, request))
        }

        get("/me") {
          val principal = call.currentPrincipal() ?: return@get call.respondUnauthorized()
          call.respondResult(store.getProfile(principal.userId))
        }

        patch("/me/profile") {
          val principal = call.currentPrincipal() ?: return@patch call.respondUnauthorized()
          val request = call.receiveOrNull<UpdateProfileRequest>() ?: return@patch call.respondBadRequest("Invalid profile payload.")
          call.respondResult(store.updateProfile(principal.userId, request))
        }

        patch("/me/password") {
          val principal = call.currentPrincipal() ?: return@patch call.respondUnauthorized()
          val request = call.receiveOrNull<ChangePasswordRequest>() ?: return@patch call.respondBadRequest("Invalid password payload.")
          call.respondResult(store.changePassword(principal.userId, request.currentPassword, request.newPassword))
        }

        get("/me/sessions") {
          val principal = call.currentPrincipal() ?: return@get call.respondUnauthorized()
          call.respond(store.listSessions(principal.userId, principal.sessionId))
        }

        delete("/me/sessions/{sessionId}") {
          val principal = call.currentPrincipal() ?: return@delete call.respondUnauthorized()
          val sessionId = call.parameters["sessionId"] ?: return@delete call.respondBadRequest("sessionId is required.")
          call.respondResult(store.deleteSession(principal.userId, sessionId, principal.sessionId))
        }

        patch("/me/notification-settings") {
          val principal = call.currentPrincipal() ?: return@patch call.respondUnauthorized()
          val request = call.receiveOrNull<UpdateNotificationSettingsRequest>() ?: return@patch call.respondBadRequest("Invalid notification settings payload.")
          call.respondResult(store.updateNotificationSettings(principal.userId, request))
        }

        patch("/me/privacy-settings") {
          val principal = call.currentPrincipal() ?: return@patch call.respondUnauthorized()
          val request = call.receiveOrNull<UpdatePrivacySettingsRequest>() ?: return@patch call.respondBadRequest("Invalid privacy settings payload.")
          call.respondResult(store.updatePrivacySettings(principal.userId, request))
        }

        patch("/volunteer/me/live") {
          val principal = call.requireRole(UserRole.Volunteer) ?: return@patch
          val body = call.receiveOrNull<JsonObject>() ?: return@patch call.respondBadRequest("Invalid volunteer live payload.")
          val isLive = body.readBoolean("is_live", "isLive")
            ?: return@patch call.respondBadRequest("is_live is required.")
          call.respondResult(store.setVolunteerLive(principal.userId, ToggleVolunteerLiveRequest(isLive)))
        }

        route("/volunteer") {
          post("/status") {
            val principal = call.requireRole(UserRole.Volunteer) ?: return@post
            val body = call.receiveOrNull<JsonObject>() ?: return@post call.respondBadRequest("Invalid volunteer live payload.")
            val isLive = body.readBoolean("is_live", "isLive")
              ?: return@post call.respondBadRequest("is_live is required.")
            call.respondResult(store.setVolunteerLive(principal.userId, ToggleVolunteerLiveRequest(isLive)))
          }

          get("/incoming") {
            val principal = call.requireRole(UserRole.Volunteer) ?: return@get
            val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 15
            call.respondResult(store.getVolunteerIncomingDashboard(principal.userId, perPage))
          }

          get("/active") {
            val principal = call.requireRole(UserRole.Volunteer) ?: return@get
            val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 15
            call.respondResult(store.getVolunteerActiveDashboard(principal.userId, perPage))
          }

          get("/history") {
            val principal = call.requireRole(UserRole.Volunteer) ?: return@get
            val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 15
            call.respondResult(store.getVolunteerHistoryDashboard(principal.userId, perPage))
          }

          get("/impact") {
            val principal = call.requireRole(UserRole.Volunteer) ?: return@get
            call.respondResult(store.getVolunteerImpactDashboard(principal.userId))
          }

          route("/analytics") {
            get("/earnings") {
              val principal = call.requireRole(UserRole.Volunteer) ?: return@get
              call.respondResult(store.getVolunteerAnalyticsEarnings(principal.userId))
            }

            get("/performance") {
              val principal = call.requireRole(UserRole.Volunteer) ?: return@get
              call.respondResult(store.getVolunteerAnalyticsPerformance(principal.userId))
            }

            get("/reviews") {
              val principal = call.requireRole(UserRole.Volunteer) ?: return@get
              val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
              val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 10
              val ratingRaw = call.request.queryParameters["rating"]?.trim().orEmpty()
              val rating = when {
                ratingRaw.isBlank() -> null
                else -> ratingRaw.toIntOrNull()?.takeIf { it in 1..5 }
                  ?: return@get call.respondBadRequest("rating must be between 1 and 5.")
              }
              call.respondResult(store.getVolunteerAnalyticsReviews(principal.userId, page, perPage, rating))
            }
          }
        }

        route("/help-requests") {
          post {
            val principal = call.requireRole(UserRole.User) ?: return@post
            val payload = call.receiveOrNull<JsonObject>() ?: return@post call.respondBadRequest("Invalid help request payload.")
            val request = payload.toCreateAssistanceRequest()
              ?: return@post call.respondBadRequest("Invalid help request payload.")
            call.respondResult(store.createAssistanceRequest(principal.userId, request))
          }

          get("/mine") {
            val principal = call.requireRole(UserRole.User) ?: return@get
            val status = call.request.queryParameters["status"] ?: "all"
            val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 15
            call.respondResult(store.getMyRequests(principal.userId, status, perPage))
          }

          route("/{id}") {
            post("/cancel") {
              val principal = call.requireRole(UserRole.User) ?: return@post
              val requestId = call.parameters["id"] ?: return@post call.respondBadRequest("Request id is required.")
              call.respondResult(store.cancelRequest(principal.userId, requestId))
            }

            post("/pay") {
              val principal = call.requireRole(UserRole.User) ?: return@post
              val requestId = call.parameters["id"] ?: return@post call.respondBadRequest("Request id is required.")
              val body = call.receiveOrNull<JsonObject>()
              val paymentMethod = body.readString("payment_method", "paymentMethod", "method")
                ?.let { parsePaymentMethod(it) }
              call.respondResult(store.payRequest(principal.userId, requestId, paymentMethod))
            }

            post("/accept") {
              val principal = call.requireRole(UserRole.Volunteer) ?: return@post
              val requestId = call.parameters["id"] ?: return@post call.respondBadRequest("Request id is required.")
              call.respondResult(store.acceptRequest(principal.userId, requestId))
            }

            post("/decline") {
              val principal = call.requireRole(UserRole.Volunteer) ?: return@post
              val requestId = call.parameters["id"] ?: return@post call.respondBadRequest("Request id is required.")
              call.respondResult(store.declineRequest(principal.userId, requestId))
            }

            post("/complete") {
              val principal = call.requireRole(UserRole.Volunteer) ?: return@post
              val requestId = call.parameters["id"] ?: return@post call.respondBadRequest("Request id is required.")
              call.respondResult(store.completeRequest(principal.userId, requestId))
            }

            post("/rate") {
              val principal = call.requireRole(UserRole.User) ?: return@post
              val requestId = call.parameters["id"] ?: return@post call.respondBadRequest("Request id is required.")
              val body = call.receiveOrNull<JsonObject>() ?: return@post call.respondBadRequest("Invalid rating payload.")
              val request = body.toSubmitVolunteerRatingRequest()
                ?: return@post call.respondBadRequest("Invalid rating payload.")
              call.respondResult(store.submitVolunteerRating(principal.userId, requestId, request))
            }

            get("/messages") {
              val principal = call.currentPrincipal() ?: return@get call.respondUnauthorized()
              val requestId = call.parameters["id"] ?: return@get call.respondBadRequest("Request id is required.")
              val perPage = call.request.queryParameters["per_page"]?.toIntOrNull() ?: 20
              call.respondResult(store.getRequestMessages(principal.userId, requestId, perPage))
            }

            post("/messages") {
              val principal = call.currentPrincipal() ?: return@post call.respondUnauthorized()
              val requestId = call.parameters["id"] ?: return@post call.respondBadRequest("Request id is required.")
              val request = call.receiveOrNull<CreateMessageRequest>() ?: return@post call.respondBadRequest("Invalid message payload.")
              call.respondResult(store.sendMessage(principal.userId, requestId, request.message))
            }
          }
        }

        route("/payments") {
          post("/card/checkout") {
            val principal = call.currentPrincipal() ?: return@post call.respondUnauthorized()
            val body = call.receiveOrNull<JsonObject>()
            val requestId = body.readString("request_id", "help_request_id", "requestId")
            val amountEgp = body?.let { it.readDouble("amount_egp", "amount", "amountEgp") }
            call.respondResult(store.checkoutCard(principal.userId, requestId, amountEgp))
          }

          post("/wallet/checkout") {
            val principal = call.currentPrincipal() ?: return@post call.respondUnauthorized()
            val body = call.receiveOrNull<JsonObject>()
            val requestId = body.readString("request_id", "help_request_id", "requestId")
            val amountEgp = body?.let { it.readDouble("amount_egp", "amount", "amountEgp") }
            call.respondResult(store.checkoutWallet(principal.userId, requestId, amountEgp))
          }

          get("/{id}") {
            val paymentId = call.parameters["id"] ?: return@get call.respondBadRequest("Payment id is required.")
            call.respondResult(store.getPaymentStatus(paymentId))
          }

          post("/{id}/refresh") {
            val paymentId = call.parameters["id"] ?: return@post call.respondBadRequest("Payment id is required.")
            call.respondResult(store.refreshPayment(paymentId))
          }
        }

        post("/locations/{id}/ratings") {
          val principal = call.currentPrincipal() ?: return@post call.respondUnauthorized()
          val locationId = call.parameters["id"] ?: return@post call.respondBadRequest("Location id is required.")
          val request = call.receiveOrNull<SubmitRatingRequest>() ?: return@post call.respondBadRequest("Invalid rating payload.")
          call.respondResult(store.submitLocationRating(principal.userId, locationId, request))
        }

        post("/support/messages") {
          val principal = call.currentPrincipal() ?: return@post call.respondUnauthorized()
          val request = call.receiveOrNull<SupportMessageRequest>() ?: return@post call.respondBadRequest("Invalid support message payload.")
          call.respondResult(store.saveSupportMessage(principal.userId, request))
        }
      }
    }
  }
}

private val indexedFieldPattern = Regex("""\[(\d+)]$""")

private data class UploadedDocument(
  val fileName: String?,
  val contentType: String?,
  val sizeBytes: Long,
  val bytes: ByteArray
)

private data class RegistrationPayload(
  val fields: Map<String, List<String>>,
  val idDocument: UploadedDocument? = null
)

private suspend fun io.ktor.server.application.ApplicationCall.handleApiRegister(authService: AuthService) {
  val payload = readRegistrationPayload()
  val fields = payload.fields
  if (fields.isEmpty()) {
    respondApiFailure(HttpStatusCode.BadRequest, "Invalid registration payload.")
    return
  }

  val fullName = fields.firstNonBlank("full_name", "name", "fullName").orEmpty()
  val email = fields.firstNonBlank("email").orEmpty()
  val password = fields.firstNonBlank("password").orEmpty()
  val confirmation = fields.firstNonBlank("password_confirmation")
  val phone = fields.firstNonBlank("phone").orEmpty()
  val role = fields.firstNonBlank("role")
    ?.lowercase()
    ?.trim()
    ?.takeIf { it.isNotBlank() }
    ?: if (fields.looksLikeVolunteerPayload()) "volunteer" else "user"

  if (!confirmation.isNullOrBlank() && confirmation != password) {
    respondApiFailure(HttpStatusCode.BadRequest, "Password confirmation does not match.")
    return
  }

  val result = if (role == "volunteer") {
    authService.registerVolunteer(
      RegisterVolunteerRequest(
        fullName = fullName,
        email = email,
        phone = phone,
        location = fields.firstNonBlank("location", "city").orEmpty(),
        password = password,
        idNumber = fields.firstNonBlank("national_id", "id_number", "idNumber").orEmpty(),
        dateOfBirth = fields.firstNonBlank("date_of_birth", "dateOfBirth").orEmpty(),
        motivation = fields.firstNonBlank("volunteer_motivation", "motivation").orEmpty(),
        languages = fields.collectList("volunteer_languages", "languages"),
        availability = fields.collectList("volunteer_availability", "availability"),
        idDocumentFileName = payload.idDocument?.fileName,
        idDocumentContentType = payload.idDocument?.contentType,
        idDocumentSizeBytes = payload.idDocument?.sizeBytes,
        idDocumentBytes = payload.idDocument?.bytes
      )
    )
  } else {
    authService.registerUser(
      RegisterUserRequest(
        fullName = fullName,
        email = email,
        phone = phone,
        location = fields.firstNonBlank("location", "city").orEmpty(),
        password = password,
        disabilityType = fields.firstNonBlank("disability_type", "disabilityType").orEmpty(),
        emergencyContactName = fields.firstNonBlank("emergency_contact_name", "emergency_name").orEmpty(),
        emergencyContactPhone = fields.firstNonBlank("emergency_contact_phone", "emergency_phone").orEmpty()
      )
    )
  }

  respondLegacyAuthResult(result)
}

private suspend fun io.ktor.server.application.ApplicationCall.readRegistrationPayload(): RegistrationPayload {
  val fields = linkedMapOf<String, MutableList<String>>()
  var idDocument: UploadedDocument? = null
  val contentType = request.contentType()

  when {
    contentType.match(ContentType.MultiPart.FormData) -> {
      val multipart = receiveMultipart()
      while (true) {
        val part = multipart.readPart() ?: break
        when (part) {
          is PartData.FormItem -> {
            val key = part.name?.trim().orEmpty()
            if (key.isNotBlank()) {
              fields.addFieldValue(key, part.value)
            }
          }
          is PartData.FileItem -> {
            val key = part.name?.trim().orEmpty()
            if (key.equals("id_document", ignoreCase = true)) {
              val bytes = part.streamProvider().use { input -> input.readBytes() }
              if (bytes.isNotEmpty()) {
                idDocument = UploadedDocument(
                  fileName = part.originalFileName?.trim()?.ifBlank { null },
                  contentType = part.contentType?.toString(),
                  sizeBytes = bytes.size.toLong(),
                  bytes = bytes
                )
              }
            }
          }
          else -> Unit
        }
        part.dispose()
      }
    }

    contentType.match(ContentType.Application.FormUrlEncoded) -> {
      val parameters = receiveParameters()
      parameters.entries().forEach { (key, values) ->
        values.forEach { value ->
          fields.addFieldValue(key, value)
        }
      }
    }

    else -> {
      val body = receiveOrNull<JsonObject>() ?: return RegistrationPayload(emptyMap())
      body.forEach { (key, value) ->
        fields.addJsonField(key, value)
      }
    }
  }

  return RegistrationPayload(fields = fields, idDocument = idDocument)
}

private fun MutableMap<String, MutableList<String>>.addFieldValue(key: String, rawValue: String?) {
  val value = rawValue?.trim().orEmpty()
  if (key.isBlank() || value.isBlank()) return
  getOrPut(key) { mutableListOf() }.add(value)
}

private fun MutableMap<String, MutableList<String>>.addJsonField(key: String, value: JsonElement) {
  when (value) {
    is JsonPrimitive -> addFieldValue(key, value.contentOrNull ?: value.content)
    is JsonArray -> value.forEachIndexed { index, item ->
      val primitive = item as? JsonPrimitive ?: return@forEachIndexed
      val raw = primitive.contentOrNull ?: primitive.content
      addFieldValue(key, raw)
      addFieldValue("$key[$index]", raw)
    }
    else -> Unit
  }
}

private fun Map<String, List<String>>.firstNonBlank(vararg keys: String): String? {
  keys.forEach { key ->
    this[key]
      ?.firstOrNull { it.isNotBlank() }
      ?.let { return it.trim() }
  }
  return null
}

private fun Map<String, List<String>>.collectList(vararg keys: String): List<String> {
  val values = mutableListOf<String>()

  keys.forEach { key ->
    this[key]
      .orEmpty()
      .forEach { candidate ->
        values += candidate.split(",")
      }

    entries
      .asSequence()
      .filter { it.key.startsWith("$key[") }
      .sortedBy { parseIndexedFieldPosition(it.key) }
      .forEach { entry ->
        entry.value.forEach { candidate ->
          values += candidate.split(",")
        }
      }
  }

  return values
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
}

private fun Map<String, List<String>>.looksLikeVolunteerPayload(): Boolean {
  return containsKey("national_id") ||
    containsKey("date_of_birth") ||
    containsKey("volunteer_motivation") ||
    keys.any { it.startsWith("volunteer_languages[") } ||
    keys.any { it.startsWith("volunteer_availability[") }
}

private fun parseIndexedFieldPosition(key: String): Int {
  return indexedFieldPattern.find(key)
    ?.groupValues
    ?.getOrNull(1)
    ?.toIntOrNull()
    ?: Int.MAX_VALUE
}

private fun JsonObject?.readString(vararg keys: String): String? {
  val obj = this ?: return null
  keys.forEach { key ->
    val value = obj[key] as? JsonPrimitive ?: return@forEach
    val content = value.contentOrNull ?: return@forEach
    if (content.isNotBlank()) return content.trim()
  }
  return null
}

private fun JsonObject?.readInt(vararg keys: String): Int? {
  val obj = this ?: return null
  keys.forEach { key ->
    val value = obj[key] as? JsonPrimitive ?: return@forEach
    value.contentOrNull?.toIntOrNull()?.let { return it }
  }
  return null
}

private fun JsonObject?.readDouble(vararg keys: String): Double? {
  val obj = this ?: return null
  keys.forEach { key ->
    val value = obj[key] as? JsonPrimitive ?: return@forEach
    value.contentOrNull?.toDoubleOrNull()?.let { return it }
  }
  return null
}

private fun JsonObject?.readBoolean(vararg keys: String): Boolean? {
  val obj = this ?: return null
  keys.forEach { key ->
    val value = obj[key] as? JsonPrimitive ?: return@forEach
    value.contentOrNull?.let { raw ->
      when (raw.trim().lowercase()) {
        "true", "1" -> return true
        "false", "0" -> return false
      }
    }
  }
  return null
}

private fun JsonObject?.readStringList(vararg keys: String): List<String> {
  val obj = this ?: return emptyList()
  keys.forEach { key ->
    when (val value = obj[key]) {
      is JsonArray -> {
        return value.mapNotNull { element ->
          (element as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        }
      }
      is JsonPrimitive -> {
        val content = value.contentOrNull?.trim().orEmpty()
        if (content.isNotBlank()) {
          return content.split(",").mapNotNull { it.trim().takeIf { item -> item.isNotBlank() } }
        }
      }
      else -> Unit
    }
  }
  return emptyList()
}

private fun parsePaymentMethod(raw: String): PaymentMethod? {
  return when (raw.trim().lowercase()) {
    "card" -> PaymentMethod.CARD
    "cash" -> PaymentMethod.CASH
    "wallet" -> PaymentMethod.WALLET
    else -> null
  }
}

private fun JsonObject.toCreateAssistanceRequest(): CreateAssistanceRequest? {
  val location = readString("from_label", "fromLabel", "location") ?: return null
  val destination = readString("to_label", "toLabel", "destination") ?: return null
  val helpType = readString("assistance_type", "help_type", "helpType") ?: return null
  val paymentMethod = readString("payment_method", "paymentMethod")
    ?.let(::parsePaymentMethod)
    ?: return null
  val serviceFee = readDouble("service_fee", "serviceFee")
    ?: return null
  val hours = readInt("hours") ?: 1
  val pricePerHour = readInt("price_per_hour", "pricePerHour") ?: 50

  return CreateAssistanceRequest(
    userType = readString("user_type", "userType", "disability_type", "disabilityType") ?: "User",
    location = location,
    destination = destination,
    distance = readString("distance") ?: "Unknown",
    urgency = readString("urgency", "priority") ?: "medium",
    helpType = helpType,
    description = readString("details", "description") ?: helpType,
    payment_method = paymentMethod,
    service_fee = serviceFee,
    hours = hours,
    price_per_hour = pricePerHour
  )
}

private fun JsonObject.toSubmitVolunteerRatingRequest(): SubmitVolunteerRatingRequest? {
  val rating = readInt("rating", "score") ?: return null
  val comment = readString("comment", "review", "message")
  return SubmitVolunteerRatingRequest(
    rating = rating,
    comment = comment,
    issues = readStringList("issues", "reported_issues", "reportedIssues")
  )
}

private suspend fun io.ktor.server.application.ApplicationCall.requireRole(
  role: UserRole
): ApiPrincipal? {
  val principal = currentPrincipal() ?: run {
    respondUnauthorized()
    return null
  }
  if (principal.role != role) {
    respond(HttpStatusCode.Forbidden, ApiError("Forbidden"))
    return null
  }
  return principal
}

private suspend fun io.ktor.server.application.ApplicationCall.respondLegacyAuthResult(
  result: ServiceResult<AuthResponseDto>
) {
  when (result) {
    is ServiceResult.Success -> {
      respond(
        HttpStatusCode.OK,
        buildJsonObject {
          put("success", true)
          put("data", result.value.toLegacyAuthPayload())
        }
      )
    }
    is ServiceResult.Failure -> respondApiFailure(result.status, result.message)
  }
}

private fun AuthResponseDto.toLegacyAuthPayload(): JsonObject {
  val roleName = if (user.role == UserRole.Volunteer) "volunteer" else "user"
  return buildJsonObject {
    put("token", tokens.accessToken)
    put(
      "user",
      buildJsonObject {
        put("id", user.id)
        put("role", roleName)
        put("name", user.fullName)
        put("full_name", user.fullName)
        put("email", user.email)
        put("phone", user.phone)
        put("location", user.location)
        user.disabilityType?.takeIf { it.isNotBlank() }?.let { put("disability_type", it) }
        put("created_at", Instant.now().toString())
        put("is_live", user.volunteerLive)
      }
    )
  }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondApiFailure(
  status: HttpStatusCode,
  message: String
) {
  respond(
    status,
    buildJsonObject {
      put("success", false)
      put("message", message)
    }
  )
}

private suspend inline fun <reified T : Any> io.ktor.server.application.ApplicationCall.receiveOrNull(): T? {
  return runCatching { this.receive<T>() }.getOrNull()
}

private suspend fun io.ktor.server.application.ApplicationCall.respondResult(result: ServiceResult<*>) {
  when (result) {
    is ServiceResult.Success<*> -> respond(result.value ?: ActionResultDto(success = true, message = "OK"))
    is ServiceResult.Failure -> respond(result.status, ApiError(result.message))
  }
}

private fun io.ktor.server.application.ApplicationCall.currentPrincipal(): ApiPrincipal? {
  return principal()
}

private suspend fun io.ktor.server.application.ApplicationCall.respondBadRequest(message: String) {
  respond(io.ktor.http.HttpStatusCode.BadRequest, ApiError(message))
}

private suspend fun io.ktor.server.application.ApplicationCall.respondUnauthorized() {
  respond(io.ktor.http.HttpStatusCode.Unauthorized, ApiError("Unauthorized"))
}
