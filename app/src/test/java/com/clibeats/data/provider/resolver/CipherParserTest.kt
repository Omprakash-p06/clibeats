package com.clibeats.data.provider.resolver

import com.clibeats.data.provider.resolver.cipher.CipherParser
import com.clibeats.data.provider.resolver.cipher.SignedUrlBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CipherParserTest {
    @Test
    fun `parse parses url and signature cipher parameters correctly`() {
        val cipherStr = "url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fid%3D123&s=abc123DEF&sp=sig"
        val parsed = CipherParser.parse(cipherStr)

        assertThat(parsed).isNotNull()
        assertThat(parsed?.url).isEqualTo("https://googlevideo.com/videoplayback?id=123")
        assertThat(parsed?.s).isEqualTo("abc123DEF")
        assertThat(parsed?.sp).isEqualTo("sig")
    }

    @Test
    fun `SignedUrlBuilder appends deciphered signature correctly`() {
        val cipherStr = "url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fid%3D123&s=abc123DEF&sp=sig"
        val parsed = CipherParser.parse(cipherStr)!!

        val signedUrl = SignedUrlBuilder.build(parsed, "DECIPHERED_SIG")
        assertThat(signedUrl).isEqualTo("https://googlevideo.com/videoplayback?id=123&sig=DECIPHERED_SIG")
    }
}
