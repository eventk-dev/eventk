package dev.eventk.store.test

import dev.eventk.store.api.AppendResult
import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.blocking.EventStore
import dev.eventk.store.storage.api.blocking.Storage
import dev.eventk.store.test.w.car.CarEvent
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarSoldEvent
import dev.eventk.store.test.w.car.CarStreamType
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("DuplicatedCode")
public open class LoadAndAppendStreamTest<R : Storage, S : EventStore, F : StreamTestFactory<R, S>>(
    protected val factory: F,
) {
    @Test
    @JsName("test1")
    public fun `given empty stream - consume appends events - events are persisted`() {
        // given
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        val metadata = mapOf("m1" to "x")

        // when
        val all = eventStore
            .withStreamType(CarStreamType)
            .loadAndAppendStream(
                streamId = car1StreamId,
                consume = { _ -> AppendResult(listOf<CarEvent>(event1), metadata) },
                finalize = { loaded, appended -> loaded + appended },
            )

        // then
        val expected = EventEnvelope(CarStreamType, car1StreamId, 1, 1, metadata, event1)
        assertEquals(listOf(expected), all)
        assertEquals(expected, storage.getEventByStreamVersion(CarStreamType, car1StreamId, 1))
    }

    @Test
    @JsName("test2")
    public fun `given existing events - consume reads then appends - loaded reflects iterated events`() {
        // given
        val storage = factory.newStorage()
        val produced = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        storage.add(CarStreamType, car1StreamId, 1, produced)
        val eventStore = factory.newEventStore(storage)
        val sold = CarSoldEvent(seller = 1, buyer = 2, price = 100.0f)

        // when
        val all = eventStore
            .withStreamType(CarStreamType)
            .loadAndAppendStream(
                streamId = car1StreamId,
                consume = { events ->
                    assertEquals(1, events.size)
                    AppendResult(listOf<CarEvent>(sold))
                },
                finalize = { loaded, appended -> loaded + appended },
            )

        // then
        assertEquals(2, all.size)
        assertEquals(produced, all[0].event)
        assertEquals(1, all[0].version)
        assertEquals(sold, all[1].event)
        assertEquals(2, all[1].version)
    }

    @Test
    @JsName("test3")
    public fun `given empty stream - consume returns no events - nothing is persisted`() {
        // given
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)

        // when
        val all = eventStore
            .withStreamType(CarStreamType)
            .loadAndAppendStream(
                streamId = car1StreamId,
                consume = { _ -> AppendResult(emptyList()) },
                finalize = { loaded, appended -> loaded + appended },
            )

        // then
        assertEquals(emptyList(), all)
        assertEquals(0, storage.useStreamEvents(CarStreamType, car1StreamId, 0) { it.toList() }.size)
    }

    @Test
    @JsName("test4")
    public fun `given consume throws - nothing is persisted`() {
        // given
        val storage = factory.newStorage()
        val produced = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        storage.add(CarStreamType, car1StreamId, 1, produced)
        val eventStore = factory.newEventStore(storage)

        // when
        assertFailsWith<IllegalStateException> {
            eventStore
                .withStreamType(CarStreamType)
                .loadAndAppendStream(
                    streamId = car1StreamId,
                    consume = { _ -> throw IllegalStateException("nope") },
                )
        }

        // then
        val remaining = storage.useStreamEvents(CarStreamType, car1StreamId, 0) { it.toList() }
        assertEquals(1, remaining.size)
        assertEquals(produced, remaining.single().event)
    }

    @Test
    @JsName("test5")
    public fun `given existing events on multiple streams - append only touches the target stream`() {
        // given
        val storage = factory.newStorage()
        storage.add(CarStreamType, car1StreamId, 1, CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio"))
        storage.add(CarStreamType, car2StreamId, 1, CarProducedEvent(vin = "456", producer = 1, make = "kia", model = "rio"))
        val eventStore = factory.newEventStore(storage)
        val sold = CarSoldEvent(seller = 1, buyer = 2, price = 100.0f)

        // when
        eventStore
            .withStreamType(CarStreamType)
            .loadAndAppendStream(
                streamId = car1StreamId,
                consume = { _ -> AppendResult(listOf<CarEvent>(sold)) },
            )

        // then
        val car1Events = storage.useStreamEvents(CarStreamType, car1StreamId, 0) { it.toList() }
        val car2Events = storage.useStreamEvents(CarStreamType, car2StreamId, 0) { it.toList() }
        assertEquals(2, car1Events.size)
        assertEquals(sold, car1Events[1].event)
        assertEquals(1, car2Events.size)
    }

    @Test
    @JsName("test6")
    public fun `sinceVersion drops earlier events from the loaded list`() {
        // given
        val storage = factory.newStorage()
        val produced = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        val sold = CarSoldEvent(seller = 1, buyer = 2, price = 100.0f)
        storage.add(CarStreamType, car1StreamId, 1, produced)
        storage.add(CarStreamType, car1StreamId, 2, sold)
        val eventStore = factory.newEventStore(storage)

        // when
        val loadedSize = eventStore
            .withStreamType(CarStreamType)
            .loadAndAppendStream(
                streamId = car1StreamId,
                sinceVersion = 1,
                consume = { _ -> AppendResult(emptyList()) },
                finalize = { loaded, _ -> loaded.size },
            )

        // then
        assertEquals(1, loadedSize)
    }

    @Test
    @JsName("test7")
    public fun `appended envelopes have correct positions and metadata`() {
        // given
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        val event2 = CarSoldEvent(seller = 1, buyer = 2, price = 100.0f)
        val metadata = mapOf("trace" to "abc")

        // when
        val appended = eventStore
            .withStreamType(CarStreamType)
            .loadAndAppendStream(
                streamId = car1StreamId,
                consume = { _ -> AppendResult(listOf<CarEvent>(event1, event2), metadata) },
                finalize = { _, appended -> appended },
            )

        // then
        assertEquals(2, appended.size)
        assertEquals(1, appended[0].version)
        assertEquals(2, appended[1].version)
        assertTrue(appended[0].position < appended[1].position)
        assertEquals(metadata, appended[0].metadata)
        assertEquals(metadata, appended[1].metadata)
    }

    @Test
    @JsName("test9")
    public fun `cached snapshot - sinceVersion skips cached events, finalize computes new cache value`() {
        // given
        val storage = factory.newStorage()
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        storage.add(CarStreamType, car1StreamId, 1, event1)
        val eventStore = factory.newEventStore(storage)
        var cache = eventStore.withStreamType(CarStreamType).loadStream(car1StreamId)
        assertEquals(1, cache.size)

        // another writer appends event2 after the cache snapshot was taken
        val event2 = CarSoldEvent(seller = 1, buyer = 2, price = 100.0f)
        storage.add(CarStreamType, car1StreamId, 2, event2)

        // when
        val event3 = CarSoldEvent(seller = 2, buyer = 3, price = 200.0f)
        val newCacheValue = eventStore
            .withStreamType(CarStreamType)
            .loadAndAppendStream(
                streamId = car1StreamId,
                sinceVersion = cache.lastOrNull()?.version ?: 0,
                consume = { events ->
                    assertEquals(1, events.size)
                    assertEquals(event2, events.single().event)
                    AppendResult(listOf<CarEvent>(event3))
                },
                finalize = { loaded, appended -> cache + loaded + appended },
            )
        cache = newCacheValue // the cache is only updated after loadAndAppendStream returns

        // then
        assertEquals(3, cache.size)
        assertEquals(event1, cache[0].event)
        assertEquals(event2, cache[1].event)
        assertEquals(event3, cache[2].event)
        assertEquals(1, cache[0].version)
        assertEquals(2, cache[1].version)
        assertEquals(3, cache[2].version)
        assertEquals(cache, storage.useStreamEvents(CarStreamType, car1StreamId, 0) { it.toList() })
    }

    @Test
    @JsName("test8")
    public fun `Unit overload delegates to the R-returning method`() {
        // given
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")

        // when
        eventStore
            .withStreamType(CarStreamType)
            .loadAndAppendStream(
                streamId = car1StreamId,
                consume = { _ -> AppendResult(listOf<CarEvent>(event1)) },
            )

        // then
        val stored = storage.useStreamEvents(CarStreamType, car1StreamId, 0) { it.toList() }
        assertEquals(1, stored.size)
        assertEquals(event1, stored.single().event)
    }
}
