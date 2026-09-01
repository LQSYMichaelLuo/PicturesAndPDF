package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object PreviewBitmapCache {

    private val maxSizeKb: Int =
        ((Runtime.getRuntime().maxMemory() / 1024L) / 6L)
            .toInt()
            .coerceAtLeast(32 * 1024)

    private val cache = object : LruCache<Long, Bitmap>(maxSizeKb) {
        override fun sizeOf(key: Long, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    private fun keyOf(bmp: Bitmap): Long =
        System.identityHashCode(bmp).toLong() and 0xFFFFFFFFL

    fun needsFitting(bmp: Bitmap): Boolean = !BitmapSafeLimits.isSafe(bmp)

    fun peek(bmp: Bitmap): Bitmap? = cache[keyOf(bmp)]

    suspend fun prepare(bmp: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        val key = keyOf(bmp)
        cache[key]?.let { return@withContext it }

        if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) return@withContext null
        coroutineContext.ensureActive()

        val target = BitmapSafeLimits.fit(bmp.width, bmp.height)
            ?: return@withContext bmp

        val fitted = runCatching {
            bmp.scale(target.first, target.second)
        }.getOrNull() ?: return@withContext null

        coroutineContext.ensureActive()

        if (BitmapSafeLimits.isSafe(fitted)) {
            cache.put(key, fitted)
            fitted
        } else {
            null
        }
    }

    fun removeAllFor(bmp: Bitmap) { cache.remove(keyOf(bmp)) }

    fun removeAll(sources: Collection<Bitmap>) {
        for (src in sources) {
            cache.remove(System.identityHashCode(src).toLong() and 0xFFFFFFFFL)
        }
    }

    fun trimToHalf() { cache.trimToSize(cache.maxSize() / 2) }

    fun clear() { cache.evictAll() }
}