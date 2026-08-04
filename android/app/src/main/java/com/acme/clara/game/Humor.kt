package com.acme.clara.game

import android.content.Context
import org.json.JSONArray
import kotlin.random.Random

/**
 * The game's comedy lines, loaded once from `assets/humor.json`. Witnesses drop one now and then
 * (venue 3's whiff, or a ~30% flourish on a real clue) and the arrival card occasionally jokes
 * instead of teaching a greeting. Free-tier lines only in the free career; the paid track unlocks
 * the rest. All best-effort: if the asset never loads (e.g. unit tests, no Context), every getter
 * returns null and the callers simply skip the joke.
 */
object Humor {
    private var witnessFree: List<String> = emptyList()   // day_job + on_trail, free tier
    private var witnessAll: List<String> = emptyList()
    private var arrivalFree: List<String> = emptyList()   // arrival_travel, free tier
    private var arrivalAll: List<String> = emptyList()
    @Volatile private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            runCatching {
                val text = context.applicationContext.assets.open("humor.json")
                    .bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val wF = ArrayList<String>(); val wA = ArrayList<String>()
                val aF = ArrayList<String>(); val aA = ArrayList<String>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val en = o.getString("en")
                    val free = o.optString("t", "free") == "free"
                    if (o.getString("s") == "arrival") { aA.add(en); if (free) aF.add(en) }
                    else { wA.add(en); if (free) wF.add(en) }   // day_job | on_trail
                }
                witnessFree = wF; witnessAll = wA; arrivalFree = aF; arrivalAll = aA
                loaded = true
            }
        }
    }

    private fun pick(pool: List<String>): String? =
        if (pool.isEmpty()) null else pool[Random.nextInt(pool.size)]

    /** A witness aside — about the crook they saw or their own odd job. */
    fun witnessLine(paid: Boolean): String? = pick(if (paid) witnessAll else witnessFree)

    /** An arrival-card quip, shown instead of the say-hello line now and then. */
    fun arrivalLine(paid: Boolean): String? = pick(if (paid) arrivalAll else arrivalFree)
}
