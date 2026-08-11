package com.acme.clara

import com.acme.clara.data.GameData
import com.acme.clara.data.Masterminds
import com.acme.clara.data.Progression
import com.acme.clara.data.Suspect
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The paid-campaign story layer (data/Masterminds.kt): Case 14 is an escape, not a capture; every
 * 8th International case afterward forces that arc's suspect and region-locks the route; and the
 * campaign finale is Clara's real, automatic (no-quiz) capture.
 */
class MastermindsTest {

    private fun ClaraViewModel.issueWarrantFor(su: Suspect) {
        setComp("sex", su.tSex); setComp("hobby", su.tHobby); setComp("hair", su.tHair)
        setComp("feature", su.tFeature); setComp("vehicle", su.tVehicle); compute()
    }

    private fun ClaraViewModel.solveCurrentCase() {
        issueWarrantFor(s.culprit!!)
        var guard = 0
        while (s.progress < s.route.size - 1 && guard++ < 16) { travelTo(s.route[s.progress + 1]); arrive() }
        for (i in 0..2) { if (s.phase == Phase.CHASE) break; openVenue(i) }
        if (s.phase == Phase.CHASE) chaseDone()
    }

    /** Solve, ace any quiz, advance. */
    private fun ClaraViewModel.advance() {
        solveCurrentCase()
        if (s.pendingPromotion) resolvePromotion(true)
        nextCase()
    }

    @Test fun wave1FinaleForcesEuropesBossAndLocksTheRouteToWave0() {
        val vm = ClaraViewModel().apply { signOn("Agent"); unlockExpansion() }
        // 21 solved cases (Case 14's enrollment + 7 ordinary Wave-1 cases) -> case 22, the loaded
        // but not-yet-solved case, is the first arc's finale.
        repeat(21) { vm.advance() }
        val arc = Masterminds.arcForTrigger(1)!!
        assertEquals("case 22's forced culprit is Europe's Boss", arc.suspectName, vm.s.culprit?.name)
        assertTrue("the whole route stays inside wave 0 (Europe)",
            vm.s.route.all { Progression.wave[it] == 0 })

        vm.solveCurrentCase()
        assertTrue("the boss capture is a real win", vm.s.won)
        assertFalse("capturing a Boss doesn't end the career", vm.s.careerOver)
        assertTrue("promotion is pending", vm.s.pendingPromotion)
        vm.resolvePromotion(true)
        assertEquals("Field Inspector awarded", "Field Inspector", GameData.ranks[vm.s.rankIndex])
        assertTrue("the Boss joins the Most Wanted gallery", arc.suspectName in vm.s.capturedVillains)
    }

    @Test fun theFinaleForcesClaraAndEndsTheCareerAutomatically() {
        val vm = ClaraViewModel().apply { signOn("Director"); unlockExpansion() }
        // 85 solved cases -> case 86, the loaded but not-yet-solved case, is the finale (trigger 9).
        repeat(85) { vm.advance() }
        assertEquals("Clara is forced as the finale's culprit", "Clara San Diego", vm.s.culprit?.name)
        assertTrue("the finale route stays inside wave 8",
            vm.s.route.all { Progression.wave[it] == 8 })

        vm.solveCurrentCase()
        assertTrue("the true finale is a win", vm.s.won)
        assertTrue("the career truly ends this time", vm.s.careerOver)
        assertFalse("Chief Director is automatic — no quiz", vm.s.pendingPromotion)
        assertEquals("Chief Director awarded directly", "Chief Director", GameData.ranks[vm.s.rankIndex])
        assertTrue("Clara is finally in the Most Wanted gallery", "Clara San Diego" in vm.s.capturedVillains)
        assertTrue("the Kingpin commendation unlocks", "kingpin" in vm.s.unlockedAchievements)
        assertTrue("the report celebrates jailing the ring-leader",
            vm.s.resultLines.any { it.contains("Clara San Diego is behind bars") })
    }

    @Test fun buyingAfterAnUnpaidCase14EscapeGrantsSpecialAgentImmediately() {
        val vm = ClaraViewModel().apply { signOn("LateBuyer") }   // no unlock yet
        repeat(14) { vm.solveCurrentCase(); if (vm.s.pendingPromotion) vm.resolvePromotion(true); vm.nextCase() }
        assertEquals("still Ace Detective — no promotion for an unpaid escape",
            "Ace Detective", GameData.ranks[vm.s.rankIndex])

        vm.unlockExpansion()
        assertTrue("buying retroactively queues the Special Agent promotion", vm.s.pendingPromotion)
        vm.resolvePromotion(true)
        assertEquals("Special Agent awarded without replaying a case",
            "Special Agent", GameData.ranks[vm.s.rankIndex])
    }

