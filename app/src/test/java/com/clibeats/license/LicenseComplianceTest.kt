package com.clibeats.license

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class LicenseComplianceTest {
    @Test
    fun `license documentation file exists and contains essential attributions`() {
        val rootDir = File(".").canonicalFile.parentFile ?: File(".")
        val licenseFile = File(rootDir, "docs/LICENSES.md")
        assertThat(licenseFile.exists()).isTrue()

        val content = licenseFile.readText()
        assertThat(content).contains("Apache License 2.0")
        assertThat(content).contains("AndroidX")
        assertThat(content).contains("Media3")
        assertThat(content).contains("OkHttp")
    }
}
