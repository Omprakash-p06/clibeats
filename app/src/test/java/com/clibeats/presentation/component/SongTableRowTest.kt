package com.clibeats.presentation.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SongTableRowTest {
    @Test
    fun `SongTableRow component definition test`() {
        val componentPackage = "com.clibeats.presentation.component.SongTableRowKt"
        val clazz = Class.forName(componentPackage)
        assertThat(clazz).isNotNull()
    }
}
