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
            // 快速入门
            HelpSectionHeader(title = "快速入门", icon = Icons.Default.Apps)

            HelpFaqItem(
                question = "小小记账本有哪些页面？",
                answer = "底部导航栏共有4个主页面和1个中心按钮：\n" +
                        "• 首页：查看本月收支概览、今日账单、待确认账单和快捷操作\n" +
                        "• 账单：查看所有交易记录，支持流水、日历、相册三种查看模式\n" +
                        "• 统计：查看收支分类统计和趋势分析\n" +
                        "• 我的：管理账户、预算、数据导入、外观设置等\n" +
                        "• 中间 \"+\" 按钮：快速记一笔（支出/收入/转账/借贷）"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 基础记账
            HelpSectionHeader(title = "基础记账", icon = Icons.Default.EditNote)

            HelpFaqItem(
                question = "如何记一笔支出？",
                answer = "点击底部中间的\"+\"按钮进入记账界面，默认为支出模式。选择分类、输入金额、选择账户和日期，点击保存即可完成一笔支出记录。保存成功后会播放\"咻\"的提示音（需在设置中开启声音）。\n" +
                        "\n日期规则：系统会检测所选日期，如果选择了未来日期（在当前日期之后），会弹出确认提示。您可以点击\"确认\"继续保存，或点击\"返回\"修改日期。\n" +
                        "\n金额规则：在记账界面输入的金额始终为正数，系统根据交易类型自动判断正负号：\n" +
                        "• 支出交易：系统自动记为负数（资金流出）\n" +
                        "• 收入交易：系统自动记为正数（资金流入）\n" +
                        "账单页面中，支出显示为负数（如 -¥100.00），收入显示为正数（如 +¥100.00）。"
            )
            HelpFaqItem(
                question = "如何记一笔收入？",
                answer = "进入记账界面后，点击顶部\"收入\"标签切换到收入模式，选择收入分类、输入金额、选择账户和日期，点击保存即可。"
            )
            HelpFaqItem(
                question = "如何添加图片附件？",
                answer = "在记账界面，点击\"添加图片\"按钮，可选择拍照或从相册选择。\n" +
                        "• 普通交易最多添加3张图片，借贷交易最多添加9张\n" +
                        "• 从相册选择支持多选，可一次性选择多张图片\n" +
                        "• 点击缩略图右上角的\"×\"可删除该图片\n" +
                        "• 图片会永久保存并与该笔交易记录绑定\n" +
                        "• 编辑已保存的交易时，可查看、删除或继续添加图片\n\n" +
                        "图片查看：在账单页或交易详情页点击图片，会以全屏模式查看，带白色柔和边框和弹性动画效果，点击任意位置可关闭。"
            )
            HelpFaqItem(
                question = "如何编辑或删除已记账的记录？",
                answer = "有多种方式可以编辑或删除记录：\n" +
                        "• 首页：点击今日账单中的任意记录进入交易详情页进行编辑\n" +
                        "• 账单-流水模式：点击记录进入详情页；向右滑动显示删除按钮，向左滑动显示编辑按钮\n" +
                        "• 详情页底部有编辑和删除操作按钮\n" +
                        "注意：删除操作不可撤销，删除前系统会弹出确认对话框。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 分类管理
            HelpSectionHeader(title = "分类管理", icon = Icons.Default.Category)

            HelpFaqItem(
                question = "如何管理分类？",
                answer = "在记账界面的分类选择区域，点击右上角的\"管理\"图标进入分类管理页面。\n" +
                        "分类管理页面功能：\n" +
                        "• 顶部可切换\"支出\"和\"收入\"分类\n" +
                        "• 点击\"新增分类\"按钮添加自定义分类（名称最多4个字）\n" +
                        "• 支持一级分类和二级分类，一级分类右上角显示三个点图标表示有子分类\n" +
                        "• 长按自定义分类可重命名或删除，默认分类不可修改\n" +
                        "• 点击有子分类的分类可展开查看二级分类\n\n" +
                        "分类名称规则：\n" +
                        "• 新增或修改分类时，系统会自动检测名称是否已存在（包括所有一级和二级分类）\n" +
                        "• 如果名称重复，会弹出提示并阻止保存\n" +
                        "• 删除二级分类时，系统会自动将该分类下的交易记录迁移到父级分类下的第一个二级分类，如果没有其他二级分类则迁移到父级分类本身"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 账户管理
            HelpSectionHeader(title = "账户管理", icon = Icons.Default.AccountBalanceWallet)

            HelpFaqItem(
                question = "如何添加账户？",
                answer = "进入\"我的\"→\"全部账户\"，点击右下角的\"+\"按钮进入新建账户页面。\n" +
                        "新建账户时需要填写：\n" +
                        "• 账户类别：现金账户、信用账户、外部往来（顶部胶囊按钮切换）\n" +
                        "• 账户类型：银行卡、微信、支付宝、现金、余额宝、信用卡、消费平台、外部个人往来、外部贷款负债等\n" +
                        "• 账户名称和期初余额\n" +
                        "• 期初余额日期\n" +
                        "• 卡号后四位（可选，便于识别）\n" +
                        "• 信用账户还可设置信用额度、账单日和还款日\n\n" +
                        "智能导航：在\"全部账户\"页面，先点击对应的账户类别标签（现金账户/信用账户/外部往来），再点击\"新建账户\"，系统会自动选中对应的账户类别，方便快速创建。\n\n" +
                        "账户唯一性：系统会检测账户类别+账户名称+账号后4位是否重复，如果已存在相同账户会弹出提示并阻止保存。"
            )
            HelpFaqItem(
                question = "账户余额是如何计算的？",
                answer = "账户余额 = 期初余额 + 该账户的所有收入 - 该账户的所有支出 + 转入金额 - 转出金额。点击账户卡片可进入账户详情页，查看每月的收支明细。"
            )
            HelpFaqItem(
                question = "如何编辑或删除账户？",
                answer = "在全部账户列表中，向右滑动账户卡片显示编辑按钮，向左滑动显示删除按钮。删除账户前需确认，且有10秒倒计时保护机制。"
            )
            HelpFaqItem(
                question = "什么是信用账户？",
                answer = "信用账户用于管理信用卡、花呗、白条等信用消费。创建时可设置信用额度、账单日和还款日。首页会显示信用账户的可用额度（额度+余额），方便掌握消费空间。\n" +
                        "信用账户支持信用卡还款功能：在信用账户详情页点击\"还款\"按钮，即可快速记录信用卡还款。"
            )
            HelpFaqItem(
                question = "什么是外部往来账户？",
                answer = "外部往来账户用于记录与他人的借贷关系，分为两种类型：\n" +
                        "• 外部个人往来：记录与个人之间的借入借出\n" +
                        "• 外部贷款负债：记录银行贷款等外部贷款\n" +
                        "首页可查看\"外部往来\"卡片，显示所有外部往来账户的总余额。正数表示别人欠我（债权），负数表示我欠别人（负债）。"

            )

            Spacer(modifier = Modifier.height(8.dp))

            // 转账与借贷
            HelpSectionHeader(title = "转账与借贷", icon = Icons.Default.SwapHoriz)

            HelpFaqItem(
                question = "如何进行转账？",
                answer = "进入记账界面，点击顶部\"转账\"标签。选择转出账户和转入账户，输入转账金额即可。\n" +
                        "转账特点：\n" +
                        "• 系统自动生成两笔交易记录：转出账户记为负数金额（资金流出），转入账户记为正数金额（资金流入）\n" +
                        "• 两笔交易自动关联，编辑任一笔时另一笔同步更新\n" +
                        "• 转账不会影响总资产，只是资金在账户间流动\n" +
                        "• 两个账户的余额会自动更新\n" +
                        "\n注意：编辑已有记录时，从支出/收入切换为转账，系统会自动删除原记录并生成一正一负两笔新记录。"
            )
            HelpFaqItem(
                question = "如何记录借贷？",
                answer = "进入记账界面，点击顶部\"借贷\"标签。根据需要选择：\n" +
                        "• 借入：他人借给我，增加负债（负债账户负数，入账账户正数）\n" +
                        "• 借出：我借给他人，增加债权（出账账户负数，债权账户正数）\n" +
                        "• 还款：我偿还借款，减少负债（出账账户负数，负债账户正数）\n" +
                        "• 收款：他人还款给我，减少债权（债权账户负数，入账账户正数）\n" +
                        "每次操作需选择对应的账户，系统会自动生成两笔关联记录，并更新账户余额。\n" +
                        "借贷交易最多可添加9张图片附件。"
            )
            HelpFaqItem(
                question = "当前债务在哪里查看？",
                answer = "在首页可以查看\"外部往来\"卡片，显示所有外部往来账户的总余额。正数表示总债权（别人欠我），负数表示总负债（我欠别人）。点击可进入外部往来账户页面查看各笔往来的详情。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 账单与搜索
            HelpSectionHeader(title = "账单与搜索", icon = Icons.Default.Receipt)

            HelpFaqItem(
                question = "账单页面有哪些查看模式？",
                answer = "账单页面顶部有三个模式标签：\n\n" +
                        "1. 流水模式：\n" +
                        "   • 按日期分组展示所有交易记录\n" +
                        "   • 顶部显示当月结余、支出、收入汇总\n" +
                        "   • 支持\"全部/支出/收入\"筛选标签\n" +
                        "   • 内置搜索框，可按关键词快速过滤\n" +
                        "   • 向右滑动记录显示删除按钮，向左滑动显示编辑按钮\n\n" +
                        "2. 日历模式：\n" +
                        "   • 日历网格展示，有交易的日期显示当日净金额（绿色正/红色负）\n" +
                        "   • 今日高亮标记\n" +
                        "   • 点击日期可查看该日交易列表\n" +
                        "   • 支持左右滑动切换月份\n\n" +
                        "3. 相册模式：\n" +
                        "   • 以网格展示带图片附件的交易记录\n" +
                        "   • 支持按日期范围和收支类型筛选\n" +
                        "   • 点击可进入交易详情页\n\n" +
                        "账单页交易明细分类名称显示规则：\n" +
                        "• 如果一级分类下有二级分类，显示\"一级分类名称-二级分类名称\"\n" +
                        "• 如果一级分类下没有二级分类，只显示\"一级分类名称\"\n" +
                        "• 修改分类名称后，所有交易明细的分类名称会实时更新"
            )
            HelpFaqItem(
                question = "如何搜索交易记录？",
                answer = "有两种搜索方式：\n" +
                        "1. 流水模式搜索：在账单-流水页面的\"全部/支出/收入\"筛选标签旁边，点击搜索框直接输入关键词，实时过滤当前月份的记录。\n" +
                        "2. 全局搜索：在账单-流水模式中，点击顶部右侧的搜索图标进入搜索页面，可按关键词搜索所有交易记录。"
            )
            HelpFaqItem(
                question = "账单页面的结余是如何计算的？",
                answer = "账单页面的结余 = 收入金额 + 支出金额。\n" +
                        "其中：\n" +
                        "• 收入金额 = 所有资金流入明细账单金额之和（正数）\n" +
                        "• 支出金额 = 所有资金流出明细账单金额之和（负数）\n" +
                        "• 结余 = 收入金额 + 支出金额（正负相加即为净收入）\n" +
                        "\n例如：收入 5000 元，支出 3000 元，则结余 = 5000 + (-3000) = 2000 元。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 统计分析
            HelpSectionHeader(title = "统计分析", icon = Icons.Default.BarChart)

            HelpFaqItem(
                question = "统计页面有哪些数据？",
                answer = "统计页面提供以下数据：\n" +
                        "• 月份切换：点击\"月份\"选项卡，使用左右箭头快速前后翻月\n" +
                        "• 本年累计：点击\"本年累计\"选项卡查看全年汇总数据\n" +
                        "• 顶部概览：当月的收入、支出和结余\n" +
                        "• 分类统计：按支出或收入分类汇总金额，以环形图和列表形式展示\n" +
                        "可帮助您了解钱花在了哪些方面、收入来源分布如何。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 预算管理
            HelpSectionHeader(title = "预算管理", icon = Icons.Default.Savings)

            HelpFaqItem(
                question = "如何设置预算？",
                answer = "进入\"我的\"→\"预算管理\"，可设置月度或年度支出预算上限。当支出接近或超过预算时，APP会进行提醒，帮助您控制消费。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 智能记账
            HelpSectionHeader(title = "智能记账", icon = Icons.Default.AutoAwesome)

            HelpFaqItem(
                question = "什么是通知自动记账？",
                answer = "通知自动记账通过监听手机通知，自动识别微信、支付宝的收款通知以及银行短信通知，自动识别金额并记账。\n\n" +
                        "设置步骤：\n" +
                        "1. 进入\"我的\"页面，找到\"自动记账\"选项\n" +
                        "2. 点击\"去授权\"按钮，系统跳转到通知使用权设置页面\n" +
                        "3. 在列表中找到\"小小记账本\"，开启开关\n" +
                        "4. 返回APP，开启\"通知自动记账\"开关\n" +
                        "5. 可选择开启\"无感自动记账\"（直接自动记账，无需确认）"
            )
            HelpFaqItem(
                question = "通知自动记账需要开启哪些权限？",
                answer = "通知自动记账需要开启以下权限：\n" +
                        "1. 通知使用权（必须）：进入\"我的\"→\"自动记账\"，首次使用需授权。系统会跳转到\"通知使用权\"设置页面，在列表中找到\"小小记账本\"并开启。此权限允许APP读取微信、支付宝、银行等应用的通知内容。\n" +
                        "2. 通知权限（必须）：在手机系统设置中，找到\"应用管理\"→\"小小记账本\"→\"通知管理\"，确保允许APP发送通知。部分手机还需开启\"锁屏通知\"、\"悬浮通知\"等选项。\n" +
                        "3. 后台运行权限（推荐）：进入手机\"应用管理\"→\"小小记账本\"→\"电池\"，选择\"无限制\"或\"允许后台活动\"，避免系统杀掉后台服务。\n" +
                        "4. 自启动权限（推荐）：在手机\"手机管家\"或\"安全中心\"中，允许小小记账本自启动，确保重启后自动记账仍能工作。"
            )
            HelpFaqItem(
                question = "什么是无感自动记账？",
                answer = "无感自动记账是通知自动记账的高级模式。开启后，当APP捕获到交易通知时，会直接自动完成记账，无需手动确认。未开启时，捕获交易后会弹出确认通知，您需点击确认才会记账。\n" +
                        "开启方法：先开启\"通知自动记账\"，然后开启\"无感自动记账\"开关。\n" +
                        "建议：新手建议先使用普通模式（需确认），熟悉后再开启无感模式。"
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
                        "1. 未开启通知使用权：进入\"我的\"→\"自动记账\"，查看是否显示\"已授权\"，如未授权请点击\"去授权\"\n" +
                        "2. 通知自动记账开关未开启：确保开关为开启状态\n" +
                        "3. 微信/支付宝未开启通知：在手机系统设置中，确保微信、支付宝的通知权限已开启\n" +
                        "4. 后台被系统杀掉：设置\"允许后台运行\"和\"电池无限制\"，关闭电池优化\n" +
                        "5. 通知内容不完整：部分手机默认隐藏通知详细内容，需在系统通知设置中开启\"显示通知内容\"\n" +
                        "6. 重启手机后未自动启动：设置\"自启动权限\"，或手动打开一次APP"
            )
            HelpFaqItem(
                question = "声音和震动提醒有什么用？",
                answer = "声音和震动提醒是记账操作的反馈机制：\n" +
                        "• 自动记账成功时：播放\"咻\"的提示音和/或震动，让您知道已完成一笔记账\n" +
                        "• 手动记账保存成功时：如果开启了声音，也会播放\"咻\"的提示音\n" +
                        "• 新建账户保存成功时：同样会播放提示音\n" +
                        "这两个功能默认关闭，可在\"我的\"页面中直接通过开关开启。建议在使用无感自动记账时开启，以便及时了解记账状态。"
            )
            HelpFaqItem(
                question = "截屏数据导入如何使用？",
                answer = "进入\"我的\"→\"截屏数据导入\"，选择支付截图，APP会自动识别截图中的金额信息。\n" +
                        "使用方法：\n" +
                        "1. 在微信/支付宝中打开收款码页面并截图\n" +
                        "2. 进入\"截屏数据导入\"功能\n" +
                        "3. 选择单张截图或批量选择多张截图\n" +
                        "4. APP自动识别金额，自动填充分类和账户信息\n" +
                        "5. 确认后可逐条保存或批量保存"
            )
            HelpFaqItem(
                question = "如何导入短信收支记录？",
                answer = "进入\"我的\"→\"导入短信收支记录\"，APP会自动扫描手机中的银行短信，识别收支信息，您可以选择性导入。\n" +
                        "\n权限说明：\n" +
                        "• 首次使用需授权\"读取短信与彩信\"和\"接收短信与彩信\"权限\n" +
                        "• 建议开启\"通知类短信\"权限以便实时捕获银行短信\n" +
                        "• 建议开启\"后台弹出界面\"和\"锁屏显示\"权限以保证导入功能完整\n" +
                        "• 导入页面顶部会显示权限检查卡片，未授权的权限会给出提示\n" +
                        "• 仅读取银行相关短信，不会读取其他隐私短信\n" +
                        "\n短信识别规则：APP根据银行短信号码和内容关键词自动识别收支类型，匹配支出分类的记为负数（资金流出），匹配收入分类的记为正数（资金流入）。\n\n" +
                        "支持银行：建行、工行、农行、中行、招行、交行、邮储、中信等各大银行。"
            )
            HelpFaqItem(
                question = "如何导入微信/支付宝账单？",
                answer = "在\"我的\"页面找到\"导入微信账单\"或\"导入支付宝账单\"，选择导出的CSV或xlsx账单文件即可批量导入历史记录。\n" +
                        "导出方法：\n" +
                        "• 微信：我→服务→钱包→账单→常见问题→下载账单→用于个人对账→选择时间范围→发送到邮箱\n" +
                        "• 支付宝：我的→账单→点击右上角\"...\"→开具交易流水证明→用于个人对账→选择时间范围→发送到邮箱\n" +
                        "收到邮件后，下载文件到手机，然后在APP中选择该文件导入。\n" +
                        "支持CSV和xlsx两种格式。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 待确认交易
            HelpSectionHeader(title = "待确认交易", icon = Icons.Default.PendingActions)

            HelpFaqItem(
                question = "什么是待确认交易？",
                answer = "当开启通知自动记账但未开启\"无感自动记账\"时，系统捕获到交易通知后会生成待确认交易。待确认交易会显示在首页的\"待确认账单\"卡片中。\n" +
                        "每条待确认记录显示分类名、备注（前40字）、日期和金额，您可以：\n" +
                        "• 点击\"待确认\"按钮：进入编辑页面，修改信息后保存为正式交易\n" +
                        "• 点击\"删除\"按钮：丢弃该条待确认记录\n" +
                        "编辑保存后，待确认记录会自动删除。"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 外观与设置
            HelpSectionHeader(title = "外观与设置", icon = Icons.Default.Palette)

            HelpFaqItem(
                question = "如何更换主题颜色？",
                answer = "进入\"我的\"→\"主题颜色\"，提供30+种主题配色方案，包括经典、系统品牌、商务、生活、极简、渐变等多种风格。选择喜欢的颜色即可实时切换，全局生效。"
            )
            HelpFaqItem(
                question = "如何设置深色模式？",
                answer = "进入\"我的\"→\"深色模式\"，提供三种模式选择：\n" +
                        "• 跟随系统：根据手机系统设置自动切换深色/浅色模式\n" +
                        "• 开启：始终使用深色模式（自动使用专属深色主题颜色）\n" +
                        "• 关闭：始终使用浅色模式"
            )
            HelpFaqItem(
                question = "如何更换货币符号？",
                answer = "进入\"我的\"→\"货币符号\"，可选择常用货币符号（¥、$、€、£、₩、₹等），也可自定义输入最多3个字符的货币符号。"
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
