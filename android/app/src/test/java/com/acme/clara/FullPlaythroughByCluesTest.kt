package com.acme.clara

import com.acme.clara.data.CityMeta
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ClueKind
import com.acme.clara.game.Phase
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plays the whole free career (14 cases, ending by catching Clara) the way a careful, EFFICIENT
 * player does: it only ever reads what a player can see — witness lines, depart options, the almanac
 * — never the hidden route or culprit. At each city it opens just enough witnesses to identify the
 * next country (via landmark / flag / currency / hello sound) and, early on, to build the warrant.
 * Run across many careers, it proves every case is beatable without exhaustive searching.
 */
class FullPlaythroughByCluesTest {

    /** Does this witness clue point at [city]? Checks every identifying feature a player can look up.
     *  Flag/currency can be shared, so the caller cross-references several clues to disambiguate. */
    private fun matches(clue: String, city: String): Boolean {
        val info = CityMeta.of(city)
        if (info.landmark.isNotBlank() && clue.contains(info.landmark, ignoreCase = true)) return true
        info.flag?.let { if (clue.contains(it)) return true }
        // the currency clue drops the article ("the euro" -> "euro"), so match on the bare name
        info.currency?.removePrefix("the ")?.let { if (clue.contains(it)) return true }
        info.greeting?.let { g ->
            Regex("\\(([^)]+)\\)").find(g)?.groupValues?.get(1)?.let { if (clue.contains(it)) return true }
        }
        return false
    }

    /** The depart option the accumulated trail clues uniquely point to, or null if none/ambiguous. */
    private fun identify(trail: List<String>, options: List<String>): String? {
        if (trail.isEmpty()) return null
        val scores = options.associateWith { c -> trail.count { matches(it, c) } }
        val best = scores.maxByOrNull { it.value } ?: return null
        if (best.value == 0) return null
        return if (scores.count { it.value == best.value } == 1) best.key else null
    }

    private class Report {
        var careersFinished = 0; var casesWon = 0
        var wrongFlights = 0; var forcedGuesses = 0; var venuesOpened = 0; var cityStops = 0
        var minMargin = Int.MAX_VALUE; var minMarginTopFree = Int.MAX_VALUE; var minMarginTopGrade = Int.MAX_VALUE
        var routeFallbacks = 0
        val failures = mutableListOf<String>()
    }

    private fun absorbTrait(vm: ClaraViewModel) {
        val c = vm.s.openClue ?: return
        if (c.kind == ClueKind.TRAIT) c.trait?.let { (cat, value) -> vm.setComp(cat, value); vm.compute() }
        vm.closeClue()
    }

    private fun playCase(vm: ClaraViewModel, r: Report, allowRoute: Boolean = false): Boolean {
        if (vm.s.phase == Phase.BRIEFING) vm.beginInvestigation()
        // When the clues don't resolve, a free-career player is stuck guessing; a paid player is
        // assumed to look the hand-authored lead up in the almanac (route fallback) — the test can't
        // reverse-map those free-text clues, but everything else (deadline, warrant, whiffs) is real.
        fun fallback(opts: List<String>): String {
            if (allowRoute) vm.s.route.getOrNull(vm.s.progress + 1)?.let { if (it in opts) { r.routeFallbacks++; return it } }
            r.forcedGuesses++; return opts.random()
        }
        var lastTrail: List<String> = emptyList()
        var guard = 0
        while (guard++ < 120) {
            if (vm.s.deadlinePassed || vm.s.phase == Phase.RESULT) break

            if (vm.s.atHideout) {
                var g = 0
                while (vm.s.phase == Phase.CITY && g++ < 4) { vm.openVenue(g - 1); r.venuesOpened++; absorbTrait(vm); if (vm.s.deadlinePassed) break }
                if (vm.s.phase == Phase.CHASE) vm.chaseDone()
                break
            }

            val opts = vm.s.departOptions
            if (opts.isEmpty()) { r.failures.add("no depart options at ${vm.s.currentCity}"); break }

            if (!vm.s.onTrack) {
                // wrong city: re-pick using the clues from the last on-track city against fresh options
                val pick = identify(lastTrail, opts) ?: fallback(opts)
                vm.travelTo(pick); vm.arrive()
                if (!vm.s.onTrack && !vm.s.atHideout) r.wrongFlights++
                continue
            }

            // On-track: open witnesses one at a time until we can identify the next country and,
            // while we still need it, have grabbed this city's trait.
            val trail = mutableListOf<String>()
            var gotTrait = false
            for (i in 0..2) {
                if (i in vm.s.visited) continue
                vm.openVenue(i); r.venuesOpened++
                vm.s.openClue?.let { c ->
                    when (c.kind) {
                        ClueKind.TRAIT -> { gotTrait = true; absorbTrait(vm) }
                        ClueKind.DESTINATION -> { trail.add(c.text); vm.closeClue() }
                        else -> vm.closeClue()
                    }
                }
                if (vm.s.deadlinePassed) break
                val id = identify(trail, opts) != null
                val enoughHere = vm.s.warrantFor != null || gotTrait
                // Free player must identify the trail from clues; a paid player will look the
                // hand-authored lead up (route fallback), so it only needs its trait done here.
                if (enoughHere && (id || allowRoute)) break   // efficient: stop once we know enough
            }
            r.cityStops++
            lastTrail = trail
            if (vm.s.deadlinePassed) break

            val pick = identify(trail, opts) ?: fallback(opts)
            vm.travelTo(pick); vm.arrive()
            if (!vm.s.onTrack && !vm.s.atHideout) r.wrongFlights++
        }
        return vm.s.won
    }

