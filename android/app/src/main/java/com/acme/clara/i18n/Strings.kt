package com.acme.clara.i18n

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
 *
 * [init] loads the (tiny) English catalog synchronously — it's the fallback for every lookup
 * below and must always be ready before the first frame. A non-English catalog can run into the
 * hundreds of KB, so it's parsed on a background thread instead; [ready] flips to true (and
 * [revision] bumps) once it lands. Callers on the UI thread should gate first-frame rendering on
 * [ready] to avoid a flash of untranslated (English-fallback) chrome.
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

    /** True once [active] holds the catalog for the resolved [language] — always true for English
     *  (loaded synchronously), false during the brief window a non-English cold-start catalog is
     *  still parsing on a background thread. The root composable should gate first-frame
     *  rendering on this so it never flashes untranslated chrome. */
    var ready by mutableStateOf(true)
        private set

    val language: String get() = lang

    fun init(context: Context) {
        val resolved = resolveLanguageAndEnglish(context)
        if (resolved == "en") {
            // English's catalog IS `en`, already loaded above — nothing to wait on.
            active = en
            ready = true
        } else {
            // Put the object in a valid, non-crashing state immediately (every lookup falls back
            // to `en`/the raw id while `active` is empty) and parse the real catalog off-thread so
            // it never blocks the first frame.
            active = emptyMap()
            ready = false
            loadAsync(resolved)
        }
    }

    /** Sets [appCtx]/[en]/[lang] from the saved preference (or device default). Shared by [init]
     *  and [ensureInit], which differ only in how they then populate [active]. */
    private fun resolveLanguageAndEnglish(context: Context): String {
        appCtx = context.applicationContext
        en = load("en")
        // No saved choice means first launch: follow the device language when we ship it.
        // The game keeps following the device until the player explicitly picks a language
        // in Options ▸ Language — only that persists a choice.
        val saved = appCtx!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        lang = if (saved != null) { if (saved in LANGUAGES) saved else "en" } else deviceDefault()
        return lang
    }

    /** Parses `assets/i18n/<code>.json` on a background thread and, if [lang] hasn't moved on to
     *  something else in the meantime, publishes it as [active] on the main thread. */
    private fun loadAsync(code: String) {
        Thread {
            val result = load(code)
            Handler(Looper.getMainLooper()).post {
                // Guards against a setLanguage() call racing ahead of this cold-start load.
                if (lang == code) {
                    active = result
                    ready = true
                    revision++
                }
            }
        }.start()
    }

    /** The device locale's language, mapped onto our catalog codes; English when unsupported.
     *  Android still reports Indonesian under the legacy ISO tag "in" — our catalog uses "id". */
    private fun deviceDefault(): String {
        val code = java.util.Locale.getDefault().language.let { if (it == "in") "id" else it }
        return if (code in LANGUAGES) code else "en"
    }

    /** Called from Options ▸ Language — a deliberate, infrequent user action, unlike [init]'s
     *  cold start. Stays synchronous on purpose: [ready] already gates the very first frame, and
     *  reusing that same gate mid-game (blanking the whole screen while the player is mid-session)
     *  would trade a sub-frame cold-start stall for a visible, worse mid-game one. The parse
     *  itself is still just a few hundred KB and finishes well within one frame in practice. */
    fun setLanguage(code: String) {
        if (code !in LANGUAGES) return
        // Persist even a pick that matches the current (device-derived) language, so an
        // explicit choice stays put if the phone's language changes later.
        appCtx?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString(KEY, code)?.apply()
        if (code == lang) return
        lang = code
        active = if (code == "en") en else load(code)
        ready = true
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

    /** Localized display name for a place (map labels, clock box, lists, sentences). The canonical
     *  English name stays the logic/save key everywhere; key is `city.<name>.name` alongside the
     *  place's other catalog entries. Falls back to the English name. */
    fun place(name: String): String = if (lang == "en") name else active["city.$name.name"] ?: name

    /** Load the catalog when called outside the Activity lifecycle (workers, notifications).
     *  Unlike [init], this stays fully synchronous: these callers (e.g. [WelcomeBackWorker])
     *  read [ui]/[get] immediately afterward on a background thread with no Compose recomposition
     *  to pick up a later async update, so a partial (English-fallback) catalog here would leak
     *  into user-visible text — a background thread has no first frame to protect, so there's
     *  nothing to gain by not blocking it for the parse. */
    fun ensureInit(context: Context) {
        if (appCtx != null) return
        val resolved = resolveLanguageAndEnglish(context)
        active = if (resolved == "en") en else load(resolved)
        ready = true
    }

    /** UI-chrome convenience: the English text stays at the call site and doubles as the fallback,
     *  so wrapping a literal never changes the English build. Translation is keyed by "ui:<english>".
     *  [args] fill `{0}`,`{1}`,… placeholders (kept out of the key). */
    fun ui(en: String, vararg args: Any?): String {
        var s = if (lang == "en") en else active["ui:$en"] ?: en
        args.forEachIndexed { i, a -> s = s.replace("{$i}", a?.toString() ?: "") }
        return s
    }

    /** [get] with `{0}`, `{1}`, … placeholder substitution (order-independent across languages). */
    fun fmt(id: String, vararg args: Any?): String {
        var s = get(id)
        args.forEachIndexed { i, a -> s = s.replace("{$i}", a?.toString() ?: "") }
        return s
    }

    /** Localized promotion-quiz text. The answer MUST use this too, not the English fallback
     *  directly — a translated question expects its answer in the same language (e.g. a
     *  Portuguese player types "Nilo", not "Nile"). */
    fun quizQuestion(item: com.acme.clara.data.QuizItem): String =
        if (lang == "en") item.question else active["quiz.${item.id}.q"] ?: item.question
    fun quizAnswer(item: com.acme.clara.data.QuizItem): String =
        if (lang == "en") item.answer else active["quiz.${item.id}.a"] ?: item.answer
}
