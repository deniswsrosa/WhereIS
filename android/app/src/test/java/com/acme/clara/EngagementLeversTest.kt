package com.acme.clara

import com.acme.clara.data.Suspect
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Overlay
import com.acme.clara.game.Phase
import com.acme.clara.save.InMemorySaveRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The playbook levers with real logic behind them: H4 daily streak, R2 near-miss ending,
 *  H3 welcome-back warm-up, and the P5 recap card drawn on return. */
class EngagementLeversTest {

    private val DAY = 86_400_000L

    private fun fresh() = ClaraViewModel().apply { signOn("Tester") }

    private fun ClaraViewModel.setWarrant(su: Suspect) {
        setComp("sex", su.tSex); setComp("hobby", su.tHobby); setComp("hair", su.tHair)
        setComp("feature", su.tFeature); setComp("vehicle", su.tVehicle); compute()
    }

    private fun ClaraViewModel.flyToHideout() {
        var guard = 0
        while (s.progress < s.route.size - 1 && guard++ < 12) { travelTo(s.route[s.progress + 1]); arrive() }
    }

    private fun ClaraViewModel.confrontAtHideout() {
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
        chaseDone()
    }

    /** Solve the current case end-to-end and clear any promotion so the next case can start. */
    private fun ClaraViewModel.solveCase() {
        setWarrant(s.culprit!!)
        flyToHideout()
        confrontAtHideout()
        assertTrue("case should be won", s.won)
        if (s.pendingPromotion) resolvePromotion(true)
    }

    // ---------- H4 case-a-day streak ----------

    @Test fun streakGrowsOnConsecutiveDaysAndFreezeCoversAGap() {
        val repo = InMemorySaveRepository()
        var now = 20_000L * DAY                       // some real-world day, well clear of 0
        val vm = fresh()
        vm.attachSave(repo, "p") { now }

        vm.solveCase()
        assertEquals("first solve opens the streak", 1, vm.s.streakDays)

        // another solve the same day doesn't inflate the streak
        vm.nextCase(); vm.solveCase()
        assertEquals("same-day solve keeps the streak", 1, vm.s.streakDays)

        // six more consecutive days -> a 7-day streak, which earns a weekly freeze
        for (d in 1..6) { now += DAY; vm.nextCase(); vm.solveCase() }
        assertEquals("seven consecutive days", 7, vm.s.streakDays)
        assertEquals("a weekly streak-freeze is earned at 7", 1, vm.s.streakFreezes)

        // miss a day (gap of 2) — the freeze absorbs it, streak survives
        now += 2 * DAY; vm.nextCase(); vm.solveCase()
        assertEquals("freeze carries the streak through one missed day", 8, vm.s.streakDays)
        assertEquals("the freeze was spent", 0, vm.s.streakFreezes)

        // miss another day with no freeze left -> the streak resets
        now += 2 * DAY; vm.nextCase(); vm.solveCase()
        assertEquals("an uncovered gap resets the streak", 1, vm.s.streakDays)
    }

    // ---------- R2 near-miss ending ----------

    @Test fun outOfTimeAtTheHideoutIsAWarmNearMiss() {
        val vm = fresh()
        vm.flyToHideout()
        assertTrue("should be at the hideout on the right trail", vm.s.atHideout)
        // burn the clock down at the hideout (COMPUTE costs 3h each) until time runs out
        var guard = 0
        while (vm.s.phase != Phase.RESULT && guard++ < 200) vm.compute()
        assertEquals(Phase.RESULT, vm.s.phase)
        assertFalse("running out of time is a loss", vm.s.won)
        assertTrue("a close loss reads as a near-miss",
            vm.s.resultLines.any { it.contains("So close") })
    }

    @Test fun outOfTimeFarFromTheHideoutIsAPlainTimeout() {
        val vm = fresh()
        // never leave the start city: computing there trips the deadline off-trail
        var guard = 0
        while (vm.s.phase != Phase.RESULT && guard++ < 200) vm.compute()
        assertEquals(Phase.RESULT, vm.s.phase)
        assertFalse(vm.s.won)
        assertTrue("a distant loss stays the plain timeout",
            vm.s.resultLines.any { it.contains("ran out of time") })
        assertFalse("and is not dressed up as a near-miss",
            vm.s.resultLines.any { it.contains("So close") })
    }

    // ---------- H3 welcome-back warm-up ----------

    @Test fun returningAfterAGapMakesTheNextCaseKinder() {
        val vm = fresh()
        val snap = vm.snapshot("p", 1_000L)           // last played long ago
        val vm2 = ClaraViewModel()
        var now = 1_000L + 5 * DAY                     // return five days later
        vm2.bindRepository(InMemorySaveRepository()) { now }
        vm2.resume(snap)
        assertTrue("a gap queues a warm-up", vm2.s.warmUpNextCase)

        vm2.nextCase()
        assertEquals("the warm-up trims one hop off a rookie route", 4, vm2.s.route.size)
        assertEquals("and pre-solves the first trait", 1, vm2.s.revealedCount)
        assertFalse("the warm-up flag is consumed", vm2.s.warmUpNextCase)
    }

    // ---------- P5 recap card ----------

    @Test fun resumingMidCaseDrawsThePreviouslyRecap() {
        val vm = fresh()
        vm.travelTo(vm.s.route[1]); vm.arrive()        // get a hop into the case
        assertTrue("we're underway", vm.s.progress >= 1)
        val snap = vm.snapshot("p", 1_000L)

        val vm2 = ClaraViewModel()
        var now = 1_000L + 5 * DAY
        vm2.bindRepository(InMemorySaveRepository()) { now }
        vm2.resume(snap)

        val ov = vm2.s.overlay
        assertTrue("a recap card is shown on return", ov is Overlay.Info)
        ov as Overlay.Info
        assertEquals("PREVIOUSLY ON THIS CASE", ov.title)
        assertTrue("it names the trail", ov.lines.any { it.startsWith("Trail:") })
    }
}
