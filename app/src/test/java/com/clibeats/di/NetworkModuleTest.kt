@file:Suppress("ForbiddenImport")

package com.clibeats.di

import com.clibeats.BuildConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.URI

/**
 * Config-guard for `BuildConfig.GATEWAY_BASE_URL` (RECOVERY-02).
 *
 * Guards against the regression that broke the release APK: the release
 * buildType used to fall back to the never-registered host
 * `https://gateway.clibeats.io/` (NXDOMAIN on public resolvers), which made
 * every gateway call throw `UnknownHostException` -> `provider_offline`.
 *
 * The build-level guard lives in `app/build.gradle.kts` (release now REQUIRES
 * `GATEWAY_URL` and throws at configuration time instead of defaulting to a
 * dead host). This test locks the contract at the API surface:
 *
 * 1. The embedded URL is a well-formed http(s) URL with a concrete host.
 * 2. The unregistered `gateway.clibeats.io` host can never leak into the
 *    build value again.
 */
class NetworkModuleTest {
    @Test
    fun `GATEWAY_BASE_URL is a well-formed http(url) URL with a host`() {
        val url = BuildConfig.GATEWAY_BASE_URL

        assertThat(url).isNotEmpty()
        val uri = URI(url)
        assertThat(uri.scheme).isAnyOf("http", "https")
        assertThat(uri.host).isNotEmpty()
        assertThat(uri.rawAuthority).isNotEmpty()
    }

    @Test
    fun `GATEWAY_BASE_URL never contains the unregistered clibeats domain`() {
        // The apex clibeats.io is NXDOMAIN on public resolvers (8.8.8.8 / 1.1.1.1),
        // so no host under it can ever resolve. Any build value embedding it is
        // guaranteed-dead config that must fail the guard.
        assertThat(BuildConfig.GATEWAY_BASE_URL).doesNotContain("clibeats.io")
    }
}
