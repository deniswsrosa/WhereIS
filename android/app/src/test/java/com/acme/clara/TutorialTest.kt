package com.acme.clara

import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ClueKind
import com.acme.clara.game.Overlay
import com.acme.clara.game.Tutorial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialTest {

    @Test fun everyStepHasAMessageAndTheEndDoesNot() {
        for (i in 0 until Tutorial.STEPS) assertNotNull(Tutorial.message(i))
        assertNull(Tutorial.message(Tutorial.STEPS))
        assertNull(Tutorial.message(-1))
    }

    @Test fun aBrandNewCareerStartsTheGuidedFirstCase() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }
        assertEquals("tutorial begins at step 0", 0, vm.s.tutorialStep)
        assertTrue("marked seen so a retry won't re-tutor", vm.s.tutorialDone)
    }

    @Test fun stepsAdvanceAsTheTaughtActionsHappen() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }
        assertEquals(0, vm.s.tutorialStep)
        vm.beginInvestigation();                       assertEquals(1, vm.s.tutorialStep)
        vm.openVenue(vm.s.venues.indexOfFirst { it.kind == ClueKind.DESTINATION }); assertEquals(2, vm.s.tutorialStep)
        vm.openOverlay(Overlay.Almanac);               assertEquals(3, vm.s.tutorialStep)
        vm.dismissOverlay()
        vm.travelTo(vm.s.route[1]);                    assertEquals(4, vm.s.tutorialStep)
        vm.arrive()
        vm.setComp("hair", vm.s.culprit!!.tHair);      assertEquals(5, vm.s.tutorialStep)
    }

    @Test fun skipEndsTheTutorial() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }
        vm.skipTutorial()
        assertEquals(-1, vm.s.tutorialStep)
        assertTrue(vm.s.tutorialDone)
    }

    @Test fun aSecondCaseIsNotGuided() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }   // tutorialDone becomes true
        vm.menuNewCase()                                       // still casesSolved 0, but seen
        assertEquals(-1, vm.s.tutorialStep)
    }
}
