package dev.eskt.store.impl.pg

import dev.eskt.store.api.EventEnvelope
import dev.eskt.store.api.EventMetadata
import dev.eskt.store.api.Serializer
import dev.eskt.store.api.StreamType
import dev.eskt.store.api.suspending.EventStore
import dev.eskt.store.api.suspending.StreamTypeHandler

public class PostgresqlR2dbcEventStore internal constructor(
    private val config: PostgresqlConfig<ReactiveDataSource>,
    private val storage: PostgresqlR2dbcStorage = PostgresqlR2dbcStorage(config),
) : EventStore {
    override val registeredTypes: Set<StreamType<*, *>> = config.registeredTypes.toSet()

    override suspend fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int): List<EventEnvelope<E, I>> {
        return storage.loadEventBatch(sincePosition, batchSize)
    }

    override suspend fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int, streamType: StreamType<E, I>): List<EventEnvelope<E, I>> {
        return storage.loadEventBatch(sincePosition, batchSize, streamType)
    }

    override fun <E, I> withStreamType(type: StreamType<E, I>): StreamTypeHandler<E, I> {
        return dev.eskt.store.impl.common.base.suspending.StreamTypeHandler(type, storage)
    }

    public val dataSource: ReactiveDataSource
        get() = config.dataSource

    @Suppress("UNCHECKED_CAST")
    public fun <E, I> getPayloadSerializer(streamType: StreamType<E, I>): Serializer<E, String> {
        return config.payloadSerializers[streamType] as Serializer<E, String>
    }

    @Suppress("UNCHECKED_CAST")
    public fun <E, I> getIdSerializer(streamType: StreamType<E, I>): Serializer<I, String> {
        return config.idSerializers[streamType] as Serializer<I, String>
    }

    public fun getMetadataSerializer(): Serializer<EventMetadata, String> {
        return config.eventMetadataSerializer
    }
}
