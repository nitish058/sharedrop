package org.nitish.project.sharedrop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class Settings(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val localNameKey = stringPreferencesKey("local_name")
    }

    val settingsFlow = dataStore.data.map { preferences ->
        AppSettings(localName = preferences[localNameKey])
    }.onEach { settings ->
        println("Settings updated: $settings")
        if (settings.localName == null) {
            val newName = NameGenerator.generate()
            updateLocalName(newName)
        }
    }

    suspend fun updateLocalName(name: String) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[localNameKey] = name
            }
        }
    }
}

data class AppSettings(
    val localName: String? = null
)