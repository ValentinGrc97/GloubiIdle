package com.example.idleads.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gloubi : une boule de poils à oreilles rondes.
 * - respire en permanence, cligne des yeux
 * - rebondit et scintille quand il est rassasié (>= 99 %)
 * - triste (larme, bouche inversée) quand la faim est basse (< 20 %)
 * - ouvre la bouche et attrape une croquette à chaque repas
 */
@Composable
fun AnimalView(satiety: Float, feedTick: Int, modifier: Modifier = Modifier) {
    val happy = satiety >= 99f
    val sad = satiety < 20f

    val infinite = rememberInfiniteTransition(label = "animal")
    val breathe by infinite.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val hop by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Restart),
        label = "hop"
    )
    val sparkle by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "sparkle"
    )
    val blink by infinite.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4000
                1f at 0
                1f at 3700
                0.05f at 3800
                1f at 3900
            }
        ),
        label = "blink"
    )

    val mouth = remember { Animatable(0f) }  // 0 = fermée, 1 = grande ouverte
    val food = remember { Animatable(1f) }   // 0 = en haut, 1 = mangée (invisible)

    LaunchedEffect(feedTick) {
        if (feedTick > 0) {
            food.snapTo(0f)
            launch { food.animateTo(1f, tween(260, easing = LinearEasing)) }
            mouth.animateTo(1f, tween(110))
            mouth.animateTo(0f, tween(200))
        }
    }

    Canvas(modifier.size(240.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val r = w * 0.30f * breathe
        val ground = h * 0.58f + w * 0.30f
        val bounce = if (happy) abs(sin(hop * PI)).toFloat() * h * 0.09f else 0f
        val cy = h * 0.58f - bounce
        val dark = Color(0xFF2B2B3A)
        val body = when {
            happy -> Color(0xFF7BDFA0)
            sad -> Color(0xFF9AA5B1)
            else -> Color(0xFF8FD3F4)
        }
        val stroke = r * 0.06f

        // ombre
        drawOval(Color(0x22000000), Offset(cx - r * 0.8f, ground - r * 0.06f), Size(r * 1.6f, r * 0.14f))

        // oreilles
        for (side in listOf(-1f, 1f)) {
            drawCircle(body, r * 0.28f, Offset(cx + side * r * 0.70f, cy - r * 0.85f))
            drawCircle(Color(0xFFFFC1CC), r * 0.14f, Offset(cx + side * r * 0.70f, cy - r * 0.85f))
        }

        // pieds
        for (side in listOf(-1f, 1f)) {
            drawCircle(body, r * 0.22f, Offset(cx + side * r * 0.45f, cy + r * 0.95f))
        }

        // corps + ventre
        drawCircle(body, r, Offset(cx, cy))
        drawOval(Color(0x33FFFFFF), Offset(cx - r * 0.55f, cy + r * 0.05f), Size(r * 1.1f, r * 0.85f))

        // joues
        for (side in listOf(-1f, 1f)) {
            drawCircle(Color(0x66FF8FA3), r * 0.13f, Offset(cx + side * r * 0.62f, cy + r * 0.22f))
        }

        // yeux
        val eyeR = r * 0.17f
        val eyeY = cy - r * 0.15f
        for (side in listOf(-1f, 1f)) {
            val ex = cx + side * r * 0.38f
            drawOval(Color.White, Offset(ex - eyeR, eyeY - eyeR * blink), Size(eyeR * 2f, eyeR * 2f * blink))
            if (blink > 0.3f) {
                drawCircle(dark, eyeR * 0.55f, Offset(ex, eyeY + if (sad) eyeR * 0.25f else 0f))
                drawCircle(Color.White, eyeR * 0.15f, Offset(ex + eyeR * 0.2f, eyeY - eyeR * 0.2f))
            }
        }
        if (sad) {
            drawCircle(Color(0xFF6EC1FF), r * 0.06f, Offset(cx + r * 0.50f, eyeY + eyeR * 1.7f))
        }

        // bouche
        val mx = cx
        val my = cy + r * 0.28f
        when {
            mouth.value > 0.05f -> drawOval(
                Color(0xFF7A1F3D),
                Offset(mx - r * 0.20f, my - r * 0.05f),
                Size(r * 0.40f, r * 0.50f * mouth.value)
            )
            sad -> drawArc(
                dark, 200f, 140f, false,
                Offset(mx - r * 0.18f, my + r * 0.05f), Size(r * 0.36f, r * 0.30f),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            happy -> drawArc(
                dark, 20f, 140f, false,
                Offset(mx - r * 0.25f, my - r * 0.15f), Size(r * 0.50f, r * 0.40f),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            else -> drawArc(
                dark, 30f, 120f, false,
                Offset(mx - r * 0.15f, my - r * 0.05f), Size(r * 0.30f, r * 0.20f),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }

        // croquette qui tombe vers la bouche
        if (food.value < 1f) {
            val fy = h * 0.02f + (my - h * 0.02f) * food.value
            drawCircle(Color(0xFFD08A4E), r * 0.10f, Offset(cx, fy))
            drawCircle(Color(0xFF9C5F2C), r * 0.04f, Offset(cx + r * 0.03f, fy - r * 0.02f))
        }

        // étincelles quand l'animal est rassasié
        if (happy) {
            for (i in 0 until 4) {
                val a = (sparkle + i / 4f) * 2f * PI.toFloat()
                val px = cx + cos(a) * r * 1.45f
                val py = cy - r * 0.2f + sin(a) * r * 1.05f
                val s = r * (0.05f + 0.04f * abs(sin(a * 2f)))
                drawCircle(Color(0xFFFFD54F), s, Offset(px, py))
            }
        }
    }
}
