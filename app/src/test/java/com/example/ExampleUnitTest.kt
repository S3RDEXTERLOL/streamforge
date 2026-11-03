package com.example

import com.example.data.AudioFormat
import com.example.data.FormatConfigState
import com.example.data.SubtitleLanguage
import com.example.data.VideoResolution
import com.example.data.YtDlpEngineManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatConfig_buildsValidYtDlpFlags() {
        val videoConfig = FormatConfigState(
            isAudioOnly = false,
            videoResolution = VideoResolution.RES_4K,
            embedSubtitles = true,
            subtitleLanguage = SubtitleLanguage.ES
        )
        val videoCli = videoConfig.buildCliPreview("https://example.com/watch?v=123")
        assertTrue(videoCli.contains("height<=2160"))
        assertTrue(videoCli.contains("--write-subs"))
        assertTrue(videoCli.contains("--sub-langs es"))

        val audioConfig = FormatConfigState(
            isAudioOnly = true,
            audioFormat = AudioFormat.FLAC,
            embedSubtitles = false
        )
        val audioCli = audioConfig.buildCliPreview("https://example.com/watch?v=123")
        assertTrue(audioCli.contains("-x"))
        assertTrue(audioCli.contains("--audio-format flac"))
    }

    @Test
    fun extractFirstUrl_parsesUrlFromSharedText() {
        val shared = "Check out this video https://studio.blender.org/playlist/open-movie-anthology right now!"
        assertEquals(
            "https://studio.blender.org/playlist/open-movie-anthology",
            YtDlpEngineManager.extractFirstUrl(shared)
        )
    }
}
