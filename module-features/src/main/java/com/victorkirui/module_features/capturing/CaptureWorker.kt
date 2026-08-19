package com.victorkirui.module_features.capturing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.victorkirui.core.model.ShareContent
import com.victorkirui.core.notification.NotificationHelper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CaptureWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val capturingUseCase: CapturingUseCase by inject()
    private val notificationHelper: NotificationHelper by inject()

    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()
        val source = inputData.getString(KEY_SOURCE)
        val content = inputData.getString(KEY_CONTENT)
        val uriString = inputData.getString(KEY_URI_STRING)
        val sourceUrl = inputData.getString(KEY_SOURCE_URL)

        val shareContent = when (type) {
            "TEXT" -> ShareContent.Text(content ?: "", source)
            "IMAGE" -> ShareContent.Image(uriString ?: "", source, sourceUrl)
            "PDF" -> ShareContent.Pdf(uriString ?: "", source, sourceUrl)
            else -> ShareContent.Unknown
        }

        if (shareContent is ShareContent.Unknown) return Result.failure()

        return when (val result = capturingUseCase(shareContent)) {
            is CaptureResult.Success -> {
                notificationHelper.showNotification("Capture Success", "Content processed and reminders set.", result.itemId)
                Result.success()
            }
            is CaptureResult.Error -> {
                notificationHelper.showNotification("Capture Failed", result.message)
                Result.failure()
            }
            is CaptureResult.SavedLocallyOnly -> {
                notificationHelper.showNotification("Saved Offline", "Analysis will retry shortly.", result.itemId)
                Result.success()
            }
            is CaptureResult.Overdue -> {
                notificationHelper.showNotification("Capture Overdue", "The item was not saved because it is in the past.")
                Result.success()
            }
        }
    }

    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_SOURCE = "source"
        private const val KEY_CONTENT = "content"
        private const val KEY_URI_STRING = "uriString"
        private const val KEY_SOURCE_URL = "sourceUrl"

        fun enqueue(context: Context, shareContent: ShareContent) {
            val dataBuilder = Data.Builder()
            when (shareContent) {
                is ShareContent.Text -> {
                    dataBuilder.putString(KEY_TYPE, "TEXT")
                    dataBuilder.putString(KEY_CONTENT, shareContent.content)
                    dataBuilder.putString(KEY_SOURCE, shareContent.source)
                }
                is ShareContent.Image -> {
                    dataBuilder.putString(KEY_TYPE, "IMAGE")
                    dataBuilder.putString(KEY_URI_STRING, shareContent.uriString)
                    dataBuilder.putString(KEY_SOURCE, shareContent.source)
                    dataBuilder.putString(KEY_SOURCE_URL, shareContent.sourceUrl)
                }
                is ShareContent.Pdf -> {
                    dataBuilder.putString(KEY_TYPE, "PDF")
                    dataBuilder.putString(KEY_URI_STRING, shareContent.uriString)
                    dataBuilder.putString(KEY_SOURCE, shareContent.source)
                    dataBuilder.putString(KEY_SOURCE_URL, shareContent.sourceUrl)
                }
                ShareContent.Unknown -> return
            }

            val workRequest = OneTimeWorkRequestBuilder<CaptureWorker>()
                .setInputData(dataBuilder.build())
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
