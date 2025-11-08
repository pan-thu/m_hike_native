package dev.panthu.mhikeapplication.presentation.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import dev.panthu.mhikeapplication.domain.model.ImageMetadata

/**
 * Grid display for images with delete functionality
 * @param images List of image metadata to display
 * @param onImageClick Callback when an image is clicked
 * @param onDeleteClick Optional callback when delete button is clicked
 * @param canDelete Whether delete button should be shown
 * @param modifier Modifier for the grid
 */
@Composable
fun ImageGrid(
    images: List<ImageMetadata>,
    onImageClick: (ImageMetadata) -> Unit,
    onDeleteClick: ((ImageMetadata) -> Unit)? = null,
    canDelete: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No images yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        // Use regular Column with Row layout instead of LazyVerticalGrid
        // This avoids nesting scrollable containers (LazyGrid inside verticalScroll)
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group images into rows of 3
            images.chunked(3).forEach { rowImages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowImages.forEach { image ->
                        Box(modifier = Modifier.weight(1f)) {
                            ImageGridItem(
                                image = image,
                                onClick = { onImageClick(image) },
                                onDeleteClick = if (canDelete && onDeleteClick != null) {
                                    { onDeleteClick(image) }
                                } else null
                            )
                        }
                    }
                    // Fill remaining spaces in incomplete rows
                    repeat(3 - rowImages.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Individual image grid item with optional delete button
 */
@Composable
private fun ImageGridItem(
    image: ImageMetadata,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image
            SubcomposeAsyncImage(
                model = image.thumbnailUrl.ifEmpty { image.url },
                contentDescription = "Hike image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = "Failed to load image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )

            // Delete button (if allowed)
            onDeleteClick?.let { deleteClick ->
                IconButton(
                    onClick = deleteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Delete image",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simplified image list for read-only display
 */
@Composable
fun ImageList(
    images: List<ImageMetadata>,
    onImageClick: (ImageMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    ImageGrid(
        images = images,
        onImageClick = onImageClick,
        canDelete = false,
        modifier = modifier
    )
}
