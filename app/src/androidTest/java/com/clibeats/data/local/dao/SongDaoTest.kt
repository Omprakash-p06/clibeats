package com.clibeats.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clibeats.data.local.CliBeatsDatabase
import com.clibeats.data.local.entity.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongDaoTest {
    private lateinit var db: CliBeatsDatabase
    private lateinit var dao: SongDao

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                CliBeatsDatabase::class.java,
            ).allowMainThreadQueries().build()
        dao = db.songDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun testSong(id: String = "s1") =
        SongEntity(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L,
            artworkUrl = null,
            streamUrl = null,
            providerId = "local",
        )

    @Test
    fun upsertAndGetById() =
        runTest {
            dao.upsert(testSong("s1"))
            val result = dao.getById("s1")
            assertNotNull(result)
            assertEquals("Song s1", result?.title)
        }

    @Test
    fun getById_returnsNullWhenMissing() =
        runTest {
            assertNull(dao.getById("missing"))
        }

    @Test
    fun upsertReplacesExistingOnConflict() =
        runTest {
            dao.upsert(testSong("s1"))
            dao.upsert(testSong("s1").copy(title = "Updated"))
            assertEquals("Updated", dao.getById("s1")?.title)
        }

    @Test
    fun getAllAsFlow_returnsAllSongs() =
        runTest {
            dao.upsert(testSong("s1"))
            dao.upsert(testSong("s2"))
            val songs = dao.getAllAsFlow().first()
            assertEquals(2, songs.size)
        }

    @Test
    fun deleteById_removesSong() =
        runTest {
            dao.upsert(testSong("s1"))
            dao.deleteById("s1")
            assertNull(dao.getById("s1"))
        }

    @Test
    fun searchAsFlow_findsMatchingTitles() =
        runTest {
            dao.upsert(testSong("s1").copy(title = "Rock Anthem"))
            dao.upsert(testSong("s2").copy(title = "Jazz Ballad"))
            val results = dao.searchAsFlow("Rock").first()
            assertEquals(1, results.size)
            assertEquals("Rock Anthem", results.first().title)
        }
}
