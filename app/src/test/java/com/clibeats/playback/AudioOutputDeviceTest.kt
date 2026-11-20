package com.clibeats.playback

import com.clibeats.domain.model.AudioDeviceType
import com.clibeats.domain.model.AudioOutputDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioOutputDeviceTest {
    @Test
    fun `icons map correctly to device types`() {
        val speaker = AudioOutputDevice("Phone speaker", AudioDeviceType.PHONE_SPEAKER)
        assertThat(speaker.displayIcon).isEqualTo("\uD83D\uDD08")

        val headphones = AudioOutputDevice("Sony WH-1000XM4", AudioDeviceType.HEADPHONES)
        assertThat(headphones.displayIcon).isEqualTo("\uD83C\uDFA7")

        val earbuds = AudioOutputDevice("Galaxy Buds", AudioDeviceType.EARBUDS)
        assertThat(earbuds.displayIcon).isEqualTo("\uD83C\uDFA7")

        val wired = AudioOutputDevice("Wired Headset", AudioDeviceType.WIRED_HEADSET)
        assertThat(wired.displayIcon).isEqualTo("\uD83C\uDFA7")

        val btSpeaker = AudioOutputDevice("JBL Flip 6", AudioDeviceType.BLUETOOTH_SPEAKER)
        assertThat(btSpeaker.displayIcon).isEqualTo("\uD83D\uDD0A")

        val other = AudioOutputDevice("External DAC", AudioDeviceType.OTHER)
        assertThat(other.displayIcon).isEqualTo("\uD83D\uDD0A")
    }

    @Test
    fun `device equality works as expected`() {
        val dev1 = AudioOutputDevice("Phone speaker", AudioDeviceType.PHONE_SPEAKER)
        val dev2 = AudioOutputDevice("Phone speaker", AudioDeviceType.PHONE_SPEAKER)
        val dev3 = AudioOutputDevice("Sony WH-1000XM4", AudioDeviceType.HEADPHONES)

        assertThat(dev1).isEqualTo(dev2)
        assertThat(dev1).isNotEqualTo(dev3)
    }
}
