package com.clibeats.theme

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.clibeats.presentation.component.PlayerBar
import com.clibeats.presentation.theme.CliBeatsTheme
import org.junit.Rule
import org.junit.Test

class PlayerBarScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun playerBar_idleState() {
        paparazzi.snapshot {
            CliBeatsTheme {
                PlayerBar(
                    trackTitle = "Not playing",
                    artist = "",
                    isPlaying = false,
                    progress = 0f,
                )
            }
        }
    }

    @Test
    fun playerBar_playingState() {
        paparazzi.snapshot {
            CliBeatsTheme {
                PlayerBar(
                    trackTitle = "Midnight City",
                    artist = "M83",
                    isPlaying = true,
                    progress = 0.4f,
                )
            }
        }
    }
}
