package com.faarigh.app.ui.navigation

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.faarigh.app.R
import com.faarigh.app.ui.screen.apps.AppSelectionScreen
import com.faarigh.app.ui.screen.categories.CategoryLimitsScreen
import com.faarigh.app.ui.screen.content.ContentFilterScreen
import com.faarigh.app.ui.screen.home.HomeScreen
import com.faarigh.app.ui.screen.learn.LearnArticleScreen
import com.faarigh.app.ui.screen.learn.LearnScreen
import com.faarigh.app.ui.screen.modules.ModuleDetailScreen
import com.faarigh.app.ui.screen.onboarding.OnboardingScreen
import com.faarigh.app.ui.screen.quarantine.QuarantineScreen
import com.faarigh.app.ui.screen.settings.SettingsScreen
import com.faarigh.app.ui.screen.stats.StatsScreen
import com.faarigh.app.ui.screen.toolkit.ToolkitScreen

sealed class Screen(val route: String, val label: String, val iconRes: Int, val selectedIconRes: Int = iconRes) {
    data object Onboarding : Screen("onboarding", "Onboarding", R.drawable.ic_nav_home)
    data object Home : Screen("home", "Home", R.drawable.ic_nav_home)
    data object Toolkit : Screen("toolkit", "Toolkit", R.drawable.ic_nav_toolkit)
    data object Settings : Screen("settings", "Settings", R.drawable.ic_nav_settings)

    // Detail screens (not in bottom nav)
    data object Stats : Screen("stats", "Progress", R.drawable.ic_nav_stats)
    data object Apps : Screen("apps", "Apps", R.drawable.ic_nav_apps)
    data object Filters : Screen("filters", "Filter", R.drawable.ic_nav_toolkit)
    data object ModuleDetail : Screen("module_detail/{moduleId}", "Module Detail", R.drawable.ic_nav_toolkit)
    data object Learn : Screen("learn", "Learn", R.drawable.ic_nav_toolkit)
    data object LearnArticle : Screen("learn_article/{articleId}", "Learn Article", R.drawable.ic_nav_toolkit)
    data object Quarantine : Screen("quarantine", "Quarantine", R.drawable.ic_nav_toolkit)
    data object CategoryLimits : Screen("category_limits", "Category Limits", R.drawable.ic_nav_toolkit)
}

private val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Toolkit,
    Screen.Settings,
)

private const val PREFS_NAME = "faarigh_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

@Composable
fun FaarighNavHost(
    onRequestVpnConsent: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val isOnboardingDone = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_DONE, false)
    }

    val startDestination = if (isOnboardingDone) Screen.Home.route else Screen.Onboarding.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on the 3 main screens
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Toolkit.route,
        Screen.Settings.route,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            // ── Onboarding ───────────────────────────────────
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onRequestVpnConsent = onRequestVpnConsent,
                    onComplete = {
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_ONBOARDING_DONE, true)
                            .apply()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }

            // ── Main Tabs ────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToModuleDetail = { moduleId ->
                        navController.navigate("module_detail/$moduleId")
                    },
                    onNavigateToApps = {
                        navController.navigate(Screen.Apps.route) { launchSingleTop = true }
                    },
                    onNavigateToToolkit = {
                        navController.navigate(Screen.Toolkit.route) { launchSingleTop = true }
                    },
                    onNavigateToProgress = {
                        navController.navigate(Screen.Stats.route) { launchSingleTop = true }
                    },
                )
            }

            composable(Screen.Toolkit.route) {
                ToolkitScreen(
                    onNavigateToModuleDetail = { moduleId ->
                        navController.navigate("module_detail/$moduleId")
                    },
                    onNavigateToApps = {
                        navController.navigate(Screen.Apps.route) { launchSingleTop = true }
                    },
                    onNavigateToContentFilter = {
                        navController.navigate(Screen.Filters.route) { launchSingleTop = true }
                    },
                    onNavigateToCategoryLimits = {
                        navController.navigate(Screen.CategoryLimits.route) { launchSingleTop = true }
                    },
                    onNavigateToLearn = {
                        navController.navigate(Screen.Learn.route) { launchSingleTop = true }
                    },
                    onNavigateToQuarantine = {
                        navController.navigate(Screen.Quarantine.route) { launchSingleTop = true }
                    },
                    onRequestVpnConsent = onRequestVpnConsent,
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToLearn = {
                        navController.navigate(Screen.Learn.route) { launchSingleTop = true }
                    },
                )
            }

            // ── Detail Screens ───────────────────────────────
            composable(Screen.Stats.route) { StatsScreen() }

            composable(Screen.Apps.route) {
                AppSelectionScreen(
                    onNavigateToQuarantine = {
                        navController.navigate(Screen.Quarantine.route) { launchSingleTop = true }
                    },
                    onNavigateToCategoryLimits = {
                        navController.navigate(Screen.CategoryLimits.route) { launchSingleTop = true }
                    },
                )
            }

            composable(Screen.Filters.route) {
                ContentFilterScreen(onRequestVpnConsent = onRequestVpnConsent)
            }

            composable(Screen.ModuleDetail.route) { backStackEntry ->
                val moduleId = backStackEntry.arguments?.getString("moduleId") ?: ""
                ModuleDetailScreen(
                    moduleId = moduleId,
                    onBack = { navController.popBackStack() },
                    onNavigateToContentFilter = {
                        navController.navigate(Screen.Filters.route) { launchSingleTop = true }
                    },
                )
            }

            composable(Screen.Learn.route) {
                LearnScreen(
                    onArticleClick = { articleId ->
                        navController.navigate("learn_article/$articleId")
                    },
                )
            }

            composable(Screen.LearnArticle.route) { backStackEntry ->
                val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
                LearnArticleScreen(
                    articleId = articleId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Quarantine.route) {
                QuarantineScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.CategoryLimits.route) {
                CategoryLimitsScreen(onBack = { navController.popBackStack() })
            }
        }

        // ── Floating pill bottom navigation — Cardboard Retro ──
        if (showBottomBar) {
            val pillBg = MaterialTheme.colorScheme.surfaceContainerLow
            val activeTint = MaterialTheme.colorScheme.primary
            val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(4.dp),
                            ambientColor = Color.Black.copy(alpha = 0.15f),
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(pillBg)
                        .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavScreens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) activeTint else inactiveTint,
                            label = "navColor",
                        )
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) activeTint.copy(alpha = 0.12f) else Color.Transparent,
                            label = "navBg",
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(bgColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = if (isSelected) screen.selectedIconRes else screen.iconRes),
                                contentDescription = screen.label,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
