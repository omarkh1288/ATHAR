package com.athar.backend

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class UserProfilePersistence(
  val emergencyContactName: String?,
  val emergencyContactPhone: String?
)

internal data class VolunteerProfilePersistence(
  val nationalId: String?,
  val dateOfBirth: String?,
  val motivation: String?,
  val languages: List<String>,
  val availability: List<String>,
  val idDocumentFileName: String?,
  val idDocumentContentType: String?,
  val idDocumentSizeBytes: Long?,
  val idDocumentBytes: ByteArray?
)

internal data class HelpRequestPersistence(
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
  val paymentMethod: String,
  val serviceFee: Double,
  val hours: Int,
  val pricePerHour: Int,
  val createdAtEpochSeconds: Long,
  val status: String,
  val volunteerId: String?,
  val volunteerName: String?
)

internal data class HelpRequestMessagePersistence(
  val id: String,
  val requestId: String,
  val senderId: String,
  val senderName: String,
  val message: String,
  val createdAtEpochSeconds: Long
)

internal data class PaymentPersistence(
  val id: String,
  val requestId: String?,
  val userId: String,
  val amount: Double,
  val currency: String,
  val paymentMethod: String,
  val status: String,
  val success: Boolean,
  val checkoutUrl: String?,
  val createdAtEpochSeconds: Long
)

internal data class VolunteerReviewPersistence(
  val id: String,
  val requestId: String,
  val volunteerId: String,
  val userId: String,
  val userName: String,
  val rating: Int,
  val comment: String,
  val issues: List<String>,
  val createdAtEpochSeconds: Long
)

