package com.acme.clara

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.acme.clara.data.GameData
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.GameState
import com.acme.clara.ui.RankProgress
import com.acme.clara.ui.Virtual
import com.acme.clara.ui.VirtualScreen
import com.acme.clara.ui.screens.ArrestSlam
import com.acme.clara.ui.screens.CityClockBox
import com.acme.clara.ui.screens.WarrantStamp
import com.acme.clara.ui.screens.promotionPerkLine
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** UI coverage for the five Compose-only levers (P1/P2/P4/S1/S2) — the ones the ViewModel
 *  tests couldn't reach. Each lever composable is rendered inside the game's VirtualScreen and
 *  asserted by the text it draws. P4 is a pure copy function, checked directly. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LeverUiTest {

    @get:Rule val rule = createComposeRule()

    private fun host(block: @Composable BoxScope.(Virtual) -> Unit) {
        rule.setContent { VirtualScreen(content = block) }
    }

    // ---------- P1 trail heat ----------
    @Test fun p1_trailHeatShowsTheCityIndex() {
        val vm = ClaraViewModel().apply { signOn("Tester") }   // rookie route of 5, at city 1
        host { v -> CityClockBox(v, vm) }
        rule.onNodeWithText("CITY 1/5").assertExists()
    }

    // ---------- P2 endowed rank meter ----------
    @Test fun p2_rankMeterNamesTheNextRankWithAHeadStart() {
        val state = GameState(casesSolved = 2, rankIndex = 1)
        host { v -> RankProgress(v, state) }
        rule.onNodeWithText("Toward", substring = true).assertExists()
        rule.onNodeWithText("2 of 5", substring = true).assertExists()  // head-start offset applied
    }

    // ---------- S1 arrest slam ----------
    @Test fun s1_arrestSlamFilesTheMugshot() {
        rule.mainClock.autoAdvance = false                     // freeze before the beat auto-clears
        host { v -> ArrestSlam(v, "Nick Brunch") }
        rule.onNodeWithText("FILED IN MOST WANTED").assertExists()
    }

    // ---------- S2 warrant stamp ----------
    @Test fun s2_warrantStampReadsApproved() {
        rule.mainClock.autoAdvance = false
        host { v -> WarrantStamp(v, reduce = false) {} }
        rule.onNodeWithText("WARRANT ISSUED").assertExists()
        rule.onNodeWithText("APPROVED", substring = true).assertExists()
    }

    // ---------- P4 promotion "what changed" copy ----------
    @Test fun p4_promotionLineNamesTheRealRouteLength() {
        // route length is 5 + rankIndex, capped at 9 — the only per-rank change that's coded
        assertTrue(promotionPerkLine(0).contains("5 cities"))
        assertTrue(promotionPerkLine(1).let { it.contains("6 cities") && it.contains(GameData.ranks[1]) })
        assertTrue("top rank caps at 9 cities", promotionPerkLine(4).contains("9 cities"))
    }
}
