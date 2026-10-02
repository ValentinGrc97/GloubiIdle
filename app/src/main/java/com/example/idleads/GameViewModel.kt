package com.example.idleads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.idleads.data.AppDb
import com.example.idleads.data.GameConfig
import com.example.idleads.data.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDb.get(app).dao()

    private val _player = MutableStateFlow(Player())
    val player: StateFlow<Player> = _player.asStateFlow()

    private val _offlineGain = MutableStateFlow(0.0)
    val offlineGain: StateFlow<Double> = _offlineGain.asStateFlow()

    /** Incrémenté à chaque repas : déclenche l'animation de nourriture/bouche. */
    private val _feedTick = MutableStateFlow(0)
    val feedTick: StateFlow<Int> = _feedTick.asStateFlow()

    private fun today(now: Long) = now / 86_400_000L

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val saved = dao.get() ?: Player(lastSeen = now)
            var p = if (saved.adsDay != today(now)) saved.copy(adsToday = 0, adsDay = today(now)) else saved

            // Hors-ligne : la faim baisse, et les gains ne courent que tant que l'animal reste >= 99 %
            val elapsedMs = (now - saved.lastSeen).coerceIn(0L, GameConfig.MAX_OFFLINE_MS)
            val aboveMs =
                if (saved.satiety >= GameConfig.GAIN_THRESHOLD)
                    min(
                        elapsedMs.toDouble(),
                        (saved.satiety - GameConfig.GAIN_THRESHOLD) / GameConfig.DECAY_PER_SEC * 1000.0
                    ).toLong()
                else 0L
            val boostedMs = (min(saved.lastSeen + aboveMs, saved.boostUntil) - saved.lastSeen)
                .coerceIn(0L, aboveMs)
            val gain = GameConfig.BASE_RATE * (aboveMs + boostedMs) / 1000.0
            val satiety = max(0.0, saved.satiety - GameConfig.DECAY_PER_SEC * elapsedMs / 1000.0)

            p = p.copy(satiety = satiety, progress = p.progress + gain, lastSeen = now)
            _offlineGain.value = gain
            _player.value = p
            dao.save(p)

            var ticks = 0
            while (true) {
                delay(1000.milliseconds)
                tick()
                if (++ticks % 5 == 0) save()
            }
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val p = _player.value
        val earning = p.satiety >= GameConfig.GAIN_THRESHOLD
        val gain = if (earning) GameConfig.rate(boosted = now < p.boostUntil) else 0.0
        _player.value = p.copy(
            satiety = max(0.0, p.satiety - GameConfig.DECAY_PER_SEC),
            progress = p.progress + gain,
            lastSeen = now,
            adsToday = if (p.adsDay != today(now)) 0 else p.adsToday,
            adsDay = today(now)
        )
    }

    fun save() {
        val p = _player.value
        viewModelScope.launch { dao.save(p) }
    }

    private fun feed(amount: Double) {
        val p = _player.value
        _player.value = p.copy(satiety = min(100.0, p.satiety + amount))
        _feedTick.value = _feedTick.value + 1
    }

    /** Bouton 1 : +0,01 %. */
    fun feedSmall() = feed(GameConfig.FEED_SMALL)

    /** Bouton 3 : +1 %. */
    fun feedBig() = feed(GameConfig.FEED_BIG)

    fun canWatchAd(): Boolean = _player.value.adsToday < GameConfig.MAX_ADS_PER_DAY

    /** Bouton 2 : à appeler uniquement quand AdMob confirme la récompense. */
    fun onAdRewarded() {
        val now = System.currentTimeMillis()
        val p = _player.value
        val bonus = GameConfig.BASE_RATE * GameConfig.INSTANT_BONUS_SECONDS
        _player.value = p.copy(
            progress = p.progress + bonus,
            boostUntil = max(now, p.boostUntil) + GameConfig.BOOST_MS,
            adsToday = p.adsToday + 1,
            adsDay = today(now)
        )
        save()
    }

    fun dismissOfflineGain() { _offlineGain.value = 0.0 }
}
