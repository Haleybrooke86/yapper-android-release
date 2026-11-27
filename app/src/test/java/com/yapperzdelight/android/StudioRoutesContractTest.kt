package com.yapperzdelight.android

import com.yapperzdelight.android.ui.CreatorLook
import com.yapperzdelight.android.ui.StudioTool
import com.yapperzdelight.android.ui.YapperTutorialSteps
import com.yapperzdelight.android.ui.extractDeepLinkJobId
import com.yapperzdelight.android.ui.formatDuration
import com.yapperzdelight.android.ui.theme.YapperTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioRoutesContractTest {

    @Test
    fun studioOriginAndCreatorLooksMatchLiveWebRoutes() {
        assertEquals("https://yapperzdelight.com", YapperTokens.STUDIO_ORIGIN)
        assertEquals("journey=storytime", CreatorLook.STORYTIME.studioQuery)
        assertEquals("journey=lifestyle", CreatorLook.LIFESTYLE.studioQuery)
        assertEquals("journey=tiktok_shop", CreatorLook.TIKTOK_SHOP.studioQuery)
        assertEquals("fresh=1", CreatorLook.JUST_EDIT.studioQuery)
        CreatorLook.entries.forEach { look ->
            assertEquals(3, look.cues.size)
            look.cues.forEach { cue ->
                assertFalse("Zero em-dashes in cues", cue.contains('\u2014'))
            }
        }
    }

    @Test
    fun studioToolsCoverTwinSplitTeleprompterTrybeAndQuietLuxury() {
        assertEquals("/app/new?format=side_by_side", StudioTool.TWIN_SPLIT.studioPath)
        assertEquals("New", StudioTool.TWIN_SPLIT.badge)
        assertEquals("/app/new?teleprompter=1", StudioTool.TELEPROMPTER.studioPath)
        assertEquals("New", StudioTool.TELEPROMPTER.badge)
        assertEquals("/app/trybe", StudioTool.TRYBE_UGC.studioPath)
        assertEquals("/app/style", StudioTool.STYLE_LOOKS.studioPath)

        StudioTool.entries.forEach { tool ->
            assertTrue(tool.studioPath.startsWith("/app/"))
            assertFalse("Zero em-dashes in StudioTool copy", tool.subtitle.contains('\u2014'))
        }

        val tourText = YapperTutorialSteps.joinToString(" ") { "${it.title} ${it.description}" }
        assertTrue(tourText.contains("Twin Split Studio"))
        assertTrue(tourText.contains("Teleprompter Camera"))
        assertTrue(tourText.contains("Trybe & UGC"))
        assertTrue(tourText.contains("Style & Quiet Luxury"))
        assertFalse(tourText.contains('\u2014'))
    }

    @Test
    fun extractDeepLinkJobIdAcceptsValidYapLinksAndRejectsUntrustedInputs() {
        val uuid = "6c0ec06a-3f9b-4b12-9c32-1b8ef902d1a4"
        assertEquals(uuid, extractDeepLinkJobId("yapper://yap/$uuid"))
        assertEquals(uuid, extractDeepLinkJobId("https://yapperzdelight.com/yap/$uuid"))
        assertEquals(uuid, extractDeepLinkJobId("https://www.yapperzdelight.com/yap/$uuid?utm_source=share#top"))
        assertEquals("26e4f382", extractDeepLinkJobId("yapper://yap/26e4f382/"))

        assertNull(extractDeepLinkJobId(null))
        assertNull(extractDeepLinkJobId(""))
        assertNull(extractDeepLinkJobId("https://evil.example.com/yap/$uuid"))
        assertNull(extractDeepLinkJobId("https://yapperzdelight.com/yap/$uuid/extra"))
        assertNull(extractDeepLinkJobId("yapper://yap/../secret"))
        assertNull(extractDeepLinkJobId("yapper://yap/ab"))
    }

    @Test
    fun formatDurationMatchesStudioTimestampFormat() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:00", formatDuration(-500L))
        assertEquals("0:07", formatDuration(6_800L))
        assertEquals("1:05", formatDuration(65_100L))
    }
}
