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
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private var pendingReminder: Pair<Event, Int>? = null
    private var permissionVersion by mutableIntStateOf(0)
    private var pendingTestNotificationEvent by mutableStateOf<Event?>(null)
    private var incomingDeepLink by mutableStateOf<String?>(null)

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingReminder
        val testEvent = pendingTestNotificationEvent
        pendingReminder = null
        pendingTestNotificationEvent = null

        if (granted) {
            if (pending != null) {
                saveReminder(this, pending.first, pending.second)
                permissionVersion++
            }
            if (testEvent != null) {
                triggerInstantReminderPreview(this, testEvent)
            }
        } else {
            Toast.makeText(
                this,
                "Разреши уведомления, чтобы получать напоминания о событиях",
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

        if (savedInstanceState == null) {
            intent?.dataString?.takeIf { it.startsWith("campuslife://", ignoreCase = true) }?.let {
                incomingDeepLink = it
            }
        }

        setContent {
            CampusTheme {
                CampusApp(
                    permissionVersion = permissionVersion,
                    incomingDeepLink = incomingDeepLink,
                    onConsumeDeepLink = { incomingDeepLink = null },
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
                    },
                    onTestNotification = { event ->
                        if (
                            Build.VERSION.SDK_INT >= 33 &&
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingTestNotificationEvent = event
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            triggerInstantReminderPreview(this, event)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.takeIf { it.startsWith("campuslife://", ignoreCase = true) }?.let {
            incomingDeepLink = it
        }
    }
}

private fun reminderIntent(context: Context, event: Event): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java)
        .putExtra("title", event.title)
        .putExtra("place", event.place)
        .putExtra("time", event.date.format(TimeFmt))
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
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pi = reminderIntent(context, event)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        }
    } catch (_: SecurityException) {
        try {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pi)
        } catch (_: Exception) {
        }
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

private fun triggerInstantReminderPreview(context: Context, event: Event) {
    val broadcast = Intent(context, ReminderReceiver::class.java)
        .putExtra("title", event.title)
        .putExtra("place", event.place)
        .putExtra("time", event.date.format(TimeFmt))
        .putExtra("id", event.id)
    ReminderReceiver().onReceive(context, broadcast)
    Toast.makeText(
        context,
        "Тестовое уведомление отправлено в шторку системы",
        Toast.LENGTH_SHORT
    ).show()
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("campus", Context.MODE_PRIVATE)
        val seed = prefs.getLong("seed_day", 0L)
        if (seed == 0L) return
        val all = CampusStorage.loadAllEvents(context, LocalDate.ofEpochDay(seed))
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
                NotificationChannel(
                    "events",
                    "Мероприятия кампуса",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val eventId = intent.getIntExtra("id", 0)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            if (eventId > 0) {
                data = android.net.Uri.parse("campuslife://register?id=$eventId")
            }
        }
        val launch = PendingIntent.getActivity(
            context,
            eventId,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val place = intent.getStringExtra("place").orEmpty()
        val time = intent.getStringExtra("time").orEmpty()
        val subtitle = if (time.isNotEmpty()) "$time · $place" else place
        val notification = NotificationCompat.Builder(context, "events")
            .setSmallIcon(R.drawable.ic_stat_campus)
            .setContentTitle("Скоро: ${intent.getStringExtra("title")}")
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(launch)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(eventId, notification)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CampusApp(
    permissionVersion: Int,
    incomingDeepLink: String?,
    onConsumeDeepLink: () -> Unit,
    onRemind: (Event, Int) -> Boolean,
    onTestNotification: (Event) -> Unit
) {
    val ctx = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val prefs = remember { ctx.getSharedPreferences("campus", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        CampusImageCache.preloadDefaults(ctx)
    }

    val seedDay = remember {
        val saved = prefs.getLong("seed_day", 0L)
        val epoch = if (saved == 0L) {
            LocalDate.now().toEpochDay().also { value ->
                prefs.edit().putLong("seed_day", value).apply()
            }
        } else {
            saved
        }
        LocalDate.ofEpochDay(epoch)
    }

    var eventsVersion by remember { mutableIntStateOf(0) }
    val allEvents = remember(seedDay, eventsVersion) {
        CampusStorage.loadAllEvents(ctx, seedDay)
    }

    var favorites by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet())
    }
    var joined by remember {
        mutableStateOf(prefs.getStringSet("joined", emptySet()).orEmpty().toSet())
    }
    var attendeesVersion by remember { mutableIntStateOf(0) }
    var reminderVersion by rememberSaveable { mutableIntStateOf(0) }

    var studentName by remember { mutableStateOf(CampusStorage.getStudentName(ctx)) }
    var studentGroup by remember { mutableStateOf(CampusStorage.getStudentGroup(ctx)) }

    var activeTabName by rememberSaveable { mutableStateOf(CampusTab.Poster.name) }
    val activeTab = remember(activeTabName) {
        CampusTab.entries.firstOrNull { it.name == activeTabName } ?: CampusTab.Poster
    }

    // Маршруты: "home" | "create" | "detail:<id>" | "edit:<id>" | "scan" | "checkin:<id>"
    var routeKey by rememberSaveable { mutableStateOf("home") }

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Все") }
    var dateFilter by rememberSaveable { mutableStateOf("Все даты") }
    var exactDateEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    val exactDate = remember(exactDateEpoch) { exactDateEpoch?.let(LocalDate::ofEpochDay) }

    var showFilterCalendar by rememberSaveable { mutableStateOf(false) }
    var showProfileModal by rememberSaveable { mutableStateOf(false) }

    // Модальное окно QR-кода: Pair(eventId, isParticipantTicket)
    var qrModalTarget by remember { mutableStateOf<Pair<Event, Boolean>?>(null) }
    var participantsModalEvent by remember { mutableStateOf<Event?>(null) }

    val posterListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()
    val bookingsListState = rememberLazyListState()

    fun toggleStoredSet(current: Set<String>, key: String, id: Int): Set<String> {
        val idStr = id.toString()
        val updated = if (idStr in current) current - idStr else current + idStr
        prefs.edit().putStringSet(key, updated).apply()
        return updated
    }

    fun registerCurrentUserForEvent(target: Event, viaQr: Boolean) {
        val idStr = target.id.toString()
        val currentParticipants = CampusStorage.totalParticipantCount(
            ctx,
            target,
            isCurrentUserJoined = idStr in joined
        )
        if (idStr !in joined && currentParticipants >= target.capacity) {
            Toast.makeText(ctx, "На это мероприятие уже заняты все места", Toast.LENGTH_SHORT).show()
            return
        }
        if (idStr !in joined) {
            val updated = joined + idStr
            prefs.edit().putStringSet("joined", updated).apply()
            joined = updated
        }
        CampusStorage.syncCurrentUserRegistration(ctx, target.id, joined = true, viaQr = viaQr)
        attendeesVersion++
    }

    fun toggleCurrentUserJoin(target: Event) {
        val idStr = target.id.toString()
        val wasJoined = idStr in joined
        if (!wasJoined) {
            val currentParticipants = CampusStorage.totalParticipantCount(ctx, target, isCurrentUserJoined = false)
            if (currentParticipants >= target.capacity) {
                Toast.makeText(ctx, "Свободных мест на событие не осталось", Toast.LENGTH_SHORT).show()
                return
            }
        }
        joined = toggleStoredSet(joined, "joined", target.id)
        CampusStorage.syncCurrentUserRegistration(ctx, target.id, joined = !wasJoined, viaQr = false)
        attendeesVersion++
        if (wasJoined) {
            cancelReminder(ctx, target)
            reminderVersion++
        }
    }

    fun applyDecodedQr(payload: ParsedQrPayload, checkInTarget: Event?) {
        when (payload) {
            is ParsedQrPayload.EventInvite -> {
                var target = allEvents.firstOrNull { it.id == payload.eventId }
                if (target == null && payload.embeddedEvent != null) {
                    CampusStorage.upsertEvent(ctx, payload.embeddedEvent)
                    eventsVersion++
                    target = payload.embeddedEvent
                }
                if (target != null) {
                    registerCurrentUserForEvent(target, viaQr = true)
                    routeKey = "detail:${target.id}"
                    Toast.makeText(
                        ctx,
                        "QR принят! Ты записан на «${target.title}»",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(ctx, "Событие из QR-кода не найдено", Toast.LENGTH_SHORT).show()
                }
            }

            is ParsedQrPayload.ParticipantTicket -> {
                val targetId = checkInTarget?.id ?: payload.eventId
                var target = allEvents.firstOrNull { it.id == targetId }
                if (target == null && payload.embeddedEvent != null) {
                    CampusStorage.upsertEvent(ctx, payload.embeddedEvent)
                    eventsVersion++
                    target = payload.embeddedEvent
                }
                if (target != null) {
                    CampusStorage.addExternalCheckInAttendee(
                        context = ctx,
                        eventId = target.id,
                        name = payload.attendeeName,
                        group = payload.attendeeGroup,
                        source = "qr"
                    )
                    attendeesVersion++
                    routeKey = "detail:${target.id}"
                    Toast.makeText(
                        ctx,
                        "Отмечен участник: ${payload.attendeeName} (${payload.attendeeGroup})",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(ctx, "Событие билета не найдено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Обработка входящего deep-link campuslife://... (из внешнего сканера камеры или уведомления)
    LaunchedEffect(incomingDeepLink, allEvents) {
        val raw = incomingDeepLink ?: return@LaunchedEffect
        onConsumeDeepLink()
        val parsed = CampusQrCodec.parsePayloadOrShortCode(raw, allEvents)
        if (parsed != null) {
            applyDecodedQr(parsed, checkInTarget = null)
        }
    }

    val isOverlayOpen = routeKey != "home"
    BackHandler(enabled = isOverlayOpen) {
        routeKey = when {
            routeKey.startsWith("edit:") -> "detail:${routeKey.removePrefix("edit:")}"
            routeKey.startsWith("checkin:") -> "detail:${routeKey.removePrefix("checkin:")}"
            else -> "home"
        }
    }

    val detailEvent = remember(routeKey, allEvents) {
        if (routeKey.startsWith("detail:")) {
            val id = routeKey.removePrefix("detail:").toIntOrNull()
            allEvents.firstOrNull { it.id == id }
        } else {
            null
        }
    }

    val editEvent = remember(routeKey, allEvents) {
        if (routeKey.startsWith("edit:")) {
            val id = routeKey.removePrefix("edit:").toIntOrNull()
            allEvents.firstOrNull { it.id == id }
        } else {
            null
        }
    }

    val checkInEvent = remember(routeKey, allEvents) {
        if (routeKey.startsWith("checkin:")) {
            val id = routeKey.removePrefix("checkin:").toIntOrNull()
            allEvents.firstOrNull { it.id == id }
        } else {
            null
        }
    }

    val isImeVisible = WindowInsets.isImeVisible

    Surface(
        color = CampusColors.DeepObsidian,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RootBgBrush)
        ) {
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
                    attendeesVersion = attendeesVersion,
                    reminderVersion = reminderVersion,
                    permissionVersion = permissionVersion,
                    studentName = studentName,
                    studentGroup = studentGroup,
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
                    onOpenScanner = { routeKey = "scan" },
                    onOpenStudentProfile = { showProfileModal = true },
                    onOpenEvent = { ev -> routeKey = "detail:${ev.id}" },
                    onShowTicketQr = { ev -> qrModalTarget = ev to true },
                    onToggleFavorite = { ev ->
                        favorites = toggleStoredSet(favorites, "favorites", ev.id)
                    },
                    onToggleJoin = { ev -> toggleCurrentUserJoin(ev) }
                )

                AnimatedVisibility(
                    visible = !isImeVisible,
                    enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(180)),
                    exit = if (reducedMotion) fadeOut(snap()) else fadeOut(tween(140)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
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
            }

            // Слой 2: Полноценная страница мероприятия с панелью организатора, напоминаниями и QR-кодами
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
                var cachedEvent by remember { mutableStateOf(detailEvent) }
                if (detailEvent != null) cachedEvent = detailEvent
                val target = cachedEvent

                if (target != null) {
                    val isJoined = target.id.toString() in joined
                    val reminderMinutes = remember(target.id, reminderVersion, permissionVersion) {
                        prefs.getInt("reminder_${target.id}", 0)
                    }
                    val attendees = remember(target.id, attendeesVersion, isJoined) {
                        CampusStorage.loadAttendees(ctx, target.id)
                    }
                    val totalParticipants = remember(target, attendees, isJoined) {
                        CampusStorage.totalParticipantCount(ctx, target, isJoined, attendees)
                    }

                    EventDetailScreen(
                        event = target,
                        isFavorite = target.id.toString() in favorites,
                        isJoined = isJoined,
                        totalParticipants = totalParticipants,
                        attendees = attendees,
                        reminderMinutes = reminderMinutes,
                        onBack = { routeKey = "home" },
                        onToggleFavorite = {
                            favorites = toggleStoredSet(favorites, "favorites", target.id)
                        },
                        onToggleJoin = { toggleCurrentUserJoin(target) },
                        onSelectReminder = { minutes ->
                            if (onRemind(target, minutes)) {
                                reminderVersion++
                            }
                        },
                        onTestNotification = { onTestNotification(target) },
                        onShowEventQr = { qrModalTarget = target to false },
                        onShowParticipantQr = { qrModalTarget = target to true },
                        onEditEvent = { routeKey = "edit:${target.id}" },
                        onOpenCheckInScanner = { routeKey = "checkin:${target.id}" },
                        onOpenParticipantsModal = { participantsModalEvent = target }
                    )
                }
            }

            // Слой 3: Создание или редактирование мероприятия организатором
            val isEditorOpen = routeKey == "create" || editEvent != null
            AnimatedVisibility(
                visible = isEditorOpen,
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
                EventEditorScreen(
                    existingEvent = editEvent,
                    onBack = {
                        routeKey = if (editEvent != null) "detail:${editEvent.id}" else "home"
                    },
                    onSave = { savedEvent ->
                        CampusStorage.upsertEvent(ctx, savedEvent)
                        eventsVersion++
                        // Если время мероприятия изменилось и стояло активное напоминание — перепланируем его
                        val existingRem = prefs.getInt("reminder_${savedEvent.id}", 0)
                        if (existingRem > 0) {
                            if (savedEvent.date.minusMinutes(existingRem.toLong()).isAfter(LocalDateTime.now())) {
                                saveReminder(ctx, savedEvent, existingRem)
                            } else {
                                cancelReminder(ctx, savedEvent)
                            }
                            reminderVersion++
                        }
                        routeKey = "detail:${savedEvent.id}"
                    },
                    onDelete = { toDelete ->
                        cancelReminder(ctx, toDelete)
                        CampusStorage.deleteCustomEvent(ctx, toDelete.id)
                        favorites = prefs.getStringSet("favorites", emptySet()).orEmpty().toSet()
                        joined = prefs.getStringSet("joined", emptySet()).orEmpty().toSet()
                        eventsVersion++
                        reminderVersion++
                        routeKey = "home"
                        Toast.makeText(ctx, "Событие удалено", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Слой 4: Сканер QR-кодов (для записи участника или проверки билетов организатором)
            val isScannerOpen = routeKey == "scan" || checkInEvent != null
            AnimatedVisibility(
                visible = isScannerOpen,
                enter = if (reducedMotion) {
                    fadeIn(snap())
                } else {
                    fadeIn(tween(200, easing = CampusMotion.EmphasizedDecelerate)) +
                        slideInVertically(tween(240, easing = CampusMotion.EmphasizedDecelerate)) { it / 15 }
                },
                exit = if (reducedMotion) {
                    fadeOut(snap())
                } else {
                    fadeOut(tween(165)) +
                        slideOutVertically(tween(195, easing = CampusMotion.EmphasizedDecelerate)) { it / 16 }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                QrScannerScreen(
                    allEvents = allEvents,
                    targetCheckInEvent = checkInEvent,
                    onBack = {
                        routeKey = if (checkInEvent != null) "detail:${checkInEvent.id}" else "home"
                    },
                    onDecodedPayload = { decoded ->
                        applyDecodedQr(decoded, checkInTarget = checkInEvent)
                    }
                )
            }

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

            val activeQrModal = qrModalTarget
            if (activeQrModal != null) {
                val latestEvent = allEvents.firstOrNull { it.id == activeQrModal.first.id } ?: activeQrModal.first
                EventQrModal(
                    event = latestEvent,
                    isParticipantTicket = activeQrModal.second,
                    studentName = studentName,
                    studentGroup = studentGroup,
                    onDismiss = { qrModalTarget = null }
                )
            }

            val activeParticipantsEvent = participantsModalEvent
            if (activeParticipantsEvent != null) {
                val refreshedEvent = allEvents.firstOrNull { it.id == activeParticipantsEvent.id }
                    ?: activeParticipantsEvent
                val isJoinedNow = refreshedEvent.id.toString() in joined
                val attendees = remember(refreshedEvent.id, attendeesVersion, isJoinedNow) {
                    CampusStorage.loadAttendees(ctx, refreshedEvent.id)
                }
                val totalCount = remember(refreshedEvent, attendees, isJoinedNow) {
                    CampusStorage.totalParticipantCount(ctx, refreshedEvent, isJoinedNow, attendees)
                }
                OrganizerParticipantsModal(
                    event = refreshedEvent,
                    totalParticipants = totalCount,
                    attendees = attendees,
                    onAddManualAttendee = { name, group ->
                        CampusStorage.addExternalCheckInAttendee(
                            context = ctx,
                            eventId = refreshedEvent.id,
                            name = name,
                            group = group,
                            source = "organizer"
                        )
                        attendeesVersion++
                    },
                    onRemoveAttendee = { attendeeId ->
                        CampusStorage.removeAttendee(ctx, refreshedEvent.id, attendeeId)
                        attendeesVersion++
                    },
                    onOpenCheckInScanner = { routeKey = "checkin:${refreshedEvent.id}" },
                    onDismiss = { participantsModalEvent = null }
                )
            }

            if (showProfileModal) {
                StudentProfileModal(
                    initialName = studentName,
                    initialGroup = studentGroup,
                    onDismiss = { showProfileModal = false },
                    onSave = { updatedName, updatedGroup ->
                        CampusStorage.saveStudentProfile(ctx, updatedName, updatedGroup)
                        studentName = CampusStorage.getStudentName(ctx)
                        studentGroup = CampusStorage.getStudentGroup(ctx)
                        // Обновляем имя в активных записях
                        joined.forEach { idStr ->
                            idStr.toIntOrNull()?.let { id ->
                                CampusStorage.syncCurrentUserRegistration(ctx, id, joined = true)
                            }
                        }
                        attendeesVersion++
                        showProfileModal = false
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
    attendeesVersion: Int,
    reminderVersion: Int,
    permissionVersion: Int,
    studentName: String,
    studentGroup: String,
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
    onOpenScanner: () -> Unit,
    onOpenStudentProfile: () -> Unit,
    onOpenEvent: (Event) -> Unit,
    onShowTicketQr: (Event) -> Unit,
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
                ev.description.contains(trimmedQuery, ignoreCase = true) ||
                ev.place.contains(trimmedQuery, ignoreCase = true) ||
                ev.category.contains(trimmedQuery, ignoreCase = true) ||
                ev.organizer.contains(trimmedQuery, ignoreCase = true) ||
                ev.subtitle.contains(trimmedQuery, ignoreCase = true)
            val evDay = ev.date.toLocalDate()
            val exactOk = exactDate == null || evDay == exactDate
            val rangeOk = when (dateFilter) {
                "Все даты" -> true
                "Сегодня" -> evDay == today
                "Завтра" -> evDay == today.plusDays(1)
                "7 дней" -> !evDay.isBefore(today) && !evDay.isAfter(today.plusDays(7))
                "30 дней" -> !evDay.isBefore(today) && !evDay.isAfter(today.plusDays(30))
                else -> true
            }
            catOk && queryOk && exactOk && rangeOk
        }
    }
    val favoritesList = remember(allEvents, favorites) {
        allEvents.filter { it.id.toString() in favorites }
    }
    val bookingsList = remember(allEvents, joined) {
        allEvents.filter { it.id.toString() in joined }
    }

    val countsByEventId = remember(allEvents, joined, attendeesVersion) {
        allEvents.associate { ev ->
            ev.id to CampusStorage.totalParticipantCount(
                ctx,
                ev,
                isCurrentUserJoined = ev.id.toString() in joined
            )
        }
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
                // Шапка с логотипом Campus Life, кнопкой QR-сканера и кнопкой создания события организатором
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            onClick = onOpenScanner,
                            size = 44.dp,
                            contentDescriptionLabel = "Сканировать QR-код"
                        ) {
                            QrCodeGlyph(color = CampusColors.SkyMuted, size = 20.dp)
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
                }

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

                        if (tab == CampusTab.Bookings) {
                            item(key = "student_profile_bar", contentType = "profile_bar") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(CampusColors.Graphite)
                                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(18.dp))
                                        .pressableScale(pressedScale = 0.98f, onClick = onOpenStudentProfile)
                                        .padding(horizontal = 15.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CampusColors.GraphiteElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            QrCodeGlyph(color = CampusColors.WarmSand, size = 18.dp)
                                        }
                                        Column {
                                            Text(
                                                text = "$studentName · $studentGroup",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = CampusColors.Milk
                                            )
                                            Text(
                                                text = "Данные для личных QR-билетов · нажми, чтобы изменить",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CampusColors.SlateMuted
                                            )
                                        }
                                    }
                                    EditGlyph(color = CampusColors.SkyMuted, size = 16.dp)
                                }
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
                                    onShowQrTicket = { onShowTicketQr(ev) },
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
                                        participantsCount = countsByEventId[first.id] ?: first.baseParticipants,
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
                                            participantsCount = countsByEventId[second.id] ?: second.baseParticipants,
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
                                    participantsCount = countsByEventId[ev.id] ?: ev.baseParticipants,
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
                items = CampusCategories,
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
                items = DateRangeOptions,
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
    totalParticipants: Int,
    attendees: List<AttendeeEntry>,
    reminderMinutes: Int,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleJoin: () -> Unit,
    onSelectReminder: (Int) -> Unit,
    onTestNotification: () -> Unit,
    onShowEventQr: () -> Unit,
    onShowParticipantQr: () -> Unit,
    onEditEvent: () -> Unit,
    onOpenCheckInScanner: () -> Unit,
    onOpenParticipantsModal: () -> Unit
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            onClick = onShowEventQr,
                            size = 42.dp,
                            contentDescriptionLabel = "QR-код события"
                        ) {
                            QrCodeGlyph(color = CampusColors.WarmSand, size = 19.dp)
                        }

                        GlassIconButton(
                            onClick = onEditEvent,
                            size = 42.dp,
                            contentDescriptionLabel = "Редактировать событие"
                        ) {
                            EditGlyph(color = CampusColors.SkyMuted, size = 18.dp)
                        }

                        FavoriteHeartButton(
                            isFavorite = isFavorite,
                            onToggle = onToggleFavorite,
                            size = 42.dp,
                            iconSize = 20.dp,
                            glassCircle = false
                        )
                    }
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

                Spacer(Modifier.height(10.dp))

                DetailMetaTile(
                    icon = { UsersGlyph(color = CampusColors.WarmSand, size = 18.dp) },
                    label = "Организатор и участники",
                    value = "${event.organizer} · записано $totalParticipants из ${event.capacity}"
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

                Spacer(Modifier.height(24.dp))

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

                AnimatedVisibility(
                    visible = isJoined,
                    enter = if (reducedMotion) fadeIn(snap()) else fadeIn(tween(180)) + expandVertically(tween(180)),
                    exit = if (reducedMotion) fadeOut(snap()) else fadeOut(tween(140)) + shrinkVertically(tween(140))
                ) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CampusColors.WarmSand.copy(alpha = 0.13f))
                                .border(1.dp, CampusColors.WarmSand.copy(alpha = 0.36f), RoundedCornerShape(16.dp))
                                .pressableScale(pressedScale = 0.97f, onClick = onShowParticipantQr)
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QrCodeGlyph(color = CampusColors.WarmSand, size = 19.dp)
                                Column {
                                    Text(
                                        text = "Открыть мой QR-билет участника",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CampusColors.Milk
                                    )
                                    Text(
                                        text = "Для быстрой отметки на входе у организатора",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CampusColors.SlateMuted
                                    )
                                }
                            }
                            ChevronRightGlyph(color = CampusColors.WarmSand, size = 17.dp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Секция напоминаний о приближающемся мероприятии
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CampusColors.Graphite)
                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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

                        Text(
                            text = "Проверить",
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(CampusColors.GraphiteElevated)
                                .pressableScale(pressedScale = 0.93f, onClick = onTestNotification)
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = CampusColors.WarmSand
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems(
                            items = listOf(
                                0 to "Нет",
                                5 to "За 5 мин",
                                15 to "За 15 мин",
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
                                text = "Уведомление запланировано и восстановится даже после перезагрузки телефона.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = CampusColors.SlateMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Панель организатора: счётчик участников, редактирование, QR для регистрации и сканер на входе
                OrganizerEventPanel(
                    event = event,
                    totalParticipants = totalParticipants,
                    attendees = attendees,
                    onEditEvent = onEditEvent,
                    onShowEventQr = onShowEventQr,
                    onOpenCheckInScanner = onOpenCheckInScanner,
                    onOpenParticipantsModal = onOpenParticipantsModal
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Все записи, избранное, участники и QR-билеты надёжно хранятся на устройстве и работают офлайн.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = CampusColors.SlateMuted.copy(alpha = 0.78f)
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
