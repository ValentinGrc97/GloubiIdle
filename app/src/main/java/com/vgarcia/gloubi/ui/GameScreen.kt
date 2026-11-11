package com.vgarcia.gloubi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.RiceBowl
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vgarcia.gloubi.LoginReward
import com.vgarcia.gloubi.R
import com.vgarcia.gloubi.data.GameConfig
import com.vgarcia.gloubi.data.Player
import com.vgarcia.gloubi.ui.theme.GloubiBackground
import com.vgarcia.gloubi.ui.theme.GloubiGreen
import com.vgarcia.gloubi.ui.theme.GloubiOrange
import com.vgarcia.gloubi.ui.theme.GloubiRed
import com.vgarcia.gloubi.ui.theme.GloubiSurface
import com.vgarcia.gloubi.ui.theme.GloubiSurface2
import com.vgarcia.gloubi.ui.theme.GloubiTeal
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

enum class ButtonStatus { FREE, AD, ACTIVE, UNAVAILABLE }

@Composable
private fun StatusPill(text: String, status: ButtonStatus) {
    val bg = when (status) {
        ButtonStatus.FREE        -> GloubiGreen
        ButtonStatus.AD          -> Color(0xFFFFC107)
        ButtonStatus.ACTIVE      -> GloubiOrange
        ButtonStatus.UNAVAILABLE -> Color(0xFF546E7A)
    }
    val fg = if (status == ButtonStatus.AD) Color(0xFF1A1A00) else Color.White
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
}

/**
 * Bouton d'action lisible en un coup d'œil :
 * - couleur de l'icône = type d'action (nourrir, boost, auto...)
 * - pastille = coût (Gratuit / Pub / Actif / Épuisé)
 * - bordure orange = action en cours
 */
