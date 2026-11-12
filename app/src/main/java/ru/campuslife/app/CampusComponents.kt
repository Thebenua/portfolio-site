package ru.campuslife.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    5 -> "Напоминание за 5 мин"
    10 -> "Напоминание за 10 мин"
    15 -> "Напоминание за 15 мин"
    60 -> "Напоминание за 1 час"
    1440 -> "Напоминание за 1 день"
    else -> "Без напоминания"
}

private val CardCornerShape = RoundedCornerShape(24.dp)
private val CoverScrimBrush = Brush.verticalGradient(
    0.0f to Color(0x520D0F13),
    0.42f to Color(0x1F0D0F13),
    1.0f to Color(0xDB111317)
)
private val TicketDashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)

/**
 * Мягкий тактильный модификатор нажатия в фазе GPU-отрисовки (graphicsLayer) с опциональной защитой от двойного клика.
 */
@Composable
fun Modifier.pressableScale(
    pressedScale: Float = 0.975f,
    enabled: Boolean = true,
    debounce: Boolean = true,
    debounceMs: Long = 260L,
    role: Role? = Role.Button,
    onClick: () -> Unit
): Modifier {
    val reducedMotion = LocalReducedMotion.current
    val interaction = remember { MutableInteractionSource() }
    val guard = remember(debounceMs) { RapidTapGuard(debounceMs) }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion && enabled) pressedScale else 1f,
        animationSpec = if (reducedMotion) snap() else CampusMotion.tactileSpring(),
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
            onClick = {
                if (!debounce || guard.tryAcquire()) {
                    onClick()
                }
            }
        )
}

/**
 * Стеклянная кнопка иконки (назад, создание события, навигация по месяцам)
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    accent: Boolean = false,
    debounce: Boolean = true,
    contentDescriptionLabel: String? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val bgBrush = remember(accent) {
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
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (accent) CampusColors.WarmSand.copy(alpha = 0.34f) else CampusColors.Hairline,
                shape = shape
            )
            .then(
                if (contentDescriptionLabel != null) {
                    Modifier.semantics { contentDescription = contentDescriptionLabel }
                } else {
                    Modifier
                }
            )
            .pressableScale(pressedScale = 0.92f, debounce = debounce, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Кнопка избранного с упругим spring-импульсом при добавлении сердечка и тактильным откликом
 */
@Composable
fun FavoriteHeartButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 18.5.dp,
    glassCircle: Boolean = true
) {
    val reducedMotion = LocalReducedMotion.current
    val haptics = LocalHapticFeedback.current
    var pulseCount by remember { mutableIntStateOf(0) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(pulseCount) {
        if (pulseCount > 0 && !reducedMotion) {
            pulseScale.snapTo(0.82f)
            pulseScale.animateTo(1.16f, tween(115, easing = CampusMotion.EmphasizedDecelerate))
            pulseScale.animateTo(1f, CampusMotion.heartBounceSpring())
        }
    }

    val tint by animateColorAsState(
        targetValue = if (isFavorite) CampusColors.WarmSand else CampusColors.Milk,
        animationSpec = if (reducedMotion) snap() else tween(180),
        label = "heartTint"
    )
    val borderTint by animateColorAsState(
        targetValue = if (isFavorite) CampusColors.WarmSand.copy(alpha = 0.52f) else CampusColors.Milk.copy(alpha = 0.16f),
        animationSpec = if (reducedMotion) snap() else tween(180),
        label = "heartBorder"
    )

    val containerShape = if (glassCircle) CircleShape else RoundedCornerShape(14.dp)
    val containerBg = if (glassCircle) Color(0xB315181E) else CampusColors.GraphiteElevated

    Box(
        modifier = modifier
            .size(size)
            .clip(containerShape)
            .background(containerBg)
            .border(width = 0.9.dp, color = borderTint, shape = containerShape)
            .semantics {
                contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное"
            }
            .pressableScale(
                pressedScale = 0.90f,
                debounce = false,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pulseCount++
                    onToggle()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = pulseScale.value
                scaleY = pulseScale.value
            }
        ) {
            NavHeartIcon(
                color = tint,
                active = isFavorite,
                filledWhenActive = true,
                size = iconSize
            )
        }
    }
}

/**
 * Единый фильтр-чип дизайн-системы Campus Life (мгновенная реакция на быстрые переключения без блокировки)
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
        animationSpec = if (reducedMotion) snap() else tween(180, easing = CampusMotion.EmphasizedDecelerate),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) CampusColors.DeepObsidian else CampusColors.SlateMuted,
        animationSpec = if (reducedMotion) snap() else tween(180, easing = CampusMotion.EmphasizedDecelerate),
        label = "chipText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) CampusColors.Milk else CampusColors.Hairline,
        animationSpec = if (reducedMotion) snap() else tween(180),
        label = "chipBorder"
    )

    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .pressableScale(pressedScale = 0.94f, debounce = false, onClick = onClick)
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
 * Основная карточка мероприятия для «Афиши» и «Избранного» (неблокирующая обложка + кэшированные кисти)
 */
