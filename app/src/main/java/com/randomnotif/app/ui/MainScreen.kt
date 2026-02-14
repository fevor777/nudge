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
import com.randomnotif.app.data.ScheduleMode
import com.randomnotif.app.data.MonthlyOrdinal
import com.randomnotif.app.data.WorkingDayPosition
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberUpdatedState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showSpecificDatePicker by remember { mutableStateOf(false) }
    
    // Local state for text fields - completely decoupled from parent during editing
    var localName by remember(item.id) { mutableStateOf(item.name) }
    var localTexts by remember(item.id) { mutableStateOf(item.notificationTexts) }
    
    // Use rememberUpdatedState so the debounced effects always see the latest item & callback,
    // preventing stale captures that could revert other settings changes during the delay
    val currentItem by rememberUpdatedState(item)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    
    // Debounced save for name - only save after 500ms of no typing
    LaunchedEffect(localName) {
        if (localName != currentItem.name) {
            delay(500)
            currentOnUpdate(currentItem.copy(name = localName))
        }
    }
    
    // Debounced save for texts - only save after 500ms of no typing
    LaunchedEffect(localTexts) {
        if (localTexts != currentItem.notificationTexts) {
            delay(500)
            currentOnUpdate(currentItem.copy(notificationTexts = localTexts))
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
                            val updated = if (enabled && item.intervalStartDateMillis == null) {
                                // Set interval start date to today when first enabled
                                item.copy(isEnabled = true, intervalStartDateMillis = System.currentTimeMillis())
                            } else {
                                item.copy(isEnabled = enabled)
                            }
                            onUpdate(updated)
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val scheduleInfo = when (item.scheduleMode) {
                            ScheduleMode.DAILY -> if (item.intervalDays > 1) "Раз в ${item.intervalDays} дн." else "Ежедневно"
                            ScheduleMode.DATE_RANGE -> {
                                val start = item.startDateMillis?.let { formatDate(it) } ?: "?"
                                val end = item.endDateMillis?.let { formatDate(it) } ?: "?"
                                "$start – $end"
                            }
                            ScheduleMode.SPECIFIC_DATE -> {
                                item.specificDateMillis?.let { formatDate(it) } ?: "Дата не выбрана"
                            }
                            ScheduleMode.WEEKLY -> {
                                val days = item.selectedWeekDays.sorted().joinToString(", ") {
                                    when (it) {
                                        1 -> "Пн"; 2 -> "Вт"; 3 -> "Ср"; 4 -> "Чт"
                                        5 -> "Пт"; 6 -> "Сб"; 7 -> "Вс"
                                        else -> ""
                                    }
                                }
                                if (days.isEmpty()) "Еженедельно" else days
                            }
                            ScheduleMode.MONTHLY_BY_DATE -> {
                                if (item.workingDaysOnly) {
                                    when (item.workingDayPosition) {
                                        WorkingDayPosition.FIRST -> "Первый раб. день"
                                        WorkingDayPosition.LAST -> "Последний раб. день"
                                        null -> "Ежемесячно"
                                    }
                                } else {
                                    val days = item.selectedMonthDays.sorted().take(3).joinToString(", ")
                                    if (days.isEmpty()) "Ежемесячно" else "$days..."
                                }
                            }
                            ScheduleMode.MONTHLY_BY_WEEKDAY -> {
                                val ordinal = when (item.monthWeekdayOrdinal) {
                                    MonthlyOrdinal.FIRST -> "1-й"
                                    MonthlyOrdinal.SECOND -> "2-й"
                                    MonthlyOrdinal.THIRD -> "3-й"
                                    MonthlyOrdinal.FOURTH -> "4-й"
                                    MonthlyOrdinal.LAST -> "Последний"
                                    null -> "?"
                                }
                                val weekday = when (item.monthWeekday) {
                                    1 -> "Пн"; 2 -> "Вт"; 3 -> "Ср"; 4 -> "Чт"
                                    5 -> "Пт"; 6 -> "Сб"; 7 -> "Вс"
                                    else -> "?"
                                }
                                "$ordinal $weekday месяца"
                            }
                            ScheduleMode.YEARLY -> {
                                val month = when (item.yearlyMonth) {
                                    1 -> "янв"; 2 -> "фев"; 3 -> "мар"; 4 -> "апр"
                                    5 -> "май"; 6 -> "июн"; 7 -> "июл"; 8 -> "авг"
                                    9 -> "сен"; 10 -> "окт"; 11 -> "ноя"; 12 -> "дек"
                                    else -> "?"
                                }
                                val day = item.yearlyDay ?: "?"
                                "$day $month"
                            }
                        }
                        Text(
                            text = "${String.format("%02d:%02d", item.startHour, item.startMinute)} - ${String.format("%02d:%02d", item.endHour, item.endMinute)} • ${item.notificationCount}x • $scheduleInfo",
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
                                        // Clamp textsPerNotification if it exceeds new list size
                                        val clampedTextsPerNotif = item.textsPerNotification.coerceAtMost(newTexts.size)
                                        onUpdate(item.copy(
                                            notificationTexts = newTexts,
                                            textsPerNotification = clampedTextsPerNotif
                                        ))
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

                    // Texts per notification
                    Text(
                        text = "Текстов в одном уведомлении",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (item.textsPerNotification > 1) {
                                    onUpdate(item.copy(textsPerNotification = item.textsPerNotification - 1))
                                }
                            },
                            enabled = item.textsPerNotification > 1
                        ) {
                            Text("-")
                        }
                        Text(
                            text = item.textsPerNotification.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                if (item.textsPerNotification < item.notificationTexts.size) {
                                    onUpdate(item.copy(textsPerNotification = item.textsPerNotification + 1))
                                }
                            },
                            enabled = item.textsPerNotification < item.notificationTexts.size
                        ) {
                            Text("+")
                        }
                    }
                    if (item.notificationTexts.size > 1) {
                        Text(
                            text = "Из ${item.notificationTexts.size} текстов будет случайно выбрано ${item.textsPerNotification}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Schedule Mode
                    Text(
                        text = "Режим расписания",
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    var expandedScheduleMode by remember { mutableStateOf(false) }
                    val scheduleText = when (item.scheduleMode) {
                        ScheduleMode.DAILY -> "Ежедневно"
                        ScheduleMode.WEEKLY -> "Еженедельно"
                        ScheduleMode.MONTHLY_BY_DATE -> "Ежемесячно (числа)"
                        ScheduleMode.MONTHLY_BY_WEEKDAY -> "Ежемесячно (дни недели)"
                        ScheduleMode.YEARLY -> "Ежегодно"
                        ScheduleMode.DATE_RANGE -> "Диапазон дат"
                        ScheduleMode.SPECIFIC_DATE -> "Конкретная дата"
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedScheduleMode = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(scheduleText)
                        }
                        
                        DropdownMenu(
                            expanded = expandedScheduleMode,
                            onDismissRequest = { expandedScheduleMode = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ежедневно") },
                                onClick = {
                                    onUpdate(item.copy(scheduleMode = ScheduleMode.DAILY))
                                    expandedScheduleMode = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Еженедельно") },
                                onClick = {
                                    onUpdate(item.copy(scheduleMode = ScheduleMode.WEEKLY))
                                    expandedScheduleMode = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ежемесячно (числа)") },
                                onClick = {
                                    onUpdate(item.copy(scheduleMode = ScheduleMode.MONTHLY_BY_DATE))
                                    expandedScheduleMode = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ежемесячно (дни недели)") },
                                onClick = {
                                    onUpdate(item.copy(scheduleMode = ScheduleMode.MONTHLY_BY_WEEKDAY))
                                    expandedScheduleMode = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ежегодно") },
                                onClick = {
                                    onUpdate(item.copy(scheduleMode = ScheduleMode.YEARLY))
                                    expandedScheduleMode = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Диапазон дат") },
                                onClick = {
                                    onUpdate(item.copy(scheduleMode = ScheduleMode.DATE_RANGE))
                                    expandedScheduleMode = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Конкретная дата") },
                                onClick = {
                                    onUpdate(item.copy(scheduleMode = ScheduleMode.SPECIFIC_DATE))
                                    expandedScheduleMode = false
                                }
                            )
                        }
                    }

                    // Date selection based on mode
                    when (item.scheduleMode) {
                        ScheduleMode.DATE_RANGE -> {
                            Text(
                                text = "Диапазон дат",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("С", style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(
                                        onClick = { showStartDatePicker = true }
                                    ) {
                                        Text(
                                            text = item.startDateMillis?.let {
                                                formatDate(it)
                                            } ?: "Выбрать"
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("По", style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(
                                        onClick = { showEndDatePicker = true }
                                    ) {
                                        Text(
                                            text = item.endDateMillis?.let {
                                                formatDate(it)
                                            } ?: "Выбрать"
                                        )
                                    }
                                }
                            }
                        }
                        ScheduleMode.SPECIFIC_DATE -> {
                            Text(
                                text = "Дата",
                                style = MaterialTheme.typography.labelLarge
                            )
                            OutlinedButton(
                                onClick = { showSpecificDatePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = item.specificDateMillis?.let {
                                        formatDate(it)
                                    } ?: "Выбрать дату"
                                )
                            }
                        }
                        ScheduleMode.DAILY -> {
                            // Interval days control
                            Text(
                                text = "Интервал (дни)",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (item.intervalDays > 1) {
                                            onUpdate(item.copy(intervalDays = item.intervalDays - 1))
                                        }
                                    },
                                    enabled = item.intervalDays > 1
                                ) {
                                    Text("-")
                                }
                                Text(
                                    text = item.intervalDays.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (item.intervalDays < 365) {
                                            onUpdate(item.copy(intervalDays = item.intervalDays + 1))
                                        }
                                    },
                                    enabled = item.intervalDays < 365
                                ) {
                                    Text("+")
                                }
                            }
                            Text(
                                text = when (item.intervalDays) {
                                    1 -> "Каждый день"
                                    2 -> "Раз в 2 дня"
                                    else -> "Раз в ${item.intervalDays} дней"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ScheduleMode.WEEKLY -> {
                            Text(
                                text = "Дни недели",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                                for (day in 1..7) {
                                    FilterChip(
                                        selected = item.selectedWeekDays.contains(day),
                                        onClick = {
                                            val newDays = if (item.selectedWeekDays.contains(day)) {
                                                item.selectedWeekDays - day
                                            } else {
                                                item.selectedWeekDays + day
                                            }
                                            onUpdate(item.copy(selectedWeekDays = newDays))
                                        },
                                        label = { Text(dayNames[day - 1]) }
                                    )
                                }
                            }
                        }
                        ScheduleMode.MONTHLY_BY_DATE -> {
                            Text(
                                text = "Выбор дня месяца",
                                style = MaterialTheme.typography.labelLarge
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.workingDaysOnly,
                                    onCheckedChange = {
                                        onUpdate(item.copy(workingDaysOnly = it))
                                    }
                                )
                                Text("Рабочий день месяца")
                            }
                            
                            if (item.workingDaysOnly) {
                                var expandedWorkingDay by remember { mutableStateOf(false) }
                                val workingDayText = when (item.workingDayPosition) {
                                    WorkingDayPosition.FIRST -> "Первый"
                                    WorkingDayPosition.LAST -> "Последний"
                                    null -> "Выбрать"
                                }
                                
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandedWorkingDay = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(workingDayText)
                                    }
                                    
                                    DropdownMenu(
                                        expanded = expandedWorkingDay,
                                        onDismissRequest = { expandedWorkingDay = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Первый") },
                                            onClick = {
                                                onUpdate(item.copy(workingDayPosition = WorkingDayPosition.FIRST))
                                                expandedWorkingDay = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Последний") },
                                            onClick = {
                                                onUpdate(item.copy(workingDayPosition = WorkingDayPosition.LAST))
                                                expandedWorkingDay = false
                                            }
                                        )
                                    }
                                }
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (day in 1..31) {
                                        FilterChip(
                                            selected = item.selectedMonthDays.contains(day),
                                            onClick = {
                                                val newDays = if (item.selectedMonthDays.contains(day)) {
                                                    item.selectedMonthDays - day
                                                } else {
                                                    item.selectedMonthDays + day
                                                }
                                                onUpdate(item.copy(selectedMonthDays = newDays))
                                            },
                                            label = { Text(day.toString()) }
                                        )
                                    }
                                }
                            }
                        }
                        ScheduleMode.MONTHLY_BY_WEEKDAY -> {
                            Text(
                                text = "День недели месяца",
                                style = MaterialTheme.typography.labelLarge
                            )
                            
                            var expandedOrdinal by remember { mutableStateOf(false) }
                            val ordinalText = when (item.monthWeekdayOrdinal) {
                                MonthlyOrdinal.FIRST -> "Первый"
                                MonthlyOrdinal.SECOND -> "Второй"
                                MonthlyOrdinal.THIRD -> "Третий"
                                MonthlyOrdinal.FOURTH -> "Четвёртый"
                                MonthlyOrdinal.LAST -> "Последний"
                                null -> "Выбрать"
                            }
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedOrdinal = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(ordinalText)
                                }
                                
                                DropdownMenu(
                                    expanded = expandedOrdinal,
                                    onDismissRequest = { expandedOrdinal = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Первый") },
                                        onClick = {
                                            onUpdate(item.copy(monthWeekdayOrdinal = MonthlyOrdinal.FIRST))
                                            expandedOrdinal = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Второй") },
                                        onClick = {
                                            onUpdate(item.copy(monthWeekdayOrdinal = MonthlyOrdinal.SECOND))
                                            expandedOrdinal = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Третий") },
                                        onClick = {
                                            onUpdate(item.copy(monthWeekdayOrdinal = MonthlyOrdinal.THIRD))
                                            expandedOrdinal = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Четвёртый") },
                                        onClick = {
                                            onUpdate(item.copy(monthWeekdayOrdinal = MonthlyOrdinal.FOURTH))
                                            expandedOrdinal = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Последний") },
                                        onClick = {
                                            onUpdate(item.copy(monthWeekdayOrdinal = MonthlyOrdinal.LAST))
                                            expandedOrdinal = false
                                        }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var expandedWeekday by remember { mutableStateOf(false) }
                            val weekdayNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
                            val weekdayText = item.monthWeekday?.let { weekdayNames[it - 1] } ?: "Выбрать день"
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedWeekday = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(weekdayText)
                                }
                                
                                DropdownMenu(
                                    expanded = expandedWeekday,
                                    onDismissRequest = { expandedWeekday = false }
                                ) {
                                    for (day in 1..7) {
                                        DropdownMenuItem(
                                            text = { Text(weekdayNames[day - 1]) },
                                            onClick = {
                                                onUpdate(item.copy(monthWeekday = day))
                                                expandedWeekday = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        ScheduleMode.YEARLY -> {
                            Text(
                                text = "Дата в году",
                                style = MaterialTheme.typography.labelLarge
                            )
                            
                            var expandedMonth by remember { mutableStateOf(false) }
                            val monthNames = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", 
                                                   "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
                            val monthText = item.yearlyMonth?.let { monthNames[it - 1] } ?: "Выбрать месяц"
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedMonth = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(monthText)
                                }
                                
                                DropdownMenu(
                                    expanded = expandedMonth,
                                    onDismissRequest = { expandedMonth = false }
                                ) {
                                    for (month in 1..12) {
                                        DropdownMenuItem(
                                            text = { Text(monthNames[month - 1]) },
                                            onClick = {
                                                onUpdate(item.copy(yearlyMonth = month))
                                                expandedMonth = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("День месяца", style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if ((item.yearlyDay ?: 1) > 1) {
                                            onUpdate(item.copy(yearlyDay = (item.yearlyDay ?: 1) - 1))
                                        }
                                    },
                                    enabled = (item.yearlyDay ?: 1) > 1
                                ) {
                                    Text("-")
                                }
                                Text(
                                    text = (item.yearlyDay ?: 1).toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if ((item.yearlyDay ?: 1) < 31) {
                                            onUpdate(item.copy(yearlyDay = (item.yearlyDay ?: 1) + 1))
                                        }
                                    },
                                    enabled = (item.yearlyDay ?: 1) < 31
                                ) {
                                    Text("+")
                                }
                            }
                        }
                    }

                    // Time Mode Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.useExactTime,
                            onCheckedChange = {
                                onUpdate(item.copy(useExactTime = it))
                            }
                        )
                        Text("Точное время")
                    }

                    if (item.useExactTime) {
                        // Exact Times List
                        Text(
                            text = "Времена уведомлений",
                            style = MaterialTheme.typography.labelLarge
                        )
                        
                        item.exactTimes.forEachIndexed { index, time ->
                            var showTimePicker by remember { mutableStateOf(false) }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(String.format("%02d:%02d", time.hour, time.minute))
                                }
                                
                                IconButton(
                                    onClick = {
                                        val newTimes = item.exactTimes.toMutableList()
                                        newTimes.removeAt(index)
                                        onUpdate(item.copy(exactTimes = newTimes))
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить время")
                                }
                            }
                            
                            if (showTimePicker) {
                                TimePickerDialog(
                                    initialHour = time.hour,
                                    initialMinute = time.minute,
                                    onConfirm = { hour, minute ->
                                        val newTimes = item.exactTimes.toMutableList()
                                        newTimes[index] = com.randomnotif.app.data.ExactTime(hour, minute)
                                        onUpdate(item.copy(exactTimes = newTimes.sortedBy { it.hour * 60 + it.minute }))
                                        showTimePicker = false
                                    },
                                    onDismiss = { showTimePicker = false }
                                )
                            }
                        }
                        
                        // Add Time Button
                        OutlinedButton(
                            onClick = {
                                val newTimes = item.exactTimes + com.randomnotif.app.data.ExactTime(12, 0)
                                onUpdate(item.copy(exactTimes = newTimes.sortedBy { it.hour * 60 + it.minute }))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Добавить время")
                        }
                    } else {
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

    // Date Picker Dialogs
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDateMillis = item.startDateMillis,
            onConfirm = { millis ->
                onUpdate(item.copy(startDateMillis = millis))
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            initialDateMillis = item.endDateMillis,
            onConfirm = { millis ->
                onUpdate(item.copy(endDateMillis = millis))
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    if (showSpecificDatePicker) {
        DatePickerDialog(
            initialDateMillis = item.specificDateMillis,
            onConfirm = { millis ->
                onUpdate(item.copy(specificDateMillis = millis))
                showSpecificDatePicker = false
            },
            onDismiss = { showSpecificDatePicker = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDateMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis()
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onConfirm(it) }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
