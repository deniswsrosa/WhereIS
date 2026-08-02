package com.acme.clara.game

import kotlin.random.Random

/**
 * L4 — spaced repetition for the world. Every place logs the case index it was last seen
 * ([GameState.cityLastSeen]); this schedules geography to recur on an expanding curve so the
 * facts stick. It only influences WHICH cities appear on a route and how the almanac flags
 * them — never any clue or the solvability guard, so the deduction loop is untouched.
 */
object SpacedRepetition {
    // Review windows, in cases elapsed. A fact whose gap lands on one of these is "due".
    private val INTERVALS = intArrayOf(1, 3, 7, 15)

    private fun gap(city: String, lastSeen: Map<String, Int>, currentCase: Int): Int? =
        lastSeen[city]?.let { currentCase - it }

    /** Seen this very case cycle — the almanac shows "seen recently". */
    fun isFresh(city: String, lastSeen: Map<String, Int>, currentCase: Int): Boolean =
        gap(city, lastSeen, currentCase) == 0

    /** Its review interval has come due — the almanac shows "review due". */
    fun isDue(city: String, lastSeen: Map<String, Int>, currentCase: Int): Boolean {
        val g = gap(city, lastSeen, currentCase) ?: return false
        return INTERVALS.any { g == it }
    }

    /**
     * Weighted route pick: never-taught and due-for-review cities are favoured, just-seen ones
     * are damped to avoid an immediate repeat. Falls back to the whole (shuffled) list when the
     * pool is no larger than the route.
     */
    fun pickRoute(cities: List<String>, lastSeen: Map<String, Int>, currentCase: Int, n: Int): List<String> {
        if (cities.size <= n) return cities.shuffled()
        fun weight(c: String): Double {
            val g = gap(c, lastSeen, currentCase)
            return when {
                g == null -> 3.0                    // never taught — introduce it
                INTERVALS.any { g == it } -> 4.0    // due for review — resurface it
                g == 0 -> 0.4                        // just seen — avoid an immediate repeat
                else -> 1.0
            }
        }
        val pool = cities.toMutableList()
        val out = ArrayList<String>(n)
        repeat(n) {
            val total = pool.sumOf { weight(it) }
            var r = Random.nextDouble() * total
            var idx = pool.lastIndex
            for (i in pool.indices) {
                r -= weight(pool[i])
                if (r <= 0.0) { idx = i; break }
            }
            out.add(pool.removeAt(idx))
        }
        return out
    }
}
