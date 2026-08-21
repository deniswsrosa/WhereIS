package com.acme.clara

import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.ClueKind
import com.acme.clara.game.GameState
import com.acme.clara.game.Phase
import com.acme.clara.data.GameData
import com.acme.clara.save.InMemorySaveRepository
import com.acme.clara.save.Json
import com.acme.clara.save.LaunchOutcome
import com.acme.clara.save.SaveCodec
import com.acme.clara.save.SaveMeta
import com.acme.clara.save.decideLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun codecMigratesRenamedCitiesInOldSaves() {
        // A pre-rename save refers to the city as "Peking" everywhere; loading it must
        // come back as "Beijing" or route/map/CityMeta lookups would all miss.
        val vm = ClaraViewModel().apply { signOn("Ada Lovelace") }
        val snap = vm.snapshot("profile-1", 12345L)
        val old = SaveCodec.encode(snap.meta, snap.state)
            .replace("\"Beijing\"", "\"Peking\"")   // simulate the old on-disk form
        val back = SaveCodec.decode(old)
        assertNotNull(back)
        val s = back!!.state
        assertFalse("no Peking survives in the route", "Peking" in s.route)
        assertFalse("no Peking in depart options", "Peking" in s.departOptions)
        assertFalse("no Peking in visited places", "Peking" in s.visitedPlaces)
        assertFalse("no Peking in city-last-seen", "Peking" in s.cityLastSeen.keys)
    }

    @Test fun codecLoadsAPreExpansionSaveMissingThisSessionsNewFields() {
        // Simulates a save written before the paid fields existed on disk: strip those keys out
        // of an otherwise-real, valid save and confirm decode() still succeeds with
        // safe defaults rather than crashing or silently corrupting the rest of the state — the
        // exact scenario an existing player's save hits on the first launch after this update.
        val vm = ClaraViewModel().apply { signOn("Ada Lovelace") }
        val snap = vm.snapshot("profile-1", 12345L)
        val fullText = SaveCodec.encode(snap.meta, snap.state)
        val preExpansionText = Json.encode(
            (Json.decode(fullText) as Map<*, *>).let { root ->
                val state = (root["state"] as Map<*, *>).toMutableMap()
                state.remove("expansionUnlocked")
                state.remove("bureauTipUsed")
                state.remove("travelBufferEnabled")
                linkedMapOf("v" to root["v"], "meta" to root["meta"], "state" to state)
            }
        )

        val back = SaveCodec.decode(preExpansionText)
        assertNotNull("an old save without these keys still loads", back)
        assertFalse("missing expansionUnlocked defaults to free tier, not a crash",
            back!!.state.expansionUnlocked)
        assertFalse("missing bureauTipUsed defaults to not-yet-spent",
            back.state.bureauTipUsed)
        assertTrue("missing travelBufferEnabled uses the paid comfort default",
            back.state.travelBufferEnabled)
        // everything else that WAS present still round-trips untouched
        assertEquals("the rest of the career is unaffected",
            snap.state.copy(expansionUnlocked = false, bureauTipUsed = false), back.state)
    }

    @Test fun codecMigratesTheLegacyCase14ClaraCaptureIntoAnEscape() {
        val vm = ClaraViewModel().apply { signOn("Veteran") }
        val clara = GameData.suspects.first { it.name == "Clara San Diego" }
        val snap = vm.snapshot("legacy", 42L)
        val legacyState = snap.state.copy(
            phase = Phase.RESULT,
            casesSolved = GameState.CAREER_CASES,
            culprit = clara,
            won = true,
            careerOver = true,
            capturedVillains = snap.state.capturedVillains + clara.name,
            resultLines = listOf(GameData.CLARA_JAILED),
        )
        val root = (Json.decode(SaveCodec.encode(snap.meta, legacyState)) as Map<*, *>).toMutableMap()
        root["v"] = 1
        val migrated = SaveCodec.decode(Json.encode(root))!!.state
        assertFalse(migrated.careerOver)
        assertFalse(clara.name in migrated.capturedVillains)
        assertTrue(migrated.resultLines.any { it.contains("escaped", ignoreCase = true) ||
            it == GameData.GOT_AWAY })
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

    @Test fun expansionOwnershipIsGlobalAcrossNewAndExistingCareers() {
        val repo = InMemorySaveRepository()
        val buyer = ClaraViewModel().apply {
            bindRepository(repo)
            signOn("Buyer")
            unlockExpansion()
        }
        assertTrue(repo.ownsExpansion())

        val oldFreeSave = ClaraViewModel().apply { signOn("Old profile") }.snapshot("old", 1L)
        repo.save(oldFreeSave)
        val reopened = ClaraViewModel().apply { bindRepository(repo); resumeById("old") }
        assertTrue("an existing free save inherits ownership", reopened.s.expansionUnlocked)

        buyer.newGameFlow()
        buyer.signOn("New profile")
        assertTrue("a new career inherits ownership", buyer.s.expansionUnlocked)
    }

    @Test fun restoreBeforeSignOnPersistsForTheCareerCreatedLater() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { bindRepository(repo); unlockExpansion() }
        vm.newGameFlow()
        vm.signOn("Restored")
        assertTrue(repo.ownsExpansion())
        assertTrue(vm.s.expansionUnlocked)
    }

    @Test fun aLegacyPaidCareerUpgradesOwnershipToGlobal() {
        val repo = InMemorySaveRepository()
        val paidSave = ClaraViewModel().apply { signOn("Early buyer"); unlockExpansion() }
            .snapshot("paid", 1L)
        repo.save(paidSave)
        assertFalse(repo.ownsExpansion())

        val vm = ClaraViewModel().apply { bindRepository(repo); resumeById("paid") }
        assertTrue(vm.s.expansionUnlocked)
        assertTrue("loading a paid save promotes its old per-career flag", repo.ownsExpansion())
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

        assertEquals("an interrupted blank sign-on takes precedence over old careers",
            LaunchOutcome.PendingSignOn,
            decideLaunch(listOf(SaveMeta("old", "A", 0, 0, 10L)), pendingSignOn = true))
    }

    @Test fun unfinishedNewCareerIsDurableButNeverCreatesABlankPickerEntry() {
        val repo = InMemorySaveRepository()
        val old = ClaraViewModel().apply { signOn("Existing") }.snapshot("old", 1L)
        repo.save(old)
        val vm = ClaraViewModel().apply { bindRepository(repo); resumeById("old") }

        vm.newGameFlow()

        assertTrue(repo.hasPendingSignOn())
        assertEquals("the existing career remains the only picker entry", listOf("Existing"),
            repo.list().map { it.name })
        assertEquals(LaunchOutcome.PendingSignOn,
            decideLaunch(repo.list(), repo.hasPendingSignOn()))

        vm.signOnStart("Confirmed")
        assertFalse(repo.hasPendingSignOn())
        assertEquals(setOf("Existing", "Confirmed"), repo.list().map { it.name }.toSet())
        assertEquals("a confirmed printer flow resumes at briefing after process death",
            Phase.BRIEFING, repo.list().first { it.name == "Confirmed" }.let { repo.load(it.id)!!.state.phase })
    }

    @Test fun printerCannotBypassIdentityWithNewCase() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { bindRepository(repo); newGameFlow() }

        vm.menuNewCase()

        assertEquals(Phase.SIGN_ON, vm.s.phase)
        assertTrue(vm.s.route.isEmpty())
        assertTrue(repo.list().isEmpty())
        assertTrue(repo.hasPendingSignOn())
    }

    @Test fun lateRepositoryBindingStillCapturesAnAlreadyOpenedPrinter() {
        val vm = ClaraViewModel().apply { introDone(); start() }
        val repo = InMemorySaveRepository()

        vm.bindRepository(repo)

        assertTrue(repo.hasPendingSignOn())
    }

    @Test fun persistedTransientPhasesResumeAtSafeCompletedScreens() {
        val vm = ClaraViewModel().apply { signOnStart("Printer") }
        assertEquals(Phase.BRIEFING, vm.snapshot("p", 1L).state.phase)

        vm.beginInvestigation()
        vm.gotoTravel()
        assertEquals(Phase.CITY, vm.snapshot("p", 1L).state.phase)

        vm.devJumpToHideoutDoorstep()
        vm.openVenue(vm.s.venues.indices.first { it !in vm.s.visited })
        assertEquals(Phase.CHASE, vm.s.phase)
        assertEquals(Phase.RESULT, vm.snapshot("p", 1L).state.phase)
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

    @Test fun optionsTogglesAutosaveImmediately() {
        // Regression: toggleSound/toggleHaptics/toggleCaptions used to flip the in-memory flag
        // without ever calling autosave(), so a crash right after opening Options and toggling
        // one lost the change on next launch — the same class of bug the autosave/SaveStore work
        // this session was about, just missed on these three call sites.
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { signOn("Grace") }
        vm.attachSave(repo, "p1") { 7L }

        vm.toggleSound()
        assertEquals("sound flip is on disk", vm.s.soundOn, repo.load("p1")!!.state.soundOn)

        vm.toggleHaptics()
        assertEquals("haptics flip is on disk", vm.s.hapticsOn, repo.load("p1")!!.state.hapticsOn)

        vm.toggleCaptions()
        assertEquals("captions flip is on disk", vm.s.captionsOn, repo.load("p1")!!.state.captionsOn)
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

    @Test fun backgroundFlushSnapshotsNavigationThatDidNotAutosaveOnItsOwn() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { attachSave(repo, "p1") { 9L }; signOn("Grace") }
        vm.gotoCrime()

        vm.flushPendingSaves()

        assertEquals(Phase.CRIME, repo.load("p1")!!.state.phase)
    }

    @Test fun openingLoadGameSavesActiveCareerAndPickerCannotOverwriteItsPhase() {
        val repo = InMemorySaveRepository()
        val vm = ClaraViewModel().apply { attachSave(repo, "p1") { 9L }; signOn("Grace") }
        vm.gotoCrime()

        vm.toChooseGame()
        assertEquals(Phase.CHOOSE_GAME, vm.s.phase)
        assertEquals("the career is saved before entering the picker", Phase.CRIME,
            repo.load("p1")!!.state.phase)

        vm.flushPendingSaves()
        assertEquals("backgrounding on the picker does not overwrite the active career",
            Phase.CRIME, repo.load("p1")!!.state.phase)
    }

    @Test fun loadingExistingCareerCancelsInterruptedNewDetectiveDraft() {
        val repo = InMemorySaveRepository()
        val saved = ClaraViewModel().apply { signOn("Existing") }.snapshot("old", 1L)
        repo.save(saved)
        val vm = ClaraViewModel().apply { bindRepository(repo); newGameFlow() }
        assertTrue(repo.hasPendingSignOn())

        vm.toChooseGame()
        vm.resumeById("old")

        assertFalse(repo.hasPendingSignOn())
        assertEquals("Existing", vm.s.detectiveName)
        assertEquals(Phase.BRIEFING, vm.s.phase)
    }

    @Test fun resumeImmediatelyPersistsWelcomeBackBenefitsAndFreshTimestamp() {
        val repo = InMemorySaveRepository()
        val original = ClaraViewModel().apply { signOn("Returner") }.snapshot("p1", 1L)
        repo.save(original)
        val now = 8L * 24 * 60 * 60 * 1000
        val vm = ClaraViewModel().apply { bindRepository(repo) { now }; resumeById("p1") }

        assertEquals(1, vm.s.freeHints)
        assertEquals(1, repo.load("p1")!!.state.freeHints)
        assertEquals(now, repo.load("p1")!!.meta.lastPlayed)
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
