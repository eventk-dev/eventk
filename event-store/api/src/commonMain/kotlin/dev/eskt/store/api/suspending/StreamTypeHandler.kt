package dev.eskt.store.api.suspending

import dev.eskt.store.api.EventEnvelope
import dev.eskt.store.api.EventMetadata
import dev.eskt.store.api.StreamType
import dev.eskt.store.api.StreamVersionMismatchException

public interface StreamTypeHandler<E, I> {
    public val streamType: StreamType<E, I>

    /**
     * Load events from an event stream.
     */
    public suspend fun loadStream(
        streamId: I,
        sinceVersion: Int = 0,
    ): List<EventEnvelope<E, I>>

    /**
     * Allow events from the stream to be consumed through a [Sequence] in a safe way.
     * Events from the stream will be loaded progressively as the sequence is iterated on,
     * and all underlying resources will be closed by the end of this call.
     */
    public suspend fun <R> useStream(
        streamId: I,
        sinceVersion: Int = 0,
        consume: (Sequence<EventEnvelope<E, I>>) -> R,
    ): R

    /**
     * Append new events into an event stream.
     *
     * @return the version of the aggregate after those events are appended, or;
     * @throws [StreamVersionMismatchException] when the stream currently is not in the expected version.
     */
    public suspend fun appendStream(
        streamId: I,
        expectedVersion: Int,
        events: List<E>,
        metadata: EventMetadata = emptyMap(),
    ): Int
}
