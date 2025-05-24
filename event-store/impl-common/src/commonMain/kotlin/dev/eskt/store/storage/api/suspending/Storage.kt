package dev.eskt.store.storage.api.suspending

import dev.eskt.store.api.EventEnvelope
import dev.eskt.store.api.EventMetadata
import dev.eskt.store.api.StreamType
import dev.eskt.store.storage.api.StorageVersionMismatchException
import kotlin.coroutines.cancellation.CancellationException

public interface Storage {
    @Throws(StorageVersionMismatchException::class, CancellationException::class)
    public suspend fun <E, I> add(streamType: StreamType<E, I>, streamId: I, expectedVersion: Int, events: List<E>, metadata: EventMetadata)

    public suspend fun <E, I, R> useStreamEvents(streamType: StreamType<E, I>, streamId: I, sinceVersion: Int, consume: (Sequence<EventEnvelope<E, I>>) -> R): R

    public suspend fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int): List<EventEnvelope<E, I>>

    public suspend fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int, streamType: StreamType<E, I>): List<EventEnvelope<E, I>>
}
