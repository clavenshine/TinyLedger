package com.tinyledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tinyledger.app.ui.navigation.AppNavHost
import com.tinyledger.app.ui.navigation.Screen
import com.tinyledger.app.ui.navigation.bottomNavItems
import com.tinyledger.app.ui.theme.TinyLedgerTheme
import com.tinyledger.app.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TinyLedgerAppContent()
        }
    }
}

@Composable
fun TinyLedgerAppContent() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    TinyLedgerTheme(
        themeMode = settingsState.settings.themeMode,
        appColorScheme = settingsState.colorScheme
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val showBottomBar = currentRoute in listOf(
            Screen.Home.route,
            Screen.Bills.route,
            Screen.Statistics.route,
            Screen.Profile.route
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    AppBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onAddClick = {
                            navController.navigate(Screen.AddTransaction.route)
                        }
                    )
                }
            }
        ) { paddingValues ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun AppBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                // 首页
                NavItemView(
                    item = bottomNavItems[0],
                    isSelected = currentRoute == bottomNavItems[0].route,
                    onClick = { onNavigate(bottomNavItems[0].route) }
                )

                // 账单
                NavItemView(
                    item = bottomNavItems[1],
                    isSelected = currentRoute == bottomNavItems[1].route,
                    onClick = { onNavigate(bottomNavItems[1].route) }
                )

                // 中间的记账按钮 (FAB style)
                CenterAddButton(onClick = onAddClick)

                // 统计
                NavItemView(
                    item = bottomNavItems[2],
                    isSelected = currentRoute == bottomNavItems[2].route,
                    onClick = { onNavigate(bottomNavItems[2].route) }
                )

                // 我的
                NavItemView(
                    item = bottomNavItems[3],
                    isSelected = currentRoute == bottomNavItems[3].route,
                    onClick = { onNavigate(bottomNavItems[3].route) }
                )
            }
        }
    }
}

@Composable
private fun CenterAddButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "记账",
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun NavItemView(
    item: com.tinyledger.app.ui.navigation.BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selected indicator dot
        Box(
            modifier = Modifier
                .width(if (isSelected) 20.dp else 0.dp)
                .height(3.dp)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.height(2.dp))

        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            }
        )
    }
}
