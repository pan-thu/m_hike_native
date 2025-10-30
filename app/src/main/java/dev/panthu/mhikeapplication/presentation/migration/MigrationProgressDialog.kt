package dev.panthu.mhikeapplication.presentation.migration

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.panthu.mhikeapplication.domain.service.MigrationProgress

/**
 * Dialog that displays migration progress when a guest user signs up.
 */
@Composable
fun MigrationProgressDialog(
    migrationState: MigrationProgress?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {}
) {
    if (migrationState == null) return

    Dialog(
        onDismissRequest = {
            // Only allow dismissal when complete or on error
            if (migrationState is MigrationProgress.Complete ||
                migrationState is MigrationProgress.Error
            ) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (migrationState) {
                    is MigrationProgress.Initializing -> {
                        InitializingContent(migrationState)
                    }

                    is MigrationProgress.MigratingHikes -> {
                        MigratingHikesContent(migrationState)
                    }

                    is MigrationProgress.MigratingObservations -> {
                        MigratingObservationsContent(migrationState)
                    }

                    is MigrationProgress.UploadingImages -> {
                        UploadingImagesContent(migrationState)
                    }

                    is MigrationProgress.Complete -> {
                        CompleteContent(migrationState, onDismiss)
                    }

                    is MigrationProgress.Error -> {
                        ErrorContent(migrationState, onDismiss, onRetry)
                    }
                }
            }
        }
    }
}

@Composable
private fun InitializingContent(state: MigrationProgress.Initializing) {
    Text(
        text = "Preparing to migrate your data",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth()
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Found:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "• ${state.stats.totalHikes} hike(s)",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "• ${state.stats.totalObservations} observation(s)",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "• ${state.stats.totalImages} image(s) (~${String.format("%.1f", state.stats.estimatedSizeMB)} MB)",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    Text(
        text = "Please wait while we transfer your data to the cloud...",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun MigratingHikesContent(state: MigrationProgress.MigratingHikes) {
    Text(
        text = "Migrating Hikes",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    LinearProgressIndicator(
        progress = { state.current.toFloat() / state.total },
        modifier = Modifier.fillMaxWidth()
    )

    Text(
        text = "Hike ${state.current} of ${state.total}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )

    Text(
        text = state.hikeName,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

@Composable
private fun MigratingObservationsContent(state: MigrationProgress.MigratingObservations) {
    Text(
        text = "Migrating Observations",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    LinearProgressIndicator(
        progress = { state.current.toFloat() / state.total },
        modifier = Modifier.fillMaxWidth()
    )

    Text(
        text = "Observation ${state.current} of ${state.total}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun UploadingImagesContent(state: MigrationProgress.UploadingImages) {
    Text(
        text = "Uploading Images",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    LinearProgressIndicator(
        progress = { state.progress },
        modifier = Modifier.fillMaxWidth()
    )

    Text(
        text = "Image ${state.current} of ${state.total}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )

    Text(
        text = "${(state.progress * 100).toInt()}% complete",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CompleteContent(
    state: MigrationProgress.Complete,
    onDismiss: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Success",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(64.dp)
    )

    Text(
        text = "Migration Complete!",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (state.result.isSuccessful) {
            Text(
                text = "All your data has been successfully migrated to the cloud!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Migrated:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• ${state.result.migratedHikes} hike(s)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• ${state.result.migratedObservations} observation(s)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• ${state.result.uploadedImages} image(s)",
                style = MaterialTheme.typography.bodySmall
            )
        } else if (state.result.hasPartialSuccess) {
            Text(
                text = "Migration completed with some errors",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Successfully migrated:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• ${state.result.migratedHikes} hike(s)",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• ${state.result.migratedObservations} observation(s)",
                style = MaterialTheme.typography.bodySmall
            )

            if (state.result.failedItems > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${state.result.failedItems} item(s) failed to migrate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Continue")
    }
}

@Composable
private fun ErrorContent(
    state: MigrationProgress.Error,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Error,
        contentDescription = "Error",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(64.dp)
    )

    Text(
        text = "Migration Failed",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error
    )

    Text(
        text = state.message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.retryable) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text("Retry")
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
        ) {
            Text("Dismiss")
        }
    }
}
