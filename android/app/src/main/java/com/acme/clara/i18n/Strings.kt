package com.acme.clara.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * Runtime string catalog. Each language is a flat JSON map (id -> text) under `assets/i18n/<code>.json`.
 *
 * Two kinds of lookups:
 *  - [get]/[fmt] — UI-chrome strings whose English lives ONLY in the catalog (en.json). Falls back to
 *    English, then to the raw id if truly missing.
 *  - [opt] — an overlay for content whose English stays in Kotlin data (city descriptions, clues,
 *    humor…). Returns null when English is active or the key isn't translated, so the caller keeps its
 *    Kotlin fallback. This lets a language be partially translated without ever breaking the build.
 *
 * Changing the language bumps [revision]; the root composable keys off it to re-render everything.
 */
object Strings {
    /** Offered languages: code -> endonym shown in the picker. */
    val LANGUAGES: Map<String, String> = linkedMapOf(
        "en" to "English",
        "pt" to "Português (BR)",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "nl" to "Nederlands",
        "pl" to "Polski",
        "ru" to "Русский",
        "tr" to "Türkçe",
        "id" to "Bahasa Indonesia",
    )

    private const val PREFS = "clara_settings"
    private const val KEY = "language"

    @Volatile private var lang = "en"
    private var active: Map<String, String> = emptyMap()
    private var en: Map<String, String> = emptyMap()
    private var appCtx: Context? = null

    /** Bumped on every language change so the Compose tree recomposes. */
    var revision by mutableIntStateOf(0)
        private set

    val language: String get() = lang

    fun init(context: Context) {
        appCtx = context.applicationContext
        en = load("en")
        val saved = appCtx!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "en") ?: "en"
        lang = if (saved in LANGUAGES) saved else "en"
        active = if (lang == "en") en else load(lang)
    }

    fun setLanguage(code: String) {
        if (code !in LANGUAGES || code == lang) return
        lang = code
        active = if (code == "en") en else load(code)
        appCtx?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString(KEY, code)?.apply()
        revision++
    }

    private fun load(code: String): Map<String, String> = runCatching {
        val txt = appCtx!!.assets.open("i18n/$code.json").bufferedReader().use { it.readText() }
        val o = JSONObject(txt)
        buildMap { o.keys().forEach { k -> put(k, o.getString(k)) } }
    }.getOrDefault(emptyMap())

    /** A UI-chrome string; the id itself is returned only if it's missing from every catalog. */
    fun get(id: String): String = active[id] ?: en[id] ?: id

    /** An overlay translation for content whose English stays in Kotlin: null when English is active
     *  or the key isn't translated, so the caller uses its own fallback. */
    fun opt(id: String): String? = if (lang == "en") null else active[id]

    /** Localize a value that ALSO doubles as a game-logic key (a rank, trait value, occupation, venue,
     *  region…): the caller keeps passing the canonical English [value] to logic and calls this only
     *  to render it. Key is `<prefix>.<value>`; falls back to the English value. */
    fun label(prefix: String, value: String): String =
        if (lang == "en") value else active["$prefix.$value"] ?: value

    /** [get] with `{0}`, `{1}`, … placeholder substitution (order-independent across languages). */
    fun fmt(id: String, vararg args: Any?): String {
        var s = get(id)
        args.forEachIndexed { i, a -> s = s.replace("{$i}", a?.toString() ?: "") }
        return s
    }
}
