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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
fun AnimalView(
    modifier: Modifier = Modifier,
    satiety: Float,
    feedTick: Int,
    bigFeedTick: Int,
    boosted: Boolean = false,
    level: Int,
    skin: String = "skin_default",
    anim: String = "anim_default",
    food: String = "food_kibble"
) {
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

    // ── Animations supplémentaires ──────────────────────────────────────────
    val danceSwing = if (!isPreview) {
        val i2 = rememberInfiniteTransition(label = "dance")
        i2.animateFloat(-1f, 1f, infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sw").value
    } else 0f

    val shakeX = if (!isPreview) {
        val i3 = rememberInfiniteTransition(label = "shake")
        i3.animateFloat(-1f, 1f, infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse), label = "sx").value
    } else 0f

    val rainbowHue = if (!isPreview) {
        val i4 = rememberInfiniteTransition(label = "rainbow")
        i4.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "hue").value
    } else 0f

    val spinAngle = if (!isPreview) {
        val i5 = rememberInfiniteTransition(label = "spin")
        i5.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "spi").value
    } else 0f

    val mouth = remember { Animatable(0f) }  // 0 = fermée, 1 = grande ouverte
    val foodAnim = remember { Animatable(1f) }   // 0 = en haut, 1 = mangée (invisible)

    // Éclats boost
    val burstProgress = remember { Animatable(0f) }
    val burstAlpha    = remember { Animatable(0f) }

    //Animation nourriture + éclats boost
    val lastFeedTick = remember { mutableIntStateOf(feedTick) }
    LaunchedEffect(feedTick) {
        if (feedTick > lastFeedTick.intValue) {
            lastFeedTick.intValue = feedTick
            foodAnim.snapTo(0f)
            launch { foodAnim.animateTo(1f, tween(500, easing = LinearEasing)) }
            delay(200.milliseconds)
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
            mouth.animateTo(0f, tween(300))
        }
    }

    // Animation grosse ration
    val bigFoodScale = remember { Animatable(0f) }
    val bigFoodAlpha = remember { Animatable(0f) }
    val bigFoodY    = remember { Animatable(0f) }  // 0 = haut de l'écran, 1 = bouche

    val lastBigFeedTick = remember { mutableIntStateOf(bigFeedTick) }
    LaunchedEffect(bigFeedTick) {
        if (bigFeedTick > lastBigFeedTick.intValue) {
            lastBigFeedTick.intValue = bigFeedTick
            // Reset : ration en haut, plein opaque, taille 0
            bigFoodScale.snapTo(0f)
            bigFoodAlpha.snapTo(1f)
            bigFoodY.snapTo(0f)

            // Phase 1 : apparition de la ration (grossit)
            launch {
                bigFoodScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
            }
            // Phase 2 : descente lente jusqu'à la bouche (bigFoodY atteint 1f = bouche)
            launch {
                bigFoodY.animateTo(1f, tween(600, easing = LinearEasing))
            }
            // Phase 3 : disparition en fondu quand la ration touche la bouche
            launch {
                delay(900.milliseconds)         // attend que la ration soit presque arrivé
                bigFoodAlpha.animateTo(0f, tween(300))
            }
            // Phase 4 : Gloubi ouvre grand la bouche à l'arrivée de la ration
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
        // Animation selon animId
        val bounce = when (anim) {
            "anim_dance" -> abs(sin(hop * PI)).toFloat() * h * 0.10f
            "anim_flip"  -> abs(sin(hop * PI)).toFloat() * h * 0.15f
            "anim_spin", "anim_shake" -> 0f
            else         -> if (happy) abs(sin(hop * PI)).toFloat() * h * 0.09f else 0f
        }
        val cy = h * 0.58f - bounce
        val cxAnim = when (anim) {
            "anim_dance" -> cx + danceSwing * r * 0.35f
            "anim_shake" -> cx + shakeX * r * 0.12f
            else         -> cx
        }
        val dark = Color(0xFF2B2B3A)

        fun hsvColor(hue: Float, sat: Float = 0.85f, `val`: Float = 0.95f): Color {
            val hsv = floatArrayOf(hue * 360f, sat, `val`)
            return Color(android.graphics.Color.HSVToColor(hsv))
        }

        // Couleur de base selon le skin
        val skinBase = when (skin) {
            "skin_gold"    -> Triple(Color(0xFFFFD54F), Color(0xFFFFF176), Color(0xFFFFC107))
            "skin_rainbow" -> Triple(
                hsvColor(rainbowHue),
                hsvColor((rainbowHue + 0.33f) % 1f),
                hsvColor((rainbowHue + 0.5f) % 1f, 0.5f, 0.75f)
            )
            "skin_ghost"   -> Triple(Color(0xFFECEFF1), Color(0xFFFFFFFF), Color(0xFFB0BEC5))
            "skin_fire"    -> Triple(Color(0xFFFF6D00), Color(0xFFFFAB40), Color(0xFFDD2C00))
            "skin_ice"     -> Triple(Color(0xFF80DEEA), Color(0xFFE0F7FA), Color(0xFF00BCD4))
            else           -> Triple(Color(0xFF8FD3F4), Color(0xFF7BDFA0), Color(0xFF9AA5B1))
        }
        val (bodyNormal, bodyHappy, bodySad) = skinBase
        val body = when { happy -> bodyHappy; sad -> bodySad; else -> bodyNormal }

        val earInner = when (skin) {
            "skin_gold"    -> Color(0xFFFFECB3)
            "skin_ghost"   -> Color(0xFFCFD8DC)
            "skin_fire"    -> Color(0xFFFFCC02)
            "skin_ice"     -> Color(0xFFB2EBF2)
            "skin_rainbow" -> hsvColor((rainbowHue + 0.16f) % 1f)
            else           -> Color(0xFFFFC1CC)
        }
        val stroke = r * 0.06f

        // ombre
        drawOval(Color(0x22000000), Offset(cxAnim - r * 0.8f, ground - r * 0.06f), Size(r * 1.6f, r * 0.14f))

        // Spin : rotation du corps entier
        if (anim == "anim_spin") {
            rotate(degrees = spinAngle, pivot = Offset(cxAnim, cy)) { /* pivot only */ }
        }

        // oreilles
        listOf(-1f, 1f).forEachIndexed { idx, side ->
            val earColor = if (skin == "skin_rainbow")
                hsvColor(((rainbowHue + idx * 0.5f) % 1f))
            else body
            drawCircle(earColor, r * 0.28f, Offset(cxAnim + side * r * 0.70f, cy - r * 0.85f))
            drawCircle(earInner, r * 0.14f, Offset(cxAnim + side * r * 0.70f, cy - r * 0.85f))
        }

        // pieds
        for (side in listOf(-1f, 1f)) {
            drawCircle(body, r * 0.22f, Offset(cxAnim + side * r * 0.45f, cy + r * 0.95f))
        }

        // corps + ventre
        drawCircle(body, r, Offset(cxAnim, cy))
        drawOval(Color(0x33FFFFFF), Offset(cxAnim - r * 0.55f, cy + r * 0.05f), Size(r * 1.1f, r * 0.85f))

        // Effet skin ghost : contour fantôme
        if (skin == "skin_ghost") {
            drawCircle(Color(0x44FFFFFF), r * 1.02f, Offset(cx, cy),
                style = Stroke(r * 0.06f)
            )
        }
        // Effet skin fire : halo de flammes
        if (skin == "skin_fire") {
            for (i in 0 until 8) {
                val a = (i.toFloat() / 8f + sparkle * 0.15f) * 2f * PI.toFloat()
                val flameR = r * (0.18f + 0.08f * abs(sin(a * 2f + sparkle * PI.toFloat())))
                drawCircle(Color(0x88FF6D00), flameR, Offset(cxAnim + cos(a) * r * 1.15f, cy + sin(a) * r * 1.05f))
                drawCircle(Color(0x55FFAB40), flameR * 0.5f, Offset(cxAnim + cos(a) * r * 1.2f, cy + sin(a) * r * 1.1f))
            }
        }
        // Effet skin ice : cristaux
        if (skin == "skin_ice") {
            for (i in 0 until 6) {
                val a = (i.toFloat() / 6f + sparkle * 0.08f) * 2f * PI.toFloat()
                val cr = r * 0.12f
                val ox = cxAnim + cos(a) * r * 1.3f
                val oy = cy + sin(a) * r * 1.1f
                val cp = androidx.compose.ui.graphics.Path().apply {
                    moveTo(ox, oy - cr); lineTo(ox + cr * 0.6f, oy)
                    lineTo(ox, oy + cr); lineTo(ox - cr * 0.6f, oy); close()
                }
                drawPath(cp, Color(0xAAB2EBF2))
                drawPath(cp, Color(0xFF80DEEA), style = Stroke(cr * 0.18f))
            }
        }

        // joues
        for (side in listOf(-1f, 1f)) {
            drawCircle(Color(0x66FF8FA3), r * 0.13f, Offset(cxAnim + side * r * 0.62f, cy + r * 0.22f))
        }

        // yeux
        val eyeR = r * 0.17f
        val eyeY = cy - r * 0.15f
        for (side in listOf(-1f, 1f)) {
            val ex = cxAnim + side * r * 0.38f
            drawOval(Color.White, Offset(ex - eyeR, eyeY - eyeR * blink), Size(eyeR * 2f, eyeR * 2f * blink))
            if (blink > 0.3f) {
                drawCircle(dark, eyeR * 0.55f, Offset(ex, eyeY + if (sad) eyeR * 0.25f else 0f))
                drawCircle(Color.White, eyeR * 0.15f, Offset(ex + eyeR * 0.2f, eyeY - eyeR * 0.2f))
            }
        }
        if (sad) {
            drawCircle(Color(0xFF6EC1FF), r * 0.06f, Offset(cxAnim + r * 0.50f, eyeY + eyeR * 1.7f))
        }

        // bouche
        val mx = cxAnim
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
                    Offset(mx - arcW / 2f, mouthCy - arcH / 2f + r * 0.05f), Size(arcW, arcH),
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

        // Nourriture qui tombe vers la bouche
        if (foodAnim.value < 1f) {
            val byStart = h * 0.02f          // légèrement hors canvas en haut
            val byEnd   = cy + r * 0.40f     // mouthCy : centre visuel de la bouche
            val fy = byStart + (byEnd - byStart) * foodAnim.value
            val fr = r * 0.15f
            when (food) {
                "food_pizza" -> {
                    // Triangle pizza
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, fy - fr)
                        lineTo(cx + fr, fy + fr * 0.8f)
                        lineTo(cx - fr, fy + fr * 0.8f)
                        close()
                    }
                    drawPath(path, Color(0xFFFDD835))
                    drawCircle(Color(0xFFEF5350), fr * 0.35f, Offset(cx, fy + fr * 0.1f))
                }
                "food_star" -> {
                    // Étoile dorée (5 cercles)
                    drawCircle(Color(0xFFFFD740), fr, Offset(cx, fy))
                    for (i in 0 until 5) {
                        val a = (i * 72f - 90f) * PI.toFloat() / 180f
                        drawCircle(Color(0xFFFFF176), fr * 0.4f,
                            Offset(cx + cos(a) * fr * 0.85f, fy + sin(a) * fr * 0.85f))
                    }
                }
                "food_diamond" -> {
                    // Losange bleu
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, fy - fr * 1.2f)
                        lineTo(cx + fr * 0.8f, fy)
                        lineTo(cx, fy + fr * 1.2f)
                        lineTo(cx - fr * 0.8f, fy)
                        close()
                    }
                    drawPath(path, Color(0xFF64B5F6))
                    drawPath(path.apply {  }, Color(0xAAFFFFFF),
                        style = Stroke(fr * 0.15f)
                    )
                }
                "food_cake" -> {
                    // Gâteau : rectangle + triangle
                    drawRoundRect(Color(0xFFF48FB1),
                        Offset(cx - fr * 0.9f, fy - fr * 0.5f), Size(fr * 1.8f, fr * 1.2f),
                        androidx.compose.ui.geometry.CornerRadius(fr * 0.2f))
                    drawCircle(Color(0xFFFCE4EC), fr * 0.3f, Offset(cx, fy - fr * 0.9f))
                }
                "food_apple" -> {
                    // Pomme rouge (déjà connue)
                    drawCircle(Color(0xFFE63A2E), fr * 0.85f, Offset(cx - fr * 0.18f, fy))
                    drawCircle(Color(0xFFE63A2E), fr * 0.85f, Offset(cx + fr * 0.18f, fy))
                    drawCircle(Color(0xAAFF7B6B), fr * 0.3f, Offset(cx - fr * 0.22f, fy - fr * 0.38f))
                    drawLine(Color(0xFF5C3A1E),
                        Offset(cx + fr * 0.08f, fy - fr * 0.75f),
                        Offset(cx + fr * 0.08f, fy - fr * 1.1f), fr * 0.12f)
                }
                else -> {
                    // Croquette par défaut
                    drawCircle(Color(0xFFD08A4E), fr, Offset(cx, fy))
                    drawCircle(Color(0xFF9C5F2C), fr * 0.4f, Offset(cx + fr * 0.3f, fy - fr * 0.2f))
                }
            }
        }

        // Grosse ration (pomme)
        // byStart : juste au-dessus du canvas ; byEnd : centre de la bouche (my)
        if (bigFeedTick > 0 && bigFoodScale.value > 0f) {
            val byStart = -r * 0.5f          // légèrement hors canvas en haut
            val byEnd   = cy + r * 0.40f    // mouthCy : centre visuel de la bouche
            val by = byStart + (byEnd - byStart) * bigFoodY.value
            val bs = r * 0.3f * bigFoodScale.value //dimension de la grosse ration
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

        // Notes de musique pour anim_dance
        if (anim == "anim_dance") {
            val noteAlpha = abs(sin(sparkle * PI.toFloat())).coerceIn(0.3f, 1f)
            listOf(
                Offset(cxAnim - r * 1.5f, cy - r * 0.6f),
                Offset(cxAnim + r * 1.4f, cy - r * 0.8f),
                Offset(cxAnim - r * 1.3f, cy + r * 0.3f),
                Offset(cxAnim + r * 1.6f, cy + r * 0.1f),
            ).forEachIndexed { i, pos ->
                val ny = pos.y - r * 0.2f * abs(sin((sparkle + i * 0.25f) * PI.toFloat()))
                drawOval(Color(0xFFFFD54F).copy(alpha = noteAlpha),
                    Offset(pos.x - r * 0.08f, ny - r * 0.06f), Size(r * 0.16f, r * 0.12f))
                drawLine(Color(0xFFFFD54F).copy(alpha = noteAlpha),
                    Offset(pos.x + r * 0.08f, ny), Offset(pos.x + r * 0.08f, ny - r * 0.22f), r * 0.03f)
            }
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