package com.acme.clara

import com.acme.clara.data.GameData
import com.acme.clara.data.Suspect
import com.acme.clara.game.CaseJournal
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ClueKind
import com.acme.clara.game.Phase
import com.acme.clara.save.InMemorySaveRepository
import com.acme.clara.save.LaunchOutcome
import com.acme.clara.save.SaveMeta
import com.acme.clara.save.decideLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end playthroughs: a whole case, a whole 14-case career to the Hall of Fame, and a
 * close-and-reopen mid-case that continues to a win — driving the real ViewModel the way the
 * UI does, not just individual functions.
 */
class PlaythroughTest {

    private fun fresh() = ClaraViewModel().apply { signOn("Sleuth") }

    private fun ClaraViewModel.issueWarrantFor(su: Suspect) {
        setComp("sex", su.tSex); setComp("hobby", su.tHobby); setComp("hair", su.tHair)
        setComp("feature", su.tFeature); setComp("vehicle", su.tVehicle); compute()
    }

    private fun ClaraViewModel.flyTheRouteToHideout() {
        var guard = 0
        while (s.progress < s.route.size - 1 && guard++ < 12) { travelTo(s.route[s.progress + 1]); arrive() }
    }

    private fun ClaraViewModel.arrestAtHideout() {
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
        if (s.phase == Phase.CHASE) chaseDone()
    }

    private fun ClaraViewModel.solveCurrentCase() {
        issueWarrantFor(s.culprit!!); flyTheRouteToHideout(); arrestAtHideout()
    }

    // ---------- one full case, tracking the journal across cities ----------

    @Test fun journalAccumulatesLeadsAcrossCities() {
        val vm = fresh()
        val firstCity = vm.s.currentCity
        vm.openVenue(vm.s.venues.indexOfFirst { it.kind == ClueKind.DESTINATION })

        vm.travelTo(vm.s.route[1]); vm.arrive()          // fly the correct first leg
        assertTrue(vm.s.onTrack)
        vm.openVenue(vm.s.venues.indexOfFirst { it.kind == ClueKind.DESTINATION })

        val leads = CaseJournal.leads(vm.s)
        assertTrue("a lead logged at each city", leads.size >= 2)
        assertEquals("first lead is tagged to the first city", firstCity, leads.first().city)
    }

    // ---------- the whole career, sign-on to the finale ----------

    @Test fun fullCareerReachesTheFinale() {
        val vm = fresh()
        var guard = 0
        var lastCulprit = ""
        while (!vm.s.careerOver && guard++ < 30) {
            lastCulprit = vm.s.culprit!!.name
            vm.solveCurrentCase()
            assertTrue("case ${vm.s.casesSolved + 1} should be solvable in time", vm.s.won)
            if (vm.s.careerOver) break
            if (vm.s.pendingPromotion) vm.resolvePromotion(true)   // ace the quiz each time
            vm.nextCase()
        }

        assertTrue("the career ends by jailing the ring-leader", vm.s.careerOver)
        assertEquals("the finale culprit is Clara", "Clara San Diego", lastCulprit)
        assertEquals("14 cases make a career", 14, vm.s.casesSolved)
        assertEquals("the free career tops out at Ace Detective",
            "Ace Detective", GameData.ranks[vm.s.rankIndex])
        assertFalse("no promotion after the finale", vm.s.pendingPromotion)
        assertTrue("the finale report celebrates jailing the ring-leader",
            vm.s.resultLines.any { it.contains("Clara San Diego is behind bars") })
        assertTrue("ring-leader captured", "Clara San Diego" in vm.s.capturedVillains)
        assertTrue("the finale unlocks the Kingpin commendation",
            "kingpin" in vm.s.unlockedAchievements)
    }

    // ---------- close mid-case, reopen, finish ----------

    @Test fun closingMidCaseAndReopeningContinuesToAWin() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Nomad") }
        vm.attachSave(repo, "p1") { 100L }

        // play partway: gather a lead and enter one computer trait, then "leave"
        vm.openVenue(vm.s.venues.indexOfFirst { it.kind != ClueKind.DANGER })
        vm.setComp("hair", vm.s.culprit!!.tHair)
        val culprit = vm.s.culprit!!
        val route = vm.s.route

        // cold start of a new ViewModel + the same store — the launch flow continues p1
        val outcome = decideLaunch(repo.list())
        assertTrue(outcome is LaunchOutcome.Continue)
        val reopened = ClaraViewModel()
        reopened.bindRepository(repo) { 200L }
        reopened.resumeById((outcome as LaunchOutcome.Continue).id)

        assertEquals("same case resumed", route, reopened.s.route)
        assertEquals(culprit.name, reopened.s.culprit?.name)
        assertTrue("the mid-case computer entry survived", reopened.s.compHair != null)

        // finish it in the reopened session
        reopened.solveCurrentCase()
        assertEquals(Phase.RESULT, reopened.s.phase)
        assertTrue("the resumed case can be won", reopened.s.won)
        assertEquals(1, reopened.s.casesSolved)
    }

    // ---------- launch flow across multiple careers ----------

    @Test fun launchFlowPicksAmongTwoSavedCareers() {
        val repo = InMemorySaveRepository()
        ClaraViewModel().apply { attachSave(repo, "alpha") { 10L }; signOn("Alpha") }
        ClaraViewModel().apply { attachSave(repo, "beta") { 99L }; signOn("Beta") }

        val outcome = decideLaunch(repo.list())
        assertTrue(outcome is LaunchOutcome.Choose)
        val metas: List<SaveMeta> = (outcome as LaunchOutcome.Choose).metas
        assertEquals("newest career first", "beta", metas.first().id)
        assertEquals(2, metas.size)
    }
}
