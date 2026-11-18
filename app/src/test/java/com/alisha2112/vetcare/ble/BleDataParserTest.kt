package com.alisha2112.vetcare.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleDataParserTest {

    @Test
    fun parseHeartRate8Bit() {
        val data = byteArrayOf(0x00, 72)

        assertEquals(
            72,
            BleDataParser.parseHeartRate(data)
        )
    }

    @Test
    fun parseHeartRate16Bit() {
        val data = byteArrayOf(
            0x01,
            0x2C,
            0x01
        )

        assertEquals(
            300,
            BleDataParser.parseHeartRate(data)
        )
    }

    @Test
    fun parseUnsignedHeartRate() {
        val data = byteArrayOf(
            0x00,
            200.toByte()
        )

        assertEquals(
            200,
            BleDataParser.parseHeartRate(data)
        )
    }

    @Test
    fun emptyHeartRateReturnsNull() {
        assertNull(
            BleDataParser.parseHeartRate(
                byteArrayOf()
            )
        )
    }

    @Test
    fun incomplete16BitHeartRateReturnsNull() {
        assertNull(
            BleDataParser.parseHeartRate(
                byteArrayOf(0x01, 0x2C)
            )
        )
    }

    @Test
    fun parseBatteryLevel() {
        assertEquals(
            85,
            BleDataParser.parseBatteryLevel(
                byteArrayOf(85)
            )
        )
    }

    @Test
    fun batteryBoundaryValues() {
        assertEquals(
            0,
            BleDataParser.parseBatteryLevel(
                byteArrayOf(0)
            )
        )

        assertEquals(
            100,
            BleDataParser.parseBatteryLevel(
                byteArrayOf(100)
            )
        )
    }

    @Test
    fun invalidBatteryLevelReturnsNull() {
        assertNull(
            BleDataParser.parseBatteryLevel(
                byteArrayOf(120)
            )
        )
    }

    @Test
    fun emptyBatteryValueReturnsNull() {
        assertNull(
            BleDataParser.parseBatteryLevel(
                byteArrayOf()
            )
        )
    }

    @Test
    fun oversizedBatteryValueReturnsNull() {
        assertNull(
            BleDataParser.parseBatteryLevel(
                byteArrayOf(80, 90)
            )
        )
    }
}
