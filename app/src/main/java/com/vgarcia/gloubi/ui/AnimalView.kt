package com.vgarcia.gloubi.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

/**
 * Gloubi : une boule de poils à oreilles rondes.
 * Respire en permanence, cligne des yeux
 * Rebondit et scintille quand il est rassasié (>= 99 %)
 * Triste (larme, bouche inversée) quand la faim est basse (< 20 %)
 * Ouvre la bouche et attrape une croquette à chaque repas
 */
@Composable
fun AnimalView(modifier: Modifier = Modifier, satiety: Float, feedTick: Int, bigFeedTick: Int, boosted: Boolean = false, level: Int) {
    val happy = satiety >= 99f
    val sad = satiety < 20f

    val isPreview = LocalInspectionMode.current

    val breathe: Float
    val hop: Float
    val sparkle: Float
    val blink: Float

    if (isPreview) {
        breathe = 1f
        hop = 0f
        sparkle = 0f
        blink = 1f
    } else {
        val infinite = rememberInfiniteTransition(label = "animal")
        val breatheState = infinite.animateFloat(
            initialValue = 0.97f, targetValue = 1.03f,
            animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breathe"
        )
        val hopState = infinite.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Restart),
            label = "hop"
        )
        val sparkleState = infinite.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
            label = "sparkle"
        )
        val blinkState = infinite.animateFloat(
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
        breathe = breatheState.value
        hop = hopState.value
        sparkle = sparkleState.value
        blink = blinkState.value
    }

    val mouth = remember { Animatable(0f) }  // 0 = fermée, 1 = grande ouverte
    val food = remember { Animatable(1f) }   // 0 = en haut, 1 = mangée (invisible)

    // Éclats boost
    val burstProgress = remember { Animatable(0f) }
    val burstAlpha    = remember { Animatable(0f) }

    //Animation nourriture + éclats boost
    LaunchedEffect(feedTick) {
        if (feedTick > 0) {
            food.snapTo(0f)
            launch { food.animateTo(1f, tween(300, easing = LinearEasing)) }
            mouth.animateTo(1f, tween(300))
            if (boosted) {
                burstProgress.snapTo(0f)
                burstAlpha.snapTo(1f)
                launch { burstProgress.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
                launch {
                    delay(250.milliseconds)
                    burstAlpha.animateTo(0f, tween(300))
                }
            }
            mouth.animateTo(0f, tween(500))
        }
    }

    // Animation grosse ration
    val bigFoodScale = remember { Animatable(0f) }
    val bigFoodAlpha = remember { Animatable(0f) }
    val bigFoodY    = remember { Animatable(0f) }  // 0 = haut de l'écran, 1 = bouche

    LaunchedEffect(bigFeedTick) {
        if (bigFeedTick > 0) {
            // Reset : steak en haut, plein opaque, taille 0
            bigFoodScale.snapTo(0f)
            bigFoodAlpha.snapTo(1f)
            bigFoodY.snapTo(0f)

            // Phase 1 : apparition du steak (grossit)
            launch {
                bigFoodScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
            }
            // Phase 2 : descente lente jusqu'à la bouche (bigFoodY atteint 1f = bouche)
            launch {
                bigFoodY.animateTo(1f, tween(600, easing = LinearEasing))
            }
            // Phase 3 : disparition en fondu quand le steak touche la bouche
            launch {
                delay(900.milliseconds)         // attend que le steak soit presque arrivé
                bigFoodAlpha.animateTo(0f, tween(300))
            }
            // Phase 4 : Gloubi ouvre grand la bouche à l'arrivée du steak
            delay(500.milliseconds)
            mouth.animateTo(1f, tween(300))
            mouth.animateTo(0f, tween(300))
        }
    }

    Canvas(modifier.size(240.dp)) {
        val w = size.width
        val h = size.height
        val variableByLevel = (0.05f + 0.05f*level)/2 //augmentation dimensions par niveau
        val cx = w / 2f
        val r = w * variableByLevel * breathe //taille
        val ground = h * 0.58f + w * variableByLevel
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
        val mouthCy = cy + r * 0.40f  // centre visuel commun

        when {
            // Bouche ouverte : ovale dont le centre est exactement mouthCy
            mouth.value > 0.05f -> {
                val ovalW = r * 0.40f
                val ovalH = r * 0.44f * mouth.value
                drawOval(
                    Color(0xFF7A1F3D),
                    Offset(mx - ovalW / 2f, mouthCy - ovalH / 2f + r * 0.05f),
                    Size(ovalW, ovalH)
                )
            }
            // Bouche triste : arc ouvert vers le haut (200°→340°)
            sad -> {
                val arcW = r * 0.36f; val arcH = r * 0.28f
                drawArc(dark, 200f, 140f, false,
                    Offset(mx - arcW / 2f, mouthCy - arcH / 2f), Size(arcW, arcH),
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            // Bouche heureuse : arc ouvert vers le bas (20°→160°)
            happy -> {
                val arcW = r * 0.48f; val arcH = r * 0.38f
                drawArc(dark, 20f, 140f, false,
                    Offset(mx - arcW / 2f, mouthCy - arcH / 2f), Size(arcW, arcH),
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            // Bouche neutre : petit arc
            else -> {
                val arcW = r * 0.30f; val arcH = r * 0.18f
                drawArc(dark, 25f, 130f, false,
                    Offset(mx - arcW / 2f, mouthCy - arcH / 2f), Size(arcW, arcH),
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }

        // croquette qui tombe vers la bouche
        if (food.value < 1f) {
            val byStart = h * 0.02f          // légèrement hors canvas en haut
            val byEnd   = cy + r * 0.40f    // mouthCy : centre visuel de la bouche
            val fy = byStart + (byEnd - byStart) * food.value
            drawCircle(Color(0xFFD08A4E), r * 0.10f, Offset(cx, fy))
            drawCircle(Color(0xFF9C5F2C), r * 0.04f, Offset(cx + r * 0.03f, fy - r * 0.02f))
        }

        // Grosse ration (pomme)
        // byStart : juste au-dessus du canvas ; byEnd : centre de la bouche (my)
        if (bigFeedTick > 0 && bigFoodScale.value > 0f) {
            val byStart = -r * 0.5f          // légèrement hors canvas en haut
            val byEnd   = cy + r * 0.40f    // mouthCy : centre visuel de la bouche
            val by = byStart + (byEnd - byStart) * bigFoodY.value
            val bs = r * 0.2f * bigFoodScale.value //dimension de la grosse ration
            val alpha = bigFoodAlpha.value

            // Corps gauche de la pomme (lobe gauche légèrement décalé)
            drawCircle(Color(0xFFE63A2E).copy(alpha = alpha), bs * 0.75f, Offset(cx - bs * 0.18f, by))
            // Corps droit de la pomme (lobe droit)
            drawCircle(Color(0xFFE63A2E).copy(alpha = alpha), bs * 0.75f, Offset(cx + bs * 0.18f, by))
            // Reflet brillant
            drawCircle(Color(0xFFFF7B6B).copy(alpha = alpha * 0.7f), bs * 0.28f, Offset(cx - bs * 0.22f, by - bs * 0.38f))
            // Tige
            drawLine(
                Color(0xFF5C3A1E).copy(alpha = alpha),
                Offset(cx + bs * 0.08f, by - bs * 0.75f),
                Offset(cx + bs * 0.08f, by - bs * 1.10f),
                strokeWidth = bs * 0.12f
            )
            // Feuille (triangle arrondi via arc)
            drawOval(
                Color(0xFF4CAF50).copy(alpha = alpha),
                Offset(cx + bs * 0.08f, by - bs * 1.10f),
                Size(bs * 0.45f, bs * 0.28f)
            )
        }

        // Éclats boost (8 rayons qui partent du centre de Gloubi)
        if (burstProgress.value > 0f && burstAlpha.value > 0f) {
            val numRays = 8
            val maxLen  = r * 1.6f
            val minLen  = r * 0.7f
            val rayLen  = minLen + (maxLen - minLen) * burstProgress.value
            val tipR    = r * 0.07f * (1f - burstProgress.value * 0.5f)
            val alpha   = burstAlpha.value
            for (i in 0 until numRays) {
                val angle = (i.toFloat() / numRays) * 2f * PI.toFloat()
                val startX = cx + cos(angle) * r * 0.5f
                val startY = cy + sin(angle) * r * 0.5f
                val endX   = cx + cos(angle) * (r * 0.5f + rayLen)
                val endY   = cy + sin(angle) * (r * 0.5f + rayLen)
                // rayon
                drawLine(
                    color = Color(0xFFFFD54F).copy(alpha = alpha),
                    start = Offset(startX, startY),
                    end   = Offset(endX, endY),
                    strokeWidth = r * 0.07f * (1f - burstProgress.value * 0.4f),
                    cap = StrokeCap.Round
                )
                // étoile au bout du rayon
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = alpha * 0.85f),
                    radius = tipR,
                    center = Offset(endX, endY)
                )
            }
            // Halo central
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = alpha * 0.25f),
                radius = r * 0.6f * burstProgress.value,
                center = Offset(cx, cy)
            )
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

// ─── Previews ───────────────────────────────────────────────────────────────

@Preview(name = "Gloubi — rassasié (>= 99 %)", showBackground = true)
@Composable
fun PreviewAnimalHappy() {
    AnimalView(satiety = 100f, feedTick = 0, bigFeedTick = 0, boosted = false, level = 1)
}

@Preview(name = "Gloubi — normal (50 %)", showBackground = true)
@Composable
fun PreviewAnimalNormal() {
    AnimalView(satiety = 50f, feedTick = 0, bigFeedTick = 0, boosted = false, level = 1)
}

@Preview(name = "Gloubi — affamé (< 20 %)", showBackground = true)
@Composable
fun PreviewAnimalSad() {
    AnimalView(satiety = 10f, feedTick = 0, bigFeedTick = 0, boosted = false, level = 1)
}

@Preview(name = "Gloubi — vide (0 %)", showBackground = true)
@Composable
fun PreviewAnimalEmpty() {
    AnimalView(satiety = 0f, feedTick = 0, bigFeedTick = 0, boosted = false, level = 1)
}

@Preview(name = "Gloubi — boost actif", showBackground = true)
@Composable
fun PreviewAnimalBoosted() {
    AnimalView(satiety = 60f, feedTick = 0, bigFeedTick = 0, boosted = true, level = 1)
}