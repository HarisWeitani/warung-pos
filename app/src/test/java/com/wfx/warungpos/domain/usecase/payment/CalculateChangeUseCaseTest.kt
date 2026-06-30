package com.wfx.warungpos.domain.usecase.payment

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateChangeUseCaseTest {

    private val useCase = CalculateChangeUseCase()

    @Test
    fun `tendered greater than total returns positive change`() {
        assertEquals(5_000L, useCase(50_000L, 45_000L))
    }

    @Test
    fun `tendered equal to total returns zero`() {
        assertEquals(0L, useCase(45_000L, 45_000L))
    }

    @Test
    fun `tendered less than total returns negative change`() {
        assertEquals(-5_000L, useCase(40_000L, 45_000L))
    }

    @Test
    fun `zero tendered returns negative of total`() {
        assertEquals(-45_000L, useCase(0L, 45_000L))
    }
}
