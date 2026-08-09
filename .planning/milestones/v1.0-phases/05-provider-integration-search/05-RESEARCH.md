# Phase 5: Provider Integration & Search — Research

**Phase Goal:** Implement default `MusicProvider` adapter (referencing `sigma67/ytmusicapi` for YouTube Music InnerTube API schemas), debounced search UI, and track metadata display.

**Requirements:** REQ-MUS-01, REQ-MUS-04, REQ-NAV-01, REQ-SET-01

---

## 1. Domain Overview

Phase 5 implements the first concrete backend integration for CLIBeats: a `YouTubeMusicProvider` that wraps the YouTube Music InnerTube API. It also introduces a functional `SearchScreen` with live debounced query input, a dense TUI results table, and a track detail view.

**Existing assets to build on:**
- `MusicProvider` interface (domain/provider/MusicProvider.kt) with `search()`, `getTrack()`, `stream()`, `playlists()`, `queue()` methods returning `ProviderResult<T>`.
- `Track` domain model: `id, title, artist, album, durationMs, artworkUrl, streamUrl, providerId`.
- `ProviderResult` sealed class: `Success<T>`, `Error(message, cause)`, `Loading`.
- `PlayerBar` (64dp), `MainLayout` with nav scaffold, `NavDestination.all` for adaptive navigation.
- Hilt DI, Room, DataStore, Media3 ExoPlayer all wired up.

---

## 2. InnerTube API Specification

YouTube Music uses the **InnerTube v1** JSON API internally.

### Base URL
```
https://music.youtube.com/youtubei/v1/
```

### Key endpoints
| Operation | Endpoint | Method |
|-----------|----------|--------|
| Search | `search` | POST |
| Get song details | `player` | POST |
| Browse (album/playlist) | `browse` | POST |
| Get stream URL | `player` | POST |

### Required Request Headers (unauthenticated browser client)
```
User-Agent: Mozilla/5.0 ...
Accept: */*
Accept-Language: en-US,en;q=0.9
Content-Type: application/json
X-Goog-Visitor-Id: (optional, for session continuity)
X-YouTube-Client-Name: 67   (WEB_REMIX = YouTube Music web client)
X-YouTube-Client-Version: 1.20240101.01.00
```

### Search Request Body
```json
{
  "context": {
    "client": {
      "clientName": "WEB_REMIX",
      "clientVersion": "1.20240101.01.00",
      "hl": "en",
      "gl": "US"
    }
  },
  "query": "<search_term>",
  "params": "EgWKAQIIAWoMEA4QChADEAQQCRAF"
}
```
The `params` field is a base64-encoded protobuf value that filters results to "songs" type. Values:
- Songs filter: `EgWKAQIIAWoMEA4QChADEAQQCRAF`
- Default (all): omit `params`
- Videos: `EgWKAQIQAWoMEA4QChADEAQQCRAF`

### Search Response Structure (song results)
The response contains `contents.tabbedSearchResultsRenderer.tabs[0].tabRenderer.content.sectionListRenderer.contents[]`.

Each shelf contains items under `musicShelfRenderer.contents[]`. Each item is a `musicResponsiveListItemRenderer`:
```json
{
  "musicResponsiveListItemRenderer": {
    "flexColumns": [
      {
        "musicResponsiveListItemFlexColumnRenderer": {
          "text": {
            "runs": [{ "text": "Song Title", "navigationEndpoint": { "watchEndpoint": { "videoId": "..." } } }]
          }
        }
      },
      {
        "musicResponsiveListItemFlexColumnRenderer": {
          "text": {
            "runs": [
              { "text": "Artist Name" },   // index 0
              { "text": " • " },           // index 1
              { "text": "Album Name" },    // index 2
              { "text": " • " },           // index 3
              { "text": "4:19" }           // index 4 = duration
            ]
          }
        }
      }
    ],
    "thumbnail": {
      "musicThumbnailRenderer": {
        "thumbnail": {
          "thumbnails": [
            { "url": "https://...", "width": 60, "height": 60 },
            { "url": "https://...", "width": 120, "height": 120 }
          ]
        }
      }
    }
  }
}
```

**Parsing rules:**
- `videoId`: from `flexColumns[0].runs[0].navigationEndpoint.watchEndpoint.videoId`
- `title`: from `flexColumns[0].runs[0].text`
- `artist`: from `flexColumns[1].runs[0].text`
- `album`: from `flexColumns[1].runs[2].text` (if exists, else "")
- `duration string`: from `flexColumns[1].runs[4].text` → parse "4:19" → 259_000 ms
- `artworkUrl`: best thumbnail from `thumbnails` array (pick highest width)

