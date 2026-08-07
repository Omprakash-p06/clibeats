package com.clibeats.data.provider.api

object SignatureTimestamp {
    private var cachedTimestamp: Int = 19842

    fun getTimestamp(): Int = cachedTimestamp

    fun updateTimestamp(timestamp: Int) {
        if (timestamp > 0) {
            cachedTimestamp = timestamp
        }
    }
}
