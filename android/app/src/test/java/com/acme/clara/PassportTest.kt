package com.acme.clara

import com.acme.clara.data.CityMeta
import com.acme.clara.data.CountryShapes
import com.acme.clara.data.Expansion
import com.acme.clara.game.ClaraViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** C4 Passport: the country data is complete, every silhouette decodes onto the map, and the
 *  visited-place log fills in from day one as the detective lands in cities. */
class PassportTest {

    private val allPlaces: Set<String> = CityMeta.all.keys + Expansion.names

    @Test fun everyGamePlaceMapsToACountry() {
        val missing = allPlaces.filter { it !in CountryShapes.placeCountry }
        assertTrue("places with no country mapping: $missing", missing.isEmpty())
    }

    @Test fun everyCountryEitherPaintsASilhouetteOrFallsBackToADot() {
        val codes = allPlaces.mapNotNull { CountryShapes.placeCountry[it] }.toSet()
        for (code in codes) {
            val hasShape = CountryShapes.rings(code).isNotEmpty()
            val isDot = code in CountryShapes.dotFallback
            assertTrue("$code must paint a silhouette or be a dot fallback", hasShape || isDot)
            assertNotNull("$code should have a display name", CountryShapes.countryName[code])
        }
    }

    @Test fun dotFallbackCountriesHaveNoSilhouette() {
        for (code in CountryShapes.dotFallback) {
            assertTrue("$code is a dot fallback, so it must have no rings",
                CountryShapes.rings(code).isEmpty())
        }
    }

    @Test fun silhouettesDecodeToNormalisedPointsOnTheMap() {
        // USA is a big multi-ring country — a good witness that decoding works.
        val rings = CountryShapes.rings("USA")
        assertTrue("USA has a silhouette", rings.isNotEmpty())
        for (ring in rings) {
            assertTrue("each ring is a polygon", ring.size >= 3)
            for (p in ring) {
                assertTrue("x in 0..1 (${p.x})", p.x in 0f..1f)
                assertTrue("y in 0..1 (${p.y})", p.y in 0f..1f)
            }
        }
    }

    @Test fun landingInACityLogsItForThePassport() {
        val vm = ClaraViewModel().apply { signOn("Tester") }
        val start = vm.s.route.first()
        assertTrue("the briefing city is logged", start in vm.s.visitedPlaces)
        assertTrue("and stamped with the case index", start in vm.s.cityLastSeen)

        val next = vm.s.route[1]
        vm.travelTo(next); vm.arrive()
        assertTrue("arriving logs the new place", next in vm.s.visitedPlaces)
        assertTrue("and stamps it for spaced repetition", next in vm.s.cityLastSeen)

        // every place the tracker records resolves to a real country
        assertTrue(vm.s.visitedPlaces.all { it in CountryShapes.placeCountry })
    }

    @Test fun freePlayerOnlyEverVisitsOriginalCountries() {
        // Without the expansion unlock, routes never leave the original 30 — so a free player's
        // passport can only ever contain original-tier places.
        val vm = ClaraViewModel().apply { signOn("Tester") }
        assertFalse("free career starts locked", vm.s.expansionUnlocked)
        repeat(30) {
            vm.travelTo(vm.s.route.getOrElse(vm.s.progress + 1) { vm.s.route.last() }); vm.arrive()
        }
        assertTrue("free visits stay within the original 30",
            vm.s.visitedPlaces.all { it in CityMeta.all.keys })
    }
}
