package app.hatent.chromer

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {
    private val BLOCK_DOMAINS = hashSetOf(
        "googleadservices.com", "googlesyndication.com", "doubleclick.net",
        "google-analytics.com", "googletagmanager.com", "adnxs.com",
        "criteo.com", "criteo.net", "outbrain.com", "taboola.com",
        "scorecardresearch.com", "rubiconproject.com", "pubmatic.com",
        "openx.net", "popads.net", "zedo.com", "adcolony.com",
        "unityads.unity3d.com", "amazon-adsystem.com", "connect.facebook.net",
        "ads-twitter.com", "analytics.twitter.com", "adroll.com"
    )

    fun isBlocked(url: String): Boolean {
        val host = Uri.parse(url).host?.lowercase() ?: return false
        return BLOCK_DOMAINS.any { host == it || host.endsWith(".$it") }
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
    }
}