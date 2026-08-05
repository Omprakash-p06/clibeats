// ForbiddenImport: data-layer test legitimately imports sibling data packages; Phase 0 pattern is over-broad.
@file:Suppress("ForbiddenImport", "MaxLineLength")

package com.clibeats.data.local.mapper

import com.clibeats.data.local.entity.PlaylistEntity
import com.clibeats.data.local.entity.SongEntity
import com.clibeats.domain.model.Playlist
import com.clibeats.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapperTest {
    private fun testTrack(id: String = "t1") =
        Track(
            id = id,
            title = "Title",
            artist = "Artist",
            album = "Album",
            durationMs = 200_000L,
            artworkUrl = null,
            streamUrl = null,
            providerId = "local",
        )

    private fun testPlaylist(id: String = "p1") =
        Playlist(
            id = id,
            name = "Favorites",
            description = null,
            artworkUrl = null,
            trackCount = 0,
            isOwned = true,
            providerId = "local",
        )

    @Test
    fun trackToEntity_preservesExistingLocalPathAndCachedAt() {
        val entity = testTrack().toEntity(existingLocalPath = "/cache/song.mp3", existingCachedAt = 1_700_000_000_000L)

        assertEquals("/cache/song.mp3", entity.localPath)
        assertEquals(1_700_000_000_000L, entity.cachedAt)
    }

    @Test
    fun trackToEntity_roundTripKeepsPersistedState() {
        val original =
            SongEntity(
                id = "t1",
                title = "Title",
                artist = "Artist",
                album = "Album",
                durationMs = 200_000L,
                artworkUrl = null,
                streamUrl = null,
                providerId = "local",
                localPath = "/cache/song.mp3",
                cachedAt = 1_700_000_000_000L,
            )

        val restored = original.toDomain().toEntity(original.localPath, original.cachedAt)

        assertEquals(original, restored)
    }

    @Test
    fun trackToEntity_defaultsToNullWhenNoExistingState() {
        val entity = testTrack().toEntity()

        assertNull(entity.localPath)
        assertNull(entity.cachedAt)
    }

    @Test
    fun playlistToEntity_preservesExistingCreatedAt() {
        val originalCreatedAt = 1_600_000_000_000L

        val entity = testPlaylist().toEntity(createdAt = originalCreatedAt, updatedAt = 1_700_000_000_000L)

        assertEquals(originalCreatedAt, entity.createdAt)
        assertEquals(1_700_000_000_000L, entity.updatedAt)
    }

    @Test
    fun playlistEntity_roundTripKeepsTimestamps() {
        val original =
            PlaylistEntity(
                id = "p1",
                name = "Favorites",
                description = null,
                artworkUrl = null,
                trackCount = 0,
                isOwned = true,
                providerId = "local",
                createdAt = 1_600_000_000_000L,
                updatedAt = 1_700_000_000_000L,
            )

        val restored = original.toDomain().toEntity(createdAt = original.createdAt, updatedAt = original.updatedAt)

        assertEquals(original, restored)
    }
}
