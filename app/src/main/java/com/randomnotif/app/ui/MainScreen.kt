package com.randomnotif.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.randomnotif.app.data.NotificationItem
import com.randomnotif.app.data.NotificationSettings
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    settings: NotificationSettings,
    onAddNotification: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onUpdateNotification: (NotificationItem) -> Unit,
    onTestClick: (NotificationItem) -> Unit,
    onExportImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nudge") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export / Import") },
                            onClick = {
                                showMenu = false
                                onExportImportClick()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNotification,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить уведомление")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (settings.items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Нет уведомлений",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нажмите + чтобы добавить",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(settings.items, key = { it.id }) { item ->
                NotificationItemCard(
                    item = item,
                    onUpdate = onUpdateNotification,
                    onDelete = { onDeleteNotification(item.id) },
                    onTest = { onTestClick(item) }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "ℹ️ Время уведомлений генерируется случайно каждый день и скрыто от пользователя для непредсказуемости.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItemCard(
    item: NotificationItem,
    onUpdate: (NotificationItem) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Local state for text fields - completely decoupled from parent during editing
    var localName by remember(item.id) { mutableStateOf(item.name) }
    var localTexts by remember(item.id) { mutableStateOf(item.notificationTexts) }
    
    // Debounced save for name - only save after 500ms of no typing
    LaunchedEffect(localName) {
        if (localName != item.name) {
            delay(500)
            onUpdate(item.copy(name = localName))
        }
    }
    
    // Debounced save for texts - only save after 500ms of no typing
    LaunchedEffect(localTexts) {
        if (localTexts != item.notificationTexts) {
            delay(500)
            onUpdate(item.copy(notificationTexts = localTexts))
        }
    }
    
    // Sync from parent only when id changes (new item loaded)
    LaunchedEffect(item.id) {
        localName = item.name
        localTexts = item.notificationTexts
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header - always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Switch(
                        checked = item.isEnabled,
                        onCheckedChange = { enabled ->
                            onUpdate(item.copy(isEnabled = enabled))
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${String.format("%02d:%02d", item.startHour, item.startMinute)} - ${String.format("%02d:%02d", item.endHour, item.endMinute)} • ${item.notificationCount}x • ${item.notificationTexts.size} текст(ов)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть"
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    // Name
                    OutlinedTextField(
                        value = localName,
                        onValueChange = { name ->
                            localName = name
                        },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Notification Texts (multiple)
                    Text(
                        text = "Тексты уведомлений (случайный выбор)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    localTexts.forEachIndexed { index, text ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = text,
                                onValueChange = { newText ->
                                    val newTexts = localTexts.toMutableList()
                                    newTexts[index] = newText
                                    localTexts = newTexts
                                },
                                label = { Text("Текст ${index + 1}") },
                                modifier = Modifier.weight(1f),
                                singleLine = false,
                                maxLines = 2
                            )
                            if (localTexts.size > 1) {
                                IconButton(
                                    onClick = {
                                        val newTexts = localTexts.toMutableList()
                                        newTexts.removeAt(index)
                                        localTexts = newTexts
                                        onUpdate(item.copy(notificationTexts = newTexts))
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить текст",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val newTexts = localTexts + "Новый текст"
                            localTexts = newTexts
                            onUpdate(item.copy(notificationTexts = newTexts))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить вариант текста")
                    }

                    // Time Range
                    Text(
                        text = "Временной промежуток",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Начало",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                onClick = { showStartTimePicker = true }
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", item.startHour, item.startMinute)
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Конец",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                onClick = { showEndTimePicker = true }
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", item.endHour, item.endMinute)
                                )
                            }
                        }
                    }


                    // Notification Count
                    Text(
                        text = "Количество уведомлений",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (item.notificationCount > 1) {
                                    onUpdate(item.copy(notificationCount = item.notificationCount - 1))
                                }
                            },
                            enabled = item.notificationCount > 1
                        ) {
                            Text("-")
                        }
                        Text(
                            text = item.notificationCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                if (item.notificationCount < 50) {
                                    onUpdate(item.copy(notificationCount = item.notificationCount + 1))
                                }
                            },
                            enabled = item.notificationCount < 50
                        ) {
                            Text("+")
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Тест")
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить")
                        }
                    }
                }
            }
        }
    }

    // Time Picker Dialogs
    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = item.startHour,
            initialMinute = item.startMinute,
            onConfirm = { hour, minute ->
                onUpdate(item.copy(startHour = hour, startMinute = minute))
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = item.endHour,
            initialMinute = item.endMinute,
            onConfirm = { hour, minute ->
                onUpdate(item.copy(endHour = hour, endMinute = minute))
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить уведомление?") },
            text = { Text("Вы уверены, что хотите удалить \"${item.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
