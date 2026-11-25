package com.alisha2112.vetcare.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BubbleLevelMathTest {

    private val g = 9.81f

    /** Показник для нахилу пристрою на заданий кут навколо осі Y (лівий/правий край). */
    private fun tiltedAroundY(degrees: Double) = AccelerationSample(
        x = (g * sin(Math.toRadians(degrees))).toFloat(),
        y = 0f,
        z = (g * cos(Math.toRadians(degrees))).toFloat()
    )

    @Test
    fun `flat device is level with centered bubble`() {
        val reading = BubbleLevelMath.reading(AccelerationSample(0f, 0f, g))!!

        assertEquals(0f, reading.tiltXDegrees, 0.01f)
        assertEquals(0f, reading.tiltYDegrees, 0.01f)
        assertEquals(0f, reading.bubbleX, 0.001f)
        assertEquals(0f, reading.bubbleY, 0.001f)
        assertTrue(reading.isLevel)
    }

    @Test
    fun `small tilt within tolerance is still level`() {
        val reading = BubbleLevelMath.reading(tiltedAroundY(1.0))!!

        assertEquals(1f, reading.tiltXDegrees, 0.05f)
        assertTrue(reading.isLevel)
    }

    @Test
    fun `tilt along X is measured and moves bubble horizontally`() {
        val right = BubbleLevelMath.reading(tiltedAroundY(10.0))!!
        val left = BubbleLevelMath.reading(tiltedAroundY(-10.0))!!

        assertEquals(10f, right.tiltXDegrees, 0.05f)
        assertEquals(-10f, left.tiltXDegrees, 0.05f)
        assertTrue(right.bubbleX > 0f)
        assertTrue(left.bubbleX < 0f)
        assertEquals(0f, right.bubbleY, 0.001f)
        assertFalse(right.isLevel)
    }

    @Test
    fun `raised top edge moves bubble up on screen`() {
        val sample = AccelerationSample(
            x = 0f,
            y = (g * sin(Math.toRadians(15.0))).toFloat(),
            z = (g * cos(Math.toRadians(15.0))).toFloat()
        )
        val reading = BubbleLevelMath.reading(sample)!!

        assertEquals(15f, reading.tiltYDegrees, 0.05f)
        assertTrue(reading.bubbleY < 0f)
        assertEquals(0f, reading.bubbleX, 0.001f)
    }

    @Test
    fun `bubble is clamped inside indicator`() {
        val reading = BubbleLevelMath.reading(AccelerationSample(g, g, 0.5f))!!
        val length = sqrt(reading.bubbleX * reading.bubbleX + reading.bubbleY * reading.bubbleY)

        assertTrue(length <= 1.0001f)
        assertEquals(1f, length, 0.001f)
    }

    @Test
    fun `face down device is not reported as level`() {
        val reading = BubbleLevelMath.reading(AccelerationSample(0f, 0f, -g))!!
        assertFalse(reading.isLevel)
    }

    @Test
    fun `invalid samples are rejected`() {
        assertNull(BubbleLevelMath.reading(AccelerationSample(Float.NaN, 0f, g)))
        assertNull(BubbleLevelMath.reading(AccelerationSample(0f, Float.POSITIVE_INFINITY, g)))
        assertNull(BubbleLevelMath.reading(AccelerationSample(0f, 0f, 0f)))
        assertNull(BubbleLevelMath.reading(AccelerationSample(0f, 0f, 100f)))
    }

    @Test
    fun `screen rotation remaps axes`() {
        val sample = AccelerationSample(1f, 2f, 9f)

        assertEquals(sample, BubbleLevelMath.remapToScreen(sample, 0))
        assertEquals(AccelerationSample(-2f, 1f, 9f), BubbleLevelMath.remapToScreen(sample, 90))
        assertEquals(AccelerationSample(-1f, -2f, 9f), BubbleLevelMath.remapToScreen(sample, 180))
        assertEquals(AccelerationSample(2f, -1f, 9f), BubbleLevelMath.remapToScreen(sample, 270))
    }

    @Test
    fun `low pass filter uses first sample and smooths next ones`() {
        val filter = LowPassFilter(alpha = 0.5f)

        val first = filter.filter(AccelerationSample(0f, 0f, g))!!
        assertEquals(AccelerationSample(0f, 0f, g), first)

        val second = filter.filter(AccelerationSample(2f, 0f, g))!!
        assertEquals(1f, second.x, 0.0001f)

        val third = filter.filter(AccelerationSample(2f, 0f, g))!!
        assertEquals(1.5f, third.x, 0.0001f)
    }

    @Test
    fun `low pass filter converges to constant input`() {
        val filter = LowPassFilter()
        filter.filter(AccelerationSample(0f, 0f, g))

        var result: AccelerationSample? = null
        repeat(100) { result = filter.filter(AccelerationSample(3f, 0f, g)) }

        assertEquals(3f, result!!.x, 0.01f)
    }

    @Test
    fun `low pass filter ignores invalid samples and can be reset`() {
        val filter = LowPassFilter(alpha = 0.5f)
        filter.filter(AccelerationSample(1f, 0f, g))

        val afterNaN = filter.filter(AccelerationSample(Float.NaN, 0f, g))
        assertNotNull(afterNaN)
        assertEquals(1f, afterNaN!!.x, 0.0001f)

        filter.reset()
        assertNull(filter.current)
        assertNull(filter.filter(AccelerationSample(0f, 0f, 0f)))
    }
}
