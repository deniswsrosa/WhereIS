package com.acme.clara

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

class MainActivity : ComponentActivity() {
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
        setContent { ClaraApp() }
    }

    // Schedule the "come back" reminder when the app leaves the foreground; cancel it on return.
    // Also pause/resume the title theme with the lifecycle so it never plays in the background.
    override fun onStart() { super.onStart(); WelcomeBackNotifier.cancel(this); GameSound.resumeTheme() }
    override fun onStop() {
        super.onStop()
        GameSound.pauseTheme()
        if (Reminders.enabled(this)) WelcomeBackNotifier.schedule(this)   // on by default
    }
}

@Composable
fun ClaraApp() {
    val vm: ClaraViewModel = viewModel()
    val context = LocalContext.current
    // Bind persistence and decide the launch: 0 saves → sign-on, 1 → continue, 2+ → picker.
    LaunchedEffect(Unit) {
        com.acme.clara.game.Humor.init(context)
        val store = SaveStore(context)
        vm.bindRepository(store)
        when (val outcome = decideLaunch(store.list())) {
            is LaunchOutcome.SignOn -> Unit
            is LaunchOutcome.Continue -> store.load(outcome.id)?.let { vm.resume(it) }
            is LaunchOutcome.Choose -> vm.toChooseGame()
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
