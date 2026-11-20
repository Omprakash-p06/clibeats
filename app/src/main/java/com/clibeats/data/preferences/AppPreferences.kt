package com.clibeats.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.clibeats.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
// Indentation: detekt 1.23.6 misparses ktlint_official @Inject constructor() style (false positive).
@Suppress("Indentation", "TooManyFunctions")
class AppPreferences
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
            val CACHE_MAX_MB = intPreferencesKey("cache_max_mb")
            val HIGH_QUALITY_STREAMING = booleanPreferencesKey("high_quality_streaming")
            val LAST_QUEUE_INDEX = intPreferencesKey("last_queue_index")
            val LAST_PLAYBACK_POSITION = longPreferencesKey("last_playback_position")
            val SAVED_REPEAT_MODE = stringPreferencesKey("saved_repeat_mode")
            val SAVED_SHUFFLE_ENABLED = booleanPreferencesKey("saved_shuffle_enabled")
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val ACCENT_COLOR_NAME = stringPreferencesKey("accent_color_name")
            val AUTO_QUEUE_RELATED = booleanPreferencesKey("auto_queue_related")
            val ARTWORK_CACHE_MB = intPreferencesKey("artwork_cache_mb")
            val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
            val LOCAL_MUSIC_TREE_URI = stringPreferencesKey("local_music_tree_uri") // legacy single folder
            val LOCAL_MUSIC_TREE_URIS = stringSetPreferencesKey("local_music_tree_uris")
            val HOME_RECENTS_CLEARED_AT = longPreferencesKey("home_recents_cleared_at")
            val LIBRARY_HISTORY_CLEARED_AT = longPreferencesKey("library_history_cleared_at")
            val RECENT_SEARCH_TRACKS = stringPreferencesKey("recent_search_tracks")
            val MOST_PLAYED_CLEARED_AT = longPreferencesKey("most_played_cleared_at")
            val MOST_PLAYED_SONG_RESETS = stringSetPreferencesKey("most_played_song_resets")
            val UPDATE_FREQUENCY = stringPreferencesKey("update_frequency")
            val UPDATE_LAST_CHECK_AT = longPreferencesKey("update_last_check_at")
            val UPDATE_ETAG = stringPreferencesKey("update_etag")
            val UPDATE_CACHED_RELEASE = stringPreferencesKey("update_cached_release")
            val UPDATE_SNOOZE_UNTIL = longPreferencesKey("update_snooze_until")
            val UPDATE_DOWNLOAD_ID = longPreferencesKey("update_download_id")
            val UPDATE_DOWNLOAD_VERSION = stringPreferencesKey("update_download_version")
            val UPDATE_DOWNLOAD_SHA256 = stringPreferencesKey("update_download_sha256")
            val ANONYMOUS_USAGE_OPT_IN = booleanPreferencesKey("anonymous_usage_opt_in")
            val ANONYMOUS_USAGE_PROMPTED = booleanPreferencesKey("anonymous_usage_prompted")
            val ANONYMOUS_DEVICE_ID = stringPreferencesKey("anonymous_device_id")
        }

        /** Home "Recently played" only shows plays after this time ([ CLEAR ] on Home moves it). */
        val homeRecentsClearedAt: Flow<Long> = dataStore.data.map { it[Keys.HOME_RECENTS_CLEARED_AT] ?: 0L }

        /** Library Online only shows plays after this time ([ CLEAR ] in Library moves it). */
        val libraryHistoryClearedAt: Flow<Long> = dataStore.data.map { it[Keys.LIBRARY_HISTORY_CLEARED_AT] ?: 0L }

        suspend fun setHomeRecentsClearedAt(epochMs: Long) {
            dataStore.edit { it[Keys.HOME_RECENTS_CLEARED_AT] = epochMs }
        }

        suspend fun setLibraryHistoryClearedAt(epochMs: Long) {
            dataStore.edit { it[Keys.LIBRARY_HISTORY_CLEARED_AT] = epochMs }
        }

        /** Most Played counts only plays after this time ([ CLEAR ALL ] moves it). */
        val mostPlayedClearedAt: Flow<Long> = dataStore.data.map { it[Keys.MOST_PLAYED_CLEARED_AT] ?: 0L }

        /** Per-song reset times for songs removed from Most Played ("<epochMs>|<songId>"). */
        val mostPlayedSongResets: Flow<Map<String, Long>> =
            dataStore.data.map { prefs ->
                prefs[Keys.MOST_PLAYED_SONG_RESETS].orEmpty().mapNotNull { entry ->
                    val time = entry.substringBefore('|').toLongOrNull() ?: return@mapNotNull null
                    entry.substringAfter('|') to time
                }.toMap()
            }

        suspend fun clearMostPlayed(epochMs: Long) {
            dataStore.edit {
                it[Keys.MOST_PLAYED_CLEARED_AT] = epochMs
                it.remove(Keys.MOST_PLAYED_SONG_RESETS) // older per-song resets no longer matter
            }
        }

        suspend fun resetMostPlayedSong(
            songId: String,
            epochMs: Long,
        ) {
            dataStore.edit { prefs ->
                val current = prefs[Keys.MOST_PLAYED_SONG_RESETS].orEmpty()
                val others = current.filterNot { it.substringAfter('|') == songId }
                prefs[Keys.MOST_PLAYED_SONG_RESETS] = others.toSet() + "$epochMs|$songId"
            }
        }

        /** Songs picked from search results, most recent first (shown when the search box is empty). */
        val recentSearchTracks: Flow<List<Track>> =
            dataStore.data.map { prefs -> RecentSearches.decode(prefs[Keys.RECENT_SEARCH_TRACKS]) }

        suspend fun addRecentSearchTrack(track: Track) {
            dataStore.edit { prefs ->
                val updated = RecentSearches.add(RecentSearches.decode(prefs[Keys.RECENT_SEARCH_TRACKS]), track)
                prefs[Keys.RECENT_SEARCH_TRACKS] = RecentSearches.encode(updated)
            }
        }

        suspend fun clearRecentSearchTracks() {
            dataStore.edit { it.remove(Keys.RECENT_SEARCH_TRACKS) }
        }

        /** Everything the in-app updater persists, read in one go. */
        data class UpdateRecord(
            val frequency: String?,
            val lastCheckAt: Long,
            val etag: String?,
            val cachedReleaseJson: String?,
            val snoozeUntil: Long,
            val downloadId: Long,
            val downloadVersion: String?,
            val downloadSha256: String?,
        )

        /** Stored name of the auto-check frequency (null = default). */
        val updateFrequency: Flow<String?> = dataStore.data.map { it[Keys.UPDATE_FREQUENCY] }

        suspend fun updateRecord(): UpdateRecord {
            val prefs = dataStore.data.first()
            return UpdateRecord(
                frequency = prefs[Keys.UPDATE_FREQUENCY],
                lastCheckAt = prefs[Keys.UPDATE_LAST_CHECK_AT] ?: 0L,
                etag = prefs[Keys.UPDATE_ETAG],
                cachedReleaseJson = prefs[Keys.UPDATE_CACHED_RELEASE],
                snoozeUntil = prefs[Keys.UPDATE_SNOOZE_UNTIL] ?: 0L,
                downloadId = prefs[Keys.UPDATE_DOWNLOAD_ID] ?: NO_DOWNLOAD,
                downloadVersion = prefs[Keys.UPDATE_DOWNLOAD_VERSION],
                downloadSha256 = prefs[Keys.UPDATE_DOWNLOAD_SHA256],
            )
        }

        suspend fun setUpdateFrequency(name: String) {
            dataStore.edit { it[Keys.UPDATE_FREQUENCY] = name }
        }

        /** Records a successful check; a null [releaseJson] means "no release" and clears the cache. */
        suspend fun saveUpdateCheck(
            checkedAt: Long,
            etag: String?,
            releaseJson: String?,
        ) {
            dataStore.edit { prefs ->
                prefs[Keys.UPDATE_LAST_CHECK_AT] = checkedAt
                if (etag != null) prefs[Keys.UPDATE_ETAG] = etag else prefs.remove(Keys.UPDATE_ETAG)
                if (releaseJson != null) {
                    prefs[Keys.UPDATE_CACHED_RELEASE] = releaseJson
                } else {
                    prefs.remove(Keys.UPDATE_CACHED_RELEASE)
                }
            }
        }

        suspend fun setUpdateLastCheckAt(checkedAt: Long) {
            dataStore.edit { it[Keys.UPDATE_LAST_CHECK_AT] = checkedAt }
        }

        suspend fun setUpdateSnoozeUntil(until: Long) {
            dataStore.edit { it[Keys.UPDATE_SNOOZE_UNTIL] = until }
        }

        suspend fun saveUpdateDownload(
            id: Long,
            version: String,
            sha256: String,
        ) {
            dataStore.edit { prefs ->
                prefs[Keys.UPDATE_DOWNLOAD_ID] = id
                prefs[Keys.UPDATE_DOWNLOAD_VERSION] = version
                prefs[Keys.UPDATE_DOWNLOAD_SHA256] = sha256
            }
        }

        suspend fun clearUpdateDownload() {
            dataStore.edit { prefs ->
                prefs.remove(Keys.UPDATE_DOWNLOAD_ID)
                prefs.remove(Keys.UPDATE_DOWNLOAD_VERSION)
                prefs.remove(Keys.UPDATE_DOWNLOAD_SHA256)
            }
        }

        val activeProviderId: Flow<String?> =
            dataStore.data.map { prefs ->
                prefs[Keys.ACTIVE_PROVIDER_ID]
            }

        val cacheMaxMb: Flow<Int> =
            dataStore.data.map { prefs ->
                prefs[Keys.CACHE_MAX_MB] ?: 512
            }

        val highQualityStreaming: Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[Keys.HIGH_QUALITY_STREAMING] ?: true
            }

        val artworkCacheMb: Flow<Int> =
            dataStore.data.map { prefs -> prefs[Keys.ARTWORK_CACHE_MB] ?: DEFAULT_ARTWORK_CACHE_MB }

        /** False until the first-run onboarding is finished (new installs only). */
        val onboardingDone: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[Keys.ONBOARDING_DONE] ?: false }

        suspend fun setArtworkCacheMb(mb: Int) {
            dataStore.edit { it[Keys.ARTWORK_CACHE_MB] = mb }
        }

        suspend fun setOnboardingDone() {
            dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
        }

        /** When true, tapping a song in a list queues the rest of that list after it. Off by default. */
        val autoQueueRelated: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[Keys.AUTO_QUEUE_RELATED] ?: false }

        /** SAF tree URIs of all folders scanned for local music (includes the legacy single folder). */
        val localMusicTreeUris: Flow<List<String>> =
            dataStore.data.map { prefs ->
                (prefs[Keys.LOCAL_MUSIC_TREE_URIS].orEmpty() + listOfNotNull(prefs[Keys.LOCAL_MUSIC_TREE_URI]))
                    .distinct()
                    .sorted()
            }

        val lastQueueIndex: Flow<Int> =
            dataStore.data.map { prefs ->
                prefs[Keys.LAST_QUEUE_INDEX] ?: 0
            }

        val lastPlaybackPosition: Flow<Long> =
            dataStore.data.map { prefs ->
                prefs[Keys.LAST_PLAYBACK_POSITION] ?: 0L
            }

        val savedRepeatMode: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[Keys.SAVED_REPEAT_MODE] ?: "OFF"
            }

        val savedShuffleEnabled: Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[Keys.SAVED_SHUFFLE_ENABLED] ?: false
            }

        val themeMode: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[Keys.THEME_MODE] ?: "DARK"
            }

        val accentColorName: Flow<String> =
            dataStore.data.map { prefs ->
                prefs[Keys.ACCENT_COLOR_NAME] ?: "GREEN"
            }

        suspend fun setActiveProviderId(providerId: String) {
            dataStore.edit { it[Keys.ACTIVE_PROVIDER_ID] = providerId }
        }

        suspend fun setCacheMaxMb(maxMb: Int) {
            dataStore.edit { it[Keys.CACHE_MAX_MB] = maxMb }
        }

        suspend fun setAutoQueueRelated(enabled: Boolean) {
            dataStore.edit { it[Keys.AUTO_QUEUE_RELATED] = enabled }
        }

        suspend fun addLocalMusicTreeUri(uri: String) {
            dataStore.edit { it[Keys.LOCAL_MUSIC_TREE_URIS] = it[Keys.LOCAL_MUSIC_TREE_URIS].orEmpty() + uri }
        }

        suspend fun removeLocalMusicTreeUri(uri: String) {
            dataStore.edit {
                it[Keys.LOCAL_MUSIC_TREE_URIS] = it[Keys.LOCAL_MUSIC_TREE_URIS].orEmpty() - uri
                if (it[Keys.LOCAL_MUSIC_TREE_URI] == uri) it.remove(Keys.LOCAL_MUSIC_TREE_URI)
            }
        }

        suspend fun setHighQualityStreaming(enabled: Boolean) {
            dataStore.edit { it[Keys.HIGH_QUALITY_STREAMING] = enabled }
        }

        suspend fun setThemeMode(mode: String) {
            dataStore.edit { it[Keys.THEME_MODE] = mode }
        }

        suspend fun setAccentColorName(name: String) {
            dataStore.edit { it[Keys.ACCENT_COLOR_NAME] = name }
        }

        suspend fun saveQueueMetadata(
            index: Int,
            positionMs: Long,
            repeatMode: String,
            shuffleEnabled: Boolean,
        ) {
            dataStore.edit { prefs ->
                prefs[Keys.LAST_QUEUE_INDEX] = index
                prefs[Keys.LAST_PLAYBACK_POSITION] = positionMs
                prefs[Keys.SAVED_REPEAT_MODE] = repeatMode
                prefs[Keys.SAVED_SHUFFLE_ENABLED] = shuffleEnabled
            }
        }

        val anonymousUsageOptIn: Flow<Boolean> =
            dataStore.data.map { it[Keys.ANONYMOUS_USAGE_OPT_IN] ?: false }

        val anonymousUsagePrompted: Flow<Boolean> =
            dataStore.data.map { it[Keys.ANONYMOUS_USAGE_PROMPTED] ?: false }

        val anonymousDeviceId: Flow<String?> =
            dataStore.data.map { it[Keys.ANONYMOUS_DEVICE_ID] }

        suspend fun setAnonymousUsageOptIn(optIn: Boolean) {
            dataStore.edit { prefs ->
                prefs[Keys.ANONYMOUS_USAGE_OPT_IN] = optIn
                prefs[Keys.ANONYMOUS_USAGE_PROMPTED] = true
                if (optIn && prefs[Keys.ANONYMOUS_DEVICE_ID] == null) {
                    prefs[Keys.ANONYMOUS_DEVICE_ID] = java.util.UUID.randomUUID().toString()
                }
            }
        }

        suspend fun markAnonymousUsagePrompted() {
            dataStore.edit { it[Keys.ANONYMOUS_USAGE_PROMPTED] = true }
        }

        suspend fun getOrCreateAnonymousDeviceId(): String {
            val existing = dataStore.data.first()[Keys.ANONYMOUS_DEVICE_ID]
            if (existing != null) return existing
            val newId = java.util.UUID.randomUUID().toString()
            dataStore.edit { it[Keys.ANONYMOUS_DEVICE_ID] = newId }
            return newId
        }
    }

private const val DEFAULT_ARTWORK_CACHE_MB = 128

/** Stored download id meaning "no update download in progress or kept". */
const val NO_DOWNLOAD = -1L
