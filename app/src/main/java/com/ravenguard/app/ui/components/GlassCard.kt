package com.ravenguard.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ravenguard.app.ui.theme.GlassSurface
import com.ravenguard.app.ui.theme.NeonPurple

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GlassSurface)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.40f), Color.White.copy(alpha = 0.18f))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .animateContentSize()
            .padding(18.dp),
        content = content
    )
}
