package com.acme.clara

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.save.SaveStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The file-backed [SaveStore] exercised against a real Android Context via Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RobolectricSaveStoreTest {

    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun careerSurvivesAcrossStoreInstances() {
        val vm = ClaraViewModel().apply { signOn("Disk Jockey") }
        SaveStore(ctx).save(vm.snapshot("p1", 500L))

        // a brand-new store instance reads it straight back from disk
        val reloaded = SaveStore(ctx).load("p1")
        assertNotNull(reloaded)
        assertEquals("state persisted to file intact", vm.snapshot("p1", 500L).state, reloaded!!.state)

        SaveStore(ctx).delete("p1")
        assertNull("delete removes the file", SaveStore(ctx).load("p1"))
    }

    @Test fun listSortsNewestFirstAndSkipsCorruptFiles() {
        val store = SaveStore(ctx)
        val vm = ClaraViewModel().apply { signOn("A") }
        store.save(vm.snapshot("old", 1L))
        store.save(vm.snapshot("new", 9L))
        // a corrupt file must be skipped, not crash the picker
        File(File(ctx.filesDir, "saves"), "save-broken.json").writeText("{ not valid json")

        assertEquals(listOf("new", "old"), store.list().map { it.id })
    }
}
