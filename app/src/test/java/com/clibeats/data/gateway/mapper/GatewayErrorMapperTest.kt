package com.clibeats.data.gateway.mapper

import com.google.common.truth.Truth.assertThat
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class GatewayErrorMapperTest {
    private fun httpException(
        code: Int,
        body: String,
    ): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody(null)))

    @Test
    fun `maps RATE_LIMITED error code`() {
        val ex = httpException(429, """{"error":{"code":"RATE_LIMITED","message":"too many","providerId":"youtube"}}""")
        assertThat(GatewayErrorMapper.message(ex)).isEqualTo("Rate limit exceeded, try again shortly")
    }

    @Test
    fun `maps GEO_BLOCKED error code`() {
        val ex = httpException(403, """{"error":{"code":"GEO_BLOCKED","message":"geo","providerId":"youtube"}}""")
        assertThat(GatewayErrorMapper.message(ex)).isEqualTo("This track is not available in your region")
    }

    @Test
    fun `maps NOT_FOUND error code`() {
        val ex = httpException(404, """{"error":{"code":"NOT_FOUND","message":"nope","providerId":"youtube"}}""")
        assertThat(GatewayErrorMapper.message(ex)).isEqualTo("Track not found")
    }

    @Test
    fun `maps PLAYBACK_ERROR error code`() {
        val ex = httpException(502, """{"error":{"code":"PLAYBACK_ERROR","message":"boom","providerId":"youtube"}}""")
        assertThat(GatewayErrorMapper.message(ex)).isEqualTo("Unable to play this track")
    }

    @Test
    fun `maps NETWORK_ERROR error code`() {
        val ex = httpException(503, """{"error":{"code":"NETWORK_ERROR","message":"down","providerId":"youtube"}}""")
        assertThat(GatewayErrorMapper.message(ex)).isEqualTo("Network error, check your connection")
    }

    @Test
    fun `falls back to throwable message for unknown code`() {
        val ex = httpException(500, """{"error":{"code":"INTERNAL_ERROR","message":"weird","providerId":"gateway"}}""")
        assertThat(GatewayErrorMapper.message(ex)).isEqualTo("weird")
    }

    @Test
    fun `falls back to generic message for non-http errors`() {
        val ex = RuntimeException("boom")
        assertThat(GatewayErrorMapper.message(ex)).isEqualTo("boom")
    }
}