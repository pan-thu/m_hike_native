package dev.panthu.mhikeapplication.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.panthu.mhikeapplication.presentation.auth.AuthEvent
import dev.panthu.mhikeapplication.presentation.auth.AuthViewModel
import dev.panthu.mhikeapplication.presentation.auth.AuthenticationState
import dev.panthu.mhikeapplication.presentation.common.components.GuestModeBanner
import dev.panthu.mhikeapplication.presentation.common.components.MHikePrimaryButton

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToHikeList: () -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isGuest = uiState.authState is AuthenticationState.Guest
    val isAuthenticated = uiState.authState is AuthenticationState.Authenticated

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to M-Hike!",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guest mode greeting
            if (isGuest) {
                Text(
                    text = "Hello, Guest!",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Using offline mode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Authenticated user greeting
            uiState.currentUser?.let { user ->
                Text(
                    text = "Hello, ${user.displayName}!",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "@${user.handle}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Guest mode banner
            if (isGuest) {
                GuestModeBanner(
                    onSignUp = onNavigateToSignUp,
                    message = "Sign up to unlock cloud backup and sharing features"
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            MHikePrimaryButton(
                text = "My Hikes",
                onClick = onNavigateToHikeList
            )

            Spacer(modifier = Modifier.height(16.dp))

            MHikePrimaryButton(
                text = "Log Out",
                onClick = {
                    viewModel.onEvent(AuthEvent.SignOut)
                    onLogout()
                }
            )
        }
    }
}
