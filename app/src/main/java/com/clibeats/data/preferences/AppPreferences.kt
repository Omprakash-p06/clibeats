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
@Suppress("Indentation", "TooManyFunctions", "LargeClass")
class AppPreferences
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        companion object {
            /**
             * v2.8.5's Classic theme brings a silver-white accent, so that is the default for a new install.
             * Anyone who already picked an accent keeps it: their choice is stored under [Keys.ACCENT_COLOR_NAME].
             */
            const val DEFAULT_ACCENT_NAME = "WHITE"

            /** Settings → Storage and Cache: artwork sizes (MB); the first is the default. */
            val ARTWORK_CACHE_OPTIONS_MB = listOf(128, 256, 512, 1024, 2048)

            /** Settings → Storage and Cache: song cache sizes (MB); the first is the default. */
            val SONG_CACHE_OPTIONS_MB = listOf(512, 1024, 2048, 2560, 3072)

            /** [mb] if it's offered, else the next size up (the largest when it's bigger than all). */
            internal fun snapToOption(
                mb: Int,
                options: List<Int>,
            ): Int = options.firstOrNull { it >= mb } ?: options.last()
        }

        private object Keys {
            val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
            val CACHE_MAX_MB = intPreferencesKey("cache_max_mb")
            val HIGH_QUALITY_STREAMING = booleanPreferencesKey("high_quality_streaming")
            val LAST_QUEUE_INDEX = intPreferencesKey("last_queue_index")
            val LAST_PLAYBACK_POSITION = longPreferencesKey("last_playback_position")
            val SAVED_REPEAT_MODE = stringPreferencesKey("saved_repeat_mode")
            val SAVED_SHUFFLE_ENABLED = booleanPreferencesKey("saved_shuffle_enabled")
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val THEME_PALETTE = stringPreferencesKey("theme_palette")
            val ACCENT_COLOR_NAME = stringPreferencesKey("accent_color_name")
            val PLAYLIST_VIEW = stringPreferencesKey("playlist_view")
            val LIBRARY_GROUP_VIEW = stringPreferencesKey("library_group_view")
            val LYRICS_TRANSLATE = booleanPreferencesKey("lyrics_translate")
            val LIBRARY_ARTIST_SORT = stringPreferencesKey("library_artist_sort")
            val LIBRARY_ALBUM_SORT = stringPreferencesKey("library_album_sort")
            val GESTURE_SONG_RIGHT = stringPreferencesKey("gesture_song_right_action")
            val GESTURE_SONG_LEFT = stringPreferencesKey("gesture_song_left_action")
            val GESTURE_TABS = booleanPreferencesKey("gesture_tab_swipe")
            val GESTURE_QUEUE_RIGHT = stringPreferencesKey("gesture_queue_right_action")
            val GESTURE_NP_DOWN = booleanPreferencesKey("gesture_now_playing_down")
            val GESTURE_NP_UP = booleanPreferencesKey("gesture_now_playing_up")
            val LYRICS_PRONOUNCE = booleanPreferencesKey("lyrics_pronounce")
            val HOME_VIEW = stringPreferencesKey("home_view")
            val AUTO_QUEUE_RELATED = booleanPreferencesKey("auto_queue_related")
            val AUTOPLAY_ENABLED = booleanPreferencesKey("autoplay_enabled")
            val ARTWORK_CACHE_MB = intPreferencesKey("artwork_cache_mb")
            val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
            val LOCAL_MUSIC_TREE_URI = stringPreferencesKey("local_music_tree_uri") // legacy single folder
            val LOCAL_MUSIC_TREE_URIS = stringSetPreferencesKey("local_music_tree_uris")
            val HOME_RECENTS_CLEARED_AT = longPreferencesKey("home_recents_cleared_at")
            val HOME_LANGUAGES = stringSetPreferencesKey("home_languages")
            val HOME_LANGUAGES_ASKED = booleanPreferencesKey("home_languages_asked")
            val LIBRARY_HISTORY_CLEARED_AT = longPreferencesKey("library_history_cleared_at")
            val LIBRARY_SECTION = stringPreferencesKey("library_section")
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
            val MIRROR_BACKUPS_TO_DOWNLOADS = booleanPreferencesKey("mirror_backups_to_downloads")
        }

        /**
         * Every setting for a backup, by stored name: type tag and value as text. Leaves out what belongs to this
         * install or device (queue position, local folder permissions, update checks, recent searches).
         */
        suspend fun exportSettings(): Map<String, Pair<String, String>> {
            val all = dataStore.data.first().asMap()
            return all.mapNotNull { (key, value) ->
                if (key.name in NOT_BACKED_UP) return@mapNotNull null
                val typed =
                    when (value) {
                        is Boolean -> "b" to value.toString()
                        is Int -> "i" to value.toString()
                        is Long -> "l" to value.toString()
                        is Float -> "f" to value.toString()
                        is String -> "s" to value
                        is Set<*> ->
                            "ss" to
                                kotlinx.serialization.json.JsonArray(
                                    value.filterIsInstance<String>().map {
                                        kotlinx.serialization.json.JsonPrimitive(it)
                                    },
                                ).toString()
                        else -> null
                    } ?: return@mapNotNull null
                key.name to typed
            }.toMap()
        }

        /** Applies settings from a backup (unknown or malformed entries are skipped). */
        suspend fun importSettings(settings: Map<String, Pair<String, String>>) {
            dataStore.edit { prefs ->
                settings.forEach { (name, typed) ->
                    if (name in NOT_BACKED_UP || name.length > MAX_SETTING_NAME) return@forEach
                    val (type, value) = typed
                    runCatching {
                        when (type) {
                            "b" -> prefs[booleanPreferencesKey(name)] = value.toBooleanStrict()
                            "i" -> prefs[intPreferencesKey(name)] = value.toInt()
                            "l" -> prefs[longPreferencesKey(name)] = value.toLong()
                            "f" ->
                                prefs[androidx.datastore.preferences.core.floatPreferencesKey(name)] = value.toFloat()
                            "s" -> prefs[stringPreferencesKey(name)] = value
                            "ss" ->
                                prefs[stringSetPreferencesKey(name)] =
                                    kotlinx.serialization.json.Json.parseToJsonElement(value)
                                        .let { it as kotlinx.serialization.json.JsonArray }
                                        .map { (it as kotlinx.serialization.json.JsonPrimitive).content }
                                        .toSet()
                        }
                    }
                }
            }
        }

        /** Home "Recently played" only shows plays after this time ([ CLEAR ] on Home moves it). */
        val homeRecentsClearedAt: Flow<Long> = dataStore.data.map { it[Keys.HOME_RECENTS_CLEARED_AT] ?: 0L }

        /** Library Online only shows plays after this time ([ CLEAR ] in Library moves it). */
        val libraryHistoryClearedAt: Flow<Long> = dataStore.data.map { it[Keys.LIBRARY_HISTORY_CLEARED_AT] ?: 0L }

        suspend fun setHomeRecentsClearedAt(epochMs: Long) {
            dataStore.edit { it[Keys.HOME_RECENTS_CLEARED_AT] = epochMs }
        }

        /** The Library section last open (Playlists / Online / Offline), so Library reopens there. */
        val librarySection: Flow<String?> = dataStore.data.map { it[Keys.LIBRARY_SECTION] }

        suspend fun setLibrarySection(name: String) {
            dataStore.edit { it[Keys.LIBRARY_SECTION] = name }
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

        /** [sha256] may arrive after the download starts (fetched in parallel); null leaves it unset. */
        suspend fun saveUpdateDownload(
            id: Long,
            version: String,
            sha256: String?,
        ) {
            dataStore.edit { prefs ->
                prefs[Keys.UPDATE_DOWNLOAD_ID] = id
                prefs[Keys.UPDATE_DOWNLOAD_VERSION] = version
                if (sha256 != null) {
                    prefs[Keys.UPDATE_DOWNLOAD_SHA256] = sha256
                } else {
                    prefs.remove(Keys.UPDATE_DOWNLOAD_SHA256)
                }
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

        /** Song cache limit in MB, one of [SONG_CACHE_OPTIONS_MB] (a size no longer offered moves up to the next). */
        val cacheMaxMb: Flow<Int> =
            dataStore.data.map { prefs ->
                snapToOption(prefs[Keys.CACHE_MAX_MB] ?: DEFAULT_SONG_CACHE_MB, SONG_CACHE_OPTIONS_MB)
            }

        val highQualityStreaming: Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[Keys.HIGH_QUALITY_STREAMING] ?: true
            }

        /** Artwork cache limit in MB, one of [ARTWORK_CACHE_OPTIONS_MB] (a size no longer offered moves up). */
        val artworkCacheMb: Flow<Int> =
            dataStore.data.map { prefs ->
                snapToOption(prefs[Keys.ARTWORK_CACHE_MB] ?: DEFAULT_ARTWORK_CACHE_MB, ARTWORK_CACHE_OPTIONS_MB)
            }

        /** False until the first-run onboarding is finished (new installs only). */
        val onboardingDone: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[Keys.ONBOARDING_DONE] ?: false }

        suspend fun setArtworkCacheMb(mb: Int) {
            dataStore.edit { it[Keys.ARTWORK_CACHE_MB] = snapToOption(mb, ARTWORK_CACHE_OPTIONS_MB) }
        }

        suspend fun setOnboardingDone() {
            dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
        }

        /** When true, tapping a song in a list queues the rest of that list after it. Off by default. */
        val autoQueueRelated: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[Keys.AUTO_QUEUE_RELATED] ?: false }

        /** When true, playing the last tracks in the queue fetches related tracks. On by default. */
        val autoplayEnabled: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[Keys.AUTOPLAY_ENABLED] ?: true }

        suspend fun setAutoplayEnabled(enabled: Boolean) {
            dataStore.edit { it[Keys.AUTOPLAY_ENABLED] = enabled }
        }

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
                prefs[Keys.ACCENT_COLOR_NAME] ?: DEFAULT_ACCENT_NAME
            }

        suspend fun setActiveProviderId(providerId: String) {
            dataStore.edit { it[Keys.ACTIVE_PROVIDER_ID] = providerId }
        }

        suspend fun setCacheMaxMb(maxMb: Int) {
            dataStore.edit { it[Keys.CACHE_MAX_MB] = snapToOption(maxMb, SONG_CACHE_OPTIONS_MB) }
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

        /** Settings → Theme: the palette (Classic, Berry, Grove, …), saved by name. */
        val themePalette: Flow<String?> = dataStore.data.map { it[Keys.THEME_PALETTE] }

        suspend fun setThemePalette(name: String) {
            dataStore.edit { it[Keys.THEME_PALETTE] = name }
        }

        suspend fun setThemeMode(mode: String) {
            dataStore.edit { it[Keys.THEME_MODE] = mode }
        }

        suspend fun setAccentColorName(name: String) {
            dataStore.edit { it[Keys.ACCENT_COLOR_NAME] = name }
        }

        /** Playlists overview layout (a PlaylistViewMode name); null = default. */
        val playlistView: Flow<String?> = dataStore.data.map { it[Keys.PLAYLIST_VIEW] }

        /** Home song list layout (a PlaylistViewMode name); null = default. */
        val homeView: Flow<String?> = dataStore.data.map { it[Keys.HOME_VIEW] }

        suspend fun setHomeView(name: String) {
            dataStore.edit { it[Keys.HOME_VIEW] = name }
        }

        /** Languages the online Home is tuned to (HomeLanguage names); empty = every language. */
        val homeLanguages: Flow<Set<String>> = dataStore.data.map { it[Keys.HOME_LANGUAGES].orEmpty() }

        /** True once the language picker was answered (chosen or skipped), so Home stops suggesting it. */
        val homeLanguagesAsked: Flow<Boolean> = dataStore.data.map { it[Keys.HOME_LANGUAGES_ASKED] ?: false }

        suspend fun setHomeLanguages(names: Set<String>) {
            dataStore.edit {
                it[Keys.HOME_LANGUAGES] = names
                it[Keys.HOME_LANGUAGES_ASKED] = true
            }
        }

        /** Settings → Gestures (all on unless turned off). */
        val gestures: Flow<com.clibeats.domain.model.GestureSettings> =
            dataStore.data.map {
                com.clibeats.domain.model.GestureSettings(
                    songSwipeRight = swipeAction(it[Keys.GESTURE_SONG_RIGHT], SwipeActionDefault.RIGHT),
                    songSwipeLeft = swipeAction(it[Keys.GESTURE_SONG_LEFT], SwipeActionDefault.LEFT),
                    queueSwipeRight = swipeAction(it[Keys.GESTURE_QUEUE_RIGHT], SwipeActionDefault.QUEUE),
                    tabSwipe = it[Keys.GESTURE_TABS] ?: true,
                    nowPlayingSwipeDown = it[Keys.GESTURE_NP_DOWN] ?: true,
                    nowPlayingSwipeUp = it[Keys.GESTURE_NP_UP] ?: true,
                )
            }

        suspend fun setGestures(gestures: com.clibeats.domain.model.GestureSettings) {
            dataStore.edit {
                it[Keys.GESTURE_SONG_RIGHT] = gestures.songSwipeRight.name
                it[Keys.GESTURE_SONG_LEFT] = gestures.songSwipeLeft.name
                it[Keys.GESTURE_QUEUE_RIGHT] = gestures.queueSwipeRight.name
                it[Keys.GESTURE_TABS] = gestures.tabSwipe
                it[Keys.GESTURE_NP_DOWN] = gestures.nowPlayingSwipeDown
                it[Keys.GESTURE_NP_UP] = gestures.nowPlayingSwipeUp
            }
        }

        /** Lyrics: show each line's translation (into the phone's language) under it. */
        val lyricsTranslate: Flow<Boolean> = dataStore.data.map { it[Keys.LYRICS_TRANSLATE] ?: false }

        /** Lyrics: show how each non-Latin line is pronounced, in Latin letters, under it. */
        val lyricsPronounce: Flow<Boolean> = dataStore.data.map { it[Keys.LYRICS_PRONOUNCE] ?: false }

        suspend fun setLyricsTranslate(on: Boolean) {
            dataStore.edit { it[Keys.LYRICS_TRANSLATE] = on }
        }

        suspend fun setLyricsPronounce(on: Boolean) {
            dataStore.edit { it[Keys.LYRICS_PRONOUNCE] = on }
        }

        /** Library Artists / Albums layout (a PlaylistViewMode name); null = default. */
        val libraryGroupView: Flow<String?> = dataStore.data.map { it[Keys.LIBRARY_GROUP_VIEW] }

        /** Library sort for Artists and for Albums, each remembered on its own. */
        val libraryArtistSort: Flow<String?> = dataStore.data.map { it[Keys.LIBRARY_ARTIST_SORT] }
        val libraryAlbumSort: Flow<String?> = dataStore.data.map { it[Keys.LIBRARY_ALBUM_SORT] }

        suspend fun setLibrarySort(
            albums: Boolean,
            name: String,
        ) {
            dataStore.edit { it[if (albums) Keys.LIBRARY_ALBUM_SORT else Keys.LIBRARY_ARTIST_SORT] = name }
        }

        suspend fun setLibraryGroupView(name: String) {
            dataStore.edit { it[Keys.LIBRARY_GROUP_VIEW] = name }
        }

        suspend fun setPlaylistView(name: String) {
            dataStore.edit { it[Keys.PLAYLIST_VIEW] = name }
        }

        /** Whether daily library backups are also mirrored to shared Download/CLIBeats/. */
        val mirrorBackupsToDownloads: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[Keys.MIRROR_BACKUPS_TO_DOWNLOADS] ?: true }

        suspend fun setMirrorBackupsToDownloads(enabled: Boolean) {
            dataStore.edit { it[Keys.MIRROR_BACKUPS_TO_DOWNLOADS] = enabled }
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
    }

