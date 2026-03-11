package com.ravenguard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    highlighted: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (highlighted) 1.03f else 1f,
        animationSpec = spring(),
        label = "info-scale"
    )
    GlassCard(modifier = Modifier.scale(scale)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontWeight = FontWeight.SemiBold)
                Icon(imageVector = icon, contentDescription = title)
            }
            Text(text = value)
        }
    }
}
