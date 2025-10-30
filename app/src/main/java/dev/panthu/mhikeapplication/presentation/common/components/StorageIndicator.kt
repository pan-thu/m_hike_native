package dev.panthu.mhikeapplication.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.panthu.mhikeapplication.domain.model.StorageType

/**
 * Small badge indicating where an image is stored
 */
@Composable
fun StorageIndicator(
    storageType: StorageType,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    val (icon, text, color) = when (storageType) {
        StorageType.LOCAL -> Triple(
            Icons.Default.PhoneAndroid,
            "Device only",
            MaterialTheme.colorScheme.tertiaryContainer
        )
        StorageType.FIREBASE -> Triple(
            Icons.Default.Cloud,
            "Cloud backup",
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )

        if (showText) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Informational text about storage location
 */
@Composable
fun StorageInfoText(
    storageType: StorageType,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (storageType) {
        StorageType.LOCAL -> Pair(
            "Images stored on device only",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        StorageType.FIREBASE -> Pair(
            "Images backed up to cloud",
            MaterialTheme.colorScheme.primary
        )
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier
    )
}

/**
 * Combined storage info with icon and detailed message
 */
@Composable
fun StorageInfoCard(
    storageType: StorageType,
    imageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val (icon, text) = when (storageType) {
            StorageType.LOCAL -> Pair(
                Icons.Default.PhoneAndroid,
                "$imageCount image(s) stored on this device"
            )
            StorageType.FIREBASE -> Pair(
                Icons.Default.Cloud,
                "$imageCount image(s) backed up to cloud"
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
