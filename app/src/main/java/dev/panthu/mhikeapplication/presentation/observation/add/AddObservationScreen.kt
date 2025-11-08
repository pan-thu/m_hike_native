package dev.panthu.mhikeapplication.presentation.observation.add

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.panthu.mhikeapplication.domain.usecase.GetCurrentLocationUseCase
import dev.panthu.mhikeapplication.presentation.common.components.ImagePicker
import dev.panthu.mhikeapplication.presentation.common.components.LocationPicker
import dev.panthu.mhikeapplication.presentation.common.components.MHikePrimaryButton
import dev.panthu.mhikeapplication.presentation.common.components.MHikeSecondaryButton
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
    observationId: String? = null, // Optional: if provided, screen is in edit mode
    onNavigateBack: () -> Unit,
    getCurrentLocationUseCase: GetCurrentLocationUseCase,
    viewModel: ObservationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isEditMode = observationId != null

    // Load observation data if in edit mode
    LaunchedEffect(observationId) {
        if (observationId != null) {
            viewModel.onEvent(ObservationEvent.LoadObservation(hikeId, observationId))
        }
    }

    // Date picker state
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(ObservationEvent.ImageSelected(it)) }
    }

    // Navigate back on success
    // Handle observation creation/update success
    LaunchedEffect(uiState.currentObservation, uiState.isCreating) {
        if (!isEditMode) {
            // Create mode: navigate when observation is created
            uiState.currentObservation?.let { observation ->
                if (!uiState.isCreating) {
                    onNavigateBack()
                }
            }
        } else {
            // Edit mode: navigate only after successful update (when isCreating goes from true to false)
            if (!uiState.isCreating && uiState.currentObservation != null && uiState.error == null) {
                // Check if we just finished updating (formState should be reset)
                if (formState.text.isEmpty()) {
                    onNavigateBack()
                }
            }
        }
    }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(ObservationEvent.ClearError)
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.timestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Keep the time part, update only date
                            val calendar = java.util.Calendar.getInstance()
                            calendar.timeInMillis = formState.timestamp
                            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(java.util.Calendar.MINUTE)

                            calendar.timeInMillis = millis
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
                            calendar.set(java.util.Calendar.MINUTE, minute)

                            viewModel.onEvent(ObservationEvent.TimestampChanged(calendar.timeInMillis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = formState.timestamp
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(java.util.Calendar.MINUTE)
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCalendar = java.util.Calendar.getInstance()
                        newCalendar.timeInMillis = formState.timestamp
                        newCalendar.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        newCalendar.set(java.util.Calendar.MINUTE, timePickerState.minute)
                        viewModel.onEvent(ObservationEvent.TimestampChanged(newCalendar.timeInMillis))
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Observation" else "Add Observation",
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
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            // Date and Time pickers
            Text(
                text = "Date & Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            val timeFormat = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date field
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(formState.timestamp)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Time field
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker = true },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(formState.timestamp)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

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
                location = formState.location ?: dev.panthu.mhikeapplication.domain.model.Location(),
                onLocationChange = { location ->
                    viewModel.onEvent(ObservationEvent.LocationChanged(location))
                },
                getCurrentLocationUseCase = getCurrentLocationUseCase,
                modifier = Modifier.fillMaxWidth()
            )

            // Image picker - limited to 1 image
            Text(
                text = "Image (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            ImagePicker(
                onImageSelected = { uri ->
                    // Only allow if no image already selected
                    if (formState.images.isEmpty()) {
                        viewModel.onEvent(ObservationEvent.ImageSelected(uri))
                    }
                },
                uploadProgress = uiState.uploadProgress,
                isUploading = uiState.isUploading,
                error = uiState.uploadError,
                onCancelUpload = { viewModel.onEvent(ObservationEvent.CancelUpload) },
                enabled = formState.images.isEmpty() && !uiState.isUploading && !uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            )

            // Display selected image
            if (formState.images.isNotEmpty()) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "1 image selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    MHikeSecondaryButton(
                        text = "Remove",
                        onClick = {
                            formState.images.firstOrNull()?.let { image ->
                                viewModel.onEvent(ObservationEvent.ImageDeleted(image))
                            }
                        },
                        enabled = !uiState.isCreating
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save/Update button
            MHikePrimaryButton(
                text = if (isEditMode) "Update Observation" else "Save Observation",
                onClick = {
                    if (isEditMode && observationId != null) {
                        viewModel.onEvent(ObservationEvent.UpdateObservation(hikeId, observationId))
                    } else {
                        viewModel.onEvent(ObservationEvent.CreateObservation(hikeId))
                    }
                },
                enabled = !uiState.isCreating && formState.isValid,
                isLoading = uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
