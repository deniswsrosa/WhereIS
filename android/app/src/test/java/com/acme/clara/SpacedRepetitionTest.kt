package com.acme.clara

import com.acme.clara.game.SpacedRepetition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** L4 — the spaced-repetition scheduler. Pure logic: freshness/due windows and the weighted
 *  route pick that resurfaces geography without ever touching a clue. */
class SpacedRepetitionTest {

    @Test fun freshMeansSeenThisCase() {
        val seen = mapOf("Cairo" to 5)
        assertTrue("gap 0 is fresh", SpacedRepetition.isFresh("Cairo", seen, 5))
        assertFalse("gap 1 is not fresh", SpacedRepetition.isFresh("Cairo", seen, 6))
        assertFalse("never-seen is not fresh", SpacedRepetition.isFresh("Paris", seen, 5))
    }

    @Test fun dueOnlyOnTheExpandingIntervals() {
        val seen = mapOf("Cairo" to 0)
        // review windows are 1, 3, 7, 15 cases out
        for (due in listOf(1, 3, 7, 15)) {
            assertTrue("gap $due should be due", SpacedRepetition.isDue("Cairo", seen, 0 + due))
        }
        for (notDue in listOf(0, 2, 4, 5, 6, 8)) {
            assertFalse("gap $notDue should not be due", SpacedRepetition.isDue("Cairo", seen, notDue))
        }
        assertFalse("never-seen is never due", SpacedRepetition.isDue("Paris", seen, 10))
    }

    @Test fun pickRouteReturnsDistinctCitiesOfTheRightLength() {
        val cities = (1..12).map { "City$it" }
        repeat(100) {
            val route = SpacedRepetition.pickRoute(cities, emptyMap(), 3, 5)
            assertEquals("route length", 5, route.size)
            assertEquals("route cities are distinct", route.size, route.toSet().size)
            assertTrue("route drawn from the pool", route.all { it in cities })
        }
    }

    @Test fun pickRouteReturnsWholePoolWhenNotLargerThanN() {
        val cities = listOf("A", "B", "C")
        val route = SpacedRepetition.pickRoute(cities, emptyMap(), 0, 5)
        assertEquals("whole pool returned", cities.toSet(), route.toSet())
    }

    @Test fun pickRouteFavoursDueAndNeverSeenOverJustSeen() {
        // "Due" (gap 1) and "Fresh" (gap 0, just seen) plus lots of neutral filler.
        val currentCase = 10
        val lastSeen = mapOf("DueCity" to currentCase - 1, "JustSeen" to currentCase)
        val cities = listOf("DueCity", "JustSeen") + (1..10).map { "Filler$it" }
        var due = 0
        var justSeen = 0
        repeat(600) {
            val route = SpacedRepetition.pickRoute(cities, lastSeen, currentCase, 4)
            if ("DueCity" in route) due++
            if ("JustSeen" in route) justSeen++
        }
        assertTrue("a due fact resurfaces far more than a just-seen one ($due vs $justSeen)",
            due > justSeen)
    }
}
