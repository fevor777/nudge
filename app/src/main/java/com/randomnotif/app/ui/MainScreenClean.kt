package com.randomnotif.app.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun MainScreenClean(
    settings: NotificationSettings,
    onAddNotification: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onUpdateNotification: (NotificationItem) -> Unit,
    onTestClick: (NotificationItem) -> Unit,
    onExportImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItem by remember { mutableStateOf<NotificationItem?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (selectedItem == null) {
            // List view
            Scaffold(
                containerColor = Color(0xFFF5F5F7),
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                "Уведомления",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFFF5F5F7)
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
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
            ) { padding ->
                if (settings.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFB0B0B0)
                            )
                            Text(
                                "Нет уведомлений",
                                fontSize = 18.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(settings.items, key = { it.id }) { item ->
                            NotificationListItem(
                                item = item,
                                onUpdate = onUpdateNotification,
                                onClick = { selectedItem = item }
                            )
                        }
                    }
                }
            }
        } else {
            // Editor view
            NotificationEditor(
                item = selectedItem!!,
                onUpdate = onUpdateNotification,
                onClose = { selectedItem = null },
                onDelete = {
                    onDeleteNotification(selectedItem!!.id)
                    selectedItem = null
                },
                onTest = { onTestClick(selectedItem!!) }
            )
        }
    }
}

@Composable
fun NotificationListItem(
    item: NotificationItem,
    onUpdate: (NotificationItem) -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = item.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF000000)
                    )
                    Text(
                        text = getScheduleDescription(item),
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            Switch(
                checked = item.isEnabled,
                onCheckedChange = { onUpdate(item.copy(isEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF007AFF),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE5E5EA)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationEditor(
    item: NotificationItem,
    onUpdate: (NotificationItem) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    var localName by remember(item.id) { mutableStateOf(item.name) }
    var localTexts by remember(item.id) { mutableStateOf(item.notificationTexts) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
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
    
    Scaffold(
        containerColor = Color(0xFFF5F5F7),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = Color(0xFFFF3B30)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F7)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon and switch
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFE3F2FD)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    text = localName,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = getScheduleDescription(item),
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                        
                        Switch(
                            checked = item.isEnabled,
                            onCheckedChange = { onUpdate(item.copy(isEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF007AFF),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE5E5EA)
                            )
                        )
                    }
                }
            }
            
            // Name field
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Название",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = localName,
                            onValueChange = { localName = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE5E5EA),
                                focusedBorderColor = Color(0xFF007AFF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
            
            // Texts section
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Тексты (${localTexts.size})",
                            fontSize = 14.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
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
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color(0xFFE5E5EA),
                                        focusedBorderColor = Color(0xFF007AFF)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
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
                                            tint = Color(0xFFFF3B30)
                                        )
                                    }
                                }
                            }
                            
                            if (index < localTexts.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextButton(
                            onClick = { localTexts = localTexts + "" },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF007AFF)
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Добавить")
                        }
                    }
                }
            }
            
            // Texts per notification
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.clickable { /* TODO: открыть выбор */ }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Сколько текстов показывать",
                            fontSize = 16.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${item.textsPerNotification}",
                                fontSize = 16.sp,
                                color = Color(0xFF8E8E93)
                            )
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFC7C7CC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Time
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.clickable { /* TODO: открыть настройки времени */ }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFF007AFF)
                            )
                            Text(
                                "Время",
                                fontSize = 16.sp
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                getTimeDescription(item),
                                fontSize = 16.sp,
                                color = Color(0xFF8E8E93)
                            )
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFC7C7CC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Preview
            item {
                Column {
                    Text(
                        "Превью",
                        fontSize = 14.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    localName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            localTexts.take(item.textsPerNotification).forEach { text ->
                                Text(
                                    text = text.ifEmpty { "Новый текст" },
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }
            
            // Action buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onTest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Отправить тест",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить уведомление?") },
            text = { Text("\"${item.name}\" будет удалено навсегда") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF3B30)
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

fun getScheduleDescription(item: NotificationItem): String {
    val schedule = when (item.scheduleMode) {
        ScheduleMode.DAILY -> if (item.intervalDays == 1) "Каждый день" else "Каждые ${item.intervalDays} дн"
        ScheduleMode.WEEKLY -> "Еженедельно"
        ScheduleMode.MONTHLY_BY_DATE -> "Ежемесячно"
        ScheduleMode.MONTHLY_BY_WEEKDAY -> "Ежемесячно"
        ScheduleMode.YEARLY -> "Ежегодно"
        ScheduleMode.SPECIFIC_DATE -> "Конкретная дата"
        ScheduleMode.DATE_RANGE -> "Диапазон"
    }
    
    val time = if (item.useExactTime && item.exactTimes.isNotEmpty()) {
        String.format("%02d:%02d", item.exactTimes.first().hour, item.exactTimes.first().minute)
    } else {
        String.format("%02d:%02d", item.startHour, item.startMinute)
    }
    
    return "$schedule • $time"
}

fun getTimeDescription(item: NotificationItem): String {
    return if (item.useExactTime && item.exactTimes.isNotEmpty()) {
        item.exactTimes.take(2).joinToString(", ") { time ->
            String.format("%02d:%02d", time.hour, time.minute)
        } + if (item.exactTimes.size > 2) "..." else ""
    } else {
        String.format("%02d:%02d", item.startHour, item.startMinute)
    }
}
