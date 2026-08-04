package com.acme.clara

import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ClueKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Rookie tour is now a set of contextual, teach-once lessons driven by real game state (not a
 * linear step counter). These tests drive the ViewModel through the taught actions and check that
 * each lesson is armed / marked seen at the right moment.
 */
class TutorialTest {

    @Test fun aBrandNewCareerArmsTheGuidedFirstCase() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }
        assertTrue("the tour is running", vm.s.tutorialActive)
        assertTrue("marked done so a retry won't re-arm it", vm.s.tutorialDone)
        assertTrue("nothing taught yet", vm.s.tutorialSeen.isEmpty())
        assertFalse(vm.s.sawTraitClue); assertFalse(vm.s.sawTrailClue)
    }

    @Test fun lessonsAreTaughtAsTheActionsHappen() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }
        vm.beginInvestigation()   // -> CITY

        // a trail witness arms the follow-the-trail lesson; a trait witness arms the computer lesson
        val destIdx = vm.s.venues.indexOfFirst { it.kind == ClueKind.DESTINATION }
        vm.openVenue(destIdx); assertTrue("a trail hint arms the fly lesson", vm.s.sawTrailClue); vm.closeClue()
        val traitIdx = vm.s.venues.indexOfFirst { it.kind == ClueKind.TRAIT }
        vm.openVenue(traitIdx); assertTrue(vm.s.sawTraitClue); vm.closeClue()

        // "interview" is taught only once all three venues have been tried
        assertTrue("not taught before all three venues", "interview" !in vm.s.tutorialSeen)
        for (i in 0..2) if (i !in vm.s.visited) { vm.openVenue(i); vm.closeClue() }
        assertTrue("interview" in vm.s.tutorialSeen)

        // tapping the plane teaches "trail"
        vm.gotoTravel()
        assertTrue("trail" in vm.s.tutorialSeen)

        // running the computer with the trait a witness actually gave teaches "computer"
        val heard = vm.s.revealedTraits.first()
        vm.gotoCrime()
        vm.setComp(heard.first, heard.second)
        vm.compute()
        assertTrue("computer" in vm.s.tutorialSeen)
    }

    @Test fun skipEndsTheTour() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }
        vm.skipTutorial()
        assertFalse("no longer running", vm.s.tutorialActive)
        assertTrue(vm.s.tutorialDone)
    }

    @Test fun aSecondCaseIsNotGuided() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }   // tutorialDone becomes true
        vm.menuNewCase()                                       // still casesSolved 0, but already seen
        assertFalse(vm.s.tutorialActive)
    }
}
