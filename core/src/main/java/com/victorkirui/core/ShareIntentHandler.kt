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

        android.util.Log.d("ShareIntentHandler", "handleIntent: action=$action, type=$type, source=$source, extraText=$extraText")

        return when (action) {
            Intent.ACTION_SEND -> {
                val uri = getStreamUri(intent)
                android.util.Log.d("ShareIntentHandler", "Resolved URI: $uri")

                val isImage = type?.startsWith("image/") == true || isImageUri(uri)
                val isPdf = type == "application/pdf" || isPdfUri(uri)

                when {
                    isImage && uri != null -> {
                        ShareContent.Image(uri.toString(), source, extractedUrl)
                    }
                    isPdf && uri != null -> {
                        ShareContent.Pdf(uri.toString(), source, extractedUrl)
                    }
                    uri != null -> {
                        // Fallback: If it's a URI we don't recognize, treat it as a PDF/Document for now
                        ShareContent.Pdf(uri.toString(), source, extractedUrl)
                    }
                    extraText != null -> {
                        ShareContent.Text(extraText, source)
                    }
                    else -> ShareContent.Unknown
                }
            }
            else -> ShareContent.Unknown
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
