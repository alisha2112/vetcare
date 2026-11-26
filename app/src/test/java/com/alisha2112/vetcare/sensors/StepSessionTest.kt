package com.alisha2112.vetcare.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepSessionTest {

    private fun started(id: Long = 1L) = StepSession().start(id)

    @Test
    fun `new session waits for first value`() {
        val session = started()

        assertEquals(StepSessionStatus.WAITING_FOR_SENSOR, session.status)
        assertTrue(session.isMeasuring)
        assertEquals(0L, session.steps)
    }

    @Test
    fun `first valid value becomes baseline`() {
        val session = started().onSensorValue(15_000f)

        assertEquals(StepSessionStatus.ACTIVE, session.status)
        assertEquals(15_000f, session.baseline)
        assertEquals(0L, session.steps)
    }

    @Test
    fun `steps are difference between current value and baseline`() {
        val session = started()
            .onSensorValue(1_000f)
            .onSensorValue(1_010f)
            .onSensorValue(1_042f)

        assertEquals(42L, session.steps)
    }

    @Test
    fun `repeated value does not change steps`() {
        val session = started()
            .onSensorValue(500f)
            .onSensorValue(520f)
            .onSensorValue(520f)
            .onSensorValue(520f)

        assertEquals(20L, session.steps)
    }

    @Test
    fun `values are ignored when session is not running`() {
        val idle = StepSession().onSensorValue(100f)
        assertEquals(StepSessionStatus.IDLE, idle.status)
        assertNull(idle.baseline)

        val stopped = started()
            .onSensorValue(100f)
            .onSensorValue(110f)
            .stop()
            .onSensorValue(500f)

        assertEquals(StepSessionStatus.STOPPED, stopped.status)
        assertEquals(10L, stopped.steps)
    }

    @Test
    fun `invalid values are ignored`() {
        var session = started()

        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -5f)
            .forEach { session = session.onSensorValue(it) }

        assertEquals(StepSessionStatus.WAITING_FOR_SENSOR, session.status)
        assertNull(session.baseline)

        session = session.onSensorValue(200f)
            .onSensorValue(Float.NaN)
            .onSensorValue(-1f)
            .onSensorValue(205f)

        assertEquals(5L, session.steps)
    }

    @Test
    fun `decrease of cumulative value keeps counted steps and sets new baseline`() {
        val session = started()
            .onSensorValue(1_000f)
            .onSensorValue(1_030f) // 30 кроків
            .onSensorValue(5f)     // апаратний лічильник скинуто
            .onSensorValue(12f)    // ще 7 кроків

        assertEquals(37L, session.steps)
        assertTrue(session.steps >= 0)
    }

    @Test
    fun `stop keeps result`() {
        val session = started()
            .onSensorValue(10f)
            .onSensorValue(60f)
            .stop()

        assertEquals(StepSessionStatus.STOPPED, session.status)
        assertFalse(session.isMeasuring)
        assertEquals(50L, session.steps)
    }

    @Test
    fun `reset returns to idle with zero steps`() {
        val session = started()
            .onSensorValue(10f)
            .onSensorValue(60f)
            .stop()
            .reset()

        assertEquals(StepSessionStatus.IDLE, session.status)
        assertEquals(0L, session.steps)
        assertEquals(0L, session.sessionId)
    }

    @Test
    fun `restart does not count steps made between sessions`() {
        val first = started(1L)
            .onSensorValue(100f)
            .onSensorValue(120f)
            .stop()

        val second = first.start(2L)
            .onSensorValue(400f) // 280 кроків між сеансами
            .onSensorValue(415f)

        assertEquals(2L, second.sessionId)
        assertEquals(15L, second.steps)
    }

    @Test
    fun `suspended segment does not count steps made in background`() {
        val session = started()
            .onSensorValue(100f)
            .onSensorValue(140f) // 40 кроків
            .suspendSegment()

        assertEquals(StepSessionStatus.WAITING_FOR_SENSOR, session.status)
        assertEquals(40L, session.steps)

        val resumed = session
            .onSensorValue(900f) // нова база після відновлення
            .onSensorValue(910f)

        assertEquals(50L, resumed.steps)
    }

    @Test
    fun `stop and suspend without values give zero steps`() {
        assertEquals(0L, started().suspendSegment().steps)
        assertEquals(0L, started().stop().steps)
    }

    @Test
    fun `fractional sensor values are truncated to whole steps`() {
        val session = started()
            .onSensorValue(10f)
            .onSensorValue(12.9f)

        assertEquals(2L, session.steps)
    }
}