### Player / Stream URL
Stream URL acquisition is the **hard part**. InnerTube's `/player` returns adaptive streaming formats (`streamingData.adaptiveFormats[]`) with signed URLs. These expire quickly and are signature-protected. Options:
1. **Simple (for Phase 5):** Use `ytdl-core` pattern — call `/player` with correct headers, extract `streamingData.formats[0].url` or `adaptiveFormats[].url`. Works for non-age-restricted content without auth.
2. **Robust (future):** Use `n` parameter deobfuscation (signature cipher) for persistent URLs. Defer to Phase 7.

For Phase 5, construct stream URL via:
```
POST https://music.youtube.com/youtubei/v1/player
{
  "context": { ... same as above ... },
  "videoId": "<videoId>",
  "playbackContext": {
    "contentPlaybackContext": { "signatureTimestamp": <sts> }
  }
}
```
Extract `streamingData.formats[].url` (mimeType="audio/mp4") directly. No signature cipher needed for most tracks.

---

## 3. Android HTTP Client — OkHttp + Retrofit

**Recommendation: OkHttp + Retrofit + kotlinx.serialization**

Rationale:
- OkHttp: Industry standard, interceptor support for headers, logging, rate limiting.
- Retrofit: Clean interface-based HTTP client declarations.
- `kotlinx.serialization` (not Gson/Moshi): Consistent with Kotlin first-party tools, no reflection overhead.

**Versions (as of 2026):**
```toml
# libs.versions.toml
okhttp = "4.12.0"
retrofit = "2.11.0"
kotlinxSerializationJson = "1.7.1"
```

**Retrofit interface for InnerTube:**
```kotlin
interface InnerTubeApi {
    @POST("search")
    suspend fun search(
        @Query("key") apiKey: String = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WARN-API-KEY",
        @Body body: SearchRequest,
    ): SearchResponse

    @POST("player")
    suspend fun player(
        @Query("key") apiKey: String = "...",
        @Body body: PlayerRequest,
    ): PlayerResponse
}
```

**Note:** The InnerTube API does NOT require an API key for basic unauthenticated requests when the correct client headers are set. The `key` param is optional / can use the public web key: `AIzaSyC9XL3ZjWddXya6X74dJoCTL-WARN`.

**OkHttp interceptor for required headers:**
```kotlin
class InnerTubeHeaderInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 ...")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", "1.20240101.01.00")
            .header("Content-Type", "application/json")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        return chain.proceed(request)
    }
}
```

---

## 4. Provider Implementation Architecture

### Directory Structure
```
data/
  provider/
    YouTubeMusicProvider.kt       ← MusicProvider implementation
    dto/
      SearchRequest.kt            ← Request body DTO
      SearchResponse.kt           ← Response DTO (nested)
      PlayerRequest.kt
      PlayerResponse.kt
    mapper/
      TrackMapper.kt              ← DTO → Track domain model
    api/
      InnerTubeApi.kt             ← Retrofit interface
di/
  NetworkModule.kt                ← Retrofit/OkHttp Hilt providers
  ProviderModule.kt               ← MusicProvider binding
```

### YouTubeMusicProvider
```kotlin
@Singleton
class YouTubeMusicProvider @Inject constructor(
    private val api: InnerTubeApi,
) : MusicProvider {
    override val providerId = "youtube_music"
    override val displayName = "YouTube Music"

    override suspend fun search(query: String, limit: Int): ProviderResult<List<Track>> =
        runCatching {
            val response = api.search(body = SearchRequest.forQuery(query))
            val tracks = response.parseSearchResults().take(limit)
            ProviderResult.Success(tracks)
        }.getOrElse { e ->
            ProviderResult.Error(e.message ?: "Search failed", e)
        }

    override suspend fun stream(trackId: String): ProviderResult<String> = ...
}
```

