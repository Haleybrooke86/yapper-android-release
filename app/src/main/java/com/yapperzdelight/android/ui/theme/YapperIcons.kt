package com.yapperzdelight.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * One hand drawn line icon set (24 grid, 1.75 stroke, round caps), the same shapes the web tab bar uses.
 * Replaces emoji as interface icons. Tinted by Icon(), so they follow the theme in light and dark.
 */
object YapperIcons {

    private fun line(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private fun solid(name: String, d: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)).build()

    val NewYap: ImageVector by lazy {
        line(
            "NewYap",
            "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1 -4 4H7a4 4 0 0 1 -4 -4V7a4 4 0 0 1 4 -4z",
            "M12 8v8",
            "M8 12h8",
        )
    }

    val MyYaps: ImageVector by lazy {
        line(
            "MyYaps",
            "M5.5 4h3a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1 -1.5 1.5h-3a1.5 1.5 0 0 1 -1.5 -1.5v-3a1.5 1.5 0 0 1 1.5 -1.5z",
            "M15.5 4h3a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1 -1.5 1.5h-3a1.5 1.5 0 0 1 -1.5 -1.5v-3a1.5 1.5 0 0 1 1.5 -1.5z",
            "M5.5 14h3a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1 -1.5 1.5h-3a1.5 1.5 0 0 1 -1.5 -1.5v-3a1.5 1.5 0 0 1 1.5 -1.5z",
            "M15.5 14h3a1.5 1.5 0 0 1 1.5 1.5v3a1.5 1.5 0 0 1 -1.5 1.5h-3a1.5 1.5 0 0 1 -1.5 -1.5v-3a1.5 1.5 0 0 1 1.5 -1.5z",
        )
    }

    val Plus: ImageVector by lazy { line("Plus", "M12 5v14", "M5 12h14") }

    val Play: ImageVector by lazy {
        solid("Play", "M8 5.6v12.8a1 1 0 0 0 1.53 0.85l10.2 -6.4a1 1 0 0 0 0 -1.7L9.53 4.75A1 1 0 0 0 8 5.6z")
    }

    val Share: ImageVector by lazy {
        line("Share", "M12 3v12", "M8 7l4 -4l4 4", "M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-7")
    }

    val Save: ImageVector by lazy {
        line("Save", "M12 3v12", "M8 11l4 4l4 -4", "M5 20h14")
    }

    val Close: ImageVector by lazy { line("Close", "M6 6l12 12", "M18 6L6 18") }

    val Help: ImageVector by lazy {
        line(
            "Help",
            "M12 3a9 9 0 1 1 0 18a9 9 0 1 1 0 -18z",
            "M9.6 9.2a2.5 2.5 0 1 1 3.4 2.3c-0.6 0.3 -1 0.9 -1 1.6v0.4",
            "M12 16.8v0.01",
        )
    }

    val OpenExternal: ImageVector by lazy {
        line(
            "OpenExternal",
            "M14 4h6v6",
            "M20 4l-9 9",
            "M18 14v5a1 1 0 0 1 -1 1H5a1 1 0 0 1 -1 -1V7a1 1 0 0 1 1 -1h5",
        )
    }

    val Check: ImageVector by lazy { line("Check", "M5 12.5l4.5 4.5L19 7.5") }

    val Trash: ImageVector by lazy {
        line(
            "Trash",
            "M4 7h16",
            "M10 11v6",
            "M14 11v6",
            "M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2 -2l1 -12",
            "M9 7V4h6v3",
        )
    }

    val Film: ImageVector by lazy {
        line(
            "Film",
            "M6 4h12a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2H6a2 2 0 0 1 -2 -2V6a2 2 0 0 1 2 -2z",
            "M8 4v16",
            "M16 4v16",
            "M4 9h4",
            "M4 15h4",
            "M16 9h4",
            "M16 15h4",
        )
    }

    val ArrowLeft: ImageVector by lazy { line("ArrowLeft", "M19 12H5", "M11 6l-6 6l6 6") }
}
