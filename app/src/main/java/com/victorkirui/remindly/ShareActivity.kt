package com.victorkirui.remindly

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.victorkirui.core.ShareIntentHandler
import com.victorkirui.core.model.ShareContent
import com.victorkirui.module_features.capturing.CaptureWorker

class ShareActivity : ComponentActivity() {

    private val shareIntentParser = ShareIntentHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleShareIntent(intent)
        finish()
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return

        val source = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                referrer?.host ?: "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: Exception) { "Unknown" }

        var sharedContent = shareIntentParser.handleIntent(intent, source)
        
        // Transit copy for URIs to preserve permissions
        sharedContent = when (sharedContent) {
            is ShareContent.Image -> {
                val localUri = copyToTransit(Uri.parse(sharedContent.uriString), "jpg")
                if (localUri != null) sharedContent.copy(uriString = localUri.toString()) else sharedContent
            }
            is ShareContent.Pdf -> {
                val localUri = copyToTransit(Uri.parse(sharedContent.uriString), "pdf")
                if (localUri != null) sharedContent.copy(uriString = localUri.toString()) else sharedContent
            }
            else -> sharedContent
        }

        if (sharedContent !is ShareContent.Unknown) {
            Toast.makeText(this, "Capture received. Analyzing in background...", Toast.LENGTH_SHORT).show()
            CaptureWorker.enqueue(this, sharedContent)
        }
    }

    private fun copyToTransit(uri: Uri, defaultExtension: String): Uri? {
        android.util.Log.d("ShareActivity", "Starting transit copy for URI: $uri, scheme: ${uri.scheme}")
        if (uri.scheme == "file") {
            android.util.Log.d("ShareActivity", "URI is already a file, returning as is.")
            return uri
        }

        return try {
            val type = contentResolver.getType(uri)
            
            var extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            if (extension == null) {
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                val name = cursor.getString(nameIndex)
                                if (name.contains(".")) {
                                    extension = name.substringAfterLast(".")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ShareActivity", "Could not query display name for extension: ${e.message}")
                }
            }
            
            val finalExtension = extension ?: defaultExtension
            val fileName = "transit_${System.currentTimeMillis()}.${finalExtension}"
            val transitFile = java.io.File(cacheDir, fileName)
            
            val inputStream = try {
                contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                try {
                    contentResolver.openFileDescriptor(uri, "r")?.let {
                        android.os.ParcelFileDescriptor.AutoCloseInputStream(it)
                    }
                } catch (e2: Exception) {
                    null
                }
            }

            inputStream?.use { input ->
                java.io.FileOutputStream(transitFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Failed to open input stream for URI: $uri")
            
            Uri.fromFile(transitFile)
        } catch (e: Exception) {
            android.util.Log.e("ShareActivity", "Transit copy failed for URI: $uri", e)
            null
        }
    }
}
