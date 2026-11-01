package com.vgarcia.gloubi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vgarcia.gloubi.data.AppDb
import com.vgarcia.gloubi.data.ShopCatalog
import com.vgarcia.gloubi.data.ShopCategory
import com.vgarcia.gloubi.data.ShopItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDb.get(app)
    private val playerDao = db.playerDao()
    private val shopDao = db.shopDao()

    val items: StateFlow<List<ShopItem>> = shopDao.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Peupler le catalogue au premier lancement
        viewModelScope.launch(Dispatchers.IO) {
            val existing = shopDao.getAll().map { it.id }.toSet()
            val toInsert = ShopCatalog.items.filter { it.id !in existing }
            if (toInsert.isNotEmpty()) shopDao.saveAll(toInsert)
        }
    }

    /** Acheter un item si le joueur a assez de coins. Retourne true si succès. */
    fun buy(item: ShopItem, currentCoins: Int, onCoinsDeducted: (Int) -> Unit): Boolean {
        if (item.purchased) return false
        if (currentCoins < item.price) return false
        viewModelScope.launch(Dispatchers.IO) {
            shopDao.save(item.copy(purchased = true))
        }
        onCoinsDeducted(item.price)
        return true
    }

    /** Équiper un item acheté (un seul équipé par catégorie). */
    fun equip(item: ShopItem) {
        if (!item.purchased) return
        viewModelScope.launch(Dispatchers.IO) {
            // Déséquiper tous les items de la même catégorie
            shopDao.getAll()
                .filter { it.category == item.category && it.equipped }
                .forEach { shopDao.save(it.copy(equipped = false)) }
            // Équiper le nouvel item
            shopDao.save(item.copy(equipped = true))
        }
    }

    fun equippedSkin()      = items.value.firstOrNull { it.category == ShopCategory.SKIN      && it.equipped }?.id ?: "skin_default"
    fun equippedAnim()      = items.value.firstOrNull { it.category == ShopCategory.ANIMATION && it.equipped }?.id ?: "anim_default"
    fun equippedFood()      = items.value.firstOrNull { it.category == ShopCategory.FOOD      && it.equipped }?.id ?: "food_kibble"

    fun resetShop() {
        viewModelScope.launch(Dispatchers.IO) {
            shopDao.saveAll(ShopCatalog.items)
        }
    }
}