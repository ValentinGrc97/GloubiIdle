package com.example.idleads.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RewardedAdManager(private val context: Context) {

    companion object {
        // ID de TEST Google pour les pubs rewarded. À remplacer par votre vrai bloc.
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TAG = "RewardedAdManager"
    }

    private var rewardedAd: RewardedAd? = null
    private var loading = false

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun load() {
        if (loading || rewardedAd != null) return
        loading = true
        RewardedAd.load(
            context,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    loading = false
                    _isReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Chargement échoué: ${error.message}")
                    rewardedAd = null
                    loading = false
                    _isReady.value = false
                }
            }
        )
    }

    /** [onReward] n'est appelé que si le joueur a regardé la pub jusqu'au bout. */
    fun show(activity: Activity, onReward: () -> Unit) {
        val ad = rewardedAd ?: run { load(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isReady.value = false
                load() // précharge la suivante
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _isReady.value = false
                load()
            }
        }
        ad.show(activity) { onReward() }
    }
}
