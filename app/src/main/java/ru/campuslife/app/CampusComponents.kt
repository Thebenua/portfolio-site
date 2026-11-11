package ru.campuslife.app

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal val RuLocale: Locale = Locale.forLanguageTag("ru")
internal val FullDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM, EEEE", RuLocale)
internal val ShortDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", RuLocale)
internal val TimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", RuLocale)

internal fun relativeDayBadge(date: LocalDateTime): String? {
    val today = LocalDate.now()
    val target = date.toLocalDate()
    val diff = ChronoUnit.DAYS.between(today, target)
    return when {
        diff == 0L -> "Сегодня"
        diff == 1L -> "Завтра"
        diff in 2L..6L -> "Через $diff дн."
        else -> null
    }
}

internal fun reminderLabel(minutes: Int): String = when (minutes) {
    10 -> "Напоминание за 10 мин"
    60 -> "Напоминание за 1 час"
    1440 -> "Напоминание за 1 день"
    else -> "Без напоминания"
}

/**
 * Мягкий тактильный модификатор нажатия для карточек и кнопок (180 мс, учитывает prefers-reduced-motion)
 */
@Composable
fun Modifier.pressableScale(
    pressedScale: Float = 0.975f,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier {
    val reducedMotion = LocalReducedMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion && enabled) pressedScale else 1f,
        animationSpec = if (reducedMotion) snap() else tween(180, easing = FastOutSlowInEasing),
        label = "pressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            role = role,
            onClick = onClick
        )
}

/**
 * Круглая или капсульная стеклянная кнопка иконки (назад, избранное, создание события)
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    accent: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                if (accent) {
                    Brush.linearGradient(
                        colors = listOf(
                            CampusColors.WarmSand.copy(alpha = 0.22f),
                            CampusColors.GraphiteElevated
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            CampusColors.GraphiteElevated,
                            CampusColors.Graphite
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (accent) CampusColors.WarmSand.copy(alpha = 0.34f) else CampusColors.Hairline,
                shape = shape
            )
            .pressableScale(pressedScale = 0.93f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Единый фильтр-чип дизайн-системы Campus Life
 */
