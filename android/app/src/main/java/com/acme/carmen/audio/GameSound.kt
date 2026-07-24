package com.acme.carmen.audio

import android.content.Context
import android.media.MediaPlayer
import com.acme.carmen.R
import com.acme.carmen.game.SoundCue

/**
 * Plays the game's own audio, recovered byte-for-byte from the original MIDISND.DAT.
 *
 * The DAT is the same container format as CARMEN.DAT (u32 dir offset at EOF, 8-byte entries);
 * its items are 12 standard SMF (MThd/MTrk) sequences (archive ids 218..229). Item 11 (id 229)
 * is the full-arrangement title theme; the other 11 are short event stingers. They are bundled
 * verbatim as res/raw MIDI and rendered by Android's built-in Sonivox synth — the same
 * General-MIDI voices the original targeted, so the music is authentic, not a re-creation.
 *
 * The stinger→event mapping below was established by auditioning each sequence's musical
 * character (resolution, key, harmony) against the game's events; the emotional tentpoles
 * (win/wrong-arrest/out-of-time/warrant) are firm, the suspense/transition cues are best-fit.
 */
object GameSound {
    private var theme: MediaPlayer? = null
    private var stinger: MediaPlayer? = null
    private var enabled = true

    // Which stinger plays for each game moment (see the audition notes in the commit history):
    //   jingle_0 playful "discovery"     -> CLUE          jingle_6 dreamy transition  -> TRAVEL
    //   jingle_1 ticking "danger"        -> DANGER        jingle_7 fast dark decisive -> CHASE
    //   jingle_2 buildup that lands      -> ARRIVE        jingle_8 half-cadence prompt-> BRIEFING
    //   jingle_3 dissonant fail sting    -> WRONG_ARREST  jingle_9 theatrical fanfare -> WIN
    //   jingle_4 diminished-7th villain  -> FLASH         jingle_10 somber game-over  -> OUT_OF_TIME
    //   jingle_5 whimsical major resolve -> WARRANT
    private val cueRes = mapOf(
        SoundCue.CLUE to R.raw.jingle_0,
        SoundCue.DANGER to R.raw.jingle_1,
        SoundCue.ARRIVE to R.raw.jingle_2,
        SoundCue.WRONG_ARREST to R.raw.jingle_3,
        SoundCue.FLASH to R.raw.jingle_4,
        SoundCue.WARRANT to R.raw.jingle_5,
        SoundCue.TRAVEL to R.raw.jingle_6,
        SoundCue.CHASE to R.raw.jingle_7,
        SoundCue.BRIEFING to R.raw.jingle_8,
        SoundCue.WIN to R.raw.jingle_9,
        SoundCue.OUT_OF_TIME to R.raw.jingle_10,
    )

    /** Play the stinger mapped to a game cue. */
    fun play(context: Context, cue: SoundCue) {
        cueRes[cue]?.let { jingle(context, it) }
    }

    fun setEnabled(context: Context, on: Boolean) {
        enabled = on
        if (!on) { stopTheme(); stinger?.release(); stinger = null }
    }

    /** Start (or keep) the looping title theme. No-op if already playing or sound is off. */
    fun startTheme(context: Context) {
        if (!enabled || theme != null) return
        theme = MediaPlayer.create(context.applicationContext, R.raw.theme)?.apply {
            isLooping = true
            setOnErrorListener { _, _, _ -> true }
            start()
        }
    }

    fun stopTheme() {
        theme?.let { runCatching { it.stop() }; it.release() }
        theme = null
    }

    /** Play a one-shot stinger (stops the theme underneath, like the DOS event fanfares). */
    fun jingle(context: Context, resId: Int) {
        if (!enabled) return
        stopTheme()
        stinger?.release()
        stinger = MediaPlayer.create(context.applicationContext, resId)?.apply {
            setOnCompletionListener { it.release(); if (stinger === it) stinger = null }
            setOnErrorListener { _, _, _ -> true }
            start()
        }
    }
}
