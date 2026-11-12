package ru.campuslife.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MonthYearFmt = DateTimeFormatter.ofPattern("LLLL yyyy", RuLocale)
private val WeekDayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/**
 * Базовый контейнер модального окна с плавным появлением (scale + fade 210 мс) в едином стиле Campus Life
 */
@Composable
internal fun CampusModalFrame(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    var entered by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(Unit) {
        if (!entered) entered = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(200, easing = CampusMotion.EmphasizedDecelerate),
        label = "modalAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec = if (reducedMotion) snap() else CampusMotion.navSpring(),
        label = "modalScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f * alpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 390.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF252A32),
                                CampusColors.GraphiteSubtle
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CampusColors.Milk.copy(alpha = 0.20f),
                                CampusColors.Hairline
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(20.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Фирменное модальное окно выбора даты (заменяет устаревший системный DatePickerDialog)
 */
@Composable
fun CampusDateModal(
    initialDate: LocalDate,
    allowClear: Boolean = false,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
    onConfirm: (LocalDate) -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    var selectedDate by remember { mutableStateOf(initialDate) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val today = remember { LocalDate.now() }

    CampusModalFrame(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Заголовок месяца и переключение
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    onClick = { currentMonth = currentMonth.minusMonths(1) },
                    size = 38.dp,
                    debounce = false,
                    contentDescriptionLabel = "Предыдущий месяц"
                ) {
                    BackArrowGlyph(color = CampusColors.Milk, size = 17.dp)
                }

                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        }
                    },
                    label = "calendarMonthTitle"
                ) { ym ->
                    val raw = ym.atDay(1).format(MonthYearFmt)
                    val capitalized = raw.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(RuLocale) else it.toString()
                    }
                    Text(
                        text = capitalized,
                        style = MaterialTheme.typography.titleMedium,
                        color = CampusColors.Milk
                    )
                }

                GlassIconButton(
                    onClick = { currentMonth = currentMonth.plusMonths(1) },
                    size = 38.dp,
                    debounce = false,
                    contentDescriptionLabel = "Следующий месяц"
                ) {
                    ChevronRightGlyph(color = CampusColors.Milk, size = 17.dp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Дни недели
            Row(modifier = Modifier.fillMaxWidth()) {
                WeekDayLabels.forEach { wd ->
                    Text(
                        text = wd,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = CampusColors.SlateMuted
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Сетка чисел месяца (6 строк максимум, плавная высота)
            val firstDayOffset = currentMonth.atDay(1).dayOfWeek.value - 1 // 0..6 (Пн..Вс)
            val daysInMonth = currentMonth.lengthOfMonth()
            val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (week in 0 until (totalCells / 7)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (dow in 0 until 7) {
                            val cellIndex = week * 7 + dow
                            val dayNum = cellIndex - firstDayOffset + 1
                            if (dayNum in 1..daysInMonth) {
                                val cellDate = currentMonth.atDay(dayNum)
                                val isSelected = cellDate == selectedDate
                                val isToday = cellDate == today

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> CampusColors.WarmSand
                                                    isToday -> CampusColors.GraphiteElevated
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(1.dp, CampusColors.SkyMuted.copy(alpha = 0.45f), CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .pressableScale(
                                                pressedScale = 0.90f,
                                                debounce = false,
                                                onClick = { selectedDate = cellDate }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) CampusColors.DeepObsidian else CampusColors.Milk
                                        )
                                    }
                                }
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Кнопки подтверждения
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (allowClear && onClear != null) {
                    Text(
                        text = "Сбросить",
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .pressableScale(onClick = onClear)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = CampusColors.SlateMuted
                    )
                } else {
                    Text(
                        text = "Отмена",
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .pressableScale(onClick = onDismiss)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = CampusColors.SlateMuted
                    )
                }

                Button(
                    onClick = { onConfirm(selectedDate) },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CampusColors.WarmSand,
                        contentColor = CampusColors.DeepObsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = "Выбрать",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

/**
 * Фирменное модальное окно выбора времени начала события
 */
@Composable
fun CampusTimeModal(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialTime.hour) }
    var minute by remember { mutableIntStateOf((initialTime.minute / 5) * 5) }

    val quickTimes = remember {
        listOf(
            12 to 0,
            14 to 30,
            15 to 0,
            16 to 30,
            18 to 0
        )
    }

    CampusModalFrame(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Время начала",
                style = MaterialTheme.typography.titleMedium,
                color = CampusColors.Milk
            )

            Spacer(Modifier.height(18.dp))

            // Крупный интерактивный барабан часов и минут
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TimeStepperColumn(
                    valueText = hour.toString().padStart(2, '0'),
                    caption = "Часы",
                    onIncrement = { hour = (hour + 1) % 24 },
                    onDecrement = { hour = (hour + 23) % 24 }
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    color = CampusColors.WarmSand
                )

                TimeStepperColumn(
                    valueText = minute.toString().padStart(2, '0'),
                    caption = "Минуты",
                    onIncrement = { minute = (minute + 5) % 60 },
                    onDecrement = { minute = (minute + 55) % 60 }
                )
            }

            Spacer(Modifier.height(18.dp))

            // Быстрые слоты популярного времени кампуса
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                quickTimes.forEach { (qh, qm) ->
                    val label = "${qh.toString().padStart(2, '0')}:${qm.toString().padStart(2, '0')}"
                    val active = hour == qh && minute == qm
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (active) CampusColors.Milk else CampusColors.GraphiteElevated)
                            .pressableScale(
                                pressedScale = 0.94f,
                                debounce = false,
                                onClick = {
                                    hour = qh
                                    minute = qm
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (active) CampusColors.DeepObsidian else CampusColors.SlateMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

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
                    onClick = { onConfirm(LocalTime.of(hour, minute)) },
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CampusColors.WarmSand,
                        contentColor = CampusColors.DeepObsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = "Готово",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeStepperColumn(
    valueText: String,
    caption: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CampusColors.GraphiteElevated)
                .pressableScale(pressedScale = 0.92f, debounce = false, onClick = onIncrement),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleMedium, color = CampusColors.Milk)
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(88.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CampusColors.Graphite)
                .border(1.dp, CampusColors.HairlineStrong, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CampusColors.Milk
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(88.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CampusColors.GraphiteElevated)
                .pressableScale(pressedScale = 0.92f, debounce = false, onClick = onDecrement),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "−", style = MaterialTheme.typography.titleMedium, color = CampusColors.Milk)
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = CampusColors.SlateMuted
        )
    }
}
