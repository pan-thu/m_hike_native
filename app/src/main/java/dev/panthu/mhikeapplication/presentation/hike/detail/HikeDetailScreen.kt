package dev.panthu.mhikeapplication.presentation.hike.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.panthu.mhikeapplication.domain.model.Difficulty
import dev.panthu.mhikeapplication.presentation.auth.AuthViewModel
import dev.panthu.mhikeapplication.presentation.auth.AuthenticationState
import dev.panthu.mhikeapplication.presentation.common.components.GuestModeBanner
import dev.panthu.mhikeapplication.presentation.common.components.MHikePrimaryButton
import dev.panthu.mhikeapplication.presentation.hike.HikeEvent
import dev.panthu.mhikeapplication.presentation.hike.HikeViewModel
import dev.panthu.mhikeapplication.presentation.observation.components.ObservationListComponent
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeDetailScreen(
    hikeId: String,
    onNavigateBack: () -> Unit,
    onShare: (String) -> Unit = {},
    onNavigateToAddObservation: (String) -> Unit = {},
    onNavigateToObservationDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToSignUp: () -> Unit = {},
    viewModel: HikeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    val isAuthenticated = authState.authState is AuthenticationState.Authenticated
    val isGuest = authState.authState is AuthenticationState.Guest

    LaunchedEffect(hikeId) {
        viewModel.onEvent(HikeEvent.LoadHike(hikeId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hike Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    // Share button only for authenticated users
                    if (isAuthenticated) {
                        IconButton(onClick = { onShare(hikeId) }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share hike"
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.onEvent(HikeEvent.DeleteHike(hikeId)) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete hike"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            uiState.currentHike?.let { hike ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero image
                    if (hike.imageUrls.isNotEmpty()) {
                        AsyncImage(
                            model = hike.imageUrls.first(),
                            contentDescription = "Hike hero image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Guest mode banner
                        if (isGuest) {
                            GuestModeBanner(
                                onSignUp = onNavigateToSignUp,
                                message = "Sign up to share this hike with friends and back up to cloud"
                            )
                        }

                        // Title
                        Text(
                            text = hike.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Info cards
                        InfoSection(
                            icon = Icons.Filled.LocationOn,
                            label = "Location",
                            value = hike.location.name
                        )

                        InfoSection(
                            icon = Icons.Filled.CalendarToday,
                            label = "Date",
                            value = formatDate(hike.date.toDate())
                        )

                        InfoSection(
                            icon = Icons.Filled.DirectionsWalk,
                            label = "Length",
                            value = "${hike.length} km"
                        )

                        // Difficulty and parking row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DifficultyBadge(
                                difficulty = hike.difficulty,
                                modifier = Modifier.weight(1f)
                            )
                            if (hike.hasParking) {
                                ParkingBadge(modifier = Modifier.weight(1f))
                            }
                        }

                        // Description
                        if (hike.description.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Description",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = hike.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Observations Section
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Observations",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add Observation Button
                        MHikePrimaryButton(
                            text = "Add Observation",
                            onClick = { onNavigateToAddObservation(hikeId) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Observation List
                        ObservationListComponent(
                            hikeId = hikeId,
                            onObservationClick = { observationId ->
                                onNavigateToObservationDetail(hikeId, observationId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DifficultyBadge(
    difficulty: Difficulty,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (difficulty) {
        Difficulty.EASY -> MaterialTheme.colorScheme.tertiaryContainer to "Easy"
        Difficulty.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer to "Medium"
        Difficulty.HARD -> MaterialTheme.colorScheme.errorContainer to "Hard"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ParkingBadge(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LocalParking,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Parking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun formatDate(date: java.util.Date): String {
    val formatter = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}
