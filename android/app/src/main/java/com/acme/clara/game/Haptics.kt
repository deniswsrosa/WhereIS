package com.acme.clara.game

/** The abstract vibration shapes; the UI layer maps each to a concrete Android effect. */
enum class HapticCue { NONE, TICK, CLICK, DOUBLE, HEAVY, SUCCESS, REJECT, RUMBLE }

/**
 * Haptics ride the existing [SoundCue] pipeline: one map, driven off the same signal the
 * sound already plays, so touch feedback never drifts out of sync with the audio.
 */
object Haptics {
    fun forCue(c: SoundCue): HapticCue = when (c) {
        SoundCue.CLUE -> HapticCue.TICK
        SoundCue.TRAVEL -> HapticCue.CLICK
        SoundCue.ARRIVE -> HapticCue.CLICK
        SoundCue.FLASH -> HapticCue.DOUBLE
        SoundCue.WARRANT -> HapticCue.HEAVY
        SoundCue.DANGER -> HapticCue.DOUBLE
        SoundCue.WIN -> HapticCue.SUCCESS
        SoundCue.WRONG_ARREST -> HapticCue.REJECT
        SoundCue.OUT_OF_TIME -> HapticCue.RUMBLE
        SoundCue.BRIEFING -> HapticCue.NONE
        SoundCue.CHASE -> HapticCue.TICK
    }
}
