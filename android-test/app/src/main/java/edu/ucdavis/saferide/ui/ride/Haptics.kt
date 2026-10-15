package edu.ucdavis.saferide.ui.ride

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** The "Driver Arrived" buzz: three pulses, strong enough to notice in a pocket. */
object Haptics {
    private val ARRIVAL_PATTERN = longArrayOf(0, 250, 120, 250, 120, 450)

    fun arrival(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() != true) return
        vibrator.vibrate(VibrationEffect.createWaveform(ARRIVAL_PATTERN, -1))
    }
}
