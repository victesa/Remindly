package com.victorkirui.remote

import com.victorkirui.core.model.CaptureRequest
import com.victorkirui.core.model.CaptureResponse
import android.net.Uri

interface CaptureApiService {
    suspend fun capture(request: CaptureRequest, mediaUri: Uri? = null): CaptureResponse
}
