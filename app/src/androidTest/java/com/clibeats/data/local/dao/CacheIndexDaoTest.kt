package com.clibeats.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clibeats.data.local.CliBeatsDatabase
import com.clibeats.data.local.entity.CacheIndexEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CacheIndexDaoTest {
    private lateinit var db: CliBeatsDatabase
    private lateinit var dao: CacheIndexDao

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                CliBeatsDatabase::class.java,
            ).allowMainThreadQueries().build()
        dao = db.cacheIndexDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGetById() =
        runTest {
            val entry =
                CacheIndexEntity(
                    songId = "s1",
                    localPath = "/cache/s1.mp3",
                    fileSizeBytes = 5_000_000L,
                    cachedAt = 1000L,
                    expiresAt = null,
                )
            dao.upsert(entry)
            assertNotNull(dao.getById("s1"))
        }

    @Test
    fun deleteById_removesEntry() =
        runTest {
            dao.upsert(CacheIndexEntity("s1", "/p", 100L, 1000L, null))
            dao.deleteById("s1")
            assertNull(dao.getById("s1"))
        }

    @Test
    fun getTotalCacheSizeBytes_sumsCorrectly() =
        runTest {
            dao.upsert(CacheIndexEntity("s1", "/p1", 1_000L, 1000L, null))
            dao.upsert(CacheIndexEntity("s2", "/p2", 2_000L, 1000L, null))
            assertEquals(3_000L, dao.getTotalCacheSizeBytes())
        }
}
