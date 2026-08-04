package com.acme.clara

import com.acme.clara.data.CityMeta
import com.acme.clara.data.Expansion
import com.acme.clara.data.Expansion2
import com.acme.clara.data.GameData
import com.acme.clara.data.Progression
import com.acme.clara.data.Suspect
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The level rules (docs/05-game-design-and-progression.md): the 133-country data, the recognition
 * waves (wave = difficulty tier), the per-rank difficulty tables, the game-theory fairness of the
 * curve, and — driving the real ViewModel — the wave-gated reveal, dynamic deadline, new-per-case
 * cap and the endless International track.
 */
class ProgressionTest {

    private val base = GameData.cities.toSet()
    private val paid = (Expansion.names + Expansion2.names)

    // ---------- data integrity: the 133 new countries ----------

    @Test fun expansion2LoadsOneThirtyThreeCleanCities() {
        assertEquals(133, Expansion2.cities.size)
        assertEquals("names unique", 133, Expansion2.names.toSet().size)
        Expansion2.cities.forEach { c ->
            assertFalse("${c.name} overlaps the free 30", c.name in base)
            assertFalse("${c.name} overlaps expansion 68", c.name in Expansion.names.toSet())
            assertTrue("${c.name} region", c.region.isNotBlank())
            assertTrue("${c.name} description", c.description.isNotBlank())
            assertTrue("${c.name} has clues", c.clues.isNotEmpty())
            assertNotNull("${c.name} has a postcard", c.drawable)
            assertTrue("${c.name} postcard name", c.drawable!!.startsWith("country_") || c.drawable!!.startsWith("city_"))
        }
        // resolves through the shared lookup the game uses
        assertEquals("Algeria", CityMeta.of("Algeria").name)
        assertTrue("resolved city carries its clues", CityMeta.of("Nigeria").clues.isNotEmpty())
    }

    // ---------- the recognition-wave map (wave = tier) ----------

    @Test fun everyPaidCityHasOneWaveAndFreeCitiesHaveNone() {
        assertEquals("201 paid destinations", 201, paid.toSet().size)
        for (c in paid) {
            val w = Progression.wave[c]
            assertNotNull("$c has no wave", w)
            assertTrue("$c wave in 0..9", w!! in 0..9)
        }
        for (c in GameData.cities) assertFalse("free city $c must not be waved", Progression.wave.containsKey(c))
        assertEquals("the wave map covers exactly the paid cities", paid.toSet(), Progression.wave.keys)
    }

    @Test fun wavesUnlockCumulativelyFamousFirst() {
        assertTrue("nothing before the International track", Progression.citiesUpToWave(-1).isEmpty())
        var prev = 0
        for (w in 0..9) {
            val n = Progression.citiesUpToWave(w).size
            assertTrue("wave $w never removes cities", n >= prev)
            prev = n
        }
        assertEquals("all 201 by the last wave", 201, Progression.citiesUpToWave(9).size)
        assertTrue("Amsterdam is a first-wave marquee city", "Amsterdam" in Progression.citiesUpToWave(0))
        assertFalse("obscure islands wait for the last waves", "Kiribati" in Progression.citiesUpToWave(0))
    }

    // ---------- per-rank difficulty tables ----------

    @Test fun difficultyTablesRampAndClamp() {
        assertEquals(5, Progression.hops(0))
        assertEquals(9, Progression.hops(4))
        assertTrue("International routes are longer", Progression.hops(Progression.LAST_RANK) >= Progression.hops(5))
        // hops never decrease as you climb
        for (r in 1..Progression.LAST_RANK) assertTrue("hops non-decreasing at $r", Progression.hops(r) >= Progression.hops(r - 1))
        // new-per-case is capped at 3 so the top stays fair
        for (r in 0..Progression.LAST_RANK) assertTrue("new/case <= 3 at $r", Progression.newPerCase(r) in 1..3)
        assertTrue("slack always positive", (0..Progression.LAST_RANK).all { Progression.slackHours(it) > 0 })
        // out-of-range ranks clamp instead of crashing
        assertEquals(Progression.hops(0), Progression.hops(-5))
        assertEquals(Progression.hops(Progression.LAST_RANK), Progression.hops(999))
    }

    @Test fun wavesGateByRankIndex() {
        for (r in 0 until Progression.FREE_RANKS) assertEquals("free rank $r unlocks nothing", -1, Progression.unlockedMaxWave(r))
        assertEquals("first International grade opens wave 0", 0, Progression.unlockedMaxWave(Progression.FREE_RANKS))
        assertEquals("top grade opens the last wave", 9, Progression.unlockedMaxWave(Progression.LAST_RANK))
        assertEquals(9, Progression.unlockedMaxWave(999))
    }

