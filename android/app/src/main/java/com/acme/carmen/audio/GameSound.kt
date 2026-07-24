package com.acme.carmen.audio

import android.content.Context
import android.media.MediaPlayer
import com.acme.carmen.R

/**
 * Plays the game's own audio, recovered byte-for-byte from the original MIDISND.DAT.
 *
 * The DAT is the same container format as CARMEN.DAT (u32 dir offset at EOF, 8-byte entries);
 * its items are 12 standard SMF (MThd/MTrk) sequences. Item 11 is the full-arrangement title
 * theme (Melody/Riff/Bass/hi-hat/snare parts); the other 11 are short event stingers. They are
 * bundled verbatim as res/raw MIDI and rendered by Android's built-in Sonivox synth — the same
 * General-MIDI voices the original targeted, so the music is authentic, not a re-creation.
 *
 * Only the title theme auto-plays for now (its role is unambiguous). The stingers are bundled
 * and addressable via [jingle] but not yet mapped to specific events, because the correct
 * event→stinger mapping can't be confirmed without auditioning them and a wrong guess would be
 * a fidelity regression. See stinger res ids jingle_0..jingle_10.
 */
object GameSound {
    private var theme: MediaPlayer? = null
    private var stinger: MediaPlayer? = null
    private var enabled = true

    /** Bundled event stingers (item order from MIDISND.DAT), for future event mapping. */
    val jingles = intArrayOf(
        R.raw.jingle_0, R.raw.jingle_1, R.raw.jingle_2, R.raw.jingle_3,
        R.raw.jingle_4, R.raw.jingle_5, R.raw.jingle_6, R.raw.jingle_7,
        R.raw.jingle_8, R.raw.jingle_9, R.raw.jingle_10,
    )

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
