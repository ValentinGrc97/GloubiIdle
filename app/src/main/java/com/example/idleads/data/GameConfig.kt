package com.example.idleads.data

object GameConfig {
    // --- Animal ---
    const val FEED_SMALL = 0.01          // bouton 1 : +0,01 % de faim
    const val FEED_BIG = 1.0             // bouton 3 : +1 %
    const val DECAY_PER_SEC = 0.05       // perte de 0,05 % par seconde
    const val GAIN_THRESHOLD = 99.0      // gains actifs à partir de 99 %

    // --- Gains ---
    const val BASE_RATE = 1.0                        // points de progression/seconde quand l'animal est rassasié
    const val MAX_OFFLINE_MS = 8 * 60 * 60 * 1000L   // gains hors-ligne plafonnés à 8 h
    const val BOOST_MS = 30 * 60 * 1000L             // durée du boost obtenu par pub
    const val INSTANT_BONUS_SECONDS = 60             // bonus immédiat = 60 s de production
    const val MAX_ADS_PER_DAY = 10                   // plafond quotidien (anti-abus)

    // --- Partage des gains (valeurs d'exemple) ---
    // En production : ces valeurs doivent venir du SERVEUR (Remote Config / backend),
    // calculées à partir de votre eCPM réel, jamais codées en dur dans l'appli.
    const val EUR_PER_1000_PROGRESS = 0.01
    const val PAYOUT_THRESHOLD_EUR = 5.0

    fun rate(boosted: Boolean): Double = BASE_RATE * (if (boosted) 2.0 else 1.0)

    fun estimatedPayoutEur(progress: Double): Double = progress / 1000.0 * EUR_PER_1000_PROGRESS
}
