package com.jimmy0933.pokemonkids

import android.media.AudioManager
import android.media.ToneGenerator

/** Simple built-in beeps, no audio files needed. Never crashes the game. */
class Sounds {
    private var tone: ToneGenerator? = null

    fun flip() = play(ToneGenerator.TONE_PROP_BEEP, 70)
    fun match() = play(ToneGenerator.TONE_PROP_ACK, 120)
    fun celebrate() = play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)

    private fun play(toneType: Int, durationMs: Int) {
        try {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            tone?.startTone(toneType, durationMs)
        } catch (_: Exception) {
            // Sound is a nice-to-have for a 4-year-old; silence is fine.
        }
    }

    fun release() {
        try { tone?.release() } catch (_: Exception) { }
        tone = null
    }
}
