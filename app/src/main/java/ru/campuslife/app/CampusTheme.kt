package ru.campuslife.app

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Campus Life — Architectural Dark & Airy Palette
 */
object CampusColors {
    val DeepObsidian = Color(0xFF111317)      // Основной тёмный фон
    val Graphite = Color(0xFF22262B)          // Графитовая поверхность карточек
    val GraphiteElevated = Color(0xFF292E35)  // Приподнятый графит (фильтры, инпуты, модальные плашки)
    val GraphiteSubtle = Color(0xFF191C21)    // Мягкая подложка секций

    val Milk = Color(0xFFF3F0EA)              // Молочно-белый основной текст и символы
    val SkyMuted = Color(0xFFBCCBD7)          // Приглушённый небесный (воздух, стекло, холодные акценты)
    val WarmSand = Color(0xFFC9B7A0)          // Тёплый бежевый акцент (ключевые действия, даты, статус)
    val SlateMuted = Color(0xFF929AA4)        // Дополнительный дымчато-серый вторичный текст

    val Hairline = Color(0x1CF3F0EA)          // Деликатный контур поверхностей (11%)
    val HairlineStrong = Color(0x30BCCBD7)    // Световой стеклянный блик (19%)
    val GlassSurface = Color(0xCC1E2228)      // Полупрозрачная графитовая поверхность капсулы
    val GlassPillActive = Color(0x29BCCBD7)   // Мягкая активная капсула внутри навигации

    val Success = Color(0xFFA8C5B5)           // Спокойный шалфейный для подтверждённой записи
    val ErrorSoft = Color(0xFFE5A9A0)         // Деликатный кораллово-бежевый для ошибок формы
}

@OptIn(ExperimentalTextApi::class)
private fun manropeFont(weight: Int): Font = Font(
    resId = R.font.manrope_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val ManropeFamily = FontFamily(
    manropeFont(400),
    manropeFont(500),
    manropeFont(600),
    manropeFont(700)
)

val CampusTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private val CampusDarkScheme = darkColorScheme(
    primary = CampusColors.WarmSand,
    onPrimary = CampusColors.DeepObsidian,
    secondary = CampusColors.SkyMuted,
    onSecondary = CampusColors.DeepObsidian,
    background = CampusColors.DeepObsidian,
    onBackground = CampusColors.Milk,
    surface = CampusColors.Graphite,
    onSurface = CampusColors.Milk,
    surfaceVariant = CampusColors.GraphiteElevated,
    onSurfaceVariant = CampusColors.SlateMuted,
    outline = CampusColors.Hairline,
    error = CampusColors.ErrorSoft
)

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun CampusTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        val scale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (_: Exception) {
            1f
        }
        scale == 0f
    }

    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = CampusDarkScheme,
            typography = CampusTypography,
            shapes = CampusShapes,
            content = content
        )
    }
}
