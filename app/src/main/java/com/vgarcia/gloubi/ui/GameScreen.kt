package com.vgarcia.gloubi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vgarcia.gloubi.data.GameConfig
import com.vgarcia.gloubi.data.Player
import java.util.Locale

private fun fmt(v: Double): String = when {
    v >= 1_000_000 -> String.format(Locale.FRANCE, "%.2f M", v / 1_000_000)
    v >= 1_000 -> String.format(Locale.FRANCE, "%.1f k", v / 1_000)
    else -> String.format(Locale.FRANCE, "%.0f", v)
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    icon: @Composable () -> Unit,
    label: String,
    sub: String? = null
) {
    val colors = if (active)
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
    else
        ButtonDefaults.buttonColors()

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        modifier = modifier.height(80.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall)
        }
    }
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
    onFeedBig: () -> Unit,
    onToggleAutoFeed: () -> Unit,
    onDismissOffline: () -> Unit
) {
    val boosted = nowMs < player.boostUntil
    val boostLeftSec = if (boosted) ((player.boostUntil - nowMs) / 1_000L) + 1 else 0L
    val boostLabel = "%d:%02d".format(boostLeftSec / 60, boostLeftSec % 60)
    val autoActive = nowMs < player.autoFeedUntil
    val autoLeftSec = if (autoActive) ((player.autoFeedUntil - nowMs) / 1_000L) + 1 else 0L
    val autoLabel = "%d:%02d".format(autoLeftSec / 60, autoLeftSec % 60)
    val autoCapSec = GameConfig.AUTO_FEED_MAX_MS / 1000L
    val autoCapLabel = "%dh%02d".format(autoCapSec / 3600, (autoCapSec % 3600) / 60)
    val earning = player.satiety >= GameConfig.GAIN_THRESHOLD
    val adsLeft = GameConfig.MAX_ADS_PER_DAY - player.adsToday
    val level = player.progress.toInt() / 100 + 1
    val xpInLevel = player.progress % 100

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Gloubi", style = MaterialTheme.typography.headlineMedium)

            if (offlineGain > 0) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Pendant votre absence : +${fmt(offlineGain/100)} niveau")
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = onDismissOffline) { Text("OK") }
                    }
                }
            }

            AnimalView(satiety = player.satiety.toFloat(), feedTick = feedTick)

            // Barre de faim
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Faim : ${String.format(Locale.FRANCE, "%.2f", player.satiety)} %",
                        style = MaterialTheme.typography.titleLarge
                    )
                    LinearProgressIndicator(
                        progress = { (player.satiety / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (earning)
                            "Gloubi est rassasié !" + if (boosted) "\nBoost actif ($boostLabel restants)" else ""
                        else
                            "Atteignez ${GameConfig.GAIN_THRESHOLD.toInt()} % pour accumuler des XP !" +
                                    if (boosted) "\nBoost actif : $boostLabel" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (autoActive) {
                        Text(
                            "Auto-nourrissage actif — encore $autoLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Grille 2×2
            val half = Modifier.weight(1f).fillMaxWidth()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bouton 1 : nourrir (bonus si boost actif)
                ActionButton(
                    onClick = onFeed,
                    icon = { Icon(Icons.Rounded.Restaurant, null, modifier = Modifier.size(20.dp)) },
                    label = "Nourrir",
                    sub = if (boosted) "+${GameConfig.FEED_BOOST} %" else "+${GameConfig.FEED_SMALL} %",
                    active = boosted,
                    modifier = half
                )
                // Bouton 2 : boost pub (x10 nourriture)
                ActionButton(
                    onClick = onWatchBoostAd,
                    enabled = adReady && adsLeft > 0,
                    icon = { Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(20.dp)) },
                    label = when {
                        adsLeft <= 0 -> "Limite journalière atteinte !"
                        !adReady     -> "Chargement…"
                        boosted      -> "Boost nourriture actif"
                        else         -> "Boost nourriture x10"
                    },
                    sub = if (adsLeft > 0 && adReady && !boosted) "5 min · $adsLeft restants" else null,
                    modifier = half
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bouton 3 : grosse ration via pub (+10 %)
                ActionButton(
                    onClick = onFeedBig,
                    enabled = adReady,
                    icon = { Icon(Icons.Rounded.Star, null, modifier = Modifier.size(20.dp)) },
                    label = if (adReady) "Grosse ration" else "Chargement…",
                    sub = if (adReady) "+10 % · vidéo" else null,
                    modifier = half
                )
                // Bouton 4 : auto-nourrissage via pub (+15 min, plafond 2h30)
                ActionButton(
                    onClick = onToggleAutoFeed,
                    enabled = adReady,
                    active = autoActive,
                    icon = { Icon(Icons.Rounded.Autorenew, null, modifier = Modifier.size(20.dp)) },
                    label = if (autoActive) "Auto $autoLabel" else "Auto OFF",
                    sub = if (autoActive) "+15 min · plafond $autoCapLabel" else "+15 min · vidéo",
                    modifier = half
                )
            }

            // Niveau / XP
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Gloubi Niveau $level ·\n Prochain niveau dans ${fmt(100.0 - xpInLevel)} XP",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(
                        progress = { (xpInLevel / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        BannerAdView()
    }
}