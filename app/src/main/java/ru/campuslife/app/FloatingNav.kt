package ru.campuslife.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CampusTab(val label: String) {
    Poster("Афиша"),
    Favorites("Избранное"),
    Bookings("Записи")
}

/**
 * Floating Bottom Navigation — парящая стеклянная капсула с тремя равноширинными вкладками,
 * плавно скользящей внутренней подушкой и тактильным откликом без изменения габаритов панели.
 */
@Composable
fun FloatingBottomNav(
    selectedTab: CampusTab,
    favoritesCount: Int,
    bookingsCount: Int,
    onSelect: (CampusTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val tabs = CampusTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    val capsuleShape = RoundedCornerShape(percent = 50)
    val innerPillShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(bottom = 18.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Внешний контейнер с деликатной глубокой тенью, создающей эффект парения над списком
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 26.dp,
                    shape = capsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.65f),
                    spotColor = Color.Black.copy(alpha = 0.72f)
                )
                .clip(capsuleShape)
                // Glassmorphism-поверхность: дымчато-графитовый полупрозрачный слой + верхний небесно-молочный рефлекс
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEB282D35), // Верхняя кромка стекла (~92% непрозрачности для идеальной читаемости)
                            Color(0xE31B1F25)  // Нижняя глубокая графитовая грань (~89%)
                        )
                    )
                )
                .drawBehind {
                    // Мягкий воздушный блик в верхней трети капсулы
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CampusColors.SkyMuted.copy(alpha = 0.11f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.52f
                        ),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CampusColors.Milk.copy(alpha = 0.22f),
                            CampusColors.SkyMuted.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.32f)
                        )
                    ),
                    shape = capsuleShape
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val innerWidth = maxWidth
            val slotWidth = innerWidth / tabs.size

            val indicatorOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = if (reducedMotion) {
                    snap()
                } else {
                    tween(durationMillis = 235, easing = FastOutSlowInEasing)
                },
                label = "navPillOffset"
            )

            // Плавно скользящая подложка активного раздела (мягкое матовое стекло без кричащих цветов)
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .clip(innerPillShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                CampusColors.SkyMuted.copy(alpha = 0.19f),
                                CampusColors.Milk.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CampusColors.Milk.copy(alpha = 0.20f),
                                CampusColors.SkyMuted.copy(alpha = 0.04f)
                            )
                        ),
                        shape = innerPillShape
                    )
            )

            // Три идеально равных слота вкладок (фиксированная геометрия без скачков ширины)
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()

                    val pressScale by animateFloatAsState(
                        targetValue = when {
                            reducedMotion -> 1f
                            isPressed -> 0.94f
                            else -> 1f
                        },
                        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                        label = "tabPressScale"
                    )

                    val iconLift by animateDpAsState(
                        targetValue = if (isSelected && !reducedMotion) (-1).dp else 0.dp,
                        animationSpec = if (reducedMotion) snap() else tween(210, easing = FastOutSlowInEasing),
                        label = "tabIconLift"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) CampusColors.Milk else CampusColors.SlateMuted,
                        animationSpec = if (reducedMotion) snap() else tween(200),
                        label = "tabContentColor"
                    )

                    val badgeCount = when (tab) {
                        CampusTab.Favorites -> favoritesCount
                        CampusTab.Bookings -> bookingsCount
                        CampusTab.Poster -> 0
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(innerPillShape)
                            .semantics {
                                selected = isSelected
                                contentDescription = tab.label
                            }
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(tab) }
                            )
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(y = iconLift)
                                    .height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (tab) {
                                    CampusTab.Poster -> NavPosterIcon(
                                        color = contentColor,
                                        active = isSelected,
                                        size = 22.5.dp
                                    )
                                    CampusTab.Favorites -> NavHeartIcon(
                                        color = contentColor,
                                        active = isSelected,
                                        size = 22.5.dp
                                    )
                                    CampusTab.Bookings -> NavTicketIcon(
                                        color = contentColor,
                                        active = isSelected,
                                        size = 22.5.dp
                                    )
                                }

                                // Деликатная точка-индикатор наличия элементов (не меняет центровку иконки)
                                if (badgeCount > 0 && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-1).dp)
                                            .size(5.5.dp)
                                            .clip(CircleShape)
                                            .background(CampusColors.WarmSand)
                                    )
                                }
                            }

                            Spacer(Modifier.height(3.5.dp))

                            // Фиксированный вес шрифта (Medium) исключает микро-прыжки ширины букв при смене вкладки
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.15.sp
                                ),
                                color = if (isSelected) CampusColors.Milk else CampusColors.SlateMuted.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
