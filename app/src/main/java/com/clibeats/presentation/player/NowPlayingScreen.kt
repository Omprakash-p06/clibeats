@file:Suppress("ktlint:standard:function-naming", "MagicNumber")

package com.clibeats.presentation.player

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.clibeats.domain.model.PlaybackState
import com.clibeats.domain.model.RepeatMode
import com.clibeats.presentation.component.ARTWORK_FULL_PX
import com.clibeats.presentation.component.ARTWORK_THUMB_PX
import com.clibeats.presentation.component.AddToPlaylistDialog
import com.clibeats.presentation.component.HintedControl
import com.clibeats.presentation.component.artworkUrlFor
import com.clibeats.presentation.theme.CliBeatsAccent
import com.clibeats.presentation.theme.CliBeatsBackground
import com.clibeats.presentation.theme.CliBeatsBorderInactive
import com.clibeats.presentation.theme.CliBeatsTextPrimary
import com.clibeats.presentation.theme.CliBeatsTextSecondary
import com.clibeats.presentation.theme.LocalAccentColor
import kotlinx.coroutines.launch

/** Full-screen player opened from the bottom PlayerBar. Collects state itself (ticks stay local). */
@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val state by viewModel.playbackState.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val hint by viewModel.hint.collectAsState()
    val sleepTimer by viewModel.sleepTimer.collectAsState()
    val isLiked by viewModel.isCurrentLiked.collectAsState()
    val waveform by viewModel.waveform.collectAsState()
    val track = state.currentTrack

    // Match the screen to the album art (Symfonium-style): a deep tint of the cover at the top fading into
    // the theme background, and the cover's vivid colour as the accent. Fades between songs.
    val themeBackground = CliBeatsBackground
    val lightTheme = themeBackground.luminance() > LIGHT_THEME_LUMINANCE
    val artColors = rememberArtworkColors(artworkUrlFor(track?.artworkUrl, ARTWORK_THUMB_PX), lightTheme)
    val accent by animateColorAsState(
        targetValue = artColors?.accent ?: LocalAccentColor.current,
        animationSpec = tween(COLOR_FADE_MS),
        label = "accent",
    )
    val topTint by animateColorAsState(
        targetValue =
            artColors?.let { topTintTarget(it, themeBackground, lightTheme) }
                ?: themeBackground,
        animationSpec = tween(COLOR_FADE_MS),
        label = "tint",
    )

    // The quality line sits over the blurred cover under the wash (or the tint gradient on Android 11 and
    // older); pick a colour that stays readable on all of those, not just the raw album colour.
    val statsTarget =
        remember(artColors, themeBackground) {
            artColors?.let { statsColorFor(it, themeBackground, lightTheme) }
        }
    val statsColor by animateColorAsState(
        targetValue = statsTarget ?: LocalAccentColor.current,
        animationSpec = tween(COLOR_FADE_MS),
        label = "stats",
    )

    // Drag down on the artwork/header to collapse: follows the finger, snaps back if not far/fast enough.
    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dismissPx = with(LocalDensity.current) { DISMISS_DRAG_DP.dp.toPx() }
    val dragToDismiss =
        Modifier.pointerInput(Unit) {
            val tracker = VelocityTracker()
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = {
                    tracker.resetTracking()
                    total = 0f
                },
                onDragEnd = {
                    val velocity = tracker.calculateVelocity().y
                    if (dragOffset.value > dismissPx || velocity > DISMISS_VELOCITY_PX_S) {
                        onClose()
                    } else {
                        scope.launch { dragOffset.animateTo(0f, tween(SNAP_BACK_MS)) }
                    }
                },
                onDragCancel = { scope.launch { dragOffset.animateTo(0f, tween(SNAP_BACK_MS)) } },
                onVerticalDrag = { change, dy ->
                    change.consume()
                    total += dy
                    // Track our own cumulative offset: local positions shift as the screen translates.
                    tracker.addPosition(change.uptimeMillis, Offset(0f, total))
                    scope.launch { dragOffset.snapTo((dragOffset.value + dy).coerceAtLeast(0f)) }
                },
            )
        }
    val closeAction =
        Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction("Close player") { onClose().let { true } })
        }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    CompositionLocalProvider(LocalAccentColor provides accent) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = dragOffset.value }
                    // Swallow taps so the screen below doesn't receive them.
                    .clickable(enabled = false) {},
        ) {
            GlassBackground(
                artworkUrl = artworkUrlFor(track?.artworkUrl, ARTWORK_THUMB_PX),
                topTint = topTint,
                base = themeBackground,
                lightTheme = lightTheme,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Header ──
                Column(
                    modifier = Modifier.fillMaxWidth().then(dragToDismiss).then(closeAction).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Grab handle hints that the screen can be dragged down.
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .background(CliBeatsBorderInactive, RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "> now_playing",
                            style = MaterialTheme.typography.labelMedium,
                            color = CliBeatsAccent,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        val queueLabel = if (queue.isNotEmpty()) "[ QUEUE (${queue.size}) ]" else "[ QUEUE ]"
                        Text(
                            text = queueLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = CliBeatsAccent,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterEnd)
                                    .clickable(onClick = onOpenQueue)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                    val outputDevice = state.activeOutputDevice
                    if (outputDevice != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "[ ${outputDevice.displayIcon} ${outputDevice.name.uppercase()} ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = CliBeatsAccent,
                            modifier =
                                Modifier
                                    .clickable { launchMediaOutputSwitcher(context) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Artwork ──
                // Two layers: the small thumbnail (already cached by the lists, so near-instant) underneath,
                // and the high-res cover on top, transparent until loaded, then crossfading in. Never blank;
                // a failed hi-res attempt (e.g. missing ytimg maxres) just keeps the thumbnail showing.
                // (Don't pass AsyncImage(placeholder=...) here: it replaces the cached-thumbnail placeholder.)
                var artFallback by remember(track?.artworkUrl) { mutableStateOf(false) }
                val artworkDescription = track?.let { "Artwork for ${it.title}" } ?: "No artwork"
                val artSize = if (artFallback) FALLBACK_ART_PX else ARTWORK_FULL_PX
                val artUrl = artworkUrlFor(track?.artworkUrl, artSize)
                val thumbUrl = artworkUrlFor(track?.artworkUrl, ARTWORK_THUMB_PX)
                val hiResRequest =
                    remember(artUrl) {
                        ImageRequest.Builder(context).data(artUrl).crossfade(ART_CROSSFADE_MS).build()
                    }
                Box(
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, CliBeatsBorderInactive, RoundedCornerShape(8.dp))
                            .then(dragToDismiss)
                            .then(closeAction)
                            .semantics { contentDescription = artworkDescription },
                ) {
                    if (thumbUrl != null) {
                        AsyncImage(
                            model = thumbUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    if (artUrl != null && artUrl != thumbUrl) {
                        AsyncImage(
                            model = hiResRequest,
                            onError = { if (!artFallback) artFallback = true },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Title / artist / quality ──
                Text(
                    text = track?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.headlineSmall,
                    color = CliBeatsTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                if (!track?.artist.isNullOrBlank()) {
                    Text(
                        text = track!!.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = CliBeatsTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = qualityLine(state),
                    style = MaterialTheme.typography.labelSmall,
                    color = statsColor,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                // ── Waveform seek: scrub without interrupting playback, snaps back to "now"; controls on top ──
                WaveformScrubber(
                    trackKey = track?.id,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    onSeek = viewModel::seekTo,
                    heights = waveform?.takeIf { it.trackId == track?.id }?.heights,
                ) {
                    TransportControls(
                        state = state,
                        hint = hint,
                        viewModel = viewModel,
                        backdropColor = themeBackground,
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    )
                }

                Spacer(Modifier.height(40.dp))

                // ── Bottom actions: sleep timer · queue · add to playlist · favourite ──
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val sleepLabel =
                        when (val t = sleepTimer) {
                            SleepTimer.Off -> null
                            is SleepTimer.Countdown -> "${t.remainingMin}m"
                            SleepTimer.EndOfTrack -> "end"
                        }
                    Row(
                        modifier = Modifier.clickable { showSleepDialog = true }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Bedtime,
                            contentDescription = "Sleep timer" + (sleepLabel?.let { ": $it" } ?: ""),
                            tint = if (sleepLabel != null) CliBeatsAccent else CliBeatsTextSecondary,
                        )
                        if (sleepLabel != null) {
                            Text(" $sleepLabel", style = MaterialTheme.typography.labelSmall, color = CliBeatsAccent)
                        }
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.AutoMirrored.Outlined.QueueMusic, "Open queue", tint = CliBeatsTextSecondary)
                    }
                    IconButton(onClick = { showPlaylistDialog = true }, enabled = track != null) {
                        Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, "Add to playlist", tint = CliBeatsTextSecondary)
                    }
                    IconButton(onClick = viewModel::toggleLike, enabled = track != null) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isLiked) "Remove from Favorites" else "Add to Favorites",
                            tint = if (isLiked) CliBeatsAccent else CliBeatsTextSecondary,
                        )
                    }
                }
            }
        }

        if (showSleepDialog) {
            val options = SLEEP_OPTIONS
            AlertDialog(
                onDismissRequest = { showSleepDialog = false },
                modifier = Modifier.border(1.dp, CliBeatsAccent),
                title = { Text("> sleep --timer") },
                text = {
                    Column {
                        options.forEach { (label, minutes) ->
                            Text(
                                text = "[ $label ]",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSleepTimer(minutes)
                                            showSleepDialog = false
                                        }.padding(vertical = 10.dp),
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSleepDialog = false }) { Text("Cancel") } },
            )
        }

        if (showPlaylistDialog && track != null) {
            // Shared, themed dialog (same as long-press elsewhere), with inline "+ New playlist".
            AddToPlaylistDialog(track = track, onDismiss = { showPlaylistDialog = false })
        }
    }
}

/** repeat · prev · play/pause · next · shuffle, drawn over the waveform with a dark backdrop each. */
@Suppress("FunctionNaming", "LongMethod")
@Composable
private fun TransportControls(
    state: PlaybackState,
    hint: ButtonHint?,
    viewModel: PlayerViewModel,
    backdropColor: Color,
    modifier: Modifier = Modifier,
) {
    val backdrop = Modifier.controlBackdrop(backdropColor)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HintedControl(hint = hint?.takeIf { it.target == HintTarget.REPEAT }?.text) {
            IconButton(onClick = viewModel::onRepeatClick, modifier = backdrop) {
                Icon(
                    imageVector =
                        if (state.repeatMode == RepeatMode.ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                    contentDescription = "Repeat: ${state.repeatMode.name.lowercase()}",
                    tint = if (state.repeatMode == RepeatMode.OFF) CliBeatsTextSecondary else CliBeatsAccent,
                )
            }
        }
        IconButton(onClick = viewModel::onSkipPreviousClick, modifier = Modifier.size(56.dp).then(backdrop)) {
            Icon(
                Icons.Outlined.SkipPrevious,
                contentDescription = "Previous",
                tint = CliBeatsTextPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .background(backdropColor, RoundedCornerShape(8.dp))
                    .border(2.dp, CliBeatsAccent, RoundedCornerShape(8.dp))
                    .clickable(onClick = viewModel::onPlayPauseClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = CliBeatsAccent,
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = viewModel::onSkipNextClick, modifier = Modifier.size(56.dp).then(backdrop)) {
            Icon(Icons.Outlined.SkipNext, "Next", tint = CliBeatsTextPrimary, modifier = Modifier.size(32.dp))
        }
        HintedControl(hint = hint?.takeIf { it.target == HintTarget.SHUFFLE }?.text) {
            IconButton(onClick = viewModel::onShuffleClick, modifier = backdrop) {
                Icon(
                    Icons.Outlined.Shuffle,
                    contentDescription = if (state.shuffleEnabled) "Shuffle: on" else "Shuffle: off",
                    tint = if (state.shuffleEnabled) CliBeatsAccent else CliBeatsTextSecondary,
                )
            }
        }
    }
}

/**
 * Frosted-glass background: the cover itself, filling the screen, heavily blurred, under a wash that keeps
 * text readable and leans toward the album colour. Uses the small cached thumbnail (instant; the blur hides
 * its size). Android 11 and older can't blur, so they keep the album-tinted gradient.
 */
@Suppress("FunctionNaming")
@Composable
private fun GlassBackground(
    artworkUrl: String?,
    topTint: Color,
    base: Color,
    lightTheme: Boolean,
) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to topTint, GRADIENT_END to base)))
    if (artworkUrl == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    Crossfade(targetState = artworkUrl, animationSpec = tween(COLOR_FADE_MS), label = "glass") { url ->
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = GLASS_SCALE
                        scaleY = GLASS_SCALE
                    }.blur(GLASS_BLUR_DP.dp, BlurredEdgeTreatment.Rectangle),
        )
    }
    val wash = if (lightTheme) base else lerp(base, topTint, WASH_TINT)
    val (top, middle, bottom) = if (lightTheme) LIGHT_WASH else DARK_WASH
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to wash.copy(alpha = top),
                WASH_MIDDLE to wash.copy(alpha = middle),
                1f to base.copy(alpha = bottom),
            ),
        ),
    )
}

