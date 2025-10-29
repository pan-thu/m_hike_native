package dev.panthu.mhikeapplication.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Home : Screen("home")
    data object HikeList : Screen("hike_list")
    data object HikeCreate : Screen("hike_create")
    data object HikeDetail : Screen("hike_detail/{hikeId}") {
        fun createRoute(hikeId: String) = "hike_detail/$hikeId"
    }
    data object ObservationAdd : Screen("observation_add/{hikeId}") {
        fun createRoute(hikeId: String) = "observation_add/$hikeId"
    }
    data object ObservationDetail : Screen("observation_detail/{hikeId}/{observationId}") {
        fun createRoute(hikeId: String, observationId: String) = "observation_detail/$hikeId/$observationId"
    }
    data object ShareHike : Screen("share_hike/{hikeId}") {
        fun createRoute(hikeId: String) = "share_hike/$hikeId"
    }
}
