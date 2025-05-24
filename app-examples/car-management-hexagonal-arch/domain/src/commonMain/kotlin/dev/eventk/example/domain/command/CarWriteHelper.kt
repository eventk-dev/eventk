package dev.eventk.example.domain.command

import com.benasher44.uuid.Uuid

/**
 * A write-side helper to check for duplicated cars, and is supposed to be used before
 * accepting commands that create new cars.
 * This is an alternative to accepting all creation commands and invalidate the duplicated ones
 * in a separate async process manager, but it requires transaction support.
 */
data class CarWriteHelper(
    val id: Uuid,
    val vin: String,
)
