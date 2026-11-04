package com.pokemango.restockalerts

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private val base: Uri = Uri.parse(BuildConfig.SERVER_URL)

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        runScript("window.dispatchEvent(new Event('ra-permission'))")
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importSound(uri, nameOf(uri))
    }

    private val pickSystem = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val title = try { RingtoneManager.getRingtone(this, uri)?.getTitle(this) } catch (e: Exception) { null }
                importSound(uri, title)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannels(this)

        web = WebView(this)
        setContentView(web)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
        }
        web.addJavascriptInterface(Bridge(this), "AndroidApp")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.scheme == "https" && url.host == base.host) return false
                if (url.scheme == "file" && url.path == "/android_asset/offline.html") return false
                LinkOpener.open(this@MainActivity, url.toString(), newTask = false)
                return true
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) view.loadUrl("file:///android_asset/offline.html")
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        Events.onHistory = { runScript("window.dispatchEvent(new Event('ra-history'))") }
        Events.onToken = { runScript("window.dispatchEvent(new Event('ra-token'))") }

        askForNotifications()
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token != null && token != Prefs.token(this)) Prefs.setToken(this, token)
        }

        if (savedInstanceState == null) load() else web.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        AlertService.stop(this)
        web.onResume()
        runScript("window.dispatchEvent(new Event('ra-history')); window.dispatchEvent(new Event('ra-permission'))")
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        Events.onHistory = null
        Events.onToken = null
        web.destroy()
        super.onDestroy()
    }

    fun launchFilePicker() {
        pickFile.launch(arrayOf("audio/*"))
    }

    fun launchSystemPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alert sound")
        pickSystem.launch(intent)
    }

    private fun nameOf(uri: Uri): String? {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getString(0)?.substringBeforeLast('.')
            }
        } catch (e: Exception) {
            // no name available
        }
        return null
    }

    private fun importSound(uri: Uri, name: String?) {
        Thread {
            val ok = CustomSound.importFrom(this, uri)
            runOnUiThread {
                if (ok) {
                    Prefs.setCustomName(this, (name ?: "Custom sound").take(40))
                    Prefs.setTone(this, Prefs.TONE_CUSTOM)
                    runScript("window.dispatchEvent(new Event('ra-tone'))")
                } else {
                    Toast.makeText(this, "That sound can’t be used. Try an audio file under 10 MB.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    fun load() {
        web.loadUrl(base.buildUpon().appendEncodedPath("app/").build().toString())
    }

    fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun runScript(js: String) {
        runOnUiThread { if (::web.isInitialized) web.evaluateJavascript(js, null) }
    }
}