@Composable
fun PosterEventCard(
    event: Event,
    isFavorite: Boolean,
    isJoined: Boolean,
    participantsCount: Int,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val relBadge = remember(event.date) { relativeDayBadge(event.date) }
    val dateSummary = remember(event.date) {
        "${event.date.format(ShortDateFmt)}, ${event.date.format(TimeFmt)}"
    }
    val seatsSummary = remember(participantsCount, event.capacity) {
        "$participantsCount/${event.capacity}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardCornerShape)
            .background(CampusColors.Graphite)
            .border(1.dp, CampusColors.Hairline, CardCornerShape)
            .pressableScale(pressedScale = 0.984f, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .background(event.tint)
        ) {
            AsyncEventCover(event = event, maxDimension = 1280)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CoverScrimBrush)
            )

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
                            .background(Color(0xB315181E))
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

                    AnimatedContent(
                        targetState = isJoined,
                        transitionSpec = {
                            if (reducedMotion) {
                                fadeIn(snap()) togetherWith fadeOut(snap())
                            } else {
                                (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.9f))
                                    .togetherWith(fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.9f))
                            }
                        },
                        label = "cardJoinedStatus"
                    ) { joinedState ->
                        if (joinedState) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Color(0xC417231E))
                                    .border(0.8.dp, CampusColors.Success.copy(alpha = 0.42f), RoundedCornerShape(percent = 50))
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
                                    .background(Color(0xA61C2128))
                                    .padding(horizontal = 9.5.dp, vertical = 5.5.dp)
                            ) {
                                Text(
                                    text = relBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = CampusColors.SkyMuted
                                )
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                    }
                }

                FavoriteHeartButton(
                    isFavorite = isFavorite,
                    onToggle = onToggleFavorite,
                    size = 37.dp,
                    iconSize = 18.dp,
                    glassCircle = true
                )
            }

            Text(
                text = event.subtitle,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = CampusColors.Milk.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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
                        text = dateSummary,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                        color = CampusColors.WarmSand,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        UsersGlyph(color = CampusColors.SkyMuted, size = 14.dp)
                        Text(
                            text = seatsSummary,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                            color = CampusColors.SkyMuted,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PinGlyph(color = CampusColors.SlateMuted, size = 14.dp)
                        Text(
                            text = event.place,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = CampusColors.SlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
    onShowQrTicket: () -> Unit,
    onCancelBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val dayNumber = remember(event.date) { event.date.dayOfMonth.toString().padStart(2, '0') }
    val monthShort = remember(event.date) {
        event.date.format(DateTimeFormatter.ofPattern("MMM", RuLocale)).trimEnd('.')
    }
    val timeAndPlace = remember(event.date, event.place) {
        "${event.date.format(TimeFmt)} · ${event.place}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardCornerShape)
            .background(CampusColors.Graphite)
            .border(1.dp, CampusColors.Hairline, CardCornerShape)
            .pressableScale(pressedScale = 0.984f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                    text = timeAndPlace,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    color = CampusColors.SlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(event.tint)
            ) {
                AsyncEventCover(event = event, maxDimension = 360)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height / 2f
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
                drawLine(
                    color = CampusColors.Milk.copy(alpha = 0.13f),
                    start = Offset(16.dp.toPx(), y),
                    end = Offset(size.width - 16.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = TicketDashEffect
                )
            }
        }

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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(CampusColors.WarmSand.copy(alpha = 0.16f))
                        .border(0.8.dp, CampusColors.WarmSand.copy(alpha = 0.42f), RoundedCornerShape(percent = 50))
                        .pressableScale(
                            pressedScale = 0.93f,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onShowQrTicket()
                            }
                        )
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    QrCodeGlyph(color = CampusColors.WarmSand, size = 13.dp)
                    Text(
                        text = "QR-билет",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                        color = CampusColors.WarmSand
                    )
                }

                Text(
                    text = "Отменить",
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(CampusColors.GraphiteElevated)
                        .pressableScale(
                            pressedScale = 0.93f,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCancelBooking()
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                    color = CampusColors.Milk.copy(alpha = 0.88f)
                )
            }
        }
    }
}

/**
 * Плавное пустое состояние разделов и поисковой выдачи
 */
@Composable
fun CampusEmptyState(
    tab: CampusTab,
    hasActiveFilters: Boolean,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    var visible by remember { androidx.compose.runtime.mutableStateOf(reducedMotion) }
    LaunchedEffect(tab, hasActiveFilters) {
        visible = true
    }

    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(220, easing = CampusMotion.EmphasizedDecelerate),
        label = "emptyAlpha"
    )
    val enterScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = if (reducedMotion) snap() else CampusMotion.navSpring(),
        label = "emptyScale"
    )

    val title: String
    val subtitle: String
    val actionLabel: String

    when {
        tab == CampusTab.Poster && hasActiveFilters -> {
            title = "По этим фильтрам пусто"
            subtitle = "Попробуй изменить поисковый запрос, направление или выбранный период дат."
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
            .graphicsLayer {
                alpha = enterAlpha
                scaleX = enterScale
                scaleY = enterScale
            }
            .padding(horizontal = 28.dp, vertical = 34.dp),
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

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = onPrimaryAction,
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(
                containerColor = CampusColors.GraphiteElevated,
                contentColor = CampusColors.Milk
            ),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

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