    @Test fun buyingBeforeCase14MakesItBothAnEscapeAndAnImmediatePromotion() {
        // win()'s isCase14Clara (the escape narrative) and intlThreshold (the promotion) are two
        // independent checks against the same solve — already-paid means both fire on the very
        // same win(), unlike an unpaid escape (which gets the narrative now, the rank only later).
        val vm = ClaraViewModel().apply { signOn("EarlyBuyer"); unlockExpansion() }
        repeat(13) { vm.advance() }
        assertEquals("Clara San Diego", vm.s.culprit?.name)

        vm.solveCurrentCase()
        assertTrue("case 14 is a win", vm.s.won)
        assertFalse("case 14 never ends the career, paid or not", vm.s.careerOver)
        assertTrue("the escape narrative still plays even though it's paid",
            vm.s.resultLines.any { it.contains("coded warrant") || it == GameData.GOT_AWAY })
        assertTrue("already paid, so the promotion is pending on this same solve", vm.s.pendingPromotion)
        assertEquals("Clara isn't captured — she escaped, purchase or not",
            0, vm.s.capturedVillains.count { it == "Clara San Diego" })

        vm.resolvePromotion(true)
        assertEquals("Special Agent awarded on the spot, no separate purchase-timing gap",
            "Special Agent", GameData.ranks[vm.s.rankIndex])
    }

    @Test fun buyingManyFreeCasesAfterCase14StillGrantsSpecialAgentRetroactively() {
        // The retroactive grant in unlockExpansion() only checks casesSolved >= CAREER_CASES — it
        // must not depend on buying right after Case 14; a player who free-plays on for a while
        // first is just as entitled to the promotion the moment they do buy.
        val vm = ClaraViewModel().apply { signOn("SlowBuyer") }   // no unlock yet
        repeat(20) { vm.solveCurrentCase(); if (vm.s.pendingPromotion) vm.resolvePromotion(true); vm.nextCase() }
        assertEquals("still Ace Detective after six ordinary free cases past the escape",
            "Ace Detective", GameData.ranks[vm.s.rankIndex])
        assertEquals(20, vm.s.casesSolved)

        vm.unlockExpansion()
        assertTrue("still retroactively queued, however many free cases came after",
            vm.s.pendingPromotion)
        vm.resolvePromotion(true)
        assertEquals("Special Agent awarded without replaying anything",
            "Special Agent", GameData.ranks[vm.s.rankIndex])
    }

    @Test fun buyingAfterAnArcsCaseNumberAlreadyPassedUnpaidStillDeliversThatArcLater() {
        // Without storyStartCase, triggers are measured from the fixed Case 14, so a late buyer
        // whose case count already passed a trigger's absolute case number (here, 22 — Europe's
        // Boss) would never see that arc at all, and the next arc that DOES fire would hand out
        // the wrong (sequential, not arc.patentRank) rank alongside it. storyStartCase fixes this
        // by measuring the 8-case cadence from the purchase itself when it happens after Case 14,
        // so every arc still fires in order — just shifted later, never skipped.
        val vm = ClaraViewModel().apply { signOn("VeryLateBuyer") }   // no unlock yet
        // 21 solved cases: past where case 22 (trigger 1, Lady Agatha Wayland) would have fired,
        // had this career been paid — it wasn't, so case 22 already happened as an ordinary case.
        repeat(21) { vm.solveCurrentCase(); if (vm.s.pendingPromotion) vm.resolvePromotion(true); vm.nextCase() }
        assertEquals(21, vm.s.casesSolved)

        vm.unlockExpansion()
        assertEquals("the story now starts counting from the purchase, not the passed case 22",
            21, vm.s.storyStartCase)
        vm.resolvePromotion(true)   // the retroactive Special Agent grant from Case 14's escape

        // Case 22 (the loaded case right after buying) was already generated pre-purchase, so it's
        // an ordinary case; solving it and the next six ordinary ones (cases 22-28) reaches case 29
        // (storyStartCase 21 + 8) as the freshly-generated, not-yet-solved case — the first arc,
        // still fired, just later than the fixed Case 14 cadence would have placed it.
        repeat(7) { vm.advance() }
        val arc = Masterminds.arcForTrigger(1)!!
        assertEquals("the skipped arc's suspect still shows up, now that the story restarted here",
            arc.suspectName, vm.s.culprit?.name)
        assertTrue("its route is still properly region-locked",
            vm.s.route.all { Progression.wave[it] == arc.waveForRoute })

        vm.solveCurrentCase()
        assertTrue("the promotion for THIS arc is pending", vm.s.pendingPromotion)
        vm.resolvePromotion(true)
        assertEquals("the rank matches the arc actually delivered (patentRank), not a sequential " +
            "count that assumed no arc was ever skipped",
            GameData.ranks[arc.patentRank], GameData.ranks[vm.s.rankIndex])
        assertTrue("the arc's suspect is credited", arc.suspectName in vm.s.capturedVillains)
    }
}
