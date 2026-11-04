package com.pokemango.restockalerts

import android.app.Activity
import android.os.Bundle

class OpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertService.stop(this)
        LinkOpener.open(this, intent.getStringExtra("url") ?: "")
        finish()
    }
}
