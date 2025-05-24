package dev.eventk.example.domain.process

import com.benasher44.uuid.Uuid
import dev.eventk.arch.hex.port.MultiStreamTypeEventListener
import dev.eventk.example.domain.EventListener
import dev.eventk.example.domain.Logger
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarStreamType
import dev.eventk.store.test.w.driver.DriverRegisteredEvent
import dev.eventk.store.test.w.driver.DriverStreamType

@EventListener
data class CarDriverLogger(
    val logger: Logger,
) : MultiStreamTypeEventListener<Any, Uuid> {
    override val id: String = "car-driver-pm"

    override val streamTypes: List<StreamType<out Any, Uuid>> = listOf(CarStreamType, DriverStreamType)

    override fun listen(envelope: EventEnvelope<Any, Uuid>) {
        when (val event = envelope.event) {
            is DriverRegisteredEvent -> logger.log("Driver registered: ${event.name}")
            is CarProducedEvent -> logger.log("Car produced: ${event.make} ${event.model}")
        }
    }
}
