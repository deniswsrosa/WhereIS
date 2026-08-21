package com.acme.clara

import com.acme.clara.data.GameData
import com.acme.clara.data.Masterminds
import com.acme.clara.data.Progression
import com.acme.clara.data.Suspect
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Phase
import com.acme.clara.save.InMemorySaveRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MastermindsTest {
    private fun ClaraViewModel.issueWarrantFor(su: Suspect) {
        setComp("sex", su.tSex); setComp("hobby", su.tHobby); setComp("hair", su.tHair)
        setComp("feature", su.tFeature); setComp("vehicle", su.tVehicle); compute()
    }

    private fun ClaraViewModel.solveCurrentCase() {
        issueWarrantFor(s.culprit!!)
        var guard = 0
        while (s.progress < s.route.size - 1 && guard++ < 20) { travelTo(s.route[s.progress + 1]); arrive() }
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
        if (s.phase == Phase.CHASE) chaseDone()
    }

    private fun ClaraViewModel.advance() {
        solveCurrentCase()
        if (s.pendingPromotion) resolvePromotion(true)
        nextCase()
    }

    @Test fun campaignHasTenExplicitFinalesAtTheAuthoredCadence() {
        assertEquals(listOf(8, 8, 8, 8, 10, 10, 10, 12, 12, 12), Masterminds.waveCases)
        assertEquals(listOf(8, 16, 24, 32, 42, 52, 62, 74, 86, 98), Masterminds.waveEndCases)
        assertEquals(10, Masterminds.arcs.size)
        assertEquals(5, Masterminds.arcs.count { it.role == "Boss" })
        assertEquals(5, Masterminds.arcs.count { it.role == "Successor" })
        assertEquals(5, Masterminds.arcs.count { it.claraFlavor })
        assertEquals("Case 14 plus Waves 6-10", 6, 1 + Masterminds.arcs.count { it.claraFlavor })
        assertTrue(Masterminds.arcs.last().final)
        assertEquals(10, Masterminds.arcs.map { it.suspectName }.toSet().size)
        assertTrue(Masterminds.arcs.all { arc -> GameData.suspects.any { it.name == arc.suspectName } })
    }

    @Test fun purchaseOpensWaveOneImmediatelyAtAnyFreeRank() {
        assertEquals(-1, Masterminds.unlockedMaxWave(0, false))
        assertEquals(0, Masterminds.unlockedMaxWave(0, true))
        assertEquals(0, Masterminds.unlockedMaxWave(4, true))
        assertEquals(1, Masterminds.unlockedMaxWave(5, true))
        assertEquals(9, Masterminds.unlockedMaxWave(14, true))
    }

    @Test fun waveOneFinaleForcesBossAndAwardsSpecialAgent() {
        val vm = ClaraViewModel().apply { signOn("Agent"); unlockExpansion() }
        repeat(21) { vm.advance() } // loaded absolute Case 22: paid campaign case 8
        val arc = Masterminds.arcForWave(0)!!
        assertEquals(arc.suspectName, vm.s.culprit?.name)
        assertTrue(vm.s.route.all { Progression.wave[it] == 0 })

        vm.solveCurrentCase()
        assertTrue(vm.s.won)
        assertFalse(vm.s.careerOver)
        assertTrue(vm.s.pendingPromotion)
        vm.resolvePromotion(true)
        assertEquals(arc.patentRank, vm.s.rankIndex)
        assertEquals("Special Agent", GameData.ranks[vm.s.rankIndex])
        assertTrue(arc.suspectName in vm.s.capturedVillains)
    }

    @Test fun case14IsAlwaysAnEscapeAndNeverAnEnrollmentPromotion() {
        val vm = ClaraViewModel().apply { signOn("EarlyBuyer"); unlockExpansion() }
        repeat(13) { vm.advance() }
        assertEquals("Clara San Diego", vm.s.culprit?.name)
        vm.solveCurrentCase()
        assertTrue(vm.s.won)
        assertFalse(vm.s.careerOver)
        assertFalse(vm.s.pendingPromotion)
        assertEquals("Ace Detective", GameData.ranks[vm.s.rankIndex])
        assertFalse("Clara San Diego" in vm.s.capturedVillains)
    }

    @Test fun latePurchasePreservesDetectiveAndStartsWaveOneWithoutRetroactiveRank() {
        val vm = ClaraViewModel().apply { signOn("LateBuyer") }
        repeat(14) {
            vm.solveCurrentCase()
            if (vm.s.pendingPromotion) vm.resolvePromotion(true)
            if (it < 13) vm.nextCase()
        }
        assertEquals("Ace Detective", GameData.ranks[vm.s.rankIndex])
        vm.nextCase()
        assertEquals("the free career does not generate ordinary cases past Clara's escape",
            14, vm.s.casesSolved)
        assertEquals("with sales live the detective remains on the result while the offer opens",
            Phase.RESULT, vm.s.phase)
        assertEquals(com.acme.clara.game.Overlay.PurchaseOffer("New case"), vm.s.overlay)
        vm.unlockExpansion()
        assertFalse(vm.s.pendingPromotion)
        assertEquals(0, Masterminds.unlockedMaxWave(vm.s.rankIndex, vm.s.expansionUnlocked))
        vm.nextCase()
        assertEquals(15, vm.s.casesSolved + 1)
    }

    @Test fun waveTenCapturesFinalSuccessorAndClaraAndAwardsChiefDirector() {
        val vm = ClaraViewModel().apply { signOn("Director"); unlockExpansion() }
        repeat(111) { vm.advance() } // loaded absolute Case 112: campaign case 98
        val arc = Masterminds.arcForWave(9)!!
        assertEquals(arc.suspectName, vm.s.culprit?.name)
        assertTrue(vm.s.route.all { Progression.wave[it] == 9 })
        vm.solveCurrentCase()
        assertTrue(vm.s.careerOver)
        assertTrue("Wave 10 still awards its patent through the promotion quiz", vm.s.pendingPromotion)
        assertEquals("Director", GameData.ranks[vm.s.rankIndex])
        vm.resolvePromotion(true)
        assertFalse(vm.s.pendingPromotion)
        assertEquals("Chief Director", GameData.ranks[vm.s.rankIndex])
        assertTrue(arc.suspectName in vm.s.capturedVillains)
        assertTrue("Clara San Diego" in vm.s.capturedVillains)
        assertTrue("kingpin" in vm.s.unlockedAchievements)
    }

    @Test fun mastermindsNeverAppearAsOrdinaryRandomCulprits() {
        val mastermindNames = Masterminds.arcs.map { it.suspectName }.toSet()
        val vm = ClaraViewModel().apply { signOn("Story Guard") }
        repeat(13) {
            assertFalse("Case ${vm.s.casesSolved + 1} dealt a future mastermind ${vm.s.culprit?.name}",
                vm.s.culprit?.name in mastermindNames)
            vm.advance()
        }
    }

    @Test fun newCaseCannotSkipAPendingPatentQuiz() {
        val vm = ClaraViewModel().apply { signOn("Quiz Guard"); unlockExpansion() }
        repeat(21) { vm.advance() }
        vm.solveCurrentCase()
        assertTrue(vm.s.pendingPromotion)
        val solved = vm.s.casesSolved
        val culprit = vm.s.culprit
        vm.menuNewCase()
        assertEquals("case count is unchanged", solved, vm.s.casesSolved)
        assertEquals("the finale result remains loaded", culprit, vm.s.culprit)
        assertTrue(vm.s.pendingPromotion)
        assertEquals(Phase.RESULT, vm.s.phase)
    }

    @Test fun salesDisabledCase14ExitDoesNotOverwriteTheFinishedCareer() {
        if (com.acme.clara.billing.BillingManager.SALES_ENABLED) return
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { bindRepository(repo); signOn("First detective") }
        repeat(14) { vm.advance() }
        assertEquals(Phase.TITLE, vm.s.phase)
        assertEquals(1, repo.list().size)

        vm.start()
        vm.signOn("Second detective")
        assertEquals("a fresh profile is created instead of reusing the Case 14 save", 2, repo.list().size)
        assertTrue(repo.list().any { it.name == "First detective" })
        assertTrue(repo.list().any { it.name == "Second detective" })
    }
}
