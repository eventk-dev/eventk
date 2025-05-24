package dev.eventk.store.api.blocking

import dev.eventk.store.api.EventEnvelope
import dev.eventk.store.api.StreamType

public interface EventStore : dev.eventk.store.api.EventStore {
    public fun <E, I> loadEventBatch(
        sincePosition: Long,
        batchSize: Int = 1000,
    ): List<EventEnvelope<E, I>>

    public fun <E, I> loadEventBatch(
        sincePosition: Long,
        batchSize: Int = 1000,
        streamType: StreamType<E, I>,
    ): List<EventEnvelope<E, I>>

    public fun <E, I> withStreamType(
        type: StreamType<E, I>,
    ): StreamTypeHandler<E, I>
}
