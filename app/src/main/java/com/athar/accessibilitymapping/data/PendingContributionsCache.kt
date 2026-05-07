package com.athar.accessibilitymapping.data

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

object PendingContributionsCache {
  private val store = ConcurrentHashMap<String, ApiAccessibilityContribution>()

  fun put(locationId: String, contribution: ApiAccessibilityContribution) {
    Log.d("PendingContribCache", "PUT locationId=$locationId contribution=$contribution")
    val existing = store[locationId]
    val merged = if (existing == null) {
      contribution.copy(locationId = locationId)
    } else {
      existing.copy(
        wheelchairAccessible = existing.wheelchairAccessible || contribution.wheelchairAccessible,
        rampAvailable = existing.rampAvailable || contribution.rampAvailable,
        elevatorAvailable = existing.elevatorAvailable || contribution.elevatorAvailable,
        parking = existing.parking || contribution.parking,
        accessibleToilet = existing.accessibleToilet || contribution.accessibleToilet,
        wideEntrance = existing.wideEntrance || contribution.wideEntrance,
        status = contribution.status ?: existing.status,
        pendingVerification = contribution.pendingVerification,
        id = contribution.id ?: existing.id
      )
    }
    store[locationId] = merged
  }

  fun get(locationId: String): ApiAccessibilityContribution? {
    val hit = store[locationId]
    Log.d("PendingContribCache", "GET locationId=$locationId hit=${hit != null} keys=${store.keys}")
    return hit
  }

  fun clear(locationId: String) {
    store.remove(locationId)
  }
}
