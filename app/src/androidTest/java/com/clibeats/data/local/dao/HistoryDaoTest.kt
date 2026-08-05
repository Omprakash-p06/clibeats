package com.clibeats.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clibeats.data.local.CliBeatsDatabase
import com.clibeats.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDaoTest {

    private lateinit var db: CliBeatsDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CliBeatsDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.historyDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndGetRecent() = runTest {
        dao.insert(HistoryEntity(songId = "s1", playedAt = 1000L, providerId = "local"))
        dao.insert(HistoryEntity(songId = "s2", playedAt = 2000L, providerId = "local"))
        val history = dao.getRecentAsFlow(10).first()
        assertEquals(2, history.size)
        assertEquals("s2", history.first().songId) // DESC order
    }

    @Test
    fun clearBefore_removesOldEntries() = runTest {
        dao.insert(HistoryEntity(songId = "old", playedAt = 500L, providerId = "local"))
        dao.insert(HistoryEntity(songId = "new", playedAt = 2000L, providerId = "local"))
        dao.clearBefore(1000L)
        val history = dao.getAllAsFlow().first()
        assertEquals(1, history.size)
        assertEquals("new", history.first().songId)
    }

    @Test
    fun clearAll_emptiesTable() = runTest {
        dao.insert(HistoryEntity(songId = "s1", playedAt = 1000L, providerId = "local"))
        dao.clearAll()
        assertTrue(dao.getAllAsFlow().first().isEmpty())
    }
}
