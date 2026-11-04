package com.pokemango.restockalerts

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File

object CustomSound {
    private const val MAX_BYTES = 10L * 1024 * 1024

    fun file(ctx: Context): File = File(ctx.applicationContext.filesDir, "custom_sound")

    fun exists(ctx: Context): Boolean = file(ctx).let { it.isFile && it.length() > 0 }

    /** Copies the chosen sound into the app's own storage. True when the copy exists and plays. */
    fun importFrom(ctx: Context, uri: Uri): Boolean {
        val app = ctx.applicationContext
        val temp = File(app.filesDir, "custom_sound.tmp")
        try {
            val input = app.contentResolver.openInputStream(uri) ?: return false
            input.use { source ->
                temp.outputStream().use { out ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val n = source.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) {
                            temp.delete()
                            return false
                        }
                        out.write(buffer, 0, n)
                    }
                }
            }
            if (!playable(temp)) {
                temp.delete()
                return false
            }
            val dest = file(app)
            if (dest.exists()) dest.delete()
            return temp.renameTo(dest)
        } catch (e: Exception) {
            temp.delete()
            return false
        }
    }

    private fun playable(f: File): Boolean {
        val mp = MediaPlayer()
        return try {
            mp.setDataSource(f.absolutePath)
            mp.prepare()
            mp.duration > 0
        } catch (e: Exception) {
            false
        } finally {
            try { mp.release() } catch (ignored: Exception) { }
        }
    }
}
