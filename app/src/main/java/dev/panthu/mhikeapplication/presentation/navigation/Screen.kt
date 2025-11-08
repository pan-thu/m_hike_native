package dev.panthu.mhikeapplication.presentation.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Home : Screen("home")
    data object HikeList : Screen("hike_list")
    data object SharedHikeList : Screen("shared_hike_list")
    data object HikeCreate : Screen("hike_create")
    data object HikeEdit : Screen("hike_edit/{hikeId}") {
        fun createRoute(hikeId: String) = "hike_edit/$hikeId"
    }
    data object HikeDetail : Screen("hike_detail/{hikeId}") {
        fun createRoute(hikeId: String) = "hike_detail/$hikeId"
    }
    data object SharedHikeDetail : Screen("shared_hike_detail/{hikeId}") {
        fun createRoute(hikeId: String) = "shared_hike_detail/$hikeId"
    }
    data object ObservationAdd : Screen("observation_add/{hikeId}") {
        fun createRoute(hikeId: String) = "observation_add/$hikeId"
    }
    data object ObservationEdit : Screen("observation_edit/{hikeId}/{observationId}") {
        fun createRoute(hikeId: String, observationId: String) = "observation_edit/$hikeId/$observationId"
    }
    data object ObservationDetail : Screen("observation_detail/{hikeId}/{observationId}") {
        fun createRoute(hikeId: String, observationId: String) = "observation_detail/$hikeId/$observationId"
    }
    data object ShareHike : Screen("share_hike/{hikeId}") {
        fun createRoute(hikeId: String) = "share_hike/$hikeId"
    }
}
