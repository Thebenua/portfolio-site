package ru.campuslife.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Immutable
data class Event(
    val id: Int,
    val title: String,
    val category: String,
    val date: LocalDateTime,
    val place: String,
    val description: String,
    val subtitle: String,
    val tint: Color,
    val photoUri: String = ""
)

private fun seedEvents(day: LocalDate): List<Event> = listOf(
    Event(
        id = 1,
        title = "Ночь науки",
        category = "Наука",
        date = day.plusDays(2).atTime(16, 30),
        place = "Актовый зал",
        description = "Опыты, которые можно не только увидеть, но и повторить. Исследуем свет, звук и неожиданные свойства привычных вещей.",
        subtitle = "Опыты и открытия рядом",
        tint = Color(0xFF333C48)
    ),
    Event(
        id = 2,
        title = "Творческая мастерская",
        category = "Творчество",
        date = day.plusDays(4).atTime(15, 0),
        place = "Кабинет 204",
        description = "Встреча для тех, кто любит придумывать и создавать. Материалы будут на месте, опыт не требуется.",
        subtitle = "Идеи, которым нужно место",
        tint = Color(0xFF423837)
    ),
    Event(
        id = 3,
        title = "Школьный турнир",
        category = "Спорт",
        date = day.plusDays(6).atTime(14, 30),
        place = "Спортивный зал",
        description = "Собираем команды для дружеских соревнований. Приходи участвовать или поддержать друзей.",
        subtitle = "Играем одной командой",
        tint = Color(0xFF2F3E3A)
    ),
    Event(
        id = 4,
        title = "Клуб создателей",
        category = "Технологии",
        date = day.plusDays(9).atTime(16, 0),
        place = "Медиатека",
        description = "Обсуждаем идеи приложений и пробуем создавать небольшие проекты вместе. Можно прийти без подготовки.",
        subtitle = "От первой идеи к проекту",
        tint = Color(0xFF2E3645)
    ),
    Event(
        id = 5,
        title = "Открытый микрофон",
        category = "Творчество",
        date = day.plusDays(12).atTime(17, 0),
        place = "Библиотека",
        description = "Музыка, стихи и истории. Выступай или просто приходи послушать в уютной компании.",
        subtitle = "Вечер, где слышно каждого",
        tint = Color(0xFF3F3632)
    )
)

class MainActivity : ComponentActivity() {
    private var pendingReminder: Pair<Event, Int>? = null
    private var permissionVersion by mutableIntStateOf(0)

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingReminder
        pendingReminder = null
        if (granted && pending != null) {
            saveReminder(this, pending.first, pending.second)
            permissionVersion++
        } else if (!granted) {
            Toast.makeText(
                this,
                "Разреши уведомления, чтобы получать напоминания",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        preferSmoothDisplayMode()

        setContent {
            CampusTheme {
                CampusApp(
                    permissionVersion = permissionVersion,
                    onRemind = { event, minutes ->
                        if (minutes == 0) {
                            cancelReminder(this, event)
                            true
                        } else if (event.date.minusMinutes(minutes.toLong()).isBefore(LocalDateTime.now())) {
                            Toast.makeText(this, "Это время напоминания уже прошло", Toast.LENGTH_SHORT).show()
                            false
                        } else if (
                            Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingReminder = event to minutes
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            false
                        } else {
                            saveReminder(this, event, minutes)
                            true
                        }
                    }
                )
            }
        }
    }
}

private fun reminderIntent(context: Context, event: Event): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java)
        .putExtra("title", event.title)
        .putExtra("place", event.place)
        .putExtra("id", event.id)
    return PendingIntent.getBroadcast(
        context,
        event.id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun saveReminder(context: Context, event: Event, minutes: Int) {
    val time = event.date
        .minusMinutes(minutes.toLong())
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    try {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .set(AlarmManager.RTC_WAKEUP, time, reminderIntent(context, event))
    } catch (_: SecurityException) {
    }
    context.getSharedPreferences("campus", Context.MODE_PRIVATE)
        .edit()
        .putInt("reminder_${event.id}", minutes)
        .apply()
}

private fun cancelReminder(context: Context, event: Event) {
    try {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .cancel(reminderIntent(context, event))
    } catch (_: SecurityException) {
    }
    context.getSharedPreferences("campus", Context.MODE_PRIVATE)
        .edit()
        .remove("reminder_${event.id}")
        .apply()
}

private fun loadCustom(context: Context): List<Event> = try {
    val raw = context.getSharedPreferences("campus", Context.MODE_PRIVATE)
        .getString("custom_events", "[]")
    val array = JSONArray(raw)
    (0 until array.length()).map { index ->
        val obj = array.getJSONObject(index)
        Event(
            id = obj.getInt("id"),
            title = obj.getString("title"),
            category = obj.getString("category"),
            date = LocalDateTime.parse(obj.getString("date")),
            place = obj.getString("place"),
            description = obj.getString("description"),
            subtitle = obj.getString("subtitle"),
            tint = Color(0xFF333C48),
            photoUri = obj.optString("photoUri", "")
        )
    }
} catch (_: Exception) {
    emptyList()
}

private fun saveCustom(context: Context, event: Event) {
    val prefs = context.getSharedPreferences("campus", Context.MODE_PRIVATE)
    val array = try {
        JSONArray(prefs.getString("custom_events", "[]"))
    } catch (_: Exception) {
        JSONArray()
    }
    array.put(
        JSONObject()
            .put("id", event.id)
            .put("title", event.title)
            .put("category", event.category)
            .put("date", event.date.toString())
            .put("place", event.place)
            .put("description", event.description)
            .put("subtitle", event.subtitle)
            .put("photoUri", event.photoUri)
    )
    prefs.edit().putString("custom_events", array.toString()).apply()
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("campus", Context.MODE_PRIVATE)
        val seed = prefs.getLong("seed_day", 0L)
        val all = (if (seed != 0L) seedEvents(LocalDate.ofEpochDay(seed)) else emptyList()) + loadCustom(context)
        all.forEach { event ->
            val minutes = prefs.getInt("reminder_${event.id}", 0)
            if (minutes > 0 && event.date.minusMinutes(minutes.toLong()).isAfter(LocalDateTime.now())) {
                saveReminder(context, event, minutes)
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel("events", "Мероприятия", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val launch = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, "events")
            .setSmallIcon(R.drawable.ic_stat_campus)
            .setContentTitle("Скоро: ${intent.getStringExtra("title")}")
            .setContentText(intent.getStringExtra("place"))
            .setContentIntent(launch)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(intent.getIntExtra("id", 0), notification)
        } catch (_: SecurityException) {
        }
    }
}

private val RootBgBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF161920),
        CampusColors.DeepObsidian,
        Color(0xFF0E1014)
    )
)

