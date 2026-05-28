package dev.eventk.store.storage.api.blocking

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.EventMetadata
import dev.eventk.store.api.StreamType
import dev.eventk.store.storage.api.StorageVersionMismatchException

public interface Storage {
    @Throws(StorageVersionMismatchException::class)
    public fun <E, I> add(streamType: StreamType<E, I>, streamId: I, expectedVersion: Int, events: List<E>, metadata: EventMetadata)

    public fun <E, I, R> useStreamEvents(streamType: StreamType<E, I>, streamId: I, sinceVersion: Int, consume: (Sequence<EventEnvelope<E, I>>) -> R): R

    public fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int): List<EventEnvelope<E, I>>

    public fun <E, I> loadEventBatch(sincePosition: Long, batchSize: Int, streamType: StreamType<E, I>): List<EventEnvelope<E, I>>

    /**
     * Load events from a stream and optionally append new ones atomically, under a per-stream lock held for the
     * duration of [block]. The [appendStream] function provided to [block] may be invoked at most once.
     */
    public fun <E, I, R> loadStreamForAppend(
        streamType: StreamType<E, I>,
        streamId: I,
        sinceVersion: Int,
        block: (
            loaded: List<EventEnvelope<E, I>>,
            appendStream: (events: List<E>, metadata: EventMetadata) -> List<EventEnvelope<E, I>>,
        ) -> R,
    ): R
}
