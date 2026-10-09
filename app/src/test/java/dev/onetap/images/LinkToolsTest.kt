package dev.onetap.images

import dev.onetap.images.core.LinkTools
import dev.onetap.images.core.Platform
import org.junit.Assert.*
import org.junit.Test

class LinkToolsTest {
    @Test fun recognizesLongShareText() {
        assertEquals("https://xhslink.com/A12b", LinkTools.firstUrl("复制打开小红书， https://xhslink.com/A12b 发现作品"))
        assertEquals(Platform.XHS, LinkTools.platform("https://xhslink.com/A12b"))
    }
    @Test fun recognizesPlatforms() {
        assertEquals(Platform.DOUYIN, LinkTools.platform("https://v.douyin.com/abc/"))
        assertEquals(Platform.X, LinkTools.platform("https://mobile.twitter.com/a/status/12345"))
        assertEquals(Platform.THREADS, LinkTools.platform("https://www.threads.net/@a/post/abc"))
        assertNull(LinkTools.platform("https://notx.com/a/status/12345"))
    }
    @Test fun extractsOnlyStatusId() {
        assertEquals("1234567890", LinkTools.xStatusId("https://x.com/abc/status/1234567890/photo/1"))
        assertNull(LinkTools.xStatusId("https://x.com/abc"))
    }
    @Test fun keepsNamesSafe() { assertEquals("hello_world", LinkTools.safeId("hello/world")) }
}
