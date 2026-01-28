package com.randomnotif.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.randomnotif.app.data.NotificationItem
import com.randomnotif.app.data.NotificationSettings
import com.randomnotif.app.data.SettingsRepository
import com.randomnotif.app.notification.NotificationScheduler
import com.randomnotif.app.ui.MainScreen
import com.randomnotif.app.ui.theme.RandomNotifTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationScheduler: NotificationScheduler

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Для работы приложения нужно разрешение на уведомления", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(applicationContext)
        notificationScheduler = NotificationScheduler(applicationContext)

        requestNotificationPermission()
        requestExactAlarmPermission()

        setContent {
            RandomNotifTheme {
                var settings by remember { mutableStateOf(NotificationSettings()) }

                LaunchedEffect(Unit) {
                    settingsRepository.settingsFlow.collectLatest { loadedSettings ->
                        settings = loadedSettings
                    }
                }

                MainScreen(
                    settings = settings,
                    onAddNotification = {
                        lifecycleScope.launch {
                            settings = settingsRepository.addNotification()
                            notificationScheduler.scheduleAllNotifications(settings)
                        }
                    },
                    onDeleteNotification = { id ->
                        lifecycleScope.launch {
                            settings = settingsRepository.deleteNotification(id)
                            notificationScheduler.scheduleAllNotifications(settings)
                        }
                    },
                    onUpdateNotification = { item ->
                        lifecycleScope.launch {
                            settings = settingsRepository.updateNotification(item)
                            notificationScheduler.scheduleAllNotifications(settings)
                        }
                    },
                    onTestClick = { item ->
                        notificationScheduler.showTestNotification(item.getRandomText(), item.name)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Для работы уведомлений нужно разрешить точные будильники в настройках",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback - open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }
        }
    }
}
