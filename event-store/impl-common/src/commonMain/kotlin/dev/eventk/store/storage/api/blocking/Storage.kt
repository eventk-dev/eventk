package dev.eventk.store.storage.api.blocking

import dev.eventk.store.api.AppendResult
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
     * Load events from a stream and append new ones atomically, under a per-stream lock acquired before the load.
     *
     * The [consume] lambda receives the currently-stored envelopes (from [sinceVersion]) and returns events to append.
     * The same list is forwarded to [finalize] alongside the freshly-appended envelopes.
     *
     * If [consume] returns an empty list of events, no rows are written but the operation still commits successfully.
     */
    public fun <E, I, R> useStreamAndAppend(
        streamType: StreamType<E, I>,
        streamId: I,
        sinceVersion: Int,
        consume: (List<EventEnvelope<E, I>>) -> AppendResult<E>,
        finalize: (loaded: List<EventEnvelope<E, I>>, appended: List<EventEnvelope<E, I>>) -> R,
    ): R
}
