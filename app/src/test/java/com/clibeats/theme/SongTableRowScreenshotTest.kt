package com.clibeats.theme

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.clibeats.presentation.component.SongTableRow
import com.clibeats.presentation.theme.CliBeatsTheme
import org.junit.Rule
import org.junit.Test

class SongTableRowScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun songRow_normalState() {
        paparazzi.snapshot {
            CliBeatsTheme {
                SongTableRow(
                    trackTitle = "Midnight City",
                    artist = "M83",
                    duration = "4:02",
                    index = 1,
                    isNowPlaying = false,
                )
            }
        }
    }

    @Test
    fun songRow_nowPlayingState() {
        paparazzi.snapshot {
            CliBeatsTheme {
                SongTableRow(
                    trackTitle = "Midnight City",
                    artist = "M83",
                    duration = "4:02",
                    index = 1,
                    isNowPlaying = true,
                )
            }
        }
    }

    @Test
    fun songRow_longTitleTruncation() {
        paparazzi.snapshot {
            CliBeatsTheme {
                SongTableRow(
                    trackTitle = "A Very Long Track Title That Should Be Truncated With Ellipsis",
                    artist = "Artist With A Longer Name Than Usual",
                    duration = "10:42",
                    index = 99,
                    isNowPlaying = false,
                )
            }
        }
    }
}
