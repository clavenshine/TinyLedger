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
                answer = "进入记账界面，点击顶部\"转账\"标签。选择转出账户和转入账户，输入转账金额即可。转账不会影响总资产，只是资金在账户间流动。"
            )
            HelpFaqItem(
                question = "如何记录借贷？",
                answer = "进入记账界面，点击顶部\"借贷\"标签。根据需要选择：借入（他人借给我，增加负债）、借出（我借给他人，增加债权）、还款（我偿还借款，减少负债）、收款（他人还款给我，减少债权）。每次操作需选择对应的账户。"
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
                question = "通知自动记账如何使用？",
                answer = "进入\"我的\"→\"通知自动记账\"，首次使用需授权通知使用权。授权后开启开关，APP将自动监听微信、支付宝的收款通知并识别金额记账。您还可以在设置中开启声音和震动提醒。"
            )
            HelpFaqItem(
                question = "截屏记账如何使用？",
                answer = "进入\"我的\"→\"截屏记账\"，选择支付截图（微信/支付宝的收款码截图），APP会自动识别截图中的金额信息，确认后即可快速记账。"
            )
            HelpFaqItem(
                question = "如何导入短信记录？",
                answer = "进入\"我的\"→\"导入短信收支记录\"，APP会自动扫描手机中的银行短信，识别收支信息，您可以选择性导入。"
            )
            HelpFaqItem(
                question = "如何导入微信/支付宝账单？",
                answer = "在\"我的\"页面找到\"导入微信账单\"或\"导入支付宝账单\"，选择导出的CSV或xlsx账单文件即可批量导入历史记录。"
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
