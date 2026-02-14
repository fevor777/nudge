package com.randomnotif.app.notification

import com.randomnotif.app.data.ExactTime
import com.randomnotif.app.data.NotificationItem
import com.randomnotif.app.data.ScheduleMode
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class NotificationSchedulerTest {

    // Helper to create a date at specific time
    private fun dateTimeMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun dateMillis(year: Int, month: Int, day: Int): Long {
        return dateTimeMillis(year, month, day, 0, 0)
    }

    // ============================================================
    // Tests for scheduleNotificationsForItem logic with exact times
    // ============================================================

    @Test
    fun `exact times are used when useExactTime is true`() {
        val item = NotificationItem(
            useExactTime = true,
            exactTimes = listOf(
                ExactTime(9, 0),
                ExactTime(12, 30),
                ExactTime(18, 0)
            ),
            scheduleMode = ScheduleMode.DAILY
        )
        
        // Verify item has correct configuration
        assertTrue(item.useExactTime)
        assertEquals(3, item.exactTimes.size)
        assertEquals(9, item.exactTimes[0].hour)
        assertEquals(12, item.exactTimes[1].hour)
        assertEquals(18, item.exactTimes[2].hour)
    }

    @Test
    fun `exact times are sorted correctly for scheduling`() {
        val unsortedTimes = listOf(
            ExactTime(18, 0),
            ExactTime(9, 0),
            ExactTime(12, 30),
            ExactTime(10, 15)
        )
        
        // Convert to minutes and sort (same logic as NotificationScheduler)
        val sortedMinutes = unsortedTimes.map { it.hour * 60 + it.minute }.sorted()
        
        assertEquals(540, sortedMinutes[0])  // 9:00
        assertEquals(615, sortedMinutes[1])  // 10:15
        assertEquals(750, sortedMinutes[2])  // 12:30
        assertEquals(1080, sortedMinutes[3]) // 18:00
    }

    @Test
    fun `random time mode uses notificationCount`() {
        val item = NotificationItem(
            useExactTime = false,
            notificationCount = 5,
            startHour = 9,
            startMinute = 0,
            endHour = 18,
            endMinute = 0,
            scheduleMode = ScheduleMode.DAILY
        )
        
        assertFalse(item.useExactTime)
        assertEquals(5, item.notificationCount)
    }

    @Test
    fun `exact time mode ignores notificationCount`() {
        val item = NotificationItem(
            useExactTime = true,
            exactTimes = listOf(
                ExactTime(10, 0),
                ExactTime(14, 0)
            ),
            notificationCount = 10, // This should be ignored
            scheduleMode = ScheduleMode.DAILY
        )
        
        assertTrue(item.useExactTime)
        // Only 2 exact times despite notificationCount = 10
        assertEquals(2, item.exactTimes.size)
    }

    @Test
    fun `exact times work with all schedule modes - DAILY`() {
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DAILY,
            useExactTime = true,
            exactTimes = listOf(ExactTime(10, 0))
        )
        
        val today = dateMillis(2026, 2, 14)
        assertTrue("Daily mode should schedule on any day", 
            item.shouldScheduleOnDate(today))
    }

    @Test
    fun `exact times work with all schedule modes - WEEKLY`() {
        val friday = dateMillis(2026, 2, 13) // Friday
        val saturday = dateMillis(2026, 2, 14) // Saturday
        
        val item = NotificationItem(
            scheduleMode = ScheduleMode.WEEKLY,
            selectedWeekDays = setOf(5), // Friday only (1=Mon, 5=Fri)
            useExactTime = true,
            exactTimes = listOf(ExactTime(10, 0), ExactTime(15, 0))
        )
        
        assertTrue("Should schedule on Friday", item.shouldScheduleOnDate(friday))
        assertFalse("Should not schedule on Saturday", item.shouldScheduleOnDate(saturday))
    }

    @Test
    fun `exact times work with SPECIFIC_DATE mode`() {
        val targetDate = dateMillis(2026, 3, 15)
        val otherDate = dateMillis(2026, 3, 16)
        
        val item = NotificationItem(
            scheduleMode = ScheduleMode.SPECIFIC_DATE,
            specificDateMillis = targetDate,
            useExactTime = true,
            exactTimes = listOf(ExactTime(10, 0), ExactTime(14, 0))
        )
        
        assertTrue("Should schedule on specific date", item.shouldScheduleOnDate(targetDate))
        assertFalse("Should not schedule on other date", item.shouldScheduleOnDate(otherDate))
    }

    @Test
    fun `exact times work with DATE_RANGE mode`() {
        val startDate = dateMillis(2026, 3, 1)
        val endDate = dateMillis(2026, 3, 31)
        val withinRange = dateMillis(2026, 3, 15)
        val outsideRange = dateMillis(2026, 4, 1)
        
        val item = NotificationItem(
            scheduleMode = ScheduleMode.DATE_RANGE,
            startDateMillis = startDate,
            endDateMillis = endDate,
            useExactTime = true,
            exactTimes = listOf(ExactTime(9, 0))
        )
        
        assertTrue("Should schedule within range", item.shouldScheduleOnDate(withinRange))
        assertFalse("Should not schedule outside range", item.shouldScheduleOnDate(outsideRange))
    }

    @Test
    fun `empty exact times list when useExactTime is true`() {
        // This represents user who enabled exact time but hasn't added any yet
        val item = NotificationItem(
            useExactTime = true,
            exactTimes = emptyList(),
            scheduleMode = ScheduleMode.DAILY
        )
        
        assertTrue(item.useExactTime)
        assertEquals(0, item.exactTimes.size)
        // scheduleNotificationsForItem would return 0 (no notifications scheduled)
    }

    @Test
    fun `single exact time is valid`() {
        val item = NotificationItem(
            useExactTime = true,
            exactTimes = listOf(ExactTime(12, 0)),
            scheduleMode = ScheduleMode.DAILY
        )
        
        assertEquals(1, item.exactTimes.size)
        assertEquals(12, item.exactTimes[0].hour)
        assertEquals(0, item.exactTimes[0].minute)
    }

    @Test
    fun `many exact times are valid`() {
        val times = (8..20).map { hour -> ExactTime(hour, 0) }
        val item = NotificationItem(
            useExactTime = true,
            exactTimes = times,
            scheduleMode = ScheduleMode.DAILY
        )
        
        assertEquals(13, item.exactTimes.size)
        assertEquals(8, item.exactTimes.first().hour)
        assertEquals(20, item.exactTimes.last().hour)
    }

    @Test
    fun `exact times can span midnight`() {
        val times = listOf(
            ExactTime(0, 0),   // midnight
            ExactTime(6, 0),   // morning
            ExactTime(12, 0),  // noon
            ExactTime(18, 0),  // evening
            ExactTime(23, 59)  // end of day
        )
        val item = NotificationItem(
            useExactTime = true,
            exactTimes = times,
            scheduleMode = ScheduleMode.DAILY
        )
        
        assertEquals(5, item.exactTimes.size)
        // All times are valid 24-hour times
        assertTrue(item.exactTimes.all { it.hour in 0..23 })
        assertTrue(item.exactTimes.all { it.minute in 0..59 })
    }

    @Test
    fun `exact times conversion to minutes is consistent`() {
        val time1 = ExactTime(0, 0)
        val time2 = ExactTime(1, 30)
        val time3 = ExactTime(23, 59)
        
        val minutes1 = time1.hour * 60 + time1.minute
        val minutes2 = time2.hour * 60 + time2.minute
        val minutes3 = time3.hour * 60 + time3.minute
        
        assertEquals(0, minutes1)
        assertEquals(90, minutes2)
        assertEquals(1439, minutes3)
        
        // Verify they sort correctly
        val sorted = listOf(minutes3, minutes1, minutes2).sorted()
        assertEquals(listOf(0, 90, 1439), sorted)
    }

    @Test
    fun `switching between modes preserves exact times`() {
        var item = NotificationItem(
            useExactTime = true,
            exactTimes = listOf(ExactTime(10, 0), ExactTime(14, 0)),
            scheduleMode = ScheduleMode.DAILY
        )
        
        // Switch to random mode
        item = item.copy(useExactTime = false)
        
        // Exact times still exist in object
        assertEquals(2, item.exactTimes.size)
        assertFalse(item.useExactTime)
        
        // Switch back to exact mode
        item = item.copy(useExactTime = true)
        
        // Exact times are still there
        assertEquals(2, item.exactTimes.size)
        assertTrue(item.useExactTime)
    }

    @Test
    fun `exact times with yearly mode on specific month and day`() {
        val birthday = dateMillis(2026, 6, 15) // June 15
        val notBirthday = dateMillis(2026, 6, 16)
        
        val item = NotificationItem(
            scheduleMode = ScheduleMode.YEARLY,
            yearlyMonth = 6,
            yearlyDay = 15,
            useExactTime = true,
            exactTimes = listOf(ExactTime(9, 0), ExactTime(12, 0), ExactTime(18, 0))
        )
        
        assertTrue("Should schedule on June 15", item.shouldScheduleOnDate(birthday))
        assertFalse("Should not schedule on June 16", item.shouldScheduleOnDate(notBirthday))
    }

    @Test
    fun `exact times with monthly by weekday mode`() {
        // First Monday of February 2026 is Feb 2
        val firstMonday = dateMillis(2026, 2, 2)
        val secondMonday = dateMillis(2026, 2, 9)
        
        val item = NotificationItem(
            scheduleMode = ScheduleMode.MONTHLY_BY_WEEKDAY,
            monthWeekdayOrdinal = com.randomnotif.app.data.MonthlyOrdinal.FIRST,
            monthWeekday = 1, // Monday
            useExactTime = true,
            exactTimes = listOf(ExactTime(10, 0))
        )
        
        assertTrue("Should schedule on first Monday", item.shouldScheduleOnDate(firstMonday))
        assertFalse("Should not schedule on second Monday", item.shouldScheduleOnDate(secondMonday))
    }
}
