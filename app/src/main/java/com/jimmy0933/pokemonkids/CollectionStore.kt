package com.jimmy0933.pokemonkids

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "collection")
private val COLLECTED_IDS = stringSetPreferencesKey("collected_ids")

/** Persists the collected Pokemon ids on the device, so the collection
 * book survives closing and reopening the app. No account, no network. */
class CollectionStore(private val context: Context) {
    val collectedIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        (prefs[COLLECTED_IDS] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
    }

    suspend fun addCollected(ids: Collection<Int>) {
        context.dataStore.edit { prefs ->
            val current = prefs[COLLECTED_IDS] ?: emptySet()
            prefs[COLLECTED_IDS] = current + ids.map { it.toString() }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs[COLLECTED_IDS] = emptySet() }
    }
}
