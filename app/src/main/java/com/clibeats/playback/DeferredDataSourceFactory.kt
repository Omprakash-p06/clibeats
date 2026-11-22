package com.clibeats.playback

import androidx.media3.datasource.DataSource

/**
 * Builds its delegate on the first [createDataSource] call instead of at construction time.
 *
 * Used for the song cache: opening it scans the cache directory and its database, which must not
 * happen while Hilt builds the player on the main thread during app start.
 */
class DeferredDataSourceFactory(
    private val provider: () -> DataSource.Factory,
) : DataSource.Factory {
    private val delegate: DataSource.Factory by lazy(LazyThreadSafetyMode.SYNCHRONIZED, provider)

    override fun createDataSource(): DataSource = delegate.createDataSource()
}
