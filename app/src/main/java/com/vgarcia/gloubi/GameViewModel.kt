package com.vgarcia.gloubi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vgarcia.gloubi.data.AppDb
import com.vgarcia.gloubi.data.GameConfig
import com.vgarcia.gloubi.data.Player
import kotlinx.coroutines.Dispatchers
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
    private val _bigFeedTick = MutableStateFlow(0)
    val bigFeedTick: StateFlow<Int> = _bigFeedTick.asStateFlow()
    private val _pendingBigFeed = MutableStateFlow(false)

    private fun today(now: Long) = now / 86_400_000L

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val saved = dao.get() ?: Player(lastSeen = now)
            var p = if (saved.adsDay != today(now)) saved.copy(adsToday = 0, adsDay = today(now)) else saved

            // Hors-ligne : la faim baisse, et les gains ne courent que tant que l'animal reste >= 95 %
            val elapsedMs = (now - saved.lastSeen).coerceIn(0L, GameConfig.MAX_OFFLINE_MS)

            // Faim hors-ligne : auto-nourrissage actif seulement tant que autoFeedUntil n'est pas dépassé
            val autoActiveMs = (min(now, saved.autoFeedUntil) - saved.lastSeen).coerceAtLeast(0L)
            val netDecay = GameConfig.DECAY_PER_SEC * elapsedMs / 1000.0 -
                    GameConfig.AUTO_FEED_PER_SEC * autoActiveMs / 1000.0
            val satiety = (saved.satiety - netDecay).coerceIn(0.0, 100.0)

            val aboveMs = if (saved.satiety >= GameConfig.GAIN_THRESHOLD)
                min(
                    elapsedMs.toDouble(),
                    (saved.satiety - GameConfig.GAIN_THRESHOLD) / GameConfig.DECAY_PER_SEC * 1000.0
                ).toLong()
            else 0L
            val boostedMs = (min(saved.lastSeen + aboveMs, saved.boostUntil) - saved.lastSeen)
                .coerceIn(0L, aboveMs)
            val gain = GameConfig.BASE_RATE * (aboveMs + boostedMs) / 1000.0

            p = p.copy(satiety = satiety, progress = p.progress + gain, lastSeen = now)
            _offlineGain.value = gain
            _player.value = p
            dao.save(p)

            var ticks = 0
            while (true) {
                delay(1000.milliseconds / GameConfig.RATIO)
                tick()
                if (++ticks % 5 == 0) save()
            }
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val p = _player.value
        val earning = p.satiety >= GameConfig.GAIN_THRESHOLD
        val gain = if (earning) GameConfig.BASE_RATE else 0.0
        val autoAdd = if (p.autoFeedUntil > now) GameConfig.AUTO_FEED_PER_SEC else 0.0
        _player.value = p.copy(
            satiety = (p.satiety - GameConfig.DECAY_PER_SEC / GameConfig.RATIO + autoAdd)
                .coerceIn(0.0, 100.0),
            progress = p.progress + gain,
            lastSeen = now,
            adsToday = if (p.adsDay != today(now)) 0 else p.adsToday,
            adsDay = today(now)
        )
    }

    fun save() {
        viewModelScope.launch(Dispatchers.IO) { dao.save(_player.value) }
    }

    fun saveOnPause() {
        val p = _player.value
        val now = System.currentTimeMillis()
        val updated = p.copy(lastSeen = now)
        _player.value = updated
        viewModelScope.launch(Dispatchers.IO) { dao.save(updated) }
    }

    private fun feed(amount: Double) {
        val p = _player.value
        _player.value = p.copy(satiety = min(100.0, p.satiety + amount))
        when {
            amount == 10.0 -> _bigFeedTick.value++
            else -> _feedTick.value++
        }

    }

    /** Bouton 1 : +0,02 %. */
    fun feedSmall() = feed(GameConfig.FEED_SMALL)
    /** Bouton 2 : Boost pour Bouton 1. */
    fun feedBoost() = feed(GameConfig.FEED_BOOST)

    /** Bouton 3 : +10 %. */
    fun feedBig() {
        feed(GameConfig.FEED_BIG)
    }

    fun canWatchAd(): Boolean = _player.value.adsToday < GameConfig.MAX_ADS_PER_DAY
    fun boosted(): Boolean = _player.value.boostUntil > System.currentTimeMillis()

    /** Appelé quand AdMob confirme la récompense pour le boost. */
    fun onBoostAdRewarded() {
        val now = System.currentTimeMillis()
        val p = _player.value
        _player.value = p.copy(
            boostUntil = max(now, p.boostUntil) + GameConfig.BOOST_MS,
            adsToday = p.adsToday + 1,
            adsDay = today(now)
        )
        save()
    }

    /** Appelé quand AdMob confirme la récompense pour la grosse ration. */
    fun onBigFeedAdRewarded() {
        _pendingBigFeed.value = true  // on marque, on ne feed pas encore
    }

    /** Appelé quand AdMob confirme la récompense pour l'auto-nourrissage. */
    fun onAutoFeedRewarded() {
        val now = System.currentTimeMillis()
        val p = _player.value
        val currentUntil = max(now, p.autoFeedUntil)
        val newUntil = min(currentUntil + GameConfig.AUTO_FEED_REWARD_MS, now + GameConfig.AUTO_FEED_MAX_MS)
        _player.value = p.copy(autoFeedUntil = newUntil)
        save()
    }

    fun dismissOfflineGain() { _offlineGain.value = 0.0 }

    fun resetProgress() {
        val now = System.currentTimeMillis()
        val fresh = Player(lastSeen = now)
        _player.value = fresh
        _offlineGain.value = 0.0
        viewModelScope.launch(Dispatchers.IO) { dao.save(fresh) }
    }

    fun consumePendingBigFeed() {
        if (_pendingBigFeed.value) {
            _pendingBigFeed.value = false
            feedBig()
        }
    }
}