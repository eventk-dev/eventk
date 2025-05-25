package dev.eventk.example.domain.process

import com.benasher44.uuid.Uuid
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.example.domain.EventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType
import dev.eventk.store.test.w.car.CarEvent
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarStreamType

@EventListener
class CarProductionNotificationProcess(
    private val notifier: CarProductionNotifier,
) : SingleStreamTypeEventListener<CarEvent, Uuid> {
    override val id: String = "car-production-notification-process"
    override val streamType: StreamType<CarEvent, Uuid> = CarStreamType

    override fun listen(envelope: EventEnvelope<CarEvent, Uuid>) {
        when (val event = envelope.event) {
            is CarProducedEvent -> notifier.notify(vin = event.vin, make = event.make, model = event.model)
            else -> {}
        }
    }
}
