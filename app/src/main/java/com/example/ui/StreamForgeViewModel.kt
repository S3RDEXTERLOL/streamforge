package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AudioFormat
import com.example.data.DownloadStatus
import com.example.data.DownloadTaskEntity
import com.example.data.EngineStatus
import com.example.data.FormatConfigState
import com.example.data.FormatPresetEntity
import com.example.data.InspectedMediaInfo
import com.example.data.SampleStreamItem
import com.example.data.StreamForgeDao
import com.example.data.StreamForgeDatabase
import com.example.data.SubtitleLanguage
import com.example.data.VideoContainer
import com.example.data.VideoResolution
import com.example.data.YtDlpEngineManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class StreamForgeTab(val route: String, val label: String) {
    EXTRACT("extract", "Extract"),
    QUEUE("queue", "Queue"),
    LIBRARY("library", "Library")
}

enum class LibraryFilter(val label: String) {
    ALL("All Media"),
    VIDEO("Video"),
    AUDIO("Audio Only"),
    SUBTITLES("With CC/Subs")
}

data class StreamForgeUiState(
    val currentTab: StreamForgeTab = StreamForgeTab.EXTRACT,
    val urlInput: String = "",
    val detectedClipboardUrl: String = "",
    val dismissedClipboardUrl: String = "",
    val isInspecting: Boolean = false,
    val inspectionError: String? = null,
    val inspectedMedia: InspectedMediaInfo? = null,
    val showFormatSheet: Boolean = false,
    val selectedPlaylistEntryIds: Set<String> = emptySet(),
    val formatConfig: FormatConfigState = FormatConfigState(),
    val selectedPresetId: Long? = null,
    val showSavePresetDialog: Boolean = false,
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val librarySearchQuery: String = "",
    val activePlayerTask: DownloadTaskEntity? = null,
    val statusMessage: String? = null
)

class StreamForgeViewModel(application: Application) : AndroidViewModel(application) {
    private val database: StreamForgeDatabase = StreamForgeDatabase.getInstance(application)
    private val dao: StreamForgeDao = database.dao()
    val engineManager = YtDlpEngineManager(application.applicationContext, dao)

    private val _uiState = MutableStateFlow(StreamForgeUiState())
    val uiState: StateFlow<StreamForgeUiState> = _uiState.asStateFlow()

    val engineStatus: StateFlow<EngineStatus> = engineManager.engineStatus

    val allTasks: StateFlow<List<DownloadTaskEntity>> = dao.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presets: StateFlow<List<FormatPresetEntity>> = dao.observeAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sampleStreams: List<SampleStreamItem> = engineManager.sampleStreams

    init {
        engineManager.initializeEngineAsync()
        seedDefaultPresetsIfNeeded()
    }

    private fun seedDefaultPresetsIfNeeded() {
        viewModelScope.launch {
            if (dao.getPresetCount() == 0) {
                val defaults = listOf(
                    FormatPresetEntity(
                        name = "1080p MP4 + EN Subs",
                        isAudioOnly = false,
                        videoResolution = VideoResolution.RES_1080P.tag,
                        videoContainer = VideoContainer.MP4.ext,
                        embedSubtitles = true,
                        subtitleLanguage = SubtitleLanguage.EN.code,
                        isDefault = true
                    ),
                    FormatPresetEntity(
                        name = "Studio MP3 320k",
                        isAudioOnly = true,
                        audioFormat = AudioFormat.MP3.ext,
                        embedSubtitles = false,
                        isDefault = true
                    ),
                    FormatPresetEntity(
                        name = "4K Archival MKV",
                        isAudioOnly = false,
                        videoResolution = VideoResolution.RES_4K.tag,
                        videoContainer = VideoContainer.MKV.ext,
                        embedSubtitles = true,
                        subtitleLanguage = SubtitleLanguage.ALL.code,
                        isDefault = true
                    ),
                    FormatPresetEntity(
                        name = "Lossless FLAC Audio",
                        isAudioOnly = true,
                        audioFormat = AudioFormat.FLAC.ext,
                        embedSubtitles = false,
                        isDefault = true
                    )
                )
                defaults.forEach { dao.insertPreset(it) }
            }
        }
    }

