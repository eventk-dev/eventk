package dev.eventk.example.domain.command

import com.benasher44.uuid.Uuid

sealed interface CarCommand {
    data class Produce(
        val id: Uuid,
        val vin: String,
        val producer: Long,
        val make: String,
        val model: String,
    ) : CarCommand
}
