package com.acme.clara

import com.acme.clara.billing.BillingManager
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

        // the next ask: today (SALES_ENABLED off) it still costs the badge; once sales are live an
        // unpaid career is offered the purchase instead of a further hint (requestHint()'s paid gate).
        reopened.requestHint()
        if (BillingManager.SALES_ENABLED) {
            assertEquals(Overlay.PurchaseOffer("Bureau hint"), reopened.s.overlay)
            assertEquals(0, reopened.s.hintsUsed)
        } else {
            assertEquals(1, reopened.s.hintsUsed)
        }
    }

    @Test fun repeatedUnplayedGapsDontStackPastOneBankedHint() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.attachSave(repo, "p1") { 0L }
        vm.openVenue(0)
        var saved = repo.load("p1")!!

        val reopened = ClaraViewModel()
        reopened.bindRepository(repo) { 5 * DAY }
        reopened.resume(saved)                       // first gap: banks 1
        assertEquals(1, reopened.s.freeHints)
        reopened.attachSave(repo, "p1") { 5 * DAY }
        reopened.openVenue(0)
        saved = repo.load("p1")!!

        val reopenedAgain = ClaraViewModel()
        reopenedAgain.bindRepository(repo) { 10 * DAY }
        reopenedAgain.resume(saved)                   // second gap without ever spending the hint
        assertEquals("capped at 1 even across repeated unplayed gaps", 1, reopenedAgain.s.freeHints)
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
