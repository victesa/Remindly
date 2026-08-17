package com.victorkirui.module_features.capturing

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.victorkirui.core.R

sealed class SourceIcon {
    data class Resource(val resId: Int) : SourceIcon()
    data class Vector(val imageVector: ImageVector) : SourceIcon()
}

object SourceIconProvider {
    fun getIconForSource(source: String?): SourceIcon {
        val s = source?.lowercase() ?: ""
        return when {
            s.contains("whatsapp") -> SourceIcon.Resource(R.drawable.ic_whatsapp)
            s.contains("linkedin") || s.contains("jobs") -> SourceIcon.Resource(R.drawable.ic_linkedin)
            s.contains("gmail") || s.contains("google.gm") || s.contains("mail") -> SourceIcon.Resource(R.drawable.ic_gmail)
            s.contains("intent") || s.contains("fab") -> SourceIcon.Vector(Icons.Default.Work)
            s.contains("chrome") || s.contains("browser") || s.contains("http") || s.contains("www") || s.contains(".") || 
            s.contains("edge") || s.contains("firefox") || s.contains("safari") -> SourceIcon.Vector(Icons.Default.Link)
            else -> SourceIcon.Vector(Icons.Default.Link)
        }
    }
}
