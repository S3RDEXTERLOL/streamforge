# StreamForge — On-Device yt-dlp Media Downloader & Studio

StreamForge is an Android media extraction and download utility powered by an embedded on-device `yt-dlp` and `FFmpeg` engine. It gives users a tactile, high-precision interface to inspect media URLs, select video resolutions or lossless/lossy audio formats, batch-select playlist items, embed subtitles, manage an active download queue with pause/resume controls, and play downloaded media directly inside the app.

## User Review & Critical Decisions

> [!IMPORTANT]
> All core architectural and workflow choices below reflect your confirmed selections from Phase 1. Review the plan and click **Proceed** to begin building, or let us know if you want any adjustments.

- **Confirmed Decision 1 — Execution Engine**: **Embedded On-Device `yt-dlp` Engine (`youtubedl-android` + `ffmpeg`)** running directly on the device for zero-server-setup extraction, format inspection, audio transcoding, and subtitle muxing, with intelligent direct HTTP stream fallback when needed.
- **Confirmed Decision 2 — Format & Extraction Controls**: **Video Resolution Picker (`4K`, `1080p`, `720p`, `480p`)**, **Audio-Only Extraction (`MP3`, `M4A`, `OPUS`, `FLAC`)**, **Subtitles & Closed Captions Embedding** (with language selection and `.srt`/embedded toggle), and **Playlist & Multi-Item Batch Selection**.
- **Confirmed Decision 3 — Workflow & Library Features**: **Active Download Queue** with real-time progress, speed, ETA, pause, resume, and retry; **Built-in Media Player & Downloaded Library**; **Automatic Clipboard URL Detection & Android Share Sheet (`ACTION_SEND`) Integration**; and **Saved Format Presets** for one-tap quick downloads.

---

## 1. Overview & Core Concept

- **What It Does**: StreamForge turns complex `yt-dlp` command-line capabilities into a streamlined, three-tab Android studio (**Extract**, **Queue**, **Library**). Users can paste or share any supported media or playlist URL, inspect real-time stream metadata and available streams, apply a saved preset for instant downloading, or fine-tune video resolution, audio codec, subtitle tracks, and playlist items before queuing.
- **Target Audience / Persona**: Creators, commuters, archivists, and power users who want reliable offline access to video and audio streams with full control over container formats, codecs, and subtitles without typing terminal commands.
- **Key Value**: Combines the power of `yt-dlp` + `FFmpeg` with a responsive Material 3 interface, one-tap presets, clipboard/Share-sheet automation, and an integrated media player.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Zero-State & Clipboard / Share Detection**:
   - Launching the app opens the **Extract** hub. If a valid URL is detected on the system clipboard, a floating **Clipboard Smart Banner** appears at the top with a one-tap *"Inspect & Download"* or *"Quick Preset Download"* action.
   - Sharing a link from any external app via Android's system Share sheet (`Intent.ACTION_SEND`) opens StreamForge directly into the Format & Stream Configuration Sheet with metadata inspection automatically triggered.
2. **One-Tap Preset vs. Custom Format Inspection**:
   - Below the URL input bar, a horizontal carousel of **Saved Format Presets** (e.g., *1080p MP4 + Subtitles*, *Best Audio MP3 320k*, *Archival 4K MKV*, *Podcast M4A*, plus user-created presets) lets users start a download in a single tap.
   - Tapping **Inspect Formats** fetches stream metadata (thumbnail, title, channel/uploader, duration, available video/audio streams, subtitle tracks, or playlist entries) and opens the interactive configuration sheet.
3. **Format & Playlist Configuration Sheet**:
   - **Mode Toggle**: Switch cleanly between **Video + Audio** (`4K 2160p`, `1080p FHD`, `720p HD`, `480p SD` with container & codec badges) and **Audio Only** (`MP3`, `M4A`, `OPUS`, `FLAC` with bitrate/quality selector).
   - **Subtitles & Captions Bar**: Toggle subtitle embedding, choose preferred language (`en`, `es`, `fr`, `de`, `ja`, `auto`), and select embedded vs. sidecar `.srt`/`.vtt`.
   - **Playlist Batch Selector**: When a playlist or multi-item URL is detected, users can select/deselect individual items, *Select All / Invert*, and apply the chosen format across the entire batch.
   - **Save as Preset**: Any custom combination of format, quality, and subtitle rules can be saved as a reusable one-tap preset.
