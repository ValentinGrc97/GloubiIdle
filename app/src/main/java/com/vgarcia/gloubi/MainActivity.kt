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
import com.vgarcia.gloubi.ui.theme.GloubiTheme

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()

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
                val player      by vm.player.collectAsState()
                val offline     by vm.offlineGain.collectAsState()
                val boostReady  by adManagerBoost.isReady.collectAsState()
                val bigReady    by adManagerBigFeed.isReady.collectAsState()
                val autoReady   by adManagerAuto.isReady.collectAsState()
                // adReady = true si au moins un manager est prêt (pour l'affichage général)
                val adReady = boostReady || bigReady || autoReady
                val feedTick    by vm.feedTick.collectAsState()
                val bigFeedTick by vm.bigFeedTick.collectAsState()
                var now         by remember { mutableLongStateOf(System.currentTimeMillis()) }
                var showSettings by remember { mutableStateOf(false) }
                val notifEnabled = remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        else true
                    )
                }

                LaunchedEffect(Unit) {
                    while (true) { now = System.currentTimeMillis(); delay(1000.milliseconds) }
                }

                if (showSettings) {
                    SettingsScreen(
                        notificationsEnabled = notifEnabled.value,
                        onToggleNotifications = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
                            }
                            notifEnabled.value = enabled
                            if (enabled) HungerScheduler.schedule(this)
                            else HungerScheduler.cancel(this)
                        },
                        onOpenConsentForm = {
                            UserMessagingPlatform.showPrivacyOptionsForm(this) {}
                        },
                        onResetProgress = {
                            vm.resetProgress()
                            showSettings = false
                        },
                        onBack = { showSettings = false }
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
                            if (vm.boosted()) vm.feedBoost() else vm.feedSmall()
                        },
                        onWatchBoostAd = {
                            when {
                                vm.canUseFreeBoost() -> vm.useFreeBoost()
                                vm.canWatchAd() && boostReady ->
                                    adManagerBoost.show(this) { vm.onBoostAdRewarded() }
                            }
                        },
                        onFeedBig = {
                            when {
                                vm.canUseFreeBigFeed() -> vm.useFreeBigFeed()
                                bigReady -> adManagerBigFeed.show(this) { vm.onBigFeedAdRewarded() }
                            }
                        },
                        onToggleAutoFeed = {
                            when {
                                vm.canUseFreeAutoFeed() -> vm.useFreeAutoFeed()
                                autoReady -> adManagerAuto.show(this) { vm.onAutoFeedRewarded() }
                            }
                        },
                        onDismissOffline = vm::dismissOfflineGain,
                        onSettings = { showSettings = true },
                        freeBoostAvailable    = vm.canUseFreeBoost(),
                        freeBigFeedAvailable  = vm.canUseFreeBigFeed(),
                        freeAutoFeedAvailable = vm.canUseFreeAutoFeed()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (consentRequested.compareAndSet(false, true)) requestConsentThenInitAds()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        vm.consumePendingBigFeed()
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

    override fun onStop() {
        super.onStop()
        vm.saveOnPause()
    }
}