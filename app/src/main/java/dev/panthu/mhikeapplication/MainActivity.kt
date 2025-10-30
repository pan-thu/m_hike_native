package dev.panthu.mhikeapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.panthu.mhikeapplication.domain.usecase.GetCurrentLocationUseCase
import dev.panthu.mhikeapplication.presentation.auth.AuthViewModel
import dev.panthu.mhikeapplication.presentation.navigation.NavGraph
import dev.panthu.mhikeapplication.presentation.navigation.Screen
import dev.panthu.mhikeapplication.ui.theme.MHikeApplicationTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var getCurrentLocationUseCase: GetCurrentLocationUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MHikeApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

                    // Determine start destination based on auth state
                    val startDestination = when (authUiState.authState) {
                        is dev.panthu.mhikeapplication.presentation.auth.AuthenticationState.Authenticated -> {
                            Screen.Home.route
                        }
                        is dev.panthu.mhikeapplication.presentation.auth.AuthenticationState.Guest -> {
                            Screen.Home.route
                        }
                        is dev.panthu.mhikeapplication.presentation.auth.AuthenticationState.Unauthenticated -> {
                            Screen.Onboarding.route
                        }
                    }

                    NavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        getCurrentLocationUseCase = getCurrentLocationUseCase
                    )
                }
            }
        }
    }
}