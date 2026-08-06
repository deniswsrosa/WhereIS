package com.acme.clara.game

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.random.Random

/**
 * The game's comedy lines, loaded once from `assets/humor.json`.
 *
 * Witness lines are authored per-occupation, so a joke fits the witness telling it — a Harbor
 * Master's aside is about the pier, a Vice President's is about the office. The getters take the
 * witness's [occupation] and draw only from that occupation's lines (falling back to any if a given
 * occupation runs dry after filtering). Two flavours, kept apart so a joke never reads as a
 * non-sequitur:
 *  - "suspect" lines (on_trail) are ABOUT the crook ("They demanded my finest office…"), so they can
 *    be tacked onto a clue. They use "They", which the caller rewrites to the culprit's pronoun;
 *    lines whose grammar wouldn't survive that rewrite are dropped on load.
 *  - "self" lines (day_job) are the witness rambling about their own job — only used standalone.
 * Plus arrival-card quips (no occupation). Free-tier lines only in the free career; the paid track
 * unlocks the rest. Best-effort: if the asset never loads (unit tests, no Context) every getter
 * returns null and the callers simply skip the joke.
 */
object Humor {
    private class Line(val occ: String, val en: String, val free: Boolean)

    private var self: List<Line> = emptyList()      // day_job, by occupation
    private var suspect: List<Line> = emptyList()   // on_trail, by occupation (pronoun-convertible only)
    private var arrivalFree: List<String> = emptyList(); private var arrivalAll: List<String> = emptyList()
    @Volatile private var loadedLang: String? = null

    // "They <present-tense/contraction>" wouldn't survive rewriting "They" -> singular "He/She".
    private val unconvertible = Regex(
        "\\bthey\\s+(are|were|have|has|do|don'?t|keep|want|seem|will|can|'re|'ve|'ll)\\b",
        RegexOption.IGNORE_CASE)

    suspend fun init(context: Context) = load(context)

    /** Reload for the current language (call when the language changes). */
    suspend fun reload(context: Context) = load(context)

    // File I/O + JSON parsing of a several-hundred-KB catalog — kept off the caller's thread
    // (both call sites are Compose LaunchedEffect coroutines, so Dispatchers.Main by default)
    // so a startup or language switch never blocks a frame.
    private suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        val lang = com.acme.clara.i18n.Strings.language
        if (loadedLang == lang) return@withContext
        synchronized(this@Humor) {
            if (loadedLang == lang) return@withContext
            val ctx = context.applicationContext
            // Per-language humor lives in humor_<lang>.json; fall back to the English humor.json.
            val text = runCatching {
                val file = if (lang == "en") "humor.json" else "humor_$lang.json"
                ctx.assets.open(file).bufferedReader().use { it.readText() }
            }.getOrNull() ?: runCatching {
                ctx.assets.open("humor.json").bufferedReader().use { it.readText() }
            }.getOrNull() ?: return@withContext
            runCatching {
                val arr = JSONArray(text)
                val sf = ArrayList<Line>(); val pf = ArrayList<Line>()
                val aF = ArrayList<String>(); val aA = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val en = o.getString("en")
                    val free = o.optString("t", "free") == "free"
                    val occ = o.optString("o", "")
                    when (o.getString("s")) {
                        "arrival" -> { aA.add(en); if (free) aF.add(en) }
                        "on_trail" -> if (!unconvertible.containsMatchIn(en)) pf.add(Line(occ, en, free))
                        else -> sf.add(Line(occ, en, free))   // day_job
                    }
                }
                self = sf; suspect = pf; arrivalFree = aF; arrivalAll = aA
                loadedLang = lang
            }
        }
    }

    private fun pick(pool: List<String>): String? = if (pool.isEmpty()) null else pool[Random.nextInt(pool.size)]

    /** Draw from [pool] the lines matching [occupation] (and tier); if that occupation has none left,
     *  fall back to any occupation so a joke still appears. */
    private fun pickFor(pool: List<Line>, occupation: String, paid: Boolean): String? {
        val tier = pool.filter { paid || it.free }
        val mine = tier.filter { it.occ == occupation }
        return pick((if (mine.isNotEmpty()) mine else tier).map { it.en })
    }

    /** A standalone witness aside from this witness's own repertoire — for venue 3's whiff. */
    fun witnessLine(occupation: String, paid: Boolean): String? =
        pickFor(self + suspect, occupation, paid)

    /** A line ABOUT the suspect ("They …") from this witness's repertoire, for tacking onto a real
     *  clue. "They" must be rewritten to the culprit's pronoun by the caller. */
    fun suspectAside(occupation: String, paid: Boolean): String? = pickFor(suspect, occupation, paid)

    /** An arrival-card quip, shown instead of the say-hello line now and then. */
    fun arrivalLine(paid: Boolean): String? = pick(if (paid) arrivalAll else arrivalFree)
}
