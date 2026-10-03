package com.example.idleads.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
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
import com.example.idleads.data.GameConfig
import com.example.idleads.data.Player
import java.util.Locale

private fun fmt(v: Double): String = when {
    v >= 1_000_000 -> String.format(Locale.FRANCE, "%.2f M", v / 1_000_000)
    v >= 1_000 -> String.format(Locale.FRANCE, "%.1f k", v / 1_000)
    else -> String.format(Locale.FRANCE, "%.0f", v)
}

@Composable
fun GameScreen(
    player: Player,
    nowMs: Long,
    adReady: Boolean,
    offlineGain: Double,
    feedTick: Int,
    onFeed: () -> Unit,
    onWatchAd: () -> Unit,
    onFeedBig: () -> Unit,
    onDismissOffline: () -> Unit
) {
    val boosted = nowMs < player.boostUntil
    val boostLeftMin = if (boosted) ((player.boostUntil - nowMs) / 1_000L) + 1 else 0
    val earning = player.satiety >= GameConfig.GAIN_THRESHOLD
    val adsLeft = GameConfig.MAX_ADS_PER_DAY - player.adsToday

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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
                        OutlinedButton(onClick = onDismissOffline) { Text("OK") }
                    }
                }
            }

            AnimalView(satiety = player.satiety.toFloat(), feedTick = feedTick)

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
                        if (earning) {
                            "Gloubi est rassasié !\n" +
                            if (boosted) "Boost actif (${boostLeftMin/60}:${boostLeftMin%60} restants)" else ""
                        } else {
                            "Atteignez ${GameConfig.GAIN_THRESHOLD.toInt()} % pour commencer à accumuler des XP !\n" +
                                    if (boosted) "Boost actif : ${boostLeftMin/60}:${boostLeftMin%60}" else ""
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Bouton 1 : nourrir
            Button(onClick = onFeed, modifier = Modifier.fillMaxWidth()) {
                if(boosted) {
                    Text("Nourrir (+" + GameConfig.FEED_BOOST + " %)")
                }
                else {
                    Text("Nourrir (+" + GameConfig.FEED_SMALL + " %)")
                }
            }

            // Bouton 2 : boost via pub rewarded
            Button(
                onClick = onWatchAd,
                enabled = adReady && adsLeft > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        contentDescription = "Pub vidéo",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            adsLeft <= 0 -> "Limite quotidienne atteinte"
                            !adReady -> "Vidéo en cours de chargement…"
                            else -> "Boost nourriture x10 (5 min) · $adsLeft restants aujourd'hui"
                        }
                    )
                }
            }

            // Bouton 3 : grosse ration (lance aussi une pub, +1 % une fois la vidéo terminée)
            Button(onClick = onFeedBig, enabled = adReady, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        contentDescription = "Pub vidéo",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (adReady) "Grosse ration (+10 %)" else "Vidéo en cours de chargement…")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Niveau du Gloubi : "+ player.progress.toInt()/100+", prochain niveau dans ${fmt(100-player.progress%100)} XP")
                }
            }
        }

        BannerAdView()
    }
}