package ru.campuslife.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Оригинальный фирменный знак Campus Life с кэшированием градиентов в drawWithCache (0 аллокаций на кадр).
 */
@Composable
fun CampusBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    withContainer: Boolean = true,
    monochrome: Boolean = false
) {
    val shape = remember(size) { RoundedCornerShape(size * 0.30f) }
    val bgBrush = remember {
        Brush.linearGradient(colors = listOf(Color(0xFF262B33), Color(0xFF15181D)))
    }
    val rimBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                CampusColors.Milk.copy(alpha = 0.22f),
                CampusColors.SkyMuted.copy(alpha = 0.05f)
            )
        )
    }

    val containerModifier = if (withContainer) {
        modifier
            .size(size)
            .clip(shape)
            .background(bgBrush)
            .border(width = 1.dp, brush = rimBrush, shape = shape)
    } else {
        modifier.size(size)
    }

    Box(modifier = containerModifier, contentAlignment = Alignment.Center) {
        val glyphFraction = if (withContainer) 0.66f else 1f
        Box(
            modifier = Modifier
                .size(size * glyphFraction)
                .drawWithCache {
                    val s = this.size.minDimension / 100f
                    val outerBrush = if (monochrome) {
                        Brush.linearGradient(listOf(CampusColors.Milk, CampusColors.Milk))
                    } else {
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFFFFF), CampusColors.Milk, CampusColors.SkyMuted),
                            start = Offset(18f * s, 16f * s),
                            end = Offset(82f * s, 84f * s)
                        )
                    }
                    val outerStroke = Stroke(width = 7.2f * s, cap = StrokeCap.Round)
                    val midStroke = Stroke(width = 6.5f * s, cap = StrokeCap.Round)
                    val innerStroke = Stroke(width = 5.9f * s, cap = StrokeCap.Round)

                    val outerTopLeft = Offset((51.5f - 32.5f) * s, (50.0f - 32.5f) * s)
                    val outerSize = Size(65.0f * s, 65.0f * s)

                    val midTopLeft = Offset((54.5f - 21.2f) * s, (48.5f - 21.2f) * s)
                    val midSize = Size(42.4f * s, 42.4f * s)

                    val innerTopLeft = Offset((57.0f - 10.8f) * s, (50.8f - 10.8f) * s)
                    val innerSize = Size(21.6f * s, 21.6f * s)

                    val midColor = if (monochrome) CampusColors.Milk else CampusColors.SkyMuted.copy(alpha = 0.92f)
                    val innerColor = if (monochrome) CampusColors.Milk else CampusColors.WarmSand

                    onDrawBehind {
                        drawArc(
                            brush = outerBrush,
                            startAngle = 40f,
                            sweepAngle = 284f,
                            useCenter = false,
                            topLeft = outerTopLeft,
                            size = outerSize,
                            style = outerStroke
                        )
                        drawArc(
                            color = midColor,
                            startAngle = 68f,
                            sweepAngle = 236f,
                            useCenter = false,
                            topLeft = midTopLeft,
                            size = midSize,
                            style = midStroke
                        )
                        drawArc(
                            color = innerColor,
                            startAngle = 102f,
                            sweepAngle = 172f,
                            useCenter = false,
                            topLeft = innerTopLeft,
                            size = innerSize,
                            style = innerStroke
                        )
                    }
                }
        )
    }
}

@Composable
fun CampusWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CampusBrandMark(size = 32.dp, withContainer = true)
        Text(
            text = "Campus Life",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = CampusColors.Milk
        )
    }
}

/**
 * 1. Иконка нижнего меню «Афиша» — единая сетка 24x24, кэширование геометрии
 */
