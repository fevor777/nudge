package com.randomnotif.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Serializable
data class NotificationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Уведомление",
    val isEnabled: Boolean = false,
    val notificationTexts: List<String> = listOf("Напоминание!"),
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 21,
    val endMinute: Int = 0,
    val notificationCount: Int = 1
) {
    // Для обратной совместимости и удобства
    fun getRandomText(): String {
        return if (notificationTexts.isEmpty()) "Напоминание!" 
        else notificationTexts.random()
    }
}

@Serializable
data class NotificationSettings(
    val items: List<NotificationItem> = listOf(NotificationItem())
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val NOTIFICATIONS_JSON = stringPreferencesKey("notifications_json")
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    val settingsFlow: Flow<NotificationSettings> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[PreferencesKeys.NOTIFICATIONS_JSON]
            if (jsonString != null) {
                try {
                    json.decodeFromString<NotificationSettings>(jsonString)
                } catch (e: Exception) {
                    NotificationSettings()
                }
            } else {
                NotificationSettings()
            }
        }

    suspend fun updateSettings(settings: NotificationSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_JSON] = json.encodeToString(settings)
        }
    }

    suspend fun addNotification(): NotificationSettings {
        var updatedSettings: NotificationSettings? = null
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.NOTIFICATIONS_JSON]?.let {
                try { json.decodeFromString<NotificationSettings>(it) } catch (e: Exception) { null }
            } ?: NotificationSettings(items = emptyList())
            
            val newItem = NotificationItem(name = "Уведомление ${current.items.size + 1}")
            updatedSettings = current.copy(items = current.items + newItem)
            preferences[PreferencesKeys.NOTIFICATIONS_JSON] = json.encodeToString(updatedSettings)
        }
        return updatedSettings ?: NotificationSettings()
    }

    suspend fun deleteNotification(id: String): NotificationSettings {
        var updatedSettings: NotificationSettings? = null
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.NOTIFICATIONS_JSON]?.let {
                try { json.decodeFromString<NotificationSettings>(it) } catch (e: Exception) { null }
            } ?: NotificationSettings()
            
            updatedSettings = current.copy(items = current.items.filter { it.id != id })
            preferences[PreferencesKeys.NOTIFICATIONS_JSON] = json.encodeToString(updatedSettings)
        }
        return updatedSettings ?: NotificationSettings()
    }

    suspend fun updateNotification(item: NotificationItem): NotificationSettings {
        var updatedSettings: NotificationSettings? = null
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.NOTIFICATIONS_JSON]?.let {
                try { json.decodeFromString<NotificationSettings>(it) } catch (e: Exception) { null }
            } ?: NotificationSettings()
            
            updatedSettings = current.copy(
                items = current.items.map { if (it.id == item.id) item else it }
            )
            preferences[PreferencesKeys.NOTIFICATIONS_JSON] = json.encodeToString(updatedSettings)
        }
        return updatedSettings ?: NotificationSettings()
    }

    fun exportToJson(): String {
        val prefs = context.getSharedPreferences("settings_export_temp", Context.MODE_PRIVATE)
        // We need to read current data synchronously for export
        // Using a blocking approach for simplicity
        return try {
            kotlinx.coroutines.runBlocking {
                context.dataStore.data.first()[PreferencesKeys.NOTIFICATIONS_JSON] ?: json.encodeToString(NotificationSettings())
            }
        } catch (e: Exception) {
            json.encodeToString(NotificationSettings())
        }
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val settings = json.decodeFromString<NotificationSettings>(jsonString)
            kotlinx.coroutines.runBlocking {
                updateSettings(settings)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importFromJsonAsync(jsonString: String): NotificationSettings? {
        return try {
            val settings = json.decodeFromString<NotificationSettings>(jsonString)
            updateSettings(settings)
            settings
        } catch (e: Exception) {
            null
        }
    }
}
