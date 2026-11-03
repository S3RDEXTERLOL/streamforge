package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AudioFormat
import com.example.data.EngineStatus
import com.example.data.FormatConfigState
import com.example.data.FormatPresetEntity
import com.example.data.SampleStreamItem
import com.example.data.SubtitleLanguage
import com.example.data.VideoResolution
import com.example.data.YtDlpEngineManager

@Composable
fun ExtractScreen(
    urlInput: String,
    detectedClipboardUrl: String,
    isInspecting: Boolean,
    inspectionError: String?,
    engineStatus: EngineStatus,
    formatConfig: FormatConfigState,
    presets: List<FormatPresetEntity>,
    selectedPresetId: Long?,
    sampleStreams: List<SampleStreamItem>,
    onUrlChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onApplyClipboardBanner: (Boolean) -> Unit,
    onDismissClipboardBanner: () -> Unit,
    onInspectClick: () -> Unit,
    onQuickDownloadClick: () -> Unit,
    onSelectPreset: (FormatPresetEntity) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onOpenSavePresetDialog: () -> Unit,
    onSetAudioOnly: (Boolean) -> Unit,
    onSelectVideoResolution: (VideoResolution) -> Unit,
    onSelectAudioFormat: (AudioFormat) -> Unit,
    onToggleSubtitles: (Boolean) -> Unit,
    onSelectSubtitleLanguage: (SubtitleLanguage) -> Unit,
    onSelectSampleStream: (SampleStreamItem, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Studio Hero Header Card
            item {
                StudioHeroCard(engineStatus = engineStatus)
            }

            // 2. Automatic Clipboard URL Detection Banner
            item {
                AnimatedVisibility(
                    visible = detectedClipboardUrl.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ClipboardSmartBanner(
                        clipboardUrl = detectedClipboardUrl,
                        onInspect = { onApplyClipboardBanner(true) },
                        onQuickDownload = { onApplyClipboardBanner(false) },
                        onDismiss = onDismissClipboardBanner
                    )
                }
            }

            // 3. Primary URL Input & Action Deck
            item {
                UrlExtractionDeckCard(
                    urlInput = urlInput,
                    isInspecting = isInspecting,
                    inspectionError = inspectionError,
                    onUrlChange = onUrlChange,
                    onPasteClick = onPasteFromClipboard,
                    onClearClick = { onUrlChange("") },
                    onInspectClick = {
                        focusManager.clearFocus()
                        onInspectClick()
                    },
                    onQuickDownloadClick = {
                        focusManager.clearFocus()
                        onQuickDownloadClick()
                    }
                )
            }

            // 4. Saved Format Presets Carousel
            item {
                SavedPresetsSection(
                    presets = presets,
                    selectedPresetId = selectedPresetId,
                    onSelectPreset = onSelectPreset,
                    onDeletePreset = onDeletePreset,
                    onSaveNewPreset = onOpenSavePresetDialog
                )
            }

            // 5. Inline Quick Format & Subtitle Selector + Live CLI Preview
            item {
                InlineFormatStudioCard(
                    urlInput = urlInput,
                    formatConfig = formatConfig,
                    onSetAudioOnly = onSetAudioOnly,
                    onSelectVideoResolution = onSelectVideoResolution,
                    onSelectAudioFormat = onSelectAudioFormat,
                    onToggleSubtitles = onToggleSubtitles,
                    onSelectSubtitleLanguage = onSelectSubtitleLanguage
                )
            }

            // 6. Instant Test Streams & Multi-Item Playlists Showcase
            item {
                Text(
                    text = stringResource(R.string.section_sample_streams),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(sampleStreams, key = { it.id }) { sample ->
                SampleStreamCard(
                    sample = sample,
                    onInspect = { onSelectSampleStream(sample, true) },
                    onQuickDownload = { onSelectSampleStream(sample, false) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StudioHeroCard(engineStatus: EngineStatus) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_hero_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_studio_hero_art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = engineStatus.versionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = "StreamForge Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Extract 4K/1080p video, lossless audio, playlists & closed captions on-device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ClipboardSmartBanner(
    clipboardUrl: String,
    onInspect: () -> Unit,
    onQuickDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium
            )
            .testTag("clipboard_smart_banner"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Clipboard URL",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.clipboard_detected_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = clipboardUrl,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_clipboard_banner")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss clipboard banner"
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clipboard_inspect_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.clipboard_action_inspect))
                }
                Button(
                    onClick = onQuickDownload,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clipboard_quick_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.clipboard_action_quick))
                }
            }
        }
    }
}

