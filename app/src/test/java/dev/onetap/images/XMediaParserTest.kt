package dev.onetap.images

import dev.onetap.images.core.Extraction
import dev.onetap.images.extract.XMediaParser
import org.junit.Assert.*
import org.junit.Test

class XMediaParserTest {
    private val sample = """
        {"id_str":"12345","mediaDetails":[
           {"id_str":"aa","type":"photo","media_url_https":"https://pbs.twimg.com/media/abc.jpg"},
           {"id_str":"bb","type":"video","media_url_https":"https://pbs.twimg.com/media/video.jpg"},
           {"id_str":"cc","type":"photo","media_url_https":"https://other.example/media.jpg"},
           {"id_str":"dd","type":"photo","media_url_https":"https://pbs.twimg.com/media/xyz.png"}
        ], "quoted_tweet":{"mediaDetails":[{"type":"photo","media_url_https":"https://pbs.twimg.com/media/quoted.jpg"}]}}
    """.trimIndent()

    @Test fun acceptsOnlyRootPhotosFromMediaHost() {
        val result = XMediaParser.parse("12345", sample) as Extraction.Success
        assertEquals(2, result.post.images.size)
        assertTrue(result.post.images[0].source.endsWith("?format=jpg&name=orig"))
        assertTrue(result.post.images[1].source.endsWith("?format=png&name=orig"))
        assertFalse(result.post.images.any { "quoted" in it.source })
    }
    @Test fun rejectsUnexpectedTweet() { assertTrue(XMediaParser.parse("99999", sample) is Extraction.Failure) }
    @Test fun doesntFallBackToVideoCover() {
        val onlyVideo = """{"id_str":"12345","mediaDetails":[{"type":"video","media_url_https":"https://pbs.twimg.com/media/cover.jpg"}]}"""
        assertTrue(XMediaParser.parse("12345", onlyVideo) is Extraction.Failure)
    }
}
