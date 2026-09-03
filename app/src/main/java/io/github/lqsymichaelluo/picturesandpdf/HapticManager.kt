package io.github.lqsymichaelluo.picturesandpdf

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticManager {
    fun vibrate(context: Context, effectType: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(effectType))
        }
    }

    const val EFFECT_CLICK = VibrationEffect.EFFECT_CLICK
    const val EFFECT_HEAVY_CLICK = VibrationEffect.EFFECT_HEAVY_CLICK
    const val EFFECT_TICK = VibrationEffect.EFFECT_TICK
}