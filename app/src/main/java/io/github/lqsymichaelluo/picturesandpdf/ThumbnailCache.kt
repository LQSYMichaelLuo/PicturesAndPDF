package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import android.util.LruCache

object ThumbnailCache {
    private val BUCKETS = intArrayOf(160, 320, 640, 1024)

    private val maxSizeKb: Int =
        ((Runtime.getRuntime().maxMemory() / 1024L) / 8L)
            .toInt()
            .coerceAtLeast(8 * 1024)

    private val cache = object : LruCache<Long, Bitmap>(maxSizeKb) {
        override fun sizeOf(key: Long, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)

        override fun entryRemoved(
            evicted: Boolean,
            key: Long,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            //qwq
        }
    }


    fun sizeBucket(px: Int): Int =
        BUCKETS.firstOrNull { px <= it } ?: BUCKETS.last()

    fun keyOf(source: Bitmap, px: Int): Long {
        val id = System.identityHashCode(source).toLong() and 0xFFFFFFFFL
        return (id shl 8) or BUCKETS.indexOf(sizeBucket(px)).toLong()
    }

    operator fun get(key: Long): Bitmap? = cache[key]

    fun put(key: Long, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
    fun removeAll(sources: Collection<Bitmap>) {
        for (src in sources) removeAllFor(src)
    }
    fun removeAllFor(source: Bitmap) {
        val id = System.identityHashCode(source).toLong() and 0xFFFFFFFFL
        for (i in BUCKETS.indices) {
            cache.remove((id shl 8) or i.toLong())
        }
    }

    fun trimToHalf() {
        cache.trimToSize(cache.maxSize() / 2)
    }

    fun clear() {
        cache.evictAll()
    }

    fun currentSizeKb(): Int = cache.size()
    fun maxSizeKb(): Int = cache.maxSize()
}