@Composable
fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val reducedMotion = LocalReducedMotion.current
    val bgColor by animateColorAsState(
        targetValue = if (selected) CampusColors.Milk else CampusColors.Graphite,
        animationSpec = if (reducedMotion) snap() else tween(190),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) CampusColors.DeepObsidian else CampusColors.SlateMuted,
        animationSpec = if (reducedMotion) snap() else tween(190),
        label = "chipText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) CampusColors.Milk else CampusColors.Hairline,
        animationSpec = if (reducedMotion) snap() else tween(190),
        label = "chipBorder"
    )

    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .pressableScale(pressedScale = 0.95f, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingContent != null) {
            leadingContent()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

/**
 * Основная карточка мероприятия для «Афиши» и «Избранного»
 */
@Composable
fun PosterEventCard(
    event: Event,
    isFavorite: Boolean,
    isJoined: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)
    val relBadge = remember(event.date) { relativeDayBadge(event.date) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(CampusColors.Graphite)
            .border(1.dp, CampusColors.Hairline, cardShape)
            .pressableScale(pressedScale = 0.982f, onClick = onClick)
    ) {
        // Фотообложка с мягкой многослойной виньеткой и стеклянными метками
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .background(event.tint)
        ) {
            EventCoverImage(event = event)

            // Кинематографический градиент для безупречной читаемости текста поверх любой фотографии
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0x520D0F13),
                            0.42f to Color(0x1F0D0F13),
                            1.0f to Color(0xDB111317)
                        )
                    )
            )

            // Верхний ряд: категория + статус записи / дата + кнопка избранного
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color(0xAD15181E))
                            .border(0.8.dp, CampusColors.Milk.copy(alpha = 0.16f), RoundedCornerShape(percent = 50))
                            .padding(horizontal = 11.dp, vertical = 5.5.dp)
                    ) {
                        Text(
                            text = event.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                letterSpacing = 0.2.sp
                            ),
                            color = CampusColors.Milk
                        )
                    }

                    if (isJoined) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xBF17231E))
                                .border(0.8.dp, CampusColors.Success.copy(alpha = 0.38f), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 10.dp, vertical = 5.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CheckGlyph(color = CampusColors.Success, size = 12.dp)
                            Text(
                                text = "Записан",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = CampusColors.Success
                            )
                        }
                    } else if (relBadge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0x9E1C2128))
                                .padding(horizontal = 9.5.dp, vertical = 5.5.dp)
                        ) {
                            Text(
                                text = relBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = CampusColors.SkyMuted
                            )
                        }
                    }
                }

                // Кнопка сердечка в стеклянном круге
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xAD15181E))
                        .border(
                            width = 0.8.dp,
                            color = if (isFavorite) CampusColors.WarmSand.copy(alpha = 0.5f) else CampusColors.Milk.copy(alpha = 0.16f),
                            shape = CircleShape
                        )
                        .pressableScale(pressedScale = 0.88f, onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    NavHeartIcon(
                        color = if (isFavorite) CampusColors.WarmSand else CampusColors.Milk,
                        active = isFavorite,
                        filledWhenActive = true,
                        size = 18.dp
                    )
                }
            }

            // Нижний подзаголовок внутри обложки
            Text(
                text = event.subtitle,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = CampusColors.Milk.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Текстовый блок карточки
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
                color = CampusColors.Milk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(11.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    ClockGlyph(color = CampusColors.WarmSand, size = 14.5.dp)
                    Text(
                        text = "${event.date.format(ShortDateFmt)}, ${event.date.format(TimeFmt)}",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                        color = CampusColors.WarmSand,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PinGlyph(color = CampusColors.SlateMuted, size = 14.5.dp)
                    Text(
                        text = event.place,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = CampusColors.SlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Специальная карточка-билет для раздела «Записи» с датой, статусом, напоминанием и линией перфорации
 */
@Composable
fun BookingTicketCard(
    event: Event,
    reminderMinutes: Int,
    onClick: () -> Unit,
    onCancelBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    val dayNumber = remember(event.date) { event.date.dayOfMonth.toString().padStart(2, '0') }
    val monthShort = remember(event.date) {
        event.date.format(DateTimeFormatter.ofPattern("MMM", RuLocale)).trimEnd('.')
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CampusColors.Graphite)
            .border(1.dp, CampusColors.Hairline, shape)
            .pressableScale(pressedScale = 0.982f, onClick = onClick)
    ) {
        // Верхняя часть билета: блок даты + название + миниатюра
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Календарный бейдж даты
            Column(
                modifier = Modifier
                    .width(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CampusColors.GraphiteElevated)
                    .border(1.dp, CampusColors.Hairline, RoundedCornerShape(16.dp))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayNumber,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CampusColors.Milk
                )
                Text(
                    text = monthShort,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = CampusColors.WarmSand
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(CampusColors.Success.copy(alpha = 0.14f))
                            .padding(horizontal = 8.5.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CheckGlyph(color = CampusColors.Success, size = 11.dp)
                        Text(
                            text = "Место за тобой",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = CampusColors.Success
                        )
                    }

                    Text(
                        text = event.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = CampusColors.SlateMuted
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    color = CampusColors.Milk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "${event.date.format(TimeFmt)} · ${event.place}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    color = CampusColors.SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Компактная превью-обложка
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(event.tint)
            ) {
                EventCoverImage(event = event)
            }
        }

        // Линия перфорации билета с боковыми полукруглыми вырезами
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height / 2f
                // Вырезы цвета фона экрана
                drawCircle(
                    color = CampusColors.DeepObsidian,
                    radius = 7.dp.toPx(),
                    center = Offset(0f, y)
                )
                drawCircle(
                    color = CampusColors.DeepObsidian,
                    radius = 7.dp.toPx(),
                    center = Offset(size.width, y)
                )
                // Пунктирная линия контроля
                drawLine(
                    color = CampusColors.Milk.copy(alpha = 0.13f),
                    start = Offset(16.dp.toPx(), y),
                    end = Offset(size.width - 16.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            }
        }

        // Нижний корешок билета: напоминание + аккуратное действие отмены
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BellGlyph(
                    color = if (reminderMinutes > 0) CampusColors.SkyMuted else CampusColors.SlateMuted,
                    size = 15.dp
                )
                Text(
                    text = reminderLabel(reminderMinutes),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = if (reminderMinutes > 0) CampusColors.SkyMuted else CampusColors.SlateMuted
                )
            }

            Text(
                text = "Отменить",
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(CampusColors.GraphiteElevated)
                    .pressableScale(pressedScale = 0.94f, onClick = onCancelBooking)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                color = CampusColors.Milk.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Продуманное пустое состояние для каждого из трёх разделов и поисковой выдачи
 */
@Composable
fun CampusEmptyState(
    tab: CampusTab,
    hasActiveFilters: Boolean,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title: String
    val subtitle: String
    val actionLabel: String

    when {
        tab == CampusTab.Poster && hasActiveFilters -> {
            title = "По этим фильтрам пусто"
            subtitle = "Попробуй изменить поисковый запрос, категорию или выбранный период дат."
            actionLabel = "Сбросить фильтры"
        }
        tab == CampusTab.Favorites -> {
            title = "В избранном пока тихо"
            subtitle = "Отмечай сердечком интересные лекции, турниры и встречи — они соберутся здесь."
            actionLabel = "Открыть афишу"
        }
        tab == CampusTab.Bookings -> {
            title = "Записей пока нет"
            subtitle = "Выбери событие в афише и нажми «Записаться» — билет и напоминание появятся в этом разделе."
            actionLabel = "Выбрать событие"
        }
        else -> {
            title = "Событий пока нет"
            subtitle = "Создай первое мероприятие для своего кампуса через кнопку «+» в правом верхнем углу."
            actionLabel = "Создать событие"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CampusColors.Graphite)
                .border(1.dp, CampusColors.Hairline, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (tab) {
                CampusTab.Poster -> NavPosterIcon(color = CampusColors.SkyMuted, active = true, size = 27.dp)
                CampusTab.Favorites -> NavHeartIcon(color = CampusColors.WarmSand, active = true, size = 27.dp)
                CampusTab.Bookings -> NavTicketIcon(color = CampusColors.SkyMuted, active = true, size = 27.dp)
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = CampusColors.Milk,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = CampusColors.SlateMuted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onPrimaryAction,
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(
                containerColor = CampusColors.GraphiteElevated,
                contentColor = CampusColors.Milk
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Информационная карточка метаданных в экране деталей события
 */
@Composable
fun DetailMetaTile(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CampusColors.Graphite)
            .border(1.dp, CampusColors.Hairline, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CampusColors.GraphiteElevated),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CampusColors.SlateMuted
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = CampusColors.Milk
            )
        }
    }
}

/**
 * Рендеринг фотообложки события (пользовательское URI или демонстрационный ресурс)
 */
@Composable
internal fun EventCoverImage(event: Event) {
    val context = LocalContext.current
    val bitmap = remember(event.photoUri) {
        if (event.photoUri.isBlank()) null else try {
            val uri = Uri.parse(event.photoUri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) {
                sample *= 2
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                )?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = event.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        val cover = when (event.id) {
            1 -> R.drawable.cover_science
            2 -> R.drawable.cover_art
            3 -> R.drawable.cover_sport
            4 -> R.drawable.cover_tech
            5 -> R.drawable.cover_music
            else -> R.drawable.event_cover
        }
        Image(
            painter = painterResource(cover),
            contentDescription = event.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
