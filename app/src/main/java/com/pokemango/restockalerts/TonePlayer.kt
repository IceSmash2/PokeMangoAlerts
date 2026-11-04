package com.pokemango.restockalerts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

object TonePlayer {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var audio: AudioManager? = null

    private val attrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    @Synchronized
    fun play(ctx: Context, tone: String, onDone: () -> Unit) {
        release()
        val app = ctx.applicationContext
        val res = if (tone == Prefs.TONE_SHORT) R.raw.alert_short else R.raw.alert_full
        val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs)
            .build()
        am.requestAudioFocus(request)
        audio = am
        focus = request
        val mp = MediaPlayer()
        try {
            val afd = app.resources.openRawResourceFd(res)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setAudioAttributes(attrs)
            mp.setOnCompletionListener { finished(onDone) }
            mp.setOnErrorListener { _, _, _ -> finished(onDone); true }
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            try { mp.release() } catch (ignored: Exception) { }
            release()
            onDone()
        }
    }

    @Synchronized
    fun stop() {
        release()
    }

    @Synchronized
    private fun finished(onDone: () -> Unit) {
        release()
        onDone()
    }

    private fun release() {
        player?.let {
            try { it.stop() } catch (ignored: Exception) { }
            try { it.release() } catch (ignored: Exception) { }
        }
        player = null
        focus?.let { audio?.abandonAudioFocusRequest(it) }
        focus = null
    }
}
