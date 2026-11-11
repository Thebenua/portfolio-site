package ru.campuslife.app

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as rowItems
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        .set(AlarmManager.RTC_WAKEUP, time, reminderIntent(context, event))
    context.getSharedPreferences("campus", Context.MODE_PRIVATE)
        .edit()
        .putInt("reminder_${event.id}", minutes)
        .apply()
}

private fun cancelReminder(context: Context, event: Event) {
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        .cancel(reminderIntent(context, event))
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

private sealed interface ScreenRoute {
    data object Home : ScreenRoute
    data class Detail(val eventId: Int) : ScreenRoute
    data object Create : ScreenRoute
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CampusApp(
    permissionVersion: Int,
    onRemind: (Event, Int) -> Boolean
) {
    val ctx = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val prefs = remember { ctx.getSharedPreferences("campus", Context.MODE_PRIVATE) }

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
    var reminderVersion by remember { mutableIntStateOf(0) }

    var activeTab by remember { mutableStateOf(CampusTab.Poster) }
    var route by remember { mutableStateOf<ScreenRoute>(ScreenRoute.Home) }

    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Все") }
    var dateFilter by remember { mutableStateOf("Все даты") }
    var exactDate by remember { mutableStateOf<LocalDate?>(null) }

    fun toggleStoredSet(current: Set<String>, key: String, id: Int): Set<String> {
        val idStr = id.toString()
        val updated = if (idStr in current) current - idStr else current + idStr
        prefs.edit().putStringSet(key, updated).apply()
        return updated
    }

    BackHandler(enabled = route != ScreenRoute.Home) {
        route = ScreenRoute.Home
    }

    val imeVisible = WindowInsets.isImeVisible

    Surface(
        color = CampusColors.DeepObsidian,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF161920),
                            CampusColors.DeepObsidian,
                            Color(0xFF0E1014)
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 26 })
                            .togetherWith(fadeOut(tween(170)))
                    }
                },
                label = "screenRouteTransition",
                modifier = Modifier.fillMaxSize()
            ) { currentRoute ->
                when (currentRoute) {
                    is ScreenRoute.Create -> {
                        CreateEventScreen(
                            onBack = { route = ScreenRoute.Home },
                            onSave = { newEvent ->
                                saveCustom(ctx, newEvent)
                                custom = loadCustom(ctx)
                                route = ScreenRoute.Detail(newEvent.id)
                            }
                        )
                    }

                    is ScreenRoute.Detail -> {
                        val target = allEvents.firstOrNull { it.id == currentRoute.eventId }
                        if (target == null) {
                            route = ScreenRoute.Home
                        } else {
                            val reminderMinutes = remember(target.id, reminderVersion, permissionVersion) {
                                prefs.getInt("reminder_${target.id}", 0)
                            }
                            EventDetailScreen(
                                event = target,
                                isFavorite = target.id.toString() in favorites,
                                isJoined = target.id.toString() in joined,
                                reminderMinutes = reminderMinutes,
                                onBack = { route = ScreenRoute.Home },
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

                    ScreenRoute.Home -> {
                        HomeTabsContent(
                            allEvents = allEvents,
                            activeTab = activeTab,
                            favorites = favorites,
                            joined = joined,
                            reminderVersion = reminderVersion,
                            permissionVersion = permissionVersion,
                            query = query,
                            category = category,
                            dateFilter = dateFilter,
                            exactDate = exactDate,
                            onQueryChange = { query = it },
                            onCategoryChange = { category = it },
                            onDateFilterChange = {
                                dateFilter = it
                                exactDate = null
                            },
                            onPickExactDate = { picked ->
                                exactDate = picked
                                if (picked != null) dateFilter = "Все даты"
                            },
                            onResetFilters = {
                                query = ""
                                category = "Все"
                                dateFilter = "Все даты"
                                exactDate = null
                            },
                            onSwitchTab = { activeTab = it },
                            onOpenCreate = { route = ScreenRoute.Create },
                            onOpenEvent = { ev -> route = ScreenRoute.Detail(ev.id) },
                            onToggleFavorite = { ev ->
                                favorites = toggleStoredSet(favorites, "favorites", ev.id)
                            },
                            onToggleJoin = { ev ->
                                joined = toggleStoredSet(joined, "joined", ev.id)
                            }
                        )
                    }
                }
            }

            // Мягкая подложка за парящей капсулой, чтобы контент уходил под неё естественно
            val showFloatingBar = route == ScreenRoute.Home && !imeVisible
            AnimatedVisibility(
                visible = showFloatingBar,
                enter = if (reducedMotion) fadeIn(tween(0)) else fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 2 },
                exit = if (reducedMotion) fadeOut(tween(0)) else fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    CampusColors.DeepObsidian.copy(alpha = 0.72f),
                                    CampusColors.DeepObsidian.copy(alpha = 0.94f)
                                )
                            )
                        )
                ) {
                    FloatingBottomNav(
                        selectedTab = activeTab,
                        favoritesCount = favorites.size,
                        bookingsCount = joined.size,
                        onSelect = { activeTab = it }
                    )
                }
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
    query: String,
    category: String,
    dateFilter: String,
    exactDate: LocalDate?,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateFilterChange: (String) -> Unit,
    onPickExactDate: (LocalDate?) -> Unit,
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

    val filteredEvents = remember(allEvents, activeTab, favorites, joined, query, category, dateFilter, exactDate) {
        allEvents.filter { ev ->
            val tabMatches = when (activeTab) {
                CampusTab.Poster -> true
                CampusTab.Favorites -> ev.id.toString() in favorites
                CampusTab.Bookings -> ev.id.toString() in joined
            }
            val filterMatches = if (activeTab != CampusTab.Poster) {
                true
            } else {
                val catOk = category == "Все" || ev.category == category
                val queryOk = query.isBlank() ||
                    "${ev.title} ${ev.place} ${ev.category} ${ev.subtitle}".contains(query, ignoreCase = true)
                val exactOk = exactDate == null || ev.date.toLocalDate() == exactDate
                val rangeOk = dateFilter == "Все даты" || (
                    !ev.date.toLocalDate().isBefore(LocalDate.now()) &&
                        ev.date.toLocalDate() <= LocalDate.now().plusDays(if (dateFilter == "7 дней") 7 else 30)
                )
                catOk && queryOk && exactOk && rangeOk
            }
            tabMatches && filterMatches
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        val horizontalPad = if (maxWidth >= 600.dp) 32.dp else 20.dp

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxSize()
            ) {
                // Единая премиальная шапка с фирменным знаком Campus Life и кнопкой создания
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = horizontalPad, end = horizontalPad, top = 14.dp, bottom = 14.dp),
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
                                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                } else {
                                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
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
                                style = MaterialTheme.typography.headlineLarge,
                                color = CampusColors.Milk
                            )
                        }
                    }

                    GlassIconButton(
                        onClick = onOpenCreate,
                        size = 44.dp,
                        accent = true
                    ) {
                        PlusGlyph(color = CampusColors.WarmSand, size = 20.dp)
                    }
                }

                // Контент активной вкладки с мягким переключением (180–240 мс)
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            fadeIn(tween(210, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(tween(160, easing = FastOutSlowInEasing))
                        }
                    },
                    label = "tabBodyTransition",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Нижний отступ 126dp гарантирует, что парящая капсула никогда не перекроет последнюю карточку
                        contentPadding = PaddingValues(
                            start = horizontalPad,
                            end = horizontalPad,
                            top = 4.dp,
                            bottom = 128.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (tab == CampusTab.Poster) {
                            item(key = "poster_controls") {
                                PosterSearchAndFilters(
                                    query = query,
                                    category = category,
                                    dateFilter = dateFilter,
                                    exactDate = exactDate,
                                    onQueryChange = onQueryChange,
                                    onCategoryChange = onCategoryChange,
                                    onDateFilterChange = onDateFilterChange,
                                    onPickExactDate = {
                                        val base = exactDate ?: LocalDate.now()
                                        DatePickerDialog(
                                            ctx,
                                            { _, y, m, d -> onPickExactDate(LocalDate.of(y, m + 1, d)) },
                                            base.year,
                                            base.monthValue - 1,
                                            base.dayOfMonth
                                        ).show()
                                    },
                                    onClearExactDate = { onPickExactDate(null) }
                                )
                            }
                        }

                        // Счётчик раздела
                        item(key = "section_summary_${tab.name}") {
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
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(CampusColors.Graphite)
                                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(percent = 50))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${filteredEvents.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CampusColors.SkyMuted
                                    )
                                }
                            }
                        }

                        if (filteredEvents.isEmpty()) {
                            item(key = "empty_${tab.name}") {
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
                            items(filteredEvents, key = { "booking_${it.id}" }) { ev ->
                                val reminderMin = remember(ev.id, reminderVersion, permissionVersion) {
                                    prefs.getInt("reminder_${ev.id}", 0)
                                }
                                BookingTicketCard(
                                    event = ev,
                                    reminderMinutes = reminderMin,
                                    onClick = { onOpenEvent(ev) },
                                    onCancelBooking = { onToggleJoin(ev) }
                                )
                            }
                        } else {
                            items(filteredEvents, key = { "event_${tab.name}_${it.id}" }) { ev ->
                                PosterEventCard(
                                    event = ev,
                                    isFavorite = ev.id.toString() in favorites,
                                    isJoined = ev.id.toString() in joined,
                                    onClick = { onOpenEvent(ev) },
                                    onToggleFavorite = { onToggleFavorite(ev) }
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
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateFilterChange: (String) -> Unit,
    onPickExactDate: () -> Unit,
    onClearExactDate: () -> Unit
) {
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
            trailingIcon = if (query.isNotEmpty()) {
                {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CampusColors.GraphiteElevated)
                            .pressableScale(pressedScale = 0.9f, onClick = { onQueryChange("") }),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseGlyph(color = CampusColors.Milk, size = 13.dp)
                    }
                }
            } else null,
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

        // Категории
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowItems(listOf("Все", "Наука", "Творчество", "Спорт", "Технологии")) { item ->
                FilterPill(
                    text = item,
                    selected = item == category,
                    onClick = { onCategoryChange(item) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Периоды дат + выбор точного дня в одной аккуратной ленте
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowItems(listOf("Все даты", "7 дней", "30 дней")) { period ->
                FilterPill(
                    text = period,
                    selected = period == dateFilter && exactDate == null,
                    onClick = { onDateFilterChange(period) }
                )
            }

            item {
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
                item {
                    Text(
                        text = "Сбросить",
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .pressableScale(pressedScale = 0.94f, onClick = onClearExactDate)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = CampusColors.WarmSand
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val horizontalPad = if (maxWidth >= 600.dp) 32.dp else 20.dp

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 14.dp)
            ) {
                // Верхняя панель навигации
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
                            .pressableScale(pressedScale = 0.95f, onClick = onBack)
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

                    GlassIconButton(
                        onClick = onToggleFavorite,
                        size = 42.dp,
                        accent = isFavorite
                    ) {
                        NavHeartIcon(
                            color = if (isFavorite) CampusColors.WarmSand else CampusColors.Milk,
                            active = isFavorite,
                            filledWhenActive = true,
                            size = 20.dp
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Крупная обложка события
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(event.tint)
                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(26.dp))
                ) {
                    EventCoverImage(event = event)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color(0x280D0F13),
                                    0.55f to Color(0x240D0F13),
                                    1.0f to Color(0xD9111317)
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

                        if (isJoined) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Color(0xC417241F))
                                    .border(0.8.dp, CampusColors.Success.copy(alpha = 0.4f), RoundedCornerShape(percent = 50))
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
                    color = CampusColors.Milk.copy(alpha = 0.84f)
                )

                Spacer(Modifier.height(26.dp))

                // Главная кнопка записи / отмены
                Button(
                    onClick = onToggleJoin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isJoined) CampusColors.GraphiteElevated else CampusColors.WarmSand,
                        contentColor = if (isJoined) CampusColors.Milk else CampusColors.DeepObsidian
                    )
                ) {
                    Text(
                        text = if (isJoined) "Отменить запись" else "Записаться на событие",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Карточка управления напоминанием
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
                            listOf(
                                0 to "Нет",
                                10 to "За 10 мин",
                                60 to "За 1 час",
                                1440 to "За 1 день"
                            )
                        ) { (minutes, label) ->
                            FilterPill(
                                text = label,
                                selected = reminderMinutes == minutes,
                                onClick = { onSelectReminder(minutes) }
                            )
                        }
                    }

                    if (reminderMinutes > 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Уведомление придёт автоматически; возможна небольшая задержка из-за энергосбережения системы.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = CampusColors.SlateMuted
                        )
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
    var title by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Наука") }
    var date by remember {
        mutableStateOf(
            LocalDateTime.now()
                .plusDays(1)
                .withHour(16)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        )
    }
    var error by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }

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

    val previewBitmap = remember(photoUri) {
        if (photoUri.isBlank()) null else try {
            ctx.contentResolver.openInputStream(Uri.parse(photoUri))?.use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val horizontalPad = if (maxWidth >= 600.dp) 32.dp else 20.dp

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
                        .pressableScale(pressedScale = 0.95f, onClick = onBack)
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

                // Блок выбора обложки
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (previewBitmap != null) 168.dp else 94.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CampusColors.Graphite)
                        .border(1.dp, CampusColors.HairlineStrong, RoundedCornerShape(20.dp))
                        .pressableScale(
                            pressedScale = 0.985f,
                            onClick = { photoPicker.launch(arrayOf("image/*")) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
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
                                if (previewBitmap != null) Color(0xCC16191F) else Color.Transparent
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
                    rowItems(listOf("Наука", "Творчество", "Спорт", "Технологии")) { item ->
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
                        onClick = {
                            DatePickerDialog(
                                ctx,
                                { _, y, m, d ->
                                    date = date.withDayOfMonth(1).withYear(y).withMonth(m + 1).withDayOfMonth(d)
                                },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth
                            ).show()
                        },
                        leadingContent = {
                            NavPosterIcon(color = CampusColors.WarmSand, active = false, size = 15.dp)
                        }
                    )

                    FilterPill(
                        text = date.format(TimeFmt),
                        selected = false,
                        onClick = {
                            TimePickerDialog(
                                ctx,
                                { _, h, min -> date = date.withHour(h).withMinute(min) },
                                date.hour,
                                date.minute,
                                true
                            ).show()
                        },
                        leadingContent = {
                            ClockGlyph(color = CampusColors.SkyMuted, size = 15.dp)
                        }
                    )
                }

                Spacer(Modifier.height(18.dp))

                if (error.isNotEmpty()) {
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

                Button(
                    onClick = {
                        when {
                            title.isBlank() || place.isBlank() || desc.isBlank() -> {
                                error = "Заполни название, место проведения и короткое описание."
                            }
                            date.isBefore(LocalDateTime.now()) -> {
                                error = "Выбери дату и время в будущем."
                            }
                            else -> {
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
}
