package com.vgarcia.gloubi.data

import kotlin.math.pow

object GameConfig {
    // --- Animal ---
    const val FEED_SMALL = 0.01                      // bouton 1 : +0,01 % de faim
    const val FEED_BOOST = 0.1                       // bouton 2 actif : +0,1 % de faim/clic bouton 1
    const val FEED_BIG = 10.0                        // bouton 3 : grosse ration (pub) : +10 % de faim
    const val AUTO_FEED_REWARD_MS = 15 * 60 * 1000L  // bouton 4 : +15 min par pub regardée, limite à AUTO_FEED_MAX_MS
    const val DECAY_PER_SEC = 0.01                   // perte de 0,01 % par seconde
    const val RATIO = 1                              // ratio par rapport à DECAY_PER_SEC pour actualiser la faim tous les 0.01%
    const val GAIN_THRESHOLD = 95.0                  // gains actifs à partir de 95 %
    const val AUTO_FEED_PER_SEC = 0.02               // nourrissage auto : +0,02 %/s
    const val AUTO_FEED_MAX_MS = 150 * 60 * 1000L    // plafond : 2h30

    // --- Gains ---
    const val BASE_RATE = 1.0                        // points de progression/seconde quand l'animal est rassasié
    const val MAX_OFFLINE_MS = 8 * 60 * 60 * 1000L   // gains hors-ligne plafonnés à 8 h
    const val BOOST_MS = 5 * 60 * 1000L              // durée du boost obtenu par pub (5 minutes)
    const val MAX_ADS_PER_DAY = 10                   // plafond quotidien (anti-abus)

    // --- Niveaux (XP exponentielle) ---
    const val XP_BASE = 100.0          // XP pour passer du niveau 0 au niveau 1
    const val XP_MULTIPLIER = 1.5      // chaque niveau coûte XP_MULTIPLIER fois plus que le précédent

    /** XP totale cumulée nécessaire pour atteindre [level]. */
    fun xpRequiredForLevel(level: Int): Double {
        if (level <= 0) return 0.0
        // Somme géométrique : XP_BASE * (2^level - 1)
        return XP_BASE * (XP_MULTIPLIER.pow(level) - 1.0) / (XP_MULTIPLIER - 1.0)
    }

    /** Niveau actuel à partir de la progression cumulée. */
    fun levelFromProgress(progress: Double): Int {
        var level = 0
        while (progress >= xpRequiredForLevel(level + 1)) level++
        return level
    }

    /** XP dans le niveau courant et XP totale nécessaire pour ce niveau. */
    fun xpInCurrentLevel(progress: Double): Pair<Double, Double> {
        val level = levelFromProgress(progress)
        val xpStart = xpRequiredForLevel(level)
        val xpEnd = xpRequiredForLevel(level + 1)
        return Pair(progress - xpStart, xpEnd - xpStart)
    }
}