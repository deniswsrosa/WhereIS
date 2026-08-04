package com.acme.clara.game

import android.content.Context
import org.json.JSONArray
import kotlin.random.Random

/**
 * The game's comedy lines, loaded once from `assets/humor.json`.
 *
 * Two witness flavours, kept apart so a joke never reads as a non-sequitur:
 *  - "suspect" lines (on_trail) are ABOUT the crook ("They demanded my finest office…"), so they
 *    can be tacked onto a clue and still flow. They use "They", which the caller rewrites to the
 *    culprit's pronoun; lines whose grammar wouldn't survive that rewrite are dropped on load.
 *  - "self" lines (day_job) are the witness rambling about their own job — only used standalone.
 * Plus arrival-card quips. Free-tier lines only in the free career; the paid track unlocks the rest.
 * Best-effort: if the asset never loads (unit tests, no Context) every getter returns null and the
 * callers simply skip the joke.
 */
object Humor {
    private var selfFree: List<String> = emptyList();    private var selfAll: List<String> = emptyList()
    private var suspectFree: List<String> = emptyList(); private var suspectAll: List<String> = emptyList()
    private var arrivalFree: List<String> = emptyList(); private var arrivalAll: List<String> = emptyList()
    @Volatile private var loaded = false

    // "They <present-tense/contraction>" wouldn't survive rewriting "They" -> singular "He/She".
    private val unconvertible = Regex(
        "\\bthey\\s+(are|were|have|has|do|don'?t|keep|want|seem|will|can|'re|'ve|'ll)\\b",
        RegexOption.IGNORE_CASE)

    fun init(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            runCatching {
                val text = context.applicationContext.assets.open("humor.json")
                    .bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val sF = ArrayList<String>(); val sA = ArrayList<String>()
                val pF = ArrayList<String>(); val pA = ArrayList<String>()
                val aF = ArrayList<String>(); val aA = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val en = o.getString("en")
                    val free = o.optString("t", "free") == "free"
                    when (o.getString("s")) {
                        "arrival" -> { aA.add(en); if (free) aF.add(en) }
                        "on_trail" -> if (!unconvertible.containsMatchIn(en)) { pA.add(en); if (free) pF.add(en) }
                        else -> { sA.add(en); if (free) sF.add(en) }   // day_job
                    }
                }
                selfFree = sF; selfAll = sA; suspectFree = pF; suspectAll = pA; arrivalFree = aF; arrivalAll = aA
                loaded = true
            }
        }
    }

    private fun pick(pool: List<String>): String? = if (pool.isEmpty()) null else pool[Random.nextInt(pool.size)]

    /** A standalone witness aside (self-ramble or suspect antic) — for venue 3's whiff. */
    fun witnessLine(paid: Boolean): String? {
        val self = if (paid) selfAll else selfFree
        val suspect = if (paid) suspectAll else suspectFree
        val pool = self + suspect
        return pick(pool)
    }

    /** A line ABOUT the suspect ("They …"), for tacking onto a real clue. "They" must be rewritten
     *  to the culprit's pronoun by the caller. */
    fun suspectAside(paid: Boolean): String? = pick(if (paid) suspectAll else suspectFree)

    /** An arrival-card quip, shown instead of the say-hello line now and then. */
    fun arrivalLine(paid: Boolean): String? = pick(if (paid) arrivalAll else arrivalFree)
}
