package dev.eventk.store.impl.pg

import dev.eventk.store.api.AppendResult
import dev.eventk.store.test.w.car.CarEvent
import dev.eventk.store.test.w.car.CarProducedEvent
import dev.eventk.store.test.w.car.CarSoldEvent
import dev.eventk.store.test.w.car.CarStreamType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class PostgresqlLoadAndAppendConcurrencyTest {
    private val factory = PostgresqlStreamTestFactory()

    @BeforeTest
    fun beforeEach() {
        factory.connectionConfig.create("event")
    }

    @AfterTest
    fun afterEach() {
        factory.closeAll()
        factory.connectionConfig.drop()
    }

    @Test
    fun `advisory lock serialises concurrent loadAndAppendStream on the same stream`() {
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)
        val handler = eventStore.withStreamType(CarStreamType)
        val streamId = java.util.UUID.randomUUID()

        // First caller will block inside consume until released; the second must wait for the lock.
        val firstInsideConsume = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val firstSawVersionsCount = AtomicReference<Int>(null)
        val secondSawVersionsCount = AtomicReference<Int>(null)

        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstFuture = executor.submit {
                handler.loadAndAppendStream(
                    streamId = streamId,
                    consume = { events ->
                        val count = events.toList().size
                        firstSawVersionsCount.set(count)
                        firstInsideConsume.countDown()
                        releaseFirst.await(5, TimeUnit.SECONDS)
                        AppendResult(listOf<CarEvent>(CarProducedEvent(vin = "1", producer = 1, make = "x", model = "y")))
                    },
                )
            }

            assertTrue(firstInsideConsume.await(5, TimeUnit.SECONDS))

            val secondFuture = executor.submit {
                handler.loadAndAppendStream(
                    streamId = streamId,
                    consume = { events ->
                        val count = events.toList().size
                        secondSawVersionsCount.set(count)
                        AppendResult(listOf<CarEvent>(CarSoldEvent(seller = 1, buyer = 2, price = 1f)))
                    },
                )
            }

            // The second call must NOT have entered consume yet — it's blocked on the advisory lock.
            Thread.sleep(500)
            assertEquals(null, secondSawVersionsCount.get())

            releaseFirst.countDown()
            firstFuture.get(5, TimeUnit.SECONDS)
            secondFuture.get(5, TimeUnit.SECONDS)

            assertEquals(0, firstSawVersionsCount.get())
            assertEquals(1, secondSawVersionsCount.get())

            val finalEvents = storage.useStreamEvents(CarStreamType, streamId, 0) { it.toList() }
            assertEquals(2, finalEvents.size)
            assertEquals(1, finalEvents[0].version)
            assertEquals(2, finalEvents[1].version)
        } finally {
            executor.shutdownNow()
        }
    }
}
