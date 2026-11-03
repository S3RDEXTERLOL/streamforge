package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AudioFormat
import com.example.data.FormatConfigState
import com.example.data.InspectedMediaInfo
import com.example.data.SubtitleLanguage
import com.example.data.VideoContainer
import com.example.data.VideoResolution
import com.example.data.YtDlpEngineManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatConfigBottomSheet(
    media: InspectedMediaInfo,
    config: FormatConfigState,
    selectedPlaylistIds: Set<String>,
    onDismiss: () -> Unit,
    onSetAudioOnly: (Boolean) -> Unit,
    onSelectVideoResolution: (VideoResolution) -> Unit,
    onSelectVideoContainer: (VideoContainer) -> Unit,
    onSelectAudioFormat: (AudioFormat) -> Unit,
    onToggleSubtitles: (Boolean) -> Unit,
    onSelectSubtitleLanguage: (SubtitleLanguage) -> Unit,
    onToggleSidecarSrt: (Boolean) -> Unit,
    onToggleSponsorBlock: (Boolean) -> Unit,
    onTogglePlaylistEntry: (String) -> Unit,
    onSelectAllPlaylistEntries: (Boolean) -> Unit,
    onSaveAsPresetClick: () -> Unit,
    onCopyCliCommand: (String) -> Unit,
    onConfirmDownload: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cliPreview = config.buildCliPreview(media.sourceUrl)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("format_config_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Media Inspection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (config.isAudioOnly) Icons.Default.AudioFile else Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = media.extractorName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${YtDlpEngineManager.formatSeconds(media.durationSeconds)} • ~${YtDlpEngineManager.formatBytes(media.estimatedSizeBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = media.uploader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Mode Switcher: Video + Audio vs Audio-Only Extraction
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SheetModeTab(
                    title = "Video + Audio",
                    subtitle = "MP4 / MKV / WEBM",
                    selected = !config.isAudioOnly,
                    onClick = { onSetAudioOnly(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_mode_video")
                )
                SheetModeTab(
                    title = "Audio Only (-x)",
                    subtitle = "MP3 / M4A / OPUS / FLAC",
                    selected = config.isAudioOnly,
                    onClick = { onSetAudioOnly(true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_mode_audio")
                )
            }

            if (!config.isAudioOnly) {
                // Video Resolution Grid
                Text(
                    text = "Select Video Resolution",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoResolution.entries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { res ->
                                val isSelected = config.videoResolution == res
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onSelectVideoResolution(res) }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .testTag("sheet_res_${res.tag}"),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "${res.displayLabel} (${res.tag})",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = res.bitrateHint,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Video Container Selection
                Text(
                    text = "Container Format (--merge-output-format)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoContainer.entries.forEach { container ->
                        FilterChip(
                            selected = config.videoContainer == container,
                            onClick = { onSelectVideoContainer(container) },
                            label = { Text(container.label) },
                            modifier = Modifier.testTag("sheet_container_${container.ext}")
                        )
                    }
                }
            } else {
                // Audio Format Grid
                Text(
                    text = "Select Audio Extraction Format (--audio-format)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioFormat.entries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { fmt ->
                                val isSelected = config.audioFormat == fmt
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onSelectAudioFormat(fmt) }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.outline
                                            },
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .testTag("sheet_audio_${fmt.ext}"),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = fmt.displayLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = fmt.codecSummary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subtitles & Closed Captions Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClosedCaption,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = "Subtitles & Closed Captions",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Download & embed caption tracks (--write-subs)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = config.embedSubtitles,
                            onCheckedChange = onToggleSubtitles,
                            modifier = Modifier.testTag("sheet_subtitle_switch")
                        )
                    }

                    if (config.embedSubtitles) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubtitleLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = config.subtitleLanguage == lang,
                                    onClick = { onSelectSubtitleLanguage(lang) },
                                    label = { Text("${lang.displayName} (${lang.code.uppercase()})") }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Also export sidecar .SRT file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = config.writeSidecarSrt,
                                onCheckedChange = onToggleSidecarSrt
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SponsorBlock segment skip (--sponsorblock-remove)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = config.sponsorBlockSkip,
                            onCheckedChange = onToggleSponsorBlock
                        )
                    }
                }
            }

            // Playlist & Multi-Item Batch Selector
            if (media.isPlaylist && media.playlistEntries.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_batch_section"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Playlist Batch (${selectedPlaylistIds.size}/${media.playlistEntries.size} selected)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row {
                                TextButton(onClick = { onSelectAllPlaylistEntries(true) }) {
                                    Text("All")
                                }
                                TextButton(onClick = { onSelectAllPlaylistEntries(false) }) {
                                    Text("None")
                                }
                            }
                        }

                        media.playlistEntries.forEach { entry ->
                            val isChecked = entry.id in selectedPlaylistIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { onTogglePlaylistEntry(entry.id) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .testTag("playlist_item_${entry.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onTogglePlaylistEntry(entry.id) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${YtDlpEngineManager.formatSeconds(entry.durationSeconds)} • ~${YtDlpEngineManager.formatBytes(entry.estimatedSizeBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Generated yt-dlp Command Preview Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = cliPreview,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onCopyCliCommand(cliPreview) }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy yt-dlp command",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Action Buttons Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveAsPresetClick,
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("sheet_save_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Preset")
                }

                Button(
                    onClick = onConfirmDownload,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("confirm_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val buttonText = if (media.isPlaylist) {
                        "Download ${selectedPlaylistIds.size} Selected"
                    } else {
                        "Start Download"
                    }
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun SheetModeTab(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun SavePresetDialog(
    formatConfig: FormatConfigState,
    onDismiss: () -> Unit,
    onConfirmSave: (String) -> Unit
) {
    val defaultName = remember(formatConfig) {
        if (formatConfig.isAudioOnly) {
            "Audio ${formatConfig.audioFormat.ext.uppercase()}" +
                if (formatConfig.embedSubtitles) " + CC" else ""
        } else {
            "${formatConfig.videoResolution.tag} ${formatConfig.videoContainer.ext.uppercase()}" +
                if (formatConfig.embedSubtitles) " + ${formatConfig.subtitleLanguage.code.uppercase()} CC" else ""
        }
    }
    var presetName by remember { mutableStateOf(defaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Format Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Create a reusable one-tap preset on the Extract screen.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmSave(presetName) },
                enabled = presetName.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text("Save Preset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
