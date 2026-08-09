package com.clibeats.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.clibeats.data.local.dao.CacheIndexDao;
import com.clibeats.data.local.dao.CacheIndexDao_Impl;
import com.clibeats.data.local.dao.HistoryDao;
import com.clibeats.data.local.dao.HistoryDao_Impl;
import com.clibeats.data.local.dao.PlaylistDao;
import com.clibeats.data.local.dao.PlaylistDao_Impl;
import com.clibeats.data.local.dao.QueueDao;
import com.clibeats.data.local.dao.QueueDao_Impl;
import com.clibeats.data.local.dao.SongDao;
import com.clibeats.data.local.dao.SongDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CliBeatsDatabase_Impl extends CliBeatsDatabase {
  private volatile SongDao _songDao;

  private volatile PlaylistDao _playlistDao;

  private volatile HistoryDao _historyDao;

  private volatile CacheIndexDao _cacheIndexDao;

  private volatile QueueDao _queueDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `songs` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration_ms` INTEGER NOT NULL, `artwork_url` TEXT, `stream_url` TEXT, `provider_id` TEXT NOT NULL, `local_path` TEXT, `cached_at` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `artwork_url` TEXT, `track_count` INTEGER NOT NULL, `is_owned` INTEGER NOT NULL, `provider_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_song_cross_ref` (`playlist_id` TEXT NOT NULL, `song_id` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`playlist_id`, `song_id`), FOREIGN KEY(`playlist_id`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_cross_ref_playlist_id` ON `playlist_song_cross_ref` (`playlist_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_cross_ref_song_id` ON `playlist_song_cross_ref` (`song_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `song_id` TEXT NOT NULL, `played_at` INTEGER NOT NULL, `provider_id` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cache_index` (`song_id` TEXT NOT NULL, `local_path` TEXT NOT NULL, `file_size_bytes` INTEGER NOT NULL, `cached_at` INTEGER NOT NULL, `expires_at` INTEGER, PRIMARY KEY(`song_id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `queue_items` (`position` INTEGER NOT NULL, `songId` TEXT NOT NULL, PRIMARY KEY(`position`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3fe9cba3f9ceb4efe3b91dc4b0b049fc')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `songs`");
        db.execSQL("DROP TABLE IF EXISTS `playlists`");
        db.execSQL("DROP TABLE IF EXISTS `playlist_song_cross_ref`");
        db.execSQL("DROP TABLE IF EXISTS `history`");
        db.execSQL("DROP TABLE IF EXISTS `cache_index`");
        db.execSQL("DROP TABLE IF EXISTS `queue_items`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSongs = new HashMap<String, TableInfo.Column>(10);
        _columnsSongs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("artist", new TableInfo.Column("artist", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("album", new TableInfo.Column("album", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("duration_ms", new TableInfo.Column("duration_ms", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("artwork_url", new TableInfo.Column("artwork_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("stream_url", new TableInfo.Column("stream_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("provider_id", new TableInfo.Column("provider_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("local_path", new TableInfo.Column("local_path", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("cached_at", new TableInfo.Column("cached_at", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSongs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSongs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSongs = new TableInfo("songs", _columnsSongs, _foreignKeysSongs, _indicesSongs);
        final TableInfo _existingSongs = TableInfo.read(db, "songs");
        if (!_infoSongs.equals(_existingSongs)) {
          return new RoomOpenHelper.ValidationResult(false, "songs(com.clibeats.data.local.entity.SongEntity).\n"
                  + " Expected:\n" + _infoSongs + "\n"
                  + " Found:\n" + _existingSongs);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaylists = new HashMap<String, TableInfo.Column>(9);
        _columnsPlaylists.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("artwork_url", new TableInfo.Column("artwork_url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("track_count", new TableInfo.Column("track_count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("is_owned", new TableInfo.Column("is_owned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("provider_id", new TableInfo.Column("provider_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylists = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylists = new TableInfo("playlists", _columnsPlaylists, _foreignKeysPlaylists, _indicesPlaylists);
        final TableInfo _existingPlaylists = TableInfo.read(db, "playlists");
        if (!_infoPlaylists.equals(_existingPlaylists)) {
          return new RoomOpenHelper.ValidationResult(false, "playlists(com.clibeats.data.local.entity.PlaylistEntity).\n"
                  + " Expected:\n" + _infoPlaylists + "\n"
                  + " Found:\n" + _existingPlaylists);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaylistSongCrossRef = new HashMap<String, TableInfo.Column>(3);
        _columnsPlaylistSongCrossRef.put("playlist_id", new TableInfo.Column("playlist_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistSongCrossRef.put("song_id", new TableInfo.Column("song_id", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistSongCrossRef.put("position", new TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylistSongCrossRef = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysPlaylistSongCrossRef.add(new TableInfo.ForeignKey("playlists", "CASCADE", "NO ACTION", Arrays.asList("playlist_id"), Arrays.asList("id")));
        _foreignKeysPlaylistSongCrossRef.add(new TableInfo.ForeignKey("songs", "CASCADE", "NO ACTION", Arrays.asList("song_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesPlaylistSongCrossRef = new HashSet<TableInfo.Index>(2);
        _indicesPlaylistSongCrossRef.add(new TableInfo.Index("index_playlist_song_cross_ref_playlist_id", false, Arrays.asList("playlist_id"), Arrays.asList("ASC")));
        _indicesPlaylistSongCrossRef.add(new TableInfo.Index("index_playlist_song_cross_ref_song_id", false, Arrays.asList("song_id"), Arrays.asList("ASC")));
        final TableInfo _infoPlaylistSongCrossRef = new TableInfo("playlist_song_cross_ref", _columnsPlaylistSongCrossRef, _foreignKeysPlaylistSongCrossRef, _indicesPlaylistSongCrossRef);
        final TableInfo _existingPlaylistSongCrossRef = TableInfo.read(db, "playlist_song_cross_ref");
        if (!_infoPlaylistSongCrossRef.equals(_existingPlaylistSongCrossRef)) {
          return new RoomOpenHelper.ValidationResult(false, "playlist_song_cross_ref(com.clibeats.data.local.entity.PlaylistSongCrossRef).\n"
                  + " Expected:\n" + _infoPlaylistSongCrossRef + "\n"
                  + " Found:\n" + _existingPlaylistSongCrossRef);
        }
        final HashMap<String, TableInfo.Column> _columnsHistory = new HashMap<String, TableInfo.Column>(4);
        _columnsHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("song_id", new TableInfo.Column("song_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("played_at", new TableInfo.Column("played_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("provider_id", new TableInfo.Column("provider_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHistory = new TableInfo("history", _columnsHistory, _foreignKeysHistory, _indicesHistory);
        final TableInfo _existingHistory = TableInfo.read(db, "history");
        if (!_infoHistory.equals(_existingHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "history(com.clibeats.data.local.entity.HistoryEntity).\n"
                  + " Expected:\n" + _infoHistory + "\n"
                  + " Found:\n" + _existingHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsCacheIndex = new HashMap<String, TableInfo.Column>(5);
        _columnsCacheIndex.put("song_id", new TableInfo.Column("song_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCacheIndex.put("local_path", new TableInfo.Column("local_path", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCacheIndex.put("file_size_bytes", new TableInfo.Column("file_size_bytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCacheIndex.put("cached_at", new TableInfo.Column("cached_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCacheIndex.put("expires_at", new TableInfo.Column("expires_at", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCacheIndex = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCacheIndex = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCacheIndex = new TableInfo("cache_index", _columnsCacheIndex, _foreignKeysCacheIndex, _indicesCacheIndex);
        final TableInfo _existingCacheIndex = TableInfo.read(db, "cache_index");
        if (!_infoCacheIndex.equals(_existingCacheIndex)) {
          return new RoomOpenHelper.ValidationResult(false, "cache_index(com.clibeats.data.local.entity.CacheIndexEntity).\n"
                  + " Expected:\n" + _infoCacheIndex + "\n"
                  + " Found:\n" + _existingCacheIndex);
        }
        final HashMap<String, TableInfo.Column> _columnsQueueItems = new HashMap<String, TableInfo.Column>(2);
        _columnsQueueItems.put("position", new TableInfo.Column("position", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQueueItems.put("songId", new TableInfo.Column("songId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysQueueItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesQueueItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoQueueItems = new TableInfo("queue_items", _columnsQueueItems, _foreignKeysQueueItems, _indicesQueueItems);
        final TableInfo _existingQueueItems = TableInfo.read(db, "queue_items");
        if (!_infoQueueItems.equals(_existingQueueItems)) {
          return new RoomOpenHelper.ValidationResult(false, "queue_items(com.clibeats.data.local.entity.QueueEntity).\n"
                  + " Expected:\n" + _infoQueueItems + "\n"
                  + " Found:\n" + _existingQueueItems);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3fe9cba3f9ceb4efe3b91dc4b0b049fc", "61496d3381f3a90af4312d611a0dd721");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "songs","playlists","playlist_song_cross_ref","history","cache_index","queue_items");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `songs`");
      _db.execSQL("DELETE FROM `playlists`");
      _db.execSQL("DELETE FROM `playlist_song_cross_ref`");
      _db.execSQL("DELETE FROM `history`");
      _db.execSQL("DELETE FROM `cache_index`");
      _db.execSQL("DELETE FROM `queue_items`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(SongDao.class, SongDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlaylistDao.class, PlaylistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HistoryDao.class, HistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CacheIndexDao.class, CacheIndexDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(QueueDao.class, QueueDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public SongDao songDao() {
    if (_songDao != null) {
      return _songDao;
    } else {
      synchronized(this) {
        if(_songDao == null) {
          _songDao = new SongDao_Impl(this);
        }
        return _songDao;
      }
    }
  }

  @Override
  public PlaylistDao playlistDao() {
    if (_playlistDao != null) {
      return _playlistDao;
    } else {
      synchronized(this) {
        if(_playlistDao == null) {
          _playlistDao = new PlaylistDao_Impl(this);
        }
        return _playlistDao;
      }
    }
  }

  @Override
  public HistoryDao historyDao() {
    if (_historyDao != null) {
      return _historyDao;
    } else {
      synchronized(this) {
        if(_historyDao == null) {
          _historyDao = new HistoryDao_Impl(this);
        }
        return _historyDao;
      }
    }
  }

  @Override
  public CacheIndexDao cacheIndexDao() {
    if (_cacheIndexDao != null) {
      return _cacheIndexDao;
    } else {
      synchronized(this) {
        if(_cacheIndexDao == null) {
          _cacheIndexDao = new CacheIndexDao_Impl(this);
        }
        return _cacheIndexDao;
      }
    }
  }

  @Override
  public QueueDao queueDao() {
    if (_queueDao != null) {
      return _queueDao;
    } else {
      synchronized(this) {
        if(_queueDao == null) {
          _queueDao = new QueueDao_Impl(this);
        }
        return _queueDao;
      }
    }
  }
}
