package io.github.lqsymichaelluo.picturesandpdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import kotlin.math.min

object BitmapLoader {

    fun decode(context: Context, uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
        }
        val sw = bounds.outWidth
        val sh = bounds.outHeight
        if (sw <= 0 || sh <= 0) return null

        val cappedW = min(reqW.coerceAtLeast(1), sw)
        val cappedH = min(reqH.coerceAtLeast(1), sh)
        val fitted = BitmapSafeLimits.fit(cappedW, cappedH)
        val targetW = fitted?.first ?: cappedW
        val targetH = fitted?.second ?: cappedH

        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(sw, sh, targetW, targetH)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rough = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }.getOrNull() ?: return null

        if (rough.width == targetW && rough.height == targetH) return rough

        val scaled = runCatching {
            rough.scale(targetW, targetH)
        }.getOrNull() ?: return rough

        if (!rough.isRecycled) rough.recycle()
        return scaled
    }

    fun fitExisting(bmp: Bitmap, recycleSource: Boolean = false): Bitmap {
        if (bmp.isRecycled) return bmp
        if (BitmapSafeLimits.isSafe(bmp)) return bmp

        val target = BitmapSafeLimits.fit(bmp.width, bmp.height) ?: return bmp
        val scaled = runCatching {
            bmp.scale(target.first, target.second)
        }.getOrNull() ?: return bmp

        if (recycleSource && !bmp.isRecycled) bmp.recycle()
        return scaled
    }

    private fun calculateInSampleSize(
        srcW: Int, srcH: Int, reqW: Int, reqH: Int
    ): Int {
        var sample = 1
        if (srcH > reqH || srcW > reqW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            while ((halfH / sample) >= reqH && (halfW / sample) >= reqW) {
                sample *= 2
            }
        }
        return sample.coerceAtLeast(1)
    }
}