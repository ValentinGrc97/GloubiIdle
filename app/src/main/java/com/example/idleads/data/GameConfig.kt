package com.example.idleads.data

object GameConfig {
    // --- Animal ---
    const val FEED_SMALL = 0.05          // bouton 1 : +0,05 % de faim
    const val FEED_BOOST = 0.5          // bouton 2 actif : +0,5 % de faim/clic bouton 1
    const val FEED_BIG = 10.0             // bouton 3 : +10 % de faim
    const val DECAY_PER_SEC = 0.05       // perte de 0,05 % par seconde
    const val RATIO = 5                  // ratio par rapport à DECAY_PER_SEC pour actualiser la faim tous les 0.01%
    const val GAIN_THRESHOLD = 90.0      // gains actifs à partir de 90 %

    // --- Gains ---
    const val BASE_RATE = 1.0                        // points de progression/seconde quand l'animal est rassasié
    const val MAX_OFFLINE_MS = 8 * 60 * 60 * 1000L   // gains hors-ligne plafonnés à 8 h
    const val BOOST_MS = 5 * 60 * 1000L             // durée du boost obtenu par pub (5 minutes)
    const val MAX_ADS_PER_DAY = 10                   // plafond quotidien (anti-abus)

    // --- Partage des gains (valeurs d'exemple) ---
    // En production : ces valeurs doivent venir du SERVEUR (Remote Config / backend),
    // calculées à partir de votre eCPM réel, jamais codées en dur dans l'appli.
    const val EUR_PER_1000_PROGRESS = 0.02
    const val PAYOUT_THRESHOLD_EUR = 5.0

    fun rate(boosted: Boolean): Double = BASE_RATE * (if (boosted) 2.0 else 1.0)

    fun estimatedPayoutEur(progress: Double): Double = progress / 1000.0 * EUR_PER_1000_PROGRESS
}