### ProviderModule (Hilt Binding)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds
    @Singleton
    abstract fun bindMusicProvider(impl: YouTubeMusicProvider): MusicProvider
}
```

---

## 5. Search ViewModel & Debounce Pattern

### SearchViewModel
```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicProvider: MusicProvider,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val searchResults: StateFlow<SearchUiState> = _query
        .debounce(300L)                         // 300ms debounce
        .filter { it.length >= 2 }              // minimum 2 chars
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(SearchUiState.Idle)
            else flow {
                emit(SearchUiState.Loading)
                emit(when (val result = musicProvider.search(q)) {
                    is ProviderResult.Success -> SearchUiState.Success(result.data)
                    is ProviderResult.Error   -> SearchUiState.Error(result.message)
                    is ProviderResult.Loading -> SearchUiState.Loading
                })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Idle)

    fun onQueryChange(q: String) { _query.value = q }
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val tracks: List<Track>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
```

**Key Kotlin operators used:**
- `debounce(300L)`: delay emission 300ms — avoids network calls on every keystroke
- `filter { it.length >= 2 }`: no-op for very short queries
- `distinctUntilChanged()`: skip if query didn't change after debounce
- `flatMapLatest`: cancels in-flight search when new query arrives
- `stateIn(... WhileSubscribed(5000))`: keeps upstream active 5s after last subscriber (survives brief recompositions)

---

## 6. UI Implementation

### SearchScreen Composable Structure
```
SearchScreen
├── Column
│   ├── SearchInputBar (TextField, 48dp, search icon leading)
│   ├── when (state) {
│   │   is Idle    → EmptyState("Start typing to search")
│   │   is Loading → CircularProgressIndicator()
│   │   is Error   → ErrorState(message)
│   │   is Success → SearchResultsList(tracks)
│   │ }
```

### SongTableRow (48dp) — Dense TUI Layout
Reuse/extend existing `SongTableRow` from Phase 2 (already exists in `presentation/component/`).

Structure:
```
Row (48dp height, horizontal padding 16dp)
├── Text "#idx" (24dp wide, monospace, secondary, right-aligned)
├── Spacer 8dp
├── AsyncImage (40x40dp, artwork) or Box placeholder
├── Spacer 8dp
├── Column (weight=1f)
│   ├── Text(title, bodyMedium, primary, 1 line, ellipsis)
│   └── Text("artist • album", labelSmall, secondary, 1 line, ellipsis)
└── Text("4:19", labelMedium, secondary, right-aligned, monospace)
```

### Navigation: SearchScreen wiring
`MainLayout` already has `NavDestination.Search` in the nav rail. The search icon in TopAppBar needs to trigger navigation to Search destination. Update `CliBeatsTopAppBar` to accept `onSearchClick: () -> Unit` and wire it to `onDestinationSelected(NavDestination.Search)`.

### TopAppBar search action (REQ-NAV-01)
When user is on SearchScreen, the TopAppBar title area should show the search `TextField` inline (Material 3 Search Bar pattern or simple TextField in top bar area).

**Approach for Phase 5:** Simpler — SearchScreen has its own search bar below the TopAppBar. The TopAppBar search icon navigates to the Search tab. Full inline search bar transition can be a Phase 8 polish item.

---

## 7. Caching Strategy

**Phase 5: In-memory cache only via ViewModel scope + `stateIn`.**

- Search results live in `SearchViewModel.searchResults` — cached as long as ViewModel is alive.
- No Room persistence for search results in Phase 5.
- Track Room persistence (SongRepository) is wired in Phase 6 when user adds tracks to library.
- Artwork images: Use Coil for async image loading (already standard Android Compose library).

**Coil version:** `2.7.0` (stable, Compose-first AsyncImage API)

---

## 8. Testing Strategy

### Unit Tests
1. **`YouTubeMusicProviderTest`**: Mock `InnerTubeApi` with Mockito; verify:
   - `search()` maps raw API response → `List<Track>` with correct title/artist/album/duration
   - `search()` handles API error → `ProviderResult.Error`
   - `stream()` returns URL from player response

2. **`SearchViewModelTest`**: Mock `MusicProvider`; verify:
   - Initial state is `Idle`
   - Query `< 2 chars` stays `Idle`
   - Query `>= 2 chars` emits `Loading` then `Success`
   - Error propagates correctly
   - `distinctUntilChanged` skips duplicate queries

3. **`TrackMapperTest`**: Pure function unit tests for DTO → Track mapping.

### Integration Tests (future / Phase 9)
- `InnerTubeApiTest` — real network calls, test against YouTube Music API with VCR-style fixtures (OkHttp MockWebServer).

### MockWebServer pattern for Provider tests:
```kotlin
val server = MockWebServer()
server.enqueue(MockResponse().setBody(SEARCH_RESPONSE_JSON))
val retrofit = Retrofit.Builder().baseUrl(server.url("/")).build()
val api = retrofit.create(InnerTubeApi::class.java)
val provider = YouTubeMusicProvider(api)
val result = provider.search("Wonderwall")
assertThat(result).isInstanceOf(ProviderResult.Success::class)
```

---

## 9. Validation Architecture

### Dimension 1: Functional Correctness
- `YouTubeMusicProvider.search("Wonderwall")` returns `ProviderResult.Success` with `List<Track>` where each Track has non-null `id`, `title`, `artist`.
- Search field with 1-character input produces no network call (debounce + filter).
- Search field with "Oa" produces network call after 300ms.

### Dimension 2: Data Integrity
- Duration "4:19" → `durationMs = 259_000L` (verified in TrackMapperTest).
- Artwork URL is the highest-resolution thumbnail from the thumbnails array.
- `Track.providerId == "youtube_music"`.

### Dimension 3: UI Correctness
- SearchScreen shows `CircularProgressIndicator` during loading.
- SearchScreen shows `SongTableRow` per result with title/artist/duration visible.
- Empty query shows idle state (no spinner).

### Dimension 4: Quality Gates
- `assembleDebug` passes, `ktlintCheck` 0 violations, `detekt` 0 critical issues.
- All unit tests pass.
- No Clean Architecture violations (Presentation never imports `data.*`).

---

## 10. Implementation Notes & Gotchas

1. **InnerTube API instability**: YouTube changes InnerTube response shapes frequently. The JSON parsing must be defensive (`?.let`, null-safe access, fallback to empty strings). Never use positional array access without bounds checks.

2. **No official API key needed**: Unauthenticated InnerTube calls work with `X-YouTube-Client-Name: 67` and `X-YouTube-Client-Version`. No OAuth required for search in Phase 5.

3. **Stream URL expiry**: URLs from `/player` expire in ~6 hours. Phase 5 fetches fresh URLs on each play. Caching stream URLs requires Phase 7 work.

4. **kotlinx.serialization ignoreUnknownKeys**: Set `ignoreUnknownKeys = true` in the `Json` instance — InnerTube responses contain hundreds of undocumented fields.

5. **Rate limiting**: InnerTube will throttle aggressive requests. Add `HttpLoggingInterceptor` in debug builds only.

6. **SongTableRow reuse**: Phase 2 created `SongTableRow` in `presentation/component/`. Check its exact signature before creating a new one — extend it rather than duplicating.

7. **NavDestination.Search**: Check `NavDestination.kt` — Search destination may already be declared. Wire `onSearchClick` from TopAppBar to navigate to it.

8. **`flatMapLatest` requires `kotlinx-coroutines-core`**: Already in dependency graph via coroutines-test.

9. **Retrofit + kotlinx.serialization**: Use `retrofit2-kotlinx-serialization-converter` bridge library (`com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0`).

10. **Coil for artwork**: Add `io.coil-kt:coil-compose:2.7.0`. Use `AsyncImage(model = artworkUrl, ...)` in SongTableRow. Handle null artworkUrl with a placeholder `Box` of matching size.

---

## 11. Recommended Libraries (with versions)

| Library | Version | Purpose |
|---------|---------|---------|
| `com.squareup.okhttp3:okhttp` | `4.12.0` | HTTP engine |
| `com.squareup.okhttp3:logging-interceptor` | `4.12.0` | Debug request logging |
| `com.squareup.retrofit2:retrofit` | `2.11.0` | REST client interface |
| `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter` | `1.0.0` | Retrofit ↔ kotlinx.serialization bridge |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | `1.7.1` | JSON serialization |
| `io.coil-kt:coil-compose` | `2.7.0` | Async image loading in Compose |
| `com.squareup.okhttp3:mockwebserver` | `4.12.0` | Unit test HTTP mocking |

---

## 12. Phase Plan Outline (suggested)

| Plan | Wave | Content |
|------|------|---------|
| 05-01 | 1 | NetworkModule (OkHttp + Retrofit + kotlinx.serialization Hilt wiring) |
| 05-02 | 1 | InnerTube DTOs (Search/Player request+response) + TrackMapper |
| 05-03 | 2 | YouTubeMusicProvider + ProviderModule (MusicProvider binding) |
| 05-04 | 2 | SearchViewModel + SearchUiState + SearchScreen UI + NavDestination wiring |
| 05-05 | 3 | Unit tests (ProviderTest, ViewModelTest, MapperTest) + ADR-005 + quality gate |

---

## RESEARCH COMPLETE
