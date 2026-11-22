package com.clibeats.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class CliBeatsDatabaseMigrationTest {
    @Test
    fun `migration 3 to 4 creates downloaded_tracks and lyrics_cache and history index`() {
        val db: SupportSQLiteDatabase = mock()

        CliBeatsDatabase.MIGRATION_3_4.migrate(db)

        verify(db, times(3)).execSQL(
            check { sql ->
                assert(
                    sql.contains("CREATE TABLE IF NOT EXISTS `downloaded_tracks`") ||
                        sql.contains("CREATE TABLE IF NOT EXISTS `lyrics_cache`") ||
                        sql.contains("CREATE INDEX IF NOT EXISTS `index_history_song_id`"),
                )
            },
        )
    }
}
