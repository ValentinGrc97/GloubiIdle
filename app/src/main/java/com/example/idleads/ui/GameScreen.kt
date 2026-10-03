package com.example.idleads.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
    val rate = GameConfig.rate(boosted)
    val payout = GameConfig.estimatedPayoutEur(player.progress)
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
                            if (boosted) "0,01% -> 0,1% /clic (boost x10, encore ~$boostLeftMin min)" else ""
                            //"Gloubi est rassasié : +${String.format(Locale.FRANCE, "%.0f", rate)} /s" +
                            //        if (boosted) " (boost x10, encore ~$boostLeftMin min)" else ""
                        } else {
                            "Atteignez ${GameConfig.GAIN_THRESHOLD.toInt()} % pour commencer à accumuler des XP et des gains !\n" +
                                    if (boosted) " Boost actif : ${boostLeftMin/60}:${boostLeftMin%60}." else ""
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
                Text(
                    when {
                        adsLeft <= 0 -> "Limite quotidienne atteinte"
                        !adReady -> "Vidéo en cours de chargement…"
                        else -> "Regarder une vidéo : boost x10 (5 min) · $adsLeft restantes aujourd'hui"
                    }
                )
            }

            // Bouton 3 : grosse ration (lance aussi une pub, +1 % une fois la vidéo terminée)
            Button(onClick = onFeedBig, enabled = adReady, modifier = Modifier.fillMaxWidth()) {
                Text(if (adReady) "Grosse ration (+10 %) · vidéo" else "Vidéo en cours de chargement…")
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Niveau du Gloubi : ${fmt(player.progress/100)}, prochain niveau dans ${fmt(100-player.progress%100)} XP")
                    Text("Votre part des gains", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (earning)
                            "Rythme actuel : +${String.format(Locale.FRANCE, "%.4f", GameConfig.estimatedPayoutEur(rate * 3600))} €/h"
                        else
                            "Aucun gain en cours (faim sous ${GameConfig.GAIN_THRESHOLD.toInt()} %)"
                    )
                    Text(
                        "Gain estimé : ${String.format(Locale.FRANCE, "%.4f", payout)} € " +
                                "(retrait dès ${GameConfig.PAYOUT_THRESHOLD_EUR.toInt()} €)"
                    )
                    LinearProgressIndicator(
                        progress = { (payout / GameConfig.PAYOUT_THRESHOLD_EUR).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Estimation non contractuelle : le montant définitif dépend des revenus publicitaires réels ",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        BannerAdView()
    }
}