    @Test fun deadlineCoversTravelAndGrowsWithIt() {
        val short = Progression.caseDeadlineHours(4, 40, 9)
        val long = Progression.caseDeadlineHours(4, 90, 9)
        assertTrue("more travel -> later deadline", long > short)
        assertTrue("deadline always covers the raw flight hours", short >= 40)
        assertTrue("deadline adds this rank's slack", short > 40)
    }

    /** The game-theory check from the design doc encoded as a test: expected wrong-guesses vs the
     *  wrong-guess budget the slack buys — the margin must stay positive and never grow (never easier). */
    @Test fun difficultyCurveIsMonotonicAndAlwaysWinnable() {
        var prevMargin = Double.MAX_VALUE
        for (rank in 0..Progression.LAST_RANK) {
            val hops = Progression.hops(rank)
            val nc = Progression.newPerCase(rank)
            val expectedWrong = 0.05 * (hops - nc) + 0.30 * nc
            val budget = Progression.slackHours(rank) / 8.0
            val margin = budget - expectedWrong
            assertTrue("rank $rank must stay winnable (margin=$margin)", margin > 0.0)
            assertTrue("rank $rank never easier than the last (margin=$margin, prev=$prevMargin)",
                margin <= prevMargin + 1e-9)
            prevMargin = margin
        }
    }

    // ---------- the deadline is dynamic in a real game ----------

    @Test fun caseDeadlineIsDynamicNotTheLegacyConstant() {
        val vm = ClaraViewModel().apply { signOn("Clock") }
        val seen = mutableSetOf<Int>()
        repeat(12) { seen += vm.s.caseDeadlineHours; vm.menuNewCase() }
        assertTrue("a deadline is set", vm.s.caseDeadlineHours > 0)
        assertTrue("deadlines vary per case rather than the fixed 152", seen.any { it != 152 })
        // Deadlines now budget flights + investigation + slack, so they're larger than the old
        // flights-only value but must still sit in a sane, playable range for a free-rank case.
        assertTrue("free-rank deadlines stay in a sane range", seen.all { it in 60..240 })
    }

    // ---------- driving the real ViewModel through the whole ladder ----------

    private fun ClaraViewModel.issueWarrantFor(su: Suspect) {
        setComp("sex", su.tSex); setComp("hobby", su.tHobby); setComp("hair", su.tHair)
        setComp("feature", su.tFeature); setComp("vehicle", su.tVehicle); compute()
    }
    private fun ClaraViewModel.solveCurrentCase() {
        issueWarrantFor(s.culprit!!)
        var guard = 0
        while (s.progress < s.route.size - 1 && guard++ < 16) { travelTo(s.route[s.progress + 1]); arrive() }
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
        if (s.phase == Phase.CHASE) chaseDone()
    }

    /** Solve the way a REAL player must: investigate ~3 witnesses at every city (each costs clock)
     *  to find the trail, not just fly the known route. The deadline has to budget for this time —
     *  the flights-only [solveCurrentCase] never exercised it, which hid the too-tight deadline. */
    private fun ClaraViewModel.solveCurrentCaseThoroughly() {
        issueWarrantFor(s.culprit!!)
        var guard = 0
        while (s.progress < s.route.size - 1 && guard++ < 24) {
            for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
            if (s.phase == Phase.CHASE) break
            travelTo(s.route[s.progress + 1]); arrive()
        }
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
        if (s.phase == Phase.CHASE) chaseDone()
    }

