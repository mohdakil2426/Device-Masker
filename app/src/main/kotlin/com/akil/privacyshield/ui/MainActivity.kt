package com.akil.privacyshield.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akil.privacyshield.PrivacyShieldApp
import com.akil.privacyshield.ui.theme.PrivacyShieldTheme
import timber.log.Timber

/**
 * Main Activity for PrivacyShield.
 *
 * Uses Jetpack Compose for the entire UI with edge-to-edge display. The activity serves as the
 * container for the Compose navigation graph.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        Timber.d("MainActivity created, module active: ${PrivacyShieldApp.isXposedModuleActive}")

        setContent {
            PrivacyShieldTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // TODO: Replace with NavHost in Phase 5
                    PrivacyShieldPlaceholder(
                            moduleActive = PrivacyShieldApp.isXposedModuleActive,
                            modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/** Temporary placeholder UI until navigation is implemented in Phase 5. */
@Composable
fun PrivacyShieldPlaceholder(moduleActive: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                    text = "🛡️ PrivacyShield",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
            )
            Text(
                    text = if (moduleActive) "✅ Module Active" else "❌ Module Inactive",
                    style = MaterialTheme.typography.titleMedium,
                    color =
                            if (moduleActive) {
                                Color(0xFF4CAF50)
                            } else {
                                Color(0xFFFF5722)
                            }
            )
            Text(
                    text = "Phase 1: Core Infrastructure Complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PrivacyShieldPlaceholderPreview() {
    PrivacyShieldTheme { PrivacyShieldPlaceholder(moduleActive = true) }
}
