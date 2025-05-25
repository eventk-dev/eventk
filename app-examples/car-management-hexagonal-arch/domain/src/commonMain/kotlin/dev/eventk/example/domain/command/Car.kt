package dev.eventk.example.domain.command

import com.benasher44.uuid.Uuid
import dev.eventk.store.test.w.car.CarDrivenEvent
import dev.eventk.store.test.w.car.CarEliminatedEvent
import dev.eventk.store.test.w.car.CarEvent
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarSoldEvent

data class Car(
    val id: Uuid,
    val version: Int = 0,
    val owner: Long = 0L,
) {
    operator fun plus(event: CarEvent): Car = when (event) {
        is CarProducedEvent -> copy(
            version = version + 1,
            owner = event.producer,
        )
        is CarSoldEvent -> TODO()
        is CarDrivenEvent -> TODO()
        is CarEliminatedEvent -> TODO()
    }
}
