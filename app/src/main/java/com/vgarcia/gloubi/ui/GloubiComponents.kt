package com.vgarcia.gloubi.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vgarcia.gloubi.R
import com.vgarcia.gloubi.ui.theme.GloubiGreen
import com.vgarcia.gloubi.ui.theme.GloubiOrange
import com.vgarcia.gloubi.ui.theme.GloubiRed
import com.vgarcia.gloubi.ui.theme.GloubiSurface2
import com.vgarcia.gloubi.ui.theme.GloubiYellow

/**
 * Barre de progression custom avec dégradé vert→orange→rouge selon la valeur.
 * [critical] : active l'animation de tremblement quand la valeur est basse.
 */
@Composable
fun GloubiProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    critical: Boolean = false
) {
    val clamp = progress.coerceIn(0f, 1f)

    val trackColor = GloubiSurface2
    val fillColor = when {
        clamp > 0.6f -> Brush.horizontalGradient(listOf(GloubiGreen, Color(0xFF81C784)))
        clamp > 0.3f -> Brush.horizontalGradient(listOf(GloubiOrange, GloubiYellow))
        else         -> Brush.horizontalGradient(listOf(GloubiRed, Color(0xFFFF8A65)))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val scale = if (critical) {
        infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.015f,
            animationSpec = infiniteRepeatable(tween(140, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "shakeScale"
        ).value
    } else {
        1f
    }

    Box(
        modifier
            .scale(scale)
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(clamp)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(fillColor)
        )
    }
}

/** Badge de niveau circulaire. */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8C42)))
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            stringResource(R.string.lvl) + level,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = Color(0xFF1A1A00)
        )
    }
}