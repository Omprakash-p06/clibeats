package com.clibeats.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.clibeats.domain.model.AudioDeviceType
import com.clibeats.domain.model.AudioOutputDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioOutputMonitor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        var onDeviceChanged: (() -> Unit)? = null
        private val audioManager =
            runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull()
        private val _currentDevice = MutableStateFlow(determineCurrentOutputDevice())
        val currentDevice: StateFlow<AudioOutputDevice> = _currentDevice.asStateFlow()

        init {
            registerDeviceCallback()
        }

        private fun registerDeviceCallback() {
            runCatching {
                val am = audioManager ?: return@runCatching
                val looper = Looper.getMainLooper() ?: return@runCatching
                val callback =
                    object : AudioDeviceCallback() {
                        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                            refreshDevice()
                        }

                        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                            refreshDevice()
                        }
                    }
                am.registerAudioDeviceCallback(callback, Handler(looper))
            }
        }

        fun refreshDevice() {
            _currentDevice.value = determineCurrentOutputDevice()
            onDeviceChanged?.invoke()
        }

        private fun determineCurrentOutputDevice(): AudioOutputDevice =
            runCatching {
                val devices =
                    audioManager?.let { am ->
                        runCatching { am.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull()
                    } ?: emptyArray()

                val priorityOrder =
                    listOf(
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_USB_HEADSET,
                        AudioDeviceInfo.TYPE_USB_DEVICE,
                    )

                val accessory = priorityOrder.firstNotNullOfOrNull { type -> devices.firstOrNull { it.type == type } }
                val bleAccessory =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        devices.firstOrNull { isBleOutput(it) }
                    } else {
                        null
                    }

                val targetDevice = accessory ?: bleAccessory
                if (targetDevice != null) {
                    mapAudioDeviceInfo(targetDevice)
                } else {
                    AudioOutputDevice("Phone speaker", AudioDeviceType.PHONE_SPEAKER)
                }
            }.getOrElse {
                AudioOutputDevice("Phone speaker", AudioDeviceType.PHONE_SPEAKER)
            }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun isBleOutput(device: AudioDeviceInfo): Boolean =
            device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                device.type == AudioDeviceInfo.TYPE_BLE_BROADCAST

        private fun mapAudioDeviceInfo(device: AudioDeviceInfo): AudioOutputDevice {
            val rawName = device.productName?.toString()?.takeIf { it.isNotBlank() }
            val name = rawName ?: defaultNameForType(device.type)
            val type = classifyDeviceType(device.type, name)
            return AudioOutputDevice(name = name, type = type)
        }

        private fun defaultNameForType(type: Int): String =
            when (type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                -> "Bluetooth Audio"
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
                else -> "Audio Output"
            }

        private fun classifyDeviceType(
            deviceType: Int,
            name: String,
        ): AudioDeviceType {
            val lower = name.lowercase(Locale.US)
            return when {
                deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    deviceType == AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.WIRED_HEADSET

                lower.contains("bud") || lower.contains("pod") || lower.contains("ear") -> AudioDeviceType.EARBUDS

                lower.contains("speaker") || lower.contains("soundbar") || lower.contains("box") ->
                    AudioDeviceType.BLUETOOTH_SPEAKER

                deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.HEADPHONES

                deviceType == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.PHONE_SPEAKER

                else -> AudioDeviceType.OTHER
            }
        }
    }
