package com.yapperzdelight.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yapperzdelight.android.ui.Yapper2DashboardScreen
import com.yapperzdelight.android.ui.theme.Yapper2Theme
import com.yapperzdelight.android.ui.theme.YapperInkBackground

/**
 * Canonical entry Activity for Yapper 2.0 Android.
 * Integrates the Trybe UGC revision studio, TikTok Shop creator looks,
 * adaptive foldable/phone scaffold, and Google Play Data Safety compliance.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Yapper2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = YapperInkBackground
                ) {
                    Yapper2DashboardScreen()
                }
            }
        }
    }
}