    /** A thorough run that also fumbles a SINGLE wrong flight out of the opening city before
     *  correcting course — the very first case must still forgive one honest mistake. */
    private fun ClaraViewModel.solveOpeningCaseWithOneWrongFlight() {
        issueWarrantFor(s.culprit!!)
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }   // investigate start
        val correct = s.route[s.progress + 1]
        val decoy = GameData.cities.firstOrNull { it !in s.route && it != s.currentCity }
        if (decoy != null) { travelTo(decoy); arrive(); openVenue(0) }        // wrong hop + a wasted look
        travelTo(correct); arrive()                                          // back on the trail
        var guard = 0
        while (s.progress < s.route.size - 1 && guard++ < 24) {
            for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
            if (s.phase == Phase.CHASE) break
            travelTo(s.route[s.progress + 1]); arrive()
        }
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
        if (s.phase == Phase.CHASE) chaseDone()
    }

    @Test fun thoroughInvestigationBeatsTheClockOnTheOpeningCase() {
        // 30 independent first cases, each solved with a full clue-hunt at every city.
        // A careful, no-mistakes rookie must never run out of time on case one.
        repeat(30) {
            val vm = ClaraViewModel().apply { signOn("Sleuth") }
            vm.solveCurrentCaseThoroughly()
            assertTrue("thorough clean play beat the clock on the opening case", vm.s.won)
        }
    }

    @Test fun thoroughInvestigationBeatsTheClockDeepIntoTheLadder() {
        val vm = ClaraViewModel().apply { signOn("Marathon"); unlockExpansion() }
        repeat(20) {
            vm.solveCurrentCaseThoroughly()
            assertTrue("thorough play beat the clock at rank ${vm.s.rankIndex}", vm.s.won)
            if (vm.s.pendingPromotion) vm.resolvePromotion(true)
            vm.nextCase()
        }
    }

    @Test fun theOpeningCaseForgivesOneWrongFlight() {
        var survived = 0
        val trials = 40
        repeat(trials) {
            val vm = ClaraViewModel().apply { signOn("Rookie") }
            vm.solveOpeningCaseWithOneWrongFlight()
            if (vm.s.won) survived++
        }
        assertTrue("the opening case survived only $survived/$trials one-mistake runs",
            survived >= trials - 2)
    }
    /** Solve, ace any promotion quiz, advance — returns false once the (free) career is over. */
    private fun ClaraViewModel.advanceOneCase(): Boolean {
        solveCurrentCase()
        if (s.pendingPromotion) resolvePromotion(true)
        if (s.careerOver) return false
        nextCase(); return true
    }

    @Test fun everyCaseAlongTheLadderIsWinnableInTime() {
        val vm = ClaraViewModel().apply { signOn("Marathon"); unlockExpansion() }
        // free arc + deep into the International grades — a clean run must always beat the clock
        repeat(40) {
            vm.solveCurrentCase()
            assertTrue("case ${vm.s.casesSolved + 1} (rank ${vm.s.rankIndex}) beat the deadline", vm.s.won)
            if (vm.s.pendingPromotion) vm.resolvePromotion(true)
            vm.nextCase()
        }
    }

    @Test fun paidCareerContinuesPastClaraIntoInternationalWaves() {
        val vm = ClaraViewModel().apply { signOn("Interpol"); unlockExpansion() }
        var guard = 0
        while (vm.s.rankIndex < Progression.FREE_RANKS && guard++ < 30) {
            assertFalse("catching Clara promotes a paid agent, never retires them", vm.s.careerOver)
            check(vm.advanceOneCase()) { "paid career should not end" }
        }
        assertTrue("reached an International grade", vm.s.rankIndex >= Progression.FREE_RANKS)
        assertTrue("played past the 14-case free arc", vm.s.casesSolved >= GameState_CAREER_CASES())
        // once International, expansion (wave) cities enter the routes
        val seen = mutableSetOf<String>()
        repeat(25) { seen += vm.s.route; vm.advanceOneCase() }
        assertTrue("International cases route through unlocked wave countries",
            seen.any { it in Progression.wave.keys })
    }

    @Test fun freeCareerNeverLeavesTheOriginalsAndRetiresAtClara() {
        val vm = ClaraViewModel().apply { signOn("Purist") }   // no unlock
        var guard = 0
        val seen = mutableSetOf<String>()
        while (!vm.s.careerOver && guard++ < 30) { seen += vm.s.route; if (!vm.advanceOneCase()) break }
        assertTrue("the free career ends (Clara jailed)", vm.s.careerOver)
        assertTrue("only original cities ever appear for a free player", seen.all { it in base })
        assertEquals("free career tops out at Ace Detective", "Ace Detective", GameData.ranks[vm.s.rankIndex])
    }

    @Test fun newCountriesPerCaseAreCappedInSteadyState() {
        val vm = ClaraViewModel().apply { signOn("Learner"); unlockExpansion() }
        // play well into the International grades so a large "seen" pool exists to reuse from
        var guard = 0
        while (vm.s.casesSolved < 25 && guard++ < 60) vm.advanceOneCase()
        repeat(12) {
            vm.solveCurrentCase()
            if (vm.s.pendingPromotion) vm.resolvePromotion(true)
            // cityLastSeen right before the next case is exactly what the route-picker's cap consults
            val seenBefore = vm.s.cityLastSeen.keys.toSet()
            vm.nextCase()
            val cap = Progression.newPerCase(vm.s.rankIndex)
            val fresh = vm.s.route.count { it !in seenBefore }
            assertTrue("case at rank ${vm.s.rankIndex} introduces <= $cap new (saw $fresh)", fresh <= cap)
        }
    }

    private fun GameState_CAREER_CASES() = 14
}