/** Readable colour for the quality line over the blurred cover and its wash (see [GlassBackground]). */
private fun statsColorFor(
    colors: ArtworkColors,
    themeBackground: Color,
    lightTheme: Boolean,
): Color {
    val topTint = topTintTarget(colors, themeBackground, lightTheme)
    val wash = if (lightTheme) themeBackground else lerp(themeBackground, topTint, WASH_TINT)
    val washAlpha = (if (lightTheme) LIGHT_WASH else DARK_WASH).second
    val behind = colors.coverColors.map { lerp(it, wash, washAlpha) } + topTint
    return readableTextColor(colors.accent, behind, lightTheme)
}

/** The album tint blended into the theme background, as used at the top of the screen. */
private fun topTintTarget(
    colors: ArtworkColors,
    themeBackground: Color,
    lightTheme: Boolean,
): Color = lerp(themeBackground, colors.tint, if (lightTheme) LIGHT_TINT else DARK_TINT)

/** Dark disc behind a control drawn over the waveform. */
private fun Modifier.controlBackdrop(color: Color): Modifier {
    val fill = color.copy(alpha = CONTROL_BACKDROP_ALPHA)
    return background(fill, CircleShape)
}

/** e.g. `[LOCAL] FLAC · 1076 kbps · 44.1 kHz · 24-bit` or `[ONLINE · YouTube Music] OPUS · 160 kbps · 48 kHz`. */
internal fun qualityLine(state: PlaybackState): String {
    val providerId = state.currentTrack?.providerId ?: return ""
    val source = if (providerId == "local") "[LOCAL]" else "[ONLINE · ${providerLabel(providerId)}]"
    val parts =
        listOfNotNull(
            state.codec,
            state.bitrateKbps?.takeIf { it > 0 }?.let { "$it kbps" },
            state.sampleRateHz?.let {
                    hz ->
                if (hz % 1000 == 0) "${hz / 1000} kHz" else String.format(java.util.Locale.US, "%.1f kHz", hz / 1000f)
            },
            state.bitDepth?.let { "$it-bit" },
        )
    return if (parts.isEmpty()) source else "$source ${parts.joinToString(" · ")}"
}

