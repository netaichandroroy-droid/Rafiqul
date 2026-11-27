package com.example

import com.example.data.GitHubReleaseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GitHubReleaseParserTest {

    @Test
    fun `parses latest GitHub release json with apk asset and build code`() {
        val sampleJson = """
            {
              "tag_name": "v1.0.3-b13",
              "name": "Rafiqul Islam v1.0.3 (Build 13)",
              "draft": false,
              "html_url": "https://github.com/netaichandroroy-droid/Netai-/releases/tag/v1.0.3-b13",
              "body": "Build Code: 13\nVersion: 1.0.3\nCommit: ledger single card update (abc1234)",
              "assets": [
                {
                  "name": "Rafiqul-Islam-v1.0.3-b13.apk",
                  "size": 18245012,
                  "browser_download_url": "https://github.com/netaichandroroy-droid/Netai-/releases/download/v1.0.3-b13/Rafiqul-Islam-v1.0.3-b13.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = GitHubReleaseParser.parseReleaseResponse(sampleJson)
        assertNotNull(parsed)
        assertEquals("v1.0.3-b13", parsed?.tagName)
        assertEquals(13, parsed?.versionCode)
        assertEquals("1.0.3", parsed?.versionName)
        assertEquals(
            "https://github.com/netaichandroroy-droid/Netai-/releases/download/v1.0.3-b13/Rafiqul-Islam-v1.0.3-b13.apk",
            parsed?.apkDownloadUrl
        )
        assertEquals("ledger single card update (abc1234)", parsed?.summary)
    }

    @Test
    fun `parses array response and skips drafts`() {
        val draftJson = """[{"tag_name":"v1.0.4-b14","draft":true,"assets":[]}]"""
        assertNull(GitHubReleaseParser.parseReleaseResponse(draftJson))

        val arrayJson = """
            [
              {
                "tag_name": "v1.0.4-b14",
                "name": "Build 14",
                "draft": false,
                "html_url": "https://github.com/netaichandroroy-droid/Netai-/releases/tag/v1.0.4-b14",
                "body": "Latest CI APK",
                "assets": [
                  {
                    "name": "app-debug.apk",
                    "size": 1000,
                    "browser_download_url": "https://github.com/netaichandroroy-droid/Netai-/releases/download/v1.0.4-b14/app-debug.apk"
                  }
                ]
              }
            ]
        """.trimIndent()

        val parsed = GitHubReleaseParser.parseReleaseResponse(arrayJson)
        assertNotNull(parsed)
        assertEquals(14, parsed?.versionCode)
        assertEquals("1.0.4", parsed?.versionName)
    }

    @Test
    fun `compares version code and semantic version names accurately`() {
        assertTrue(GitHubReleaseParser.isNewerRelease(1, "1.0", 13, "1.0.3"))
        assertTrue(GitHubReleaseParser.isNewerRelease(2, "1.1", 13, "1.0.3"))
        assertFalse(GitHubReleaseParser.isNewerRelease(13, "1.0.3", 13, "1.0.3"))
        assertFalse(GitHubReleaseParser.isNewerRelease(14, "1.0.4", 13, "1.0.3"))

        assertTrue(GitHubReleaseParser.isNewerRelease(2, "1.1", null, "1.2.0"))
        assertFalse(GitHubReleaseParser.isNewerRelease(2, "1.2", null, "1.1.9"))
    }
}
