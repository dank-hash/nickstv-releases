package com.nickstv.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.ActivityManager
import android.graphics.BitmapFactory
import com.nickstv.app.R
import androidx.navigation.*
import androidx.navigation.compose.*
import com.nickstv.app.ui.*
import com.nickstv.app.ui.browse.BrowseScreen
import com.nickstv.app.ui.detail.DetailScreen
import com.nickstv.app.ui.home.HomeScreen
import com.nickstv.app.ui.player.PlayerScreen
import com.nickstv.app.ui.search.SearchScreen
import com.nickstv.app.ui.settings.SettingsScreen
import com.nickstv.app.ui.watchlist.WatchlistScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set task description so app appears in recents with correct name/icon
        @Suppress("DEPRECATION")
        setTaskDescription(
            ActivityManager.TaskDescription(
                "Nicks TV",
                BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher),
                android.graphics.Color.parseColor("#0A0A0F")
            )
        )

        // Full-screen / immersive for Fire TV
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            NicksTVAppContent()
        }
    }
}

@Composable
fun NicksTVAppContent() {
    val navController = rememberNavController()
    val currentBack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBack?.destination?.route

    val showNavBar = currentRoute != null &&
            !currentRoute.startsWith("player/") &&
            !currentRoute.startsWith("detail/")

    Box(modifier = Modifier.fillMaxSize().background(NicksTheme.BgPrimary)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left nav bar
            if (showNavBar) {
                SideNavBar(
                    currentRoute = currentRoute ?: "",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(NavRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Main content
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.HOME,
                    enterTransition = { fadeIn(tween(250)) },
                    exitTransition = { fadeOut(tween(200)) }
                ) {
                    composable(NavRoutes.HOME) {
                        HomeScreen(
                            onNavigateToDetail = { type, id ->
                                navController.navigate(NavRoutes.detail(type, id))
                            }
                        )
                    }

                    composable(NavRoutes.SEARCH) {
                        SearchScreen(
                            onNavigateToDetail = { type, id ->
                                navController.navigate(NavRoutes.detail(type, id))
                            }
                        )
                    }

                    composable(NavRoutes.BROWSE) {
                        BrowseScreen(
                            onNavigateToDetail = { type, id ->
                                navController.navigate(NavRoutes.detail(type, id))
                            }
                        )
                    }

                    composable(NavRoutes.WATCHLIST) {
                        WatchlistScreen(
                            onNavigateToDetail = { type, id ->
                                navController.navigate(NavRoutes.detail(type, id))
                            }
                        )
                    }

                    composable(NavRoutes.SETTINGS) {
                        SettingsScreen()
                    }

                    composable(
                        route = NavRoutes.DETAIL,
                        arguments = listOf(
                            navArgument("mediaType") { type = NavType.StringType },
                            navArgument("mediaId") { type = NavType.StringType }
                        )
                    ) { back ->
                        val mediaType = back.arguments?.getString("mediaType") ?: "movie"
                        val mediaId = back.arguments?.getString("mediaId") ?: "0"
                        DetailScreen(
                            mediaId = mediaId,
                            mediaType = mediaType,
                            onNavigateToPlayer = { type, id, season, episode ->
                                navController.navigate(NavRoutes.player(type, id, season, episode))
                            },
                            onNavigateToDetail = { type, id ->
                                navController.navigate(NavRoutes.detail(type, id))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = NavRoutes.PLAYER,
                        arguments = listOf(
                            navArgument("mediaType") { type = NavType.StringType },
                            navArgument("mediaId") { type = NavType.StringType },
                            navArgument("season") { type = NavType.StringType; nullable = true; defaultValue = null },
                            navArgument("episode") { type = NavType.StringType; nullable = true; defaultValue = null }
                        )
                    ) { back ->
                        val mediaType = back.arguments?.getString("mediaType") ?: "movie"
                        val mediaId = back.arguments?.getString("mediaId") ?: "0"
                        val season = back.arguments?.getString("season")?.toIntOrNull()
                        val episode = back.arguments?.getString("episode")?.toIntOrNull()
                        PlayerScreen(
                            mediaId = mediaId,
                            mediaType = mediaType,
                            season = season,
                            episode = episode,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SideNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.Browse,
        BottomNavItem.Watchlist,
        BottomNavItem.Settings
    )

    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(NicksTheme.BgSecondary)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // App name / logo
        Text(
            "NICKS TV",
            color = NicksTheme.AccentRed,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 12.dp, bottom = 20.dp)
        )

        items.forEach { item ->
            NavItem(
                label = item.label,
                icon = navIcon(item.icon),
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}

@Composable
private fun NavItem(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    selected -> NicksTheme.AccentRed.copy(alpha = 0.15f)
                    focused -> NicksTheme.BgSurface
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NicksTheme.FocusBorder else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = if (selected) NicksTheme.AccentRed else if (focused) NicksTheme.TextPrimary else NicksTheme.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NicksTheme.AccentRed)
            )
        }
    }
}

private fun navIcon(icon: String): String = when (icon) {
    "home" -> "🏠"
    "search" -> "🔍"
    "grid_view" -> "🎬"
    "bookmark" -> "🔖"
    "settings" -> "⚙"
    else -> "●"
}