@Composable
fun NavPosterIcon(
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val stroke = Stroke(width = 1.85f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val bodyTopLeft = Offset(3.5f * u, 4.5f * u)
                val bodySize = Size(17f * u, 16f * u)
                val corner = CornerRadius(4.5f * u, 4.5f * u)

                onDrawBehind {
                    if (active) {
                        drawRoundRect(
                            color = color.copy(alpha = 0.15f),
                            topLeft = bodyTopLeft,
                            size = bodySize,
                            cornerRadius = corner
                        )
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = bodyTopLeft,
                        size = bodySize,
                        cornerRadius = corner,
                        style = stroke
                    )
                    drawLine(
                        color = color,
                        start = Offset(8.2f * u, 2.7f * u),
                        end = Offset(8.2f * u, 6.1f * u),
                        strokeWidth = 1.85f * u,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color,
                        start = Offset(15.8f * u, 2.7f * u),
                        end = Offset(15.8f * u, 6.1f * u),
                        strokeWidth = 1.85f * u,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color,
                        start = Offset(3.8f * u, 9.5f * u),
                        end = Offset(20.2f * u, 9.5f * u),
                        strokeWidth = 1.7f * u,
                        cap = StrokeCap.Round
                    )
                    val dotR = if (active) 1.35f * u else 1.15f * u
                    drawCircle(color = color, radius = dotR, center = Offset(8.8f * u, 14.2f * u))
                    drawCircle(
                        color = if (active) CampusColors.WarmSand else color.copy(alpha = 0.85f),
                        radius = dotR,
                        center = Offset(12.8f * u, 14.2f * u)
                    )
                    drawLine(
                        color = color.copy(alpha = 0.85f),
                        start = Offset(8.5f * u, 17.3f * u),
                        end = Offset(15.5f * u, 17.3f * u),
                        strokeWidth = 1.55f * u,
                        cap = StrokeCap.Round
                    )
                }
            }
    )
}

/**
 * 2. Иконка «Избранное» — Path кэшируется один раз на размер в drawWithCache
 */
@Composable
fun NavHeartIcon(
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp,
    filledWhenActive: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val path = Path().apply {
                    moveTo(12f * u, 20.1f * u)
                    cubicTo(9.2f * u, 17.7f * u, 3.4f * u, 13.5f * u, 3.4f * u, 8.6f * u)
                    cubicTo(3.4f * u, 5.7f * u, 5.7f * u, 3.7f * u, 8.4f * u, 3.7f * u)
                    cubicTo(10.1f * u, 3.7f * u, 11.3f * u, 4.5f * u, 12f * u, 5.7f * u)
                    cubicTo(12.7f * u, 4.5f * u, 13.9f * u, 3.7f * u, 15.6f * u, 3.7f * u)
                    cubicTo(18.3f * u, 3.7f * u, 20.6f * u, 5.7f * u, 20.6f * u, 8.6f * u)
                    cubicTo(20.6f * u, 13.5f * u, 14.8f * u, 17.7f * u, 12f * u, 20.1f * u)
                    close()
                }
                val stroke = Stroke(width = 1.85f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

                onDrawBehind {
                    if (active) {
                        drawPath(
                            path = path,
                            color = if (filledWhenActive) color else color.copy(alpha = 0.18f)
                        )
                    }
                    drawPath(path = path, color = color, style = stroke)
                }
            }
    )
}

/**
 * 3. Иконка «Записи» — Path билета кэшируется один раз на размер
 */
