package com.acme.clara

import com.acme.clara.data.CityMeta
import com.acme.clara.data.Expansion
import com.acme.clara.data.Expansion2
import com.acme.clara.data.GameData
import org.junit.Test
import java.io.File

/**
 * Not a test — a translation-source EXTRACTOR run via the JUnit harness (the only reliable way to
 * read the real Kotlin data objects). Writes flat {key: englishText} catalogs under repo
 * `translation/source/` that the translation pipeline consumes. Keys match the runtime overlay
 * lookups (CityMeta.localize, the label wrappers, Strings.get). Re-run whenever source text changes.
 */
class ExtractCatalog {

    private val root = "/home/deniswsrosa/Documents/projects/WhereIS/translation/source"

    private fun q(s: String): String = buildString {
        append('"')
        for (ch in s) when (ch) {
            '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n")
            '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
        }
        append('"')
    }

    private fun write(name: String, map: Map<String, String>) {
        val f = File("$root/$name.json"); f.parentFile.mkdirs()
        f.writeText(buildString {
            append("{\n")
            val e = map.entries.toList()
            e.forEachIndexed { i, (k, v) ->
                append("  ").append(q(k)).append(": ").append(q(v))
                append(if (i < e.size - 1) ",\n" else "\n")
            }
            append("}\n")
        })
        println("[extract] ${map.size} -> $f")
    }

    @Test fun extractCities() {
        val out = LinkedHashMap<String, String>()
        for (c in CityMeta.all.values + Expansion.cities + Expansion2.cities) {
            val n = c.name
            if (c.description.isNotBlank()) out["city.$n.description"] = c.description
            if (c.landmark.isNotBlank()) out["city.$n.landmark"] = c.landmark
            c.greeting?.let { out["city.$n.greeting"] = it }
            c.flag?.let { out["city.$n.flag"] = it }
            c.clues.forEachIndexed { i, cl -> if (cl.isNotBlank()) out["city.$n.clue.$i"] = cl }
        }
        write("cities", out)
    }

    @Test fun extractGameplayLabels() {
        // Logic-key display values (rendered via label wrappers that keep the canonical English key).
        val out = LinkedHashMap<String, String>()
        GameData.ranks.forEach { out["rank.$it"] = it }
        GameData.occupations.forEach { out["occupation.$it"] = it }
        GameData.hobbies.forEach { out["hobby.$it"] = it }
        GameData.hairColors.forEach { out["hair.$it"] = it }
        GameData.features.forEach { out["feature.$it"] = it }
        GameData.vehicles.forEach { out["vehicle.$it"] = it }
        GameData.sexes.forEach { out["sex.$it"] = it }
        write("gameplay_labels", out)
    }
}
