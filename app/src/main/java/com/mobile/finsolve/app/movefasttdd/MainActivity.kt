package com.mobile.finsolve.app.movefasttdd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.Navigator
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerStateDataStore
import com.mobile.finsolve.app.movefasttdd.presentation.setup.SetupScreen
import com.mobile.finsolve.app.movefasttdd.presentation.timer.TimerScreen
import com.mobile.finsolve.app.movefasttdd.presentation.theme.MoveFastTDDTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var timerStateDataStore: TimerStateDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val screens = resolveInitialStack()
            setContent {
                MoveFastTDDTheme {
                    Navigator(screens = screens)
                }
            }
        }
    }

    private suspend fun resolveInitialStack(): List<cafe.adriel.voyager.core.screen.Screen> {
        val hasActiveTimer = timerStateDataStore.load() != null
        return if (hasActiveTimer) listOf(SetupScreen(), TimerScreen)
        else listOf(SetupScreen())
    }
}
