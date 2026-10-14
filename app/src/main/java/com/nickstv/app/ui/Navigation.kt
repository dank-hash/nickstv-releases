package com.nickstv.app.ui

// Navigation route constants — mediaId is always a String
object NavRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val BROWSE = "browse"
    const val WATCHLIST = "watchlist"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{mediaType}/{mediaId}"
    const val PLAYER = "player/{mediaType}/{mediaId}?season={season}&episode={episode}"

    fun detail(type: String, id: String) = "detail/$type/$id"
    fun player(type: String, id: String, season: Int? = null, episode: Int? = null): String {
        var route = "player/$type/$id"
        val params = buildList {
            season?.let { add("season=$it") }
            episode?.let { add("episode=$it") }
        }
        if (params.isNotEmpty()) route += "?${params.joinToString("&")}"
        return route
    }
}

enum class BottomNavItem(val route: String, val label: String, val icon: String) {
    Home(NavRoutes.HOME, "Home", "home"),
    Search(NavRoutes.SEARCH, "Search", "search"),
    Browse(NavRoutes.BROWSE, "Browse", "grid_view"),
    Watchlist(NavRoutes.WATCHLIST, "Watchlist", "bookmark"),
    Settings(NavRoutes.SETTINGS, "Settings", "settings"),
}
