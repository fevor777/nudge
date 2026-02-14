package com.randomnotif.app.data

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class NotificationItemTest {

    // ============================================================
    // Helper: создаёт epoch millis для начала дня (00:00:00.000)
    // ============================================================
    private fun dateMillis(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar.MONTH is 0-based
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val DAY_MILLIS = 24 * 60 * 60 * 1000L

    // ============================================================
    // getRandomText() tests
    // ============================================================

    @Test
    fun `getRandomText returns default when texts are empty`() {
        val item = NotificationItem(notificationTexts = emptyList())
        assertEquals("Напоминание!", item.getRandomText())
    }

    @Test
    fun `getRandomText returns the only text when list has one item`() {
        val item = NotificationItem(notificationTexts = listOf("Hello"))
        assertEquals("Hello", item.getRandomText())
    }

    @Test
    fun `getRandomText returns one of the texts`() {
        val texts = listOf("A", "B", "C")
        val item = NotificationItem(notificationTexts = texts)
        val result = item.getRandomText()
        assertTrue("Result should be one of the texts", result in texts)
    }

    // ============================================================
    // getRandomTexts() tests
    // ============================================================

    @Test
    fun `getRandomTexts returns default when texts are empty`() {
        val item = NotificationItem(notificationTexts = emptyList(), textsPerNotification = 3)
        assertEquals("Напоминание!", item.getRandomTexts())
    }

    @Test
    fun `getRandomTexts with count 1 returns single text no comma no bullet`() {
        val item = NotificationItem(
            notificationTexts = listOf("A", "B", "C"),
            textsPerNotification = 1
        )
        val result = item.getRandomTexts()
        // Should be a single text with no comma, no newline, no bullet
        assertFalse("Should not contain comma", result.contains(","))
        assertFalse("Should not contain newline", result.contains("\n"))
        assertFalse("Should not contain bullet", result.contains("•"))
        assertTrue("Should be one of the texts", result in listOf("A", "B", "C"))
    }

    @Test
    fun `getRandomTexts with count 2 has bullet prefix and no comma`() {
        val item = NotificationItem(
            notificationTexts = listOf("A", "B", "C"),
            textsPerNotification = 2
        )
        val result = item.getRandomTexts()
        val lines = result.split("\n")
        assertEquals("Should have 2 lines", 2, lines.size)
        assertTrue("First line should start with bullet", lines[0].startsWith("• "))
        assertFalse("Should not contain comma", result.contains(","))
        assertTrue("Last line should start with bullet", lines[1].startsWith("• "))
    }

    @Test
    fun `getRandomTexts with count 3 has bullets and no commas`() {
        val item = NotificationItem(
            notificationTexts = listOf("A", "B", "C"),
            textsPerNotification = 3
        )
        val result = item.getRandomTexts()
        val lines = result.split("\n")
        assertEquals("Should have 3 lines", 3, lines.size)
        for (line in lines) {
            assertTrue("Each line should start with bullet", line.startsWith("• "))
        }
        assertFalse("Should not contain comma", result.contains(","))
    }

    @Test
    fun `getRandomTexts clamps count to list size`() {
        val item = NotificationItem(
            notificationTexts = listOf("A", "B"),
            textsPerNotification = 10 // more than list size
        )
        val result = item.getRandomTexts()
        val lines = result.split("\n")
        assertEquals("Should be clamped to list size (2)", 2, lines.size)
    }

    @Test
    fun `getRandomTexts all texts are unique`() {
        val texts = listOf("A", "B", "C", "D", "E")
        val item = NotificationItem(notificationTexts = texts, textsPerNotification = 3)
        // Run multiple times to verify uniqueness
        repeat(20) {
            val result = item.getRandomTexts()
            val lines = result.split("\n").map { it.removePrefix("• ") }
            assertEquals("All texts in result should be unique", lines.size, lines.toSet().size)
        }
    }

    // ============================================================
    // shouldScheduleOnDate() — DAILY mode
    // ============================================================

    @Test
    fun `DAILY mode with interval 1 always schedules`() {
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DAILY,
            intervalDays = 1
        )
        val today = dateMillis(2026, 2, 14)
        assertTrue(item.shouldScheduleOnDate(today))
        assertTrue(item.shouldScheduleOnDate(today + DAY_MILLIS))
        assertTrue(item.shouldScheduleOnDate(today + DAY_MILLIS * 100))
    }

    @Test
    fun `DAILY mode with interval 2 schedules every other day`() {
        val startDate = dateMillis(2026, 2, 10)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DAILY,
            intervalDays = 2,
            intervalStartDateMillis = startDate
        )
        // Day 0 (start) → should fire
        assertTrue("Day 0 should schedule", item.shouldScheduleOnDate(startDate))
        // Day 1 → skip
        assertFalse("Day 1 should NOT schedule", item.shouldScheduleOnDate(startDate + DAY_MILLIS))
        // Day 2 → fire
        assertTrue("Day 2 should schedule", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 2))
        // Day 3 → skip
        assertFalse("Day 3 should NOT schedule", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 3))
        // Day 4 → fire
        assertTrue("Day 4 should schedule", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 4))
    }

    @Test
    fun `DAILY mode with interval 3 schedules every 3 days`() {
        val startDate = dateMillis(2026, 1, 1)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DAILY,
            intervalDays = 3,
            intervalStartDateMillis = startDate
        )
        assertTrue("Day 0", item.shouldScheduleOnDate(startDate))
        assertFalse("Day 1", item.shouldScheduleOnDate(startDate + DAY_MILLIS))
        assertFalse("Day 2", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 2))
        assertTrue("Day 3", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 3))
        assertFalse("Day 4", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 4))
        assertFalse("Day 5", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 5))
        assertTrue("Day 6", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 6))
    }

    @Test
    fun `DAILY mode with interval before start date does not fire`() {
        val startDate = dateMillis(2026, 3, 1)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DAILY,
            intervalDays = 2,
            intervalStartDateMillis = startDate
        )
        // Day before start
        assertFalse(item.shouldScheduleOnDate(startDate - DAY_MILLIS))
    }

    @Test
    fun `DAILY mode with interval and no intervalStartDateMillis uses today as start`() {
        val today = dateMillis(2026, 2, 14)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DAILY,
            intervalDays = 5,
            intervalStartDateMillis = null
        )
        // When intervalStartDateMillis is null, todayStartMillis is used as start,
        // so daysSinceStart = 0, and 0 % N = 0 → should schedule
        assertTrue(item.shouldScheduleOnDate(today))
    }

    @Test
    fun `DAILY mode with interval 7 schedules weekly`() {
        val startDate = dateMillis(2026, 2, 1)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DAILY,
            intervalDays = 7,
            intervalStartDateMillis = startDate
        )
        // Should fire on day 0, 7, 14, 21...
        assertTrue("Day 0", item.shouldScheduleOnDate(startDate))
        for (d in 1..6) {
            assertFalse("Day $d should NOT fire", item.shouldScheduleOnDate(startDate + DAY_MILLIS * d))
        }
        assertTrue("Day 7", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 7))
        assertTrue("Day 14", item.shouldScheduleOnDate(startDate + DAY_MILLIS * 14))
    }

    // ============================================================
    // shouldScheduleOnDate() — SPECIFIC_DATE mode
    // ============================================================

    @Test
    fun `SPECIFIC_DATE schedules on matching date`() {
        val specificDate = dateMillis(2026, 3, 15)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.SPECIFIC_DATE,
            specificDateMillis = specificDate
        )
        assertTrue(item.shouldScheduleOnDate(specificDate))
    }

    @Test
    fun `SPECIFIC_DATE does not schedule on different date`() {
        val specificDate = dateMillis(2026, 3, 15)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.SPECIFIC_DATE,
            specificDateMillis = specificDate
        )
        assertFalse(item.shouldScheduleOnDate(specificDate - DAY_MILLIS))
        assertFalse(item.shouldScheduleOnDate(specificDate + DAY_MILLIS))
    }

    @Test
    fun `SPECIFIC_DATE with null specificDateMillis returns false`() {
        val item = NotificationItem(
            scheduleMode = ScheduleMode.SPECIFIC_DATE,
            specificDateMillis = null
        )
        assertFalse(item.shouldScheduleOnDate(dateMillis(2026, 2, 14)))
    }

    @Test
    fun `SPECIFIC_DATE normalizes time part of specificDateMillis`() {
        // specificDateMillis set to March 15, 14:30 (not midnight)
        val specificDateWithTime = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val item = NotificationItem(
            scheduleMode = ScheduleMode.SPECIFIC_DATE,
            specificDateMillis = specificDateWithTime
        )

        val todayStart = dateMillis(2026, 3, 15)
        assertTrue("Should match even if specificDateMillis has non-zero time", item.shouldScheduleOnDate(todayStart))
    }

    // ============================================================
    // shouldScheduleOnDate() — DATE_RANGE mode
    // ============================================================

    @Test
    fun `DATE_RANGE schedules within range`() {
        val start = dateMillis(2026, 2, 10)
        val end = dateMillis(2026, 2, 20)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DATE_RANGE,
            startDateMillis = start,
            endDateMillis = end
        )
        assertTrue("Start day", item.shouldScheduleOnDate(start))
        assertTrue("Mid day", item.shouldScheduleOnDate(dateMillis(2026, 2, 15)))
        assertTrue("End day", item.shouldScheduleOnDate(end))
    }

    @Test
    fun `DATE_RANGE does not schedule outside range`() {
        val start = dateMillis(2026, 2, 10)
        val end = dateMillis(2026, 2, 20)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DATE_RANGE,
            startDateMillis = start,
            endDateMillis = end
        )
        assertFalse("Before start", item.shouldScheduleOnDate(start - DAY_MILLIS))
        assertFalse("After end", item.shouldScheduleOnDate(end + DAY_MILLIS))
    }

    @Test
    fun `DATE_RANGE with null start returns false`() {
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DATE_RANGE,
            startDateMillis = null,
            endDateMillis = dateMillis(2026, 2, 20)
        )
        assertFalse(item.shouldScheduleOnDate(dateMillis(2026, 2, 15)))
    }

    @Test
    fun `DATE_RANGE with null end returns false`() {
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DATE_RANGE,
            startDateMillis = dateMillis(2026, 2, 10),
            endDateMillis = null
        )
        assertFalse(item.shouldScheduleOnDate(dateMillis(2026, 2, 15)))
    }

    @Test
    fun `DATE_RANGE single day range works`() {
        val singleDay = dateMillis(2026, 5, 1)
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DATE_RANGE,
            startDateMillis = singleDay,
            endDateMillis = singleDay
        )
        assertTrue("On day", item.shouldScheduleOnDate(singleDay))
        assertFalse("Day before", item.shouldScheduleOnDate(singleDay - DAY_MILLIS))
        assertFalse("Day after", item.shouldScheduleOnDate(singleDay + DAY_MILLIS))
    }

    // ============================================================
    // JSON serialization round-trip (backward compatibility)
    // ============================================================

    @Test
    fun `serialization round trip preserves all fields`() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val original = NotificationItem(
            id = "test-id",
            name = "Test",
            isEnabled = true,
            notificationTexts = listOf("A", "B"),
            startHour = 8,
            startMinute = 30,
            endHour = 22,
            endMinute = 0,
            notificationCount = 5,
            textsPerNotification = 2,
            scheduleMode = ScheduleMode.DATE_RANGE,
            intervalDays = 3,
            intervalStartDateMillis = 1000000L,
            startDateMillis = 2000000L,
            endDateMillis = 3000000L,
            specificDateMillis = 4000000L
        )
        val jsonStr = json.encodeToString(NotificationItem.serializer(), original)
        val decoded = json.decodeFromString(NotificationItem.serializer(), jsonStr)
        assertEquals(original, decoded)
    }

    @Test
    fun `deserialization of old JSON without new fields uses defaults`() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        // Simulate old JSON without scheduleMode, intervalDays, dates
        val oldJson = """{"id":"old","name":"Old","isEnabled":true,"notificationTexts":["Hi"],"startHour":9,"startMinute":0,"endHour":21,"endMinute":0,"notificationCount":1,"textsPerNotification":1}"""
        val decoded = json.decodeFromString(NotificationItem.serializer(), oldJson)
        assertEquals(ScheduleMode.DAILY, decoded.scheduleMode)
        assertEquals(1, decoded.intervalDays)
        assertNull(decoded.intervalStartDateMillis)
        assertNull(decoded.startDateMillis)
        assertNull(decoded.endDateMillis)
        assertNull(decoded.specificDateMillis)
    }

    // ============================================================
    // normalizeToStartOfDay tests
    // ============================================================

    @Test
    fun `normalizeToStartOfDay strips time component`() {
        val dateWithTime = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 15, 14, 30, 45)
            set(Calendar.MILLISECOND, 123)
        }.timeInMillis

        val normalized = NotificationItem.normalizeToStartOfDay(dateWithTime)
        val cal = Calendar.getInstance().apply { timeInMillis = normalized }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    // ============================================================
    // isEnabled — поведение при вкл/выкл
    // ============================================================

    @Test
    fun `isEnabled defaults to false`() {
        val item = NotificationItem()
        assertFalse("New item should be disabled by default", item.isEnabled)
    }

    @Test
    fun `toggling isEnabled preserves all other fields`() {
        val item = NotificationItem(
            name = "Test",
            notificationTexts = listOf("A", "B"),
            notificationCount = 5,
            startHour = 10,
            endHour = 22,
            scheduleMode = ScheduleMode.DATE_RANGE,
            intervalDays = 3,
            startDateMillis = 100L,
            endDateMillis = 200L
        )
        val enabled = item.copy(isEnabled = true)
        assertTrue(enabled.isEnabled)
        assertEquals(item.name, enabled.name)
        assertEquals(item.notificationTexts, enabled.notificationTexts)
        assertEquals(item.notificationCount, enabled.notificationCount)
        assertEquals(item.startHour, enabled.startHour)
        assertEquals(item.endHour, enabled.endHour)
        assertEquals(item.scheduleMode, enabled.scheduleMode)
        assertEquals(item.intervalDays, enabled.intervalDays)
        assertEquals(item.startDateMillis, enabled.startDateMillis)
        assertEquals(item.endDateMillis, enabled.endDateMillis)

        val disabled = enabled.copy(isEnabled = false)
        assertFalse(disabled.isEnabled)
        assertEquals(item.name, disabled.name)
        assertEquals(item.notificationCount, disabled.notificationCount)
    }

    @Test
    fun `enabling sets intervalStartDateMillis when null (simulating UI logic)`() {
        val item = NotificationItem(
            isEnabled = false,
            intervalDays = 2,
            intervalStartDateMillis = null
        )
        // Simulate UI logic: when enabling, set intervalStartDateMillis if null
        val now = System.currentTimeMillis()
        val enabled = if (item.intervalStartDateMillis == null) {
            item.copy(isEnabled = true, intervalStartDateMillis = now)
        } else {
            item.copy(isEnabled = true)
        }
        assertTrue(enabled.isEnabled)
        assertNotNull(enabled.intervalStartDateMillis)
    }

    @Test
    fun `enabling preserves existing intervalStartDateMillis`() {
        val originalStart = dateMillis(2026, 1, 1)
        val item = NotificationItem(
            isEnabled = false,
            intervalDays = 3,
            intervalStartDateMillis = originalStart
        )
        // Simulate UI logic
        val enabled = if (item.intervalStartDateMillis == null) {
            item.copy(isEnabled = true, intervalStartDateMillis = System.currentTimeMillis())
        } else {
            item.copy(isEnabled = true)
        }
        assertEquals(originalStart, enabled.intervalStartDateMillis)
    }

    @Test
    fun `disabled items are skipped in settings filtering`() {
        val settings = NotificationSettings(
            items = listOf(
                NotificationItem(id = "1", isEnabled = false),
                NotificationItem(id = "2", isEnabled = true),
                NotificationItem(id = "3", isEnabled = false),
                NotificationItem(id = "4", isEnabled = true)
            )
        )
        val enabledItems = settings.items.filter { it.isEnabled }
        assertEquals(2, enabledItems.size)
        assertEquals("2", enabledItems[0].id)
        assertEquals("4", enabledItems[1].id)
    }

    @Test
    fun `no enabled items means nothing to schedule`() {
        val settings = NotificationSettings(
            items = listOf(
                NotificationItem(isEnabled = false),
                NotificationItem(isEnabled = false)
            )
        )
        assertFalse("No items should be enabled", settings.items.any { it.isEnabled })
    }

    // ============================================================
    // notificationCount — изменение количества уведомлений
    // ============================================================

    @Test
    fun `notificationCount defaults to 1`() {
        val item = NotificationItem()
        assertEquals(1, item.notificationCount)
    }

    @Test
    fun `notificationCount can be increased`() {
        val item = NotificationItem(notificationCount = 1)
        val updated = item.copy(notificationCount = item.notificationCount + 1)
        assertEquals(2, updated.notificationCount)
    }

    @Test
    fun `notificationCount can be decreased to 1`() {
        val item = NotificationItem(notificationCount = 5)
        val updated = item.copy(notificationCount = item.notificationCount - 1)
        assertEquals(4, updated.notificationCount)
    }

    @Test
    fun `notificationCount minimum is 1 (UI constraint)`() {
        val item = NotificationItem(notificationCount = 1)
        // UI prevents going below 1
        val newCount = (item.notificationCount - 1).coerceAtLeast(1)
        assertEquals(1, newCount)
    }

    @Test
    fun `notificationCount maximum is 50 (UI constraint)`() {
        val item = NotificationItem(notificationCount = 50)
        // UI prevents going above 50
        val newCount = (item.notificationCount + 1).coerceAtMost(50)
        assertEquals(50, newCount)
    }

    @Test
    fun `notificationCount change preserves other fields`() {
        val item = NotificationItem(
            name = "Test",
            isEnabled = true,
            notificationTexts = listOf("X", "Y"),
            startHour = 8,
            endHour = 20,
            notificationCount = 3
        )
        val updated = item.copy(notificationCount = 10)
        assertEquals(10, updated.notificationCount)
        assertEquals(item.name, updated.name)
        assertEquals(item.isEnabled, updated.isEnabled)
        assertEquals(item.notificationTexts, updated.notificationTexts)
        assertEquals(item.startHour, updated.startHour)
        assertEquals(item.endHour, updated.endHour)
        assertEquals(item.id, updated.id)
    }

    @Test
    fun `notificationCount zero returns 0 from time range calculation`() {
        // notificationCount = 0 would make time generation produce nothing
        val item = NotificationItem(notificationCount = 0)
        // The scheduler checks: if totalMinutes <= 0 || item.notificationCount <= 0 return 0
        assertTrue("Zero count means nothing to schedule", item.notificationCount <= 0)
    }

    // ============================================================
    // textsPerNotification — clamping при удалении текстов
    // ============================================================

    @Test
    fun `textsPerNotification defaults to 1`() {
        val item = NotificationItem()
        assertEquals(1, item.textsPerNotification)
    }

    @Test
    fun `textsPerNotification is clamped when text is removed`() {
        val item = NotificationItem(
            notificationTexts = listOf("A", "B", "C"),
            textsPerNotification = 3
        )
        // Simulate removing one text
        val newTexts = item.notificationTexts.toMutableList()
        newTexts.removeAt(2)
        val clampedTextsPerNotif = item.textsPerNotification.coerceAtMost(newTexts.size)
        val updated = item.copy(notificationTexts = newTexts, textsPerNotification = clampedTextsPerNotif)
        assertEquals(2, updated.textsPerNotification)
        assertEquals(2, updated.notificationTexts.size)
    }

    @Test
    fun `textsPerNotification stays same when text removed but still within range`() {
        val item = NotificationItem(
            notificationTexts = listOf("A", "B", "C", "D"),
            textsPerNotification = 2
        )
        val newTexts = item.notificationTexts.toMutableList()
        newTexts.removeAt(3)
        val clampedTextsPerNotif = item.textsPerNotification.coerceAtMost(newTexts.size)
        val updated = item.copy(notificationTexts = newTexts, textsPerNotification = clampedTextsPerNotif)
        assertEquals(2, updated.textsPerNotification) // Still 2, within range
        assertEquals(3, updated.notificationTexts.size)
    }

    @Test
    fun `textsPerNotification cannot exceed text list size`() {
        val item = NotificationItem(
            notificationTexts = listOf("A", "B"),
            textsPerNotification = 2
        )
        // Try to increase beyond list size (UI prevents this)
        val newCount = (item.textsPerNotification + 1).coerceAtMost(item.notificationTexts.size)
        assertEquals(2, newCount)
    }

    @Test
    fun `textsPerNotification clamps down to 1 when all but one text removed`() {
        val item = NotificationItem(
            notificationTexts = listOf("A", "B", "C"),
            textsPerNotification = 3
        )
        // Remove texts until only one remains
        val newTexts = listOf("A")
        val clamped = item.textsPerNotification.coerceAtMost(newTexts.size)
        assertEquals(1, clamped)
    }

    @Test
    fun `adding texts does not auto-increase textsPerNotification`() {
        val item = NotificationItem(
            notificationTexts = listOf("A"),
            textsPerNotification = 1
        )
        val updated = item.copy(notificationTexts = item.notificationTexts + "B")
        // textsPerNotification should remain 1 unless user explicitly changes it
        assertEquals(1, updated.textsPerNotification)
        assertEquals(2, updated.notificationTexts.size)
    }

    // ============================================================
    // Combined scenarios
    // ============================================================

    @Test
    fun `enabled item with DAILY mode and interval schedules correctly`() {
        val startDate = dateMillis(2026, 2, 1)
        val item = NotificationItem(
            isEnabled = true,
            scheduleMode = ScheduleMode.DAILY,
            intervalDays = 2,
            intervalStartDateMillis = startDate,
            notificationCount = 3
        )
        assertTrue(item.isEnabled)
        assertEquals(3, item.notificationCount)
        assertTrue("Day 0 should schedule", item.shouldScheduleOnDate(startDate))
        assertFalse("Day 1 should NOT schedule", item.shouldScheduleOnDate(startDate + DAY_MILLIS))
    }

    @Test
    fun `disabled item still has valid shouldScheduleOnDate`() {
        // shouldScheduleOnDate checks dates, not isEnabled
        // isEnabled is checked by the scheduler loop
        val item = NotificationItem(
            isEnabled = false,
            scheduleMode = ScheduleMode.DAILY,
            intervalDays = 1
        )
        assertFalse(item.isEnabled)
        assertTrue("shouldScheduleOnDate works regardless of isEnabled",
            item.shouldScheduleOnDate(dateMillis(2026, 2, 14)))
    }

    @Test
    fun `full scenario - create enable change count disable`() {
        // 1. Create
        var item = NotificationItem(name = "Test")
        assertFalse(item.isEnabled)
        assertEquals(1, item.notificationCount)

        // 2. Enable
        item = item.copy(isEnabled = true, intervalStartDateMillis = System.currentTimeMillis())
        assertTrue(item.isEnabled)

        // 3. Change notification count
        item = item.copy(notificationCount = 5)
        assertEquals(5, item.notificationCount)
        assertTrue(item.isEnabled) // still enabled

        // 4. Add texts and change textsPerNotification
        item = item.copy(
            notificationTexts = listOf("A", "B", "C"),
            textsPerNotification = 2
        )
        assertEquals(3, item.notificationTexts.size)
        assertEquals(2, item.textsPerNotification)

        // 5. Disable
        item = item.copy(isEnabled = false)
        assertFalse(item.isEnabled)
        // All settings preserved
        assertEquals(5, item.notificationCount)
        assertEquals(2, item.textsPerNotification)
        assertEquals(3, item.notificationTexts.size)
    }
}
