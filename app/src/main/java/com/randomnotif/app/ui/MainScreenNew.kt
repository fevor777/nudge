package com.randomnotif.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randomnotif.app.data.*
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberUpdatedState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreenNew(
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
                CompactNotificationCard(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompactNotificationCard(
    item: NotificationItem,
    onUpdate: (NotificationItem) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Expandable sections
    var textsExpanded by remember { mutableStateOf(true) }
    var timeExpanded by remember { mutableStateOf(false) }
    var scheduleExpanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getScheduleSummary(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = item.isEnabled,
                        onCheckedChange = { onUpdate(item.copy(isEnabled = it)) }
                    )
                    
                    IconButton(onClick = { expanded = !expanded }) {
                        val rotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            label = "expand"
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Свернуть" else "Развернуть",
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
            }

            // Action Buttons Row
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Тест")
                        }
                        
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Expandable Sections
                    TextsSection(
                        item = item,
                        expanded = textsExpanded,
                        onExpandChange = { textsExpanded = it },
                        onUpdate = onUpdate
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TimeSection(
                        item = item,
                        expanded = timeExpanded,
                        onExpandChange = { timeExpanded = it },
                        onUpdate = onUpdate
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ScheduleSection(
                        item = item,
                        expanded = scheduleExpanded,
                        onExpandChange = { scheduleExpanded = it },
                        onUpdate = onUpdate
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AdvancedSection(
                        item = item,
                        expanded = advancedExpanded,
                        onExpandChange = { advancedExpanded = it },
                        onUpdate = onUpdate
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить уведомление?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
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

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        onClick = { onExpandChange(!expanded) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                label = "section_expand"
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextsSection(
    item: NotificationItem,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onUpdate: (NotificationItem) -> Unit
) {
    var localName by remember(item.id) { mutableStateOf(item.name) }
    var localTexts by remember(item.id) { mutableStateOf(item.notificationTexts) }
    
    val currentItem by rememberUpdatedState(item)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    
    // Debounced save
    LaunchedEffect(localName) {
        if (localName != currentItem.name) {
            delay(500)
            currentOnUpdate(currentItem.copy(name = localName))
        }
    }
    
    LaunchedEffect(localTexts) {
        if (localTexts != currentItem.notificationTexts) {
            delay(500)
            currentOnUpdate(currentItem.copy(notificationTexts = localTexts))
        }
    }
    
    LaunchedEffect(item.id) {
        localName = item.name
        localTexts = item.notificationTexts
    }

    Column {
        SectionHeader(
            title = "📝 Тексты (${localTexts.size})",
            icon = Icons.Default.Edit,
            expanded = expanded,
            onExpandChange = onExpandChange
        )
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                OutlinedTextField(
                    value = localName,
                    onValueChange = { localName = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Тексты уведомлений:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                localTexts.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                localTexts = localTexts.toMutableList().apply {
                                    set(index, newText)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Текст уведомления") }
                        )
                        
                        if (localTexts.size > 1) {
                            IconButton(onClick = {
                                localTexts = localTexts.toMutableList().apply {
                                    removeAt(index)
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    if (index < localTexts.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        localTexts = localTexts + ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить текст")
                }
            }
        }
    }
}

// Placeholder composables - will be implemented with actual logic
@Composable
fun TimeSection(
    item: NotificationItem,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onUpdate: (NotificationItem) -> Unit
) {
    Column {
        SectionHeader(
            title = "⏰ Время",
            icon = Icons.Default.Edit,
            expanded = expanded,
            onExpandChange = onExpandChange
        )
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Text(
                text = "Time settings will be here",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ScheduleSection(
    item: NotificationItem,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onUpdate: (NotificationItem) -> Unit
) {
    Column {
        SectionHeader(
            title = "📅 Расписание",
            icon = Icons.Default.DateRange,
            expanded = expanded,
            onExpandChange = onExpandChange
        )
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Text(
                text = "Schedule settings will be here",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AdvancedSection(
    item: NotificationItem,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onUpdate: (NotificationItem) -> Unit
) {
    Column {
        SectionHeader(
            title = "⚙️ Дополнительно",
            icon = Icons.Default.Settings,
            expanded = expanded,
            onExpandChange = onExpandChange
        )
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Количество уведомлений в день: ${item.notificationCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = item.notificationCount.toFloat(),
                    onValueChange = { onUpdate(item.copy(notificationCount = it.toInt())) },
                    valueRange = 1f..50f,
                    steps = 48
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Текстов на уведомление: ${item.textsPerNotification}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = item.textsPerNotification.toFloat(),
                    onValueChange = { 
                        val newValue = it.toInt().coerceIn(1, item.notificationTexts.size)
                        onUpdate(item.copy(textsPerNotification = newValue))
                    },
                    valueRange = 1f..item.notificationTexts.size.toFloat().coerceAtLeast(1f),
                    steps = (item.notificationTexts.size - 2).coerceAtLeast(0)
                )
            }
        }
    }
}

// Helper function to generate schedule summary
fun getScheduleSummary(item: NotificationItem): String {
    val scheduleText = when (item.scheduleMode) {
        ScheduleMode.DAILY -> {
            if (item.intervalDays == 1) "Каждый день"
            else "Каждые ${item.intervalDays} дня"
        }
        ScheduleMode.WEEKLY -> {
            val days = item.selectedWeekDays.sorted().joinToString(", ") {
                when (it) {
                    1 -> "Пн"
                    2 -> "Вт"
                    3 -> "Ср"
                    4 -> "Чт"
                    5 -> "Пт"
                    6 -> "Сб"
                    7 -> "Вс"
                    else -> ""
                }
            }
            if (days.isEmpty()) "Не выбраны дни" else days
        }
        ScheduleMode.MONTHLY_BY_DATE -> {
            val days = item.selectedMonthDays.sorted().take(3).joinToString(", ")
            if (days.isEmpty()) "Не выбраны числа" 
            else if (item.selectedMonthDays.size > 3) "$days..."
            else days
        }
        ScheduleMode.MONTHLY_BY_WEEKDAY -> {
            val ordinal = when (item.monthWeekdayOrdinal) {
                MonthlyOrdinal.FIRST -> "1-й"
                MonthlyOrdinal.SECOND -> "2-й"
                MonthlyOrdinal.THIRD -> "3-й"
                MonthlyOrdinal.FOURTH -> "4-й"
                MonthlyOrdinal.LAST -> "Последний"
                else -> ""
            }
            val day = when (item.monthWeekday) {
                1 -> "Пн"
                2 -> "Вт"
                3 -> "Ср"
                4 -> "Чт"
                5 -> "Пт"
                6 -> "Сб"
                7 -> "Вс"
                else -> ""
            }
            if (ordinal.isEmpty() || day.isEmpty()) "Не настроено" else "$ordinal $day месяца"
        }
        ScheduleMode.YEARLY -> {
            if (item.yearlyMonth != null && item.yearlyDay != null) {
                val monthName = when (item.yearlyMonth) {
                    1 -> "янв"
                    2 -> "фев"
                    3 -> "мар"
                    4 -> "апр"
                    5 -> "мая"
                    6 -> "июн"
                    7 -> "июл"
                    8 -> "авг"
                    9 -> "сен"
                    10 -> "окт"
                    11 -> "ноя"
                    12 -> "дек"
                    else -> ""
                }
                "${item.yearlyDay} $monthName"
            } else "Не настроено"
        }
        ScheduleMode.SPECIFIC_DATE -> {
            item.specificDateMillis?.let {
                SimpleDateFormat("d MMM yyyy", Locale("ru")).format(Date(it))
            } ?: "Не выбрана дата"
        }
        ScheduleMode.DATE_RANGE -> {
            val start = item.startDateMillis?.let { 
                SimpleDateFormat("d MMM", Locale("ru")).format(Date(it))
            } ?: "?"
            val end = item.endDateMillis?.let {
                SimpleDateFormat("d MMM", Locale("ru")).format(Date(it))
            } ?: "?"
            "$start - $end"
        }
    }
    
    val timeText = if (item.useExactTime && item.exactTimes.isNotEmpty()) {
        "${item.exactTimes.size} раз"
    } else {
        "${item.notificationCount} раз"
    }
    
    return "$scheduleText • $timeText"
}
