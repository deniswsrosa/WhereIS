package com.acme.carmen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acme.carmen.game.CarmenViewModel
import com.acme.carmen.game.Phase
import com.acme.carmen.ui.screens.*
import com.acme.carmen.ui.theme.Vga

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw edge-to-edge so the soft keyboard is reported as an inset instead of
        // resizing (shrinking) the whole window. The HQ printer screen reads that inset
        // to pan itself up at full size, rather than collapsing into a tiny box.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CarmenApp() }
    }
}

@Composable
fun CarmenApp() {
    val vm: CarmenViewModel = viewModel()
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
        }
    }
}
