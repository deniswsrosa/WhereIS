package com.acme.clara

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.ui.TutorialCoach
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the tutorial coach-mark on the JVM and checks the tip, GOT IT, and SKIP. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TutorialCoachTest {

    @get:Rule val compose = createComposeRule()

    @Test fun theCoachShowsTheFirstStepAndSkipEndsIt() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }   // starts the guided case at step 0
        compose.setContent { TutorialCoach(vm) }

        compose.onNodeWithText("Interpol needs you. Read the case, then tap BEGIN.").assertIsDisplayed()
        compose.onNodeWithText("TUTORIAL   1 / 7").assertIsDisplayed()

        compose.onNodeWithText("SKIP").performClick()
        assertEquals("SKIP ends the tutorial", -1, vm.s.tutorialStep)
    }

    @Test fun gotItDismissesTheCurrentTip() {
        val vm = ClaraViewModel().apply { signOn("Rookie") }
        compose.setContent { TutorialCoach(vm) }

        compose.onNodeWithText("GOT IT").assertIsDisplayed()
        compose.onNodeWithText("GOT IT").performClick()
        compose.onNodeWithText("GOT IT").assertDoesNotExist()   // tip dismissed for this step
    }
}
