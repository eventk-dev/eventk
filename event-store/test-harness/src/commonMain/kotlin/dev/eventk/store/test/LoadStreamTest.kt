package dev.eventk.store.test

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.blocking.EventStore
import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarSoldEvent
import dev.eventk.store.test.w.car.CarStreamType
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("DuplicatedCode")
public open class LoadStreamTest<R : Storage, S : EventStore, F : StreamTestFactory<R, S>>(
    protected val factory: F,
) {
    @Test
    @JsName("test1")
    public fun `given no events - when loading events of a stream - list is empty`() {
        // given
        val storage = factory.newStorage()

        // when
        val eventStore = factory.newEventStore(storage)
        val eventEnvelopes = eventStore
            .withStreamType(CarStreamType)
            .loadStream(
                streamId = car1StreamId,
            )

        // then
        assertEquals(0, eventEnvelopes.size)
    }

    @Test
    @JsName("test2")
    public fun `given no events - when using events of a stream - list is empty`() {
        // given
        val storage = factory.newStorage()

        // when
        val eventStore = factory.newEventStore(storage)
        val eventStream = eventStore.withStreamType(CarStreamType)

        val listFromUse = eventStream.useStream(streamId = car1StreamId) { stream ->
            stream.toList()
        }

        // then
        assertEquals(eventStream.loadStream(streamId = car1StreamId), listFromUse)
    }

    @Test
    @JsName("test3")
    public fun `given 3 event from different streams - when loading one stream - correct events are loaded`() {
        // given
        val storage = factory.newStorage()
        storage.add(CarStreamType, car1StreamId, 1, CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio"))
        storage.add(CarStreamType, car2StreamId, 1, CarProducedEvent(vin = "456", producer = 1, make = "kia", model = "rio"))
        storage.add(CarStreamType, car1StreamId, 2, CarSoldEvent(seller = 1, buyer = 2, 2500.00f))

        // when
        val eventStore = factory.newEventStore(storage)

        (0..1).forEach { sinceVersion ->
            val eventEnvelopes = eventStore
                .withStreamType(CarStreamType)
                .loadStream(
                    streamId = car1StreamId,
                    sinceVersion = sinceVersion,
                )

            // then
            assertEquals(2 - sinceVersion, eventEnvelopes.size)
            assertEquals(
                expected = EventEnvelope(
                    streamType = CarStreamType,
                    streamId = car1StreamId,
                    version = 2,
                    position = 3,
                    metadata = emptyMap(),
                    event = CarSoldEvent(seller = 1, buyer = 2, 2500.00f),
                ),
                actual = eventEnvelopes.last(),
            )
        }
    }

    @Test
    @JsName("test4")
    public fun `given 3 event from different streams - when using one stream - correct events are loaded`() {
        // given
        val storage = factory.newStorage()
        storage.add(CarStreamType, car1StreamId, 1, CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio"))
        storage.add(CarStreamType, car2StreamId, 1, CarProducedEvent(vin = "456", producer = 1, make = "kia", model = "rio"))
        storage.add(CarStreamType, car1StreamId, 2, CarSoldEvent(seller = 1, buyer = 2, 2500.00f))

        // when
        val eventStore = factory.newEventStore(storage)

        (0..1).forEach { sinceVersion ->
            val eventStream = eventStore.withStreamType(CarStreamType)
            val events = eventStream.useStream(
                streamId = car1StreamId,
                sinceVersion = sinceVersion,
            ) { stream ->
                stream.toList()
            }

            // then
            assertEquals(eventStream.loadStream(streamId = car1StreamId, sinceVersion = sinceVersion), events)
            assertEquals(
                expected = EventEnvelope(
                    streamType = CarStreamType,
                    streamId = car1StreamId,
                    version = 2,
                    position = 3,
                    metadata = emptyMap(),
                    event = CarSoldEvent(seller = 1, buyer = 2, 2500.00f),
                ),
                actual = events.last(),
            )
        }
    }
}
