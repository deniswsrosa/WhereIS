package com.acme.clara

import com.acme.clara.data.GameData
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Phase
import com.acme.clara.game.SoundCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** End-of-case outcomes: correct arrest (win), false arrest, no-warrant capture, and the
 *  promotion cadence — the corner cases of the confrontation. */
class EndingsTest {

    private fun fresh() = ClaraViewModel().apply { signOn("Tester") }

    private fun ClaraViewModel.warrantFor(s: com.acme.clara.data.Suspect) {
        setComp("sex", s.tSex); setComp("hobby", s.tHobby); setComp("hair", s.tHair)
        setComp("feature", s.tFeature); setComp("vehicle", s.tVehicle); compute()
    }

    private fun ClaraViewModel.flyToHideout() {
        var guard = 0
        while (s.progress < s.route.size - 1 && guard++ < 12) { travelTo(s.route[s.progress + 1]); arrive() }
    }

    private fun ClaraViewModel.confrontAtHideout() {
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
        chaseDone()
    }

    @Test fun correctWarrantWinsTheCase() {
        repeat(20) {
            val vm = fresh()
            val c = vm.s.culprit!!
            vm.warrantFor(c)
            assertEquals(c.name, vm.s.warrantFor?.name)
            vm.flyToHideout()
            vm.confrontAtHideout()
            assertEquals(Phase.RESULT, vm.s.phase)
            assertTrue("correct arrest should win", vm.s.won)
            assertEquals(SoundCue.WIN, vm.soundCue?.second)
            assertTrue("report names the culprit",
                vm.s.resultLines.any { it.contains(c.name) })
            assertEquals("a solved case counts", 1, vm.s.casesSolved)
        }
    }

    @Test fun noWarrantCaptureIsALoss() {
        repeat(20) {
            val vm = fresh()
            vm.flyToHideout()                       // never ran the crime computer
            vm.confrontAtHideout()
            assertEquals(Phase.RESULT, vm.s.phase)
            assertFalse("no warrant cannot arrest", vm.s.won)
            assertEquals(SoundCue.WRONG_ARREST, vm.soundCue?.second)
            assertTrue(vm.s.resultLines.any { it.contains("without a warrant") })
            assertEquals("a lost case does not count", 0, vm.s.casesSolved)
        }
    }

    @Test fun wrongWarrantIsAFalseArrestLoss() {
        repeat(20) {
            val vm = fresh()
            val c = vm.s.culprit!!
            val wrong = GameData.suspects.first { it.name != c.name && it.name != "Clara San Diego" }
            vm.warrantFor(wrong)
            // only meaningful when the decoy is uniquely identified and differs from the culprit
            if (vm.s.warrantFor?.name != wrong.name || wrong.name == c.name) return@repeat
            vm.flyToHideout()
            vm.confrontAtHideout()
            assertFalse(vm.s.won)
            assertEquals(SoundCue.WRONG_ARREST, vm.soundCue?.second)
            assertTrue(vm.s.resultLines.any { it.contains("false arrest") })
        }
    }

    @Test fun firstSolvedCaseEarnsAPromotion() {
        val vm = fresh()
        assertEquals("next promotion is the very first solve", 1, vm.casesToNextPromotion())
        vm.warrantFor(vm.s.culprit!!)
        vm.flyToHideout()
        vm.confrontAtHideout()
        assertTrue(vm.s.won)
        assertTrue("case 1 triggers a promotion", vm.s.pendingPromotion)
        // after solving 1, the next threshold is 5 -> four more to go
        assertEquals(4, vm.casesToNextPromotion())
    }

    @Test fun resolvingThePromotionQuizBumpsRank() {
        val vm = fresh()
        vm.warrantFor(vm.s.culprit!!)
        vm.flyToHideout()
        vm.confrontAtHideout()
        val before = vm.s.rankIndex
        vm.resolvePromotion(true)
        assertEquals("correct quiz answer promotes", before + 1, vm.s.rankIndex)
        assertFalse(vm.s.pendingPromotion)
    }
}