@Composable
fun NavTicketIcon(
    color: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val ticket = Path().apply {
                    val l = 3.0f * u
                    val r = 21.0f * u
                    val t = 5.4f * u
                    val b = 18.6f * u
                    val rad = 3.2f * u
                    val notchR = 2.0f * u

                    moveTo(l + rad, t)
                    lineTo(r - rad, t)
                    arcTo(Rect(r - 2 * rad, t, r, t + 2 * rad), -90f, 90f, false)
                    lineTo(r, 12f * u - notchR)
                    arcTo(Rect(r - notchR, 12f * u - notchR, r + notchR, 12f * u + notchR), -90f, -180f, false)
                    lineTo(r, b - rad)
                    arcTo(Rect(r - 2 * rad, b - 2 * rad, r, b), 0f, 90f, false)
                    lineTo(l + rad, b)
                    arcTo(Rect(l, b - 2 * rad, l + 2 * rad, b), 90f, 90f, false)
                    lineTo(l, 12f * u + notchR)
                    arcTo(Rect(l - notchR, 12f * u - notchR, l + notchR, 12f * u + notchR), 90f, -180f, false)
                    lineTo(l, t + rad)
                    arcTo(Rect(l, t, l + 2 * rad, t + 2 * rad), 180f, 90f, false)
                    close()
                }
                val stroke = Stroke(width = 1.85f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val perfX = 9.3f * u

                onDrawBehind {
                    if (active) {
                        drawPath(path = ticket, color = color.copy(alpha = 0.15f))
                    }
                    drawPath(path = ticket, color = color, style = stroke)
                    drawLine(
                        color = color.copy(alpha = 0.85f),
                        start = Offset(perfX, 8.2f * u),
                        end = Offset(perfX, 10.3f * u),
                        strokeWidth = 1.65f * u,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color.copy(alpha = 0.85f),
                        start = Offset(perfX, 13.7f * u),
                        end = Offset(perfX, 15.8f * u),
                        strokeWidth = 1.65f * u,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = if (active) CampusColors.WarmSand else color.copy(alpha = 0.85f),
                        start = Offset(12.6f * u, 10.6f * u),
                        end = Offset(17.4f * u, 10.6f * u),
                        strokeWidth = 1.7f * u,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = color.copy(alpha = 0.7f),
                        start = Offset(12.6f * u, 13.7f * u),
                        end = Offset(15.8f * u, 13.7f * u),
                        strokeWidth = 1.55f * u,
                        cap = StrokeCap.Round
                    )
                }
            }
    )
}

@Composable
fun SearchGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / 24f
        drawCircle(
            color = color,
            radius = 7.0f * u,
            center = Offset(10.8f * u, 10.8f * u),
            style = Stroke(width = 1.9f * u)
        )
        drawLine(
            color = color,
            start = Offset(16.0f * u, 16.0f * u),
            end = Offset(20.3f * u, 20.3f * u),
            strokeWidth = 1.9f * u,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PlusGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / 24f
        drawLine(color, Offset(12f * u, 5.2f * u), Offset(12f * u, 18.8f * u), strokeWidth = 2.0f * u, cap = StrokeCap.Round)
        drawLine(color, Offset(5.2f * u, 12f * u), Offset(18.8f * u, 12f * u), strokeWidth = 2.0f * u, cap = StrokeCap.Round)
    }
}

@Composable
fun BackArrowGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val p = Path().apply {
                    moveTo(14.5f * u, 5.5f * u)
                    lineTo(8.0f * u, 12.0f * u)
                    lineTo(14.5f * u, 18.5f * u)
                }
                val st = Stroke(width = 2.0f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind { drawPath(p, color = color, style = st) }
            }
    )
}

@Composable
fun ChevronRightGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val p = Path().apply {
                    moveTo(9.5f * u, 5.5f * u)
                    lineTo(16.0f * u, 12.0f * u)
                    lineTo(9.5f * u, 18.5f * u)
                }
                val st = Stroke(width = 2.0f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind { drawPath(p, color = color, style = st) }
            }
    )
}

@Composable
fun CloseGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / 24f
        drawLine(color, Offset(6.5f * u, 6.5f * u), Offset(17.5f * u, 17.5f * u), strokeWidth = 2.0f * u, cap = StrokeCap.Round)
        drawLine(color, Offset(17.5f * u, 6.5f * u), Offset(6.5f * u, 17.5f * u), strokeWidth = 2.0f * u, cap = StrokeCap.Round)
    }
}

