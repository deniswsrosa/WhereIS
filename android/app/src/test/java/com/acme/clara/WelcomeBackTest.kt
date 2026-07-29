package com.acme.clara

import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Overlay
import com.acme.clara.game.WelcomeBack
import com.acme.clara.save.InMemorySaveRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeBackTest {

    private val DAY = 24L * 60 * 60 * 1000

    // ---------- pure return logic ----------

    @Test fun daysAwayAndGrantThreshold() {
        assertEquals(0, WelcomeBack.daysAway(0, 0))
        assertEquals(0, WelcomeBack.daysAway(100, 50))          // clock going backwards → 0
        assertEquals(5, WelcomeBack.daysAway(0, 5 * DAY))
        assertFalse(WelcomeBack.grantsHint(0, 2 * DAY))
        assertTrue(WelcomeBack.grantsHint(0, 3 * DAY))
    }

    // ---------- returning banks a free hint, spent before the badge ----------

    @Test fun returningAfterDaysBanksAFreeHintThenSpendsItFirst() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.attachSave(repo, "p1") { 0L }        // career saved at t=0
        vm.openVenue(0)
        val saved = repo.load("p1")!!           // meta.lastPlayed == 0

        val reopened = ClaraViewModel()
        reopened.bindRepository(repo) { 5 * DAY }   // returning five days later
        reopened.resume(saved)

        assertEquals("a free hint is banked", 1, reopened.s.freeHints)
        assertTrue("welcome-back message shown", reopened.s.overlay is Overlay.Info)
        reopened.dismissOverlay()

        // spending the free hint leaves the hint-free record intact
        reopened.requestHint()
        assertEquals(0, reopened.s.freeHints)
        assertEquals("free hint doesn't cost the badge", 0, reopened.s.hintsUsed)
        reopened.dismissOverlay()

        // the next hint costs the badge
        reopened.requestHint()
        assertEquals(1, reopened.s.hintsUsed)
    }

    @Test fun returningPromptlyBanksNothing() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.attachSave(repo, "p1") { 0L }
        vm.openVenue(0)
        val saved = repo.load("p1")!!

        val reopened = ClaraViewModel()
        reopened.bindRepository(repo) { DAY }       // one day later — under the threshold
        reopened.resume(saved)

        assertEquals(0, reopened.s.freeHints)
    }
}
