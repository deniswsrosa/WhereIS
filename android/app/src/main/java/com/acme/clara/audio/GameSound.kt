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
    private var themePaused = false
    private var themePrepared = false

    // Short PCM click for the HQ printer teletype, played via SoundPool so rapid repeats overlap
    // cheaply (MediaPlayer can't). Loaded lazily on the first keystroke.
    private var pool: android.media.SoundPool? = null
    private var clickId = 0

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

    /** A single dot-matrix printer click, for the HQ teletype. Cheap and overlappable; no-op if
     *  sound is off or the sample hasn't finished loading yet (the first few keystrokes). */
    fun typeClick(context: Context) {
        if (!enabled) return
        if (pool == null) {
            val sp = android.media.SoundPool.Builder().setMaxStreams(6)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_GAME)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                ).build()
            clickId = runCatching {
                context.applicationContext.assets.openFd("$DIR/type_click.wav").use { sp.load(it, 1) }
            }.getOrDefault(0)
            pool = sp
        }
        if (clickId != 0) {
            val rate = 0.92f + kotlin.random.Random.nextFloat() * 0.16f   // slight pitch jitter
            pool?.play(clickId, 0.35f, 0.35f, 1, 0, rate)
        }
    }

    fun setEnabled(context: Context, on: Boolean) {
        enabled = on
        if (!on) { stopTheme(); stinger?.release(); stinger = null }
    }

    /** Start (or keep) the looping title theme. No-op if already playing or sound is off. */
    fun startTheme(context: Context) {
        if (!enabled || theme != null) return
        themePaused = false
        themePrepared = false
        val player = createUnprepared(context, THEME) ?: return
        theme = player
        player.isLooping = true
        player.setOnPreparedListener {
            if (theme === it && enabled && !themePaused) {
                themePrepared = true
                runCatching { it.start() }
            } else if (theme === it && enabled) {
                // It finished preparing while the Activity was in the background. Leave it
                // prepared and silent; resumeTheme() will start it on return.
                themePrepared = true
            } else if (theme !== it || !enabled) {
                it.release()
            }
        }
        installErrorHandler(player)
        prepareAsync(player)
    }

    fun stopTheme() {
        theme?.let { if (themePrepared) runCatching { it.stop() }; it.release() }
        theme = null
        themePaused = false
        themePrepared = false
    }

    /** Pause the looping theme so it never keeps playing while the app is in the background;
     *  paired with [resumeTheme] on return. No-op if no theme is currently loaded. */
    fun pauseTheme() {
        themePaused = true
        theme?.let { if (themePrepared) runCatching { if (it.isPlaying) it.pause() } }
        // Event stingers are intentionally not resumed after leaving the app; otherwise a cue can
        // finish preparing and play over another app while Clara is backgrounded.
        stinger?.release()
        stinger = null
    }

    /** Resume a theme that [pauseTheme] paused (only if it's still the active title-screen theme
     *  and sound is on). No-op once the theme has been stopped for gameplay. */
    fun resumeTheme() {
        if (!enabled) return
        themePaused = false
        theme?.let { if (themePrepared) runCatching { if (!it.isPlaying) it.start() } }
    }

    /** Play a one-shot stinger by asset filename (stops the theme underneath, like the DOS
     *  event fanfares). No-op if the file isn't present in assets/audio/ yet. */
    fun jingle(context: Context, file: String) {
        if (!enabled) return
        stopTheme()
        stinger?.release()
        val player = createUnprepared(context, file) ?: return
        stinger = player
        player.setOnPreparedListener {
            if (stinger === it && enabled) runCatching { it.start() }
            else it.release()
        }
        player.setOnCompletionListener { it.release(); if (stinger === it) stinger = null }
        installErrorHandler(player)
        prepareAsync(player)
    }

    /** Configure a MediaPlayer for assets/audio/<file>, or return null if the asset is absent.
     *  Preparation remains asynchronous so MIDI synthesis never stalls the UI thread. */
    private fun createUnprepared(context: Context, file: String): MediaPlayer? = runCatching {
        context.applicationContext.assets.openFd("$DIR/$file").use { afd ->
            MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
        }
    }.getOrNull()

    private fun prepareAsync(player: MediaPlayer) {
        if (runCatching { player.prepareAsync() }.isFailure) {
            if (theme === player) theme = null
            if (stinger === player) stinger = null
            player.release()
        }
    }

    private fun installErrorHandler(player: MediaPlayer) {
        player.setOnErrorListener { failed, _, _ ->
            if (theme === failed) {
                theme = null
                themePrepared = false
            }
            if (stinger === failed) stinger = null
            failed.release()
            true
        }
    }
}
