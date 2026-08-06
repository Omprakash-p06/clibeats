package com.clibeats.data.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NetworkMonitorTest {
    @Test
    fun `NetworkMonitor class structure test`() {
        assertThat(NetworkMonitor::class.java).isNotNull()
    }
}
