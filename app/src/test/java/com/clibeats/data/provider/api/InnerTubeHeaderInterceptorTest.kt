@file:Suppress("ForbiddenImport")

package com.clibeats.data.provider.api

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class InnerTubeHeaderInterceptorTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            OkHttpClient.Builder()
                .addInterceptor(InnerTubeHeaderInterceptor())
                .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `interceptor attaches YouTube Music headers`() {
        server.enqueue(MockResponse().setBody("{}"))

        val request = Request.Builder().url(server.url("/test")).build()
        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        assertThat(recordedRequest.getHeader("User-Agent")).isNotNull()
        assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json")
    }
}
