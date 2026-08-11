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
        // save() writes off-thread (see SaveStore) — awaitPendingWrites() is the test-only way
        // to know the file is actually there before reading it back.
        val writer = SaveStore(ctx)
        writer.save(vm.snapshot("p1", 500L))
        writer.awaitPendingWrites()

        // a brand-new store instance reads it straight back from disk
        val reloaded = SaveStore(ctx).load("p1")
        assertNotNull(reloaded)
        assertEquals("state persisted to file intact", vm.snapshot("p1", 500L).state, reloaded!!.state)

        SaveStore(ctx).delete("p1")
        assertNull("delete removes the file", SaveStore(ctx).load("p1"))
    }

    /** MainActivity.onStop() calls vm.flushPendingSaves() to close the small data-loss window an
     *  abrupt process kill could otherwise catch mid-write (autosave() itself is fire-and-forget
     *  — see ClaraViewModel/SaveStore). This drives that exact call, not the test-only
     *  awaitPendingWrites() helper, to prove the production flush path actually blocks until the
     *  write lands: without it, reading the file back right after toggleSound() would be racing
     *  the background writer and could see the pre-toggle value. */
    @Test fun flushPendingSavesMakesTheLatestActionVisibleOnDiskImmediately() {
        val store = SaveStore(ctx)
        val vm = ClaraViewModel().apply {
            bindRepository(store) { 700L }
            signOn("Flusher")
        }
        assertEquals(false, vm.s.expansionUnlocked)
        vm.unlockExpansion()   // flips s.expansionUnlocked and calls the private autosave() internally
        assertEquals("in-memory state flips immediately", true, vm.s.expansionUnlocked)

        vm.flushPendingSaves()   // the exact call onStop() makes

        val onDisk = SaveStore(ctx).load(vm.savedGames().first().id)
        assertNotNull("a flush must leave a readable file behind", onDisk)
        assertEquals("the write visible right after flush reflects the latest action, not a stale value",
            true, onDisk!!.state.expansionUnlocked)
    }

    @Test fun listSortsNewestFirstAndSkipsCorruptFiles() {
        val store = SaveStore(ctx)
        val vm = ClaraViewModel().apply { signOn("A") }
        store.save(vm.snapshot("old", 1L))
        store.save(vm.snapshot("new", 9L))
        store.awaitPendingWrites()
        // a corrupt file must be skipped, not crash the picker
        File(File(ctx.filesDir, "saves"), "save-broken.json").writeText("{ not valid json")

        assertEquals(listOf("new", "old"), store.list().map { it.id })
    }
}
