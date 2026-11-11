package com.vgarcia.gloubi

import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.vgarcia.gloubi.ads.RewardedAdManager
import com.vgarcia.gloubi.ui.GameScreen
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.vgarcia.gloubi.notifications.HungerNotificationWorker
import com.vgarcia.gloubi.notifications.HungerScheduler
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.edit
import com.vgarcia.gloubi.ui.SettingsScreen
import com.vgarcia.gloubi.ui.ShopScreen
import com.vgarcia.gloubi.ui.theme.GloubiTheme
import kotlinx.coroutines.launch

/**
 * Indique si l'activité principale est visible à l'écran.
 * Mis à jour par MainActivity (onStart / onStop). Lu par HungerNotificationWorker
 * pour ne pas notifier un joueur qui a déjà l'appli ouverte.
 */
object AppForeground {
    @Volatile
    var isForeground: Boolean = false
}

class MainActivity : ComponentActivity() {

    private val gameVm: GameViewModel by viewModels()
    val shopVm: ShopViewModel by viewModels()

    // Trois managers distincts : boost, grosse ration, auto-nourrissage
    private lateinit var adManagerBoost:   RewardedAdManager
    private lateinit var adManagerBigFeed: RewardedAdManager
    private lateinit var adManagerAuto:    RewardedAdManager

    private val adsInitialized  = AtomicBoolean(false)
    private val consentRequested = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Chaque manager reçoit son propre ID de bloc depuis BuildConfig
        adManagerBoost   = RewardedAdManager(applicationContext, BuildConfig.ADMOB_REWARDED_BOOST_ID)
        adManagerBigFeed = RewardedAdManager(applicationContext, BuildConfig.ADMOB_REWARDED_BIG_FEED_ID)
        adManagerAuto    = RewardedAdManager(applicationContext, BuildConfig.ADMOB_REWARDED_AUTO_ID)

        HungerScheduler.schedule(this)

        setContent {
            GloubiTheme {
                val player      by gameVm.player.collectAsState()
                val offline     by gameVm.offlineGain.collectAsState()
                val loginReward by gameVm.loginReward.collectAsState()
                val boostReady  by adManagerBoost.isReady.collectAsState()
                val bigReady    by adManagerBigFeed.isReady.collectAsState()
                val autoReady   by adManagerAuto.isReady.collectAsState()
                // adReady = true si au moins un manager est prêt (pour l'affichage général)
                val adReady = boostReady || bigReady || autoReady
                var feedTick    by remember { mutableIntStateOf(0) }
                var bigFeedTick by remember { mutableIntStateOf(0) }
                var now         by remember { mutableLongStateOf(System.currentTimeMillis()) }
                var showSettings by remember { mutableStateOf(false) }
                val notifEnabled = remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val shopItems by shopVm.items.collectAsState()
                var showShop by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    while (true) { now = System.currentTimeMillis(); delay(1000.milliseconds) }
                }
                LaunchedEffect(Unit) {
                    launch { gameVm.feedEvent.collect    { feedTick++ } }
                    launch { gameVm.bigFeedEvent.collect { bigFeedTick++ } }
                }

                if (showSettings) {
                    SettingsScreen(
                        notificationsEnabled = notifEnabled.value,
                        onToggleNotifications = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestPermissions(
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    0
                                )
                            }
                            notifEnabled.value = enabled
                            if (enabled) HungerScheduler.schedule(this)
                            else HungerScheduler.cancel(this)
                        },
                        onOpenConsentForm = {
                            UserMessagingPlatform.showPrivacyOptionsForm(this) {}
                        },
                        onResetProgress = {
                            gameVm.resetProgress()
                            shopVm.resetShop()
                            showSettings = false
                        },
                        onBack = { showSettings = false }
                    )
                } else if (showShop) {
                    ShopScreen(
                        items  = shopItems,
                        coins  = player.coins,
                        onBuy  = { item -> shopVm.buy(item, player.coins) { gameVm.spendCoins(it) } },
                        onEquip = shopVm::equip,
                        onBack = { showShop = false }
                    )
                } else {
                    GameScreen(
                        player = player,
                        nowMs = now,
                        adReady = adReady,
                        offlineGain = offline,
                        feedTick = feedTick,
                        bigFeedTick = bigFeedTick,
                        onFeed = {
                            if (gameVm.boosted()) gameVm.feedBoost() else gameVm.feedSmall()
                        },
                        onWatchBoostAd = {
                            when {
                                gameVm.canUseFreeBoost() -> gameVm.useFreeBoost()
                                gameVm.canWatchAd() && boostReady ->
                                    adManagerBoost.show(this) { gameVm.onBoostAdRewarded() }
                            }
                        },
                        onFeedBig = {
                            when {
                                gameVm.canUseFreeBigFeed() -> gameVm.useFreeBigFeed()
                                bigReady -> adManagerBigFeed.show(this) { gameVm.onBigFeedAdRewarded() }
                            }
                        },
                        onToggleAutoFeed = {
                            when {
                                gameVm.canUseFreeAutoFeed() -> gameVm.useFreeAutoFeed()
                                autoReady -> adManagerAuto.show(this) { gameVm.onAutoFeedRewarded() }
                            }
                        },
                        onDismissOffline = gameVm::dismissOfflineGain,
                        onSettings = { showSettings = true },
                        onShop = { showShop = true },
                        equippedSkin = shopVm.equippedSkin(),
                        equippedAnim = shopVm.equippedAnim(),
                        equippedFood = shopVm.equippedFood(),
                        freeBoostAvailable    = gameVm.canUseFreeBoost(),
                        freeBigFeedAvailable  = gameVm.canUseFreeBigFeed(),
                        freeAutoFeedAvailable = gameVm.canUseFreeAutoFeed(),
                        loginReward = loginReward,
                        onDismissLoginReward = gameVm::dismissLoginReward
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gameVm.onResumed()
        if (consentRequested.compareAndSet(false, true)) requestConsentThenInitAds()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        gameVm.consumePendingBigFeed()
        getSharedPreferences(HungerNotificationWorker.PREFS_NAME, MODE_PRIVATE).edit {
            putBoolean(HungerNotificationWorker.KEY_THRESHOLD_PENDING, false)
            putBoolean(HungerNotificationWorker.KEY_EMPTY_PENDING, false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(HungerNotificationWorker.NOTIF_ID_THRESHOLD)
        nm.cancel(HungerNotificationWorker.NOTIF_ID_EMPTY)
    }

    private fun requestConsentThenInitAds() {
        val info = UserMessagingPlatform.getConsentInformation(this)
        info.requestConsentInfoUpdate(
            this,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) {
                    if (info.canRequestAds()) initAds()
                }
            },
            { if (info.canRequestAds()) initAds() }
        )
        if (info.canRequestAds()) initAds()
    }

    private fun initAds() {
        if (adsInitialized.getAndSet(true)) return
        MobileAds.initialize(this) {
            adManagerBoost.load()
            adManagerBigFeed.load()
            adManagerAuto.load()
        }
    }

    override fun onStart() {
        super.onStart()
        AppForeground.isForeground = true
    }

    override fun onStop() {
        super.onStop()
        AppForeground.isForeground = false
        gameVm.saveOnPause()
    }
}