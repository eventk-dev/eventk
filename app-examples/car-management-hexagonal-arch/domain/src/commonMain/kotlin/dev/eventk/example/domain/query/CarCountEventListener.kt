package dev.eventk.example.domain.query

import com.benasher44.uuid.Uuid
import dev.eventk.arch.hex.port.SingleStreamTypeEventListener
import dev.eventk.example.domain.EventListener
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType
import dev.eventk.store.test.w.car.CarEliminatedEvent
import dev.eventk.store.test.w.car.CarEvent
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarStreamType

@EventListener
class CarCountEventListener(
    private val makeModelCarCountRepository: MakeModelCarCountRepository,
    private val makeModelCarRepository: MakeModelCarRepository,
) : SingleStreamTypeEventListener<CarEvent, Uuid> {
    override val id: String = "car-count-read-model"
    override val streamType: StreamType<CarEvent, Uuid> = CarStreamType

    override fun listen(envelope: EventEnvelope<CarEvent, Uuid>) {
        when (val event = envelope.event) {
            is CarProducedEvent -> {
                val car = makeModelCarRepository.find(envelope.streamId)
                if (car != null) return // already processed
                makeModelCarRepository.add(MakeModelCar(id = envelope.streamId, make = event.make, model = event.model))

                val makeModelCount = makeModelCarCountRepository.find(make = event.make, model = event.model)
                    ?: MakeModelCarCount(make = event.make, model = event.model, count = 0)

                makeModelCarCountRepository.save(makeModelCount.copy(count = makeModelCount.count + 1))
            }

            is CarEliminatedEvent -> {
                val car = makeModelCarRepository.find(envelope.streamId)
                    ?: return // already processed
                makeModelCarRepository.removeById(envelope.streamId)

                val makeModelCount = makeModelCarCountRepository.find(make = car.make, model = car.model)
                    ?: throw IllegalStateException("Count should exist as car ${envelope.streamId} is being eliminated")

                makeModelCarCountRepository.save(makeModelCount.copy(count = makeModelCount.count - 1))
            }

            else -> {}
        }
    }
}