@Composable
fun PinGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val p = Path().apply {
                    moveTo(12f * u, 20.5f * u)
                    cubicTo(12f * u, 20.5f * u, 5.5f * u, 14.4f * u, 5.5f * u, 9.8f * u)
                    cubicTo(5.5f * u, 6.2f * u, 8.4f * u, 3.5f * u, 12f * u, 3.5f * u)
                    cubicTo(15.6f * u, 3.5f * u, 18.5f * u, 6.2f * u, 18.5f * u, 9.8f * u)
                    cubicTo(18.5f * u, 14.4f * u, 12f * u, 20.5f * u, 12f * u, 20.5f * u)
                    close()
                }
                val st = Stroke(width = 1.85f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val innerSt = Stroke(width = 1.75f * u)
                onDrawBehind {
                    drawPath(p, color = color, style = st)
                    drawCircle(color = color, radius = 2.3f * u, center = Offset(12f * u, 9.8f * u), style = innerSt)
                }
            }
    )
}

@Composable
fun ClockGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val hands = Path().apply {
                    moveTo(12f * u, 7.2f * u)
                    lineTo(12f * u, 12.2f * u)
                    lineTo(15.4f * u, 14.2f * u)
                }
                val ringSt = Stroke(width = 1.85f * u)
                val handSt = Stroke(width = 1.85f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    drawCircle(color = color, radius = 8.4f * u, center = Offset(12f * u, 12f * u), style = ringSt)
                    drawPath(hands, color = color, style = handSt)
                }
            }
    )
}

@Composable
fun BellGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 17.dp) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val bell = Path().apply {
                    moveTo(6.2f * u, 16.5f * u)
                    lineTo(17.8f * u, 16.5f * u)
                    cubicTo(16.6f * u, 15.0f * u, 16.4f * u, 12.8f * u, 16.4f * u, 10.2f * u)
                    cubicTo(16.4f * u, 7.5f * u, 14.5f * u, 5.4f * u, 12f * u, 5.4f * u)
                    cubicTo(9.5f * u, 5.4f * u, 7.6f * u, 7.5f * u, 7.6f * u, 10.2f * u)
                    cubicTo(7.6f * u, 12.8f * u, 7.4f * u, 15.0f * u, 6.2f * u, 16.5f * u)
                    close()
                }
                val st = Stroke(width = 1.85f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val arcSt = Stroke(width = 1.8f * u, cap = StrokeCap.Round)
                onDrawBehind {
                    drawPath(bell, color = color, style = st)
                    drawArc(
                        color = color,
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(10.0f * u, 16.8f * u),
                        size = Size(4.0f * u, 3.4f * u),
                        style = arcSt
                    )
                }
            }
    )
}

@Composable
fun PhotoGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 19.dp) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val mountain = Path().apply {
                    moveTo(5.2f * u, 17.5f * u)
                    lineTo(10.6f * u, 12.4f * u)
                    lineTo(14.2f * u, 15.6f * u)
                    lineTo(16.8f * u, 13.3f * u)
                    lineTo(19.2f * u, 16.8f * u)
                }
                val frameSt = Stroke(width = 1.85f * u)
                val lineSt = Stroke(width = 1.75f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(3.5f * u, 4.5f * u),
                        size = Size(17f * u, 15f * u),
                        cornerRadius = CornerRadius(3.8f * u, 3.8f * u),
                        style = frameSt
                    )
                    drawCircle(color = color, radius = 1.8f * u, center = Offset(8.8f * u, 9.5f * u))
                    drawPath(path = mountain, color = color, style = lineSt)
                }
            }
    )
}

@Composable
fun CheckGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 15.dp) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val u = this.size.minDimension / 24f
                val p = Path().apply {
                    moveTo(5.2f * u, 12.5f * u)
                    lineTo(10.0f * u, 17.2f * u)
                    lineTo(19.0f * u, 7.4f * u)
                }
                val st = Stroke(width = 2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind { drawPath(p, color = color, style = st) }
            }
    )
}
