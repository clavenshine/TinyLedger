package com.tinyledger.app.ui.components

import androidx.compose.ui.graphics.Color
import com.tinyledger.app.R

/**
 * 银行信息数据类
 */
data class BankInfo(
    val displayName: String,        // 银行全称，如 "中国工商银行"
    val shortName: String,          // 简称，如 "工商"
    val brandColor: Color,          // 品牌色
    val iconResId: Int? = null      // 图标资源ID（预留，未来可替换为真实Logo图片）
)

/**
 * 国内主要银行 Logo 映射表
 * 根据账户名称中的关键词自动匹配对应银行的品牌色和简称
 *
 * 匹配优先级：精确匹配 > 模糊匹配 > 默认
 * 如果没有匹配到任何银行，返回 null（使用默认图标）
 */

// 品牌色定义（尽量接近各银行官方品牌色）
val ICBC_RED = Color(0xFFC7000B)           // 工商银行 红
val CCB_BLUE = Color(0xFF0066B3)           // 建设银行 蓝
val ABC_GREEN = Color(0xFF00903E)          // 农业银行 绿
val BOC_RED = Color(0xFFBF0029)            // 中国银行 红
val BCOM_BLUE = Color(0xFF003D7A)          // 交通银行 蓝
val CMB_RED = Color(0xFFD4233A)            // 招商银行 红
val PSBC_GREEN = Color(0xFF00783A)         // 邮储银行 绿
val CMBC_GREEN = Color(0xFF1FA25F)         // 民生银行 绿
val CIB_BLUE = Color(0xFF1060AD)           // 兴业银行 蓝
val CITIC_RED = Color(0xFFCE1126)          // 中信银行 红
val CEB_PURPLE = Color(0xFF501E73)         // 光大银行 紫
val PAB_ORANGE = Color(0xFFFF6600)         // 平安银行 橙
val SPDB_BLUE = Color(0xFF000E80)          // 浦发银行 蓝
val HXB_PURPLE = Color(0xFFC70048)         // 华夏银行 紫/红
val CGB_RED = Color(0xFFDA2812)            // 广发银行 红
val BOB_RED = Color(0xFFCC0000)            // 北京银行 红
val SHB_BLUE = Color(0xFF004EA2)          // 上海银行 蓝
val HZBANK_GREEN = Color(0xFF00795D)       // 杭州银行 绿
val NINGBO_RED = Color(0xFFC40016)         // 宁波银行 红
val NJCB_RED = Color(0xFFDE2910)           // 南京银行 红
val CQRCB_BLUE = Color(0xFF005BAB)         // 重庆银行 蓝
val SDB_BLUE = Color(0xFF00539B)           // 济南银行 蓝
val CBQRC_BLUE = Color(0xFF01438A)         // 渤海银行 蓝
val BANK_DEFAULT_GRAY = Color(0xFF607D8B)  // 默认灰色

/**
 * 银行映射列表（按匹配优先级排列）
 */
