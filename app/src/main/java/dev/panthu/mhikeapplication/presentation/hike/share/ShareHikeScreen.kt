package dev.panthu.mhikeapplication.presentation.hike.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.panthu.mhikeapplication.domain.model.User
import dev.panthu.mhikeapplication.presentation.common.components.UserSearchComponent
import dev.panthu.mhikeapplication.presentation.common.components.rememberDebouncedSearch
import dev.panthu.mhikeapplication.presentation.hike.HikeEvent
import dev.panthu.mhikeapplication.presentation.hike.HikeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareHikeScreen(
    hikeId: String,
    onNavigateBack: () -> Unit,
    viewModel: HikeViewModel = hiltViewModel(),
    authViewModel: dev.panthu.mhikeapplication.presentation.auth.AuthViewModel = hiltViewModel()
) {
    val hikeState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val currentHike = hikeState.currentHike
    val currentUserId = authState.currentUser?.uid ?: ""

    // Get currently shared users
    val sharedUserIds = currentHike?.accessControl?.let { access ->
        access.invitedUsers + access.sharedUsers
    }?.toSet() ?: emptySet()

    // Load hike
    LaunchedEffect(hikeId) {
        viewModel.onEvent(HikeEvent.LoadHike(hikeId))
    }

    // Debounced search
    rememberDebouncedSearch(searchQuery) { query ->
        if (query.length >= 2) {
            isSearching = true
            // Trigger user search
            viewModel.onEvent(HikeEvent.SearchUsers(query))
        }
    }

    // Update search results from ViewModel
    LaunchedEffect(hikeState.searchResults) {
        searchResults = hikeState.searchResults
        isSearching = false
    }

    // Show errors
    LaunchedEffect(hikeState.error) {
        hikeState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(HikeEvent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Share Hike",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Disclaimer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Guests can view only",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Current access list
            if (sharedUserIds.isNotEmpty()) {
                Text(
                    text = "Shared with (${sharedUserIds.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // TODO: Load full user details for shared users
                // For now, just show count
                Text(
                    text = "${sharedUserIds.size} user(s) have access",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search and add users
            Text(
                text = "Add more users",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            UserSearchComponent(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                users = searchResults.filter { it.uid != currentUserId },
                isSearching = isSearching,
                selectedUserIds = sharedUserIds,
                onUserAdd = { user ->
                    viewModel.onEvent(HikeEvent.ShareHike(hikeId, user.uid))
                    searchQuery = ""
                    searchResults = emptyList()
                }
            )
        }
    }
}
