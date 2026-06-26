package com.wfx.warungpos.data.remote.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictResolverTest {

    private val resolver = ConflictResolver()

    @Test
    fun `accept when no existing record`() {
        val result = resolver.resolve(incomingUpdatedAt = 1000L, existingUpdatedAt = null)
        assertEquals(ConflictResolution.ACCEPT, result)
    }

    @Test
    fun `accept when incoming is newer`() {
        val result = resolver.resolve(incomingUpdatedAt = 2000L, existingUpdatedAt = 1000L)
        assertEquals(ConflictResolution.ACCEPT, result)
    }

    @Test
    fun `reject when incoming is older`() {
        val result = resolver.resolve(incomingUpdatedAt = 500L, existingUpdatedAt = 1000L)
        assertEquals(ConflictResolution.REJECT, result)
    }

    @Test
    fun `reject when timestamps are equal`() {
        val result = resolver.resolve(incomingUpdatedAt = 1000L, existingUpdatedAt = 1000L)
        assertEquals(ConflictResolution.REJECT, result)
    }

    @Test
    fun `reject when incoming updatedAt is null`() {
        val result = resolver.resolve(incomingUpdatedAt = null, existingUpdatedAt = 1000L)
        assertEquals(ConflictResolution.REJECT, result)
    }

    @Test
    fun `reject status regression PAID to OPEN`() {
        val result = resolver.resolve(
            incomingUpdatedAt = 2000L,
            existingUpdatedAt = 1000L,
            existingStatus = "PAID",
            incomingStatus = "OPEN",
        )
        assertEquals(ConflictResolution.REJECT, result)
    }

    @Test
    fun `reject status regression VOID to PAID`() {
        val result = resolver.resolve(
            incomingUpdatedAt = 2000L,
            existingUpdatedAt = 1000L,
            existingStatus = "VOID",
            incomingStatus = "PAID",
        )
        assertEquals(ConflictResolution.REJECT, result)
    }

    @Test
    fun `accept forward status progression OPEN to PAID`() {
        val result = resolver.resolve(
            incomingUpdatedAt = 2000L,
            existingUpdatedAt = 1000L,
            existingStatus = "OPEN",
            incomingStatus = "PAID",
        )
        assertEquals(ConflictResolution.ACCEPT, result)
    }

    @Test
    fun `accept forward status progression OPEN to VOID`() {
        val result = resolver.resolve(
            incomingUpdatedAt = 2000L,
            existingUpdatedAt = 1000L,
            existingStatus = "OPEN",
            incomingStatus = "VOID",
        )
        assertEquals(ConflictResolution.ACCEPT, result)
    }

    @Test
    fun `non-bill statuses do not block LWW`() {
        // Status strings not in the OPEN/PAID/VOID order list are treated as non-regressions
        val result = resolver.resolve(
            incomingUpdatedAt = 2000L,
            existingUpdatedAt = 1000L,
            existingStatus = "SYNCED",
            incomingStatus = "PENDING",
        )
        assertEquals(ConflictResolution.ACCEPT, result)
    }
}
