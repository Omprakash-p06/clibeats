@file:Suppress("ForbiddenImport", "ReturnCount", "MagicNumber")

package com.clibeats.data.provider.mapper

import com.clibeats.data.provider.dto.IaFile
import com.clibeats.data.provider.dto.IaMetadataResponse
import com.clibeats.data.provider.dto.IaSearchDoc
import com.clibeats.domain.model.Track
import com.clibeats.domain.provider.ProviderId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.URLEncoder

internal const val IA_PROVIDER_ID = "internet_archive"
private const val MIN_REASONABLE_DURATION_S = 45L
private const val MAX_REASONABLE_DURATION_S = 1800L

/** Normalises a JSON string-or-array field (IA `creator`/`collection`). */
internal fun JsonElement?.asStringList(): List<String> =
    when (this) {
        null -> emptyList()
        is JsonPrimitive -> listOfNotNull(contentOrNull)
        is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        else -> emptyList()
    }

internal fun JsonElement?.creatorName(): String = asStringList().firstOrNull() ?: ""

internal fun JsonElement?.collectionName(): String = asStringList().firstOrNull() ?: ""

/**
 * True when the file is a playable, Media3-compatible audio file.
 * Rejects images, PDFs, playlists, XML metadata, videos and derived files.
 */
internal fun IaFile.isPlayableAudio(): Boolean {
    val name = name.orEmpty().lowercase()
    val format = format.orEmpty().uppercase()
    val audioKeyword =
        listOf("MP3", "OGG", "FLAC", "M4A", "AAC", "OPUS", "WAV", "MPEG").any { format.contains(it) }
    val audioExtension =
        listOf(".mp3", ".ogg", ".flac", ".m4a", ".aac", ".opus", ".wav").any { name.endsWith(it) }
    if (!audioKeyword && !audioExtension) return false
    val banned = listOf("PLAYLIST", "M3U", "TEXT", "XML", "JSON", "PDF", "SPECTROGRAM", "JPEG", "PNG", "TILE")
    return banned.none { format.contains(it) } && !name.contains("spectrogram")
}

/** Relative quality score for the file format (higher = preferred). */
internal fun IaFile.audioFormatScore(): Int {
    val format = format.orEmpty().uppercase()
    return when {
        format.contains("VBR MP3") || format.contains("320K") || format.contains("192K") -> 10
        format.contains("128K") -> 9
        format.contains("MP3") -> 8
        format.contains("OGG") -> 7
        format.contains("FLAC") -> 6
        format.contains("M4A") || format.contains("AAC") -> 5
        else -> 0
    }
}

/** Duration in seconds parsed from the API `length` string ("245.5" → 245). */
internal fun IaFile.durationSeconds(): Long? = length?.trim()?.toDoubleOrNull()?.toLong()?.takeIf { it > 0L }

/** Picks the best playable audio file for an item, preferring quality MP3s. */
internal fun List<IaFile>.bestAudioFile(): IaFile? =
    filter { it.isPlayableAudio() }
        .maxWithOrNull(
            compareBy<IaFile> { it.audioFormatScore() }
                .thenBy { it.durationSeconds() ?: 0L },
        )

/**
 * Relevance score for one IA item against a user query.
 * Returns -1 for items with no title or no playable audio.
 */
internal fun scoreIaItem(
    doc: IaSearchDoc,
    meta: IaMetadataResponse,
    query: String?,
): Int {
    val title = (meta.metadata.title ?: doc.title)?.trim().orEmpty()
    if (title.isEmpty()) return -1
    val audioFile = meta.files.bestAudioFile() ?: return -1

    var score = 0
    if (!query.isNullOrBlank()) {
        val normTitle = title.lowercase()
        val normQuery = query.lowercase().trim()
        when {
            normTitle == normQuery -> score += 10
            normTitle.contains(normQuery) -> score += 5
            else -> score += normQuery.split(Regex("\\s+")).count { it.length > 2 && normTitle.contains(it) } * 2
        }
        val creator =
            (meta.metadata.creator ?: doc.creator).creatorName().lowercase()
        if (creator.isNotEmpty() && (creator.contains(normQuery) || normQuery.contains(creator))) score += 4
    }
    val duration = audioFile.durationSeconds()
    if (duration in MIN_REASONABLE_DURATION_S..MAX_REASONABLE_DURATION_S) score += 3
    score += audioFile.audioFormatScore()
    return score
}

/** Builds the direct, Range-capable stream URL for an IA audio file. */
internal fun iaStreamUrl(
    identifier: String,
    fileName: String,
): String {
    val encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
    return "https://archive.org/download/$identifier/$encodedName"
}

internal fun iaArtworkUrl(identifier: String): String = "https://archive.org/services/img/$identifier"

/** Maps a scored IA item to a domain [Track]. */
internal fun IaMetadataResponse.toDomainTrack(
    doc: IaSearchDoc,
    fallbackIdentifier: String? = null,
): Track? {
    val identifier = metadata.identifier ?: doc.identifier ?: fallbackIdentifier ?: return null
    val title = metadata.title ?: doc.title ?: return null
    val audioFile = files.bestAudioFile() ?: return null
    val name = audioFile.name ?: return null
    val creator = (metadata.creator ?: doc.creator).creatorName()
    val collection = doc.collection.collectionName()

    return Track(
        id = ProviderId.composite(IA_PROVIDER_ID, identifier),
        title = title,
        artist = creator.ifBlank { "Internet Archive" },
        album = collection.ifBlank { metadata.date ?: doc.date ?: "" },
        durationMs = (audioFile.durationSeconds() ?: 0L) * 1000L,
        artworkUrl = iaArtworkUrl(identifier),
        streamUrl = iaStreamUrl(identifier, name),
        providerId = IA_PROVIDER_ID,
    )
}
