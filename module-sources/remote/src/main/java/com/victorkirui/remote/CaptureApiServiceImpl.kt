package com.victorkirui.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import com.victorkirui.core.model.CaptureRequest
import com.victorkirui.core.model.CaptureResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class CaptureApiServiceImpl(
    private val api: CaptureApi,
    private val context: Context
) : CaptureApiService {
    override suspend fun capture(request: CaptureRequest, mediaUri: Uri?): CaptureResponse {
        return if (mediaUri != null) {
            val file = getFileFromUri(mediaUri)
            val extension = file.extension.lowercase()
            val detectedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
            
            Log.e("CaptureApiService", "Uploading file: ${file.name}, extension: $extension, mimeType: $detectedMimeType")
            
            val mediaPart = MultipartBody.Part.createFormData(
                "media",
                file.name,
                file.asRequestBody(detectedMimeType.toMediaTypeOrNull())
            )

            val partMap = mutableMapOf<String, RequestBody>()
            val textMediaType = "text/plain".toMediaTypeOrNull()
            partMap["itemId"] = request.itemId.toRequestBody(textMediaType)
            partMap["contentType"] = request.contentType.toRequestBody(textMediaType)
            partMap["capturedAt"] = request.capturedAt.toRequestBody(textMediaType)
            partMap["metadata"] = Gson().toJson(request.metadata).toRequestBody(textMediaType)
            request.extractedText?.let {
                partMap["extractedText"] = it.toRequestBody(textMediaType)
            }

            try {
                val response = api.captureMultipartV2(
                    parts = partMap,
                    media = mediaPart
                )
                Log.e("CaptureApiService", "Response success: ${Gson().toJson(response)}")
                response
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("CaptureApiService", "HTTP Error ${e.code()}: $errorBody")
                throw e
            } finally {
                // Only delete if it's a temporary cache file, not a persistent capture from internal storage
                if (file.exists() && file.path.contains(context.cacheDir.path)) {
                    file.delete()
                }
            }
        } else {
            api.captureJson(request)
        }
    }

    private fun getFileFromUri(uri: Uri): File {
        if (uri.scheme == "file") {
            val file = File(uri.path ?: throw IllegalArgumentException("Invalid file path"))
            if (!file.exists()) {
                throw java.io.FileNotFoundException("Persistent file not found: ${file.absolutePath}")
            }
            return file
        }
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw java.io.FileNotFoundException("Could not open input stream for URI: $uri")
        
        val mimeType = context.contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        
        val fileName = "upload_${System.currentTimeMillis()}${if (extension != null) ".$extension" else ""}"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun getMimeType(uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }
}
