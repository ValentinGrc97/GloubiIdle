package com.example.idleads.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "player")
data class Player(
    @PrimaryKey val id: Int = 1,
    val satiety: Double = 50.0,     // barre de faim de l'animal, de 0 à 100 (%)
    val progress: Double = 0.0,     // progression cumulée : base du partage des gains
    val lastSeen: Long = System.currentTimeMillis(),
    val boostUntil: Long = 0L,      // fin du boost x2 obtenu via pub rewarded
    val adsToday: Int = 0,
    val adsDay: Long = 0L           // jour (UTC) du compteur adsToday
)

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player WHERE id = 1")
    suspend fun get(): Player?

    @Upsert
    suspend fun save(player: Player)
}

@Database(entities = [Player::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): PlayerDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: android.content.Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "idle.db")
                    .fallbackToDestructiveMigration() // schéma changé (pièces -> faim) : OK tant que l'appli n'est pas publiée
                    .build().also { instance = it }
            }
    }
}
