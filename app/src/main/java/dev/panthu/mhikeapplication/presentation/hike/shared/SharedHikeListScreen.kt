package dev.panthu.mhikeapplication.presentation.hike.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.panthu.mhikeapplication.domain.model.Difficulty
import dev.panthu.mhikeapplication.presentation.hike.HikeEvent
import dev.panthu.mhikeapplication.presentation.hike.HikeViewModel
import dev.panthu.mhikeapplication.presentation.hike.components.HikeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedHikeListScreen(
    onHikeClick: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: HikeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(HikeEvent.LoadSharedHikes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shared Hikes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.allHikes.isEmpty() -> {
                    // No shared hikes
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No shared hikes",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Hikes shared with you will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                else -> {
                    // Has shared hikes - show search and filters
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onEvent(HikeEvent.SearchHikes(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text("Search hikes...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onEvent(HikeEvent.SearchHikes("")) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true
                        )

                        // Filter chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Clear filters chip
                            if (uiState.filterDifficulty != null || uiState.filterHasParking != null) {
                                item {
                                    AssistChip(
                                        onClick = { viewModel.onEvent(HikeEvent.ClearFilters) },
                                        label = { Text("Clear Filters") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear filters",
                                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                                            )
                                        }
                                    )
                                }
                            }

                            // Difficulty filters
                            item {
                                FilterChip(
                                    selected = uiState.filterDifficulty == Difficulty.EASY,
                                    onClick = {
                                        viewModel.onEvent(
                                            HikeEvent.FilterByDifficulty(
                                                if (uiState.filterDifficulty == Difficulty.EASY) null else Difficulty.EASY
                                            )
                                        )
                                    },
                                    label = { Text("Easy") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.filterDifficulty == Difficulty.MEDIUM,
                                    onClick = {
                                        viewModel.onEvent(
                                            HikeEvent.FilterByDifficulty(
                                                if (uiState.filterDifficulty == Difficulty.MEDIUM) null else Difficulty.MEDIUM
                                            )
                                        )
                                    },
                                    label = { Text("Medium") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = uiState.filterDifficulty == Difficulty.HARD,
                                    onClick = {
                                        viewModel.onEvent(
                                            HikeEvent.FilterByDifficulty(
                                                if (uiState.filterDifficulty == Difficulty.HARD) null else Difficulty.HARD
                                            )
                                        )
                                    },
                                    label = { Text("Hard") }
                                )
                            }

                            // Parking filter
                            item {
                                FilterChip(
                                    selected = uiState.filterHasParking == true,
                                    onClick = {
                                        viewModel.onEvent(
                                            HikeEvent.FilterByParking(
                                                if (uiState.filterHasParking == true) null else true
                                            )
                                        )
                                    },
                                    label = { Text("Parking") }
                                )
                            }
                        }

                        // Hikes list or "no results" message
                        if (uiState.hikes.isEmpty()) {
                            // Has hikes but none match filters/search
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No hikes found",
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Try adjusting your search or filters",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.hikes, key = { it.id }) { hike ->
                                    HikeCard(
                                        hike = hike,
                                        onClick = { onHikeClick(hike.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error display
            uiState.error?.let { error ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
