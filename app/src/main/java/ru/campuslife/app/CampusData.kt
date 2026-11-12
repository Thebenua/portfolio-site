package ru.campuslife.app

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

internal val CampusCategories = listOf(
    "Все",
    "Спорт",
    "Наука",
    "Творчество",
    "Конкурсы",
    "Технологии"
)

internal val EditableCategories = listOf(
    "Спорт",
    "Наука",
    "Творчество",
    "Конкурсы",
    "Технологии"
)

internal val DateRangeOptions = listOf(
    "Все даты",
    "Сегодня",
    "Завтра",
    "7 дней",
    "30 дней"
)

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
    val photoUri: String = "",
    val organizer: String = "Школьный актив",
    val capacity: Int = 40,
    val baseParticipants: Int = 0,
    val isUserCreated: Boolean = false
)

@Immutable
data class AttendeeEntry(
    val id: String,
    val name: String,
    val group: String,
    val source: String, // "app" | "qr" | "organizer"
    val joinedAt: String
)

internal fun tintForCategory(category: String): Color = when (category) {
    "Наука" -> Color(0xFF333C48)
    "Творчество" -> Color(0xFF423837)
    "Спорт" -> Color(0xFF2F3E3A)
    "Конкурсы" -> Color(0xFF3D3746)
    "Технологии" -> Color(0xFF2E3645)
    else -> Color(0xFF333C48)
}

internal fun eventQrCodeToken(eventId: Int): String =
    "CL-${eventId.toString().takeLast(6).padStart(4, '0')}"

internal fun seedEvents(day: LocalDate): List<Event> = listOf(
    Event(
        id = 6,
        title = "Отборочный спринт TASKILLS",
        category = "Конкурсы",
        date = day.atTime(18, 30),
        place = "Центр IT-Куб · каб. 301",
        description = "Командная защита прототипов школьных цифровых сервисов и мобильных приложений. Эксперты разбирают архитектуру, UX-сценарии и помогают подготовить питч к финалу.",
        subtitle = "Презентация командных проектов сегодня",
        tint = Color(0xFF3D3746),
        organizer = "Школьный IT-клуб",
        capacity = 45,
        baseParticipants = 28
    ),
    Event(
        id = 7,
        title = "Утренний забег и разминка кампуса",
        category = "Спорт",
        date = day.plusDays(1).atTime(15, 30),
        place = "Школьный стадион",
        description = "Лёгкая командная пробежка на 2 км, функциональная разминка с тренером и дружеская эстафета между параллелями 8–11 классов. Уровень подготовки любой.",
        subtitle = "Заряд энергии после уроков",
        tint = Color(0xFF2F3E3A),
        organizer = "Спортивный клуб «Импульс»",
        capacity = 60,
        baseParticipants = 34
    ),
    Event(
        id = 1,
        title = "Ночь науки: физика света и звука",
        category = "Наука",
        date = day.plusDays(2).atTime(16, 30),
        place = "Актовый зал",
        description = "Интерактивные демонстрации с лазерами, резонансом и акустической левитацией. Каждый участник сможет провести серию безопасных экспериментов на лабораторных станциях.",
        subtitle = "Опыты и открытия рядом",
        tint = Color(0xFF333C48),
        organizer = "Научное общество лицеистов",
        capacity = 80,
        baseParticipants = 51
    ),
    Event(
        id = 2,
        title = "Творческая мастерская плаката",
        category = "Творчество",
        date = day.plusDays(4).atTime(15, 0),
        place = "Арт-студия · каб. 204",
        description = "Придумываем визуальный стиль для школьных событий: работаем с композицией, типографикой и коллажем. Все краски, маркеры и бумага ждут на месте.",
        subtitle = "Идеи, которым нужно место",
        tint = Color(0xFF423837),
        organizer = "Медиацентр школы",
        capacity = 24,
        baseParticipants = 17
    ),
    Event(
        id = 3,
        title = "Школьный кубок по волейболу",
        category = "Спорт",
        date = day.plusDays(6).atTime(14, 30),
        place = "Большой спортивный зал",
        description = "Собираем сборные классов и смешанные команды для осенне-зимнего турнира. Регистрируйся как игрок или приходи поддержать друзей на трибунах.",
        subtitle = "Играем одной командой",
        tint = Color(0xFF2F3E3A),
        organizer = "Кафедра физкультуры и совет старшеклассников",
        capacity = 72,
        baseParticipants = 46
    ),
    Event(
        id = 8,
        title = "Олимпиада роботов и 3D-моделирования",
        category = "Конкурсы",
        date = day.plusDays(8).atTime(15, 30),
        place = "Лаборатория технологии · каб. 112",
        description = "Соревнование по скоростному прототипированию деталей для FDM 3D-принтера и прохождению трассы автономными мини-роботами. Призы лучшим инженерным дуэтам.",
        subtitle = "Инженерный вызов для команд 2–3 человека",
        tint = Color(0xFF3D3746),
        organizer = "Инженерный кружок Кванториум-класс",
        capacity = 30,
        baseParticipants = 21
    ),
    Event(
        id = 4,
        title = "Клуб создателей мобильных приложений",
        category = "Технологии",
        date = day.plusDays(10).atTime(16, 0),
        place = "Медиатека",
        description = "Разбираем живые интерфейсы на Kotlin и Jetpack Compose, учимся делать плавные анимации и тестировать приложения прямо на смартфонах участников.",
        subtitle = "От первой идеи к рабочему APK",
        tint = Color(0xFF2E3645),
        organizer = "Школьный IT-клуб",
        capacity = 32,
        baseParticipants = 19
    ),
    Event(
        id = 9,
        title = "Битва школьных научных слэмов",
        category = "Наука",
        date = day.plusDays(14).atTime(16, 0),
        place = "Лекторий библиотеки",
        description = "Пять старшеклассников рассказывают о собственных исследованиях — от нейросетей до экологии города — за 7 минут простым языком. Победителя выбирает зал шумомером!",
        subtitle = "Наука ярко, коротко и по делу",
        tint = Color(0xFF333C48),
        organizer = "Научное общество лицеистов",
        capacity = 65,
        baseParticipants = 38
    ),
    Event(
        id = 5,
        title = "Акустический вечер «Открытый микрофон»",
        category = "Творчество",
        date = day.plusDays(18).atTime(17, 0),
        place = "Пространство библиотеки",
        description = "Авторские песни под гитару, чтение стихов и коротких историй при тёплом камерном свете. Выступай на сцене или бронируй уютное место в зале.",
        subtitle = "Вечер, где слышно каждого",
        tint = Color(0xFF3F3632),
        organizer = "Творческое объединение «Сцена»",
        capacity = 50,
        baseParticipants = 33
    ),
    Event(
        id = 10,
        title = "Большой предновогодний квиз классов",
        category = "Конкурсы",
        date = day.plusDays(25).atTime(16, 30),
        place = "Актовый зал",
        description = "Шесть интеллектуальных раундов про науку, кино, музыку и историю школы. Регистрация открыта как для готовых команд, так и для индивидуальных игроков.",
        subtitle = "Главный интеллектуальный турнир месяца",
        tint = Color(0xFF3D3746),
        organizer = "Совет старшеклассников",
        capacity = 90,
        baseParticipants = 54
    )
)

