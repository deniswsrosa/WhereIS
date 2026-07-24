package com.acme.carmen

import com.acme.carmen.data.GameData
import com.acme.carmen.game.CarmenViewModel
import com.acme.carmen.game.ClueKind
import com.acme.carmen.game.Phase
import com.acme.carmen.game.SoundCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {

    private fun fresh() = CarmenViewModel().apply { signOn("Tester") }

    // ---------- case generation ----------

    @Test fun rookieCaseHasFiveCityRouteAndNonCarmenCulprit() {
        repeat(200) {
            val vm = fresh()
            assertEquals("rookie route length", 5, vm.s.route.size)
            assertEquals(0, vm.s.progress)
            assertNull("fresh case has no warrant", vm.s.warrantFor)
            assertNotNull(vm.s.culprit)
            assertFalse("Carmen is never the culprit early", vm.s.culprit!!.name == "Carmen Sandiego")
            assertEquals("case starts at route[0]", vm.s.route.first(), vm.s.currentCity)
            assertEquals("route cities are distinct", vm.s.route.size, vm.s.route.toSet().size)
        }
    }

    @Test fun revealOrderUniquelyIdentifiesTheCulprit() {
        // A prefix of the discriminating order must pin down exactly one suspect (the culprit).
        fun value(su: com.acme.carmen.data.Suspect, cat: String) = when (cat) {
            "sex" -> su.tSex; "hobby" -> su.tHobby; "hair" -> su.tHair
            "feature" -> su.tFeature; else -> su.tVehicle
        }
        repeat(200) {
            val vm = fresh()
            val order = vm.s.revealOrder
            assertTrue("reveal order non-empty", order.isNotEmpty())
            val narrowed = GameData.suspects.filter { su -> order.all { (cat, v) -> value(su, cat) == v } }
            assertEquals("full reveal order identifies exactly the culprit",
                listOf(vm.s.culprit!!.name), narrowed.map { it.name })
        }
    }

    // ---------- clue phrasing (§19) ----------

    @Test fun venueCluesNeverContainUnsubstitutedPlaceholders() {
        repeat(100) {
            val vm = fresh()
            for (v in vm.s.venues) {
                assertFalse("placeholder left in: ${v.text}", v.text.contains("{") || v.text.contains("}"))
            }
        }
    }

    @Test fun traitCluesUseTheCulpritsGenderPronoun() {
        val he = Regex("\\b(he|his|him)\\b", RegexOption.IGNORE_CASE)
        val she = Regex("\\b(she|her|hers)\\b", RegexOption.IGNORE_CASE)
        repeat(100) {
            val vm = fresh()
            val female = vm.s.culprit!!.sex == "Female"
            for (v in vm.s.venues.filter { it.kind == ClueKind.TRAIT }) {
                // every trait fragment carries a pronoun slot, so the correct-gender pronoun
                // must appear and the wrong-gender one must not (whole-word match; "She"
                // does not count as "he" thanks to the boundary).
                if (female) {
                    assertTrue("expected female pronoun in: ${v.text}", she.containsMatchIn(v.text))
                    assertFalse("male pronoun leaked into female clue: ${v.text}", he.containsMatchIn(v.text))
                } else {
                    assertTrue("expected male pronoun in: ${v.text}", he.containsMatchIn(v.text))
                    assertFalse("female pronoun leaked into male clue: ${v.text}", she.containsMatchIn(v.text))
                }
            }
        }
    }

    @Test fun firstCityHasADestinationClueToTheNextCity() {
        repeat(50) {
            val vm = fresh()
            val dest = vm.s.venues.firstOrNull { it.kind == ClueKind.DESTINATION }
            assertNotNull("first city should offer a destination clue", dest)
        }
    }

    // ---------- sound cues ----------

    @Test fun crimeComputerEmitsFlashCue() {
        val vm = fresh()
        vm.gotoCrime()
        assertEquals(Phase.CRIME, vm.s.phase)
        assertEquals(SoundCue.FLASH, vm.soundCue?.second)
    }

    @Test fun startingAFlightEmitsTravelCue() {
        val vm = fresh()
        vm.travelTo(vm.s.route[1])
        assertEquals(SoundCue.TRAVEL, vm.soundCue?.second)
        assertNotNull(vm.s.flying)
    }

    @Test fun warrantEmitsWarrantCue() {
        val vm = fresh()
        val c = vm.s.culprit!!
        vm.setComp("sex", c.tSex); vm.setComp("hobby", c.tHobby); vm.setComp("hair", c.tHair)
        vm.setComp("feature", c.tFeature); vm.setComp("vehicle", c.tVehicle)
        vm.compute()
        assertEquals(c.name, vm.s.warrantFor?.name)
        assertEquals(SoundCue.WARRANT, vm.soundCue?.second)
    }

    @Test fun openingAClueVenueEmitsClueCue() {
        val vm = fresh()
        // first city, first venue is a non-danger destination clue
        val idx = vm.s.venues.indexOfFirst { it.kind != ClueKind.DANGER }
        vm.openVenue(idx)
        assertEquals(SoundCue.CLUE, vm.soundCue?.second)
    }

    @Test fun cueSeqIncrementsSoRepeatsRetrigger() {
        val vm = fresh()
        vm.gotoCrime()
        val first = vm.soundCue!!.first
        vm.gotoCrime()
        assertTrue("seq must advance on repeat cue", vm.soundCue!!.first > first)
    }

    // ---------- deadline ----------

    @Test fun runningOutOfTimeEndsTheCaseAsALoss() {
        val vm = fresh()
        // COMPUTE costs 3h each; the deadline is 152h — enough computes must trip it.
        var guard = 0
        while (vm.s.phase != Phase.RESULT && guard++ < 200) vm.compute()
        assertEquals("deadline should force a RESULT", Phase.RESULT, vm.s.phase)
        assertFalse("out-of-time is a loss", vm.s.won)
        assertEquals(SoundCue.OUT_OF_TIME, vm.soundCue?.second)
    }

    // ---------- travel / WorldMap ----------

    @Test fun flightCostIsAlwaysWithinBounds() {
        // every start/destination pair yields a flight time in the DOS-plausible [2,14]h range
        repeat(30) {
            val vm = fresh()
            for (dest in GameData.cities.filter { it != vm.s.currentCity }.shuffled().take(6)) {
                val clone = fresh()
                clone.travelTo(dest)
                assertTrue("flight ${clone.s.flightHours}h out of bounds",
                    clone.s.flightHours in 2..14)
            }
        }
    }

    // ---------- hideout confrontation odds ----------

    private fun flyToHideout(vm: CarmenViewModel) {
        // fly the correct route leg by leg until at the final (hideout) city
        var guard = 0
        while (vm.s.progress < vm.s.route.size - 1 && guard++ < 12) {
            vm.travelTo(vm.s.route[vm.s.progress + 1])
            vm.arrive()
        }
    }

    @Test fun neverCaughtAtTheFirstHideoutVenue() {
        repeat(40) {
            val vm = fresh()
            flyToHideout(vm)
            assertTrue("should be at the hideout", vm.s.atHideout)
            vm.openVenue(0)               // first distinct venue
            assertFalse("first venue must never trigger the chase", vm.s.phase == Phase.CHASE)
        }
    }

    @Test fun alwaysCaughtByTheThirdHideoutVenue() {
        repeat(40) {
            val vm = fresh()
            flyToHideout(vm)
            if (!vm.s.atHideout) return@repeat
            // open up to three distinct venues; by the third the crook is certain
            for (i in 0..2) {
                if (vm.s.phase == Phase.CHASE) break
                vm.openVenue(i)
            }
            assertEquals("caught within three hideout venues", Phase.CHASE, vm.s.phase)
        }
    }
}
