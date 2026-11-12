package ru.campuslife.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Экран создания и редактирования мероприятия организатором (Create + Edit).
 */
@Composable
fun EventEditorScreen(
    existingEvent: Event?,
    onBack: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: ((Event) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()
    val submitGuard = remember { RapidTapGuard(600L) }
    val isEditing = existingEvent != null

    var title by rememberSaveable(existingEvent?.id) {
        mutableStateOf(existingEvent?.title.orEmpty())
    }
    var subtitle by rememberSaveable(existingEvent?.id) {
        mutableStateOf(existingEvent?.subtitle.orEmpty())
    }
    var place by rememberSaveable(existingEvent?.id) {
        mutableStateOf(existingEvent?.place.orEmpty())
    }
    var organizer by rememberSaveable(existingEvent?.id) {
        mutableStateOf(existingEvent?.organizer ?: "Школьный актив")
    }
    var capacityText by rememberSaveable(existingEvent?.id) {
        mutableStateOf((existingEvent?.capacity ?: 40).toString())
    }
    var desc by rememberSaveable(existingEvent?.id) {
        mutableStateOf(existingEvent?.description.orEmpty())
    }
    var category by rememberSaveable(existingEvent?.id) {
        mutableStateOf(existingEvent?.category ?: "Наука")
    }
    var dateIso by rememberSaveable(existingEvent?.id) {
        mutableStateOf(
            (existingEvent?.date ?: LocalDateTime.now()
                .plusDays(1)
                .withHour(16)
                .withMinute(0)
                .withSecond(0)
                .withNano(0))
                .toString()
        )
    }
    val date = remember(dateIso) { LocalDateTime.parse(dateIso) }

    var error by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable(existingEvent?.id) {
        mutableStateOf(existingEvent?.photoUri.orEmpty())
    }

    var showDateModal by rememberSaveable { mutableStateOf(false) }
    var showTimeModal by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val persisted = withContext(Dispatchers.IO) {
                    CampusStorage.persistPickedCoverImage(ctx, uri)
                }
                if (persisted != null) {
                    photoUri = persisted
                } else {
                    Toast.makeText(
                        ctx,
                        "Не удалось загрузить выбранную фотографию",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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

    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF161920),
                CampusColors.DeepObsidian,
                Color(0xFF0E1014)
            )
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
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
                            text = if (isEditing) "К карточке" else "К афише",
                            style = MaterialTheme.typography.labelLarge,
                            color = CampusColors.Milk
                        )
                    }

                    if (isEditing && existingEvent?.isUserCreated == true && onDelete != null) {
                        GlassIconButton(
                            onClick = { showDeleteConfirm = true },
                            size = 42.dp,
                            contentDescriptionLabel = "Удалить событие"
                        ) {
                            TrashGlyph(color = CampusColors.ErrorSoft, size = 18.dp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = if (isEditing) "Редактирование события" else "Новое событие",
                    style = MaterialTheme.typography.headlineLarge,
                    color = CampusColors.Milk
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isEditing) {
                        "Изменения сразу обновят афишу, билеты участников и QR-код регистрации."
                    } else {
                        "Заполни карточку организатора: после публикации событие получит собственный QR-код регистрации."
                    },
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
                            .background(if (bmp != null) Color(0xCC16191F) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        PhotoGlyph(color = CampusColors.WarmSand, size = 20.dp)
                        Text(
                            text = if (photoUri.isEmpty()) {
                                "Выбрать фотообложку события"
                            } else {
                                "Обложка сохранена · нажми, чтобы заменить"
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
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Короткий слоган на обложке (необязательно)") },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = organizer,
                        onValueChange = { organizer = it },
                        label = { Text("Организатор") },
                        modifier = Modifier.weight(1.35f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )

                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(4)
                            capacityText = digits
                            if (error.isNotEmpty()) error = ""
                        },
                        label = { Text("Мест") },
                        modifier = Modifier.weight(0.65f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )
                }

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
                    text = "Категория",
                    style = MaterialTheme.typography.labelLarge,
                    color = CampusColors.SlateMuted
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = EditableCategories, key = { it }) { item ->
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
                        val cap = capacityText.toIntOrNull() ?: 0
                        when {
                            title.isBlank() || place.isBlank() || desc.isBlank() -> {
                                error = "Заполни название, место проведения и описание события."
                            }
                            cap < 2 -> {
                                error = "Укажи корректное количество мест (минимум 2 участника)."
                            }
                            !isEditing && date.isBefore(LocalDateTime.now().minusMinutes(5)) -> {
                                error = "Выбери дату и время в будущем."
                            }
                            submitGuard.tryAcquire() -> {
                                val finalSubtitle = subtitle.trim().ifBlank {
                                    "Организатор: ${organizer.trim().ifBlank { "Школьный актив" }}"
                                }
                                val savedEvent = Event(
                                    id = existingEvent?.id
                                        ?: ((System.currentTimeMillis() % 800_000L).toInt() + 100),
                                    title = title.trim(),
                                    category = category,
                                    date = date,
                                    place = place.trim(),
                                    description = desc.trim(),
                                    subtitle = finalSubtitle,
                                    tint = tintForCategory(category),
                                    photoUri = photoUri,
                                    organizer = organizer.trim().ifBlank { "Школьный актив" },
                                    capacity = cap,
                                    baseParticipants = existingEvent?.baseParticipants ?: 0,
                                    isUserCreated = existingEvent?.isUserCreated ?: true
                                )
                                onSave(savedEvent)
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
                        text = if (isEditing) "Сохранить изменения" else "Опубликовать событие",
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

    if (showDeleteConfirm && existingEvent != null && onDelete != null) {
        CampusModalFrame(onDismiss = { showDeleteConfirm = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Удалить событие?",
                    style = MaterialTheme.typography.titleLarge,
                    color = CampusColors.Milk
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Мероприятие «${existingEvent.title}» будет удалено из афиши и списков записей.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CampusColors.SlateMuted
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Отмена",
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .pressableScale(onClick = { showDeleteConfirm = false })
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = CampusColors.SlateMuted
                    )

                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(existingEvent)
                        },
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CampusColors.ErrorSoft,
                            contentColor = CampusColors.DeepObsidian
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Удалить",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Панель организатора внутри карточки мероприятия: статистика заполненности, редактирование,
 * выпуск QR-кода для регистрации, сканер QR-билетов на входе и журнал отмеченных участников.
 */
@Composable
fun OrganizerEventPanel(
    event: Event,
    totalParticipants: Int,
    attendees: List<AttendeeEntry>,
    onEditEvent: () -> Unit,
    onShowEventQr: () -> Unit,
    onOpenCheckInScanner: () -> Unit,
    onOpenParticipantsModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val fillFraction = (totalParticipants.toFloat() / event.capacity.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = fillFraction,
        animationSpec = if (reducedMotion) snap() else tween(260, easing = CampusMotion.EmphasizedDecelerate),
        label = "capacityProgress"
    )
    val seatsLeft = (event.capacity - totalParticipants).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CampusColors.Graphite)
            .border(1.dp, CampusColors.Hairline, RoundedCornerShape(22.dp))
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
                OrganizerBadgeGlyph(color = CampusColors.SkyMuted, size = 18.dp)
                Column {
                    Text(
                        text = "Панель организатора",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = CampusColors.Milk
                    )
                    Text(
                        text = event.organizer,
                        style = MaterialTheme.typography.labelSmall,
                        color = CampusColors.SlateMuted
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CampusColors.GraphiteElevated)
                    .border(1.dp, CampusColors.Hairline, RoundedCornerShape(12.dp))
                    .pressableScale(pressedScale = 0.93f, onClick = onEditEvent)
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditGlyph(color = CampusColors.WarmSand, size = 15.dp)
                Text(
                    text = "Изменить",
                    style = MaterialTheme.typography.labelMedium,
                    color = CampusColors.Milk
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Индикатор количества записавшихся участников
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CampusColors.GraphiteSubtle)
                .border(1.dp, CampusColors.Hairline, RoundedCornerShape(16.dp))
                .pressableScale(pressedScale = 0.98f, onClick = onOpenParticipantsModal)
                .padding(13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UsersGlyph(color = CampusColors.WarmSand, size = 16.dp)
                    Text(
                        text = "Записалось: $totalParticipants из ${event.capacity}",
                        style = MaterialTheme.typography.labelLarge,
                        color = CampusColors.Milk
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(CampusColors.GraphiteElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFill)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                if (seatsLeft == 0) CampusColors.ErrorSoft else CampusColors.WarmSand
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (seatsLeft > 0) {
                        "Свободно мест: $seatsLeft · отмечено в журнале: ${attendees.size}"
                    } else {
                        "Все места заняты · отмечено в журнале: ${attendees.size}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = CampusColors.SlateMuted
                )
            }

            Spacer(Modifier.width(10.dp))

            ChevronRightGlyph(color = CampusColors.SlateMuted, size = 17.dp)
        }

        Spacer(Modifier.height(12.dp))

        // Действия с QR-кодами для организатора
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CampusColors.GraphiteElevated)
                    .border(1.dp, CampusColors.WarmSand.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
                    .pressableScale(pressedScale = 0.95f, onClick = onShowEventQr)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                QrCodeGlyph(color = CampusColors.WarmSand, size = 16.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "QR события",
                    style = MaterialTheme.typography.labelLarge,
                    color = CampusColors.Milk,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CampusColors.GraphiteElevated)
                    .border(1.dp, CampusColors.Hairline, RoundedCornerShape(14.dp))
                    .pressableScale(pressedScale = 0.95f, onClick = onOpenCheckInScanner)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ScanFrameGlyph(color = CampusColors.SkyMuted, size = 16.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Скан входа",
                    style = MaterialTheme.typography.labelLarge,
                    color = CampusColors.Milk,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Модальное окно списка участников и ручной отметки для организатора
 */
@Composable
fun OrganizerParticipantsModal(
    event: Event,
    totalParticipants: Int,
    attendees: List<AttendeeEntry>,
    onAddManualAttendee: (String, String) -> Unit,
    onRemoveAttendee: (String) -> Unit,
    onOpenCheckInScanner: () -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newGroup by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CampusColors.SkyMuted.copy(alpha = 0.6f),
        unfocusedBorderColor = CampusColors.Hairline,
        focusedContainerColor = CampusColors.Graphite,
        unfocusedContainerColor = CampusColors.Graphite,
        cursorColor = CampusColors.WarmSand
    )

    CampusModalFrame(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Участники события",
                        style = MaterialTheme.typography.titleMedium,
                        color = CampusColors.Milk
                    )
                    Text(
                        text = "Всего зарегистрировано: $totalParticipants из ${event.capacity}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = CampusColors.WarmSand
                    )
                }

                GlassIconButton(
                    onClick = onDismiss,
                    size = 34.dp,
                    debounce = false,
                    contentDescriptionLabel = "Закрыть список"
                ) {
                    CloseGlyph(color = CampusColors.Milk, size = 14.dp)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (event.baseParticipants > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CampusColors.Graphite)
                        .border(1.dp, CampusColors.Hairline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "Предварительная запись от классов: ${event.baseParticipants} чел.",
                        style = MaterialTheme.typography.labelMedium,
                        color = CampusColors.SkyMuted
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            if (attendees.isEmpty()) {
                Text(
                    text = "Персональных отметок с этого устройства пока нет. Запишись сам, отсканируй QR-билет или добавь участника ниже.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    color = CampusColors.SlateMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    attendees.take(8).forEach { person ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CampusColors.Graphite)
                                .border(1.dp, CampusColors.Hairline, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CampusColors.Milk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val badgeText = when (person.source) {
                                    "qr" -> "QR-код"
                                    "organizer" -> "На входе"
                                    else -> "В приложении"
                                }
                                Text(
                                    text = "${person.group} · $badgeText ${if (person.joinedAt.isNotEmpty()) "в ${person.joinedAt}" else ""}".trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CampusColors.SlateMuted
                                )
                            }

                            if (person.id != "self") {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(CampusColors.GraphiteElevated)
                                        .pressableScale(
                                            pressedScale = 0.9f,
                                            onClick = { onRemoveAttendee(person.id) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CloseGlyph(color = CampusColors.SlateMuted, size = 12.dp)
                                }
                            } else {
                                Text(
                                    text = "Вы",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CampusColors.Success
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Быстрая отметка участника на месте",
                style = MaterialTheme.typography.labelLarge,
                color = CampusColors.Milk
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Имя и фамилия", style = MaterialTheme.typography.bodyMedium, color = CampusColors.SlateMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(1.25f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = CampusColors.Milk),
                    shape = RoundedCornerShape(13.dp),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = newGroup,
                    onValueChange = { newGroup = it },
                    placeholder = { Text("Класс", style = MaterialTheme.typography.bodyMedium, color = CampusColors.SlateMuted) },
                    singleLine = true,
                    modifier = Modifier.weight(0.75f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = CampusColors.Milk),
                    shape = RoundedCornerShape(13.dp),
                    colors = fieldColors
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAddManualAttendee(newName, newGroup.ifBlank { "9Б класс" })
                            newName = ""
                            newGroup = ""
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CampusColors.WarmSand,
                        contentColor = CampusColors.DeepObsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Добавить",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = {
                        onDismiss()
                        onOpenCheckInScanner()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CampusColors.GraphiteElevated,
                        contentColor = CampusColors.Milk
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    ScanFrameGlyph(color = CampusColors.WarmSand, size = 15.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "По QR",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Модальное окно карточки ученика (имя и класс подставляются в личный QR-билет при записи)
 */
@Composable
fun StudentProfileModal(
    initialName: String,
    initialGroup: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var group by remember { mutableStateOf(initialGroup) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CampusColors.SkyMuted.copy(alpha = 0.6f),
        unfocusedBorderColor = CampusColors.Hairline,
        focusedContainerColor = CampusColors.Graphite,
        unfocusedContainerColor = CampusColors.Graphite,
        focusedLabelColor = CampusColors.SkyMuted,
        unfocusedLabelColor = CampusColors.SlateMuted,
        cursorColor = CampusColors.WarmSand
    )

    CampusModalFrame(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Профиль участника для QR-билета",
                style = MaterialTheme.typography.titleMedium,
                color = CampusColors.Milk
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Эти данные кодируются в твой персональный QR-билет для быстрой регистрации у организатора.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                color = CampusColors.SlateMuted
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя и фамилия") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
                shape = RoundedCornerShape(15.dp),
                colors = fieldColors
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = group,
                onValueChange = { group = it },
                label = { Text("Класс или команда") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = CampusColors.Milk),
                shape = RoundedCornerShape(15.dp),
                colors = fieldColors
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Отмена",
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .pressableScale(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = CampusColors.SlateMuted
                )

                Button(
                    onClick = { onSave(name, group) },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CampusColors.WarmSand,
                        contentColor = CampusColors.DeepObsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = "Сохранить",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
