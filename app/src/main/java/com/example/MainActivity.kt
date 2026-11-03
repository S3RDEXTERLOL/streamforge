package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.QueuePlayNext
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DownloadStatus
import com.example.data.DownloadTaskEntity
import com.example.ui.StreamForgeTab
import com.example.ui.StreamForgeViewModel
import com.example.ui.screens.BuiltInMediaPlayerSheet
import com.example.ui.screens.ExtractScreen
import com.example.ui.screens.FormatConfigBottomSheet
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SavePresetDialog
import com.example.ui.theme.StreamForgeTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: StreamForgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingShareIntent(intent)

        setContent {
            StreamForgeTheme(darkTheme = true) {
                StreamForgeApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.onExternalSharedText(sharedText)
                intent.removeExtra(Intent.EXTRA_TEXT)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamForgeApp(
    viewModel: StreamForgeViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle hardware/gesture back button on secondary tabs
    BackHandler(enabled = uiState.currentTab != StreamForgeTab.EXTRACT) {
        viewModel.selectTab(StreamForgeTab.EXTRACT)
    }

    // Check clipboard on app resume for automatic URL detection banner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val clipText = readPrimaryClipboardText(context)
                viewModel.onClipboardTextChecked(clipText)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Show status toast/snackbar messages
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeStatusMessage()
        }
    }

    val activeQueueCount = allTasks.count {
        it.statusEnum == DownloadStatus.DOWNLOADING ||
            it.statusEnum == DownloadStatus.QUEUED ||
            it.statusEnum == DownloadStatus.PAUSED ||
            it.statusEnum == DownloadStatus.PROCESSING
    }
    val completedLibraryCount = allTasks.count {
        it.statusEnum == DownloadStatus.COMPLETED
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    StreamForgeBottomBar(
                        currentTab = uiState.currentTab,
                        activeQueueCount = activeQueueCount,
                        completedLibraryCount = completedLibraryCount,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    StreamForgeNavigationRail(
                        currentTab = uiState.currentTab,
                        activeQueueCount = activeQueueCount,
                        completedLibraryCount = completedLibraryCount,
                        onSelectTab = viewModel::selectTab
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.currentTab) {
                        StreamForgeTab.EXTRACT -> {
                            ExtractScreen(
                                urlInput = uiState.urlInput,
                                detectedClipboardUrl = uiState.detectedClipboardUrl,
                                isInspecting = uiState.isInspecting,
                                inspectionError = uiState.inspectionError,
                                engineStatus = engineStatus,
                                formatConfig = uiState.formatConfig,
                                presets = presets,
                                selectedPresetId = uiState.selectedPresetId,
                                sampleStreams = viewModel.sampleStreams,
                                onUrlChange = viewModel::updateUrlInput,
                                onPasteFromClipboard = {
                                    val clipText = readPrimaryClipboardText(context)
                                    if (!clipText.isNullOrBlank()) {
                                        viewModel.updateUrlInput(clipText)
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Clipboard is empty")
                                        }
                                    }
                                },
                                onApplyClipboardBanner = viewModel::applyClipboardUrl,
                                onDismissClipboardBanner = viewModel::dismissClipboardBanner,
                                onInspectClick = { viewModel.inspectCurrentUrl() },
                                onQuickDownloadClick = { viewModel.quickDownloadUrl() },
                                onSelectPreset = viewModel::applyPreset,
                                onDeletePreset = viewModel::deletePreset,
                                onOpenSavePresetDialog = viewModel::openSavePresetDialog,
                                onSetAudioOnly = viewModel::setAudioOnlyMode,
                                onSelectVideoResolution = viewModel::selectVideoResolution,
                                onSelectAudioFormat = viewModel::selectAudioFormat,
                                onToggleSubtitles = viewModel::toggleEmbedSubtitles,
                                onSelectSubtitleLanguage = viewModel::selectSubtitleLanguage,
                                onSelectSampleStream = { sample, inspect ->
                                    if (inspect) {
                                        viewModel.inspectCurrentUrl(sample.url)
                                    } else {
                                        viewModel.quickDownloadUrl(sample.url)
                                    }
                                }
                            )
                        }

                        StreamForgeTab.QUEUE -> {
                            QueueScreen(
                                tasks = allTasks,
                                onPauseTask = viewModel::pauseDownload,
                                onResumeTask = viewModel::resumeDownload,
                                onCancelTask = viewModel::cancelDownload,
                                onDeleteTask = viewModel::deleteTaskAndFile,
                                onPlayTask = viewModel::openBuiltInPlayer,
                                onPauseAll = viewModel::pauseAllActive,
                                onResumeAll = viewModel::resumeAllPaused,
                                onGoToExtract = { viewModel.selectTab(StreamForgeTab.EXTRACT) }
                            )
                        }

                        StreamForgeTab.LIBRARY -> {
                            LibraryScreen(
                                tasks = allTasks,
                                activeFilter = uiState.libraryFilter,
                                searchQuery = uiState.librarySearchQuery,
                                onFilterChange = viewModel::setLibraryFilter,
                                onSearchQueryChange = viewModel::updateLibrarySearch,
                                onPlayMedia = viewModel::openBuiltInPlayer,
                                onShareMedia = { task ->
                                    shareDownloadedTask(context, task)
                                },
                                onDeleteMedia = viewModel::deleteTaskAndFile,
                                onGoToExtract = { viewModel.selectTab(StreamForgeTab.EXTRACT) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Format & Playlist Configuration Bottom Sheet
    val inspectedMedia = uiState.inspectedMedia
    if (uiState.showFormatSheet && inspectedMedia != null) {
        FormatConfigBottomSheet(
            media = inspectedMedia,
            config = uiState.formatConfig,
            selectedPlaylistIds = uiState.selectedPlaylistEntryIds,
            onDismiss = viewModel::closeFormatSheet,
            onSetAudioOnly = viewModel::setAudioOnlyMode,
            onSelectVideoResolution = viewModel::selectVideoResolution,
            onSelectVideoContainer = viewModel::selectVideoContainer,
            onSelectAudioFormat = viewModel::selectAudioFormat,
            onToggleSubtitles = viewModel::toggleEmbedSubtitles,
            onSelectSubtitleLanguage = viewModel::selectSubtitleLanguage,
            onToggleSidecarSrt = viewModel::toggleSidecarSrt,
            onToggleSponsorBlock = viewModel::toggleSponsorBlock,
            onTogglePlaylistEntry = viewModel::togglePlaylistEntrySelection,
            onSelectAllPlaylistEntries = viewModel::selectAllPlaylistEntries,
            onSaveAsPresetClick = viewModel::openSavePresetDialog,
            onCopyCliCommand = { cmd ->
                copyTextToClipboard(context, cmd)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Copied yt-dlp command to clipboard")
                }
            },
            onConfirmDownload = viewModel::confirmAndStartConfiguredDownload
        )
    }

    // Save Format Preset Dialog
    if (uiState.showSavePresetDialog) {
        SavePresetDialog(
            formatConfig = uiState.formatConfig,
            onDismiss = viewModel::closeSavePresetDialog,
            onConfirmSave = viewModel::saveCurrentConfigAsPreset
        )
    }

    // Built-in Media3 ExoPlayer Modal Sheet
    val activePlayerTask = uiState.activePlayerTask
    if (activePlayerTask != null) {
        BuiltInMediaPlayerSheet(
            task = activePlayerTask,
            onDismiss = viewModel::closeBuiltInPlayer,
            onShareFile = { task -> shareDownloadedTask(context, task) }
        )
    }
}

@Composable
private fun StreamForgeBottomBar(
    currentTab: StreamForgeTab,
    activeQueueCount: Int,
    completedLibraryCount: Int,
    onSelectTab: (StreamForgeTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        StreamForgeTab.entries.forEach { tab ->
            val selected = currentTab == tab
            val badgeCount = when (tab) {
                StreamForgeTab.EXTRACT -> 0
                StreamForgeTab.QUEUE -> activeQueueCount
                StreamForgeTab.LIBRARY -> completedLibraryCount
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge { Text(badgeCount.toString()) }
                            }
                        }
                    ) {
                        val icon = when (tab) {
                            StreamForgeTab.EXTRACT -> if (selected) Icons.Filled.Download else Icons.Outlined.Download
                            StreamForgeTab.QUEUE -> if (selected) Icons.Filled.QueuePlayNext else Icons.Outlined.QueuePlayNext
                            StreamForgeTab.LIBRARY -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
                        }
                        Icon(imageVector = icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label) }
            )
        }
    }
}

@Composable
private fun StreamForgeNavigationRail(
    currentTab: StreamForgeTab,
    activeQueueCount: Int,
    completedLibraryCount: Int,
    onSelectTab: (StreamForgeTab) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .testTag("side_nav_rail"),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        StreamForgeTab.entries.forEach { tab ->
            val selected = currentTab == tab
            val badgeCount = when (tab) {
                StreamForgeTab.EXTRACT -> 0
                StreamForgeTab.QUEUE -> activeQueueCount
                StreamForgeTab.LIBRARY -> completedLibraryCount
            }

            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.testTag("nav_rail_${tab.route}"),
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge { Text(badgeCount.toString()) }
                            }
                        }
                    ) {
                        val icon = when (tab) {
                            StreamForgeTab.EXTRACT -> if (selected) Icons.Filled.Download else Icons.Outlined.Download
                            StreamForgeTab.QUEUE -> if (selected) Icons.Filled.QueuePlayNext else Icons.Outlined.QueuePlayNext
                            StreamForgeTab.LIBRARY -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
                        }
                        Icon(imageVector = icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label) }
            )
        }
    }
}

private fun readPrimaryClipboardText(context: Context): String? {
    return runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val item = clipboard?.primaryClip?.getItemAt(0)
        item?.coerceToText(context)?.toString()?.trim()
    }.getOrNull()
}

private fun copyTextToClipboard(context: Context, text: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("yt-dlp command", text))
    }
}

private fun shareDownloadedTask(context: Context, task: DownloadTaskEntity) {
    runCatching {
        val file = File(task.outputFilePath)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                type = if (task.isAudioOnly) "audio/*" else "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${task.title} — ${task.url}")
            }
            putExtra(Intent.EXTRA_SUBJECT, task.title)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${task.title}"))
    }
}
