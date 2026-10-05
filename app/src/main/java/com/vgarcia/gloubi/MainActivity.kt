package com.vgarcia.gloubi

import android.os.Bundle
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
import com.vgarcia.gloubi.ads.RewardedAdManager
import com.vgarcia.gloubi.ui.GameScreen
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()
    private lateinit var adManager: RewardedAdManager
    private val adsInitialized = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adManager = RewardedAdManager(applicationContext)
        requestConsentThenInitAds()

        setContent {
            MaterialTheme {
                val player by vm.player.collectAsState()
                val offline by vm.offlineGain.collectAsState()
                val adReady by adManager.isReady.collectAsState()
                val feedTick by vm.feedTick.collectAsState()
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
                    onFeed = {
                        if (vm.boosted()) vm.feedBoost() else vm.feedSmall()
                    },
                    onWatchBoostAd = {
                        if (vm.canWatchAd()) adManager.show(this) { vm.onBoostAdRewarded() }
                    },
                    onFeedBig = { adManager.show(this) { vm.feedBig() } },
                    onToggleAutoFeed = {
                        adManager.show(this) { vm.onAutoFeedRewarded() }
                    },
                    onDismissOffline = vm::dismissOfflineGain
                )
            }
        }
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