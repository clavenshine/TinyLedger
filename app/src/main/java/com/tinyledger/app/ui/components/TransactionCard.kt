package com.tinyledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.Transaction
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.theme.ExpenseRed
import com.tinyledger.app.ui.theme.IncomeGreen
import com.tinyledger.app.util.CurrencyUtils
import com.tinyledger.app.util.DateUtils

@Composable
fun TransactionCard(
    transaction: Transaction,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    flat: Boolean = false
) {
    // Use sign-based amount display: positive = income (+), negative = expense (-)
    // For TRANSFER/LENDING, amount is already signed (positive for inflow, negative for outflow)
    val isIncome = when (transaction.type) {
        TransactionType.INCOME -> true
        TransactionType.EXPENSE -> false
        TransactionType.TRANSFER, TransactionType.LENDING -> transaction.amount > 0
    }
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val amountPrefix = if (isIncome) "+" else "-"
    val displayAmount = kotlin.math.abs(transaction.amount)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!flat) Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (flat) 0.dp else 3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Category and Note
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getDisplayCategoryName(transaction.category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note.take(80),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.formatDisplayDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 如果有图片，显示照片小图标
                    if (!transaction.imagePath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val iconSize = with(LocalDensity.current) {
                            MaterialTheme.typography.bodySmall.fontSize.toDp()
                        }
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "有图片",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }

            // Amount and reimbursement status
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyUtils.format(displayAmount, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                // 报销状态标签（仅当有报销标记时显示）
                if (transaction.reimbursementStatus != com.tinyledger.app.domain.model.ReimbursementStatus.NONE) {
                    val statusColor = when (transaction.reimbursementStatus) {
                        com.tinyledger.app.domain.model.ReimbursementStatus.PENDING -> MaterialTheme.colorScheme.error
                        com.tinyledger.app.domain.model.ReimbursementStatus.REIMBURSED -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val statusText = when (transaction.reimbursementStatus) {
                        com.tinyledger.app.domain.model.ReimbursementStatus.PENDING -> "待报销"
                        com.tinyledger.app.domain.model.ReimbursementStatus.REIMBURSED -> "已报销"
                        else -> ""
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        // 旧版ic_前缀图标
        "ic_food" -> Icons.Default.Restaurant
        "ic_transport" -> Icons.Default.DirectionsCar
        "ic_shopping" -> Icons.Default.ShoppingBag
        "ic_entertainment" -> Icons.Default.SportsEsports
        "ic_home" -> Icons.Default.Home
        "ic_medical" -> Icons.Default.LocalHospital
        "ic_education" -> Icons.Default.School
        "ic_communication" -> Icons.Default.Phone
        "ic_other" -> Icons.Default.MoreHoriz
        "ic_salary" -> Icons.Default.AccountBalance
        "ic_bonus" -> Icons.Default.CardGiftcard
        "ic_investment" -> Icons.Default.TrendingUp
        "ic_parttime" -> Icons.Default.Work
        "ic_redpacket" -> Icons.Default.Redeem
        "ic_other_income" -> Icons.Default.AttachMoney
        // 兼容Transaction.kt中的图标名
        "restaurant" -> Icons.Default.Restaurant
        "directions_bus" -> Icons.Default.DirectionsBus
        "shopping_bag" -> Icons.Default.ShoppingBag
        "local_movies" -> Icons.Default.LocalMovies
        "home" -> Icons.Default.Home
        "medical" -> Icons.Default.MedicalServices
        "education" -> Icons.Default.School
        "communication" -> Icons.Default.Phone
        "insurance" -> Icons.Default.Security
        "travel" -> Icons.Default.Flight
        "lend" -> Icons.Default.Money
        "investment_expense" -> Icons.Default.TrendingDown
        "other" -> Icons.Default.MoreHoriz
        "salary" -> Icons.Default.Work
        "bonus" -> Icons.Default.Star
        "investment" -> Icons.Default.TrendingUp
        "financial" -> Icons.Default.AccountBalance
        "redpacket" -> Icons.Default.Redeem
        "utilities" -> Icons.Default.ElectricalServices
        "dividend" -> Icons.Default.Paid
        "refund" -> Icons.Default.AssignmentReturn
        "deposit_back" -> Icons.Default.AccountBalanceWallet
        "accommodation" -> Icons.Default.Hotel
        "charity" -> Icons.Default.VolunteerActivism
        "send_redpacket" -> Icons.Default.CardGiftcard
        "family_living" -> Icons.Default.FamilyRestroom
        "children" -> Icons.Default.ChildCare
        "elderly_care" -> Icons.Default.Elderly
        "reimbursement" -> Icons.Default.RequestPage
        // 吃喝分类
        "ramen_dining" -> Icons.Default.RamenDining
        "local_cafe" -> Icons.Default.LocalCafe
        "local_bar" -> Icons.Default.LocalBar
        "lunch_dining" -> Icons.Default.LunchDining
        "dinner_dining" -> Icons.Default.DinnerDining
        "kebab_dining" -> Icons.Default.KebabDining
        "bakery_dining" -> Icons.Default.BakeryDining
        "local_pizza" -> Icons.Default.LocalPizza
        "icecream" -> Icons.Default.Icecream
        "cake" -> Icons.Default.Cake
        "hamburger" -> Icons.Default.Fastfood
        "flatware" -> Icons.Default.Flatware
        "local_drink" -> Icons.Default.LocalDrink
        "tapas" -> Icons.Default.Tapas
        "wine_bar" -> Icons.Default.WineBar
        // 购物分类
        "shopping_cart" -> Icons.Default.ShoppingCart
        "local_mall" -> Icons.Default.LocalMall
        "checkroom" -> Icons.Default.Checkroom
        "store" -> Icons.Default.Store
        "diamond" -> Icons.Default.Diamond
        "camera_alt" -> Icons.Default.CameraAlt
        "smartphone" -> Icons.Default.Smartphone
        "content_cut" -> Icons.Default.ContentCut
        "local_offer" -> Icons.Default.LocalOffer
        // 交通分类
        "directions_car" -> Icons.Default.DirectionsCar
        "directions_bike" -> Icons.Default.DirectionsBike
        "flight" -> Icons.Default.Flight
        "train" -> Icons.Default.Train
        "ev_station" -> Icons.Default.EvStation
        "local_parking" -> Icons.Default.LocalParking
        "local_gas_station" -> Icons.Default.LocalGasStation
        "directions_boat" -> Icons.Default.DirectionsBoat
        "two_wheeler" -> Icons.Default.TwoWheeler
        "directions_railway" -> Icons.Default.DirectionsRailway
        "pedal_bike" -> Icons.Default.PedalBike
        // 住房分类
        "home_work" -> Icons.Default.HomeWork
        "bolt" -> Icons.Default.Bolt
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "water_drop" -> Icons.Default.WaterDrop
        "wifi" -> Icons.Default.Wifi
        "bed" -> Icons.Default.Bed
        "hotel" -> Icons.Default.Hotel
        "ac_unit" -> Icons.Default.AcUnit
        "phone_android" -> Icons.Default.PhoneAndroid
        "kitchen" -> Icons.Default.Kitchen
        "tv" -> Icons.Default.Tv
        // 娱乐分类
        "sports_esports" -> Icons.Default.SportsEsports
        "videogame_asset" -> Icons.Default.VideogameAsset
        "theaters" -> Icons.Default.Theaters
        "movie" -> Icons.Default.Movie
        "music_note" -> Icons.Default.MusicNote
        "sports_soccer" -> Icons.Default.SportsSoccer
        "sports_basketball" -> Icons.Default.SportsBasketball
        "beach_access" -> Icons.Default.BeachAccess
        "pool" -> Icons.Default.Pool
        "celebration" -> Icons.Default.Celebration
        "festival" -> Icons.Default.Festival
        "casino" -> Icons.Default.Casino
        // 运动分类
        "fitness_center" -> Icons.Default.FitnessCenter
        "sports_tennis" -> Icons.Default.SportsTennis
        "sports_badminton" -> Icons.Default.SportsTennis
        "golf_course" -> Icons.Default.GolfCourse
        "directions_run" -> Icons.Default.DirectionsRun
        "directions_walk" -> Icons.Default.DirectionsWalk
        "sports_volleyball" -> Icons.Default.SportsVolleyball
        "sports_cricket" -> Icons.Default.SportsCricket
        "skateboarding" -> Icons.Default.Skateboarding
        "skiing" -> Icons.Default.AcUnit
        "surfing" -> Icons.Default.Surfing
        // 人情分类
        "redeem" -> Icons.Default.Redeem
        "card_giftcard" -> Icons.Default.CardGiftcard
        "gift" -> Icons.Default.CardGiftcard
        "favorite" -> Icons.Default.Favorite
        "emoji_people" -> Icons.Default.EmojiPeople
        "savings" -> Icons.Default.Savings
        "volunteer_activism" -> Icons.Default.VolunteerActivism
        "handshake" -> Icons.Default.Handshake
        // 家庭分类
        "family_restroom" -> Icons.Default.FamilyRestroom
        "child_care" -> Icons.Default.ChildCare
        "elderly" -> Icons.Default.Elderly
        "shower" -> Icons.Default.Shower
        "local_laundry_service" -> Icons.Default.LocalLaundryService
        "cleaning_services" -> Icons.Default.CleaningServices
        // 学习分类
        "menu_book" -> Icons.Default.MenuBook
        "class" -> Icons.Default.Class
        "auto_stories" -> Icons.Default.AutoStories
        "palette" -> Icons.Default.Palette
        "brush" -> Icons.Default.Brush
        "architecture" -> Icons.Default.Architecture
        "newspaper" -> Icons.Default.Newspaper
        "science" -> Icons.Default.Science
        // 医疗分类
        "local_hospital" -> Icons.Default.LocalHospital
        "medication" -> Icons.Default.Medication
        "vaccines" -> Icons.Default.Vaccines
        "healing" -> Icons.Default.Healing
        "health_and_safety" -> Icons.Default.HealthAndSafety
        "bloodtype" -> Icons.Default.Bloodtype
        "monitor_heart" -> Icons.Default.MonitorHeart
        "biotech" -> Icons.Default.Biotech
        "spa" -> Icons.Default.Spa
        "dentistry" -> Icons.Default.MedicalServices
        // 理财分类
        "credit_card" -> Icons.Default.CreditCard
        "paid" -> Icons.Default.Paid
        "currency_exchange" -> Icons.Default.CurrencyExchange
        "attach_money" -> Icons.Default.AttachMoney
        "assessment" -> Icons.Default.Assessment
        "query_stats" -> Icons.Default.QueryStats
        "timeline" -> Icons.Default.Timeline
        "data_usage" -> Icons.Default.DataUsage
        // 宠物分类
        "pets" -> Icons.Default.Pets
        "eco" -> Icons.Default.Eco
        "forest" -> Icons.Default.Forest
        "yard" -> Icons.Default.Yard
        "park" -> Icons.Default.Park
        "grass" -> Icons.Default.Grass
        // 生意分类
        "business" -> Icons.Default.Business
        "work" -> Icons.Default.Work
        "equalizer" -> Icons.Default.Equalizer
        "price_change" -> Icons.Default.PriceChange
        // 其他
        "more_horiz" -> Icons.Default.MoreHoriz
        "category" -> Icons.Default.Category
        "help" -> Icons.Default.Help
        "info" -> Icons.Default.Info
        "settings" -> Icons.Default.Settings
        "build" -> Icons.Default.Build
        else -> Icons.Default.Category
    }
}

/**
 * 获取显示用的分类名称
 * 规则：
 * 1. 如果是二级分类（有parentId），显示“一级分类名称-二级分类名称”
 * 2. 如果是一级分类（无parentId），直接显示名称
 * 
 * 注意：通过 categoryId 实时查找最新的分类名称，确保分类名称修改后能实时显示
 */
fun getDisplayCategoryName(category: Category): String {
    // 通过 categoryId 和 type 实时查找最新的分类信息
    val latestCategory = Category.getCategoriesByType(category.type)
        .find { it.id == category.id } ?: category
    
    return if (latestCategory.parentId != null) {
        // 二级分类：查找父分类名称
        val parentCategory = Category.getCategoriesByType(latestCategory.type)
            .find { it.id == latestCategory.parentId }
        if (parentCategory != null) {
            "${parentCategory.name}-${latestCategory.name}"
        } else {
            // 如果找不到父分类，只显示二级分类名称
            latestCategory.name
        }
    } else {
        // 一级分类：直接显示名称
        latestCategory.name
    }
}
