package com.acme.clara

import com.acme.clara.billing.BillingManager
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Overlay
import com.acme.clara.save.InMemorySaveRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bureau ▸ Hint (ClaraViewModel.requestHint()). While BillingManager.SALES_ENABLED is false (this
 * release), Hint must stay exactly as it always was — that's the branch every current player
 * actually exercises, so it's asserted unconditionally. The paid-gated behavior (unpaid -> offer
 * the purchase, paid -> one concrete tip per case) only exists on the other side of that switch;
 * it's asserted here too, but skips itself while the switch is off so it can't fail against dead
 * code, and starts holding the moment someone flips SALES_ENABLED for the real launch.
 */
class BureauHintTest {

    @Test fun today_hintStaysUnlimitedAndFreeRegardlessOfPaidStatus() {
        if (BillingManager.SALES_ENABLED) return   // this test describes the switched-off release only
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.requestHint()
        assertTrue("still an Info overlay, never a purchase prompt, while sales are off",
            vm.s.overlay is Overlay.Info)
        assertEquals(1, vm.s.hintsUsed)
        vm.dismissOverlay()

        vm.requestHint()   // a second ask in the same case is not blocked today
        assertEquals("no per-case lockout while SALES_ENABLED is false", 2, vm.s.hintsUsed)
    }

    @Test fun onceSalesGoLive_unpaidTapOffersThePurchaseInsteadOfAHint() {
        if (!BillingManager.SALES_ENABLED) return
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.requestHint()
        assertEquals(Overlay.PurchaseOffer("Bureau hint"), vm.s.overlay)
        assertEquals("no hint state spent on the offer", 0, vm.s.hintsUsed)
        assertEquals(false, vm.s.bureauTipUsed)
    }

    @Test fun onceSalesGoLive_paidCareerGetsExactlyOneConcreteTipPerCase() {
        if (!BillingManager.SALES_ENABLED) return
        val vm = ClaraViewModel().apply { signOn("Ada"); unlockExpansion() }

        vm.requestHint()
        assertTrue("first paid ask is granted", vm.s.overlay is Overlay.Info)
        assertTrue("the tip is spent", vm.s.bureauTipUsed)
        assertEquals(1, vm.s.hintsUsed)
        vm.dismissOverlay()

        vm.requestHint()   // second ask, same case
        assertEquals("no second tip is spent", 1, vm.s.hintsUsed)
        val lines = (vm.s.overlay as Overlay.Info).lines
        assertTrue("told there's nothing left, not repeated or vagued out",
            lines.any { it.contains("no additional tips") })
    }

    @Test fun onceSalesGoLive_aBankedWelcomeBackHintBypassesThePaidGate() {
        if (!BillingManager.SALES_ENABLED) return
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Ada") }   // unpaid
        vm.attachSave(repo, "p1") { 0L }
        vm.openVenue(0)   // an action after attaching, so this save actually lands
        val saved = repo.load("p1")!!

        val reopened = ClaraViewModel()
        reopened.bindRepository(repo) { 5L * 24 * 60 * 60 * 1000 }   // five days later
        reopened.resume(saved)
        assertEquals(1, reopened.s.freeHints)
        reopened.dismissOverlay()

        reopened.requestHint()
        assertTrue("the welcome-back promise is honored even without buying",
            reopened.s.overlay is Overlay.Info)
        assertEquals(0, reopened.s.freeHints)
        assertEquals("a free hint never touches the paid tip", false, reopened.s.bureauTipUsed)
    }
}
