package com.acme.clara

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.save.InMemorySaveRepository
import com.acme.clara.ui.isReducedMotion
import com.acme.clara.ui.screens.ChooseGameScreen
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

    @Test fun reducedMotionDefaultsOffOnAStandardDevice() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        assertFalse(isReducedMotion(ctx))
    }

    @Test fun pickerRowsExposeScreenReaderLabels() {
        val repo = InMemorySaveRepository()
        ClaraViewModel().apply { attachSave(repo, "a") { 1L }; signOn("Ada") }.flushPendingSave()
        ClaraViewModel().apply { attachSave(repo, "b") { 2L }; signOn("Grace") }.flushPendingSave()
        val vm = ClaraViewModel().apply { bindRepository(repo) { 0L } }

        compose.setContent { ChooseGameScreen(vm) }

        compose.onNodeWithContentDescription("Continue Ada").assertIsDisplayed()
        compose.onNodeWithContentDescription("+  NEW DETECTIVE").assertIsDisplayed()
    }
}
