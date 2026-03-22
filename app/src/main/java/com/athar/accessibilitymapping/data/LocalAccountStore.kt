package com.athar.accessibilitymapping.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val LOCAL_ACCOUNTS_STORE_NAME = "local_accounts_store"
private val Context.localAccountsDataStore by preferencesDataStore(name = LOCAL_ACCOUNTS_STORE_NAME)

@Serializable
private data class LocalStoredAccount(
  val id: String,
  val role: UserRole,
  val fullName: String,
  val email: String,
  val passwordHash: String,
  val disabilityType: String? = null,
  val volunteerLive: Boolean = false
)

class LocalAccountStore(private val context: Context) {
  private val accountsKey = stringPreferencesKey("accounts_json")
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  suspend fun login(email: String, password: String): AuthOperationResult {
    val normalizedEmail = email.trim().lowercase()
    val normalizedPassword = password.trim()
    if (normalizedEmail.isBlank() || normalizedPassword.isBlank()) {
      return AuthOperationResult.Error("Email or password is wrong.")
    }
    val account = readAccounts().firstOrNull { it.email == normalizedEmail }
      ?: return AuthOperationResult.Error("Email or password is wrong.")
    if (account.passwordHash != hashPassword(normalizedPassword)) {
      return AuthOperationResult.Error("Email or password is wrong.")
    }
    return AuthOperationResult.Success(
      AuthSession(
        userId = account.id,
        role = account.role,
        fullName = account.fullName,
        email = account.email,
        disabilityType = account.disabilityType,
        volunteerLive = account.volunteerLive
      )
    )
  }

  suspend fun registerUser(payload: UserRegistrationPayload): AuthOperationResult {
    val normalizedEmail = payload.email.trim().lowercase()
    val fullName = payload.fullName.trim()
    if (fullName.isBlank()) return AuthOperationResult.Error("Full name is required.")
    if (normalizedEmail.isBlank()) return AuthOperationResult.Error("Email is required.")
    if (payload.password.length < 8) return AuthOperationResult.Error("Password must be at least 8 characters.")

    val accounts = readAccounts().toMutableList()
    val existing = accounts.firstOrNull { it.email == normalizedEmail }
    if (existing != null) {
      val roleName = if (existing.role == UserRole.Volunteer) "volunteer" else "user"
      return AuthOperationResult.Error("This email is already registered as $roleName. Please sign in.")
    }

    val account = LocalStoredAccount(
      id = "local-user-${UUID.randomUUID()}",
      role = UserRole.User,
      fullName = fullName,
      email = normalizedEmail,
      passwordHash = hashPassword(payload.password),
      disabilityType = payload.disabilityType.ifBlank { null },
      volunteerLive = false
    )
    accounts += account
    writeAccounts(accounts)
    return AuthOperationResult.Success(
      AuthSession(
        userId = account.id,
        role = account.role,
        fullName = account.fullName,
        email = account.email,
        disabilityType = account.disabilityType,
        volunteerLive = account.volunteerLive
      )
    )
  }

  suspend fun registerVolunteer(payload: VolunteerRegistrationPayload): AuthOperationResult {
    val normalizedEmail = payload.email.trim().lowercase()
    val fullName = payload.fullName.trim()
    if (fullName.isBlank()) return AuthOperationResult.Error("Full name is required.")
    if (normalizedEmail.isBlank()) return AuthOperationResult.Error("Email is required.")
    if (payload.password.length < 8) return AuthOperationResult.Error("Password must be at least 8 characters.")

    val accounts = readAccounts().toMutableList()
    val existing = accounts.firstOrNull { it.email == normalizedEmail }
    if (existing != null) {
      val roleName = if (existing.role == UserRole.Volunteer) "volunteer" else "user"
      return AuthOperationResult.Error("This email is already registered as $roleName. Please sign in.")
    }

    val account = LocalStoredAccount(
      id = "local-vol-${UUID.randomUUID()}",
      role = UserRole.Volunteer,
      fullName = fullName,
      email = normalizedEmail,
      passwordHash = hashPassword(payload.password),
      disabilityType = null,
      volunteerLive = false
    )
    accounts += account
    writeAccounts(accounts)
    return AuthOperationResult.Success(
      AuthSession(
        userId = account.id,
        role = account.role,
        fullName = account.fullName,
        email = account.email,
        disabilityType = null,
        volunteerLive = false
      )
    )
  }

  private suspend fun readAccounts(): List<LocalStoredAccount> {
    val raw = context.localAccountsDataStore.data.first()[accountsKey].orEmpty()
    if (raw.isBlank()) return emptyList()
    return runCatching { json.decodeFromString<List<LocalStoredAccount>>(raw) }
      .getOrElse { emptyList() }
  }

  private suspend fun writeAccounts(accounts: List<LocalStoredAccount>) {
    context.localAccountsDataStore.edit { prefs ->
      prefs[accountsKey] = json.encodeToString(ListSerializer(LocalStoredAccount.serializer()), accounts)
    }
  }

  private fun hashPassword(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return bytes.joinToString("") { byte -> "%02x".format(byte) }
  }
}
