package com.vgarcia.gloubi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vgarcia.gloubi.data.GameConfig
import com.vgarcia.gloubi.data.Player
import com.vgarcia.gloubi.ui.theme.GloubiBackground
import com.vgarcia.gloubi.ui.theme.GloubiGreen
import com.vgarcia.gloubi.ui.theme.GloubiGreenDark
import com.vgarcia.gloubi.ui.theme.GloubiOrange
import com.vgarcia.gloubi.ui.theme.GloubiRed
import com.vgarcia.gloubi.ui.theme.GloubiSurface
import com.vgarcia.gloubi.ui.theme.GloubiSurface2
import com.vgarcia.gloubi.ui.theme.GloubiTheme
import com.vgarcia.gloubi.ui.theme.GloubiYellow
import java.util.Locale

private fun fmt(v: Double): String = when {
    v >= 1_000_000 -> String.format(Locale.FRANCE, "%.2f M", v / 1_000_000)
    v >= 1_000     -> String.format(Locale.FRANCE, "%.1f k", v / 1_000)
    else           -> String.format(Locale.FRANCE, "%.0f", v)
}

// Fond de particules flottantes (bulles lentes)
@Composable
private fun FloatingParticles(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "particles")
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "pTime"
    )
    androidx.compose.foundation.Canvas(modifier) {
        val particles = listOf(
            Triple(0.15f, 0.80f, 5f), Triple(0.72f, 0.60f, 4f),
            Triple(0.38f, 0.40f, 6f), Triple(0.85f, 0.20f, 3f),
            Triple(0.52f, 0.90f, 4f), Triple(0.25f, 0.10f, 5f),
            Triple(0.90f, 0.75f, 3f), Triple(0.62f, 0.30f, 4f),
        )
        particles.forEachIndexed { i, (rx, ry, r) ->
            val phase = (t + i * 0.125f) % 1f
            val y = (ry - phase * 0.3f + 1f) % 1f
            val alpha = (0.06f + 0.04f * kotlin.math.sin(phase * Math.PI.toFloat()))
            drawCircle(
                color = Color(0xFF7BDFA0).copy(alpha = alpha),
                radius = r.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(rx * size.width, y * size.height)
            )
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    urgent: Boolean = false,
    icon: @Composable () -> Unit,
    label: String,
    sub: String? = null
) {
    val haptic = LocalHapticFeedback.current

    val pulse by if (urgent && enabled) {
        rememberInfiniteTransition(label = "pulse_$label").animateFloat(
            initialValue = 0.92f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseVal"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
            .let { v -> object : androidx.compose.runtime.State<Float> { override val value get() = v.floatValue } }
    }

    val gradient = when {
        !enabled -> Brush.linearGradient(listOf(GloubiSurface2, GloubiSurface2))
        active   -> Brush.linearGradient(listOf(GloubiOrange, Color(0xFFFF6D00)))
        urgent   -> Brush.linearGradient(listOf(GloubiRed, GloubiOrange))
        else     -> Brush.linearGradient(listOf(GloubiGreen, GloubiGreenDark))
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(90.dp)
            .scale(pulse)
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun GloubiCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GloubiSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content = { content() }
    )
}

@Composable
fun GameScreen(
    player: Player,
    nowMs: Long,
    adReady: Boolean,
    offlineGain: Double,
    feedTick: Int,
    onFeed: () -> Unit,
    onWatchBoostAd: () -> Unit,
    bigFeedTick: Int,
    onFeedBig: () -> Unit,
    onToggleAutoFeed: () -> Unit,
    onDismissOffline: () -> Unit
) {
    val boosted    = nowMs < player.boostUntil
    val boostLeftSec = if (boosted) ((player.boostUntil - nowMs) / 1_000L) + 1 else 0L
    val boostLabel = "%d:%02d".format(boostLeftSec / 60, boostLeftSec % 60)
    val autoActive = nowMs < player.autoFeedUntil
    val autoLeftSec = if (autoActive) ((player.autoFeedUntil - nowMs) / 1_000L) + 1 else 0L
    val autoLabel  = "%d:%02d".format(autoLeftSec / 60, autoLeftSec % 60)
    val autoCapSec = GameConfig.AUTO_FEED_MAX_MS / 1000L
    val autoCapLabel = "%dh%02d".format(autoCapSec / 3600, (autoCapSec % 3600) / 60)
    val earning    = player.satiety >= GameConfig.GAIN_THRESHOLD
    val critical   = player.satiety < 20.0
    val adsLeft    = GameConfig.MAX_ADS_PER_DAY - player.adsToday
    val level      = GameConfig.levelFromProgress(player.progress)
    val (xpInLevel, xpForLevel) = GameConfig.xpInCurrentLevel(player.progress)

    // Fond dégradé vertical qui évolue selon l'état de Gloubi
    val bgTop = when {
        critical -> Color(0xFF1F0A0A)
        earning  -> Color(0xFF0A1F10)
        else     -> GloubiBackground
    }
    val bgBottom = when {
        critical -> Color(0xFF2A0E0E)
        earning  -> Color(0xFF0E2A18)
        else     -> Color(0xFF0E1A12)
    }

    Box(Modifier.fillMaxSize()) {
        // Fond dégradé
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
        )
        // Particules flottantes
        FloatingParticles(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Titre
                Text(
                    "Gloubi",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )

                // Bannière hors-ligne
                AnimatedVisibility(
                    visible = offlineGain > 0,
                    enter = fadeIn() + slideInVertically()
                ) {
                    GloubiCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "🌙 Pendant votre absence : +${fmt(offlineGain / 100)} niveau",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(onClick = onDismissOffline) { Text("OK") }
                        }
                    }
                }

                // Animal
                AnimalView(
                    satiety = player.satiety.toFloat(),
                    feedTick = feedTick,
                    bigFeedTick = bigFeedTick,
                    boosted = boosted,
                    level = level
                )

                // Barre de faim
                AnimatedVisibility(visible = true, enter = fadeIn(tween(400))) {
                    GloubiCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Faim",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${String.format(Locale.FRANCE, "%.2f", player.satiety)} %",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = when {
                                        critical -> GloubiRed
                                        earning  -> GloubiGreen
                                        else     -> GloubiYellow
                                    }
                                )
                            }
                            GloubiProgressBar(
                                progress = (player.satiety / 100.0).toFloat(),
                                critical = critical
                            )
                            Text(
                                when {
                                    earning && boosted -> "✨ Rassasié · Boost $boostLabel"
                                    earning            -> "✅ Gloubi est rassasié !"
                                    boosted            -> "⚡ Boost $boostLabel · Atteindre ${GameConfig.GAIN_THRESHOLD.toInt()} %"
                                    critical           -> "⚠️ Gloubi a très faim !"
                                    else               -> "Atteindre ${GameConfig.GAIN_THRESHOLD.toInt()} % pour gagner des XP"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (autoActive) {
                                Text(
                                    "🔄 Auto-nourrissage actif — encore $autoLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GloubiOrange
                                )
                            }
                        }
                    }
                }

                // Grille 2×2
                val half = Modifier.weight(1f).fillMaxWidth()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        onClick = onFeed,
                        urgent = critical,
                        active = boosted,
                        icon = { Icon(Icons.Rounded.Restaurant, null, Modifier.size(22.dp), tint = Color.White) },
                        label = "Nourrir",
                        sub = if (boosted) "+${GameConfig.FEED_BOOST} %" else "+${GameConfig.FEED_SMALL} %",
                        modifier = half
                    )
                    ActionButton(
                        onClick = onWatchBoostAd,
                        enabled = adReady && adsLeft > 0,
                        icon = { Icon(Icons.Rounded.PlayCircle, null, Modifier.size(22.dp), tint = Color.White) },
                        label = when {
                            adsLeft <= 0 -> "Limite atteinte"
                            !adReady     -> "Chargement…"
                            boosted      -> "Boost actif"
                            else         -> "Boost x10"
                        },
                        sub = if (adsLeft > 0 && adReady && !boosted) "5 min · $adsLeft restants" else null,
                        modifier = half
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        onClick = onFeedBig,
                        enabled = adReady,
                        icon = { Icon(Icons.Rounded.Star, null, Modifier.size(22.dp), tint = Color.White) },
                        label = if (adReady) "Grosse ration" else "Chargement…",
                        sub = if (adReady) "+10 % · vidéo" else null,
                        modifier = half
                    )
                    ActionButton(
                        onClick = onToggleAutoFeed,
                        enabled = adReady,
                        active = autoActive,
                        icon = { Icon(Icons.Rounded.Autorenew, null, Modifier.size(22.dp), tint = Color.White) },
                        label = if (autoActive) "Auto $autoLabel" else "Auto OFF",
                        sub = if (autoActive) "+15 min · plafond $autoCapLabel" else "+15 min · vidéo",
                        modifier = half
                    )
                }

                // Carte niveau
                AnimatedVisibility(visible = true, enter = fadeIn(tween(600))) {
                    GloubiCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Progression",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LevelBadge(level)
                            }
                            GloubiProgressBar(progress = (xpInLevel / xpForLevel).toFloat())
                            Text(
                                "${fmt(xpInLevel)} / ${fmt(xpForLevel)} XP · prochain niveau dans ${fmt(xpForLevel - xpInLevel)} XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            BannerAdView()
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

private fun previewPlayer(
    satiety: Double = 50.0, progress: Double = 0.0,
    boostUntil: Long = 0L, autoFeedUntil: Long = 0L, adsToday: Int = 0
) = Player(satiety = satiety, progress = progress, lastSeen = System.currentTimeMillis(),
    boostUntil = boostUntil, autoFeedUntil = autoFeedUntil, adsToday = adsToday)

@Preview(name = "Normal", showBackground = true, showSystemUi = true)
@Composable private fun PreviewNormal() { GloubiTheme { GameScreen(previewPlayer(50.0, 250.0), System.currentTimeMillis(), true, 0.0, 0, {}, {}, 0, {}, {}, {}) } }

@Preview(name = "Rassasié + boost", showBackground = true, showSystemUi = true)
@Composable private fun PreviewBoosted() { val n = System.currentTimeMillis(); GloubiTheme { GameScreen(previewPlayer(99.5, 750.0, n + 180_000), n, true, 0.0, 0, {}, {}, 0, {}, {}, {}) } }

@Preview(name = "Critique", showBackground = true, showSystemUi = true)
@Composable private fun PreviewCritical() { GloubiTheme { GameScreen(previewPlayer(10.0, 500.0), System.currentTimeMillis(), true, 0.0, 0, {}, {}, 0, {}, {}, {}) } }

@Preview(name = "Hors-ligne", showBackground = true, showSystemUi = true)
@Composable private fun PreviewOffline() { GloubiTheme { GameScreen(previewPlayer(60.0, 3100.0), System.currentTimeMillis(), true, 42.0, 0, {}, {}, 0, {}, {}, {}) } }