private const val DEFAULT_ARTWORK_CACHE_MB = 128
private const val DEFAULT_SONG_CACHE_MB = 512

/** Stored download id meaning "no update download in progress or kept". */
const val NO_DOWNLOAD = -1L

private object SwipeActionDefault {
    val RIGHT = com.clibeats.domain.model.SwipeAction.QUEUE
    val LEFT = com.clibeats.domain.model.SwipeAction.FAVORITE
    val QUEUE = com.clibeats.domain.model.SwipeAction.REMOVE
}

private fun swipeAction(
    saved: String?,
    default: com.clibeats.domain.model.SwipeAction,
): com.clibeats.domain.model.SwipeAction {
    val all = com.clibeats.domain.model.SwipeAction.entries
    return all.firstOrNull { it.name == saved } ?: default
}

/** Settings that stay with this install: playback position, folder permissions, update state, recent searches. */
private val NOT_BACKED_UP =
    setOf(
        "last_queue_index",
        "last_playback_position",
        "local_music_tree_uri",
        "local_music_tree_uris",
        // Restoring from the setup screen must not close setup before a folder and source are picked.
        "onboarding_done",
        "recent_search_tracks",
        "update_last_check_at",
        "update_etag",
        "update_cached_release",
        "update_snooze_until",
        "update_download_id",
        "update_download_version",
        "update_download_sha256",
    )

private const val MAX_SETTING_NAME = 100
