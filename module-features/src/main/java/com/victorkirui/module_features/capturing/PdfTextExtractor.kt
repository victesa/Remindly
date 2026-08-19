package com.victorkirui.module_features.capturing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

class PdfTextExtractor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(uri: Uri): String? {
        Log.d("PdfTextExtractor", "Extracting text from PDF: $uri, scheme: ${uri.scheme}")
        return try {
            val fileDescriptor = if (uri.scheme == "file") {
                val path = uri.path ?: throw Exception("File path is null")
                val file = File(path)
                Log.d("PdfTextExtractor", "Opening file descriptor for path: $path, exists: ${file.exists()}, canRead: ${file.canRead()}")
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                Log.d("PdfTextExtractor", "Opening file descriptor via ContentResolver for URI: $uri")
                context.contentResolver.openFileDescriptor(uri, "r")
            } ?: throw Exception("File descriptor is null")

            val pdfRenderer = PdfRenderer(fileDescriptor)
            Log.d("PdfTextExtractor", "PdfRenderer opened. Page count: ${pdfRenderer.pageCount}")
            
            val stringBuilder = StringBuilder()
            
            // Limit to first 5 pages to avoid excessive processing
            val pageCount = Math.min(pdfRenderer.pageCount, 5)
            
            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                
                // Increase scale for better OCR accuracy
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(image).await()
                
                stringBuilder.append(result.text)
                stringBuilder.append("\n")
                
                page.close()
                bitmap.recycle()
            }
            
            pdfRenderer.close()
            fileDescriptor.close()
            
            val extractedText = stringBuilder.toString().trim()
            Log.d("PdfTextExtractor", "Extracted ${extractedText.length} characters")
            extractedText.ifEmpty { null }
        } catch (e: Exception) {
            Log.e("PdfTextExtractor", "Failed to extract text from PDF", e)
            null
        }
    }

    suspend fun extractTextFromImage(uri: Uri): String? {
        Log.d("PdfTextExtractor", "Extracting text from Image: $uri")
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            result.text.trim().ifEmpty { null }
        } catch (e: Exception) {
            Log.e("PdfTextExtractor", "Failed to extract text from Image", e)
            null
        }
    }
}