private val BottomScrimBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        CampusColors.DeepObsidian.copy(alpha = 0.68f),
        CampusColors.DeepObsidian.copy(alpha = 0.94f)
    )
)

@Composable
private fun CampusApp(
    permissionVersion: Int,
    onRemind: (Event, Int) -> Boolean
) {
    val ctx = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val prefs = remember { ctx.getSharedPreferences("campus", Context.MODE_PRIVATE) }

    // Фоновый прогрев обложек сразу после старта для мгновенного скролла 120 Гц без рывков
    LaunchedEffect(Unit) {
        CampusImageCache.preloadDefaults(ctx)
    }

    val seed = remember {
        val start = prefs.getLong("seed_day", 0L).let { saved ->
            if (saved == 0L) {
                LocalDate.now().toEpochDay().also { value ->
                    prefs.edit().putLong("seed_day", value).apply()
                }
            } else {
                saved
            }
        }
        seedEvents(LocalDate.ofEpochDay(start))
    }

    var custom by remember { mutableStateOf(loadCustom(ctx)) }
    val allEvents = remember(seed, custom) { (seed + custom).sortedBy { it.date } }

    var favorites by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet())
    }
    var joined by remember {
        mutableStateOf(prefs.getStringSet("joined", emptySet()).orEmpty().toSet())
    }
    var reminderVersion by rememberSaveable { mutableIntStateOf(0) }

    // Все пользовательские состояния сохраняются при повороте экрана и смене размера окна (rememberSaveable)
    var activeTabName by rememberSaveable { mutableStateOf(CampusTab.Poster.name) }
    val activeTab = remember(activeTabName) {
        CampusTab.entries.firstOrNull { it.name == activeTabName } ?: CampusTab.Poster
    }

    // "home", "create" или "detail:<id>"
    var routeKey by rememberSaveable { mutableStateOf("home") }

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Все") }
    var dateFilter by rememberSaveable { mutableStateOf("Все даты") }
    var exactDateEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    val exactDate = remember(exactDateEpoch) { exactDateEpoch?.let(LocalDate::ofEpochDay) }

    var showFilterCalendar by rememberSaveable { mutableStateOf(false) }

    // Независимая позиция прокрутки для каждой из 3 вкладок — скролл никогда не сбрасывается при возврате из деталей!
    val posterListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()
    val bookingsListState = rememberLazyListState()

    fun toggleStoredSet(current: Set<String>, key: String, id: Int): Set<String> {
        val idStr = id.toString()
        val updated = if (idStr in current) current - idStr else current + idStr
        prefs.edit().putStringSet(key, updated).apply()
        return updated
    }

    val isOverlayOpen = routeKey != "home"
    BackHandler(enabled = isOverlayOpen) {
        routeKey = "home"
    }

    val detailEvent = remember(routeKey, allEvents) {
        if (routeKey.startsWith("detail:")) {
            val id = routeKey.removePrefix("detail:").toIntOrNull()
            allEvents.firstOrNull { it.id == id }
        } else {
            null
        }
    }

    Surface(
        color = CampusColors.DeepObsidian,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RootBgBrush)
        ) {
            // Слой 1: Домашний экран и парящая нижняя навигация остаются смонтированными постоянно.
            // Это полностью исключает мерцание, пересоздание списков, сброс скролла и прыжки нижней капсулы!
            val homeLayerAlpha by animateFloatAsState(
                targetValue = if (isOverlayOpen) 0.45f else 1f,
                animationSpec = if (reducedMotion) snap() else tween(220, easing = CampusMotion.EmphasizedDecelerate),
                label = "homeDimAlpha"
            )
            val homeLayerScale by animateFloatAsState(
                targetValue = if (isOverlayOpen && !reducedMotion) 0.975f else 1f,
                animationSpec = if (reducedMotion) snap() else tween(240, easing = CampusMotion.EmphasizedDecelerate),
                label = "homeDepthScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = homeLayerAlpha
                        scaleX = homeLayerScale
                        scaleY = homeLayerScale
                    }
            ) {
                HomeTabsContent(
                    allEvents = allEvents,
                    activeTab = activeTab,
                    favorites = favorites,
                    joined = joined,
                    reminderVersion = reminderVersion,
                    permissionVersion = permissionVersion,
                    posterListState = posterListState,
                    favoritesListState = favoritesListState,
                    bookingsListState = bookingsListState,
                    query = query,
                    category = category,
                    dateFilter = dateFilter,
                    exactDate = exactDate,
                    onQueryChange = { query = it },
                    onCategoryChange = { category = it },
                    onDateFilterChange = {
                        dateFilter = it
                        exactDateEpoch = null
                    },
                    onOpenDateModal = { showFilterCalendar = true },
                    onClearExactDate = { exactDateEpoch = null },
                    onResetFilters = {
                        query = ""
                        category = "Все"
                        dateFilter = "Все даты"
                        exactDateEpoch = null
                    },
                    onSwitchTab = { activeTabName = it.name },
                    onOpenCreate = { routeKey = "create" },
                    onOpenEvent = { ev -> routeKey = "detail:${ev.id}" },
                    onToggleFavorite = { ev ->
                        favorites = toggleStoredSet(favorites, "favorites", ev.id)
                    },
                    onToggleJoin = { ev ->
                        val wasJoined = ev.id.toString() in joined
                        joined = toggleStoredSet(joined, "joined", ev.id)
                        if (wasJoined) {
                            cancelReminder(ctx, ev)
                            reminderVersion++
                        }
                    }
                )

                // Парящая капсула всегда неподвижно закреплена внизу домашнего слоя (без прыжков при вводе в поиск и переходах)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(BottomScrimBrush)
                ) {
                    FloatingBottomNav(
                        selectedTab = activeTab,
                        favoritesCount = favorites.size,
                        bookingsCount = joined.size,
                        onSelect = { selected ->
                            if (routeKey == "home") {
                                activeTabName = selected.name
                            }
                        }
                    )
                }
            }

            // Слой 2: Плавный экран деталей события поверх домашнего слоя (240 мс без моргания)
            AnimatedVisibility(
                visible = detailEvent != null,
                enter = if (reducedMotion) {
                    fadeIn(snap())
                } else {
                    fadeIn(tween(210, easing = CampusMotion.EmphasizedDecelerate)) +
                        slideInVertically(tween(250, easing = CampusMotion.EmphasizedDecelerate)) { it / 16 }
                },
                exit = if (reducedMotion) {
                    fadeOut(snap())
                } else {
                    fadeOut(tween(175)) +
                        slideOutVertically(tween(200, easing = CampusMotion.EmphasizedDecelerate)) { it / 18 }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                // Помним последнее событие во время анимации закрытия, чтобы контент не пропадал до конца кадра
                var cachedEvent by remember { mutableStateOf(detailEvent) }
                if (detailEvent != null) cachedEvent = detailEvent
                val target = cachedEvent

                if (target != null) {
                    val reminderMinutes = remember(target.id, reminderVersion, permissionVersion) {
                        prefs.getInt("reminder_${target.id}", 0)
                    }
                    EventDetailScreen(
                        event = target,
                        isFavorite = target.id.toString() in favorites,
                        isJoined = target.id.toString() in joined,
                        reminderMinutes = reminderMinutes,
                        onBack = { routeKey = "home" },
                        onToggleFavorite = {
                            favorites = toggleStoredSet(favorites, "favorites", target.id)
                        },
                        onToggleJoin = {
                            joined = toggleStoredSet(joined, "joined", target.id)
                        },
                        onSelectReminder = { minutes ->
                            if (onRemind(target, minutes)) {
                                reminderVersion++
                            }
                        }
                    )
                }
            }

            // Слой 3: Плавный экран создания нового события
            AnimatedVisibility(
                visible = routeKey == "create",
                enter = if (reducedMotion) {
                    fadeIn(snap())
                } else {
                    fadeIn(tween(210, easing = CampusMotion.EmphasizedDecelerate)) +
                        slideInVertically(tween(250, easing = CampusMotion.EmphasizedDecelerate)) { it / 14 }
                },
                exit = if (reducedMotion) {
                    fadeOut(snap())
                } else {
                    fadeOut(tween(170)) +
                        slideOutVertically(tween(200, easing = CampusMotion.EmphasizedDecelerate)) { it / 16 }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                CreateEventScreen(
                    onBack = { routeKey = "home" },
                    onSave = { newEvent ->
                        saveCustom(ctx, newEvent)
                        custom = loadCustom(ctx)
                        routeKey = "detail:${newEvent.id}"
                    }
                )
            }

            // Фирменный модальный календарь для фильтра точной даты
            if (showFilterCalendar) {
                CampusDateModal(
                    initialDate = exactDate ?: LocalDate.now(),
                    allowClear = exactDate != null,
                    onDismiss = { showFilterCalendar = false },
                    onClear = {
                        exactDateEpoch = null
                        showFilterCalendar = false
                    },
                    onConfirm = { picked ->
                        exactDateEpoch = picked.toEpochDay()
                        dateFilter = "Все даты"
                        showFilterCalendar = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeTabsContent(
    allEvents: List<Event>,
    activeTab: CampusTab,
    favorites: Set<String>,
    joined: Set<String>,
    reminderVersion: Int,
    permissionVersion: Int,
    posterListState: LazyListState,
    favoritesListState: LazyListState,
    bookingsListState: LazyListState,
    query: String,
    category: String,
    dateFilter: String,
    exactDate: LocalDate?,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateFilterChange: (String) -> Unit,
    onOpenDateModal: () -> Unit,
    onClearExactDate: () -> Unit,
    onResetFilters: () -> Unit,
    onSwitchTab: (CampusTab) -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEvent: (Event) -> Unit,
    onToggleFavorite: (Event) -> Unit,
    onToggleJoin: (Event) -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("campus", Context.MODE_PRIVATE) }
    val reducedMotion = LocalReducedMotion.current

    val hasActiveFilters = query.isNotBlank() || category != "Все" || dateFilter != "Все даты" || exactDate != null

    val posterFiltered = remember(allEvents, query, category, dateFilter, exactDate) {
        val trimmedQuery = query.trim()
        val today = LocalDate.now()
        allEvents.filter { ev ->
            val catOk = category == "Все" || ev.category == category
            val queryOk = trimmedQuery.isEmpty() ||
                ev.title.contains(trimmedQuery, ignoreCase = true) ||
                ev.place.contains(trimmedQuery, ignoreCase = true) ||
                ev.category.contains(trimmedQuery, ignoreCase = true) ||
                ev.subtitle.contains(trimmedQuery, ignoreCase = true)
            val exactOk = exactDate == null || ev.date.toLocalDate() == exactDate
            val rangeOk = dateFilter == "Все даты" || (
                !ev.date.toLocalDate().isBefore(today) &&
                    ev.date.toLocalDate() <= today.plusDays(if (dateFilter == "7 дней") 7 else 30)
            )
            catOk && queryOk && exactOk && rangeOk
        }
    }
    val favoritesList = remember(allEvents, favorites) {
        allEvents.filter { it.id.toString() in favorites }
    }
    val bookingsList = remember(allEvents, joined) {
        allEvents.filter { it.id.toString() in joined }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        val isCompactScreen = maxWidth < 360.dp
        val isTabletWide = maxWidth >= 680.dp
        val horizontalPad = when {
            isCompactScreen -> 14.dp
            isTabletWide -> 32.dp
            else -> 20.dp
        }
        val contentMaxWidth = if (isTabletWide) 920.dp else 680.dp

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxSize()
            ) {
                // Шапка с логотипом Campus Life и кнопкой создания события
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizontalPad, end = horizontalPad, top = 14.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        CampusWordmark()
                        Spacer(Modifier.height(10.dp))
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                if (reducedMotion) {
                                    fadeIn(snap()) togetherWith fadeOut(snap())
                                } else {
                                    fadeIn(tween(190, easing = CampusMotion.EmphasizedDecelerate)) togetherWith
                                        fadeOut(tween(140))
                                }
                            },
                            label = "headerTitleAnim"
                        ) { tab ->
                            Text(
                                text = when (tab) {
                                    CampusTab.Poster -> "События кампуса"
                                    CampusTab.Favorites -> "Сохранённые встречи"
                                    CampusTab.Bookings -> "Мои записи и билеты"
                                },
                                style = if (isCompactScreen) {
                                    MaterialTheme.typography.headlineMedium
                                } else {
                                    MaterialTheme.typography.headlineLarge
                                },
                                color = CampusColors.Milk
                            )
                        }
                    }

                    GlassIconButton(
                        onClick = onOpenCreate,
                        size = 44.dp,
                        accent = true,
                        contentDescriptionLabel = "Создать событие"
                    ) {
                        PlusGlyph(color = CampusColors.WarmSand, size = 20.dp)
                    }
                }

                // Направленный переход между вкладками в зависимости от их порядка (Афиша <-> Избранное <-> Записи)
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                            (fadeIn(tween(210, easing = CampusMotion.EmphasizedDecelerate)) +
                                slideInHorizontally(tween(235, easing = CampusMotion.EmphasizedDecelerate)) { (it / 18) * dir })
                                .togetherWith(
                                    fadeOut(tween(150)) +
                                        slideOutHorizontally(tween(190, easing = CampusMotion.EmphasizedDecelerate)) { -(it / 22) * dir }
                                )
                        }
                    },
                    label = "tabBodyDirectionalTransition",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    val listForTab = when (tab) {
                        CampusTab.Poster -> posterFiltered
                        CampusTab.Favorites -> favoritesList
                        CampusTab.Bookings -> bookingsList
                    }
                    val stateForTab = when (tab) {
                        CampusTab.Poster -> posterListState
                        CampusTab.Favorites -> favoritesListState
                        CampusTab.Bookings -> bookingsListState
                    }

                    LazyColumn(
                        state = stateForTab,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = horizontalPad,
                            end = horizontalPad,
                            top = 4.dp,
                            bottom = 132.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (tab == CampusTab.Poster) {
                            item(key = "poster_controls", contentType = "controls") {
                                PosterSearchAndFilters(
                                    query = query,
                                    category = category,
                                    dateFilter = dateFilter,
                                    exactDate = exactDate,
                                    hasActiveFilters = hasActiveFilters,
                                    onQueryChange = onQueryChange,
                                    onCategoryChange = onCategoryChange,
                                    onDateFilterChange = onDateFilterChange,
                                    onPickExactDate = onOpenDateModal,
                                    onClearExactDate = onClearExactDate,
                                    onResetAll = onResetFilters
                                )
                            }
                        }

                        item(key = "section_summary_${tab.name}", contentType = "summary") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (tab) {
                                        CampusTab.Poster -> if (hasActiveFilters) "Результаты подбора" else "Ближайшие анонсы"
                                        CampusTab.Favorites -> "Отмечено сердечком"
                                        CampusTab.Bookings -> "Активные регистрации"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = CampusColors.SlateMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                )

                                AnimatedContent(
                                    targetState = listForTab.size,
                                    transitionSpec = {
                                        if (reducedMotion) {
                                            fadeIn(snap()) togetherWith fadeOut(snap())
                                        } else {
                                            (fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.88f))
                                                .togetherWith(fadeOut(tween(120)))
                                        }
                                    },
                                    label = "countPillAnim"
                                ) { count ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(percent = 50))
                                            .background(CampusColors.Graphite)
                                            .border(1.dp, CampusColors.Hairline, RoundedCornerShape(percent = 50))
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CampusColors.SkyMuted
                                        )
                                    }
                                }
                            }
                        }

                        if (listForTab.isEmpty()) {
                            item(key = "empty_${tab.name}", contentType = "empty") {
                                CampusEmptyState(
                                    tab = tab,
                                    hasActiveFilters = hasActiveFilters,
                                    onPrimaryAction = {
                                        when {
                                            tab == CampusTab.Poster && hasActiveFilters -> onResetFilters()
                                            tab == CampusTab.Poster -> onOpenCreate()
                                            else -> onSwitchTab(CampusTab.Poster)
                                        }
                                    }
                                )
                            }
                        } else if (tab == CampusTab.Bookings) {
                            items(
                                items = listForTab,
                                key = { "booking_${it.id}" },
                                contentType = { "booking_card" }
                            ) { ev ->
                                val reminderMin = remember(ev.id, reminderVersion, permissionVersion) {
                                    prefs.getInt("reminder_${ev.id}", 0)
                                }
                                BookingTicketCard(
                                    event = ev,
                                    reminderMinutes = reminderMin,
                                    onClick = { onOpenEvent(ev) },
                                    onCancelBooking = { onToggleJoin(ev) },
                                    modifier = if (reducedMotion) {
                                        Modifier
                                    } else {
                                        Modifier.animateItem(
                                            fadeInSpec = tween(200),
                                            fadeOutSpec = tween(160),
                                            placementSpec = CampusMotion.navSpring()
                                        )
                                    }
                                )
                            }
                        } else if (isTabletWide) {
                            // На широких экранах планшетов — адаптивная двухколоночная сетка карточек
                            val rows = listForTab.chunked(2)
                            items(
                                items = rows,
                                key = { pair -> "row_${tab.name}_${pair.first().id}" },
                                contentType = { "poster_row" }
                            ) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val first = pair[0]
                                    PosterEventCard(
                                        event = first,
                                        isFavorite = first.id.toString() in favorites,
                                        isJoined = first.id.toString() in joined,
                                        onClick = { onOpenEvent(first) },
                                        onToggleFavorite = { onToggleFavorite(first) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (pair.size > 1) {
                                        val second = pair[1]
                                        PosterEventCard(
                                            event = second,
                                            isFavorite = second.id.toString() in favorites,
                                            isJoined = second.id.toString() in joined,
                                            onClick = { onOpenEvent(second) },
                                            onToggleFavorite = { onToggleFavorite(second) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(
                                items = listForTab,
                                key = { "event_${tab.name}_${it.id}" },
                                contentType = { "poster_card" }
                            ) { ev ->
                                PosterEventCard(
                                    event = ev,
                                    isFavorite = ev.id.toString() in favorites,
                                    isJoined = ev.id.toString() in joined,
                                    onClick = { onOpenEvent(ev) },
                                    onToggleFavorite = { onToggleFavorite(ev) },
                                    modifier = if (reducedMotion) {
                                        Modifier
                                    } else {
                                        Modifier.animateItem(
                                            fadeInSpec = tween(200),
                                            fadeOutSpec = tween(160),
                                            placementSpec = CampusMotion.navSpring()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterSearchAndFilters(
    query: String,
    category: String,
    dateFilter: String,
    exactDate: LocalDate?,
    hasActiveFilters: Boolean,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateFilterChange: (String) -> Unit,
    onPickExactDate: () -> Unit,
    onClearExactDate: () -> Unit,
    onResetAll: () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
            placeholder = {
                Text(
                    text = "Поиск по названию, месту или теме",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CampusColors.SlateMuted
                )
            },
            leadingIcon = {
                SearchGlyph(color = CampusColors.SkyMuted, size = 19.dp)
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(150)) + scaleIn(tween(150)),
                    exit = if (reducedMotion) fadeOut(snap()) else fadeOut(tween(120)) + scaleOut(tween(120))
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CampusColors.GraphiteElevated)
                            .pressableScale(pressedScale = 0.88f, debounce = false, onClick = { onQueryChange("") }),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseGlyph(color = CampusColors.Milk, size = 13.dp)
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CampusColors.SkyMuted.copy(alpha = 0.55f),
                unfocusedBorderColor = CampusColors.Hairline,
                focusedContainerColor = CampusColors.Graphite,
                unfocusedContainerColor = CampusColors.Graphite,
                cursorColor = CampusColors.WarmSand
            )
        )

        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowItems(
                items = listOf("Все", "Наука", "Творчество", "Спорт", "Технологии"),
                key = { it }
            ) { item ->
                FilterPill(
                    text = item,
                    selected = item == category,
                    onClick = { onCategoryChange(item) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowItems(
                items = listOf("Все даты", "7 дней", "30 дней"),
                key = { it }
            ) { period ->
                FilterPill(
                    text = period,
                    selected = period == dateFilter && exactDate == null,
                    onClick = { onDateFilterChange(period) }
                )
            }

            item(key = "exact_date_chip") {
                val dateLabel = exactDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy", RuLocale))
                    ?: "Точная дата"
                FilterPill(
                    text = dateLabel,
                    selected = exactDate != null,
                    onClick = onPickExactDate,
                    leadingContent = {
                        NavPosterIcon(
                            color = if (exactDate != null) CampusColors.DeepObsidian else CampusColors.WarmSand,
                            active = false,
                            size = 15.dp
                        )
                    }
                )
            }

            if (exactDate != null) {
                item(key = "clear_exact_date") {
                    Text(
                        text = "Сбросить дату",
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .pressableScale(pressedScale = 0.94f, debounce = false, onClick = onClearExactDate)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = CampusColors.WarmSand
                    )
                }
            } else if (hasActiveFilters) {
                item(key = "reset_all_filters") {
                    Text(
                        text = "Сбросить всё",
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .pressableScale(pressedScale = 0.94f, debounce = false, onClick = onResetAll)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = CampusColors.SlateMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDetailScreen(
    event: Event,
    isFavorite: Boolean,
    isJoined: Boolean,
    reminderMinutes: Int,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleJoin: () -> Unit,
    onSelectReminder: (Int) -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val haptics = LocalHapticFeedback.current

    val btnContainer by animateColorAsState(
        targetValue = if (isJoined) CampusColors.GraphiteElevated else CampusColors.WarmSand,
        animationSpec = if (reducedMotion) snap() else tween(200, easing = CampusMotion.EmphasizedDecelerate),
        label = "joinBtnBg"
    )
    val btnContent by animateColorAsState(
        targetValue = if (isJoined) CampusColors.Milk else CampusColors.DeepObsidian,
        animationSpec = if (reducedMotion) snap() else tween(200, easing = CampusMotion.EmphasizedDecelerate),
        label = "joinBtnContent"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(RootBgBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val horizontalPad = when {
            maxWidth < 360.dp -> 14.dp
            maxWidth >= 600.dp -> 32.dp
            else -> 20.dp
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(CampusColors.Graphite)
                            .border(1.dp, CampusColors.Hairline, RoundedCornerShape(14.dp))
                            .pressableScale(pressedScale = 0.94f, onClick = onBack)
                            .padding(start = 10.dp, end = 15.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BackArrowGlyph(color = CampusColors.Milk, size = 18.dp)
                        Text(
                            text = "Назад",
                            style = MaterialTheme.typography.labelLarge,
                            color = CampusColors.Milk
                        )
                    }

                    FavoriteHeartButton(
                        isFavorite = isFavorite,
                        onToggle = onToggleFavorite,
                        size = 42.dp,
                        iconSize = 20.dp,
                        glassCircle = false
                    )
                }

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(236.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(event.tint)
                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(26.dp))
                ) {
                    AsyncEventCover(event = event, maxDimension = 1280)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color(0x280D0F13),
                                    0.55f to Color(0x240D0F13),
                                    1.0f to Color(0xDB111317)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xBA16191F))
                                .border(0.8.dp, CampusColors.Milk.copy(alpha = 0.18f), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = event.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = CampusColors.WarmSand
                            )
                        }

                        AnimatedVisibility(
                            visible = isJoined,
                            enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.9f),
                            exit = if (reducedMotion) fadeOut(snap()) else fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Color(0xC917241F))
                                    .border(0.8.dp, CampusColors.Success.copy(alpha = 0.42f), RoundedCornerShape(percent = 50))
                                    .padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CheckGlyph(color = CampusColors.Success, size = 13.dp)
                                Text(
                                    text = "Ты в списке участников",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CampusColors.Success
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = CampusColors.Milk
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = event.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CampusColors.SlateMuted
                )

                Spacer(Modifier.height(20.dp))

                DetailMetaTile(
                    icon = { ClockGlyph(color = CampusColors.WarmSand, size = 18.dp) },
                    label = "Дата и время",
                    value = "${event.date.format(FullDateFmt)} · ${event.date.format(TimeFmt)}"
                )

                Spacer(Modifier.height(10.dp))

                DetailMetaTile(
                    icon = { PinGlyph(color = CampusColors.SkyMuted, size = 18.dp) },
                    label = "Место проведения",
                    value = event.place
                )

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "О встрече",
                    style = MaterialTheme.typography.titleMedium,
                    color = CampusColors.Milk
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CampusColors.Milk.copy(alpha = 0.86f)
                )

                Spacer(Modifier.height(26.dp))

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleJoin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = btnContainer,
                        contentColor = btnContent
                    )
                ) {
                    AnimatedContent(
                        targetState = isJoined,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(snap()) togetherWith fadeOut(snap())
                            } else {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(130))
                            }
                        },
                        label = "joinBtnLabelAnim"
                    ) { joinedState ->
                        Text(
                            text = if (joinedState) "Отменить запись" else "Записаться на событие",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CampusColors.Graphite)
                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BellGlyph(color = CampusColors.SkyMuted, size = 17.dp)
                        Text(
                            text = "Напоминание на телефон",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = CampusColors.Milk
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems(
                            items = listOf(
                                0 to "Нет",
                                10 to "За 10 мин",
                                60 to "За 1 час",
                                1440 to "За 1 день"
                            ),
                            key = { it.first }
                        ) { (minutes, label) ->
                            FilterPill(
                                text = label,
                                selected = reminderMinutes == minutes,
                                onClick = { onSelectReminder(minutes) }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = reminderMinutes > 0,
                        enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = if (reducedMotion) fadeOut(snap()) else fadeOut(tween(140)) + shrinkVertically(tween(150))
                    ) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Уведомление придёт автоматически; возможна небольшая задержка из-за энергосбережения системы.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = CampusColors.SlateMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Данные сохраняются локально на этом устройстве и доступны без сети.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = CampusColors.SlateMuted.copy(alpha = 0.78f)
                )

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CreateEventScreen(
    onBack: () -> Unit,
    onSave: (Event) -> Unit
) {
    val ctx = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val submitGuard = remember { RapidTapGuard(600L) }

    var title by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var desc by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Наука") }
    var dateIso by rememberSaveable {
        mutableStateOf(
            LocalDateTime.now()
                .plusDays(1)
                .withHour(16)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString()
        )
    }
    val date = remember(dateIso) { LocalDateTime.parse(dateIso) }

    var error by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf("") }

    var showDateModal by rememberSaveable { mutableStateOf(false) }
    var showTimeModal by rememberSaveable { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                photoUri = uri.toString()
            } catch (_: SecurityException) {
                Toast.makeText(
                    ctx,
                    "Не удалось сохранить доступ к фотографии",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var previewBitmap by remember(photoUri) {
        mutableStateOf(
            if (photoUri.isNotBlank()) CampusImageCache.getCached("uri:1280:$photoUri") else null
        )
    }
    LaunchedEffect(photoUri) {
        if (photoUri.isNotBlank() && previewBitmap == null) {
            previewBitmap = CampusImageCache.loadUriBitmap(ctx, photoUri, maxDimension = 1280)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CampusColors.SkyMuted.copy(alpha = 0.6f),
        unfocusedBorderColor = CampusColors.Hairline,
        focusedContainerColor = CampusColors.Graphite,
        unfocusedContainerColor = CampusColors.Graphite,
        focusedLabelColor = CampusColors.SkyMuted,
        unfocusedLabelColor = CampusColors.SlateMuted,
        cursorColor = CampusColors.WarmSand
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(RootBgBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val horizontalPad = when {
            maxWidth < 360.dp -> 14.dp
            maxWidth >= 600.dp -> 32.dp
            else -> 20.dp
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CampusColors.Graphite)
                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(14.dp))
                        .pressableScale(pressedScale = 0.94f, onClick = onBack)
                        .padding(start = 10.dp, end = 15.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BackArrowGlyph(color = CampusColors.Milk, size = 18.dp)
                    Text(
                        text = "К афише",
                        style = MaterialTheme.typography.labelLarge,
                        color = CampusColors.Milk
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Новое событие",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CampusColors.Milk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Анонс сохранится на этом устройстве и сразу появится в общей ленте кампуса.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CampusColors.SlateMuted
                )

                Spacer(Modifier.height(20.dp))

                val bmp: ImageBitmap? = previewBitmap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (bmp != null) 172.dp else 94.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CampusColors.Graphite)
                        .border(1.dp, CampusColors.HairlineStrong, RoundedCornerShape(20.dp))
                        .pressableScale(
                            pressedScale = 0.985f,
                            onClick = { photoPicker.launch(arrayOf("image/*")) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Выбранная обложка",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.38f))
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                if (bmp != null) Color(0xCC16191F) else Color.Transparent
                            )
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        PhotoGlyph(color = CampusColors.WarmSand, size = 20.dp)
                        Text(
                            text = if (photoUri.isEmpty()) {
                                "Выбрать фотообложку события"
                            } else {
                                "Обложка выбрана · нажми, чтобы заменить"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = CampusColors.Milk
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (error.isNotEmpty()) error = ""
                    },
                    label = { Text("Название события") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = place,
                    onValueChange = {
                        place = it
                        if (error.isNotEmpty()) error = ""
                    },
                    label = { Text("Место (аудитория, зал, площадка)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = {
                        desc = it
                        if (error.isNotEmpty()) error = ""
                    },
                    label = { Text("Описание и детали участия") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Направление",
                    style = MaterialTheme.typography.labelLarge,
                    color = CampusColors.SlateMuted
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems(
                        items = listOf("Наука", "Творчество", "Спорт", "Технологии"),
                        key = { it }
                    ) { item ->
                        FilterPill(
                            text = item,
                            selected = item == category,
                            onClick = { category = item }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Дата и время начала",
                    style = MaterialTheme.typography.labelLarge,
                    color = CampusColors.SlateMuted
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterPill(
                        text = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", RuLocale)),
                        selected = false,
                        onClick = { showDateModal = true },
                        leadingContent = {
                            NavPosterIcon(color = CampusColors.WarmSand, active = false, size = 15.dp)
                        }
                    )

                    FilterPill(
                        text = date.format(TimeFmt),
                        selected = false,
                        onClick = { showTimeModal = true },
                        leadingContent = {
                            ClockGlyph(color = CampusColors.SkyMuted, size = 15.dp)
                        }
                    )
                }

                Spacer(Modifier.height(18.dp))

                AnimatedVisibility(
                    visible = error.isNotEmpty(),
                    enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(170)) + expandVertically(tween(170)),
                    exit = if (reducedMotion) fadeOut(snap()) else fadeOut(tween(130)) + shrinkVertically(tween(130))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CampusColors.ErrorSoft.copy(alpha = 0.14f))
                                .border(1.dp, CampusColors.ErrorSoft.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CampusColors.ErrorSoft
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }

                Button(
                    onClick = {
                        when {
                            title.isBlank() || place.isBlank() || desc.isBlank() -> {
                                error = "Заполни название, место проведения и короткое описание."
                            }
                            date.isBefore(LocalDateTime.now()) -> {
                                error = "Выбери дату и время в будущем."
                            }
                            submitGuard.tryAcquire() -> {
                                onSave(
                                    Event(
                                        id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                                        title = title.trim(),
                                        category = category,
                                        date = date,
                                        place = place.trim(),
                                        description = desc.trim(),
                                        subtitle = "Событие школьного сообщества",
                                        tint = Color(0xFF333C48),
                                        photoUri = photoUri
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CampusColors.WarmSand,
                        contentColor = CampusColors.DeepObsidian
                    )
                ) {
                    Text(
                        text = "Опубликовать событие",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(Modifier.height(26.dp))
            }
        }
    }

    if (showDateModal) {
        CampusDateModal(
            initialDate = date.toLocalDate(),
            allowClear = false,
            onDismiss = { showDateModal = false },
            onConfirm = { picked ->
                dateIso = LocalDateTime.of(picked, date.toLocalTime()).toString()
                if (error.isNotEmpty()) error = ""
                showDateModal = false
            }
        )
    }

    if (showTimeModal) {
        CampusTimeModal(
            initialTime = date.toLocalTime(),
            onDismiss = { showTimeModal = false },
            onConfirm = { pickedTime ->
                dateIso = LocalDateTime.of(date.toLocalDate(), pickedTime).toString()
                if (error.isNotEmpty()) error = ""
                showTimeModal = false
            }
        )
    }
}
