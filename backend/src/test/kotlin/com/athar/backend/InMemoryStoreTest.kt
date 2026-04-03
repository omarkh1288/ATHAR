package com.athar.backend

import io.ktor.http.HttpStatusCode
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class InMemoryStoreTest {
  private data class TestStoreContext(
    val store: InMemoryStore,
    val jdbcUrl: String
  )

  private fun newStoreContext(): TestStoreContext {
    val dbFile = Files.createTempFile("athar-backend-test-", ".db")
    dbFile.toFile().deleteOnExit()
    val jdbcUrl = "jdbc:sqlite:${dbFile.toAbsolutePath()}"
    return TestStoreContext(
      jdbcUrl = jdbcUrl,
      store = InMemoryStore(
      accountDatabase = AccountDatabase("jdbc:sqlite:${dbFile.toAbsolutePath()}")
      )
    )
  }

  private fun rowCount(jdbcUrl: String, table: String): Int {
    return DriverManager.getConnection(jdbcUrl).use { connection ->
      connection.prepareStatement("SELECT COUNT(*) FROM $table").use { statement ->
        statement.executeQuery().use { result ->
          if (result.next()) result.getInt(1) else 0
        }
      }
    }
  }

  private fun volunteerDocumentSnapshot(jdbcUrl: String, accountId: String): Triple<String?, String?, Int?> {
    return DriverManager.getConnection(jdbcUrl).use { connection ->
      connection.prepareStatement(
        """
        SELECT id_document_name, id_document_content_type, LENGTH(id_document_blob) AS blob_len
        FROM volunteer_profiles
        WHERE account_id = ?
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, accountId)
        statement.executeQuery().use { result ->
          if (!result.next()) return Triple(null, null, null)
          Triple(
            result.getString("id_document_name"),
            result.getString("id_document_content_type"),
            result.getInt("blob_len").takeIf { !result.wasNull() }
          )
        }
      }
    }
  }

  @Test
  fun registerBlocksCrossRoleEmailReuse() {
    val store = newStoreContext().store

    val conflict = store.registerUser(
      request = RegisterUserRequest(
        fullName = "Conflict User",
        email = "volunteer@athar.app",
        phone = "+10000000000",
        location = "Riyadh",
        password = "Password123!",
        disabilityType = "Mobility challenges",
        emergencyContactName = "EC",
        emergencyContactPhone = "+10000000001"
      ),
      passwordHash = "unused"
    )

    val failure = assertIs<ServiceResult.Failure>(conflict)
    assertEquals(HttpStatusCode.Conflict, failure.status)
    assertTrue(failure.message.contains("already registered"))
  }

  @Test
  fun loginReturnsCorrectRoleForSeedAccounts() {
    val store = newStoreContext().store
    val auth = AuthService(store)

    val userLogin = auth.login(LoginRequest(email = "user@athar.app", password = "Password123!"))
    val volunteerLogin = auth.login(LoginRequest(email = "volunteer@athar.app", password = "Password123!"))

    val user = assertIs<ServiceResult.Success<AuthResponseDto>>(userLogin).value.user
    val volunteer = assertIs<ServiceResult.Success<AuthResponseDto>>(volunteerLogin).value.user

    assertEquals(UserRole.User, user.role)
    assertEquals(UserRole.Volunteer, volunteer.role)
  }

  @Test
  fun loginUsesPersistedDatabaseAccountsAfterStoreRestart() {
    val context = newStoreContext()
    val initialStore = context.store

    val registeredVolunteer = initialStore.registerVolunteer(
      request = RegisterVolunteerRequest(
        fullName = "Persisted Volunteer",
        email = "persisted-volunteer@example.com",
        phone = "+10000000030",
        location = "Riyadh",
        password = "Password123!",
        idNumber = "4455667788",
        dateOfBirth = "1997-05-06",
        motivation = "Verify persisted auth",
        languages = listOf("Arabic", "English"),
        availability = listOf("Weekends")
      ),
      passwordHash = org.mindrot.jbcrypt.BCrypt.hashpw("Password123!", org.mindrot.jbcrypt.BCrypt.gensalt())
    )
    val created = assertIs<ServiceResult.Success<AccountRecord>>(registeredVolunteer).value

    val restartedStore = InMemoryStore(accountDatabase = AccountDatabase(context.jdbcUrl))
    val auth = AuthService(restartedStore)
    val login = auth.login(LoginRequest(email = "persisted-volunteer@example.com", password = "Password123!"))

    val user = assertIs<ServiceResult.Success<AuthResponseDto>>(login).value.user
    assertEquals(created.id, user.id)
    assertEquals(UserRole.Volunteer, user.role)
    assertEquals("persisted-volunteer@example.com", user.email)
  }

  @Test
  fun analyticsStayBoundToTheAuthenticatedVolunteerAfterRestart() {
    val context = newStoreContext()
    val initialStore = context.store

    val registeredVolunteer = initialStore.registerVolunteer(
      request = RegisterVolunteerRequest(
        fullName = "Second Volunteer",
        email = "second-volunteer@example.com",
        phone = "+10000000031",
        location = "Riyadh",
        password = "Password123!",
        idNumber = "4455667799",
        dateOfBirth = "1996-05-06",
        motivation = "Verify analytics ownership",
        languages = listOf("Arabic", "English"),
        availability = listOf("Weekends")
      ),
      passwordHash = org.mindrot.jbcrypt.BCrypt.hashpw("Password123!", org.mindrot.jbcrypt.BCrypt.gensalt())
    )
    val volunteerId = assertIs<ServiceResult.Success<AccountRecord>>(registeredVolunteer).value.id

    val restartedStore = InMemoryStore(accountDatabase = AccountDatabase(context.jdbcUrl))

    val seedAnalytics = assertIs<ServiceResult.Success<VolunteerAnalyticsPerformanceResponseDto>>(
      restartedStore.getVolunteerAnalyticsPerformance("vol-seed-1")
    ).value
    val secondVolunteerAnalytics = assertIs<ServiceResult.Success<VolunteerAnalyticsPerformanceResponseDto>>(
      restartedStore.getVolunteerAnalyticsPerformance(volunteerId)
    ).value

    assertTrue(seedAnalytics.completed > 0)
    assertEquals(0, secondVolunteerAnalytics.completed)
    assertEquals(0, secondVolunteerAnalytics.weekly_activity.sumOf { it.completed })
  }

  @Test
  fun requestLifecycleCreateAcceptCompleteWorks() {
    val store = newStoreContext().store

    val create = store.createAssistanceRequest(
      userId = "user-seed-1",
      request = CreateAssistanceRequest(
        userType = "Wheelchair user",
        location = "QA Start",
        destination = "QA End",
        distance = "0.4 km",
        urgency = "medium",
        helpType = "QA Help",
        description = "QA request",
        payment_method = PaymentMethod.CASH,
        service_fee = 0.0
      )
    )
    val requestId = assertIs<ServiceResult.Success<VolunteerRequestDto>>(create).value.id

    val accept = store.acceptRequest(userId = "vol-seed-1", requestId = requestId)
    val acceptResult = assertIs<ServiceResult.Success<ActionResultDto>>(accept).value
    assertTrue(acceptResult.success)

    val complete = store.completeRequest(userId = "vol-seed-1", requestId = requestId)
    val completeResult = assertIs<ServiceResult.Success<ActionResultDto>>(complete).value
    assertTrue(completeResult.success)

    val mine = store.getMyRequests("user-seed-1")
    val mineResult = assertIs<ServiceResult.Success<MyRequestsResponse>>(mine).value
    val request = mineResult.userRequests.first { it.id == requestId }
    assertEquals("completed", request.status)
  }

  @Test
  fun registerPersistsUserAndVolunteerIntoSeparateTables() {
    val context = newStoreContext()
    val store = context.store
    val usersBefore = rowCount(context.jdbcUrl, "user_profiles")
    val volunteersBefore = rowCount(context.jdbcUrl, "volunteer_profiles")

    val userRegistration = store.registerUser(
      request = RegisterUserRequest(
        fullName = "New User",
        email = "new-user@example.com",
        phone = "+10000000010",
        location = "Riyadh",
        password = "Password123!",
        disabilityType = "Low vision",
        emergencyContactName = "Contact A",
        emergencyContactPhone = "+10000000011"
      ),
      passwordHash = "hash-user"
    )
    assertIs<ServiceResult.Success<AccountRecord>>(userRegistration)

    val volunteerRegistration = store.registerVolunteer(
      request = RegisterVolunteerRequest(
        fullName = "New Volunteer",
        email = "new-volunteer@example.com",
        phone = "+10000000020",
        location = "Riyadh",
        password = "Password123!",
        idNumber = "1122334455",
        dateOfBirth = "1999-01-01",
        motivation = "Helping community",
        languages = listOf("Arabic", "English"),
        availability = listOf("Weekends")
      ),
      passwordHash = "hash-volunteer"
    )
    assertIs<ServiceResult.Success<AccountRecord>>(volunteerRegistration)

    assertEquals(usersBefore + 1, rowCount(context.jdbcUrl, "user_profiles"))
    assertEquals(volunteersBefore + 1, rowCount(context.jdbcUrl, "volunteer_profiles"))
  }

  @Test
  fun registerVolunteerPersistsUploadedIdDocumentMetadataAndBytes() {
    val context = newStoreContext()
    val store = context.store
    val fileBytes = "sample-id-document".encodeToByteArray()

    val volunteerRegistration = store.registerVolunteer(
      request = RegisterVolunteerRequest(
        fullName = "Doc Volunteer",
        email = "doc-volunteer@example.com",
        phone = "+10000000021",
        location = "Riyadh",
        password = "Password123!",
        idNumber = "7788990011",
        dateOfBirth = "1995-04-03",
        motivation = "Help with accessibility support",
        languages = listOf("Arabic"),
        availability = listOf("Weekday evenings"),
        idDocumentFileName = "id-proof.pdf",
        idDocumentContentType = "application/pdf",
        idDocumentSizeBytes = fileBytes.size.toLong(),
        idDocumentBytes = fileBytes
      ),
      passwordHash = "hash-volunteer-doc"
    )
    val account = assertIs<ServiceResult.Success<AccountRecord>>(volunteerRegistration).value

    val (name, contentType, blobLength) = volunteerDocumentSnapshot(context.jdbcUrl, account.id)
    assertEquals("id-proof.pdf", name)
    assertEquals("application/pdf", contentType)
    assertNotNull(blobLength)
    assertEquals(fileBytes.size, blobLength)
  }

  @Test
  fun volunteerAnalyticsEarningsReflectWithdrawalHistory() {
    val store = newStoreContext().store

    val create = store.createAssistanceRequest(
      userId = "user-seed-1",
      request = CreateAssistanceRequest(
        userType = "Wheelchair user",
        location = "Mall",
        destination = "Gate B",
        distance = "0.5 km",
        urgency = "medium",
        helpType = "Navigation assistance",
        description = "Need navigation help",
        payment_method = PaymentMethod.CARD,
        service_fee = 200.0,
        hours = 2,
        price_per_hour = 100
      )
    )
    val requestId = assertIs<ServiceResult.Success<VolunteerRequestDto>>(create).value.id
    assertTrue(assertIs<ServiceResult.Success<ActionResultDto>>(store.acceptRequest("vol-seed-1", requestId)).value.success)
    assertTrue(assertIs<ServiceResult.Success<PayRequestResponseDto>>(store.payRequest("user-seed-1", requestId, PaymentMethod.CASH)).value.status == "active")
    assertTrue(assertIs<ServiceResult.Success<ActionResultDto>>(store.completeRequest("vol-seed-1", requestId)).value.success)

    val beforeWithdrawal = assertIs<ServiceResult.Success<VolunteerAnalyticsEarningsResponseDto>>(
      store.getVolunteerAnalyticsEarnings("vol-seed-1")
    ).value
    assertTrue(beforeWithdrawal.available_balance >= 140.0)
    assertTrue(beforeWithdrawal.withdrawal_history.isEmpty())

    val withdrawal = store.submitVolunteerWithdrawal("vol-seed-1", amount = 100.0, method = "wallet")
    assertTrue(assertIs<ServiceResult.Success<ActionResultDto>>(withdrawal).value.success)

    val afterWithdrawal = assertIs<ServiceResult.Success<VolunteerAnalyticsEarningsResponseDto>>(
      store.getVolunteerAnalyticsEarnings("vol-seed-1")
    ).value
    assertFalse(afterWithdrawal.withdrawal_history.isEmpty())
    assertEquals("completed", afterWithdrawal.withdrawal_history.first().status)
    assertEquals(100.0, afterWithdrawal.withdrawal_history.first().amount)
    assertEquals(beforeWithdrawal.available_balance - 100.0, afterWithdrawal.available_balance)
  }

  @Test
  fun cashRequestCapturesVolunteerPaymentWhenCompleted() {
    val store = newStoreContext().store

    val create = store.createAssistanceRequest(
      userId = "user-seed-1",
      request = CreateAssistanceRequest(
        userType = "Wheelchair user",
        location = "Mall",
        destination = "Gate B",
        distance = "0.5 km",
        urgency = "medium",
        helpType = "Navigation assistance",
        description = "Need navigation help",
        payment_method = PaymentMethod.CASH,
        service_fee = 200.0,
        hours = 2,
        price_per_hour = 100
      )
    )
    val requestId = assertIs<ServiceResult.Success<VolunteerRequestDto>>(create).value.id

    val accept = assertIs<ServiceResult.Success<ActionResultDto>>(store.acceptRequest("vol-seed-1", requestId)).value
    assertTrue(accept.success)

    val beforeCompletePerformance = assertIs<ServiceResult.Success<VolunteerAnalyticsPerformanceResponseDto>>(
      store.getVolunteerAnalyticsPerformance("vol-seed-1")
    ).value
    val beforeCompleteEarnings = assertIs<ServiceResult.Success<VolunteerAnalyticsEarningsResponseDto>>(
      store.getVolunteerAnalyticsEarnings("vol-seed-1")
    ).value

    assertTrue(assertIs<ServiceResult.Success<ActionResultDto>>(store.completeRequest("vol-seed-1", requestId)).value.success)

    val afterCompletePerformance = assertIs<ServiceResult.Success<VolunteerAnalyticsPerformanceResponseDto>>(
      store.getVolunteerAnalyticsPerformance("vol-seed-1")
    ).value
    val afterCompleteEarnings = assertIs<ServiceResult.Success<VolunteerAnalyticsEarningsResponseDto>>(
      store.getVolunteerAnalyticsEarnings("vol-seed-1")
    ).value

    assertTrue(afterCompletePerformance.completed >= beforeCompletePerformance.completed + 1)
    assertTrue(afterCompletePerformance.weekly_activity.sumOf { it.completed } >= beforeCompletePerformance.weekly_activity.sumOf { it.completed } + 1)
    assertTrue(afterCompleteEarnings.payment_history.any { it.status == "completed" })
    assertTrue(afterCompleteEarnings.total_net > beforeCompleteEarnings.total_net)
    assertTrue(afterCompleteEarnings.available_balance > beforeCompleteEarnings.available_balance)
  }

  @Test
  fun cardPaymentRefreshMovesVolunteerRequestIntoActiveDashboard() {
    val store = newStoreContext().store

    val create = store.createAssistanceRequest(
      userId = "user-seed-1",
      request = CreateAssistanceRequest(
        userType = "Wheelchair user",
        location = "Mall",
        destination = "Gate B",
        distance = "0.5 km",
        urgency = "medium",
        helpType = "Navigation assistance",
        description = "Need navigation help",
        payment_method = PaymentMethod.CARD,
        service_fee = 200.0,
        hours = 2,
        price_per_hour = 100
      )
    )
    val requestId = assertIs<ServiceResult.Success<VolunteerRequestDto>>(create).value.id

    assertTrue(assertIs<ServiceResult.Success<ActionResultDto>>(store.acceptRequest("vol-seed-1", requestId)).value.success)

    val checkout = assertIs<ServiceResult.Success<CheckoutResponseDto>>(
      store.checkoutCard(userId = "user-seed-1", requestId = requestId, amountEgp = 200.0)
    ).value

    val beforeRefreshActive = assertIs<ServiceResult.Success<VolunteerActiveResponseDto>>(
      store.getVolunteerActiveDashboard("vol-seed-1")
    ).value
    assertTrue(beforeRefreshActive.requests.any { it.id == requestId })

    val refreshedPayment = assertIs<ServiceResult.Success<PaymentStatusDto>>(
      store.refreshPayment(checkout.payment_id)
    ).value
    assertTrue(refreshedPayment.success)
    assertEquals("captured", refreshedPayment.status)

    val afterRefreshActive = assertIs<ServiceResult.Success<VolunteerActiveResponseDto>>(
      store.getVolunteerActiveDashboard("vol-seed-1")
    ).value
    assertTrue(afterRefreshActive.requests.any { it.id == requestId })
    assertTrue(afterRefreshActive.counts.active >= 1)
  }

  @Test
  fun paidRequestStateIsReturnedFromMyRequestsAfterRefreshAndRestart() {
    val context = newStoreContext()
    val store = context.store

    val create = store.createAssistanceRequest(
      userId = "user-seed-1",
      request = CreateAssistanceRequest(
        userType = "Wheelchair user",
        location = "Mall",
        destination = "Gate B",
        distance = "0.5 km",
        urgency = "medium",
        helpType = "Navigation assistance",
        description = "Need navigation help",
        payment_method = PaymentMethod.CARD,
        service_fee = 200.0,
        hours = 2,
        price_per_hour = 100
      )
    )
    val requestId = assertIs<ServiceResult.Success<VolunteerRequestDto>>(create).value.id

    assertTrue(assertIs<ServiceResult.Success<ActionResultDto>>(store.acceptRequest("vol-seed-1", requestId)).value.success)

    val checkout = assertIs<ServiceResult.Success<CheckoutResponseDto>>(
      store.checkoutCard(userId = "user-seed-1", requestId = requestId, amountEgp = 200.0)
    ).value

    val refreshedPayment = assertIs<ServiceResult.Success<PaymentStatusDto>>(
      store.refreshPayment(checkout.payment_id)
    ).value
    assertTrue(refreshedPayment.success)

    val beforeRestart = assertIs<ServiceResult.Success<MyRequestsResponse>>(
      store.getMyRequests("user-seed-1")
    ).value.userRequests.first { it.id == requestId }
    assertEquals("active", beforeRestart.status)
    assertEquals("captured", beforeRestart.payment_status)
    assertTrue(beforeRestart.is_paid)

    val restartedStore = InMemoryStore(accountDatabase = AccountDatabase(context.jdbcUrl))
    val afterRestart = assertIs<ServiceResult.Success<MyRequestsResponse>>(
      restartedStore.getMyRequests("user-seed-1")
    ).value.userRequests.first { it.id == requestId }
    assertEquals("active", afterRestart.status)
    assertEquals("captured", afterRestart.payment_status)
    assertTrue(afterRestart.is_paid)
  }

  @Test
  fun volunteerCompletionUsesPersistedPaymentStateAfterRestart() {
    val context = newStoreContext()
    val store = context.store

    val create = store.createAssistanceRequest(
      userId = "user-seed-1",
      request = CreateAssistanceRequest(
        userType = "Wheelchair user",
        location = "Mall",
        destination = "Gate B",
        distance = "0.5 km",
        urgency = "medium",
        helpType = "Navigation assistance",
        description = "Need navigation help",
        payment_method = PaymentMethod.CARD,
        service_fee = 200.0,
        hours = 2,
        price_per_hour = 100
      )
    )
    val requestId = assertIs<ServiceResult.Success<VolunteerRequestDto>>(create).value.id

    assertTrue(assertIs<ServiceResult.Success<ActionResultDto>>(store.acceptRequest("vol-seed-1", requestId)).value.success)

    val checkout = assertIs<ServiceResult.Success<CheckoutResponseDto>>(
      store.checkoutCard(userId = "user-seed-1", requestId = requestId, amountEgp = 200.0)
    ).value
    assertTrue(assertIs<ServiceResult.Success<PaymentStatusDto>>(store.refreshPayment(checkout.payment_id)).value.success)

    val restartedStore = InMemoryStore(accountDatabase = AccountDatabase(context.jdbcUrl))
    val complete = assertIs<ServiceResult.Success<ActionResultDto>>(
      restartedStore.completeRequest("vol-seed-1", requestId)
    ).value
    assertTrue(complete.success)

    val history = assertIs<ServiceResult.Success<VolunteerHistoryResponseDto>>(
      restartedStore.getVolunteerHistoryDashboard("vol-seed-1")
    ).value
    assertTrue(history.requests.any { it.id == requestId && it.status == "completed" })
  }
}
