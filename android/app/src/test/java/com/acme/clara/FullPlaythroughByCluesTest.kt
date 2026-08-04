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
        info.currency?.let { if (clue.contains(it)) return true }
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
        var minMargin = Int.MAX_VALUE; var minMarginTopFree = Int.MAX_VALUE
        val failures = mutableListOf<String>()
    }

    private fun absorbTrait(vm: ClaraViewModel) {
        val c = vm.s.openClue ?: return
        if (c.kind == ClueKind.TRAIT) c.trait?.let { (cat, value) -> vm.setComp(cat, value); vm.compute() }
        vm.closeClue()
    }

    private fun playCase(vm: ClaraViewModel, r: Report): Boolean {
        if (vm.s.phase == Phase.BRIEFING) vm.beginInvestigation()
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
                val pick = identify(lastTrail, opts) ?: run { r.forcedGuesses++; opts.random() }
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
                val haveWarrant = vm.s.warrantFor != null
                if (id && (haveWarrant || gotTrait)) break   // efficient: stop once we know enough
            }
            r.cityStops++
            lastTrail = trail
            if (vm.s.deadlinePassed) break

            val pick = identify(trail, opts) ?: run { r.forcedGuesses++; opts.random() }
            vm.travelTo(pick); vm.arrive()
            if (!vm.s.onTrack && !vm.s.atHideout) r.wrongFlights++
        }
        return vm.s.won
    }

    @Test fun theFreeCareerIsBeatableByAnEfficientPlayer() {
        val r = Report()
        val careers = 40
        repeat(careers) { n ->
            val vm = ClaraViewModel().apply { signOn("Gumshoe$n") }
            var guard = 0
            while (!vm.s.careerOver && guard++ < 30) {
                val rank = vm.s.rankIndex
                if (playCase(vm, r)) {
                    r.casesWon++
                    val margin = vm.s.caseDeadlineHours - vm.s.clock
                    r.minMargin = minOf(r.minMargin, margin)
                    if (rank == 4) r.minMarginTopFree = minOf(r.minMarginTopFree, margin)
                } else { r.failures.add("career $n LOST case ${vm.s.casesSolved + 1} (rank $rank): " +
                    vm.s.resultLines.joinToString(" ").take(70)); break }
                if (vm.s.pendingPromotion) vm.resolvePromotion(true)
                if (vm.s.careerOver) break
                vm.nextCase()
            }
            if (vm.s.careerOver) r.careersFinished++
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
}