internal class AccountDatabase(
  private val jdbcUrl: String = defaultJdbcUrl(),
  private val json: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }
) {
  init {
    runCatching { Class.forName("org.sqlite.JDBC") }
    initialize()
  }

  fun hasAnyAccount(): Boolean {
    return withConnection { connection ->
      connection.prepareStatement("SELECT 1 FROM accounts LIMIT 1").use { statement ->
        statement.executeQuery().use { result -> result.next() }
      }
    }
  }

  fun loadAccounts(): List<AccountRecord> {
    return withConnection { connection ->
      connection.prepareStatement(
        """
        SELECT id, role, full_name, email, password_hash, phone, location, disability_type, member_since,
               volunteer_live, role_verified_at, contribution_stats_json, notification_settings_json, privacy_settings_json
        FROM accounts
        ORDER BY id
        """.trimIndent()
      ).use { statement ->
        statement.executeQuery().use { result ->
          buildList {
            while (result.next()) {
              val role = runCatching { UserRole.valueOf(result.getString("role")) }.getOrElse { UserRole.User }
              val contributionStats = decodeOrDefault(
                raw = result.getString("contribution_stats_json"),
                fallback = ContributionStatsDto(0, 0, 0)
              )
              val notificationSettings = decodeOrDefault(
                raw = result.getString("notification_settings_json"),
                fallback = NotificationSettingsDto()
              )
              val privacySettings = decodeOrDefault(
                raw = result.getString("privacy_settings_json"),
                fallback = PrivacySettingsDto()
              )
              
              add(
                AccountRecord(
                  id = result.getString("id"),
                  role = role,
                  fullName = result.getString("full_name").orEmpty(),
                  email = result.getString("email").orEmpty(),
                  passwordHash = result.getString("password_hash").orEmpty(),
                  phone = result.getString("phone").orEmpty(),
                  location = result.getString("location").orEmpty(),
                  disabilityType = result.getString("disability_type")?.takeIf { it.isNotBlank() },
                  memberSince = result.getString("member_since").orEmpty(),
                  volunteerLive = result.getInt("volunteer_live") != 0,
                  roleVerifiedAt = result.getString("role_verified_at"),
                  contributionStats = contributionStats,
                  notificationSettings = notificationSettings,
                  privacySettings = privacySettings
                )
              )
            }
          }
        }
      }
    }
  }

  fun upsertAccount(account: AccountRecord) {
    withConnection { connection ->
      connection.prepareStatement(
        """
        INSERT INTO accounts (
          id, role, full_name, email, password_hash, phone, location, disability_type, member_since,
          volunteer_live, role_verified_at, contribution_stats_json, notification_settings_json, privacy_settings_json
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
          role = excluded.role,
          full_name = excluded.full_name,
          email = excluded.email,
          password_hash = excluded.password_hash,
          phone = excluded.phone,
          location = excluded.location,
          disability_type = excluded.disability_type,
          member_since = excluded.member_since,
          volunteer_live = excluded.volunteer_live,
          role_verified_at = excluded.role_verified_at,
          contribution_stats_json = excluded.contribution_stats_json,
          notification_settings_json = excluded.notification_settings_json,
          privacy_settings_json = excluded.privacy_settings_json
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, account.id)
        statement.setString(2, account.role.name)
        statement.setString(3, account.fullName)
        statement.setString(4, account.email)
        statement.setString(5, account.passwordHash)
        statement.setString(6, account.phone)
        statement.setString(7, account.location)
        statement.setString(8, account.disabilityType)
        statement.setString(9, account.memberSince)
        statement.setInt(10, if (account.volunteerLive) 1 else 0)
        statement.setString(11, account.roleVerifiedAt)
        statement.setString(12, json.encodeToString(account.contributionStats))
        statement.setString(13, json.encodeToString(account.notificationSettings))
        statement.setString(14, json.encodeToString(account.privacySettings))
        statement.executeUpdate()
      }
    }
  }

  fun upsertUserProfile(accountId: String, profile: UserProfilePersistence) {
    withConnection { connection ->
      connection.prepareStatement(
        """
        INSERT INTO user_profiles (account_id, emergency_contact_name, emergency_contact_phone)
        VALUES (?, ?, ?)
        ON CONFLICT(account_id) DO UPDATE SET
          emergency_contact_name = excluded.emergency_contact_name,
          emergency_contact_phone = excluded.emergency_contact_phone
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, accountId)
        statement.setString(2, profile.emergencyContactName)
        statement.setString(3, profile.emergencyContactPhone)
        statement.executeUpdate()
      }
    }
  }

  fun upsertVolunteerProfile(accountId: String, profile: VolunteerProfilePersistence) {
    withConnection { connection ->
      connection.prepareStatement(
        """
        INSERT INTO volunteer_profiles (
          account_id, national_id, date_of_birth, motivation, languages_json, availability_json,
          id_document_name, id_document_content_type, id_document_size_bytes, id_document_blob
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(account_id) DO UPDATE SET
          national_id = excluded.national_id,
          date_of_birth = excluded.date_of_birth,
          motivation = excluded.motivation,
          languages_json = excluded.languages_json,
          availability_json = excluded.availability_json,
          id_document_name = excluded.id_document_name,
          id_document_content_type = excluded.id_document_content_type,
          id_document_size_bytes = excluded.id_document_size_bytes,
          id_document_blob = excluded.id_document_blob
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, accountId)
        statement.setString(2, profile.nationalId)
        statement.setString(3, profile.dateOfBirth)
        statement.setString(4, profile.motivation)
        statement.setString(5, json.encodeToString(profile.languages))
        statement.setString(6, json.encodeToString(profile.availability))
        statement.setString(7, profile.idDocumentFileName)
        statement.setString(8, profile.idDocumentContentType)
        if (profile.idDocumentSizeBytes != null) {
          statement.setLong(9, profile.idDocumentSizeBytes)
        } else {
          statement.setNull(9, Types.BIGINT)
        }
        if (profile.idDocumentBytes != null) {
          statement.setBytes(10, profile.idDocumentBytes)
        } else {
          statement.setNull(10, Types.BLOB)
        }
        statement.executeUpdate()
      }
    }
  }

  fun loadHelpRequests(): List<HelpRequestPersistence> {
    return withConnection { connection ->
      connection.prepareStatement(
        """
        SELECT id, user_id, user_name, user_type, location, destination, distance, urgency, help_type, description,
               payment_method, service_fee, hours, price_per_hour, created_at, status, volunteer_id, volunteer_name
        FROM help_requests
        ORDER BY created_at DESC
        """.trimIndent()
      ).use { statement ->
        statement.executeQuery().use { result ->
          buildList {
            while (result.next()) {
              add(
                HelpRequestPersistence(
                  id = result.getString("id"),
                  userId = result.getString("user_id"),
                  userName = result.getString("user_name"),
                  userType = result.getString("user_type"),
                  location = result.getString("location"),
                  destination = result.getString("destination"),
                  distance = result.getString("distance"),
                  urgency = result.getString("urgency"),
                  helpType = result.getString("help_type"),
                  description = result.getString("description"),
                  paymentMethod = result.getString("payment_method"),
                  serviceFee = result.getDouble("service_fee"),
                  hours = result.getInt("hours"),
                  pricePerHour = result.getInt("price_per_hour"),
                  createdAtEpochSeconds = result.getLong("created_at"),
                  status = result.getString("status"),
                  volunteerId = result.getString("volunteer_id"),
                  volunteerName = result.getString("volunteer_name")
                )
              )
            }
          }
        }
      }
    }
  }

  fun upsertHelpRequest(request: HelpRequestPersistence) {
    withConnection { connection ->
      connection.prepareStatement(
        """
        INSERT INTO help_requests (
          id, user_id, user_name, user_type, location, destination, distance, urgency, help_type, description,
          payment_method, service_fee, hours, price_per_hour, created_at, status, volunteer_id, volunteer_name
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
          status = excluded.status,
          volunteer_id = excluded.volunteer_id,
          volunteer_name = excluded.volunteer_name,
          payment_method = excluded.payment_method
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, request.id)
        statement.setString(2, request.userId)
        statement.setString(3, request.userName)
        statement.setString(4, request.userType)
        statement.setString(5, request.location)
        statement.setString(6, request.destination)
        statement.setString(7, request.distance)
        statement.setString(8, request.urgency)
        statement.setString(9, request.helpType)
        statement.setString(10, request.description)
        statement.setString(11, request.paymentMethod)
        statement.setDouble(12, request.serviceFee)
        statement.setInt(13, request.hours)
        statement.setInt(14, request.pricePerHour)
        statement.setLong(15, request.createdAtEpochSeconds)
        statement.setString(16, request.status)
        statement.setString(17, request.volunteerId)
        statement.setString(18, request.volunteerName)
        statement.executeUpdate()
      }
    }
  }

  fun loadHelpRequestMessages(requestId: String): List<HelpRequestMessagePersistence> {
    return withConnection { connection ->
      connection.prepareStatement(
        "SELECT id, request_id, sender_id, sender_name, message, created_at FROM help_request_messages WHERE request_id = ? ORDER BY created_at"
      ).use { statement ->
        statement.setString(1, requestId)
        statement.executeQuery().use { result ->
          buildList {
            while (result.next()) {
              add(
                HelpRequestMessagePersistence(
                  id = result.getString("id"),
                  requestId = result.getString("request_id"),
                  senderId = result.getString("sender_id"),
                  senderName = result.getString("sender_name"),
                  message = result.getString("message"),
                  createdAtEpochSeconds = result.getLong("created_at")
                )
              )
            }
          }
        }
      }
    }
  }

  fun insertHelpRequestMessage(message: HelpRequestMessagePersistence) {
    withConnection { connection ->
      connection.prepareStatement(
        "INSERT INTO help_request_messages (id, request_id, sender_id, sender_name, message, created_at) VALUES (?, ?, ?, ?, ?, ?)"
      ).use { statement ->
        statement.setString(1, message.id)
        statement.setString(2, message.requestId)
        statement.setString(3, message.senderId)
        statement.setString(4, message.senderName)
        statement.setString(5, message.message)
        statement.setLong(6, message.createdAtEpochSeconds)
        statement.executeUpdate()
      }
    }
  }

  fun loadPayments(): List<PaymentPersistence> {
    return withConnection { connection ->
      connection.prepareStatement(
        "SELECT id, request_id, user_id, amount, currency, payment_method, status, success, checkout_url, created_at FROM payments"
      ).use { statement ->
        statement.executeQuery().use { result ->
          buildList {
            while (result.next()) {
              add(
                PaymentPersistence(
                  id = result.getString("id"),
                  requestId = result.getString("request_id"),
                  userId = result.getString("user_id"),
                  amount = result.getDouble("amount"),
                  currency = result.getString("currency"),
                  paymentMethod = result.getString("payment_method"),
                  status = result.getString("status"),
                  success = result.getInt("success") != 0,
                  checkoutUrl = result.getString("checkout_url"),
                  createdAtEpochSeconds = result.getLong("created_at")
                )
              )
            }
          }
        }
      }
    }
  }

  fun upsertPayment(payment: PaymentPersistence) {
    withConnection { connection ->
      connection.prepareStatement(
        """
        INSERT INTO payments (id, request_id, user_id, amount, currency, payment_method, status, success, checkout_url, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
          request_id = excluded.request_id,
          user_id = excluded.user_id,
          amount = excluded.amount,
          currency = excluded.currency,
          payment_method = excluded.payment_method,
          status = excluded.status,
          success = excluded.success,
          checkout_url = excluded.checkout_url,
          created_at = excluded.created_at
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, payment.id)
        statement.setString(2, payment.requestId)
        statement.setString(3, payment.userId)
        statement.setDouble(4, payment.amount)
        statement.setString(5, payment.currency)
        statement.setString(6, payment.paymentMethod)
        statement.setString(7, payment.status)
        statement.setInt(8, if (payment.success) 1 else 0)
        statement.setString(9, payment.checkoutUrl)
        statement.setLong(10, payment.createdAtEpochSeconds)
        statement.executeUpdate()
      }
    }
  }

  fun loadVolunteerReviews(volunteerId: String? = null): List<VolunteerReviewPersistence> {
    return withConnection { connection ->
      val sql = buildString {
        append(
          """
          SELECT id, request_id, volunteer_id, user_id, user_name, rating, comment, issues_json, created_at
          FROM volunteer_reviews
          """.trimIndent()
        )
        if (volunteerId != null) {
          append(" WHERE volunteer_id = ?")
        }
        append(" ORDER BY created_at DESC")
      }
      connection.prepareStatement(sql).use { statement ->
        if (volunteerId != null) {
          statement.setString(1, volunteerId)
        }
        statement.executeQuery().use { result ->
          buildList {
            while (result.next()) {
              add(
                VolunteerReviewPersistence(
                  id = result.getString("id"),
                  requestId = result.getString("request_id"),
                  volunteerId = result.getString("volunteer_id"),
                  userId = result.getString("user_id"),
                  userName = result.getString("user_name"),
                  rating = result.getInt("rating"),
                  comment = result.getString("comment").orEmpty(),
                  issues = decodeOrDefault(result.getString("issues_json"), emptyList()),
                  createdAtEpochSeconds = result.getLong("created_at")
                )
              )
            }
          }
        }
      }
    }
  }

  fun upsertVolunteerReview(review: VolunteerReviewPersistence) {
    withConnection { connection ->
      connection.prepareStatement(
        """
        INSERT INTO volunteer_reviews (
          id, request_id, volunteer_id, user_id, user_name, rating, comment, issues_json, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(request_id) DO UPDATE SET
          volunteer_id = excluded.volunteer_id,
          user_id = excluded.user_id,
          user_name = excluded.user_name,
          rating = excluded.rating,
          comment = excluded.comment,
          issues_json = excluded.issues_json,
          created_at = excluded.created_at
        """.trimIndent()
      ).use { statement ->
        statement.setString(1, review.id)
        statement.setString(2, review.requestId)
        statement.setString(3, review.volunteerId)
        statement.setString(4, review.userId)
        statement.setString(5, review.userName)
        statement.setInt(6, review.rating)
        statement.setString(7, review.comment)
        statement.setString(8, json.encodeToString(review.issues))
        statement.setLong(9, review.createdAtEpochSeconds)
        statement.executeUpdate()
      }
    }
  }

  private fun initialize() {
    withConnection { connection ->
      connection.createStatement().use { statement ->
        statement.execute(
          """
          CREATE TABLE IF NOT EXISTS accounts (
            id TEXT PRIMARY KEY,
            role TEXT NOT NULL CHECK(role IN ('User', 'Volunteer')),
            full_name TEXT NOT NULL,
            email TEXT NOT NULL UNIQUE,
            password_hash TEXT NOT NULL,
            phone TEXT NOT NULL,
            location TEXT NOT NULL DEFAULT '',
            disability_type TEXT,
            member_since TEXT NOT NULL,
            volunteer_live INTEGER NOT NULL DEFAULT 0,
            role_verified_at TEXT,
            contribution_stats_json TEXT NOT NULL,
            notification_settings_json TEXT NOT NULL,
            privacy_settings_json TEXT NOT NULL
          )
          """.trimIndent()
        )
        statement.execute(
          """
          CREATE TABLE IF NOT EXISTS user_profiles (
            account_id TEXT PRIMARY KEY,
            emergency_contact_name TEXT,
            emergency_contact_phone TEXT,
            FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE
          )
          """.trimIndent()
        )
        statement.execute(
          """
          CREATE TABLE IF NOT EXISTS volunteer_profiles (
            account_id TEXT PRIMARY KEY,
            national_id TEXT,
            date_of_birth TEXT,
            motivation TEXT,
            languages_json TEXT NOT NULL,
            availability_json TEXT NOT NULL,
            id_document_name TEXT,
            id_document_content_type TEXT,
            id_document_size_bytes INTEGER,
            id_document_blob BLOB,
            FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE
          )
          """.trimIndent()
        )
        statement.execute(
          """
          CREATE TABLE IF NOT EXISTS help_requests (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            user_name TEXT NOT NULL,
            user_type TEXT NOT NULL,
            location TEXT NOT NULL,
            destination TEXT NOT NULL,
            distance TEXT NOT NULL,
            urgency TEXT NOT NULL,
            help_type TEXT NOT NULL,
            description TEXT NOT NULL,
            payment_method TEXT NOT NULL,
            service_fee REAL NOT NULL,
            hours INTEGER DEFAULT 1,
            price_per_hour INTEGER DEFAULT 50,
            created_at INTEGER NOT NULL,
            status TEXT NOT NULL,
            volunteer_id TEXT,
            volunteer_name TEXT,
            FOREIGN KEY(user_id) REFERENCES accounts(id) ON DELETE CASCADE,
            FOREIGN KEY(volunteer_id) REFERENCES accounts(id) ON DELETE SET NULL
          )
          """.trimIndent()
        )
        statement.execute(
          """
          CREATE TABLE IF NOT EXISTS help_request_messages (
            id TEXT PRIMARY KEY,
            request_id TEXT NOT NULL,
            sender_id TEXT NOT NULL,
            sender_name TEXT NOT NULL,
            message TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            FOREIGN KEY(request_id) REFERENCES help_requests(id) ON DELETE CASCADE,
            FOREIGN KEY(sender_id) REFERENCES accounts(id) ON DELETE CASCADE
          )
          """.trimIndent()
        )
        statement.execute(
          """
          CREATE TABLE IF NOT EXISTS payments (
            id TEXT PRIMARY KEY,
            request_id TEXT,
            user_id TEXT NOT NULL,
            amount REAL NOT NULL,
            currency TEXT NOT NULL,
            payment_method TEXT NOT NULL,
            status TEXT NOT NULL,
            success INTEGER NOT NULL,
            checkout_url TEXT,
            created_at INTEGER NOT NULL,
            FOREIGN KEY(user_id) REFERENCES accounts(id) ON DELETE CASCADE,
            FOREIGN KEY(request_id) REFERENCES help_requests(id) ON DELETE SET NULL
          )
          """.trimIndent()
        )
        statement.execute(
          """
          CREATE TABLE IF NOT EXISTS volunteer_reviews (
            id TEXT PRIMARY KEY,
            request_id TEXT NOT NULL UNIQUE,
            volunteer_id TEXT NOT NULL,
            user_id TEXT NOT NULL,
            user_name TEXT NOT NULL,
            rating INTEGER NOT NULL,
            comment TEXT NOT NULL,
            issues_json TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            FOREIGN KEY(request_id) REFERENCES help_requests(id) ON DELETE CASCADE,
            FOREIGN KEY(volunteer_id) REFERENCES accounts(id) ON DELETE CASCADE,
            FOREIGN KEY(user_id) REFERENCES accounts(id) ON DELETE CASCADE
          )
          """.trimIndent()
        )
        ensureVolunteerProfileColumns(connection)
        ensureHelpRequestColumns(connection)
      }
    }
  }

  private fun ensureVolunteerProfileColumns(connection: Connection) {
    val existingColumns = mutableSetOf<String>()
    connection.prepareStatement("PRAGMA table_info(volunteer_profiles)").use { statement ->
      statement.executeQuery().use { result ->
        while (result.next()) {
          existingColumns += result.getString("name")
        }
      }
    }

    fun addColumnIfMissing(columnName: String, definition: String) {
      if (existingColumns.contains(columnName)) return
      connection.createStatement().use { statement ->
        statement.execute("ALTER TABLE volunteer_profiles ADD COLUMN $columnName $definition")
      }
      existingColumns += columnName
    }

    addColumnIfMissing("id_document_name", "TEXT")
    addColumnIfMissing("id_document_content_type", "TEXT")
    addColumnIfMissing("id_document_size_bytes", "INTEGER")
    addColumnIfMissing("id_document_blob", "BLOB")
  }

  private fun ensureHelpRequestColumns(connection: Connection) {
    val existingColumns = mutableSetOf<String>()
    connection.prepareStatement("PRAGMA table_info(help_requests)").use { statement ->
      statement.executeQuery().use { result ->
        while (result.next()) {
          existingColumns += result.getString("name")
        }
      }
    }

    fun addColumnIfMissing(columnName: String, definition: String) {
      if (existingColumns.contains(columnName)) return
      connection.createStatement().use { statement ->
        statement.execute("ALTER TABLE help_requests ADD COLUMN $columnName $definition")
      }
      existingColumns += columnName
    }

    addColumnIfMissing("hours", "INTEGER DEFAULT 1")
    addColumnIfMissing("price_per_hour", "INTEGER DEFAULT 50")
  }

  private inline fun <T> withConnection(block: (Connection) -> T): T {
    DriverManager.getConnection(jdbcUrl).use { connection ->
      connection.createStatement().use { statement ->
        statement.execute("PRAGMA foreign_keys = ON")
      }
      return block(connection)
    }
  }

  private inline fun <reified T> decodeOrDefault(raw: String?, fallback: T): T {
    return raw
      ?.takeIf { it.isNotBlank() }
      ?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }
      ?: fallback
  }

  companion object {
    private fun defaultJdbcUrl(): String {
      val configured = System.getenv("ATHAR_DB_URL")?.trim().orEmpty()
      if (configured.isNotBlank()) return configured

      val backendDirectory = resolveBackendDirectory()
      val databasePath = backendDirectory?.resolve("athar.db")
        ?: Paths.get(System.getProperty("user.home"), ".athar", "athar.db")
      databasePath.parent?.let { Files.createDirectories(it) }
      return "jdbc:sqlite:${databasePath.toAbsolutePath()}"
    }

    private fun resolveBackendDirectory(): Path? {
      var current = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
      repeat(8) {
        if (looksLikeBackendDirectory(current)) return current
        val backendChild = current.resolve("backend")
        if (looksLikeBackendDirectory(backendChild)) return backendChild
        current = current.parent ?: return null
      }
      return null
    }

    private fun looksLikeBackendDirectory(path: Path): Boolean {
      return Files.exists(
        path.resolve("src")
          .resolve("main")
          .resolve("kotlin")
          .resolve("com")
          .resolve("athar")
          .resolve("backend")
      )
    }
  }
}
