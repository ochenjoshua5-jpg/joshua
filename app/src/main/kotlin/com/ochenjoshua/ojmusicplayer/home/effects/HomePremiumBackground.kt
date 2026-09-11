package com.ochenjoshua.ojmusicplayer.home.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun HomePremiumBackground(
    blobColor: Color,
    surfaceColor: Color = MaterialTheme.colorScheme.background,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX: Float = size.width * 0.5f
        val centerY: Float = size.height * 0.15f
        val radius: Float = size.width * 1.15f

        val brush = Brush.radialGradient(
            colors = listOf(
                blobColor.copy(alpha = 0.40f),
                blobColor.copy(alpha = 0.0f)
            ),
            center = Offset(centerX, centerY),
            radius = radius
        )

        drawCircle(
            brush = brush,
            radius = radius,
            center = Offset(centerX, centerY)
        )
    }
}