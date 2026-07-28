package com.acme.clara.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.acme.clara.game.HapticCue

/**
 * Plays the game's abstract [HapticCue]s on the device vibrator, mirroring [GameSound].
 * Driven off the same SoundCue signal, so touch feedback never drifts from the audio.
 * Silently no-ops when the cue is NONE, the toggle is off, or there's no vibrator.
 */
object HapticEngine {

    fun play(context: Context, cue: HapticCue, enabled: Boolean) {
        if (!enabled || cue == HapticCue.NONE) return
        val vib = vibrator(context)?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(effect(cue))
        } else {
            legacy(vib, cue)
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private fun effect(cue: HapticCue): VibrationEffect = when (cue) {
        HapticCue.TICK -> VibrationEffect.createOneShot(12, 60)
        HapticCue.CLICK -> VibrationEffect.createOneShot(20, 120)
        HapticCue.HEAVY -> VibrationEffect.createOneShot(35, 255)
        HapticCue.RUMBLE -> VibrationEffect.createOneShot(220, 180)
        HapticCue.DOUBLE -> VibrationEffect.createWaveform(
            longArrayOf(0, 18, 55, 18), intArrayOf(0, 150, 0, 150), -1)
        HapticCue.REJECT -> VibrationEffect.createWaveform(
            longArrayOf(0, 45, 55, 45), intArrayOf(0, 220, 0, 220), -1)
        HapticCue.SUCCESS -> VibrationEffect.createWaveform(
            longArrayOf(0, 15, 40, 30), intArrayOf(0, 120, 0, 255), -1)
        HapticCue.NONE -> VibrationEffect.createOneShot(1, 1)
    }

    @Suppress("DEPRECATION")
    private fun legacy(vib: Vibrator, cue: HapticCue) = when (cue) {
        HapticCue.DOUBLE, HapticCue.REJECT -> vib.vibrate(longArrayOf(0, 20, 60, 20), -1)
        HapticCue.SUCCESS -> vib.vibrate(longArrayOf(0, 15, 40, 30), -1)
        HapticCue.TICK -> vib.vibrate(15)
        HapticCue.CLICK -> vib.vibrate(20)
        HapticCue.HEAVY -> vib.vibrate(35)
        HapticCue.RUMBLE -> vib.vibrate(200)
        HapticCue.NONE -> Unit
    }
}
