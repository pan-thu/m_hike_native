package dev.panthu.mhikeapplication.presentation.observation.add

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.panthu.mhikeapplication.presentation.common.components.LocationPicker
import dev.panthu.mhikeapplication.presentation.common.components.MHikePrimaryButton
import dev.panthu.mhikeapplication.presentation.common.components.MHikeTextField
import dev.panthu.mhikeapplication.presentation.observation.ObservationEvent
import dev.panthu.mhikeapplication.presentation.observation.ObservationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObservationScreen(
    hikeId: String,
    onNavigateBack: () -> Unit,
    viewModel: ObservationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Date picker state
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(ObservationEvent.ImageSelected(it)) }
    }

    // Navigate back on success
    LaunchedEffect(uiState.currentObservation) {
        if (uiState.currentObservation != null && !uiState.isCreating) {
            onNavigateBack()
        }
    }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(ObservationEvent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Observation",
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
            // Text field (required)
            Text(
                text = "Observation *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            MHikeTextField(
                value = formState.text,
                onValueChange = { viewModel.onEvent(ObservationEvent.TextChanged(it)) },
                label = "What did you observe?",
                placeholder = "Describe what you saw, heard, or experienced",
                isError = formState.textError != null,
                errorMessage = formState.textError,
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            // Time display
            Text(
                text = "Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            val timeFormat = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()) }
            Text(
                text = timeFormat.format(Date(formState.timestamp)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Comments (optional)
            Text(
                text = "Additional Comments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            MHikeTextField(
                value = formState.comments,
                onValueChange = { viewModel.onEvent(ObservationEvent.CommentsChanged(it)) },
                label = "Comments (optional)",
                placeholder = "Any additional notes",
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Location picker
            Text(
                text = "Location (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            LocationPicker(
                location = formState.location,
                onLocationSelected = { location ->
                    viewModel.onEvent(ObservationEvent.LocationChanged(location))
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            MHikePrimaryButton(
                text = "Save Observation",
                onClick = {
                    viewModel.onEvent(ObservationEvent.CreateObservation(hikeId))
                },
                enabled = !uiState.isCreating && formState.isValid,
                isLoading = uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
