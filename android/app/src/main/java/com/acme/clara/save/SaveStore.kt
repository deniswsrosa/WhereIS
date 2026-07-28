package com.acme.clara.save

import android.content.Context
import java.io.File

/**
 * File-backed [SaveRepository]: one JSON file per career under `filesDir/saves/`.
 * Continuous autosave writes here on every consequential action; a corrupt or
 * partially-written file is skipped rather than crashing the picker.
 */
class SaveStore(context: Context) : SaveRepository {

    private val dir = File(context.filesDir, "saves").apply { mkdirs() }

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
        runCatching { fileFor(data.meta.id).writeText(SaveCodec.encode(data.meta, data.state)) }
    }

    override fun delete(id: String) {
        runCatching { fileFor(id).delete() }
    }
}