val BANK_LIST = listOf(
    BankInfo("中国工商银行", "工", ICBC_RED),
    BankInfo("工商银行", "工", ICBC_RED),
    BankInfo("工商", "工", ICBC_RED),

    BankInfo("中国建设银行", "建", CCB_BLUE),
    BankInfo("建设银行", "建", CCB_BLUE),
    BankInfo("建设", "建", CCB_BLUE),

    BankInfo("中国农业银行", "农", ABC_GREEN),
    BankInfo("农业银行", "农", ABC_GREEN),
    BankInfo("农业", "农", ABC_GREEN),

    BankInfo("中国银行", "中", BOC_RED),  // 注意：需要配合"银行"关键字

    BankInfo("交通银行", "交", BCOM_BLUE),
    BankInfo("交通", "交", BCOM_BLUE),

    BankInfo("招商银行", "招", CMB_RED),
    BankInfo("招商", "招", CMB_RED),

    BankInfo("邮储银行", "邮", PSBC_GREEN),
    BankInfo("邮政储蓄银行", "邮", PSBC_GREEN),
    BankInfo("邮政", "邮", PSBC_GREEN),

    BankInfo("民生银行", "民", CMBC_GREEN),
    BankInfo("民生", "民", CMBC_GREEN),

    BankInfo("兴业银行", "兴", CIB_BLUE),
    BankInfo("兴业", "兴", CIB_BLUE),

    BankInfo("中信银行", "信", CITIC_RED),
    BankInfo("中信", "信", CITIC_RED),

    BankInfo("光大银行", "光", CEB_PURPLE),
    BankInfo("光大", "光", CEB_PURPLE),

    BankInfo("平安银行", "平", PAB_ORANGE),
    BankInfo("平安", "平", PAB_ORANGE),

    BankInfo("浦发银行", "浦", SPDB_BLUE),
    BankInfo("浦发", "浦", SPDB_BLUE),

    BankInfo("华夏银行", "华", HXB_PURPLE),
    BankInfo("华夏", "华", HXB_PURPLE),

    BankInfo("广发银行", "广", CGB_RED),
    BankInfo("广发", "广", CGB_RED),

    BankInfo("北京银行", "京", BOB_RED),
    BankInfo("上海银行", "海", SHB_BLUE),
    BankInfo("杭州银行", "杭", HZBANK_GREEN),
    BankInfo("宁波银行", "宁", NINGBO_RED),
    BankInfo("南京银行", "南", NJCB_RED),
    BankInfo("重庆银行", "重", CQRCB_BLUE),
    BankInfo("江苏银行", "苏", Color(0xFF0072BC)),
    BankInfo("浙商银行", "浙", Color(0xFFC7000B)),
    BankInfo("徽商银行", "徽", Color(0xFFBE002F)),
    BankInfo("青岛银行", "青", Color(0xFF0066B3)),
    BankInfo("郑州银行", "郑", Color(0xFFCF102D)),
    BankInfo("成都银行", "成", Color(0xFFE60012)),
    BankInfo("西安银行", "西", Color(0xFFC7000B)),
    BankInfo("长沙银行", "长", Color(0xFF005BAB)),
    BankInfo("厦门国际银行", "厦", Color(0xFF00539B)),
    BankInfo("渤海银行", "渤", CBQRC_BLUE),
    BankInfo("恒丰银行", "恒", Color(0xFFBE002F)),
    BankInfo("晋商银行", "晋", Color(0xFFCD1227)),
    BankInfo("盛京银行", "盛", Color(0xFFC7000B)),
    BankInfo("锦州银行", "锦", Color(0xFFC41D24)),
    BankInfo("吉林银行", "吉", Color(0xFFE60012)),
    BankInfo("哈尔滨银行", "哈", Color(0xFF0066B3)),
    BankInfo("乌鲁木齐银行", "乌", Color(0xFFC7000B)),
    BankInfo("河北银行", "冀", Color(0xFFBE002F)),
    BankInfo("天津银行", "津", Color(0xFF004098)),
    BankInfo("甘肃银行", "甘", Color(0xFFC7000B)),
    BankInfo("青海银行", "青", Color(0xFF00539B)),
    BankInfo("西安银行", "西", Color(0xFFC7000B)),
    BankInfo("富滇银行", "富", Color(0xFFC7000B)),
    BankInfo("贵州银行", "贵", Color(0xFF00539B)),
    BankInfo("广西北部湾银行", "桂", Color(0xFFC7000B)),
    BankInfo("江西银行", "赣", Color(0xFFC7000B)),
    BankInfo("泉州银行", "泉", Color(0xFF0066B3)),
    BankInfo("温州银行", "温", Color(0xFF0066B3)),
    BankInfo("苏州银行", "苏", Color(0xFF0066B3)),
    BankInfo("东莞银行", "莞", Color(0xFFC7000B)),
    BankInfo("珠海华润银行", "珠", Color(0xFFC7000B)),
    BankInfo("广东南粤银行", "粤", Color(0xFF00539B)),
    BankInfo("华融湘江银行", "华", Color(0xFFBE002F)),
    BankInfo("河北银行", "河", Color(0xFFBE002F)),
    BankInfo("内蒙古银行", "蒙", Color(0xFFC7000B)),
    BankInfo("汉口银行", "汉", Color(0xFF0066B3)),
    BankInfo("柳州银行", "柳", Color(0xFF00539B)),
    BankInfo("兰州银行", "兰", Color(0xFF00539B)),
    BankInfo("齐鲁银行", "齐", Color(0xFF00539B)),

    // 通用银行匹配（兜底）
    BankInfo("银行", "", BANK_DEFAULT_GRAY)
)

/**
 * 根据账户名称解析对应的银行信息
 *
 * @param accountName 账户名称（如 "工商银行储蓄卡"、"建行信用卡"等）
 * @return 匹配到的 BankInfo，如果未匹配则返回 null
 */
fun resolveBankLogo(accountName: String): BankInfo? {
    if (accountName.isBlank()) return null

    // 先尝试精确/模糊匹配
    for (bank in BANK_LIST) {
        if (accountName.contains(bank.displayName)) {
            return bank
        }
    }

    // 兜底：包含"银行"二字但没匹配到具体银行的
    if (accountName.contains("银行")) {
        return BankInfo(
            displayName = accountName,
            shortName = accountName.first().toString(),
            brandColor = BANK_DEFAULT_GRAY
        )
    }

    return null
}
