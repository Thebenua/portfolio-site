package ru.campuslife.app

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.view.Display
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Высокопроизводительный кэш обложек мероприятий (LruCache + фоновое декодирование на Dispatchers.IO).
 * Исключает синхронный BitmapFactory I/O на главном потоке и предотвращает просадку кадров при скролле 120 Гц.
 */
object CampusImageCache {
    // До 32 МБ под готовые ImageBitmap в памяти
    private val memoryCache = object : LruCache<String, ImageBitmap>(32 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            // Приблизительный размер в КБ (4 байта на пиксель)
            return ((value.width.toLong() * value.height.toLong() * 4L) / 1024L)
                .coerceAtLeast(1L)
                .toInt()
        }
    }

    fun getCached(key: String): ImageBitmap? = memoryCache.get(key)

    suspend fun loadUriBitmap(
        context: Context,
        uriString: String,
        maxDimension: Int = 1280
    ): ImageBitmap? {
        if (uriString.isBlank()) return null
        val cacheKey = "uri:$maxDimension:$uriString"
        memoryCache.get(cacheKey)?.let { return it }

        return withContext(Dispatchers.IO) {
            memoryCache.get(cacheKey)?.let { return@withContext it }
            try {
                val uri = Uri.parse(uriString)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

                var sample = 1
                while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Bitmap.Config.HARDWARE
                    } else {
                        Bitmap.Config.ARGB_8888
                    }
                }
                val decoded = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@withContext null

                decoded.asImageBitmap().also { bmp ->
                    memoryCache.put(cacheKey, bmp)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun loadDrawableBitmap(
        context: Context,
        @DrawableRes resId: Int,
        maxDimension: Int = 1280
    ): ImageBitmap? {
        val cacheKey = "res:$maxDimension:$resId"
        memoryCache.get(cacheKey)?.let { return it }

        return withContext(Dispatchers.IO) {
            memoryCache.get(cacheKey)?.let { return@withContext it }
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(context.resources, resId, bounds)
                var sample = 1
                if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                    while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
                        sample *= 2
                    }
                }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Bitmap.Config.HARDWARE
                    } else {
                        Bitmap.Config.ARGB_8888
                    }
                }
                val decoded = BitmapFactory.decodeResource(context.resources, resId, opts)
                    ?: return@withContext null
                decoded.asImageBitmap().also { bmp ->
                    memoryCache.put(cacheKey, bmp)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Фоновый прогрев стандартных обложек при старте приложения — скролл и переходы мгновенны с первого кадра.
     */
    suspend fun preloadDefaults(context: Context) {
        val defaults = listOf(
            R.drawable.cover_science,
            R.drawable.cover_art,
            R.drawable.cover_sport,
            R.drawable.cover_tech,
            R.drawable.cover_music,
            R.drawable.event_cover
        )
        withContext(Dispatchers.IO) {
            defaults.forEach { resId ->
                loadDrawableBitmap(context, resId, maxDimension = 1280)
            }
        }
    }
}

@DrawableRes
internal fun defaultCoverResForEvent(eventId: Int): Int = when (eventId) {
    1 -> R.drawable.cover_science
    2 -> R.drawable.cover_art
    3 -> R.drawable.cover_sport
    4 -> R.drawable.cover_tech
    5 -> R.drawable.cover_music
    else -> R.drawable.event_cover
}

/**
 * Неблокирующая отрисовка обложки мероприятия с мгновенной отдачей из LruCache и мягким проявлением при первой загрузке.
 */
@Composable
fun AsyncEventCover(
    event: Event,
    modifier: Modifier = Modifier,
    maxDimension: Int = 1280
) {
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val fallbackRes = remember(event.id) { defaultCoverResForEvent(event.id) }
    val primaryKey = remember(event.photoUri, fallbackRes, maxDimension) {
        if (event.photoUri.isNotBlank()) "uri:$maxDimension:${event.photoUri}" else "res:$maxDimension:$fallbackRes"
    }

    val initialCached = remember(primaryKey) { CampusImageCache.getCached(primaryKey) }
    var bitmap by remember(primaryKey) { mutableStateOf(initialCached) }
    var loadedFromCache by remember(primaryKey) { mutableStateOf(initialCached != null) }

    LaunchedEffect(primaryKey) {
        if (bitmap == null) {
            val loaded = if (event.photoUri.isNotBlank()) {
                CampusImageCache.loadUriBitmap(context, event.photoUri, maxDimension)
                    ?: CampusImageCache.loadDrawableBitmap(context, fallbackRes, maxDimension)
            } else {
                CampusImageCache.loadDrawableBitmap(context, fallbackRes, maxDimension)
            }
            bitmap = loaded
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0f,
        animationSpec = if (reducedMotion || loadedFromCache) {
            snap()
        } else {
            tween(durationMillis = 200, easing = FastOutSlowInEasing)
        },
        label = "coverRevealAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

/**
 * Запрашивает максимальную поддерживаемую частоту обновления дисплея (90 / 120 Гц) на поддерживаемых Android-экранах,
 * не ломая работу на стандартных 60 Гц матрицах.
 */
fun Activity.preferSmoothDisplayMode() {
    try {
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        val modes = display?.supportedModes ?: return
        if (modes.size <= 1) return

        val activeMode = display.mode
        val highestSameRes = modes
            .filter { it.physicalWidth == activeMode.physicalWidth && it.physicalHeight == activeMode.physicalHeight }
            .maxByOrNull { it.refreshRate }

        if (highestSameRes != null && highestSameRes.refreshRate > activeMode.refreshRate) {
            val params: WindowManager.LayoutParams = window.attributes
            params.preferredDisplayModeId = highestSameRes.modeId
            window.attributes = params
        }
    } catch (_: Exception) {
        // Если оболочка OEM запрещает переключение режима, система безопасно остаётся на текущем режиме
    }
}
