package com.athar.accessibilitymapping.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SAVED_LOCATIONS_STORE = "saved_locations"
private val Context.savedLocationsDataStore by preferencesDataStore(name = SAVED_LOCATIONS_STORE)

class SavedLocationsStore(private val context: Context) {
  private val savedIdsKey = stringSetPreferencesKey("saved_ids")

  val savedIds: Flow<Set<String>> = context.savedLocationsDataStore.data.map { prefs ->
    prefs[savedIdsKey] ?: emptySet()
  }

  suspend fun setSaved(id: String, saved: Boolean) {
    context.savedLocationsDataStore.edit { prefs ->
      val current = prefs[savedIdsKey] ?: emptySet()
      prefs[savedIdsKey] = if (saved) current + id else current - id
    }
  }

  suspend fun toggleSaved(id: String) {
    context.savedLocationsDataStore.edit { prefs ->
      val current = prefs[savedIdsKey] ?: emptySet()
      prefs[savedIdsKey] = if (current.contains(id)) current - id else current + id
    }
  }
}
