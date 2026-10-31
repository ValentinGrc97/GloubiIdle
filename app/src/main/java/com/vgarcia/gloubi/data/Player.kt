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

    // Usage gratuit quotidien : 0 = disponible, sinon = jour (UTC) de la dernière utilisation
    val freeBoostUsedDay: Long = 0L,
    val freeBigFeedUsedDay: Long = 0L,
    val freeAutoFeedUsedDay: Long = 0L,
)