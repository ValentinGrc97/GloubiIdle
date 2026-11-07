package com.vgarcia.gloubi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vgarcia.gloubi.data.AppDb
import com.vgarcia.gloubi.data.GameConfig
import com.vgarcia.gloubi.data.Missions
import com.vgarcia.gloubi.data.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

/** Récompense de connexion journalière, affichée une fois à l'ouverture de l'appli. */
data class LoginReward(
    val streak: Int,
    val dayIndex: Int,
    val satietyBonus: Double,
    val coinBonus: Int,
    val bigFeed: Boolean
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val playerDao = AppDb.get(app).playerDao()

    private val _player = MutableStateFlow(Player())
    val player: StateFlow<Player> = _player.asStateFlow()

    private val _offlineGain = MutableStateFlow(0.0)
    val offlineGain: StateFlow<Double> = _offlineGain.asStateFlow()

    private val _loginReward = MutableStateFlow<LoginReward?>(null)
    val loginReward: StateFlow<LoginReward?> = _loginReward.asStateFlow()

    private val _feedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val feedEvent: SharedFlow<Unit> = _feedEvent.asSharedFlow()
    private val _bigFeedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val bigFeedEvent: SharedFlow<Unit> = _bigFeedEvent.asSharedFlow()
    private val _pendingBigFeed = MutableStateFlow(false)

    private fun today(now: Long = System.currentTimeMillis()) = now / 86_400_000L

    // ── Missions : remise à zéro quotidienne ─────────────────────────────────

    private fun Player.withMissionDayRefreshed(now: Long): Player =
        if (missionDay == today(now)) this
        else copy(
            missionDay = today(now),
            missionFeedCount = 0,
            missionAboveMs = 0L,
            missionAdsCount = 0,
            missionClaimedMask = 0
        )

    /** Verse la récompense de chaque mission atteinte et pas encore versée (bits 0, 1, 2). */
    private fun Player.applyMissions(): Player {
        var mask = missionClaimedMask
        var reward = 0
        if (missionFeedCount >= Missions.FEED_TARGET && (mask and 1) == 0) {
            mask = mask or 1
            reward += Missions.REWARD_FEED
        }
        if (missionAboveMs >= Missions.ABOVE_MS_TARGET && (mask and 2) == 0) {
            mask = mask or 2
            reward += Missions.REWARD_ABOVE
        }
        if (missionAdsCount >= Missions.ADS_TARGET && (mask and 4) == 0) {
            mask = mask or 4
            reward += Missions.REWARD_ADS
        }
        if (mask == missionClaimedMask) return this
        return copy(
            missionClaimedMask = mask,
            progress = progress + reward,
            coins = coins + reward
        )
    }

    // ── Connexion journalière ────────────────────────────────────────────────

    /**
     * Met à jour la série à l'ouverture de l'appli. Une seule récompense par jour.
     * Une connexion la veille prolonge la série, sinon elle repart à 1.
     */
    private fun processDailyLogin(now: Long) {
        val p = _player.value
        val todayDay = today(now)
        if (p.lastLoginDay == todayDay) return

        val streak = if (p.lastLoginDay == todayDay - 1) p.loginStreak + 1 else 1
        val dayIndex = GameConfig.streakDayIndex(streak)
        val bigFeed = dayIndex == GameConfig.STREAK_BIG_FEED_DAY
        val satietyBonus = if (bigFeed) GameConfig.FEED_BIG else GameConfig.streakSatietyBonus(dayIndex)
        val coinBonus = GameConfig.streakCoinBonus(dayIndex)

        _player.value = p.copy(
            loginStreak = streak,
            lastLoginDay = todayDay,
            satiety = min(100.0, p.satiety + satietyBonus),
            coins = p.coins + coinBonus
        ).withMissionDayRefreshed(now)

        _loginReward.value = LoginReward(streak, dayIndex, satietyBonus, coinBonus, bigFeed)
        if (bigFeed) viewModelScope.launch { _bigFeedEvent.emit(Unit) }
        save()
    }

    fun dismissLoginReward() { _loginReward.value = null }

    // ── Accès gratuit quotidien ───────────────────────────────────────────────

    fun canUseFreeBoost()    = _player.value.freeBoostUsedDay    != today()
    fun canUseFreeBigFeed()  = _player.value.freeBigFeedUsedDay  != today()
    fun canUseFreeAutoFeed() = _player.value.freeAutoFeedUsedDay != today()

    /** Boost gratuit : même récompense qu'une pub, consomme le slot quotidien. */
    fun useFreeBoost() {
        val now = System.currentTimeMillis()
        val p = _player.value
        _player.value = p.copy(
            boostUntil = max(now, p.boostUntil) + GameConfig.BOOST_MS,
            freeBoostUsedDay = today(now)
        )
        save()
    }

    /** Grosse ration gratuite : marque le pending, sera consommé à onResume. */
    fun useFreeBigFeed() {
        val now = System.currentTimeMillis()
        _player.value = _player.value.copy(freeBigFeedUsedDay = today(now))
        _pendingBigFeed.value = true
        save()
        consumePendingBigFeed()
    }

    /** Auto-nourrissage gratuit : +15 min, consomme le slot quotidien. */
    fun useFreeAutoFeed() {
        val now = System.currentTimeMillis()
        val p = _player.value
        val newUntil = min(
            max(now, p.autoFeedUntil) + GameConfig.AUTO_FEED_REWARD_MS,
            now + GameConfig.AUTO_FEED_MAX_MS
        )
        _player.value = p.copy(autoFeedUntil = newUntil, freeAutoFeedUsedDay = today(now))
        save()
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val saved = playerDao.get() ?: Player(lastSeen = now)
            var p = if (saved.adsDay != today(now)) saved.copy(adsToday = 0, adsDay = today(now)) else saved

            // Hors-ligne : la faim baisse, et les gains ne courent que tant que l'animal reste >= 95 %
            val elapsedMs = (now - saved.lastSeen).coerceIn(0L, GameConfig.MAX_OFFLINE_MS)
            // Faim hors-ligne : auto-nourrissage actif seulement tant qu'autoFeedUntil n'est pas dépassé
            val autoActiveMs = (min(now, saved.autoFeedUntil) - saved.lastSeen).coerceAtLeast(0L)
            val netDecay = GameConfig.DECAY_PER_SEC * elapsedMs / 1000.0 - GameConfig.AUTO_FEED_PER_SEC * autoActiveMs / 1000.0
            val satiety = (saved.satiety - netDecay).coerceIn(0.0, 100.0)
            val aboveMs = if (saved.satiety >= GameConfig.GAIN_THRESHOLD)
                min(elapsedMs.toDouble(),
                    (saved.satiety - GameConfig.GAIN_THRESHOLD) / GameConfig.DECAY_PER_SEC * 1000.0).toLong()
            else 0L
            val boostedMs = (min(saved.lastSeen + aboveMs, saved.boostUntil) - saved.lastSeen).coerceIn(0L, aboveMs)
            val gain = GameConfig.BASE_RATE * (aboveMs + boostedMs) / 1000.0

            p = p.copy(
                satiety = satiety,
                progress = p.progress + gain,
                coins = p.coins + gain.toInt(),
                lastSeen = now
            ).withMissionDayRefreshed(now)

            _offlineGain.value = gain
            _player.value = p
            playerDao.save(p)

            processDailyLogin(now)

            var ticks = 0
            while (true) {
                delay(1000.milliseconds / GameConfig.RATIO)
                tick()
                if (++ticks % 5 == 0) save()
            }
        }
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private fun tick() {
        val now = System.currentTimeMillis()
        val p = _player.value.withMissionDayRefreshed(now)
        val earning = p.satiety >= GameConfig.GAIN_THRESHOLD
        val gain = if (earning) GameConfig.BASE_RATE else 0.0
        val autoAdd = if (p.autoFeedUntil > now) GameConfig.AUTO_FEED_PER_SEC else 0.0
        _player.value = p.copy(
            satiety = (p.satiety - GameConfig.DECAY_PER_SEC / GameConfig.RATIO + autoAdd).coerceIn(0.0, 100.0),
            progress = p.progress + gain,
            coins = p.coins + gain.toInt(),
            lastSeen = now,
            adsToday = if (p.adsDay != today(now)) 0 else p.adsToday,
            adsDay = today(now),
            missionAboveMs = if (earning) p.missionAboveMs + 1000L else p.missionAboveMs
        ).applyMissions()
    }

    // ── Coins ────────────────────────────────────────────────────────────────

    fun spendCoins(amount: Int) {
        val p = _player.value
        if (p.coins < amount) return
        _player.value = p.copy(coins = p.coins - amount)
        save()
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    fun save() {
        viewModelScope.launch(Dispatchers.IO) { playerDao.save(_player.value) }
    }

    fun saveOnPause() {
        val now = System.currentTimeMillis()
        val updated = _player.value.copy(lastSeen = now)
        _player.value = updated
        viewModelScope.launch(Dispatchers.IO) { playerDao.save(updated) }
    }

    // ── Alimentation ─────────────────────────────────────────────────────────

    private fun feed(amount: Double) {
        val now = System.currentTimeMillis()
        val p = _player.value.withMissionDayRefreshed(now)
        _player.value = p.copy(
            satiety = min(100.0, p.satiety + amount),
            missionFeedCount = p.missionFeedCount + 1
        ).applyMissions()
        viewModelScope.launch {
            when {
                amount == GameConfig.FEED_BIG -> _bigFeedEvent.emit(Unit)
                else -> _feedEvent.emit(Unit)
            }
        }
    }

    /** Bouton 1 : +0,01 %. */
    fun feedSmall() = feed(GameConfig.FEED_SMALL)
    /** Bouton 2 : Boost pour Bouton 1. */
    fun feedBoost() = feed(GameConfig.FEED_BOOST)
    fun feedBig()   = feed(GameConfig.FEED_BIG)

    // ── Récompenses pub ──────────────────────────────────────────────────────

    fun canWatchAd(): Boolean = _player.value.adsToday < GameConfig.MAX_ADS_PER_DAY
    fun boosted(): Boolean = _player.value.boostUntil > System.currentTimeMillis()

    /** Compte une pub regardée pour la mission « regarder trois pubs ». */
    private fun recordAdWatched(now: Long) {
        val p = _player.value.withMissionDayRefreshed(now)
        _player.value = p.copy(missionAdsCount = p.missionAdsCount + 1).applyMissions()
    }

    /** Appelé quand AdMob confirme la récompense pour le boost. */
    fun onBoostAdRewarded() {
        val now = System.currentTimeMillis()
        val p = _player.value
        _player.value = p.copy(
            boostUntil = max(now, p.boostUntil) + GameConfig.BOOST_MS,
            adsToday = p.adsToday + 1,
            adsDay = today(now)
        )
        recordAdWatched(now)
        save()
    }

    /** Appelé quand AdMob confirme la récompense pour la grosse ration. */
    fun onBigFeedAdRewarded() {
        _pendingBigFeed.value = true // on marque, on ne feed pas encore
        recordAdWatched(System.currentTimeMillis())
        save()
    }

    /** Appelé quand AdMob confirme la récompense pour l'auto-nourrissage. */
    fun onAutoFeedRewarded() {
        val now = System.currentTimeMillis()
        val p = _player.value
        val newUntil = min(
            max(now, p.autoFeedUntil) + GameConfig.AUTO_FEED_REWARD_MS,
            now + GameConfig.AUTO_FEED_MAX_MS
        )
        _player.value = p.copy(autoFeedUntil = newUntil)
        recordAdWatched(now)
        save()
    }

    fun consumePendingBigFeed() {
        if (_pendingBigFeed.value) {
            _pendingBigFeed.value = false
            feedBig()
        }
    }

    fun dismissOfflineGain() { _offlineGain.value = 0.0 }

    fun resetProgress() {
        val now = System.currentTimeMillis()
        val fresh = Player(lastSeen = now)
        _player.value = fresh
        _offlineGain.value = 0.0
        _loginReward.value = null
        viewModelScope.launch(Dispatchers.IO) { playerDao.save(fresh) }
    }
}