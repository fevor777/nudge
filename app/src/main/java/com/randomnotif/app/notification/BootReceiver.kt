package com.randomnotif.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.randomnotif.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
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
}
