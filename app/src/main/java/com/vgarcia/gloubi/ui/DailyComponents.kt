package com.vgarcia.gloubi.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vgarcia.gloubi.LoginReward
import com.vgarcia.gloubi.R
import com.vgarcia.gloubi.data.GameConfig
import com.vgarcia.gloubi.data.Missions
import com.vgarcia.gloubi.data.Player
import com.vgarcia.gloubi.ui.theme.GloubiGreen
import com.vgarcia.gloubi.ui.theme.GloubiOrange
import com.vgarcia.gloubi.ui.theme.GloubiSurface
import com.vgarcia.gloubi.ui.theme.GloubiYellow

/** Badge de série : couleur, pulsation et nom changent avec la longueur de la série. */
@Composable
fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    val tier = when {
        streak >= 14 -> 3
        streak >= 7  -> 2
        streak >= 3  -> 1
        else         -> 0
    }
    val colors = when (tier) {
        3 -> listOf(Color(0xFFD50000), Color(0xFFFFD600))
        2 -> listOf(Color(0xFFFF6D00), Color(0xFFFFD54F))
        1 -> listOf(Color(0xFFE65100), Color(0xFFFF8C42))
        else -> listOf(Color(0xFF546E7A), Color(0xFF90A4AE))
    }
    val pulse by rememberInfiniteTransition(label = "streakPulse").animateFloat(
        initialValue = 1f,
        targetValue = if (tier == 0) 1f else 1.07f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse"
    )
    Row(
        modifier
            .scale(pulse)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(colors))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔥", fontSize = (16 + tier * 4).sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "$streak ${if (streak > 1) stringResource(R.string.days) else stringResource(R.string.day)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.ongoing_series),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/** Carte de série : badge + ce que la prochaine connexion rapporte (la peur de perdre la série). */
@Composable
fun StreakCard(streak: Int, loggedInToday: Boolean, modifier: Modifier = Modifier) {
    val nextIndex = GameConfig.streakDayIndex(streak + 1)
    val nextText = if (nextIndex == GameConfig.STREAK_BIG_FEED_DAY) stringResource(R.string.big_feed) + stringResource(R.string.offered)
    else "+${GameConfig.streakSatietyBonus(nextIndex).toInt()} % " + stringResource(R.string.hunger)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GloubiSurface)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StreakBadge(streak)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.tomorrow) + nextText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (loggedInToday) stringResource(R.string.come_back)
                    else stringResource(R.string.open_app),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Bannière affichée à l'ouverture quand la récompense du jour vient d'être versée. */
@Composable
fun LoginRewardBanner(reward: LoginReward, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF243B2A))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🎁 " + stringResource(R.string.day_reward) + " · " + stringResource(R.string.streak_of) + reward.streak + if (reward.streak > 1) stringResource(R.string.days) else stringResource(R.string.day),
                style = MaterialTheme.typography.titleMedium,
                color = GloubiYellow
            )
            Text(
                if (reward.bigFeed) stringResource(R.string.week_streak)
                else "+${reward.satietyBonus.toInt()} %" + stringResource(R.string.hunger) + ", +${reward.coinBonus} 🪙",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GloubiGreen)
            ) { Text(stringResource(R.string.great)) }
        }
    }
}

/** Les trois missions du jour, avec progression et récompense XP. */
@Composable
fun MissionsCard(player: Player, todayDay: Long, modifier: Modifier = Modifier) {
    val sameDay = player.missionDay == todayDay
    val feeds   = if (sameDay) player.missionFeedCount else 0
    val aboveMs = if (sameDay) player.missionAboveMs else 0L
    val ads     = if (sameDay) player.missionAdsCount else 0
    val claimed = if (sameDay) player.missionClaimedMask else 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GloubiSurface)
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(R.string.daily_missions),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            MissionRow(
                title = stringResource(R.string.feed) + " ${Missions.FEED_TARGET} " + stringResource(R.string.times),
                progress = feeds.toFloat() / Missions.FEED_TARGET,
                label = "${feeds.coerceAtMost(Missions.FEED_TARGET)} / ${Missions.FEED_TARGET}",
                done = (claimed and 1) != 0,
                reward = Missions.REWARD_FEED
            )
            MissionRow(
                title = stringResource(R.string.stay) + " ≥ ${GameConfig.GAIN_THRESHOLD.toInt()} % " + stringResource(R.string.during) + " ${Missions.ABOVE_MS_TARGET / 60_000L} min",
                progress = aboveMs.toFloat() / Missions.ABOVE_MS_TARGET.toFloat(),
                label = "${(aboveMs / 60_000L).coerceAtMost(Missions.ABOVE_MS_TARGET / 60_000L)} / ${Missions.ABOVE_MS_TARGET / 60_000L} min",
                done = (claimed and 2) != 0,
                reward = Missions.REWARD_ABOVE
            )
            MissionRow(
                title = stringResource(R.string.watch) + " ${Missions.ADS_TARGET} " + stringResource(R.string.ads),
                progress = ads.toFloat() / Missions.ADS_TARGET,
                label = "${ads.coerceAtMost(Missions.ADS_TARGET)} / ${Missions.ADS_TARGET}",
                done = (claimed and 4) != 0,
                reward = Missions.REWARD_ADS
            )
        }
    }
}

@Composable
private fun MissionRow(
    title: String,
    progress: Float,
    label: String,
    done: Boolean,
    reward: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (done) "✅ +$reward XP, +$reward 🪙" else "+$reward XP, +$reward 🪙",
                style = MaterialTheme.typography.labelSmall,
                color = if (done) GloubiGreen else GloubiYellow,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (done) GloubiGreen else GloubiOrange
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}