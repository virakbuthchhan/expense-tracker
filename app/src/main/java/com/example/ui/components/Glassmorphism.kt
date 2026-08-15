package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated Ambient Mesh background with floating soft glowing radial gradients.
 * Gives glassmorphic surfaces a luminous, premium backdrop to refract against.
 */
@Composable
fun GlassBackgroundMesh(
    modifier: Modifier = Modifier,
    isAnimated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.background

    val infiniteTransition = rememberInfiniteTransition(label = "mesh_transition")

    val animProgress by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient_drift"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Base background fill
            drawRect(color = surfaceColor)

            // Top-Right Glowing Orb
            val orb1X = width * (0.8f + (animProgress - 0.5f) * 0.2f)
            val orb1Y = height * (0.15f + (0.5f - animProgress) * 0.1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.22f),
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = width * 0.65f
                ),
                center = Offset(orb1X, orb1Y),
                radius = width * 0.65f
            )

            // Bottom-Left Glowing Orb
            val orb2X = width * (0.2f + (0.5f - animProgress) * 0.25f)
            val orb2Y = height * (0.75f + (animProgress - 0.5f) * 0.15f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = 0.18f),
                        tertiaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = width * 0.7f
                ),
                center = Offset(orb2X, orb2Y),
                radius = width * 0.7f
            )

            // Center Subtle Accent Glow
            val orb3X = width * (0.5f + (animProgress - 0.5f) * 0.1f)
            val orb3Y = height * (0.45f + (animProgress - 0.5f) * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = width * 0.45f
                ),
                center = Offset(orb3X, orb3Y),
                radius = width * 0.45f
            )
        }

        content()
    }
}

/**
 * Modern Frosted Glass Card with translucent layered background and gradient highlight border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    isGlassEnabled: Boolean = true,
    elevation: Dp = 4.dp,
    containerColor: Color? = null,
    borderAlpha: Float = 0.45f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val baseSurface = containerColor ?: MaterialTheme.colorScheme.surface

    val glassBackground = if (isGlassEnabled) {
        val alpha = if (isDark) 0.55f else 0.72f
        baseSurface.copy(alpha = alpha)
    } else {
        baseSurface
    }

    val borderStroke = if (isGlassEnabled) {
        val topHighlight = if (isDark) Color.White.copy(alpha = borderAlpha * 0.7f) else Color.White.copy(alpha = borderAlpha)
        val bottomHighlight = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
        BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(topHighlight, bottomHighlight),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isGlassEnabled) elevation else elevation * 0.5f,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            .clip(shape)
            .background(glassBackground)
            .border(borderStroke, shape)
            .then(clickableModifier)
    ) {
        content()
    }
}

/**
 * Translucent Glass Button with soft highlight shine and haptic feedback
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    isGlassEnabled: Boolean = true,
    isPrimary: Boolean = false,
    content: @Composable () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    val backgroundBrush = if (isPrimary) {
        Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = if (isGlassEnabled) 0.90f else 1.0f),
                primaryColor.copy(alpha = if (isGlassEnabled) 0.75f else 1.0f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                surfaceColor.copy(alpha = if (isGlassEnabled) 0.65f else 0.95f),
                surfaceColor.copy(alpha = if (isGlassEnabled) 0.45f else 0.90f)
            )
        )
    }

    val borderStroke = if (isPrimary) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundBrush)
            .border(borderStroke, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Translucent Pill Badge for Tags & Status
 */
@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(50)
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), shape),
        color = color.copy(alpha = 0.12f),
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }
    }
}
