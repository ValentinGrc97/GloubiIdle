package com.vgarcia.gloubi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vgarcia.gloubi.R
import com.vgarcia.gloubi.data.ShopCatalog
import com.vgarcia.gloubi.data.ShopCategory
import com.vgarcia.gloubi.data.ShopItem
import com.vgarcia.gloubi.ui.theme.GloubiBackground
import com.vgarcia.gloubi.ui.theme.GloubiGreen
import com.vgarcia.gloubi.ui.theme.GloubiOrange
import com.vgarcia.gloubi.ui.theme.GloubiRed
import com.vgarcia.gloubi.ui.theme.GloubiSurface
import com.vgarcia.gloubi.ui.theme.GloubiSurface2
import com.vgarcia.gloubi.ui.theme.GloubiTheme
import com.vgarcia.gloubi.ui.theme.GloubiYellow

// Emoji associé à chaque item
private fun itemEmoji(id: String) = when (id) {
    "skin_default"  -> "🐾"
    "skin_gold"     -> "✨"
    "skin_rainbow"  -> "🌈"
    "skin_ghost"    -> "👻"
    "skin_fire"     -> "🔥"
    "skin_ice"      -> "❄️"
    "anim_default"  -> "💤"
    "anim_dance"    -> "💃"
    "anim_spin"     -> "🌀"
    "anim_shake"    -> "😵"
    "anim_flip"     -> "🤸"
    "food_kibble"   -> "🍪"
    "food_apple"    -> "🍎"
    "food_pizza"    -> "🍕"
    "food_star"     -> "⭐"
    "food_diamond"  -> "💎"
    "food_cake"     -> "🎂"
    else -> "❓"
}

// Couleur d'accent par item
private fun itemAccent(id: String) = when {
    id.contains("gold")    -> Color(0xFFFFD54F)
    id.contains("rainbow") -> Color(0xFFFF80AB)
    id.contains("ghost")   -> Color(0xFFB0BEC5)
    id.contains("fire")    -> Color(0xFFFF6D00)
    id.contains("ice")     -> Color(0xFF80DEEA)
    id.contains("dance")   -> Color(0xFFE040FB)
    id.contains("spin")    -> Color(0xFF40C4FF)
    id.contains("star")    -> Color(0xFFFFD740)
    id.contains("diamond") -> Color(0xFF64B5F6)
    else                   -> GloubiGreen
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    coins: Int,
    onBuy: (ShopItem) -> Unit,
    onEquip: (ShopItem) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val accent = itemAccent(item.id)
    val borderColor by animateColorAsState(
        if (item.equipped) accent else Color.Transparent,
        animationSpec = tween(300), label = "border"
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.shop_confirm_buy, item.price)) },
            text  = { Text(stringResource(R.string.shop_confirm_desc, coins)) },
            confirmButton = {
                Button(
                    onClick = { onBuy(item); showDialog = false },
                    enabled = coins >= item.price,
                    colors = ButtonDefaults.buttonColors(containerColor = GloubiGreen)
                ) { Text(stringResource(R.string.shop_buy)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = GloubiSurface
        )
    }

    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(GloubiSurface2)
            .clickable {
                when {
                    item.equipped   -> { /* déjà équipé */ }
                    item.purchased  -> onEquip(item)
                    else            -> showDialog = true
                }
            }
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Emoji grand format
            Text(itemEmoji(item.id), fontSize = 36.sp, modifier = Modifier.padding(bottom = 6.dp))

            // Nom
            Text(
                stringResource(shopNameRes(item.nameKey)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))

            // Badge état
            when {
                item.equipped -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(14.dp), tint = accent)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.shop_equipped), style = MaterialTheme.typography.labelSmall, color = accent)
                }
                item.purchased -> Button(
                    onClick = { onEquip(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = GloubiSurface),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(stringResource(R.string.shop_equip), style = MaterialTheme.typography.labelSmall)
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(12.dp),
                        tint = if (coins >= item.price) GloubiYellow else GloubiRed)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        item.price.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (coins >= item.price) GloubiYellow else GloubiRed
                    )
                    Spacer(Modifier.width(2.dp))
                    Image(painter = painterResource(R.drawable.coins), contentDescription = stringResource(id = R.string.coins), Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun ShopScreen(
    items: List<ShopItem>,
    coins: Int,
    onBuy: (ShopItem) -> Unit,
    onEquip: (ShopItem) -> Unit,
    onBack: () -> Unit
) {
    val tabs = listOf(
        ShopCategory.SKIN      to stringResource(R.string.shop_tab_skins),
        ShopCategory.ANIMATION to stringResource(R.string.shop_tab_animations),
        ShopCategory.FOOD      to stringResource(R.string.shop_tab_foods),
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val filtered = items.filter { it.category == tabs[selectedTab].first }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GloubiBackground, Color(0xFF0E1A12))))
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    Text(stringResource(R.string.shop_title),
                        style = MaterialTheme.typography.headlineMedium, color = Color.White)
                }
                // Solde coins
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFB8860B), GloubiYellow)))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            coins.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1A1A00),
                            fontWeight = FontWeight.Bold,
                           fontSize = 16.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Image(painter = painterResource(R.drawable.coins), contentDescription = stringResource(id = R.string.coins), Modifier.size(20.dp))
                    }
                }
            }

            // ── Onglets ────────────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = GloubiSurface,
                contentColor = GloubiGreen,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { i, (_, label) ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text = { Text(label, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }

            // ── Grille items ───────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.id }) { item ->
                    ShopItemCard(item = item, coins = coins, onBuy = onBuy, onEquip = onEquip)
                }
            }
        }
    }
}

// Helper pour mapper nameKey -> res ID
@Composable
private fun shopNameRes(key: String): Int = when (key) {
    "shop_skin_default"  -> R.string.shop_skin_default
    "shop_skin_gold"     -> R.string.shop_skin_gold
    "shop_skin_rainbow"  -> R.string.shop_skin_rainbow
    "shop_skin_ghost"    -> R.string.shop_skin_ghost
    "shop_skin_fire"     -> R.string.shop_skin_fire
    "shop_skin_ice"      -> R.string.shop_skin_ice
    "shop_anim_default"  -> R.string.shop_anim_default
    "shop_anim_dance"    -> R.string.shop_anim_dance
    "shop_anim_spin"     -> R.string.shop_anim_spin
    "shop_anim_shake"    -> R.string.shop_anim_shake
    "shop_anim_flip"     -> R.string.shop_anim_flip
    "shop_food_kibble"   -> R.string.shop_food_kibble
    "shop_food_apple"    -> R.string.shop_food_apple
    "shop_food_pizza"    -> R.string.shop_food_pizza
    "shop_food_star"     -> R.string.shop_food_star
    "shop_food_diamond"  -> R.string.shop_food_diamond
    "shop_food_cake"     -> R.string.shop_food_cake
    else                 -> R.string.shop_skin_default
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewShop() {
    GloubiTheme {
        ShopScreen(
            items = ShopCatalog.items,
            coins = 850,
            onBuy = {}, onEquip = {}, onBack = {}
        )
    }
}