package com.vgarcia.gloubi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ShopCategory { SKIN, ANIMATION, FOOD }

@Entity(tableName = "shop_items")
data class ShopItem(
    @PrimaryKey val id: String,
    val category: ShopCategory,
    val nameKey: String,        // clé de ressource string pour la localisation
    val price: Int,             // prix en coins (0 = gratuit/déjà débloqué)
    val purchased: Boolean = false,
    val equipped: Boolean = false
)

/** Catalogue complet des items disponibles. Généré une seule fois au premier lancement. */
object ShopCatalog {
    val items = listOf(
        // ── Skins ─────────────────────────────────────────────────────────────
        ShopItem("skin_default",   ShopCategory.SKIN, "shop_skin_default",   0,    purchased = true, equipped = true),
        ShopItem("skin_gold",      ShopCategory.SKIN, "shop_skin_gold",      500),
        ShopItem("skin_rainbow",   ShopCategory.SKIN, "shop_skin_rainbow",   1200),
        ShopItem("skin_ghost",     ShopCategory.SKIN, "shop_skin_ghost",     800),
        ShopItem("skin_fire",      ShopCategory.SKIN, "shop_skin_fire",      1500),
        ShopItem("skin_ice",       ShopCategory.SKIN, "shop_skin_ice",       1500),

        // ── Animations ────────────────────────────────────────────────────────
        ShopItem("anim_default",   ShopCategory.ANIMATION, "shop_anim_default",   0,   purchased = true, equipped = true),
        ShopItem("anim_dance",     ShopCategory.ANIMATION, "shop_anim_dance",     600),
        ShopItem("anim_spin",      ShopCategory.ANIMATION, "shop_anim_spin",      400),
        ShopItem("anim_shake",     ShopCategory.ANIMATION, "shop_anim_shake",     300),
        ShopItem("anim_flip",      ShopCategory.ANIMATION, "shop_anim_flip",      700),

        // ── Nourritures ───────────────────────────────────────────────────────
        ShopItem("food_kibble",    ShopCategory.FOOD, "shop_food_kibble",    0,   purchased = true, equipped = true),
        ShopItem("food_apple",     ShopCategory.FOOD, "shop_food_apple",     0,   purchased = true),
        ShopItem("food_pizza",     ShopCategory.FOOD, "shop_food_pizza",     350),
        ShopItem("food_star",      ShopCategory.FOOD, "shop_food_star",      800),
        ShopItem("food_diamond",   ShopCategory.FOOD, "shop_food_diamond",   2000),
        ShopItem("food_cake",      ShopCategory.FOOD, "shop_food_cake",      600),
    )
}