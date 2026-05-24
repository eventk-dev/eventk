package dev.eventk.store.impl.pg

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

internal class PostgresqlLoadStreamForAppendConcurrencyTest {
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
    fun `advisory lock serialises concurrent loadStreamForAppend on the same stream`() {
        val storage = factory.newStorage()
        val eventStore = factory.newEventStore(storage)
        val handler = eventStore.withStreamType(CarStreamType)
        val streamId = java.util.UUID.randomUUID()

        // First caller will block inside the block until released; the second must wait for the lock.
        val firstInsideBlock = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val firstSawLoadedSize = AtomicReference<Int>(null)
        val secondSawLoadedSize = AtomicReference<Int>(null)

        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstFuture = executor.submit {
                handler.loadStreamForAppend(streamId) { loaded, appendStream ->
                    firstSawLoadedSize.set(loaded.size)
                    firstInsideBlock.countDown()
                    releaseFirst.await(5, TimeUnit.SECONDS)
                    appendStream(listOf<CarEvent>(CarProducedEvent(vin = "1", producer = 1, make = "x", model = "y")), emptyMap())
                }
            }

            assertTrue(firstInsideBlock.await(5, TimeUnit.SECONDS))

            val secondFuture = executor.submit {
                handler.loadStreamForAppend(streamId) { loaded, appendStream ->
                    secondSawLoadedSize.set(loaded.size)
                    appendStream(listOf<CarEvent>(CarSoldEvent(seller = 1, buyer = 2, price = 1f)), emptyMap())
                }
            }

            // The second call must NOT have entered the block yet — it's blocked on the advisory lock.
            Thread.sleep(500)
            assertEquals(null, secondSawLoadedSize.get())

            releaseFirst.countDown()
            firstFuture.get(5, TimeUnit.SECONDS)
            secondFuture.get(5, TimeUnit.SECONDS)

            assertEquals(0, firstSawLoadedSize.get())
            assertEquals(1, secondSawLoadedSize.get())

            val finalEvents = storage.useStreamEvents(CarStreamType, streamId, 0) { it.toList() }
            assertEquals(2, finalEvents.size)
            assertEquals(1, finalEvents[0].version)
            assertEquals(2, finalEvents[1].version)
        } finally {
            executor.shutdownNow()
        }
    }
}
