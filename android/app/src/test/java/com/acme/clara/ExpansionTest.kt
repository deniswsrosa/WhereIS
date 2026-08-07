package com.acme.clara

import com.acme.clara.data.CityMeta
import com.acme.clara.data.Expansion
import com.acme.clara.data.GameData
import com.acme.clara.data.Progression
import com.acme.clara.data.WorldMap
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.save.SaveCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * The paid-tier expansion: 68 new destinations + 17 new venues. Covers data integrity, map
 * projection / flight-time sanity, the free-vs-paid gate, the authored-clue engine, and that
 * the entitlement round-trips through the save.
 */
class ExpansionTest {

    private val base = GameData.cities.toSet()

    // ---- data integrity -------------------------------------------------------------------

    @Test fun loadsSixtyEightDistinctNewCities() {
        assertEquals(68, Expansion.cities.size)
        assertEquals("names are unique", 68, Expansion.names.toSet().size)
        assertTrue("no overlap with the free 30", Expansion.names.none { it in base })
    }

    @Test fun everyCityHasCluesRegionLandmarkAndCoords() {
        Expansion.cities.forEach { c ->
            assertTrue("${c.name} region", c.region.isNotBlank())
            assertTrue("${c.name} landmark", c.landmark.isNotBlank())
            assertTrue("${c.name} description", c.description.length > 20)
            // Flag/currency were pulled into their own fields, so clues[] is now the pure "general
            // hint" pool (1-3); with the flag every city still clears the 3-hint minimum.
            assertTrue("${c.name} has general clues", c.clues.isNotEmpty())
            assertTrue("${c.name} has a flag (Antarctica excepted)", c.name == "Antarctica" || c.flag != null)
            assertTrue("${c.name} has coordinates", Expansion.latLon.containsKey(c.name))
            assertNotNull("${c.name} has a map dot", Expansion.pos[c.name])
        }
    }

    /** The authored clues were rewritten to pronoun slots so they never leak the culprit's sex.
     *  Guard against a raw she/her sneaking back in from a future edit. */
    @Test fun cluesNeverHardcodeAGender() {
        val gendered = Regex("\\b([Ss]he|hers?|herself|[Hh]is|him|himself)\\b")
        Expansion.cities.forEach { c ->
            c.clues.forEach { clue ->
                assertFalse("gendered word in ${c.name}: \"$clue\"", gendered.containsMatchIn(clue))
            }
        }
    }

    @Test fun structuredAttributesAreAttached() {
        // Berlin's say-hello line teaches the local greeting with its pronunciation.
        assertTrue(Expansion.byName["Berlin"]?.greeting?.contains("Guten Tag") == true)
        // Currencies were normalised to the "the <money>" form used in the money clue template.
        assertTrue(Expansion.byName["Novosibirsk"]?.currency?.contains("ruble") == true)
        assertTrue(Expansion.byName["Philippines"]?.currency?.contains("peso") == true)
    }

    @Test fun cityMetaResolvesExpansionCities() {
        val berlin = CityMeta.of("Berlin")
        assertEquals("Europe", berlin.region)
        assertTrue("carries authored clues", berlin.clues.isNotEmpty())
    }

    // ---- venues ---------------------------------------------------------------------------

    @Test fun seventeenNewVenuesWithoutDuplicatingRiverfront() {
        assertEquals(17, Expansion.venues.size)
        assertFalse("Riverfront already exists in the base set", "Riverfront" in Expansion.venues)
        assertTrue("no overlap with base venues", Expansion.venues.none { it in GameData.venues })
        Expansion.venues.forEach { v ->
            assertTrue("$v has a witness", Expansion.venueOccupations[v]?.isNotEmpty() == true)
            assertTrue("$v has a no-info line", Expansion.noInformationByVenue.containsKey(v))
        }
    }

    @Test fun venueAffinityReferencesRealVenues() {
        val known = (GameData.venues + Expansion.venues).toSet()
        Expansion.cityVenueAffinity.forEach { (city, pool) ->
            assertTrue("affinity city $city exists", city in base || city in Expansion.names)
            pool.forEach { v -> assertTrue("affinity venue $v exists", v in known) }
        }
    }

    // ---- map projection / flight times ----------------------------------------------------

    @Test fun everyActiveCityHasAMapPosition() {
        (GameData.cities + Expansion.names).forEach { c ->
            assertNotNull("no map dot for $c → flight time would fall back", WorldMap.of(c))
        }
    }

    @Test fun projectedDotsStayOnTheMap() {
        Expansion.pos.forEach { (city, p) ->
            assertTrue("$city x in [0,1] (${p.x})", p.x in 0f..1f)
            assertTrue("$city y in [0,1] (${p.y})", p.y in 0f..1f)
        }
        // longitude sanity: far-west Honolulu sits left of central-Europe Berlin sits left of Tokyo
        assertTrue(Expansion.pos["Honolulu"]!!.x < Expansion.pos["Berlin"]!!.x)
        assertTrue(Expansion.pos["Berlin"]!!.x < WorldMap.of("Tokyo")!!.x)
    }

    @Test fun distancesRankNearBeforeFar() {
        fun d(a: String, b: String): Double {
            val pa = WorldMap.of(a)!!; val pb = WorldMap.of(b)!!
            return hypot(((pa.x - pb.x) * 2f).toDouble(), (pa.y - pb.y).toDouble())
        }
        assertTrue("Berlin–Cologne closer than Berlin–Honolulu", d("Berlin", "Cologne") < d("Berlin", "Honolulu"))
        assertTrue("Berlin–London closer than Berlin–Sydney", d("Berlin", "London") < d("Berlin", "Sydney"))
    }

    @Test fun flightTimesStayInTheGameRange() {
        val vm = ClaraViewModel().apply { signOn("Ace"); unlockExpansion() }
        repeat(40) {
            vm.menuNewCase()
            vm.s.route.forEach { city ->
                val h = vm.flightHoursTo(city)
                assertTrue("$city flight $h in 2..14", h in 2..14)
            }
        }
    }

    // ---- the free-vs-paid gate ------------------------------------------------------------

    @Test fun lockedCareerNeverRoutesThroughExpansion() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }
        assertFalse(vm.s.expansionUnlocked)
        repeat(60) {
            vm.menuNewCase()
            vm.s.route.forEach { city ->
                assertTrue("$city must be a free city while locked", city in base)
            }
        }
    }

    @Test fun unlockDuringFreeRanksStaysOnOriginals() {
        // Wave model: unlocking grants the *ability* to earn countries, but during the free career
        // (ranks 0..4) only the original 30 appear — waves reveal at the International grades.
        val vm = ClaraViewModel().apply { signOn("Rookie"); unlockExpansion() }
        assertTrue(vm.s.expansionUnlocked)
        assertEquals("no wave unlocked in the free ranks", -1, Progression.unlockedMaxWave(vm.s.rankIndex))
        val seen = mutableSetOf<String>()
        repeat(60) { vm.menuNewCase(); seen += vm.s.route }
        assertTrue("free cities appear", seen.any { it in base })
        assertTrue("no expansion city while still in the free ranks", seen.none { it in Expansion.names })
    }

    @Test fun entitlementRoundTripsThroughSave() {
        val vm = ClaraViewModel().apply { signOn("Ace"); unlockExpansion() }
        val snap = vm.snapshot("p1", 7L)
        val back = SaveCodec.decode(SaveCodec.encode(snap.meta, snap.state))
        assertNotNull(back)
        assertTrue("unlock persists", back!!.state.expansionUnlocked)
    }
}
