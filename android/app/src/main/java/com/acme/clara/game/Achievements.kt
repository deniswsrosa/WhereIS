package com.acme.clara.game

import com.acme.clara.data.GameData

/** One in-world commendation. Titles stay diegetic (detective ranks / honours). */
data class Achievement(val id: String, val title: String, val desc: String)

/** A snapshot of the career facts the catalog checks against. */
data class CareerSummary(
    val casesSolved: Int,
    val rankIndex: Int,
    val hintFreeSolves: Int,
    val hadCleanCase: Boolean,
    val capturedCount: Int,
    val totalVillains: Int,
    val careerOver: Boolean,
)

/**
 * A small, curated set of commendations — milestones and play styles, never stage-count
 * spam. Each is a pure function of the career record, so re-evaluating is idempotent.
 */
object Achievements {
    val catalog: List<Achievement> = listOf(
        Achievement("first_arrest", "First Arrest", "Close your first case."),
        Achievement("clean_sweep", "Clean Sweep", "Solve a case with no wrong flight."),
        Achievement("promoted", "Promoted", "Earn your first promotion."),
        Achievement("bloodhound", "Bloodhound", "Solve five cases without a hint."),
        Achievement("veteran", "Veteran", "Solve ten cases."),
        Achievement("most_wanted", "Most Wanted", "Capture every villain on the board."),
        Achievement("kingpin", "Kingpin", "Jail the ring-leader and close the career."),
    )

    private val ids = catalog.map { it.id }.toSet()

    fun titleOf(id: String): String = catalog.firstOrNull { it.id == id }?.title ?: id

    /** Every achievement id the career currently satisfies (cumulative, order-independent). */
    fun earned(c: CareerSummary): Set<String> = buildSet {
        if (c.casesSolved >= 1) add("first_arrest")
        if (c.hadCleanCase) add("clean_sweep")
        if (c.rankIndex >= 1) add("promoted")
        if (c.hintFreeSolves >= 5) add("bloodhound")
        if (c.casesSolved >= 10) add("veteran")
        if (c.totalVillains > 0 && c.capturedCount >= c.totalVillains) add("most_wanted")
        if (c.careerOver) add("kingpin")
    }

    fun summarise(state: GameState) = CareerSummary(
        casesSolved = state.casesSolved,
        rankIndex = state.rankIndex,
        hintFreeSolves = state.hintFreeSolves,
        hadCleanCase = state.hadCleanCase,
        capturedCount = state.capturedVillains.size,
        totalVillains = GameData.suspects.size,
        careerOver = state.careerOver,
    )

    /** Any newly-unlocked ids not already in [already] — for a "commendation earned" toast. */
    fun newlyEarned(state: GameState, already: Set<String>): Set<String> =
        earned(summarise(state)).filter { it in ids }.toSet() - already
}