private fun providerLabel(id: String): String =
    when (id) {
        "youtube_music" -> "YouTube Music"
        "jamendo" -> "Jamendo"
        "audius" -> "Audius"
        "internet_archive" -> "Internet Archive"
        else -> id.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

private const val DISMISS_DRAG_DP = 120
private const val DISMISS_VELOCITY_PX_S = 1_500f
private const val SNAP_BACK_MS = 200
private const val ART_CROSSFADE_MS = 450
private const val FALLBACK_ART_PX = 480

private val SLEEP_OPTIONS =
    listOf("Off" to 0, "15 min" to 15, "30 min" to 30, "45 min" to 45, "60 min" to 60, "End of track" to null)

private const val COLOR_FADE_MS = 600
private const val LIGHT_THEME_LUMINANCE = 0.5f
private const val DARK_TINT = 0.6f
private const val LIGHT_TINT = 0.28f
private const val GRADIENT_END = 0.8f
private const val CONTROL_BACKDROP_ALPHA = 0.85f
private const val GLASS_BLUR_DP = 56
private const val GLASS_SCALE = 1.15f
private const val WASH_TINT = 0.35f
private const val WASH_MIDDLE = 0.55f
private val DARK_WASH = Triple(0.42f, 0.6f, 0.9f)
private val LIGHT_WASH = Triple(0.62f, 0.74f, 0.92f)

internal fun launchMediaOutputSwitcher(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val intent =
            Intent("android.settings.panel.action.MEDIA_OUTPUT").apply {
                putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        if (context.packageManager.resolveActivity(intent, 0) != null) {
            context.startActivity(intent)
            return
        }
    }
    val btIntent =
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    runCatching { context.startActivity(btIntent) }
}
