package dev.eskt.store.impl.pg

import dev.eskt.store.test.w.car.CarProducedEvent
import dev.eskt.store.test.w.car.CarStreamType
import dev.eskt.store.test.w.driver.DriverRegisteredEvent
import dev.eskt.store.test.w.driver.DriverStreamType
import dev.eskt.store.test.w.part.PartProducedEvent
import dev.eskt.store.test.w.part.PartStreamType
import org.junit.Test
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal class PostgresqlR2dbcEventStorePublicConstructorTest {
    private val connectionConfig = generateTestConnectionConfig()
    private val closeables = mutableListOf<AutoCloseable>()

    @BeforeTest
    fun beforeEach() {
        connectionConfig.create("event", streamIdType = "text")
    }

    @AfterTest
    fun afterEach() {
        closeables.forEach { it.close() }
        connectionConfig.drop()
    }

    @Test
    fun `public constructor creates event store and allow withStreamType to be called`() {
        val dataSource = connectionConfig.dataSource(closeables)
        val eventStore = PostgresqlEventStore(dataSource, "event") {
            registerStreamType(PartStreamType)
            registerStreamType(DriverStreamType)
            registerStreamType(CarStreamType, idSerializer = CarStreamType.stringIdSerializer)
        }

        val newPartId = "pr-43234"
        val partStreamType = eventStore.withStreamType(PartStreamType)
        partStreamType.appendStream(newPartId, 0, listOf(PartProducedEvent(1, "PR43234")))
        partStreamType.loadStream(newPartId)

        val newDriverId = UUID.randomUUID()
        val driverStreamType = eventStore.withStreamType(DriverStreamType)
        driverStreamType.appendStream(newDriverId, 0, listOf(DriverRegisteredEvent(licence = "9753510", name = "Johnny")))
        driverStreamType.loadStream(newDriverId)

        val newCarId = UUID.randomUUID()
        val carStreamType = eventStore.withStreamType(CarStreamType)
        carStreamType.appendStream(newCarId, 0, listOf(CarProducedEvent(vin = "1233", producer = 1, make = "a", model = "b")))
        carStreamType.loadStream(newCarId)
    }

    @Test
    fun `serializers are available for registered types`() {
        val dataSource = connectionConfig.dataSource(closeables)
        val eventStore = PostgresqlEventStore(dataSource, "event") {
            registerStreamType(CarStreamType, idSerializer = CarStreamType.stringIdSerializer)
        }

        assertEquals(CarStreamType.stringIdSerializer, eventStore.getIdSerializer(CarStreamType))
        val payloadSerializer = eventStore.getPayloadSerializer(CarStreamType)
        val event = CarProducedEvent(vin = "1233", producer = 1, make = "a", model = "b")
        val payload = payloadSerializer.serialize(event)
        assertEquals(event, payloadSerializer.deserialize(payload))
    }
}
