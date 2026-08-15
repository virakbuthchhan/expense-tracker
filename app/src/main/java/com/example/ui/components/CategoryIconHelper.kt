package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    val availableIcons = listOf(
        "restaurant" to Icons.Default.Restaurant,
        "fastfood" to Icons.Default.Fastfood,
        "local_cafe" to Icons.Default.LocalCafe,
        "directions_car" to Icons.Default.DirectionsCar,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "home" to Icons.Default.Home,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "fitness_center" to Icons.Default.FitnessCenter,
        "medical_services" to Icons.Default.MedicalServices,
        "movie" to Icons.Default.Movie,
        "flight" to Icons.Default.Flight,
        "school" to Icons.Default.School,
        "payments" to Icons.Default.Payments,
        "account_balance" to Icons.Default.AccountBalance,
        "laptop" to Icons.Default.Laptop,
        "trending_up" to Icons.Default.TrendingUp,
        "card_giftcard" to Icons.Default.CardGiftcard,
        "pets" to Icons.Default.Pets,
        "wifi" to Icons.Default.Wifi,
        "work" to Icons.Default.Work,
        "more_horiz" to Icons.Default.MoreHoriz
    )

    fun getIcon(name: String): ImageVector {
        return availableIcons.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
            ?: Icons.Default.Category
    }
}
