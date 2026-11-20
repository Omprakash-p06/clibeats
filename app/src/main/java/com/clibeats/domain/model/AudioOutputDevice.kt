package com.clibeats.domain.model

enum class AudioDeviceType {
    PHONE_SPEAKER,
    WIRED_HEADSET,
    EARBUDS,
    HEADPHONES,
    BLUETOOTH_SPEAKER,
    OTHER,
}

data class AudioOutputDevice(
    val name: String,
    val type: AudioDeviceType,
) {
    val displayIcon: String
        get() =
            when (type) {
                AudioDeviceType.PHONE_SPEAKER -> "\uD83D\uDD08" // 🔈
                AudioDeviceType.WIRED_HEADSET,
                AudioDeviceType.EARBUDS,
                AudioDeviceType.HEADPHONES,
                -> "\uD83C\uDFA7" // 🎧
                AudioDeviceType.BLUETOOTH_SPEAKER,
                AudioDeviceType.OTHER,
                -> "\uD83D\uDD0A" // 🔊
            }
}
