package com.acme.clara

import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ClueKind
import com.acme.clara.save.InMemorySaveRepository
import com.acme.clara.save.Json
import com.acme.clara.save.LaunchOutcome
import com.acme.clara.save.SaveCodec
import com.acme.clara.save.SaveMeta
import com.acme.clara.save.decideLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveTest {

    // ---------- Json codec ----------

    @Test fun jsonRoundTripsNestedStructures() {
        val v = linkedMapOf<String, Any?>(
            "s" to "he said \"hi\"\n\tover a bay",
            "n" to 42L,
            "neg" to -7L,
            "b" to true,
            "nil" to null,
            "list" to listOf("a", 1L, false, null),
            "nested" to linkedMapOf("x" to listOf(1L, 2L, 3L)),
            "unicode" to "café — ☂",
        )
        val text = Json.encode(v)
        assertEquals(v, Json.decode(text))
    }

    @Test fun jsonHandlesEmptyContainers() {
        assertEquals(emptyList<Any?>(), Json.decode(Json.encode(emptyList<Any?>())))
        assertEquals(emptyMap<String, Any?>(), Json.decode(Json.encode(emptyMap<String, Any?>())))
    }

    // ---------- SaveCodec round-trip ----------

    @Test fun codecRoundTripPreservesTheEntireCareerAndCase() {
        val vm = ClaraViewModel().apply { signOn("Ada Lovelace") }
        // play: open a lead, enter a computer trait, so the snapshot is non-trivial
        vm.openVenue(vm.s.venues.indexOfFirst { it.kind != ClueKind.DANGER })
        vm.setComp("hair", vm.s.culprit!!.tHair)

        val snap = vm.snapshot("profile-1", 12345L)
        val text = SaveCodec.encode(snap.meta, snap.state)
        val back = SaveCodec.decode(text)

        assertNotNull(back)
        assertEquals("meta round-trips", snap.meta, back!!.meta)
        assertEquals("full state round-trips", snap.state, back.state)
    }

    @Test fun codecReturnsNullOnGarbage() {
        assertNull(SaveCodec.decode("not json"))
        assertNull(SaveCodec.decode("{}"))            // missing meta/state
        assertNull(SaveCodec.decode("[1,2,3]"))
    }

    // ---------- repository ----------

    @Test fun repositoryStoresListsNewestFirstAndDeletes() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("A") }
        repo.save(vm.snapshot("a", 100L))
        repo.save(vm.snapshot("b", 300L))
        repo.save(vm.snapshot("c", 200L))

        assertEquals(listOf("b", "c", "a"), repo.list().map { it.id })
        assertNotNull(repo.load("b"))
        repo.delete("b")
        assertNull(repo.load("b"))
        assertEquals(2, repo.list().size)
    }

    // ---------- launch decision ----------

    @Test fun launchDecisionMapsSaveCount() {
        assertEquals(LaunchOutcome.SignOn, decideLaunch(emptyList()))

        val one = decideLaunch(listOf(SaveMeta("only", "A", 0, 0, 1L)))
        assertTrue(one is LaunchOutcome.Continue && one.id == "only")

        val choose = decideLaunch(
            listOf(SaveMeta("old", "A", 0, 0, 10L), SaveMeta("new", "B", 0, 0, 99L)),
        )
        assertTrue(choose is LaunchOutcome.Choose)
        assertEquals(listOf("new", "old"), (choose as LaunchOutcome.Choose).metas.map { it.id })
    }

    // ---------- ViewModel autosave + restore ----------

    @Test fun autosavePersistsAfterEveryAction() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Grace") }
        vm.attachSave(repo, "p1") { 7L }

        vm.openVenue(0)
        val loaded = repo.load("p1")
        assertNotNull(loaded)
        assertEquals("p1", loaded!!.meta.id)
        assertEquals(7L, loaded.meta.lastPlayed)
        assertTrue("visit is captured on disk", loaded.state.visited.isNotEmpty())
        assertEquals("disk equals screen", vm.snapshot("p1", 7L).state, loaded.state)
    }

    @Test fun loadCareerRestoresExactly() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Grace") }
        vm.attachSave(repo, "p1") { 7L }
        vm.openVenue(0)
        vm.setComp("sex", vm.s.culprit!!.tSex)
        val saved = repo.load("p1")!!

        val reopened = ClaraViewModel()
        reopened.loadCareer(saved)
        assertEquals(saved.state, reopened.s)
    }

    @Test fun aWrongFlightSurvivesReload_soItCannotBeUndone() {
        // The anti-scum property: fly somewhere wrong, and the mistake is on disk immediately.
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Grace") }
        vm.attachSave(repo, "p1") { 1L }
        val wrong = vm.s.departOptions.first { it != vm.s.route[vm.s.progress + 1] }
        vm.travelTo(wrong); vm.arrive()

        val loaded = repo.load("p1")!!
        assertEquals("landed at the wrong city on disk", wrong, loaded.state.currentCity)
        assertEquals(1, loaded.state.wrongFlights)
    }
}
