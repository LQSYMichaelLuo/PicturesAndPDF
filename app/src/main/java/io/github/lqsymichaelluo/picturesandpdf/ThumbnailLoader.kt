package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.max

object ThumbnailLoader {
    private val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    suspend fun load(source: Bitmap, targetPx: Int): Bitmap? = withContext(dispatcher) {
        val key = ThumbnailCache.keyOf(source, targetPx)
        ThumbnailCache[key]?.let { return@withContext it }

        coroutineContext.ensureActive()
        val decoded = decode(source, targetPx) ?: return@withContext null

        coroutineContext.ensureActive()

        if (decoded !== source) {
            ThumbnailCache.put(key, decoded)
        }
        decoded
    }

    private fun decode(source: Bitmap, targetPx: Int): Bitmap? {
        if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
        val target = ThumbnailCache.sizeBucket(targetPx)
        val maxSide = max(source.width, source.height)
        if (maxSide <= target) return source

        val ratio = target.toFloat() / maxSide
        val w = (source.width * ratio).toInt().coerceAtLeast(1)
        val h = (source.height * ratio).toInt().coerceAtLeast(1)
        return runCatching { source.scale(w, h) }.getOrNull()
    }
}