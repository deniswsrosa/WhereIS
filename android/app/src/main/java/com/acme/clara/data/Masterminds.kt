package com.acme.clara.data

/**
 * The paid-campaign story layer: Clara runs a network of regional crime families. Each family
 * has a Boss (caught first, in that region's marquee wave) and a Successor (caught later, in
 * that region's lesser-known wave, five waves on) who took over when the Boss fell. Clara
 * herself is only ever the forced culprit twice: Case 14 (she escapes — the inciting incident)
 * and the campaign finale (she's truly caught).
 *
 * Deliberately reuses the EXISTING International-track promotion cadence in [ClaraViewModel] —
 * one promotion every 8 cases starting at case 14 (see `intlThreshold`) — rather than inventing a
 * parallel gating system. Case 14's promotion (Ace Detective -> Special Agent) is the
 * "enrollment" beat and isn't in [arcs]; the 9 promotions after it (cases 22, 30, ..., 86) each
 * map to one entry here, keyed by [triggerIndex] = (casesSolved - GameState.CAREER_CASES) / 8.
 *
 * Cast: every mastermind below is one of the 9 existing non-Clara dossier suspects
 * (GameData.suspects) — all nine already ship a portrait sprite
 * (assets/sprites/suspects/suspect_*.png), so this adds zero new character art. Only 9
 * International promotions remain after Case 14's enrollment, and there are 5 Bosses + 5
 * Successors (10) worth of story to tell, so Africa's Successor (Nick Brunch) is folded into the
 * finale case's flavor text — captured in the same raid as Clara — rather than getting a
 * separate case. Oceania & the Frontiers' lesser-known wave (Progression.wave index 9) has no
 * dedicated capture case either; its cities simply join the pool once Chief Director is reached.
 * Both are easy to split into their own beats later if an 11th/12th promotion tier is ever added.
 */
data class MastermindArc(
    val triggerIndex: Int,     // (casesSolved - GameState.CAREER_CASES) / 8 at the case this fires on
    val patentRank: Int,       // GameData.ranks index this capture awards (6..14)
    val family: String,        // region label used in result/UI copy
    val role: String,          // "Boss" | "Successor" | "Finale"
    val suspectName: String,   // GameData.suspects entry forced as this case's culprit
    val waveForRoute: Int?,    // Progression.wave index every destination is restricted to
    val claraFlavor: Boolean,  // append a "Clara was seen fleeing" line to the result
)

object Masterminds {
    val arcs: List<MastermindArc> = listOf(
        MastermindArc(1, 6, "Europe", "Boss", "Lady Agatha Wayland", waveForRoute = 0, claraFlavor = false),
        MastermindArc(2, 7, "the Americas", "Boss", "Merey LaRoc", waveForRoute = 1, claraFlavor = false),
        MastermindArc(3, 8, "Asia", "Boss", "Fast Eddie B.", waveForRoute = 2, claraFlavor = false),
        MastermindArc(4, 9, "Africa", "Boss", "Scar Graynolt", waveForRoute = 3, claraFlavor = false),
        MastermindArc(5, 10, "Oceania & the Frontiers", "Boss", "Dazzle Annie Nonker", waveForRoute = 4, claraFlavor = false),
        MastermindArc(6, 11, "Europe", "Successor", "Ihor Ihorovich", waveForRoute = 5, claraFlavor = true),
        MastermindArc(7, 12, "the Americas", "Successor", "Len \"Red\" Bulk", waveForRoute = 6, claraFlavor = true),
        MastermindArc(8, 13, "Asia", "Successor", "Katherine \"Boom-Boom\" Drib", waveForRoute = 7, claraFlavor = true),
        // The finale: Clara herself is forced as culprit (see ClaraViewModel), not Nick Brunch —
        // his capture is folded into this same case's flavor text as "taken in the same raid."
        MastermindArc(9, 14, "Africa", "Finale", "Nick Brunch", waveForRoute = 8, claraFlavor = true),
    )

    fun arcForTrigger(i: Int): MastermindArc? = arcs.firstOrNull { it.triggerIndex == i }

    /** Highest trigger index reachable at [rank] (5..14) — i.e. the arc last captured. */
    fun arcForRank(rank: Int): MastermindArc? = arcs.lastOrNull { it.patentRank <= rank }
}