@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    urgent: Boolean = false,
    icon: ImageVector,
    accent: Color,
    title: String,
    detail: String? = null,
    statusText: String,
    status: ButtonStatus
) {
    val haptic = LocalHapticFeedback.current
    val pulse = if (urgent && enabled) {
        rememberInfiniteTransition(label = "pulse_$title").animateFloat(
            initialValue = 0.94f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseVal"
        ).value
    } else 1f

    val shape = RoundedCornerShape(18.dp)
    val borderColor = when {
        active  -> GloubiOrange
        enabled -> accent.copy(alpha = 0.55f)
        else    -> Color.Transparent
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        shape = shape,
        contentPadding = PaddingValues(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GloubiSurface2,
            disabledContainerColor = GloubiSurface2.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .height(92.dp)
            .scale(pulse)
            .border(1.5.dp, borderColor, shape)
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (enabled) 0.22f else 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(22.dp), tint = if (enabled) accent else Color.White.copy(alpha = 0.4f))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                StatusPill(statusText, status)
            }
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
    onDismissOffline: () -> Unit,
    onSettings: () -> Unit,
    onShop: () -> Unit,
    equippedSkin: String,
    equippedAnim: String,
    equippedFood: String,
    freeBoostAvailable: Boolean = false,
    freeBigFeedAvailable: Boolean = false,
    freeAutoFeedAvailable: Boolean = false,
    loginReward: LoginReward? = null,
    onDismissLoginReward: () -> Unit = {}
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
    val level      = GameConfig.levelFromProgress(player.progress)+1
    val (xpInLevel, xpForLevel) = GameConfig.xpInCurrentLevel(player.progress)
    var showMissionsDialog by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(
        targetValue = if (showMissionsDialog) 16.dp else 0.dp,
        label = "BlurAnimation"
    )

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

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Fond dégradé
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
        )
        // Particules flottantes
        FloatingParticles(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().blur(radius = blurRadius)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    androidx.compose.material3.IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Box {
                    // Animal
                    AnimalView(
                        modifier = Modifier.fillMaxWidth(),
                        satiety = player.satiety.toFloat(),
                        feedTick = feedTick,
                        bigFeedTick = bigFeedTick,
                        boosted = boosted,
                        level = level,
                        skin = equippedSkin,
                        anim = equippedAnim,
                        food = equippedFood
                    )
                    Row(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp)) {
                        //Missions badge
                        Button(modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFB8860B), GloubiTeal))),
                            onClick = { showMissionsDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(painter = painterResource(R.drawable.ic_missions), contentDescription = stringResource(id = R.string.missions), Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        //Coins badge
                        Button(modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFB8860B),
                                        GloubiYellow
                                    )
                                )
                            ),
                            onClick = onShop, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    player.coins.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Image(painter = painterResource(R.drawable.coins), contentDescription = stringResource(id = R.string.coins), Modifier.size(20.dp))
                            }
                        }
                    }
                }
                loginReward?.let { LoginRewardBanner(it, onDismissLoginReward) }

                // Barre de faim
                AnimatedVisibility(visible = true, enter = fadeIn(tween(400))) {
                    GloubiCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.satiety),
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
                                    earning && boosted -> stringResource(R.string.earning_boosted) + " $boostLabel"
                                    earning            -> stringResource(R.string.earning)
                                    boosted            -> stringResource(R.string.boost_remaining) + " $boostLabel · " + stringResource(R.string.reach) + " ${GameConfig.GAIN_THRESHOLD.toInt()}%"
                                    critical           -> stringResource(R.string.hungry)
                                    else               -> stringResource(R.string.reach) + " ${GameConfig.GAIN_THRESHOLD.toInt()}% " + stringResource(R.string.get_gloubi_grow)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (autoActive) {
                                Text(
                                    stringResource(R.string.auto_feed_active) + " $autoLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GloubiOrange
                                )
                            }
                        }
                    }
                }

                // Grille 2×2 : couleur = type d'action, pastille = coût
                val half = Modifier.weight(1f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(
                        modifier = half,
                        onClick = onFeed,
                        urgent = critical,
                        active = boosted,
                        icon = Icons.Rounded.Egg,
                        accent = GloubiGreen,
                        title = stringResource(R.string.feed),
                        detail = if (boosted) "+${GameConfig.FEED_BOOST} %" else "+${GameConfig.FEED_SMALL} %",
                        statusText = if (boosted) stringResource(R.string.boosted_active) else stringResource(R.string.free),
                        status = if (boosted) ButtonStatus.ACTIVE else ButtonStatus.FREE
                    )
                    ActionButton(
                        modifier = half,
                        onClick = onWatchBoostAd,
                        enabled = freeBoostAvailable || (adReady && adsLeft > 0),
                        active = boosted,
                        icon = Icons.Rounded.Bolt,
                        accent = Color(0xFF4FC3F7),
                        title = stringResource(R.string.boost_feed),
                        detail = "${GameConfig.BOOST_MS / 60_000L} min",
                        statusText = when {
                            boosted            -> "⏱ $boostLabel"
                            freeBoostAvailable -> stringResource(R.string.free)
                            adsLeft <= 0       -> stringResource(R.string.btn_status_empty)
                            adReady            -> "${stringResource(R.string.btn_status_ad)} · $adsLeft ${stringResource(R.string.remaining)}"
                            else               -> stringResource(R.string.loading)
                        },
                        status = when {
                            boosted            -> ButtonStatus.ACTIVE
                            freeBoostAvailable -> ButtonStatus.FREE
                            adReady && adsLeft > 0 -> ButtonStatus.AD
                            else               -> ButtonStatus.UNAVAILABLE
                        }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(
                        modifier = half,
                        onClick = onFeedBig,
                        enabled = freeBigFeedAvailable || adReady,
                        icon = Icons.Rounded.RiceBowl,
                        accent = Color(0xFF9CCC65),
                        title = stringResource(R.string.big_feed),
                        detail = "+${GameConfig.FEED_BIG.toInt()} %",
                        statusText = when {
                            freeBigFeedAvailable -> stringResource(R.string.free)
                            adReady              -> stringResource(R.string.btn_status_ad)
                            else                 -> stringResource(R.string.loading)
                        },
                        status = when {
                            freeBigFeedAvailable -> ButtonStatus.FREE
                            adReady              -> ButtonStatus.AD
                            else                 -> ButtonStatus.UNAVAILABLE
                        }
                    )
                    ActionButton(
                        modifier = half,
                        onClick = onToggleAutoFeed,
                        enabled = freeAutoFeedAvailable || adReady,
                        active = autoActive,
                        icon = Icons.Rounded.Autorenew,
                        accent = Color(0xFFBA68C8),
                        title = stringResource(R.string.auto_feed),
                        detail = if (autoActive)
                            "+${GameConfig.AUTO_FEED_REWARD_MS / 60_000L} min · max $autoCapLabel"
                        else "+${GameConfig.AUTO_FEED_REWARD_MS / 60_000L} min",
                        statusText = when {
                            autoActive            -> "⏱ $autoLabel"
                            freeAutoFeedAvailable -> stringResource(R.string.free)
                            adReady               -> stringResource(R.string.btn_status_ad)
                            else                  -> stringResource(R.string.loading)
                        },
                        status = when {
                            autoActive            -> ButtonStatus.ACTIVE
                            freeAutoFeedAvailable -> ButtonStatus.FREE
                            adReady               -> ButtonStatus.AD
                            else                  -> ButtonStatus.UNAVAILABLE
                        }
                    )
                }

                // Carte niveau
                AnimatedVisibility(visible = true, enter = fadeIn(tween(600))) {
                    GloubiCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.progress),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LevelBadge(level)
                            }
                            GloubiProgressBar(progress = (xpInLevel / xpForLevel).toFloat())
                            Text(
                                "${fmt(xpInLevel)} / ${fmt(xpForLevel)} XP · " + stringResource(R.string.next_level_in) + " ${fmt(xpForLevel - xpInLevel)} XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            BannerAdView()
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 24.dp)) {
            Box {
                // Bannière hors-ligne
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    AnimatedVisibility(
                        visible = offlineGain > 0,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        GloubiCard(Modifier
                            .fillMaxWidth()
                            .graphicsLayer(alpha = 0.8f)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.during_offline) + " +${offlineGain.toInt()} XP",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(onClick = onDismissOffline) { Text("OK") }
                            }
                        }
                    }
                }
            }
        }

        // Toucher en dehors de la vue des missions la ferme (même effet que le bouton OK)
        if (showMissionsDialog) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showMissionsDialog = false }
            )
        }

        Column {
            Box {
                // Liste missions
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    AnimatedVisibility(
                        visible = showMissionsDialog,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        GloubiCard(
                            Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { }
                        ) {
                            Column(
                                Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StreakCard(
                                    streak = player.loginStreak,
                                    loggedInToday = player.lastLoginDay == nowMs / 86_400_000L
                                )
                                MissionsCard(player = player, todayDay = nowMs / 86_400_000L)
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(modifier = Modifier.align(Alignment.CenterHorizontally), onClick = {
                                    showMissionsDialog = false
                                }) { Text("OK") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

private fun previewPlayer(
    satiety: Double = 50.0, progress: Double = 0.0, coins: Int = 0,
    boostUntil: Long = 0L, autoFeedUntil: Long = 0L, adsToday: Int = 0
) = Player(satiety = satiety, progress = progress, coins = coins, lastSeen = System.currentTimeMillis(),
    boostUntil = boostUntil, autoFeedUntil = autoFeedUntil, adsToday = adsToday)

@Preview(name = "Normal", showBackground = true, showSystemUi = true)
@Composable private fun PreviewNormal() { GloubiTheme { GameScreen(
    previewPlayer(75.0, 250.0, 500),
    System.currentTimeMillis(),
    true,
    0.0,
    0,
    {},
    {},
    0,
    {},
    {},
    {},
    {},
    {},
    "skin_default",
    "anim_default",
    "food_kibble",
    freeBoostAvailable = true,
    freeBigFeedAvailable = true,
    freeAutoFeedAvailable = true
) } }

//@Preview(name = "Rassasié + boost", showBackground = true, showSystemUi = true)
//@Composable private fun PreviewBoosted() { val n = System.currentTimeMillis(); GloubiTheme { GameScreen(previewPlayer(99.5, 750.0, n + 180_000), n, true, 0.0, 0, {}, {}, 0, {}, {}, {}, {}, {}, "skin_default", "anim_default", "food_kibble") } }

//@Preview(name = "Critique", showBackground = true, showSystemUi = true)
//@Composable private fun PreviewCritical() { GloubiTheme { GameScreen(previewPlayer(10.0, 500.0), System.currentTimeMillis(), true, 0.0, 0, {}, {}, 0, {}, {}, {}, {}, {}, "skin_default", "anim_default", "food_kibble") } }

//@Preview(name = "Hors-ligne", showBackground = true, showSystemUi = true)
//@Composable private fun PreviewOffline() { GloubiTheme { GameScreen(previewPlayer(60.0, 3100.0), System.currentTimeMillis(), true, 42.0, 0, {}, {}, 0, {}, {}, {}, {}, {}, "skin_default", "anim_default", "food_kibble") } }