    fun selectTab(tab: StreamForgeTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateUrlInput(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl, inspectionError = null) }
    }

    fun onClipboardTextChecked(rawClipText: String?) {
        val url = YtDlpEngineManager.extractFirstUrl(rawClipText.orEmpty())
        if (url.isNotBlank() &&
            url != _uiState.value.dismissedClipboardUrl &&
            url != _uiState.value.urlInput
        ) {
            _uiState.update { it.copy(detectedClipboardUrl = url) }
        }
    }

    fun dismissClipboardBanner() {
        _uiState.update {
            it.copy(
                dismissedClipboardUrl = it.detectedClipboardUrl,
                detectedClipboardUrl = ""
            )
        }
    }

    fun applyClipboardUrl(inspectImmediately: Boolean) {
        val clipUrl = _uiState.value.detectedClipboardUrl
        if (clipUrl.isBlank()) return
        _uiState.update {
            it.copy(
                urlInput = clipUrl,
                dismissedClipboardUrl = clipUrl,
                detectedClipboardUrl = "",
                inspectionError = null
            )
        }
        if (inspectImmediately) {
            inspectCurrentUrl(clipUrl)
        } else {
            quickDownloadUrl(clipUrl)
        }
    }

    fun onExternalSharedText(sharedText: String?) {
        val url = YtDlpEngineManager.extractFirstUrl(sharedText.orEmpty()).ifBlank {
            sharedText?.trim().orEmpty()
        }
        if (url.isNotBlank()) {
            _uiState.update {
                it.copy(
                    currentTab = StreamForgeTab.EXTRACT,
                    urlInput = url,
                    inspectionError = null,
                    statusMessage = "Received URL from Android Share Sheet"
                )
            }
            inspectCurrentUrl(url)
        }
    }

    fun applyPreset(preset: FormatPresetEntity) {
        _uiState.update { state ->
            state.copy(
                selectedPresetId = preset.id,
                formatConfig = state.formatConfig.copy(
                    isAudioOnly = preset.isAudioOnly,
                    videoResolution = VideoResolution.fromTag(preset.videoResolution),
                    videoContainer = VideoContainer.entries.firstOrNull {
                        it.ext.equals(preset.videoContainer, ignoreCase = true)
                    } ?: VideoContainer.MP4,
                    audioFormat = AudioFormat.fromExt(preset.audioFormat),
                    embedSubtitles = preset.embedSubtitles,
                    subtitleLanguage = SubtitleLanguage.fromCode(preset.subtitleLanguage)
                ),
                statusMessage = "Applied preset: ${preset.name}"
            )
        }
    }

    fun deletePreset(presetId: Long) {
        viewModelScope.launch {
            dao.deletePresetById(presetId)
            _uiState.update {
                it.copy(
                    selectedPresetId = if (it.selectedPresetId == presetId) null else it.selectedPresetId,
                    statusMessage = "Preset removed"
                )
            }
        }
    }

    fun openSavePresetDialog() {
        _uiState.update { it.copy(showSavePresetDialog = true) }
    }

    fun closeSavePresetDialog() {
        _uiState.update { it.copy(showSavePresetDialog = false) }
    }

    fun saveCurrentConfigAsPreset(presetName: String) {
        val trimmed = presetName.trim()
        if (trimmed.isBlank()) return
        val cfg = _uiState.value.formatConfig
        viewModelScope.launch {
            val newId = dao.insertPreset(
                FormatPresetEntity(
                    name = trimmed,
                    isAudioOnly = cfg.isAudioOnly,
                    videoResolution = cfg.videoResolution.tag,
                    videoContainer = cfg.videoContainer.ext,
                    audioFormat = cfg.audioFormat.ext,
                    embedSubtitles = cfg.embedSubtitles,
                    subtitleLanguage = cfg.subtitleLanguage.code,
                    isDefault = false
                )
            )
            _uiState.update {
                it.copy(
                    selectedPresetId = newId,
                    showSavePresetDialog = false,
                    statusMessage = "Saved preset \"$trimmed\""
                )
            }
        }
    }

    fun inspectCurrentUrl(overrideUrl: String? = null) {
        val target = (overrideUrl ?: _uiState.value.urlInput).trim()
        if (target.isBlank()) {
            _uiState.update { it.copy(inspectionError = "Paste or enter a media URL first.") }
            return
        }
        _uiState.update {
            it.copy(
                urlInput = target,
                isInspecting = true,
                inspectionError = null
            )
        }
        viewModelScope.launch {
            val result = engineManager.inspectUrl(target)
            result.fold(
                onSuccess = { mediaInfo ->
                    val defaultSelectedIds = if (mediaInfo.isPlaylist) {
                        mediaInfo.playlistEntries.map { it.id }.toSet()
                    } else {
                        emptySet()
                    }
                    _uiState.update {
                        it.copy(
                            isInspecting = false,
                            inspectedMedia = mediaInfo,
                            selectedPlaylistEntryIds = defaultSelectedIds,
                            showFormatSheet = true,
                            inspectionError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isInspecting = false,
                            inspectionError = error.message ?: "Unable to inspect URL"
                        )
                    }
                }
            )
        }
    }

    fun quickDownloadUrl(overrideUrl: String? = null) {
        val target = (overrideUrl ?: _uiState.value.urlInput).trim()
        if (target.isBlank()) {
            _uiState.update { it.copy(inspectionError = "Enter or paste a URL to start downloading.") }
            return
        }
        _uiState.update { it.copy(urlInput = target, isInspecting = true, inspectionError = null) }
        viewModelScope.launch {
            val infoResult = engineManager.inspectUrl(target)
            _uiState.update { it.copy(isInspecting = false) }
            infoResult.fold(
                onSuccess = { mediaInfo ->
                    val ids = mediaInfo.playlistEntries.map { it.id }.toSet()
                    enqueueDownloadsForMedia(mediaInfo, _uiState.value.formatConfig, ids)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(inspectionError = err.message ?: "Invalid URL") }
                }
            )
        }
    }

    fun closeFormatSheet() {
        _uiState.update { it.copy(showFormatSheet = false) }
    }

    fun setAudioOnlyMode(audioOnly: Boolean) {
        _uiState.update {
            it.copy(
                selectedPresetId = null,
                formatConfig = it.formatConfig.copy(isAudioOnly = audioOnly)
            )
        }
    }

    fun selectVideoResolution(resolution: VideoResolution) {
        _uiState.update {
            it.copy(
                selectedPresetId = null,
                formatConfig = it.formatConfig.copy(
                    isAudioOnly = false,
                    videoResolution = resolution
                )
            )
        }
    }

    fun selectVideoContainer(container: VideoContainer) {
        _uiState.update {
            it.copy(
                selectedPresetId = null,
                formatConfig = it.formatConfig.copy(videoContainer = container)
            )
        }
    }

    fun selectAudioFormat(audioFormat: AudioFormat) {
        _uiState.update {
            it.copy(
                selectedPresetId = null,
                formatConfig = it.formatConfig.copy(
                    isAudioOnly = true,
                    audioFormat = audioFormat
                )
            )
        }
    }

    fun toggleEmbedSubtitles(enabled: Boolean) {
        _uiState.update {
            it.copy(
                selectedPresetId = null,
                formatConfig = it.formatConfig.copy(embedSubtitles = enabled)
            )
        }
    }

    fun selectSubtitleLanguage(language: SubtitleLanguage) {
        _uiState.update {
            it.copy(
                selectedPresetId = null,
                formatConfig = it.formatConfig.copy(subtitleLanguage = language)
            )
        }
    }

    fun toggleSidecarSrt(writeSrt: Boolean) {
        _uiState.update {
            it.copy(formatConfig = it.formatConfig.copy(writeSidecarSrt = writeSrt))
        }
    }

    fun toggleSponsorBlock(enabled: Boolean) {
        _uiState.update {
            it.copy(formatConfig = it.formatConfig.copy(sponsorBlockSkip = enabled))
        }
    }

    fun togglePlaylistEntrySelection(entryId: String) {
        _uiState.update { state ->
            val updated = state.selectedPlaylistEntryIds.toMutableSet()
            if (!updated.add(entryId)) {
                updated.remove(entryId)
            }
            state.copy(selectedPlaylistEntryIds = updated)
        }
    }

    fun selectAllPlaylistEntries(selectAll: Boolean) {
        val entries = _uiState.value.inspectedMedia?.playlistEntries.orEmpty()
        _uiState.update {
            it.copy(
                selectedPlaylistEntryIds = if (selectAll) entries.map { e -> e.id }.toSet() else emptySet()
            )
        }
    }

    fun confirmAndStartConfiguredDownload() {
        val media = _uiState.value.inspectedMedia ?: return
        val cfg = _uiState.value.formatConfig
        val selectedIds = _uiState.value.selectedPlaylistEntryIds
        if (media.isPlaylist && selectedIds.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Select at least one playlist item to download.") }
            return
        }
        viewModelScope.launch {
            enqueueDownloadsForMedia(media, cfg, selectedIds)
        }
    }

    private suspend fun enqueueDownloadsForMedia(
        media: InspectedMediaInfo,
        cfg: FormatConfigState,
        selectedPlaylistIds: Set<String>
    ) {
        val createdIds = mutableListOf<Long>()
        if (media.isPlaylist && media.playlistEntries.isNotEmpty()) {
            val chosenEntries = media.playlistEntries.filter { it.id in selectedPlaylistIds }
            for (entry in chosenEntries) {
                val cli = cfg.buildCliPreview(entry.pageUrl)
                val task = DownloadTaskEntity(
                    url = entry.pageUrl,
                    directMediaUrl = entry.directStreamUrl,
                    title = entry.title,
                    uploader = entry.uploader,
                    thumbnailUrl = media.thumbnailUrl,
                    durationSeconds = entry.durationSeconds,
                    isAudioOnly = cfg.isAudioOnly,
                    videoResolution = cfg.videoResolution.tag,
                    videoContainer = cfg.videoContainer.ext,
                    audioFormat = cfg.audioFormat.ext,
                    embedSubtitles = cfg.embedSubtitles,
                    subtitleLanguage = cfg.subtitleLanguage.code,
                    writeSidecarSrt = cfg.writeSidecarSrt,
                    playlistTitle = media.title,
                    playlistIndex = entry.index,
                    status = DownloadStatus.QUEUED.name,
                    totalBytes = entry.estimatedSizeBytes,
                    cliCommandPreview = cli
                )
                val id = dao.insertTask(task)
                createdIds.add(id)
            }
        } else {
            val cli = cfg.buildCliPreview(media.sourceUrl)
            val task = DownloadTaskEntity(
                url = media.sourceUrl,
                directMediaUrl = media.directStreamUrl,
                title = media.title,
                uploader = media.uploader,
                thumbnailUrl = media.thumbnailUrl,
                durationSeconds = media.durationSeconds,
                isAudioOnly = cfg.isAudioOnly,
                videoResolution = cfg.videoResolution.tag,
                videoContainer = cfg.videoContainer.ext,
                audioFormat = cfg.audioFormat.ext,
                embedSubtitles = cfg.embedSubtitles,
                subtitleLanguage = cfg.subtitleLanguage.code,
                writeSidecarSrt = cfg.writeSidecarSrt,
                status = DownloadStatus.QUEUED.name,
                totalBytes = media.estimatedSizeBytes,
                cliCommandPreview = cli
            )
            val id = dao.insertTask(task)
            createdIds.add(id)
        }

        _uiState.update {
            it.copy(
                showFormatSheet = false,
                currentTab = StreamForgeTab.QUEUE,
                statusMessage = if (createdIds.size > 1) {
                    "Added ${createdIds.size} playlist items to download queue"
                } else {
                    "Download started in queue"
                }
            )
        }

        createdIds.forEach { taskId ->
            engineManager.startOrResumeTask(taskId)
        }
    }

    fun pauseDownload(taskId: Long) {
        engineManager.pauseTask(taskId)
    }

    fun resumeDownload(taskId: Long) {
        engineManager.startOrResumeTask(taskId)
    }

    fun cancelDownload(taskId: Long) {
        engineManager.cancelTask(taskId)
    }

    fun deleteTaskAndFile(task: DownloadTaskEntity) {
        engineManager.cancelTask(task.id)
        viewModelScope.launch {
            if (task.outputFilePath.isNotBlank()) {
                runCatching { File(task.outputFilePath).delete() }
                runCatching { File("${task.outputFilePath}.part").delete() }
            }
            if (task.subtitleFilePath.isNotBlank()) {
                runCatching { File(task.subtitleFilePath).delete() }
            }
            dao.deleteTaskById(task.id)
            _uiState.update {
                it.copy(
                    activePlayerTask = if (it.activePlayerTask?.id == task.id) null else it.activePlayerTask,
                    statusMessage = "Deleted \"${task.title}\""
                )
            }
        }
    }

    fun pauseAllActive() {
        allTasks.value
            .filter { it.statusEnum == DownloadStatus.DOWNLOADING || it.statusEnum == DownloadStatus.QUEUED }
            .forEach { engineManager.pauseTask(it.id) }
    }

    fun resumeAllPaused() {
        allTasks.value
            .filter { it.statusEnum == DownloadStatus.PAUSED || it.statusEnum == DownloadStatus.FAILED }
            .forEach { engineManager.startOrResumeTask(it.id) }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(libraryFilter = filter) }
    }

    fun updateLibrarySearch(query: String) {
        _uiState.update { it.copy(librarySearchQuery = query) }
    }

    fun openBuiltInPlayer(task: DownloadTaskEntity) {
        _uiState.update { it.copy(activePlayerTask = task) }
    }

    fun closeBuiltInPlayer() {
        _uiState.update { it.copy(activePlayerTask = null) }
    }

    fun consumeStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
