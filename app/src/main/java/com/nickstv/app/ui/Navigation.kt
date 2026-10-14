package com.nickstv.app.ui

import java.net.URLDecoder
import java.net.URLEncoder

// Navigation route constants — mediaId is always a String
object NavRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val BROWSE = "browse"
    const val WATCHLIST = "watchlist"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{mediaType}/{mediaId}"
    // streamUrl is optional encoded URL — passed when user pre-selects a server from detail page
    const val PLAYER = "player/{mediaType}/{mediaId}?season={season}&episode={episode}&streamUrl={streamUrl}"

    fun detail(type: String, id: String) = "detail/$type/$id"

    fun player(
        type: String,
        id: String,
        season: Int? = null,
        episode: Int? = null,
        streamUrl: String? = null
    ): String {
        var route = "player/$type/$id"
        val params = buildList {
            season?.let { add("season=$it") }
            episode?.let { add("episode=$it") }
            streamUrl?.takeIf { it.isNotBlank() }?.let {
                add("streamUrl=${URLEncoder.encode(it, "UTF-8")}")
            }
        }
        if (params.isNotEmpty()) route += "?${params.joinToString("&")}"
        return route
    }

    fun decodeStreamUrl(encoded: String?): String? =
        encoded?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
}

enum class BottomNavItem(val route: String, val label: String, val icon: String) {
    Home(NavRoutes.HOME, "Home", "home"),
    Search(NavRoutes.SEARCH, "Search", "search"),
    Browse(NavRoutes.BROWSE, "Browse", "grid_view"),
    Watchlist(NavRoutes.WATCHLIST, "Watchlist", "bookmark"),
    Settings(NavRoutes.SETTINGS, "Settings", "settings"),
}
