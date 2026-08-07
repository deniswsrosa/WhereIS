package com.acme.clara

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.acme.clara.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [Strings.init]'s cold-start path: English loads synchronously, a non-English catalog loads on a
 * background thread and only lands once that thread's completion runnable is drained through the
 * (Robolectric-paused-by-default) main-thread Looper — so asserting "not applied yet" right after
 * [Strings.init] returns is deterministic, not a race, regardless of how fast the real parse is.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StringsAsyncTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun savePreferredLanguage(code: String) {
        ctx.getSharedPreferences("clara_settings", Context.MODE_PRIVATE)
            .edit().putString("language", code).apply()
    }

    /** Pumps the main Looper (where the background load's completion runnable is queued) until
     *  [Strings.ready] flips true, or fails after a generous timeout. */
    private fun awaitReady(timeoutMs: Long = 5000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!Strings.ready) {
            shadowOf(Looper.getMainLooper()).idle()
            if (Strings.ready) return
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("Strings never became ready within ${timeoutMs}ms")
            }
            Thread.sleep(5)
        }
    }

    @Test fun englishColdStartIsReadySynchronously() {
        savePreferredLanguage("en")
        Strings.init(ctx)

        // No background load involved: the object is immediately usable, exactly like before.
        assertTrue(Strings.ready)
        assertEquals("en", Strings.language)
    }

    @Test fun nonEnglishColdStartFallsBackToEnglishUntilTheBackgroundLoadLands() {
        savePreferredLanguage("pt")
        val revisionBefore = Strings.revision

        Strings.init(ctx)

        // Resolved immediately (cheap, synchronous), but the catalog itself hasn't landed —
        // this is exactly the gate MainActivity's ClaraApp() checks before rendering any screen.
        assertEquals("pt", Strings.language)
        assertFalse("catalog parse is still in flight on a background thread", Strings.ready)
        // Every lookup must degrade to the English fallback while `active` is still empty —
        // never a flash of raw ids or an exception.
        assertEquals("Cold cases", Strings.ui("Cold cases"))
        assertEquals(revisionBefore, Strings.revision)

        awaitReady()

        // Once the background parse lands, the real translation takes over and recomposition
        // is signaled via revision.
        assertTrue(Strings.ready)
        assertEquals("Casos arquivados", Strings.ui("Cold cases"))
        assertTrue("revision must bump so Compose re-renders with real translations",
            Strings.revision > revisionBefore)
    }
}
