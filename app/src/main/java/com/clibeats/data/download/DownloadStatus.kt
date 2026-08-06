package com.clibeats.data.download

import java.io.File

sealed interface DownloadStatus {
    data object Idle : DownloadStatus

    data class Downloading(val progressPercent: Int) : DownloadStatus

    data class Completed(val file: File) : DownloadStatus

    data class Failed(val message: String) : DownloadStatus
}
