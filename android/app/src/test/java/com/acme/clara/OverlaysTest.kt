package com.acme.clara

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.acme.clara.data.AlmanacFlags
import com.acme.clara.data.CityMeta
import com.acme.clara.data.Expansion
import com.acme.clara.data.Expansion2
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Overlay
import com.acme.clara.ui.OverlayHost
import com.acme.clara.ui.Virtual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        // The grid is lazy, so assert on an expansion entry near the top of the list.
        compose.onNodeWithText("Abu Simbel").assertExists()
    }

    @Test fun freeDatabaseUnlocksEveryCardFromAnOriginalCountry() {
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.openOverlay(Overlay.Almanac)
        host(vm)

        // Cairo makes Egypt available, including the separate Abu Simbel postcard.
        compose.onNodeWithContentDescription("Abu Simbel").assertExists()
        // Albania has no original-30 destination and remains visibly locked.
        compose.onNodeWithContentDescription("Albania, locked").assertExists()
    }

    @Test fun worldDatabaseUsesPersistentCornerNavigation() {
        val vm = ClaraViewModel().apply { signOn("Ada") }
        vm.openOverlay(Overlay.Almanac)
        host(vm)

        compose.onNodeWithContentDescription("CLOSE WORLD DATABASE").assertIsDisplayed()
        compose.onNodeWithText("Athens").performClick()
        compose.onNodeWithText("◀ BACK").assertIsDisplayed()
        compose.onNodeWithContentDescription("CLOSE WORLD DATABASE").performClick()
        assertEquals(null, vm.s.overlay)
    }

    @Test fun everyWorldDatabaseEntryHasABundledFlag() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bundled = context.assets.list("sprites/flags").orEmpty().toSet()
        val names = CityMeta.all.keys + Expansion.byName.keys + Expansion2.byName.keys
        assertEquals(231, names.size)
        names.forEach { name ->
            val asset = AlmanacFlags.assetName(name)
            assertNotNull("No flag mapping for $name", asset)
            assertTrue("Missing bundled asset for $name ($asset)", "$asset.png" in bundled)
        }
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
