package com.vgarcia.gloubi.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player WHERE id = 1")
    suspend fun get(): Player?

    @Upsert
    suspend fun save(player: Player)
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_items ORDER BY category, price")
    fun observe(): Flow<List<ShopItem>>

    @Query("SELECT * FROM shop_items ORDER BY category, price")
    suspend fun getAll(): List<ShopItem>

    @Upsert
    suspend fun save(item: ShopItem)

    @Upsert
    suspend fun saveAll(items: List<ShopItem>)

    @Query("SELECT * FROM shop_items WHERE category = :cat AND equipped = 1 LIMIT 1")
    suspend fun getEquipped(cat: ShopCategory): ShopItem?
}

@Database(entities = [Player::class, ShopItem::class], version = 8, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun shopDao(): ShopDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: android.content.Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDb::class.java, "idle.db"
                ).build().also { instance = it }
            }
    }
}