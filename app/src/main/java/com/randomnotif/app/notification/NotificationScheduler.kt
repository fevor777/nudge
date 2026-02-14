package com.randomnotif.app.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.randomnotif.app.R
import com.randomnotif.app.data.NotificationItem
import com.randomnotif.app.data.NotificationSettings
import java.util.*
import kotlin.random.Random

class NotificationScheduler(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "random_notification_channel"
        const val CHANNEL_NAME = "Nudge Notifications"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW_NOTIFICATION = "com.randomnotif.app.SHOW_NOTIFICATION"
        const val EXTRA_NOTIFICATION_TEXT = "notification_text"
        const val EXTRA_NOTIFICATION_NAME = "notification_name"
        const val EXTRA_NOTIFICATION_INDEX = "notification_index"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for Nudge notifications"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleAllNotifications(settings: NotificationSettings) {
        cancelAllNotifications()

        var globalIndex = 0
        for ((itemIndex, item) in settings.items.withIndex()) {
            if (!item.isEnabled) continue
            
            val scheduledCount = scheduleNotificationsForItem(item, itemIndex, globalIndex)
            globalIndex += scheduledCount
        }

        // Schedule daily reschedule at midnight
        if (settings.items.any { it.isEnabled }) {
            scheduleDailyReschedule()
        }
    }

    private fun scheduleNotificationsForItem(item: NotificationItem, itemIndex: Int, startIndex: Int): Int {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        // Check date mode constraints
        val todayStartMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (!item.shouldScheduleOnDate(todayStartMillis)) return 0

        // Use date + item id as seed for consistent but unique random times per day per item
        val seed = (year * 1000L + today) + item.id.hashCode()
        val random = Random(seed)

        val startTimeMinutes = item.startHour * 60 + item.startMinute
        val endTimeMinutes = item.endHour * 60 + item.endMinute
        
        val totalMinutes = if (endTimeMinutes > startTimeMinutes) {
            endTimeMinutes - startTimeMinutes
        } else {
            (24 * 60 - startTimeMinutes) + endTimeMinutes
        }

        if (totalMinutes <= 0 || item.notificationCount <= 0) return 0

        // Минимальный интервал между уведомлениями (15 минут)
        val minIntervalMinutes = 15
        
        // Generate random times for notifications with minimum interval
        val randomTimes = mutableListOf<Int>()
        var attempts = 0
        val maxAttempts = 100
        
        while (randomTimes.size < item.notificationCount && attempts < maxAttempts) {
            val randomOffset = random.nextInt(totalMinutes)
            val notificationMinutes = (startTimeMinutes + randomOffset) % (24 * 60)
            
            // Проверяем, что новое время достаточно далеко от уже добавленных
            val isFarEnough = randomTimes.all { existingTime ->
                val diff = kotlin.math.abs(notificationMinutes - existingTime)
                val minDiff = kotlin.math.min(diff, 24 * 60 - diff) // учитываем переход через полночь
                minDiff >= minIntervalMinutes
            }
            
            if (isFarEnough) {
                randomTimes.add(notificationMinutes)
            }
            attempts++
        }
        
        // Если не удалось найти достаточно уникальных времён, равномерно распределяем
        if (randomTimes.size < item.notificationCount) {
            randomTimes.clear()
            val interval = totalMinutes / item.notificationCount
            for (i in 0 until item.notificationCount) {
                val offset = interval * i + random.nextInt(interval.coerceAtLeast(1))
                val notificationMinutes = (startTimeMinutes + offset) % (24 * 60)
                randomTimes.add(notificationMinutes)
            }
        }
        
        randomTimes.sort()

        val currentTimeMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        for ((index, timeMinutes) in randomTimes.withIndex()) {
            val notificationCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timeMinutes / 60)
                set(Calendar.MINUTE, timeMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // If time has passed, schedule for tomorrow
            if (timeMinutes <= currentTimeMinutes) {
                notificationCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val globalIndex = startIndex + index
            // Выбираем случайные тексты из списка (textsPerNotification штук)
            val randomText = item.getRandomTexts()
            scheduleExactAlarm(
                notificationCalendar.timeInMillis, 
                randomText, 
                item.name,
                globalIndex
            )
        }

        return randomTimes.size
    }

    private fun scheduleExactAlarm(triggerTime: Long, notificationText: String, notificationName: String, index: Int) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SHOW_NOTIFICATION
            putExtra(EXTRA_NOTIFICATION_TEXT, notificationText)
            putExtra(EXTRA_NOTIFICATION_NAME, notificationName)
            putExtra(EXTRA_NOTIFICATION_INDEX, index)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun scheduleDailyReschedule() {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.randomnotif.app.DAILY_RESCHEDULE"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAllNotifications() {
        // IMPORTANT: Intent must match the action used when scheduling,
        // otherwise PendingIntent won't match and alarms won't be cancelled
        for (i in 0..500) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                i,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        // Cancel daily reschedule — action must match scheduleDailyReschedule()
        val rescheduleIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.randomnotif.app.DAILY_RESCHEDULE"
        }
        val reschedulePendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            rescheduleIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        reschedulePendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun showTestNotification(text: String, name: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(name)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + name.hashCode(), notification)
    }
}