    @Test fun theFreeCareerIsBeatableByAnEfficientPlayer() {
        // Case 14 is the story's inciting incident, not a clean capture: an unpaid player always
        // sees Clara get away there (see Masterminds.kt), and the free career keeps looping after
        // it rather than ending — so "finished" now means "reached and solved case 14", checked by
        // case count, not by the career-over flag (which an unpaid career never sets anymore).
        val r = Report()
        val careers = 40
        repeat(careers) { n ->
            val vm = ClaraViewModel().apply { signOn("Gumshoe$n") }
            var guard = 0
            while (vm.s.casesSolved < 14 && guard++ < 30) {
                val rank = vm.s.rankIndex
                if (playCase(vm, r)) {
                    r.casesWon++
                    val margin = vm.s.caseDeadlineHours - vm.s.clock
                    r.minMargin = minOf(r.minMargin, margin)
                    if (rank == 4) r.minMarginTopFree = minOf(r.minMarginTopFree, margin)
                } else { r.failures.add("career $n LOST case ${vm.s.casesSolved + 1} (rank $rank): " +
                    vm.s.resultLines.joinToString(" ").take(70)); break }
                if (vm.s.pendingPromotion) vm.resolvePromotion(true)
                if (vm.s.casesSolved >= 14) break
                vm.nextCase()
            }
            if (vm.s.casesSolved >= 14) r.careersFinished++
        }

        println("=============== EFFICIENT FREE-CAREER PLAYTHROUGH (x$careers) ===============")
        println("careers finished (caught Clara at case 14): ${r.careersFinished}/$careers")
        println("cases won: ${r.casesWon}   wrong flights: ${r.wrongFlights}   forced guesses: ${r.forcedGuesses}")
        println("avg venues opened per city stop: ${"%.2f".format(r.venuesOpened.toDouble() / r.cityStops.coerceAtLeast(1))}")
        println("min spare hours at a win: ${r.minMargin}h overall   |   ${r.minMarginTopFree}h at the top free rank (Ace)")
        if (r.failures.isNotEmpty()) { println("---- problems ----"); r.failures.take(20).forEach { println("  - $it") } }
        println("==========================================================================")

        assertTrue("every free career must be beatable by an efficient player " +
            "(finished ${r.careersFinished}/$careers)", r.careersFinished == careers)
    }

    @Test fun theWholeLadderIsWinnableIncludingThePaidGrades() {
        // Climb from Rookie all the way to the top International grade. Free-career hops navigate by
        // clue; paid hops fall back to an almanac lookup (their hand-authored leads aren't
        // reverse-mappable), but the clock, warrant-building, promotions, venue-3 whiffs and the
        // catch are all exercised for real — this is the check the free-career test can't give.
        val r = Report()
        val runs = 12
        val casesEach = 100          // deep enough to climb well into the International grades
        var minRankReached = 99
        repeat(runs) { n ->
            val vm = ClaraViewModel().apply { signOn("Interpol$n"); unlockExpansion() }
            var guard = 0
            while (guard++ < casesEach) {
                val rank = vm.s.rankIndex
                if (playCase(vm, r, allowRoute = true)) {
                    r.casesWon++
                    val margin = vm.s.caseDeadlineHours - vm.s.clock
                    r.minMargin = minOf(r.minMargin, margin)
                    if (rank >= 12) r.minMarginTopGrade = minOf(r.minMarginTopGrade, margin)
                } else {
                    r.failures.add("run $n LOST case ${vm.s.casesSolved + 1} (rank $rank): " +
                        vm.s.resultLines.joinToString(" ").take(70)); break
                }
                if (vm.s.pendingPromotion) vm.resolvePromotion(true)
                vm.nextCase()
            }
            minRankReached = minOf(minRankReached, vm.s.rankIndex)
        }
        val topGrade = if (r.minMarginTopGrade == Int.MAX_VALUE) "n/a" else "${r.minMarginTopGrade}h"

        println("=============== FULL-LADDER PLAYTHROUGH (x$runs, $casesEach cases each) ===============")
        println("lowest rank any run climbed to: $minRankReached (of 14)")
        println("cases won: ${r.casesWon}   losses: ${r.failures.size}   wrong flights: ${r.wrongFlights}")
        println("min spare hours at a win: ${r.minMargin}h overall   |   $topGrade at the top grade (12+)")
        if (r.failures.isNotEmpty()) { println("---- problems ----"); r.failures.take(20).forEach { println("  - $it") } }
        println("=================================================================================")

        // The whole game must be beatable: no paid case ever lost or dead-ended, and progression
        // actually carries you deep into the International grades.
        assertTrue("no paid case may be lost or dead-end (${r.failures.size} failed)", r.failures.isEmpty())
        assertTrue("progression must climb deep into the International grades (min rank $minRankReached)",
            minRankReached >= 12)
    }
}
