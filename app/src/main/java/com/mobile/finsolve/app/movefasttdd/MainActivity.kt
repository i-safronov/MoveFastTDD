package com.mobile.finsolve.app.movefasttdd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.navigator.Navigator
import com.mobile.finsolve.app.movefasttdd.presentation.setup.SetupScreen
import com.mobile.finsolve.app.movefasttdd.ui.theme.MoveFastTDDTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoveFastTDDTheme {
                Navigator(SetupScreen())
            }
        }
    }
}
