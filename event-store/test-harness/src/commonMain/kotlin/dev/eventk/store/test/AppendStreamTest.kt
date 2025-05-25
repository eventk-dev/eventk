package dev.eventk.store.test

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamVersionMismatchException
import dev.eventk.store.api.blocking.EventStore
import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarSoldEvent
import dev.eventk.store.test.w.car.CarStreamType
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("DuplicatedCode")
public open class AppendStreamTest<R : Storage, S : EventStore, F : StreamTestFactory<R, S>>(
    protected val factory: F,
) {
    @Test
    @JsName("test1")
    public fun `given no events - when appending events on a stream - event is added`() {
        // given
        val storage = factory.newStorage()

        // when
        val eventStore = factory.newEventStore(storage)
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        val metadata = mapOf(
            "m1" to "some text",
            "m2" to 123,
        )
        eventStore
            .withStreamType(CarStreamType)
            .appendStream(
                streamId = car1StreamId,
                expectedVersion = 0,
                events = listOf(
                    event1,
                ),
                metadata = metadata,
            )

        // then
        val eventEnvelope1 = EventEnvelope(CarStreamType, car1StreamId, 1, 1, metadata, event1)
        assertEquals(eventEnvelope1, storage.getEventByPosition(1))
        assertEquals(eventEnvelope1, storage.getEventByStreamVersion(CarStreamType, car1StreamId, 1))
    }

    @Test
    @JsName("test2")
    public fun `given no events - when appending muliple events on a stream - events are added`() {
        // given
        val storage = factory.newStorage()

        // when
        val eventStore = factory.newEventStore(storage)
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        val event2 = CarSoldEvent(seller = 1, buyer = 2, price = 23000f)
        val metadata = mapOf(
            "m1" to "some text",
            "m2" to 123,
        )
        eventStore
            .withStreamType(CarStreamType)
            .appendStream(
                streamId = car1StreamId,
                expectedVersion = 0,
                events = listOf(
                    event1,
                    event2,
                ),
                metadata = metadata,
            )

        // then
        val eventEnvelope1 = EventEnvelope(CarStreamType, car1StreamId, 1, 1, metadata, event1)
        val eventEnvelope2 = EventEnvelope(CarStreamType, car1StreamId, 2, 2, metadata, event2)
        assertEquals(eventEnvelope1, storage.getEventByPosition(1))
        assertEquals(eventEnvelope1, storage.getEventByStreamVersion(CarStreamType, car1StreamId, 1))
        assertEquals(eventEnvelope2, storage.getEventByPosition(2))
        assertEquals(eventEnvelope2, storage.getEventByStreamVersion(CarStreamType, car1StreamId, 2))
    }

    @Test
    @JsName("test3")
    public fun `given 2 event from different streams - when appending events on a stream - event is added`() {
        // given
        val storage = factory.newStorage()
        storage.add(CarStreamType, car1StreamId, 1, CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio"))
        storage.add(CarStreamType, car2StreamId, 1, CarProducedEvent(vin = "456", producer = 1, make = "kia", model = "rio"))

        // when
        val event1 = CarSoldEvent(seller = 1, buyer = 2, 2500.00f)
        val eventStore = factory.newEventStore(storage)
        eventStore
            .withStreamType(CarStreamType)
            .appendStream(
                streamId = car1StreamId,
                expectedVersion = 1,
                events = listOf(
                    event1,
                ),
            )

        // then
        val eventEnvelope1 = EventEnvelope(CarStreamType, car1StreamId, 2, 3, emptyMap(), event1)
        assertEquals(eventEnvelope1, storage.getEventByPosition(3))
        assertEquals(eventEnvelope1, storage.getEventByStreamVersion(CarStreamType, car1StreamId, 2))
    }

    @Test
    @JsName("test4")
    public fun `given 1 event from same stream - when appending event with already existing version - append is rejected`() {
        // given
        val storage = factory.newStorage()
        storage.add(CarStreamType, car1StreamId, 1, CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio"))
        storage.add(CarStreamType, car1StreamId, 2, CarSoldEvent(seller = 1, buyer = 2, 2500.00f))

        // when
        val event1 = CarSoldEvent(seller = 1, buyer = 3, 2500.00f)
        val eventStore = factory.newEventStore(storage)
        val exception = assertFailsWith<StreamVersionMismatchException> {
            eventStore
                .withStreamType(CarStreamType)
                .appendStream(
                    streamId = car1StreamId,
                    expectedVersion = 1,
                    events = listOf(
                        event1,
                    ),
                )
        }

        // then
        assertEquals(StreamVersionMismatchException(currentVersion = 2, expectedVersion = 1), exception)
    }

    @Test
    @JsName("test5")
    public fun `given 1 event from same stream - when appending event out of order - append is rejected`() {
        // given
        val storage = factory.newStorage()
        storage.add(CarStreamType, car1StreamId, 1, CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio"))

        // when
        val event1 = CarSoldEvent(seller = 1, buyer = 2, 2500.00f)
        val eventStore = factory.newEventStore(storage)
        val exception = assertFailsWith<StreamVersionMismatchException> {
            eventStore
                .withStreamType(CarStreamType)
                .appendStream(
                    streamId = car1StreamId,
                    // this would create a gap if accepted
                    expectedVersion = 2,
                    events = listOf(
                        event1,
                    ),
                )
        }

        // then
        assertEquals(StreamVersionMismatchException(currentVersion = 1, expectedVersion = 2), exception)
    }
}
