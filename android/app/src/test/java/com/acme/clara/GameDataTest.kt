package com.acme.clara

import com.acme.clara.data.GameData
import com.acme.clara.data.Progression
import com.acme.clara.data.WorldMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Integrity of the extracted game data (GameData.kt is generated from the corpus). */
class GameDataTest {

    @Test fun nineteenSuspectsIncludingCarmen() {
        assertEquals(19, GameData.suspects.size)
        assertTrue(GameData.suspects.any { it.name == "Clara San Diego" })
    }

    @Test fun everySuspectTraitTupleIsUnique() {
        // The crime computer relies on each suspect being uniquely identifiable by the
        // 5-trait description; duplicate tuples would make some cases unsolvable.
        val tuples = GameData.suspects.map {
            listOf(it.tSex, it.tHobby, it.tHair, it.tFeature, it.tVehicle)
        }
        assertEquals("trait tuples must be unique", tuples.size, tuples.toSet().size)
    }

    @Test fun everyTraitValueIsInItsDomain() {
        for (s in GameData.suspects) {
            assertTrue("${s.name} sex", s.tSex in GameData.sexes)
            assertTrue("${s.name} hobby ${s.tHobby}", s.tHobby in GameData.hobbies)
            assertTrue("${s.name} hair ${s.tHair}", s.tHair in GameData.hairColors)
            assertTrue("${s.name} feature ${s.tFeature}", s.tFeature in GameData.features)
            assertTrue("${s.name} vehicle ${s.tVehicle}", s.tVehicle in GameData.vehicles)
        }
    }

    @Test fun raceCarIsTheImpossibleTrait() {
        // "race car" is the computer trait no suspect has (selecting it eliminates everyone) —
        // a documented 1990 mechanic and the reason Lady Agatha was fixed to "convertible".
        assertTrue("race car" in GameData.vehicles)
        assertFalse(GameData.suspects.any { it.tVehicle == "race car" })
    }

    @Test fun traitClueFragmentsCoverEveryUsedValue() {
        // Every hobby/hair/feature/vehicle value that a real suspect has must have witness
        // phrasings, or that trait clue would fall back to a generic line.
        for (s in GameData.suspects.filter { it.name != "Clara San Diego" }) {
            for (key in listOf("hobby:${s.tHobby}", "hair:${s.tHair}",
                    "feature:${s.tFeature}", "vehicle:${s.tVehicle}")) {
                val frags = GameData.traitClueFragments[key]
                assertNotNull("missing fragments for $key (${s.name})", frags)
                assertTrue("empty fragments for $key", frags!!.isNotEmpty())
            }
        }
    }

    @Test fun traitClueFragmentsHaveNoStrayPlaceholders() {
        // Only {S} {s} {p} are valid pronoun slots.
        val valid = Regex("\\{[Ssp]\\}")
        for ((k, frags) in GameData.traitClueFragments) {
            for (f in frags) {
                val leftover = f.replace(valid, "")
                assertFalse("bad placeholder in $k: $f", leftover.contains("{") || leftover.contains("}"))
            }
        }
    }

    @Test fun noInformationCoversEveryVenue() {
        for (v in GameData.venues) {
            assertNotNull("no no-info line for venue $v", GameData.noInformationByVenue[v])
        }
        assertEquals(12, GameData.venues.size)
    }

    @Test fun venueOccupationsCoverEveryVenueWithValidOccupations() {
        for (v in GameData.venues) {
            val occs = GameData.venueOccupations[v]
            assertNotNull("no occupations for venue $v", occs)
            assertTrue("empty occupations for $v", occs!!.isNotEmpty())
            for (o in occs) assertTrue("occupation '$o' not in list", o in GameData.occupations)
        }
    }

    @Test fun dossierMenuNamesMatchSuspectCount() {
        assertEquals(GameData.suspects.size, GameData.dossierMenuNames.size)
        assertEquals("Clara San Diego", GameData.dossierMenuNames.first())
    }

    @Test fun rankLadderFreeAndInternational() {
        assertEquals("free career is 5 ranks", 5, Progression.FREE_RANKS)
        assertEquals("Rookie", GameData.ranks.first())
        assertEquals("free ladder tops out at Ace Detective",
            "Ace Detective", GameData.ranks[Progression.FREE_RANKS - 1])
        assertEquals("15 ranks: 5 free + 10 International grades", 15, GameData.ranks.size)
        assertEquals("Chief Director", GameData.ranks.last())
        assertEquals("last rank index matches LAST_RANK", Progression.LAST_RANK, GameData.ranks.lastIndex)
        assertTrue(GameData.promotionQuiz.isNotEmpty())
        for (item in GameData.promotionQuiz) {
            assertTrue("quiz question should have a blank", item.question.contains("_"))
            assertTrue("quiz answer blank", item.answer.isNotBlank())
        }
        assertEquals("quiz ids are unique", GameData.promotionQuiz.size,
            GameData.promotionQuiz.map { it.id }.distinct().size)
    }

    @Test fun thirtyCitiesAllOnTheWorldMap() {
        assertEquals(30, GameData.cities.size)
        for (c in GameData.cities) {
            assertNotNull("city $c missing from WorldMap", WorldMap.pos[c])
        }
        // and the map has no phantom cities
        for (c in WorldMap.pos.keys) assertTrue("$c not a real city", c in GameData.cities)
    }
}
