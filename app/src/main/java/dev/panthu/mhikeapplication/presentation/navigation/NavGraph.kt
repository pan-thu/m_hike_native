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
import dev.panthu.mhikeapplication.presentation.auth.AuthViewModel
import dev.panthu.mhikeapplication.presentation.auth.AuthEvent
import dev.panthu.mhikeapplication.presentation.auth.login.LoginScreen
import dev.panthu.mhikeapplication.presentation.auth.signup.SignUpScreen
import dev.panthu.mhikeapplication.presentation.hike.create.HikeCreationScreen
import dev.panthu.mhikeapplication.presentation.hike.detail.HikeDetailScreen
import dev.panthu.mhikeapplication.presentation.hike.list.HikeListScreen
import dev.panthu.mhikeapplication.presentation.hike.shared.SharedHikeDetailScreen
import dev.panthu.mhikeapplication.presentation.hike.shared.SharedHikeListScreen
import dev.panthu.mhikeapplication.presentation.home.HomeScreen
import dev.panthu.mhikeapplication.presentation.observation.add.AddObservationScreen
import dev.panthu.mhikeapplication.presentation.observation.detail.ObservationDetailScreen
import dev.panthu.mhikeapplication.presentation.hike.share.ShareHikeScreen
import dev.panthu.mhikeapplication.presentation.onboarding.OnboardingScreen

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
        composable(Screen.Onboarding.route) {
            val authViewModel: AuthViewModel = hiltViewModel()

            OnboardingScreen(
                onContinueAsGuest = {
                    authViewModel.onEvent(AuthEvent.ContinueAsGuest)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onSignIn = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

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
                },
                onNavigateToSharedHikes = {
                    navController.navigate(Screen.SharedHikeList.route)
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
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
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.SharedHikeList.route) {
            SharedHikeListScreen(
                onHikeClick = { hikeId ->
                    navController.navigate(Screen.SharedHikeDetail.createRoute(hikeId))
                },
                onNavigateBack = {
                    navController.popBackStack()
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

        composable(Screen.HikeEdit.route,
            arguments = listOf(navArgument("hikeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hikeId = backStackEntry.arguments?.getString("hikeId") ?: return@composable
            HikeCreationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onHikeCreated = { _ ->
                    navController.popBackStack()
                },
                getCurrentLocationUseCase = getCurrentLocationUseCase,
                hikeId = hikeId // Pass hikeId for edit mode
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
                onEdit = { hikeId ->
                    navController.navigate(Screen.HikeEdit.createRoute(hikeId))
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
            route = Screen.SharedHikeDetail.route,
            arguments = listOf(navArgument("hikeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hikeId = backStackEntry.arguments?.getString("hikeId") ?: return@composable
            SharedHikeDetailScreen(
                hikeId = hikeId,
                onNavigateBack = {
                    navController.popBackStack()
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
                },
                getCurrentLocationUseCase = getCurrentLocationUseCase
            )
        }

        composable(
            route = Screen.ObservationEdit.route,
            arguments = listOf(
                navArgument("hikeId") { type = NavType.StringType },
                navArgument("observationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val hikeId = backStackEntry.arguments?.getString("hikeId") ?: return@composable
            val observationId = backStackEntry.arguments?.getString("observationId") ?: return@composable
            AddObservationScreen(
                hikeId = hikeId,
                observationId = observationId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                getCurrentLocationUseCase = getCurrentLocationUseCase
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
                },
                onNavigateToEdit = { hikeId, observationId ->
                    navController.navigate(Screen.ObservationEdit.createRoute(hikeId, observationId))
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