@Composable
private fun UrlExtractionDeckCard(
    urlInput: String,
    isInspecting: Boolean,
    inspectionError: String?,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    onInspectClick: () -> Unit,
    onQuickDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.url_input_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onPasteClick,
                    modifier = Modifier.testTag("paste_url_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste URL",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_paste))
                }
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input_field"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.url_input_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                isError = inspectionError != null,
                supportingText = if (inspectionError != null) {
                    {
                        Text(
                            text = inspectionError,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("url_error_text")
                        )
                    }
                } else null,
                trailingIcon = {
                    if (urlInput.isNotEmpty()) {
                        IconButton(
                            onClick = onClearClick,
                            modifier = Modifier.testTag("clear_url_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onInspectClick() }),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onInspectClick,
                    enabled = !isInspecting,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("inspect_formats_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isInspecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Inspecting…")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inspect & Formats",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onQuickDownloadClick,
                    enabled = !isInspecting,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("quick_download_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_quick_download),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPresetsSection(
    presets: List<FormatPresetEntity>,
    selectedPresetId: Long?,
    onSelectPreset: (FormatPresetEntity) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onSaveNewPreset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.section_presets),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = onSaveNewPreset,
                modifier = Modifier.testTag("save_preset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_new_preset))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = selectedPresetId == preset.id
                Surface(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelectPreset(preset) }
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = MaterialTheme.shapes.medium
                        )
                        .testTag("preset_chip_${preset.id}"),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (preset.isAudioOnly) Icons.Default.AudioFile else Icons.Default.Movie,
                            contentDescription = null,
                            tint = if (preset.isAudioOnly) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preset.summaryBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!preset.isDefault) {
                            IconButton(
                                onClick = { onDeletePreset(preset.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete preset ${preset.name}",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected preset",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineFormatStudioCard(
    urlInput: String,
    formatConfig: FormatConfigState,
    onSetAudioOnly: (Boolean) -> Unit,
    onSelectVideoResolution: (VideoResolution) -> Unit,
    onSelectAudioFormat: (AudioFormat) -> Unit,
    onToggleSubtitles: (Boolean) -> Unit,
    onSelectSubtitleLanguage: (SubtitleLanguage) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inline_format_studio_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Active Extraction Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Video vs Audio segmented pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModePill(
                        label = "Video",
                        selected = !formatConfig.isAudioOnly,
                        onClick = { onSetAudioOnly(false) },
                        testTag = "inline_mode_video"
                    )
                    ModePill(
                        label = "Audio Only",
                        selected = formatConfig.isAudioOnly,
                        onClick = { onSetAudioOnly(true) },
                        testTag = "inline_mode_audio"
                    )
                }
            }

            if (!formatConfig.isAudioOnly) {
                Text(
                    text = "Video Resolution",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoResolution.entries.forEach { res ->
                        FilterChip(
                            selected = formatConfig.videoResolution == res,
                            onClick = { onSelectVideoResolution(res) },
                            label = {
                                Text("${res.displayLabel} (${res.tag})")
                            },
                            modifier = Modifier.testTag("res_chip_${res.tag}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            } else {
                Text(
                    text = "Audio Extraction Codec",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioFormat.entries.forEach { audioFmt ->
                        FilterChip(
                            selected = formatConfig.audioFormat == audioFmt,
                            onClick = { onSelectAudioFormat(audioFmt) },
                            label = {
                                Text("${audioFmt.displayLabel} (${audioFmt.ext.uppercase()})")
                            },
                            modifier = Modifier.testTag("audio_chip_${audioFmt.ext}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            // Subtitle & Closed Captions quick row
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
                            text = "Embed Subtitles / CC",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fetch & mux closed captions (${formatConfig.subtitleLanguage.displayName})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = formatConfig.embedSubtitles,
                    onCheckedChange = onToggleSubtitles,
                    modifier = Modifier.testTag("inline_subtitle_switch")
                )
            }

            AnimatedVisibility(visible = formatConfig.embedSubtitles) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubtitleLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = formatConfig.subtitleLanguage == lang,
                            onClick = { onSelectSubtitleLanguage(lang) },
                            label = { Text("${lang.displayName} (${lang.code.uppercase()})") },
                            modifier = Modifier.testTag("sub_lang_${lang.code}")
                        )
                    }
                }
            }

            // Live generated yt-dlp CLI command preview
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formatConfig.buildCliPreview(
                            urlInput.ifBlank { "https://media.example/watch?v=stream" }
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("cli_command_preview")
                    )
                }
            }
        }
    }
}

@Composable
private fun ModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SampleStreamCard(
    sample: SampleStreamItem,
    onInspect: () -> Unit,
    onQuickDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sample_card_${sample.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (sample.isPlaylist) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (sample.isPlaylist) {
                                    Icons.AutoMirrored.Filled.PlaylistPlay
                                } else {
                                    Icons.Default.Movie
                                },
                                contentDescription = null,
                                tint = if (sample.isPlaylist) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = sample.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${sample.uploader} • ${YtDlpEngineManager.formatSeconds(sample.durationSeconds)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = sample.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onInspect,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sample_inspect_${sample.id}")
                    ) {
                        Text("Configure", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = onQuickDownload,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sample_download_${sample.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (sample.isPlaylist) "Batch" else "Download",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
