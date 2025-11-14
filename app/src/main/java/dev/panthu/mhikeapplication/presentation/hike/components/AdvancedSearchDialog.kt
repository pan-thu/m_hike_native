package dev.panthu.mhikeapplication.presentation.hike.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class to hold advanced search criteria
 */
data class AdvancedSearchCriteria(
    val name: String = "",
    val location: String = "",
    val minLength: String = "",
    val maxLength: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null
)

/**
 * Advanced search dialog that allows users to filter hikes by multiple criteria:
 * - Name
 * - Location
 * - Length range (min/max)
 * - Date range (start/end)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSearchDialog(
    initialCriteria: AdvancedSearchCriteria = AdvancedSearchCriteria(),
    onDismiss: () -> Unit,
    onSearch: (AdvancedSearchCriteria) -> Unit,
    onClear: () -> Unit
) {
    var name by remember { mutableStateOf(initialCriteria.name) }
    var location by remember { mutableStateOf(initialCriteria.location) }
    var minLength by remember { mutableStateOf(initialCriteria.minLength) }
    var maxLength by remember { mutableStateOf(initialCriteria.maxLength) }
    var startDate by remember { mutableStateOf(initialCriteria.startDate) }
    var endDate by remember { mutableStateOf(initialCriteria.endDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Date format for display
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Date pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = datePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = datePickerState.selectedDateMillis
                    showEndDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Advanced Search")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hike Name") },
                    placeholder = { Text("e.g., Snowdon") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Location field
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("e.g., Wales, UK") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Length range section
                Text(
                    text = "Length Range (km)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Min length
                    OutlinedTextField(
                        value = minLength,
                        onValueChange = { minLength = it },
                        label = { Text("Min") },
                        placeholder = { Text("0") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("km") }
                    )

                    // Max length
                    OutlinedTextField(
                        value = maxLength,
                        onValueChange = { maxLength = it },
                        label = { Text("Max") },
                        placeholder = { Text("100") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text("km") }
                    )
                }

                // Date range section
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start date button
                    OutlinedTextField(
                        value = startDate?.let { dateFormat.format(Date(it)) } ?: "",
                        onValueChange = { },
                        label = { Text("Start Date") },
                        placeholder = { Text("Tap to select") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        readOnly = true,
                        enabled = false,
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
                        },
                        trailingIcon = {
                            if (startDate != null) {
                                IconButton(onClick = { startDate = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear start date")
                                }
                            }
                        },
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // End date button
                    OutlinedTextField(
                        value = endDate?.let { dateFormat.format(Date(it)) } ?: "",
                        onValueChange = { },
                        label = { Text("End Date") },
                        placeholder = { Text("Tap to select") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        readOnly = true,
                        enabled = false,
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
                        },
                        trailingIcon = {
                            if (endDate != null) {
                                IconButton(onClick = { endDate = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear end date")
                                }
                            }
                        },
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Helper text
                Text(
                    text = "Leave fields empty to skip that filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSearch(
                        AdvancedSearchCriteria(
                            name = name.trim(),
                            location = location.trim(),
                            minLength = minLength.trim(),
                            maxLength = maxLength.trim(),
                            startDate = startDate,
                            endDate = endDate
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Search")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onClear()
                    name = ""
                    location = ""
                    minLength = ""
                    maxLength = ""
                    startDate = null
                    endDate = null
                }) {
                    Text("Clear All")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
