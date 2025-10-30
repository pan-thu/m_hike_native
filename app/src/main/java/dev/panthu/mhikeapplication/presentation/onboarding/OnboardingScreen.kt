package dev.panthu.mhikeapplication.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.panthu.mhikeapplication.presentation.common.components.MHikePrimaryButton
import dev.panthu.mhikeapplication.presentation.common.components.MHikeSecondaryButton

/**
 * Onboarding screen shown on first app launch
 * Allows user to continue as guest or sign up/sign in
 */
@Composable
fun OnboardingScreen(
    onContinueAsGuest: () -> Unit,
    onSignUp: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // App branding
        Text(
            text = "M-Hike",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Plan hikes, record observations, share adventures",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Feature comparison card
        FeatureComparisonCard()

        Spacer(modifier = Modifier.height(32.dp))

        // Call-to-action buttons
        MHikePrimaryButton(
            text = "Sign Up",
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        MHikeSecondaryButton(
            text = "Sign In",
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Guest mode option
        TextButton(
            onClick = onContinueAsGuest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Continue as Guest",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your guest data will be saved when you sign up later",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureComparisonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Choose Your Experience",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Guest features
            FeatureSection(
                title = "Guest Mode",
                subtitle = "Offline-only features",
                features = listOf(
                    FeatureItem("Create hikes & observations", true),
                    FeatureItem("Add photos (stored locally)", true),
                    FeatureItem("Search & filter hikes", true),
                    FeatureItem("Share with other users", false),
                    FeatureItem("Cloud backup", false)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(20.dp))

            // Authenticated features
            FeatureSection(
                title = "With Account",
                subtitle = "Full cloud features",
                features = listOf(
                    FeatureItem("All guest features", true),
                    FeatureItem("Share with other users", true),
                    FeatureItem("Cloud backup & sync", true),
                    FeatureItem("Access from any device", true),
                    FeatureItem("Find other hikers", true)
                ),
                highlighted = true
            )
        }
    }
}

@Composable
private fun FeatureSection(
    title: String,
    subtitle: String,
    features: List<FeatureItem>,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (highlighted) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "RECOMMENDED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        features.forEach { feature ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (feature.available) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (feature.available) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = feature.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (feature.available) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private data class FeatureItem(
    val text: String,
    val available: Boolean
)
