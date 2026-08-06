package com.acme.clara

import android.content.Context
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Overlay
import com.acme.clara.save.InMemorySaveRepository
import com.acme.clara.ui.OverlayHost
import com.acme.clara.ui.Virtual
import com.acme.clara.ui.VirtualScreen
import com.acme.clara.ui.isReducedMotion
import com.acme.clara.ui.screens.ArrestSlam
import com.acme.clara.ui.screens.ChooseGameScreen
import com.acme.clara.ui.screens.CityPhoto
import com.acme.clara.ui.screens.GameToolbar
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Accessibility behaviour that needs an Android Context / Compose: reduced-motion + TalkBack labels. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityTest {

    @get:Rule val compose = createComposeRule()

    private fun host(block: @Composable BoxScope.(Virtual) -> Unit) {
        compose.setContent { VirtualScreen(content = block) }
    }

    @Test fun reducedMotionDefaultsOffOnAStandardDevice() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        assertFalse(isReducedMotion(ctx))
    }

    @Test fun pickerRowsExposeScreenReaderLabels() {
        val repo = InMemorySaveRepository()
        ClaraViewModel().apply { attachSave(repo, "a") { 1L }; signOn("Ada") }
        ClaraViewModel().apply { attachSave(repo, "b") { 2L }; signOn("Grace") }
        val vm = ClaraViewModel().apply { bindRepository(repo) { 0L } }

        compose.setContent { ChooseGameScreen(vm) }

        compose.onNodeWithContentDescription("Continue Ada").assertIsDisplayed()
        compose.onNodeWithContentDescription("+  NEW DETECTIVE").assertIsDisplayed()
    }

    // ---------- main toolbar: SEE / DEPART / INVESTIGATE / CRIME COMPUTER ----------
    @Test fun toolbarExposesAllFourToolsToTalkBack() {
        val vm = ClaraViewModel().apply { signOn("Tester") }   // rookie route, tools unlocked
        host { v -> GameToolbar(v, vm) }

        compose.onNodeWithContentDescription("SEE").assertExists()
        compose.onNodeWithContentDescription("DEPART").assertExists()
        compose.onNodeWithContentDescription("INVESTIGATE").assertExists()
        compose.onNodeWithContentDescription("CRIME COMPUTER").assertExists()
    }

    @Test fun toolbarSeeLabelSwapsToHideWhileTheDropdownIsOpen() {
        val vm = ClaraViewModel().apply { signOn("Tester") }
        host { v -> GameToolbar(v, vm, seeLabel = "HIDE") }

        compose.onNodeWithContentDescription("HIDE").assertExists()
        compose.onNodeWithContentDescription("SEE").assertDoesNotExist()
    }

    // ---------- a dialog: Game > Quit's Yes/No confirmation ----------
    @Test fun confirmQuitDialogExposesYesAndNoToTalkBack() {
        val vm = ClaraViewModel().apply { signOn("Tester"); openOverlay(Overlay.ConfirmQuit) }
        host { v -> OverlayHost(v, vm) }

        compose.onNodeWithText("Do you really want to quit?").assertIsDisplayed()
        compose.onNodeWithContentDescription("Yes").assertExists()
        compose.onNodeWithContentDescription("No").assertExists()
    }

    // ---------- an image: the S1 arrest slam still surfaces "FILED IN MOST WANTED" even when
    // no captured mugshot sprite is packaged for the unit-test resource set (see LeverUiTest.s1) ----------
    @Test fun arrestSlamStillReadsAsFiledEvenWithoutAMugshotSprite() {
        compose.mainClock.autoAdvance = false
        host { v -> ArrestSlam(v, "Nick Brunch") {} }
        compose.onNodeWithText("FILED IN MOST WANTED").assertExists()
    }

    // ---------- an image: city art is described by place name, not a raw asset filename ----------
    @Test fun cityPhotoIsDescribedByPlaceName() {
        host { v -> CityPhoto("Paris", v, Modifier.size(v.w(50), v.w(50))) }
        compose.onNodeWithContentDescription("Photo of Paris").assertExists()
    }
}
