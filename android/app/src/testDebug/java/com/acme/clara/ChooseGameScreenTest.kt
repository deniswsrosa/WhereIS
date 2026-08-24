package com.acme.clara

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Phase
import com.acme.clara.save.InMemorySaveRepository
import com.acme.clara.ui.screens.ChooseGameScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Compose-UI test for the "Choose a game" picker, run on the JVM via Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChooseGameScreenTest {

    @get:Rule val compose = createComposeRule()

    @Test fun listsEverySavedCareerAndStartsANewGame() {
        val repo = InMemorySaveRepository()
        ClaraViewModel().apply { attachSave(repo, "a") { 1L }; signOn("Ada") }
        ClaraViewModel().apply { attachSave(repo, "b") { 2L }; signOn("Grace") }
        val vm = ClaraViewModel().apply { bindRepository(repo) { 0L } }

        compose.setContent { ChooseGameScreen(vm) }

        compose.onNodeWithText("Ada").assertIsDisplayed()
        compose.onNodeWithText("Grace").assertIsDisplayed()

        compose.onNodeWithText("+  NEW DETECTIVE").performClick()
        assertEquals("New Game routes to sign-on for a fresh career", Phase.SIGN_ON, vm.s.phase)
    }

    @Test fun emptyPickerOffersSafeNewDetectivePath() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { bindRepository(repo); toChooseGame() }
        compose.setContent { ChooseGameScreen(vm) }

        compose.onNodeWithText("No saved games yet.").assertIsDisplayed()
        compose.onNodeWithText("+  NEW DETECTIVE").performClick()

        assertEquals(Phase.SIGN_ON, vm.s.phase)
        assertEquals(true, repo.hasPendingSignOn())
    }
}
