package dev.eskt.store.impl.pg

import dev.eskt.store.api.EventEnvelope
import dev.eskt.store.api.EventMetadata
import dev.eskt.store.api.Serializer
import dev.eskt.store.api.StreamType
import dev.eskt.store.storage.api.suspending.Storage

internal class PostgresqlR2dbcStorage(
    internal val config: PostgresqlConfig<ReactiveDataSource>,
) : Storage {
    private val registeredTypes = config.registeredTypes.associateBy { it.id }
    private val eventMetadataSerializer = config.eventMetadataSerializer

    private val databaseAdapter = config.dataSource.createAdapter()

    override suspend fun <E, I> add(streamType: StreamType<E, I>, streamId: I, expectedVersion: Int, events: List<E>, metadata: EventMetadata) {
        if (streamType.id !in registeredTypes) throw IllegalStateException("Unregistered type: $streamType")
        val serializedId = streamType.stringIdSerializer.serialize(streamId)
        val entries = events.mapIndexed { index, event ->
            DatabaseEntry(
                type = streamType.id,
                id = serializedId,
                version = expectedVersion + index + 1,
                eventPayload = streamType.stringEventSerializer.serialize(event),
                metadataPayload = eventMetadataSerializer.serialize(metadata),
            )
        }

        databaseAdapter.persistEntries(serializedId, expectedVersion, entries, config.tableInfo)
    }

    override suspend fun <E, I, R> useStreamEvents(
        streamType: StreamType<E, I>,
        streamId: I,
        sinceVersion: Int,
        consume: (Sequence<EventEnvelope<E, I>>) -> R,
    ): R {
        if (streamType.id !in registeredTypes) throw IllegalStateException("Unregistered type: $streamType")
        return databaseAdapter.useEntriesByStreamIdAndVersion(
            streamId = streamType.stringIdSerializer.serialize(streamId),
            sinceVersion = sinceVersion,
            tableInfo = config.tableInfo,
        ) { entries ->
            consume(entries.map { it.toEventEnvelope() })
        }
    }

    override suspend fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int): List<EventEnvelope<E, I>> {
        val entries = databaseAdapter.getEntryBatch(
            sincePosition = sincePosition,
            batchSize = batchSize,
            type = null,
            tableInfo = config.tableInfo,
        )
        return entries.map { entry -> entry.toEventEnvelope() }
    }

    override suspend fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int, streamType: StreamType<E, I>): List<EventEnvelope<E, I>> {
        val entries = databaseAdapter.getEntryBatch(
            sincePosition = sincePosition,
            batchSize = batchSize,
            type = streamType.id,
            tableInfo = config.tableInfo,
        )
        return entries.map { entry -> entry.toEventEnvelope() }
    }

    private fun <E, I> DatabaseEntry.toEventEnvelope(): EventEnvelope<E, I> {
        val streamType = config.streamType<E, I, StreamType<E, I>>(type)
        return EventEnvelope(
            streamType = streamType,
            streamId = streamType.stringIdSerializer.deserialize(id),
            version = version,
            position = position,
            event = streamType.stringEventSerializer.deserialize(eventPayload),
            metadata = eventMetadataSerializer.deserialize(metadataPayload),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val <E, I> StreamType<E, I>.stringIdSerializer: Serializer<I, String>
        get() = config.idSerializers[this] as Serializer<I, String>

    @Suppress("UNCHECKED_CAST")
    private val <E, I> StreamType<E, I>.stringEventSerializer: Serializer<E, String>
        get() = config.payloadSerializers[this] as Serializer<E, String>
}
