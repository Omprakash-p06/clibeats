package com.clibeats.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clibeats.data.local.CliBeatsDatabase
import com.clibeats.data.local.entity.PlaylistEntity
import com.clibeats.data.local.entity.PlaylistSongCrossRef
import com.clibeats.data.local.entity.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {
    private lateinit var db: CliBeatsDatabase
    private lateinit var playlistDao: PlaylistDao
    private lateinit var songDao: SongDao

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                CliBeatsDatabase::class.java,
            ).allowMainThreadQueries().build()
        playlistDao = db.playlistDao()
        songDao = db.songDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addAndGetSongsForPlaylist() =
        runTest {
            val playlist = PlaylistEntity("p1", "Favs", null, null, 1, true, "local", 1000L, 1000L)
            val song = SongEntity("s1", "Title", "Artist", "Album", 180_000L, null, null, "local")
            playlistDao.upsert(playlist)
            songDao.upsert(song)
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("p1", "s1", 0))
            val songs = playlistDao.getSongsForPlaylistAsFlow("p1").first()
            assertEquals(1, songs.size)
            assertEquals("s1", songs.first().id)
        }

    @Test
    fun removeSongFromPlaylist() =
        runTest {
            val playlist = PlaylistEntity("p1", "Favs", null, null, 1, true, "local", 1000L, 1000L)
            val song = SongEntity("s1", "Title", "Artist", "Album", 180_000L, null, null, "local")
            playlistDao.upsert(playlist)
            songDao.upsert(song)
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef("p1", "s1", 0))
            playlistDao.removeSongFromPlaylist("p1", "s1")
            assertEquals(0, playlistDao.getSongsForPlaylistAsFlow("p1").first().size)
        }
}
