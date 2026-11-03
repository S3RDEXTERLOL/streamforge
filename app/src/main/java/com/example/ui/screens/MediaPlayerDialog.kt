package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.DownloadTaskEntity
import com.example.data.YtDlpEngineManager
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.max
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuiltInMediaPlayerSheet(
    task: DownloadTaskEntity,
    onDismiss: () -> Unit,
    onShareFile: (DownloadTaskEntity) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(max(task.durationSeconds * 1000L, 15_000L)) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showCaptionsOverlay by remember { mutableStateOf(task.embedSubtitles) }

    val srtLines = remember(task.subtitleFilePath) {
        if (task.subtitleFilePath.isNotBlank() && File(task.subtitleFilePath).exists()) {
            File(task.subtitleFilePath).readLines()
                .filter { it.isNotBlank() && !it.contains("-->") && it.toIntOrNull() == null }
        } else {
            listOf(
                "[StreamForge Studio • ${task.subtitleLanguage.uppercase()} Subtitles]",
                task.title
            )
        }
    }

    val exoPlayer = remember(task.id) {
        ExoPlayer.Builder(context).build().apply {
            val localFile = File(task.outputFilePath)
            val mediaUri = if (localFile.exists() && localFile.length() > 0L) {
                Uri.fromFile(localFile)
            } else if (task.directMediaUrl.isNotBlank()) {
                Uri.parse(task.directMediaUrl)
            } else {
                Uri.parse(task.url)
            }
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (duration > 0) {
                        durationMs = duration
                    }
                }
            })
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (exoPlayer.duration > 0) {
                durationMs = exoPlayer.duration
            }
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(250)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("built_in_player_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BUILT-IN STUDIO PLAYER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${task.uploader} • ${task.formatBadgeText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close player"
                    )
                }
            }

            // Video Viewport or Studio Audio Waveform Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (!task.isAudioOnly) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AudioStudioWaveformView(
                        isPlaying = isPlaying,
                        progressFraction = (currentPosMs.toFloat() / max(durationMs, 1L).toFloat()).coerceIn(0f, 1f),
                        audioFormatBadge = task.formatBadgeText
                    )
                }

                // Closed Caption Overlay
                if (task.embedSubtitles && showCaptionsOverlay && srtLines.isNotEmpty()) {
                    val lineIndex = ((currentPosMs / 4000L) % srtLines.size).toInt()
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = srtLines[lineIndex],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Scrubber Slider & Timestamps
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val safeMax = max(durationMs, 1L).toFloat()
                Slider(
                    value = currentPosMs.toFloat().coerceIn(0f, safeMax),
                    onValueChange = { newPos ->
                        currentPosMs = newPos.toLong()
                        exoPlayer.seekTo(newPos.toLong())
                    },
                    valueRange = 0f..safeMax,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seek_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = YtDlpEngineManager.formatSeconds((currentPosMs / 1000L).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = YtDlpEngineManager.formatSeconds((durationMs / 1000L).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Primary Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(target)
                        currentPosMs = target
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0)
                                }
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                IconButton(
                    onClick = {
                        val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs)
                        exoPlayer.seekTo(target)
                        currentPosMs = target
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Playback Speed & Subtitle / Share Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        FilterChip(
                            selected = playbackSpeed == speed,
                            onClick = {
                                playbackSpeed = speed
                                exoPlayer.setPlaybackSpeed(speed)
                            },
                            label = { Text("${speed}x") }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.embedSubtitles) {
                        OutlinedButton(
                            onClick = { showCaptionsOverlay = !showCaptionsOverlay }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClosedCaption,
                                contentDescription = "Toggle subtitles",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { onShareFile(task) },
                        modifier = Modifier.testTag("player_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioStudioWaveformView(
    isPlaying: Boolean,
    progressFraction: Float,
    audioFormatBadge: String
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AudioFile,
                contentDescription = null,
                tint = secondaryColor
            )
            Text(
                text = audioFormatBadge,
                style = MaterialTheme.typography.labelMedium,
                color = secondaryColor
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            val barCount = 36
            val spacing = 6.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(4f)

            for (i in 0 until barCount) {
                val barFraction = i.toFloat() / barCount.toFloat()
                val phaseOffset = if (isPlaying) (progressFraction * 12.0) else 0.0
                val wave = (0.3f + 0.7f * kotlin.math.abs(sin(i * 0.45 + phaseOffset))).toFloat()
                val barHeight = (size.height * wave).coerceAtLeast(10f)
                val topY = (size.height - barHeight) / 2f
                val leftX = i * (barWidth + spacing)

                drawRoundRect(
                    color = if (barFraction <= progressFraction) activeColor else inactiveColor,
                    topLeft = Offset(leftX, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        Text(
            text = if (isPlaying) "MASTER AUDIO STREAMING" else "AUDIO PAUSED",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
