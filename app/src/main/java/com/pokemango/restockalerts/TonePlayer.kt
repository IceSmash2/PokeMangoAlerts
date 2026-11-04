package com.pokemango.restockalerts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

object TonePlayer {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var audio: AudioManager? = null
    private var stopper: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private const val MAX_PLAY_MS = 120_000L

    private val attrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    @Synchronized
    fun play(ctx: Context, tone: String, onDone: () -> Unit) {
        release()
        val app = ctx.applicationContext
        val custom = tone == Prefs.TONE_CUSTOM && CustomSound.exists(app)
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
            if (custom) {
                mp.setDataSource(CustomSound.file(app).absolutePath)
            } else {
                val afd = app.resources.openRawResourceFd(res)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }
            mp.setAudioAttributes(attrs)
            mp.setOnCompletionListener { finished(onDone) }
            mp.setOnErrorListener { _, _, _ -> finished(onDone); true }
            mp.prepare()
            mp.start()
            player = mp
            val cut = Runnable { finished(onDone) }
            stopper = cut
            handler.postDelayed(cut, MAX_PLAY_MS)
        } catch (e: Exception) {
            try { mp.release() } catch (ignored: Exception) { }
            release()
            if (custom) play(ctx, Prefs.TONE_FULL, onDone) else onDone()
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
        stopper?.let { handler.removeCallbacks(it) }
        stopper = null
        player?.let {
            try { it.stop() } catch (ignored: Exception) { }
            try { it.release() } catch (ignored: Exception) { }
        }
        player = null
        focus?.let { audio?.abandonAudioFocusRequest(it) }
        focus = null
    }
}
