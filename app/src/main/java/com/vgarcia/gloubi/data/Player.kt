package com.vgarcia.gloubi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player")
data class Player(
    @PrimaryKey val id: Int = 1,
    val satiety: Double = 75.0,                         // barre de faim de l'animal, de 0 à 100 (%)
    val progress: Double = 0.0,                         // progression cumulée -> XP
    val coins: Int = 0,                                 // monnaie virtuelle : 1 XP gagné = 1 coin
    val lastSeen: Long = System.currentTimeMillis(),    // timestamp de la dernière interaction
    val boostUntil: Long = 0L,                          // fin du boost x10 obtenu via pub rewarded
    val autoFeedUntil: Long = 0L,                       // timestamp de fin de l'auto-nourrissage (0 = inactif)
    val adsToday: Int = 0,                              // nombre d'Ads vues sur la journée
    val adsDay: Long = 0L,

    // Usage gratuit quotidien (jour UTC de la dernière utilisation, 0 = jamais)
    val freeBoostUsedDay: Long = 0L,
    val freeBigFeedUsedDay: Long = 0L,
    val freeAutoFeedUsedDay: Long = 0L,

    // Connexion journalière
    val loginStreak: Int = 0,          // nombre de jours consécutifs de connexion
    val lastLoginDay: Long = 0L,       // jour UTC de la dernière connexion traitée

    // Missions quotidiennes
    val missionDay: Long = 0L,         // jour UTC auquel les compteurs ci-dessous se rapportent
    val missionFeedCount: Int = 0,     // nourrissages du jour
    val missionAboveMs: Long = 0L,     // temps passé >= 95 % aujourd'hui (ms)
    val missionAdsCount: Int = 0,      // pubs regardées aujourd'hui
    val missionClaimedMask: Int = 0,   // bit 0 = nourrir, bit 1 = 95 %, bit 2 = pubs (récompense versée)
)