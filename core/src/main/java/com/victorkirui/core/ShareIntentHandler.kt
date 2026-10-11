package com.victorkirui.core

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import com.victorkirui.core.model.ShareContent

class ShareIntentHandler {
    fun handleIntent(intent: Intent?, source: String? = null): ShareContent {
        if (intent == null) return ShareContent.Unknown

        val action = intent.action
        val type = intent.type
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val extractedUrl = extraText?.let { extractUrl(it) }

        val resolvedSource = resolveSource(source, extractedUrl, type)

        android.util.Log.d("ShareIntentHandler", "handleIntent: action=$action, type=$type, rawSource=$source, resolvedSource=$resolvedSource, extraText=$extraText")

        return when (action) {
            Intent.ACTION_SEND -> {
                val uri = getStreamUri(intent)
                android.util.Log.d("ShareIntentHandler", "Resolved URI: $uri")

                val isImage = type?.startsWith("image/") == true || isImageUri(uri)
                val isPdf = type == "application/pdf" || isPdfUri(uri)

                when {
                    isImage && uri != null -> {
                        ShareContent.Image(uri.toString(), resolvedSource, extractedUrl)
                    }
                    isPdf && uri != null -> {
                        ShareContent.Pdf(uri.toString(), resolvedSource, extractedUrl)
                    }
                    uri != null -> {
                        // Fallback: If it's a URI we don't recognize, treat it as a PDF/Document for now
                        ShareContent.Pdf(uri.toString(), resolvedSource, extractedUrl)
                    }
                    extraText != null -> {
                        ShareContent.Text(extraText, resolvedSource)
                    }
                    else -> ShareContent.Unknown
                }
            }
            else -> ShareContent.Unknown
        }
    }

    fun resolveSource(rawSource: String?, url: String?, type: String?): String {
        if (!rawSource.isNullOrBlank() && !rawSource.equals("Unknown", ignoreCase = true) && !rawSource.equals("android", ignoreCase = true)) {
            return rawSource
        }

        val domainSource = detectSourceFromUrl(url)
        if (domainSource != null) return domainSource

        return when {
            type?.startsWith("image/") == true -> "Image"
            type == "application/pdf" -> "PDF"
            else -> "Capture"
        }
    }

    fun detectSourceFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return null

            when {
                host.contains("linkedin.com") || host.contains("lnkd.in") -> "LinkedIn"
                host.contains("twitter.com") || host.contains("x.com") || host.contains("t.co") -> "Twitter"
                host.contains("github.com") -> "GitHub"
                host.contains("youtube.com") || host.contains("youtu.be") -> "YouTube"
                host.contains("instagram.com") -> "Instagram"
                host.contains("reddit.com") || host.contains("redd.it") -> "Reddit"
                host.contains("medium.com") -> "Medium"
                host.contains("facebook.com") || host.contains("fb.watch") -> "Facebook"
                host.contains("tiktok.com") -> "TikTok"
                host.contains("spotify.com") -> "Spotify"
                host.contains("amazon.") || host.contains("amzn.to") -> "Amazon"
                host.contains("gmail.com") || host.contains("mail.google.com") -> "Gmail"
                else -> {
                    val cleanHost = host.removePrefix("www.").substringBefore(".")
                    if (cleanHost.length in 3..20) {
                        cleanHost.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    } else {
                        "Web"
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getStreamUri(intent: Intent): Uri? {
        // 1. Try EXTRA_STREAM
        val streamUri = getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (streamUri != null) return streamUri

        // 2. Try ClipData (Modern Android way)
        intent.clipData?.let { clipData ->
            if (clipData.itemCount > 0) {
                val uri = clipData.getItemAt(0).uri
                if (uri != null) return uri
            }
        }

        // 3. Try data
        return intent.data
    }

    private fun isImageUri(uri: Uri?): Boolean {
        if (uri == null) return false
        val path = uri.toString().lowercase()
        return path.contains(".jpg") || path.contains(".jpeg") || path.contains(".png") || path.contains(".webp")
    }

    private fun isPdfUri(uri: Uri?): Boolean {
        if (uri == null) return false
        val path = uri.toString().lowercase()
        return path.contains(".pdf")
    }

    private fun extractUrl(text: String): String? {
        val urlRegex = "(https?://[\\w\\d\\-.?#=/%&]+)".toRegex()
        return urlRegex.find(text)?.value
    }

    private fun <T : Parcelable> getParcelableExtra(intent: Intent, name: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(name) as? T
        }
    }
}
