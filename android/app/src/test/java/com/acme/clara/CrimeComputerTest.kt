package com.acme.clara

import com.acme.clara.data.GameData
import com.acme.clara.game.ClaraViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The crime computer filters suspects by the 5-trait description and issues a warrant
 *  when exactly one matches — independent of the current case's culprit. */
class CrimeComputerTest {

    private fun vm() = ClaraViewModel().apply { signOn("Tester") }

    private fun ClaraViewModel.setAll(sex: String?, hobby: String?, hair: String?, feat: String?, veh: String?) {
        setComp("sex", sex); setComp("hobby", hobby); setComp("hair", hair)
        setComp("feature", feat); setComp("vehicle", veh)
    }

    @Test fun eachSuspectFullyDescribedMatchesOnlyThemselves() {
        val vm = vm()
        for (s in GameData.suspects) {
            vm.setAll(s.tSex, s.tHobby, s.tHair, s.tFeature, s.tVehicle)
            val m = vm.matches()
            assertEquals("full description of ${s.name} should be unique", listOf(s.name), m.map { it.name })
        }
    }

    @Test fun computeIssuesWarrantForUniqueMatch() {
        val vm = vm()
        val target = GameData.suspects.first { it.name == "Nick Brunch" }
        vm.setAll(target.tSex, target.tHobby, target.tHair, target.tFeature, target.tVehicle)
        assertNull("no warrant before compute", vm.s.warrantFor)
        vm.compute()
        assertEquals(target.name, vm.s.warrantFor?.name)
    }

    @Test fun raceCarEliminatesAllSuspects() {
        val vm = vm()
        vm.setAll("female", null, null, null, "race car")
        assertTrue("no suspect drives a race car", vm.matches().isEmpty())
        vm.compute()
        assertNull("no warrant when nobody matches", vm.s.warrantFor)
    }

    @Test fun partialDescriptionNarrowsButMayNotWarrant() {
        val vm = vm()
        vm.setAll("male", null, null, null, null)
        val males = GameData.suspects.count { it.tSex == "male" }
        assertEquals(males, vm.matches().size)
        vm.compute()
        // more than one suspect -> no warrant issued (unless males happens to be 1, it isn't)
        assertTrue(males > 1)
        assertNull(vm.s.warrantFor)
    }

    @Test fun anyFilterSetReflectsState() {
        val vm = vm()
        assertFalse(vm.anyFilterSet())
        vm.setComp("hair", "red")
        assertTrue(vm.anyFilterSet())
        vm.setComp("hair", null)
        assertFalse(vm.anyFilterSet())
    }

    @Test fun noFilterMatchesEveryone() {
        val vm = vm()
        assertEquals(GameData.suspects.size, vm.matches().size)
    }
}
