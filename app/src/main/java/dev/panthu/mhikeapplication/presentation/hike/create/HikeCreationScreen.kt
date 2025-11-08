package dev.panthu.mhikeapplication.presentation.hike.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.panthu.mhikeapplication.domain.model.Difficulty
import dev.panthu.mhikeapplication.domain.usecase.GetCurrentLocationUseCase
import dev.panthu.mhikeapplication.presentation.common.components.ImagePicker
import dev.panthu.mhikeapplication.presentation.common.components.LocationPicker
import dev.panthu.mhikeapplication.presentation.common.components.MHikePrimaryButton
import dev.panthu.mhikeapplication.presentation.common.components.MHikeSecondaryButton
import dev.panthu.mhikeapplication.presentation.common.components.MHikeTextField
import dev.panthu.mhikeapplication.presentation.hike.HikeEvent
import dev.panthu.mhikeapplication.presentation.hike.HikeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HikeCreationScreen(
    onNavigateBack: () -> Unit,
    onHikeCreated: (String) -> Unit,
    getCurrentLocationUseCase: GetCurrentLocationUseCase,
    hikeId: String? = null, // Optional: if provided, screen is in edit mode
    viewModel: HikeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    val isEditMode = hikeId != null

    // Load hike data if in edit mode
    LaunchedEffect(hikeId) {
        hikeId?.let {
            viewModel.onEvent(HikeEvent.LoadHike(it))
        }
    }

    // Handle hike creation/update success
    // Only navigate after creation (not edit mode) or after successful update (when isCreating becomes false)
    LaunchedEffect(uiState.currentHike, uiState.isCreating) {
        if (!isEditMode) {
            // Create mode: navigate when hike is created
            uiState.currentHike?.let { hike ->
                if (!uiState.isCreating) {
                    onHikeCreated(hike.id)
                }
            }
        } else {
            // Edit mode: navigate only after successful update (when isCreating goes from true to false)
            if (!uiState.isCreating && uiState.currentHike != null && uiState.error == null) {
                // Check if we just finished updating (formState should be reset)
                if (formState.name.isEmpty()) {
                    onNavigateBack()
                }
            }
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error, uiState.uploadError) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(HikeEvent.ClearError)
        }
        uiState.uploadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(HikeEvent.ClearError)
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.date
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onEvent(HikeEvent.DateChanged(millis))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Hike" else "Create Hike") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name field
            MHikeTextField(
                value = formState.name,
                onValueChange = { viewModel.onEvent(HikeEvent.NameChanged(it)) },
                label = "Hike Name *",
                placeholder = "e.g., Mount Hood Trail",
                isError = formState.nameError != null,
                errorMessage = formState.nameError,
                enabled = !uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            )

            // Location picker
            LocationPicker(
                location = formState.location,
                onLocationChange = { viewModel.onEvent(HikeEvent.LocationChanged(it)) },
                getCurrentLocationUseCase = getCurrentLocationUseCase,
                modifier = Modifier.fillMaxWidth()
            )

            // Date picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                MHikeTextField(
                    value = formatDate(formState.date),
                    onValueChange = { /* Handled by date picker */ },
                    label = "Date *",
                    placeholder = "Select date",
                    isError = formState.dateError != null,
                    errorMessage = formState.dateError,
                    readOnly = true,
                    enabled = !uiState.isCreating,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Length field
            MHikeTextField(
                value = formState.length,
                onValueChange = { viewModel.onEvent(HikeEvent.LengthChanged(it)) },
                label = "Length (km) *",
                placeholder = "e.g., 12.5",
                isError = formState.lengthError != null,
                errorMessage = formState.lengthError,
                enabled = !uiState.isCreating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Difficulty selector
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Difficulty",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Difficulty.entries.forEachIndexed { index, difficulty ->
                        SegmentedButton(
                            selected = formState.difficulty == difficulty,
                            onClick = { viewModel.onEvent(HikeEvent.DifficultyChanged(difficulty)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = Difficulty.entries.size
                            ),
                            enabled = !uiState.isCreating
                        ) {
                            Text(difficulty.name.lowercase().capitalize())
                        }
                    }
                }
            }

            // Parking switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parking Available",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = formState.hasParking,
                    onCheckedChange = { viewModel.onEvent(HikeEvent.ParkingChanged(it)) },
                    enabled = !uiState.isCreating
                )
            }

            // Description field (optional)
            MHikeTextField(
                value = formState.description,
                onValueChange = { viewModel.onEvent(HikeEvent.DescriptionChanged(it)) },
                label = "Description (Optional)",
                placeholder = "Add notes about the hike...",
                enabled = !uiState.isCreating,
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            // Image picker - limited to 1 image
            ImagePicker(
                onImageSelected = { uri ->
                    // Only allow if no image already selected
                    if (formState.images.isEmpty()) {
                        viewModel.onEvent(HikeEvent.ImageSelected(uri))
                    }
                },
                uploadProgress = uiState.uploadProgress,
                isUploading = uiState.isUploading,
                error = uiState.uploadError,
                onCancelUpload = { viewModel.onEvent(HikeEvent.CancelUpload) },
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.images.isEmpty() && !uiState.isUploading && !uiState.isCreating
            )

            // Display selected image
            if (formState.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                                viewModel.onEvent(HikeEvent.ImageDeleted(image))
                            }
                        },
                        enabled = !uiState.isCreating
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Create/Update button
            MHikePrimaryButton(
                text = if (isEditMode) "Update Hike" else "Create Hike",
                onClick = {
                    if (isEditMode && hikeId != null) {
                        viewModel.onEvent(HikeEvent.UpdateHike(hikeId))
                    } else {
                        viewModel.onEvent(HikeEvent.CreateHike)
                    }
                },
                enabled = formState.isValid && !uiState.isCreating && !uiState.isUploading,
                isLoading = uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            )

            // Loading indicator
            if (uiState.isCreating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestamp))
}

private fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault())
        else it.toString()
    }
}
