package com.acme.clara

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Phase
import com.acme.clara.save.InMemorySaveRepository
import com.acme.clara.ui.screens.ChooseGameScreen
import com.acme.clara.ui.screens.ResultScreen
import com.acme.clara.ui.screens.SignOnScreen
import com.acme.clara.ui.screens.TitleScreen
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

    @Test fun tappingTitleArtResumesTheFinishedCaseAfterSavingAndExiting() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply {
            bindRepository(repo) { 1L }
            signOn("Ada")
            devJumpToResultWin(promotion = true)
            resolvePromotion(true)
            leaveCaseResult()
        }
        compose.setContent {
            when (vm.s.phase) {
                Phase.TITLE -> TitleScreen(vm, launchReady = true)
                Phase.RESULT -> ResultScreen(vm)
                else -> Unit
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Continue").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onRoot().performTouchInput { click(center) }
        compose.waitUntil(5_000) { vm.s.phase == Phase.RESULT }
        compose.onNodeWithText("New Case").assertIsDisplayed()
        assertEquals(1, vm.s.rankIndex)
        assertEquals(false, vm.s.pendingPromotion)
        compose.onNodeWithText("Save & exit").performClick()
        assertEquals(Phase.TITLE, vm.s.phase)
        assertEquals(1, repo.list().size)
    }

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

    @Test fun tappingTitleWithMultipleSavesLetsThePlayerChooseWhichCareerToResume() {
        val repo = InMemorySaveRepository()
        ClaraViewModel().apply { attachSave(repo, "a") { 1L }; signOn("Ada") }
        ClaraViewModel().apply { attachSave(repo, "b") { 2L }; signOn("Grace") }
        val vm = ClaraViewModel().apply { bindRepository(repo) { 3L }; introDone() }
        compose.setContent {
            when (vm.s.phase) {
                Phase.TITLE -> TitleScreen(vm, launchReady = true)
                Phase.CHOOSE_GAME -> ChooseGameScreen(vm)
                else -> Unit
            }
        }

        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Load Game").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onRoot().performTouchInput { click(center) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Ada").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Grace").assertIsDisplayed()
        compose.onNodeWithText("Ada").performClick()
        compose.waitUntil(5_000) { vm.s.phase == Phase.BRIEFING }
        assertEquals("Ada", vm.s.detectiveName)
    }

    @Test fun enteringARegisteredNameResumesThatCareerWithoutCreatingADuplicate() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply {
            bindRepository(repo) { 1L }
            signOn("Ada")
            beginInvestigation()
            newGameFlow()
        }
        compose.setContent { if (vm.s.phase == Phase.SIGN_ON) SignOnScreen(vm) }

        compose.mainClock.advanceTimeBy(2_000)
        val nameField = compose.onNode(hasSetTextAction())
        nameField.performTextInput("  aDA  ")
        nameField.performImeAction()

        compose.waitUntil(5_000) { vm.s.phase == Phase.CITY }
        assertEquals("Ada", vm.s.detectiveName)
        assertEquals(1, repo.list().size)
        assertEquals(false, repo.hasPendingSignOn())
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
