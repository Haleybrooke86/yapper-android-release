package com.yapperzdelight.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yapperzdelight.android.ui.Yapper2DashboardScreen
import com.yapperzdelight.android.ui.extractDeepLinkJobId
import com.yapperzdelight.android.ui.theme.Yapper2Theme
import com.yapperzdelight.android.ui.theme.YapperTheme

/**
 * Entry Activity for Yapperz Delight on Android: the same blush, plum and magenta brand as the web studio,
 * following the phone's light or dark setting, and honoring incoming /yap/{jobId} links.
 */
class MainActivity : ComponentActivity() {
    private var deepLinkJobId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkJobId = extractDeepLinkJobId(intent?.dataString)
        enableEdgeToEdge()
        setContent {
            Yapper2Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = YapperTheme.colors.background) {
                    Yapper2DashboardScreen(
                        deepLinkJobId = deepLinkJobId,
                        onDismissDeepLink = { deepLinkJobId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLinkJobId(intent.dataString)?.let { deepLinkJobId = it }
    }
}