internal object CampusStorage {
    private const val PREFS_NAME = "campus"
    private const val KEY_CUSTOM_EVENTS = "custom_events"
    private const val KEY_SEED_OVERRIDES = "seed_overrides"
    private const val KEY_ATTENDEES_PREFIX = "attendees_"
    private const val KEY_USER_NAME = "student_name"
    private const val KEY_USER_GROUP = "student_group"

    fun getStudentName(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, "Ученик кампуса")
            ?.takeIf { it.isNotBlank() }
            ?: "Ученик кампуса"

    fun getStudentGroup(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_GROUP, "10А класс")
            ?.takeIf { it.isNotBlank() }
            ?: "10А класс"

    fun saveStudentProfile(context: Context, name: String, group: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_NAME, name.trim().ifBlank { "Ученик кампуса" })
            .putString(KEY_USER_GROUP, group.trim().ifBlank { "10А класс" })
            .apply()
    }

    /**
     * Копирует выбранное изображение в локальное хранилище приложения, чтобы обложка
     * гарантированно сохранялась после перезапуска устройства и любых очисток временных URI.
     */
    fun persistPickedCoverImage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "covers").apply { mkdirs() }
            val dest = File(dir, "cover_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (dest.length() > 0L) Uri.fromFile(dest).toString() else null
        } catch (_: Exception) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    sourceUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                sourceUri.toString()
            } catch (_: Exception) {
                sourceUri.toString()
            }
        }
    }

    fun loadCustomEvents(context: Context): List<Event> = try {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_EVENTS, "[]")
        parseEventArray(raw ?: "[]", defaultUserCreated = true)
    } catch (_: Exception) {
        emptyList()
    }

    fun loadSeedOverrides(context: Context): Map<Int, Event> = try {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SEED_OVERRIDES, "[]")
        parseEventArray(raw ?: "[]", defaultUserCreated = false).associateBy { it.id }
    } catch (_: Exception) {
        emptyMap()
    }

    fun loadAllEvents(context: Context, seedDay: LocalDate): List<Event> {
        val overrides = loadSeedOverrides(context)
        val mergedSeeds = seedEvents(seedDay).map { seed -> overrides[seed.id] ?: seed }
        val custom = loadCustomEvents(context)
        return (mergedSeeds + custom).sortedBy { it.date }
    }

    fun upsertEvent(context: Context, event: Event) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seedIds = setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        if (event.id in seedIds && !event.isUserCreated) {
            val current = loadSeedOverrides(context).toMutableMap()
            current[event.id] = event
            prefs.edit()
                .putString(KEY_SEED_OVERRIDES, serializeEvents(current.values.toList()))
                .apply()
        } else {
            val existing = loadCustomEvents(context).toMutableList()
            val index = existing.indexOfFirst { it.id == event.id }
            val normalized = event.copy(isUserCreated = true, tint = tintForCategory(event.category))
            if (index >= 0) {
                existing[index] = normalized
            } else {
                existing.add(normalized)
            }
            prefs.edit()
                .putString(KEY_CUSTOM_EVENTS, serializeEvents(existing))
                .apply()
        }
    }

    fun deleteCustomEvent(context: Context, eventId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = loadCustomEvents(context).filterNot { it.id == eventId }
        val favorites = prefs.getStringSet("favorites", emptySet()).orEmpty() - eventId.toString()
        val joined = prefs.getStringSet("joined", emptySet()).orEmpty() - eventId.toString()
        prefs.edit()
            .putString(KEY_CUSTOM_EVENTS, serializeEvents(updated))
            .putStringSet("favorites", favorites)
            .putStringSet("joined", joined)
            .remove("reminder_$eventId")
            .remove("$KEY_ATTENDEES_PREFIX$eventId")
            .apply()
    }

    fun loadAttendees(context: Context, eventId: Int): List<AttendeeEntry> = try {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_ATTENDEES_PREFIX$eventId", "[]")
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).map { idx ->
            val obj = array.getJSONObject(idx)
            AttendeeEntry(
                id = obj.optString("id", "u_$idx"),
                name = obj.optString("name", "Участник"),
                group = obj.optString("group", ""),
                source = obj.optString("source", "app"),
                joinedAt = obj.optString("joinedAt", "")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun saveAttendees(context: Context, eventId: Int, list: List<AttendeeEntry>) {
        val array = JSONArray()
        list.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("group", item.group)
                    .put("source", item.source)
                    .put("joinedAt", item.joinedAt)
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_ATTENDEES_PREFIX$eventId", array.toString())
            .apply()
    }

    fun syncCurrentUserRegistration(
        context: Context,
        eventId: Int,
        joined: Boolean,
        viaQr: Boolean = false
    ): List<AttendeeEntry> {
        val current = loadAttendees(context, eventId).filterNot { it.id == "self" }.toMutableList()
        if (joined) {
            val now = LocalDateTime.now().format(TimeFmt)
            current.add(
                0,
                AttendeeEntry(
                    id = "self",
                    name = getStudentName(context),
                    group = getStudentGroup(context),
                    source = if (viaQr) "qr" else "app",
                    joinedAt = now
                )
            )
        }
        saveAttendees(context, eventId, current)
        return current
    }

    fun addExternalCheckInAttendee(
        context: Context,
        eventId: Int,
        name: String,
        group: String,
        source: String = "qr"
    ): List<AttendeeEntry> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return loadAttendees(context, eventId)
        val current = loadAttendees(context, eventId).toMutableList()
        val alreadyExists = current.any {
            it.name.equals(trimmedName, ignoreCase = true) &&
                it.group.equals(group.trim(), ignoreCase = true)
        }
        if (!alreadyExists) {
            current.add(
                0,
                AttendeeEntry(
                    id = "p_${System.currentTimeMillis()}",
                    name = trimmedName,
                    group = group.trim().ifBlank { "Школьное сообщество" },
                    source = source,
                    joinedAt = LocalDateTime.now().format(TimeFmt)
                )
            )
            saveAttendees(context, eventId, current)
        }
        return current
    }

    fun removeAttendee(context: Context, eventId: Int, attendeeId: String): List<AttendeeEntry> {
        val updated = loadAttendees(context, eventId).filterNot { it.id == attendeeId }
        saveAttendees(context, eventId, updated)
        return updated
    }

    fun totalParticipantCount(
        context: Context,
        event: Event,
        isCurrentUserJoined: Boolean,
        cachedAttendees: List<AttendeeEntry>? = null
    ): Int {
        val attendees = cachedAttendees ?: loadAttendees(context, event.id)
        val extraCheckIns = attendees.count { it.id != "self" }
        val selfAdd = if (isCurrentUserJoined) 1 else 0
        return event.baseParticipants + extraCheckIns + selfAdd
    }

    private fun parseEventArray(raw: String, defaultUserCreated: Boolean): List<Event> {
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val cat = obj.optString("category", "Наука")
            Event(
                id = obj.getInt("id"),
                title = obj.getString("title"),
                category = cat,
                date = LocalDateTime.parse(obj.getString("date")),
                place = obj.getString("place"),
                description = obj.getString("description"),
                subtitle = obj.optString("subtitle", "Событие школьного сообщества"),
                tint = tintForCategory(cat),
                photoUri = obj.optString("photoUri", ""),
                organizer = obj.optString("organizer", "Школьный актив"),
                capacity = obj.optInt("capacity", 40).coerceAtLeast(1),
                baseParticipants = obj.optInt("baseParticipants", 0).coerceAtLeast(0),
                isUserCreated = obj.optBoolean("isUserCreated", defaultUserCreated)
            )
        }
    }

    private fun serializeEvents(events: List<Event>): String {
        val array = JSONArray()
        events.forEach { event ->
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
                    .put("organizer", event.organizer)
                    .put("capacity", event.capacity)
                    .put("baseParticipants", event.baseParticipants)
                    .put("isUserCreated", event.isUserCreated)
            )
        }
        return array.toString()
    }
}
