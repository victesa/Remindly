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

        android.util.Log.d("ShareIntentHandler", "handleIntent: action=$action, type=$type, source=$source")

        return when (action) {
            Intent.ACTION_SEND -> {
                when {
                    type == "text/plain" -> {
                        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (text != null) ShareContent.Text(text, source) else ShareContent.Unknown
                    }
                    type?.startsWith("image/") == true -> {
                        val imageUri = getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        if (imageUri != null) ShareContent.Image(imageUri.toString(), source) else ShareContent.Unknown
                    }
                    type == "application/pdf" -> {
                        val pdfUri = getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                        if (pdfUri != null) ShareContent.Pdf(pdfUri.toString(), source) else ShareContent.Unknown
                    }
                    else -> ShareContent.Unknown
                }
            }
            else -> ShareContent.Unknown
        }
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
