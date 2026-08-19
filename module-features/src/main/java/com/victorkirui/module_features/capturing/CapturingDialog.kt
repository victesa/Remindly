package com.victorkirui.module_features.capturing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CapturingDialog(
    uiState: CapturingUiState,
    onDismiss: () -> Unit
) {
    if (uiState is CapturingUiState.Idle) return

    Dialog(
        onDismissRequest = { 
            if (uiState is CapturingUiState.Success || 
                uiState is CapturingUiState.Error || 
                uiState is CapturingUiState.Overdue ||
                uiState is CapturingUiState.SavedLocally
            ) onDismiss() 
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            when (uiState) {
                                is CapturingUiState.Processing -> Color(0xFFEAF2EE)
                                is CapturingUiState.Overdue -> Color(0xFFFFF4E5) // Light Orange
                                is CapturingUiState.SavedLocally -> Color(0xFFE3F2FD) // Light Blue
                                is CapturingUiState.Error -> Color(0xFFFFEBEE) // Light Red
                                else -> Color(0xFF2D6A4F)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (uiState) {
                        is CapturingUiState.Processing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFF2D6A4F),
                                strokeWidth = 3.dp
                            )
                        }
                        is CapturingUiState.Success -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        is CapturingUiState.SavedLocally -> {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF1976D2), // Blue
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        is CapturingUiState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        is CapturingUiState.Overdue -> {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFED6C02), // Orange
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val title = when (uiState) {
                    CapturingUiState.Processing -> "Analysing Captures"
                    CapturingUiState.Success -> "Successfully Captured"
                    is CapturingUiState.SavedLocally -> "Saved Offline"
                    is CapturingUiState.Error -> "Capture Failed"
                    is CapturingUiState.Overdue -> "Capture Rejected"
                    else -> ""
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val description = when (uiState) {
                    CapturingUiState.Processing -> "We're using AI to extract key details and set reminders for you. This will take a moment."
                    CapturingUiState.Success -> "Everything has been set up! You'll receive a notification shortly."
                    is CapturingUiState.SavedLocally -> uiState.message
                    is CapturingUiState.Error -> uiState.message
                    is CapturingUiState.Overdue -> uiState.message
                    else -> ""
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7C6E),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                if (uiState is CapturingUiState.Success) {
                    val strategy = "Strategy: Cloud Extraction" // This can be dynamic in future
                    Text(
                        text = strategy,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (uiState is CapturingUiState.Success || 
                    uiState is CapturingUiState.Error || 
                    uiState is CapturingUiState.Overdue ||
                    uiState is CapturingUiState.SavedLocally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF9FAF9))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Powered by AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2D6A4F)
                        )
                    }
                }
            }
        }
    }
}