4. **Active Download Queue (`Queue` Tab)**:
   - Displays active, paused, queued, and failed jobs with live progress bars, downloaded/total byte counters, transfer speed, ETA, and the generated `yt-dlp` command flags pill (expandable for transparency).
   - Each job supports **Pause**, **Resume**, **Cancel**, and **Retry**.
5. **Downloaded Media Library & Built-in Player (`Library` Tab)**:
   - Filterable by **All**, **Video**, and **Audio**, with search and sort (Newest, Size, Duration).
   - Tapping any item launches the **Built-in Media Player** (powered by Jetpack Media3 / ExoPlayer) supporting video playback with aspect-ratio controls, subtitle rendering, and an audio player bar with seek, playback speed (`0.75x`–`2.0x`), share, and file details.

### Visual Identity & Theme

- **Aesthetic Direction**: **Precision Dark Studio** — inspired by tactile broadcast and audio hardware interfaces (Teenage Engineering / studio field recorders meet modern Material 3). Crisp geometric cards, monospace telemetry badges for bitrates/resolutions/speeds, and atmospheric radial surface depth.
- **Color Palette & Mood**:
  - **Surface / Background (`background` / `surface`)**: Deep Obsidian (`#0B0E14`) and Elevated Slate (`#131822`) with subtle card containers (`#1B2230`).
  - **Primary Accent (`primary`)**: Electric Cyan-Mint (`#00E5A3`) for primary extraction actions, active progress indicators, and selected format pills.
  - **Secondary Accent (`secondary`)**: Signal Amber (`#FFB020`) for audio-only badges, lossless FLAC indicators, and paused state telemetry.
  - **Tertiary Accent (`tertiary`)**: Hyper Indigo (`#6E7BF2`) for subtitle badges, playlist counters, and preset tags.
  - **Adaptive Support**: Full dark and light theme palettes defined semantically via `MaterialTheme.colorScheme`.
- **Typography & Hierarchy** (bundled locally via `font-util`):
  - **Display & Headlines**: **Space Grotesk** — bold, technical, high-contrast headers for screen titles and media titles.
  - **Body & UI Labels**: **Plus Jakarta Sans** — crisp legibility for controls, metadata subtitles, and navigation.
  - **Telemetry & CLI Badges**: **JetBrains Mono** — tabular numbers for live download speed (`4.8 MB/s`), progress percentages (`74.2%`), resolutions (`2160p60`), and `yt-dlp` flag previews.
- **Interactive Feedback & Motion**:
  - Spring-physics animated progress bars, staggered list item reveals, tactile chip selection transitions, and expandable `yt-dlp` console log drawers on active downloads.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Embedded `youtubedl-android` + `FFmpeg` with Resilient Stream Fallback**
  - *Chosen Approach*: Initialize `YoutubeDL` and `FFmpeg` on startup on a background dispatcher, execute real `YoutubeDLRequest` commands with process ID tracking for pause/cancel support, and provide an integrated fallback extractor for direct media URLs or offline/sandboxed test streams so the user can test every queue, pause/resume, and player flow immediately.
  - *Why*: Gives full native `yt-dlp` power across 1,000+ sites while ensuring that network restrictions or direct MP4/MP3 links still work seamlessly and play back in the built-in player.
- **Decision 2: Local Room Database for Queue, Library, and Format Presets**
  - *Chosen Approach*: Persist download tasks, completed media library records, and user-created format presets in an on-device **Room** SQLite database with reactive `Flow` queries.
  - *Why*: Downloaded media files live on the device's local storage; Room guarantees instant startup, zero sign-in friction, and reliable state recovery if a download is paused and resumed across app restarts.
- **Decision 3: Pause & Resume Execution Strategy**
  - *Chosen Approach*: Combine `YoutubeDL.getInstance().destroyProcessById(taskId)` with `yt-dlp`'s native `--continue` (`-c`) partial file resumption flag and HTTP `Range` header resumption so pausing an active download cleanly halts transfer and resuming continues from the exact byte offset on disk.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        ANDROID SYSTEM & INTENTS                             │
│   ClipboardManager (Auto-Detect)       Intent.ACTION_SEND (Share Sheet)     │
└─────────────────┬───────────────────────────────────────┬───────────────────┘
                  │                                       │
                  ▼                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     JETPACK COMPOSE UI LAYER (Material 3)                   │
