package com.alisha2112.vetcare.ble

object BleDataParser {

    /**
     * Декодування Heart Rate Measurement (0x2A37).
     * Біт 0 прапорців визначає формат:
     * 0 — UINT8, 1 — UINT16 (little-endian).
     */
    fun parseHeartRate(bytes: ByteArray): Int? {
        if (bytes.size < 2) return null

        val flags = bytes[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0

        return if (is16Bit) {
            if (bytes.size < 3) {
                null
            } else {
                (bytes[1].toInt() and 0xFF) or
                        ((bytes[2].toInt() and 0xFF) shl 8)
            }
        } else {
            bytes[1].toInt() and 0xFF
        }
    }

    /**
     * Battery Level (0x2A19):
     * один беззнаковий байт у діапазоні 0..100.
     */
    fun parseBatteryLevel(bytes: ByteArray): Int? {
        if (bytes.size != 1) return null

        val level = bytes[0].toInt() and 0xFF

        return level.takeIf { it in 0..100 }
    }
}
