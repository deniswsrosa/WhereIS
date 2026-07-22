package com.acme.carmen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acme.carmen.game.CarmenViewModel
import com.acme.carmen.game.Phase
import com.acme.carmen.ui.screens.*
import com.acme.carmen.ui.theme.Vga

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CarmenApp() }
    }
}

@Composable
fun CarmenApp() {
    val vm: CarmenViewModel = viewModel()
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().background(Vga.Black).windowInsetsPadding(WindowInsets.systemBars).imePadding()
    ) {
        when (vm.s.phase) {
            Phase.TITLE -> TitleScreen(vm)
            Phase.SIGN_ON -> SignOnScreen(vm)
            Phase.BRIEFING -> BriefingScreen(vm)
            Phase.CITY -> CityScreen(vm)
            Phase.TRAVEL -> TravelScreen(vm)
            Phase.CRIME -> CrimeScreen(vm)
            Phase.RESULT -> ResultScreen(vm)
        }
    }
}
