package com.acme.clara.save

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

/**
 * File-backed [SaveRepository]: one JSON file per career under `filesDir/saves/`.
 * Continuous autosave writes here on every consequential action; a corrupt or
 * partially-written file is skipped rather than crashing the picker.
 */
class SaveStore(context: Context) : SaveRepository {

    private val dir = File(context.filesDir, "saves").apply { mkdirs() }

    // autosave() (ClaraViewModel) calls save() synchronously on nearly every player action, and
    // a save now carries the full 231-city roster — encoding + writing that on the caller's own
    // thread (the UI thread, for every real call site) is a genuine ANR source. A single-threaded
    // executor moves the work off the caller without needing a coroutine dispatcher (which would
    // require Dispatchers.Main to be live — not guaranteed in a plain JVM unit test), and being
    // single-threaded keeps writes strictly in submission order: a newer state can never be
    // overwritten by an older one that happened to finish encoding later. list()/load()/delete()
    // stay synchronous — callers either already dispatch them off the main thread themselves
    // (MainActivity's cold-start read) or specifically depend on them completing before the next
    // line runs (ChooseGameScreen's delete-then-refresh).
    private val writer = Executors.newSingleThreadExecutor()

    private fun sanitize(id: String) = id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
    private fun fileFor(id: String) = File(dir, "save-${sanitize(id)}.json")

    override fun list(): List<SaveMeta> =
        (dir.listFiles { f -> f.extension == "json" } ?: emptyArray())
            .mapNotNull { f -> runCatching { SaveCodec.decode(f.readText())?.meta }.getOrNull() }
            .sortedByDescending { it.lastPlayed }

    override fun load(id: String): SaveData? =
        fileFor(id).takeIf { it.exists() }
            ?.let { runCatching { SaveCodec.decode(it.readText()) }.getOrNull() }

    override fun save(data: SaveData) {
        val file = fileFor(data.meta.id)
        writer.execute { runCatching { file.writeText(SaveCodec.encode(data.meta, data.state)) } }
    }

    override fun delete(id: String) {
        runCatching { fileFor(id).delete() }
    }

    /** Test-only: block until every write submitted so far has landed on disk. Production code
     *  never needs this — the whole point of [save] is that callers don't wait — but a test that
     *  saves and then immediately reads back via a fresh [SaveStore]/[File] needs the write to be
     *  visible first. Submitting a no-op to the same single-threaded executor and joining it is
     *  enough: FIFO order guarantees every write queued before this one has already run. */
    fun awaitPendingWrites() { writer.submit {}.get() }
}
