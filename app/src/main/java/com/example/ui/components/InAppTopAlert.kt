package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import kotlinx.coroutines.delay

enum class AlertType {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}

data class TopAlertData(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String? = null,
    val type: AlertType = AlertType.SUCCESS,
    val durationMs: Long = 3200L
)

@Stable
class TopAlertHostState {
    var currentAlert by mutableStateOf<TopAlertData?>(null)
        private set

    fun showAlert(
        title: String,
        message: String? = null,
        type: AlertType = AlertType.SUCCESS,
        durationMs: Long = 3200L
    ) {
        currentAlert = TopAlertData(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            type = type,
            durationMs = durationMs
        )
    }

    fun dismiss() {
        currentAlert = null
    }
}

@Composable
fun rememberTopAlertHostState(): TopAlertHostState {
    return remember { TopAlertHostState() }
}

@Composable
fun InAppTopAlertHost(
    hostState: TopAlertHostState,
    modifier: Modifier = Modifier
) {
    val alert = hostState.currentAlert

    LaunchedEffect(alert?.id) {
        if (alert != null) {
            delay(alert.durationMs)
            if (hostState.currentAlert?.id == alert.id) {
                hostState.dismiss()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(9999f)
            .padding(top = 12.dp, end = 12.dp, start = 36.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        AnimatedVisibility(
            visible = alert != null,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(320, easing = LinearEasing)
            ) + fadeIn(animationSpec = tween(250)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(250, easing = LinearEasing)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            if (alert != null) {
                TopAlertCard(
                    alert = alert,
                    onDismiss = { hostState.dismiss() }
                )
            }
        }
    }
}

@Composable
fun TopAlertCard(
    alert: TopAlertData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(alert.id) { Animatable(1f) }

    LaunchedEffect(alert.id) {
        progress.snapTo(1f)
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = alert.durationMs.toInt(), easing = LinearEasing)
        )
    }

    val (accentColor, iconBgColor, icon) = when (alert.type) {
        AlertType.SUCCESS -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            Icons.Default.CheckCircle
        )
        AlertType.INFO -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            Icons.Default.Info
        )
        AlertType.WARNING -> Triple(
            Color(0xFFF59E0B),
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Icons.Default.Warning
        )
        AlertType.ERROR -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            Icons.Default.Error
        )
    }

    Surface(
        modifier = modifier
            .widthIn(min = 220.dp, max = 340.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp), spotColor = accentColor.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onDismiss() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Alert Type Icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = alert.type.name,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!alert.message.isNullOrBlank()) {
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom Progress Bar Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(accentColor.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.value)
                        .height(2.5.dp)
                        .background(accentColor)
                )
            }
        }
    }
}
