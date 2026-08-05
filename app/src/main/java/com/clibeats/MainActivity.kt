package com.clibeats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.clibeats.presentation.layout.MainLayout
import com.clibeats.presentation.layout.NavDestination
import com.clibeats.presentation.theme.CliBeatsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CliBeatsTheme {
                var selectedDestination by remember {
                    mutableStateOf<NavDestination>(NavDestination.Home)
                }
                MainLayout(
                    selectedDestination = selectedDestination,
                    onDestinationSelected = { selectedDestination = it },
                )
            }
        }
    }
}
