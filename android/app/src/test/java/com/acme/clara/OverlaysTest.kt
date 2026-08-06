package com.acme.clara

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Overlay
import com.acme.clara.ui.OverlayHost
import com.acme.clara.ui.Virtual
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Compose-UI coverage for the menu overlays, rendered through OverlayHost on the JVM. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OverlaysTest {

    @get:Rule val compose = createComposeRule()

    private fun host(vm: ClaraViewModel) {
        compose.setContent {
            val v = Virtual(2.dp, LocalDensity.current)
            Box(Modifier.fillMaxSize()) { OverlayHost(v, vm) }
        }
    }

    @Test fun worldDatabaseListsPlaces() {
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.openOverlay(Overlay.Almanac)
        host(vm)
        compose.onNodeWithText("WORLD DATABASE").assertIsDisplayed()
        // The grid is lazy, so assert on an entry near the top of the alphabetical list
        // (an expansion place — listed even on the free tier, just locked).
        compose.onNodeWithText("Abu Simbel").assertExists()
    }

    @Test fun hintOverlayShowsAHint() {
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.requestHint()
        host(vm)
        compose.onNodeWithText("HINT").assertIsDisplayed()
    }

    @Test fun mostWantedBoardRenders() {
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.openOverlay(Overlay.MostWanted)
        host(vm)
        compose.onNodeWithText("MOST WANTED").assertIsDisplayed()
    }

    @Test fun commendationsShowStats() {
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.openOverlay(Overlay.Commendations)
        host(vm)
        compose.onNodeWithText("COMMENDATIONS").assertIsDisplayed()
    }
}
