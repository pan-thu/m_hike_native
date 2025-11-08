package dev.panthu.mhikeapplication.presentation.common.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.GeoPoint
import dev.panthu.mhikeapplication.domain.usecase.GetCurrentLocationUseCase
import dev.panthu.mhikeapplication.util.Result
import kotlinx.coroutines.launch

@Composable
fun LocationPicker(
    location: dev.panthu.mhikeapplication.domain.model.Location,
    onLocationChange: (dev.panthu.mhikeapplication.domain.model.Location) -> Unit,
    getCurrentLocationUseCase: GetCurrentLocationUseCase,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasRequestedPermission by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted && hasRequestedPermission) {
            // Permission granted, fetch location
            scope.launch {
                isLoading = true
                error = null
                when (val result = getCurrentLocationUseCase()) {
                    is Result.Success -> {
                        val locationResult = result.data
                        onLocationChange(
                            dev.panthu.mhikeapplication.domain.model.Location(
                                name = locationResult.placeName,
                                coordinates = locationResult.geoPoint,
                                manualOverride = false
                            )
                        )
                        isLoading = false
                    }
                    is Result.Error -> {
                        error = result.message ?: "Failed to get location"
                        isLoading = false
                    }
                    is Result.Loading -> { /* Already handling */ }
                }
            }
        } else if (!granted && hasRequestedPermission) {
            error = "Location permission denied. Please enable it in settings or enter manually."
        }
        hasRequestedPermission = false
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Location *",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp)
                        )
                    } else {
                        IconButton(
                            onClick = {
                                hasRequestedPermission = true
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "Use current location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            MHikeTextField(
                value = location.name,
                onValueChange = { newName ->
                    onLocationChange(
                        location.copy(
                            name = newName,
                            manualOverride = true
                        )
                    )
                },
                label = "Place Name",
                placeholder = "e.g., Mount Hood Trail",
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MHikeTextField(
                    value = String.format("%.6f", location.coordinates.latitude),
                    onValueChange = { lat ->
                        lat.toDoubleOrNull()?.let { latitude ->
                            // Validate latitude is in range [-90, 90]
                            if (latitude in -90.0..90.0) {
                                onLocationChange(
                                    location.copy(
                                        coordinates = GeoPoint(latitude, location.coordinates.longitude),
                                        manualOverride = true
                                    )
                                )
                            }
                        }
                    },
                    label = "Latitude",
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )

                MHikeTextField(
                    value = String.format("%.6f", location.coordinates.longitude),
                    onValueChange = { lon ->
                        lon.toDoubleOrNull()?.let { longitude ->
                            // Validate longitude is in range [-180, 180]
                            if (longitude in -180.0..180.0) {
                                onLocationChange(
                                    location.copy(
                                        coordinates = GeoPoint(location.coordinates.latitude, longitude),
                                        manualOverride = true
                                    )
                                )
                            }
                        }
                    },
                    label = "Longitude",
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }

            error?.let { errorMsg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (location.manualOverride && !isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Location manually set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
