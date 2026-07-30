package com.acme.clara.audio

import android.content.Context
import android.media.MediaPlayer
import com.acme.clara.game.SoundCue

/**
 * Plays the game's music and event stingers from `assets/audio/`.
 *
 * Audio files are MIDI, rendered by Android's built-in Sonivox General-MIDI synth:
 * `theme.mid` is the looping title theme and `jingle_0.mid`..`jingle_10.mid` are the short
 * event stingers, mapped to game moments below. Drop generated replacements straight into
 * `assets/audio/` (same filenames); the original set is archived in `assets/audio/original/`
 * for reference and is not shipped as active audio.
 *
 * Everything is best-effort: if a file isn't present yet, playback simply no-ops, so the
 * game stays silent (never crashes) while a new soundtrack is being produced.
 */
object GameSound {
    private var theme: MediaPlayer? = null
    private var stinger: MediaPlayer? = null
    private var enabled = true

    private const val DIR = "audio"
    private const val THEME = "theme.mid"

    // Which stinger plays for each game moment:
    //   jingle_0 playful "discovery"     -> CLUE          jingle_6 dreamy transition  -> TRAVEL
    //   jingle_1 ticking "danger"        -> DANGER        jingle_7 fast dark decisive -> CHASE
    //   jingle_2 buildup that lands      -> ARRIVE        jingle_8 half-cadence prompt-> BRIEFING
    //   jingle_3 dissonant fail sting    -> WRONG_ARREST  jingle_9 theatrical fanfare -> WIN
    //   jingle_4 diminished-7th villain  -> FLASH         jingle_10 somber game-over  -> OUT_OF_TIME
    //   jingle_5 whimsical major resolve -> WARRANT
    private val cueFile = mapOf(
        SoundCue.CLUE to "jingle_0.mid",
        SoundCue.DANGER to "jingle_1.mid",
        SoundCue.ARRIVE to "jingle_2.mid",
        SoundCue.WRONG_ARREST to "jingle_3.mid",
        SoundCue.FLASH to "jingle_4.mid",
        SoundCue.WARRANT to "jingle_5.mid",
        SoundCue.TRAVEL to "jingle_6.mid",
        SoundCue.CHASE to "jingle_7.mid",
        SoundCue.BRIEFING to "jingle_8.mid",
        SoundCue.WIN to "jingle_9.mid",
        SoundCue.OUT_OF_TIME to "jingle_10.mid",
    )

    /** Play the stinger mapped to a game cue. */
    fun play(context: Context, cue: SoundCue) {
        cueFile[cue]?.let { jingle(context, it) }
    }

    fun setEnabled(context: Context, on: Boolean) {
        enabled = on
        if (!on) { stopTheme(); stinger?.release(); stinger = null }
    }

    /** Start (or keep) the looping title theme. No-op if already playing or sound is off. */
    fun startTheme(context: Context) {
        if (!enabled || theme != null) return
        theme = create(context, THEME)?.apply {
            isLooping = true
            start()
        }
    }

    fun stopTheme() {
        theme?.let { runCatching { it.stop() }; it.release() }
        theme = null
    }

    /** Play a one-shot stinger by asset filename (stops the theme underneath, like the DOS
     *  event fanfares). No-op if the file isn't present in assets/audio/ yet. */
    fun jingle(context: Context, file: String) {
        if (!enabled) return
        stopTheme()
        stinger?.release()
        stinger = create(context, file)?.apply {
            setOnCompletionListener { it.release(); if (stinger === it) stinger = null }
            start()
        }
    }

    /** Build a prepared MediaPlayer for assets/audio/<file>, or null if the asset is absent
     *  (or won't decode). Requires the file to be stored uncompressed — see noCompress "mid". */
    private fun create(context: Context, file: String): MediaPlayer? = runCatching {
        context.applicationContext.assets.openFd("$DIR/$file").use { afd ->
            MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setOnErrorListener { _, _, _ -> true }
                prepare()
            }
        }
    }.getOrNull()
}
