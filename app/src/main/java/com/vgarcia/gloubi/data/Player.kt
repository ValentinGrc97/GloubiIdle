package com.vgarcia.gloubi.data

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
    val satiety: Double = 50.0,                         // barre de faim de l'animal, de 0 à 100 (%)
    val progress: Double = 0.0,                         // progression cumulée -> XP
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

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player WHERE id = 1")
    suspend fun get(): Player?

    @Upsert
    suspend fun save(player: Player)
}

@Database(entities = [Player::class], version = 5, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): PlayerDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: android.content.Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDb::class.java, "idle.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build().also { instance = it }
            }
    }
}