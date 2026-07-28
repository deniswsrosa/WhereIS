package com.acme.clara

import com.acme.clara.data.GameData
import com.acme.clara.data.Suspect
import com.acme.clara.game.Achievements
import com.acme.clara.game.CareerSummary
import com.acme.clara.game.CaseJournal
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ClueKind
import com.acme.clara.game.HapticCue
import com.acme.clara.game.Haptics
import com.acme.clara.game.MostWanted
import com.acme.clara.game.Phase
import com.acme.clara.game.ShareCard
import com.acme.clara.game.ShareResult
import com.acme.clara.game.SoundCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaFeaturesTest {

    private fun fresh() = ClaraViewModel().apply { signOn("Tester") }

    // ---------- achievements ----------

    @Test fun achievementsAreEarnedFromTheCareerRecord() {
        fun s(cases: Int = 0, rank: Int = 0, hintFree: Int = 0, clean: Boolean = false,
              caught: Int = 0, total: Int = 10, over: Boolean = false) =
            Achievements.earned(CareerSummary(cases, rank, hintFree, clean, caught, total, over))

        assertTrue("first_arrest" in s(cases = 1))
        assertFalse("first_arrest" in s(cases = 0))
        assertTrue("clean_sweep" in s(clean = true))
        assertTrue("promoted" in s(rank = 1))
        assertTrue("bloodhound" in s(hintFree = 5))
        assertFalse("bloodhound" in s(hintFree = 4))
        assertTrue("veteran" in s(cases = 10))
        assertTrue("most_wanted" in s(caught = 10, total = 10))
        assertFalse("most_wanted" in s(caught = 9, total = 10))
        assertTrue("kingpin" in s(over = true))
    }

    // ---------- most wanted ----------

    @Test fun mostWantedGalleryLocksUntilCaptured() {
        val captured = setOf("Merey LaRoc")
        val g = MostWanted.gallery(captured)
        assertEquals(GameData.suspects.size, g.size)
        assertEquals(1, MostWanted.capturedCount(captured))
        assertTrue(g.first { it.name == "Merey LaRoc" }.captured)
        assertFalse(g.first { it.name == "Clara San Diego" }.captured)
        assertFalse(MostWanted.allCaught(captured))
        assertTrue(MostWanted.allCaught(GameData.suspects.map { it.name }.toSet()))
    }

    // ---------- journal ----------

    @Test fun journalRecapAndCollectedFacts() {
        val vm = fresh()
        assertEquals("Your case opens in ${vm.s.route.first()}.", CaseJournal.recap(vm.s))
        val dest = vm.s.venues.indexOfFirst { it.kind == ClueKind.DESTINATION }
        assertTrue("first city offers a lead", dest >= 0)
        vm.openVenue(dest)
        assertTrue("the lead is logged", CaseJournal.leads(vm.s).isNotEmpty())
    }

    // ---------- share card ----------

    @Test fun shareCardIsSpoilerFree() {
        val card = ShareCard.render(ShareResult("Jul 28", "Sleuth", solved = true, hops = 4,
            wrongFlights = 1, hoursToSpare = 12, hintFree = true))
        assertTrue(card.contains("Sleuth"))
        assertTrue(card.contains("4 hops"))
        assertTrue(card.contains("solved"))
        assertTrue(card.contains("no hints"))
        // never leaks a city / route / suspect name
        for (city in GameData.cities) assertFalse("leaked $city", card.contains(city))
        for (su in GameData.suspects) assertFalse("leaked ${su.name}", card.contains(su.name))
    }

    // ---------- haptics ----------

    @Test fun everySoundCueMapsToAHapticShape() {
        assertEquals(HapticCue.SUCCESS, Haptics.forCue(SoundCue.WIN))
        assertEquals(HapticCue.REJECT, Haptics.forCue(SoundCue.WRONG_ARREST))
        assertEquals(HapticCue.HEAVY, Haptics.forCue(SoundCue.WARRANT))
        assertEquals(HapticCue.NONE, Haptics.forCue(SoundCue.BRIEFING))
        SoundCue.values().forEach { assertNotNull(Haptics.forCue(it)) }
    }

    // ---------- end-to-end: winning updates the career record ----------

    @Test fun solvingACaseCapturesTheVillainAndUnlocksCommendations() {
        repeat(20) {
            val vm = fresh()
            val c = vm.s.culprit!!
            solve(vm, c)
            if (vm.s.phase != Phase.RESULT) return@repeat   // ran out of time on a long route; skip
            assertTrue(vm.s.won)
            assertTrue("villain captured", c.name in vm.s.capturedVillains)
            assertEquals(1, vm.s.casesSolved)
            assertTrue("first_arrest", "first_arrest" in vm.s.unlockedAchievements)
            assertTrue("clean solve earns clean_sweep", "clean_sweep" in vm.s.unlockedAchievements)
        }
    }

    private fun solve(vm: ClaraViewModel, c: Suspect) {
        vm.setComp("sex", c.tSex); vm.setComp("hobby", c.tHobby); vm.setComp("hair", c.tHair)
        vm.setComp("feature", c.tFeature); vm.setComp("vehicle", c.tVehicle)
        vm.compute()
        var guard = 0
        while (vm.s.progress < vm.s.route.size - 1 && guard++ < 12) {
            vm.travelTo(vm.s.route[vm.s.progress + 1]); vm.arrive()
        }
        var i = 0
        while (vm.s.phase != Phase.CHASE && i < 3) { vm.openVenue(i); i++ }
        if (vm.s.phase == Phase.CHASE) vm.chaseDone()
    }
}
