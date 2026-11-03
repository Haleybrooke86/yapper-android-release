package com.yapper.android.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    COMPACT,  // Standard Phones (< 600dp)
    MEDIUM,   // Foldables / Small Tablets (600dp - 839dp)
    EXPANDED  // Tablets / Large Foldables / Desktop (>= 840dp)
}

/**
 * Adaptive layout scaffolding for Yapper Android.
 * Renders a single-column flow on standard phones, and automatically elevates to a
 * two-pane List-Detail layout on foldables and tablets.
 */
@Composable
fun YapperAdaptiveScaffold(
    windowWidthClass: WindowWidthClass,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    when (windowWidthClass) {
        WindowWidthClass.COMPACT -> {
            // Standard Phone: Stacked single-column
            Box(modifier = modifier.fillMaxSize()) {
                listPane()
            }
        }
        WindowWidthClass.MEDIUM, WindowWidthClass.EXPANDED -> {
            // Tablet / Foldable: Two-pane split layout
            Row(modifier = modifier.fillMaxSize()) {
                // Left Pane: My Yaps & Clip Reordering (380dp fixed or 40% width)
                Box(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    listPane()
                }

                // Right Pane: 4K HDR Preview & Live Correction Controls
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp)
                ) {
                    detailPane()
                }
            }
        }
    }
}
