package com.athar.accessibilitymapping.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first

private const val APP_PREFERENCES_STORE_NAME = "app_preferences_store"
private val Context.appPreferencesDataStore by preferencesDataStore(name = APP_PREFERENCES_STORE_NAME)

class AppPreferencesStore(private val context: Context) {
  private val appLanguageKey = stringPreferencesKey("app_language")
  private val profileLocationKey = stringPreferencesKey("profile_location")
  private val memberSinceKey = stringPreferencesKey("member_since")

  private val pushEnabledKey = booleanPreferencesKey("notif_push_enabled")
  private val emailEnabledKey = booleanPreferencesKey("notif_email_enabled")
  private val smsEnabledKey = booleanPreferencesKey("notif_sms_enabled")
  private val volunteerRequestsKey = booleanPreferencesKey("notif_volunteer_requests")
  private val volunteerAcceptedKey = booleanPreferencesKey("notif_volunteer_accepted")
  private val locationUpdatesKey = booleanPreferencesKey("notif_location_updates")
  private val newRatingsKey = booleanPreferencesKey("notif_new_ratings")
  private val communityUpdatesKey = booleanPreferencesKey("notif_community_updates")
  private val marketingEmailsKey = booleanPreferencesKey("notif_marketing_emails")
  private val soundEnabledKey = booleanPreferencesKey("notif_sound_enabled")
  private val vibrationEnabledKey = booleanPreferencesKey("notif_vibration_enabled")

  private val locationSharingKey = booleanPreferencesKey("privacy_location_sharing")
  private val profileVisibilityKey = booleanPreferencesKey("privacy_profile_visibility")
  private val showRatingsKey = booleanPreferencesKey("privacy_show_ratings")
  private val activityStatusKey = booleanPreferencesKey("privacy_activity_status")
  private val twoFactorAuthKey = booleanPreferencesKey("privacy_two_factor_auth")

  suspend fun saveProfileLocation(location: String) {
    context.appPreferencesDataStore.edit { prefs ->
      prefs[profileLocationKey] = location
    }
  }

  suspend fun saveLanguage(languageCode: String) {
    context.appPreferencesDataStore.edit { prefs ->
      prefs[appLanguageKey] = languageCode
    }
  }

  suspend fun readLanguage(): String {
    return context.appPreferencesDataStore.data.first()[appLanguageKey] ?: "en"
  }

  suspend fun readProfileLocation(): String {
    return context.appPreferencesDataStore.data.first()[profileLocationKey].orEmpty()
  }

  suspend fun saveProfilePhotoPath(userId: String, photoPath: String?) {
    val key = stringPreferencesKey("profile_photo_path_$userId")
    context.appPreferencesDataStore.edit { prefs ->
      if (photoPath.isNullOrBlank()) {
        prefs.remove(key)
      } else {
        prefs[key] = photoPath
      }
    }
  }

  suspend fun readProfilePhotoPath(userId: String): String? {
    val key = stringPreferencesKey("profile_photo_path_$userId")
    return context.appPreferencesDataStore.data.first()[key]
  }

  suspend fun readOrCreateMemberSince(): String {
    val prefs = context.appPreferencesDataStore.data.first()
    val existing = prefs[memberSinceKey]
    if (!existing.isNullOrBlank()) {
      return existing
    }
    val now = YearMonth.now()
    val formatted = now.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
    context.appPreferencesDataStore.edit { mutable ->
      mutable[memberSinceKey] = formatted
    }
    return formatted
  }

  suspend fun readNotificationSettings(): ApiNotificationSettings {
    val prefs = context.appPreferencesDataStore.data.first()
    return ApiNotificationSettings(
      pushEnabled = prefs.readBoolean(pushEnabledKey, true),
      emailEnabled = prefs.readBoolean(emailEnabledKey, true),
      smsEnabled = prefs.readBoolean(smsEnabledKey, false),
      volunteerRequests = prefs.readBoolean(volunteerRequestsKey, true),
      volunteerAccepted = prefs.readBoolean(volunteerAcceptedKey, true),
      locationUpdates = prefs.readBoolean(locationUpdatesKey, true),
      newRatings = prefs.readBoolean(newRatingsKey, true),
      communityUpdates = prefs.readBoolean(communityUpdatesKey, false),
      marketingEmails = prefs.readBoolean(marketingEmailsKey, false),
      soundEnabled = prefs.readBoolean(soundEnabledKey, true),
      vibrationEnabled = prefs.readBoolean(vibrationEnabledKey, true)
    )
  }

  suspend fun saveNotificationSettings(settings: ApiNotificationSettings) {
    context.appPreferencesDataStore.edit { prefs ->
      prefs[pushEnabledKey] = settings.pushEnabled
      prefs[emailEnabledKey] = settings.emailEnabled
      prefs[smsEnabledKey] = settings.smsEnabled
      prefs[volunteerRequestsKey] = settings.volunteerRequests
      prefs[volunteerAcceptedKey] = settings.volunteerAccepted
      prefs[locationUpdatesKey] = settings.locationUpdates
      prefs[newRatingsKey] = settings.newRatings
      prefs[communityUpdatesKey] = settings.communityUpdates
      prefs[marketingEmailsKey] = settings.marketingEmails
      prefs[soundEnabledKey] = settings.soundEnabled
      prefs[vibrationEnabledKey] = settings.vibrationEnabled
    }
  }

  suspend fun readPrivacySettings(): ApiPrivacySettings {
    val prefs = context.appPreferencesDataStore.data.first()
    return ApiPrivacySettings(
      locationSharing = prefs.readBoolean(locationSharingKey, true),
      profileVisibility = prefs.readBoolean(profileVisibilityKey, true),
      showRatings = prefs.readBoolean(showRatingsKey, true),
      activityStatus = prefs.readBoolean(activityStatusKey, true),
      twoFactorAuth = prefs.readBoolean(twoFactorAuthKey, false)
    )
  }

  suspend fun savePrivacySettings(settings: ApiPrivacySettings) {
    context.appPreferencesDataStore.edit { prefs ->
      prefs[locationSharingKey] = settings.locationSharing
      prefs[profileVisibilityKey] = settings.profileVisibility
      prefs[showRatingsKey] = settings.showRatings
      prefs[activityStatusKey] = settings.activityStatus
      prefs[twoFactorAuthKey] = settings.twoFactorAuth
    }
  }
}

private fun Preferences.readBoolean(key: Preferences.Key<Boolean>, defaultValue: Boolean): Boolean {
  return this[key] ?: defaultValue
}
