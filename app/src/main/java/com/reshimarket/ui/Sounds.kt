package com.reshimarket.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** צלילים קצרים שמסונתזים בזמן אמת, כמו בדמו (בלי קבצי שמע) */
object Sounds {
    var enabled = true
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val RATE = 44100

    fun tone(f1: Double, f2: Double, delaySec: Double, dur: Double) {
        if (!enabled) return
        scope.launch {
            delay((delaySec * 1000).toLong())
            runCatching { play(f1, f2, dur) }
        }
    }

    /** "פופ" של מוצר שנכנס לעגלה */
    fun pop(delaySec: Double = 0.0) = tone(420.0, 980.0, delaySec, 0.18)
    /** מוצר שחוזר מהעגלה */
    fun back(delaySec: Double = 0.0) = tone(820.0, 330.0, delaySec, 0.18)
    /** צליל חגיגי של "סיימתי קנייה" */
    fun fanfare() = listOf(523.0, 659.0, 784.0, 1046.0).forEachIndexed { i, f -> tone(f, f * 1.01, i * 0.12, 0.25) }

    private suspend fun play(f1: Double, f2: Double, dur: Double) {
        val n = (RATE * (dur + 0.05)).toInt()
        val buf = ShortArray(n)
        var phase = 0.0
        val sweep = dur * 0.7
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val f = if (t < sweep) f1 * (f2 / f1).pow(t / sweep) else f2
            phase += 2 * PI * f / RATE
            val g = when {
                t < 0.01 -> 0.0001 * (0.25 / 0.0001).pow(t / 0.01)
                t < dur -> 0.25 * (0.0001 / 0.25).pow((t - 0.01) / (dur - 0.01))
                else -> 0.0
            }
            buf[i] = (sin(phase) * g * Short.MAX_VALUE).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(n * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(buf, 0, n)
            track.play()
            delay(((dur + 0.25) * 1000).toLong())
        } finally {
            track.release()
        }
    }
}
