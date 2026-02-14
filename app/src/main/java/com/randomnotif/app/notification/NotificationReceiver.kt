package com.randomnotif.app.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import com.randomnotif.app.MainActivity
import com.randomnotif.app.R
import com.randomnotif.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationScheduler.ACTION_SHOW_NOTIFICATION -> {
                val text = intent.getStringExtra(NotificationScheduler.EXTRA_NOTIFICATION_TEXT) 
                    ?: "Напоминание!"
                val name = intent.getStringExtra(NotificationScheduler.EXTRA_NOTIFICATION_NAME)
                    ?: "Nudge"
                val index = intent.getIntExtra(NotificationScheduler.EXTRA_NOTIFICATION_INDEX, 0)
                showNotification(context, text, name, index)
                
                // Reschedule for next occurrence
                rescheduleIfNeeded(context)
            }
            "com.randomnotif.app.DAILY_RESCHEDULE" -> {
                rescheduleNotifications(context)
            }
        }
    }

    private fun showNotification(context: Context, text: String, name: String, index: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open MainActivity when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            index,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(name)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NotificationScheduler.NOTIFICATION_ID + index, notification)
    }

    private fun rescheduleIfNeeded(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = SettingsRepository(context)
            val settings = repository.settingsFlow.first()
            if (settings.items.any { it.isEnabled }) {
                val scheduler = NotificationScheduler(context)
                scheduler.scheduleAllNotifications(settings)
            }
        }
    }

    private fun rescheduleNotifications(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = SettingsRepository(context)
            val settings = repository.settingsFlow.first()
            if (settings.items.any { it.isEnabled }) {
                val scheduler = NotificationScheduler(context)
                scheduler.scheduleAllNotifications(settings)
            }
        }
    }
}
