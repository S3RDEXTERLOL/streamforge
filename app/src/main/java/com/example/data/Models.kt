package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus(val label: String) {
    QUEUED("Queued"),
    FETCHING_INFO("Inspecting"),
    DOWNLOADING("Downloading"),
    PROCESSING("Muxing / FFmpeg"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELED("Canceled")
}

enum class VideoResolution(
    val tag: String,
    val displayLabel: String,
    val maxHeight: Int,
    val bitrateHint: String
) {
    RES_4K("2160p", "4K UHD", 2160, " ~18 Mbps • VP9/HEVC"),
    RES_1080P("1080p", "Full HD", 1080, " ~6.5 Mbps • H.264"),
    RES_720P("720p", "HD Ready", 720, " ~3.2 Mbps • H.264"),
    RES_480P("480p", "Standard", 480, " ~1.4 Mbps • H.264");

    companion object {
        fun fromTag(tag: String): VideoResolution =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: RES_1080P
    }
}

enum class VideoContainer(val ext: String, val label: String) {
    MP4("mp4", "MP4 (Universal)"),
    MKV("mkv", "MKV (Matroska)"),
    WEBM("webm", "WEBM (Native)")
}

enum class AudioFormat(
    val ext: String,
    val displayLabel: String,
    val codecSummary: String,
    val qualityFlag: String
) {
    MP3("mp3", "MP3", "320 kbps CBR • ID3 Tags", "0"),
    M4A("m4a", "M4A", "256 kbps AAC • Fast Mux", "0"),
    OPUS("opus", "OPUS", "160 kbps VBR • Transparent", "0"),
    FLAC("flac", "FLAC", "24-bit Lossless Studio", "0");

    companion object {
        fun fromExt(ext: String): AudioFormat =
            entries.firstOrNull { it.ext.equals(ext, ignoreCase = true) } ?: MP3
    }
}

enum class SubtitleLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    ES("es", "Spanish"),
    FR("fr", "French"),
    DE("de", "German"),
    JA("ja", "Japanese"),
    ALL("all", "All Languages");

    companion object {
        fun fromCode(code: String): SubtitleLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val directMediaUrl: String = "",
    val title: String,
    val uploader: String,
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val isAudioOnly: Boolean = false,
    val videoResolution: String = VideoResolution.RES_1080P.tag,
    val videoContainer: String = VideoContainer.MP4.ext,
    val audioFormat: String = AudioFormat.MP3.ext,
    val embedSubtitles: Boolean = false,
    val subtitleLanguage: String = SubtitleLanguage.EN.code,
    val writeSidecarSrt: Boolean = false,
    val playlistTitle: String = "",
    val playlistIndex: Int = 0,
    val status: String = DownloadStatus.QUEUED.name,
    val progressPercent: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedText: String = "0 KB/s",
    val etaText: String = "--:--",
    val cliCommandPreview: String = "",
    val outputFilePath: String = "",
    val subtitleFilePath: String = "",
    val lastLogLine: String = "Waiting in queue…",
    val createdAt: Long = System.currentTimeMillis()
) {
    val statusEnum: DownloadStatus
        get() = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.QUEUED)

    val formatBadgeText: String
        get() = if (isAudioOnly) {
            "AUDIO • ${audioFormat.uppercase()}"
        } else {
            "${videoResolution.uppercase()} • ${videoContainer.uppercase()}"
        }
}

@Entity(tableName = "format_presets")
data class FormatPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isAudioOnly: Boolean,
    val videoResolution: String = VideoResolution.RES_1080P.tag,
    val videoContainer: String = VideoContainer.MP4.ext,
    val audioFormat: String = AudioFormat.MP3.ext,
    val embedSubtitles: Boolean = false,
    val subtitleLanguage: String = SubtitleLanguage.EN.code,
    val isDefault: Boolean = false
) {
    val summaryBadge: String
        get() = buildString {
            if (isAudioOnly) {
                append(audioFormat.uppercase())
                append(" Audio")
            } else {
                append(videoResolution)
                append(" ")
                append(videoContainer.uppercase())
            }
            if (embedSubtitles) {
                append(" + CC (${subtitleLanguage.uppercase()})")
            }
        }
}

data class PlaylistEntryItem(
    val id: String,
    val index: Int,
    val title: String,
    val uploader: String,
    val durationSeconds: Int,
    val pageUrl: String,
    val directStreamUrl: String = "",
    val estimatedSizeBytes: Long = 12_500_000L
)

data class InspectedMediaInfo(
    val sourceUrl: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Int,
    val thumbnailUrl: String,
    val isPlaylist: Boolean = false,
    val playlistEntries: List<PlaylistEntryItem> = emptyList(),
    val availableResolutions: List<VideoResolution> = VideoResolution.entries,
    val availableAudioFormats: List<AudioFormat> = AudioFormat.entries,
    val availableSubtitleLanguages: List<SubtitleLanguage> = SubtitleLanguage.entries,
    val directStreamUrl: String = "",
    val estimatedSizeBytes: Long = 15_800_000L,
    val extractorName: String = "yt-dlp"
)

data class FormatConfigState(
    val isAudioOnly: Boolean = false,
    val videoResolution: VideoResolution = VideoResolution.RES_1080P,
    val videoContainer: VideoContainer = VideoContainer.MP4,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val embedSubtitles: Boolean = true,
    val subtitleLanguage: SubtitleLanguage = SubtitleLanguage.EN,
    val writeSidecarSrt: Boolean = true,
    val sponsorBlockSkip: Boolean = false,
    val customExtraFlags: String = ""
) {
    fun buildCliPreview(targetUrl: String): String {
        val parts = mutableListOf("yt-dlp", "-c")
        if (isAudioOnly) {
            parts.add("-x")
            parts.add("--audio-format ${audioFormat.ext}")
            parts.add("--audio-quality ${audioFormat.qualityFlag}")
            parts.add("--embed-metadata")
        } else {
            val h = videoResolution.maxHeight
            parts.add("-f \"bv*[height<=$h]+ba/b[height<=$h]/b\"")
            parts.add("--merge-output-format ${videoContainer.ext}")
        }
        if (embedSubtitles) {
            parts.add("--write-subs")
            parts.add("--write-auto-subs")
            parts.add("--sub-langs ${subtitleLanguage.code}")
            if (writeSidecarSrt) {
                parts.add("--convert-subs srt")
            } else {
                parts.add("--embed-subs")
            }
        }
        if (sponsorBlockSkip) {
            parts.add("--sponsorblock-remove sponsor")
        }
        if (customExtraFlags.isNotBlank()) {
            parts.add(customExtraFlags.trim())
        }
        parts.add("\"$targetUrl\"")
        return parts.joinToString(" ")
    }
}

data class SampleStreamItem(
    val id: String,
    val title: String,
    val uploader: String,
    val badge: String,
    val durationSeconds: Int,
    val url: String,
    val isPlaylist: Boolean = false
)
