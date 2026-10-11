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
    private val context: Context,
    private val authRepository: com.victorkirui.core.repository.AuthRepository
) : CaptureApiService {
    override suspend fun getMe(): com.victorkirui.core.model.UserProfileResponse {
        return try {
            val res = api.getMe()
            Log.d("CaptureApiService", "GET /v1/me successful: success=${res.success}, tier=${res.tier}, isProUser=${res.isProUser}")
            res
        } catch (e: Exception) {
            Log.e("CaptureApiService", "Failed GET /v1/me [${e.javaClass.name}]: ${e.message}", e)
            com.victorkirui.core.model.UserProfileResponse(success = false, message = "${e.javaClass.name}: ${e.message}")
        }
    }


    override suspend fun getCaptures(limit: Int?): List<com.victorkirui.core.model.StoredReminderItem> {
        val response = api.getItems(limit)
        val items = response.items ?: emptyList()
        Log.d("CaptureApiService", "GET /v1/items response: success=${response.success}, userId=${response.userId}, tier=${response.userTier}, count=${response.count}, itemsSize=${items.size}")
        return items
    }


    override suspend fun getSyncDeltas(since: String?): com.victorkirui.core.model.SyncDeltaResponse {
        return try {
            val response = api.getSyncDeltas(since)
            Log.d("CaptureApiService", "GET /v1/items/sync response: success=${response.success}, timestamp=${response.syncTimestamp}, updatedCount=${response.updatedItems?.size}, deletedCount=${response.deletedItemIds?.size}")
            response
        } catch (e: Exception) {
            Log.w("CaptureApiService", "GET /v1/items/sync endpoint not available or failed (${e.message}). Falling back to getCaptures().")
            val fullItems = getCaptures()
            val timestamp = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.now().toString()
            } else {
                java.util.Date().toString()
            }
            com.victorkirui.core.model.SyncDeltaResponse(
                success = true,
                syncTimestamp = timestamp,
                updatedItems = fullItems,
                deletedItemIds = emptyList()
            )
        }
    }

    override suspend fun updateItem(id: String, updates: Map<String, Any?>): Boolean {
        return try {
            api.updateItem(id, updates)
            true
        } catch (e: Exception) {
            Log.e("CaptureApiService", "Failed to update item $id", e)
            false
        }
    }

    override suspend fun deleteItem(id: String): Boolean {
        return try {
            val response = api.deleteItem(id)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("CaptureApiService", "Failed to delete item $id", e)
            false
        }
    }

    override suspend fun purgeAccount(): Boolean {
        return try {
            val response = api.purgeAccount()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("CaptureApiService", "Failed to purge account data", e)
            false
        }
    }

    override suspend fun verifyPurchase(purchaseToken: String, productId: String): com.victorkirui.core.model.VerifyPurchaseResponse {
        return try {
            val userId = authRepository.currentUser.value?.uid
            api.verifyPurchase(com.victorkirui.core.model.VerifyPurchaseRequest(purchaseToken, productId, userId))
        } catch (e: Exception) {
            Log.e("CaptureApiService", "Failed to verify purchase", e)
            com.victorkirui.core.model.VerifyPurchaseResponse(success = false, message = e.message)
        }
    }

    private fun sniffMimeType(file: File): String? {
        return try {
            val bytes = ByteArray(4)
            java.io.FileInputStream(file).use { it.read(bytes) }
            val hex = bytes.joinToString("") { "%02X".format(it) }
            
            when {
                hex.startsWith("25504446") -> "application/pdf"
                hex.startsWith("FFD8FF") -> "image/jpeg"
                hex.startsWith("89504E47") -> "image/png"
                else -> null
            }
        } catch (e: Exception) {
            Log.w("CaptureApiService", "Failed to sniff MIME type: ${e.message}")
            null
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
