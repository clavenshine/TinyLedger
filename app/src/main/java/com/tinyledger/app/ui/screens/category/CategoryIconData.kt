package com.tinyledger.app.ui.screens.category

/**
 * 图标分组数据类
 * 用于分类管理页面的图标选择器
 */
data class IconGroup(
    val name: String,
    val icons: List<String>
)

/**
 * 所有图标分组数据
 */
val iconGroups = listOf(
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
