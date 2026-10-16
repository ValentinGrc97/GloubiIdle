package com.vgarcia.gloubi

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
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

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()
    private lateinit var adManager: RewardedAdManager
    private val adsInitialized = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adManager = RewardedAdManager(applicationContext)
        requestConsentThenInitAds()

        // Plein écran immersif
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // API < 30 : ancienne méthode
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }

        // Notifications
        HungerScheduler.schedule(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        setContent {
            MaterialTheme {
                val player by vm.player.collectAsState()
                val offline by vm.offlineGain.collectAsState()
                val adReady by adManager.isReady.collectAsState()
                val feedTick by vm.feedTick.collectAsState()
                val bigFeedTick by vm.bigFeedTick.collectAsState()
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) { now = System.currentTimeMillis(); delay(1000.milliseconds) }
                }

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
                        if (vm.canWatchAd()) adManager.show(this) { vm.onBoostAdRewarded() }
                    },
                    onFeedBig = {
                        adManager.show(this) { vm.onBigFeedAdRewarded() }
                    },
                    onToggleAutoFeed = {
                        adManager.show(this) { vm.onAutoFeedRewarded() }
                    },
                    onDismissOffline = vm::dismissOfflineGain
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.consumePendingBigFeed()
        // L'utilisateur a ouvert l'appli : les notifications sont "vues"
        getSharedPreferences(HungerNotificationWorker.PREFS_NAME, MODE_PRIVATE)
            .edit {
                putBoolean(HungerNotificationWorker.KEY_THRESHOLD_PENDING, false)
                    .putBoolean(HungerNotificationWorker.KEY_EMPTY_PENDING, false)
            }
        // Annuler les notifications visuelles encore visibles dans le tiroir
        val nm = getSystemService(NotificationManager::class.java)
        nm.cancel(HungerNotificationWorker.NOTIF_ID_THRESHOLD)
        nm.cancel(HungerNotificationWorker.NOTIF_ID_EMPTY)
    }

    /** Consentement RGPD (UMP) avant toute demande de pub. */
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
        MobileAds.initialize(this) { adManager.load() }
    }

    override fun onStop() {
        super.onStop()
        vm.save()
    }
}