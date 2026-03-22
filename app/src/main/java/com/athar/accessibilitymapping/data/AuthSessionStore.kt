package com.athar.accessibilitymapping.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private const val AUTH_STORE_NAME = "auth_session_store"
private val Context.authDataStore by preferencesDataStore(name = AUTH_STORE_NAME)

class AuthSessionStore(private val context: Context) {
  private val userIdKey = stringPreferencesKey("user_id")
  private val accessTokenKey = stringPreferencesKey("access_token")
  private val refreshTokenKey = stringPreferencesKey("refresh_token")
  private val expiresAtKey = longPreferencesKey("expires_at_epoch_seconds")
  private val roleKey = stringPreferencesKey("role")
  private val fullNameKey = stringPreferencesKey("full_name")
  private val emailKey = stringPreferencesKey("email")
  private val phoneKey = stringPreferencesKey("phone")
  private val disabilityTypeKey = stringPreferencesKey("disability_type")
  private val volunteerLiveKey = booleanPreferencesKey("volunteer_live")

  suspend fun saveSession(
    userId: String,
    accessToken: String,
    refreshToken: String,
    expiresAtEpochSeconds: Long,
    role: UserRole,
    fullName: String,
    email: String,
    phone: String,
    disabilityType: String?,
    volunteerLive: Boolean
  ) {
    context.authDataStore.edit { prefs ->
      prefs[userIdKey] = userId
      prefs[accessTokenKey] = accessToken
      prefs[refreshTokenKey] = refreshToken
      prefs[expiresAtKey] = expiresAtEpochSeconds
      prefs[roleKey] = role.name
      prefs[fullNameKey] = fullName
      prefs[emailKey] = email
      prefs[phoneKey] = phone
      prefs[volunteerLiveKey] = volunteerLive
      if (disabilityType.isNullOrBlank()) {
        prefs.remove(disabilityTypeKey)
      } else {
        prefs[disabilityTypeKey] = disabilityType
      }
    }
  }

  suspend fun clearSession() {
    context.authDataStore.edit { prefs ->
      prefs.clear()
    }
  }

  suspend fun updateVolunteerLive(volunteerLive: Boolean) {
    context.authDataStore.edit { prefs ->
      if (prefs.contains(roleKey) && prefs[roleKey] == UserRole.Volunteer.name) {
        prefs[volunteerLiveKey] = volunteerLive
      }
    }
  }

  suspend fun getAccessToken(): String? {
    return context.authDataStore.data.first()[accessTokenKey]
  }

  suspend fun getRefreshToken(): String? {
    return context.authDataStore.data.first()[refreshTokenKey]
  }

  suspend fun readAuthSession(): AuthSession? {
    val prefs = context.authDataStore.data.first()
    val roleName = prefs[roleKey] ?: return null
    val role = runCatching { UserRole.valueOf(roleName) }.getOrNull() ?: return null
    val userId = prefs[userIdKey] ?: return null
    val fullName = prefs[fullNameKey] ?: return null
    val email = prefs[emailKey] ?: return null
    val phone = prefs[phoneKey].orEmpty()
    return AuthSession(
      userId = userId,
      role = role,
      fullName = fullName,
      email = email,
      phone = phone,
      disabilityType = prefs[disabilityTypeKey],
      volunteerLive = prefs[volunteerLiveKey] == true
    )
  }
}
