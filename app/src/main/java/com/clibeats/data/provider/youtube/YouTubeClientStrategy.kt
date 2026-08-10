@file:Suppress("MaxLineLength")

package com.clibeats.data.provider.youtube

data class YouTubeClientConfig(
    val name: String,
    val clientName: String,
    val clientVersion: String,
    val userAgent: String,
    val osName: String = "Android",
    val osVersion: String = "11",
    val deviceModel: String = "CliBeats",
)

object YouTubeClientStrategy {
    val PRIMARY =
        YouTubeClientConfig(
            name = "ANDROID_MUSIC",
            clientName = "ANDROID_MUSIC",
            clientVersion = "6.42.52",
            userAgent = "com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 11; gts4lvw)",
            osName = "Android",
            osVersion = "11",
        )

    val SECONDARY =
        YouTubeClientConfig(
            name = "IOS",
            clientName = "IOS",
            clientVersion = "19.22.3",
            userAgent = "com.google.ios.youtube/19.22.3 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X)",
            osName = "iOS",
            osVersion = "17.5.1",
        )

    val TERTIARY =
        YouTubeClientConfig(
            name = "WEB_REMIX",
            clientName = "WEB_REMIX",
            clientVersion = "1.20240618.01.00",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            osName = "Windows",
            osVersion = "10.0",
        )

    val QUATERNARY =
        YouTubeClientConfig(
            name = "TVHTML5",
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            userAgent = "Mozilla/5.0 (SmartHub; SMART-TV; U; Linux/SmartTV) AppleWebKit/537.42",
            osName = "SmartTV",
            osVersion = "1.0",
        )

    val FALLBACK_CHAIN = listOf(PRIMARY, SECONDARY, TERTIARY, QUATERNARY)
}
