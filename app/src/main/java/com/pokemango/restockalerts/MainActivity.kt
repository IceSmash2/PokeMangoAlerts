package com.pokemango.restockalerts

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
                val safe = Alert.safeUrl(url.toString())
                if (safe.isNotEmpty()) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, url)) } catch (e: Exception) { /* no app for this link */ }
                }
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
