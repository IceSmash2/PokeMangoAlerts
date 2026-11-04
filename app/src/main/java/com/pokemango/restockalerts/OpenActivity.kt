package com.pokemango.restockalerts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class OpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertService.stop(this)
        val url = Alert.safeUrl(intent.getStringExtra("url"))
        if (url.isNotEmpty()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                // no app can open this link
            }
        }
        finish()
    }
}
