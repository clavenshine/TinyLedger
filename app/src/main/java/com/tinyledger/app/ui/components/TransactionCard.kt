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
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val amountPrefix = if (isIncome) "+" else "-"

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
                    text = transaction.category.name,
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
                Text(
                    text = DateUtils.formatDisplayDate(transaction.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount
            Text(
                text = "$amountPrefix${CurrencyUtils.format(transaction.amount, currencySymbol)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
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
        // 新增分类图标（使用原始icon名作为key，兼容Transaction.kt中的定义）
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
        "bonus" -> Icons.Default.CardGiftcard
        "investment" -> Icons.Default.TrendingUp
        "financial" -> Icons.Default.AccountBalance
        "redpacket" -> Icons.Default.CardGiftcard
        "utilities" -> Icons.Default.ElectricalServices
        "credit_card_repay" -> Icons.Default.CreditCard
        "mortgage" -> Icons.Default.House
        "repay_loan" -> Icons.Default.Payments
        "alipay_repay" -> Icons.Default.Payment
        "douyin_repay" -> Icons.Default.Payment
        "jd_repay" -> Icons.Default.Payment
        "account_transfer" -> Icons.Default.SwapHoriz
        "dividend" -> Icons.Default.Paid
        "refund" -> Icons.Default.AssignmentReturn
        "deposit_back" -> Icons.Default.Savings
        "收回借款" -> Icons.Default.CallReceived
        "accommodation" -> Icons.Default.Hotel
        "charity" -> Icons.Default.VolunteerActivism
        "send_redpacket" -> Icons.Default.CardGiftcard
        "family_living" -> Icons.Default.FamilyRestroom
        "income_transfer" -> Icons.Default.SwapHoriz
        "reimbursement" -> Icons.Default.RequestPage
        else -> Icons.Default.Category
    }
}
