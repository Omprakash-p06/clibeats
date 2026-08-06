package com.clibeats.presentation.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerBarTest {
    @Test
    fun `PlayerBar component definition test`() {
        val componentPackage = "com.clibeats.presentation.component.PlayerBarKt"
        val clazz = Class.forName(componentPackage)
        assertThat(clazz).isNotNull()
    }
}
