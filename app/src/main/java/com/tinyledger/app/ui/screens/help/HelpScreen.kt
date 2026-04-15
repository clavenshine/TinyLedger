package com.tinyledger.app.ui.screens.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "使用帮助",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 基础记账
            HelpSectionHeader(title = "基础记账", icon = Icons.Default.EditNote)

            HelpFaqItem(
                question = "如何记一笔支出？",
                answer = "点击底部中间的\"+\"按钮进入记账界面，默认为支出模式。选择分类、输入金额、选择账户和日期，点击保存即可完成一笔支出记录。"
            )
            HelpFaqItem(
                question = "如何记一笔收入？",
                answer = "进入记账界面后，点击顶部\"收入\"标签切换到收入模式，选择收入分类、输入金额、选择账户和日期，点击保存即可。"
            )
            HelpFaqItem(
                question = "如何编辑或删除已记账的记录？",
                answer = "在首页或账单页面，点击任意一条记录即可进入编辑界面修改信息。在账单页面，左滑或右滑记录卡片可显示编辑和删除按钮。"
            )
            HelpFaqItem(
                question = "分类可以自定义吗？",
                answer = "可以。在记账界面的分类选择区域，点击\"新增\"按钮添加自定义分类（名称最多4个字）。长按自定义分类可重命名或删除，默认分类不可修改。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 账户管理
            HelpSectionHeader(title = "账户管理", icon = Icons.Default.AccountBalanceWallet)

            HelpFaqItem(
                question = "如何添加账户？",
                answer = "进入\"我的\"→\"资产账户\"，点击右下角的\"+\"按钮。输入账户名称、选择账户类型（银行卡、微信、支付宝、现金等）、设置期初余额，还可填写卡号后四位便于识别。"
            )
            HelpFaqItem(
                question = "账户余额是如何计算的？",
                answer = "账户余额 = 期初余额 + 该账户的所有收入 - 该账户的所有支出 + 转入金额 - 转出金额。点击账户卡片可展开查看每月的收支明细。"
            )
            HelpFaqItem(
                question = "如何编辑或删除账户？",
                answer = "在资产账户列表中，向右滑动账户卡片显示编辑按钮，向左滑动显示删除按钮。删除账户前需确认，且有10秒倒计时保护机制。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 转账与借贷
            HelpSectionHeader(title = "转账与借贷", icon = Icons.Default.SwapHoriz)

            HelpFaqItem(
                question = "如何进行转账？",
                answer = "进入记账界面，点击顶部\"转账\"标签。选择转出账户和转入账户，输入转账金额即可。\n" +
                        "转账特点：\n" +
                        "• 自动记录两笔交易：转出账户记录为负数金额，转入账户记录为正数金额\n" +
                        "• 两笔交易自动关联，可在账单中查看\n" +
                        "• 转账不会影响总资产，只是资金在账户间流动\n" +
                        "• 两个账户的余额会自动更新"
            )
            HelpFaqItem(
                question = "如何记录借贷？",
                answer = "进入记账界面，点击顶部\"借贷\"标签。根据需要选择：\n" +
                        "• 借入：他人借给我，增加负债（负债账户负数，入账账户正数）\n" +
                        "• 借出：我借给他人，增加债权（出账账户负数，债权账户正数）\n" +
                        "• 还款：我偿还借款，减少负债（出账账户负数，负债账户正数）\n" +
                        "• 收款：他人还款给我，减少债权（债权账户负数，入账账户正数）\n" +
                        "每次操作需选择对应的账户，系统会自动生成两笔关联记录，并更新账户余额。"
            )
            HelpFaqItem(
                question = "当前债务在哪里查看？",
                answer = "在首页可以查看\"当前债务\"卡片，显示总债权（别人欠我）和总负债（我欠别人）的金额。点击可进入资产账户页面查看详情。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 统计分析
            HelpSectionHeader(title = "统计分析", icon = Icons.Default.BarChart)

            HelpFaqItem(
                question = "统计页面的数据如何查看？",
                answer = "统计页面顶部显示当月的收入、支出和结余。点击\"月份\"选项卡切换查看不同月份，使用左右箭头快速前后翻月。点击\"本年累计\"选项卡可查看全年汇总数据。"
            )
            HelpFaqItem(
                question = "分类统计是什么？",
                answer = "分类统计按支出或收入分类汇总金额，以环形图和列表形式展示。可帮助您了解钱花在了哪些方面、收入来源分布如何。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 智能记账
            HelpSectionHeader(title = "智能记账", icon = Icons.Default.AutoAwesome)

            HelpFaqItem(
                question = "通知自动记账需要开启哪些权限？",
                answer = "通知自动记账需要开启以下权限：\n" +
                        "1. 通知使用权（必须）：进入\"我的\"→\"通知自动记账\"，首次使用需授权通知使用权。系统会跳转到\"通知使用权\"设置页面，在列表中找到\"小小记账本\"并开启开关。此权限允许APP读取微信、支付宝、银行等应用的通知内容。\n" +
                        "2. 通知权限（必须）：在手机系统设置中，找到\"应用管理\"→\"小小记账本\"→\"通知管理\"，确保允许APP发送通知。部分手机还需开启\"锁屏通知\"、\"悬浮通知\"等选项。\n" +
                        "3. 后台运行权限（推荐）：进入手机\"应用管理\"→\"小小记账本\"→\"电池\"，选择\"无限制\"或\"允许后台活动\"，避免系统杀掉后台服务。\n" +
                        "4. 自启动权限（推荐）：在手机\"手机管家\"或\"安全中心\"中，允许小小记账本自启动，确保重启后自动记账仍能工作。"
            )
            HelpFaqItem(
                question = "如何设置通知自动记账？",
                answer = "设置步骤：\n" +
                        "1. 进入\"我的\"页面，找到\"通知自动记账\"选项\n" +
                        "2. 点击\"去授权\"按钮，系统跳转到通知使用权设置页面\n" +
                        "3. 在列表中找到\"小小记账本\"，开启开关\n" +
                        "4. 返回APP，开启\"通知自动记账\"开关\n" +
                        "5. 授权后，APP将自动监听微信、支付宝的收款通知以及银行短信通知，自动识别金额并记账\n" +
                        "注意：开启后需要在微信、支付宝、银行APP的设置中确保开启收款通知功能。"
            )
            HelpFaqItem(
                question = "什么是无感自动记账？",
                answer = "无感自动记账是通知自动记账的高级模式。开启后，当APP捕获到交易通知时，会直接自动完成记账，无需手动确认。未开启时，捕获交易后会弹出确认通知，您需点击确认才会记账。\n" +
                        "开启方法：先开启\"通知自动记账\"，然后开启下方的\"无感自动记账\"开关。\n" +
                        "建议：新手建议先使用普通模式（需确认），熟悉后再开启无感模式。"
            )
            HelpFaqItem(
                question = "声音和震动提醒有什么用？",
                answer = "声音和震动提醒是自动记账的反馈机制。当自动记账成功时：\n" +
                        "• 声音提醒：播放提示音，让您知道已完成一笔记账\n" +
                        "• 震动提醒：手机震动，在不方便听声音时也能感知\n" +
                        "这两个功能默认关闭，可在\"我的\"→\"通知自动记账\"设置页面中开启。建议在使用无感自动记账时开启，以便及时了解记账状态。"
            )
            HelpFaqItem(
                question = "通知自动记账支持哪些应用？",
                answer = "目前支持：\n" +
                        "• 微信支付：收款码收款、转账到账等通知\n" +
                        "• 支付宝：收款码收款、转账到账等通知\n" +
                        "• 银行短信：各大银行（建行、工行、农行、中行、招行、交行、邮储、中信等）的账户变动短信通知\n" +
                        "APP会自动识别通知中的金额、收支类型，并自动分类记账。"
            )
            HelpFaqItem(
                question = "为什么通知自动记账不工作？",
                answer = "常见原因及解决方法：\n" +
                        "1. 未开启通知使用权：进入\"我的\"→\"通知自动记账\"，查看是否显示\"已授权\"，如未授权请点击\"去授权\"\n" +
                        "2. 通知自动记账开关未开启：确保\"通知自动记账\"开关为开启状态\n" +
                        "3. 微信/支付宝未开启通知：在手机系统设置中，确保微信、支付宝的通知权限已开启\n" +
                        "4. 后台被系统杀掉：设置\"允许后台运行\"和\"电池无限制\"，关闭电池优化\n" +
                        "5. 通知内容不完整：部分手机默认隐藏通知详细内容，需在系统通知设置中开启\"显示通知内容\"\n" +
                        "6. 重启手机后未自动启动：设置\"自启动权限\"，或手动打开一次APP"
            )
            HelpFaqItem(
                question = "截屏记账如何使用？",
                answer = "进入\"我的\"→\"截屏记账\"，选择支付截图（微信/支付宝的收款码截图），APP会自动识别截图中的金额信息，确认后即可快速记账。支持从相册选择或拍照。\n" +
                        "使用方法：\n" +
                        "1. 在微信/支付宝中打开收款码页面并截图\n" +
                        "2. 进入\"截屏记账\"功能\n" +
                        "3. 选择截图或拍照\n" +
                        "4. APP自动识别金额，确认后完成记账"
            )
            HelpFaqItem(
                question = "如何导入短信记录？",
                answer = "进入\"我的\"→\"导入短信收支记录\"，需要授予\"读取短信\"权限。APP会自动扫描手机中的银行短信，识别收支信息，您可以选择性导入。\n" +
                        "权限说明：\n" +
                        "• 仅读取银行相关短信，不会读取其他隐私短信\n" +
                        "• 首次使用需授权\"读取短信\"权限，系统会弹出权限请求对话框\n" +
                        "• 可在手机\"应用管理\"→\"小小记账本\"→\"权限管理\"中查看和管理短信权限\n" +
                        "支持银行：建行、工行、农行、中行、招行、交行、邮储、中信等各大银行。"
            )
            HelpFaqItem(
                question = "如何导入微信/支付宝账单？",
                answer = "在\"我的\"页面找到\"导入微信账单\"或\"导入支付宝账单\"，选择导出的CSV或xlsx账单文件即可批量导入历史记录。\n" +
                        "导出方法：\n" +
                        "• 微信：我→服务→钱包→账单→常见问题→下载账单→用于个人对账→选择时间范围→发送到邮箱\n" +
                        "• 支付宝：我的→账单→点击右上角\"...\"→开具交易流水证明→用于个人对账→选择时间范围→发送到邮箱\n" +
                        "收到邮件后，下载文件到手机，然后在APP中选择该文件导入。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 账单与搜索
            HelpSectionHeader(title = "账单与搜索", icon = Icons.Default.Receipt)

            HelpFaqItem(
                question = "账单页面如何筛选记录？",
                answer = "在账单页面，可以按收支类型（全部/支出/收入）筛选，也可以按月份查看。点击搜索图标可进入搜索页面，按关键词搜索交易记录。"
            )
            HelpFaqItem(
                question = "如何搜索交易记录？",
                answer = "在账单页面点击右上角搜索图标，输入关键词可搜索分类名称、备注等。还支持按金额范围筛选。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 预算管理
            HelpSectionHeader(title = "预算管理", icon = Icons.Default.Savings)

            HelpFaqItem(
                question = "如何设置预算？",
                answer = "进入\"我的\"→\"预算管理\"，可设置月度或年度支出预算上限。当支出接近或超过预算时，APP会进行提醒。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 外观与设置
            HelpSectionHeader(title = "外观与设置", icon = Icons.Default.Palette)

            HelpFaqItem(
                question = "如何更换主题颜色？",
                answer = "进入\"我的\"→\"主题颜色\"，提供30+种主题配色，包括经典、系统品牌、商务、生活、极简、渐变等多种风格，选择喜欢的颜色即可实时切换。"
            )
            HelpFaqItem(
                question = "如何更换货币符号？",
                answer = "进入\"我的\"→\"货币符号\"，可选择常用货币符号（人民币¥、美元$、欧元€）等等，或自定义输入最多3个字符的货币符号。"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun HelpFaqItem(
    question: String,
    answer: String
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Question row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .wrapContentSize(Alignment.Center)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Answer
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .wrapContentSize(Alignment.Center)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
