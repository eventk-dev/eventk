package dev.eventk.store.test

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
public open class LoadStreamForAppendTest<R : Storage, S : EventStore, F : StreamTestFactory<R, S>>(
    protected val factory: F,
) {
    @Test
    @JsName("test1")
    public fun `given empty stream - appendStream writes events`() {
        // given
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        val metadata = mapOf("m1" to "x")

        // when
        val appended = eventStore
            .withStreamType(CarStreamType)
            .loadStreamForAppend(car1StreamId) { _, appendStream ->
                appendStream(listOf<CarEvent>(event1), metadata)
            }

        // then
        val expected = EventEnvelope(CarStreamType, car1StreamId, 1, 1, metadata, event1)
        assertEquals(listOf(expected), appended)
        assertEquals(expected, storage.getEventByStreamVersion(CarStreamType, car1StreamId, 1))
    }

    @Test
    @JsName("test2")
    public fun `given existing events - block reads loaded then appends`() {
        // given
        val storage = factory.newStorage()
        val produced = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        storage.add(CarStreamType, car1StreamId, 1, produced)
        val eventStore = factory.newEventStore(storage)
        val sold = CarSoldEvent(seller = 1, buyer = 2, price = 100.0f)

        // when
        val all = eventStore
            .withStreamType(CarStreamType)
            .loadStreamForAppend(car1StreamId) { loaded, appendStream ->
                assertEquals(1, loaded.size)
                loaded + appendStream(listOf<CarEvent>(sold), emptyMap())
            }

        // then
        assertEquals(2, all.size)
        assertEquals(produced, all[0].event)
        assertEquals(1, all[0].version)
        assertEquals(sold, all[1].event)
        assertEquals(2, all[1].version)
    }

    @Test
    @JsName("test3")
    public fun `given empty stream - block returns without calling appendStream - nothing is persisted`() {
        // given
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)

        // when
        eventStore
            .withStreamType(CarStreamType)
            .loadStreamForAppend(car1StreamId) { loaded, _ ->
                assertEquals(0, loaded.size)
            }

        // then
        assertEquals(0, storage.useStreamEvents(CarStreamType, car1StreamId, 0) { it.toList() }.size)
    }

    @Test
    @JsName("test4")
    public fun `given block throws after appending - nothing is persisted`() {
        // given
        val storage = factory.newStorage()
        val produced = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        storage.add(CarStreamType, car1StreamId, 1, produced)
        val eventStore = factory.newEventStore(storage)
        val sold = CarSoldEvent(seller = 1, buyer = 2, price = 100.0f)

        // when
        assertFailsWith<IllegalStateException> {
            eventStore
                .withStreamType(CarStreamType)
                .loadStreamForAppend(car1StreamId) { _, appendStream ->
                    appendStream(listOf<CarEvent>(sold), emptyMap())
                    throw IllegalStateException("nope")
                }
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
            .loadStreamForAppend(car1StreamId) { _, appendStream ->
                appendStream(listOf<CarEvent>(sold), emptyMap())
            }

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
            .loadStreamForAppend(car1StreamId, sinceVersion = 1) { loaded, _ -> loaded.size }

        // then
        assertEquals(1, loadedSize)
    }

    @Test
    @JsName("test7")
    public fun `appended envelopes have correct versions - positions and metadata`() {
        // given
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")
        val event2 = CarSoldEvent(seller = 1, buyer = 2, price = 100.0f)
        val metadata = mapOf("trace" to "abc")

        // when
        val appended = eventStore
            .withStreamType(CarStreamType)
            .loadStreamForAppend(car1StreamId) { _, appendStream ->
                appendStream(listOf<CarEvent>(event1, event2), metadata)
            }

        // then
        assertEquals(2, appended.size)
        assertEquals(1, appended[0].version)
        assertEquals(2, appended[1].version)
        assertTrue(appended[0].position < appended[1].position)
        assertEquals(metadata, appended[0].metadata)
        assertEquals(metadata, appended[1].metadata)
    }

    @Test
    @JsName("test8")
    public fun `appendStream called twice throws IllegalStateException`() {
        // given
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)
        val event1 = CarProducedEvent(vin = "123", producer = 1, make = "kia", model = "rio")

        // when
        assertFailsWith<IllegalStateException> {
            eventStore
                .withStreamType(CarStreamType)
                .loadStreamForAppend(car1StreamId) { _, appendStream ->
                    appendStream(listOf<CarEvent>(event1), emptyMap())
                    appendStream(listOf<CarEvent>(event1), emptyMap())
                }
        }

        // then - second call throws inside the block, so the whole call rolls back
        assertEquals(0, storage.useStreamEvents(CarStreamType, car1StreamId, 0) { it.toList() }.size)
    }

    @Test
    @JsName("test9")
    public fun `cached snapshot - sinceVersion skips cached event - then block composes new cache value`() {
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
            .loadStreamForAppend(car1StreamId, sinceVersion = cache.last().version) { loaded, appendStream ->
                assertEquals(1, loaded.size)
                assertEquals(event2, loaded.single().event)
                cache + loaded + appendStream(listOf<CarEvent>(event3), emptyMap())
            }
        cache = newCacheValue // the cache is only updated after loadStreamForAppend returns,

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
}
