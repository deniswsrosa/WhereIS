package com.acme.clara.save

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.concurrent.Executors

/**
 * File-backed [SaveRepository]: one JSON file per career under `filesDir/saves/`.
 * Continuous autosave writes here on every consequential action; a corrupt or
 * partially-written file is skipped rather than crashing the picker.
 */
class SaveStore(context: Context) : SaveRepository {

    private val dir = File(context.filesDir, "saves").apply { mkdirs() }
    private val expansionEntitlement = File(context.filesDir, "world-campaign-owned")
    private val pendingSignOn = File(context.filesDir, "new-career-pending")

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

    /** AtomicFile keeps the previous complete JSON if the process dies during replacement. */
    private fun writeNow(data: SaveData) {
        val atomic = AtomicFile(fileFor(data.meta.id))
        var output: java.io.FileOutputStream? = null
        try {
            output = atomic.startWrite()
            output.write(SaveCodec.encode(data.meta, data.state).toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (e: Exception) {
            output?.let { atomic.failWrite(it) }
            throw e
        }
    }

    private fun readNow(file: File): SaveData? = runCatching {
        AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { SaveCodec.decode(it.readText()) }
    }.getOrNull()

    override fun list(): List<SaveMeta> =
        (dir.listFiles() ?: emptyArray())
            .mapNotNull { f ->
                when {
                    f.name.endsWith(".json") -> f
                    // AtomicFile.openRead() restores this backup if the process died between
                    // moving the old base aside and finishing its replacement.
                    f.name.endsWith(".json.bak") -> File(dir, f.name.removeSuffix(".bak"))
                    else -> null
                }
            }
            .distinctBy { it.path }
            .mapNotNull { f -> readNow(f)?.meta }
            .sortedByDescending { it.lastPlayed }

    override fun load(id: String): SaveData? =
        fileFor(id).takeIf { it.exists() || File("${it.path}.bak").exists() }
            ?.let(::readNow)

    override fun save(data: SaveData) {
        writer.execute { runCatching { writeNow(data) } }
    }

    override fun delete(id: String) {
        // Serialize behind older autosaves and wait: otherwise a queued write can recreate the
        // career immediately after the picker has retired it.
        runCatching { writer.submit { AtomicFile(fileFor(id)).delete() }.get() }
    }

    override fun hasPendingSignOn(): Boolean = pendingSignOn.exists()

    override fun setPendingSignOn(pending: Boolean) {
        runCatching { if (pending) pendingSignOn.writeText("pending") else pendingSignOn.delete() }
    }

    override fun saveNewCareer(data: SaveData) {
        // This one small, one-time write is synchronous so a confirmed name cannot be followed by
        // a process death that clears the draft marker before the career itself reaches disk.
        runCatching {
            awaitPendingWrites()
            writeNow(data)
            pendingSignOn.delete()
        }
    }

    override fun ownsExpansion(): Boolean = expansionEntitlement.exists()

    override fun setExpansionOwned() {
        // A tiny monotonic marker is deliberately separate from career saves: deleting, replacing,
        // or creating a profile must never revoke a Play-owned non-consumable purchase.
        runCatching { if (!expansionEntitlement.exists()) expansionEntitlement.writeText("owned") }
    }

    /** Test-only: block until every write submitted so far has landed on disk. Production code
     *  never needs this — the whole point of [save] is that callers don't wait — but a test that
     *  saves and then immediately reads back via a fresh [SaveStore]/[File] needs the write to be
     *  visible first. Submitting a no-op to the same single-threaded executor and joining it is
     *  enough: FIFO order guarantees every write queued before this one has already run. */
    fun awaitPendingWrites() { writer.submit {}.get() }
}
