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
import com.randomnotif.app.ui.ExportImportScreen
import com.randomnotif.app.ui.MainScreen
import com.randomnotif.app.ui.theme.RandomNotifTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationScheduler: NotificationScheduler
    
    private var pendingImportCallback: ((String?) -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Для работы приложения нужно разрешение на уведомления", Toast.LENGTH_LONG).show()
        }
    }
    
    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { exportToUri(it) }
    }
    
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importFromUri(it) }
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
                var showExportImport by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    settingsRepository.settingsFlow.collectLatest { loadedSettings ->
                        settings = loadedSettings
                    }
                }

                if (showExportImport) {
                    ExportImportScreen(
                        onBack = { showExportImport = false },
                        onExportToFile = {
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                            val fileName = "nudge_backup_${dateFormat.format(Date())}.json"
                            exportFileLauncher.launch(fileName)
                        },
                        onGetExportData = {
                            settingsRepository.exportToJson()
                        },
                        onImportFromFile = {
                            importFileLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        onImportFromText = { jsonText ->
                            val success = settingsRepository.importFromJson(jsonText)
                            if (success) {
                                lifecycleScope.launch {
                                    val loadedSettings = settingsRepository.settingsFlow.first()
                                    settings = loadedSettings
                                    notificationScheduler.scheduleAllNotifications(loadedSettings)
                                }
                            }
                            success
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MainScreen(
                        settings = settings,
                        onAddNotification = {
                            lifecycleScope.launch {
                                val updatedSettings = settingsRepository.addNotification()
                                notificationScheduler.scheduleAllNotifications(updatedSettings)
                            }
                        },
                        onDeleteNotification = { id ->
                            lifecycleScope.launch {
                                val updatedSettings = settingsRepository.deleteNotification(id)
                                notificationScheduler.scheduleAllNotifications(updatedSettings)
                            }
                        },
                        onUpdateNotification = { item ->
                            lifecycleScope.launch {
                                val updatedSettings = settingsRepository.updateNotification(item)
                                notificationScheduler.scheduleAllNotifications(updatedSettings)
                            }
                        },
                        onTestClick = { item ->
                            notificationScheduler.showTestNotification(item.getRandomTexts(), item.name)
                        },
                        onExportImportClick = { showExportImport = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
    
    private fun exportToUri(uri: Uri) {
        try {
            val jsonData = settingsRepository.exportToJson()
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonData.toByteArray())
            }
            Toast.makeText(this, "Data exported successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun importFromUri(uri: Uri) {
        try {
            val jsonData = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: throw Exception("Could not read file")
            
            lifecycleScope.launch {
                val settings = settingsRepository.importFromJsonAsync(jsonData)
                if (settings != null) {
                    notificationScheduler.scheduleAllNotifications(settings)
                    Toast.makeText(this@MainActivity, "Data imported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Import failed: Invalid JSON format", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
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
