package com.pokemango.restockalerts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object LinkOpener {
    private val STORE_APPS = listOf(
        listOf("target.com") to listOf("com.target.ui"),
        listOf("samsclub.com") to listOf("com.rfi.sams.android"),
        listOf("costco.com") to listOf("com.costco.app.android"),
    )

    /** Opens the store's own app when it is installed, otherwise the default app for the link. */
    fun open(ctx: Context, url: String, newTask: Boolean = true): Boolean {
        val safe = Alert.safeUrl(url)
        if (safe.isEmpty()) return false
        val uri = Uri.parse(safe)
        val host = uri.host?.lowercase() ?: return false

        val packages = STORE_APPS
            .firstOrNull { (domains, _) -> domains.any { host == it || host.endsWith(".$it") } }
            ?.second.orEmpty()

        for (pkg in packages) {
            try {
                ctx.startActivity(intent(uri, newTask).setPackage(pkg))
                return true
            } catch (e: ActivityNotFoundException) {
                // that store's app is not installed or can't open this link
            } catch (e: SecurityException) {
                // the app refused the link
            }
        }
        return try {
            ctx.startActivity(intent(uri, newTask))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun intent(uri: Uri, newTask: Boolean): Intent {
        val i = Intent(Intent.ACTION_VIEW, uri)
        if (newTask) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return i
    }
}
