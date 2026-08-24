package com.acme.clara

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.acme.clara.notify.Reminders
import com.acme.clara.notify.WelcomeBackNotifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acme.clara.audio.GameSound
import com.acme.clara.audio.HapticEngine
import com.acme.clara.game.ClaraViewModel
import com.acme.clara.game.Haptics
import com.acme.clara.game.Phase
import com.acme.clara.save.LaunchOutcome
import com.acme.clara.save.SaveStore
import com.acme.clara.save.decideLaunch
import com.acme.clara.ui.screens.*
import com.acme.clara.ui.theme.Vga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // Same ViewModelStoreOwner (this Activity) as the ClaraApp() composable's own viewModel()
    // call below, so this always resolves to the identical instance — a second handle used only
    // to reach flushPendingSaves() from the Activity lifecycle, which Compose's LaunchedEffect
    // scoping doesn't observe directly.
    private val vm: ClaraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw edge-to-edge so the soft keyboard is reported as an inset instead of
        // resizing (shrinking) the whole window. The HQ printer screen reads that inset
        // to pan itself up at full size, rather than collapsing into a tiny box.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Ask once for notification permission so the welcome-back reminder can show (Android 13+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        com.acme.clara.i18n.Strings.init(this)
        setContent { ClaraApp() }
    }

    // Schedule the "come back" reminder when the app leaves the foreground; cancel it on return.
    // Also pause/resume the title theme with the lifecycle so it never plays in the background.
    override fun onStart() { super.onStart(); WelcomeBackNotifier.cancel(this); GameSound.resumeTheme() }
    override fun onStop() {
        super.onStop()
        GameSound.pauseTheme()
        if (Reminders.enabled(this)) WelcomeBackNotifier.schedule(this)   // on by default
        // autosave() writes off the main thread now (see ClaraViewModel/SaveStore) so gameplay
        // never blocks on disk I/O — but that leaves a small window where the most recent write
        // is still in flight if the process dies right after. A real backgrounding always reaches
        // onStop() first, so flushing here (bounded: at most one small pending write) closes that
        // window for the actual "app goes away" path, without reintroducing a stall during play.
        vm.flushPendingSaves()
    }
}

@Composable
fun ClaraApp() {
    val vm: ClaraViewModel = viewModel()
    val context = LocalContext.current
    // Bind persistence and decide the launch: 0 saves → sign-on, 1 → continue, 2+ → picker.
    LaunchedEffect(Unit) {
        // Bind storage before the heavier humor-corpus parse. Even an unusually fast player who
        // skips the intro while that parse is running must get a durable sign-on draft marker.
        val store = SaveStore(context)
        vm.bindRepository(store)
        // Humor.init() parses a several-hundred-KB JSON asset (up to ~680KB for the largest
        // language) — off the main thread for the same reason as the save list/load below.
        withContext(Dispatchers.IO) { com.acme.clara.game.Humor.init(context) }
        // list()/load() are synchronous file reads (SaveStore) — off the main thread so a cold
        // start with several saved careers can't stall the first frame.
        val outcome = withContext(Dispatchers.IO) {
            decideLaunch(store.list(), pendingSignOn = store.hasPendingSignOn())
        }
        when (outcome) {
            // Debug builds skip sign-on on a fresh install (no save yet) and land straight at
            // the hideout doorstep — chase/result testing shouldn't need playing through
            // name-entry and the whole clue-gathering loop on every reinstall.
            is LaunchOutcome.SignOn -> if (BuildConfig.DEBUG) vm.devAutoStart()
            is LaunchOutcome.Continue -> {
                val data = withContext(Dispatchers.IO) { store.load(outcome.id) }
                data?.let { vm.resume(it) }
            }
            is LaunchOutcome.Choose -> vm.toChooseGame()
            is LaunchOutcome.PendingSignOn -> vm.restorePendingSignOn()
        }
    }
    // Reload the (per-language) humor corpus whenever the language changes — off the main
    // thread, same reasoning as the cold-start Humor.init() above.
    LaunchedEffect(com.acme.clara.i18n.Strings.revision) {
        withContext(Dispatchers.IO) { com.acme.clara.game.Humor.reload(context) }
    }
    // World Campaign entitlement: connect once and reconcile with Play — grant a fresh buy/restore
    // or revoke only when a successful query confirms it is no longer owned. Failed and offline
    // queries preserve the last verified state. Gated on SALES_ENABLED too, not just
    // the purchase-CTA UI: without this, queryExistingPurchases() could silently grant entitlement
    // to a Play Console license-test account the moment the product is created for testing, even
    // in a build that's supposed to be fully dark until the switch flips.
    LaunchedEffect(Unit) {
        if (com.acme.clara.billing.BillingManager.SALES_ENABLED) {
            com.acme.clara.billing.BillingManager.connect(
                context,
                onOwnershipChanged = vm::reconcileExpansionOwnership,
            )
        }
    }
    // Keep the audio engine's mute state in sync with the Options > Sound toggle.
    LaunchedEffect(vm.s.soundOn) { GameSound.setEnabled(context, vm.s.soundOn) }
    // The title theme plays over the intro and title only; it stops the moment you sit down at
    // the HQ printer (sign-on), where the teletype clatter takes over, and stays off in-game.
    LaunchedEffect(vm.s.phase, vm.s.soundOn) {
        when (vm.s.phase) {
            Phase.INTRO, Phase.TITLE -> GameSound.startTheme(context)
            else -> GameSound.stopTheme()
        }
    }
    // Event stingers: the ViewModel emits a (seq, cue) pair at each game moment; the seq
    // makes repeats distinct so this re-fires even for the same cue twice running.
    LaunchedEffect(vm.soundCue) {
        vm.soundCue?.let {
            GameSound.play(context, it.second)
            HapticEngine.play(context, Haptics.forCue(it.second), vm.s.hapticsOn)
        }
    }
    // Note: only systemBars are padded here — the IME inset is deliberately NOT consumed,
    // so opening the keyboard never shrinks the 320x200 canvas. Screens that host a text
    // field (the HQ printer) handle the keyboard themselves by panning up.
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().background(Vga.Black).windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Re-render the whole tree when the language changes so every Strings.get() re-reads.
        androidx.compose.runtime.key(com.acme.clara.i18n.Strings.revision) {
            when (vm.s.phase) {
                Phase.INTRO -> IntroScreen(vm)
                Phase.TITLE -> TitleScreen(vm)
                Phase.SIGN_ON -> SignOnScreen(vm)
                Phase.BRIEFING -> BriefingScreen(vm)
                Phase.CITY -> CityScreen(vm)
                Phase.TRAVEL -> TravelScreen(vm)
                Phase.CRIME -> CrimeScreen(vm)
                Phase.CHASE -> ChaseScreen(vm)
                Phase.RESULT -> ResultScreen(vm)
                Phase.CHOOSE_GAME -> ChooseGameScreen(vm)
            }
            com.acme.clara.ui.CaptionOverlay(vm)
        }
    }
}
