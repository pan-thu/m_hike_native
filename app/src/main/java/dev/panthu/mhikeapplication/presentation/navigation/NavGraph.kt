package dev.panthu.mhikeapplication.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.panthu.mhikeapplication.domain.usecase.GetCurrentLocationUseCase
import dev.panthu.mhikeapplication.presentation.auth.login.LoginScreen
import dev.panthu.mhikeapplication.presentation.auth.signup.SignUpScreen
import dev.panthu.mhikeapplication.presentation.hike.create.HikeCreationScreen
import dev.panthu.mhikeapplication.presentation.hike.detail.HikeDetailScreen
import dev.panthu.mhikeapplication.presentation.hike.list.HikeListScreen
import dev.panthu.mhikeapplication.presentation.home.HomeScreen
import dev.panthu.mhikeapplication.presentation.observation.add.AddObservationScreen
import dev.panthu.mhikeapplication.presentation.observation.detail.ObservationDetailScreen
import dev.panthu.mhikeapplication.presentation.hike.share.ShareHikeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    getCurrentLocationUseCase: GetCurrentLocationUseCase
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHikeList = {
                    navController.navigate(Screen.HikeList.route)
                }
            )
        }

        composable(Screen.HikeList.route) {
            HikeListScreen(
                onHikeClick = { hikeId ->
                    navController.navigate(Screen.HikeDetail.createRoute(hikeId))
                },
                onCreateHike = {
                    navController.navigate(Screen.HikeCreate.route)
                }
            )
        }

        composable(Screen.HikeCreate.route) {
            HikeCreationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onHikeCreated = { hikeId ->
                    navController.navigate(Screen.HikeDetail.createRoute(hikeId)) {
                        popUpTo(Screen.HikeList.route)
                    }
                },
                getCurrentLocationUseCase = getCurrentLocationUseCase
            )
        }

        composable(
            route = Screen.HikeDetail.route,
            arguments = listOf(navArgument("hikeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hikeId = backStackEntry.arguments?.getString("hikeId") ?: return@composable
            HikeDetailScreen(
                hikeId = hikeId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onShare = { hikeId ->
                    navController.navigate(Screen.ShareHike.createRoute(hikeId))
                },
                onNavigateToAddObservation = { hikeId ->
                    navController.navigate(Screen.ObservationAdd.createRoute(hikeId))
                },
                onNavigateToObservationDetail = { hikeId, observationId ->
                    navController.navigate(Screen.ObservationDetail.createRoute(hikeId, observationId))
                }
            )
        }

        composable(
            route = Screen.ObservationAdd.route,
            arguments = listOf(navArgument("hikeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hikeId = backStackEntry.arguments?.getString("hikeId") ?: return@composable
            AddObservationScreen(
                hikeId = hikeId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ObservationDetail.route,
            arguments = listOf(
                navArgument("hikeId") { type = NavType.StringType },
                navArgument("observationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val hikeId = backStackEntry.arguments?.getString("hikeId") ?: return@composable
            val observationId = backStackEntry.arguments?.getString("observationId") ?: return@composable
            ObservationDetailScreen(
                hikeId = hikeId,
                observationId = observationId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ShareHike.route,
            arguments = listOf(navArgument("hikeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hikeId = backStackEntry.arguments?.getString("hikeId") ?: return@composable
            ShareHikeScreen(
                hikeId = hikeId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
