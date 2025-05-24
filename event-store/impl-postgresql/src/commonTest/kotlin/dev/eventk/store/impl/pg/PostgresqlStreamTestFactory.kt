package dev.eventk.store.impl.pg

import dev.eventk.store.impl.common.string.serialization.DefaultEventMetadataSerializer
import dev.eventk.store.test.StreamTestFactory
import dev.eventk.store.test.w.car.CarStreamType
import dev.eventk.store.test.w.driver.DriverStreamType

@OptIn(ExperimentalStdlibApi::class)
internal class PostgresqlStreamTestFactory : StreamTestFactory<PostgresqlJdbcStorage, PostgresqlJdbcEventStore>() {
    internal val connectionConfig = generateTestConnectionConfig()

    private val config
        get() = PostgresqlConfig(
            registeredTypes = listOf(
                CarStreamType,
                DriverStreamType,
            ),
            payloadSerializers = mapOf(
                CarStreamType to CarStreamType.stringEventSerializer,
                DriverStreamType to DriverStreamType.stringEventSerializer,
            ),
            idSerializers = mapOf(
                CarStreamType to CarStreamType.stringIdSerializer,
                DriverStreamType to DriverStreamType.stringIdSerializer,
            ),
            eventMetadataSerializer = DefaultEventMetadataSerializer,
            dataSource = connectionConfig.dataSource(closeables),
            eventTable = "event",
        )

    override fun createStorage(): PostgresqlJdbcStorage {
        return PostgresqlJdbcStorage(config)
    }

    override fun createEventStore(storage: PostgresqlJdbcStorage): PostgresqlJdbcEventStore {
        return PostgresqlJdbcEventStore(config, storage)
    }
}
