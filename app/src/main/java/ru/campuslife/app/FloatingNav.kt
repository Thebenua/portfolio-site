package ru.campuslife.app

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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

private val CapsuleShape = RoundedCornerShape(percent = 50)
private val InnerPillShape = RoundedCornerShape(percent = 50)

/**
 * Floating Bottom Navigation (120Hz GPU-accelerated):
 * - Перемещение активного индикатора и масштабирование выполняются в graphicsLayer (Draw-фаза GPU, 0 layout-перерасчётов).
 * - Градиенты и световые блики стекла кэшируются в drawWithCache.
 * - Высота и геометрия капсулы строго постоянны, исключены любые скачки, мигания и исчезновения.
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
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val tabs = CampusTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    val outerRimBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                CampusColors.Milk.copy(alpha = 0.24f),
                CampusColors.SkyMuted.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.35f)
            )
        )
    }
    val innerPillBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                CampusColors.SkyMuted.copy(alpha = 0.21f),
                CampusColors.Milk.copy(alpha = 0.08f)
            )
        )
    }
    val innerPillRimBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                CampusColors.Milk.copy(alpha = 0.22f),
                CampusColors.SkyMuted.copy(alpha = 0.05f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(bottom = 16.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 432.dp)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 26.dp,
                    shape = CapsuleShape,
                    ambientColor = Color.Black.copy(alpha = 0.68f),
                    spotColor = Color.Black.copy(alpha = 0.76f)
                )
                .clip(CapsuleShape)
                .drawWithCache {
                    val surfaceBrush = Brush.verticalGradient(
                        colors = listOf(
                            CampusColors.GlassSurfaceTop,
                            CampusColors.GlassSurfaceBottom
                        )
                    )
                    val topSheenBrush = Brush.verticalGradient(
                        colors = listOf(
                            CampusColors.SkyMuted.copy(alpha = 0.13f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.54f
                    )
                    val glowBrush = Brush.radialGradient(
                        colors = listOf(
                            CampusColors.WarmSand.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.65f
                    )
                    val cr = CornerRadius(size.height / 2f, size.height / 2f)

                    onDrawBehind {
                        drawRoundRect(brush = surfaceBrush, cornerRadius = cr)
                        drawRoundRect(brush = topSheenBrush, cornerRadius = cr)
                        drawRoundRect(brush = glowBrush, cornerRadius = cr)
                    }
                }
                .border(width = 1.dp, brush = outerRimBrush, shape = CapsuleShape)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val slotWidth = maxWidth / tabs.size
            val slotWidthPx = with(density) { slotWidth.toPx() }

            // Плавная spring-анимация позиции индикатора напрямую в пикселях для GPU translationX
            val animatedSlotProgress by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = if (reducedMotion) {
                    snap()
                } else {
                    CampusMotion.navSpring()
                },
                label = "navPillSlotProgress"
            )

            // Скользящая подушка активной вкладки (движется без перерасчёта layout)
            Box(
                modifier = Modifier
                    .width(slotWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = animatedSlotProgress * slotWidthPx
                    }
                    .clip(InnerPillShape)
                    .background(innerPillBrush)
                    .border(width = 0.8.dp, brush = innerPillRimBrush, shape = InnerPillShape)
            )

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
                            isPressed -> 0.93f
                            else -> 1f
                        },
                        animationSpec = if (reducedMotion) snap() else CampusMotion.tactileSpring(),
                        label = "tabPressScale"
                    )

                    val iconLiftPx by animateFloatAsState(
                        targetValue = if (isSelected && !reducedMotion) with(density) { (-1.2).dp.toPx() } else 0f,
                        animationSpec = if (reducedMotion) snap() else CampusMotion.navSpring(),
                        label = "tabIconLiftPx"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) CampusColors.Milk else CampusColors.SlateMuted,
                        animationSpec = if (reducedMotion) snap() else tween(190, easing = CampusMotion.EmphasizedDecelerate),
                        label = "tabContentColor"
                    )

                    val badgeCount = when (tab) {
                        CampusTab.Favorites -> favoritesCount
                        CampusTab.Bookings -> bookingsCount
                        CampusTab.Poster -> 0
                    }

                    val badgeAlpha by animateFloatAsState(
                        targetValue = if (badgeCount > 0 && !isSelected) 1f else 0f,
                        animationSpec = if (reducedMotion) snap() else tween(180),
                        label = "tabBadgeAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(InnerPillShape)
                            .semantics {
                                selected = isSelected
                                contentDescription = tab.label
                            }
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Tab,
                                onClick = {
                                    if (!isSelected) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onSelect(tab)
                                }
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
                                    .height(24.dp)
                                    .graphicsLayer { translationY = iconLiftPx },
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

                                if (badgeAlpha > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-1).dp)
                                            .size(5.5.dp)
                                            .graphicsLayer {
                                                alpha = badgeAlpha
                                                scaleX = badgeAlpha
                                                scaleY = badgeAlpha
                                            }
                                            .clip(CircleShape)
                                            .background(CampusColors.WarmSand)
                                    )
                                }
                            }

                            Spacer(Modifier.height(3.5.dp))

                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.15.sp
                                ),
                                color = contentColor,
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
