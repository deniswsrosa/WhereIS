package com.acme.clara.data

/** One explicit finale in the ten-wave World Campaign. */
data class MastermindArc(
    val waveIndex: Int,
    val patentRank: Int,
    val family: String,
    val role: String,
    val suspectName: String,
    val claraFlavor: Boolean,
    val final: Boolean = false,
)

/**
 * The paid story starts after free Case 14. Every wave has a deliberate length and ends in one
 * regional capture. The first five finales catch the original bosses; the last five catch their
 * successors while Clara appears and escapes, until both the final successor and Clara are taken
 * in Wave 10. Keeping the cadence here makes route gating, rank progress and result copy agree.
 */
object Masterminds {
    val waveCases: List<Int> = listOf(8, 8, 8, 8, 10, 10, 10, 12, 12, 12)
    val waveEndCases: List<Int> = waveCases.runningFold(0, Int::plus).drop(1)

    val arcs: List<MastermindArc> = listOf(
        MastermindArc(0, 5, "Europe", "Boss", "Lady Agatha Wayland", claraFlavor = false),
        MastermindArc(1, 6, "the Americas", "Boss", "Merey LaRoc", claraFlavor = false),
        MastermindArc(2, 7, "Asia", "Boss", "Fast Eddie B.", claraFlavor = false),
        MastermindArc(3, 8, "Africa", "Boss", "Scar Graynolt", claraFlavor = false),
        MastermindArc(4, 9, "Oceania & the Frontiers", "Boss", "Dazzle Annie Nonker", claraFlavor = false),
        MastermindArc(5, 10, "Europe", "Successor", "Ihor Ihorovich", claraFlavor = true),
        MastermindArc(6, 11, "the Americas", "Successor", "Len \"Red\" Bulk", claraFlavor = true),
        MastermindArc(7, 12, "Asia", "Successor", "Katherine \"Boom-Boom\" Drib", claraFlavor = true),
        MastermindArc(8, 13, "Africa", "Successor", "Nick Brunch", claraFlavor = true),
        MastermindArc(9, 14, "Oceania & the Frontiers", "Successor", "Natasha Zhuravleva", claraFlavor = true, final = true),
    )

    fun arcForWave(waveIndex: Int): MastermindArc? = arcs.getOrNull(waveIndex)

    /** Returns an arc only when [campaignCasesSolved] lands exactly on a wave finale. */
    fun arcForCampaignCase(campaignCasesSolved: Int): MastermindArc? {
        val wave = waveEndCases.indexOf(campaignCasesSolved)
        return arcForWave(wave)
    }

    /** Wave currently being played; after the finale it remains Wave 10 for display purposes. */
    fun waveForCampaignCasesSolved(campaignCasesSolved: Int): Int {
        if (campaignCasesSolved <= 0) return 0
        val next = waveEndCases.indexOfFirst { campaignCasesSolved < it }
        return if (next < 0) arcs.lastIndex else next
    }

    fun casesIntoCurrentWave(campaignCasesSolved: Int): Int {
        val wave = waveForCampaignCasesSolved(campaignCasesSolved)
        val priorEnd = waveEndCases.getOrElse(wave - 1) { 0 }
        return (campaignCasesSolved - priorEnd).coerceIn(0, waveCases[wave])
    }

    fun casesToWaveFinale(campaignCasesSolved: Int): Int {
        val wave = waveForCampaignCasesSolved(campaignCasesSolved)
        return (waveCases[wave] - casesIntoCurrentWave(campaignCasesSolved)).coerceAtLeast(0)
    }

    /** Purchase opens Wave 1 immediately. Each captured mastermind opens the following wave. */
    fun unlockedMaxWave(rankIndex: Int, entitled: Boolean): Int =
        if (!entitled) -1 else Progression.unlockedMaxWave(rankIndex)

    fun arcForRank(rankIndex: Int): MastermindArc? = arcs.lastOrNull { it.patentRank <= rankIndex }
}
