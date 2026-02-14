package com.randomnotif.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randomnotif.app.data.*
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberUpdatedState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenModern(
    settings: NotificationSettings,
    onAddNotification: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onUpdateNotification: (NotificationItem) -> Unit,
    onTestClick: (NotificationItem) -> Unit,
    onExportImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedItemForSheet by remember { mutableStateOf<NotificationItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Nudge",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
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
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (settings.items.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(settings.items, key = { it.id }) { item ->
                        ModernNotificationCard(
                            item = item,
                            onUpdate = onUpdateNotification,
                            onDelete = { onDeleteNotification(item.id) },
                            onTest = { onTestClick(item) },
                            onOpenSettings = { selectedItemForSheet = item }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
    
    // Settings Bottom Sheet
    if (selectedItemForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedItemForSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            SettingsBottomSheet(
                item = selectedItemForSheet!!,
                onUpdate = onUpdateNotification,
                onClose = { selectedItemForSheet = null }
            )
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                text = "Нет уведомлений",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Нажмите + чтобы создать",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNotificationCard(
    item: NotificationItem,
    onUpdate: (NotificationItem) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ScheduleChip(item)
                }
                
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = { onUpdate(item.copy(isEnabled = it)) },
                    modifier = Modifier.scale(0.85f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick info pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill(
                    icon = Icons.Default.Edit,
                    text = "${item.notificationTexts.size} текст${if (item.notificationTexts.size > 1) "а" else ""}"
                )
                
                if (item.useExactTime && item.exactTimes.isNotEmpty()) {
                    InfoPill(
                        icon = Icons.Default.Notifications,
                        text = "${item.exactTimes.size}×"
                    )
                } else {
                    InfoPill(
                        icon = Icons.Default.Star,
                        text = "${item.notificationCount}× случайно"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Настройки")
                }
                
                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Тест")
                }
                
                IconButton(
                    onClick = { showDeleteDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Удалить уведомление?") },
            text = { Text("\"${item.name}\" будет удалено навсегда") },
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
fun ScheduleChip(item: NotificationItem) {
    val scheduleText = when (item.scheduleMode) {
        ScheduleMode.DAILY -> if (item.intervalDays == 1) "Ежедневно" else "Каждые ${item.intervalDays} дн"
        ScheduleMode.WEEKLY -> {
            val days = item.selectedWeekDays.sorted().take(2).joinToString(", ") {
                when (it) {
                    1 -> "Пн"; 2 -> "Вт"; 3 -> "Ср"; 4 -> "Чт"
                    5 -> "Пт"; 6 -> "Сб"; 7 -> "Вс"
                    else -> ""
                }
            }
            if (item.selectedWeekDays.size > 2) "$days..." else days.ifEmpty { "Еженедельно" }
        }
        ScheduleMode.MONTHLY_BY_DATE -> "Ежемесячно"
        ScheduleMode.MONTHLY_BY_WEEKDAY -> "По дням недели"
        ScheduleMode.YEARLY -> "Ежегодно"
        ScheduleMode.SPECIFIC_DATE -> "Конкретная дата"
        ScheduleMode.DATE_RANGE -> "Диапазон"
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = scheduleText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    item: NotificationItem,
    onUpdate: (NotificationItem) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Тексты", "Время", "Расписание")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть")
            }
        }
        
        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> TextsTab(item, onUpdate)
                1 -> TimeTab(item, onUpdate)
                2 -> ScheduleTab(item, onUpdate)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TextsTab(item: NotificationItem, onUpdate: (NotificationItem) -> Unit) {
    var localName by remember(item.id) { mutableStateOf(item.name) }
    var localTexts by remember(item.id) { mutableStateOf(item.notificationTexts) }
    
    val currentItem by rememberUpdatedState(item)
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    
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
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = localName,
                onValueChange = { localName = it },
                label = { Text("Название уведомления") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Тексты уведомлений",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(localTexts.size) { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = localTexts[index],
                    onValueChange = { newText ->
                        localTexts = localTexts.toMutableList().apply { set(index, newText) }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Текст ${index + 1}") },
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (localTexts.size > 1) {
                    FilledIconButton(
                        onClick = {
                            localTexts = localTexts.toMutableList().apply { removeAt(index) }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
        }
        
        item {
            FilledTonalButton(
                onClick = { localTexts = localTexts + "" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить текст")
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Количество текстов на уведомление",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "${item.textsPerNotification} из ${localTexts.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = item.textsPerNotification.toFloat(),
                onValueChange = {
                    val newValue = it.toInt().coerceIn(1, localTexts.size)
                    onUpdate(item.copy(textsPerNotification = newValue))
                },
                valueRange = 1f..localTexts.size.toFloat().coerceAtLeast(1f),
                steps = (localTexts.size - 2).coerceAtLeast(0)
            )
        }
    }
}

@Composable
fun TimeTab(item: NotificationItem, onUpdate: (NotificationItem) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Настройки времени",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Точное время",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (item.useExactTime) "Уведомления в заданное время" else "Случайное время",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = item.useExactTime,
                    onCheckedChange = { onUpdate(item.copy(useExactTime = it)) }
                )
            }
        }
        
        Text(
            "⏰ Полная настройка времени будет доступна в следующей версии",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ScheduleTab(item: NotificationItem, onUpdate: (NotificationItem) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Режим расписания",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Текущий режим: ${getScheduleModeName(item.scheduleMode)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            "📅 Полная настройка расписания будет доступна в следующей версии",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getScheduleModeName(mode: ScheduleMode): String = when (mode) {
    ScheduleMode.DAILY -> "Ежедневно"
    ScheduleMode.WEEKLY -> "Еженедельно"
    ScheduleMode.MONTHLY_BY_DATE -> "Ежемесячно (числа)"
    ScheduleMode.MONTHLY_BY_WEEKDAY -> "Ежемесячно (дни недели)"
    ScheduleMode.YEARLY -> "Ежегодно"
    ScheduleMode.DATE_RANGE -> "Диапазон дат"
    ScheduleMode.SPECIFIC_DATE -> "Конкретная дата"
}
