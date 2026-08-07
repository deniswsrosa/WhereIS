package com.acme.clara

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.acme.clara.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [Strings.ensureInit] backs callers outside the Activity lifecycle (e.g. [WelcomeBackWorker])
 * that read [Strings.ui]/[Strings.get] immediately afterward on a background thread, with no
 * Compose recomposition around to pick up a later async update. Unlike [Strings.init]'s
 * cold-start path (see [StringsAsyncTest]), it must stay fully synchronous: the correct-language
 * catalog has to be loaded and applied before the call returns.
 *
 * ensureInit is a one-shot guarded by `appCtx == null`, so it'd wrongly no-op if [Strings] were
 * already initialized — which, across a whole suite run, it may be: Robolectric reuses one
 * sandbox/classloader (and so [Strings]' static state) across test classes that share a config,
 * regardless of file. Reflectively clearing `appCtx` first makes this test deterministic
 * independent of suite run order, without adding a test-only reset hook to production code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StringsEnsureInitTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun ensureInitLoadsTheCatalogSynchronously() {
        ctx.getSharedPreferences("clara_settings", Context.MODE_PRIVATE)
            .edit().putString("language", "pt").apply()

        Strings.javaClass.getDeclaredField("appCtx").apply { isAccessible = true }.set(Strings, null)
        Strings.ensureInit(ctx)

        assertTrue("no background thread to wait on: the catalog must be loaded by the time " +
            "ensureInit() returns", Strings.ready)
        assertEquals("Casos arquivados", Strings.ui("Cold cases"))
    }
}
