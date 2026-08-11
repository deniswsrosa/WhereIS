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

    @Test fun fullCareerReachesCase14WhereClaraEscapes() {
        // Case 14 is the story's inciting incident, not a clean capture: an unpaid player always
        // sees Clara get away there (see Masterminds.kt) — the free career keeps going after it
        // rather than ending, so this loop is bounded by case count, not by careerOver.
        val vm = fresh()
        var lastCulprit = ""
        // Captured right after the case-14 solve, before nextCase() wipes resultLines for case 15.
        var case14ResultLines = emptyList<String>()
        var case14CapturedVillains = emptySet<String>()
        repeat(14) { i ->
            lastCulprit = vm.s.culprit!!.name
            vm.solveCurrentCase()
            assertTrue("case ${vm.s.casesSolved} should be solvable in time", vm.s.won)
            assertFalse("an unpaid career never ends, not even at Clara", vm.s.careerOver)
            if (i == 13) { case14ResultLines = vm.s.resultLines; case14CapturedVillains = vm.s.capturedVillains }
            if (vm.s.pendingPromotion) vm.resolvePromotion(true)   // ace the quiz each time
            vm.nextCase()
        }

        assertEquals("the case-14 culprit is Clara", "Clara San Diego", lastCulprit)
        assertEquals("14 cases make the free career", 14, vm.s.casesSolved)
        assertEquals("the free career tops out at Ace Detective",
            "Ace Detective", GameData.ranks[vm.s.rankIndex])
        assertFalse("no promotion for an unpaid Case 14 escape", vm.s.pendingPromotion)
        assertTrue("the Case 14 report says she got away, not that she's jailed",
            case14ResultLines.any { it.contains("slipped away") })
        assertFalse("she's not in the Most Wanted gallery yet — she escaped, not was jailed",
            "Clara San Diego" in case14CapturedVillains)
        assertFalse("Case 14 alone doesn't unlock the Kingpin commendation — only the true finale does",
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
