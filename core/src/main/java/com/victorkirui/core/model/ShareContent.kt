package com.victorkirui.core.model

sealed class ShareContent {
    data class Text(val content: String, val source: String? = null) : ShareContent()
    data class Image(val uriString: String, val source: String? = null) : ShareContent()
    data class Pdf(val uriString: String, val source: String? = null) : ShareContent()
    object Unknown : ShareContent()
}
