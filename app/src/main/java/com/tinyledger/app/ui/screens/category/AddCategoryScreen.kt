package com.tinyledger.app.ui.screens.category

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinyledger.app.domain.model.Category
import com.tinyledger.app.domain.model.TransactionType
import com.tinyledger.app.ui.components.getCategoryIcon

// 图标分组数据
private data class IconGroup(
    val name: String,
    val icons: List<String>
)

// 按截图的分类图标分组
private val iconGroups = listOf(
    IconGroup("吃喝", listOf(
        "restaurant", "ramen_dining", "local_cafe", "local_bar", "lunch_dining",
        "dinner_dining", "kebab_dining", "bakery_dining", "local_pizza", "icecream",
        "cake", "hamburger", "flatware", "local_drink", "tapas", "wine_bar"
    )),
    IconGroup("购物", listOf(
        "shopping_bag", "shopping_cart", "local_mall", "checkroom", "store",
        "diamond", "camera_alt", "smartphone", "content_cut", "local_offer"
    )),
    IconGroup("交通", listOf(
        "directions_car", "directions_bike", "flight", "train", "ev_station",
        "local_parking", "local_gas_station", "directions_boat", "two_wheeler",
        "directions_railway", "directions_bus", "pedal_bike"
    )),
    IconGroup("住房", listOf(
        "home", "home_work", "bolt", "local_fire_department", "water_drop",
        "wifi", "bed", "hotel", "ac_unit", "phone_android", "kitchen", "tv"
    )),
    IconGroup("娱乐", listOf(
        "sports_esports", "videogame_asset", "theaters", "movie", "local_movies",
        "music_note", "sports_soccer", "sports_basketball", "beach_access", "pool",
        "celebration", "festival", "casino"
    )),
    IconGroup("运动", listOf(
        "fitness_center", "sports_basketball", "sports_soccer", "sports_tennis",
        "sports_badminton", "golf_course", "directions_run", "directions_walk",
        "sports_volleyball", "sports_cricket", "skateboarding", "skiing", "surfing"
    )),
    IconGroup("人情", listOf(
        "redeem", "card_giftcard", "gift", "favorite", "emoji_people",
        "savings", "volunteer_activism", "handshake"
    )),
    IconGroup("家庭", listOf(
        "family_restroom", "child_care", "elderly", "shower", "tv",
        "ac_unit", "camera_alt", "local_laundry_service", "cleaning_services"
    )),
    IconGroup("学习", listOf(
        "school", "menu_book", "class", "work", "auto_stories",
        "palette", "brush", "architecture", "newspaper", "science"
    )),
    IconGroup("医疗", listOf(
        "local_hospital", "medical", "medication", "vaccines", "healing",
        "health_and_safety", "bloodtype", "monitor_heart", "biotech",
        "spa", "dentistry"
    )),
    IconGroup("理财", listOf(
        "account_balance", "trending_up", "trending_down", "credit_card", "paid",
        "currency_exchange", "savings", "attach_money", "assessment",
        "query_stats", "timeline", "data_usage"
    )),
    IconGroup("宠物", listOf(
        "pets", "eco", "forest", "yard", "park", "grass"
    )),
    IconGroup("生意", listOf(
        "business", "work", "store", "equalizer", "price_change"
    )),
    IconGroup("其他", listOf(
        "more_horiz", "category", "help", "info", "settings", "build"
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    transactionType: TransactionType,
    onNavigateBack: () -> Unit = {}
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    var selectedGroupIndex by remember { mutableStateOf(0) }
    val maxNameLength = 4  // 最多4个汉字

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "新建一级分类",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        if (categoryName.trim().isBlank()) return@Button
                        if (selectedIcon == null) return@Button

                        // 检查分类名称是否已存在
                        val existingCategories = Category.getCategoriesByType(transactionType)
                        val isDuplicate = existingCategories.any {
                            it.name.equals(categoryName.trim(), ignoreCase = true)
                        }
                        if (isDuplicate) return@Button

                        Category.addCustomCategory(
                            name = categoryName.trim(),
                            type = transactionType,
                            icon = selectedIcon!!
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    enabled = categoryName.trim().isNotBlank() && selectedIcon != null,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // 分类名称输入
            Text(
                "分类名称",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = categoryName,
                onValueChange = {
                    if (it.length <= maxNameLength) {
                        categoryName = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("请输入分类名称") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    Text(
                        text = "${categoryName.length}/$maxNameLength",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (categoryName.length > maxNameLength)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 分类图标标题
            Text(
                "分类图标",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )

            // 左右布局：左侧分类导航 + 右侧图标网格
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp, max = 450.dp)
                ) {
                    // 左侧分类导航
                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .verticalScroll(rememberScrollState())
                    ) {
                        iconGroups.forEachIndexed { index, group ->
                            val isSelected = selectedGroupIndex == index
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGroupIndex = index }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 右侧图标网格
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val currentGroup = iconGroups[selectedGroupIndex]
                        // 当前分类标题
                        Text(
                            text = currentGroup.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 图标网格（每行5个）
                        currentGroup.icons.chunked(5).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowIcons.forEach { icon ->
                                    val isSelected = selectedIcon == icon
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable { selectedIcon = icon },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(icon),
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                // 填充空位
                                repeat(5 - rowIcons.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
