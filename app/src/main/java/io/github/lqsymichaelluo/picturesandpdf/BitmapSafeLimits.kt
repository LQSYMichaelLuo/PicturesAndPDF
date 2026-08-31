package io.github.lqsymichaelluo.picturesandpdf

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.sqrt

object BitmapSafeLimits {

    private const val FALLBACK_TEXTURE = 2048

    private const val MAX_RENDER_BYTES = 64 * 1024 * 1024

    private val textureSize = AtomicInteger(FALLBACK_TEXTURE)

    init {
        Executors.newSingleThreadExecutor().execute {
            textureSize.set(queryMaxTextureSize())
        }
    }

    fun maxTextureSize(): Int = textureSize.get()

    fun maxRenderBytes(): Int = MAX_RENDER_BYTES

    fun isSafe(bmp: Bitmap): Boolean {
        if (bmp.isRecycled || bmp.width <= 0 || bmp.height <= 0) return false
        return max(bmp.width, bmp.height) <= textureSize.get() &&
                bmp.allocationByteCount <= MAX_RENDER_BYTES
    }

    fun fit(srcW: Int, srcH: Int): Pair<Int, Int>? {
        if (srcW <= 0 || srcH <= 0) return null
        val tex = textureSize.get().toFloat()
        var w = srcW.toFloat()
        var h = srcH.toFloat()

        val maxSide = max(w, h)
        if (maxSide > tex) {
            val r = tex / maxSide
            w *= r
            h *= r
        }

        val bytes = w * h * 4.0
        if (bytes > MAX_RENDER_BYTES) {
            val r = sqrt(MAX_RENDER_BYTES / bytes).toFloat()
            w *= r
            h *= r
        }

        val ow = w.toInt().coerceAtLeast(1)
        val oh = h.toInt().coerceAtLeast(1)
        return if (ow >= srcW && oh >= srcH) null else ow to oh
    }

    private fun queryMaxTextureSize(): Int = runCatching {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
            return@runCatching FALLBACK_TEXTURE
        }

        val attrs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, num, 0)) {
            return@runCatching FALLBACK_TEXTURE
        }
        val config = configs[0] ?: return@runCatching FALLBACK_TEXTURE

        val ctxAttrs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT, ctxAttrs, 0
        )
        if (context == EGL14.EGL_NO_CONTEXT) return@runCatching FALLBACK_TEXTURE

        val surfAttrs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        val surface = EGL14.eglCreatePbufferSurface(display, config, surfAttrs, 0)

        var result = FALLBACK_TEXTURE
        if (surface != EGL14.EGL_NO_SURFACE) {
            if (EGL14.eglMakeCurrent(display, surface, surface, context)) {
                val max = IntArray(1)
                GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, max, 0)
                GLES20.glGetError() // 清空错误状态
                if (max[0] > 0) result = max[0]
            }

            EGL14.eglMakeCurrent(
                display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(display, surface)
        }
        EGL14.eglDestroyContext(display, context)
        result
    }.getOrDefault(FALLBACK_TEXTURE).coerceIn(1024, 16384)
}