│                                                                             │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌───────────────────┐  │
│  │     EXTRACT HUB      │  │    DOWNLOAD QUEUE    │  │   MEDIA LIBRARY   │  │
│  │ • URL Bar & Paste    │  │ • Active Progress    │  │ • Video/Audio     │  │
│  │ • Clipboard Banner   │  │ • Speed / ETA / CLI  │  │   Filter & Search │  │
│  │ • Preset Carousel    │  │ • Pause / Resume /   │  │ • Built-in Media3 │  │
│  │ • Format & Playlist  │  │   Cancel / Retry     │  │   ExoPlayer Modal │  │
│  │   Config BottomSheet │  │ • Live Console Log   │  │ • Share / Delete  │  │
│  └──────────┬───────────┘  └──────────┬───────────┘  └─────────┬─────────┘  │
└─────────────┼─────────────────────────┼────────────────────────┼────────────┘
              │                         │                        │
              ▼                         ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    MAIN VIEWMODEL (Unidirectional StateFlow)                │
│     UiState: ExtractState • QueueListState • LibraryState • PlayerState     │
└─────────────┬──────────────────────────────────────────────────┬────────────┘
              │                                                  │
              ▼                                                  ▼
┌────────────────────────────────────────┐  ┌─────────────────────────────────┐
│       EXTRACTION & DOWNLOAD ENGINE     │  │     ROOM PERSISTENCE LAYER      │
│ • YoutubeDL + FFmpeg Native Bridge     │  │ • DownloadTaskDao (Queue Flow)  │
│ • Metadata & Playlist Inspector        │  │ • MediaLibraryDao (Saved Files) │
│ • Flag Builder (-f, -x, --write-subs)  │  │ • FormatPresetDao (One-Tap)     │
│ • Process Controller (Pause/Resume/-c) │  │ • Local App Media Storage Dir   │
└────────────────────────────────────────┘  └─────────────────────────────────┘
```

### Data Model & State

1. **`DownloadTaskEntity` (Room Table `download_tasks`)**:
   - `id`, `url`, `title`, `uploader`, `thumbnailUrl`, `durationSeconds`, `isAudioOnly`, `videoResolution` (`2160p`, `1080p`, `720p`, `480p`), `audioFormat` (`MP3`, `M4A`, `OPUS`, `FLAC`), `embedSubtitles`, `subtitleLang`, `status` (`QUEUED`, `FETCHING_INFO`, `DOWNLOADING`, `PROCESSING`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELED`), `progressPercent`, `downloadedBytes`, `totalBytes`, `speedText`, `etaText`, `cliCommandPreview`, `outputFilePath`, `lastLogLine`, `createdAt`.
2. **`FormatPresetEntity` (Room Table `format_presets`)**:
   - `id`, `name`, `isAudioOnly`, `videoResolution`, `videoContainer`, `audioFormat`, `audioBitrate`, `embedSubtitles`, `subtitleLanguage`, `isDefault`.
3. **`MediaMetadata` & `PlaylistEntry` (Domain Models)**:
   - Parsed representation of single videos or multi-item playlists with selectable per-item checkboxes, available resolution streams, audio codecs, and subtitle languages.

### Interactive Component & State Mapping

- **URL Input & Clipboard Banner**:
  - Typing or pasting updates `urlInput`; observing window focus checks `ClipboardManager` for HTTP/HTTPS URLs and surfaces a dismissible banner with **Inspect** and **Quick Download (Active Preset)** actions.
  - Also includes a **Sample Media Showcase** button for instant testing with real Creative Commons multi-resolution video/audio/subtitle streams and multi-item playlists.
- **Format & Playlist Sheet**:
  - Switching between **Video** and **Audio-Only** dynamically updates the live `yt-dlp` command preview pill (e.g., `yt-dlp -f "bv*[height<=1080]+ba/b[height<=1080]" --embed-subs --sub-langs en`).
  - For playlists, toggling item checkboxes updates the batch count (`Download 4 Selected Items`) and enqueues individual tracked tasks into the Room-backed queue.
- **Queue Controller**:
  - Enqueuing launches a coroutine job managed by `DownloadEngineManager`. Progress callbacks update Room and in-memory `StateFlow` at throttled intervals for smooth 60fps UI rendering.
  - Tapping **Pause** halts the process ID via `destroyProcessById` and marks status `PAUSED`; tapping **Resume** re-invokes `yt-dlp` with `-c` (continue partial file) into the same working directory.
- **Media Library & Built-in Player**:
  - Completed downloads verify the output file on disk and transition into the Library view.
  - Tapping **Play** opens the full-featured Media3 `ExoPlayer` sheet with play/pause, timeline scrubbing, playback speed selector, audio/video mode display, external share (`FileProvider`), and delete confirmation.
