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
enum class ScheduleMode {
    DAILY,              // Каждый день (только временной промежуток)
    DATE_RANGE,         // Промежуток дат (от даты до даты)
    SPECIFIC_DATE,      // Конкретная дата
    WEEKLY,             // Еженедельно (по дням недели)
    MONTHLY_BY_DATE,    // Ежемесячно (по числам месяца)
    MONTHLY_BY_WEEKDAY, // Ежемесячно (N-й день недели)
    YEARLY              // Ежегодно (конкретный месяц и день)
}

@Serializable
enum class MonthlyOrdinal {
    FIRST,   // Первый
    SECOND,  // Второй
    THIRD,   // Третий
    FOURTH,  // Четвёртый
    LAST     // Последний
}

@Serializable
enum class WorkingDayPosition {
    FIRST,  // Первый рабочий день месяца
    LAST    // Последний рабочий день месяца
}

@Serializable
data class ExactTime(
    val hour: Int,
    val minute: Int
)

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
    val notificationCount: Int = 1,
    val textsPerNotification: Int = 1,
    val scheduleMode: ScheduleMode = ScheduleMode.DAILY,
    // Интервал в днях для DAILY режима (1 = каждый день, 2 = раз в 2 дня, и т.д.)
    val intervalDays: Int = 1,
    // Дата начала отсчёта интервала (epoch millis), чтобы считать дни
    val intervalStartDateMillis: Long? = null,
    // Для DATE_RANGE: начало и конец диапазона дат (epoch millis)
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    // Для SPECIFIC_DATE: конкретная дата (epoch millis)
    val specificDateMillis: Long? = null,
    // Для WEEKLY: выбранные дни недели (1=Пн, 2=Вт, ..., 7=Вс)
    val selectedWeekDays: Set<Int> = emptySet(),
    // Для MONTHLY_BY_DATE: выбранные числа месяца (1-31)
    val selectedMonthDays: Set<Int> = emptySet(),
    // Для MONTHLY_BY_WEEKDAY: порядковый номер недели и день недели
    val monthWeekdayOrdinal: MonthlyOrdinal? = null,
    val monthWeekday: Int? = null, // 1=Пн, 2=Вт, ..., 7=Вс
    // Для YEARLY: месяц (1-12) и день (1-31)
    val yearlyMonth: Int? = null,
    val yearlyDay: Int? = null,
    // Для MONTHLY_BY_DATE: опция "рабочий день месяца"
    val workingDaysOnly: Boolean = false,
    val workingDayPosition: WorkingDayPosition? = null,
    // Режим времени: точное время или случайное в промежутке
    val useExactTime: Boolean = false,
    val exactTimes: List<ExactTime> = emptyList()
) {
    // Для обратной совместимости и удобства
    fun getRandomText(): String {
        return if (notificationTexts.isEmpty()) "Напоминание!" 
        else notificationTexts.random()
    }

    /**
     * Выбирает [count] случайных уникальных текстов из списка.
     * Если count >= размера списка, возвращает все тексты в случайном порядке.
     * Каждый текст на новой строке.
     */
    fun getRandomTexts(count: Int = textsPerNotification): String {
        if (notificationTexts.isEmpty()) return "Напоминание!"
        val effectiveCount = count.coerceIn(1, notificationTexts.size)
        val selected = notificationTexts.shuffled().take(effectiveCount)
        if (effectiveCount == 1) return selected.first()
        return selected.map { text -> "• $text" }.joinToString("\n")
    }

    /**
     * Проверяет, должно ли уведомление быть запланировано на указанную дату.
     * @param todayStartMillis начало дня (00:00:00.000) в epoch millis
     * @return true если уведомление должно сработать в этот день
     */
    fun shouldScheduleOnDate(todayStartMillis: Long): Boolean {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = todayStartMillis
        }
        
        return when (scheduleMode) {
            ScheduleMode.SPECIFIC_DATE -> {
                val specificDate = specificDateMillis ?: return false
                val specificDayStart = normalizeToStartOfDay(specificDate)
                todayStartMillis == specificDayStart
            }
            ScheduleMode.DATE_RANGE -> {
                val startDate = startDateMillis ?: return false
                val endDate = endDateMillis ?: return false
                val startDayStart = normalizeToStartOfDay(startDate)
                val endDayStart = normalizeToStartOfDay(endDate)
                todayStartMillis in startDayStart..endDayStart
            }
            ScheduleMode.DAILY -> {
                if (intervalDays > 1) {
                    val startMillis = intervalStartDateMillis ?: todayStartMillis
                    val normalizedStart = normalizeToStartOfDay(startMillis)
                    val daysSinceStart = ((todayStartMillis - normalizedStart) / (24 * 60 * 60 * 1000)).toInt()
                    daysSinceStart >= 0 && daysSinceStart % intervalDays == 0
                } else {
                    true
                }
            }
            ScheduleMode.WEEKLY -> {
                val dayOfWeek = getDayOfWeek(calendar)
                selectedWeekDays.contains(dayOfWeek)
            }
            ScheduleMode.MONTHLY_BY_DATE -> {
                if (workingDaysOnly) {
                    when (workingDayPosition) {
                        WorkingDayPosition.FIRST -> isFirstWorkingDay(calendar)
                        WorkingDayPosition.LAST -> isLastWorkingDay(calendar)
                        null -> false
                    }
                } else {
                    val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    selectedMonthDays.contains(dayOfMonth)
                }
            }
            ScheduleMode.MONTHLY_BY_WEEKDAY -> {
                val ordinal = monthWeekdayOrdinal ?: return false
                val weekday = monthWeekday ?: return false
                isNthWeekdayOfMonth(calendar, ordinal, weekday)
            }
            ScheduleMode.YEARLY -> {
                val month = yearlyMonth ?: return false
                val day = yearlyDay ?: return false
                calendar.get(java.util.Calendar.MONTH) + 1 == month &&
                calendar.get(java.util.Calendar.DAY_OF_MONTH) == day
            }
        }
    }

    companion object {
        /** Нормализует timestamp к началу дня (00:00:00.000) */
        fun normalizeToStartOfDay(millis: Long): Long {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        
        /** Преобразует Calendar.DAY_OF_WEEK в ISO формат (1=Пн, 7=Вс) */
        private fun getDayOfWeek(calendar: java.util.Calendar): Int {
            val calendarDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            return when (calendarDay) {
                java.util.Calendar.MONDAY -> 1
                java.util.Calendar.TUESDAY -> 2
                java.util.Calendar.WEDNESDAY -> 3
                java.util.Calendar.THURSDAY -> 4
                java.util.Calendar.FRIDAY -> 5
                java.util.Calendar.SATURDAY -> 6
                java.util.Calendar.SUNDAY -> 7
                else -> 1
            }
        }
        
        /** Проверяет, является ли дата N-м днём недели в месяце */
        private fun isNthWeekdayOfMonth(calendar: java.util.Calendar, ordinal: MonthlyOrdinal, weekday: Int): Boolean {
            val currentDayOfWeek = getDayOfWeek(calendar)
            if (currentDayOfWeek != weekday) return false
            
            if (ordinal == MonthlyOrdinal.LAST) {
                // Проверяем, что это последнее появление этого дня недели в месяце
                val testCal = calendar.clone() as java.util.Calendar
                testCal.add(java.util.Calendar.DAY_OF_MONTH, 7)
                return testCal.get(java.util.Calendar.MONTH) != calendar.get(java.util.Calendar.MONTH)
            } else {
                // Считаем, какой это по счёту день недели в месяце
                val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                val occurrence = (dayOfMonth - 1) / 7 + 1
                val targetOccurrence = when (ordinal) {
                    MonthlyOrdinal.FIRST -> 1
                    MonthlyOrdinal.SECOND -> 2
                    MonthlyOrdinal.THIRD -> 3
                    MonthlyOrdinal.FOURTH -> 4
                    else -> return false
                }
                return occurrence == targetOccurrence
            }
        }
        
        /** Проверяет, является ли день рабочим (Пн-Пт) */
        private fun isWorkingDay(calendar: java.util.Calendar): Boolean {
            val dayOfWeek = getDayOfWeek(calendar)
            return dayOfWeek in 1..5
        }
        
        /** Проверяет, является ли дата первым рабочим днём месяца */
        private fun isFirstWorkingDay(calendar: java.util.Calendar): Boolean {
            if (!isWorkingDay(calendar)) return false
            
            val testCal = calendar.clone() as java.util.Calendar
            testCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            
            while (testCal.get(java.util.Calendar.MONTH) == calendar.get(java.util.Calendar.MONTH)) {
                if (isWorkingDay(testCal)) {
                    return testCal.get(java.util.Calendar.DAY_OF_MONTH) == calendar.get(java.util.Calendar.DAY_OF_MONTH)
                }
                testCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            return false
        }
        
        /** Проверяет, является ли дата последним рабочим днём месяца */
        private fun isLastWorkingDay(calendar: java.util.Calendar): Boolean {
            if (!isWorkingDay(calendar)) return false
            
            val testCal = calendar.clone() as java.util.Calendar
            testCal.set(java.util.Calendar.DAY_OF_MONTH, testCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            
            while (testCal.get(java.util.Calendar.MONTH) == calendar.get(java.util.Calendar.MONTH)) {
                if (isWorkingDay(testCal)) {
                    return testCal.get(java.util.Calendar.DAY_OF_MONTH) == calendar.get(java.util.Calendar.DAY_OF_MONTH)
                }
                testCal.add(java.util.Calendar.DAY_OF_MONTH, -1)
            }
            return false
        }
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
